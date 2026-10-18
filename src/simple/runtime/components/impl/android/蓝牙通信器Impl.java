package simple.runtime.components.impl.android;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;

import simple.runtime.android.MainActivity;
import simple.runtime.collections.集合;
import simple.runtime.components.组件容器;
import simple.runtime.components.蓝牙通信器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.components.impl.android.util.bluetooth.AcceptThread;
import simple.runtime.components.impl.android.util.bluetooth.ConnectThread;
import simple.runtime.components.impl.android.util.bluetooth.ConnectedThread;
import simple.runtime.components.impl.android.util.bluetooth.BluetoothUuids;
import simple.runtime.events.EventDispatcher;
import simple.runtime.errors.运行错误;
import simple.runtime.variants.ArrayVariant;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 经典蓝牙通信实现，协调 UI 线程事件与可取消的 RFCOMM 连接线程。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class 蓝牙通信器Impl extends 组件Impl implements 蓝牙通信器, MainActivity.OnActivityResultListener {

	private static final AtomicInteger NEXT_REQUEST = new AtomicInteger(0x4000);
	private final int enableRequest = NEXT_REQUEST.getAndAdd(2);
	private final int discoverableRequest = enableRequest + 1;
	private final MainActivity activity;
	private final BluetoothAdapter adapter;
	private final Handler handler;
	private final BroadcastReceiver receiver;
	private AcceptThread acceptThread;
	private ConnectThread connectThread;
	private ConnectedThread connectedThread;
	private BluetoothSocket socket;
	private UUID service;
	private boolean chatMode;
	private boolean enablePending;
	private boolean discoverablePending;
	private boolean receiverRegistered;
	private boolean destroyed;

	public 蓝牙通信器Impl(组件容器 container) {
		super(container);

		if (discoverableRequest > 0x4fff) {
			throw new IllegalStateException("蓝牙通信器请求标识已耗尽");
		}

		activity = MainActivity.getContext();
		adapter = BluetoothAdapter.getDefaultAdapter();
		handler = new Handler(Looper.getMainLooper()) {
			@Override
			public void handleMessage(Message message) {
				handleConnectionMessage(message);
			}
		};

		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (destroyed || !BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
					return;
				}

				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (device != null) {
					发现设备(device.getName(), device.getAddress(), device.getBondState() == BluetoothDevice.BOND_BONDED);
				}
			}
		};

		if (adapter != null) {
			activity.registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
			receiverRegistered = true;
		}

		activity.addOnActivityResultListener(this);
	}

	/* 组件事件 */

	@Override
	public void 蓝牙设置完毕(int result) {
		EventDispatcher.dispatchEvent(this, "蓝牙设置完毕", result);
	}

	@Override
	public void 发现设备(String name, String address, boolean bonded) {
		EventDispatcher.dispatchEvent(this, "发现设备", name, address, bonded);
	}

	@Override
	public void 连接完毕(boolean success, String name, String address, int mode) {
		EventDispatcher.dispatchEvent(this, "连接完毕", success, name, address, mode);
	}

	@Override
	public void 收到数据(byte[] data, String name, String address) {
		EventDispatcher.dispatchEvent(this, "收到数据", data, name, address);
	}

	@Override
	public void 发送完毕(byte[] data, String name, String address) {
		EventDispatcher.dispatchEvent(this, "发送完毕", data, name, address);
	}

	@Override
	public void 连接断开() {
		EventDispatcher.dispatchEvent(this, "连接断开");
	}

	/* 组件方法 */

	@Override
	public void 打开蓝牙设置() {
		activity.startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
	}

	@Override
	public boolean 是否存在() {
		return adapter != null;
	}

	@Override
	public boolean 是否已开启() {
		return adapter != null && adapter.isEnabled();
	}

	@Override
	public void 开启蓝牙() {
		if (available() && !enablePending) {
			enablePending = true;

			try {
				activity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), enableRequest);
			} catch (RuntimeException error) {
				enablePending = false;
				throw error;
			}
		}
	}

	@Override
	public void 关闭蓝牙() {
		if (available()) {
			disconnect();
			if (!adapter.disable()) {
				throw new BluetoothOperationError("系统未接受关闭蓝牙请求");
			}
		}
	}

	@Override
	public void 置可被发现() {
		if (available() && !discoverablePending) {
			discoverablePending = true;

			try {
				activity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE), discoverableRequest);
			} catch (RuntimeException error) {
				discoverablePending = false;
				throw error;
			}
		}
	}

	@Override
	public void 置工作模式(int mode) {
		if (!available()) {
			return;
		}

		if (acceptThread != null) {
			acceptThread.cancel();
			acceptThread = null;
		}

		chatMode = mode == 1;
		if (chatMode) {
			acceptThread = new AcceptThread(handler, adapter, service == null ? BluetoothUuids.CHAT : service);
			acceptThread.start();
		}
	}

	@Override
	public void 置UUID(String uuid) {
		service = uuid == null || uuid.isEmpty() ? null : UUID.fromString(uuid);
	}

	@Override
	public boolean 置本机名称(String name) {
		return available() && adapter.setName(name);
	}

	@Override
	public String 取本机名称() {
		return adapter == null || adapter.getName() == null ? "" : adapter.getName();
	}

	@Override
	public String 取本机地址() {
		return adapter == null || adapter.getAddress() == null ? "" : adapter.getAddress();
	}

	@Override
	public 集合 取已配对设备() {
		集合 devices = new 集合();
		if (available()) {
			for (BluetoothDevice device : adapter.getBondedDevices()) {
				devices.加入(ArrayVariant.getArrayVariant(new String[] { device.getName(), device.getAddress() }));
			}
		}

		return devices;
	}

	@Override
	public void 搜索设备() {
		if (!available()) {
			throw new BluetoothOperationError("蓝牙适配器不可用或组件已销毁");
		}

		try {
			adapter.cancelDiscovery();
			if (!adapter.startDiscovery()) {
				throw new BluetoothOperationError("系统未接受蓝牙搜索请求，请检查蓝牙状态与权限");
			}
		} catch (SecurityException error) {
			throw new BluetoothOperationError("蓝牙搜索权限不足");
		}
	}

	@Override
	public void 停止搜索() {
		if (available()) {
			adapter.cancelDiscovery();
		}
	}

	@Override
	public void 连接设备(String address) {
		if (!available()) {
			连接完毕(false, "", "", 0);
			return;
		}

		BluetoothDevice device = adapter.getRemoteDevice(address);
		adapter.cancelDiscovery();
		disconnect();
		UUID chosen = service == null ? (chatMode ? BluetoothUuids.CHAT : BluetoothUuids.SERIAL) : service;
		connectThread = new ConnectThread(device, handler, chosen);
		connectThread.start();
	}

	@Override
	public void 发送数据(byte[] data) {
		if (connectedThread == null) {
			throw new BluetoothOperationError("尚未连接蓝牙设备");
		}

		if (data == null) {
			throw new IllegalArgumentException("发送数据不能为空引用");
		}

		connectedThread.write(data);
	}

	@Override
	public void 断开连接() {
		boolean connected = socket != null;
		disconnect();
		if (connected && !destroyed) {
			连接断开();
		}
	}

	@Override
	public String 取已连接设备名称() {
		return socket == null || socket.getRemoteDevice().getName() == null ? "" : socket.getRemoteDevice().getName();
	}

	@Override
	public String 取已连接设备地址() {
		return socket == null ? "" : socket.getRemoteDevice().getAddress();
	}

	@Override
	public void 销毁() {
		if (destroyed) {
			return;
		}

		destroyed = true;
		enablePending = false;
		discoverablePending = false;

		// 权限被撤销时停止搜索可能失败，仍须关闭连接并撤销监听。
		try {
			if (adapter != null) {
				adapter.cancelDiscovery();
			}
		} catch (RuntimeException error) {
			Log.w("蓝牙", "销毁时停止搜索失败", error);
		}

		try {
			disconnect();
		} finally {
			handler.removeCallbacksAndMessages(null);

			try {
				if (receiverRegistered) {
					receiverRegistered = false;
					activity.unregisterReceiver(receiver);
				}
			} finally {
				activity.removeOnActivityResultListener(this);
				super.销毁();
			}
		}
	}

	/* 内部接口与平台回调 */

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (destroyed) {
			return;
		}

		if (requestCode == enableRequest && enablePending) {
			enablePending = false;
			蓝牙设置完毕(resultCode == Activity.RESULT_OK ? 1 : 2);
		} else if (requestCode == discoverableRequest && discoverablePending) {
			discoverablePending = false;

			// 成功返回可发现时长，不固定为 120 秒。
			蓝牙设置完毕(resultCode > 0 ? 3 : 4);
		}
	}

	/* 内部辅助方法 */

	private void handleConnectionMessage(Message message) {
		if (destroyed) {
			return;
		}

		// 消息携带发出它的线程身份；取消后排队到达的旧消息不得接管新连接。
		switch (message.what) {
			case ConnectThread.FAILED:
				if (message.obj == connectThread) {
					connectThread = null;
					连接完毕(false, "", "", 0);
				}

				break;
			case ConnectThread.CONNECTED:
				if (message.obj == connectThread) {
					BluetoothSocket connection = connectThread.takeSocket();
					connectThread.cancel();
					connectThread = null;
					beginConnection(connection, 1);
				}

				break;
			case AcceptThread.FAILED:
				if (message.obj == acceptThread) {
					acceptThread.cancel();
					acceptThread = null;
					连接完毕(false, "", "", 0);
				}

				break;
			case AcceptThread.CONNECTED:
				if (message.obj == acceptThread) {
					BluetoothSocket connection = acceptThread.takeSocket();
					acceptThread.cancel();
					acceptThread = null;
					beginConnection(connection, 2);
				}

				break;
			case ConnectedThread.RECEIVED:
			case ConnectedThread.SENT:
				if (message.obj == connectedThread && socket != null) {
					BluetoothDevice device = socket.getRemoteDevice();
					byte[] data = message.getData().getByteArray("data");

					if (message.what == ConnectedThread.RECEIVED) {
						收到数据(data, device.getName(), device.getAddress());
					} else {
						发送完毕(data, device.getName(), device.getAddress());
					}
				}

				break;
			case ConnectedThread.DISCONNECTED:
				if (message.obj == connectedThread) {
					disconnect();
					连接断开();
				}

				break;
			default:
				break;
		}
	}

	private void beginConnection(BluetoothSocket connection, int mode) {
		if (connection == null) {
			return;
		}

		// 连接线程已交出套接字；从此由数据线程负责关闭。
		disconnect();
		socket = connection;
		connectedThread = new ConnectedThread(connection, handler);
		connectedThread.start();
		BluetoothDevice device = connection.getRemoteDevice();
		连接完毕(true, device.getName(), device.getAddress(), mode);
	}

	/**
	 * 让无返回值操作的失败也能由 Simple 的错误分支处理。
	 */
	@SuppressWarnings("serial")
	private static final class BluetoothOperationError extends 运行错误 {

		BluetoothOperationError(String message) {
			super(message);
		}
	}

	private boolean available() {
		return !destroyed && adapter != null;
	}

	// 关闭套接字才能打断阻塞 I/O；仅调用 Thread.interrupt 无法释放这些资源。
	private void disconnect() {
		if (connectThread != null) {
			connectThread.cancel();
			connectThread = null;
		}

		if (acceptThread != null) {
			acceptThread.cancel();
			acceptThread = null;
		}

		if (connectedThread != null) {
			connectedThread.cancel();
			connectedThread = null;
		}

		socket = null;
	}
}

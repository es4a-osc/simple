package simple.runtime.components.impl.android.util.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * 发起一条 RFCOMM 连接；每次请求固定服务标识，取消时关闭阻塞连接使用的套接字。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class ConnectThread extends Thread {

	public static final int FAILED = 4;
	public static final int CONNECTED = 5;
	private final BluetoothDevice device;
	private final Handler handler;
	private final UUID service;
	private BluetoothSocket socket;
	private volatile boolean cancelled;

	public ConnectThread(BluetoothDevice device, Handler handler, UUID service) {
		this.device = device;
		this.handler = handler;
		this.service = service;
	}

	@Override
	public void run() {
		try {
			BluetoothSocket connection = device.createRfcommSocketToServiceRecord(service);

			synchronized (this) {
				if (cancelled) {
					connection.close();
					return;
				}

				socket = connection;
			}

			connection.connect();
			if (!cancelled) {
				handler.sendMessage(handler.obtainMessage(CONNECTED, this));
			}
		} catch (IOException | RuntimeException error) {
			boolean report = !cancelled;
			cancel();
			if (report) {
				handler.sendMessage(handler.obtainMessage(FAILED, this));
			}
		}
	}

	/**
	 * 取出并移交线程持有的套接字，后续清理不再关闭已移交连接。
	 *
	 * @return 移交给调用方的套接字；没有可移交连接时为 {@code null}
	 */
	public synchronized BluetoothSocket takeSocket() {
		BluetoothSocket result = socket;
		socket = null;
		return result;
	}

	/**
	 * 取消当前任务并释放相应资源。
	 */
	public synchronized void cancel() {
		cancelled = true;
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException error) {
				Log.w("蓝牙", "关闭连接失败", error);
			}

			socket = null;
		}
	}
}

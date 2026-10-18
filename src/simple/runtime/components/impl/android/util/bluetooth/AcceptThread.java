package simple.runtime.components.impl.android.util.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * 接收一条 RFCOMM 连接；取消时关闭服务器套接字，使阻塞的 accept 真正退出。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class AcceptThread extends Thread {

	public static final int CONNECTED = 6;
	public static final int FAILED = 10;
	private final Handler handler;
	private final BluetoothAdapter adapter;
	private final UUID service;
	private BluetoothServerSocket serverSocket;
	private BluetoothSocket socket;
	private volatile boolean cancelled;

	public AcceptThread(Handler handler, BluetoothAdapter adapter, UUID service) {
		this.handler = handler;
		this.adapter = adapter;
		this.service = service;
	}

	@Override
	public void run() {
		try {
			BluetoothServerSocket server = adapter.listenUsingRfcommWithServiceRecord("Simple", service);

			synchronized (this) {
				if (cancelled) {
					server.close();
					return;
				}

				serverSocket = server;
			}

			BluetoothSocket accepted = server.accept();

			synchronized (this) {
				if (cancelled) {
					accepted.close();
					return;
				}

				socket = accepted;
			}

			handler.sendMessage(handler.obtainMessage(CONNECTED, this));
		} catch (IOException | RuntimeException error) {
			if (!cancelled) {
				Log.e("蓝牙", "接收连接失败", error);
				handler.sendMessage(handler.obtainMessage(FAILED, this));
			}
		} finally {
			closeServer();
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
	 * 关闭监听套接字。
	 */
	private synchronized void closeServer() {
		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException error) {
				Log.w("蓝牙", "关闭监听套接字失败", error);
			}

			serverSocket = null;
		}
	}

	/**
	 * 取消当前任务并释放相应资源。
	 */
	public synchronized void cancel() {
		cancelled = true;
		closeServer();
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException error) {
				Log.w("蓝牙", "关闭待接收连接失败", error);
			}

			socket = null;
		}
	}
}

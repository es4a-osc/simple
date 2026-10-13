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
			}
		} finally {
			closeServer();
		}
	}

	// 交出已接受的连接；关闭服务器监听不影响已交给组件的数据连接。
	public synchronized BluetoothSocket takeSocket() {
		BluetoothSocket result = socket;
		socket = null;
		return result;
	}

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

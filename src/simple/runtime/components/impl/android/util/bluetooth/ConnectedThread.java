package simple.runtime.components.impl.android.util.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * RFCOMM 数据收发；关闭套接字解除阻塞读取，流结束只通知一次断开。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class ConnectedThread extends Thread {

	public static final int RECEIVED = 7;
	public static final int SENT = 8;
	public static final int DISCONNECTED = 9;
	private final BluetoothSocket socket;
	private final Handler handler;
	private volatile boolean cancelled;
	private boolean disconnected;

	public ConnectedThread(BluetoothSocket socket, Handler handler) {
		this.socket = socket;
		this.handler = handler;
	}

	@Override
	public void run() {
		byte[] buffer = new byte[1024];

		try {
			InputStream input = socket.getInputStream();

			while (!cancelled) {
				int size = input.read(buffer);

				// EOF 是连接结束，不能继续空转读取同一条流。
				if (size < 0) {
					break;
				}

				if (size > 0) {
					sendData(Arrays.copyOf(buffer, size), RECEIVED);
				}
			}
		} catch (IOException error) {
			if (!cancelled) {
				Log.w("蓝牙", "读取数据失败", error);
			}
		} finally {
			notifyDisconnected();
			closeSocket();
		}
	}

	/**
	 * 写出当前数据。
	 *
	 * @param data 本次传入的数据
	 */
	public synchronized void write(byte[] data) {
		if (cancelled || disconnected) {
			return;
		}

		// 调用者可以继续修改原数组；发送事件必须保留实际写出的那份数据。
		byte[] snapshot = data.clone();

		try {
			socket.getOutputStream().write(snapshot);
			sendData(snapshot, SENT);
		} catch (IOException error) {
			Log.w("蓝牙", "发送数据失败", error);
			notifyDisconnected();
			closeSocket();
		}
	}

	/**
	 * 发送接收数据及其类型对应的消息。
	 *
	 * @param data 本次传入的数据
	 * @param kind 操作或数据类型
	 */
	private void sendData(byte[] data, int kind) {
		if (cancelled) {
			return;
		}

		Message message = handler.obtainMessage(kind, this);
		Bundle bundle = new Bundle();

		bundle.putByteArray("data", data);
		message.setData(bundle);

		handler.sendMessage(message);
	}

	/**
	 * 通知连接已经断开。
	 */
	private synchronized void notifyDisconnected() {
		if (!cancelled && !disconnected) {
			disconnected = true;
			handler.sendMessage(handler.obtainMessage(DISCONNECTED, this));
		}
	}

	/**
	 * 关闭线程持有的蓝牙套接字。
	 */
	private void closeSocket() {
		try {
			socket.close();
		} catch (IOException error) {
			Log.w("蓝牙", "关闭数据连接失败", error);
		}
	}

	/**
	 * 取消当前任务并释放相应资源。
	 */
	public void cancel() {
		cancelled = true;
		closeSocket();
	}
}

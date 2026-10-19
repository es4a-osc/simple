package simple.runtime.components.impl.android;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import simple.runtime.components.组件容器;
import simple.runtime.components.TCP客户端;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Android TCP客户端实现。
 *
 * @author 树先生 xhwsd@qq.com
 */
public class TCP客户端Impl extends 组件Impl implements TCP客户端 {

	/* 消息标识 */

	private static final int CONNECT_FAIL = 1;
	private static final int CONNECT_SUCCEED = 2;
	private static final int RECEIVE_MSG = 3;
	private static final int DISCONNECT = 4;

	/* 连接及接收状态 */

	private ConnectThread connectThread;
	private ConnectedThread connectedThread;
	private Socket socket;
	private boolean stop = true;
	private int receiveSize = 1024;

	// 沿用原组件创建线程的消息循环，网络线程只投递消息。
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case CONNECT_SUCCEED: {
					TCP客户端Impl.this.socket = TCP客户端Impl.this.connectThread.getSocket();
					TCP客户端Impl.this.stop = false;

					// 连接结果回到组件线程后，建立接收线程并通知连接成功。
					TCP客户端Impl.this.connectedThread = new ConnectedThread(TCP客户端Impl.this.socket);
					TCP客户端Impl.this.connectedThread.start();
					TCP客户端Impl.this.连接完毕(true);
					break;
				}
				case CONNECT_FAIL: {
					TCP客户端Impl.this.连接完毕(false);
					break;
				}
				case RECEIVE_MSG: {
					byte[] received = msg.getData().getByteArray("byte");
					TCP客户端Impl.this.收到数据(received);
					break;
				}
				case DISCONNECT: {
					TCP客户端Impl.this.断开连接();
					TCP客户端Impl.this.连接断开();
				}
			}
		}
	};

	/**
	 * 创建不可视网络组件；消息处理器绑定组件创建线程的消息循环。
	 *
	 * @param container 容纳组件的窗口。
	 */
	public TCP客户端Impl(组件容器 container) {
		super(container);
	}

	/* 组件事件 */

	@Override
	public void 连接完毕(boolean result) {
		EventDispatcher.dispatchEvent(this, "连接完毕", result);
	}

	@Override
	public void 收到数据(byte[] data) {
		EventDispatcher.dispatchEvent(this, "收到数据", data);
	}

	@Override
	public void 连接断开() {
		EventDispatcher.dispatchEvent(this, "连接断开");
	}

	/* 组件方法 */

	@Override
	public void 置接收数据包大小(int size) {
		this.receiveSize = size;
	}

	@Override
	public void 连接服务器(String address, int port, int timeout) {
		this.connectThread = new ConnectThread(address, port, timeout);
		this.connectThread.start();
	}

	@Override
	public boolean 取连接状态() {
		boolean result = false;
		if (this.socket != null) {
			try {
				this.socket.sendUrgentData(255);
				result = true;
			} catch (IOException e) {
				e.printStackTrace();
				result = false;
			}
		}

		return result;
	}

	@Override
	public void 发送数据(byte[] data) {
		if (this.socket != null) {
			this.connectedThread.writeByte(data);
		}
	}

	@Override
	public void 断开连接() {
		this.stop = true;
		if (this.socket != null) {
			try {
				this.socket.shutdownInput();
				this.socket.shutdownOutput();
				InputStream in = this.socket.getInputStream();
				OutputStream ou = this.socket.getOutputStream();

				in.close();
				ou.close();
				this.socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (this.connectThread != null) {
			this.connectThread.interrupt();
		}

		if (this.connectedThread != null) {
			this.connectedThread.interrupt();
		}
	}

	/* 内部辅助方法 */

	/**
	 * 取得当前接收缓冲区大小。
	 *
	 * @return 缓冲区大小，单位字节。
	 */
	private int getReceiveSize() {
		return this.receiveSize;
	}

	/* 内部线程 */

	/**
	 * 接收服务端字节并执行同步写入，不增加消息边界协议。
	 */
	private class ConnectedThread extends Thread {
		private InputStream input = null;
		private OutputStream output = null;
		private Socket socket;

		ConnectedThread(Socket s) {
			this.socket = s;
		}

		@Override
		public void run() {
			int size = 0;

			while (!TCP客户端Impl.this.stop) {
				Message msg;
				byte[] buf = new byte[TCP客户端Impl.this.getReceiveSize()];

				try {
					this.input = this.socket.getInputStream();
					size = this.input.read(buf);
				} catch (IOException e) {
					e.printStackTrace();
					size = 0;
					TCP客户端Impl.this.stop = true;
					msg = new Message();
					msg.what = DISCONNECT;
					TCP客户端Impl.this.handler.sendMessage(msg);
				}

				if (size <= 0) {
					continue;
				}

				// 仅复制本次读取到的字节，不解析应用协议。
				byte[] buf2 = new byte[size];
				System.arraycopy(buf, 0, buf2, 0, size);

				Bundle bundle = new Bundle();
				bundle.putByteArray("byte", buf2);
				msg = new Message();
				msg.setData(bundle);
				msg.what = RECEIVE_MSG;
				TCP客户端Impl.this.handler.sendMessage(msg);
			}
		}

		/**
		 * 向连接输出流写入字节数据。
		 *
		 * @param buf 要写出的数据缓冲区
		 */
		public void writeByte(byte[] buf) {
			try {
				if (this.socket.isConnected() && !this.socket.isClosed()) {
					this.output = this.socket.getOutputStream();
					this.output.write(buf);
					this.output.flush();
				}
			} catch (IOException e) {
				e.printStackTrace();
				Message msg = new Message();
				msg.what = DISCONNECT;
				TCP客户端Impl.this.handler.sendMessage(msg);
			}
		}

		/**
		 * 向连接输出流写入整数数据。
		 *
		 * @param buf 要写出的数据缓冲区
		 * @return 操作成功时为 {@code true}，失败时为 {@code false}
		 */
		public boolean writeInt(int buf) {
			try {
				this.output = this.socket.getOutputStream();
				this.output.write(buf);
				this.output.flush();
				return true;
			} catch (IOException e) {
				e.printStackTrace();
				Message msg = new Message();
				msg.what = DISCONNECT;
				TCP客户端Impl.this.handler.sendMessage(msg);
				return false;
			}
		}
	}

	/**
	 * 在后台创建并连接客户端 Socket，按原逻辑投递连接结果。
	 */
	private class ConnectThread extends Thread {
		private Socket socket;
		private String address;
		private int port;
		private int timeout;

		ConnectThread(String ip, int port, int timeout) {
			this.address = ip;
			this.port = port;
			this.timeout = timeout;
		}

		@Override
		public void run() {
			try {
				this.socket = new Socket();
				this.socket.setOOBInline(true);
				this.socket.setKeepAlive(true);
				this.socket.setTcpNoDelay(true);
				this.socket.setSendBufferSize(TCP客户端Impl.this.receiveSize);
				this.socket.setReceiveBufferSize(TCP客户端Impl.this.receiveSize);

				// 套接字选项准备完成后再执行带超时的连接，结果由消息处理器转为事件。
				this.socket.connect(new InetSocketAddress(this.address, this.port), this.timeout);
			} catch (IOException e) {
				TCP客户端Impl.this.handler.sendEmptyMessage(CONNECT_FAIL);
				this.socket = null;
			}

			if (this.socket != null) {
				Message msg = new Message();
				msg.what = CONNECT_SUCCEED;
				TCP客户端Impl.this.handler.sendMessage(msg);
			}
		}

		/**
		 * 返回连接线程保存的套接字。
		 *
		 * @return 当前连接线程保存的套接字
		 */
		public Socket getSocket() {
			return this.socket;
		}
	}
}

package simple.runtime.components.impl.android;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import simple.runtime.components.组件容器;
import simple.runtime.components.TCP服务端;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Android TCP服务端实现。
 *
 * @author 树先生 xhwsd@qq.com
 */
public class TCP服务端Impl extends 组件Impl implements TCP服务端 {

	/* 消息标识 */

	private static final int CLIENT_ENTER = 1;
	private static final int RECEIVE_MSG = 2;
	private static final int CLIENT_LEAVE = 3;

	/* 连接及接收状态 */

	private WaitClientThread waitThread;
	private ServerSocket serverSocket = null;
	private boolean started = false;
	private List<Client> clients = new ArrayList<Client>();
	private int receiveSize = 1024;

	// 沿用原组件创建线程的消息循环，网络线程只投递消息。
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case CLIENT_ENTER: {
					String[] temp1 = msg.getData().getStringArray("IP");
					TCP服务端Impl.this.客户进入(temp1[0], Integer.parseInt(temp1[1]));
					break;
				}
				case RECEIVE_MSG: {
					byte[] data = msg.getData().getByteArray("DATA");
					String[] temp2 = msg.getData().getStringArray("IP");

					TCP服务端Impl.this.收到数据(data, temp2[0], Integer.parseInt(temp2[1]));
					break;
				}
				case CLIENT_LEAVE: {
					String[] temp3 = msg.getData().getStringArray("IP");
					TCP服务端Impl.this.客户离开(temp3[0], Integer.parseInt(temp3[1]));
				}
			}
		}
	};

	/**
	 * 创建不可视网络组件；消息处理器绑定组件创建线程的消息循环。
	 *
	 * @param container 容纳组件的窗口。
	 */
	public TCP服务端Impl(组件容器 container) {
		super(container);
	}

	/* 组件事件 */

	@Override
	public void 客户进入(String address, int port) {
		EventDispatcher.dispatchEvent(this, "客户进入", address, port);
	}

	@Override
	public void 收到数据(byte[] data, String address, int port) {
		EventDispatcher.dispatchEvent(this, "收到数据", data, address, port);
	}

	@Override
	public void 客户离开(String address, int port) {
		EventDispatcher.dispatchEvent(this, "客户离开", address, port);
	}

	/* 组件方法 */

	@Override
	public void 置接收数据包大小(int size) {
		this.receiveSize = size;
	}

	@Override
	public void 启动服务器(int port) {
		this.waitThread = new WaitClientThread(port);
		this.waitThread.start();
	}

	@Override
	public void 发送数据(byte[] data, String address, int port) {
		if (this.started) {
			for (int i = 0; i < this.clients.size(); ++i) {
				Client c = this.clients.get(i);
				String temp = this.getAddress(c.s.getInetAddress());

				if (!temp.equals(address) || c.s.getPort() != port) {
					continue;
				}

				c.send(data);
				break;
			}
		}
	}

	@Override
	public void 关闭服务器() {
		this.started = false;
		if (this.serverSocket != null) {
			try {
				this.serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (this.waitThread != null) {
			this.waitThread.interrupt();
		}

		this.clients.clear();
	}

	/* 内部辅助方法 */

	/**
	 * 按原组件规则从网络地址取得地址文本。
	 *
	 * @param address 网络地址对象。
	 * @return IP 地址文本；无法分离地址时为空文本。
	 */
	private String getAddress(InetAddress address) {
		String local = address.toString();
		String[] temp = local.split("/");

		if (temp.length == 2) {
			return temp[1];
		}

		return "";
	}

	/**
	 * 保留原组件通过紧急数据探测客户连接的辅助方法。
	 *
	 * @param socket 客户连接 Socket。
	 * @return 紧急数据发送成功时返回真。
	 */
	private boolean isClientConnected(Socket socket) {
		boolean result = false;
		if (socket != null) {
			try {
				socket.sendUrgentData(255);
				result = true;
			} catch (IOException e) {
				e.printStackTrace();
				result = false;
			}
		}

		return result;
	}

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
	 * 保存单个客户连接的输入输出流并接收字节。
	 */
	private class Client implements Runnable {
		private Socket s = null;
		private InputStream input = null;
		private OutputStream output = null;
		private boolean connected = false;
		private String sendmsg = null;

		Client(Socket s) {
			this.s = s;

			try {
				this.input = s.getInputStream();
				this.output = s.getOutputStream();
				this.connected = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * 向当前连接发送数据。
		 *
		 * @param buf 要写出的数据缓冲区
		 */
		public void send(byte[] buf) {
			try {
				this.output.write(buf);
				this.output.flush();
			} catch (IOException e) {
				TCP服务端Impl.this.clients.remove(this);
			}
		}

		@Override
		public void run() {
			int size = 0;

			while (this.connected) {
				byte[] buf = new byte[TCP服务端Impl.this.getReceiveSize()];

				try {
					size = this.input.read(buf);
				} catch (IOException e) {
					e.printStackTrace();
					size = 0;
				}

				if (size <= 0) {
					continue;
				}

				// 仅复制本次读取到的字节，不解析应用协议。
				byte[] buf2 = new byte[size];
				System.arraycopy(buf, 0, buf2, 0, size);
				String[] temp = new String[]{TCP服务端Impl.this.getAddress(this.s.getInetAddress()), Integer.toString(this.s.getPort())};

				Bundle bundle = new Bundle();
				bundle.putByteArray("DATA", buf2);
				bundle.putStringArray("IP", temp);
				Message msg = new Message();
				msg.setData(bundle);
				msg.what = RECEIVE_MSG;
				TCP服务端Impl.this.handler.sendMessage(msg);
			}
		}
	}

	/**
	 * 绑定监听端口并等待网络数据或客户连接。
	 */
	private class WaitClientThread extends Thread {
		private int port;

		WaitClientThread(int port) {
			this.port = port;
		}

		@Override
		public void run() {
			try {
				// 监听端口绑定成功后才置为已启动，后续循环按该状态等待连接。
				TCP服务端Impl.this.serverSocket = new ServerSocket(this.port);
				TCP服务端Impl.this.started = true;
			} catch (BindException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				while (TCP服务端Impl.this.started) {
					Socket s = TCP服务端Impl.this.serverSocket.accept();

					// 接入信息先投递到组件消息处理器，网络线程不直接触发界面事件。
					String[] temp = new String[]{TCP服务端Impl.this.getAddress(s.getInetAddress()), Integer.toString(s.getPort())};
					Bundle bundle = new Bundle();

					bundle.putStringArray("IP", temp);
					Message msg = new Message();
					msg.setData(bundle);
					msg.what = CLIENT_ENTER;
					TCP服务端Impl.this.handler.sendMessage(msg);

					// 每个客户端创建独立接收线程，再记录到当前客户端列表。
					Client c = new Client(s);
					new Thread(c).start();
					TCP服务端Impl.this.clients.add(c);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

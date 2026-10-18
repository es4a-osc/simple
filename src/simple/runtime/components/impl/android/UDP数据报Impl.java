package simple.runtime.components.impl.android;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import simple.runtime.components.组件容器;
import simple.runtime.components.UDP数据报;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Android UDP数据报实现。
 */
public class UDP数据报Impl extends 组件Impl implements UDP数据报 {


	/* 消息标识 */

	private static final int RECEIVE_MSG = 1;

	/* 连接及接收状态 */

	private DatagramSocket socket;
	private WaitClientThread waitThread;
	private boolean started = false;
	private int receiveSize = 1024;

	// 沿用原组件创建线程的消息循环，网络线程只投递消息。
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == RECEIVE_MSG) {
				byte[] data = msg.getData().getByteArray("RecMsg");
				String remoteAddress = msg.getData().getString("RemoteIP");
				int remotePort = msg.getData().getInt("RemotePort");

				UDP数据报Impl.this.收到数据(data, remoteAddress, remotePort);
			}
		}
	};

	/**
	 * 创建不可视网络组件；消息处理器绑定组件创建线程的消息循环。
	 *
	 * @param container 容纳组件的窗口。
	 */
	public UDP数据报Impl(组件容器 container) {
		super(container);
	}

	/* 组件事件 */

	@Override
	public void 收到数据(byte[] data, String address, int port) {
		EventDispatcher.dispatchEvent(this, "收到数据", data, address, port);
	}

	/* 组件方法 */

	@Override
	public void 置接收数据包大小(int size) {
		this.receiveSize = size;
	}

	@Override
	public void 开始监听(int port) {
		this.waitThread = new WaitClientThread(port);
		this.waitThread.start();
	}

	@Override
	public void 发送数据(byte[] data, String address, int port) {
		try {
			DatagramPacket packet = new DatagramPacket(data, data.length);
			if (address.equals("")) {
				address = "255.255.255.255";
			}

			packet.setAddress(InetAddress.getByName(address));
			packet.setPort(port);
			packet.setData(data);
			this.socket.send(packet);
		} catch (Exception exception) {
			// 保留原组件忽略发送异常的处理，不增加错误事件。
		}
	}

	@Override
	public void 停止监听() {
		this.started = false;
		if (this.socket != null) {
			try {
				this.socket.close();
				this.socket = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (this.waitThread != null) {
			this.waitThread.interrupt();
		}
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
	 * 取得当前接收缓冲区大小。
	 *
	 * @return 缓冲区大小，单位字节。
	 */
	private int getReceiveSize() {
		return this.receiveSize;
	}

	/* 内部线程 */

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
				// 数据报套接字绑定成功后才进入接收循环，收发共用本次绑定的端口。
				UDP数据报Impl.this.socket = new DatagramSocket(this.port);
				UDP数据报Impl.this.started = true;
			} catch (Exception e) {
				e.printStackTrace();
			}

			while (UDP数据报Impl.this.started) {
				try {
					byte[] buf = new byte[UDP数据报Impl.this.getReceiveSize()];
					DatagramPacket packet = new DatagramPacket(buf, buf.length);

					UDP数据报Impl.this.socket.receive(packet);

					// 复制实际收到的数据报内容，再投递组件事件。
					byte[] received = new byte[packet.getLength()];
					System.arraycopy(buf, 0, received, 0, packet.getLength());
					String remoteAddress = UDP数据报Impl.this.getAddress(packet.getAddress());
					int remotePort = packet.getPort();
					Message msg = new Message();
					Bundle b = new Bundle();

					b.putByteArray("RecMsg", received);
					b.putString("RemoteIP", remoteAddress);
					b.putInt("RemotePort", remotePort);
					msg.setData(b);
					msg.what = RECEIVE_MSG;
					UDP数据报Impl.this.handler.sendMessage(msg);
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
		}
	}
}

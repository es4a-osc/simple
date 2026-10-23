package simple.runtime.components.impl.android.util;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;

/**
 * 管理单个 UDP 监听会话，隔离后台收发、有限积压和已取消会话的回调。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class UdpTransport {

	public static final int MAX_PACKET_SIZE = 65507;
	private static final int MAX_PENDING_BYTES = 4 * 1024 * 1024;
	private final Object lock = new Object();
	private final Executor callbacks;
	private final Listener listener;
	private Session current;
	private long revision;
	private boolean destroyed;
	private volatile int bufferSize = 1024;
	private volatile String lastError = "";

	/**
	 * 接收有序交付到调用方执行器的传输结果。
	 */
	public interface Listener {

		/**
		 * 通知绑定结果；失败端口为零。
		 *
		 * @param success 是否成功
		 * @param port 实际监听端口
		 */
		void started(boolean success, int port);

		/**
		 * 通知完整数据报及发送方。
		 *
		 * @param data 独立字节数组，可为空数组
		 * @param address 来源 IP
		 * @param port 来源端口
		 */
		void received(byte[] data, String address, int port);

		/**
		 * 通知数据报已经交给本地网络栈。
		 *
		 * @param length 发送字节数
		 * @param address 实际目标 IP
		 * @param port 目标端口
		 */
		void sent(int length, String address, int port);

		/**
		 * 通知已启动的监听停止。
		 */
		void stopped();

		/**
		 * 通知未受理或异步失败的操作。
		 *
		 * @param operation 操作名称
		 * @param message 失败原因
		 */
		void failed(String operation, String message);
	}

	/**
	 * 创建传输器；执行器必须按提交顺序串行执行回调。
	 *
	 * @param callbacks 回调执行器
	 * @param listener 传输监听器
	 * @throws IllegalArgumentException 执行器或监听器为空
	 */
	public UdpTransport(Executor callbacks, Listener listener) {
		if (callbacks == null || listener == null) {
			throw new IllegalArgumentException("回调执行器和监听器不能为空");
		}
		this.callbacks = callbacks;
		this.listener = listener;
	}

	/**
	 * 设置完整数据报的接收上限，超限时丢弃整个数据报并报告错误。
	 *
	 * @param size 上限字节数，1～65507
	 * @return 参数有效且组件尚未销毁时为真
	 */
	public boolean setBufferSize(int size) {
		synchronized (lock) {
			if (destroyed) {
				return reject("设置接收大小", "组件已销毁");
			}
			if (size < 1 || size > MAX_PACKET_SIZE) {
				return reject("设置接收大小", "接收大小必须为 1～65507 字节");
			}
			bufferSize = size;
			return true;
		}
	}

	/**
	 * 异步绑定本地端口，不替换尚在启动或正在监听的会话。
	 *
	 * @param port 本地端口，0～65535
	 * @return 请求受理为真，绑定结果另行回调
	 */
	public boolean start(int port) {
		synchronized (lock) {
			lastError = "";
			if (destroyed) {
				return reject("开始监听", "组件已销毁");
			}
			if (port < 0 || port > 65535) {
				return reject("开始监听", "本地端口必须为 0～65535");
			}
			if (current != null) {
				return reject("开始监听", "正在启动或监听，请先停止监听");
			}
			try {
				Session session = new Session(++revision, port);
				current = session;
				session.reader.start();
				return true;
			} catch (SocketException | SecurityException exception) {
				return reject("开始监听", message(exception));
			}
		}
	}

	/**
	 * 查询本地端口是否已经绑定且尚未关闭。
	 *
	 * @return 正在监听时为真
	 */
	public boolean isListening() {
		synchronized (lock) {
			return current != null && current.started;
		}
	}

	/**
	 * 读取成功绑定的实际端口。
	 *
	 * @return 未启动或已停止时为零
	 */
	public int getLocalPort() {
		synchronized (lock) {
			return current != null && current.started ? current.socket.getLocalPort() : 0;
		}
	}

	/**
	 * 读取最近一次失败原因。
	 *
	 * @return 失败文本，新启动或发送请求前清空
	 */
	public String getLastError() {
		return lastError;
	}

	/**
	 * 复制数据并异步发送，使用监听 Socket 保持来源端口一致。
	 *
	 * @param data 0～65507 字节，不允许 null
	 * @param address IP 或主机名，空文本表示 IPv4 广播
	 * @param port 目标端口，1～65535
	 * @return 入队成功为真；不代表对端收到
	 */
	public boolean send(byte[] data, String address, int port) {
		synchronized (lock) {
			lastError = "";
			if (destroyed) {
				return reject("发送数据", "组件已销毁");
			}
			if (data == null || data.length > MAX_PACKET_SIZE || address == null || port < 1 || port > 65535) {
				return reject("发送数据", "数据应为 0～65507 字节，地址不能为 null，目标端口应为 1～65535");
			}
			Session session = current;
			if (session == null || !session.started) {
				return reject("发送数据", "尚未成功开始监听");
			}
			if (session.pending >= 128 || session.pendingBytes + data.length > MAX_PENDING_BYTES) {
				return reject("发送数据", "待发送队列已满");
			}
			String host = address.trim();
			if (host.isEmpty()) {
				host = "255.255.255.255";
			}
			session.pending++;
			session.pendingBytes += data.length;
			session.sends.add(new Send(data.clone(), host, port));
			return true;
		}
	}

	/**
	 * 取消启动或结束监听，释放 Socket 并过滤旧会话回调。
	 */
	public void stop() {
		synchronized (lock) {
			Session session = current;
			long token = ++revision;
			if (session == null) {
				return;
			}
			current = null;
			session.close();
			if (session.started && !destroyed) {
				post(token, listener::stopped);
			}
		}
	}

	/**
	 * 永久释放组件，后续启动及发送请求均被拒绝。
	 */
	public void destroy() {
		synchronized (lock) {
			if (!destroyed) {
				destroyed = true;
				stop();
				revision++;
			}
		}
	}

	/**
	 * 记录参数或状态错误，并安排失败回调。
	 *
	 * @param operation 操作名称
	 * @param text 错误文本
	 * @return 固定返回假
	 */
	private boolean reject(String operation, String text) {
		lastError = text;
		post(revision, () -> listener.failed(operation, text));
		return false;
	}

	/**
	 * 提交带版本校验的回调；锁允许事件中重入停止或重新启动。
	 *
	 * @param token 所属版本
	 * @param action 实际回调
	 */
	private void post(long token, Runnable action) {
		callbacks.execute(() -> {
			synchronized (lock) {
				if (!destroyed && revision == token) {
					action.run();
				}
			}
		});
	}

	/**
	 * 提取可读错误信息。
	 *
	 * @param exception 原始异常
	 * @return 非空错误文本
	 */
	private static String message(Exception exception) {
		String text = exception.getMessage();
		return text == null || text.isEmpty() ? exception.getClass().getSimpleName() : text;
	}

	/**
	 * 持有一轮绑定及收发资源，关闭不会影响后来启动的新会话。
	 */
	private final class Session {

		private final long token;
		private final int port;
		private final DatagramSocket socket;
		private final ArrayBlockingQueue<Send> sends = new ArrayBlockingQueue<>(128);
		private final Semaphore deliveries = new Semaphore(16);
		private final Thread reader;
		private final Thread writer;
		private boolean started;
		private int pending;
		private int pendingBytes;

		/**
		 * 先创建未绑定 Socket，使异步绑定能够被立即取消。
		 *
		 * @param token 所属版本
		 * @param port 请求端口
		 * @throws SocketException 无法创建 Socket
		 */
		Session(long token, int port) throws SocketException {
			this.token = token;
			this.port = port;
			socket = new DatagramSocket(null);
			reader = new Thread(this::receive, "SimpleUdp-receive-" + token);
			writer = new Thread(this::transmit, "SimpleUdp-send-" + token);
			reader.setDaemon(true);
			writer.setDaemon(true);
		}

		/**
		 * 判断当前会话是否仍拥有组件。
		 *
		 * @return 会话仍有效时为真
		 */
		private boolean active() {
			synchronized (lock) {
				return !destroyed && current == this && revision == token;
			}
		}

		/**
		 * 绑定并逐包接收；异常结束循环，不在关闭后反复读取。
		 */
		private void receive() {
			try {
				// DatagramSocket 与显式主线程 Handler 均适用于项目 API 24～30，无需版本分流或弃用抑制。
				socket.setBroadcast(true);
				socket.bind(new InetSocketAddress(port));
				synchronized (lock) {
					if (!active()) {
						return;
					}
					started = true;
					int actualPort = socket.getLocalPort();
					post(token, () -> listener.started(true, actualPort));
					writer.start();
				}

				byte[] buffer = new byte[MAX_PACKET_SIZE + 1];
				while (active()) {
					int limit = bufferSize;
					// 多接收一个字节用于识别超限；拒绝交付截断的半包。
					DatagramPacket packet = new DatagramPacket(buffer, limit + 1);
					socket.receive(packet);
					if (packet.getLength() > limit) {
						fail("接收数据", "数据报超过接收上限 " + limit + " 字节，已整包丢弃");
						continue;
					}
					byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
					String address = packet.getAddress().getHostAddress();
					int remotePort = packet.getPort();
					deliver(() -> listener.received(data, address, remotePort));
				}
			} catch (IOException | SecurityException exception) {
				synchronized (lock) {
					if (active()) {
						current = null;
						close();
						lastError = message(exception);
						String error = lastError;
						post(token, () -> listener.failed(started ? "接收数据" : "开始监听", error));
						if (started) {
							post(token, listener::stopped);
						} else {
							post(token, () -> listener.started(false, 0));
						}
					}
				}
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
			} finally {
				synchronized (lock) {
					if (active()) {
						current = null;
						if (started) {
							post(token, listener::stopped);
						}
					}
					close();
				}
			}
		}

		/**
		 * 依入队顺序解析地址并发送，单包失败不会终止整个监听。
		 */
		private void transmit() {
			try {
				while (active()) {
					Send item = sends.take();
					try {
						InetAddress target = InetAddress.getByName(item.address);
						if (!active()) {
							return;
						}
						socket.send(new DatagramPacket(item.data, item.data.length, target, item.port));
						deliver(() -> listener.sent(item.data.length, target.getHostAddress(), item.port));
					} catch (IOException | SecurityException exception) {
						fail("发送数据", message(exception));
					} finally {
						synchronized (lock) {
							pending--;
							pendingBytes -= item.data.length;
						}
					}
				}
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
			}
		}

		/**
		 * 限制尚未交付的收发回调数量，防止主线程停顿时持续积累字节数组。
		 *
		 * @param action 结果回调
		 * @throws InterruptedException 会话关闭时取消等待
		 */
		private void deliver(Runnable action) throws InterruptedException {
			deliveries.acquire();
			if (!active()) {
				deliveries.release();
				return;
			}
			callbacks.execute(() -> {
				try {
					synchronized (lock) {
						if (active()) {
							action.run();
						}
					}
				} finally {
					deliveries.release();
				}
			});
		}

		/**
		 * 记录当前会话错误并使用同一有界回调通道通知。
		 *
		 * @param operation 操作名称
		 * @param text 失败文本
		 * @throws InterruptedException 会话关闭时取消等待
		 */
		private void fail(String operation, String text) throws InterruptedException {
			synchronized (lock) {
				if (!active()) {
					return;
				}
				lastError = text;
			}
			deliver(() -> listener.failed(operation, text));
		}

		/**
		 * 关闭原生 Socket 以解除阻塞读取，并唤醒等待发送或回调额度的线程。
		 */
		private void close() {
			socket.close();
			reader.interrupt();
			writer.interrupt();
			sends.clear();
		}
	}

	/**
	 * 保存调用时复制的数据及目标，不与调用方共享可变数组。
	 */
	private static final class Send {

		private final byte[] data;
		private final String address;
		private final int port;

		/**
		 * 创建一条待发送数据报。
		 *
		 * @param data 数据副本
		 * @param address 目标文本
		 * @param port 目标端口
		 */
		Send(byte[] data, String address, int port) {
			this.data = data;
			this.address = address;
			this.port = port;
		}
	}
}

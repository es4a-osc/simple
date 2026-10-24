package simple.runtime.components.impl.android.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/**
 * TCP 字节流传输：管理连接归属、有限发送队列、接收背压及独立关闭，不依赖 Android。
 *
 * <p>回调通过调用方提供的串行异步执行器交付；连接替换或销毁后，丢弃旧连接的排队回调。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class TcpTransport {

	public static final int MAX_BUFFER_SIZE = 1024 * 1024;
	public static final int MAX_SEND_BYTES = 4 * 1024 * 1024;
	public static final int MAX_CLIENTS = 64;

	/**
	 * 静态传输实现不创建外层实例。
	 */
	private TcpTransport() {
	}

	/**
	 * 客户端传输结果。
	 */
	public interface ClientListener {
		/**
		 * 连接尝试完成。
		 *
		 * @param success 是否建立了连接
		 */
		void connected(boolean success);

		/**
		 * 接收原始字节流片段。
		 *
		 * @param data 独立的数据副本
		 */
		void received(byte[] data);

		/**
		 * 已建立的连接正常结束或异常断开。
		 */
		void disconnected();

		/**
		 * 本次发送已写入本地 Socket 输出流，不表示对端已读取。
		 *
		 * @param length 数据长度
		 */
		void sent(int length);

		/**
		 * 操作失败。
		 *
		 * @param operation 操作名称
		 * @param message 失败原因
		 */
		void failed(String operation, String message);
	}

	/**
	 * 服务端传输结果。
	 */
	public interface ServerListener {
		/**
		 * 监听尝试完成。
		 *
		 * @param success 是否绑定成功
		 * @param port 实际监听端口；失败为零
		 */
		void started(boolean success, int port);

		/**
		 * 已成功启动的监听结束。
		 */
		void stopped();

		/**
		 * 客户连接已建立。
		 *
		 * @param address 客户 IP
		 * @param port 客户端口
		 */
		void entered(String address, int port);

		/**
		 * 收到客户的字节流片段。
		 *
		 * @param data 独立的数据副本
		 * @param address 客户 IP
		 * @param port 客户端口
		 */
		void received(byte[] data, String address, int port);

		/**
		 * 一个客户连接结束。
		 *
		 * @param address 客户 IP
		 * @param port 客户端口
		 */
		void left(String address, int port);

		/**
		 * 本次发送已写入本地 Socket 输出流。
		 *
		 * @param length 数据长度
		 * @param address 客户 IP
		 * @param port 客户端口
		 */
		void sent(int length, String address, int port);

		/**
		 * 操作失败。
		 *
		 * @param operation 操作名称
		 * @param message 失败原因
		 */
		void failed(String operation, String message);
	}

	/**
	 * 共享状态、参数验证和回调交付。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	private abstract static class Endpoint {

		final Object lock = new Object();
		final Executor callbacks;
		volatile int bufferSize = 1024;
		volatile String lastError = "";
		volatile boolean destroyed;
		long revision;

		/**
		 * 保存回调执行器。
		 *
		 * @param callbacks 串行回调执行器
		 */
		Endpoint(Executor callbacks) {
			this.callbacks = callbacks;
		}

		/**
		 * 设置应用层接收缓冲区，正在阻塞的读取在下一次读取时采用新值。
		 *
		 * @param size 1～1048576 字节
		 * @return 参数有效且组件尚未销毁
		 */
		public final boolean setBufferSize(int size) {
			lastError = "";
			if (size < 1 || size > MAX_BUFFER_SIZE) {
				error("设置接收缓冲区", "接收缓冲区必须为 1～1048576 字节");
				return false;
			}
			if (destroyed) {
				lastError = "组件已销毁";
				return false;
			}
			bufferSize = size;
			return true;
		}

		/**
		 * 获取最近一次错误。
		 *
		 * @return 错误文本；新操作会清空
		 */
		public final String getLastError() {
			return lastError;
		}

		/**
		 * 排队交付回调；即使因归属过期而丢弃，也必须归还接收背压许可。
		 *
		 * @param valid 当前回调是否仍有效
		 * @param action 交付动作
		 * @param release 可为空的许可归还动作
		 */
		final void deliver(BooleanSupplier valid, Runnable action, Runnable release) {
			try {
				callbacks.execute(new Runnable() {
					@Override
					public void run() {
						try {
							if (!destroyed && valid.getAsBoolean()) {
								action.run();
							}
						} finally {
							if (release != null) {
								release.run();
							}
						}
					}
				});
			} catch (RuntimeException rejected) {
				if (release != null) {
					release.run();
				}
				throw rejected;
			}
		}

		/**
		 * 发布当前操作错误。
		 *
		 * @param operation 操作名
		 * @param message 失败原因
		 */
		abstract void error(String operation, String message);
	}

	/**
	 * 每次连接拥有独立的 Socket 和代次，取消无需等待 DNS 或连接线程退出。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	public static final class Client extends Endpoint {

		private final ClientListener listener;
		private Attempt current;

		/**
		 * 创建客户端。
		 *
		 * @param callbacks 串行回调执行器
		 * @param listener 结果接收器
		 */
		public Client(Executor callbacks, ClientListener listener) {
			super(callbacks);
			this.listener = listener;
		}

		/**
		 * 发起新连接，并取消前一次连接及其尚未交付的回调。
		 *
		 * @param address IP 或主机名
		 * @param port 1～65535
		 * @param timeout 连接超时毫秒，零表示无限；不包含系统 DNS 查询耗时
		 * @return 请求是否受理
		 */
		public boolean connect(String address, int port, int timeout) {
			lastError = "";
			if (address == null || address.trim().isEmpty() || port < 1 || port > 65535 || timeout < 0) {
				error("连接", "地址不能为空，端口必须为 1～65535，超时不能为负数");
				return false;
			}
			Attempt next;
			Attempt previous;
			try {
				synchronized (lock) {
					if (destroyed) {
						lastError = "组件已销毁";
						return false;
					}
					next = new Attempt();
					++revision;
					previous = current;
					current = next;
				}
			} catch (IOException failure) {
				error("连接", failure.toString());
				return false;
			}
			if (previous != null) {
				previous.close();
			}
			thread("SimpleTcpConnect", new Runnable() {
				@Override
				public void run() {
					connect(next, address.trim(), port, timeout);
				}
			});
			return true;
		}

		/**
		 * 在网络线程解析地址并建立连接，结果始终关联本次尝试。
		 *
		 * @param attempt 本次尝试
		 * @param address 对端地址
		 * @param port 对端端口
		 * @param timeout 连接超时
		 */
		private void connect(Attempt attempt, String address, int port, int timeout) {
			try {
				InetSocketAddress destination = new InetSocketAddress(address, port);
				if (!owns(attempt)) {
					return;
				}
				attempt.socket.connect(destination, timeout);
				// 连接创建后即归本次尝试持有，由 Attempt.close() 统一释放。
				attempt.connection = new Connection(attempt.socket, new IntSupplier() {
					@Override
					public int getAsInt() {
						return bufferSize;
					}
				}, new Sink() {
					@Override
					public void data(byte[] data, Runnable release) {
						deliver(new BooleanSupplier() {
							@Override
							public boolean getAsBoolean() {
								return owns(attempt);
							}
						}, new Runnable() {
							@Override
							public void run() {
								listener.received(data);
							}
						}, release);
					}

					@Override
					public void sent(int length) {
						deliver(new BooleanSupplier() {
							@Override
							public boolean getAsBoolean() {
								return owns(attempt);
							}
						}, new Runnable() {
							@Override
							public void run() {
								listener.sent(length);
							}
						}, null);
					}

					@Override
					public void closed(String message) {
						synchronized (lock) {
							if (current != attempt || destroyed) {
								return;
							}
							attempt.connected = false;
						}
						if (message != null) {
							failure(attempt, "通信", message);
						}
						deliver(new BooleanSupplier() {
							@Override
							public boolean getAsBoolean() {
								return owns(attempt);
							}
						}, new Runnable() {
							@Override
							public void run() {
								listener.disconnected();
							}
						}, null);
					}
				});
				Connection connection = attempt.connection;
				synchronized (lock) {
					if (current != attempt || destroyed) {
						connection.close();
						return;
					}
					attempt.connected = true;
					attempt.connecting = false;
				}
				deliver(new BooleanSupplier() {
					@Override
					public boolean getAsBoolean() {
						return owns(attempt);
					}
				}, new Runnable() {
					@Override
					public void run() {
						listener.connected(true);
					}
				}, null);
				if (owns(attempt)) {
					connection.start();
				}
			} catch (IOException | RuntimeException failure) {
				if (owns(attempt)) {
					attempt.connecting = false;
					attempt.connected = false;
					failure(attempt, "连接", failure.toString());
					deliver(new BooleanSupplier() {
						@Override
						public boolean getAsBoolean() {
							return owns(attempt);
						}
					}, new Runnable() {
						@Override
						public void run() {
							listener.connected(false);
						}
					}, null);
				}
				attempt.close();
			} finally {
				// 错误通知本身可能提交失败，仍须释放未成功交接或已经取消的连接。
				if (!owns(attempt) || !attempt.connected) {
					attempt.close();
				}
			}
		}

		/**
		 * 判断当前观察到的本地连接状态，不发送探测字节，不能保证对端仍在线。
		 *
		 * @return 当前连接尚未检测到断开
		 */
		public boolean isConnected() {
			synchronized (lock) {
				return !destroyed && current != null && current.connected && !current.socket.isClosed();
			}
		}

		/**
		 * 获取是否仍在尝试连接。
		 *
		 * @return 当前连接尝试尚未结束
		 */
		public boolean isConnecting() {
			synchronized (lock) {
				return !destroyed && current != null && current.connecting;
			}
		}

		/**
		 * 将数据副本加入当前连接的有限发送队列。
		 *
		 * @param data 非空字节集，最大 4 MiB
		 * @return 请求是否入队；不表示对端已经收到
		 */
		public boolean send(byte[] data) {
			lastError = "";
			Attempt attempt;
			synchronized (lock) {
				attempt = current;
			}
			if (attempt == null || !attempt.connected || attempt.connection == null) {
				error("发送", "当前没有已建立的连接");
				return false;
			}
			String rejected = attempt.connection.send(data);
			if (rejected != null) {
				failure(attempt, "发送", rejected);
				return false;
			}
			return true;
		}

		/**
		 * 取消连接或关闭现有连接；已有连接只产生一次本地主动断开通知。
		 */
		public void disconnect() {
			Attempt previous;
			long expected;
			synchronized (lock) {
				previous = current;
				current = null;
				expected = ++revision;
			}
			if (previous != null) {
				boolean notify = previous.connected;
				previous.close();
				if (notify) {
					deliver(new BooleanSupplier() {
						@Override
						public boolean getAsBoolean() {
							return revisionIs(expected);
						}
					}, new Runnable() {
						@Override
						public void run() {
							listener.disconnected();
						}
					}, null);
				}
			}
		}

		/**
		 * 永久关闭客户端，并过滤全部尚未交付的回调。
		 */
		public void destroy() {
			synchronized (lock) {
				destroyed = true;
			}
			disconnect();
		}

		/**
		 * 检查连接身份。
		 *
		 * @param attempt 待检查的尝试
		 * @return 本次尝试仍属于客户端
		 */
		private boolean owns(Attempt attempt) {
			synchronized (lock) {
				return !destroyed && current == attempt;
			}
		}

		/**
		 * 检查无活动连接的通知代次。
		 *
		 * @param expected 通知所属代次
		 * @return 代次尚未变化
		 */
		private boolean revisionIs(long expected) {
			synchronized (lock) {
				return revision == expected;
			}
		}

		/**
		 * 记录属于当前连接的错误。
		 *
		 * @param attempt 连接尝试
		 * @param operation 操作名称
		 * @param message 失败原因
		 */
		private void failure(Attempt attempt, String operation, String message) {
			synchronized (lock) {
				if (current != attempt || destroyed) {
					return;
				}
				lastError = message;
			}
			deliver(new BooleanSupplier() {
				@Override
				public boolean getAsBoolean() {
					return owns(attempt);
				}
			}, new Runnable() {
				@Override
				public void run() {
					listener.failed(operation, message);
				}
			}, null);
		}

		@Override
		void error(String operation, String message) {
			long expected;
			synchronized (lock) {
				lastError = message;
				expected = revision;
			}
			deliver(new BooleanSupplier() {
				@Override
				public boolean getAsBoolean() {
					return revisionIs(expected);
				}
			}, new Runnable() {
				@Override
				public void run() {
					listener.failed(operation, message);
				}
			}, null);
		}
	}

	/**
	 * 服务端每次启动拥有独立监听及客户表，关闭操作会关闭全部连接。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	public static final class Server extends Endpoint {

		private final ServerListener listener;
		private Listening current;

		/**
		 * 创建服务端。
		 *
		 * @param callbacks 串行回调执行器
		 * @param listener 结果接收器
		 */
		public Server(Executor callbacks, ServerListener listener) {
			super(callbacks);
			this.listener = listener;
		}

		/**
		 * 异步绑定端口；已有监听或启动请求时拒绝重复启动。
		 *
		 * @param port 0～65535，零表示自动分配
		 * @return 请求是否受理
		 */
		public boolean start(int port) {
			lastError = "";
			if (port < 0 || port > 65535) {
				error("启动", "监听端口必须为 0～65535");
				return false;
			}
			Listening next;
			try {
				synchronized (lock) {
					if (destroyed) {
						lastError = "组件已销毁";
						return false;
					}
					if (current != null) {
						error("启动", "服务器正在启动或监听，请先关闭");
						return false;
					}
					next = new Listening(++revision);
					current = next;
				}
			} catch (IOException failure) {
				error("启动", failure.toString());
				return false;
			}
			thread("SimpleTcpAccept", new Runnable() {
				@Override
				public void run() {
					accept(next, port);
				}
			});
			return true;
		}

		/**
		 * 完成端口绑定并持续接受客户；错误只终止所属的监听代次。
		 *
		 * @param owner 监听实例
		 * @param port 请求端口
		 */
		private void accept(Listening owner, int port) {
			try {
				owner.socket.setReuseAddress(true);
				owner.socket.bind(new InetSocketAddress(port));
				synchronized (lock) {
					if (current != owner || destroyed) {
						return;
					}
					owner.started = true;
				}
				final int actualPort = owner.socket.getLocalPort();
				deliver(new BooleanSupplier() {
					@Override
					public boolean getAsBoolean() {
						return revisionIs(owner.revision);
					}
				}, new Runnable() {
					@Override
					public void run() {
						listener.started(true, actualPort);
					}
				}, null);
				while (owns(owner)) {
					Socket accepted = owner.socket.accept();
					Connection connection;
					try {
						connection = new Connection(accepted, new IntSupplier() {
							@Override
							public int getAsInt() {
								return bufferSize;
							}
						}, new Sink() {
							@Override
							public void data(byte[] data, Runnable release) {
								deliver(new BooleanSupplier() {
									@Override
									public boolean getAsBoolean() {
										return revisionIs(owner.revision);
									}
								},
										new Runnable() {
											@Override
											public void run() {
												listener.received(data, accepted.getInetAddress().getHostAddress(), accepted.getPort());
											}
										}, release);
							}

							@Override
							public void sent(int length) {
								deliver(new BooleanSupplier() {
									@Override
									public boolean getAsBoolean() {
										return revisionIs(owner.revision);
									}
								},
										new Runnable() {
											@Override
											public void run() {
												listener.sent(length, accepted.getInetAddress().getHostAddress(), accepted.getPort());
											}
										}, null);
							}

							@Override
							public void closed(String message) {
								String key = key(accepted);
								synchronized (lock) {
									Connection registered = owner.clients.get(key);
									if (current != owner || registered == null || registered.socket != accepted) {
										return;
									}
									owner.clients.remove(key);
								}
								if (message != null) {
									failure(owner, "客户通信", message);
								}
								deliver(new BooleanSupplier() {
									@Override
									public boolean getAsBoolean() {
										return revisionIs(owner.revision);
									}
								},
										new Runnable() {
											@Override
											public void run() {
												listener.left(accepted.getInetAddress().getHostAddress(), accepted.getPort());
											}
										}, null);
							}
						});
					} catch (IOException | RuntimeException failure) {
						closeSocket(accepted);
						failure(owner, "接入客户", failure.toString());
						continue;
					}
					synchronized (lock) {
						if (current != owner || destroyed || owner.clients.size() >= MAX_CLIENTS) {
							connection.close();
							if (current == owner && !destroyed) {
								failure(owner, "接入客户", "已达到 64 个客户连接上限");
							}
							continue;
						}
						owner.clients.put(key(accepted), connection);
					}
					deliver(new BooleanSupplier() {
						@Override
						public boolean getAsBoolean() {
							return revisionIs(owner.revision);
						}
					},
							new Runnable() {
								@Override
								public void run() {
									listener.entered(accepted.getInetAddress().getHostAddress(), accepted.getPort());
								}
							}, null);
					if (owns(owner)) {
						connection.start();
					}
				}
			} catch (IOException | RuntimeException failure) {
				if (owns(owner)) {
					boolean wasStarted = owner.started;
					failure(owner, wasStarted ? "监听" : "启动", failure.toString());
					stop(owner, false);
					if (wasStarted) {
						deliver(new BooleanSupplier() {
							@Override
							public boolean getAsBoolean() {
								return revisionIs(owner.revision);
							}
						}, new Runnable() {
							@Override
							public void run() {
								listener.stopped();
							}
						}, null);
					} else {
						deliver(new BooleanSupplier() {
							@Override
							public boolean getAsBoolean() {
								return revisionIs(owner.revision);
							}
						}, new Runnable() {
							@Override
							public void run() {
								listener.started(false, 0);
							}
						}, null);
					}
				}
			} finally {
				closeSocket(owner.socket);
			}
		}

		/**
		 * 查询实际监听状态，不把启动请求当作绑定成功。
		 *
		 * @return 已经绑定并仍在监听
		 */
		public boolean isListening() {
			synchronized (lock) {
				return !destroyed && current != null && current.started;
			}
		}

		/**
		 * 获取系统实际绑定的端口。
		 *
		 * @return 当前端口；未监听返回零
		 */
		public int getPort() {
			synchronized (lock) {
				return isListening() ? current.socket.getLocalPort() : 0;
			}
		}

		/**
		 * 获取当前客户数量。
		 *
		 * @return 当前已登记的连接数
		 */
		public int getClientCount() {
			synchronized (lock) {
				return current == null ? 0 : current.clients.size();
			}
		}

		/**
		 * 向匹配 IP 和端口的当前客户异步发送。
		 *
		 * @param data 非空字节集，最大 4 MiB
		 * @param address 客户 IP，与客户进入事件一致
		 * @param port 客户端口
		 * @return 请求是否入队
		 */
		public boolean send(byte[] data, String address, int port) {
			lastError = "";
			Listening owner;
			Connection connection;
			synchronized (lock) {
				owner = current;
				connection = owner == null ? null : owner.clients.get(address + "\n" + port);
			}
			if (connection == null) {
				error("发送", "没有匹配该 IP 和端口的客户连接");
				return false;
			}
			String rejected = connection.send(data);
			if (rejected != null) {
				failure(owner, "发送", rejected);
				return false;
			}
			return true;
		}

		/**
		 * 关闭监听和所有客户，过滤本次监听尚未交付的数据及发送回调。
		 */
		public void stop() {
			Listening owner;
			synchronized (lock) {
				owner = current;
			}
			if (owner != null) {
				stop(owner, true);
			}
		}

		/**
		 * 永久销毁服务端。
		 */
		public void destroy() {
			synchronized (lock) {
				destroyed = true;
			}
			stop();
		}

		/**
		 * 终止指定监听，不让旧 accept 或读取线程影响后续启动。
		 *
		 * @param owner 本次监听
		 * @param explicit 是否主动关闭并另立通知代次
		 */
		private void stop(Listening owner, boolean explicit) {
			ArrayList<Connection> peers;
			long expected;
			synchronized (lock) {
				if (current != owner) {
					return;
				}
				current = null;
				expected = explicit ? ++revision : revision;
				peers = new ArrayList<>(owner.clients.values());
				owner.clients.clear();
			}
			closeSocket(owner.socket);
			for (Connection connection : peers) {
				connection.close();
			}
			if (explicit && owner.started) {
				deliver(new BooleanSupplier() {
					@Override
					public boolean getAsBoolean() {
						return revisionIs(expected);
					}
				}, new Runnable() {
					@Override
					public void run() {
						listener.stopped();
					}
				}, null);
			}
		}

		/**
		 * 检查监听归属。
		 *
		 * @param owner 监听实例
		 * @return 仍为当前监听
		 */
		private boolean owns(Listening owner) {
			synchronized (lock) {
				return !destroyed && current == owner;
			}
		}

		/**
		 * 检查监听代次；允许正常 EOF 之前排队的数据按序交付。
		 *
		 * @param expected 代次
		 * @return 未被主动关闭或重启替换
		 */
		private boolean revisionIs(long expected) {
			synchronized (lock) {
				return revision == expected;
			}
		}

		/**
		 * 记录指定监听的错误。
		 *
		 * @param owner 本次监听
		 * @param operation 操作名称
		 * @param message 原因
		 */
		private void failure(Listening owner, String operation, String message) {
			synchronized (lock) {
				if (current != owner || destroyed) {
					return;
				}
				lastError = message;
			}
			deliver(new BooleanSupplier() {
				@Override
				public boolean getAsBoolean() {
					return revisionIs(owner.revision);
				}
			}, new Runnable() {
				@Override
				public void run() {
					listener.failed(operation, message);
				}
			}, null);
		}

		@Override
		void error(String operation, String message) {
			long expected;
			synchronized (lock) {
				lastError = message;
				expected = revision;
			}
			deliver(new BooleanSupplier() {
				@Override
				public boolean getAsBoolean() {
					return revisionIs(expected);
				}
			}, new Runnable() {
				@Override
				public void run() {
					listener.failed(operation, message);
				}
			}, null);
		}
	}

	/**
	 * 单次客户端连接的独立资源。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	private static final class Attempt {

		final Socket socket;
		volatile Connection connection;
		volatile boolean connecting = true;
		volatile boolean connected;

		/**
		 * 在网络线程启动前发布可被关闭的 Socket。
		 *
		 * @throws IOException 无法创建 Socket
		 */
		Attempt() throws IOException {
			socket = new Socket();
		}

		/**
		 * 取消待连接 Socket 或现有连接。
		 */
		void close() {
			connecting = false;
			connected = false;
			closeSocket(socket);
			if (connection != null) {
				connection.close();
			}
		}
	}

	/**
	 * 一次服务启动的监听资源及客户表；客户表由 Endpoint.lock 保护。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	private static final class Listening {

		final long revision;
		final ServerSocket socket;
		final Map<String, Connection> clients = new LinkedHashMap<>();
		volatile boolean started;

		/**
		 * 在绑定线程启动前发布可被取消的监听 Socket。
		 *
		 * @param revision 监听代次
		 * @throws IOException 无法创建监听 Socket
		 */
		Listening(long revision) throws IOException {
			this.revision = revision;
			socket = new ServerSocket();
		}
	}

	/**
	 * 单个连接对所属端点的回调；数据交付后必须归还许可。
	 */
	private interface Sink {
		/**
		 * 交付接收数据。
		 *
		 * @param data 独立字节副本
		 * @param release 交付或丢弃后归还背压许可
		 */
		void data(byte[] data, Runnable release);

		/**
		 * 交付本地发送结果。
		 *
		 * @param length 已写出的字节数
		 */
		void sent(int length);

		/**
		 * 连接唯一的终止通知。
		 *
		 * @param message EOF 为 null，异常为错误文本
		 */
		void closed(String message);
	}

	/**
	 * 一个 Socket 的双向传输；收发独立，关闭不等待正在阻塞的写入。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	private static final class Connection implements Closeable {

		final Socket socket;
		final InputStream input;
		final OutputStream output;
		final IntSupplier bufferSize;
		final Sink sink;
		final AtomicBoolean closed = new AtomicBoolean();
		final ArrayBlockingQueue<byte[]> writes = new ArrayBlockingQueue<>(128);
		final Semaphore deliveries = new Semaphore(16);
		final Object writeLock = new Object();
		final Object eventLock = new Object();
		long pendingBytes;
		volatile Thread reader;
		volatile Thread writer;

		/**
		 * 准备已经连接的 Socket；不启用带内紧急数据，也不发送探测字节。
		 *
		 * @param socket 已连接的 Socket
		 * @param bufferSize 当前接收缓冲区大小
		 * @param sink 结果接收器
		 * @throws IOException 配置连接失败
		 */
		Connection(Socket socket, IntSupplier bufferSize, Sink sink) throws IOException {
			this.socket = socket;
			this.bufferSize = bufferSize;
			this.sink = sink;
			socket.setKeepAlive(true);
			socket.setTcpNoDelay(true);
			input = socket.getInputStream();
			output = socket.getOutputStream();
		}

		/**
		 * 接入及连接结果入队后才启动读取，保证事件先后顺序。
		 */
		void start() {
			if (!closed.get()) {
				reader = thread("SimpleTcpRead", new Runnable() {
					@Override
					public void run() {
						Connection.this.read();
					}
				});
				writer = thread("SimpleTcpWrite", new Runnable() {
					@Override
					public void run() {
						Connection.this.write();
					}
				});
			}
		}

		/**
		 * 在固定上限内复制并排队发送数据；不会在调用线程写 Socket。
		 *
		 * @param data 待发送数据
		 * @return 拒绝原因；null 表示入队成功
		 */
		String send(byte[] data) {
			if (data == null || data.length == 0 || data.length > MAX_SEND_BYTES) {
				return "发送数据必须非空且不超过 4 MiB";
			}
			synchronized (writeLock) {
				if (closed.get()) {
					return "连接已关闭";
				}
				if (writes.remainingCapacity() == 0 || pendingBytes + data.length > MAX_SEND_BYTES) {
					return "发送队列已满，请等待发送完毕后重试";
				}
				pendingBytes += data.length;
				writes.add(data.clone());
				return null;
			}
		}

		/**
		 * 读取到 EOF 或异常立即结束，接收回调积压时向 TCP 读取施加背压。
		 */
		private void read() {
			byte[] buffer = new byte[bufferSize.getAsInt()];
			try {
				while (!closed.get()) {
					int requested = bufferSize.getAsInt();
					if (buffer.length != requested) {
						buffer = new byte[requested];
					}
					int count = input.read(buffer);
					if (count < 0) {
						terminate(null);
						return;
					}
					if (count == 0) {
						continue;
					}
					while (!closed.get() && !deliveries.tryAcquire(100, TimeUnit.MILLISECONDS)) {
						// 关闭后定期退出，不能永远等待已经失效的界面回调。
					}
					if (closed.get()) {
						return;
					}
					synchronized (eventLock) {
						if (closed.get()) {
							deliveries.release();
							return;
						}
						sink.data(Arrays.copyOf(buffer, count), new Runnable() {
							@Override
							public void run() {
								deliveries.release();
							}
						});
					}
				}
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
			} catch (IOException | RuntimeException failure) {
				terminate(failure.toString());
			}
		}

		/**
		 * 串行写出已入队数据，避免并发发送导致不同调用的字节交错。
		 */
		private void write() {
			try {
				while (!closed.get()) {
					byte[] data = writes.take();
					try {
						if (closed.get()) {
							return;
						}
						output.write(data);
						output.flush();
					} finally {
						synchronized (writeLock) {
							pendingBytes = Math.max(0, pendingBytes - data.length);
						}
					}
					synchronized (eventLock) {
						if (!closed.get()) {
							sink.sent(data.length);
						}
					}
				}
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
			} catch (IOException | RuntimeException failure) {
				terminate(failure.toString());
			}
		}

		/**
		 * 只由第一个终止原因关闭资源并通知端点。
		 *
		 * @param message EOF 或主动关闭为空，错误为原因
		 */
		private void terminate(String message) {
			synchronized (eventLock) {
				if (!closed.compareAndSet(false, true)) {
					return;
				}
				closeSocket(socket);
				synchronized (writeLock) {
					writes.clear();
					pendingBytes = 0;
				}
				if (reader != null) {
					reader.interrupt();
				}
				if (writer != null) {
					writer.interrupt();
				}
				sink.closed(message);
			}
		}

		@Override
		public void close() {
			terminate(null);
		}
	}

	/**
	 * 使用明确的 IP 和端口构成客户键，不解析 InetAddress.toString 的显示文本。
	 *
	 * @param socket 客户 Socket
	 * @return 不含歧义的客户键
	 */
	private static String key(Socket socket) {
		return socket.getInetAddress().getHostAddress() + "\n" + socket.getPort();
	}

	/**
	 * 创建网络工作线程；守护标志便于桌面测试退出，不改变显式资源释放要求。
	 *
	 * @param name 线程名称
	 * @param work 工作任务
	 * @return 已启动线程
	 */
	private static Thread thread(String name, Runnable work) {
		Thread thread = new Thread(work, name);
		thread.setDaemon(true);
		thread.start();
		return thread;
	}

	/**
	 * 独立关闭资源，不因某个资源关闭失败而跳过其余连接。
	 *
	 * @param resource 待关闭资源
	 */
	private static void closeSocket(Closeable resource) {
		try {
			resource.close();
		} catch (IOException ignored) {
			// 连接已经不可再用；调用方继续清理同一代次的其他资源。
		}
	}
}

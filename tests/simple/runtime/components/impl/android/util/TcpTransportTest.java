package simple.runtime.components.impl.android.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import junit.framework.TestCase;

/**
 * 使用真实回环 Socket 验证 TCP 字节流、并发发送、EOF、取消、重启和资源回收。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class TcpTransportTest extends TestCase {

	private final Queue callbacks = new Queue();
	private final List<TcpTransport.Client> clients = new ArrayList<>();
	private final List<TcpTransport.Server> servers = new ArrayList<>();
	private final List<Socket> sockets = new ArrayList<>();

	@Override
	protected void tearDown() throws Exception {
		for (TcpTransport.Client client : clients) {
			client.destroy();
		}
		for (TcpTransport.Server server : servers) {
			server.destroy();
		}
		for (Socket socket : sockets) {
			socket.close();
		}
		callbacks.drain();
		await(() -> workerCount() == 0, false);
	}

	/**
	 * 小接收缓冲区、多次读取和独立发送必须保持原始字节顺序，不能引入消息边界。
	 *
	 * @throws Exception 网络操作失败
	 */
	public void testBidirectionalStreamAndBufferValidation() throws Exception {
		ServerEvents serverEvents = new ServerEvents();
		TcpTransport.Server server = server(serverEvents);
		assertTrue(server.setBufferSize(7));
		assertFalse(server.setBufferSize(0));
		assertFalse(server.setBufferSize(TcpTransport.MAX_BUFFER_SIZE + 1));
		assertTrue(server.start(0));
		await(() -> serverEvents.starts == 1, true);
		ClientEvents clientEvents = new ClientEvents();
		TcpTransport.Client client = client(clientEvents);
		assertTrue(client.setBufferSize(11));
		assertTrue(client.connect("127.0.0.1", server.getPort(), 1000));
		await(() -> clientEvents.successes == 1 && serverEvents.enters == 1, true);
		byte[] sent = new byte[8193];
		for (int i = 0; i < sent.length; i++) {
			sent[i] = (byte) (i * 17);
		}
		assertTrue(client.send(sent));
		await(() -> serverEvents.data.size() == sent.length, true);
		assertTrue(Arrays.equals(sent, serverEvents.data.toByteArray()));
		assertTrue(server.send(sent, serverEvents.address, serverEvents.port));
		await(() -> clientEvents.data.size() == sent.length && serverEvents.sends == 1, true);
		assertTrue(Arrays.equals(sent, clientEvents.data.toByteArray()));
		assertFalse(server.send(sent, "127.0.0.2", serverEvents.port));
		assertFalse(client.send(new byte[0]));
		assertTrue(client.isConnected());
	}

	/**
	 * 状态查询不发送紧急字节；对端正常半关闭会结束读取、关闭 Socket 并只通知一次。
	 *
	 * @throws Exception 网络操作失败
	 */
	public void testStatusHasNoWireSideEffectsAndEofCloses() throws Exception {
		try (ServerSocket remote = listening()) {
			ClientEvents events = new ClientEvents();
			TcpTransport.Client client = client(events);
			assertTrue(client.connect("127.0.0.1", remote.getLocalPort(), 1000));
			Socket peer = remember(remote.accept());
			peer.setOOBInline(true);
			peer.setSoTimeout(100);
			await(() -> events.successes == 1, true);
			for (int i = 0; i < 30; i++) {
				assertTrue(client.isConnected());
			}
			try {
				peer.getInputStream().read();
				fail("查询连接状态不得发送额外数据");
			} catch (SocketTimeoutException expected) {
				// 对端确实没有收到探测字节。
			}
			peer.shutdownOutput();
			await(() -> events.disconnects == 1 && !client.isConnected(), true);
			assertEquals(-1, peer.getInputStream().read());
			callbacks.drain();
			assertEquals(1, events.disconnects);
		}
	}

	/**
	 * 关闭服务端必须关闭所有客户；正常客户 EOF 通知一次，原端口可以重新监听。
	 *
	 * @throws Exception 网络操作失败
	 */
	public void testServerEofShutdownAndRestart() throws Exception {
		ServerEvents events = new ServerEvents();
		TcpTransport.Server server = server(events);
		assertTrue(server.start(0));
		await(() -> events.starts == 1, true);
		int port = server.getPort();
		Socket first = connect(port);
		Socket second = connect(port);
		await(() -> events.enters == 2, true);
		first.shutdownOutput();
		await(() -> events.leaves == 1 && server.getClientCount() == 1, true);
		assertEquals(-1, first.getInputStream().read());
		server.stop();
		assertEquals(-1, second.getInputStream().read());
		await(() -> events.stops == 1, true);
		assertEquals(0, server.getClientCount());
		assertEquals(0, server.getPort());
		assertFalse(server.isListening());
		assertTrue(server.start(port));
		await(() -> events.starts == 2, true);
		assertEquals(port, server.getPort());
	}

	/**
	 * 被替换连接的成功与数据回调不得串入新连接，旧 Socket 必须实际关闭。
	 *
	 * @throws Exception 网络操作失败
	 */
	public void testReconnectDropsQueuedOldCallbacks() throws Exception {
		try (ServerSocket oldServer = listening(); ServerSocket newServer = listening()) {
			ClientEvents events = new ClientEvents();
			TcpTransport.Client client = client(events);
			assertTrue(client.connect("127.0.0.1", oldServer.getLocalPort(), 1000));
			Socket oldPeer = remember(oldServer.accept());
			await(client::isConnected, false);
			oldPeer.getOutputStream().write(new byte[]{1, 2, 3});
			await(() -> callbacks.size() >= 2, false);
			assertTrue(client.connect("127.0.0.1", newServer.getLocalPort(), 1000));
			Socket newPeer = remember(newServer.accept());
			newPeer.getOutputStream().write(new byte[]{9, 8, 7});
			await(() -> events.successes == 1 && events.data.size() == 3, true);
			assertTrue(Arrays.equals(new byte[]{9, 8, 7}, events.data.toByteArray()));
			assertEquals(-1, oldPeer.getInputStream().read());
			assertEquals(0, events.disconnects);
		}
	}

	/**
	 * 连接失败、端口冲突及非法参数均有真实失败结果，之后仍能重新启动和连接。
	 *
	 * @throws Exception 网络操作失败
	 */
	public void testFailuresAndRecovery() throws Exception {
		int unavailable;
		try (ServerSocket temporary = listening()) {
			unavailable = temporary.getLocalPort();
		}
		ClientEvents clientEvents = new ClientEvents();
		TcpTransport.Client client = client(clientEvents);
		assertFalse(client.connect("", 10, 100));
		assertFalse(client.connect("127.0.0.1", -1, 100));
		assertFalse(client.connect("127.0.0.1", 10, -1));
		assertTrue(client.connect("127.0.0.1", unavailable, 1000));
		await(() -> clientEvents.failures == 1, true);
		assertFalse(client.isConnecting());
		assertFalse(client.isConnected());
		assertFalse(client.getLastError().isEmpty());
		try (ServerSocket occupied = listening()) {
			ServerEvents serverEvents = new ServerEvents();
			TcpTransport.Server server = server(serverEvents);
			assertTrue(server.start(occupied.getLocalPort()));
			await(() -> serverEvents.failedStarts == 1, true);
			assertFalse(server.isListening());
			assertFalse(server.getLastError().isEmpty());
			assertTrue(server.start(0));
			await(() -> serverEvents.starts == 1, true);
			assertFalse(server.start(0));
			assertTrue(client.connect("127.0.0.1", server.getPort(), 1000));
			await(() -> clientEvents.successes == 1, true);
		}
	}

	/**
	 * 并发发送按每次调用完整排队；调用方随后修改数组不能改变已受理的数据。
	 *
	 * @throws Exception 网络操作失败
	 */
	public void testConcurrentSendsCopyAndDoNotInterleave() throws Exception {
		try (ServerSocket remote = listening()) {
			ClientEvents events = new ClientEvents();
			TcpTransport.Client client = client(events);
			assertTrue(client.connect("127.0.0.1", remote.getLocalPort(), 1000));
			Socket peer = remember(remote.accept());
			await(() -> events.successes == 1, true);
			List<Thread> senders = new ArrayList<>();
			ConcurrentLinkedQueue<String> rejected = new ConcurrentLinkedQueue<>();
			for (int i = 0; i < 32; i++) {
				final byte id = (byte) i;
				Thread sender = new Thread(() -> {
					byte[] data = new byte[2048];
					Arrays.fill(data, id);
					if (!client.send(data)) {
						rejected.add(client.getLastError());
					}
					Arrays.fill(data, (byte) 127);
				});
				senders.add(sender);
				sender.start();
			}
			for (Thread sender : senders) {
				sender.join(2000);
				assertFalse(sender.isAlive());
			}
			assertTrue(rejected.toString(), rejected.isEmpty());
			boolean[] seen = new boolean[32];
			InputStream input = peer.getInputStream();
			for (int i = 0; i < 32; i++) {
				byte[] record = new byte[2048];
				for (int offset = 0; offset < record.length;) {
					int count = input.read(record, offset, record.length - offset);
					assertTrue(count > 0);
					offset += count;
				}
				int id = record[0] & 255;
				assertTrue(id < 32);
				assertFalse(seen[id]);
				seen[id] = true;
				for (byte value : record) {
					assertEquals(id, value & 255);
				}
			}
			await(() -> events.sends == 32, true);
		}
	}

	/**
	 * 慢接收端不能造成无限发送积压，关闭必须取消阻塞写入并及时收回线程。
	 *
	 * @throws Exception 网络操作失败
	 */
	public void testSendQueueBoundAndCloseBlockedWrite() throws Exception {
		try (ServerSocket remote = listening()) {
			remote.setReceiveBufferSize(1024);
			ClientEvents events = new ClientEvents();
			TcpTransport.Client client = client(events);
			assertTrue(client.connect("127.0.0.1", remote.getLocalPort(), 1000));
			Socket peer = remember(remote.accept());
			peer.setReceiveBufferSize(1024);
			await(() -> events.successes == 1, true);
			byte[] data = new byte[TcpTransport.MAX_SEND_BYTES];
			assertTrue(client.send(data));
			boolean rejected = false;
			for (int i = 0; i < 4; i++) {
				if (!client.send(data)) {
					rejected = true;
					break;
				}
			}
			assertTrue("慢客户必须触发发送积压上限", rejected);
			long begin = System.nanoTime();
			client.disconnect();
			assertTrue(elapsedMillis(begin) < 1000);
			await(() -> events.disconnects == 1, true);
			assertFalse(client.isConnected());
		}
	}

	/**
	 * 接收回调积压有界；主动关闭和销毁会过滤尚未派发的数据及成功回调。
	 *
	 * @throws Exception 网络操作失败
	 */
	public void testReceiveBackpressureAndDestroyFiltersCallbacks() throws Exception {
		try (ServerSocket remote = listening()) {
			ClientEvents events = new ClientEvents();
			TcpTransport.Client client = client(events);
			assertTrue(client.connect("127.0.0.1", remote.getLocalPort(), 1000));
			Socket peer = remember(remote.accept());
			await(client::isConnected, false);
			peer.getOutputStream().write(new byte[64 * 1024]);
			await(() -> callbacks.size() >= 17, false);
			Thread.sleep(100);
			assertEquals(17, callbacks.size());
			client.destroy();
			callbacks.drain();
			assertEquals(0, events.successes);
			assertEquals(0, events.data.size());
			assertEquals(0, events.disconnects);
			assertFalse(client.connect("127.0.0.1", remote.getLocalPort(), 100));
		}
	}

	/**
	 * 连续取消尚未交付结果的连接，不能复活 Socket 或派发旧连接结果。
	 *
	 * @throws Exception 网络操作失败
	 */
	public void testImmediateCancellationDoesNotReviveConnections() throws Exception {
		try (ServerSocket remote = listening()) {
			ClientEvents events = new ClientEvents();
			TcpTransport.Client client = client(events);
			for (int i = 0; i < 20; i++) {
				assertTrue(client.connect("127.0.0.1", remote.getLocalPort(), 1000));
				client.disconnect();
			}
			await(() -> workerCount() == 0, false);
			callbacks.drain();
			assertFalse(client.isConnected());
			assertFalse(client.isConnecting());
			assertEquals(0, events.successes);
			assertEquals(0, events.failures);
			assertEquals(0, events.data.size());
		}
	}

	/**
	 * 回调队列连续拒绝连接结果和错误通知时，也必须释放已建立的 Socket。
	 *
	 * @throws Exception 网络操作失败
	 */
	public void testRejectedCallbacksStillCloseConnectedSocket() throws Exception {
		try (ServerSocket remote = listening()) {
			AtomicInteger submissions = new AtomicInteger();
			AtomicInteger failures = new AtomicInteger();
			Executor rejecting = action -> {
				if (submissions.incrementAndGet() <= 2) {
					Thread.currentThread().setUncaughtExceptionHandler((thread, failure) -> failures.incrementAndGet());
					throw new RejectedExecutionException("测试回调队列已关闭");
				}
				callbacks.execute(action);
			};
			TcpTransport.Client client = new TcpTransport.Client(rejecting, new ClientEvents());
			clients.add(client);
			assertTrue(client.connect("127.0.0.1", remote.getLocalPort(), 1000));
			Socket peer = remember(remote.accept());
			assertEquals("回调提交失败后，对端应收到 EOF", -1, peer.getInputStream().read());
			await(() -> failures.get() == 1 && workerCount() == 0, false);
			assertFalse(client.isConnected());
		}
	}

	/**
	 * 服务端停止再启动后，排队的旧监听、旧客户和旧数据不能串入新会话。
	 *
	 * @throws Exception 网络操作失败
	 */
	public void testServerRestartDropsQueuedOldCallbacks() throws Exception {
		ServerEvents events = new ServerEvents();
		TcpTransport.Server server = server(events);
		assertTrue(server.start(0));
		await(server::isListening, false);
		Socket oldPeer = connect(server.getPort());
		await(() -> server.getClientCount() == 1, false);
		oldPeer.getOutputStream().write(new byte[]{1, 2, 3});
		await(() -> callbacks.size() >= 3, false);
		server.stop();
		assertTrue(server.start(0));
		await(server::isListening, false);
		Socket newPeer = connect(server.getPort());
		newPeer.getOutputStream().write(new byte[]{9, 8, 7});
		await(() -> events.starts == 1 && events.enters == 1 && events.data.size() == 3, true);
		assertTrue(Arrays.equals(new byte[]{9, 8, 7}, events.data.toByteArray()));
		assertEquals(-1, oldPeer.getInputStream().read());
		assertEquals(0, events.stops);
	}

	/**
	 * 使用测试结果接收器创建客户端并登记清理。
	 *
	 * @param events 结果接收器
	 * @return 客户端
	 */
	private TcpTransport.Client client(ClientEvents events) {
		TcpTransport.Client client = new TcpTransport.Client(callbacks, events);
		clients.add(client);
		return client;
	}

	/**
	 * 使用测试结果接收器创建服务端并登记清理。
	 *
	 * @param events 结果接收器
	 * @return 服务端
	 */
	private TcpTransport.Server server(ServerEvents events) {
		TcpTransport.Server server = new TcpTransport.Server(callbacks, events);
		servers.add(server);
		return server;
	}

	/**
	 * 打开仅供回环测试使用的系统分配端口。
	 *
	 * @return 有有限接受超时的监听 Socket
	 * @throws Exception 创建失败
	 */
	private static ServerSocket listening() throws Exception {
		ServerSocket socket = new ServerSocket(0, 10, java.net.InetAddress.getByName("127.0.0.1"));
		socket.setSoTimeout(3000);
		return socket;
	}

	/**
	 * 连接真实服务端并登记清理。
	 *
	 * @param port 服务端端口
	 * @return 客户 Socket
	 * @throws Exception 连接失败
	 */
	private Socket connect(int port) throws Exception {
		Socket socket = new Socket();
		socket.connect(new InetSocketAddress("127.0.0.1", port), 1000);
		return remember(socket);
	}

	/**
	 * 为外部 Socket 设置读超时并登记清理。
	 *
	 * @param socket 待登记 Socket
	 * @return 同一个 Socket
	 * @throws Exception 设置失败
	 */
	private Socket remember(Socket socket) throws Exception {
		socket.setSoTimeout(3000);
		sockets.add(socket);
		return socket;
	}

	/**
	 * 在有限时间内等待真实网络条件，按需交付串行回调。
	 *
	 * @param condition 预期条件
	 * @param drain 是否交付回调
	 * @throws Exception 等待失败
	 */
	private void await(BooleanSupplier condition, boolean drain) throws Exception {
		long deadline = System.nanoTime() + 5000000000L;
		while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
			if (drain) {
				callbacks.drain();
			}
			Thread.sleep(2);
		}
		if (drain) {
			callbacks.drain();
		}
		assertTrue("等待网络条件超时", condition.getAsBoolean());
	}

	/**
	 * 统计本传输实现仍在运行的线程。
	 *
	 * @return 网络工作线程数
	 */
	private static int workerCount() {
		int count = 0;
		for (Thread thread : Thread.getAllStackTraces().keySet()) {
			if (thread.isAlive() && thread.getName().startsWith("SimpleTcp")) {
				count++;
			}
		}
		return count;
	}

	/**
	 * 计算已耗时的毫秒数。
	 *
	 * @param begin 开始的纳秒时间
	 * @return 已耗时毫秒数
	 */
	private static long elapsedMillis(long begin) {
		return (System.nanoTime() - begin) / 1000000;
	}

	/**
	 * 可控制交付时机的线程安全串行消息队列。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	private static final class Queue implements Executor {

		private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();

		@Override
		public void execute(Runnable action) {
			tasks.add(action);
		}

		/**
		 * 交付当前全部任务，网络结果仍可继续排队。
		 */
		void drain() {
			Runnable action;
			while ((action = tasks.poll()) != null) {
				action.run();
			}
		}

		/**
		 * 取得尚未交付的任务数。
		 *
		 * @return 任务数量
		 */
		int size() {
			return tasks.size();
		}
	}

	/**
	 * 客户端事件观测结果。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	private static final class ClientEvents implements TcpTransport.ClientListener {

		int successes;
		int failures;
		int disconnects;
		int sends;
		final ByteArrayOutputStream data = new ByteArrayOutputStream();

		@Override
		public void connected(boolean success) {
			if (success) {
				successes++;
			} else {
				failures++;
			}
		}

		@Override
		public void received(byte[] bytes) {
			data.write(bytes, 0, bytes.length);
		}

		@Override
		public void disconnected() {
			disconnects++;
		}

		@Override
		public void sent(int length) {
			sends++;
		}

		@Override
		public void failed(String operation, String message) {
			assertFalse(message.isEmpty());
		}
	}

	/**
	 * 服务端事件观测结果。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	private static final class ServerEvents implements TcpTransport.ServerListener {

		int starts;
		int failedStarts;
		int stops;
		int enters;
		int leaves;
		int sends;
		String address;
		int port;
		final ByteArrayOutputStream data = new ByteArrayOutputStream();

		@Override
		public void started(boolean success, int port) {
			if (success) {
				starts++;
			} else {
				failedStarts++;
			}
		}

		@Override
		public void stopped() {
			stops++;
		}

		@Override
		public void entered(String address, int port) {
			enters++;
			this.address = address;
			this.port = port;
		}

		@Override
		public void received(byte[] bytes, String address, int port) {
			data.write(bytes, 0, bytes.length);
		}

		@Override
		public void left(String address, int port) {
			leaves++;
		}

		@Override
		public void sent(int length, String address, int port) {
			sends++;
		}

		@Override
		public void failed(String operation, String message) {
			assertFalse(message.isEmpty());
		}
	}
}

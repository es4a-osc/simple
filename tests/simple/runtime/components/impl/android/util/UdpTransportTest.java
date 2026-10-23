package simple.runtime.components.impl.android.util;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import junit.framework.TestCase;

/**
 * 使用真实 UDP 回环验证数据报边界、错误反馈、取消隔离和队列上限。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class UdpTransportTest extends TestCase {

	private final List<Endpoint> endpoints = new ArrayList<>();
	private final List<DatagramSocket> peers = new ArrayList<>();
	private final List<ExecutorService> executors = new ArrayList<>();

	/**
	 * 保存测试中交付的数据和事件。
	 */
	private static final class Endpoint implements UdpTransport.Listener {

		private final BlockingQueue<Integer> starts = new LinkedBlockingQueue<>();
		private final BlockingQueue<DatagramPacket> received = new LinkedBlockingQueue<>();
		private final BlockingQueue<Integer> sent = new LinkedBlockingQueue<>();
		private final BlockingQueue<String> errors = new LinkedBlockingQueue<>();
		private final UdpTransport transport;
		private volatile int stops;

		/**
		 * 创建一个测试端点。
		 *
		 * @param callbacks 回调执行器
		 */
		Endpoint(Executor callbacks) {
			transport = new UdpTransport(callbacks, this);
		}

		@Override
		public void started(boolean success, int port) {
			starts.add(success ? port : 0);
		}

		@Override
		public void received(byte[] data, String address, int port) {
			DatagramPacket packet = new DatagramPacket(data, data.length);
			packet.setPort(port);
			received.add(packet);
		}

		@Override
		public void sent(int length, String address, int port) {
			sent.add(length);
		}

		@Override
		public void stopped() {
			stops++;
		}

		@Override
		public void failed(String operation, String message) {
			errors.add(operation + ":" + message);
		}
	}

	/**
	 * 手动推进主线程回调，用于模拟界面停顿。
	 */
	private static final class ManualExecutor implements Executor {

		private final BlockingQueue<Runnable> actions = new LinkedBlockingQueue<>();

		@Override
		public void execute(Runnable action) {
			actions.add(action);
		}

		/**
		 * 交付当前已经排队的回调。
		 */
		void drain() {
			Runnable action;
			while ((action = actions.poll()) != null) {
				action.run();
			}
		}
	}

	/**
	 * 创建有独立串行回调线程的端点。
	 *
	 * @return 测试端点
	 */
	private Endpoint endpoint() {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executors.add(executor);
		return endpoint(executor);
	}

	/**
	 * 创建指定回调执行器的端点。
	 *
	 * @param executor 指定执行器
	 * @return 测试端点
	 */
	private Endpoint endpoint(Executor executor) {
		Endpoint endpoint = new Endpoint(executor);
		endpoints.add(endpoint);
		return endpoint;
	}

	/**
	 * 创建绑定本地随机端口的真实对端。
	 *
	 * @return 对端 Socket
	 * @throws Exception 创建失败
	 */
	private DatagramSocket peer() throws Exception {
		DatagramSocket socket = new DatagramSocket(0, InetAddress.getByName("127.0.0.1"));
		socket.setSoTimeout(5000);
		peers.add(socket);
		return socket;
	}

	/**
	 * 在有限期限内等待异步条件。
	 *
	 * @param condition 待验证条件
	 * @throws Exception 等待被中断
	 */
	private static void await(BooleanSupplier condition) throws Exception {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
			Thread.sleep(5);
		}
		assertTrue("异步条件未完成", condition.getAsBoolean());
	}

	/**
	 * 取出一个回调结果。
	 *
	 * @param queue 结果队列
	 * @param <T> 结果类型
	 * @return 实际结果
	 * @throws Exception 等待被中断
	 */
	private static <T> T take(BlockingQueue<T> queue) throws Exception {
		T result = queue.poll(5, TimeUnit.SECONDS);
		assertNotNull("缺少异步结果", result);
		return result;
	}

	/**
	 * 从真实对端发送一个完整数据报。
	 *
	 * @param socket 对端 Socket
	 * @param port 目标端口
	 * @param data 数据报
	 * @throws Exception 发送失败
	 */
	private static void send(DatagramSocket socket, int port, byte[] data) throws Exception {
		socket.send(new DatagramPacket(data, data.length, InetAddress.getByName("127.0.0.1"), port));
	}

	/**
	 * 验证独立数据报、二进制、零字节报及同源端口的双向收发。
	 *
	 * @throws Exception 网络操作或等待失败
	 */
	public void testBidirectionalDatagramsAndZeroLength() throws Exception {
		Endpoint endpoint = endpoint();
		assertTrue(endpoint.transport.start(0));
		int port = take(endpoint.starts);
		assertTrue(port > 0);
		DatagramSocket peer = peer();
		for (byte[] bytes : new byte[][]{{0, -1, 127}, {}, new byte[1024], {42}}) {
			assertTrue(endpoint.transport.send(bytes, "localhost", peer.getLocalPort()));
			Arrays.fill(bytes, (byte) 99);
			DatagramPacket packet = new DatagramPacket(new byte[65535], 65535);
			peer.receive(packet);
			assertEquals(port, packet.getPort());
			assertEquals(bytes.length, packet.getLength());
			if (bytes.length == 3) {
				assertEquals(-1, packet.getData()[1]);
			}
			assertEquals(bytes.length, take(endpoint.sent).intValue());
			send(peer, port, Arrays.copyOf(packet.getData(), packet.getLength()));
			DatagramPacket received = take(endpoint.received);
			assertEquals(peer.getLocalPort(), received.getPort());
			assertEquals(packet.getLength(), received.getLength());
			assertTrue(Arrays.equals(Arrays.copyOf(packet.getData(), packet.getLength()), received.getData()));
		}
	}

	/**
	 * 超限数据不能作为半包交付，后续合法数据仍可接收。
	 *
	 * @throws Exception 网络操作或等待失败
	 */
	public void testOversizedPacketRejectedAndReceiverRecovers() throws Exception {
		Endpoint endpoint = endpoint();
		assertTrue(endpoint.transport.setBufferSize(4));
		assertTrue(endpoint.transport.start(0));
		int port = take(endpoint.starts);
		DatagramSocket peer = peer();
		send(peer, port, new byte[5]);
		assertTrue(take(endpoint.errors).contains("整包丢弃"));
		assertNull(endpoint.received.poll());
		send(peer, port, new byte[]{1, 2, 3, 4});
		assertEquals(4, take(endpoint.received).getLength());
	}

	/**
	 * 覆盖无效端口、数组和接收大小，不创建伪成功状态。
	 *
	 * @throws Exception 创建失败
	 */
	public void testInvalidParametersAndUnstartedSend() throws Exception {
		Endpoint endpoint = endpoint();
		assertFalse(endpoint.transport.start(-1));
		assertFalse(endpoint.transport.start(65536));
		assertFalse(endpoint.transport.setBufferSize(0));
		assertFalse(endpoint.transport.setBufferSize(65508));
		assertFalse(endpoint.transport.send(null, "127.0.0.1", 1234));
		assertFalse(endpoint.transport.send(new byte[65508], "127.0.0.1", 1234));
		assertFalse(endpoint.transport.send(new byte[1], null, 1234));
		assertFalse(endpoint.transport.send(new byte[1], "", 0));
		assertFalse(endpoint.transport.send(new byte[1], "", 65536));
		assertFalse(endpoint.transport.send(new byte[1], "127.0.0.1", 1234));
		assertFalse(endpoint.transport.isListening());
		assertEquals(0, endpoint.transport.getLocalPort());
		assertTrue(endpoint.transport.getLastError().contains("尚未"));
	}

	/**
	 * 端口占用产生启动失败，释放后能够重新绑定。
	 *
	 * @throws Exception 网络操作或等待失败
	 */
	public void testBindFailureAndRetry() throws Exception {
		Endpoint endpoint = endpoint();
		DatagramSocket peer = peer();
		int port = peer.getLocalPort();
		assertTrue(endpoint.transport.start(port));
		assertEquals(0, take(endpoint.starts).intValue());
		assertFalse(endpoint.transport.isListening());
		assertTrue(take(endpoint.errors).startsWith("开始监听:"));
		peer.close();
		assertTrue(endpoint.transport.start(port));
		assertEquals(port, take(endpoint.starts).intValue());
		assertEquals("", endpoint.transport.getLastError());
	}

	/**
	 * 重复启动不能覆盖原 Socket，停止后原端口必须可被重新使用。
	 *
	 * @throws Exception 网络操作或等待失败
	 */
	public void testDuplicateStartAndPortRelease() throws Exception {
		Endpoint endpoint = endpoint();
		assertTrue(endpoint.transport.start(0));
		int port = take(endpoint.starts);
		assertFalse(endpoint.transport.start(0));
		assertEquals(port, endpoint.transport.getLocalPort());
		endpoint.transport.stop();
		assertEquals(0, endpoint.transport.getLocalPort());
		DatagramSocket replacement = new DatagramSocket(port);
		peers.add(replacement);
		assertFalse(endpoint.transport.isListening());
		await(() -> endpoint.stops == 1);
	}

	/**
	 * 停止重启必须过滤已排队的旧接收及旧停止回调。
	 *
	 * @throws Exception 网络操作或等待失败
	 */
	public void testRestartFiltersQueuedOldCallbacks() throws Exception {
		ManualExecutor callbacks = new ManualExecutor();
		Endpoint endpoint = endpoint(callbacks);
		assertTrue(endpoint.transport.start(0));
		await(endpoint.transport::isListening);
		callbacks.drain();
		int port = endpoint.transport.getLocalPort();
		send(peer(), port, new byte[]{1});
		await(() -> callbacks.actions.size() == 1);
		endpoint.transport.stop();
		assertTrue(endpoint.transport.start(port));
		await(endpoint.transport::isListening);
		callbacks.drain();
		assertNull(endpoint.received.poll());
		assertEquals(0, endpoint.stops);
		send(peer(), port, new byte[]{2});
		await(() -> !callbacks.actions.isEmpty());
		callbacks.drain();
		assertEquals(2, take(endpoint.received).getData()[0]);
	}

	/**
	 * 反复立即取消后，不允许旧绑定重新占据组件。
	 *
	 * @throws Exception 网络操作或等待失败
	 */
	public void testImmediateCancellation() throws Exception {
		Endpoint endpoint = endpoint();
		for (int index = 0; index < 40; index++) {
			assertTrue(endpoint.transport.start(0));
			endpoint.transport.stop();
			assertFalse(endpoint.transport.isListening());
		}
		assertTrue(endpoint.transport.start(0));
		await(endpoint.transport::isListening);
		assertTrue(endpoint.transport.getLocalPort() > 0);
	}

	/**
	 * 有效端点中的解析失败应异步报告，且不破坏后续收发。
	 *
	 * @throws Exception 网络操作或等待失败
	 */
	public void testResolutionFailureAndRecovery() throws Exception {
		Endpoint endpoint = endpoint();
		assertTrue(endpoint.transport.start(0));
		take(endpoint.starts);
		assertTrue(endpoint.transport.send(new byte[]{1}, "invalid host name", 1234));
		assertTrue(take(endpoint.errors).startsWith("发送数据:"));
		assertTrue(endpoint.transport.isListening());
		DatagramSocket peer = peer();
		assertTrue(endpoint.transport.send(new byte[]{2}, "127.0.0.1", peer.getLocalPort()));
		assertEquals(1, take(endpoint.sent).intValue());
		assertEquals("", endpoint.transport.getLastError());
	}

	/**
	 * 模拟主线程停顿，待交付回调与发送项均应有上限。
	 *
	 * @throws Exception 网络操作或等待失败
	 */
	public void testBoundedCallbacksAndSendQueue() throws Exception {
		ManualExecutor callbacks = new ManualExecutor();
		Endpoint endpoint = endpoint(callbacks);
		assertTrue(endpoint.transport.start(0));
		await(endpoint.transport::isListening);
		callbacks.drain();
		DatagramSocket peer = peer();
		for (int index = 0; index < 20; index++) {
			assertTrue(endpoint.transport.send(new byte[0], "127.0.0.1", peer.getLocalPort()));
		}
		await(() -> callbacks.actions.size() == 16);
		int accepted = 0;
		while (endpoint.transport.send(new byte[0], "127.0.0.1", peer.getLocalPort())) {
			accepted++;
			assertTrue(accepted <= 128);
		}
		assertTrue(accepted > 0);
		assertEquals(17, callbacks.actions.size());
		assertTrue(endpoint.transport.getLastError().contains("队列已满"));
		endpoint.transport.stop();
		callbacks.drain();
		assertTrue(endpoint.sent.isEmpty());
	}

	/**
	 * 未完成发送的数据总量达到上限时应拒绝继续复制大数组。
	 *
	 * @throws Exception 网络操作或等待失败
	 */
	public void testBoundedPendingByteBudget() throws Exception {
		ManualExecutor callbacks = new ManualExecutor();
		Endpoint endpoint = endpoint(callbacks);
		assertTrue(endpoint.transport.start(0));
		await(endpoint.transport::isListening);
		callbacks.drain();
		DatagramSocket peer = peer();
		for (int index = 0; index < 20; index++) {
			assertTrue(endpoint.transport.send(new byte[0], "127.0.0.1", peer.getLocalPort()));
		}
		await(() -> callbacks.actions.size() == 16);
		byte[] large = new byte[UdpTransport.MAX_PACKET_SIZE];
		int accepted = 0;
		while (endpoint.transport.send(large, "127.0.0.1", peer.getLocalPort())) {
			accepted++;
			assertTrue(accepted <= 64);
		}
		assertEquals(64, accepted);
		endpoint.transport.stop();
		callbacks.drain();
	}

	/**
	 * 最大允许数据报仍必须以完整长度接收。
	 *
	 * @throws Exception 网络操作或等待失败
	 */
	public void testMaximumDatagram() throws Exception {
		Endpoint endpoint = endpoint();
		assertTrue(endpoint.transport.setBufferSize(UdpTransport.MAX_PACKET_SIZE));
		assertTrue(endpoint.transport.start(0));
		int port = take(endpoint.starts);
		byte[] data = new byte[UdpTransport.MAX_PACKET_SIZE];
		Arrays.fill(data, (byte) 85);
		send(peer(), port, data);
		assertTrue(Arrays.equals(data, take(endpoint.received).getData()));
	}

	/**
	 * 销毁释放端口并永久拒绝重启，且不交付销毁前排队事件。
	 *
	 * @throws Exception 网络操作或等待失败
	 */
	public void testDestroyCancelsQueuedCallbacksAndReleasesPort() throws Exception {
		ManualExecutor callbacks = new ManualExecutor();
		Endpoint endpoint = endpoint(callbacks);
		assertTrue(endpoint.transport.start(0));
		await(endpoint.transport::isListening);
		int port = endpoint.transport.getLocalPort();
		endpoint.transport.destroy();
		callbacks.drain();
		assertTrue(endpoint.starts.isEmpty());
		assertFalse(endpoint.transport.start(0));
		assertFalse(endpoint.transport.send(new byte[0], "", 1234));
		assertFalse(endpoint.transport.setBufferSize(10));
		assertEquals("组件已销毁", endpoint.transport.getLastError());
		DatagramSocket replacement = new DatagramSocket(port);
		peers.add(replacement);
	}

	@Override
	protected void tearDown() throws Exception {
		for (Endpoint endpoint : endpoints) {
			endpoint.transport.destroy();
		}
		for (DatagramSocket socket : peers) {
			socket.close();
		}
		for (ExecutorService executor : executors) {
			executor.shutdown();
			assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
		}
		await(() -> Thread.getAllStackTraces().keySet().stream()
				.noneMatch(thread -> thread.isAlive() && thread.getName().startsWith("SimpleUdp-")));
	}
}

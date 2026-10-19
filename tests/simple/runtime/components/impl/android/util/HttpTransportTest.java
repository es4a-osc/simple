package simple.runtime.components.impl.android.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 通过本机真实 HTTP 验证正文格式、流式发送、响应完整性及取消。
 * 使用项目现有 JUnit 测试入口，不加载或替代 Android 组件。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class HttpTransportTest extends TestCase {

	private static final Charset UTF8 = Charset.forName("UTF-8");
	private final AtomicInteger redirectTargets = new AtomicInteger();
	private final CountDownLatch slowEntered = new CountDownLatch(1);
	private String base;
	private HttpServer server;
	private ExecutorService workers;

	@Override
	protected void setUp() throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		workers = Executors.newCachedThreadPool();
		server.setExecutor(workers);
		server.createContext("/", this::respond);
		server.start();
		base = "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@Override
	protected void tearDown() throws Exception {
		server.stop(0);
		workers.shutdownNow();
		workers.awaitTermination(2, TimeUnit.SECONDS);
	}

	private static String hex(byte[] bytes) {
		StringBuilder result = new StringBuilder();
		for (byte value : bytes) {
			result.append(String.format("%02x", value & 255));
		}
		return result.toString();
	}

	private static String repeatedDigest() throws Exception {
		java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
		byte[] block = new byte[32768];
		java.util.Arrays.fill(block, (byte) 37);
		for (int i = 0; i < 1024; i++) {
			digest.update(block);
		}
		return hex(digest.digest());
	}

	private HttpTransport.Result request(String method, String path, HttpTransport.Body body) throws IOException {
		return HttpTransport.execute(method, base + path, Collections.<String, String>emptyMap(), body,
			1000, 1500, 1048576, new HttpTransport.Control(), (upload, count, total) -> { });
	}

	private static void fails(String label, IoAction action) throws Exception {
		try {
			action.run();
			fail(label);
		} catch (IOException expected) {
			// 预期的传输错误。
		}
	}

	private interface IoAction {
		void run() throws Exception;
	}

	private void respond(HttpExchange exchange) throws IOException {
		try {
			String path = exchange.getRequestURI().getPath();
			if (path.equals("/target")) {
				redirectTargets.incrementAndGet();
			}
			if (path.equals("/redirect")) {
				exchange.getResponseHeaders().add("Location", "/target");
				exchange.sendResponseHeaders(302, -1);
				return;
			}
			if (path.equals("/hash")) {
				java.security.MessageDigest digest;
				try {
					digest = java.security.MessageDigest.getInstance("SHA-256");
				} catch (java.security.NoSuchAlgorithmException impossible) {
					throw new IOException(impossible);
				}
				byte[] buffer = new byte[32768];
				long size = 0;
				int count;
				while ((count = exchange.getRequestBody().read(buffer)) != -1) {
					digest.update(buffer, 0, count);
					size += count;
				}
				byte[] result = (Long.toString(size) + ":" + hex(digest.digest())).getBytes(UTF8);
				exchange.sendResponseHeaders(200, result.length);
				exchange.getResponseBody().write(result);
				return;
			}
			if (path.equals("/slow")) {
				slowEntered.countDown();
				try {
					Thread.sleep(10000);
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
				}
			}
			ByteArrayOutputStream body = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int count;
			while ((count = exchange.getRequestBody().read(buffer)) != -1) {
				body.write(buffer, 0, count);
			}
			exchange.getResponseHeaders().add("X-Method", exchange.getRequestMethod());
			String type = exchange.getRequestHeaders().getFirst("Content-Type");
			if (type != null) {
				exchange.getResponseHeaders().add("X-Body-Type", type);
			}
			exchange.getResponseHeaders().add("Content-Type", path.equals("/latin")
				? "text/plain; charset=ISO-8859-1" : "text/plain; charset=UTF-8");
			byte[] result = path.equals("/latin") ? new byte[]{(byte) 233}
				: body.size() == 0 ? "响应中文".getBytes(UTF8) : body.toByteArray();
			int status = path.equals("/created") ? 201 : path.equals("/empty") ? 204
				: path.equals("/missing") ? 404 : path.equals("/failed") ? 500 : 200;
			if (exchange.getRequestMethod().equals("HEAD") || status == 204) {
				exchange.sendResponseHeaders(status, -1);
				return;
			}
			if (path.equals("/truncated")) {
				exchange.sendResponseHeaders(200, result.length + 10);
				exchange.getResponseBody().write(result);
				return;
			}
			exchange.sendResponseHeaders(status, path.equals("/chunked") ? 0 : result.length);
			exchange.getResponseBody().write(result);
		} catch (IOException closed) {
			// 客户端取消、响应上限和截断用例会主动关闭连接。
		} finally {
			exchange.close();
		}
	}

	/**
	 * 验证文本、字节正文、类型与响应副本。
	 *
	 * @throws Exception 本机 HTTP 传输失败。
	 */
	public void testTextAndByteBodies() throws Exception {
		assertEquals("响应中文", request("GET", "/", null).text());
		HttpTransport.Result json = request("POST", "/echo",
			HttpTransport.bytes("{\"中文\":1}".getBytes(UTF8), "application/json; charset=UTF-8"));
		assertEquals("{\"中文\":1}", json.text());
		assertTrue(json.header("x-body-type").startsWith("application/json"));
		assertEquals("POST", json.header("X-METHOD"));
		byte[] copy = json.data();
		copy[0] = 0;
		assertTrue("响应副本不应改变已保存的数据", json.data()[0] != 0);
		assertTrue(java.util.Arrays.equals(new byte[]{1, 2, 3},
			request("PUT", "/echo", HttpTransport.bytes(new byte[]{1, 2, 3}, "application/octet-stream")).data()));
	}

	/**
	 * 验证状态码、空正文、字符集及未知长度响应。
	 *
	 * @throws Exception 本机 HTTP 传输失败。
	 */
	public void testStatusAndCharsetResponses() throws Exception {
		assertEquals(204, request("POST", "/empty", HttpTransport.bytes(new byte[0], "text/plain")).status());
		assertEquals("响应中文", request("GET", "/created", null).text());
		assertEquals(404, request("GET", "/missing", null).status());
		assertEquals("响应中文", request("GET", "/failed", null).text());
		assertEquals(0, request("HEAD", "/", null).data().length);
		assertEquals("é", request("GET", "/latin", null).text());
		assertEquals("响应中文", request("GET", "/chunked", null).text());
	}

	/**
	 * 重定向响应不得自动向目标再次发送请求。
	 *
	 * @throws Exception 本机 HTTP 传输失败。
	 */
	public void testRedirectIsNotFollowed() throws Exception {
		assertEquals(302, request("GET", "/redirect", null).status());
		assertEquals(0, redirectTargets.get());
	}

	/**
	 * 验证文件表单的中文字段、文件名转义及正文。
	 *
	 * @throws Exception 本机 HTTP 传输失败。
	 */
	public void testMultipartForm() throws Exception {
		Map<String, String> fields = new LinkedHashMap<String, String>();
		fields.put("memo", "附加中文 & =");
		HttpTransport.Result form = request("POST", "/echo", HttpTransport.multipart(textSource(),
			"文件内容".getBytes(UTF8).length, "attachment", "文\"件.txt", "text/plain", fields));
		assertTrue(form.header("X-Body-Type").startsWith("multipart/form-data; boundary="));
		assertTrue(form.text().contains("name=\"memo\"") && form.text().contains("附加中文 & ="));
		assertTrue(form.text().contains("name=\"attachment\"; filename=\"文\\\"件.txt\"")
			&& form.text().contains("文件内容"));
	}

	/**
	 * 未知长度文件通过分块传输完整提交。
	 *
	 * @throws Exception 本机 HTTP 传输失败。
	 */
	public void testUnknownLengthFile() throws Exception {
		assertEquals("文件内容", request("POST", "/echo", HttpTransport.file(textSource(), -1, "text/plain")).text());
	}

	/**
	 * 文件实际长度与声明不一致时拒绝返回成功。
	 *
	 * @throws Exception 本机 HTTP 传输失败。
	 */
	public void testChangedSourceLengthFails() throws Exception {
		fails("文件长度变化", () -> request("POST", "/echo", HttpTransport.file(textSource(), 3, "text/plain")));
	}

	/**
	 * GET 不接受提交正文。
	 *
	 * @throws Exception 本机 HTTP 传输失败。
	 */
	public void testGetBodyFails() throws Exception {
		fails("GET 正文拒绝", () -> request("GET", "/", HttpTransport.bytes(new byte[]{1}, "text/plain")));
	}

	/**
	 * 响应截断必须报告失败。
	 *
	 * @throws Exception 本机 HTTP 传输失败。
	 */
	public void testTruncatedResponseFails() throws Exception {
		fails("响应截断", () -> request("GET", "/truncated", null));
	}

	/**
	 * 响应上限同时约束已知与未知正文长度。
	 *
	 * @throws Exception 本机 HTTP 传输失败。
	 */
	public void testResponseLimit() throws Exception {
		for (String path : new String[]{"/", "/chunked"}) {
			fails("响应大小限制", () -> HttpTransport.execute("GET", base + path, Collections.emptyMap(), null,
				1000, 1000, 3, new HttpTransport.Control(), (upload, count, total) -> { }));
		}
	}

	/**
	 * 传输只接受 HTTP 和 HTTPS。
	 *
	 * @throws Exception 地址校验未产生预期错误。
	 */
	public void testInvalidProtocolFails() throws Exception {
		fails("非法协议", () -> HttpTransport.validate("GET", "file:/tmp/a"));
	}

	/**
	 * 文件表单字段名不能注入新行。
	 */
	public void testMultipartFieldInjectionFails() {
		try {
			HttpTransport.multipart(textSource(), -1, "bad\r\nname", "a", "text/plain", Collections.emptyMap());
			fail("应拒绝字段名中的换行");
		} catch (IllegalArgumentException expected) {
			// 字段名校验必须在打开来源前完成。
		}
	}

	/**
	 * 32 MiB 内容即时生成并流式发送，校验服务端收到的长度和摘要。
	 *
	 * @throws Exception 本机 HTTP 传输失败。
	 */
	public void testLargeFileStreamsWithMatchingHash() throws Exception {
		HttpTransport.Source generated = () -> new InputStream() {
			private int remaining = 32 * 1024 * 1024;

			@Override
			public int read() {
				return remaining-- > 0 ? 37 : -1;
			}

			@Override
			public int read(byte[] buffer, int offset, int length) {
				if (remaining <= 0) {
					return -1;
				}
				int count = Math.min(length, remaining);
				java.util.Arrays.fill(buffer, offset, offset + count, (byte) 37);
				remaining -= count;
				return count;
			}
		};
		assertEquals("33554432:" + repeatedDigest(), request("POST", "/hash",
			HttpTransport.file(generated, 32 * 1024 * 1024L, "application/octet-stream")).text());
	}

	/**
	 * 服务端确认已收到请求后取消，不得依靠读取超时结束测试。
	 *
	 * @throws Exception 本机 HTTP 传输或线程等待失败。
	 */
	public void testCancellingBlockedResponse() throws Exception {
		HttpTransport.Control control = new HttpTransport.Control();
		AtomicReference<Throwable> outcome = new AtomicReference<Throwable>();
		Thread waiting = new Thread(() -> {
			try {
				HttpTransport.execute("GET", base + "/slow", Collections.emptyMap(), null,
					1000, 10000, 1000, control, (upload, count, total) -> { });
				outcome.set(new AssertionError("取消后仍成功"));
			} catch (IOException expected) {
				outcome.set(expected);
			}
		});
		waiting.setDaemon(true);
		waiting.start();
		try {
			assertTrue("请求应到达服务端", slowEntered.await(2, TimeUnit.SECONDS));
			long start = System.nanoTime();
			control.abort();
			waiting.join(3000);
			assertFalse("取消后仍在等待响应", waiting.isAlive());
			assertTrue(outcome.get() instanceof IOException);
			assertTrue("取消不应等待 10 秒的读取超时", System.nanoTime() - start < TimeUnit.SECONDS.toNanos(3));
		} finally {
			control.markCancelled();
			waiting.interrupt();
		}
	}

	private static HttpTransport.Source textSource() {
		return () -> new ByteArrayInputStream("文件内容".getBytes(UTF8));
	}
}

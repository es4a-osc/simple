package simple.runtime.components.impl.android.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP 客户端的单次阻塞传输，独立于 Android 界面和 Simple 事件。
 * 请求正文按流发送，响应受大小上限约束；本类不自动重试或跟随重定向。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class HttpTransport {

	/**
	 * 可重新打开的文件来源，打开和元数据查询由组件工作线程执行。
	 */
	public interface Source {

		/**
		 * 打开本次读取使用的输入流。
		 *
		 * @return 由传输负责关闭的输入流。
		 * @throws IOException 无法读取文件。
		 */
		InputStream open() throws IOException;
	}

	/**
	 * 工作线程进度通知；接收方负责节流和切换到主线程。
	 */
	public interface Progress {

		/**
		 * 通知正文写出或响应读入的字节数，不代表服务端业务处理完成。
		 *
		 * @param upload 真表示请求正文，假表示响应正文。
		 * @param count 已传输字节数。
		 * @param total 总字节数，未知时为 -1。
		 */
		void update(boolean upload, long count, long total);
	}

	/**
	 * 单次任务的取消标记及当前资源；取消不得在界面线程执行阻塞关闭。
	 */
	public static final class Control {

		private volatile boolean cancelled;
		private HttpURLConnection connection;
		private Closeable input;

		/**
		 * 立即登记取消，后续读取和写入检查会停止传输。
		 */
		public void markCancelled() {
			cancelled = true;
		}

		/**
		 * 在工作线程尝试中断连接和当前输入流；不承诺提供方立即返回。
		 */
		public void abort() {
			markCancelled();
			HttpURLConnection current;
			Closeable source;
			synchronized (this) {
				current = connection;
				source = input;
			}
			if (current != null) {
				try {
					current.disconnect();
				} catch (RuntimeException ignored) {
					// 某些连接实现关闭时可能报错，仍须尝试关闭文件来源。
				}
			}
			if (source != null) {
				try {
					source.close();
				} catch (IOException ignored) {
					// 正常收尾仍会关闭资源，取消错误不能覆盖终止结果。
				}
			}
		}

		private void check() throws IOException {
			if (cancelled || Thread.currentThread().isInterrupted()) {
				throw new InterruptedIOException("请求已取消。");
			}
		}

		private synchronized void attach(HttpURLConnection value) throws IOException {
			check();
			connection = value;
		}

		private synchronized void attach(Closeable value) throws IOException {
			check();
			input = value;
		}
	}

	/**
	 * 已准备好的正文描述；文件内容在真正发送时打开，不预先读入内存。
	 */
	public static final class Body {

		private final byte[] prefix;
		private final Source source;
		private final long sourceLength;
		private final byte[] suffix;
		private final String contentType;

		private Body(byte[] prefix, Source source, long sourceLength, byte[] suffix, String contentType) {
			this.prefix = prefix;
			this.source = source;
			this.sourceLength = sourceLength;
			this.suffix = suffix;
			this.contentType = contentType;
		}

		private long length() throws IOException {
			if (sourceLength < 0) {
				return -1;
			}
			long extra = (long) prefix.length + suffix.length;
			if (sourceLength > Long.MAX_VALUE - extra) {
				throw new IOException("请求正文长度超出长整数范围。");
			}
			return sourceLength + extra;
		}
	}

	/**
	 * 独立响应快照；组件在主线程发布后再通知完成事件。
	 */
	public static final class Result {

		private final int status;
		private final Map<String, List<String>> headers;
		private final byte[] data;
		private final String text;

		private Result(int status, Map<String, List<String>> headers, byte[] data, String type) {
			this.status = status;
			Map<String, List<String>> copy = new LinkedHashMap<String, List<String>>();
			for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
				copy.put(entry.getKey(), Collections.unmodifiableList(new java.util.ArrayList<String>(entry.getValue())));
			}
			this.headers = Collections.unmodifiableMap(copy);
			this.data = data;
			Charset charset = Charset.forName("UTF-8");
			if (type != null) {
				for (String parameter : type.split(";")) {
					String value = parameter.trim();
					if (value.toLowerCase(java.util.Locale.US).startsWith("charset=")) {
						try {
							charset = Charset.forName(value.substring(8).trim().replace("\"", ""));
						} catch (IllegalArgumentException ignored) {
							// 无效或不支持的响应字符集按公开约定回退为 UTF-8。
						}
					}
				}
			}
			text = new String(data, charset);
		}

		/**
		 * 返回 HTTP 状态码，4xx 和 5xx 同样是有效响应。
		 *
		 * @return HTTP 状态码。
		 */
		public int status() {
			return status;
		}

		/**
		 * 按响应字符集解码正文，缺失或不支持的字符集使用 UTF-8。
		 *
		 * @return 响应文本。
		 */
		public String text() {
			return text;
		}

		/**
		 * 返回正文副本，避免调用方修改已发布结果。
		 *
		 * @return 独立字节数组。
		 */
		public byte[] data() {
			return data.clone();
		}

		/**
		 * 不区分大小写查找响应头，同名多值以逗号及空格连接。
		 *
		 * @param name 响应头名称。
		 * @return 响应头值；不存在时为空文本。
		 */
		public String header(String name) {
			for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
				if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
					StringBuilder value = new StringBuilder();
					for (String part : entry.getValue()) {
						if (value.length() > 0) {
							value.append(", ");
						}
						value.append(part);
					}
					return value.toString();
				}
			}
			return "";
		}
	}

	private HttpTransport() {
	}

	/**
	 * 校验地址与请求方式，不建立网络连接。
	 *
	 * @param method 请求方式，支持 GET、POST、PUT、DELETE、HEAD 和 OPTIONS。
	 * @param address HTTP/HTTPS 完整地址。
	 * @throws IOException 地址或方式无效。
	 */
	public static void validate(String method, String address) throws IOException {
		if (!java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS").contains(method)) {
			throw new IOException("不支持的请求方式：" + method);
		}
		URL url = new URL(address);
		if (!"http".equals(url.getProtocol()) && !"https".equals(url.getProtocol())) {
			throw new IOException("请求地址只支持 HTTP 和 HTTPS。");
		}
	}

	/**
	 * 建立字节正文，调用方应提供独立数组。
	 *
	 * @param data 完整正文。
	 * @param type 内容类型。
	 * @return 正文描述。
	 */
	public static Body bytes(final byte[] data, String type) {
		return file(new Source() {
			@Override
			public InputStream open() {
				return new ByteArrayInputStream(data);
			}
		}, data.length, type);
	}

	/**
	 * 建立原始文件正文，源长度未知时使用分块传输。
	 *
	 * @param source 文件来源。
	 * @param length 字节数，未知时为 -1。
	 * @param type 内容类型。
	 * @return 正文描述。
	 */
	public static Body file(Source source, long length, String type) {
		return new Body(new byte[0], source, length, new byte[0], type);
	}

	/**
	 * 建立单文件与附加文本字段组成的 multipart 正文。
	 *
	 * @param source 文件来源。
	 * @param length 文件字节数，未知时为 -1。
	 * @param name 表单文件字段名。
	 * @param filename 提供给服务器的文件名。
	 * @param type 文件内容类型。
	 * @param fields 附加字段，名称和值均按 UTF-8 编码。
	 * @return 流式文件表单正文。
	 */
	public static Body multipart(Source source, long length, String name, String filename,
			String type, Map<String, String> fields) {
		String boundary = "ES4A" + java.util.UUID.randomUUID().toString().replace("-", "");
		StringBuilder prefix = new StringBuilder();
		for (Map.Entry<String, String> entry : fields.entrySet()) {
			prefix.append("--").append(boundary).append("\r\nContent-Disposition: form-data; name=\"")
				.append(quoted(entry.getKey())).append("\"\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\n")
				.append(entry.getValue()).append("\r\n");
		}
		prefix.append("--").append(boundary).append("\r\nContent-Disposition: form-data; name=\"")
			.append(quoted(name)).append("\"; filename=\"").append(quoted(filename))
			.append("\"\r\nContent-Type: ").append(token(type)).append("\r\n\r\n");
		return new Body(prefix.toString().getBytes(Charset.forName("UTF-8")), source, length,
			("\r\n--" + boundary + "--\r\n").getBytes(Charset.forName("UTF-8")),
			"multipart/form-data; boundary=" + boundary);
	}

	/**
	 * 拒绝会破坏 HTTP 头或 multipart 行结构的值。
	 *
	 * @param value 待校验值。
	 * @return 原值。
	 * @throws IllegalArgumentException 值为空引用或包含换行、零字符。
	 */
	public static String token(String value) {
		if (value == null || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0 || value.indexOf(0) >= 0) {
			throw new IllegalArgumentException("请求头、字段名或文件名包含无效字符。");
		}
		return value;
	}

	private static String quoted(String value) {
		return token(value).replace("\\", "\\\\").replace("\"", "\\\"");
	}

	/**
	 * 执行一次请求；全部 I/O、进度回调和资源关闭均在调用线程进行。
	 *
	 * @param method 请求方式。
	 * @param address HTTP/HTTPS 完整地址。
	 * @param headers 独立请求头快照，正文类型和传输长度由本实现管理。
	 * @param body 正文描述；无正文时为空引用。
	 * @param connectTimeout 连接超时毫秒。
	 * @param readTimeout 读取超时毫秒。
	 * @param limit 最大响应字节数。
	 * @param control 当前任务取消控制。
	 * @param progress 工作线程进度接收者。
	 * @return 独立响应快照。
	 * @throws IOException 网络、文件、取消、长度校验或响应上限错误。
	 */
	public static Result execute(String method, String address, Map<String, String> headers, Body body,
			int connectTimeout, int readTimeout, int limit, Control control, Progress progress) throws IOException {
		validate(method, address);
		control.check();
		HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
		try {
			control.attach(connection);
			connection.setRequestMethod(method);
			connection.setInstanceFollowRedirects(false);
			connection.setConnectTimeout(connectTimeout);
			connection.setReadTimeout(readTimeout);
			connection.setUseCaches(false);
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				connection.setRequestProperty(entry.getKey(), entry.getValue());
			}
			if (body != null) {
				if ("GET".equals(method) || "HEAD".equals(method)) {
					throw new IOException("GET 和 HEAD 请求不能配置提交正文。");
				}
				long length = body.length();
				connection.setDoOutput(true);
				connection.setRequestProperty("Content-Type", body.contentType);
				// int 固定长度入口兼容旧 Android；超大或未知长度使用分块，计数仍为 long。
				if (length >= 0 && length <= Integer.MAX_VALUE) {
					connection.setFixedLengthStreamingMode((int) length);
				} else {
					connection.setChunkedStreamingMode(32768);
				}
				OutputStream output = null;
				InputStream input = null;
				try {
					output = connection.getOutputStream();
					input = body.source.open();
					if (input == null) {
						throw new IOException("文件提供方未返回输入流。");
					}
					control.attach(input);
					output.write(body.prefix);
					long copied = copy(input, output, body.prefix.length, length, control, progress, true, Long.MAX_VALUE);
					if (body.sourceLength >= 0 && copied != body.sourceLength) {
						throw new IOException("文件实际长度与发送前查询的长度不一致。");
					}
					control.check();
					output.write(body.suffix);
					progress.update(true, copied + body.prefix.length + body.suffix.length, length);
					// 显式关闭以完成分块正文，并让写入错误进入请求失败事件。
					output.close();
					output = null;
				} finally {
					closeQuietly(input);
					closeQuietly(output);
				}
			}
			control.check();
			int status = connection.getResponseCode();
			if (status < 100) {
				throw new IOException("服务器未返回有效 HTTP 状态码。");
			}
			ByteArrayOutputStream data = new ByteArrayOutputStream();
			if (!"HEAD".equals(method) && status != 204 && status != 304) {
				long total = responseLength(connection);
				if (total > limit) {
					throw new IOException("响应内容超过大小上限。");
				}
				InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
				try {
					if (input == null && total > 0) {
						throw new IOException("响应连接提前结束。");
					}
					if (input != null) {
						control.attach(input);
						long received = copy(input, data, 0, total, control, progress, false, limit);
						if (total >= 0 && received != total) {
							throw new IOException("响应连接提前结束。");
						}
					}
				} finally {
					closeQuietly(input);
				}
			}
			control.check();
			return new Result(status, connection.getHeaderFields(), data.toByteArray(), connection.getContentType());
		} finally {
			connection.disconnect();
		}
	}

	private static void closeQuietly(Closeable resource) {
		// 最低 API 14 不支持 Throwable.addSuppressed，避免生成对应字节码调用。
		if (resource != null) {
			try {
				resource.close();
			} catch (IOException ignored) {
				// 主体 I/O 已给出传输结果，清理失败不能覆盖它。
			}
		}
	}

	private static long responseLength(HttpURLConnection connection) {
		String encoding = connection.getHeaderField("Content-Encoding");
		if (encoding != null && !"identity".equalsIgnoreCase(encoding)) {
			return -1;
		}
		try {
			String value = connection.getHeaderField("Content-Length");
			return value == null ? -1 : Math.max(-1, Long.parseLong(value));
		} catch (NumberFormatException ignored) {
			return -1;
		}
	}

	private static long copy(InputStream input, OutputStream output, long offset, long total,
			Control control, Progress progress, boolean upload, long limit) throws IOException {
		byte[] buffer = new byte[32768];
		long count = 0;
		progress.update(upload, offset, total);
		int size;
		while ((size = input.read(buffer)) != -1) {
			control.check();
			if (size > limit - count) {
				throw new IOException("响应内容超过大小上限。");
			}
			output.write(buffer, 0, size);
			count += size;
			progress.update(upload, offset + count, total);
		}
		return count;
	}
}

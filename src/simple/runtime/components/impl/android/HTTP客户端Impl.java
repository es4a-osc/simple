package simple.runtime.components.impl.android;

import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import simple.runtime.android.MainActivity;
import simple.runtime.collections.哈希表;
import simple.runtime.components.HTTP客户端;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.components.impl.android.util.HttpTransport;
import simple.runtime.events.EventDispatcher;
import simple.runtime.variants.Variant;

/**
 * HTTP 客户端的主线程配置、后台传输及生命周期适配。
 *
 * <p>每个任务保存参数快照并竞争唯一终止权；结果只在主线程发布。
 * 终止事件前释放忙碌状态，事件中开始的新任务不会被旧任务收尾覆盖。</p>
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class HTTP客户端Impl extends 组件Impl implements HTTP客户端 {

	private final MainActivity activity;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private final Map<String, String> headers = new LinkedHashMap<String, String>();
	private int connectTimeout = 10000;
	private int readTimeout = 30000;
	private int responseLimit = 8388608;
	private PendingBody pendingBody;
	private volatile Session current;
	private volatile HttpTransport.Result result;
	private volatile String lastError = "";
	private volatile boolean destroyed;
	private final MainActivity.OnDestroyListener destroyListener = new MainActivity.OnDestroyListener() {

		@Override
		public void onDestroy() {
			// Activity 正在遍历监听器，此处保留登记，避免修改该集合。
			dispose(false);
		}
	};

	/**
	 * 创建组件并登记宿主销毁通知，不提前建立连接或启动线程。
	 *
	 * @param container 容纳组件的窗口。
	 */
	public HTTP客户端Impl(组件容器 container) {
		super(container);
		activity = MainActivity.getContext();
		activity.addOnDestroyListener(destroyListener);
	}

	/* 组件事件 */

	@Override
	public void 发送进度(long sent, long total) {
		EventDispatcher.dispatchEvent(this, "发送进度", sent, total);
	}

	@Override
	public void 接收进度(long received, long total) {
		EventDispatcher.dispatchEvent(this, "接收进度", received, total);
	}

	@Override
	public void 请求完成(int status) {
		EventDispatcher.dispatchEvent(this, "请求完成", status);
	}

	@Override
	public void 请求失败(String message) {
		EventDispatcher.dispatchEvent(this, "请求失败", message);
	}

	@Override
	public void 请求取消() {
		EventDispatcher.dispatchEvent(this, "请求取消");
	}

	/* 组件属性 */

	@Override
	public int 连接超时() {
		return connectTimeout;
	}

	@Override
	public void 连接超时(int value) {
		requireActive();
		if (value < 0) {
			throw new IllegalArgumentException("连接超时不能为负数。");
		}
		connectTimeout = value;
	}

	@Override
	public int 读入超时() {
		return readTimeout;
	}

	@Override
	public void 读入超时(int value) {
		requireActive();
		if (value < 0) {
			throw new IllegalArgumentException("读入超时不能为负数。");
		}
		readTimeout = value;
	}

	@Override
	public int 响应大小上限() {
		return responseLimit;
	}

	@Override
	public void 响应大小上限(int value) {
		requireActive();
		if (value < 1) {
			throw new IllegalArgumentException("响应大小上限必须大于零。");
		}
		responseLimit = value;
	}

	@Override
	public boolean 正在请求() {
		return current != null;
	}

	@Override
	public int 响应代码() {
		HttpTransport.Result snapshot = result;
		return snapshot == null ? 0 : snapshot.status();
	}

	@Override
	public String 响应文本() {
		HttpTransport.Result snapshot = result;
		return snapshot == null ? "" : snapshot.text();
	}

	@Override
	public byte[] 响应数据() {
		HttpTransport.Result snapshot = result;
		return snapshot == null ? new byte[0] : snapshot.data();
	}

	@Override
	public String 错误信息() {
		return lastError;
	}

	/* 请求配置 */

	@Override
	public void 置请求头(String name, String value) {
		requireActive();
		if (name == null || !name.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+")) {
			throw new IllegalArgumentException("请求头名称无效。");
		}
		String key = name.toLowerCase(Locale.US);
		if ("content-type".equals(key) || "content-length".equals(key) || "transfer-encoding".equals(key)) {
			throw new IllegalArgumentException("正文类型、长度和传输编码由提交内容管理。");
		}
		headers.put(key, HttpTransport.token(value));
	}

	@Override
	public void 清空请求头() {
		requireActive();
		headers.clear();
	}

	@Override
	public void 置提交文本(String text) {
		置提交文本(text, "text/plain");
	}

	@Override
	public void 置提交文本(String text, String type) {
		requireActive();
		if (text == null || type == null || !type.matches("[A-Za-z0-9!#$&^_.+-]+/[A-Za-z0-9!#$&^_.+-]+")) {
			throw new IllegalArgumentException("提交文本或内容类型无效，内容类型不应带字符集参数。");
		}
		pendingBody = new PendingBody(text.getBytes(Charset.forName("UTF-8")), type + "; charset=UTF-8");
	}

	@Override
	public void 置提交数据(byte[] data) {
		requireActive();
		if (data == null) {
			throw new IllegalArgumentException("提交字节集不能是空引用。");
		}
		pendingBody = new PendingBody(data.clone(), "application/octet-stream");
	}

	@Override
	public void 置提交表单(哈希表 fields) {
		requireActive();
		StringBuilder encoded = new StringBuilder();
		try {
			for (Map.Entry<String, String> entry : copyFields(fields).entrySet()) {
				if (encoded.length() > 0) {
					encoded.append('&');
				}
				encoded.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append('=')
					.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
			}
		} catch (java.io.UnsupportedEncodingException impossible) {
			throw new IllegalStateException("系统不支持 UTF-8。", impossible);
		}
		pendingBody = new PendingBody(encoded.toString().getBytes(Charset.forName("UTF-8")),
			"application/x-www-form-urlencoded; charset=UTF-8");
	}

	@Override
	public void 置提交文件(String address) {
		requireActive();
		pendingBody = new PendingBody(address, null, Collections.<String, String>emptyMap());
	}

	@Override
	public void 置提交文件表单(String name, String address) {
		requireActive();
		pendingBody = new PendingBody(address, checkedName(name), Collections.<String, String>emptyMap());
	}

	@Override
	public void 置提交文件表单(String name, String address, 哈希表 fields) {
		requireActive();
		pendingBody = new PendingBody(address, checkedName(name), copyFields(fields));
	}

	@Override
	public void 清空提交内容() {
		requireActive();
		pendingBody = null;
	}

	/* 请求执行 */

	@Override
	public boolean 发送请求(String address) {
		return 发送请求("GET", address);
	}

	@Override
	public boolean 发送请求(String method, String address) {
		requireActive();
		if (current != null) {
			lastError = "当前组件已有请求，等待终止事件后再发送。";
			return false;
		}
		try {
			String verb = method == null ? "" : method.trim().toUpperCase(Locale.US);
			HttpTransport.validate(verb, address);
			if (pendingBody != null && ("GET".equals(verb) || "HEAD".equals(verb))) {
				throw new IOException("GET 和 HEAD 请求不能配置提交正文。");
			}
			final Session session = new Session(verb, address);
			current = session;
			pendingBody = null;
			lastError = "";
			Thread worker = new Thread(session, "Simple-HTTP");
			worker.setDaemon(true);
			worker.start();
			return true;
		} catch (IOException error) {
			lastError = message(error);
			return false;
		}
	}

	@Override
	public boolean 取消请求() {
		requireActive();
		Session session = current;
		if (session == null || !session.terminated.compareAndSet(false, true)) {
			return false;
		}
		session.cancelled = true;
		abort(session);
		deliver(session, null, "请求已取消。");
		return true;
	}

	@Override
	public String 取响应头(String name) {
		HttpTransport.Result snapshot = result;
		return snapshot == null ? "" : snapshot.header(name);
	}

	@Override
	public void 销毁() {
		requireMain();
		dispose(true);
		super.销毁();
	}

	private void requireMain() {
		if (Looper.myLooper() != Looper.getMainLooper()) {
			throw new IllegalStateException("HTTP客户端的配置、发送和取消方法应在主线程调用。");
		}
	}

	private void requireActive() {
		requireMain();
		if (destroyed) {
			throw new IllegalStateException("HTTP客户端已经销毁。");
		}
	}

	private void dispose(boolean unregister) {
		if (destroyed) {
			return;
		}
		destroyed = true;
		Session session = current;
		current = null;
		pendingBody = null;
		headers.clear();
		result = null;
		if (session != null) {
			session.terminated.set(true);
			abort(session);
		}
		if (unregister) {
			activity.removeOnDestroyListener(destroyListener);
		}
		EventDispatcher.unregisterEvent(this);
	}

	private void abort(final Session session) {
		session.control.markCancelled();
		Thread closer = new Thread(new Runnable() {
			@Override
			public void run() {
				session.control.abort();
			}
		}, "Simple-HTTP-cancel");
		closer.setDaemon(true);
		closer.start();
	}

	private void deliver(final Session session, final HttpTransport.Result response, final String error) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (destroyed || current != session) {
					return;
				}
				result = response;
				lastError = error;
				// 先释放当前任务，再通知事件；事件中可立即开始下一任务。
				current = null;
				if (session.cancelled) {
					请求取消();
				} else if (response != null) {
					请求完成(response.status());
				} else {
					请求失败(error);
				}
			}
		});
	}

	private static String message(Exception error) {
		return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
	}

	private static String checkedName(String name) {
		if (name == null || name.length() == 0) {
			throw new IllegalArgumentException("文件字段名不能为空。");
		}
		return HttpTransport.token(name);
	}

	private static Map<String, String> copyFields(哈希表 fields) {
		if (fields == null) {
			throw new IllegalArgumentException("表单参数不能是空引用。");
		}
		Map<String, String> copy = new LinkedHashMap<String, String>();
		for (Map.Entry<String, Variant> entry : fields.getMap().entrySet()) {
			copy.put(HttpTransport.token(entry.getKey()), entry.getValue().getString());
		}
		return copy;
	}

	/**
	 * 主线程准备的不可变正文配置，元数据查询和文件读取延迟到工作线程。
	 */
	private static final class PendingBody {

		final byte[] bytes;
		final String type;
		final String address;
		final String field;
		final Map<String, String> fields;

		PendingBody(byte[] bytes, String type) {
			this.bytes = bytes;
			this.type = type;
			address = null;
			field = null;
			fields = null;
		}

		PendingBody(String address, String field, Map<String, String> fields) {
			if (address == null || address.trim().length() == 0) {
				throw new IllegalArgumentException("文件地址不能为空。");
			}
			this.address = address;
			this.field = field;
			this.fields = fields;
			bytes = null;
			type = null;
		}
	}

	/**
	 * 一次已受理请求的参数及终止状态，工作线程不写组件的响应属性。
	 */
	private final class Session implements Runnable, HttpTransport.Progress {

		final String method;
		final String address;
		final Map<String, String> requestHeaders = new LinkedHashMap<String, String>(headers);
		final PendingBody body = pendingBody;
		final int connect = connectTimeout;
		final int read = readTimeout;
		final int limit = responseLimit;
		final HttpTransport.Control control = new HttpTransport.Control();
		final AtomicBoolean terminated = new AtomicBoolean();
		final AtomicBoolean sendPosted = new AtomicBoolean();
		final AtomicBoolean receivePosted = new AtomicBoolean();
		volatile boolean cancelled;
		volatile long sent;
		volatile long sendTotal;
		volatile long received;
		volatile long receiveTotal;
		long lastSend;
		long lastReceive;

		Session(String method, String address) {
			this.method = method;
			this.address = address;
		}

		@Override
		public void run() {
			HttpTransport.Result response = null;
			String error = "";
			try {
				response = HttpTransport.execute(method, address, requestHeaders, prepareBody(body),
					connect, read, limit, control, this);
			} catch (Exception failure) {
				error = message(failure);
			}
			if (terminated.compareAndSet(false, true)) {
				deliver(this, response, error);
			}
		}

		@Override
		public void update(final boolean upload, long count, long total) {
			if (upload) {
				sent = count;
				sendTotal = total;
			} else {
				received = count;
				receiveTotal = total;
			}
			long now = System.nanoTime();
			long last = upload ? lastSend : lastReceive;
			if (now - last < 200000000L && (total < 0 || count != total)) {
				return;
			}
			if (upload) {
				lastSend = now;
			} else {
				lastReceive = now;
			}
			final AtomicBoolean posted = upload ? sendPosted : receivePosted;
			if (!posted.compareAndSet(false, true)) {
				return;
			}
			handler.post(new Runnable() {
				@Override
				public void run() {
					posted.set(false);
					if (destroyed || current != Session.this || cancelled) {
						return;
					}
					if (upload) {
						发送进度(sent, sendTotal);
					} else {
						接收进度(received, receiveTotal);
					}
				}
			});
		}
	}

	private HttpTransport.Body prepareBody(PendingBody body) throws IOException {
		if (body == null) {
			return null;
		}
		if (body.bytes != null) {
			return HttpTransport.bytes(body.bytes, body.type);
		}
		final Uri uri;
		final File file;
		String filename;
		String type;
		long length;
		if (body.address.startsWith("content://")) {
			uri = Uri.parse(body.address);
			file = null;
			filename = "upload.bin";
			length = -1;
			Cursor cursor = activity.getContentResolver().query(uri,
				new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null);
			try {
				if (cursor != null && cursor.moveToFirst()) {
					int nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
					int sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE);
					if (nameColumn >= 0 && !cursor.isNull(nameColumn)) {
						String displayName = cursor.getString(nameColumn);
						if (displayName != null && displayName.length() > 0) {
							filename = displayName;
						}
					}
					if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) {
						length = Math.max(-1, cursor.getLong(sizeColumn));
					}
				}
			} finally {
				if (cursor != null) {
					cursor.close();
				}
			}
			type = activity.getContentResolver().getType(uri);
		} else {
			if (body.address.contains("://")) {
				throw new IOException("文件来源仅支持本地路径和 content URI。");
			}
			uri = null;
			File target = new File(body.address);
			if (!target.isAbsolute()) {
				File base = activity.getFilesDir().getCanonicalFile();
				target = new File(base, body.address).getCanonicalFile();
				if (!target.getPath().startsWith(base.getPath() + File.separator)) {
					throw new IOException("相对路径不能越出应用内部文件目录。");
				}
			}
			file = target.getCanonicalFile();
			if (!file.isFile()) {
				throw new IOException("提交文件不存在或不是普通文件。");
			}
			filename = file.getName();
			length = file.length();
			type = URLConnection.guessContentTypeFromName(filename);
		}
		if (type == null || type.length() == 0) {
			type = "application/octet-stream";
		}
		HttpTransport.token(type);
		HttpTransport.Source source = new HttpTransport.Source() {
			@Override
			public InputStream open() throws IOException {
				return uri == null ? new FileInputStream(file) : activity.getContentResolver().openInputStream(uri);
			}
		};
		return body.field == null ? HttpTransport.file(source, length, type)
			: HttpTransport.multipart(source, length, body.field, filename, type, body.fields);
	}
}

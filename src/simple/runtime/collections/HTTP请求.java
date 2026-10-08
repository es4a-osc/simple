package simple.runtime.collections;

import simple.runtime.日志输出;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.UsesPermissions;
import simple.runtime.variants.Variant;

import java.net.URL;
import java.net.URLEncoder;
import java.net.Proxy;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * HTTP请求对象。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
@UsesPermissions(permissionNames =
		// 允许程序打开网络套接字
		"android.permission.INTERNET")
public final class HTTP请求 {
	/*
	java.net：
	https://developer.android.google.cn/reference/java/net/package-summary
	*/

	// 代理
	private Proxy proxy = Proxy.NO_PROXY;
	// 连接
	private HttpURLConnection connection;
	// 提交数据
	private byte[] postData;
	// 响应字节集
	private byte[] responseData;

	/**
	 * 身份验证类
	 *
	 * <p>这样这个类是个静态内部类
	 */
	private static class MyAuth extends Authenticator {
		private String user;
		private String password;

		public MyAuth(String user, String password) {
			this.user = user;
			this.password = password;
		}

		@Override
		protected PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(user, password.toCharArray());
		}
	}

	/**
	 * 设置代理IP地址，在{@link HTTP请求#打开(String)}前设置生效。
	 *
	 * <p>信息将一直有效或直到{@link HTTP请求#取消代理()}
	 *
	 * @param hostname IP地址
	 * @param port 端口号
	 */
	@SimpleFunction
	public void 置代理地址(String hostname, int port) {
		proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(hostname, port));
	}

	/**
	 * 设置代理用户名和密码，在{@link HTTP请求#打开(String)}前设置生效。
	 *
	 * <p>信息将一直有效或直到{@link HTTP请求#取消代理()}
	 *
	 * @param user 用户名
	 * @param password 密码
	 */
	@SimpleFunction
	public void 置代理用户(String user, String password) {
		Authenticator.setDefault(new MyAuth(user, password));
	}

	/**
	 * 取消当前已设置代理地址和用户。
	 */
	@SimpleFunction
	public void 取消代理() {
		proxy = Proxy.NO_PROXY;
		Authenticator.setDefault(null);
	}

	/**
	 * 打开一个请求地址。
	 *
	 * @param requestUrl 请求地址
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 打开(String requestUrl) {
		// 重置
		postData = null;
		responseData = null;

		// 创建HTTP连接
		try {
			// URL对象
			URL url = new URL(requestUrl);
			// 使用URL打开一个链接，注意这里还未建立连接
			connection = (HttpURLConnection) url.openConnection(proxy);
			return true;
		} catch (MalformedURLException e) {
			// 如果没有指定协议，或者发现未知协议，或者spec为null。
			日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/HTTP请求.打开] 未知协议；请求地址=" + requestUrl);
		} catch (UnsupportedOperationException e) {
			// 如果实现该协议处理程序的子类不支持此方法。
			日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/HTTP请求.打开] 子类不支持；请求地址=" + requestUrl);
		} catch (IllegalArgumentException e) {
			// 如果代理为 null，或代理的类型错误
			日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/HTTP请求.打开] 代理为空");
		} catch (SecurityException e) {
			// 如果有安全管理器并且调用方没有连接到代理的权限。
			日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/HTTP请求.打开] 安全异常" );
		} catch (IOException e) {
			// 如果发生 I/O 异常。
			日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/HTTP请求.打开] I/O异常");
		}
		return false;
	}

	/**
	 * 返回连接超时设置。
	 *
	 * @return 连接超时毫秒，0 为无限制
	 */
	@SimpleProperty
	public int 连接超时() {
		return connection != null ? connection.getConnectTimeout() : -1; // 暂时不清楚默认值
	}

	/**
	 * 设置连接超时毫秒，在{@link HTTP请求#发送()}前设置生效。
	 *
	 * @param timeout 连接超时毫秒，0 为无限制
	 */
	@SimpleProperty
	public void 连接超时(int timeout) {
		if (connection != null && timeout >= 0) connection.setConnectTimeout(timeout);
	}

	/**
	 * 返回读入超时毫秒。
	 *
	 * @return 连接超时毫秒，0 为无限制
	 */
	@SimpleProperty
	public int 读入超时() {
		return connection != null ? connection.getReadTimeout() : -1; // 暂时不清楚默认值
	}

	/**
	 * 设置读入超时毫秒，在{@link HTTP请求#发送()}前设置生效。
	 *
	 * @param timeout 读入超时毫秒，0 为无限制
	 */
	@SimpleProperty
	public void 读入超时(int timeout) {
		if (connection != null && timeout >= 0) connection.setReadTimeout(timeout);
	}

	/**
	 * 返回当前请求地址。
	 *
	 * @return 请求地址
	 */
	@SimpleProperty
	public String 请求网址() {
		return connection != null ? connection.toString() : "";
	}

	/**
	 * 返回请求方法。
	 *
	 * @return 请求方法
	 */
	@SimpleProperty
	public String 请求方式() {
		return connection != null ? connection.getRequestMethod() : "GET";
	}

	/**
	 * 设置请求方法，在{@link HTTP请求#发送()}前设置生效。
	 *
	 * @param method 请求方法，可选值：GET POST HEAD OPTIONS PUT DELETE TRACE
	 */
	@SimpleProperty
	public void 请求方式(String method) {
		if (connection != null) {
			try {
				connection.setRequestMethod(method);
			} catch (ProtocolException e) {
				// 如果无法重置方法或者请求的方法对 HTTP 无效。
				日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/HTTP请求.请求方式] 无法设置请求方式；请求方式=" + method);
			}
		}
	}

	/**
	 * 返回是否自动执行重定向（响应代码为 3xx 的请求）。
	 *
	 * @return 自动返回{@code true}，否则返回{@code false}
	 */
	@SimpleProperty
	public boolean 自动重定向() {
		return connection != null ? connection.getInstanceFollowRedirects() : true;
	}

	/**
	 * 设置是否自动执行重定向（响应代码为 3xx 的请求），在{@link HTTP请求#发送()}前设置生效。
	 *
	 * @param followRedirects 自动返回{@code true}，否则返回{@code false}
	 */
	@SimpleProperty
	public void 自动重定向(boolean followRedirects) {
		if (connection != null) connection.setInstanceFollowRedirects(followRedirects);
	}

	/**
	 * 返回是否允许连接使用任何可用的缓存。
	 *
	 * @return 允许返回{@code true}，否则返回{@code false}
	 */
	@SimpleProperty
	public boolean 允许缓存() {
		return connection != null ? connection.getUseCaches() : true;
	}

	/**
	 * 设置是否允许连接使用任何可用的缓存，在{@link HTTP请求#发送()}前设置生效。
	 *
	 * @param usecaches 允许返回{@code true}，否则返回{@code false}
	 */
	@SimpleProperty
	public void 允许缓存(boolean usecaches) {
		if (connection != null) {
			try {
				connection.setUseCaches(usecaches);
			} catch (IllegalStateException e) {
				// 如果已连接
				日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/HTTP请求.允许缓存] 已连接到服务器");
			}
		}
	}

	/**
	 * 设置请求协议头，在{@link HTTP请求#发送()}前设置生效。
	 *
	 * @param key
	 * @param value
	 * @return
	 */
	@SimpleFunction
	public boolean 置请求头(String key, String value) {
		if (connection != null) {
			try {
				connection.setRequestProperty(key, value);
				return true;
			} catch (IllegalStateException e) {
				// 如果已连接
				日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/HTTP请求.置请求头] 已经连接到服务" );
			} catch (NullPointerException e) {
				// 如果键为null
				日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/HTTP请求.置请求头] 协议头为空");
			}
		}
		return false;
	}

	/**
	 * 返回指定请求头名值。
	 *
	 * <p>将强制传为POST方式
	 *
	 * @param key 请求头名
	 * @return 成功返回请求头值，否则返回空文本
	 */
	@SimpleFunction
	public String 取请求头(String key) {
		if (connection != null) {
			try {
				return connection.getRequestProperty(key);
			} catch (IllegalStateException e) {
				// 如果已连接
				日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/HTTP请求.取请求头] 已连接");
			}
		}
		return "";
	}

	/**
	 * 设置提交哈希表，在{@link HTTP请求#发送()}前设置生效。
	 *
	 * <p>将强制传为POST方式
	 *
	 * @param data 哈希表
	 * @param enc  编码方式（如：utf-8），{@code null}或空文本将不编码
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 置提交哈希表(哈希表 data, String enc) {
		// 将哈希表转为查询参数
		StringBuilder sb = new StringBuilder();
		Map<String, Variant> map = data.getMap();
		for (String key : map.keySet()) {
			String value = map.get(key).getString();
			// 编码键值对
			if(enc != null && enc.length() > 0) {
				try {
					key = URLEncoder.encode(key, enc);
					value = URLEncoder.encode(value, enc);
				} catch (UnsupportedEncodingException e) {
					日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/HTTP请求.置提交哈希表] 编码转换异常");
					return false;
				}
			}
			// 拼接键值对
			if (sb.length() > 0) sb.append("&");
			sb.append(key + "=" + value);
		}
		return 置提交文本(sb.toString());
	}

	@SimpleFunction
	public boolean 置提交文本(String data) {
		return 置提交数据(data.getBytes());
	}

	/**
	 * 设置提交数据，在{@link HTTP请求#发送()}前设置生效。
	 *
	 * <p>将强制传为POST方式
	 *
	 * @param data 提交数据
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 置提交数据(byte[] data) {
		if (connection == null) {
			日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/HTTP请求.置提交数据] 未执行前置方法");
			return false;
		}

		// 自动将GET转为POST
		if (connection.getRequestMethod().equalsIgnoreCase("POST") == false) {
			try {
				connection.setRequestMethod("POST");
			} catch (ProtocolException e) {
				// 如果无法重置方法或者请求的方法对 HTTP 无效。
				日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/HTTP请求.置提交数据] 无法重置请求方式");
				return false;
			}
		}

		// 保存POST数据
		postData = data;
		return true;
	}

	/**
	 * 与服务器建立连接，并发送和获取数据。
	 *
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 发送() {
		if (connection == null) {
			日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/HTTP请求.发送] 未执行前置方法");
			return false;
		}

		// 连接服务器
		try {
			// 打开到此 URL 引用的资源的通信链接（如果尚未建立这样的连接）。
			connection.connect();
		} catch (SocketTimeoutException e) {
			// 如果在建立连接之前超时期满
			日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/HTTP请求.发送] 连接已超时");
			return false;
		} catch (IOException e) {
			// 如果打开连接时发生 I/O 错误。
			日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/HTTP请求.发送] 连接发生 I/O 错误");
			return false;
		}

		// 写提交数据
		if (postData != null && postData.length > 0 && connection.getRequestMethod().equalsIgnoreCase("POST")) {
			try {
				// 将字节集写入输出流
				OutputStream outputStream = connection.getOutputStream();
				DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
				dataOutputStream.write(postData);
				dataOutputStream.close();
				// 释放提交数据
				postData = null;
			} catch (IOException e) {
				connection.disconnect();
				日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/HTTP请求.发送] 写入发生 I/O 错误");
				return false;
			}
		}

		// 取响应字节集
		boolean success = true;
		try {
			// 将输入流读出字节集
			InputStream is = connection.getResponseCode() == HttpURLConnection.HTTP_OK ? connection.getInputStream() : connection.getErrorStream();
			if (is != null) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int len = 0;
				while ((len = is.read(buffer)) != -1) {
					baos.write(buffer, 0, len);
				}
				baos.close();
				is.close();
				responseData = baos.toByteArray();
			} else {
				responseData = null;
			}
		} catch (IOException e) {
			success = false;
			日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/HTTP请求.发送] 读出发生 I/O 错误");
		} finally {
			// 指示近期服务器不太可能有其他请求。
			connection.disconnect();
		}
		return success;
	}

	/**
	 * 返回请求响应代码，在{@link HTTP请求#发送()}后生效。
	 *
	 * @return 响应代码
	 */
	@SimpleProperty
	public int 响应代码() {
		if (connection != null) {
			try {
				return connection.getResponseCode();
			} catch (IOException e) {
				// 如果连接到服务器时发生错误。
				日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/HTTP请求.响应代码] 连接到服务器发生错误");
			}
		} else {
			日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/HTTP请求.响应代码] 未执行前置方法");
		}
		return 0;
	}

	/**
	 * 返回请求响应消息（如果有），在{@link HTTP请求#发送()}后生效。
	 *
	 * @return 响应消息
	 */
	@SimpleProperty
	public String 响应消息() {
		if (connection != null) {
			try {
				return connection.getResponseMessage();
			} catch (IOException e) {
				// 如果连接到服务器时发生错误。
				日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/HTTP请求.响应消息] 连接到服务器发生错误");
			}
		}
		return "";
	}

	/**
	 * 返回响应文本，在{@link HTTP请求#发送()}后生效。
	 *
	 * @return 响应文本
	 */
	@SimpleProperty
	public String 响应文本() {
		return (responseData != null && responseData.length > 0) ? new String(responseData) : "";
	}

	/**
	 * 返回响应数据，在{@link HTTP请求#发送()}后生效。
	 *
	 * @return 响应数据
	 */
	@SimpleProperty
	public byte[] 响应数据() {
		return responseData;
	}
}

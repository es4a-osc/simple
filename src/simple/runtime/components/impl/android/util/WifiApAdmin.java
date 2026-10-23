package simple.runtime.components.impl.android.util;

import android.content.Context;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

/**
 * 按系统版本适配指定配置的系统热点和系统生成配置的本地热点。
 *
 * <p>指定配置的接口仍受系统权限限制；本地热点从 API 26 起提供，
 * 不提供互联网共享，资源由当前实例持有和释放。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class WifiApAdmin {

	/**
	 * 接收无参本地热点请求的异步结果。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	public interface Listener {

		/**
		 * 本地热点已就绪，返回系统实际生成的连接信息。
		 *
		 * @param ssid 热点名称
		 * @param password 热点密码；开放热点为空字符串
		 */
		void onStarted(String ssid, String password);

		/**
		 * 本地热点请求失败。
		 *
		 * @param reason 系统错误码；权限异常或不支持时使用 ERROR_GENERIC
		 */
		void onFailed(int reason);
	}

	private final Context context;
	private final WifiManager manager;
	private final Listener listener;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private LocalHotspot localHotspot;
	private boolean localMode;
	private volatile boolean released;

	/**
	 * 获取 Wi-Fi 服务，不改变当前 Wi-Fi 或热点状态。
	 *
	 * @param context Android 上下文，不可为 {@code null}
	 */
	public WifiApAdmin(Context context) {
		this(context, null);
	}

	/**
	 * 获取 Wi-Fi 服务并登记本地热点的结果接收器。
	 *
	 * @param context Android 上下文，不可为 {@code null}
	 * @param listener 本地热点结果接收器，可为 {@code null}
	 */
	public WifiApAdmin(Context context, Listener listener) {
		this.context = context.getApplicationContext();
		manager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
		this.listener = listener;
	}

	/**
	 * 按指定名称和密码请求启动系统热点。
	 *
	 * <p>API 30 起使用 SoftApConfiguration，API 26～29 使用 startSoftAp，
	 * 更旧系统使用 setWifiApEnabled；这些系统接口仍要求对应权限。
	 * 请求被受理不代表热点已启动，不触发本地热点结果回调。
	 *
	 * @param ssid 无线网络名称，UTF-8 长度为 1～32 字节
	 * @param password 8～63 个 ASCII 字符；为 {@code null} 或空字符串时创建开放热点
	 * @return 系统接受请求时为真；受限、不可用或不在主线程时为假
	 */
	public boolean startWifiAp(String ssid, String password) {
		if (Looper.myLooper() != Looper.getMainLooper()) {
			return false;
		}

		if (released || manager == null) {
			Log.e("WIFI管理", "创建热点失败：组件已释放或设备未提供 Wi-Fi 服务");
			return false;
		}
		// 在关闭旧版 Wi-Fi 等副作用之前校验，不能把无效配置提交为开放热点。
		if (ssid == null || ssid.isEmpty() || ssid.getBytes(StandardCharsets.UTF_8).length > 32
				|| (password != null && !password.isEmpty()
						&& (password.length() < 8 || password.length() > 63 || !StandardCharsets.US_ASCII.newEncoder().canEncode(password)))) {
			Log.e("WIFI管理", "创建热点失败：名称须为 1～32 字节，密码须为 8～63 个 ASCII 字符或为空");
			return false;
		}
		if (localHotspot != null && localHotspot.isActive()) {
			Log.e("WIFI管理", "创建热点失败：请先关闭当前实例的本地热点");
			return false;
		}

		try {
			// 保留指定配置语义；新版系统接口负责处理 Wi-Fi 与热点的并发限制。
			boolean accepted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
					? startConfiguredHotspot30(ssid, password)
					: startConfiguredHotspotBefore30(ssid, password);
			if (accepted) {
				localMode = false;
			} else {
				Log.e("WIFI管理", "创建热点失败：系统未接受指定配置的热点请求");
			}
			return accepted;
		} catch (ReflectiveOperationException | RuntimeException error) {
			Log.e("WIFI管理", "创建指定热点失败：系统接口或所需权限不可用", error);
			return false;
		}
	}

	/**
	 * 请求系统生成名称和密码的本地热点。
	 *
	 * <p>从 API 26 起可用，需要定位权限及系统要求的定位服务状态。
	 * 实际名称和密码通过结果接收器返回；该热点不提供互联网共享。
	 * 所有请求和回调在主线程串行处理。
	 */
	public void startWifiAp() {
		if (Looper.myLooper() != Looper.getMainLooper()) {
			handler.post(this::startWifiAp);
			return;
		}

		if (released) {
			Log.e("WIFI管理", "创建本地热点失败：组件已释放");
			return;
		}

		// 即使请求不受支持，也保持本地模式，后续关闭不得误用全局热点接口。
		localMode = true;
		if (manager == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
			Log.e("WIFI管理", "创建本地热点失败：Wi-Fi 服务不可用或系统低于 API 26");
			if (listener != null) {
				listener.onFailed(WifiManager.LocalOnlyHotspotCallback.ERROR_GENERIC);
			}
			return;
		}

		if (localHotspot == null) {
			localHotspot = new LocalHotspot(manager, listener, handler);
		}
		localHotspot.start();
	}

	/**
	 * 关闭当前实例的本地热点，或请求关闭指定配置的系统热点。
	 *
	 * <p>本地热点尚在创建时，标记取消并在资源到达后释放；等待系统回调期间返回假。
	 * 本地模式不调用全局系统热点关闭接口。
	 *
	 * @return 本地资源释放调用已完成或系统受理关闭请求时为真；等待回调、不可用或不在主线程时为假
	 */
	public boolean closeWifiAp() {
		if (released) {
			return false;
		}

		if (Looper.myLooper() != Looper.getMainLooper()) {
			return false;
		}

		if (localMode) {
			return localHotspot == null || localHotspot.close();
		}
		return closeWifiAp(context);
	}

	/**
	 * 释放当前实例持有的本地热点资源并拒绝后续创建请求。
	 *
	 * <p>指定配置的系统热点延续原有行为，不随组件销毁自动关闭。
	 * 释放标记立即生效，已排队但未执行的创建请求不得再访问系统热点接口。
	 */
	public void release() {
		released = true;
		if (Looper.myLooper() != Looper.getMainLooper()) {
			handler.post(this::release);
			return;
		}

		if (localHotspot != null) {
			localHotspot.close();
		}
	}

	/**
	 * 按系统版本请求关闭指定配置的系统热点。
	 *
	 * @param context Android 上下文，不可为 {@code null}
	 * @return 系统接受关闭请求时为真，否则为假
	 */
	public static boolean closeWifiAp(Context context) {
		WifiManager manager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		if (manager == null) {
			Log.e("WIFI管理", "关闭热点失败：设备未提供 Wi-Fi 服务");
			return false;
		}

		try {
			// API 26 起使用新的系统关闭入口，避免继续调用已失效的旧入口。
			boolean accepted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
					? Boolean.TRUE.equals(manager.getClass().getMethod("stopSoftAp").invoke(manager))
					: closeConfiguredHotspotBefore26(manager);
			if (!accepted) {
				Log.e("WIFI管理", "关闭热点失败：系统未接受关闭请求");
			}
			return accepted;
		} catch (ReflectiveOperationException | RuntimeException error) {
			Log.e("WIFI管理", "关闭指定热点失败：系统接口或所需权限不可用", error);
			return false;
		}
	}

	/**
	 * 在 API 30 起通过新版系统配置请求启动热点。
	 *
	 * @param ssid 热点名称
	 * @param password 认证密码；空值表示开放热点
	 * @return 系统是否接受请求
	 * @throws ReflectiveOperationException 系统构建器或热点接口不可用、不可访问或执行失败
	 */
	private boolean startConfiguredHotspot30(String ssid, String password) throws ReflectiveOperationException {
		// 构建器属于 API 30 的系统接口，普通 SDK 不暴露它，按原有系统适配方式反射访问。
		Class<?> configurationClass = Class.forName("android.net.wifi.SoftApConfiguration");
		Class<?> builderClass = Class.forName("android.net.wifi.SoftApConfiguration$Builder");
		Object builder = builderClass.getConstructor().newInstance();
		builderClass.getMethod("setSsid", String.class).invoke(builder, ssid);
		boolean open = password == null || password.isEmpty();
		int security = configurationClass.getField(open ? "SECURITY_TYPE_OPEN" : "SECURITY_TYPE_WPA2_PSK").getInt(null);
		builderClass.getMethod("setPassphrase", String.class, int.class)
				.invoke(builder, open ? null : password, security);
		Object configuration = builderClass.getMethod("build").invoke(builder);
		return Boolean.TRUE.equals(manager.getClass()
				.getMethod("startTetheredHotspot", configurationClass).invoke(manager, configuration));
	}

	/**
	 * 在 API 30 以下按旧配置类型请求启动指定热点。
	 *
	 * @param ssid 热点名称
	 * @param password 认证密码；空值表示开放热点
	 * @return 系统是否接受请求
	 * @throws ReflectiveOperationException 系统热点接口不可用、不可访问或执行失败
	 */
	@SuppressWarnings("deprecation")
	private boolean startConfiguredHotspotBefore30(String ssid, String password) throws ReflectiveOperationException {
		android.net.wifi.WifiConfiguration configuration = new android.net.wifi.WifiConfiguration();
		configuration.SSID = ssid;
		if (password == null || password.isEmpty()) {
			configuration.allowedKeyManagement.set(android.net.wifi.WifiConfiguration.KeyMgmt.NONE);
		} else {
			configuration.preSharedKey = password;
			// 系统热点使用 WPA2_PSK；连接路由器使用的 WPA_PSK 不能替代热点认证类型。
			int security = android.net.wifi.WifiConfiguration.KeyMgmt.class.getField("WPA2_PSK").getInt(null);
			configuration.allowedKeyManagement.set(security);
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			return Boolean.TRUE.equals(manager.getClass()
					.getMethod("startSoftAp", android.net.wifi.WifiConfiguration.class).invoke(manager, configuration));
		}

		// 仅旧入口需要由调用方关闭 Wi-Fi；失败后尝试恢复原先开启的状态。
		Method enable = manager.getClass().getMethod("setWifiApEnabled", android.net.wifi.WifiConfiguration.class, boolean.class);
		boolean wifiEnabled = manager.isWifiEnabled();
		if (wifiEnabled && !manager.setWifiEnabled(false)) {
			Log.e("WIFI管理", "创建热点失败：系统未接受关闭 Wi-Fi 的请求");
			return false;
		}
		boolean accepted = false;
		try {
			accepted = Boolean.TRUE.equals(enable.invoke(manager, configuration, true));
			return accepted;
		} finally {
			if (!accepted && wifiEnabled) {
				try {
					if (!manager.setWifiEnabled(true)) {
						Log.e("WIFI管理", "创建热点失败后，系统未接受恢复 Wi-Fi 的请求");
					}
				} catch (RuntimeException error) {
					Log.e("WIFI管理", "创建热点失败后，恢复 Wi-Fi 时发生异常", error);
				}
			}
		}
	}

	/**
	 * 在 API 26 以下通过旧配置入口关闭热点。
	 *
	 * @param manager Wi-Fi 服务
	 * @return 热点已关闭，或系统是否接受关闭请求
	 * @throws ReflectiveOperationException 系统热点接口不可用、不可访问或执行失败
	 */
	@SuppressWarnings("deprecation")
	private static boolean closeConfiguredHotspotBefore26(WifiManager manager) throws ReflectiveOperationException {
		if (!Boolean.TRUE.equals(manager.getClass().getMethod("isWifiApEnabled").invoke(manager))) {
			return true;
		}
		android.net.wifi.WifiConfiguration configuration = (android.net.wifi.WifiConfiguration) manager.getClass()
				.getMethod("getWifiApConfiguration").invoke(manager);
		return Boolean.TRUE.equals(manager.getClass()
				.getMethod("setWifiApEnabled", android.net.wifi.WifiConfiguration.class, boolean.class).invoke(manager, configuration, false));
	}

	/**
	 * 隔离 API 26 的本地热点资源与回调，并使用请求编号拒绝迟到结果。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	private static final class LocalHotspot {

		private final WifiManager manager;
		private final Listener listener;
		private final Handler handler;
		private WifiManager.LocalOnlyHotspotReservation reservation;
		private long generation;
		private boolean pending;
		private boolean cancelled;

		/**
		 * 创建本地热点会话；调用方保证当前系统不低于 API 26。
		 *
		 * @param manager Wi-Fi 服务
		 * @param listener 请求结果接收器，可为空
		 * @param handler 主线程处理器
		 */
		private LocalHotspot(WifiManager manager, Listener listener, Handler handler) {
			this.manager = manager;
			this.listener = listener;
			this.handler = handler;
		}

		/**
		 * 判断本会话是否存在尚未结束的热点请求或已取得的资源。
		 *
		 * @return 存在请求或资源时为 {@code true}
		 */
		private boolean isActive() {
			return pending || reservation != null;
		}

		/**
		 * 提交一次本地热点请求，重复调用不覆盖正在进行的请求。
		 */
		private void start() {
			if (isActive()) {
				Log.e("WIFI管理", "本地热点请求正在处理或热点已经创建，请先关闭后重试");
				return;
			}

			final long request = ++generation;
			cancelled = false;
			pending = true;
			try {
				manager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {
					/**
					 * 保存当前请求的资源并返回实际连接信息；过期请求只释放资源。
					 *
					 * @param result 系统返回的热点资源
					 */
					@Override
					public void onStarted(WifiManager.LocalOnlyHotspotReservation result) {
						if (request != generation) {
							closeReservation(result);
							return;
						}

						pending = false;
						reservation = result;
						if (cancelled) {
							close();
							return;
						}
						String[] configuration;
						try {
							configuration = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
									? Api30.configuration(result) : legacyConfiguration(result);
						} catch (RuntimeException error) {
							close();
							Log.e("WIFI管理", "读取本地热点配置失败", error);
							if (listener != null) {
								listener.onFailed(WifiManager.LocalOnlyHotspotCallback.ERROR_GENERIC);
							}
							return;
						}

						if (listener != null) {
							listener.onStarted(configuration[0], configuration[1]);
						}
					}

					/**
					 * 清除系统已停止的当前热点资源引用。
					 */
					@Override
					public void onStopped() {
						if (request == generation) {
							pending = false;
							close();
						}
					}

					/**
					 * 返回当前请求的系统错误，忽略过期请求的失败回调。
					 *
					 * @param reason 系统错误码
					 */
					@Override
					public void onFailed(int reason) {
						if (request == generation) {
							boolean report = !cancelled;
							pending = false;
							close();
							Log.e("WIFI管理", "创建本地热点失败，系统错误码：" + reason);
							if (report && listener != null) {
								listener.onFailed(reason);
							}
						}
					}
				}, handler);
			} catch (RuntimeException error) {
				pending = false;
				close();
				Log.e("WIFI管理", "无法提交本地热点请求，请检查定位权限、定位服务和系统限制", error);
				if (listener != null) {
					listener.onFailed(WifiManager.LocalOnlyHotspotCallback.ERROR_GENERIC);
				}
			}
		}

		/**
		 * 取消当前会话并释放已取得的资源；未完成的系统请求仍占用会话直到回调到达。
		 *
		 * @return 无待完成请求且资源释放调用完成时为真；等待回调或释放异常时为假
		 */
		private boolean close() {
			cancelled = true;
			if (pending) {
				// API 26～30 没有公开的待定请求取消入口，不能提前放行下一次创建。
				return false;
			}
			++generation;
			WifiManager.LocalOnlyHotspotReservation previous = reservation;
			reservation = null;
			boolean closed = closeReservation(previous);
			if (!closed) {
				// 释放失败仍保留所有权，后续可重试关闭，也不能覆盖为新的会话。
				reservation = previous;
			}
			return closed;
		}

		/**
		 * 释放一个本地热点资源，释放异常只记录日志。
		 *
		 * @param reservation 待释放的资源，可为空
		 * @return 释放调用已完成或资源本来为空时为真，调用抛出异常时为假
		 */
		private static boolean closeReservation(WifiManager.LocalOnlyHotspotReservation reservation) {
			if (reservation != null) {
				try {
					reservation.close();
				} catch (RuntimeException error) {
					Log.e("WIFI管理", "释放本地热点资源失败", error);
					return false;
				}
			}
			return true;
		}

		/**
		 * 在 API 26～29 读取系统生成的旧类型连接配置。
		 *
		 * @param reservation 已启动热点的资源
		 * @return 名称和密码；开放热点的密码为空字符串
		 * @throws IllegalStateException 系统没有提供可用配置
		 */
		@SuppressWarnings("deprecation")
		private static String[] legacyConfiguration(WifiManager.LocalOnlyHotspotReservation reservation) {
			android.net.wifi.WifiConfiguration configuration = reservation.getWifiConfiguration();
			if (configuration == null || configuration.SSID == null) {
				throw new IllegalStateException("系统未返回本地热点配置");
			}
			return new String[] {configuration.SSID,
					configuration.preSharedKey == null ? "" : configuration.preSharedKey};
		}
	}

	/**
	 * 隔离 API 30 的本地热点配置类型。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	private static final class Api30 {

		/**
		 * 静态适配器不创建实例。
		 */
		private Api30() {
		}

		/**
		 * 通过 API 30 的新配置类型取得系统生成的连接信息。
		 *
		 * @param reservation 已启动热点的资源
		 * @return 名称和密码；开放热点的密码为空字符串
		 * @throws IllegalStateException 系统没有提供可用配置
		 */
		private static String[] configuration(WifiManager.LocalOnlyHotspotReservation reservation) {
			SoftApConfiguration configuration = reservation.getSoftApConfiguration();
			if (configuration == null || configuration.getSsid() == null) {
				throw new IllegalStateException("系统未返回本地热点配置");
			}
			return new String[] {configuration.getSsid(),
					configuration.getPassphrase() == null ? "" : configuration.getPassphrase()};
		}
	}
}

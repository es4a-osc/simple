package simple.runtime.components.impl.android;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import simple.runtime.android.MainActivity;
import simple.runtime.components.WIFI管理器;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.components.impl.android.util.WifiApAdmin;
import simple.runtime.events.EventDispatcher;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wi-Fi 网络、扫描与热点管理实现，按系统版本选择通知和资源管理接口。
 *
 * <p>网络配置修改及指定配置热点受系统权限限制；异步结果在主线程分发。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class WIFI管理器Impl extends 组件Impl implements WIFI管理器, MainActivity.OnActivityResultListener {

	// 位于蓝牙、相机、语音和文件选择器的请求区间之后；每次请求使用新编号，隔离迟到结果。
	private static final AtomicInteger NEXT_NETWORK_REQUEST = new AtomicInteger(0x8000);
	private final MainActivity activity;
	private final Context context;
	private final WifiManager manager;
	private final BroadcastReceiver receiver;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private NetworkObserver networkObserver;
	private Runnable unregisterScanCallback;
	private WifiManager.WifiLock wifiLock;
	private WifiApAdmin hotspot;
	private boolean scanPending;
	private int wifiState = WifiManager.WIFI_STATE_UNKNOWN;
	private int legacyConnectionState;
	private boolean receiverRegistered;
	private boolean resultListenerRegistered;
	private int pendingNetworkRequest;
	private String pendingNetworkSsid;
	private volatile boolean destroyed;

	/**
	 * 初始化 Wi-Fi 服务，并注册与系统版本对应的状态和扫描通知。
	 *
	 * @param container 所属组件容器
	 */
	public WIFI管理器Impl(组件容器 container) {
		super(container);

		activity = MainActivity.getContext();
		context = activity.getApplicationContext();
		manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		receiver = new BroadcastReceiver() {

			/**
			 * 处理 Wi-Fi 开关、旧版连接状态及扫描失败通知。
			 *
			 * @param context 接收广播的上下文
			 * @param intent 系统发送的状态信息
			 */
			@Override
			public void onReceive(Context context, Intent intent) {
				if (destroyed) {
					return;
				}

				String action = intent.getAction();
				if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
					boolean updated = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
					if (unregisterScanCallback == null || !updated) {
						// API 30 的成功结果由回调通知；各版本的失败通知都结束等待并返回假。
						completeScan(updated);
					}
				} else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
					// 只报告开启、关闭这两个稳定状态；初始粘性广播不作为一次变化。
					int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
					if (state == WifiManager.WIFI_STATE_ENABLED || state == WifiManager.WIFI_STATE_DISABLED) {
						boolean changed = state != wifiState;
						wifiState = state;
						if (!isInitialStickyBroadcast() && changed) {
							开启状态改变(state == WifiManager.WIFI_STATE_ENABLED);
						}
					}
				} else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
					if (networkObserver == null || !networkObserver.registered) {
						legacyNetworkState(intent, isInitialStickyBroadcast());
					}
				} else {
					legacyAuthenticationError(intent);
				}
			}
		};

		// 接收器随组件存活；保存注册状态，以便销毁时准确注销。
		if (manager != null) {
			IntentFilter filter = new IntentFilter();
			filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
			filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
			// 新版连接回调注册失败时仍可接收旧广播，避免整个连接事件入口失效。
			filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
			addLegacyAuthenticationAction(filter);

			try {
				context.registerReceiver(receiver, filter);
				receiverRegistered = true;
			} catch (RuntimeException error) {
				Log.e("WIFI管理", "无法注册 Wi-Fi 广播", error);
			}

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				networkObserver = new NetworkObserver();
				networkObserver.register();
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				try {
					// 外层只保存稳定类型，避免旧系统解析字段及合成访问方法时加载新回调类。
					unregisterScanCallback = Api30.registerScanCallback(manager, context, new Runnable() {
						@Override
						public void run() {
							completeScan(true);
						}
					});
				} catch (RuntimeException error) {
					Log.e("WIFI管理", "无法注册 Wi-Fi 扫描通知，改用广播", error);
				}
			}
		}
	}

	/* 组件事件 */

	/**
	 * 分发本组件正在等待的扫描通知，明确结果是否已更新。
	 *
	 * @param updated 扫描是否成功更新结果
	 */
	@Override
	public void 扫描完毕(boolean updated) {
		EventDispatcher.dispatchEvent(this, "扫描完毕", updated);
	}

	/**
	 * 分发 Wi-Fi 开关的实际状态变化。
	 *
	 * @param enabled Wi-Fi 是否已开启
	 */
	@Override
	public void 开启状态改变(boolean enabled) {
		EventDispatcher.dispatchEvent(this, "开启状态改变", enabled);
	}

	/**
	 * 分发连接状态；新版回调只提供已连接、断开和暂停状态。
	 *
	 * @param state 组件约定的状态编号
	 */
	@Override
	public void 网络状态改变(int state) {
		EventDispatcher.dispatchEvent(this, "网络状态改变", state);
	}

	/**
	 * 分发系统旧版认证广播报告的错误，新系统不保证提供该广播。
	 */
	@Override
	public void 密码错误() {
		EventDispatcher.dispatchEvent(this, "密码错误");
	}

	/**
	 * 分发保存网络的最终结果，不将状态码解释为系统网络标识。
	 *
	 * @param ssid 本次请求的网络名称
	 * @param result 0 成功，1 失败，2 已存在，-1 用户取消
	 */
	@Override
	public void 网络添加结果(String ssid, int result) {
		EventDispatcher.dispatchEvent(this, "网络添加结果", ssid, result);
	}

	/**
	 * 分发本地热点创建成功事件及系统生成的连接信息。
	 *
	 * @param ssid 实际热点名称
	 * @param password 实际热点密码；开放热点为空字符串
	 */
	@Override
	public void 本地热点创建成功(String ssid, String password) {
		EventDispatcher.dispatchEvent(this, "本地热点创建成功", ssid, password);
	}

	/**
	 * 分发本地热点请求的失败结果。
	 *
	 * @param reason 系统错误码，非系统回调的提交异常使用通用错误码 2
	 */
	@Override
	public void 本地热点创建失败(int reason) {
		EventDispatcher.dispatchEvent(this, "本地热点创建失败", reason);
	}

	/* 组件方法 */

	/**
	 * 返回系统允许读取的已保存网络，维持名称和系统标识的格式。
	 *
	 * <p>API 30 没有公开的完整列表替代接口；普通应用只能读取系统授权范围内的配置。
	 *
	 * @return 名称与标识列表；没有可读配置时为空数组
	 */
	@Override
	@SuppressWarnings("deprecation")
	public String[] 取已保存网络列表() {
		List<String> result = new ArrayList<String>();

		for (android.net.wifi.WifiConfiguration network : savedNetworks()) {
			result.add(unquote(network.SSID) + "|*|" + network.networkId);
		}

		return result.toArray(new String[result.size()]);
	}

	/**
	 * 在系统允许读取的已保存配置中查找网络标识。
	 *
	 * <p>使用旧配置类型是为了兼容低版本及新版系统的获准管理场景。
	 *
	 * @param ssid 网络名称
	 * @return 系统网络标识；未找到或不可读取时为 -1
	 */
	@Override
	@SuppressWarnings("deprecation")
	public int 取已保存网络标识(String ssid) {
		android.net.wifi.WifiConfiguration network = findNetwork(ssid);
		return network == null ? -1 : network.networkId;
	}

	/**
	 * 打开系统 Wi-Fi 控制界面；API 29 起使用无需离开应用的设置面板。
	 */
	@Override
	public void 打开WIFI设置() {
		if (destroyed) {
			return;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			try {
				activity.startActivity(new Intent(Settings.Panel.ACTION_WIFI));
				return;
			} catch (ActivityNotFoundException error) {
				// 部分系统未提供面板，继续打开完整设置页。
			}
		}
		activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
	}

	/**
	 * 请求系统直接切换 Wi-Fi，返回真实的受理结果。
	 *
	 * <p>API 29 起普通应用无法直接切换；公开替代流程由“打开WIFI设置”提供，不能把打开面板当成切换成功。
	 *
	 * @param enabled 是否开启
	 * @return 系统接受切换请求时为真，被限制或组件不可用时为假
	 */
	@Override
	public boolean 置WIFI状态(boolean enabled) {
		return !destroyed && manager != null && legacySetWifiEnabled(enabled);
	}

	/**
	 * 返回系统当前的 Wi-Fi 开启状态。
	 *
	 * @return Wi-Fi 服务存在且已开启时为真
	 */
	@Override
	public boolean 是否已开启() {
		return manager != null && manager.isWifiEnabled();
	}

	/**
	 * 请求写入包含指定网络前缀的完整静态 IPv4 配置。
	 *
	 * <p>使用 API 24 起的静态配置结构；隐藏接口或网络修改权限被限制时记录失败。
	 *
	 * @param ssid 已保存网络名称
	 * @param ip IPv4 地址
	 * @param prefixLength IPv4 网络前缀长度，范围为 0～32
	 * @param gateway IPv4 网关
	 * @param DNS IPv4 DNS 地址
	 * @return 系统接受完整配置时为真；不可读取、不可修改或参数无效时为假
	 */
	@Override
	public boolean 置静态IP(String ssid, String ip, int prefixLength, String gateway, String DNS) {
		if (destroyed || manager == null || ssid == null || prefixLength < 0 || prefixLength > 32) {
			return false;
		}

		try {
			@SuppressWarnings("deprecation")
			android.net.wifi.WifiConfiguration config = findNetwork(ssid);
			if (config == null) {
				return false;
			}

			// 完整构造配置后才提交，避免继续访问早已移除的 linkProperties 字段。
			applyStaticIp(config, ip, prefixLength, gateway, DNS);
			if (updateNetwork(config) >= 0) {
				return true;
			}
			Log.e("WIFI管理", "系统拒绝更新静态 IP 配置");
		} catch (ReflectiveOperationException | IOException | RuntimeException error) {
			Log.e("WIFI管理", "当前系统无法设置静态 IP", error);
		}
		return false;
	}

	/**
	 * 按系统网络标识连接已保存网络，保持禁用其它网络的约定。
	 *
	 * <p>新连接请求接口不接受 networkId，不能等价替换；新版系统是否允许操作由管理权限决定。
	 *
	 * @param networkId 系统网络标识
	 * @param disableOthers 是否同时禁用其它已保存网络
	 * @return 系统接受请求时为真，否则为假
	 */
	@Override
	public boolean 置指定WIFI状态(int networkId, boolean disableOthers) {
		return !destroyed && manager != null && legacyEnableNetwork(networkId, disableOthers);
	}

	/**
	 * 返回指定网络的连接状态；新版先读取当前连接，避免依赖受限的配置列表。
	 *
	 * @param networkId 系统网络标识，不是列表下标
	 * @return 0 当前连接，1 禁用、不存在或不可读取，2 启用
	 */
	@Override
	public int 取连接状态(int networkId) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && networkId >= 0) {
			WifiInfo info = connectionInfo();
			if (info != null && info.getNetworkId() == networkId
					&& info.getSupplicantState() == SupplicantState.COMPLETED) {
				return 0;
			}
		}

		return legacyNetworkStatus(networkId);
	}

	/**
	 * 请求扫描周围网络；API 30 以内仍需使用 startScan 主动发起扫描。
	 *
	 * @return 系统接受扫描请求时为真；不可用、节流或已有请求时为假
	 */
	@Override
	public synchronized boolean 开始扫描() {
		if (destroyed || manager == null || scanPending
				|| (!receiverRegistered && unregisterScanCallback == null)) {
			return false;
		}

		// 仅成功发起的扫描等待广播；失败或异常必须清除状态，允许再次尝试。
		scanPending = true;

		try {
			if (startScan()) {
				return true;
			}

			scanPending = false;
			return false;
		} catch (SecurityException error) {
			scanPending = false;
			Log.e("WIFI管理", "系统未授权 Wi-Fi 扫描", error);
			return false;
		} catch (RuntimeException error) {
			scanPending = false;
			throw error;
		}
	}

	/**
	 * 序列化当前可读取的扫描结果，未完成新扫描时也可能返回系统缓存。
	 *
	 * @return 按“名称*地址*加密信息*信号*频率”排列、以 / 分隔的结果；无结果时为空字符串
	 */
	@Override
	public String 获取扫描结果() {
		List<ScanResult> results;
		try {
			results = destroyed || manager == null ? null : manager.getScanResults();
		} catch (SecurityException error) {
			Log.e("WIFI管理", "系统未授权读取 Wi-Fi 扫描结果", error);
			return "";
		}
		if (results == null) {
			return "";
		}

		StringBuilder text = new StringBuilder();

		for (ScanResult result : results) {
			if (text.length() > 0) {
				text.append('/');
			}

			text.append(result.SSID)
					.append('*')
					.append(result.BSSID)
					.append('*')
					.append(result.capabilities)
					.append('*')
					.append(result.level)
					.append('*')
					.append(result.frequency);
		}

		return text.toString();
	}

	/**
	 * 按系统版本实际调用新旧保存接口，并统一通过事件返回最终结果。
	 *
	 * @param ssid 网络名称
	 * @param password 网络密码
	 * @param type 1 开放，2 WEP，3 WPA；API 30 起类型 3 使用 WPA2
	 * @return 请求已进入主线程队列时为真；未受理时为假且不触发事件
	 */
	@Override
	public boolean 添加网络(final String ssid, final String password, final int type) {
		if (destroyed || manager == null || pendingNetworkRequest != 0
				|| Looper.myLooper() != handler.getLooper() || activity.isFinishing() || activity.isDestroyed()
				|| ssid == null || ssid.isEmpty() || type < 1 || type > 3 || (type != 1 && password == null)) {
			return false;
		}

		// 新系统直接构造公开接口的配置；不把不支持的认证方式改成其它类型。
		final Intent intent;
		final int request;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			if (type == 2) {
				return false;
			}
			try {
				intent = Api30.addNetworkIntent(ssid, password, type);
			} catch (IllegalArgumentException error) {
				return false;
			}
			request = NEXT_NETWORK_REQUEST.getAndIncrement();
			if (request > 0xffff || request < 0x8000) {
				return false;
			}
		} else {
			intent = null;
			request = -1;
		}

		pendingNetworkRequest = request;
		pendingNetworkSsid = ssid;
		// 延后注册监听器，避免在 Activity 正遍历结果监听列表时改变该列表。
		boolean accepted = handler.post(new Runnable() {

			/**
			 * API 30 启动系统确认；低版本实际调用旧添加 API，再转换结果状态。
			 */
			@Override
			public void run() {
				if (destroyed || pendingNetworkRequest != request) {
					return;
				}
				if (activity.isFinishing() || activity.isDestroyed()) {
					completeNetworkRequest(request, 1);
					return;
				}
				int result = 1;
				try {
					if (intent != null) {
						if (!resultListenerRegistered) {
							activity.addOnActivityResultListener(WIFI管理器Impl.this);
							resultListenerRegistered = true;
						}
						activity.startActivityForResult(intent, request);
						return;
					}

					// 低版本缺少系统确认 API，必须调用旧接口，不能只返回固定失败值。
					if (取已保存网络标识(ssid) >= 0) {
						result = 2;
					} else {
						result = addLegacyNetwork(ssid, password, type) >= 0 ? 0 : 1;
					}
				} catch (RuntimeException error) {
					Log.e("WIFI管理", "系统未能完成保存网络请求", error);
				}
				completeNetworkRequest(request, result);
			}
		});
		if (!accepted) {
			pendingNetworkRequest = 0;
			pendingNetworkSsid = null;
		}
		return accepted;
	}

	/**
	 * 读取本实例的系统确认结果，忽略其它组件或先前请求的返回。
	 *
	 * @param requestCode 系统请求编号
	 * @param resultCode 用户是否确认
	 * @param data 单个网络的保存结果，取消时可能为空
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (destroyed || requestCode <= 0 || requestCode != pendingNetworkRequest) {
			return;
		}
		int result = 1;
		if (resultCode == Activity.RESULT_CANCELED) {
			result = -1;
		} else if (resultCode == Activity.RESULT_OK && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			result = Api30.networkResult(data);
		}
		completeNetworkRequest(requestCode, result);
	}

	/**
	 * 按系统网络标识删除已保存配置，返回系统的真实结果。
	 *
	 * <p>API 30 没有允许普通应用按 networkId 删除任意配置的替代接口，管理权限仍由系统判定。
	 *
	 * @param networkId 系统网络标识
	 * @return 删除成功时为真，否则为假
	 */
	@Override
	public boolean 删除网络(int networkId) {
		return !destroyed && manager != null && legacyRemoveNetwork(networkId);
	}

	/**
	 * 获取 Wi-Fi 锁；API 29 起改用仍有效的高性能锁模式。
	 */
	@Override
	public synchronized void 锁定WIFI() {
		if (destroyed || manager == null) {
			return;
		}

		if (wifiLock == null) {
			wifiLock = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
					? manager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Simple.WIFI管理")
					: legacyWifiLock();
		}

		if (!wifiLock.isHeld()) {
			wifiLock.acquire();
		}
	}

	/**
	 * 释放本组件持有的 Wi-Fi 锁，重复释放不产生额外操作。
	 */
	@Override
	public synchronized void 解锁WIFI() {
		if (wifiLock != null && wifiLock.isHeld()) {
			wifiLock.release();
		}
	}

	/**
	 * 按指定名称和密码请求系统热点，保留指定配置的语义。
	 *
	 * @param ssid 热点名称
	 * @param password 热点密码；空值表示开放热点
	 * @return 系统接受创建请求时为真；不可用、受限或不在主线程时为假
	 */
	@Override
	public boolean 创建热点(String ssid, String password) {
		if (destroyed || manager == null || Looper.myLooper() != handler.getLooper()) {
			return false;
		}

		WifiApAdmin adapter = hotspot();
		return adapter != null && adapter.startWifiAp(ssid, password);
	}

	/**
	 * 请求系统生成连接信息的本地热点，结果通过事件返回。
	 */
	@Override
	public void 创建本地热点() {
		if (destroyed) {
			return;
		}

		WifiApAdmin adapter = hotspot();
		if (adapter != null) {
			adapter.startWifiAp();
		}
	}

	/**
	 * 关闭当前实例的本地热点，或请求关闭指定配置的系统热点。
	 *
	 * @return 本地资源释放成功或系统接受关闭请求时为真；不可用或不在主线程时为假
	 */
	@Override
	public boolean 关闭热点() {
		if (destroyed || Looper.myLooper() != handler.getLooper()) {
			return false;
		}
		WifiApAdmin adapter = hotspot();
		return adapter != null && adapter.closeWifiAp();
	}

	/**
	 * 返回当前连接的接入点地址，排除系统用于隐私限制的占位地址。
	 *
	 * @return 可读取的 BSSID；缺失或受限时为空字符串
	 */
	@Override
	public String 取当前BSSID() {
		WifiInfo info = connectionInfo();
		return readableMacAddress(info == null ? null : info.getBSSID());
	}

	/**
	 * 返回当前网络名称，未知名称不作为实际 SSID 暴露给调用方。
	 *
	 * @return 去除外围引号的名称；缺失或受限时为空字符串
	 */
	@Override
	public String 取当前SSID() {
		WifiInfo info = connectionInfo();
		String ssid = info == null ? null : info.getSSID();
		return WifiManager.UNKNOWN_SSID.equals(ssid) ? "" : unquote(ssid);
	}

	/**
	 * 返回当前 Wi-Fi 的系统网络标识。
	 *
	 * @return 系统网络标识；没有连接或不可读取时为 -1
	 */
	@Override
	public int 取当前网络标识() {
		WifiInfo info = connectionInfo();
		return info == null ? -1 : info.getNetworkId();
	}

	/**
	 * 返回当前 Wi-Fi 连接的内网 IPv4 地址。
	 *
	 * @return 点分十进制地址；没有地址时为空字符串
	 */
	@Override
	public String 取内网IP() {
		WifiInfo info = connectionInfo();
		int address = info == null ? 0 : info.getIpAddress();
		return address == 0 ? "" : ipv4Address(address);
	}

	/**
	 * 返回系统允许读取的设备 Wi-Fi MAC，排除系统占位地址。
	 *
	 * @return 可读取的 MAC 地址；缺失或受限时为空字符串
	 */
	@Override
	public String 取MAC地址() {
		WifiInfo info = connectionInfo();
		return readableMacAddress(info == null ? null : info.getMacAddress());
	}

	/**
	 * 将当前 Wi-Fi 信号强度转换为组件约定的 0～4 档位。
	 *
	 * @return 0 为最弱，4 为最强；没有连接信息时为 0
	 */
	@Override
	public int 取当前信号强度() {
		WifiInfo info = connectionInfo();
		if (info == null) {
			return 0;
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			// 新接口使用设备的信号阈值，换算后继续返回约定的 0～4 档位。
			int maximum = manager.getMaxSignalLevel();
			return maximum <= 0 ? 0 : (int) (manager.calculateSignalLevel(info.getRssi()) * 4L / maximum);
		}

		return legacySignalLevel(info.getRssi());
	}

	/**
	 * 返回当前连接的链路速度。
	 *
	 * @return 系统报告的速度，单位为 Mbps；没有信息时为 0
	 */
	@Override
	public int 取当前连接速度() {
		WifiInfo info = connectionInfo();
		return info == null ? 0 : info.getLinkSpeed();
	}

	/**
	 * 返回 DHCP 提供的 IPv4 网关地址。
	 *
	 * @return 点分十进制地址；没有 DHCP 信息时为空字符串
	 */
	@Override
	public String 取网关() {
		DhcpInfo info = manager == null ? null : manager.getDhcpInfo();
		return info == null ? "" : ipv4Address(info.gateway);
	}

	/**
	 * 返回 DHCP 提供的 IPv4 子网掩码。
	 *
	 * @return 点分十进制掩码；没有 DHCP 信息时为空字符串
	 */
	@Override
	public String 取子网掩码() {
		DhcpInfo info = manager == null ? null : manager.getDhcpInfo();
		return info == null ? "" : ipv4Address(info.netmask);
	}

	/**
	 * 返回 DHCP 提供的首选 IPv4 DNS 地址。
	 *
	 * @return 点分十进制地址；没有 DHCP 信息时为空字符串
	 */
	@Override
	public String 取DNS1() {
		DhcpInfo info = manager == null ? null : manager.getDhcpInfo();
		return info == null ? "" : ipv4Address(info.dns1);
	}

	/**
	 * 返回 DHCP 提供的备用 IPv4 DNS 地址。
	 *
	 * @return 点分十进制地址；没有 DHCP 信息时为空字符串
	 */
	@Override
	public String 取DNS2() {
		DhcpInfo info = manager == null ? null : manager.getDhcpInfo();
		return info == null ? "" : ipv4Address(info.dns2);
	}

	/**
	 * 停止对外通知，注销广播并释放本实例持有的锁和本地热点资源。
	 */
	@Override
	public void 销毁() {
		if (destroyed) {
			return;
		}
		destroyed = true;
		pendingNetworkRequest = 0;
		pendingNetworkSsid = null;
		scanPending = false;
		handler.post(new Runnable() {

			/**
			 * 在 Activity 的结果遍历结束后移除监听器，避免销毁触发并发修改。
			 */
			@Override
			public void run() {
				if (resultListenerRegistered) {
					activity.removeOnActivityResultListener(WIFI管理器Impl.this);
					resultListenerRegistered = false;
				}
			}
		});
		if (networkObserver != null) {
			networkObserver.unregister();
			networkObserver = null;
		}
		if (unregisterScanCallback != null) {
			Runnable unregister = unregisterScanCallback;
			unregisterScanCallback = null;
			try {
				unregister.run();
			} catch (RuntimeException error) {
				Log.e("WIFI管理", "无法注销 Wi-Fi 扫描通知", error);
			}
		}

		// 先使本地热点请求失效，迟到回调不得重新持有资源或分发结果。
		WifiApAdmin adapter;
		synchronized (this) {
			adapter = hotspot;
			hotspot = null;
		}
		if (adapter != null) {
			adapter.release();
		}

		if (receiverRegistered) {
			receiverRegistered = false;
			try {
				context.unregisterReceiver(receiver);
			} catch (RuntimeException error) {
				// 广播注销失败也必须继续释放 Wi-Fi 锁和组件事件。
				Log.e("WIFI管理", "注销 Wi-Fi 广播失败", error);
			}
		}

		try {
			解锁WIFI();
		} catch (RuntimeException error) {
			Log.e("WIFI管理", "释放 Wi-Fi 锁失败", error);
		} finally {
			// 某项系统资源清理失败也不能遗留组件事件注册。
			super.销毁();
		}
	}

	/* 内部辅助方法 */

	/**
	 * 在 API 30 以下通过旧接口写入尚未保存的网络，返回系统网络标识。
	 *
	 * <p>用于低版本兼容及系统允许的管理场景，普通应用在 API 29 起可能被系统拒绝。
	 *
	 * @param ssid 网络名称
	 * @param password 网络密码，开放网络忽略
	 * @param type 1 开放，2 WEP，3 WPA
	 * @return 新增的系统网络标识；失败时为 -1
	 * @throws IllegalArgumentException 网络类型不在 1～3 内时
	 */
	@SuppressWarnings("deprecation")
	private int addLegacyNetwork(String ssid, String password, int type) {
		if (destroyed || manager == null) {
			return -1;
		}

		// 调用方已经检查已有配置，这里只构造并提交新配置。
		android.net.wifi.WifiConfiguration config = new android.net.wifi.WifiConfiguration();
		config.SSID = quote(ssid);

		switch (type) {
			case 1:
				config.allowedKeyManagement.set(android.net.wifi.WifiConfiguration.KeyMgmt.NONE);
				break;
			case 2:
				config.wepKeys[0] = password.matches("(?:[0-9a-fA-F]{10}|[0-9a-fA-F]{26}|[0-9a-fA-F]{58})") ? password : quote(password);
				config.allowedKeyManagement.set(android.net.wifi.WifiConfiguration.KeyMgmt.NONE);
				config.allowedAuthAlgorithms.set(android.net.wifi.WifiConfiguration.AuthAlgorithm.OPEN);
				config.allowedAuthAlgorithms.set(android.net.wifi.WifiConfiguration.AuthAlgorithm.SHARED);
				config.wepTxKeyIndex = 0;
				break;
			case 3:
				config.preSharedKey = password.matches("[0-9a-fA-F]{64}") ? password : quote(password);
				config.allowedKeyManagement.set(android.net.wifi.WifiConfiguration.KeyMgmt.WPA_PSK);
				break;
			default:
				throw new IllegalArgumentException("网络类型应为 1（开放）、2（WEP）或 3（WPA）");
		}

		return manager.addNetwork(config);
	}

	/**
	 * 调用低版本或新版系统豁免应用仍可使用的直接开关接口。
	 *
	 * @param enabled 是否开启
	 * @return 系统是否接受；普通目标 API 29 及以上应用在新版系统上返回假
	 */
	@SuppressWarnings("deprecation")
	private boolean legacySetWifiEnabled(boolean enabled) {
		try {
			return manager.setWifiEnabled(enabled);
		} catch (SecurityException error) {
			Log.e("WIFI管理", "系统未授权切换 Wi-Fi 状态", error);
			return false;
		}
	}

	/**
	 * 在低版本或系统授予管理权限时按原网络标识启用配置。
	 *
	 * @param networkId 系统网络标识
	 * @param disableOthers 是否禁用其它配置
	 * @return 系统是否接受请求
	 */
	@SuppressWarnings("deprecation")
	private boolean legacyEnableNetwork(int networkId, boolean disableOthers) {
		try {
			return networkId >= 0 && manager.enableNetwork(networkId, disableOthers);
		} catch (SecurityException error) {
			Log.e("WIFI管理", "系统未授权连接已保存网络", error);
			return false;
		}
	}

	/**
	 * 在低版本或系统授予管理权限时删除原系统标识对应的配置。
	 *
	 * @param networkId 系统网络标识
	 * @return 系统是否成功删除
	 */
	@SuppressWarnings("deprecation")
	private boolean legacyRemoveNetwork(int networkId) {
		try {
			return networkId >= 0 && manager.removeNetwork(networkId);
		} catch (SecurityException error) {
			Log.e("WIFI管理", "系统未授权删除已保存网络", error);
			return false;
		}
	}

	/**
	 * 从旧配置读取状态，供低版本及新版无法直接确认当前连接时使用。
	 *
	 * @param networkId 系统网络标识，不是列表下标
	 * @return 0 当前连接，1 禁用、不存在或不可读取，2 启用
	 */
	@SuppressWarnings("deprecation")
	private int legacyNetworkStatus(int networkId) {
		for (android.net.wifi.WifiConfiguration network : savedNetworks()) {
			if (network.networkId == networkId) {
				return network.status;
			}
		}
		return android.net.wifi.WifiConfiguration.Status.DISABLED;
	}

	/**
	 * 结束当前保存请求，先清除等待状态，允许事件处理程序立即发起下一次请求。
	 *
	 * @param request 预期的请求编号
	 * @param result 要分发的统一结果码
	 */
	private void completeNetworkRequest(int request, int result) {
		if (!destroyed && pendingNetworkRequest == request) {
			String ssid = pendingNetworkSsid;
			pendingNetworkRequest = 0;
			pendingNetworkSsid = null;
			网络添加结果(ssid, result);
		}
	}

	/**
	 * 结束一次有效扫描等待，成功和失败均通知，避免调用方一直等待。
	 *
	 * @param updated 系统是否成功更新扫描结果
	 */
	private synchronized void completeScan(boolean updated) {
		if (!destroyed && scanPending) {
			scanPending = false;
			扫描完毕(updated);
		}
	}

	/**
	 * 主动请求扫描；API 30 尚无语义相同的非弃用替代接口。
	 *
	 * @return 系统是否接受请求
	 */
	@SuppressWarnings("deprecation")
	private boolean startScan() {
		return manager.startScan();
	}

	/**
	 * 为 API 29 以下系统创建原有的普通 Wi-Fi 锁。
	 *
	 * @return 尚未获取的锁
	 */
	@SuppressWarnings("deprecation")
	private WifiManager.WifiLock legacyWifiLock() {
		return manager.createWifiLock("Simple.WIFI管理");
	}

	/**
	 * 添加旧版认证失败广播；API 28 起已无保证，系统仍发送时保持原有通知。
	 *
	 * @param filter 本组件的广播过滤器
	 */
	@SuppressWarnings("deprecation")
	private static void addLegacyAuthenticationAction(IntentFilter filter) {
		filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
	}

	/**
	 * 解读系统旧版的认证失败通知，不将普通连接失败推断为密码错误。
	 *
	 * @param intent 收到的广播
	 */
	@SuppressWarnings("deprecation")
	private void legacyAuthenticationError(Intent intent) {
		if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(intent.getAction())
				&& intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1) == WifiManager.ERROR_AUTHENTICATING) {
			密码错误();
		}
	}

	/**
	 * 解读旧版连接广播，供 API 29 以下及新版回调注册失败时使用。
	 *
	 * @param intent 系统连接状态广播
	 * @param initialSticky 是否为注册时补发的粘性广播，仅建立基线
	 */
	@SuppressWarnings("deprecation")
	private void legacyNetworkState(Intent intent, boolean initialSticky) {
		android.net.NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
		if (info != null) {
			int state = networkState(info.getState());
			boolean changed = state != legacyConnectionState;
			legacyConnectionState = state;
			if (!initialSticky && changed) {
				网络状态改变(state);
			}
		}
	}

	/**
	 * 在系统仍允许修改已保存网络时提交完整配置。
	 *
	 * @param config 待提交的系统配置
	 * @return 系统网络标识；被拒绝时为 -1
	 */
	@SuppressWarnings("deprecation")
	private int updateNetwork(android.net.wifi.WifiConfiguration config) {
		return manager.updateNetwork(config);
	}

	/**
	 * 构造 API 24～30 使用的静态 IP 配置；不尝试绕过隐藏接口限制。
	 *
	 * @param config 要修改的已保存网络
	 * @param ip IPv4 地址
	 * @param prefixLength 网络前缀长度
	 * @param gateway IPv4 网关
	 * @param dns IPv4 DNS 地址
	 * @throws ReflectiveOperationException 系统隐藏配置接口不可访问时
	 * @throws IOException 地址格式错误或不能解析时
	 */
	@SuppressWarnings("deprecation")
	private static void applyStaticIp(android.net.wifi.WifiConfiguration config, String ip, int prefixLength, String gateway, String dns)
			throws ReflectiveOperationException, IOException {
		InetAddress address = ipv4Literal(ip);
		InetAddress route = ipv4Literal(gateway);
		InetAddress server = ipv4Literal(dns);
		Class<?> type = Class.forName("android.net.StaticIpConfiguration");
		Object settings = type.getConstructor().newInstance();
		Object link = Class.forName("android.net.LinkAddress").getConstructor(InetAddress.class, int.class)
				.newInstance(address, prefixLength);
		type.getField("ipAddress").set(settings, link);
		type.getField("gateway").set(settings, route);
		Object servers = type.getField("dnsServers").get(settings);
		List.class.getMethod("add", Object.class).invoke(servers, server);

		Class<?> assignment = Class.forName("android.net.IpConfiguration$IpAssignment");
		Object staticValue = assignment.getField("STATIC").get(null);
		config.getClass().getMethod("setStaticIpConfiguration", type).invoke(config, settings);
		config.getClass().getMethod("setIpAssignment", assignment).invoke(config, staticValue);
	}

	/**
	 * 解析 IPv4 字面值，避免把错误地址作为主机名发起 DNS 查询。
	 *
	 * @param text 点分十进制地址
	 * @return 解析后的 IPv4 地址
	 * @throws IOException 输入不是有效 IPv4 地址时
	 */
	private static InetAddress ipv4Literal(String text) throws IOException {
		if (text == null || !text.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) {
			throw new IOException("需要点分十进制 IPv4 地址");
		}
		String[] parts = text.split("\\.");
		byte[] address = new byte[4];
		for (int i = 0; i < parts.length; i++) {
			int value = Integer.parseInt(parts[i]);
			if (value > 255) {
				throw new IOException("IPv4 地址段超出范围");
			}
			address[i] = (byte) value;
		}
		// 直接构造四字节地址，避免系统字符串解析对前导零的差异或回退到 DNS。
		return InetAddress.getByAddress(address);
	}

	/**
	 * 延迟创建热点适配器，并将当前实例的本地热点结果接入组件事件。
	 *
	 * @return 当前组件使用的热点适配器；组件已销毁时为 {@code null}
	 */
	private synchronized WifiApAdmin hotspot() {
		if (destroyed) {
			return null;
		}
		if (hotspot == null) {
			hotspot = new WifiApAdmin(context, new WifiApAdmin.Listener() {
				/**
				 * 将有效组件的实际热点配置交给成功事件。
				 *
				 * @param ssid 系统生成的热点名称
				 * @param password 系统生成的热点密码
				 */
				@Override
				public void onStarted(String ssid, String password) {
					if (!destroyed) {
						本地热点创建成功(ssid, password);
					}
				}

				/**
				 * 将有效组件的本地热点失败原因交给失败事件。
				 *
				 * @param reason 系统错误码
				 */
				@Override
				public void onFailed(int reason) {
					if (!destroyed) {
						本地热点创建失败(reason);
					}
				}
			});
		}
		return hotspot;
	}

	/**
	 * 在 API 30 以下使用旧接口计算固定的五级信号档位。
	 *
	 * @param rssi 信号强度，单位为 dBm
	 * @return 0～4 的信号档位
	 */
	@SuppressWarnings("deprecation")
	private static int legacySignalLevel(int rssi) {
		return WifiManager.calculateSignalLevel(rssi, 5);
	}

	/**
	 * 按 Wi-Fi 整数地址的低字节在前顺序生成 IPv4 文本。
	 *
	 * @param address Wi-Fi 或 DHCP 返回的整数地址
	 * @return 与原格式化接口一致的点分十进制文本，包括全零地址
	 */
	private static String ipv4Address(int address) {
		return (address & 0xff) + "." + ((address >>> 8) & 0xff) + "."
				+ ((address >>> 16) & 0xff) + "." + ((address >>> 24) & 0xff);
	}

	/**
	 * 统一处理 Wi-Fi 隐私限制返回的空值和占位 MAC 地址。
	 *
	 * @param address 系统报告的地址，可能为空
	 * @return 可用地址；系统未提供真实地址时为空字符串
	 */
	private static String readableMacAddress(String address) {
		return address == null || "02:00:00:00:00:00".equalsIgnoreCase(address) ? "" : address;
	}

	/**
	 * 将旧版连接广播的状态转换为组件约定值；NetworkInfo.State 本身也已弃用。
	 *
	 * @param state 当前状态
	 * @return 组件约定的网络状态值
	 */
	@SuppressWarnings("deprecation")
	private static int networkState(android.net.NetworkInfo.State state) {
		switch (state) {
			case CONNECTED:
				return 1;
			case CONNECTING:
				return 2;
			case DISCONNECTED:
				return 3;
			case DISCONNECTING:
				return 4;
			case SUSPENDED:
				return 5;
			default:
				return 6;
		}
	}

	/**
	 * 取得系统允许读取的旧类型无线配置，供低版本兼容及获准的管理操作使用。
	 *
	 * @return 系统允许读取的配置列表；无配置或被限制时为空列表
	 */
	@SuppressWarnings("deprecation")
	private List<android.net.wifi.WifiConfiguration> savedNetworks() {
		if (!destroyed && manager != null) {
			try {
				List<android.net.wifi.WifiConfiguration> networks = manager.getConfiguredNetworks();
				if (networks != null) {
					return networks;
				}
			} catch (SecurityException error) {
				Log.e("WIFI管理", "系统未授权读取已保存网络", error);
			}
		}
		return new ArrayList<android.net.wifi.WifiConfiguration>();
	}

	/**
	 * 查找与指定名称匹配的无线网络配置。
	 *
	 * @param name 要操作的名称
	 * @return 匹配的网络配置；未找到时为 {@code null}
	 */
	@SuppressWarnings("deprecation")
	private android.net.wifi.WifiConfiguration findNetwork(String name) {
		if (name == null || name.isEmpty()) {
			return null;
		}
		String quoted = quote(name);

		for (android.net.wifi.WifiConfiguration network : savedNetworks()) {
			if (quoted.equals(network.SSID)) {
				return network;
			}
		}

		return null;
	}

	/**
	 * 为旧配置值添加外围双引号，保留名称和密码中的实际字符。
	 *
	 * @param text 待处理文本
	 * @return 仅添加外围双引号的文本
	 */
	private static String quote(String text) {
		// Android 的配置转换只去除外围引号，不解码 Java 风格的反斜杠转义。
		return "\"" + text + "\"";
	}

	/**
	 * 去除文本外围的引用符号。
	 *
	 * @param text 待处理文本
	 * @return 移除外围引用符号后的文本
	 */
	private static String unquote(String text) {
		if (text == null) {
			return "";
		}

		return text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")
				? text.substring(1, text.length() - 1) : text;
	}

	/**
	 * 读取当前无线网络连接信息。
	 *
	 * @return 当前网络的连接信息；不可读取或组件已销毁时为空
	 */
	private WifiInfo connectionInfo() {
		if (!destroyed && manager != null) {
			try {
				return manager.getConnectionInfo();
			} catch (SecurityException error) {
				Log.e("WIFI管理", "系统未授权读取当前 Wi-Fi 信息", error);
			}
		}
		return null;
	}

	/**
	 * 隔离 API 30 的网络配置和扫描回调类型，低版本不会加载该实现。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	private static final class Api30 {

		/**
		 * 注册主线程扫描通知，将新回调类型及其注销过程封闭在版本分支内。
		 *
		 * @param manager Wi-Fi 服务
		 * @param context 提供主线程执行器的应用上下文
		 * @param onResults 结果更新时执行的通知
		 * @return 仅注销本次注册的动作
		 */
		private static Runnable registerScanCallback(WifiManager manager, Context context, Runnable onResults) {
			WifiManager.ScanResultsCallback callback = new WifiManager.ScanResultsCallback() {

				@Override
				public void onScanResultsAvailable() {
					onResults.run();
				}
			};
			manager.registerScanResultsCallback(context.getMainExecutor(), callback);
			return new Runnable() {
				@Override
				public void run() {
					manager.unregisterScanResultsCallback(callback);
				}
			};
		}

		/**
		 * 构造单个网络的系统确认请求，不向应用伪造网络标识。
		 *
		 * @param ssid 网络名称
		 * @param password WPA2 密码；开放网络忽略
		 * @param type 1 开放，3 WPA2
		 * @return 携带单个配置的系统保存意图
		 * @throws IllegalArgumentException 配置不满足系统要求时
		 */
		private static Intent addNetworkIntent(String ssid, String password, int type) {
			WifiNetworkSuggestion.Builder builder = new WifiNetworkSuggestion.Builder().setSsid(ssid);
			if (type == 3) {
				if (password.length() < 8 || password.length() > 63) {
					throw new IllegalArgumentException("WPA2 密码长度应为 8～63 个字符");
				}
				builder.setWpa2Passphrase(password);
			}
			ArrayList<WifiNetworkSuggestion> networks = new ArrayList<WifiNetworkSuggestion>();
			networks.add(builder.build());
			return new Intent(Settings.ACTION_WIFI_ADD_NETWORKS)
					.putParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST, networks);
		}

		/**
		 * 读取单个网络的保存状态；缺失、数量错误或未知状态均按失败处理。
		 *
		 * @param data 系统返回的结果意图
		 * @return 0 成功，1 失败，2 已存在
		 */
		private static int networkResult(Intent data) {
			try {
				ArrayList<Integer> results = data == null ? null
						: data.getIntegerArrayListExtra(Settings.EXTRA_WIFI_NETWORK_RESULT_LIST);
				if (results != null && results.size() == 1 && results.get(0) != null) {
					int result = results.get(0);
					if (result == Settings.ADD_WIFI_RESULT_SUCCESS || result == Settings.ADD_WIFI_RESULT_ALREADY_EXISTS) {
						return result;
					}
				}
			} catch (RuntimeException error) {
				Log.e("WIFI管理", "系统返回了无效的保存网络结果");
			}
			return Settings.ADD_WIFI_RESULT_ADD_OR_UPDATE_FAILED;
		}
	}

	/**
	 * API 29 起通过 Wi-Fi 传输类型观察连接，避免只观察默认网络而漏掉本地连接。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	private final class NetworkObserver extends ConnectivityManager.NetworkCallback {

		private final ConnectivityManager connectivity;
		private final Map<Network, Boolean> networks = new HashMap<Network, Boolean>();
		private volatile boolean registered;
		private int state = 3;

		/**
		 * 保存本实例要注册和注销的网络服务。
		 */
		private NetworkObserver() {
			connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		}

		/**
		 * 订阅主线程通知并建立当前状态基线，不主动请求或切换网络。
		 */
		private synchronized void register() {
			if (connectivity == null) {
				return;
			}
			try {
				// 先订阅再取基线，避免快照中的网络在订阅前断开后一直残留。
				NetworkRequest request = new NetworkRequest.Builder().clearCapabilities()
						.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
						.addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();
				connectivity.registerNetworkCallback(request, this, handler);
				registered = true;
			} catch (RuntimeException error) {
				Log.e("WIFI管理", "无法注册 Wi-Fi 连接通知，改用广播", error);
				return;
			}

			try {
				for (Network network : connectivity.getAllNetworks()) {
					NetworkCapabilities capabilities = connectivity.getNetworkCapabilities(network);
					if (isWifi(capabilities)) {
						networks.put(network, capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED));
					}
				}
				state = currentState();
			} catch (RuntimeException error) {
				// 基线读取失败不撤销已成功注册的回调，之后仍按回调提供的能力更新。
				state = currentState();
				Log.e("WIFI管理", "无法读取 Wi-Fi 连接状态基线", error);
			}
		}

		/**
		 * 停止本实例的通知，后续已排队回调由销毁标记拒绝。
		 */
		private synchronized void unregister() {
			if (registered) {
				registered = false;
				try {
					connectivity.unregisterNetworkCallback(this);
				} catch (RuntimeException error) {
					Log.e("WIFI管理", "无法注销 Wi-Fi 连接通知", error);
				}
			}
			networks.clear();
		}

		/**
		 * 使用回调携带的能力更新连接状态，不在回调中同步查询系统快照。
		 *
		 * @param network 发生变化的网络
		 * @param capabilities 有序送达的网络能力
		 */
		@Override
		public synchronized void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
			if (!destroyed && registered) {
				if (isWifi(capabilities)) {
					networks.put(network, capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED));
				} else {
					networks.remove(network);
				}
				notifyState();
			}
		}

		/**
		 * 移除断开的 Wi-Fi；仍有其它 Wi-Fi 可用时不误报整体断开。
		 *
		 * @param network 已断开的网络
		 */
		@Override
		public synchronized void onLost(Network network) {
			if (!destroyed && registered) {
				networks.remove(network);
				notifyState();
			}
		}

		/**
		 * 判断是否为直接 Wi-Fi 连接，排除承载于 Wi-Fi 的 VPN。
		 *
		 * @param capabilities 系统网络能力，可能为空
		 * @return 是否属于本组件观察的网络
		 */
		private boolean isWifi(NetworkCapabilities capabilities) {
			return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
					&& capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN);
		}

		/**
		 * 汇总直接 Wi-Fi 的状态，不以互联网验证结果判断是否连接。
		 *
		 * @return 1 已连接，3 已断开，5 全部暂停
		 */
		private int currentState() {
			if (networks.isEmpty()) {
				return 3;
			}
			return networks.containsValue(Boolean.TRUE) ? 1 : 5;
		}

		/**
		 * 只在汇总状态变化时触发事件，抑制重复能力通知。
		 */
		private void notifyState() {
			int current = currentState();
			if (!destroyed && current != state) {
				state = current;
				网络状态改变(current);
			}
		}
	}

}

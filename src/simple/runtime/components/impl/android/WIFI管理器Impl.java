package simple.runtime.components.impl.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;

import simple.runtime.android.MainActivity;
import simple.runtime.components.WIFI管理器;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.components.impl.android.util.WifiApAdmin;
import simple.runtime.events.EventDispatcher;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Wi-Fi 管理实现。静态 IP 和热点保留旧系统反射入口，不绕过系统权限及版本限制。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class WIFI管理器Impl extends 组件Impl implements WIFI管理器 {

	private final Context context;
	private final WifiManager manager;
	private final BroadcastReceiver receiver;
	private WifiManager.WifiLock wifiLock;
	private WifiApAdmin hotspot;
	private boolean scanPending;
	private boolean firstWifiState = true;
	private boolean firstNetworkState = true;
	private boolean receiverRegistered;
	private boolean destroyed;

	public WIFI管理器Impl(组件容器 container) {
		super(container);

		context = MainActivity.getContext().getApplicationContext();
		manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (destroyed) {
					return;
				}

				String action = intent.getAction();
				if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
					if (scanPending) {
						scanPending = false;
						扫描完毕();
					}
				} else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
					if (firstWifiState) {
						firstWifiState = false;
					} else {
						开启状态改变(是否已开启());
					}
				} else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
					if (firstNetworkState) {
						firstNetworkState = false;
					} else {
						NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
						if (info != null) {
							网络状态改变(networkState(info.getState()));
						}
					}
				} else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)
						&& intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1) == WifiManager.ERROR_AUTHENTICATING) {
					密码错误();
				}
			}
		};

		// 接收器随组件存活；保存注册状态，以便销毁时准确注销。
		if (manager != null) {
			IntentFilter filter = new IntentFilter();
			filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
			filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
			filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
			filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);

			context.registerReceiver(receiver, filter);
			receiverRegistered = true;
		}
	}

	/* 组件事件 */

	@Override
	public void 扫描完毕() {
		EventDispatcher.dispatchEvent(this, "扫描完毕");
	}

	@Override
	public void 开启状态改变(boolean enabled) {
		EventDispatcher.dispatchEvent(this, "开启状态改变", enabled);
	}

	@Override
	public void 网络状态改变(int state) {
		EventDispatcher.dispatchEvent(this, "网络状态改变", state);
	}

	@Override
	public void 密码错误() {
		EventDispatcher.dispatchEvent(this, "密码错误");
	}

	/* 组件方法 */

	@Override
	public String[] 取已保存网络列表() {
		List<String> result = new ArrayList<String>();
		for (WifiConfiguration network : savedNetworks()) {
			result.add(unquote(network.SSID) + "|*|" + network.networkId);
		}

		return result.toArray(new String[result.size()]);
	}

	@Override
	public int 取已保存网络(String ssid) {
		WifiConfiguration network = findNetwork(ssid);
		return network == null ? -1 : network.networkId;
	}

	@Override
	public void 打开WIFI设置() {
		MainActivity.getContext().startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
	}

	@Override
	public boolean 置WIFI状态(boolean enabled) {
		return !destroyed && manager != null && manager.setWifiEnabled(enabled);
	}

	@Override
	public boolean 是否已开启() {
		return manager != null && manager.isWifiEnabled();
	}

	@Override
	public void 置静态IP(String ssid, String ip, String gateway, String DNS) {
		WifiConfiguration config = findNetwork(ssid);
		if (destroyed || config == null) {
			return;
		}

		try {
			// 旧 Android 的隐藏结构；不支持该结构时拒绝写入，避免应用半份 IP 配置。
			Field assignment = config.getClass().getField("ipAssignment");
			Object staticValue = null;
			for (Object value : assignment.getType().getEnumConstants()) {
				if ("STATIC".equals(((Enum<?>) value).name())) {
					staticValue = value;
					break;
				}
			}

			if (staticValue == null) {
				throw new IllegalStateException("系统不支持静态 IP 配置");
			}

			Object links = config.getClass().getField("linkProperties").get(config);
			Object address = Class.forName("android.net.LinkAddress").getConstructor(InetAddress.class, int.class)
					.newInstance(InetAddress.getByName(ip), 24);
			Object route = Class.forName("android.net.RouteInfo").getConstructor(InetAddress.class)
					.newInstance(InetAddress.getByName(gateway));
			replaceLink(links, "mLinkAddresses", address);
			replaceLink(links, "mRoutes", route);
			replaceLink(links, "mDnses", InetAddress.getByName(DNS));
			assignment.set(config, staticValue);
			manager.updateNetwork(config);
		} catch (ReflectiveOperationException | IOException | RuntimeException error) {
			Log.e("WIFI管理", "当前系统无法设置静态 IP", error);
		}
	}

	@Override
	public boolean 置指定WIFI状态(int networkId, boolean disableOthers) {
		return !destroyed && manager != null && manager.enableNetwork(networkId, disableOthers);
	}

	@Override
	public int 取连接状态(int networkId) {
		// networkId 是系统标识，不是已保存网络列表的下标。
		for (WifiConfiguration network : savedNetworks()) {
			if (network.networkId == networkId) {
				return network.status;
			}
		}

		return WifiConfiguration.Status.DISABLED;
	}

	@Override
	public boolean 开始扫描() {
		if (destroyed || manager == null || scanPending) {
			return false;
		}

		// 仅成功发起的扫描等待广播；失败或异常必须清除状态，允许再次尝试。
		scanPending = true;

		try {
			if (manager.startScan()) {
				return true;
			}

			scanPending = false;
			return false;
		} catch (RuntimeException error) {
			scanPending = false;
			throw error;
		}
	}

	@Override
	public String 获取扫描结果() {
		List<ScanResult> results = manager == null ? null : manager.getScanResults();
		if (results == null) {
			return "";
		}

		StringBuilder text = new StringBuilder();
		for (ScanResult result : results) {
			if (text.length() > 0) {
				text.append('/');
			}

			text.append(result.SSID).append('*').append(result.BSSID).append('*')
					.append(result.capabilities).append('*').append(result.level).append('*').append(result.frequency);
		}

		return text.toString();
	}

	@Override
	public int 添加网络(String ssid, String password, int type) {
		if (destroyed || manager == null) {
			return -1;
		}

		int existing = 取已保存网络(ssid);
		if (existing != -1) {
			return existing;
		}

		// 已保存网络保持原配置；新网络按公开的类型编号设置认证信息。
		WifiConfiguration config = new WifiConfiguration();
		config.SSID = quote(ssid);

		switch (type) {
			case 1:
				config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
				break;
			case 2:
				config.hiddenSSID = true;
				config.wepKeys[0] = password.matches("(?:[0-9a-fA-F]{10}|[0-9a-fA-F]{26}|[0-9a-fA-F]{58})") ? password : quote(password);
				config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
				config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
				config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
				config.wepTxKeyIndex = 0;
				break;
			case 3:
				config.hiddenSSID = true;
				config.preSharedKey = password.matches("[0-9a-fA-F]{64}") ? password : quote(password);
				config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
				break;
			default:
				throw new IllegalArgumentException("网络类型应为 1（开放）、2（WEP）或 3（WPA）");
		}

		return manager.addNetwork(config);
	}

	@Override
	public boolean 删除网络(int networkId) {
		return !destroyed && manager != null && manager.removeNetwork(networkId);
	}

	@Override
	public void 锁定WIFI() {
		if (destroyed || manager == null) {
			return;
		}

		if (wifiLock == null) {
			wifiLock = manager.createWifiLock("Simple.WIFI管理");
		}

		if (!wifiLock.isHeld()) {
			wifiLock.acquire();
		}
	}

	@Override
	public void 解锁WIFI() {
		if (wifiLock != null && wifiLock.isHeld()) {
			wifiLock.release();
		}
	}

	@Override
	public void 创建热点(String ssid, String password) {
		if (destroyed || manager == null) {
			return;
		}

		if (hotspot == null) {
			hotspot = new WifiApAdmin(context);
		}

		hotspot.startWifiAp(ssid, password);
	}

	@Override
	public void 关闭热点() {
		WifiApAdmin.closeWifiAp(context);
	}

	@Override
	public String 取当前BSSID() {
		WifiInfo info = connectionInfo();
		return info == null || info.getBSSID() == null ? "" : info.getBSSID();
	}

	@Override
	public String 取当前SSID() {
		WifiInfo info = connectionInfo();
		return info == null ? "" : unquote(info.getSSID());
	}

	@Override
	public int 取当前networkid() {
		WifiInfo info = connectionInfo();
		return info == null ? -1 : info.getNetworkId();
	}

	@Override
	public String 取内网IP() {
		WifiInfo info = connectionInfo();
		return info == null || info.getIpAddress() == 0 ? "" : Formatter.formatIpAddress(info.getIpAddress());
	}

	@Override
	public String 取MAC地址() {
		WifiInfo info = connectionInfo();
		return info == null || info.getMacAddress() == null ? "" : info.getMacAddress();
	}

	@Override
	public int 取当前信号强度() {
		WifiInfo info = connectionInfo();
		return info == null ? 0 : WifiManager.calculateSignalLevel(info.getRssi(), 5);
	}

	@Override
	public int 取当前连接速度() {
		WifiInfo info = connectionInfo();
		return info == null ? 0 : info.getLinkSpeed();
	}

	@Override
	public String 取网关() {
		return manager == null || manager.getDhcpInfo() == null ? "" : Formatter.formatIpAddress(manager.getDhcpInfo().gateway);
	}

	@Override
	public String 取网络前缀() {
		return manager == null || manager.getDhcpInfo() == null ? "" : Formatter.formatIpAddress(manager.getDhcpInfo().netmask);
	}

	@Override
	public String 取DNS1() {
		return manager == null || manager.getDhcpInfo() == null ? "" : Formatter.formatIpAddress(manager.getDhcpInfo().dns1);
	}

	@Override
	public String 取DNS2() {
		return manager == null || manager.getDhcpInfo() == null ? "" : Formatter.formatIpAddress(manager.getDhcpInfo().dns2);
	}

	@Override
	public void 销毁() {
		destroyed = true;
		scanPending = false;

		if (receiverRegistered) {
			context.unregisterReceiver(receiver);
			receiverRegistered = false;
		}

		解锁WIFI();

		super.销毁();
	}

	/* 内部辅助方法 */

	private static int networkState(NetworkInfo.State state) {
		switch (state) {
			case CONNECTED: return 1;
			case CONNECTING: return 2;
			case DISCONNECTED: return 3;
			case DISCONNECTING: return 4;
			case SUSPENDED: return 5;
			default: return 6;
		}
	}

	private List<WifiConfiguration> savedNetworks() {
		List<WifiConfiguration> networks = manager == null ? null : manager.getConfiguredNetworks();
		return networks == null ? new ArrayList<WifiConfiguration>() : networks;
	}

	private WifiConfiguration findNetwork(String name) {
		String quoted = quote(name);
		for (WifiConfiguration network : savedNetworks()) {
			if (quoted.equals(network.SSID)) {
				return network;
			}
		}

		return null;
	}

	private static String quote(String text) {
		return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	private static String unquote(String text) {
		if (text == null) {
			return "";
		}

		return text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")
				? text.substring(1, text.length() - 1) : text;
	}

	private WifiInfo connectionInfo() {
		return manager == null ? null : manager.getConnectionInfo();
	}

	private static void replaceLink(Object links, String fieldName, Object value) throws ReflectiveOperationException {
		Field field = links.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		Object collection = field.get(links);
		if (!(collection instanceof List<?>)) {
			throw new IllegalStateException("系统网络配置结构无效");
		}

		((List<?>) collection).clear();

		// 隐藏 Android 类型没有编译期泛型信息，通过反射添加，避免未经检查的集合强转。
		List.class.getMethod("add", Object.class).invoke(collection, value);
	}
}

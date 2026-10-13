package simple.runtime.components.impl.android.util;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * 旧 Android 热点反射适配；构造时不改变当前 Wi-Fi 或热点状态。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class WifiApAdmin {

	private final WifiManager manager;

	public WifiApAdmin(Context context) {
		manager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
	}

	public void startWifiAp(String ssid, String password) {
		if (manager == null) {
			return;
		}

		WifiConfiguration config = new WifiConfiguration();
		config.SSID = ssid;
		if (password == null || password.isEmpty()) {
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
		} else {
			config.preSharedKey = password;
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
		}

		try {
			Method enable = manager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);

			// 先确认热点入口存在；创建失败时恢复此前开启的 Wi-Fi 状态。
			boolean wifiEnabled = manager.isWifiEnabled();
			if (wifiEnabled && !manager.setWifiEnabled(false)) {
				return;
			}

			try {
				Object result = enable.invoke(manager, config, true);
				if (!Boolean.TRUE.equals(result) && wifiEnabled) {
					manager.setWifiEnabled(true);
				}
			} catch (ReflectiveOperationException | RuntimeException error) {
				if (wifiEnabled) {
					manager.setWifiEnabled(true);
				}

				throw error;
			}
		} catch (ReflectiveOperationException | RuntimeException error) {
			Log.e("WIFI管理", "当前系统无法创建热点", error);
		}
	}

	public static void closeWifiAp(Context context) {
		WifiManager manager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		if (manager == null) {
			return;
		}

		try {
			Method enabled = manager.getClass().getMethod("isWifiApEnabled");
			if (!Boolean.TRUE.equals(enabled.invoke(manager))) {
				return;
			}

			WifiConfiguration config = (WifiConfiguration) manager.getClass().getMethod("getWifiApConfiguration").invoke(manager);
			manager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class).invoke(manager, config, false);
		} catch (ReflectiveOperationException | RuntimeException error) {
			Log.e("WIFI管理", "当前系统无法关闭热点", error);
		}
	}
}

/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simple.runtime.components.impl.android;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.text.TextUtils;

import simple.runtime.android.MainActivity;
import simple.runtime.components.位置传感器;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 管理前台位置订阅、旧版状态回调和后台地址查询。
 *
 * <p>API 30 使用 Executor 定位入口，API 24～29 使用 Looper 入口；
 * API 29 起不再接收旧状态回调，改由提供者开关回调及系统广播更新订阅。
 *
 * @author Ellen Spertus
 * @author 树先生 xhwsd@qq.com
 */
public final class 位置传感器Impl extends 组件Impl implements 位置传感器,
		MainActivity.OnResumeListener, MainActivity.OnPauseListener,
		MainActivity.OnDestroyListener, MainActivity.OnRequestPermissionsResultListener {

	private static final long MIN_TIME_MS = 60000;
	private static final float MIN_DISTANCE_M = 5;
	private final MainActivity activity;
	private final LocationManager manager;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private final Executor mainExecutor = new Executor() {
		@Override
		public void execute(Runnable command) {
			handler.post(command);
		}
	};
	private final ExecutorService addressWorker = Executors.newSingleThreadExecutor();
	private final Set<String> providers = new LinkedHashSet<>();
	private final ProviderReceiver receiver = new ProviderReceiver();
	private LocationSensorListener listener;
	private Location lastLocation;
	private boolean initialized;
	private boolean attached;
	private boolean receiverRegistered;
	private boolean refreshQueued;
	private boolean enabled;
	private volatile boolean destroyed;
	private boolean queryingAddress;
	private String currentAddress = "";
	private String lastError = "";
	private String reportedState = "";

	/**
	 * 取得宿主和定位服务，尚不申请定位或登记生命周期监听。
	 *
	 * @param container 所属组件容器
	 */
	public 位置传感器Impl(组件容器 container) {
		super(container);
		activity = MainActivity.getContext();
		manager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
	}

	/* 组件事件 */

	@Override
	public void 位置改变(double latitude, double longitude, double altitude) {
		if (!destroyed && enabled && activity.isInForeground()) {
			EventDispatcher.dispatchEvent(this, "位置改变", latitude, longitude, altitude);
		}
	}

	@Override
	public void 定位状态改变(boolean listening, String message) {
		EventDispatcher.dispatchEvent(this, "定位状态改变", listening, message);
	}

	@Override
	public void 地址查询完毕(double latitude, double longitude, String address) {
		EventDispatcher.dispatchEvent(this, "地址查询完毕", latitude, longitude, address);
	}

	@Override
	public void 操作失败(String operation, String message) {
		EventDispatcher.dispatchEvent(this, "操作失败", operation, message);
	}

	@Override
	public void 初始化() {
		// 延后挂接宿主监听，避开初始化事件中的重入。
		if (initialized || destroyed) {
			return;
		}
		initialized = true;
		EventDispatcher.dispatchEvent(this, "初始化");
		queueRefresh();
	}

	/* 组件属性 */

	@Override
	public boolean 是否可用() {
		return lastLocation != null;
	}

	@Override
	public boolean 具备海拔() {
		return lastLocation != null && lastLocation.hasAltitude()
				&& Double.isFinite(lastLocation.getAltitude());
	}

	@Override
	public boolean 具备精度() {
		return lastLocation != null && lastLocation.hasAccuracy()
				&& Float.isFinite(lastLocation.getAccuracy()) && lastLocation.getAccuracy() >= 0;
	}

	@Override
	public double 经度() {
		return lastLocation == null ? 0 : lastLocation.getLongitude();
	}

	@Override
	public double 纬度() {
		return lastLocation == null ? 0 : lastLocation.getLatitude();
	}

	@Override
	public double 海拔() {
		return 具备海拔() ? lastLocation.getAltitude() : 0;
	}

	@Override
	public double 精度() {
		return 具备精度() ? lastLocation.getAccuracy() : 0;
	}

	@Override
	public boolean 启用() {
		return enabled && !destroyed;
	}

	@Override
	public void 启用(boolean value) {
		if (!usable()) {
			return;
		}
		enabled = value;
		if (!value) {
			// 先使监听失效，避免已排队的位置在停用之后仍触发用户事件。
			stopListening();
		}
		if (initialized) {
			queueRefresh();
		}
	}

	@Override
	public boolean 正在定位() {
		return !destroyed && enabled && activity.isInForeground() && listener != null
				&& providers.size() > listener.unavailable.size();
	}

	@Override
	public String 错误信息() {
		return lastError;
	}

	@Override
	public String 当前地址() {
		return currentAddress;
	}

	/* 组件方法 */

	@Override
	public boolean 请求地址() {
		if (!usable()) {
			return false;
		}
		if (lastLocation == null || queryingAddress) {
			return reject(lastLocation == null ? "尚未取得有效位置" : "上一项地址查询尚未结束");
		}
		try {
			if (!Geocoder.isPresent()) {
				return reject("系统没有提供地址查询服务");
			}
		} catch (RuntimeException error) {
			return reject(errorMessage(error));
		}
		final double latitude = lastLocation.getLatitude();
		final double longitude = lastLocation.getLongitude();
		queryingAddress = true;
		lastError = "";
		try {
			addressWorker.execute(new Runnable() {
				@Override
				public void run() {
					try {
						// 编译上限为 API 30，尚不能使用 API 33 的异步入口；同步服务仅在工作线程调用。
						List<Address> addresses = new Geocoder(activity).getFromLocation(latitude, longitude, 1);
						final String address = addressText(addresses);
						handler.post(new Runnable() {
							@Override
							public void run() {
								if (destroyed) {
									return;
								}
								queryingAddress = false;
								if (lastLocation != null && lastLocation.getLatitude() == latitude
										&& lastLocation.getLongitude() == longitude) {
									currentAddress = address;
								}
								lastError = "";
								地址查询完毕(latitude, longitude, address);
							}
						});
					} catch (IOException | RuntimeException error) {
						handler.post(new Runnable() {
							@Override
							public void run() {
								if (!destroyed) {
									queryingAddress = false;
									failure("查询地址", errorMessage(error));
								}
							}
						});
					}
				}
			});
			return true;
		} catch (RuntimeException error) {
			queryingAddress = false;
			return reject(errorMessage(error));
		}
	}

	@Override
	public void 销毁() {
		// 先使请求失效；宿主监听的注销排队执行，避免修改正在遍历的集合。
		if (destroyed) {
			return;
		}
		destroyed = true;
		enabled = false;
		queryingAddress = false;
		addressWorker.shutdownNow();
		handler.post(new Runnable() {
			@Override
			public void run() {
				stopListening();
				stopReceiver();
				if (attached) {
					activity.removeOnResumeListener(位置传感器Impl.this);
					activity.removeOnPauseListener(位置传感器Impl.this);
					activity.removeOnDestroyListener(位置传感器Impl.this);
					activity.removeOnRequestPermissionsResultListener(位置传感器Impl.this);
					attached = false;
				}
				位置传感器Impl.super.销毁();
			}
		});
	}

	/* 宿主回调 */

	@Override
	public void onResume() {
		queueRefresh();
	}

	@Override
	public void onPause() {
		// 仅前台定位，离开前台即注销，不申请后台定位权限。
		stopListening();
		stopReceiver();
		publishState("应用已离开前台");
	}

	@Override
	public void onDestroy() {
		销毁();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		// 权限变化后重新检查定位状态，不依赖某一个请求号或授予数组。
		queueRefresh();
	}

	/* 内部辅助方法 */

	/**
	 * 校验修改操作的线程和组件状态。
	 *
	 * @return 主线程且实例有效时为真
	 */
	private boolean usable() {
		if (Looper.myLooper() != handler.getLooper() || destroyed) {
			return reject("组件已销毁或当前不在主线程");
		}
		return true;
	}

	/**
	 * 记录未受理原因，不伪造异步结果。
	 *
	 * @param message 未受理原因
	 * @return 始终为假
	 */
	private boolean reject(String message) {
		lastError = message;
		return false;
	}

	/**
	 * 记录实际失败并延后分发，避免宿主或提供者回调中的用户代码重入。
	 *
	 * @param operation 失败操作
	 * @param message 错误说明
	 */
	private void failure(String operation, String message) {
		lastError = message;
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (!destroyed) {
					操作失败(operation, message);
				}
			}
		});
	}

	/**
	 * 将异常类型和原因转换为可读文本。
	 *
	 * @param error 实际异常
	 * @return 错误说明
	 */
	private static String errorMessage(Exception error) {
		return error.getClass().getSimpleName() + "：" + String.valueOf(error.getMessage());
	}

	/**
	 * 合并同一轮权限、开关和生命周期变化，延后注册避免修改宿主正在遍历的集合。
	 */
	private void queueRefresh() {
		if (!destroyed && initialized && !refreshQueued) {
			refreshQueued = true;
			handler.post(new Runnable() {
				@Override
				public void run() {
					refreshQueued = false;
					if (!destroyed) {
						refresh();
					}
				}
			});
		}
	}

	/**
	 * 根据真实权限和已开启的提供者调整订阅，保留网络与卫星的互补定位路径。
	 */
	private void refresh() {
		if (!attached) {
			activity.addOnResumeListener(this);
			activity.addOnPauseListener(this);
			activity.addOnDestroyListener(this);
			activity.addOnRequestPermissionsResultListener(this);
			attached = true;
		}
		if (!enabled || !activity.isInForeground() || activity.isFinishing() || activity.isDestroyed()) {
			stopListening();
			stopReceiver();
			publishState(enabled ? "应用未在前台" : "已停用");
			return;
		}
		try {
			ensureReceiver();
			boolean fine = granted(Manifest.permission.ACCESS_FINE_LOCATION);
			boolean coarse = granted(Manifest.permission.ACCESS_COARSE_LOCATION);
			if (!fine && !coarse) {
				stopListening();
				publishState("未授予定位权限");
				if (!"未授予定位权限".equals(lastError)) {
					failure("定位", "未授予定位权限");
				}
				return;
			}
			if (manager == null) {
				throw new IllegalStateException("系统没有定位服务");
			}
			Set<String> desired = new LinkedHashSet<>();
			List<String> available = manager.getProviders(true);
			if (fine && available.contains(LocationManager.GPS_PROVIDER)) {
				desired.add(LocationManager.GPS_PROVIDER);
			}
			if (available.contains(LocationManager.NETWORK_PROVIDER)) {
				desired.add(LocationManager.NETWORK_PROVIDER);
			}
			if (desired.equals(providers) && listener != null) {
				publishState("正在等待定位：" + TextUtils.join("、", providers));
				return;
			}

			stopListening();
			if (desired.isEmpty()) {
				publishState("没有已开启且可使用的定位提供者");
				return;
			}
			LocationSensorListener next = new LocationSensorListener();
			listener = next;
			String registrationError = "";
			for (String provider : desired) {
				try {
					subscribe(provider, next);
					providers.add(provider);
				} catch (RuntimeException error) {
					registrationError = errorMessage(error);
				}
			}
			if (providers.isEmpty()) {
				// 注册异常也尝试注销，覆盖服务端已经登记但调用返回失败的边界。
				stopListening();
				failure("定位", registrationError);
				publishState("定位监听未能启动");
			} else {
				lastError = "";
				publishState("正在等待定位：" + TextUtils.join("、", providers));
				if (!registrationError.isEmpty()) {
					failure("定位", registrationError);
				}
			}
		} catch (RuntimeException error) {
			stopListening();
			failure("定位", errorMessage(error));
			publishState("定位监听未能启动");
		}
	}

	/**
	 * 检查已声明的定位权限实际是否授予。
	 *
	 * @param permission 权限名称
	 * @return 已授予时为真
	 */
	private boolean granted(String permission) {
		return activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
	}

	/**
	 * 按设备 API 等级调用真实可用的定位注册入口。
	 *
	 * @param provider 提供者名称
	 * @param target 本轮订阅的监听器
	 */
	private void subscribe(String provider, LocationSensorListener target) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			manager.requestLocationUpdates(provider, MIN_TIME_MS, MIN_DISTANCE_M, mainExecutor, target);
		} else {
			// API 24～29 没有 Executor 重载，实际使用旧 Looper 入口并指定主线程。
			manager.requestLocationUpdates(provider, MIN_TIME_MS, MIN_DISTANCE_M, target, handler.getLooper());
		}
	}

	/**
	 * 先使当前监听失效再注销，忽略系统队列中尚未送达的旧位置。
	 */
	private void stopListening() {
		LocationSensorListener previous = listener;
		listener = null;
		providers.clear();
		if (previous != null && manager != null) {
			try {
				manager.removeUpdates(previous);
			} catch (RuntimeException error) {
				Log.w("位置传感器", "注销定位监听失败", error);
				lastError = errorMessage(error);
			}
		}
	}

	/**
	 * 登记提供者开关广播，全部定位关闭时仍可在再次开启后恢复。
	 */
	private void ensureReceiver() {
		if (!receiverRegistered) {
			IntentFilter filter = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
			filter.addAction(LocationManager.MODE_CHANGED_ACTION);
			activity.registerReceiver(receiver, filter);
			receiverRegistered = true;
		}
	}

	/**
	 * 停用或离开前台时注销开关广播。
	 */
	private void stopReceiver() {
		if (receiverRegistered) {
			receiverRegistered = false;
			try {
				activity.unregisterReceiver(receiver);
			} catch (RuntimeException error) {
				Log.w("位置传感器", "注销定位开关监听失败", error);
			}
		}
	}

	/**
	 * 仅在状态确实改变后排队通知，不在定位注册过程中执行用户代码。
	 *
	 * @param message 状态说明
	 */
	private void publishState(String message) {
		boolean listening = 正在定位();
		String state = listening + ":" + message;
		if (!state.equals(reportedState)) {
			reportedState = state;
			handler.post(new Runnable() {
				@Override
				public void run() {
					if (!destroyed) {
						定位状态改变(listening, message);
					}
				}
			});
		}
	}

	/**
	 * 验证坐标范围和非有限值，避免向用户报告无效快照。
	 *
	 * @param location 系统位置结果
	 * @return 坐标有效时为真
	 */
	private static boolean validLocation(Location location) {
		return location != null && Double.isFinite(location.getLatitude())
				&& Double.isFinite(location.getLongitude())
				&& location.getLatitude() >= -90 && location.getLatitude() <= 90
				&& location.getLongitude() >= -180 && location.getLongitude() <= 180;
	}

	/**
	 * 将服务返回的第一份地址按行合并，不添加多余尾换行。
	 *
	 * @param addresses 服务返回的地址集合，可为空
	 * @return 地址文本；没有匹配地址时为空
	 */
	private static String addressText(List<Address> addresses) {
		if (addresses == null || addresses.isEmpty() || addresses.get(0) == null) {
			return "";
		}
		Address address = addresses.get(0);
		List<String> lines = new ArrayList<>();
		for (int index = 0; index <= address.getMaxAddressLineIndex(); index++) {
			String line = address.getAddressLine(index);
			if (line != null && !line.isEmpty()) {
				lines.add(line);
			}
		}
		return TextUtils.join("\n", lines);
	}

	/**
	 * 接收实际系统开关变化，再通过合并队列重建定位订阅。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	private final class ProviderReceiver extends BroadcastReceiver {

		/**
		 * 重新读取真实状态，不信任广播中可选的版本相关附加字段。
		 *
		 * @param context 接收上下文
		 * @param intent 系统广播
		 */
		@Override
		public void onReceive(Context context, Intent intent) {
			queueRefresh();
		}
	}

	/**
	 * 持有本轮订阅身份，过滤停用、重订阅和销毁后的迟到回调。
	 *
	 * @author Ellen Spertus
	 * @author 树先生 xhwsd@qq.com
	 */
	private final class LocationSensorListener implements LocationListener {

		private final Set<String> unavailable = new LinkedHashSet<>();

		/**
		 * 接收有效且不早于上一份结果的位置，并原子更新海拔、精度及坐标快照。
		 *
		 * @param location 系统结果
		 */
		@Override
		public void onLocationChanged(Location location) {
			if (!current() || !validLocation(location)) {
				return;
			}
			if (lastLocation != null
					&& location.getElapsedRealtimeNanos() < lastLocation.getElapsedRealtimeNanos()) {
				return;
			}
			if (lastLocation == null || location.getLatitude() != lastLocation.getLatitude()
					|| location.getLongitude() != lastLocation.getLongitude()) {
				currentAddress = "";
			}
			Location copy = new Location(location);
			lastLocation = copy;
			if (unavailable.remove(location.getProvider())) {
				publishState("定位提供者已恢复：" + location.getProvider());
			}
			位置改变(纬度(), 经度(), 海拔());
		}

		/**
		 * 提供者被关闭后按当前系统状态调整订阅。
		 *
		 * @param provider 提供者名称
		 */
		@Override
		public void onProviderDisabled(String provider) {
			if (current()) {
				queueRefresh();
			}
		}

		/**
		 * 提供者开启后重新检查可用定位来源。
		 *
		 * @param provider 提供者名称
		 */
		@Override
		public void onProviderEnabled(String provider) {
			if (current()) {
				queueRefresh();
			}
		}

		/**
		 * 保留 API 24～28 实际使用的旧状态回调；API 29 起系统不再调用此入口。
		 *
		 * @param provider 提供者名称
		 * @param status 旧版提供者状态
		 * @param extras 附加信息
		 */
		@Override
		@SuppressWarnings("deprecation")
		public void onStatusChanged(String provider, int status, Bundle extras) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || !current() || !providers.contains(provider)) {
				return;
			}
			// 弃用常量仅用于旧系统回调，不抑制其它定位方法中的警告。
			if (status == LocationProvider.OUT_OF_SERVICE) {
				unavailable.add(provider);
				publishState("定位提供者暂时不可用：" + provider);
			} else if (status == LocationProvider.AVAILABLE) {
				unavailable.remove(provider);
				publishState("定位提供者已恢复：" + provider);
			}
		}

		/**
		 * 判断监听器是否仍归当前前台订阅所有。
		 *
		 * @return 当前轮次且组件已启用时为真
		 */
		private boolean current() {
			return !destroyed && enabled && activity.isInForeground() && listener == this;
		}
	}
}

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

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.CallLog;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import simple.runtime.android.MainActivity;
import simple.runtime.collections.集合;
import simple.runtime.components.电话;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.errors.运行错误;
import simple.runtime.events.EventDispatcher;
import simple.runtime.variants.ArrayVariant;
import simple.runtime.日志输出;

/**
 * 使用系统公开接口实现电话、SIM 卡、通话记录和振动功能。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public final class 电话Impl extends 组件Impl implements 电话 {

	private final MainActivity activity;
	private final TelephonyManager telephony;
	private final Vibrator vibrator;
	private final MainActivity.OnDestroyListener destroyListener;
	private final Object lifecycle = new Object();

	// 监听代次用于丢弃停止、重启或销毁前已排队的回调。
	private PhoneStateListener phoneListener;
	private long listenerGeneration;
	private boolean listening;
	private boolean vibrating;
	private boolean destroyed;

	/**
	 * 创建组件，仅获取系统服务，不在权限确认前开始监听。
	 *
	 * @param container 容纳此不可视组件的窗口，不得为空。
	 */
	public 电话Impl(组件容器 container) {
		super(container);
		activity = MainActivity.getContext();
		telephony = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
		vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
		destroyListener = new MainActivity.OnDestroyListener() {
			@Override
			public void onDestroy() {
				// Activity 正在遍历监听器列表，此处不能修改该列表。
				release(false);
			}
		};
		activity.addOnDestroyListener(destroyListener);
	}

	/* 组件事件 */

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void 通话状态改变(int state, String phoneNumber) {
		EventDispatcher.dispatchEvent(this, "通话状态改变", state, phoneNumber);
	}

	/* 组件属性 */

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean 可用() {
		return telephony != null
			&& activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean 监听中() {
		synchronized (lifecycle) {
			return listening;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int 通话状态() {
		requireAlive();
		if (!可用()) {
			return -1;
		}

		requirePermission("android.permission.READ_PHONE_STATE");

		try {
			return telephony.getCallState();
		} catch (RuntimeException exception) {
			throw operationError("查询通话状态失败", exception);
		}
	}

	/* 组件方法 */

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void 呼叫(String phoneNumber) {
		requirePhone();
		if (phoneNumber == null || phoneNumber.trim().length() == 0) {
			throw new IllegalArgumentException("电话号码不得为空");
		}

		requirePermission("android.permission.CALL_PHONE");
		launchPhoneIntent(Intent.ACTION_CALL, phoneNumber.trim());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void 启动拨号界面(String phoneNumber) {
		requireAlive();
		launchPhoneIntent(Intent.ACTION_DIAL, phoneNumber == null ? "" : phoneNumber.trim());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("deprecation")
	public void 振动(int duration) {
		if (duration < 0) {
			throw new IllegalArgumentException("振动持续时间不得为负数");
		}

		synchronized (lifecycle) {
			requireAlive();
			requirePermission("android.permission.VIBRATE");
			if (vibrator == null || !vibrator.hasVibrator()) {
				if (duration == 0) {
					return;
				}

				throw new PhoneOperationError("设备不支持振动");
			}

			try {
				if (duration == 0) {
					vibrator.cancel();
				} else if (Build.VERSION.SDK_INT >= 26) {
					Api26.vibrate(vibrator, duration);
				} else {
					vibrator.vibrate(duration);
				}

				vibrating = duration > 0;
			} catch (RuntimeException exception) {
				throw operationError("控制振动失败", exception);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void 开始监听() {
		requirePhone();
		requirePermission("android.permission.READ_PHONE_STATE");
		if (Looper.myLooper() != Looper.getMainLooper()) {
			throw new PhoneOperationError("请在主线程开始监听通话状态");
		}

		synchronized (lifecycle) {
			requireAlive();
			if (listening) {
				return;
			}

			// 上次注销失败时保留监听器引用，成功清理后才允许重新注册。
			if (phoneListener != null) {
				停止监听();
			}

			final long generation = ++listenerGeneration;
			PhoneStateListener listener = new PhoneStateListener() {
				@Override
				public void onCallStateChanged(int state, String phoneNumber) {
					synchronized (lifecycle) {
						if (destroyed || !listening || generation != listenerGeneration) {
							return;
						}

						通话状态改变(state, phoneNumber == null ? "" : phoneNumber);
					}
				}
			};

			// 先登记状态，使系统返回的首次状态也能派发。
			phoneListener = listener;
			listening = true;

			try {
				telephony.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
			} catch (RuntimeException exception) {
				listening = false;
				listenerGeneration++;
				unregisterQuietly(listener);
				throw operationError("开始监听通话状态失败", exception);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void 停止监听() {
		synchronized (lifecycle) {
			PhoneStateListener listener = phoneListener;
			listening = false;
			listenerGeneration++;
			if (listener != null) {
				try {
					telephony.listen(listener, PhoneStateListener.LISTEN_NONE);
					phoneListener = null;
				} catch (RuntimeException exception) {
					throw operationError("停止监听通话状态失败", exception);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int 取SIM卡状态() {
		requirePhone();

		try {
			return telephony.getSimState();
		} catch (RuntimeException exception) {
			throw operationError("读取 SIM 卡状态失败", exception);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int 取SIM卡状态(int slotIndex) {
		requirePhone();
		if (slotIndex < 0) {
			throw new IllegalArgumentException("卡槽索引不得为负数");
		}

		if (Build.VERSION.SDK_INT < 26) {
			throw new PhoneOperationError("按卡槽读取 SIM 卡状态需要 Android 8.0 或更高版本");
		}

		try {
			return Api26.getSimState(telephony, slotIndex);
		} catch (RuntimeException exception) {
			throw operationError("读取指定卡槽状态失败", exception);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String 取本机号码() {
		requirePhone();

		// 旧系统和旧目标版本仍允许通过电话状态权限读取号码。
		boolean legacyPermission = (Build.VERSION.SDK_INT < 30
			|| activity.getApplicationInfo().targetSdkVersion <= 29)
			&& hasPermission("android.permission.READ_PHONE_STATE");
		if (!legacyPermission) {
			requirePermission(Build.VERSION.SDK_INT >= 26
				? "android.permission.READ_PHONE_NUMBERS" : "android.permission.READ_PHONE_STATE");
		}

		try {
			String number = telephony.getLine1Number();
			return number == null ? "" : number;
		} catch (RuntimeException exception) {
			throw operationError("读取本机号码失败", exception);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public 集合 取通话记录() {
		requireCallLog("android.permission.READ_CALL_LOG");
		集合 result = new 集合();
		String[] projection = {
			CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME,
			CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION
		};

		// 同步查询只遍历最近 100 条，游标在成功和失败路径均关闭。
		Cursor cursor = null;

		try {
			cursor = activity.getContentResolver().query(CallLog.Calls.CONTENT_URI,
				projection, null, null, CallLog.Calls.DATE + " DESC, " + CallLog.Calls._ID + " DESC");
			if (cursor == null) {
				throw new PhoneOperationError("系统未提供通话记录查询结果");
			}

			while (result.计数() < 100 && cursor.moveToNext()) {
				String[] row = new String[projection.length];

				for (int index = 0; index < row.length; index++) {
					row[index] = cursor.isNull(index) ? "" : cursor.getString(index);
				}

				result.加入(ArrayVariant.getArrayVariant(row));
			}

			return result;
		} catch (RuntimeException exception) {
			throw operationError("读取通话记录失败", exception);
		} finally {
			// 不使用 try-with-resources，避免低版本系统缺少 addSuppressed。
			if (cursor != null) {
				try {
					cursor.close();
				} catch (RuntimeException exception) {
					日志输出.warning("电话组件关闭通话记录游标失败：%s", exception.getMessage());
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean 删除通话记录(String recordId) {
		requireCallLog("android.permission.WRITE_CALL_LOG");
		long id = Long.parseLong(recordId == null ? "" : recordId.trim());
		if (id <= 0) {
			throw new IllegalArgumentException("通话记录编号必须为正整数文本");
		}

		// 使用记录 URI 精确删除一条记录，不按电话号码批量删除。
		Uri uri = ContentUris.withAppendedId(CallLog.Calls.CONTENT_URI, id);

		try {
			return activity.getContentResolver().delete(uri, null, null) > 0;
		} catch (RuntimeException exception) {
			throw operationError("删除通话记录失败", exception);
		}
	}

	/* 生命周期与内部辅助 */

	/**
	 * 显式销毁时同步解除 Activity 回调并释放组件资源。
	 */
	@Override
	public void 销毁() {
		release(true);
	}

	/**
	 * 使旧回调失效，并尽力注销监听、取消振动；重复释放不再操作资源。
	 *
	 * @param removeListener 是否允许从 Activity 的监听器列表移除自身。
	 */
	private void release(boolean removeListener) {
		synchronized (lifecycle) {
			if (destroyed) {
				return;
			}

			destroyed = true;
			listenerGeneration++;
			listening = false;
			unregisterQuietly(phoneListener);
			phoneListener = null;
			if (vibrating && vibrator != null) {
				try {
					vibrator.cancel();
				} catch (RuntimeException exception) {
					日志输出.warning("电话组件停止振动失败：%s", exception.getMessage());
				}
			}

			vibrating = false;
			if (removeListener) {
				activity.removeOnDestroyListener(destroyListener);
			}

			super.销毁();
		}
	}

	/**
	 * 清理路径不能因系统注销失败而阻止其余组件释放。
	 *
	 * @param listener 待注销的监听器，允许为空。
	 */
	private void unregisterQuietly(PhoneStateListener listener) {
		if (listener != null && telephony != null) {
			try {
				telephony.listen(listener, PhoneStateListener.LISTEN_NONE);
			} catch (RuntimeException exception) {
				日志输出.warning("电话组件注销监听失败：%s", exception.getMessage());
			}
		}
	}

	/**
	 * 使用号码专用 URI 编码，避免井号等字符被当作 URI 片段。
	 *
	 * @param action 系统电话动作。
	 * @param phoneNumber 已整理的号码。
	 */
	private void launchPhoneIntent(String action, String phoneNumber) {
		try {
			activity.startActivity(new Intent(action, Uri.fromParts("tel", phoneNumber, null)));
		} catch (RuntimeException exception) {
			throw operationError("打开系统电话界面失败", exception);
		}
	}

	/**
	 * 检查组件生命周期，拒绝销毁后的新操作。
	 */
	private void requireAlive() {
		synchronized (lifecycle) {
			if (destroyed) {
				throw new PhoneOperationError("电话组件已销毁");
			}
		}
	}

	/**
	 * 检查设备确实提供电话服务。
	 */
	private void requirePhone() {
		requireAlive();
		if (!可用()) {
			throw new PhoneOperationError("设备不支持电话功能");
		}
	}

	/**
	 * 通话记录权限自 API 16 提供，不通过旧版联系人权限绕过。
	 *
	 * @param permission 通话记录操作所需权限。
	 */
	private void requireCallLog(String permission) {
		requireAlive();
		if (Build.VERSION.SDK_INT < 16) {
			throw new PhoneOperationError("通话记录操作需要 Android 4.1 或更高版本");
		}

		requirePermission(permission);
	}

	/**
	 * 查询系统当前实际授权状态，不缓存权限结果。
	 *
	 * @param permission Android 权限名称。
	 * @return 权限已授予时返回真。
	 */
	private boolean hasPermission(String permission) {
		return activity.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
	}

	/**
	 * 缺少权限时产生可由 Simple 错误处理捕获的异常。
	 *
	 * @param permission Android 权限名称。
	 */
	private void requirePermission(String permission) {
		if (!hasPermission(permission)) {
			throw new PhoneOperationError("未授予权限：" + permission);
		}
	}

	/**
	 * 将平台运行时异常转换为 Simple 可处理的电话操作错误。
	 *
	 * @param message 操作说明。
	 * @param cause 原始系统异常。
	 * @return 保留原始原因的运行错误。
	 */
	private static PhoneOperationError operationError(String message, RuntimeException cause) {
		PhoneOperationError error = new PhoneOperationError(message + "：" + cause.getMessage());
		error.initCause(cause);
		return error;
	}

	/**
	 * 隔离 API 26 才存在的类和方法，避免旧系统提前解析。
	 */
	@TargetApi(26)
	private static final class Api26 {

		/**
		 * 按系统默认强度执行单次振动。
		 *
		 * @param vibrator 系统振动服务。
		 * @param duration 持续毫秒数，必须大于零。
		 */
		static void vibrate(Vibrator vibrator, int duration) {
			vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
		}

		/**
		 * 查询指定卡槽，不把卡槽索引当作订阅编号。
		 *
		 * @param telephony 系统电话服务。
		 * @param slotIndex 从 0 开始的卡槽索引。
		 * @return 系统 SIM 卡状态。
		 */
		static int getSimState(TelephonyManager telephony, int slotIndex) {
			return telephony.getSimState(slotIndex);
		}
	}

	/**
	 * 电话功能的系统拒绝与不可用状态，供 Simple 错误处理捕获。
	 */
	@SuppressWarnings("serial")
	private static final class PhoneOperationError extends 运行错误 {

		/**
		 * 创建电话操作错误。
		 *
		 * @param message 用户可读的错误说明。
		 */
		PhoneOperationError(String message) {
			super(message);
		}
	}
}

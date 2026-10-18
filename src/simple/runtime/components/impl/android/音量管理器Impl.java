package simple.runtime.components.impl.android;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;

import simple.runtime.android.MainActivity;
import simple.runtime.components.组件容器;
import simple.runtime.components.音量管理器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.errors.运行错误;

/**
 * Android 系统音量管理，保持五种公开类型与系统音频流之间的固定映射。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class 音量管理器Impl extends 组件Impl implements 音量管理器 {

	// 仅持有系统音频服务，不创建监听器或改变音频路由。
	private final AudioManager audioManager;

	/**
	 * 创建不可视组件并取得系统音频服务。
	 *
	 * @param container 容纳此组件的窗口。
	 */
	public 音量管理器Impl(组件容器 container) {
		super(container);
		audioManager = (AudioManager) MainActivity.getContext().getSystemService(Context.AUDIO_SERVICE);
	}

	/* 组件方法 */

	@Override
	public int 取音量(int type) {
		try {
			return requireAudioManager().getStreamVolume(toStreamType(type));
		} catch (RuntimeException exception) {
			throw operationError("读取音量失败", exception);
		}
	}

	@Override
	public int 取最大音量(int type) {
		try {
			return requireAudioManager().getStreamMaxVolume(toStreamType(type));
		} catch (RuntimeException exception) {
			throw operationError("读取最大音量失败", exception);
		}
	}

	@Override
	public void 置音量(int type, int volume) {
		try {
			// 不传显示面板或播放提示音标志，实际档位仍由系统音频策略决定。
			requireAudioManager().setStreamVolume(toStreamType(type), volume, 0);
		} catch (RuntimeException exception) {
			throw operationError("设置音量失败；请检查音量范围、系统限制及通知策略访问状态", exception);
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean 有线耳机是否连接() {
		try {
			AudioManager manager = requireAudioManager();

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				return Api23.hasWiredHeadset(manager);
			}

			return manager.isWiredHeadsetOn();
		} catch (RuntimeException exception) {
			throw operationError("检测有线耳机失败", exception);
		}
	}

	/* 内部辅助方法 */

	/**
	 * 保留公开类型 1 至 5 的映射，其它值回退到系统提示音。
	 *
	 * @param type 组件公开的音量类型。
	 * @return Android 音频流常量。
	 */
	private static int toStreamType(int type) {
		switch (type) {
			case 音量_通话:
				return AudioManager.STREAM_VOICE_CALL;
			case 音量_铃声:
				return AudioManager.STREAM_RING;
			case 音量_媒体:
				return AudioManager.STREAM_MUSIC;
			case 音量_闹钟:
				return AudioManager.STREAM_ALARM;
			default:
				return AudioManager.STREAM_SYSTEM;
		}
	}

	/**
	 * 拒绝缺少音频服务的设备，不用零音量或未连接伪装查询成功。
	 *
	 * @return 可用的系统音频服务。
	 */
	private AudioManager requireAudioManager() {
		if (audioManager == null) {
			throw new IllegalStateException("当前设备未提供系统音频服务");
		}

		return audioManager;
	}

	/**
	 * 将系统拒绝或服务故障转换为 Simple 能捕获的运行错误，并保留原始原因。
	 *
	 * @param message 操作及限制说明。
	 * @param cause 原始系统异常。
	 * @return 可由 Simple 错误处理接住的运行错误。
	 */
	private static VolumeOperationError operationError(String message, RuntimeException cause) {
		VolumeOperationError error = new VolumeOperationError(message + "：" + cause.getMessage());
		error.initCause(cause);
		return error;
	}

	/**
	 * 隔离 API 23 的设备枚举类型，使旧系统不提前解析新 API。
	 */
	@TargetApi(Build.VERSION_CODES.M)
	private static final class Api23 {

		/**
		 * 从输出设备中识别有线耳机，不改变任何音频路由。
		 *
		 * @param manager 系统音频服务。
		 * @return 存在有线耳机或明确标识的 USB 耳机时返回真。
		 */
		static boolean hasWiredHeadset(AudioManager manager) {
			for (AudioDeviceInfo device : manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
				int type = device.getType();

				// USB 耳机常量在 API 26 加入，常量值在编译时内联，旧系统不会调用新方法。
				if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET
						|| type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
						|| type == AudioDeviceInfo.TYPE_USB_HEADSET) {
					return true;
				}
			}

			return false;
		}
	}

	/**
	 * 将 Java 系统异常纳入既有 Simple 错误处理，不新增公开错误类型。
	 */
	@SuppressWarnings("serial")
	private static final class VolumeOperationError extends 运行错误 {

		/**
		 * 创建带操作说明的运行错误。
		 *
		 * @param message 供调试使用的错误信息。
		 */
		VolumeOperationError(String message) {
			super(message);
		}
	}
}

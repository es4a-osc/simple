package simple.runtime.components.impl.android;

import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import simple.runtime.android.MainActivity;
import simple.runtime.components.指纹验证;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;

/**
 * 在主线程管理单次指纹请求，隔离 API 23 类型并丢弃过期回调。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class 指纹验证Impl extends 组件Impl implements 指纹验证 {

	private static final int RESULT_SUCCESS = 0;
	private static final int RESULT_FAILED = 1;
	private static final int RESULT_HELP = 2;
	private static final int RESULT_ERROR = 3;
	private static final int ERROR_CANCELED = 5;

	private final MainActivity activity;
	private final Handler handler;
	private final MainActivity.OnPauseListener pauseListener;
	private final MainActivity.OnResumeListener resumeListener;
	private final MainActivity.OnDestroyListener destroyListener;
	private long sequence;
	private long activeRequest;
	private Object cancellation;
	private boolean paused;
	private boolean destroyed;
	private String lastError = "";

	/**
	 * 绑定宿主生命周期；构造时不访问指纹硬件，也不开始验证。
	 *
	 * @param container 所属组件容器。
	 */
	public 指纹验证Impl(组件容器 container) {
		super(container);
		activity = MainActivity.getContext();
		handler = new Handler(Looper.getMainLooper());
		pauseListener = new MainActivity.OnPauseListener() {
			@Override
			public void onPause() {
				paused = true;
				cancelCurrent(true);
			}
		};
		resumeListener = new MainActivity.OnResumeListener() {
			@Override
			public void onResume() {
				paused = false;
			}
		};
		destroyListener = new MainActivity.OnDestroyListener() {
			@Override
			public void onDestroy() {
				release();
			}
		};
		activity.addOnPauseListener(pauseListener);
		activity.addOnResumeListener(resumeListener);
		activity.addOnDestroyListener(destroyListener);
	}

	/* 事件 */

	@Override
	public void 验证成功() {
		EventDispatcher.dispatchEvent(this, "验证成功");
	}

	@Override
	public void 验证未通过() {
		EventDispatcher.dispatchEvent(this, "验证未通过");
	}

	@Override
	public void 验证提示(int code, String message) {
		EventDispatcher.dispatchEvent(this, "验证提示", code, message);
	}

	@Override
	public void 验证取消() {
		EventDispatcher.dispatchEvent(this, "验证取消");
	}

	@Override
	public void 验证错误(int code, String message) {
		EventDispatcher.dispatchEvent(this, "验证错误", code, message);
	}

	/* 属性 */

	@Override
	public boolean 正在验证() {
		requireMainThread();
		return activeRequest != 0;
	}

	@Override
	public String 错误信息() {
		requireMainThread();
		return lastError;
	}

	/* 方法 */

	@Override
	public int 取可用状态() {
		requireMainThread();
		if (destroyed) {
			return 状态_已销毁;
		}
		if (Build.VERSION.SDK_INT < 23) {
			return 状态_系统不支持;
		}
		try {
			return Api23.status(activity);
		} catch (SecurityException failure) {
			return 状态_权限不足;
		} catch (RuntimeException failure) {
			return 状态_暂不可用;
		}
	}

	@Override
	public boolean 开始验证() {
		requireMainThread();
		if (destroyed) {
			lastError = statusMessage(状态_已销毁);
			return false;
		}
		if (activeRequest != 0) {
			lastError = "已有验证正在进行。";
			return false;
		}
		if (paused) {
			lastError = "应用不在前台，不能开始指纹验证。";
			return false;
		}
		int status = 取可用状态();
		if (status != 状态_可用) {
			lastError = statusMessage(status);
			return false;
		}
		// 请求先登记再交给系统，所有结果延后到消息队列，避免开始方法中的事件重入。
		long request = ++sequence;
		try {
			cancellation = Api23.newCancellation();
			activeRequest = request;
			lastError = "";
			Api23.authenticate(this, request, cancellation);
			return true;
		} catch (RuntimeException failure) {
			activeRequest = 0;
			Object signal = cancellation;
			cancellation = null;
			cancelSignal(signal);
			lastError = errorMessage(failure);
			return false;
		}
	}

	@Override
	public void 取消验证() {
		requireMainThread();
		cancelCurrent(true);
	}

	@Override
	public boolean 打开设置() {
		requireMainThread();
		if (destroyed || Build.VERSION.SDK_INT < 23) {
			lastError = statusMessage(destroyed ? 状态_已销毁 : 状态_系统不支持);
			return false;
		}
		try {
			activity.startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
			lastError = "";
			return true;
		} catch (RuntimeException failure) {
			lastError = errorMessage(failure);
			return false;
		}
	}

	@Override
	public void 销毁() {
		requireMainThread();
		release();
	}

	/* 请求与生命周期 */

	/**
	 * 投递原始系统回调；派发时核对请求归属，旧请求不能结束或通知新请求。
	 *
	 * @param request 回调所属请求。
	 * @param result 结果种类。
	 * @param code 系统代码。
	 * @param message 系统说明，允许为空。
	 */
	private void postResult(final long request, final int result, final int code, final CharSequence message) {
		final String text = message == null ? "" : message.toString();
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (destroyed || activeRequest != request) {
					return;
				}
				if (result == RESULT_FAILED) {
					验证未通过();
				} else if (result == RESULT_HELP) {
					验证提示(code, text);
				} else {
					activeRequest = 0;
					cancellation = null;
					if (result == RESULT_SUCCESS) {
						lastError = "";
						验证成功();
					} else if (code == ERROR_CANCELED) {
						lastError = "";
						验证取消();
					} else {
						lastError = text.length() == 0 ? "指纹验证错误：" + code : text;
						验证错误(code, lastError);
					}
				}
			}
		});
	}

	/**
	 * 先使请求失效，再取消硬件扫描；取消事件允许调用者立即发起新请求。
	 *
	 * @param notify 是否报告取消事件。
	 */
	private void cancelCurrent(boolean notify) {
		if (activeRequest == 0) {
			return;
		}
		activeRequest = 0;
		Object signal = cancellation;
		cancellation = null;
		lastError = "";
		cancelSignal(signal);
		if (notify && !destroyed) {
			验证取消();
		}
	}

	/**
	 * 尽力取消系统请求，失败时保留说明；调用者已经使对应回调失效。
	 *
	 * @param signal API 23 分支创建的取消对象，允许为空。
	 */
	private void cancelSignal(Object signal) {
		if (signal != null && Build.VERSION.SDK_INT >= 23) {
			try {
				Api23.cancel(signal);
			} catch (RuntimeException failure) {
				lastError = errorMessage(failure);
			}
		}
	}

	/**
	 * 终止请求并释放生命周期监听；销毁过程中不派发终止事件。
	 */
	private void release() {
		if (destroyed) {
			return;
		}
		destroyed = true;
		cancelCurrent(false);
		handler.removeCallbacksAndMessages(null);
		// 取消事件可能在宿主遍历监听器时重入销毁，延后移除以免修改正在遍历的集合。
		handler.post(new Runnable() {
			@Override
			public void run() {
				activity.removeOnPauseListener(pauseListener);
				activity.removeOnResumeListener(resumeListener);
				activity.removeOnDestroyListener(destroyListener);
			}
		});
		super.销毁();
	}

	/**
	 * 拒绝后台线程直接操作组件状态。
	 *
	 * @throws IllegalStateException 当前不是主线程。
	 */
	private static void requireMainThread() {
		if (Looper.myLooper() != Looper.getMainLooper()) {
			throw new IllegalStateException("指纹验证组件必须在主线程调用。");
		}
	}

	/**
	 * 返回未满足验证条件的中文说明。
	 *
	 * @param status 可用状态常量。
	 * @return 状态说明。
	 */
	private static String statusMessage(int status) {
		switch (status) {
			case 状态_系统不支持: return "指纹验证需要 Android 6.0（API 23）或以上。";
			case 状态_无指纹硬件: return "设备没有可用的指纹硬件。";
			case 状态_未设置锁屏: return "请先设置安全锁屏。";
			case 状态_未录入指纹: return "请先在系统安全设置中录入指纹。";
			case 状态_权限不足: return "应用没有 USE_FINGERPRINT 权限。";
			case 状态_已销毁: return "指纹验证组件已经销毁。";
			default: return "指纹服务暂不可用。";
		}
	}

	/**
	 * 为无说明的系统异常补充可读文本。
	 *
	 * @param failure 操作异常。
	 * @return 错误说明。
	 */
	private static String errorMessage(RuntimeException failure) {
		String message = failure.getMessage();
		return message == null || message.length() == 0 ? failure.getClass().getSimpleName() : message;
	}

	/**
	 * API 23 类型只在此分支的字段、签名及字节码中出现，低版本不进入该分支。
	 */
	@TargetApi(23)
	private static final class Api23 {

		/**
		 * 检查指纹硬件与录入状态，系统临时锁定由验证回调报告。
		 *
		 * @param activity 宿主 Activity。
		 * @return 组件可用状态常量。
		 */
		static int status(MainActivity activity) {
			if (activity.checkSelfPermission("android.permission.USE_FINGERPRINT") != PackageManager.PERMISSION_GRANTED) {
				return 状态_权限不足;
			}
			FingerprintManager manager = (FingerprintManager) activity.getSystemService(Context.FINGERPRINT_SERVICE);
			if (manager == null || !manager.isHardwareDetected()) {
				return 状态_无指纹硬件;
			}
			KeyguardManager keyguard = (KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE);
			if (keyguard == null || !keyguard.isKeyguardSecure()) {
				return 状态_未设置锁屏;
			}
			return manager.hasEnrolledFingerprints() ? 状态_可用 : 状态_未录入指纹;
		}

		/**
		 * 创建不暴露 API 类型的取消对象。
		 *
		 * @return 系统取消对象。
		 */
		static Object newCancellation() {
			return new CancellationSignal();
		}

		/**
		 * 使用指纹传感器验证；未绑定加密操作，不提供密钥解锁或数据加解密功能。
		 *
		 * @param owner 组件实例。
		 * @param request 所属请求编号。
		 * @param cancellation 系统取消对象。
		 */
		static void authenticate(final 指纹验证Impl owner, final long request, Object cancellation) {
			FingerprintManager manager = (FingerprintManager) owner.activity.getSystemService(Context.FINGERPRINT_SERVICE);
			if (manager == null) {
				throw new IllegalStateException("指纹服务暂不可用。");
			}
			manager.authenticate(null, (CancellationSignal) cancellation, 0,
				new FingerprintManager.AuthenticationCallback() {
					@Override
					public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
						owner.postResult(request, RESULT_SUCCESS, 0, "");
					}

					@Override
					public void onAuthenticationFailed() {
						owner.postResult(request, RESULT_FAILED, 0, "");
					}

					@Override
					public void onAuthenticationHelp(int code, CharSequence message) {
						owner.postResult(request, RESULT_HELP, code, message);
					}

					@Override
					public void onAuthenticationError(int code, CharSequence message) {
						owner.postResult(request, RESULT_ERROR, code, message);
					}
				}, owner.handler);
		}

		/**
		 * 取消已创建的系统请求。
		 *
		 * @param cancellation 系统取消对象。
		 */
		static void cancel(Object cancellation) {
			((CancellationSignal) cancellation).cancel();
		}
	}
}

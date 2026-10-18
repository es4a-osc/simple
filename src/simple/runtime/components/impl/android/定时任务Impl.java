package simple.runtime.components.impl.android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;

import simple.runtime.android.MainActivity;
import simple.runtime.components.定时任务;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.errors.运行错误;

/**
 * 使用独立 PendingIntent 标识系统任务，不依赖窗口实例或进程内任务表。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class 定时任务Impl extends 组件Impl implements 定时任务 {

	private final Context context;
	private final AlarmManager alarmManager;

	/**
	 * 创建组件，只持有应用上下文和系统闹钟服务。
	 *
	 * @param container 容纳此组件的窗口。
	 */
	public 定时任务Impl(组件容器 container) {
		super(container);
		context = MainActivity.getContext().getApplicationContext();
		alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	}

	/* 组件方法 */

	/**
	 * 设置不带文本参数的单次服务任务。
	 *
	 * <p>直接委托完整接口，保持校验、任务身份及调度行为一致。</p>
	 *
	 * @param taskId 非负任务编号。
	 * @param serviceName 本应用已声明服务的完整限定名。
	 * @param delayMillis 首次执行延迟，单位毫秒，不能为负；0 表示尽快执行。
	 */
	@Override
	public void 设置服务任务(int taskId, String serviceName, long delayMillis) {
		设置服务任务(taskId, serviceName, delayMillis, 0, "");
	}

	/**
	 * 设置携带文本参数的单次服务任务。
	 *
	 * <p>直接委托完整接口，保持校验、任务身份及调度行为一致。</p>
	 *
	 * @param taskId 非负任务编号。
	 * @param serviceName 本应用已声明服务的完整限定名。
	 * @param delayMillis 首次执行延迟，单位毫秒，不能为负；0 表示尽快执行。
	 * @param parameter 传递给目标的文本，null 按空文本处理。
	 */
	@Override
	public void 设置服务任务(int taskId, String serviceName, long delayMillis, String parameter) {
		设置服务任务(taskId, serviceName, delayMillis, 0, parameter);
	}

	/**
	 * 设置或覆盖单次、重复服务任务，并将业务参数交给目标服务。
	 *
	 * <p>先校验本应用的服务声明，再更新令牌；正常返回仅表示已提交系统调度。</p>
	 *
	 * @param taskId 非负任务编号。
	 * @param serviceName 本应用已声明服务的完整限定名。
	 * @param delayMillis 首次执行延迟，单位毫秒，不能为负；0 表示尽快执行。
	 * @param intervalMillis 重复间隔，单位毫秒；0 表示单次，正数表示非精确重复。
	 * @param parameter 传递给目标的文本，null 按空文本处理。
	 */
	@Override
	public void 设置服务任务(int taskId, String serviceName, long delayMillis,
		long intervalMillis, String parameter) {
		try {
			long triggerTime = triggerTime(taskId, delayMillis, intervalMillis);
			Intent intent = serviceIntent(taskId, serviceName);

			// 先检查本应用的服务声明，错误名称不能覆盖已有任务。
			context.getPackageManager().getServiceInfo(intent.getComponent(), 0);
			putParameters(intent, taskId, parameter);
			PendingIntent operation = PendingIntent.getService(context, taskId, intent,
				PendingIntent.FLAG_UPDATE_CURRENT | immutableFlag());
			schedule(triggerTime, intervalMillis, operation);
		} catch (PackageManager.NameNotFoundException exception) {
			throw operationError("服务未在本应用清单中声明：" + serviceName, exception);
		} catch (RuntimeException exception) {
			throw operationError("设置服务任务失败", exception);
		}
	}

	/**
	 * 设置不带文本参数的单次应用启动任务。
	 *
	 * <p>直接委托完整接口，保持校验、任务身份及调度行为一致。</p>
	 *
	 * @param taskId 非负任务编号。
	 * @param delayMillis 首次执行延迟，单位毫秒，不能为负；0 表示尽快执行。
	 */
	@Override
	public void 设置启动任务(int taskId, long delayMillis) {
		设置启动任务(taskId, delayMillis, 0, "");
	}

	/**
	 * 设置携带文本参数的单次应用启动任务。
	 *
	 * <p>直接委托完整接口，保持校验、任务身份及调度行为一致。</p>
	 *
	 * @param taskId 非负任务编号。
	 * @param delayMillis 首次执行延迟，单位毫秒，不能为负；0 表示尽快执行。
	 * @param parameter 传递给目标的文本，null 按空文本处理。
	 */
	@Override
	public void 设置启动任务(int taskId, long delayMillis, String parameter) {
		设置启动任务(taskId, delayMillis, 0, parameter);
	}

	/**
	 * 设置或覆盖单次、重复应用启动任务，并保留原有主界面复用规则。
	 *
	 * <p>启动参数通过程序被启动事件传递，界面能否从后台显示仍由系统决定。</p>
	 *
	 * @param taskId 非负任务编号。
	 * @param delayMillis 首次执行延迟，单位毫秒，不能为负；0 表示尽快执行。
	 * @param intervalMillis 重复间隔，单位毫秒；0 表示单次，正数表示非精确重复。
	 * @param parameter 传递给目标的文本，null 按空文本处理。
	 */
	@Override
	public void 设置启动任务(int taskId, long delayMillis, long intervalMillis, String parameter) {
		try {
			long triggerTime = triggerTime(taskId, delayMillis, intervalMillis);
			Intent intent = applicationIntent(taskId);
			putParameters(intent, taskId, parameter);

			// 复用已有主 Activity，由其分发程序被启动事件，避免重复窗口实例。
			PendingIntent operation = PendingIntent.getActivity(context, taskId, intent,
				PendingIntent.FLAG_UPDATE_CURRENT | immutableFlag());
			schedule(triggerTime, intervalMillis, operation);
		} catch (RuntimeException exception) {
			throw operationError("设置启动任务失败", exception);
		}
	}

	/**
	 * 根据服务限定名和编号恢复任务令牌，取消尚未派发的调用。
	 *
	 * <p>不存在时不创建新令牌，也不要求原组件实例或原进程仍存在。</p>
	 *
	 * @param taskId 非负任务编号。
	 * @param serviceName 设置任务时使用的服务完整限定名。
	 */
	@Override
	public void 取消服务任务(int taskId, String serviceName) {
		try {
			Intent intent = serviceIntent(taskId, serviceName);
			PendingIntent operation = PendingIntent.getService(context, taskId, intent,
				PendingIntent.FLAG_NO_CREATE | immutableFlag());
			cancel(operation);
		} catch (RuntimeException exception) {
			throw operationError("取消服务任务失败", exception);
		}
	}

	/**
	 * 根据编号恢复应用启动令牌并取消任务，重复取消不报错。
	 *
	 * @param taskId 非负任务编号。
	 */
	@Override
	public void 取消启动任务(int taskId) {
		try {
			PendingIntent operation = PendingIntent.getActivity(context, taskId, applicationIntent(taskId),
				PendingIntent.FLAG_NO_CREATE | immutableFlag());
			cancel(operation);
		} catch (RuntimeException exception) {
			throw operationError("取消启动任务失败", exception);
		}
	}

	/* 调度与任务身份 */

	/**
	 * 使用设备运行时间计算首次触发点，避免用户修改日期导致相对延迟漂移。
	 *
	 * @param taskId 任务编号。
	 * @param delayMillis 首次延迟。
	 * @param intervalMillis 重复间隔。
	 * @return 基于 elapsedRealtime 的首次触发时间。
	 */
	private static long triggerTime(int taskId, long delayMillis, long intervalMillis) {
		checkTaskId(taskId);
		long now = SystemClock.elapsedRealtime();
		if (delayMillis < 0 || intervalMillis < 0 || delayMillis > Long.MAX_VALUE - now) {
			throw new IllegalArgumentException("延迟和间隔必须非负，首次触发时间不能溢出");
		}
		return now + delayMillis;
	}

	/**
	 * 单次和重复分别调用对应系统接口，不把时间窗口误作循环周期。
	 *
	 * @param triggerTime 首次触发时间。
	 * @param intervalMillis 重复间隔，0 表示单次。
	 * @param operation 到期时交给系统执行的调用。
	 */
	private void schedule(long triggerTime, long intervalMillis, PendingIntent operation) {
		if (intervalMillis == 0) {
			alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, operation);
		} else {
			alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				triggerTime, intervalMillis, operation);
		}
	}

	/**
	 * 通过组件和编号定位原 PendingIntent，不依赖组件字段或历史进程。
	 *
	 * @param operation 已有任务令牌；不存在时为 null。
	 */
	private void cancel(PendingIntent operation) {
		if (operation != null) {
			alarmManager.cancel(operation);
			operation.cancel();
		}
	}

	/**
	 * 构造只能发往本应用服务的显式意图。
	 *
	 * @param taskId 任务编号。
	 * @param serviceName 服务完整限定名。
	 * @return 不带业务附加值的意图。
	 */
	private Intent serviceIntent(int taskId, String serviceName) {
		if (serviceName == null || serviceName.trim().length() == 0) {
			throw new IllegalArgumentException("服务限定名不能为空");
		}
		return baseIntent(taskId).setComponent(new ComponentName(context.getPackageName(), serviceName));
	}

	/**
	 * 构造主 Activity 的显式意图，已打开时通过 onNewIntent 接收参数。
	 *
	 * @param taskId 任务编号。
	 * @return 应用启动意图。
	 */
	private Intent applicationIntent(int taskId) {
		return baseIntent(taskId).setClass(context, MainActivity.class).addFlags(
			Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
	}

	/**
	 * 附加值不参与 Intent 身份比较，使用数据 URI 隔离不同编号。
	 *
	 * @param taskId 任务编号。
	 * @return 带稳定身份的意图。
	 */
	private Intent baseIntent(int taskId) {
		checkTaskId(taskId);
		Uri data = new Uri.Builder().scheme("simple-task").authority(context.getPackageName())
			.appendPath(Integer.toString(taskId)).build();
		return new Intent("simple.runtime.TIMER_TASK").setData(data);
	}

	/**
	 * 写入服务处理和程序被启动事件共同使用的参数。
	 *
	 * @param intent 目标意图。
	 * @param taskId 任务编号。
	 * @param parameter 文本参数。
	 */
	private static void putParameters(Intent intent, int taskId, String parameter) {
		intent.putExtra("任务编号", taskId);
		intent.putExtra("参数", parameter == null ? "" : parameter);
	}

	/**
	 * 校验设置和取消方法共享的任务编号约定。
	 *
	 * @param taskId 任务编号。
	 */
	private static void checkTaskId(int taskId) {
		if (taskId < 0) {
			throw new IllegalArgumentException("任务编号不能为负");
		}
	}

	/**
	 * 新平台使用不可变令牌，旧平台保留可识别的原有标志。
	 *
	 * @return 当前平台支持的不可变标志。
	 */
	private static int immutableFlag() {
		return Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;
	}

	/**
	 * 转为 Simple 可捕获的错误，同时保留具体原因供日志诊断。
	 *
	 * @param message 操作说明。
	 * @param cause 原始异常。
	 * @return 可进入 Simple 错误分支的运行错误。
	 */
	private static 运行错误 operationError(String message, Exception cause) {
		运行错误 error = new 运行错误(message + "：" + cause.getMessage()) {
			private static final long serialVersionUID = 1L;
		};
		error.initCause(cause);
		return error;
	}
}

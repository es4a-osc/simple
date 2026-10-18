package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleDataElement;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

/**
 * 将单次或重复任务交给 Android 系统调度的不可视组件。
 *
 * <p>任务使用非精确定时，可能受系统批处理、省电及后台启动限制而延后。
 * 不需要精确闹钟权限，不保证保活、强制停止后执行或设备重启后恢复。
 * 窗口销毁不取消任务，需要显式调用对应取消方法。</p>
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 定时任务 extends 组件 {

	/* 时间常量，单位为毫秒 */

	/** 十五分钟。 */
	@SimpleDataElement
	long 十五分钟 = 900000L;

	/** 半个小时。 */
	@SimpleDataElement
	long 半个小时 = 1800000L;

	/** 一个小时。 */
	@SimpleDataElement
	long 一个小时 = 3600000L;

	/** 半天。 */
	@SimpleDataElement
	long 半天 = 43200000L;

	/** 一天。 */
	@SimpleDataElement
	long 一天 = 86400000L;

	/* 组件方法 */

	/**
	 * 设置不带文本参数的单次服务任务。
	 *
	 * <p>使用完整接口的校验与调度规则，重复间隔固定为 0；省略参数时传递空文本。</p>
	 *
	 * @param taskId 非负任务编号。
	 * @param serviceName 本应用已声明服务的完整限定名。
	 * @param delayMillis 首次执行延迟，单位毫秒，不能为负；0 表示尽快执行。
	 */
	@SimpleFunction
	void 设置服务任务(int taskId, String serviceName, long delayMillis);

	/**
	 * 设置携带文本参数的单次服务任务。
	 *
	 * <p>使用完整接口的校验与调度规则，重复间隔固定为 0；省略参数时传递空文本。</p>
	 *
	 * @param taskId 非负任务编号。
	 * @param serviceName 本应用已声明服务的完整限定名。
	 * @param delayMillis 首次执行延迟，单位毫秒，不能为负；0 表示尽快执行。
	 * @param parameter 传递给目标的文本，null 按空文本处理。
	 */
	@SimpleFunction
	void 设置服务任务(int taskId, String serviceName, long delayMillis, String parameter);

	/**
	 * 设置或覆盖发往本应用已声明服务的任务。
	 *
	 * <p>同一服务、同一编号的任务相互覆盖，不同服务或编号可以并存。
	 * 服务通过处理意图的“任务编号”和“参数”附加值读取数据。
	 * 服务单元必须随项目编译并声明在应用清单中，不能指定其它应用的服务。
	 * 正常返回仅表示系统已接受调度，实际执行仍受后台服务限制。</p>
	 *
	 * @param taskId 非负任务编号。
	 * @param serviceName 服务单元的完整限定名。
	 * @param delayMillis 首次执行的延迟，单位毫秒，不能为负；0 表示尽快执行。
	 * @param intervalMillis 重复间隔，单位毫秒；0 表示仅执行一次，正数表示非精确重复。
	 * @param parameter 传给服务的文本，null 按空文本处理。
	 */
	@SimpleFunction
	void 设置服务任务(int taskId, String serviceName, long delayMillis,
		long intervalMillis, String parameter);

	/**
	 * 设置不带文本参数的单次应用启动任务。
	 *
	 * <p>使用完整接口的校验与调度规则，重复间隔固定为 0；省略参数时传递空文本。</p>
	 *
	 * @param taskId 非负任务编号。
	 * @param delayMillis 首次执行延迟，单位毫秒，不能为负；0 表示尽快执行。
	 */
	@SimpleFunction
	void 设置启动任务(int taskId, long delayMillis);

	/**
	 * 设置携带文本参数的单次应用启动任务。
	 *
	 * <p>使用完整接口的校验与调度规则，重复间隔固定为 0；省略参数时传递空文本。</p>
	 *
	 * @param taskId 非负任务编号。
	 * @param delayMillis 首次执行延迟，单位毫秒，不能为负；0 表示尽快执行。
	 * @param parameter 传递给目标的文本，null 按空文本处理。
	 */
	@SimpleFunction
	void 设置启动任务(int taskId, long delayMillis, String parameter);

	/**
	 * 设置或覆盖启动本应用主 Activity 的任务。
	 *
	 * <p>相同编号的启动任务相互覆盖，与服务任务分别标识。当前窗口通过
	 * “程序被启动”事件中的意图读取“任务编号”和“参数”。
	 * 正常返回不代表界面已启动，系统可能禁止应用从后台弹出界面。</p>
	 *
	 * @param taskId 非负任务编号。
	 * @param delayMillis 首次执行的延迟，单位毫秒，不能为负；0 表示尽快执行。
	 * @param intervalMillis 重复间隔，单位毫秒；0 表示仅执行一次，正数表示非精确重复。
	 * @param parameter 传给应用的文本，null 按空文本处理。
	 */
	@SimpleFunction
	void 设置启动任务(int taskId, long delayMillis, long intervalMillis, String parameter);

	/**
	 * 取消指定服务和编号的任务，重复取消或任务不存在时不报错。
	 *
	 * <p>取消会撤销尚未派发的调用，不中断已经开始处理的服务。
	 * 只要目标名称和编号一致，组件或应用进程重新创建后仍可取消。</p>
	 *
	 * @param taskId 非负任务编号。
	 * @param serviceName 设置任务时使用的服务单元完整限定名。
	 */
	@SimpleFunction
	void 取消服务任务(int taskId, String serviceName);

	/**
	 * 取消指定编号的应用启动任务，重复取消或任务不存在时不报错。
	 *
	 * @param taskId 非负任务编号。
	 */
	@SimpleFunction
	void 取消启动任务(int taskId);
}

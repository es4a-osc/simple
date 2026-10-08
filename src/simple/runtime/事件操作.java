package simple.runtime;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.components.组件;
import simple.runtime.errors.非法参数错误;
import simple.runtime.events.EventDispatcher;
import simple.runtime.variants.Variant;

import java.util.ArrayList;

/**
 * 组件之间事件转移相关功能。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 事件操作 {

	/**
	 * 取当前发生事件的对象。
	 *
	 * @return 返回事件对象
	 */
	@SimpleFunction
	public static 组件 取事件组件() {
		return (组件) EventDispatcher.getEventObject();
	}

	/**
	 * 将第一个组件的事件转移第二个组件上，注意组件类型必须一致。
	 *
	 * @param sourceComponent 被转移的组件
	 * @param targetComponent 触发事件的组件
	 * @throws 非法参数错误 组件为空或组件类型不一致
	 */
	@SimpleFunction
	public static void 事件转移(组件 sourceComponent, 组件 targetComponent) {
		// 不可为空，必须为实例
		if (sourceComponent == null || targetComponent == null) {
			throw new IllegalArgumentException("组件不可为空");
		}

		// 类型必须一致
		if (!sourceComponent.getClass().equals(targetComponent.getClass())) {
			throw new IllegalArgumentException("组件类型不一致");
		}

		EventDispatcher.transferEvent(sourceComponent, targetComponent);
	}

	/**
	 * 动态执行指定组件的事件。
	 *
	 * @param component 欲执行的已实例化组件
	 * @param name 事件名称
	 * @throws 非法参数错误 组件为空
	 */
	@SimpleFunction
	public static void 执行事件(组件 component, String name) {
		if (component == null) {
			throw new IllegalArgumentException("组件不可为空");
		}

		EventDispatcher.dispatchEvent(component, name);
	}

	/**
	 * 动态执行指定组件的事件。
	 *
	 * @param component 欲执行的已实例化组件
	 * @param name 事件名称
	 * @param param 事件参数
	 * @throws 非法参数错误 组件为空
	 */
	@SimpleFunction
	public static void 执行事件(组件 component, String name, Variant[] params) {
		// 不可为空，必须为实例
		if (component == null) {
			throw new IllegalArgumentException("组件不可为空");
		}

		// 转换参数
		ArrayList<Object> args = new ArrayList<Object>();
		for (Variant value : params) {
			args.add(value.getObject());
		}

		EventDispatcher.dispatchEvent(component, name, args.toArray());
	}
}

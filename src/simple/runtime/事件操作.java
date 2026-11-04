package simple.runtime;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.components.组件;
import simple.runtime.errors.非法参数错误;
import simple.runtime.events.EventDispatcher;
import simple.runtime.variants.Variant;

/**
 * 组件之间事件转移相关功能。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 事件操作 {

	/**
	 * 静态方法集，不创建实例。
	 */
	private 事件操作() {
	}

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
	 * <p>注意：目标组件请勿再事件转移
	 * 
	 * @param source 被转移的组件
	 * @param target 触发事件的组件
	 * @throws 非法参数错误 组件为空或组件类型不一致
	 */
	@SimpleFunction
	public static void 事件转移(组件 source, 组件 target) {
		EventDispatcher.transferEvent(source, target);
	}

	/**
	 * 动态执行指定组件的事件。
	 *
	 * <p>注意：未支持含传址参数的事件
	 * 
	 * @param component 欲执行的已实例化组件
	 * @param name 事件名称
	 * @throws 非法参数错误 组件为空
	 */
	@SimpleFunction
	public static void 执行事件(组件 component, String name) {
		EventDispatcher.executeEvent(component, name, null);
	}

	/**
	 * 按组件的事件声明准备参数，并动态执行该事件。
	 *
	 * <p>注意：未支持含传址参数的事件
	 * 
	 * @param component 欲执行的已实例化组件
	 * @param name 事件名称
	 * @param params 依声明顺序排列的变体参数，空引用表示无参数
	 * @throws 非法参数错误 参数数量与事件声明不符
	 */
	@SimpleFunction
	public static void 执行事件(组件 component, String name, Variant[] params) {
		EventDispatcher.executeEvent(component, name, params);
	}
}

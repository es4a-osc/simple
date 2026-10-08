package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.collections.意图;

/**
 * 广播接收器组件。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public abstract interface 广播接收器 extends 组件 {

	/**
	 * 收到广播事件默认处理方法。
	 *
	 * @param action 广播动作。
	 * @param intent 意图实例。
	 */
	@SimpleEvent
	void 收到广播(String action, 意图 intent);

	/**
	 * 返回已经注册的动作数量。
	 *
	 * @return 动作数量。
	 */
	@SimpleProperty
	int 动作计数();

	/**
	 * 返回所有已经注册的动作。
	 *
	 * @return 按注册顺序排列的动作数组。
	 */
	@SimpleProperty
	String[] 所有动作();

	/**
	 * 注册一个广播动作；注册后立即开始接收该动作的广播。
	 *
	 * @param action 广播动作。
	 */
	@SimpleFunction
	void 注册动作(String action);

	/**
	 * 注销一个广播动作。
	 *
	 * @param action 广播动作。
	 */
	@SimpleFunction
	void 注销动作(String action);

	/**
	 * 判断是否已经注册指定动作。
	 *
	 * @param action 广播动作。
	 * @return 已经注册返回{@code true}，否则返回{@code false}。
	 */
	@SimpleFunction
	boolean 具有动作(String action);
}

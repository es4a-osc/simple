package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.variants.Variant;

/**
 * 可以跨线程传递消息的组件。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 消息传递器 extends 组件 {
	/**
	 * 收到消息。
	 *
	 * @param what 消息标记值。
	 * @param payload 消息载荷值。
	 */
	@SimpleEvent
	void 收到消息(int what, Variant payload);

	/**
	 * 发送消息。
	 *
	 * @param what 消息标记值。
	 * @param payload 消息载荷值，支持任意数据类型。
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	boolean 发送消息(int what, Variant payload);

	/**
	 * 发送消息。
	 *
	 * @param what 消息标记值。
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	boolean 发送消息(int what);
}

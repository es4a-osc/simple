package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleEvent;

/**
 * 非完美版，后期会优化
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 垂直滑块条 extends 进度组件 {

	/* 组件事件 */

	/**
	 * 通知开始拖动事件。
	 */
	@SimpleEvent
	void 开始拖动();

	/**
	 * 通知停止拖动事件。
	 */
	@SimpleEvent
	void 停止拖动();

	/**
	 * 通知位置被改变事件。
	 *
	 * @param progress 当前进度值
	 */
	@SimpleEvent
	void 位置被改变(int progress);
}

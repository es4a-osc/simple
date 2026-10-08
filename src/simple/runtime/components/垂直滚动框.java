package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.SimpleFunction;

/**
 * 垂直滚动框
 *
 * <p>注意该容器组件仅可容纳一个可视组件（如 面板）。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 垂直滚动框 extends 可视组件, 组件容器 {

	/**
	 * 被滚动事件事件处理方法。
	 *
	 * @param 滚动距离 新的滚动值（以像素为单位）。
	 * @param 是否到边 是否滚动到边界。
	 */
	@SimpleEvent
	void 被滚动(int 滚动距离, boolean 是否到边);

	/**
	 * 启用滚动条属性获取方法。
	 *
	 * @return 是否启用滚动条。
	 */
	@SimpleProperty
	boolean 启用滚动条();

	/**
	 * 启用滚动条属性设置方法。
	 *
	 * @param 是否启用 是否启用滚动条。
	 */
	@SimpleProperty
	void 启用滚动条(boolean 是否启用);

	/**
	 * 最大滚动值属性设置方法。
	 *
	 * @return 最大滚动值。
	 */
	@SimpleProperty
	int 最大滚动值();

	/**
	 * 滚动的指点位置。
	 *
	 * @param 位置
	 */
	@SimpleFunction
	void 滚动(int 位置);

	/**
	 * 滚动到最顶边。
	 */
	@SimpleFunction
	void 滚动到顶();

	/**
	 * 滚动到最底边。
	 */
	@SimpleFunction
	void 滚动到底();
}

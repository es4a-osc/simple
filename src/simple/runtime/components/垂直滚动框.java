package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.SimpleFunction;

/**
 * 提供垂直滚动的组件容器，只能直接包含一个可视组件，通常放入面板。
 *
 * <p>尺寸与位置在布局完成后确定；调用滚动方法会排队执行，排队期间只保留最后一次目标。
 * 组件销毁后忽略滚动请求，不再派发滚动事件。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 垂直滚动框 extends 可视组件, 组件容器 {

	/* 组件事件 */

	/**
	 * 垂直滚动位置实际改变时触发，手势和程序滚动都会通知。
	 *
	 * @param 滚动距离 当前相对最顶边的像素偏移，不是本次移动距离；回弹时可能短暂超出正常范围
	 * @param 是否到边 到达或越过顶边或底边时为真
	 */
	@SimpleEvent
	void 被滚动(int 滚动距离, boolean 是否到边);

	/* 组件属性 */

	/**
	 * 判断是否启用垂直滚动条。
	 *
	 * @return 启用时为真，不代表滚动条此刻可见
	 */
	@SimpleProperty
	boolean 启用滚动条();

	/**
	 * 设置是否启用垂直滚动条，不影响内容能否滚动。
	 *
	 * @param 是否启用 是否允许显示滚动条
	 */
	@SimpleProperty
	void 启用滚动条(boolean 是否启用);

	/**
	 * 返回内容可滚动到的最大垂直偏移。
	 *
	 * @return 最大像素偏移；没有内容、尚未布局或内容未超出视口时为 0，内容或容器尺寸变化后重新计算
	 */
	@SimpleProperty
	int 最大滚动值();

	/* 组件方法 */

	/**
	 * 平滑滚动到相对最顶边的指定位置，布局完成后执行。
	 *
	 * @param 位置 目标像素偏移；小于 0 按 0 处理，超出内容时限制到最大滚动值
	 */
	@SimpleFunction
	void 滚动(int 位置);

	/**
	 * 平滑滚动到最顶边，布局完成后执行。
	 */
	@SimpleFunction
	void 滚动到顶();

	/**
	 * 平滑滚动到最底边，使用执行时的实际内容尺寸。
	 */
	@SimpleFunction
	void 滚动到底();
}

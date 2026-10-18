package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;

/**
 * 读取环境照度的非可见传感器组件，照度单位为勒克斯（lx）。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
@SimpleComponent
public interface 光线传感器 extends 传感器组件 {

	/* 组件事件 */

	/**
	 * 取得有效环境照度时触发，单位为 lx。
	 *
	 * @param light 照度，单位为勒克斯（lx）。
	 */
	@SimpleEvent
	void 光线改变(float light);

	/* 组件属性 */

	/**
	 * 设备是否具有光线传感器。
	 *
	 * @return 当前“是否有效”状态
	 */
	@SimpleProperty
	boolean 是否有效();
}

package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;

/**
 * 检测物体与设备之间距离的非可见组件。距离单位为厘米；部分设备只提供靠近和离开两种读数。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
@SimpleComponent
public interface 距离传感器 extends 传感器组件 {

	/* 组件事件 */

	/**
	 * 首次取得有效读数或距离变化时触发。
	 */
	@SimpleEvent
	void 距离改变(float distance);

	/**
	 * 收到小于最小距离的读数时触发；相同读数也可重复触发。
	 */
	@SimpleEvent
	void 物体靠近();

	/**
	 * 收到大于或等于最小距离的读数时触发；相同读数也可重复触发。
	 */
	@SimpleEvent
	void 物体离开();

	/* 组件属性 */

	/**
	 * 最近一次有效距离，单位为厘米；尚未取得读数时为零。
	 */
	@SimpleProperty
	float 距离();

	/**
	 * 是否正在监听距离。保留原有可读写名称“可用”，关闭监听或设备不支持时为假。
	 */
	@SimpleProperty
	boolean 可用();

	/**
	 * 启停距离监听；设备不支持或监听注册失败时不能启用。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "True"
	)
	void 可用(boolean enable);

	/**
	 * 靠近与离开的判断阈值，单位为厘米。
	 */
	@SimpleProperty
	int 最小距离();

	/**
	 * 设置非负距离阈值，默认一厘米。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER,
		initializer = "1"
	)
	void 最小距离(int distance);

	/**
	 * Android 采样参数：0～3 为系统采样档位，其它正整数为微秒周期，不保证实际采样频率。
	 */
	@SimpleProperty
	int 检测间隔();

	/**
	 * 设置非负采样参数，默认 3（正常档位）；关闭监听时只保存参数。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER,
		initializer = "3"
	)
	void 检测间隔(int interval);
}

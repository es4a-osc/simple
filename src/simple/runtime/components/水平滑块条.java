package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.SimpleEvent;

/**
 * 水平滑块条组件
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 水平滑块条 extends 进度组件 {

	@SimpleEvent
	void 开始拖动();

	@SimpleEvent
	void 停止拖动();

	@SimpleEvent
	void 位置被改变(int progress);

	/**
	 * 最大高度属性获取方法。
	 *
	 * @return 最大高度。
	 */
	@SimpleProperty
	int 最大高度();

	/**
	 * 最大高度属性设置方法。
	 *
	 * @param maxHeight 最大高度。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER
	)
	void 最大高度(int maxHeight);

	/**
	 * 最小高度属性获取方法。
	 *
	 * @return 最小高度。
	 */
	@SimpleProperty
	int 最小高度();

	/**
	 * 最小高度属性设置方法。
	 *
	 * @param minHeight 最小高度。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER
	)
	void 最小高度(int minHeight);

	/**
	 * 滑块图片属性设置方法。
	 *
	 * @param image 图片路径。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_ASSET
	)
	void 滑块图片(String image);
}

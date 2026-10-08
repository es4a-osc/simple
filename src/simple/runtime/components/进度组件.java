package simple.runtime.components;

import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;

/**
 * 进度组件。
 *
 * @author 树先生 xhwd@qq.com
 */
@SimpleObject
public interface 进度组件 extends 可视组件 {

	/**
	 * 最大位置属性获取方法。
	 *
	 * @return 最大位置
	 */
	@SimpleProperty
	int 最大位置();

	/**
	 * 最大位置属性设置方法。
	 *
	 * @param 最大位置
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER,
		initializer = "100"
	)
	void 最大位置(int max);

	/**
	 * 位置属性获取方法。
	 *
	 * @return 位置。
	 */
	@SimpleProperty
	int 位置();

	/**
	 * 位置属性设置方法。
	 *
	 * @param progress 位置。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER,
		initializer = "0"
	)
	void 位置(int progress);

	/**
	 * 进度图片属性设置方法。
	 *
	 * @param image 图片路径。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_ASSET
	)
	void 进度图片(String image);
}

package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;

/**
 * 水平进度条组件。
 *
 * @author 树先生 xhwd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 水平进度条 extends 进度组件 {

	/**
	 * 循环模式属性获取方法。
	 *
	 * @return 是否无限循环；{@code true}为无限循环，否则为{@code false}。
	 */
	@SimpleProperty
	boolean 循环模式();

	/**
	 * 循环模式属性设置方法。
	 *
	 * @param indeterminate 是否无限循环；{@code true}为无限循环，否则为{@code false}。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "False"
	)
	void 循环模式(boolean indeterminate);

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
}

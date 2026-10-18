package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;

/**
 * 下拉列表框
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 下拉列表框 extends 可视组件 {

	/* 组件事件 */

	/**
	 * 用户选择列表项目时触发。
	 *
	 * @param index 项目索引，从 0 开始。
	 */
	@SimpleEvent
	void 项目被选择(int index);

	/* 组件属性 */

	/**
	 * 读取当前选中项目的索引。
	 *
	 * @return 选中项目的索引；没有选中项时为 -1。
	 */
	@SimpleProperty
	int 现行选中项();

	/**
	 * 选择指定索引的列表项目。
	 *
	 * @param index 项目索引，从 0 开始。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER,
		initializer = "0"
	)
	void 现行选中项(int index);

	/* 组件方法 */

	/**
	 * 返回列表中的项目总数。
	 *
	 * @return 列表项目总数。
	 */
	@SimpleFunction
	int 取项目数();

	/**
	 * 删除指定索引的项目并刷新显示。
	 *
	 * @param index 项目索引，从 0 开始。
	 */
	@SimpleFunction
	void 删除项目(int index);

	/**
	 * 移除列表中的全部项目并刷新显示。
	 */
	@SimpleFunction
	void 清空项目();

	/**
	 * 在列表末尾追加项目并刷新显示。
	 *
	 * @param text 新的项目文本。
	 * @return 新项目的索引，从 0 开始。
	 */
	@SimpleFunction
	int 添加项目(String text);

	/**
	 * 在指定索引前插入项目并刷新显示。
	 *
	 * @param index 项目索引，从 0 开始。
	 * @param text 新的项目文本。
	 */
	@SimpleFunction
	void 插入项目(int index, String text);

	/**
	 * 读取指定索引的项目文本。
	 *
	 * @param index 项目索引，从 0 开始。
	 * @return 指定项目的文本。
	 */
	@SimpleFunction
	String 取项目文本(int index);

	/**
	 * 替换指定索引的项目文本并刷新显示。
	 *
	 * @param index 项目索引，从 0 开始。
	 * @param text 新的项目文本。
	 */
	@SimpleFunction
	void 置项目文本(int index, String text);
}

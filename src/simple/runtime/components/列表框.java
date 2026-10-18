package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

/**
 * Simple的列表框组件
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 列表框 extends 列表组件 {

	/* 组件事件 */

	/**
	 * 用户单击列表项目时触发。
	 *
	 * @param index 项目索引，从 0 开始。
	 */
	@SimpleEvent
	void 项目被单击(int index);

	/**
	 * 用户长按列表项目时触发。
	 *
	 * @param index 项目索引，从 0 开始。
	 */
	@SimpleEvent
	void 项目被长按(int index);

	/**
	 * 列表滚动时报告当前可见范围。
	 *
	 * @param firstVisibleItem 第一个可见项目索引
	 * @param visibleItemCount 可见项目数
	 * @param totalItemCount 项目总数
	 */
	@SimpleEvent
	void 项目被滚动(int firstVisibleItem, int visibleItemCount, int totalItemCount);

	/* 组件方法 */

	/**
	 * 请求让列表获得输入焦点。
	 *
	 * @return 成功获得焦点时为真，否则为假。
	 */
	@SimpleFunction
	boolean 获取焦点();

	/**
	 * 清除列表当前的输入焦点。
	 */
	@SimpleFunction
	void 清除焦点();

	/**
	 * 按列表视图高度的指定百分比垂直滚动。
	 *
	 * @param x 垂直位移占视图高度的百分比，正值向下、负值向上。
	 */
	@SimpleFunction
	void 垂直滚动(int x);

	/**
	 * 返回列表中的项目总数。
	 *
	 * @return 列表项目总数。
	 */
	@SimpleFunction
	int 取项目数();

	/**
	 * 将指定项目滚动至选择位置。
	 *
	 * @param index 项目索引，从 0 开始。
	 */
	@SimpleFunction
	void 选择项目(int index);

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

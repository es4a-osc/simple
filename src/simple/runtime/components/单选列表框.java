package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

/**
 * 单选列表框组件。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 单选列表框 extends 列表组件 {
	@SimpleEvent
	void 项目被单击(int index);

	@SimpleEvent
	void 项目被长按(int index);

	@SimpleEvent
	void 项目被滚动(int firstVisibleItem, int visibleItemCount, int totalItemCount);

	@SimpleFunction
	boolean 获取焦点();

	@SimpleFunction
	void 清除焦点();

	@SimpleFunction
	void 垂直滚动(int x);

	@SimpleFunction
	int 取项目数();

	@SimpleFunction
	void 选择项目(int index);

	@SimpleFunction
	void 删除项目(int index);

	@SimpleFunction
	void 清空项目();

	@SimpleFunction
	int 添加项目(String text);

	@SimpleFunction
	void 插入项目(int index, String text);

	@SimpleFunction
	String 取项目文本(int index);

	@SimpleFunction
	void 置项目文本(int index, String value);

	@SimpleFunction
	boolean 取项目选中(int index);

	@SimpleFunction
	void 置项目选中(int index, boolean value);

	@SimpleFunction
	int 取选中项目();
}

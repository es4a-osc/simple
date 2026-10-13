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

	@SimpleEvent
	void 项目被选择(int index);

	/* 组件属性 */

	@SimpleProperty
	int 现行选中项();

	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER,
		initializer = "0"
	)
	void 现行选中项(int index);

	/* 组件方法 */

	@SimpleFunction
	int 取项目数();

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
	void 置项目文本(int index, String text);
}

package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;

/**
 * 分组列表框组件。
 * 
 * @author 小刀(Siyu1840)
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 分组列表框 extends 列表组件 {
	@SimpleEvent
	void 子项被单击(int groupid, int childid);

	@SimpleEvent
	void 子项被长按(int groupid, int childid);

	@SimpleEvent
	void 子项按钮被单击(int groupid, int childid);

	@SimpleEvent
	void 分组被展开(int groupid);

	@SimpleEvent
	void 分组被收起(int groupid);

	@SimpleEvent
	void 分组被单击(int groupid);

	@SimpleFunction
	boolean 获取焦点();

	@SimpleFunction
	void 清除焦点();

	@SimpleProperty
	int 分组标题字体大小();

	@SimpleProperty(
		type = "simple.integer",
		initializer = "14"
	)
	void 分组标题字体大小(int size);

	@SimpleProperty
	int 分组标题字体颜色();

	@SimpleProperty(
		type = "simple.color",
		initializer = "-16777216"
	)
	void 分组标题字体颜色(int argb);

	@SimpleProperty
	int 分组信息字体大小();

	@SimpleProperty(
		type = "simple.integer",
		initializer = "12"
	)
	void 分组信息字体大小(int size);

	@SimpleProperty
	int 分组信息字体颜色();

	@SimpleProperty(
		type = "simple.color",
		initializer = "-7829368"
	)
	void 分组信息字体颜色(int argb);

	@SimpleProperty
	int 子项标题字体大小();

	@SimpleProperty(
		type = "simple.integer",
		initializer = "14"
	)
	void 子项标题字体大小(int size);

	@SimpleProperty
	int 子项标题字体颜色();

	@SimpleProperty(
		type = "simple.color",
		initializer = "-16777216"
	)
	void 子项标题字体颜色(int argb);

	@SimpleProperty
	int 子项信息字体大小();

	@SimpleProperty(
		type = "simple.integer",
		initializer = "12"
	)
	void 子项信息字体大小(int size);

	@SimpleProperty
	int 子项信息字体颜色();

	@SimpleProperty(
		type = "simple.color",
		initializer = "-7829368"
	)
	void 子项信息字体颜色(int argb);

	@SimpleProperty
	int 子项按钮字体大小();

	@SimpleProperty(
		type = "simple.integer",
		initializer = "12"
	)
	void 子项按钮字体大小(int size);

	@SimpleProperty
	int 子项按钮字体颜色();

	@SimpleProperty(
		type = "simple.color",
		initializer = "-16777216"
	)
	void 子项按钮字体颜色(int argb);

	@SimpleProperty
	String 背景图片();

	@SimpleProperty(
		type = "simple.asset",
		initializer = "\"\""
	)
	void 背景图片(String imagePath);

	@SimpleProperty
	int 背景图片2();

	@SimpleProperty
	void 背景图片2(int id);

	@SimpleProperty
	boolean 显示子项按钮();

	@SimpleProperty(
		type = "simple.boolean",
		initializer = "True"
	)
	void 显示子项按钮(boolean value);

	@SimpleFunction
	int 添加分组(String title, String info);

	@SimpleFunction
	int 添加分组(String title);

	@SimpleFunction
	String 取分组标记(int position);

	@SimpleFunction
	void 置分组标记(int position, String tag);

	@SimpleFunction
	String 取分组标题(int groupid);

	@SimpleFunction
	void 置分组标题(int groupid, String title);

	@SimpleFunction
	String 取分组信息(int groupid);

	@SimpleFunction
	void 置分组信息(int groupid, String info);

	@SimpleFunction
	int 取分组总数();

	@SimpleFunction
	void 展开分组(int groupid);

	@SimpleFunction
	void 收起分组(int groupid);

	@SimpleFunction
	boolean 取分组状态(int groupid);

	@SimpleFunction
	void 选中分组(int groupPosition);

	@SimpleFunction
	void 删除分组(int groupid);

	@SimpleFunction
	void 清空所有数据();

	@SimpleFunction
	void 添加子项(int groupid, String image, String title, String info, String buttonimage, String buttontitle);

	@SimpleFunction
	void 添加子项(int groupid, String title);

	@SimpleFunction
	String 取子项标记(int groupid, int childid);

	@SimpleFunction
	void 置子项标记(int groupid, int childid, String tag);

	@SimpleFunction
	String 取子项图片(int groupid, int childid);

	@SimpleFunction
	void 置子项图片(int groupid, int childid, String image);

	@SimpleFunction
	String 取子项标题(int groupid, int childid);

	@SimpleFunction
	void 置子项标题(int groupid, int childid, String title);

	@SimpleFunction
	String 取子项信息(int groupid, int childid);

	@SimpleFunction
	void 置子项信息(int groupid, int childid, String info);

	@SimpleFunction
	String 取子项按钮图片(int groupid, int childid);

	@SimpleFunction
	void 置子项按钮图片(int groupid, int childid, String buttonimage);

	@SimpleFunction
	String 取子项按钮标题(int groupid, int childid);

	@SimpleFunction
	void 置子项按钮标题(int groupid, int childid, String buttontitle);

	@SimpleFunction
	int 取子项总数(int groupid);

	@SimpleFunction
	void 删除子项(int groupid, int childid);

	@SimpleFunction
	void 清空子项(int groupid);

	@SimpleFunction
	void 选中子项(int groupPosition, int childPosition, boolean shouldExpandGroup);
}

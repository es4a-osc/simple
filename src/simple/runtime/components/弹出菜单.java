package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;

/**
 * 以可视组件为锚点显示动态菜单或 XML 菜单资源的非可视组件。
 *
 * <p>动态菜单项以标识访问，0 表示顶层或无分组。菜单资源模式与动态模式互斥，
 * XML 项标识由资源定义；动态项标识由添加方法返回。组件操作应在界面主线程执行。</p>
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 弹出菜单 extends 组件 {

	/* 组件事件 */

	/**
	 * 用户选中菜单项时触发；展开父项时系统也可能先回调该父项标识。
	 *
	 * @param itemId 动态菜单项标识或 XML 菜单项资源标识。
	 */
	@SimpleEvent
	void 被选中(int itemId);

	/**
	 * 菜单因选择、返回或点击菜单外部而关闭时触发；组件销毁时不派发。
	 */
	@SimpleEvent
	void 被关闭();

	/* 组件属性 */

	/**
	 * 返回当前菜单资源路径。
	 *
	 * @return 菜单资源路径，动态菜单模式返回空文本。
	 */
	@SimpleProperty
	String 菜单项();

	/**
	 * 设置 XML 菜单资源，成功后清空动态项目；空文本切换为动态菜单模式。
	 *
	 * @param menuRes 菜单资源路径，格式为 {@code @menu/资源名}；空引用按空文本处理。
	 * @throws simple.runtime.errors.文件未存在错误 非空菜单资源路径无效或资源不存在。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_STRING,
		initializer = "\"\""
	)
	void 菜单项(String menuRes);

	/* 动态菜单项 */

	/**
	 * 添加顶层菜单项并切换为动态菜单模式，已有 XML 菜单不会转成动态项目。
	 *
	 * @param title 菜单项标题，不得为空引用。
	 * @return 当前组件内唯一的正整数标识；删除或清空后不复用。
	 * @throws IllegalArgumentException 标题为空引用。
	 */
	@SimpleFunction
	int 添加(String title);

	/**
	 * 删除指定动态菜单项及其所有子项，不存在的标识不作处理。
	 *
	 * @param itemId 动态菜单项标识。
	 */
	@SimpleFunction
	void 删除(int itemId);

	/**
	 * 清空动态项目，不改变当前菜单资源路径。
	 */
	@SimpleFunction
	void 清空();

	/**
	 * 读取动态菜单项的分组。
	 *
	 * @param itemId 动态菜单项标识。
	 * @return 分组标识，0 表示未分组。
	 * @throws simple.runtime.errors.索引超出界限错误 菜单项不存在。
	 */
	@SimpleFunction
	int 取分组(int itemId);

	/**
	 * 设置动态菜单项的分组，同级且同组的项目由系统菜单处理。
	 *
	 * @param itemId 动态菜单项标识。
	 * @param groupId 分组标识，0 表示未分组。
	 * @throws simple.runtime.errors.索引超出界限错误 菜单项不存在。
	 */
	@SimpleFunction
	void 置分组(int itemId, int groupId);

	/**
	 * 读取动态菜单项的排序值。
	 *
	 * @param itemId 动态菜单项标识。
	 * @return Android 菜单排序值，默认为 0。
	 * @throws simple.runtime.errors.索引超出界限错误 菜单项不存在。
	 */
	@SimpleFunction
	int 取排序(int itemId);

	/**
	 * 设置动态菜单项排序值，相同排序值保持添加次序。
	 *
	 * @param itemId 动态菜单项标识。
	 * @param order Android 菜单排序值，简单排序可使用非负小整数。
	 * @throws simple.runtime.errors.索引超出界限错误 菜单项不存在。
	 */
	@SimpleFunction
	void 置排序(int itemId, int order);

	/**
	 * 读取动态菜单项的父项。
	 *
	 * @param itemId 动态菜单项标识。
	 * @return 父项标识，0 表示顶层菜单项。
	 * @throws simple.runtime.errors.索引超出界限错误 菜单项不存在。
	 */
	@SimpleFunction
	int 取父项(int itemId);

	/**
	 * 设置动态菜单项的父项，拒绝自身或后代作为父项，避免形成循环。
	 *
	 * @param itemId 动态菜单项标识。
	 * @param parentId 父项标识，0 表示移至顶层。
	 * @throws simple.runtime.errors.索引超出界限错误 项目或非零父项不存在。
	 * @throws IllegalArgumentException 父子关系会形成循环。
	 */
	@SimpleFunction
	void 置父项(int itemId, int parentId);

	/* 菜单显示 */

	/**
	 * 以已经附着到窗口的可视组件为锚点弹出菜单，重复调用先关闭旧菜单。
	 *
	 * <p>显示时读取菜单数据，后续数据修改在下一次弹出时生效。
	 * 子菜单的展开方式由 Android 系统决定。空菜单不弹出，也不触发关闭事件。</p>
	 *
	 * @param visibleComponent 锚点可视组件，不得为空或属于其他 Activity。
	 * @throws IllegalArgumentException 锚点不是当前运行库支持的视图组件。
	 * @throws IllegalStateException 组件已销毁、Activity 正在退出或锚点尚未附着到窗口。
	 */
	@SimpleFunction
	void 弹出(可视组件 visibleComponent);
}

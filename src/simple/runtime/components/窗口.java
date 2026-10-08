/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.collections.意图;
import simple.runtime.parameters.BooleanReferenceParameter;
import simple.runtime.variants.Variant;

/**
 * 根组件容器。所有其他组件需要直接或间接添加到窗口中。
 *
 * @author Herbert Czymontek
 */
@SimpleComponent
@SimpleObject
public interface 窗口 extends 可视组件, 组件容器 {
	/**
	关联代码：
	{@link simple.compiler.parser.Parser#parseFormPropertiesSection() parseFormPropertiesSection}
	*/

	// 定义事件

	/**
	 * 程序被启动事件默认处理程序：当程序被启动时触发。
	 *
	 * <p>所有实例窗口都会触发该事件。
	 *
	 * @param intent 启动意图。
	 */
	@SimpleEvent
	void 程序被启动(意图 intent);

	/**
	 * 程序被恢复事件默认处理程序：活动从后台切换到前台触发该事件。
	 *
	 * <p>所有实例窗口都会触发该事件。
	 */
	@SimpleEvent
	void 程序被恢复();

	/**
	 * 程序被停止事件默认处理程序：活动从前台换到到后台触发。
	 *
	 * <p>所有实例窗口都会触发该事件。
	 */
	@SimpleEvent
	void 程序被停止();

	/**
	 * 程序被销毁事件默认处理程序：当程序被销毁时触发。
	 *
	 * <p>所有实例窗口都会触发该事件。
	 */
	@SimpleEvent
	void 程序被销毁();

	/**
	 * 屏幕方向改变事件默认处理程序：当屏幕方向改变时触发。
	 *
	 * <p>所有实例窗口都会触发该事件。
	 *
	 * @param orientation 屏幕方向
	 */
	@SimpleEvent
	void 屏幕方向改变(int orientation);

	/**
	 * 即将切换事件默认处理程序：即将切换到其它窗口时触发。
	 */
	@SimpleEvent
	void 即将切换();

	/**
	 * 切换完毕事件默认处理程序：切换到当前窗口完毕后触发。
	 */
	@SimpleEvent
	void 切换完毕();

	/**
	 * 反馈消息事件默认处理程序：当收到反馈消息时触发。
	 *
	 * @param mark 标记
	 * @param param 参数
	 */
	@SimpleEvent
	void 反馈消息(Variant mark, Variant param);

	/**
	 * 默认键盘事件处理器。
	 *
	 * @param keycode 常数标识按下键 (@see 组件)
	 * @param shield 是否已处理该事件
	 */
	@SimpleEvent
	void 按下某键(int keycode, BooleanReferenceParameter shield);

	/**
	 * 默认触摸手势事件处理器。
	 *
	 * @param direction  常数标识触摸手势方向
	 *                   (@see 组件)
	 */
	@SimpleEvent
	void 触摸手势(int direction);

	/**
	 * 默认菜单事件处理器。
	 *
	 * @param caption  字符串标识所选菜单项
	 */
	@SimpleEvent
	void 选中菜单(String caption);

	// 定义属性

	/**
	 * 背景图片属性设置器方法：为窗口设置背景图片。
	 *
	 * @param imagePath 路径和图片名称
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_ASSET,
		initializer = "\"\""
	)
	void 背景图片(String imagePath);

	/**
	 * 布局属性获取器。
	 *
	 * @return 布局实例
	 */
	@SimpleProperty
	Variant 布局();

	/**
	 * 布局属性设置器：我们为将常量转换为布局对象实例的属性为属性分配一个布局。
	 *
	 * @param layoutType 其中之一{@link 组件#布局_线性}，或
	 *                    {@link 组件#布局_表格}，或
	 *                    {@link 组件#布局_单帧}
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_LAYOUT,
		initializer = 组件.布局_线性 + ""
	)
	void 布局(Variant layoutType);

	/**
	 * 指示窗口的内容是否可滚动。
	 *
	 * @return {@code true}表示可滚动，否则为{@code false}
	 */
	@SimpleProperty
	boolean 滚动();

	/**
	 * 使窗口的内容可滚动（或不滚动）。
	 *
	 * @param scrollable {@code true}表示可滚动，否则为{@code false}
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "False"
	)
	void 滚动(boolean scrollable);

	/**
	 * 标题属性获取器方法。
	 *
	 * @return  窗口标题
	 */
	@SimpleProperty
	String 标题();

	/**
	 * 标题属性设置器方法：在窗口的标题栏中为窗口设置新标题。
	 *
	 * @param title 新建窗口标题
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_STRING,
		initializer = "\"\""
	)
	void 标题(String title);

	/**
	 * 高度属性获取器方法（只读属性）。
	 *
	 * @return 布局所使用的高度属性
	 */
	@SimpleProperty
	Variant 高度();

	/**
	 * 宽度属性获取方法（只读属性）。
	 *
	 * @return 布局所使用的宽度属性
	 */
	@SimpleProperty
	Variant 宽度();

	/* 定义方法 */

	/**
	 * 将消息投递给窗口，可在线程下调用。
	 *
	 * <p>如果调用线程是UI线程，则立即执行该操作。
	 * 如果调用线程非UI线程，则将发送到UI线程的事件队列等待处理。
	 *
	 * @param mark 标记
	 * @param param 参数
	 */
	@SimpleFunction
	void 投递消息(Variant mark, Variant param);

	/**
	 * 将消息投递给窗口，可在线程下调用。
	 *
	 * <p>如果调用线程是UI线程，则立即执行该操作。
	 * 如果调用线程非UI线程，则将发送到UI线程的事件队列等待处理。
	 *
	 * @param mark 标记
	 * @param param 参数
	 */
	@SimpleFunction
	void 投递消息(Variant mark);

	/* 覆盖属性默认值 */

	@Override
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_COLOR,
		initializer = 组件.颜色_白 + ""
	)
	void 背景颜色(int color);
}

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

/**
 * Simple的按钮组件。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 按钮 extends 文本组件 {

	/* 组件 事件声明 */

	/**
	 * 默认获得焦点事件处理程序。
	 */
	@SimpleEvent
	void 获得焦点();

	/**
	 * 默认失去焦点事件处理程序。
	 */
	@SimpleEvent
	void 失去焦点();

	/**
	 * 默认按下事件处理方法。
	 *
	 * @param x 按下横向位置。
	 * @param y 按下纵向位置。
	 */
	@SimpleEvent
	void 被按下(int x, int y);

	/**
	 * 默认弹起事件处理方法。
	 *
	 * @param x 弹起横向位置。
	 * @param y 弹起纵向位置。
	 */
	@SimpleEvent
	void 被弹起(int x, int y);

	/**
	 * 默认触摸移动事件处理方法。
	 *
	 * @param lastX 移动起始横向位置。
	 * @param lastY 移动起始纵向位置。
	 * @param currX 移动起始横向位置。
	 * @param currY 移动结束纵向位置。
	 */
	@SimpleEvent
	void 触摸移动(int lastX, int lastY, int currX, int currY);

	/**
	 * 默认触摸手势事件处理方法。
	 *
	 * @param direction 不断识别触摸手势的方向（@see 组件）。
	 */
	@SimpleEvent
	void 触摸手势(int direction);

	/**
	 * 默认单击事件处理方法。
	 */
	@SimpleEvent
	void 被单击();

	/**
	 * 默认长按事件处理方法。
	 */
	@SimpleEvent
	void 被长按();

	/* 组件 属性声明 */

	/**
	 * 启用属性获取器方法。
	 *
	 * @return   {@code true}表示启用，{@code false}禁用
	 */
	@SimpleProperty
	boolean 启用();

	/**
	 * 启用属性设置器方法。
	 *
	 * @param enabled  {@code true}已启用，{@ code false}已禁用
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "True"
	)
	void 启用(boolean enabled);

	/* 组件 方法声明 */

	/**
	 * 使其获得焦点。
	 *
	 * @return 获得焦点返回{@cdoe true}，否则返回{@code false}。
	 */
	@SimpleFunction
	boolean 获取焦点();

	/**
	 * 使其清除焦点。
	 */
	@SimpleFunction
	void 清除焦点();

	/* 覆盖属性默认值 */

	@Override
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_TEXTJUSTIFICATION,
		initializer = 组件.对齐_居中 + ""
	)
	void 内容对齐(int justification);
}

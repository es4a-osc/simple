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
 * Simple画板组件。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 画板 extends 可视组件 {

	/**
	 * 默认按下事件处理方法。
	 *
	 * @param x 按下横向位置。
	 * @param y 按下纵向位置。
	 */
	@SimpleEvent
	void 被按下(int x, int y);

	/**
	 * 默认被弹起事件处理方法。
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
	 * 画笔颜色属性获取器方法。
	 *
	 * @return  画笔Alpha绘制RGB颜色。
	 */
	@SimpleProperty
	int 画笔颜色();

	/**
	 * 画笔颜色属性设置器方法。
	 *
	 * @param argb  画笔Alpha绘制RGB颜色。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_COLOR,
		initializer = 组件.颜色_黑 + ""
	)
	void 画笔颜色(int argb);

	/**
	 * 画笔粗细属性获取器方法。
	 *
	 * @return 画笔粗细值。
	 */
	@SimpleProperty
	float 画笔粗细();

	/**
	 * 画笔粗细属性设置器方法。
	 *
	 * @param size 画笔粗细值。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_SINGLE,
		initializer = 1.0F + ""
	)
	void 画笔粗细(float size);

	/**
	 * 清除画板（用背景颜色填充）。
	 */
	@SimpleFunction
	void 清空();

	/**
	 * 在画板上的给定坐标处绘制点。
	 *
	 * @param x  x坐标
	 * @param y  y坐标
	 */
	@SimpleFunction
	void 画点(int x, int y);

	/**
	 * 在画板上给定的坐标，用给定的半径画一个圆。
	 *
	 * @param x  x坐标
	 * @param y  y坐标
	 * @param r  半径
	 */
	@SimpleFunction
	void 画圆(int x, int y, float r);

	/**
	 * 在画板上给定的坐标之间绘制一条直线。
	 *
	 * @param x1  第一点的x坐标
	 * @param y1  第一点的y坐标
	 * @param x2  第二点的x坐标
	 * @param y2  第二点的y坐标
	 */
	@SimpleFunction
	void 画直线(int x1, int y1, int x2, int y2);

	// 属性默认值重写

	@Override
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_COLOR,
		initializer = 组件.颜色_白 + ""
	)
	void 背景颜色(int argb);
}

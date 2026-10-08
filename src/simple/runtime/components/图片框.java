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
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;

/**
 * 用于显示图像和动画的组件。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 图片框 extends 图片组件 {

	/**
	 * 默认按下事件处理方法。
	 *
	 * @param x 按下横向位置。
	 * @param y 按下纵向位置。
	 */
	@SimpleEvent
	void 被按下(int x, int y) ;

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
	 * 默认点击事件处理方法。
	 */
	@SimpleEvent
	void 被单击();

	/**
	 * 默认长按事件处理方法。
	 */
	@SimpleEvent
	void 被长按();

	// 属性初始值重写

	@Override
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "True"
	)
	void 保持宽高比(boolean adjustViewBounds);
}

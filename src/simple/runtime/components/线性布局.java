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

import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;

/**
 * 用于水平或垂直放置组件的线性布局。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public interface 线性布局 extends 布局 {

	/**
	 * 方向属性获取方法。
	 *
	 * @return 可选值为{@link 组件#布局_方向_水平} 或
	 * 		{@link 组件#布局_方向_垂直}
	 */
	@SimpleProperty
	int 方向();

	/**
	 * 方向属性设置器方法。
	 *
	 * @param orientation  {@link 组件#布局_方向_水平} 或
	 * 						{@link 组件#布局_方向_垂直}
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER,
		initializer = 组件.布局_方向_垂直 + ""
	)
	void 方向(int orientation);

	/**
	 * 内容对齐获取器方法：其内容相对于自身的对齐方式。
	 *
	 * @return 可选值为{@link 组件#对齐_水平居中} 或
	 * 		{@link 组件#布局_方向_垂直}
	 */
	@SimpleProperty
	int 内容对齐();

	/**
	 * 内容对齐获设置器方法：其内容相对于自身的对齐方式。
	 *
	 * @param justification  可选值为{@link 组件#对齐_水平居中} 或
	 * 		{@link 组件#布局_方向_垂直}
	 */
	@SimpleProperty
	void 内容对齐(int justification);

	/**
	 * 基线对齐属性获取方法。
	 *
	 * @return {@code true}表示基线对齐，{@code false}正常
	 */
	@SimpleProperty
	boolean 基线对齐();

	/**
	 * 基线对齐设置器方法。
	 *
	 * @param baselineAligned {@code true}表示基线对齐，{@code false}正常
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "False"
	)
	void 基线对齐(boolean baselineAligned);

	/**
	 * 取置权重总和，如未设置通过计算布局中子组件的权重总和。
	 *
	 * @return 当前权重总和
	 */
	@SimpleProperty
	float 权重总和();

	/**
	 * 设置权重总和，如未设置通过计算布局中子组件的权重总和。
	 *
	 * @param weight 欲设置的权重总和
	 */
	@SimpleProperty
	void 权重总和(float weight);
}

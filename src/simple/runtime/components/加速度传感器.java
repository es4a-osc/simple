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
 * 用于测量加速度3个维度的传感器。
 *
 * @author Herbert Czymontek
 */
// TODO: ideas - event for knocking
@SimpleComponent
@SimpleObject
public interface 加速度传感器 extends 传感器组件 {

	/**
	 * 默认加速度改变事件处理方法。
	 *
	 * @param xAccel  x轴上的加速度减去Gx
	 * @param yAccel  y轴上的加速度减去Gy
	 * @param zAccel  z轴上的加速度减去Gz
	 */
	@SimpleEvent
	void 加速度改变(float xAccel, float yAccel, float zAccel);

	/**
	 * 默认摇动事件处理器。
	 */
	@SimpleEvent
	void 摇晃();

	/**
	 * 可用属性获取器（只读属性）。
	 *
	 * @return {@code true}表示加速度计可用，{@code false}表示不可用
	 */
	@SimpleProperty
	boolean 可用();

	/**
	 * 启用属性获取器。
	 *
	 * @return {@code true}表示传感器生成事件， {@code false}表示传感器未生成事件
	 */
	@SimpleProperty
	boolean 启用();

	/**
	 * 启用属性设置器。
	 *
	 * @param enabled  {@code true}表示传感器生成事件， {@code false}表示传感器未生成事件
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "True"
	)
	void 启用(boolean enabled);

	/**
	 * X加速属性获取器（只读属性）。
	 *
	 * <p>要返回有意义的值，必须启用传感器。
	 *
	 * @return  x加速属
	 */
	@SimpleProperty
	float X加速度();

	/**
	 * Y加速属性获取器（只读属性）。
	 *
	 * <p>要返回有意义的值，必须启用传感器。
	 *
	 * @return  y加速属
	 */
	@SimpleProperty
	float Y加速度();

	/**
	 * Z加速属性获取方法（只读属性）。
	 *
	 * <p>要返回有意义的值，必须启用传感器。
	 *
	 * @return  z加速属
	 */
	@SimpleProperty
	float Z加速度();
}

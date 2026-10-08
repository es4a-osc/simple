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
 * Simple的复选框组件。
 *
 * @author Herbert Czymontek
 */
@SimpleComponent
@SimpleObject
public interface 复选框 extends 文本组件 {

	// 组件 声明

	/**
	 * 默认状态改变事件处理器。
	 */
	@SimpleEvent
	void 状态改变();

	/**
	 * 默认获得焦点事件处理器。
	 */
	@SimpleEvent
	void 获得焦点();

	/**
	 * 默认失去焦点事件处理器。
	 */
	@SimpleEvent
	void 失去焦点();

	/**
	 * 启用属性获取器方法。
	 *
	 * @return  {@code true}表示启用，{@code false}禁用
	 */
	@SimpleProperty
	boolean 启用();

	/**
	 * 启用属性设置器方法。
	 *
	 * @param enabled  {@code true}表示启用，{@code false}禁用
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "True"
	)
	void 启用(boolean enabled);

	/**
	 * 选中属性获取器方法。
	 *
	 * @return  {@code true}表示选中，{@code false}表示未选中
	 */
	@SimpleProperty
	boolean 选中();

	/**
	 * 选中属性设置器方法。
	 *
	 * @param value  {@code true}表示选中，{@code false}表示未选中
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "False"
	)
	void 选中(boolean value);
}

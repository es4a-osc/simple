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
 * 用于输入密码的文本框。
 *
 * @author Damon Kohler
 */
@SimpleComponent
@SimpleObject
public interface 密码编辑框 extends 文本组件 {

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
	 * 启用属性获取器。
	 *
	 * @return  {@code true}表示启用，{@code false}表示禁用
	 */
	@SimpleProperty
	boolean 启用();

	/**
	 * 启用属性获取器。
	 *
	 * @param enabled  {@code true}表示启用，{@code false}表示禁用
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "True"
	)
	void 启用(boolean enabled);

	/**
	 * 提示属性获取器。
	 *
	 * @return  提示文本
	 */
	@SimpleProperty
	String 提示();

	/**
	 * 提示属性设置器。
	 *
	 * @param newhint  提示文本
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_STRING,
		initializer = "\"\""
	)
	void 提示(String newhint);
}

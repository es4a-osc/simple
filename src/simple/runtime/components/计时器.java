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
 * Simple的计时器组件。
 *
 * @author Herbert Czymontek
 */
@SimpleComponent
@SimpleObject
public interface 计时器 extends 组件 {

	/**
	 * 默认计时事件处理器。
	 */
	@SimpleEvent
	void 计时();

	/**
	 * 间隔属性获取器方法。
	 *
	 * @return  计时间隔（毫秒）
	 */
	@SimpleProperty
	int 间隔();

	/**
	 * 隔属性设置器方法：设置计时事件之间的间隔。
	 *
	 * @param interval  计时间隔（毫秒）
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER,
		initializer = "1000"
	)
	void 间隔(int interval);

	/**
	 * 启用属性获取器方法。
	 *
	 * @return  {@code true}表示运行的计时器，{@code false}停止计时器
	 */
	@SimpleProperty
	boolean 启用();

	/**
	 * 启用属性设置器方法：启动或停止计时器。
	 *
	 * @param enabled  {@code true}启动计时器，{@code false}停止它
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "True"
	)
	void 启用(boolean enabled);
}

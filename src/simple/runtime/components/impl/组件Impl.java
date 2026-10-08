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

package simple.runtime.components.impl;

import simple.runtime.components.组件;
import simple.runtime.components.组件容器;
import simple.runtime.events.EventDispatcher;

/**
 * 所有Simple组件的超类。
 *
 * @author Herbert Czymontek
 */
public abstract class 组件Impl implements 组件 {

	// 组件容器
	private final 组件容器 componentContainer;

	/**
	 * 创建一个新组件。
	 *
	 * @param container  将容纳组件的容器（不得为{@code null}，因为非可见组件必须是窗口）
	 */
	protected 组件Impl(组件容器 container) {
		componentContainer = container;
	}

	/**
	 * 返回持有此组件的组件容器。
	 *
	 * @return  组件容器或{@code null}用于窗口等根组件（对于不可见的组件，窗口将被退回）
	 */
	protected 组件容器 getComponentContainer() {
		return componentContainer;
	}

	@Override
	public void 初始化() {
		EventDispatcher.dispatchEvent(this, "初始化");
	}

	@Override
	public void 销毁() {
		EventDispatcher.unregisterEvent(this);
	}
}

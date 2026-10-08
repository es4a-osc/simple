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

package simple.runtime.components.impl.android;

import simple.runtime.components.布局;

import android.view.ViewGroup;

/**
 * 布局接口提供了使用Simple组件布局的方法。
 *
 * @author Herbert Czymontek
 */
public abstract class 布局Impl implements 布局 {

	private final 视图组件容器 container;
	private final ViewGroup layoutManager;

	/**
	 * 创建一个新的布局。
	 *
	 * @param layoutManager  安卓布局管理器
	 * @param container  视图容器
	 */
	布局Impl(ViewGroup layoutManager, 视图组件容器 container) {
		this.layoutManager = layoutManager;
		this.container = container;
	}

	/**
	 * 返回与布局关联的视图组（它是一个包含布局管理器的容器）。
	 *
	 * @return  视图组
	 */
	protected final ViewGroup getLayoutManager() {
		return layoutManager;
	}

	/**
	 * 返回与此布局关联的容器。
	 *
	 * @return  组件容器
	 */
	protected final 视图组件容器 getContainer() {
		return container;
	}

	/**
	 * 将指定的组件添加到此布局。
	 *
	 * @param component  要添加的组件
	 */
	public abstract void addComponent(视图组件 component);
}

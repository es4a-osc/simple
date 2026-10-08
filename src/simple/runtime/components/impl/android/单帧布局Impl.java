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

import simple.runtime.android.MainActivity;
import simple.runtime.components.单帧布局;
import simple.runtime.components.窗口;

import android.widget.FrameLayout;

/**
 * Simple组件的单帧布局。
 *
 * @author David Foster
 * @author 树先生 xhwsd@qq.com
 */
public class 单帧布局Impl extends 布局Impl implements 单帧布局 {
	/*
	FrameLayout
	https://developer.android.google.cn/reference/android/widget/FrameLayout

	FrameLayout.LayoutParams
	https://developer.android.google.cn/reference/android/widget/FrameLayout.LayoutParams
	*/

	/**
	 * 创建一个新的单帧布局。
	 *
	 * @param container  视图容器
	 */
	单帧布局Impl(视图组件容器 container) {
		super(new FrameLayout(MainActivity.getContext()), container);
	}

	@Override
	public void addComponent(视图组件 component) {
		getLayoutManager().addView(component.getView(), new FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT,
			FrameLayout.LayoutParams.MATCH_PARENT
		));
	}

	/* 单帧布局 实现 */

	@Override
	public void 添加(窗口 form) {
		// 为什么不把 添加 方法放到 布局 类中？
		// 是因为其它布局有操作子组件属性，而如果是是子窗口将不可操作视图属性。
		// 所以单帧布局做嵌入容器就非常合适。
		addComponent((窗口Impl) form);
	}
}

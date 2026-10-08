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

import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import simple.runtime.android.MainActivity;
import simple.runtime.components.单选框;
import simple.runtime.components.组件容器;
import simple.runtime.events.EventDispatcher;

/**
 * Android实现Simple的单选框组件。
 *
 * @author Damon Kohler
 */
public final class 单选框Impl extends 文本视图组件
		implements 单选框, OnCheckedChangeListener, OnFocusChangeListener {

	/**
	 * 创建一个新的单选框组件。
	 *
	 * @param container  将容纳组件的容器（不可为{@code null}）
	 */
	public 单选框Impl(组件容器 container) {
		super(container);
	}

	@Override
	protected View createView() {
		android.widget.RadioButton view = new android.widget.RadioButton(MainActivity.getContext());

		// 监听焦点变化
		view.setOnFocusChangeListener(this);
		view.setOnCheckedChangeListener(this);

		return view;
	}

	// 单选框 实现

	@Override
	public void 状态改变() {
		EventDispatcher.dispatchEvent(this, "状态改变");
	}

	@Override
	public void 获得焦点() {
		EventDispatcher.dispatchEvent(this, "获得焦点");
	}

	@Override
	public void 失去焦点() {
		EventDispatcher.dispatchEvent(this, "失去焦点");
	}

	@Override
	public boolean 启用() {
		return getView().isEnabled();
	}

	@Override
	public void 启用(boolean enabled) {
		View view = getView();
		view.setEnabled(enabled);
		view.invalidate();
	}

	@Override
	public boolean 选中() {
		return ((android.widget.RadioButton) getView()).isChecked();
	}

	@Override
	public void 选中(boolean value) {
		android.widget.RadioButton view = (android.widget.RadioButton) getView();
		view.setChecked(value);
		view.invalidate();
	}

	// OnCheckedChangeListener 实现

	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		状态改变();
	}

	// OnFocusChangeListener 实现

	public void onFocusChange(View previouslyFocused, boolean gainFocus) {
		if (gainFocus) {
			获得焦点();
		} else {
			失去焦点();
		}
	}
}

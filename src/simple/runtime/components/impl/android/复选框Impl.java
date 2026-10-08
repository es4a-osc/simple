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
import simple.runtime.components.复选框;
import simple.runtime.components.组件容器;
import simple.runtime.events.EventDispatcher;

/**
 * Android实现的Simple复选框组件。
 *
 * @author Herbert Czymontek
 */
public final class 复选框Impl extends 文本视图组件
		implements 复选框, OnCheckedChangeListener, OnFocusChangeListener {

	/**
	 * 创建新的复选框组件。
	 *
	 * @param container  将容纳组件的容器（不得为{@code null}）。
	 */
	public 复选框Impl(组件容器 container) {
		super(container);
	}

	@Override
	protected View createView() {
		android.widget.CheckBox view = new android.widget.CheckBox(MainActivity.getContext());

		// 监听焦点改变
		view.setOnFocusChangeListener(this);
		view.setOnCheckedChangeListener(this);

		return view;
	}

	// 复选框 实现

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
		return ((android.widget.CheckBox) getView()).isChecked();
	}

	@Override
	public void 选中(boolean value) {
		android.widget.CheckBox view = (android.widget.CheckBox) getView();
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

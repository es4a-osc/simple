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
import simple.runtime.components.密码编辑框;
import simple.runtime.components.组件容器;
import simple.runtime.events.EventDispatcher;

import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.EditText;
import android.text.method.PasswordTransformationMethod;

/**
 * 用于输入密码的文本框。
 *
 * @author Damon Kohler
 */
public final class 密码编辑框Impl extends 文本视图组件
		implements 密码编辑框, OnFocusChangeListener {

	// 支持提示文本
	private String hint;

	/**
	 * 创建一个新的密码编辑框
	 *
	 * @param container  将容纳组件的容器（不得为{@code null}）
	 */
	public 密码编辑框Impl(组件容器 container) {
		super(container);
	}

	@Override
	protected View createView() {
		EditText view = new EditText(MainActivity.getContext());

		// 监听焦点变化
		view.setOnFocusChangeListener(this);

		// 添加转换方法以隐藏密码文本。
		view.setTransformationMethod(new PasswordTransformationMethod());

		return view;
	}

	/* 密码编辑框 实现 */

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
		view.setFocusable(enabled);
		view.setFocusableInTouchMode(enabled);
		view.invalidate();
	}

	@Override
	public String 提示() {
		return hint;
	}

	@Override
	public void 提示(String newhint) {
		hint = newhint;
		EditText view = (EditText) getView();
		view.setHint(hint);
		view.invalidate();
	}

	/* OnFocusChangeListener 实现 */

	@Override
	public void onFocusChange(View previouslyFocused, boolean gainFocus) {
		if (gainFocus) {
			获得焦点();
		} else {
			失去焦点();
		}
	}
}

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

import android.app.Activity;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.AutoCompleteTextView;
import simple.runtime.android.MainActivity;
import simple.runtime.components.组件容器;
import simple.runtime.components.邮箱选择器;
import simple.runtime.components.impl.android.util.EmailAddressAdapter;
import simple.runtime.events.EventDispatcher;

/**
 * 使用自动完成的文本框从联系人挑出电子邮件地址。
 *
 * @author Sharon Perl
 */
public final class 邮箱选择器Impl extends 文本视图组件
		implements 邮箱选择器, OnFocusChangeListener {

	/**
	 * 创建一个新的邮箱选择器组件。
	 *
	 * @param container  将容纳组件的容器（不得为{@code null}）
	 */
	public 邮箱选择器Impl(组件容器 container) {
		super(container);
	}

	@Override
	protected View createView() {
		Activity context = MainActivity.getContext();
		AutoCompleteTextView view = new AutoCompleteTextView(context);
		view.setAdapter(new EmailAddressAdapter(context));

		// 监听焦点变化
		view.setOnFocusChangeListener(this);

		return view;
	}

	/* 邮箱选择器 实现 */

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

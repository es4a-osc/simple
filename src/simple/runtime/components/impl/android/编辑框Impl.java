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
import simple.runtime.components.组件容器;
import simple.runtime.components.编辑框;
import simple.runtime.components.impl.android.util.EditTextUtil;
import simple.runtime.components.impl.android.util.ViewUtil;
import simple.runtime.events.EventDispatcher;
import simple.runtime.parameters.BooleanReferenceParameter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Simple文本框组件的Android实现。
 *
 * @author Herbert Czymontek
 */
public final class 编辑框Impl extends 文本视图组件 implements 编辑框 {
	// 提示文本颜色
	private int hintColor;
	// 输入模式
	private int inputModel;

	/**
	 * 创建一个新的文本框组件。
	 *
	 * @param container  将容纳组件的容器（不得为{@code null}）
	 */
	public 编辑框Impl(组件容器 container) {
		super(container);
	}

	@Override
	protected View createView() {
		// EditText https://developer.android.google.cn/reference/android/widget/EditText
		EditText view = new EditText(MainActivity.getContext());

		// 添加处理器以进行输入验证
		view.addTextChangedListener(new TextWatcher() {
			private boolean recursionGuard;
			private CharSequence beforeText;

			@Override
			public void afterTextChanged(Editable s) {
				if (!recursionGuard) {
					recursionGuard = true;
					BooleanReferenceParameter accept = new BooleanReferenceParameter(true);
					文本改变(s.toString(), accept);
					if (!accept.get()) {
						s.replace(0, s.length(), beforeText);
					}
					recursionGuard = false;
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				beforeText = ((TextView) getView()).getText();
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		});

		// 监听焦点变化
		view.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View previouslyFocused, boolean gainFocus) {
				if (gainFocus) {
					获得焦点();
				} else {
					失去焦点();
				}
			}
		});

		return view;
	}

	// 编辑框 实现

	@Override
	public void 获得焦点() {
		EventDispatcher.dispatchEvent(this, "获得焦点");
	}

	@Override
	public void 失去焦点() {
		EventDispatcher.dispatchEvent(this, "失去焦点");
	}

	@Override
	public void 文本改变(String sourceText, BooleanReferenceParameter accept) {
		EventDispatcher.dispatchEvent(this, "文本改变", sourceText, accept);
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
	public String 提示文本() {
		return EditTextUtil.getHintText((EditText) getView());
	}

	@Override
	public void 提示文本(String newhint) {
		EditTextUtil.setHintText((EditText) getView(), newhint);;
	}

	@Override
	public int 提示颜色() {
		return hintColor;
	}

	@Override
	public void 提示颜色(int color) {
		hintColor = color;
		EditTextUtil.setHintTextColor((EditText) getView(), color);
	}

	@Override
	public boolean 光标可视() {
		return EditTextUtil.isCursorVisible((EditText) getView());
	}

	@Override
	public void 光标可视(boolean visible) {
		EditTextUtil.setCursorVisible((EditText) getView(), visible);
	}

	@Override
	public int 光标位置() {
		return EditTextUtil.getSelectionStart((EditText) getView());
	}

	@Override
	public void 光标位置(int location) {
		EditTextUtil.setSelection((EditText) getView(), location);
	}

	@Override
	public int 输入模式() {
		return inputModel;
	}

	@Override
	public void 输入模式(int model) {
		inputModel = model;
		EditTextUtil.setInputModel((EditText) getView(), model);
	}

	@Override
	public boolean 获取焦点() {
		return ViewUtil.requestFocus(getView());
	}

	@Override
	public void 清除焦点() {
		ViewUtil.clearFocus(getView());
	}

	@Override
	public void 显示输入法() {
		EditTextUtil.showSoftInput((EditText) getView(), false);
	}

	@Override
	public void 隐藏输入法() {
		EditTextUtil.hideSoftInput((EditText) getView(), false);
	}

	@Override
	public void 加入文本(String text) {
		EditTextUtil.appendText((EditText) getView(), text);
	}

	@Override
	public void 插入文本(int location, String text) {
		EditTextUtil.insertText((EditText) getView(), location, text);
	}

	@Override
	public void 删除文本(int start, int stop) {
		EditTextUtil.deleteText((EditText) getView(), start, stop);
	}

	@Override
	public void 选中文本(int start, int stop) {
		EditTextUtil.setSelection((EditText) getView(), start, stop);
	}

	@Override
	public void 全选文本() {
		EditTextUtil.selectAll((EditText) getView());
	}
}

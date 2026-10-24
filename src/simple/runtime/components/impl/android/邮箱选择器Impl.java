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

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.os.Handler;
import android.os.Looper;
import simple.runtime.android.MainActivity;
import simple.runtime.components.组件容器;
import simple.runtime.components.邮箱选择器;
import simple.runtime.components.impl.android.util.EmailAddressAdapter;
import simple.runtime.events.EventDispatcher;

/**
 * 管理邮箱输入与联系人建议，按焦点、权限和宿主生命周期协调异步查询。
 *
 * @author Sharon Perl
 * @author 树先生 xhwsd@qq.com
 */
public final class 邮箱选择器Impl extends 文本视图组件 implements 邮箱选择器,
		View.OnFocusChangeListener, View.OnAttachStateChangeListener, TextWatcher,
		MainActivity.OnResumeListener, MainActivity.OnPauseListener, MainActivity.OnDestroyListener,
		MainActivity.OnRequestPermissionsResultListener {

	private final MainActivity activity;
	private final Handler handler;
	private final EmailAddressAdapter suggestions;
	private boolean initialized;
	private boolean attached;
	private volatile boolean destroyed;

	/**
	 * 在基础视图创建后设置适配器，避免在父类构造期间读取尚未初始化的字段。
	 *
	 * @param container 所属组件容器
	 */
	public 邮箱选择器Impl(组件容器 container) {
		super(container);
		super.单行模式(true);
		activity = MainActivity.getContext();
		handler = new Handler(Looper.getMainLooper());
		suggestions = new EmailAddressAdapter(activity, new EmailAddressAdapter.FailureListener() {
			@Override
			public void failed(String message) {
				if (!destroyed) {
					操作失败(message);
				}
			}
		});
		AutoCompleteTextView view = input();
		view.setAdapter(suggestions);
		view.addTextChangedListener(this);
		view.setOnFocusChangeListener(this);
		view.addOnAttachStateChangeListener(this);
		view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View itemView, int position, long id) {
				EmailAddressAdapter.Item item = suggestions.getItem(position);
				if (!destroyed && item != null) {
					选择完毕(item.name, item.address);
				}
			}
		});
	}

	@Override
	protected View createView() {
		AutoCompleteTextView view = new AutoCompleteTextView(MainActivity.getContext());
		view.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		view.setSingleLine(true);
		view.setThreshold(1);
		return view;
	}

	@Override
	public void 初始化() {
		initialized = true;
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (destroyed) {
					return;
				}
				if (activity.isFinishing() || activity.isDestroyed()) {
					销毁();
					return;
				}
				if (!attached) {
					activity.addOnResumeListener(邮箱选择器Impl.this);
					activity.addOnPauseListener(邮箱选择器Impl.this);
					activity.addOnDestroyListener(邮箱选择器Impl.this);
					activity.addOnRequestPermissionsResultListener(邮箱选择器Impl.this);
					attached = true;
				}
				refresh(true);
			}
		});
		super.初始化();
	}

	/* 组件事件 */

	@Override
	public void 获得焦点() {
		EventDispatcher.dispatchEvent(this, "获得焦点");
	}

	@Override
	public void 失去焦点() {
		EventDispatcher.dispatchEvent(this, "失去焦点");
	}

	@Override
	public void 选择完毕(String name, String address) {
		EventDispatcher.dispatchEvent(this, "选择完毕", name, address);
	}

	@Override
	public void 操作失败(String message) {
		EventDispatcher.dispatchEvent(this, "操作失败", message);
	}

	/* 组件属性 */

	@Override
	public boolean 启用() {
		return !destroyed && input().isEnabled();
	}

	@Override
	public void 启用(boolean enabled) {
		if (destroyed) {
			return;
		}
		AutoCompleteTextView view = input();
		view.setEnabled(enabled);
		view.setFocusable(enabled);
		view.setFocusableInTouchMode(enabled);
		view.invalidate();
		refresh(true);
	}

	@Override
	public void 单行模式(boolean singleLine) {
		if (!destroyed) {
			super.单行模式(singleLine);
			input().setSingleLine(singleLine);
		}
	}

	@Override
	public String 错误信息() {
		return suggestions.getLastError();
	}

	/* 生命周期与输入回调 */

	@Override
	public void 销毁() {
		if (Looper.myLooper() != Looper.getMainLooper()) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					邮箱选择器Impl.this.销毁();
				}
			});
			return;
		}
		if (destroyed) {
			return;
		}
		destroyed = true;
		suggestions.close();
		AutoCompleteTextView view = input();
		view.dismissDropDown();
		view.removeTextChangedListener(this);
		view.removeOnAttachStateChangeListener(this);
		view.setOnFocusChangeListener(null);
		view.setOnItemClickListener(null);
		view.setAdapter((EmailAddressAdapter) null);
		view.setEnabled(false);
		view.setFocusable(false);
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (attached) {
					activity.removeOnResumeListener(邮箱选择器Impl.this);
					activity.removeOnPauseListener(邮箱选择器Impl.this);
					activity.removeOnDestroyListener(邮箱选择器Impl.this);
					activity.removeOnRequestPermissionsResultListener(邮箱选择器Impl.this);
					attached = false;
				}
			}
		});
		super.销毁();
	}

	@Override
	public void onResume() {
		refresh(true);
	}

	@Override
	public void onPause() {
		suggestions.prepare("", false, false);
		input().dismissDropDown();
	}

	@Override
	public void onDestroy() {
		销毁();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		refresh(true);
	}

	@Override
	public void onFocusChange(View view, boolean focused) {
		if (destroyed) {
			return;
		}
		refresh(focused);
		if (focused) {
			获得焦点();
		} else {
			失去焦点();
		}
	}

	@Override
	public void onViewAttachedToWindow(View view) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				refresh(true);
			}
		});
	}

	@Override
	public void onViewDetachedFromWindow(View view) {
		suggestions.prepare("", false, false);
		input().dismissDropDown();
	}

	@Override
	public void beforeTextChanged(CharSequence text, int start, int count, int after) {
	}

	@Override
	public void onTextChanged(CharSequence text, int start, int before, int count) {
		// AutoCompleteTextView 在 afterTextChanged 中发起过滤，先使旧结果失效。
		refresh(false);
	}

	@Override
	public void afterTextChanged(Editable text) {
	}

	/* 内部辅助方法 */

	/**
	 * 获取组件内部的邮箱输入框。
	 *
	 * @return 原生自动补全视图
	 */
	private AutoCompleteTextView input() {
		return (AutoCompleteTextView) getView();
	}

	/**
	 * 更新查询版本，并按需重新过滤；完成候选选择时保留条目直至点击事件读取。
	 *
	 * @param query 是否主动发起当前文本的过滤
	 */
	private void refresh(boolean query) {
		if (destroyed) {
			return;
		}
		AutoCompleteTextView view = input();
		boolean active = initialized && view.isEnabled() && view.hasFocus() && view.isShown()
				&& activity.isInForeground() && !activity.isFinishing() && !activity.isDestroyed();
		suggestions.prepare(view.getText(), active, view.isPerformingCompletion());
		if (query && active && view.enoughToFilter()) {
			suggestions.getFilter().filter(view.getText(), view);
		}
		if (!active) {
			view.dismissDropDown();
		}
	}
}

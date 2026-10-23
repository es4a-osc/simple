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

import simple.runtime.日志输出;
import simple.runtime.android.MainActivity;
import simple.runtime.collections.意图;
import simple.runtime.components.窗口;
import simple.runtime.errors.属性访问错误;
import simple.runtime.errors.文件未存在错误;
import simple.runtime.events.EventDispatcher;
import simple.runtime.parameters.BooleanReferenceParameter;
import simple.runtime.variants.IntegerVariant;
import simple.runtime.variants.Variant;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import java.io.IOException;
import java.io.InputStream;

/**
 * 窗口组件的实现。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public abstract class 窗口Impl extends 面板Impl implements 窗口 {

	// 标题属性支持
	private String title;

	// 各窗口独立保存背景路径和图片，避免活动窗口之间相互覆盖。
	private String backgroundImage = "";
	private Drawable backgroundDrawable;

	// 窗口的滚动视图
	private ScrollView scroller;

	/**
	 * 创建新窗口。
	 */
	public 窗口Impl() {
		super(null);

		// 恢复监听器
		MainActivity context = MainActivity.getContext();
		context.addOnResumeListener(new MainActivity.OnResumeListener() {
			@Override
			public void onResume() {
				程序被恢复();
			}
		});

		// 停止监听器
		context.addOnStopListener(new MainActivity.OnStopListener() {
			@Override
			public void onStop() {
				程序被停止();
			}
		});

		// 销毁监听器
		context.addOnDestroyListener(new MainActivity.OnDestroyListener() {
			@Override
			public void onDestroy() {
				try {
					程序被销毁();
				} finally {
					// 普通活动销毁不再杀进程，注销本窗口拥有的全部事件闭包。
					EventDispatcher.unregisterEvent(窗口Impl.this);
				}
			}
		});

		// 配置更改监听器
		context.addOnConfigurationChangedListener(new MainActivity.OnConfigurationChangedListener() {
			@Override
			public void onConfigurationChanged(Configuration newConfig) {
				if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT ||
						newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
					屏幕方向改变(newConfig.orientation - 1);
				}
			}
		});

		// 新意图监听器
		context.addOnNewIntentListener(new MainActivity.OnNewIntentListener() {
			@Override
			public void onNewIntent(Intent intent) {
				程序被启动(new 意图(intent));
			}
		});
	}

	/* 组件事件 */

	@Override
	public void 程序被启动(意图 intent) {
		EventDispatcher.dispatchEvent(this, "程序被启动", intent);
	}

	@Override
	public void 程序被恢复() {
		EventDispatcher.dispatchEvent(this, "程序被恢复");
	}

	@Override
	public void 程序被停止() {
		EventDispatcher.dispatchEvent(this, "程序被停止");
	}

	@Override
	public void 程序被销毁() {
		EventDispatcher.dispatchEvent(this, "程序被销毁");
	}

	@Override
	public void 屏幕方向改变(int orientation) {
		EventDispatcher.dispatchEvent(this, "屏幕方向改变", orientation);
	}

	@Override
	public void 即将切换() {
		EventDispatcher.dispatchEvent(this, "即将切换");
	}

	@Override
	public void 切换完毕() {
		EventDispatcher.dispatchEvent(this, "切换完毕");
	}

	@Override
	public void 反馈消息(Variant mark, Variant param) {
		EventDispatcher.dispatchEvent(this, "反馈消息", mark, param);
	}

	@Override
	public void 按下某键(int keycode, BooleanReferenceParameter shield) {
		EventDispatcher.dispatchEvent(this, "按下某键", keycode, shield);
	}

	@Override
	public void 触摸手势(int direction) {
		EventDispatcher.dispatchEvent(this, "触摸手势", direction);
	}

	@Override
	public void 选中菜单(String caption) {
		EventDispatcher.dispatchEvent(this, "选中菜单", caption);
	}

	/* 组件属性 */

	@Override
	public String 背景图片() {
		return backgroundImage;
	}

	@Override
	public void 背景图片(String imagePath) {
		MainActivity context = MainActivity.getContext();

		// 图片读取成功后才更新窗口自身状态；切换时复用已载入的图片。
		if (!imagePath.equals(backgroundImage)) {
			Drawable drawable = null;
			if (imagePath.length() != 0) {
				try (InputStream stream = context.getAssets().open(imagePath)) {
					drawable = Drawable.createFromStream(stream, imagePath);
				} catch (IOException ioe) {
					throw new 文件未存在错误(imagePath);
				}
			}
			backgroundImage = imagePath;
			backgroundDrawable = drawable;
		}

		// 未显示的窗口仅保存背景，不能覆盖活动窗口的背景。
		if (context.isActiveForm(this)) {
			Drawable drawable = backgroundDrawable;
			if (backgroundImage.length() == 0) {
				// 清空或切换到无背景图片的窗口时，恢复当前主题的默认背景。
				TypedArray attributes = context.obtainStyledAttributes(new int[]{android.R.attr.windowBackground});
				try {
					drawable = attributes.getDrawable(0);
				} finally {
					attributes.recycle();
				}
			}
			context.getWindow().setBackgroundDrawable(drawable);
		}
	}

	@Override
	public boolean 滚动() {
		return scroller != null;
	}

	@Override
	public void 滚动(boolean scrollable) {
		MainActivity app = MainActivity.getContext();
		View view = viewLayout.getLayoutManager();

		if (scrollable) {
			if (scroller == null) {
				scroller = new ScrollView(app);
				// 当前窗口为目前激活窗口，那就把滚动框设置为当前根视图
				if (app.isActiveForm(this)) {
					app.setContent(scroller);
				}

				// 把当前视图添加到滚动框中
				scroller.addView(view, new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT
				));
			}
		} else {
			if (scroller != null) {
				// 从滚动框中移除当前视图
				scroller.removeView(view);
				scroller = null;

				// 当前窗口为目前活跃窗口，那就把当前视图设置为根视图
				if (app.isActiveForm(this)) {
					app.setContent(view);
				}
			}
		}
	}

	@Override
	public String 标题() {
		return title;
	}

	@Override
	public void 标题(String newTitle) {
		title = newTitle;

		// 未显示的窗口仅保存自身标题，不改写当前界面。
		MainActivity context = MainActivity.getContext();
		if (context.isActiveForm(this)) {
			context.setTitle(title);
		}
	}

	@Override
	public Variant 高度() {
		return IntegerVariant.getIntegerVariant(viewLayout.getLayoutManager().getHeight());
	}

	@Override
	public void 高度(Variant height) {
		throw new 属性访问错误();
	}

	@Override
	public Variant 宽度() {
		return IntegerVariant.getIntegerVariant(viewLayout.getLayoutManager().getWidth());
	}

	@Override
	public void 宽度(Variant width) {
		throw new 属性访问错误();
	}

	@Override
	public int 行() {
		throw new 属性访问错误();
	}

	@Override
	public void 行(int newRow) {
		日志输出.warning("(%d) 尝试为窗口组件设置行(%d)", hashCode(), newRow);
	}

	@Override
	public int 列() {
		throw new 属性访问错误();
	}

	@Override
	public void 列(int newColumn) {
		日志输出.warning("(%d) 尝试为窗口组件设置列(%d)", hashCode(), newColumn);
	}

	/* 组件方法 */

	@Override
	public void 投递消息(final Variant mark, final Variant param) {
		// Activity.runOnUiThread
		// https://developer.android.google.cn/reference/android/app/Activity.html#runOnUiThread(java.lang.Runnable)}
		MainActivity.getContext().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				反馈消息(mark, param);
			}
		});
	}

	@Override
	public void 投递消息(final Variant mark) {
		投递消息(mark, null);
	}

	/* 面板 */

	@Override
	protected void addToContainer() {
		// 无事可做
	}

	@Override
	public View getView() {
		return scroller != null ? scroller : viewLayout.getLayoutManager();
	}
}

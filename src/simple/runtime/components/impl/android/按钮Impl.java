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
import simple.runtime.components.按钮;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.android.util.TextViewUtil;
import simple.runtime.components.impl.android.util.ViewUtil;
import simple.runtime.events.EventDispatcher;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

/**
 * Simple按钮组件的Android实现。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public final class 按钮Impl extends 文本视图组件 implements 按钮 {

	// 手势检测器
	private GestureDetector gestureDetector;

	/**
	 * 创建新的按钮组件。
	 *
	 * @param container  包含组件的容器（不可为{@code null}）
	 */
	public 按钮Impl(组件容器 container) {
		super(container);
	}

	@Override
	protected View createView() {
		// Button https://developer.android.google.cn/reference/android/widget/Button
		Button view = new Button(MainActivity.getContext());

		// 因SDK5.0等默认主题按钮样式定义为标题中字母自动转大写，这里关闭自动转大写
		TextViewUtil.setAllCaps(view, false);

		// 焦点改变监听器
		view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View previouslyFocused, boolean gainFocus) {
				if (gainFocus) {
					获得焦点();
				} else {
					失去焦点();
				}
			}
		});

		// 手势检测器
		gestureDetector = new GestureDetector(MainActivity.getContext(),
				new GestureDetector.SimpleOnGestureListener() {

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				int direction;
				int deltaX = (int) (e1.getRawX() - e2.getRawX());
				int deltaY = (int) (e1.getRawY() - e2.getRawY());
				if (Math.abs(deltaX) > Math.abs(deltaY)) {
					// 水平移动
					direction = deltaX > 0 ? 触摸_左滑 : 触摸_右滑;
				} else {
					// 垂直移动
					direction = deltaY > 0 ? 触摸_上滑 : 触摸_下滑;
				}
				触摸手势(direction);
				return true;
			}

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				int direction;
				if (Math.abs(distanceX) > Math.abs(distanceY)) {
					// 水平移动
					direction = distanceX > 0 ? 触摸_左移 : 触摸_右移;
				} else {
					// 垂直移动
					direction = distanceY > 0 ? 触摸_上移 : 触摸_下移;
				}
				触摸手势(direction);
				return true;
			}

			@Override
			public boolean onSingleTapConfirmed(MotionEvent e) {
				触摸手势(触摸_单击);
				return true;
			}

			@Override
			public boolean onDoubleTap(MotionEvent e) {
				触摸手势(触摸_双击);
				return true;
			}
		});

		// 触摸监听器
		view.setOnTouchListener(new View.OnTouchListener() {
			private int lastX, lastY, currX, currY;

			@Override
			public boolean onTouch(View view, MotionEvent event) {
				lastX = currX;
				lastY = currY;
				currX = (int) event.getRawX();
				currY = (int) event.getRawY();
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						lastX = currX;
						lastY = currY;
						被按下(currX, currY);
						break;
					case MotionEvent.ACTION_UP:
						lastX = currX;
						lastY = currY;
						被按下(currX, currY);
						break;
					case MotionEvent.ACTION_MOVE:
						触摸移动(lastX, lastY, currX, currY);
						break;
				}
				gestureDetector.onTouchEvent(event);
				// 注意这里不可消耗掉该事件（返回 true），否则会造成后续如单击等事件无法触发。
				return false;
			}
		});

		// 单击监听器
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				被单击();
			}
		});

		// 长按监听器
		view.setOnLongClickListener(new View.OnLongClickListener(){
			@Override
			public boolean onLongClick(View view) {
				被长按();
				return true;
			}
		});

		return view;
	}

	/* 按钮 实现 */

	@Override
	public void 获得焦点() {
		EventDispatcher.dispatchEvent(this, "获得焦点");
	}

	@Override
	public void 失去焦点() {
		EventDispatcher.dispatchEvent(this, "失去焦点");
	}

	@Override
	public void 被按下(int x, int y) {
		EventDispatcher.dispatchEvent(this, "被按下", x, y);
	}

	@Override
	public void 被弹起(int x, int y) {
		EventDispatcher.dispatchEvent(this, "被弹起", x, y);
	}

	@Override
	public void 触摸移动(int lastX, int lastY, int currX, int currY) {
		EventDispatcher.dispatchEvent(this, "触摸移动", lastX, lastY, currX, currY);
	}

	@Override
	public void 触摸手势(int direction) {
		EventDispatcher.dispatchEvent(this, "触摸手势", direction);
	}

	@Override
	public void 被单击() {
		EventDispatcher.dispatchEvent(this, "被单击");
	}

	@Override
	public void 被长按() {
		EventDispatcher.dispatchEvent(this, "被长按");
	}

	@Override
	public boolean 启用() {
		return ViewUtil.isEnabled(getView());
	}

	@Override
	public void 启用(boolean enabled) {
		ViewUtil.setEnabled(getView(), enabled);
	}

	@Override
	public boolean 获取焦点() {
		return ViewUtil.requestFocus(getView());
	}

	@Override
	public void 清除焦点() {
		ViewUtil.clearFocus(getView());
	}
}

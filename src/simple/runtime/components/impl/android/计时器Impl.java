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

import simple.runtime.components.组件容器;
import simple.runtime.components.计时器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;

import android.os.Handler;

/**
 * Android实现Simple的计时器组件
 *
 * @author Herbert Czymontek
 */
public final class 计时器Impl extends 组件Impl implements 计时器, Runnable {

	// 用作计时器的Android消息处理程序
	private Handler handler;

	// 指示计时器是否正在运行
	private boolean enabled;

	// 计时器事件之间的间隔（毫秒）
	private int interval;

	/**
	 * 创建新的计时器组件。
	 *
	 * @param container 将容纳组件的容器（不得为{@code null}，对于非可见组件也是如此，必须是窗口）
	 */
	public 计时器Impl(组件容器 container) {
		super(container);

		handler = new Handler();
	}

	@Override
	public void 计时() {
		EventDispatcher.dispatchEvent(this, "计时");
	}

	@Override
	public int 间隔() {
		return interval;
	}

	@Override
	public void 间隔(int newInterval) {
		interval = newInterval;

		if (enabled) {
			handler.removeCallbacks(this);
			handler.postDelayed(this, newInterval);
		}
	}

	@Override
	public boolean 启用() {
		return enabled;
	}

	@Override
	public void 启用(boolean enable) {
		if (enabled) {
			handler.removeCallbacks(this);
		}

		enabled = enable;

		if (enable) {
			handler.postDelayed(this, interval);
		}
	}

	/* Runnable 实现 */

	public void run() {
		if (enabled) {
			// 触发事件
			计时();
			
			handler.postDelayed(this, interval);
		}
	}
}

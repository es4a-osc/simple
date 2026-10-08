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
import simple.runtime.components.电话;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

/**
 * 实现电话相关功能。
 *
 * @author Herbert Czymontek
 * @author xhwsd@qq.com
 */
public final class 电话Impl extends 组件Impl implements 电话 {

	private final Vibrator vibrator;

	/**
	 * 创建新的电话组件。
	 *
	 * @param container  将容纳组件的容器（不得为{@code null}，对于非可见组件也是，必须是窗口）
	 */
	public 电话Impl(组件容器 container) {
		super(container);

		vibrator = (Vibrator) MainActivity.getContext().getSystemService(Context.VIBRATOR_SERVICE);
	}

	/* 电话 实现 */

	@Override
	public boolean 可用() {
		return true;
	}

	@Override
	public void 呼叫(String phoneNumber) {
		if (null != phoneNumber && phoneNumber.length() > 0) {
			MainActivity.getContext().startActivity(new Intent(Intent.ACTION_CALL,
					Uri.parse("tel:" + phoneNumber)));
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public void 振动(int duration) {
		// 低API兼容处理
		if (VERSION.SDK_INT < VERSION_CODES.O) {
			// 弃用于API26：Vibrator.vibrate(long)
			vibrator.vibrate(duration);
		} else {
			// 添加于API26：VibrationEffect.createOneShot(long, int)
			// 添加于API26：VibrationEffect.DEFAULT_AMPLITUDE
			vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
		}
	}
}

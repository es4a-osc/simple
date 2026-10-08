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

package simple.runtime.android;

import simple.runtime.LogFunctions;

import android.content.Context;
import android.util.Log;

/**
 * 实现日志相关功能。
 *
 * @author Herbert Czymontek
 */
public final class LogImpl implements LogFunctions {

	/**
	 * 创建和初始化新的日志函数实现
	 *
	 * @param context  活动上下文
	 */
	public LogImpl(Context context) {
		// 到目前为止，我们实际上并不需要日志上下文，但是拥有对称的API很好
	}

	/* LogFunctions 实现 */

	@Override
	public void error(String moduleName, String message) {
		Log.e(moduleName, message);
	}

	@Override
	public void info(String moduleName, String message) {
		Log.i(moduleName, message);
	}

	@Override
	public void warning(String moduleName, String message) {
		Log.w(moduleName, message);
	}

	@Override
	public void debug(String moduleName, String message) {
		Log.d(moduleName, message);
	}
}

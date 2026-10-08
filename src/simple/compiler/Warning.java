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

package simple.compiler;

/**
 * 此类为编译器的报告提供警告消息。
 *
 * @author Herbert Czymontek
 */
public final class Warning extends Message {

	/**
	 * 警告消息模板。
	 */
	public static final String warnLargeNumber;
	public static final String warnSmallNumber;

	static {
		warnSmallNumber = Message.localize("warnSmallNumber",
				"数字太小 - 使用0");
		warnLargeNumber = Message.localize("warnLargeNumber",
				"数字太大 - 使用无穷大");
	}

	/**
	 * 创建新的警告消息。
	 *
	 * @param position  源位置
	 * @param message  警告消息模板
	 * @param params  警告消息的参数
	 */
	public Warning(long position, String message, String... params) {
		super(position, message, params);
	}

	@Override
	protected String messageKind() {
		return "警告";
	}
}

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

package simple.runtime.errors;

import simple.runtime.annotations.SimpleObject;

/**
 * 运行时错误，表示访问文件时出现问题。
 *
 * @author Herbert Czymontek
 */
@SuppressWarnings("serial")
@SimpleObject
public final class 文件读写错误 extends 运行错误 {

	/**
	 * 创建新的文件I/O错误。
	 *
	 * @param message  详细的消息
	 */
	public 文件读写错误(String message) {
		super(message);
	}
}

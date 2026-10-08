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
 * 所有Simple运行时错误的超类。
 *
 * @author Herbert Czymontek
 */
@SuppressWarnings("serial")
@SimpleObject
public abstract class 运行错误 extends RuntimeException {
	/**
	关联代码：
	{@link simple.compiler.Compiler RUNTIME_ERROR_INTERNAL_NAME}
	*/

	/**
	 * 创建运行时错误。
	 */
	protected 运行错误() {
	}

	/**
	 * 使用更详细的错误消息创建运行时错误。
	 *
	 * @param message  详细错误消息
	 */
	protected 运行错误(String message) {
		super(message);
	}

	/**
	 * 将Java{@link Throwable}转换为Simple运行时错误。
	 *
	 * @param throwable  要转换的 Java Throwable（可能已经是一个Simple的运行时错误）
	 * @return  Simple运行时错误
	 */
	public static 运行错误 convertToRuntimeError(Throwable throwable) {
		if (throwable instanceof 运行错误) {
			return (运行错误) throwable;
		}

		// Java异常的转换
		if (throwable instanceof ArrayIndexOutOfBoundsException) {
			return new 索引超出界限错误();
		}
		if (throwable instanceof IllegalArgumentException) {
			return new 非法参数错误();
		}
		if (throwable instanceof NullPointerException) {
			return new 未初始化实例错误();
		}

		throw new UnsupportedOperationException(throwable);
	}
}

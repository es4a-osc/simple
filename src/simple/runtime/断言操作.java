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

package simple.runtime;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.errors.断言失败;
import simple.runtime.variants.Variant;

/**
 * 断言允许测试有关应用程序的运行时状态的假设。
 * 失败的断言将导致{@link 断言失败}运行时错误
 *
 * @author Herbert Czymontek
 */
@SimpleObject
public class 断言操作 {

	private 断言操作() {
	}

	/**
	 * 测试断言是否为真。计算给定的表达式，
	 * 如果表达式的结果不为{@code True}，
	 * 则导致{@link 断言失败}运行时错误。
	 *
	 * @param expression  要测试的表达式
	 */
	@SimpleFunction
	public static void 断言真(Variant expression) {
		if (!expression.getBoolean()) {
			throw new 断言失败();
		}
	}

	/**
	 * 测试断言是否为假。计算给定的表达式，
	 * 如果表达式的结果不为{@code False}，
	 * 则导致{@link 断言失败}运行时错误。
	 *
	 * @param expression  expression to test
	 */
	@SimpleFunction
	public static void 断言假(Variant expression) {
		if (expression.getBoolean()) {
			throw new 断言失败();
		}
	}
}

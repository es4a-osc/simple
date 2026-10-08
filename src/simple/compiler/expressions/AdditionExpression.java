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

package simple.compiler.expressions;

import simple.classfiles.Method;
import simple.compiler.Compiler;
import simple.compiler.symbols.FunctionSymbol;

/**
 * 此类表示加法表达式。
 *
 * @author Herbert Czymontek
 */
public final class AdditionExpression extends ArithmeticExpression {

	/**
	 * 创建新的加法表达式。
	 *
	 * @param position  源代码表达式的起始位置
	 * @param leftOperand  要添加到的操作数
	 * @param rightOperand  要添加的操作数
	 */
	public AdditionExpression(long position, Expression leftOperand, Expression rightOperand) {
		super(position, leftOperand, rightOperand);
	}

	@Override
	protected Expression fold(Compiler compiler, FunctionSymbol currentFunction) {
		// 左右操作数都是常量数表达式
		if (leftOperand instanceof ConstantNumberExpression &&
				rightOperand instanceof ConstantNumberExpression) {
			return new ConstantNumberExpression(getPosition(),
					((ConstantNumberExpression) leftOperand).value.add(
							((ConstantNumberExpression) rightOperand).value)).resolve(compiler, currentFunction);
		}

		return this;
	}

	@Override
	public void generate(Method m) {
		super.generate(m);
		type.generateAddition(m);
	}

	@Override
	public String toString() {
		return leftOperand.toString() + " + " + rightOperand.toString(); // COV_NF_LINE
	}
}

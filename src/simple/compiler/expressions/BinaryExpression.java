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
 * 这是具有两个操作数的表达式的超类。
 *
 * @author Herbert Czymontek
 */
public abstract class BinaryExpression extends Expression {
	// 第一个或左操作数
	protected Expression leftOperand;

	// 第二个或右操作数
	protected Expression rightOperand;

	/**
	 * 创建具有两个操作数的表达式。
	 *
	 * @param position  源代码表达式的起始位置
	 * @param leftOperand  第一个或左操作数
	 * @param rightOperand  第二个或右操作数
	 */
	public BinaryExpression(long position, Expression leftOperand, Expression rightOperand) {
		super(position);

		this.leftOperand = leftOperand;
		this.rightOperand = rightOperand;
	}

	@Override
	public Expression resolve(Compiler compiler, FunctionSymbol currentFunction) {
		leftOperand = leftOperand.resolve(compiler, currentFunction);
		rightOperand = rightOperand.resolve(compiler, currentFunction);

		return this;
	}

	@Override
	public void generate(Method m) {
		leftOperand.generate(m);
		rightOperand.generate(m);
	}
}

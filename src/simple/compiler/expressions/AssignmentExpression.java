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
import simple.compiler.Error;
import simple.compiler.symbols.FunctionSymbol;
import simple.compiler.symbols.ObjectSymbol;

/**
 * 此类表示赋值表达式。
 *
 * @author Herbert Czymontek
 */
public final class AssignmentExpression extends BinaryExpression {

	/**
	 * 创建新的赋值表达式。
	 *
	 * @param position  源代码表达式的起始位置
	 * @param leftOperand  左操作数（必须是左值）
	 * @param rightOperand  右操作数
	 */
	public AssignmentExpression(long position, Expression leftOperand, Expression rightOperand) {
		super(position, leftOperand, rightOperand);
	}

	@Override
	public Expression resolve(Compiler compiler, FunctionSymbol currentFunction) {
		super.resolve(compiler, currentFunction);

		if (!leftOperand.isAssignable()) {
			compiler.error(leftOperand.getPosition(), Error.errOperandNotAssignable);
		}

		// 需要确保它是一个值，而不仅仅是一个类型的名称
		if (rightOperand instanceof IdentifierExpression &&
				((IdentifierExpression) rightOperand).resolvedIdentifier instanceof ObjectSymbol) {
			compiler.error(leftOperand.getPosition(), Error.errValueExpected);
		}

		type = leftOperand.type;
		rightOperand = rightOperand.checkType(compiler, type);

		return fold(compiler, currentFunction);
	}

	@Override
	public boolean isAssignmentExpression() {
		return true;
	}

	@Override
	public void generate(Method m) {
		leftOperand.generatePrepareWrite(m);
		rightOperand.generate(m);
		if (type.isObjectType() || type.isArrayType()) {
			m.generateInstrCheckcast(type.internalName());
		}
		leftOperand.generateWrite(m);
	}

	@Override
	public String toString() {
		return leftOperand.toString() + " = " + rightOperand.toString();  // COV_NF_LINE
	}
}

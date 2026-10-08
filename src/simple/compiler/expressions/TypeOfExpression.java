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
import simple.compiler.types.BooleanType;
import simple.compiler.types.Type;
import simple.compiler.types.VariantType;

/**
 * 此类表示类型检查操作。
 *
 * @author Herbert Czymontek
 */
public final class TypeOfExpression extends UnaryExpression {

	// 要检查的类型
	private Type expectedType;

	/**
	 * 创建新的类型检查表达式。
	 *
	 * @param position  源代码表达式的起始位置
	 * @param operand  要检查类型的操作数
	 * @param expectedType  预期的操作数
	 */
	public TypeOfExpression(long position, Expression operand, Type expectedType) {
		super(position, operand);

		this.expectedType = expectedType;
	}

	@Override
	public Expression resolve(Compiler compiler, FunctionSymbol currentFunction) {
		super.resolve(compiler, currentFunction);
		expectedType.resolve(compiler);
		if ((!expectedType.isArrayType() && !expectedType.isObjectType()) ||
				(!operand.type.isArrayType() && !operand.type.isObjectType())) {
			operand = operand.checkType(compiler, VariantType.variantType);
		}

		type = BooleanType.booleanType;

		return fold(compiler, currentFunction);
	}

	@Override
	public void generate(Method m) {
		operand.generate(m);
		operand.type.generateTypeOf(m, expectedType.internalName());
	}

	@Override
	public String toString() {
		return "类型检验 " + operand.toString() + " 是 " + expectedType.toString(); // COV_NF_LINE
	}
}

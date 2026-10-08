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

import simple.compiler.Compiler;
import simple.compiler.expressions.synthetic.ConversionExpression;
import simple.compiler.symbols.FunctionSymbol;
import simple.compiler.types.BooleanType;
import simple.compiler.types.Type;
import simple.compiler.types.VariantType;

/**
 * This class is the superclass for comparison expressions.
 *
 * @author Herbert Czymontek
 */
public abstract class ComparisonExpression extends BinaryExpression {

	/**
	 * Creates a new comparison expression.
	 *
	 * @param position  source code start position of expression
	 * @param leftOperand  left operand of comparison
	 * @param rightOperand  right operand of comparison
	 */
	public ComparisonExpression(long position, Expression leftOperand, Expression rightOperand) {
		super(position, leftOperand, rightOperand);
	}

	@Override
	public Expression resolve(Compiler compiler, FunctionSymbol currentFunction) {
		super.resolve(compiler, currentFunction);

		Type leftType = leftOperand.type;
		Type rightType = rightOperand.type;

		// 比较中的空采用另一侧操作数类型，保证变体型空值生成UninitializedVariant xhwsd@qq.com 2026-9-4
		if (leftOperand instanceof NothingExpression) {
			leftOperand = ConversionExpression.convert(compiler, leftOperand, rightType); 
			leftType = leftOperand.type;
		}

		// 比较中的空采用另一侧操作数类型，并兼容“值 = 空”的操作数顺序 xhwsd@qq.com 2026-9-4
		if (rightOperand instanceof NothingExpression) {
			rightOperand = ConversionExpression.convert(compiler, rightOperand, leftType);
			rightType = rightOperand.type;
		}

		if (!leftType.equals(rightType)) {
			// TODO: generate better code - avoid use of variants
			type = VariantType.variantType;
			rightOperand = ConversionExpression.convert(compiler, rightOperand, type);
			leftOperand = ConversionExpression.convert(compiler, leftOperand, type);
		}

		type = BooleanType.booleanType;

		return fold(compiler, currentFunction);
	}
}

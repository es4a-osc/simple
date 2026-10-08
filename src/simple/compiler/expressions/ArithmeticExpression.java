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
import simple.compiler.Error;
import simple.compiler.expressions.synthetic.ConversionExpression;
import simple.compiler.symbols.FunctionSymbol;
import simple.compiler.types.StringType;
import simple.compiler.types.Type;
import simple.compiler.types.VariantType;

/**
 * 这个类是所有算术表达式的超类。
 *
 * @author Herbert Czymontek
 */
public abstract class ArithmeticExpression extends BinaryExpression {

	/**
	 * 创建新的算术表达式。
	 *
	 * @param position  源代码表达式的起始位置
	 * @param leftOperand  左操作数
	 * @param rightOperand  右操作数
	 */
	public ArithmeticExpression(long position, Expression leftOperand, Expression rightOperand) {
		super(position, leftOperand, rightOperand);
	}

	@Override
	public Expression resolve(Compiler compiler, FunctionSymbol currentFunction) {
		super.resolve(compiler, currentFunction);

		Type leftType = leftOperand.type;
		if (!leftType.isScalarType()) {
			return reportScalarTypeNeededError(compiler, leftOperand);
		}

		Type rightType = rightOperand.type;
		if (!rightType.isScalarType()) {
			return reportScalarTypeNeededError(compiler, rightOperand);
		}

		// xhwsd@qq.com 2021-5-29 修复常量值表达式可以引用常量
		if (currentFunction == null && leftOperand instanceof ConstantExpression && rightOperand instanceof ConstantExpression) {
			// 当前函数为nill，那就全局作用域
			// 检验对象类是否一致
			if (!leftOperand.getClass().equals(rightOperand.getClass())) {
				compiler.error(getPosition(), Error.errCannotConvertType, rightType.toString(),
						leftType.toString());
			}
			type = leftType;
		} else if (!leftType.equals(rightType) || leftType.equals(StringType.stringType)) {
			// TODO: 生成更好的代码 - 避免使用变体
			type = VariantType.variantType;
			rightOperand = ConversionExpression.convert(compiler, rightOperand, type);
			leftOperand = ConversionExpression.convert(compiler, leftOperand, type);
		} else {
			type = leftType;
		}

		return fold(compiler, currentFunction);
	}
}

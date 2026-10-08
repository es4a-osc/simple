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

package simple.compiler.expressions.synthetic;

import simple.classfiles.Method;
import simple.compiler.Compiler;
import simple.compiler.Error;
import simple.compiler.expressions.ConstantNumberExpression;
import simple.compiler.expressions.Expression;
import simple.compiler.expressions.NothingExpression;
import simple.compiler.expressions.UnaryExpression;
import simple.compiler.types.ByteType;
import simple.compiler.types.DoubleType;
import simple.compiler.types.IntegerType;
import simple.compiler.types.LongType;
import simple.compiler.types.ShortType;
import simple.compiler.types.SingleType;
import simple.compiler.types.Type;

/**
 * 此类表示转换表达式。
 *
 * @author Herbert Czymontek
 */
public final class ConversionExpression extends UnaryExpression {

	private ConversionExpression(Expression operand, Type type) {
		super(operand.getPosition(), operand);
		this.type = type;
	}

	/**
	 * 将给定表达式的类型转换为新类型。
	 *
	 * @param compiler  当前编译器实例
	 * @param operand  类型需要转换的表达式
	 * @param toType  所需类型
	 * @return  具有所需类型的表达式
	 */
	public static Expression convert(Compiler compiler, Expression operand, Type toType) {
		// 空表示上下文目标类型的缺省值。必须先确定目标类型，再进行普通类型相等判断；
		// 否则NothingExpression的占位ErrorType会与任意类型相等，导致空无法生成目标类型的缺省值。
		// 例如目标为变体型时，应生成UninitializedVariant，而不是Java null。
		// xhwsd@qq.com 2026-9-4
		if (operand instanceof NothingExpression) {
			((NothingExpression) operand).changeType(toType);
			return operand;
		}

		// Nothing to do if the type is already correct
		Type fromType = operand.getType();
		if (fromType.equals(toType)) {
			return operand;
		}

		// Constant numerical expressions can be converted directly
		if (operand instanceof ConstantNumberExpression) {
			ConstantNumberExpression constExpr = (ConstantNumberExpression) operand;
			if (toType.equals(ByteType.byteType)) {
				return constExpr.convertToByte();
			}
			if (toType.equals(ShortType.shortType)) {
				return constExpr.convertToShort();
			}
			if (toType.equals(IntegerType.integerType)) {
				return constExpr.convertToInteger();
			}
			if (toType.equals(LongType.longType)) {
				return constExpr.convertToLong();
			}
			if (toType.equals(SingleType.singleType)) {
				return constExpr.convertToSingle();
			}
			if (toType.equals(DoubleType.doubleType)) {
				return constExpr.convertToDouble();
			}
		}

		// Check whether this is a legal conversion (might still fail at runtime though)
		if (!fromType.canConvertTo(toType)) {
			compiler.error(operand.getPosition(), Error.errCannotConvertType, fromType.toString(),
					toType.toString());
		}

		// Create conversion expression to perform conversion at runtime
		return new ConversionExpression(operand, toType);
	}

	@Override
	public void generate(Method m) {
		operand.generate(m);
		operand.getType().generateConversion(m, type);
	}
}

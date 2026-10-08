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
import simple.util.Preconditions;

/**
 * 此类表示逻辑或且（AND）表达式，具体取决于操作数的类型。
 *
 * @author Herbert Czymontek
 */
public final class AndExpression extends LogicalOrBitOpExpression {

	/**
	 * 创建新的且（AND）表达式。
	 *
	 * @param position  源代码表达式的起始位置
	 * @param leftOperand  左操作数
	 * @param rightOperand  右操作数
	 */
	public AndExpression(long position, Expression leftOperand, Expression rightOperand) {
		super(position, leftOperand, rightOperand);
	}

	@Override
	public void generate(Method m) {
		if (type.isBooleanType()) {
			leftOperand.generate(m);
			Method.Label contLabel = Method.newLabel();
			m.generateInstrDup();
			m.generateInstrIfeq(contLabel);
			m.generateInstrPop();
			rightOperand.generate(m);
			m.setLabel(contLabel);
		} else {
			super.generate(m);
			type.generateBitAnd(m);
		}
	}

	@Override
	public void generateBranchOnFalse(Method m, Method.Label falseLabel) {
		Preconditions.checkState(type.isBooleanType());

		leftOperand.generateBranchOnFalse(m, falseLabel);
		rightOperand.generateBranchOnFalse(m, falseLabel);
	}

	@Override
	public void generateBranchOnTrue(Method m, Method.Label trueLabel) {
		Preconditions.checkState(type.isBooleanType());

		Method.Label contLabel = Method.newLabel();
		leftOperand.generateBranchOnFalse(m, contLabel);
		rightOperand.generateBranchOnTrue(m, trueLabel);
		m.setLabel(contLabel);
	}

	@Override
	public String toString() {
		return leftOperand.toString() + " 且 " + rightOperand.toString();  // COV_NF_LINE
	}
}

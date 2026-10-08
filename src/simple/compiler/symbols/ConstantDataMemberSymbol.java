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

package simple.compiler.symbols;

import simple.classfiles.ClassFile;
import simple.classfiles.Field;
import simple.classfiles.Method;
import simple.compiler.Compiler;
import simple.compiler.Error;
import simple.compiler.expressions.ConstantExpression;
import simple.compiler.expressions.Expression;
import simple.compiler.types.Type;
import simple.compiler.types.synthetic.ErrorType;

/**
 * 用于常量数据成员的符号。
 *
 * @author Herbert Czymontek
 */
public final class ConstantDataMemberSymbol extends ObjectDataMemberSymbol {

	// 与数据成员关联的常量
	private Expression constExpr;

	/**
	 * 创建新的常量数据成员符号。
	 *
	 * @param position  源代码的符号开始位置
	 * @param objectSymbol  定义对象
	 * @param name  数据成员名称
	 * @param type  数据成员类型
	 * @param constExpr  与数据成员关联的常量
	 */
	public ConstantDataMemberSymbol(long position, ObjectSymbol objectSymbol, String name, Type type,
			Expression constExpr) {
		super(position, objectSymbol, name, type);
		this.constExpr = constExpr;
	}

	/**
	 * 返回常量数据成员的值。
	 *
	 * @return  常量值
	 */
	public Expression getConstant() {
		return constExpr;
	}

	@Override
	public void resolve(Compiler compiler, FunctionSymbol currentFunction) {
		getType().resolve(compiler);

		// 解析并检验常量表达式的类型是否匹配常量的类型
		constExpr = constExpr.resolve(compiler, currentFunction).checkType(compiler, getType());

		// 常量值必须是常量
		if (!(constExpr instanceof ConstantExpression) &&
				!constExpr.getType().equals(ErrorType.errorType)) {
			compiler.error(constExpr.getPosition(), Error.errConstantValueExpected);
		}
	}

	@Override
	public void generateRead(Method m) {
		constExpr.generate(m);
	}

	@Override
	public void generateWrite(Method m) {
		Compiler.internalError();  // COV_NF_LINE
	}

	@Override
	public void generate(ClassFile cf) {
		Field field = cf.newField((short) (Field.ACC_PUBLIC | Field.ACC_FINAL | Field.ACC_STATIC),
				getName(), getType().signature());

		((ConstantExpression) constExpr).generate(field);
	}

	@Override
	public String toString() {
		return "常量 " + getName() + " 为 " + getType().toString();  // COV_NF_LINE
	}
}

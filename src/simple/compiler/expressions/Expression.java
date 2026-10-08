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
import simple.compiler.expressions.synthetic.ConversionExpression;
import simple.compiler.symbols.FunctionSymbol;
import simple.compiler.types.Type;
import simple.compiler.types.VariantType;

/**
 * 所有表达式的超类。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public abstract class Expression {

	// 源代码表达式的起始位置
	private final long position;

	// 表达式类型（对于无类型表达式可以为null）
	protected Type type;

	/**
	 * 创建新表达式。
	 *
	 * @param position  源代码表达式的起始位置
	 */
	public Expression(long position) {
		this.position = position;
	}

	/**
	 * 返回表达式的源代码位置。
	 *
	 * @return  源代码表达式的起始位置
	 */
	public long getPosition() {
		return position;
	}

	/**
	 * 返回表达式的类型（对于无类型表达式，可以是{@code null}）。
	 *
	 * @return  表达式的类型
	 */
	public Type getType() {
		return type.getType();
	}

	/**
	 * 解析表达式中涉及的任何标识符或类型。<p>
	 *
	 * 解析表达式中涉及的任何标识符或类型。
	 * 应用任何必要的类型转换，还启动表达式折叠。
	 * 解析之后，表达式就可以为其生成代码了
	 *
	 * @param compiler  当前编译器实例
	 * @param currentFunction  正在解析的当前函数（可以是{code null}）
	 * @return  解析表达式（可以是与原始表达式不同的表达式，例如在表达式折叠或类型转换之后）
	 */
	public abstract Expression resolve(Compiler compiler, FunctionSymbol currentFunction);

	/**
	 * 通过尝试应用常量折叠来简化表达式。
	 *
	 * @param compiler  当前编译器实例
	 * @param currentFunction  正在解析的当前函数（可以是{@code null}）
	 * @return  简化表达式（可以是与原始表达式不同的表达式）
	 */
	protected Expression fold(Compiler compiler, FunctionSymbol currentFunction) {
		return this;
	}

	/**
	 * 检查类型；检查表达式的类型并将其转换为指定的类型（如有必要）。
	 *
	 * @param compiler  当前的编译器实例
	 * @param expectedType  预期类型
	 * @return  具有预期类型的表达式（这可以是一个不同于原始表达式的表达式）
	 */
	public final Expression checkType(Compiler compiler, Type expectedType) {
		// 检验右操作数无可分配数，如Simple过程或运行库方法等无返回值（type都为null）xhwsd@qq.com 2021-5-27
		if (type == null) {
			return reportScalarTypeNeededError(compiler, this);
		}

		// 类型匹配与转换统一由ConversionExpression处理；空也必须在那里先取得上下文目标类型。
		// 不在此处提前比较类型，避免NothingExpression的占位ErrorType把任意目标类型误判为相等。
		// xhwsd@qq.com 2026-9-4
		return ConversionExpression.convert(compiler, this, expectedType);
	}

	/**
	 * 如果表达式是左操作值，则返回true。
	 *
	 * @return  {@code true}对于左值，否则{@code false}
	 */
	public boolean isAssignable() {
		return false;
	}

	/**
	 * 检查表达式是否为赋值表达式。
	 *
	 * @return  {@code true}如果表达式是赋值表达式
	 */
	public boolean isAssignmentExpression() {
		return false;
	}

	/**
	 * 检查表达式是否为调用表达式。
	 *
	 * @return  {@code true}如果表达式是调用表达式
	 */
	public boolean isCallExpression() {
		return false;
	}

	/**
	 * 为表达式生成代码。
	 *
	 * @param m  生成方法的代码
	 */
	public abstract void generate(Method m);

	/**
	 * 生成代码以准备写操作。
	 *
	 * @param m  生成代码的方法
	 */
	public void generatePrepareWrite(Method m) {
		Compiler.internalError();  // COV_NF_LINE
	}

	/**
	 * 为写操作生成代码。
	 *
	 * @param m  生成代码的方法
	 */
	public void generateWrite(Method m) {
		Compiler.internalError();  // COV_NF_LINE
	}

	/**
	 * 生成代码以准备方法调用。
	 *
	 * @param m  生成代码的方法
	 */
	public void generatePrepareInvoke(Method m) {
		Compiler.internalError();  // COV_NF_LINE
	}

	/**
	 * 生成代码以执行方法调用。
	 *
	 * @param m  生成代码的方法
	 */
	public void generateInvoke(Method m) {
		Compiler.internalError();  // COV_NF_LINE
	}

	/**
	 * 如果表达式的计算结果为{@code false}，则生成代码以分支到标签。
	 *
	 * @param m  生成代码的方法
	 * @param falseLabel  要分支到的标签
	 */
	public void generateBranchOnFalse(Method m, Method.Label falseLabel) {
		generate(m);
		m.generateInstrIfeq(falseLabel);
	}

	/**
	 * 如果表达式的计算结果为{@code true}，则生成代码以分支到标签。
	 *
	 * @param m  生成代码的方法
	 * @param trueLabel  要分支到的标签
	 */
	public void generateBranchOnTrue(Method m, Method.Label trueLabel) {
		generate(m);
		m.generateInstrIfne(trueLabel);
	}

	/**
	 * 报告“需要标量类型”错误。
	 *
	 * @param compiler  当前编译器实例
	 * @param operand  要报告错误的操作数
	 * @return  与操作数相同
	 */
	protected Expression reportScalarTypeNeededError(Compiler compiler, Expression operand) {
		compiler.error(operand.getPosition(), Error.errScalarTypeNeededForOperand);
		type = VariantType.variantType;
		return this;
	}

	/**
	 * 报告“需要标量整数类型”错误。
	 *
	 * @param compiler  当前编译器实例
	 * @param operand  要报告错误的操作数
	 * @return  与操作数相同
	 */
	protected Expression reportScalarIntegerTypeNeededError(Compiler compiler, Expression operand) {
		compiler.error(operand.getPosition(), Error.errScalarIntegerTypeNeededForOperand);
		type = VariantType.variantType;
		return this;
	}
}

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
import simple.compiler.scopes.Scope;
import simple.compiler.scopes.synthetic.ErrorScope;
import simple.compiler.symbols.ConstantDataMemberSymbol;
import simple.compiler.symbols.DataMemberSymbol;
import simple.compiler.symbols.FunctionSymbol;
import simple.compiler.symbols.InstanceFunctionSymbol;
import simple.compiler.symbols.LocalVariableSymbol;
import simple.compiler.symbols.NamespaceSymbol;
import simple.compiler.symbols.ObjectSymbol;
import simple.compiler.symbols.PropertySymbol;
import simple.compiler.symbols.Symbol;
import simple.compiler.types.ObjectType;
import simple.compiler.types.VariantType;
import simple.util.Preconditions;

/**
 * 此类是标识符表达式的超类。
 *
 * <p>标识符表达式是由带有或不带有任何进一步限定的标识符组成的表达式。
 *
 * @author Herbert Czymontek
 */
public abstract class IdentifierExpression extends Expression implements ExpressionWithScope {

	// 标识符的限定表达式（对于simple标识符可以为空）
	protected Expression qualifyingExpression;

	// 要解析的标识符
	protected final String identifier;

	// 标识符引用点的作用域（对于合格标识符可以为空）
	protected Scope scope;

	// 解析标识符符号
	protected Symbol resolvedIdentifier;

	/**
	 * 创建新的标识符表达式。
	 *
	 * @param position  源代码表达式的起始位置
	 * @param scope  标识符引用点的作用域
	 * @param qualifyingExpression  标识符的限定表达式
	 * @param identifier  要解析的标识符
	 */
	public IdentifierExpression(long position, Scope scope, Expression qualifyingExpression,
			String identifier) {
		super(position);

		this.scope = scope;
		this.qualifyingExpression = qualifyingExpression;
		this.identifier = identifier;
	}

	/**
	 * 返回解析的标识符。
	 *
	 * @return  已解析标识符
	 */
	public Symbol getResolvedIdentifier() {
		return resolvedIdentifier;
	}

	/**
	 * 将调用表达式绑定到已经按参数个数选中的具体函数或过程。
	 *
	 * @param function 选中的重载
	 */
	void selectFunction(FunctionSymbol function) {
		resolvedIdentifier = function;
		type = function.getType();
	}

	public Scope getScope() {
		if (resolvedIdentifier instanceof NamespaceSymbol) {
			// 如果标识符是命名空间标识符，则返回其作用域
			return ((NamespaceSymbol) resolvedIdentifier).getScope();
		} else if (type != null && type instanceof ObjectType) {
			// 如果标识符具有对象类型，则返回对象的作用域
			return ((ObjectType) type).getScope();
		} else {
			// 否则返回引用点处的作用域
			return scope;
		}
	}

	/**
	 * 检查标识符表达式是否已成功解析。
	 * 否则会报告错误。
	 *
	 * @param compiler  当前编译器实例
	 * @return  {@code false}如果报告了错误，否则{@code true}
	 */
	protected boolean checkFound(Compiler compiler) {
		if (resolvedIdentifier == null) {
			if (!(scope instanceof ErrorScope)) {
				compiler.error(getPosition(), Error.errIdentifierNotFound, identifier);
			}
			return false;
		}

		return !resolvedIdentifier.isErrorSymbol();
	}

	@Override
	public boolean isAssignable() {
		if (resolvedIdentifier instanceof LocalVariableSymbol) {
			return true;
		}

		if (resolvedIdentifier instanceof DataMemberSymbol) {
			return !(resolvedIdentifier instanceof ConstantDataMemberSymbol);
		}

		if (resolvedIdentifier instanceof PropertySymbol) {
			return true;
		}

		return resolvedIdentifier.isErrorSymbol();
	}

	@Override
	public Expression fold(Compiler compiler, FunctionSymbol currentFunction) {
		if (resolvedIdentifier instanceof ConstantDataMemberSymbol) {
			return ((ConstantDataMemberSymbol) resolvedIdentifier).getConstant();
		} else {
			return this;
		}
	}

	@Override
	public void generate(Method m) {
		if (qualifyingExpression != null) {
			qualifyingExpression.generate(m);
		}

		if (resolvedIdentifier == null) {
			// 运行时解析
			Preconditions.checkState(type.isVariantType());
			m.generateInstrLdc(identifier);
			m.generateInstrInvokevirtual(VariantType.VARIANT_INTERNAL_NAME, "dataMember",
					"(Ljava/lang/String;)L" + VariantType.VARIANT_INTERNAL_NAME + ';');

		} else {
			resolvedIdentifier.generateRead(m);
		}
	}

	@Override
	public void generatePrepareWrite(Method m) {
		if (qualifyingExpression != null) {
			qualifyingExpression.generate(m);
		}

		if (resolvedIdentifier == null) {
			m.generateInstrLdc(identifier);
		}
	}

	@Override
	public void generateWrite(Method m) {
		if (resolvedIdentifier == null) {
			// 运行时解析
			Preconditions.checkState(type.isVariantType());
			m.generateInstrInvokevirtual(VariantType.VARIANT_INTERNAL_NAME, "dataMember",
					"(Ljava/lang/String;L" + VariantType.VARIANT_INTERNAL_NAME + ";)V");
		} else {
			resolvedIdentifier.generateWrite(m);
		}
	}

	@Override
	public void generatePrepareInvoke(Method m) {
		if (qualifyingExpression != null) {
			qualifyingExpression.generate(m);
		}
	}

	@Override
	public void generateInvoke(Method m) {
		FunctionSymbol functionSymbol = (FunctionSymbol) resolvedIdentifier;
		ObjectSymbol functionObjectSymbol = functionSymbol.getDefiningObject();
		String functionClassInternalName = functionObjectSymbol.getType().internalName();
		String functionName = functionSymbol.getName();
		String functionSignature = functionSymbol.getType().signature();

		// 确定适当的调用字节码
		if (functionSymbol instanceof InstanceFunctionSymbol) {
			if (functionObjectSymbol.isInterface()) {
				m.generateInstrInvokeinterface(functionClassInternalName, functionName, functionSignature);
			} else {
				m.generateInstrInvokevirtual(functionClassInternalName, functionName, functionSignature);
			}
		} else {
			m.generateInstrInvokestatic(functionClassInternalName, functionName, functionSignature);
		}
	}
}

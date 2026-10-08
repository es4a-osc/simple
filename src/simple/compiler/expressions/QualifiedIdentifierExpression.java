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
import simple.compiler.scopes.ObjectScope;
import simple.compiler.symbols.FunctionSymbol;
import simple.compiler.symbols.InstanceMember;
import simple.compiler.symbols.ObjectSymbol;
import simple.compiler.symbols.SymbolWithType;
import simple.compiler.symbols.synthetic.ErrorSymbol;
import simple.compiler.types.VariantType;
import simple.compiler.types.synthetic.ErrorType;

/**
 * 此类表示显式限定的标识符表达式。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public final class QualifiedIdentifierExpression extends IdentifierExpression {

	/**
	 * 创建新的限定标识符表达式。
	 *
	 * @param position  源代码表达式的起始位置
	 * @param qualifyingExpression  限定表达式
	 * @param identifier  要解析的标识符
	 */
	public QualifiedIdentifierExpression(long position, Expression qualifyingExpression,
			String identifier) {
		super(position, null, qualifyingExpression, identifier);
	}

	@Override
	public Expression resolve(Compiler compiler, FunctionSymbol currentFunction) {
		qualifyingExpression = qualifyingExpression.resolve(compiler, currentFunction);

		// 如果限定表达式具有变量类型，则将在运行时解析名称
		if (qualifyingExpression.type != null && qualifyingExpression.type.isVariantType()) {
			// 我们将类型设置为变体型（variant），将解析延迟到运行时
			type = VariantType.variantType;
			return this;
		} else if (qualifyingExpression instanceof ExpressionWithScope) {
			// 在限定表达式的作用域内解析标识符
			scope = ((ExpressionWithScope) qualifyingExpression).getScope();
			if (scope instanceof ObjectScope) {
				// 如果作用域属于一个对象，那么我们需要在对象层次结构中查找
				resolvedIdentifier = ((ObjectScope) scope).lookupInObject(identifier);
			} else {
				// 否则浅查找（仅在给定的作用域内）就够了
				resolvedIdentifier = scope.lookupShallow(identifier);
			}

			// 确保标识符已成功解析或报告错误
			if (!checkFound(compiler)) {
				resolvedIdentifier = new ErrorSymbol(identifier);
				scope.enterSymbol(resolvedIdentifier);
			}

			resolvedIdentifier.resolve(compiler, currentFunction);

			// 如果已解析的标识符具有类型，则使其成为此表达式的类型
			if (resolvedIdentifier instanceof SymbolWithType) {
				type = ((SymbolWithType) resolvedIdentifier).getType();
				type.resolve(compiler);
			}

			// xhwsd@qq.com 2021-5-27 检验直接以“对象名.实例成员”方式引用需实例化的事件、过程或函数
			// 限定标识符是否是对象名
			if (qualifyingExpression instanceof IdentifierExpression &&
					((IdentifierExpression) qualifyingExpression).resolvedIdentifier instanceof ObjectSymbol) {
				// 检验标识符是否是实例成员（需要实例化才可调用的成员）
				if (resolvedIdentifier instanceof InstanceMember) {
					compiler.error(getPosition(), Error.errInstanceMemberWithoutMe, identifier);
				}
			}
		} else {
			compiler.error(getPosition(), Error.errIdentifierDoesNotResolveToSymbol, toString());
			type = ErrorType.errorType;
		}

		return fold(compiler, currentFunction);
	}

	@Override
	public boolean isAssignable() {
		// 对于运行时解析，resolvedIdentifier 为 null。我们默认假设它是可转让的。
		return resolvedIdentifier == null ? true : super.isAssignable();
	}

	@Override
	public String toString() {
		return qualifyingExpression.toString() + '.' + identifier;
	}
}

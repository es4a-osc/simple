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

package simple.compiler.types;

import simple.compiler.Compiler;
import simple.compiler.Error;
import simple.compiler.expressions.Expression;
import simple.compiler.expressions.IdentifierExpression;
import simple.compiler.symbols.ObjectSymbol;
import simple.compiler.symbols.Symbol;
import simple.util.Preconditions;

/**
 * 表示未解析的对象类型。
 *
 * @author Herbert Czymontek
 */
public final class UnresolvedType extends ObjectType {

	// 类型名称表达式
	private Expression expression;

	/**
	 * 创建新的未解析对象类型。
	 *
	 * @param expression  未解析的类型名表达式
	 */
	public UnresolvedType(Expression expression) {
		super(null);

		this.expression = expression;
	}

	@Override
	public void resolve(Compiler compiler) {
		expression = expression.resolve(compiler, null);
		if (!(expression instanceof IdentifierExpression)) {
			compiler.error(expression.getPosition(), Error.errObjectTypeNeeded, expression.toString());
		} else {
			Symbol symbol = ((IdentifierExpression) expression).getResolvedIdentifier();
			// 检验标识是否为空，为空将排除异常终止执行。
			Preconditions.checkNotNull(symbol);

			if (!(symbol instanceof ObjectSymbol)) {
				// 如果符号是特殊错误符号，则无需报告另一个错误
				if (!symbol.isErrorSymbol()) {
					compiler.error(expression.getPosition(), Error.errObjectTypeNeeded, symbol.getName());
				}
			} else {
				// 这是为了确保加载的类被延迟解析。
				// 我们不希望对已编译类执行此操作以避免难看的递归解析检测。
				objectSymbol = (ObjectSymbol) symbol;
				if (objectSymbol.isLoaded()) {
					objectSymbol.resolve(compiler, null);
				}
			}
		}
	}
}

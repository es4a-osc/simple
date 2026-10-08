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
import simple.compiler.symbols.AliasSymbol;
import simple.compiler.symbols.FunctionSymbol;

/**
 * 此类表示定义符号备用名称的别名表达式。
 *
 * @author Herbert Czymontek
 */
public final class AliasExpression extends Expression {
	// 新备用名称
	private final String aliasName;

	// 原名表达式
	private final IdentifierExpression aliasExpr;

	// 备用名称符号
	private AliasSymbol aliasSymbol;

	/**
	 * 创建新的别名表达式
	 *
	 * @param position  源代码表达式的起始位置
	 * @param aliasName  备用名称
	 * @param aliasExpr  原名表达式
	 */
	public AliasExpression(long position, String aliasName, IdentifierExpression aliasExpr) {
		super(position);

		this.aliasName = aliasName;
		this.aliasExpr = aliasExpr;
	}

	/**
	 * 返回备用名称的符号。
	 *
	 * @return  备用名称符号
	 */
	public AliasSymbol getAliasSymbol() {
		return aliasSymbol;
	}

	@Override
	public Expression resolve(Compiler compiler, FunctionSymbol currentFunction) {
		aliasExpr.resolve(compiler, currentFunction);
		aliasSymbol = new AliasSymbol(getPosition(), aliasName, aliasExpr.getResolvedIdentifier());

		return this;
	}

	@Override
	public void generate(Method m) {
		Compiler.internalError();  // COV_NF_LINE
	}
}

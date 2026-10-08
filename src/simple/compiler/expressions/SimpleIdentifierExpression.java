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
import simple.compiler.scopes.Scope;
import simple.compiler.symbols.FunctionSymbol;
import simple.compiler.symbols.InstanceMember;
import simple.compiler.symbols.ObjectSymbol;
import simple.compiler.symbols.SymbolWithType;
import simple.compiler.symbols.synthetic.ErrorSymbol;
import simple.compiler.types.ObjectType;
import simple.compiler.types.synthetic.ErrorType;

/**
 * 这个类表示一个标识符表达式，没有任何进一步的显式限定（只是一个单独的标识符）。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public final class SimpleIdentifierExpression extends IdentifierExpression {

	/**
	 * 创建一个新的simple的非限定标识符表达式
	 *
	 * @param position  源代码表达式的起始位置
	 * @param scope  参考点的范围
	 * @param identifier  要解析的标识符
	 */
	public SimpleIdentifierExpression(long position, Scope scope, String identifier) {
		super(position, scope, null, identifier);
	}

	@Override
	public Expression resolve(Compiler compiler, FunctionSymbol currentFunction) {
		// 解析标识符
		resolvedIdentifier = scope.lookupDeep(identifier);

		// 检查结果
		if (!checkFound(compiler)) {
			createErrorIdentifier();
		}

		resolvedIdentifier.resolve(compiler, currentFunction);

		// 检查解析的标识符是否是实例成员，因此是否意味着本对象（Me）引用
		if (resolvedIdentifier instanceof InstanceMember) {
			ObjectSymbol memberClass = ((InstanceMember) resolvedIdentifier).getDefiningObject();
			// xhwsd@qq.com 2021-5-27 检验静态事件、过程、函数中调用需要实例化的变量、过程、函数
			// 当前函数是静态的，而当前标识是需要实例化的
			if (currentFunction == null || !currentFunction.hasMeArgument()) {
				// This indicates that we looked up the 'wrong' symbol, e.g. one symbol hiding another from
				// an outer scope which would have been the 'correct' symbol
				compiler.error(getPosition(), Error.errInstanceMemberWithoutMe, identifier);
				createErrorIdentifier();
			} else {
				ObjectSymbol currentClass = currentFunction.getDefiningObject();

				MeExpression me = new MeExpression(getPosition(), currentClass);
				qualifyingExpression = me;

				// 如果成员是在外部类中定义的，那么我们需要使用外部类Me
				//（我们只需检查它是否在当前类层次结构中定义，如果不是，则假定它在外部类中）
				ObjectType currentClassType = (ObjectType) currentClass.getType();
				ObjectType memberClassType = (ObjectType) memberClass.getType();
				if (!currentClassType.equals(memberClassType) &&
						!currentClassType.isBaseObject(memberClassType)) {
					me.setOuterClass(currentClass.getOuterclass());
				}
			}
		}

		// If the identifier has a type then the expression will have the same type
		if (resolvedIdentifier instanceof SymbolWithType) {
			type = ((SymbolWithType) resolvedIdentifier).getType();
			assert type != null;
			type.resolve(compiler);
		}

		return fold(compiler, currentFunction);
	}

	private void createErrorIdentifier() {
		resolvedIdentifier = new ErrorSymbol(identifier);
		scope.enterSymbol(resolvedIdentifier);
		type = ErrorType.errorType;
	}

	@Override
	public String toString() {
		return qualifyingExpression != null ? qualifyingExpression.toString() + '.' + identifier :
				identifier;  // COV_NF_LINE
	}
}

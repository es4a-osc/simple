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
import simple.compiler.expressions.Expression;
import simple.compiler.scanner.Scanner;
import simple.compiler.symbols.FunctionSymbol;
import simple.compiler.types.ObjectType;
import simple.compiler.types.Type;

/**
 * 此类用于组件实例化表达式。
 *
 * @author Herbert Czymontek
 */
public final class NewComponentExpression extends Expression {

	// 组件容器
	private Expression container;

	// 组件实现的内部名称
	private String internalName;

	/**
	 * 创建新的组件实例化表达式。
	 *
	 * @param container  组件所在的容器
	 * @param type  要实例化的组件类型
	 */
	public NewComponentExpression(Expression container, Type type) {
		super(Scanner.NO_POSITION);

		this.type = type;
		this.container = container;
	}

	@Override
	public Expression resolve(Compiler compiler, FunctionSymbol currentFunction) {
		type.resolve(compiler);

		container = container.resolve(compiler, currentFunction);

		internalName = compiler.getComponentImplementationInternalName((ObjectType) type);

		return this;
	}

	@Override
	public void generate(Method m) {
		m.generateInstrNew(internalName);
		m.generateInstrDup();
		container.generate(m);
		m.generateInstrInvokespecial(internalName, "<init>",
				"(L" + Compiler.RUNTIME_ROOT_INTERNAL + "/components/组件容器;)V");
		m.generateInstrDup();
		m.generateInstrInvokestatic(Compiler.RUNTIME_ROOT_INTERNAL + "/Objects",
				"initializeProperties", "(Ljava/lang/Object;)V");
	}

	@Override
	public String toString() {
		return "创建 " + type.toString() + " 位于 " + container.toString();
	}
}

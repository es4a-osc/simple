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

import simple.classfiles.Method;
import simple.compiler.Compiler;
import simple.compiler.scanner.Scanner;
import simple.compiler.scopes.GlobalScope;
import simple.compiler.scopes.NamespaceScope;
import simple.compiler.scopes.Scope;
import simple.util.Files;
import simple.util.Preconditions;

import java.io.File;

/**
 * 用于定义命名空间的符号。
 *
 * @author Herbert Czymontek
 */
public final class NamespaceSymbol extends Symbol implements SymbolCanBePartOfQualifiedIdentifier {

	// 包含命名空间
	private NamespaceSymbol outerNamespace;

	// 命名空间作用域
	private Scope scope;

	/**
	 * 命名空间符号的工厂方法。
	 *
	 * @param compiler  当前编译器实例
	 * @param packageName  内部格式的命名空间的包名称
	 * @return  命名空间符号
	 */
	public static NamespaceSymbol getNamespaceSymbol(Compiler compiler, String packageName) {
		// 搜索现有命名空间符号始终以全局/根命名空间开始
		NamespaceSymbol outerNamespace = compiler.getGlobalNamespaceSymbol();

		for (;;) {
			// 查找下一个包的范围
			Scope scope = outerNamespace.getScope();
			// xhwsd@qq.com 2021-7-1 修复内部命名空间分割符（从 . 修改为 /）。
			int index = packageName.indexOf('/');
			if (index == -1) {
				// 最后一个包组件
				NamespaceSymbol namespace = (NamespaceSymbol)scope.lookupShallow(packageName);
				return namespace == null ? new NamespaceSymbol(packageName, outerNamespace) : namespace;
			} else {
				// 包前缀 aaa/bbb/ccc -> aaa
				String name = packageName.substring(0, index);
				// 取包后缀 aaa/bbb/ccc -> bbb/ccc
				packageName = packageName.substring(index + 1);
				// 在当前作用域中查找 包前缀
				NamespaceSymbol namespace = (NamespaceSymbol)scope.lookupShallow(name);
				// 如果未找到则新建，否则使用使用已有。
				outerNamespace = namespace == null ? new NamespaceSymbol(name, outerNamespace) : namespace;
			}
		}
	}

	/*
	 * 创建新的嵌套命名空间符号。
	 */
	private NamespaceSymbol(String name, NamespaceSymbol outerNamespace) {
		super(Scanner.NO_POSITION, name);
		Preconditions.checkArgument(outerNamespace != null);

		this.outerNamespace = outerNamespace;

		Scope outerScope = outerNamespace.getScope();
		scope = new NamespaceScope(outerScope);
		outerScope.enterSymbol(this);
	}

	/**
	 * 为全局作用域创建新的根命名空间符号。
	 *
	 * <p>请注意，对于任何编译器实例，
	 * 此构造函数只能调用一次，最好在其构造函数中调用。
	 */
	public NamespaceSymbol() {
		super(Scanner.NO_POSITION, null);
		scope = new GlobalScope();
	}

	public Scope getScope() {
		return scope;
	}

	@Override
	public void generateRead(Method m) {
	}

	@Override
	public void generateWrite(Method m) {
	}

	/**
	 * 生成文件系统上命名空间的目录结构。
	 *
	 * @param rootDir  命名空间结构的根目录
	 * @return  命名空间的文件描述符
	 */
	public File generate(File rootDir) {
		if (outerNamespace == null) {
			return rootDir;
		}

		return Files.createDirectory(outerNamespace.generate(rootDir), getName());
	}

	/**
	 * 返回命名空间的内部名称。
	 *
	 * @return  内部命名空间名称
	 */
	public String internalName() {
		return (outerNamespace == null) ? "" : (outerNamespace.internalName() + getName() + '/');
	}

	/*
	 * 返回 toString() 的外部命名空间。
	 */
	private String prependOuterNamespaceName() {
		return (outerNamespace == null) ? "" :
				(outerNamespace.prependOuterNamespaceName() + getName() + ':');
	}

	@Override
	public String toString() {
		return "<命名空间> " + prependOuterNamespaceName();
	}
}

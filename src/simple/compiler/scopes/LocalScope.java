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

package simple.compiler.scopes;

import simple.compiler.statements.synthetic.MarkerStatement;
import simple.compiler.symbols.LocalVariableSymbol;
import simple.compiler.symbols.Symbol;

/**
 * 函数和语句块的作用域。
 *
 * @author Herbert Czymontek
 */
public class LocalScope extends Scope {

	/**
	 * 创建新的本地作用域（用于函数或语句块）。
	 *
	 * @param outerScope  封闭作用域
	 */
	public LocalScope(Scope outerScope) {
		super(outerScope);
	}

	/**
	 * 标记在此作用域中定义的所有局部变量的作用域结束。
	 *
	 * @param marker  结束作用域标记
	 */
	public void markEndOfScope(MarkerStatement marker) {
		for (Symbol symbol : symbolMap.values()) {
			if (symbol instanceof LocalVariableSymbol) {
				((LocalVariableSymbol) symbol).setEndScope(marker);
			}
		}
	}
}

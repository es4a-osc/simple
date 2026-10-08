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

import simple.compiler.symbols.Symbol;

/**
 * 全局作用域（它是所有其他作用域的最外层作用域）。
 *
 * @author Herbert Czymontek
 */
public final class GlobalScope extends Scope {

	/**
	 * 创建全局作用域。
	 */
	public GlobalScope() {
		super(null);
	}

	@Override
	public final Symbol lookupShallow(String identifier) {
		// 只有全局作用域可以包含别名标识符。因此，我们需要调用 getActualSymbol() 方法来只返回实际标识符！
		Symbol symbol = super.lookupShallow(identifier);
		return symbol != null ? symbol.getActualSymbol() : null;
	}

	@Override
	public Symbol lookupDeep(String identifier) {
		return lookupShallow(identifier);
	}
}

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

import java.util.HashMap;

/**
 * 所有作用域实现的超类。
 *
 * @author Herbert Czymontek
 */
public abstract class Scope {

	// 将标识符映射到符号
	protected final HashMap<String, Symbol> symbolMap;

	// 封闭作用域
	protected final Scope outerScope;

	/**
	 * 创建新作用域。
	 *
	 * @param outerScope  封闭作用域
	 */
	public Scope(Scope outerScope) {
		this.outerScope = outerScope;

		symbolMap = new HashMap<String, Symbol>();
	}

	/**
	 * 向作用域添加符号。
	 *
	 * @param symbol  要添加的符号
	 */
	public void enterSymbol(Symbol symbol) {
		symbolMap.put(symbol.getName(), symbol);
	}

	/**
	 * 从作用域中删除符号
	 *
	 * @param symbol  要删除的符号
	 */
	public void removeSymbol(Symbol symbol) {
		symbolMap.remove(symbol.getName());
	}

	/**
	 * 在此作用域内查找标识符。
	 *
	 * @param identifier  查找标识符
	 * @return  找到符号或{@code null}
	 */
	public Symbol lookupShallow(String identifier) {
		return symbolMap.get(identifier);
	}

	/**
	 * 首先在此作用域内查找标识符，如果无法则在任何封闭作用域内找到。
	 *
	 * @param identifier  查找标识符
	 * @return  找到符号或{@code null}
	 */
	public Symbol lookupDeep(String identifier) {
		Symbol symbol = lookupShallow(identifier);
		if (symbol != null) {
			return symbol;
		}
		return outerScope.lookupDeep(identifier);
	}
}

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

import simple.compiler.symbols.ObjectSymbol;
import simple.compiler.symbols.Symbol;
import simple.compiler.types.ObjectType;

/**
 * 对象的作用域。
 *
 * @author Herbert Czymontek
 */
public final class ObjectScope extends Scope {
	// 此作用域对应的对象符号
	private ObjectSymbol objectSymbol;

	/**
	 * 创建新的对象作用域。
	 *
	 * @param objectSymbol  对应对象符号
	 * @param outerScope  封闭作用域
	 */
	public ObjectScope(ObjectSymbol objectSymbol, Scope outerScope) {
		super(outerScope);
		this.objectSymbol = objectSymbol;
	}

	/**
	 * 首先在这个对象中查找标识符，如果未找到则在任何基本对象作用域中查找。
	 *
	 * @param identifier  要查找的标识符
	 * @return  找到符号或{@code null}
	 */
	public Symbol lookupInObject(String identifier) {
		ObjectType objectType = (ObjectType)objectSymbol.getType();
		do {
			Symbol symbol =
					objectType.getObjectSymbol().getScope().lookupShallow(identifier);
			if (symbol != null) {
				return symbol;
			}

			objectType = objectType.getObjectSymbol().getBaseObject();
		} while (objectType != null);

		return null;
	}

	@Override
	public Symbol lookupDeep(String identifier) {
		Symbol symbol = lookupInObject(identifier);
		if (symbol != null) {
			return symbol;
		}

		return outerScope.lookupDeep(identifier);
	}
}

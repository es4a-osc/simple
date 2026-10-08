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

package simple.compiler.symbols.synthetic;

import simple.classfiles.Method;
import simple.compiler.Compiler;
import simple.compiler.scopes.Scope;
import simple.compiler.scopes.synthetic.ErrorScope;
import simple.compiler.symbols.Symbol;
import simple.compiler.symbols.SymbolCanBePartOfQualifiedIdentifier;
import simple.compiler.symbols.SymbolWithType;
import simple.compiler.types.Type;
import simple.compiler.types.synthetic.ErrorType;

/**
 * Symbol for error situations.
 *
 * @author Herbert Czymontek
 */
public final class ErrorSymbol extends Symbol
		implements SymbolWithType, SymbolCanBePartOfQualifiedIdentifier {

	private final ErrorScope scope;

	/**
	 * Creates a new symbol for use when an undefined identifier was found.
	 *
	 * @param name  symbol name
	 */
	public ErrorSymbol(String name) {
		super(0, name);
		scope = new ErrorScope();
	}

	@Override
	public boolean isErrorSymbol() {
		return true;
	}

	@Override
	public Symbol getActualSymbol() {
		return this;
	}

	@Override
	public void generateRead(Method m) {
		Compiler.internalError();  // COV_NF_LINE
	}

	@Override
	public void generateWrite(Method m) {
		Compiler.internalError();  // COV_NF_LINE
	}

	@Override
	public Type getType() {
		return ErrorType.errorType;
	}

	@Override
	public void setType(Type type) {
	}

	@Override
	public Scope getScope() {
		return scope;
	}
}

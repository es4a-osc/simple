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

/**
 * 作为另一个符号的别名使用的符号。
 *
 * @author Herbert Czymontek
 */
public final class AliasSymbol extends Symbol {

	// 别名所指的实际符号
	private final Symbol actualSymbol;

	/**
	 * 创建一个新的别名符号。
	 *
	 * @param position  源代码的符号开始位置
	 * @param name  别名的名称
	 * @param actualSymbol  符号别名指的是
	 */
	public AliasSymbol(long position, String name, Symbol actualSymbol) {
		super(position, name);
		this.actualSymbol = actualSymbol;
	}

	@Override
	public Symbol getActualSymbol() {
		return actualSymbol;
	}

	@Override
	public void generateRead(Method m) {
		Compiler.internalError();  // COV_NF_LINE
	}

	@Override
	public void generateWrite(Method m) {
		Compiler.internalError();  // COV_NF_LINE
	}
}

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

import simple.compiler.Compiler;
import simple.compiler.Error;
import simple.compiler.scanner.TokenKind;
import simple.compiler.scopes.ObjectScope;
import simple.compiler.scopes.Scope;
import simple.compiler.scopes.synthetic.ErrorScope;
import simple.compiler.types.ObjectType;
import simple.compiler.types.Type;

/**
 * 事件处理器的符号。
 *
 * @author Herbert Czymontek
 */
public final class EventHandlerSymbol extends FunctionSymbol implements InstanceMember {

	// 事件处理器应适用的数据成员或类名
	private final String eventTargetName;

	// 事件名称
	private final String eventName;

	/**
	 * 创建一个新的事件处理器符号。
	 *
	 * @param position 源代码的符号开始位置
	 * @param objectSymbol 定义对象
	 * @param eventTargetName 事件处理器应适用的数据成员或类名
	 * @param eventName 事件处理器名称
	 */
	public EventHandlerSymbol(long position, ObjectSymbol objectSymbol,
			String eventTargetName, String eventName) {
		super(position, objectSymbol, eventTargetName + '$' + eventName);

		this.eventTargetName = eventTargetName;
		this.eventName = eventName;

		setIsCompiled();
	}

	/**
	 * 返回应用事件处理器的目标的名称。
	 *
	 * @return 事件处理器目标的名称
	 */
	public String getEventTargetName() {
		return eventTargetName;
	}

	/**
	 * 返回此处理器实现的事件的名称。
	 *
	 * @return 事件名称
	 */
	public String getEventName() {
		return eventName;
	}

	@Override
	public TokenKind getExitToken() {
		return TokenKind.TOK_EVENT;
	}

	@Override
	public boolean hasMeArgument() {
		return !(eventTargetName.equals(getDefiningObject().getName()) && eventName.equals("加载"));
	}

	@Override
	public void resolve(Compiler compiler, FunctionSymbol currentFunction) {
		super.resolve(compiler, currentFunction);

		ObjectSymbol definingObject = getDefiningObject();
		if (eventTargetName.equals(definingObject.getName())) {
			// 类初始化事件处理器
			if (eventName.equals("加载")) {
				definingObject.setObjectLoadEventHandler(this);
				return;
			} else if (eventName.equals("初始化")) {
				definingObject.setInstanceInitializeEventHandler(this);
				return;
			}
		}

		// 数据成员绑定事件处理程序

		Symbol symbol = definingObject.getScope().lookupShallow(eventTargetName);
		if (symbol == null) {
			compiler.error(getPosition(), Error.errUndefinedSymbol, eventTargetName);
			return;
		}

		if (!(symbol instanceof DataMemberSymbol)) {
			compiler.error(getPosition(), Error.errDataMemberExpected, eventTargetName);
			return;
		}

		DataMemberSymbol targetDataMember = (DataMemberSymbol) symbol;
		Type targetDataMemberType = targetDataMember.getType();
		if (!targetDataMemberType.isObjectType()) {
			compiler.error(getPosition(), Error.errObjectTypeNeeded, targetDataMemberType.toString());
			return;
		}

		targetDataMemberType.resolve(compiler);

		Scope scope = ((ObjectType) targetDataMemberType).getScope();
		if (scope instanceof ErrorScope) {
			// 错误已经报道了
			return;
		}

		Symbol event = ((ObjectScope) scope).lookupInObject(eventName);
		if (event == null || !(event instanceof EventSymbol)) {
			compiler.error(getPosition(), Error.errUndefinedSymbol, eventName);
		}
	}
}

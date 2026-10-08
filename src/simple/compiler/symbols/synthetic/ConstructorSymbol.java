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

import simple.classfiles.ClassFile;
import simple.classfiles.Method;
import simple.compiler.Compiler;
import simple.compiler.scanner.Scanner;
import simple.compiler.symbols.DataMemberSymbol;
import simple.compiler.symbols.EventHandlerSymbol;
import simple.compiler.symbols.FunctionSymbol;
import simple.compiler.symbols.InstanceDataMemberSymbol;
import simple.compiler.symbols.ObjectSymbol;

/**
 * 默认构造函数的综合符号。
 *
 * <p>所有Simple对象都使用默认构造函数实例化之后，
 * 将触发一个初始化事件，该事件允许对实例进行特定的初始化。
 *
 * @author Herbert Czymontek
 */
public class ConstructorSymbol extends FunctionSymbol {

	/**
	 * 创建新的默认构造函数符号。
	 *
	 * @param objectSymbol  要由构造函数实例化的对象
	 */
	public ConstructorSymbol(ObjectSymbol objectSymbol) {
		super(Scanner.NO_POSITION, objectSymbol, "<init>");
	}

	@Override
	public boolean hasMeArgument() {
		return true;
	}

	@Override
	public void generate(Compiler compiler, ClassFile cf) {
		Method m = cf.newMethod(Method.ACC_PUBLIC, "<init>", getType().signature());

		m.startCodeGeneration();

		// 调用超类构造函数
		m.generateInstrAload((short) 0);
		ObjectSymbol definingObject = getDefiningObject();
		m.generateInstrInvokespecial(definingObject.getBaseObjectInternalName(compiler),
				"<init>", "()V");

		// 初始化静态大小的数据成员
		for (DataMemberSymbol dataMember : definingObject.getDataMembers()) {
			if (dataMember instanceof InstanceDataMemberSymbol) {
				dataMember.generateInitializer(m);
			}
		}

		// 注册事件处理器
		// 请注意，窗口在合成 $define() 方法中注册事件处理程序。
		// 在完成初始化事件之后，它们还会引发初始化事件。
		// TODO: forms shouldn't have special treatment for their Initialize events
		String internalName = definingObject.getType().internalName();
		if (definingObject.isForm()) {
			// Need to initialize properties before executing $define() method (otherwise property
			// initializers would overwrite changes made by $define()).
			m.generateInstrAload((short) 0);
			m.generateInstrInvokestatic(Compiler.RUNTIME_ROOT_INTERNAL + "/Objects",
					"initializeProperties", "(Ljava/lang/Object;)V");
			m.generateInstrAload((short) 0);
			m.generateInstrInvokevirtual(internalName, "$define", "()V");
		} else {
			for (EventHandlerSymbol eventHandler : definingObject.getEventHandlers()) {
				m.generateInstrAload((short) 0);
				m.generateInstrLdc(eventHandler.getEventTargetName());
				m.generateInstrLdc(eventHandler.getEventName());
				m.generateInstrInvokestatic(Compiler.RUNTIME_ROOT_INTERNAL + "/events/EventDispatcher",
						"registerEvent", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V");
			}

			// 触发实例初始化事件
			EventHandlerSymbol initializeEventHandler =
					definingObject.getInstanceInitializeEventHandler();
			if (initializeEventHandler != null) {
				m.generateInstrAload((short) 0);
				m.generateInstrInvokevirtual(internalName, initializeEventHandler.getName(),
						initializeEventHandler.getType().signature());
			}
		}

		m.generateInstrReturn();
		m.finishCodeGeneration();
	}
}

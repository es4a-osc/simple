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

import simple.classfiles.ClassFile;
import simple.classfiles.Field;
import simple.classfiles.Method;
import simple.compiler.RuntimeLoader;
import simple.compiler.types.Type;


/**
 * Symbol used for instance data members.
 *
 * @author Herbert Czymontek
 */
public final class InstanceDataMemberSymbol extends DataMemberSymbol implements InstanceMember {

	/**
	 * Creates a new instance data member.
	 *
	 * @param position  source code start position of symbol
	 * @param objectSymbol  defining object
	 * @param name  data member name
	 * @param type  data member type
	 */
	public InstanceDataMemberSymbol(long position, ObjectSymbol objectSymbol, String name,
			Type type) {
		super(position, objectSymbol, name, type);
	}

	@Override
	public void generateRead(Method m) {
		m.generateInstrGetfield(getDefiningObject().getType().internalName(),
				getName(), getType().signature());
	}

	@Override
	public void generateWrite(Method m) {
		m.generateInstrPutfield(getDefiningObject().getType().internalName(),
				getName(), getType().signature());
	}

	@Override
	public void generate(ClassFile cf) {
		cf.newField(Field.ACC_PUBLIC, getName(),
				getType().signature()).getRuntimeVisibleAnnotationsAttribute().newAnnotation(
						'L' + RuntimeLoader.ANNOTATION_INTERNAL + "/SimpleDataElement;");
	}

	@Override
	protected void prepareInitializerWrite(Method m) {
		// 只在确实写入默认值或分配数组时压入 this；显式初值走普通赋值 AST。
		m.generateInstrAload((short) 0);
	}

	@Override
	public String toString() {
		return "变量 " + super.toString();  // COV_NF_LINE
	}
}

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

import java.util.List;

import simple.classfiles.ClassFile;
import simple.classfiles.Method;
import simple.compiler.expressions.Expression;
import simple.compiler.types.StringType;
import simple.compiler.types.Type;

/**
 * 这是所有数据成员符号的超类。
 *
 * @author Herbert Czymontek
 */
public abstract class DataMemberSymbol extends VariableSymbol {

	// 定义数据成员的对象
	private final ObjectSymbol definingObject;

	/**
	 * 创建新的数据成员符号。
	 *
	 * @param position  源代码的符号开始位置
	 * @param objectSymbol  定义对象
	 * @param name  数据成员名称
	 * @param type  数据成员类型
	 */
	public DataMemberSymbol(long position, ObjectSymbol objectSymbol, String name, Type type) {
		super(position, name, type);
		this.definingObject = objectSymbol;
	}

	/**
	 * 返回定义数据成员的对象。
	 *
	 * @return  定义对象
	 */
	public ObjectSymbol getDefiningObject() {
		return definingObject;
	}

	/**
	 * 为类文件中的数据成员生成一个条目。
	 *
	 * @param cf  类文件
	 */
	public abstract void generate(ClassFile cf);

	@Override
	protected void generateInitializer(Method m, List<Expression> dimensions) {
		if (dimensions != null) {
			for (Expression dimension : dimensions) {
				dimension.generate(m);
			}
			getType().generateAllocateArray(m);
			generateWrite(m);
		} else if (getType() == StringType.stringType) {
			getType().generateDefaultInitializationValue(m);
			generateWrite(m);
		}
	}
}

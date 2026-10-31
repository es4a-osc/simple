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
import simple.compiler.Compiler;
import simple.compiler.types.StringType;
import simple.compiler.types.Type;

/**
 * 这是所有数据成员符号的超类。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public abstract class DataMemberSymbol extends VariableSymbol {

	// 定义数据成员的对象
	private final ObjectSymbol definingObject;

	// 初值使用普通赋值 AST；只由所属对象显式解析，读取字段不会递归解析其初值。
	private Expression initializer;

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

	/**
	 * 设置声明处的赋值表达式；字段引用时的声明解析不会递归解析此初值。
	 *
	 * @param initializer  包含字段写入的赋值表达式；未指定初值时为 {@code null}
	 */
	public final void setInitializer(Expression initializer) {
		this.initializer = initializer;
	}

	/**
	 * 在所有声明可用后，于对应构造器或静态初始化上下文中解析初值。
	 *
	 * @param compiler  当前编译器实例
	 * @param context  实例构造器或静态初始化函数的解析上下文
	 */
	public final void resolveInitializer(Compiler compiler, FunctionSymbol context) {
		if (initializer != null) {
			initializer = initializer.resolve(compiler, context);
		}
	}

	/**
	 * 为数组分配或文本默认值的字段写入准备接收对象。
	 * 静态字段无需接收对象；实例字段由子类压入接收对象。
	 *
	 * @param m  生成字段初始化代码的 JVM 方法
	 */
	protected void prepareInitializerWrite(Method m) {
	}

	@Override
	protected void generateInitializer(Method m, List<Expression> dimensions) {
		if (initializer != null) {
			// 普通赋值自行准备接收者，并执行与独立赋值相同的类型转换和 checkcast。
			initializer.generate(m);
		} else if (dimensions != null) {
			prepareInitializerWrite(m);
			for (Expression dimension : dimensions) {
				dimension.generate(m);
			}
			getType().generateAllocateArray(m);
			generateWrite(m);
		} else if (getType() == StringType.stringType) {
			prepareInitializerWrite(m);
			getType().generateDefaultInitializationValue(m);
			generateWrite(m);
		}
	}
}

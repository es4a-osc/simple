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

package simple.compiler.expressions;

import java.util.List;

import simple.classfiles.Method;
import simple.compiler.Compiler;
import simple.compiler.Error;
import simple.compiler.symbols.FunctionSymbol;
import simple.compiler.types.ArrayType;
import simple.compiler.types.IntegerType;
import simple.compiler.types.ObjectType;
import simple.compiler.types.Type;

/**
 * 表示对象和数组实例化表达式。
 *
 * @author Herbert Czymontek
 */
public final class NewExpression extends Expression {

	// 新数组表达式的维度（否则为空）
	List<Expression> dimensions;

	/**
	 * 创建新的对象或数组实例化表达式。
	 *
	 * @param position  源代码表达式的起始位置
	 * @param type  要实例化的对象或数组的类型
	 * @param dimensions  新数组的维度（否则{@code null}）
	 */
	public NewExpression(long position, Type type, List<Expression> dimensions) {
		super(position);

		this.type = type;
		this.dimensions = dimensions;
	}

	@Override
	public Expression resolve(Compiler compiler, FunctionSymbol currentFunction) {
		type.resolve(compiler);

		if (dimensions != null) {
			// 解析数组的维度
			if (!type.isArrayType()) {
				Compiler.internalError();  // COV_NF_LINE
			}

			int argDimensions = dimensions.size();
			int arrayDimensions = ((ArrayType) type).getDimensions();
			if (arrayDimensions != argDimensions) {
				compiler.error(getPosition(),
						argDimensions > arrayDimensions ? Error.errTooManyIndices : Error.errTooFewIndices);
			}

			for (int i = 0; i < dimensions.size(); i++) {
				Expression dimension = dimensions.get(i);
				if (dimension == null) {
					compiler.error(getPosition(), Error.errArrayDimensionExpected);
				} else {
					dimensions.set(i, dimension.resolve(compiler, currentFunction).checkType(compiler,
							IntegerType.integerType));
				}
			}
		} else if (!type.isObjectType()) {
			compiler.error(getPosition(), Error.errObjectOrArrayTypeNeeded);
		}

		return this;
	}

	@Override
	public void generate(Method m) {
		if (type.isArrayType()) {
			ArrayType arrayType = (ArrayType) type;
			for (Expression dimension : dimensions) {
				dimension.generate(m);
			}
			arrayType.generateAllocateArray(m);
		} else {
			m.generateInstrNew(type.internalName());
			m.generateInstrDup();
			m.generateInstrInvokespecial(type.internalName(), "<init>", "()V");
			// 除了窗口之外，我们总是在构造函数之后立即调用属性初始值设定项。
			// 这样，无论对象是用 Java 还是 Simple 定义的，属性都将正确初始化。
			// 窗口需要区别对待，因为它们的初始化顺序更复杂（参见 ConstructorSymbol 类中的注释）。
			if (!((ObjectType) type).getObjectSymbol().isForm()) {
				m.generateInstrDup();
				m.generateInstrInvokestatic(Compiler.RUNTIME_ROOT_INTERNAL + "/Objects",
						"initializeProperties", "(Ljava/lang/Object;)V");
			}
		}
	}

	@Override
	public String toString() {
		return "创建 " + type.toString(); // COV_NF_LINE
	}
}

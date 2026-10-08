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

package simple.runtime.variants;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import simple.runtime.errors.未知标识符错误;
import simple.runtime.helpers.ConvHelpers;

/**
 * 对象变体实现。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public final class ObjectVariant extends Variant {

	// 对象值
	private Object value;

	/**
	 * 用于创建对象变体的工厂方法。
	 *
	 * @param value  对象值
	 * @return  新对象变体
	 */
	public static final ObjectVariant getObjectVariant(Object value) {
		return new ObjectVariant(value);
	}

	/*
	 * 创建一个新的对象变量。
	 */
	private ObjectVariant(Object value) {
		super(VARIANT_OBJECT);
		this.value = value;
	}

	@Override
	public Object getObject() {
		return value;
	}

	@Override
	public boolean identical(Variant rightOp) {
		if (rightOp.getKind() != VARIANT_OBJECT) {
			// 将导致运行时错误
			return super.identical(rightOp);
		}

		return value == rightOp.getObject();
	}

	/*
	 * 将变体值转换为等效的Java值。
	 */
	private Object convertVariant(Class<?> type, Variant variant) {
		if (type.isArray()) {
			return variant.getArray();
		}
		if (type == Boolean.TYPE) {
			return variant.getBoolean();
		}
		if (type == Byte.TYPE) {
			return variant.getByte();
		}
		if (type == Short.TYPE) {
			return variant.getShort();
		}
		if (type == Integer.TYPE) {
			return variant.getInteger();
		}
		if (type == Long.TYPE) {
			return variant.getLong();
		}
		if (type == Float.TYPE) {
			return variant.getSingle();
		}
		if (type == Double.TYPE) {
			return variant.getDouble();
		}
		if (type == String.class) {
			return variant.getString();
		}

		// 解决调用函数或方法参数类型为变体型的异常 xhwsd@qq.com 2021-6-8
		if (type == Variant.class) {
			// 向上转型
			return variant;
		}

		return variant.getObject();
	}

	/*
	 * 将Java值转换为变体值。
	 */
	private Variant convertObject(Class<?> type, Object object) {
		if (type == Void.TYPE) {
			return UninitializedVariant.getUninitializedVariant();
		}
		if (type == Boolean.TYPE) {
			return BooleanVariant.getBooleanVariant(((Boolean)object).booleanValue());
		}
		if (type == Byte.TYPE) {
			return ByteVariant.getByteVariant(ConvHelpers.integer2byte(((Byte)object).intValue()));
		}
		if (type == Short.TYPE) {
			return ShortVariant.getShortVariant(ConvHelpers.integer2short(((Short)object).intValue()));
		}
		if (type == Integer.TYPE) {
			return IntegerVariant.getIntegerVariant(((Integer)object).intValue());
		}
		if (type == Long.TYPE) {
			return LongVariant.getLongVariant(((Long)object).longValue());
		}
		if (type == Float.TYPE) {
			return SingleVariant.getSingleVariant(((Float)object).floatValue());
		}
		if (type == Double.TYPE) {
			return DoubleVariant.getDoubleVariant(((Double)object).doubleValue());
		}
		if (type == String.class) {
			return StringVariant.getStringVariant((String)object);
		}
		if (type.isArray()) {
			return ArrayVariant.getArrayVariant(object);
		}

		// 解决调用函数返回类型为变体型异常问题 xhwsd@qq.com 2021-6-8
		if (type == Variant.class) {
			// 向下转型，得强转
			return (Variant) object;
		}

		return ObjectVariant.getObjectVariant(object);
	}

	/*
	 * 将函数参数转换为适当的Java方法参数。
	 */
	private Object[] convertArguments(Class<?>[] argTypes, Variant[] args) {
		int len = argTypes.length;
		Object[] objArgs = new Object[len];
		for (int i = 0; i < len; i++) {
			objArgs[i] = convertVariant(argTypes[i], args[i]);
		}
		return objArgs;
	}

	@Override
	public Variant function(String name, Variant[] args) {
		Class<?> type = value.getClass();

		// 变体调用没有静态参数类型列表，因此按Simple的重载规则使用方法名和参数个数查找。
		// 同名、同参数个数的方法在编译期已被禁止，这里最多只能命中一个方法。
		for (Method method : type.getMethods()) {
			if (method.getName().equals(name)) {
				Class<?>[] argTypes = method.getParameterTypes();
				if (argTypes.length == args.length) {
					// 转换实际参数
					Object[] convertedArgs = convertArguments(argTypes, args);

					// 调用方法
					Object result = null;
					try {
						result = method.invoke(value, convertedArgs);
					} catch (IllegalAccessException iae) {
						// 不可能发生 - 所有Simple方法都是公开（public）的
					} catch (InvocationTargetException ite) {
						throw new 未知标识符错误();
					}

					// 将结果转换为变体
					return convertObject(method.getReturnType(), result);
				}
			}
		}

		// 未找到匹配方法
		throw new 未知标识符错误();
	}

	@Override
	public Variant dataMember(String name) {
		Class<?> type = value.getClass();
		try {
			Field field = type.getField(name);
			return convertObject(field.getType(), field.get(value));
		} catch (NoSuchFieldException nsfe) {
			// 因此，不存在该名称的数据成员，但仍可能存在属性获取器
			return function(name, new Variant[0]);
		} catch (IllegalAccessException iae) {
			// 不能发生 - 所有Simple的字段都是公开的
		}
		// 不应该到这里。
		return null;
	}

	@Override
	public void dataMember(String name, Variant variant) {
		Class<?> type = value.getClass();
		try {
			Field field = type.getField(name);
			field.set(value, convertVariant(field.getType(), variant));
		} catch (NoSuchFieldException nsfe) {
			// 因此，不存在该名称的数据成员，但仍可能存在属性设置器
			function(name, new Variant[] { variant });
		} catch (IllegalAccessException iae) {
			// 不能发生 - 所有Simple的字段都是公开的
		}
	}
}

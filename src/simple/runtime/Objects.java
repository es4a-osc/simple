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

package simple.runtime;

import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.variants.IntegerVariant;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用于处理与Simple对象相关内容的辅助类（如属性初始化）。
 *
 * @author Herbert Czymontek
 */
public class Objects {

	// 关联对象 {@link simple.compiler.symbols.ObjectSymbol#generate(Compiler) generate}

	private Objects() {
	}

	private static class PropertyDescriptor {
		private final PropertyInitializer pi;
		private final Method m;
		private final String initializer;

		PropertyDescriptor(PropertyInitializer pi, Method m, String initializer) {
			this.pi = pi;
			this.m = m;
			this.initializer = initializer;
		}

		void runInitializer(Object object) {
			pi.run(object, m, initializer);
		}
	}

	private static abstract class PropertyInitializer {
		void run(Object object, Method m, String value) {
			try {
				initializer(object, m, value);
			} catch (IllegalArgumentException e) {
				reportInitializePropertiesError(m, e);
			} catch (IllegalAccessException e) {
				// 不应该发生
			} catch (InvocationTargetException e) {
				reportInitializePropertiesError(m, e);
			}
		}

		abstract void initializer(Object object, Method m, String value)
				throws IllegalArgumentException, IllegalAccessException, InvocationTargetException;

		private static void reportInitializePropertiesError(Method m, Exception e) {
			日志输出.输出错误(日志输出.MODULE_NAME_RTL, "运行时异常设置属性默认值：" +
					m.getName());
			e.printStackTrace();
		}
	}

	// 属性初始值设定项映射
	private final static Map<String, PropertyInitializer> PROPERTY_INITIALIZERS =
			new HashMap<String, PropertyInitializer>();
	static {
		PROPERTY_INITIALIZERS.put(SimpleProperty.PROPERTY_TYPE_ASSET, new PropertyInitializer() {
			@Override
			void initializer(Object object, Method m, String value)
					throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
				m.invoke(object, unquote(value));
			}
		});

		PROPERTY_INITIALIZERS.put(SimpleProperty.PROPERTY_TYPE_BOOLEAN, new PropertyInitializer() {
			@Override
			void initializer(Object object, Method m, String value)
					throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
				m.invoke(object, value.equals("True"));
			}
		});

		PROPERTY_INITIALIZERS.put(SimpleProperty.PROPERTY_TYPE_COLOR, new PropertyInitializer() {
			@Override
			void initializer(Object object, Method m, String value)
					throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
				m.invoke(object, Integer.valueOf(value));
			}
		});

		PROPERTY_INITIALIZERS.put(SimpleProperty.PROPERTY_TYPE_DOUBLE, new PropertyInitializer() {
			@Override
			void initializer(Object object, Method m, String value)
					throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
				m.invoke(object, Double.valueOf(value));
			}
		});

		PROPERTY_INITIALIZERS.put(SimpleProperty.PROPERTY_TYPE_HORIZONTAL_ALIGNMENT,
				new PropertyInitializer() {
			@Override
			void initializer(Object object, Method m, String value)
					throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
				m.invoke(object, Integer.valueOf(value));
			}
		});

		PROPERTY_INITIALIZERS.put(SimpleProperty.PROPERTY_TYPE_INTEGER, new PropertyInitializer() {
			@Override
			void initializer(Object object, Method m, String value)
					throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
				m.invoke(object, Integer.valueOf(value));
			}
		});

		PROPERTY_INITIALIZERS.put(SimpleProperty.PROPERTY_TYPE_LAYOUT, new PropertyInitializer() {
			@Override
			void initializer(Object object, Method m, String value)
					throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
				m.invoke(object, IntegerVariant.getIntegerVariant(Integer.valueOf(value)));
			}
		});

		PROPERTY_INITIALIZERS.put(SimpleProperty.PROPERTY_TYPE_LONG, new PropertyInitializer() {
			@Override
			void initializer(Object object, Method m, String value)
					throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
				m.invoke(object, Long.valueOf(value));
			}
		});

		PROPERTY_INITIALIZERS.put(SimpleProperty.PROPERTY_TYPE_SINGLE, new PropertyInitializer() {
			@Override
			void initializer(Object object, Method m, String value)
					throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
				m.invoke(object, Float.valueOf(value));
			}
		});

		PROPERTY_INITIALIZERS.put(SimpleProperty.PROPERTY_TYPE_STRING, new PropertyInitializer() {
			@Override
			void initializer(Object object, Method m, String value)
					throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
				m.invoke(object, unquote(value));
			}
		});

		PROPERTY_INITIALIZERS.put(SimpleProperty.PROPERTY_TYPE_TEXT, new PropertyInitializer() {
			@Override
			void initializer(Object object, Method m, String value)
					throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
				m.invoke(object, value);
			}
		});

		PROPERTY_INITIALIZERS.put(SimpleProperty.PROPERTY_TYPE_TEXTJUSTIFICATION,
				new PropertyInitializer() {
			@Override
			void initializer(Object object, Method m, String value)
					throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
				m.invoke(object, Integer.valueOf(value));
			}
		});

		PROPERTY_INITIALIZERS.put(SimpleProperty.PROPERTY_TYPE_TYPEFACE, new PropertyInitializer() {
			@Override
			void initializer(Object object, Method m, String value)
					throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
				m.invoke(object, Integer.valueOf(value));
			}
		});

		PROPERTY_INITIALIZERS.put(SimpleProperty.PROPERTY_TYPE_VERTICAL_ALIGNMENT,
				new PropertyInitializer() {
			@Override
			void initializer(Object object, Method m, String value)
					throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
				m.invoke(object, Integer.valueOf(value));
			}
		});
	}

	// 将组件映射到其属性初始值设定项
	private static final Map<Class<?>, List<PropertyDescriptor>> COMPONENT_MAP =
			new HashMap<Class<?>, List<PropertyDescriptor>>();

	/**
	 * 使用{@code @SimpleProperty}声明定义的默认值初始化属性。
	 *
	 * <p>在组件的构造函数完成后，将自动调用此方法。永远不应该明确调用它！
	 *
	 * @param object  必须初始化其属性的Simple对象实例
	 */
	public static void initializeProperties(Object object) {
		Class<?> cls = object.getClass();
		List<PropertyDescriptor> pdl = COMPONENT_MAP.get(cls);
		if (pdl == null) {
			pdl = new ArrayList<PropertyDescriptor>();
			initializeProperties(object, cls, pdl);
			COMPONENT_MAP.put(cls, pdl);
		} else {
			for (PropertyDescriptor pd : pdl) {
				pd.runInitializer(object);
			}
		}
	}

	/*
	 * 第一次属性初始值设定项将收集所有属性初始值设定项，
	 * 以便在后续运行中不需要对组件进行内省。
	 */
	private static void initializeProperties(Object object, Class<?> cls,
			List<PropertyDescriptor> pdl) {
		Class<?> scls = cls.getSuperclass();
		if (scls != null && scls != Object.class) {
			initializeProperties(object, scls, pdl);
		}

		for (Class<?> iface : cls.getInterfaces()) {
			initializeProperties(object, iface, pdl);
		}

		SimpleObject c = cls.getAnnotation(SimpleObject.class);
		if (c != null) {
			for (Method m : cls.getDeclaredMethods()) {
				SimpleProperty p = m.getAnnotation(SimpleProperty.class);
				if (p != null) {
					String initializer = p.initializer();
					if (initializer.length() != 0) {
						if (!m.getReturnType().equals(Void.TYPE)) {
							日志输出.输出警告(日志输出.MODULE_NAME_RTL, "默认初始化属性获取方法：" +
									cls.getName() + '.' + m.getName());
						} else {
							PropertyInitializer pi = PROPERTY_INITIALIZERS.get(p.type());
							if (pi == null) {
								// 忽略未知的属性类型
								日志输出.输出警告(日志输出.MODULE_NAME_RTL, "未知的属性类型:" + p.type());
							} else {
								PropertyDescriptor pd = new PropertyDescriptor(pi, m, initializer);
								pd.runInitializer(object);
								pdl.add(pd);
							}
						}
					}
				}
			}
		}
	}

	private static String unquote(String s) {
		// 假设在给定字符串的开头和结尾处引用引号
		return s.substring(1, s.length() - 1);
	}
}

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

package simple.compiler;

import simple.compiler.expressions.ConstantBooleanExpression;
import simple.compiler.expressions.ConstantExpression;
import simple.compiler.expressions.ConstantNumberExpression;
import simple.compiler.expressions.ConstantStringExpression;
import simple.compiler.scanner.Scanner;
import simple.compiler.scopes.GlobalScope;
import simple.compiler.scopes.ObjectScope;
import simple.compiler.symbols.AliasSymbol;
import simple.compiler.symbols.ConstantDataMemberSymbol;
import simple.compiler.symbols.DataMemberSymbol;
import simple.compiler.symbols.EventSymbol;
import simple.compiler.symbols.FunctionSymbol;
import simple.compiler.symbols.InstanceDataMemberSymbol;
import simple.compiler.symbols.InstanceFunctionSymbol;
import simple.compiler.symbols.ObjectDataMemberSymbol;
import simple.compiler.symbols.ObjectFunctionSymbol;
import simple.compiler.symbols.ObjectSymbol;
import simple.compiler.symbols.PropertySymbol;
import simple.compiler.symbols.Symbol;
import simple.compiler.types.ArrayType;
import simple.compiler.types.BooleanType;
import simple.compiler.types.ByteType;
import simple.compiler.types.DateType;
import simple.compiler.types.DoubleType;
import simple.compiler.types.IntegerType;
import simple.compiler.types.LongType;
import simple.compiler.types.ObjectType;
import simple.compiler.types.ShortType;
import simple.compiler.types.SingleType;
import simple.compiler.types.StringType;
import simple.compiler.types.Type;
import simple.compiler.types.VariantType;
import simple.util.Preconditions;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 此类加载并分析Simple运行库，从而为编译过程创建必要的符号。
 *
 * <p>Simple类文件包含注释，这些注释标记了Simple编译器可见的信息。
 * 由Simple编译器生成的类文件将自动添加此信息。
 * 从Java源代码（例如Simple运行库）生成的类文件需要手动添加注释。
 * 否则Simple编译器将不会从类文件中拾取任何符号。
 *
 * @author Herbert Czymontek
 */
public final class RuntimeLoader {

	/*
	 * 用于加载Simple运行库类的特殊类加载器。
	 * 需要将新类路径注入正在运行的编译器。
	 */
	private static class RuntimeClassLoader extends URLClassLoader {

		public RuntimeClassLoader(URL[] urls, ClassLoader parent) {
			super(urls, parent);
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			try {
				return findClass(name);
			} catch (Exception e) {
				return super.loadClass(name);
			}
		}

		public void addURL(URL url) {
			super.addURL(url);
		}
	}

	// 日志记录支持
	private static final Logger LOG = Logger.getLogger(RuntimeLoader.class.getName());

	// 类文件扩展名
	private static final String CLASSFILE_EXTENSION = ".class";

	// Simple注释所在的包
	public static final String ANNOTATION_PACKAGE = Compiler.RUNTIME_ROOT_PACKAGE + ".annotations";
	public static final String ANNOTATION_INTERNAL = ANNOTATION_PACKAGE.replace('.', '/');

	// 参考参数运行类名称
	private static final String REFERENCE_PARAMETER_PACKAGE =
			Compiler.RUNTIME_ROOT_PACKAGE + ".parameters";

	private static final String BOOLEAN_REFERENCE_PARAMETER_NAME =
			REFERENCE_PARAMETER_PACKAGE + ".BooleanReferenceParameter";
	private static final String BYTE_REFERENCE_PARAMETER_NAME =
			REFERENCE_PARAMETER_PACKAGE + ".ByteReferenceParameter";
	private static final String DATE_REFERENCE_PARAMETER_NAME =
			REFERENCE_PARAMETER_PACKAGE + ".DateReferenceParameter";
	private static final String DOUBLE_REFERENCE_PARAMETER_NAME =
			REFERENCE_PARAMETER_PACKAGE + ".DoubleReferenceParameter";
	private static final String INTEGER_REFERENCE_PARAMETER_NAME =
			REFERENCE_PARAMETER_PACKAGE + ".IntegerReferenceParameter";
	private static final String LONG_REFERENCE_PARAMETER_NAME =
			REFERENCE_PARAMETER_PACKAGE + ".LongReferenceParameter";
	private static final String OBJECT_REFERENCE_PARAMETER_NAME =
			REFERENCE_PARAMETER_PACKAGE + ".ObjectReferenceParameter";
	private static final String SHORT_REFERENCE_PARAMETER_NAME =
			REFERENCE_PARAMETER_PACKAGE + ".ShortReferenceParameter";
	private static final String SINGLE_REFERENCE_PARAMETER_NAME =
			REFERENCE_PARAMETER_PACKAGE + ".SingleReferenceParameter";
	private static final String STRING_REFERENCE_PARAMETER_NAME =
			REFERENCE_PARAMETER_PACKAGE + ".StringReferenceParameter";
	private static final String VARIANT_REFERENCE_PARAMETER_NAME =
			REFERENCE_PARAMETER_PACKAGE + ".VariantReferenceParameter";

	// 用于加载Simple运行库类的类加载器
	private RuntimeClassLoader runtimeClassLoader;

	// Simple运行库文件或测试使用的运行库类目录
	private final File runtimeRoot;

	// 可选的扩展库根目录
	private final File librariesRoot;

	// 已发现且包含classes.jar的扩展库目录，按目录名稳定排序
	private final List<File> extensionLibraryDirectories;

	// 类名到来源文件的映射，用于按项目实际引用收集扩展库
	private final Map<String, File> classOriginFiles;

	// 扩展库classes.jar到扩展库目录的映射
	private final Map<File, File> originLibraryDirectories;

	// Simple编译器实例
	private final Compiler compiler;

	// Simple的类文件注释
	private Class<? extends Annotation> simpleComponent;
	private Class<? extends Annotation> simpleObject;
	private Class<? extends Annotation> simpleDataElement;
	private Class<? extends Annotation> simpleEvent;
	private Class<? extends Annotation> simpleFunction;
	private Class<? extends Annotation> simpleProperty;
	private Class<? extends Annotation> androidUsesPermission;
	private Class<? extends Annotation> androidManifestNodes;

	private Class<?> simpleReferenceParameter;

	// 用于将组件映射到其实现者
	private List<Class<?>> classList;
	private Map<Class<?>, ObjectSymbol> classMap;
	private Map<ObjectSymbol, String> componentMap;

	/*
	 * 加载注解类；加载Simple类文件注释。
	 */
	@SuppressWarnings("unchecked")
	private Class<? extends Annotation> loadAnnotationClass(String name)
			throws ClassNotFoundException {
		return (Class<? extends Annotation>) Class.forName(ANNOTATION_PACKAGE + '.' + name, false,
				runtimeClassLoader);
	}

	/**
	 * 获取此类加载器的 UsesPermission 属性
	 *
	 * @return  来自运行库的类 {@code UsesPermissions}
	 */
	public Class<? extends Annotation> getAndroidUsesPermission() {
		return androidUsesPermission;
	}

	/**
	 * 获取此类加载器加载的清单节点注解类型。
	 *
	 * @return 来自运行库的{@code ManifestNodes}类
	 */
	public Class<? extends Annotation> getAndroidManifestNodes() {
		return androidManifestNodes;
	}

	/**
	 * 创建一个新的Simple运行时加载程序。
	 *
	 * @param compiler  Simple编译器实例
	 * @param platformRuntime  平台运行库的路径
	 * @param simpleRuntime  Simple运行库的路径
	 * @param librariesHome  可选的扩展库根目录；为{@code null}或空字符串时不加载扩展库
	 *
	 * <p>注意：之所以需要平台运行库（Android或其他），
	 * 是因为Simple运行库依赖于该平台，因此从其运行库加载Simple类时，
	 * 如果没有该库则会导致ClassNotFoundExceptions。
	 */
	public RuntimeLoader(Compiler compiler, String platformRuntime, String simpleRuntime,
			String librariesHome) {
		this.compiler = compiler;
		runtimeRoot = new File(simpleRuntime).getAbsoluteFile();
		librariesRoot = librariesHome == null || librariesHome.trim().isEmpty()
				? null : new File(librariesHome).getAbsoluteFile();
		extensionLibraryDirectories = new ArrayList<File>();
		classOriginFiles = new HashMap<String, File>();
		originLibraryDirectories = new HashMap<File, File>();

		// 实例化Simple运行时库的类文件加载器
		try {
			runtimeClassLoader = new RuntimeClassLoader(new URL[] {
					new File(simpleRuntime).toURI().toURL(), new File(platformRuntime).toURI().toURL() },
					getClass().getClassLoader());
			loadExtensionLibraryURLs();
		} catch (MalformedURLException mue) {
			compiler.error(Scanner.NO_POSITION, Error.errReadError,
					librariesRoot == null ? simpleRuntime : librariesRoot.toString());
		}

		// 组件哈希表
		componentMap = new HashMap<ObjectSymbol, String>();

		try {
			// 加载Simple类文件注释
			simpleComponent = loadAnnotationClass("SimpleComponent");
			simpleObject = loadAnnotationClass("SimpleObject");
			simpleDataElement = loadAnnotationClass("SimpleDataElement");
			simpleEvent = loadAnnotationClass("SimpleEvent");
			simpleFunction = loadAnnotationClass("SimpleFunction");
			simpleProperty = loadAnnotationClass("SimpleProperty");
			androidUsesPermission = loadAnnotationClass("UsesPermissions");
			androidManifestNodes = loadAnnotationClass("ManifestNodes");

			// 加载Simple参考参数超类
			simpleReferenceParameter = Class.forName(Compiler.RUNTIME_ROOT_PACKAGE +
					".parameters.ReferenceParameter", false, runtimeClassLoader);

		} catch (ClassNotFoundException e) {
			LOG.log(Level.SEVERE, "Runtime library load failure", e);
			compiler.error(Scanner.NO_POSITION, Error.errReadError, simpleRuntime);
		}
	}

	/**
	 * 返回指定Simple类的来源文件。
	 *
	 * @param internalName 使用斜杠分隔的类内部名称
	 * @return 运行库目录、运行库JAR或扩展库classes.jar；未找到时返回{@code null}
	 */
	public File getClassOriginFile(String internalName) {
		return classOriginFiles.get(internalName.replace('/', '.'));
	}

	/**
	 * 返回指定Simple类所属的扩展库目录。
	 *
	 * @param internalName 使用斜杠分隔的类内部名称
	 * @return 扩展库目录；核心运行库类或未知类返回{@code null}
	 */
	public File getExtensionLibraryDirectory(String internalName) {
		File originFile = getClassOriginFile(internalName);
		return originFile == null ? null : originLibraryDirectories.get(originFile);
	}

	/*
	 * 发现固定结构的扩展库，并把全部类路径先加入类加载器。
	 * 先完成类路径装配、后分析classes.jar，可保证类库之间的加载顺序不影响依赖解析。
	 */
	private void loadExtensionLibraryURLs() throws MalformedURLException {
		if (librariesRoot == null) {
			return;
		}
		// 扩展库是可选能力；调用者即使传入尚未创建的约定目录，也按未安装类库处理。
		if (!librariesRoot.exists()) {
			return;
		}
		if (!librariesRoot.isDirectory()) {
			compiler.error(Scanner.NO_POSITION, "扩展库目录无效：%1", librariesRoot.toString());
			return;
		}

		File[] directories = librariesRoot.listFiles();
		if (directories == null) {
			compiler.error(Scanner.NO_POSITION, Error.errReadError, librariesRoot.toString());
			return;
		}
		Arrays.sort(directories, new Comparator<File>() {
			@Override
			public int compare(File left, File right) {
				int result = left.getName().compareToIgnoreCase(right.getName());
				return result != 0 ? result : left.getName().compareTo(right.getName());
			}
		});

		for (File directory : directories) {
			if (!directory.isDirectory()) {
				continue;
			}
			File classesFile = new File(directory, "classes.jar").getAbsoluteFile();
			if (!classesFile.isFile()) {
				// 未构建的源码目录不属于已安装类库，不能阻断其它项目编译。
				continue;
			}
			extensionLibraryDirectories.add(directory.getAbsoluteFile());
			originLibraryDirectories.put(classesFile, directory.getAbsoluteFile());
		}

		// 扩展库自己的依赖必须先于classes.jar分析进入类加载器。
		for (File directory : extensionLibraryDirectories) {
			for (File dependency : listJarFiles(new File(directory, "libs"))) {
				runtimeClassLoader.addURL(dependency.toURI().toURL());
			}
		}
		for (File directory : extensionLibraryDirectories) {
			runtimeClassLoader.addURL(new File(directory, "classes.jar").toURI().toURL());
		}
	}

	private static List<File> listJarFiles(File directory) {
		if (!directory.isDirectory()) {
			return Collections.emptyList();
		}
		File[] files = directory.listFiles();
		if (files == null) {
			return Collections.emptyList();
		}
		Arrays.sort(files, new Comparator<File>() {
			@Override
			public int compare(File left, File right) {
				int result = left.getName().compareToIgnoreCase(right.getName());
				return result != 0 ? result : left.getName().compareTo(right.getName());
			}
		});
		List<File> jars = new ArrayList<File>();
		for (File file : files) {
			if (file.isFile() && file.getName().toLowerCase().endsWith(".jar")) {
				jars.add(file.getAbsoluteFile());
			}
		}
		return jars;
	}

	/*
	 * 将一个类转换为其对应的Simple类型。
	 */
	private Type convertToSimpleType(Class<?> cls) {
		if (cls.equals(Boolean.TYPE)) {
			return BooleanType.booleanType;
		} else if (cls.equals(Byte.TYPE)) {
			return ByteType.byteType;
		} else if (cls.equals(Short.TYPE)) {
			return ShortType.shortType;
		} else if (cls.equals(Integer.TYPE)) {
			return IntegerType.integerType;
		} else if (cls.equals(Long.TYPE)) {
			return LongType.longType;
		} else if (cls.equals(Float.TYPE)) {
			return SingleType.singleType;
		} else if (cls.equals(Double.TYPE)) {
			return DoubleType.doubleType;
		} else if (cls.equals(Void.TYPE)) {
			return null;
		} else if (cls.equals(String.class)) {
			return StringType.stringType;
		} else if (cls.equals(Calendar.class)) { // xhwsd@qq.com 2021-5-27 修复编译器无法正确的将`java.util.Calendar`转为`日期时间型`
			return DateType.dateType;
		} else if (cls.getName().equals(VariantType.VARIANT_NAME)) {
			return VariantType.variantType;
		} else if (cls.isArray()) {
			int dimensions = 1;
			cls = cls.getComponentType();
			while (cls.isArray()) {
				dimensions++;
				cls = cls.getComponentType();
			}
			return new ArrayType(convertToSimpleType(cls), dimensions);
		} else if (isSimpleReferenceParameter(cls)) {
			String className = cls.getName();
			if (className.equals(BOOLEAN_REFERENCE_PARAMETER_NAME)) {
				return BooleanType.booleanType;
			} else if (className.equals(BYTE_REFERENCE_PARAMETER_NAME)) {
				return ByteType.byteType;
			} else if (className.equals(DATE_REFERENCE_PARAMETER_NAME)) {
				return DateType.dateType;
			} else if (className.equals(DOUBLE_REFERENCE_PARAMETER_NAME)) {
				return DoubleType.doubleType;
			} else if (className.equals(INTEGER_REFERENCE_PARAMETER_NAME)) {
				return IntegerType.integerType;
			} else if (className.equals(LONG_REFERENCE_PARAMETER_NAME)) {
				return LongType.longType;
			} else if (className.equals(SINGLE_REFERENCE_PARAMETER_NAME)) {
				return SingleType.singleType;
			} else if (className.equals(SHORT_REFERENCE_PARAMETER_NAME)) {
				return ShortType.shortType;
			} else if (className.equals(STRING_REFERENCE_PARAMETER_NAME)) {
				return StringType.stringType;
			} else if (className.equals(VARIANT_REFERENCE_PARAMETER_NAME)) {
				return VariantType.variantType;
			} else {
				Preconditions.checkState(className.equals(OBJECT_REFERENCE_PARAMETER_NAME));
				return ObjectType.objectType;
			}
		} else {
			ObjectSymbol objectSymbol =
					ObjectSymbol.getObjectSymbol(compiler, cls.getName().replace('.', '/'));
			return objectSymbol.getType();
		}
	}

	/*
	 * 获取常量值。
	 */
	private ConstantExpression getConstantExpression(Field f) {
		Class<?> cls = f.getType();
		try {
			if (cls.equals(Boolean.TYPE)) {
				return new ConstantBooleanExpression(Scanner.NO_POSITION, f.getBoolean(null));
			} else if (cls.equals(Byte.TYPE)) {
				return new ConstantNumberExpression(Scanner.NO_POSITION, new BigDecimal(f.getByte(null)));
			} else if (cls.equals(Short.TYPE)) {
				return new ConstantNumberExpression(Scanner.NO_POSITION, new BigDecimal(f.getShort(null)));
			} else if (cls.equals(Integer.TYPE)) {
				return new ConstantNumberExpression(Scanner.NO_POSITION, new BigDecimal(f.getInt(null)));
			} else if (cls.equals(Long.TYPE)) {
				return new ConstantNumberExpression(Scanner.NO_POSITION, new BigDecimal(f.getLong(null)));
			} else if (cls.equals(Float.TYPE)) {
				return new ConstantNumberExpression(Scanner.NO_POSITION, new BigDecimal(f.getFloat(null)));
			} else if (cls.equals(Double.TYPE)) {
				return new ConstantNumberExpression(Scanner.NO_POSITION, new BigDecimal(f.getDouble(null)));
			} else if (cls.equals(String.class)) {
				return new ConstantStringExpression(Scanner.NO_POSITION, (String) f.get(null));
			} else {
				return null;
			}
		} catch (IllegalAccessException ignored) {
			// 这不应该发生
			return null;
		}
	}

	/*
	 * 检查类是否表示引用参数。
	 */
	private boolean isSimpleReferenceParameter(Class<?> cls) {
		Class<?> supercls = cls.getSuperclass();
		return supercls != null && supercls.equals(simpleReferenceParameter);
	}

	/*
	 * 分析Simple类文件的信息。
	 */
	private void analyzeClassFile(String className) {
		try {
			// 从注释包中跳过类文件 (它们不包含任何运行时功能，仅用于标记)
			if (!className.startsWith(ANNOTATION_PACKAGE)) {
				// 类名取类
				Class<?> cls = Class.forName(className, false, runtimeClassLoader);
				// 将类添加到类列表
				classList.add(cls);

				// 只对标记为 SimpleObjects 的类感兴趣
				if (cls.isAnnotationPresent(simpleObject)) {
					// 将类名（. 分割的）转为内部类名（/ 分割的）
					String internalClassName = cls.getName().replace('.', '/'); // 取类名

					// 将对象的符号添加到编译器符号表
					ObjectSymbol objectSymbol = ObjectSymbol.getObjectSymbol(compiler, internalClassName);

					// Simple对象的基础对象必须是其他 Simple Objects 或 java.lang.Object
					Class<?> superclass = cls.getSuperclass();
					if (superclass != null && !superclass.equals(Object.class)) {
						objectSymbol.setBaseObject((ObjectType) ObjectSymbol.getObjectSymbol(compiler,
								superclass.getCanonicalName().replace('.', '/')).getType());
					}

					// 来自包 'simple.runtime' 及其子包的对象，将自动接收全局命名空间中的别名条目。
					GlobalScope globalScope = (GlobalScope) compiler.getGlobalNamespaceSymbol().getScope();
					String packageName = cls.getPackage().getName();
					if (packageName.startsWith(Compiler.RUNTIME_ROOT_PACKAGE)) {
						globalScope.enterSymbol(new AliasSymbol(Scanner.NO_POSITION, objectSymbol.getName(),
								objectSymbol));
						LOG.log(Level.FINEST, "加载运行库: 对象的全局别名 " +
								objectSymbol.getName());
					}

					// 检查接口对象
					if (cls.isInterface()) {
						objectSymbol.markAsInterface();
					}

					// 这是一个组件吗？记住它 - 在第二次遍历时，我们将尝试找到它的实现者。
					if (cls.isAnnotationPresent(simpleComponent)) {
						classMap.put(cls, objectSymbol);
					}

					// 接下来检查类文件中定义的所有方法
					for (Method m : cls.getMethods()) {
						if (m.isAnnotationPresent(simpleProperty)) {
							// 此方法是Simple的属性获取器或属性设置器定义
							String propertyName = m.getName();

							// 找出属性类型
							Type type;
							Class<?> returnType = m.getReturnType();
							if (returnType.equals(Void.TYPE)) {
								// 这是一个属性设置器，最多可以有一个参数（这是新的属性值）。
								Class<?> params[] = m.getParameterTypes();
								if (params.length != 1) {
									Compiler.internalError();  // COV_NF_LINE
								}
								type = convertToSimpleType(params[0]);
							} else {
								type = convertToSimpleType(returnType);
							}

							// Add a new symbol for the property to the object's symbol tables. Since the
							// property is split up into getter and setter and we can't track which one we
							// have already seen, we need to check whether there is already a property symbol
							// defined.
							ObjectScope classScope = objectSymbol.getScope();
							Symbol symbol = classScope.lookupInObject(propertyName);
							if (symbol == null) {
								objectSymbol.addProperty(new PropertySymbol(Scanner.NO_POSITION, objectSymbol,
										propertyName, type), null, null);
							}
						} else if (m.isAnnotationPresent(simpleFunction)) {
							// 此方法是Simple函数或过程定义
							FunctionSymbol function = ((m.getModifiers() & Modifier.STATIC) != 0) ?
									new ObjectFunctionSymbol(Scanner.NO_POSITION, objectSymbol, m.getName()) :
									new InstanceFunctionSymbol(Scanner.NO_POSITION, objectSymbol, m.getName());
							function.setResultType(convertToSimpleType(m.getReturnType()));

							// 分析其结果和参数类型
							for (Class<?> argClass : m.getParameterTypes()) {
								function.addParameter(Scanner.NO_POSITION, null, convertToSimpleType(argClass),
										isSimpleReferenceParameter(argClass));
							}

							// 将同名、不同参数个数的方法保存在同一个重载组中。
							boolean functionAdded = objectSymbol.addFunction(function);
							if (!functionAdded) {
								compiler.error(Scanner.NO_POSITION, Error.errSymbolRedefinition,
										function.getName());
							}

							// 来自包 'simple.runtime' 下的静态函数，会自动在全局命名空间中接收别名项。
							if (functionAdded && function instanceof ObjectFunctionSymbol &&
									packageName.equals(Compiler.RUNTIME_ROOT_PACKAGE)) {
								FunctionSymbol firstOverload = (FunctionSymbol)
										objectSymbol.getScope().lookupShallow(function.getName());
								Symbol globalFunction = globalScope.lookupShallow(function.getName());
								if (globalFunction == null) {
									globalScope.enterSymbol(new AliasSymbol(Scanner.NO_POSITION,
											function.getName(), firstOverload));
									LOG.log(Level.FINEST, "加载运行库: 函数的全局别名 " +
											objectSymbol.getName());
								} else if (globalFunction != firstOverload) {
									// 全局名称只能指向一个对象中的重载组，避免把其它对象的方法
									// 混入该对象自己的限定调用。
									compiler.error(Scanner.NO_POSITION, Error.errSymbolRedefinition,
											function.getName());
								}
							}

						} else if (m.isAnnotationPresent(simpleEvent)) {
							// 此方法是Simple事件定义
							EventSymbol event = new EventSymbol(Scanner.NO_POSITION, objectSymbol, m.getName());
							// 分析其结果和参数类型
							for (Class<?> argClass : m.getParameterTypes()) {
								event.addParameter(Scanner.NO_POSITION, null, convertToSimpleType(argClass),
										isSimpleReferenceParameter(argClass));
							}

							// 将函数的符号添加到对象的符号表
							objectSymbol.addEvent(event);
						}
					}

					// 接下来检查类文件中定义的所有字段
					for (Field f : cls.getFields()) {
						if (f.isAnnotationPresent(simpleDataElement)) {
							// 此字段为Simple数据元素
							String name = f.getName();
							Type type = convertToSimpleType(f.getType());

							DataMemberSymbol dataMember;
							// 含静态修饰
							if ((f.getModifiers() & Modifier.STATIC) != 0) {

								ConstantExpression constExpr = null;
								// 含最终修饰
								if ((f.getModifiers() & Modifier.FINAL) != 0) {
									constExpr = getConstantExpression(f);
								}

								// 含 静态 + 禁止 修饰的成员字段
								if (constExpr != null) {
									dataMember = new ConstantDataMemberSymbol(Scanner.NO_POSITION, objectSymbol, name,
											type, constExpr);
								} else {
									// 含 静态 修饰的成员字段
									dataMember = new ObjectDataMemberSymbol(Scanner.NO_POSITION, objectSymbol, name,
											type);
								}
							} else {
								// 成员字段
								dataMember = new InstanceDataMemberSymbol(Scanner.NO_POSITION, objectSymbol, name,
										type);
							}

							// 将数据成员的符号添加到对象的符号表中
							objectSymbol.addDataMember(dataMember);

							// 来自中对象的静态数据成员
							// 来自包 'simple.runtime' 下的静态成员，会自动在全局命名空间中接收别名项。
							if (dataMember instanceof ObjectDataMemberSymbol &&
									packageName.equals(Compiler.RUNTIME_ROOT_PACKAGE)) {
								globalScope.enterSymbol(new AliasSymbol(Scanner.NO_POSITION, dataMember.getName(),
										dataMember));
								LOG.log(Level.FINEST, "加载运行库: 数据成员的全局别名 " +
										objectSymbol.getName());
							}
						}
					}
					// TODO: 这里放错地方了。
					//       我们在这里加载所有的运行时库类，不仅仅是应用程序使用的类。
					//       我们应该只关心我们编译的应用程序使用的类
					if (cls.isAnnotationPresent(androidUsesPermission)) {
						compiler.addToPermissions(cls, objectSymbol);
					}
					if (cls.isAnnotationPresent(androidManifestNodes)) {
						compiler.addToManifestNodes(cls, objectSymbol);
					}
				}
			}
		} catch (ClassNotFoundException e) {
			// 如果无法定位该类
			LOG.log(Level.SEVERE, "运行库加载失败", e);
			compiler.error(Scanner.NO_POSITION, Error.errReadError, className);
		}
	}

	/*
	 * 递归访问运行库目录或JAR，加载其中对Simple可见的类。
	 */
	private void visitClassDirectories(File file, File originRoot) {
		if (file.isDirectory()) {
			String[] children = file.list();
			if (children == null) {
				compiler.error(Scanner.NO_POSITION, Error.errReadError, file.toString());
				return;
			}
			Arrays.sort(children, String.CASE_INSENSITIVE_ORDER);
			for (String child : children) {
				visitClassDirectories(new File(file, child), originRoot);
			}
		} else if (file.getName().endsWith(".jar")) {
			visitClassArchive(file);
		} else {
			String name = file.getName();
			if (name.endsWith(CLASSFILE_EXTENSION)) {
				String path = file.getAbsolutePath();
				String className = path.substring(originRoot.getAbsolutePath().length() + 1,
						path.length() - CLASSFILE_EXTENSION.length()).replace(File.separatorChar, '.');
				rememberClassOrigin(className, originRoot);
				analyzeClassFile(className);
			}
		}
	}

	private void visitClassArchive(File archive) {
		try {
			JarFile jar = new JarFile(archive, false, JarFile.OPEN_READ);
			try {
				Enumeration<JarEntry> jarEntries = jar.entries();
				while (jarEntries.hasMoreElements()) {
					JarEntry entry = jarEntries.nextElement();
					String name = entry.getName();
					if (name.endsWith(CLASSFILE_EXTENSION)) {
						// JAR条目始终使用斜杠作为路径分隔符。
						String className = name.substring(0,
								name.length() - CLASSFILE_EXTENSION.length()).replace('/', '.');
						rememberClassOrigin(className, archive.getAbsoluteFile());
						analyzeClassFile(className);
					}
				}
			} finally {
				jar.close();
			}
		} catch (IOException ioe) {
			LOG.log(Level.SEVERE, "运行库加载失败", ioe);
			compiler.error(Scanner.NO_POSITION, Error.errReadError, archive.toString());
		} catch (SecurityException ignored) {
			throw ignored;
		}
	}

	private void rememberClassOrigin(String className, File originFile) {
		File previous = classOriginFiles.get(className);
		if (previous != null && !previous.equals(originFile)) {
			compiler.error(Scanner.NO_POSITION, "运行库类重复：%1", className);
			return;
		}
		classOriginFiles.put(className, originFile);
	}

	/*
	 * 将组件映射到其实际实现
	 */
	private void findComponentImplementations() {
		for (Class<?> cls : classList) {
			for (Class<?> iface : cls.getInterfaces()) {
				if (checkComponentImplementation(cls, iface)) {
					break;
				}
			}
			checkComponentImplementation(cls, cls.getSuperclass());
		}

		// 如果缺少任何组件实现，将报告错误
		for (ObjectSymbol component : classMap.values()) {
			compiler.error(Scanner.NO_POSITION, Error.errNoComponentImplementation,
					component.getType().toString());
		}
	}

	/*
	 * 检查组件实现
	 */
	private boolean checkComponentImplementation(Class<?> cls, Class<?> implementor) {
		// 从对象标识符哈希表中取指定组件的
		ObjectSymbol component = classMap.get(implementor);
		if (component != null) {
			// 将 组件对象标识符 和 内部类名压入
			componentMap.put(component, cls.getName().replace('.', '/'));
			classMap.remove(implementor);
			return true;
		}

		return false;
	}

	/**
	 * 返回给定组件对象类型的实现的内部名称。
	 *
	 * @param type  组件对象类型
	 * @return  组件对象的内部名称
	 */
	String getComponentImplementationInternalName(ObjectType type) {
		return componentMap.get(type.getObjectSymbol());
	}

	/**
	 * 加载和分析Simple运行时库中的类，
	 * 并将符号信息输入编译器符号表中。
	 */
	public void loadSimpleObjects() {
		// 加载所有Simple对象
		classList = new ArrayList<Class<?>>();
		classMap = new HashMap<Class<?>, ObjectSymbol>();
		visitClassDirectories(runtimeRoot, runtimeRoot);
		for (File directory : extensionLibraryDirectories) {
			visitClassArchive(new File(directory, "classes.jar").getAbsoluteFile());
		}

		// 查找Simple的组件实现
		findComponentImplementations();
		classList = null;
		classMap = null;
	}
}

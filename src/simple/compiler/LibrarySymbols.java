package simple.compiler;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import simple.compiler.expressions.ConstantBooleanExpression;
import simple.compiler.expressions.ConstantExpression;
import simple.compiler.expressions.ConstantNumberExpression;
import simple.compiler.expressions.ConstantStringExpression;
import simple.compiler.scopes.ObjectScope;
import simple.compiler.scopes.GlobalScope;
import simple.compiler.symbols.AliasSymbol;
import simple.compiler.symbols.Symbol;
import simple.compiler.scanner.Scanner;
import simple.compiler.symbols.ConstantDataMemberSymbol;
import simple.compiler.symbols.EventSymbol;
import simple.compiler.symbols.FunctionSymbol;
import simple.compiler.symbols.InstanceDataMemberSymbol;
import simple.compiler.symbols.InstanceFunctionSymbol;
import simple.compiler.symbols.ObjectDataMemberSymbol;
import simple.compiler.symbols.ObjectFunctionSymbol;
import simple.compiler.symbols.ObjectSymbol;
import simple.compiler.symbols.PropertySymbol;
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
import simple.compiler.util.ClassFiles;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 直接读取扩展库的类文件注解和签名，发现符号时不加载第三方实现或执行静态初始化。
 *
 * <p>运行库仍由原加载器处理；扩展库继承的公开成员从类文件收集，实际使用的库确定后，
 * 才准备其运行依赖。符号依据始终是 classes.jar，而非供编辑器显示的 library.json。
 *
 * @author 树先生 xhwsd@qq.com
 */
final class LibrarySymbols {

	private static final String ANNOTATIONS = "Lsimple/runtime/annotations/";
	private final Map<String, Definition> definitions = new LinkedHashMap<String, Definition>();
	private final Compiler compiler;
	private final RuntimeLoader loader;

	/**
	 * 为一次符号发现读取运行库和已安装扩展库，保留实现类头而不解析方法体。
	 *
	 * @param compiler 当前编译器。
	 * @param loader 负责来源登记和组件映射的加载器。
	 * @param runtime 运行库归档。
	 * @param libraries 扩展库目录。
	 * @throws IOException 类文件损坏或归档无法读取。
	 */
	LibrarySymbols(Compiler compiler, RuntimeLoader loader, File runtime, List<File> libraries) throws IOException {
		this.compiler = compiler;
		this.loader = loader;
		read(runtime, false);
		for (File library : libraries) {
			read(new File(library, "classes.jar"), true);
		}
	}

	/**
	 * 建立全部扩展库符号后再查找组件实现，避免归档条目顺序影响映射。
	 */
	void load() {
		for (Definition definition : definitions.values()) {
			if (definition.extension && definition.annotations.containsKey("SimpleObject")) {
				declare(definition);
			}
		}
		for (Definition definition : definitions.values()) {
			if (definition.extension && (definition.access & (Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT)) == 0) {
				findComponents(definition.name, definition.name, new HashSet<String>());
			}
		}
		for (Definition definition : definitions.values()) {
			if (definition.extension && definition.annotations.containsKey("SimpleObject")
					&& definition.annotations.containsKey("SimpleComponent")) {
				ObjectType type = (ObjectType) ObjectSymbol.getObjectSymbol(compiler, definition.name).getType();
				if (loader.getComponentImplementationInternalName(type) == null) {
					compiler.error(Scanner.NO_POSITION, Error.errNoComponentImplementation, type.toString());
				}
			}
		}
	}

	/**
	 * 读取基础普通类，并拒绝跨扩展库的同名类；不解构或改写输入归档。
	 */
	private void read(File archive, final boolean extension) throws IOException {
		if (archive.isDirectory()) {
			File[] children = archive.listFiles();
			if (children == null) throw new IOException("无法读取运行库类目录：" + archive);
			Arrays.sort(children);
			for (File child : children) read(child, extension);
			return;
		}
		if (!archive.isFile()) return;
		if (archive.getName().endsWith(".class")) {
			try (InputStream input = Files.newInputStream(archive.toPath())) {
				register(parse(input, extension), archive);
			}
			return;
		}
		if (!archive.getName().endsWith(".jar")) return;
		try (JarFile jar = new JarFile(archive)) {
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if (ClassFiles.isBaseClass(entry.getName())) {
					try (InputStream input = jar.getInputStream(entry)) {
						register(parse(input, extension), archive);
					}
				}
			}
		}
	}

	/** 登记基础类及扩展库来源，模块描述不参与对象和组件匹配。 */
	private void register(Definition definition, File archive) throws IOException {
		if ((definition.access & Opcodes.ACC_MODULE) != 0) {
			return;
		}
		if (definitions.putIfAbsent(definition.name, definition) != null) {
			throw new IOException("扩展库类重复：" + definition.name + "\n来源：" + archive);
		}
		if (definition.extension) {
			loader.rememberClassOrigin(definition.name.replace('/', '.'), archive.getAbsoluteFile());
		}
	}

	/**
	 * 只读取类头、公开签名和注解；目录与归档形式共用同一规则。
	 *
	 * @param input 当前类文件的输入流。
	 * @param extension 是否属于扩展库。
	 * @return 包含符号签名及原始注解字段的类型定义。
	 * @throws IOException 类文件无法读取。
	 */
	private static Definition parse(InputStream input, final boolean extension) throws IOException {
		final Definition definition = new Definition();
		definition.extension = extension;
		new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
			@Override
			public void visit(int version, int access, String name, String signature,
					String superName, String[] interfaces) {
				definition.name = name;
				definition.parent = superName;
				definition.interfaces = interfaces;
				definition.access = access;
			}

			@Override
			public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				if ((ANNOTATIONS + "UsesPermission;").equals(descriptor)) {
					return permissionAnnotation(definition.permissionAnnotations);
				}
				if ((ANNOTATIONS + "UsesPermission$List;").equals(descriptor)) {
					return permissionContainer(definition.permissionAnnotations);
				}
				return annotation(definition.annotations, descriptor);
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor,
					String signature, String[] exceptions) {
				final Member member = new Member(access, name, descriptor, null);
				definition.methods.add(member);
				return new MethodVisitor(Opcodes.ASM9) {
					@Override
					public AnnotationVisitor visitAnnotation(String type, boolean visible) {
						return annotation(member.annotations, type);
					}
				};
			}

			@Override
			public FieldVisitor visitField(int access, String name, String descriptor,
					String signature, Object value) {
				final Member member = new Member(access, name, descriptor, value);
				definition.fields.add(member);
				return new FieldVisitor(Opcodes.ASM9) {
					@Override
					public AnnotationVisitor visitAnnotation(String type, boolean visible) {
						return annotation(member.annotations, type);
					}
				};
			}
		}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
		return definition;
	}

	/**
	 * 读取单项权限的字符串及整数常量，不加载注解类型或第三方实现。
	 *
	 * @param declarations 所属对象的原始权限字段，验证推迟到实际引用时。
	 * @return 接收一条权限注解的访问器。
	 */
	private static AnnotationVisitor permissionAnnotation(List<Map<String, String>> declarations) {
		final Map<String, String> fields = new HashMap<String, String>();
		declarations.add(fields);
		return new AnnotationVisitor(Opcodes.ASM9) {
			/**
			 * 保存 class 文件显式携带的字段；省略的最高版本由权限模型使用缺省值。
			 *
			 * @param name 注解字段名。
			 * @param value 字段常量。
			 */
			@Override
			public void visit(String name, Object value) {
				if (value instanceof String || value instanceof Integer) {
					fields.put(name, value.toString());
				}
			}
		};
	}

	/**
	 * 展开 Java 为重复权限注解生成的容器，单项与多项使用同一字段读取方式。
	 *
	 * @param declarations 所属对象的原始权限字段。
	 * @return 接收重复注解容器的访问器。
	 */
	private static AnnotationVisitor permissionContainer(final List<Map<String, String>> declarations) {
		return new AnnotationVisitor(Opcodes.ASM9) {
			/**
			 * 只消费容器的 value 数组，不把未知字段当作权限声明。
			 *
			 * @param name 数组字段名。
			 * @return value 数组访问器，其它字段返回 null。
			 */
			@Override
			public AnnotationVisitor visitArray(String name) {
				if (!"value".equals(name)) {
					return null;
				}
				return new AnnotationVisitor(Opcodes.ASM9) {
					/**
					 * 将容器中的权限注解加入所属对象。
					 *
					 * @param name 数组元素名。
					 * @param descriptor 元素注解的 JVM 描述符。
					 * @return 权限字段访问器，非权限元素返回 null。
					 */
					@Override
					public AnnotationVisitor visitAnnotation(String name, String descriptor) {
						return (ANNOTATIONS + "UsesPermission;").equals(descriptor)
								? permissionAnnotation(declarations) : null;
					}
				};
			}
		};
	}

	/** 保存 Simple 注解的字符串字段；不解释第三方注解或执行注解类。 */
	private static AnnotationVisitor annotation(Map<String, Map<String, String>> annotations, String descriptor) {
		if (!descriptor.startsWith(ANNOTATIONS)) {
			return null;
		}
		final Map<String, String> values = new HashMap<String, String>();
		annotations.put(descriptor.substring(ANNOTATIONS.length(), descriptor.length() - 1), values);
		return new AnnotationVisitor(Opcodes.ASM9) {
			@Override
			public void visit(String name, Object value) {
				if (value instanceof String) {
					values.put(name, (String) value);
				}
			}
		};
	}

	/**
	 * 建立对象、继承、成员及清单声明，保持符号所属对象与真实字节码名称一致。
	 *
	 * @param definition 从扩展库类文件读取的类型定义。
	 */
	private void declare(Definition definition) {
		ObjectSymbol object = ObjectSymbol.getObjectSymbol(compiler, definition.name);
		if (definition.name.replace('/', '.').startsWith(Compiler.RUNTIME_ROOT_PACKAGE)) {
			globalAlias(object.getName(), object);
		}
		if (definition.parent != null && !"java/lang/Object".equals(definition.parent)) {
			object.setBaseObject((ObjectType) ObjectSymbol.getObjectSymbol(compiler, definition.parent).getType());
		}
		if ((definition.access & Opcodes.ACC_INTERFACE) != 0) {
			object.markAsInterface();
		}
		Map<String, Member> methods = new LinkedHashMap<String, Member>();
		Map<String, Member> fields = new LinkedHashMap<String, Member>();
		members(definition, methods, fields, new HashSet<String>());
		for (Member member : methods.values()) {
			method(object, member);
		}
		for (Member member : fields.values()) {
			if (member.annotations.containsKey("SimpleDataElement")) {
				Type type = type(org.objectweb.asm.Type.getType(member.descriptor));
				if ((member.access & Opcodes.ACC_STATIC) == 0) {
					object.addDataMember(new InstanceDataMemberSymbol(Scanner.NO_POSITION, object, member.name, type));
				} else {
					ConstantExpression constant = (member.access & Opcodes.ACC_FINAL) == 0 ? null : constant(member);
					object.addDataMember(constant == null
							? new ObjectDataMemberSymbol(Scanner.NO_POSITION, object, member.name, type)
							: new ConstantDataMemberSymbol(Scanner.NO_POSITION, object, member.name, type, constant));
				}
				if ((member.access & Opcodes.ACC_STATIC) != 0 && rootObject(object)) {
					globalAlias(member.name, object.getScope().lookupShallow(member.name));
				}
			}
		}
		compiler.addLibraryAnnotations(object, definition.annotations.get("UsesPermissions"),
				definition.permissionAnnotations, definition.annotations.get("ManifestNodes"));
	}

	/** 子类声明先登记，覆盖父类同签名成员；访问祖先时显式防止损坏继承图形成环。 */
	private void members(Definition definition, Map<String, Member> methods, Map<String, Member> fields,
			Set<String> visited) {
		if (definition == null || !visited.add(definition.name)) {
			return;
		}
		for (Member member : definition.methods) {
			if ((member.access & Opcodes.ACC_PUBLIC) != 0 && !member.name.startsWith("<")) {
				methods.putIfAbsent(member.name + member.descriptor.substring(0, member.descriptor.indexOf(')') + 1), member);
			}
		}
		for (Member member : definition.fields) {
			if ((member.access & Opcodes.ACC_PUBLIC) != 0) {
				fields.putIfAbsent(member.name, member);
			}
		}
		members(definitions.get(definition.parent), methods, fields, visited);
		for (String name : definition.interfaces) {
			members(definitions.get(name), methods, fields, visited);
		}
	}

	/** 按注解建立属性、函数或事件，参数描述直接来自 JVM 方法签名。 */
	private void method(ObjectSymbol object, Member member) {
		org.objectweb.asm.Type result = org.objectweb.asm.Type.getReturnType(member.descriptor);
		org.objectweb.asm.Type[] arguments = org.objectweb.asm.Type.getArgumentTypes(member.descriptor);
		if (member.annotations.containsKey("SimpleProperty")) {
			if (result.getSort() == org.objectweb.asm.Type.VOID && arguments.length != 1) {
				compiler.error(Scanner.NO_POSITION, "类库属性设置器参数无效：%1", member.name);
				return;
			}
			ObjectScope scope = object.getScope();
			if (scope.lookupInObject(member.name) == null) {
				object.addProperty(new PropertySymbol(Scanner.NO_POSITION, object, member.name,
						type(result.getSort() == org.objectweb.asm.Type.VOID ? arguments[0] : result)), null, null);
			}
		} else if (member.annotations.containsKey("SimpleFunction") || member.annotations.containsKey("SimpleEvent")) {
			boolean event = member.annotations.containsKey("SimpleEvent");
			FunctionSymbol function = event ? new EventSymbol(Scanner.NO_POSITION, object, member.name)
					: (member.access & Opcodes.ACC_STATIC) != 0
					? new ObjectFunctionSymbol(Scanner.NO_POSITION, object, member.name)
					: new InstanceFunctionSymbol(Scanner.NO_POSITION, object, member.name);
			if (!event) {
				function.setResultType(type(result));
			}
			for (org.objectweb.asm.Type argument : arguments) {
				function.addParameter(Scanner.NO_POSITION, null, type(argument), reference(argument));
			}
			if (event) {
				object.addEvent((EventSymbol) function);
			} else if (!object.addFunction(function)) {
				compiler.error(Scanner.NO_POSITION, Error.errSymbolRedefinition, member.name);
			} else if ((member.access & Opcodes.ACC_STATIC) != 0 && rootObject(object)) {
				globalAlias(member.name, object.getScope().lookupShallow(member.name));
			}
		}
	}

	/** 仅运行库根命名空间中的静态函数和数据成员自动公开全局别名。 */
	private static boolean rootObject(ObjectSymbol object) {
		return object.getNamespace().internalName().equals(Compiler.RUNTIME_ROOT_PACKAGE.replace('.', '/') + "/");
	}

	/** 全局别名指向同一重载组，不混入其它对象的同名函数。 */
	private void globalAlias(String name, Symbol target) {
		GlobalScope scope = (GlobalScope) compiler.getGlobalNamespaceSymbol().getScope();
		Symbol previous = scope.lookupShallow(name);
		if (previous == null) {
			scope.enterSymbol(new AliasSymbol(Scanner.NO_POSITION, name, target));
		} else if (previous != target) {
			compiler.error(Scanner.NO_POSITION, Error.errSymbolRedefinition, name);
		}
	}

	/** 从实现类的接口和祖先寻找组件，不加载其 Android 或第三方父类。 */
	private void findComponents(String implementation, String name, Set<String> visited) {
		Definition definition = definitions.get(name);
		if (definition == null || !visited.add(name)) {
			return;
		}
		if (definition.extension && definition.annotations.containsKey("SimpleObject")
					&& definition.annotations.containsKey("SimpleComponent")) {
			loader.registerLibraryComponent(name, implementation);
			return;
		}
		for (String iface : definition.interfaces) {
			findComponents(implementation, iface, visited);
		}
		findComponents(implementation, definition.parent, visited);
	}

	/** 识别运行库既有的引用参数包装类型，不把普通对象误认成引用参数。 */
	private static boolean reference(org.objectweb.asm.Type value) {
		if (value.getSort() != org.objectweb.asm.Type.OBJECT
				|| !value.getInternalName().startsWith("simple/runtime/parameters/")) return false;
		String name = value.getInternalName().substring(value.getInternalName().lastIndexOf('/') + 1);
		switch (name) {
		case "BooleanReferenceParameter": case "ByteReferenceParameter": case "ShortReferenceParameter":
		case "IntegerReferenceParameter": case "LongReferenceParameter": case "SingleReferenceParameter":
		case "DoubleReferenceParameter": case "StringReferenceParameter": case "DateReferenceParameter":
		case "VariantReferenceParameter": case "ObjectReferenceParameter": return true;
		default: return false;
		}
	}

	/** 将 JVM 类型转换为 Simple 类型；对象签名只登记名称，不加载对应 Java 类。 */
	private Type type(org.objectweb.asm.Type value) {
		switch (value.getSort()) {
		case org.objectweb.asm.Type.VOID: return null;
		case org.objectweb.asm.Type.BOOLEAN: return BooleanType.booleanType;
		case org.objectweb.asm.Type.BYTE: return ByteType.byteType;
		case org.objectweb.asm.Type.SHORT: return ShortType.shortType;
		case org.objectweb.asm.Type.INT: return IntegerType.integerType;
		case org.objectweb.asm.Type.LONG: return LongType.longType;
		case org.objectweb.asm.Type.FLOAT: return SingleType.singleType;
		case org.objectweb.asm.Type.DOUBLE: return DoubleType.doubleType;
		case org.objectweb.asm.Type.ARRAY: return new ArrayType(type(value.getElementType()), value.getDimensions());
		default:
			String name = value.getInternalName();
			if (reference(value)) {
				String kind = name.substring(name.lastIndexOf('/') + 1).replace("ReferenceParameter", "");
				switch (kind) {
				case "Boolean": return BooleanType.booleanType;
				case "Byte": return ByteType.byteType;
				case "Short": return ShortType.shortType;
				case "Integer": return IntegerType.integerType;
				case "Long": return LongType.longType;
				case "Single": return SingleType.singleType;
				case "Double": return DoubleType.doubleType;
				case "String": return StringType.stringType;
				case "Date": return DateType.dateType;
				case "Variant": return VariantType.variantType;
				default: return ObjectType.objectType;
				}
			}
			if ("java/lang/String".equals(name)) return StringType.stringType;
			if ("java/util/Calendar".equals(name)) return DateType.dateType;
			if (VariantType.VARIANT_NAME.replace('.', '/').equals(name)) return VariantType.variantType;
			return ObjectSymbol.getObjectSymbol(compiler, name).getType();
		}
	}

	/** 只折叠 class 文件明确记录的编译期常量，不执行静态初始化获取值。 */
	private ConstantExpression constant(Member member) {
		if (member.value instanceof String) {
			return new ConstantStringExpression(Scanner.NO_POSITION, (String) member.value);
		}
		if (member.value instanceof Number) {
			if ("Z".equals(member.descriptor)) {
				return new ConstantBooleanExpression(Scanner.NO_POSITION, ((Number) member.value).intValue() != 0);
			}
			return new ConstantNumberExpression(Scanner.NO_POSITION, new BigDecimal(member.value.toString()));
		}
		return null;
	}

	/** 单个类的头、成员和 Simple 注解；不保存方法体。 */
	private static final class Definition {
		private String name;
		private String parent;
		private String[] interfaces = new String[0];
		private int access;
		private boolean extension;
		private final List<Member> methods = new ArrayList<Member>();
		private final List<Member> fields = new ArrayList<Member>();
		private final List<Map<String, String>> permissionAnnotations = new ArrayList<Map<String, String>>();
		private final Map<String, Map<String, String>> annotations = new HashMap<String, Map<String, String>>();
	}

	/** 单个成员的 JVM 签名、编译期常量和导出注解。 */
	private static final class Member {
		private final int access;
		private final String name;
		private final String descriptor;
		private final Object value;
		private final Map<String, Map<String, String>> annotations = new HashMap<String, Map<String, String>>();

		private Member(int access, String name, String descriptor, Object value) {
			this.access = access;
			this.name = name;
			this.descriptor = descriptor;
			this.value = value;
		}
	}
}

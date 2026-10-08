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
import simple.classfiles.ConstantPoolOverflowException;
import simple.classfiles.Method;
import simple.classfiles.MethodTooBigException;
import simple.compiler.Compiler;
import simple.compiler.Error;
import simple.compiler.Project;
import simple.compiler.RuntimeLoader;
import simple.compiler.expressions.AliasExpression;
import simple.compiler.scanner.Scanner;
import simple.compiler.scopes.ObjectScope;
import simple.compiler.scopes.Scope;
import simple.compiler.symbols.synthetic.ConstructorSymbol;
import simple.compiler.types.ObjectType;
import simple.compiler.types.Type;
import simple.compiler.util.Signatures;
import simple.util.Files;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 定义对象的符号（相当于Java类）。
 *
 * @author Herbert Czymontek
 * @author xhwsd@qq.com
 */
public class ObjectSymbol extends Symbol
		implements SymbolCanBePartOfQualifiedIdentifier, SymbolWithType {

	// 日志记录支持
	private static final Logger LOG = Logger.getLogger(ObjectSymbol.class.getName());

	// 对象类型
	private Type type;

	// 在对象源中定义的别名列表
	private List<AliasExpression>aliases;

	// 对象在其中定义的命名空间
	private NamespaceSymbol namespace;

	// 默认构造函数
	private ConstructorSymbol constructor;

	// 由对象定义的数据成员列表
	private List<DataMemberSymbol> dataMembers;

	// 对象定义的函数和过程列表
	private List<FunctionSymbol> functions;

	// 对象定义的属性列表
	private List<PropertySymbol> properties;

	// 对象定义的事件处理程序列表
	private Map<String, EventHandlerSymbol> eventHandlers;

	// 对象定义的事件列表
	private List<EventSymbol> events;

	// 对象的基础对象（等价于Java超类）
	// 请注意与Java有所不同：如果没有基础对象，
	// 而Java假定至少具有 java.lang.Object 作为超类，则此参数为 null。
	private ObjectType baseObject;

	// 对象实现的接口列表
	private List<ObjectType> interfaces;

	// 对象的外部类（仅用于编译器生成的对象）
	private ObjectSymbol outerclass;

	// 对象的内部类列表（只有编译器生成内部类）
	private List<ObjectSymbol> innerclasses;

	// 对象初始化事件处理程序（可以为null）
	private EventHandlerSymbol objectLoadEventHandler;

	// 实例初始化事件处理程序（可以为null）
	private EventHandlerSymbol instanceInitializeEventHandler;

	// 对象的作用域
	private ObjectScope scope;

	// 指示对象是否为接口
	private boolean isInterface;

	// 指示对象是否为窗口
	private boolean isForm;

	/**
	 * 此工厂方法要么返回对象的现有符号，要么创建一个新的符号。
	 *
	 * @param compiler  当前编译器实例
	 * @param internalName  对象的内部名称
	 * @return  对象符号
	 */
	public static ObjectSymbol getObjectSymbol(Compiler compiler, String internalName) {
		String classname = Signatures.getInternalClassName(internalName);
		NamespaceSymbol namespace = NamespaceSymbol.getNamespaceSymbol(compiler,
				Signatures.getInternalPackageName(internalName));

		ObjectSymbol objectSymbol = (ObjectSymbol) namespace.getScope().lookupShallow(classname);
		if (objectSymbol == null) {
			objectSymbol = new ObjectSymbol(Scanner.NO_POSITION, classname, namespace, null);
			namespace.getScope().enterSymbol(objectSymbol);
		}
		return objectSymbol;
	}

	/**
	 * 创建新的对象符号
	 *
	 * @param position  源代码符号的起始位置
	 * @param name  对象名称
	 * @param namespace  对象所属的命名空间
	 * @param outerclass  对象的外部类（必须是{@code null}，除非对象是编译器生成的）
	 */
	public ObjectSymbol(long position, String name, NamespaceSymbol namespace,
			ObjectSymbol outerclass) {
		super(position, name);

		this.namespace = namespace;
		this.outerclass = outerclass;

		aliases = new ArrayList<AliasExpression>();

		dataMembers = new ArrayList<DataMemberSymbol>();
		functions = new ArrayList<FunctionSymbol>();
		properties = new ArrayList<PropertySymbol>();
		eventHandlers = new HashMap<String, EventHandlerSymbol>();
		events = new ArrayList<EventSymbol>();
		innerclasses = new ArrayList<ObjectSymbol>();
		interfaces = new ArrayList<ObjectType>();

		scope = new ObjectScope(this,
				outerclass != null ? outerclass.getScope() : namespace.getScope());
		type = new ObjectType(this);

		if (outerclass != null) {
			outerclass.addInnerclass(this);
		}
	}

	/**
	 * 设置基础对象（与Java中的超类相同）。
	 *
	 * @param baseObject  基础对象
	 */
	public void setBaseObject(ObjectType baseObject) {
		this.baseObject = baseObject;
	}

	/**
	 * 将对象标记为接口对象。
	 */
	public void markAsInterface() {
		isInterface = true;
	}

	/**
	 * 将类标记为已编译（而不是正在从ClassFile加载)。
	 */
	public void markAsCompiled() {
		// 这只是需要生成默认构造函数
		constructor = new ConstructorSymbol(this);
	}

	/**
	 * 在对象的源文件中添加别名定义。
	 *
	 * @param alias  alias definition
	 */
	public void addAlias(AliasExpression alias) {
		// TODO: check for re-alias? Warning?
		// TODO: is it OK for an alias to hide a member?
		aliases.add(alias);
	}

	/**
	 * 将新数据成员添加到对象
	 *
	 * @param dataMember  data member symbol to be added
	 * @return  {@code true} after successfully added the member, {@code false}
	 *          if another member with the same name was already defined in the
	 *          object
	 */
	public boolean addDataMember(DataMemberSymbol dataMember) {
		// All names must be unique within an object
		if (scope.lookupShallow(dataMember.getName()) != null) {
			return false;
		}

		dataMembers.add(dataMember);
		scope.enterSymbol(dataMember);
		return true;
	}

	/**
	 * 为对象添加新函数。
	 *
	 * @param function  function symbol to be added
	 * @return  {@code true} after successfully added the member, {@code false}
	 *          if another member with the same name was already defined in the
	 *          object
	 */
	public boolean addFunction(FunctionSymbol function) {
		Symbol existing = scope.lookupShallow(function.getName());
		if (existing == null) {
			functions.add(function);
			scope.enterSymbol(function);
			return true;
		}

		// 函数和过程可以同名重载，但同一重载组只能按参数个数区分。
		// 数据成员、属性和事件仍与函数名称互斥；对象函数和实例函数也不混入同一组。
		if (!(existing instanceof FunctionSymbol)) {
			return false;
		}
		FunctionSymbol firstOverload = (FunctionSymbol) existing;
		if (firstOverload.hasMeArgument() != function.hasMeArgument() ||
				!firstOverload.addOverload(function)) {
			return false;
		}
		functions.add(function);
		return true;
	}

	/**
	 * 从当前对象开始，按名称和参数个数查找函数或过程。
	 *
	 * <p>当前对象存在同名成员但没有对应参数个数时，继续到基础对象中查找；
	 * 同参数个数的派生成员自然优先于基础对象成员。
	 *
	 * @param name 函数或过程名称
	 * @param parameterCount 调用实参数量
	 * @param instanceFunction 是否查找实例函数或过程
	 * @return 匹配的函数或过程；没有匹配项时返回{@code null}
	 */
	public FunctionSymbol lookupFunction(String name, int parameterCount,
			boolean instanceFunction) {
		ObjectType objectType = (ObjectType) getType();
		do {
			Symbol member = objectType.getObjectSymbol().getScope().lookupShallow(name);
			if (member != null) {
				if (!(member instanceof FunctionSymbol)) {
					return null;
				}
				FunctionSymbol firstOverload = (FunctionSymbol) member;
				if (firstOverload.hasMeArgument() != instanceFunction) {
					return null;
				}
				FunctionSymbol overload = firstOverload.getOverload(parameterCount);
				if (overload != null) {
					return overload;
				}
			}
			objectType = objectType.getObjectSymbol().getBaseObject();
		} while (objectType != null);

		return null;
	}

	/**
	 * 为对象添加新属性。
	 *
	 * @param property  property symbol to be added
	 * @param getter  getter function for the property or {@code null} if there
	 *                is no getter function
	 * @param setter  setter procedure for the property or {@code null} if there
	 *                is no setter function
	 * @return  {@code true} after successfully added the member, {@code false}
	 *          if another member with the same name was already defined in the
	 *          object
	 */
	public boolean addProperty(PropertySymbol property, FunctionSymbol getter,
			FunctionSymbol setter) {
		// All names must be unique within an object
		if (scope.lookupShallow(property.getName()) != null) {
			return false;
		}

		properties.add(property);
		scope.enterSymbol(property);

		if (getter != null) {
			functions.add(getter);
		}

		if (setter != null) {
			functions.add(setter);
		}

		return true;
	}

	/**
	 * 为对象添加新的事件定义。
	 *
	 * @param event  event symbol to be added
	 * @return  {@code true} after successfully adding the member, {@code false}
	 *          if another member with the same name was already defined in the
	 *          object
	 */
	public boolean addEvent(EventSymbol event) {
		// All names must be unique within an object
		if (scope.lookupShallow(event.getName()) != null) {
			return false;
		}

		events.add(event);
		functions.add(event);

		scope.enterSymbol(event);
		return true;
	}

	/**
	 * 将新事件处理程序定义添加到对象。
	 *
	 * @param eventHandler  event handler symbol to be added
	 * @return  {@code true} after successfully adding the member, {@code false}
	 *          if another event handler with the same name was already defined
	 *          in the object
	 */
	public boolean addEventHandler(EventHandlerSymbol eventHandler) {
		String combinedName = eventHandler.getEventTargetName() + '.' + eventHandler.getName();
		if (eventHandlers.containsKey(combinedName)) {
			return false;
		}

		eventHandlers.put(combinedName, eventHandler);
		functions.add(eventHandler);
		return true;
	}

	/**
	 * Adds an object as an inner class to this object. Note that only compiler
	 * generated objects can be inner classes.
	 *
	 * @param innerclass  inner class object
	 */
	protected void addInnerclass(ObjectSymbol innerclass) {
		innerclasses.add(innerclass);
	}

	/**
	 * Adds an object type as an interface to be implemented by this object.
	 *
	 * @param in  interface to be implemented
	 */
	public void addInterface(ObjectType in) {
		// TODO: warning on re-implementation?
		interfaces.add(in);
	}

	/**
	 * Sets an event handler to be invoked upon object loading (invoked from
	 * <clinit>).
	 *
	 * @param eventHandler  object load event handler
	 */
	public void setObjectLoadEventHandler(EventHandlerSymbol eventHandler) {
		objectLoadEventHandler = eventHandler;
	}

	/**
	 * Sets an event handler to be invoked upon instance creation (invoked from
	 * <init>).
	 *
	 * @param eventHandler  instance initialization event handler
	 */
	public void setInstanceInitializeEventHandler(EventHandlerSymbol eventHandler) {
		instanceInitializeEventHandler = eventHandler;
	}

	/**
	 * 返回实例初始化事件处理程序。
	 *
	 * @return  instance initialization event handler
	 */
	public EventHandlerSymbol getInstanceInitializeEventHandler() {
		return instanceInitializeEventHandler;
	}

	/**
	 * 返回对象的默认构造函数。
	 *
	 * @return  default constructor
	 */
	public ConstructorSymbol getConstructor() {
		return constructor;
	}

	/**
	 * 指示对象是从类文件加载还是正在编译。
	 *
	 * @return  {@code true} if object was loaded from a class file, {@code
	 *          false} otherwise
	 */
	public boolean isLoaded() {
		return constructor == null;
	}

	/**
	 * 指示对象是否为接口对象。
	 *
	 * @return  {@code true} if the object is an interface object, {@code false}
	 *          otherwise
	 */
	public boolean isInterface() {
		return isInterface;
	}

	/**
	 * 指示对象是否为窗口。
	 *
	 * @return  {@code true} if the object is a form, {@code false} otherwise
	 */
	public boolean isForm() {
		return isForm;
	}

	/**
	 * 返回对象的基础对象。
	 *
	 * @return  base object or {@code null} if there is no base object
	 */
	public ObjectType getBaseObject() {
		return baseObject;
	}

	/**
	 * 返回对象的外部类。只有编译器生成的对象才能有外部类。
	 *
	 * @return  outer class or {@code null}
	 */
	public ObjectSymbol getOuterclass() {
		return outerclass;
	}

	/**
	 * 返回由对象实现的接口的列表。
	 *
	 * @return  list of interfaces (never {@code null} but can be empty of
	 *          course)
	 */
	public List<ObjectType> getInterfaces() {
		return interfaces;
	}

	/**
	 * 返回由对象定义的数据成员列表。
	 *
	 * @return  list of data members (never {@code null} but can be empty of
	 *          course)
	 */
	public List<DataMemberSymbol> getDataMembers() {
		return dataMembers;
	}

	/**
	 * 返回由对象定义的函数列表。
	 *
	 * @return  list of functions (never {@code null} but can be empty of
	 *          course)
	 */
	public List<FunctionSymbol> getFunctions() {
		return functions;
	}

	/**
	 * 返回由对象定义的事件列表。
	 *
	 * @return  list of events (never {@code null} but can be empty of
	 *          course)
	 */
	public List<EventSymbol> getEvents() {
		return events;
	}

	/**
	 * 返回对象中定义的事件处理程序列表。
	 *
	 * @return  事件处理程序列表
	 */
	public Collection<EventHandlerSymbol> getEventHandlers() {
		return eventHandlers.values();
	}

	@Override
	public ObjectScope getScope() {
		return scope;
	}

	/**
	 * 返回对象所属的命名空间。
	 *
	 * @return  命名空间符号
	 */
	public NamespaceSymbol getNamespace() {
		return namespace;
	}

	/**
	 * 返回对象的名称。对于编译器生成的对象，
	 * 将生成唯一的名称。
	 *
	 * @return  对象的名称
	 */
	public String getObjectName() {
		StringBuilder objectName = new StringBuilder();
		if (outerclass != null) {
			objectName.append(outerclass.getObjectName());
			objectName.append('$');
		}

		if (getName() == null) {
			objectName.append(outerclass.innerclasses.indexOf(this));
		} else {
			objectName.append(getName());
		}

		return objectName.toString();
	}

	@Override
	public void resolve(Compiler compiler, FunctionSymbol currentFunction) {
		// 在开始解析之前，我们需要在符号表中输入别名符号
		Scope globalScope = compiler.getGlobalNamespaceSymbol().getScope();
		for (AliasExpression alias : aliases) {
			alias.resolve(compiler, null);
			globalScope.enterSymbol(alias.getAliasSymbol());
		}

		// 首先解析基础对象
		if (baseObject != null) {
			baseObject.resolve(compiler);
		}

		// 其次是接口类型
		for (ObjectType interfaceType : interfaces) {
			interfaceType.resolve(compiler);
		}

		// 并生成内部类
		for (ObjectSymbol innerclass : innerclasses) {
			innerclass.resolve(compiler, null);
		}

		// 最后解析数据成员，属性，事件和函数（但还没有它们的主体！）
		for (DataMemberSymbol dataMember : dataMembers) {
			dataMember.resolve(compiler, currentFunction);
		}
		for (PropertySymbol property : properties) {
			property.resolve(compiler, currentFunction);
		}
		for (EventSymbol event : events) {
			event.resolve(compiler, null);
		}
		for (FunctionSymbol function : functions) {
			function.resolve(compiler, null);
		}

		isForm = scope.lookupShallow("$define") != null;

		// 解析构造函数（如果对象有一个）
		if (constructor != null) {
			constructor.resolve(compiler, null);
		}

		// 不要忘记再次删除别名符号 （它们是在此对象本地定义的，必须徘徊并被其他对象拾取）
		for (AliasExpression alias : aliases) {
			globalScope.removeSymbol(alias.getAliasSymbol());
		}

		// 检查此符号是否引用需要Android权限的组件类
		compiler.checkForPermissions(this);
		// 清单节点和权限一样，只对当前项目实际引用的运行库对象生效。
		compiler.checkForManifestNodes(this);

		// 检验这个对象是否来自已经加载的类文件。
		if (isLoaded()) {
			// 核心运行库对象会被忽略，只有扩展库对象进入后续打包集合。
			compiler.addSimpleNeedingLibraryObject(this);
		} else {
			// 单元对象
			// 检验当前基础对象是否为 simple/runtime/collections/服务
			if (getBaseObjectInternalName(compiler).equals(Compiler.RUNTIME_ROOT_INTERNAL + "/collections/服务")) {
				// 添加项目需要服务
				compiler.addSimpleNeedingService(getNamespace().internalName() + getObjectName());
			}
		}
	}

	/**
	 * 解析对象定义的函数的函数实体。
	 *
	 * @param compiler  当前编译器实例
	 */
	public void resolveFunctionBodies(Compiler compiler) {
		// 在开始解析之前，我们需要在符号表中输入别名符号
		Scope globalScope = compiler.getGlobalNamespaceSymbol().getScope();
		for (AliasExpression alias : aliases) {
			globalScope.enterSymbol(alias.getAliasSymbol());
		}

		// 解析函数体
		for (FunctionSymbol function : functions) {
			function.resolveBody(compiler);
		}

		// 不要忘记再次删除别名符号（它们是在此对象本地定义的，必须徘徊并被其他对象拾取）
		for (AliasExpression alias : aliases) {
			globalScope.removeSymbol(alias.getAliasSymbol());
		}
	}

	@Override
	public void generateRead(Method m) {
		// 静态数据成员或函数的限定标识符将发生这种情况
	}

	@Override
	public void generateWrite(Method m) {
		// 静态数据成员的限定标识符将发生这种情况
	}

	/**
	 * 为对象生成类文件。
	 *
	 * @param compiler  当前编译器实例e
	 * @param buildDirectory  类文件的目标根目录
	 */
	public void generate(Compiler compiler, File buildDirectory) {
		writeClassFile(compiler, generate(compiler), buildDirectory);
	}

	/**
	 * 在内存中生成类文件，并将类文件添加到给定列表中。
	 *
	 * @param compiler  当前编译器实例
	 * @param classes  生成的类文件列表
	 */
	public void generate(Compiler compiler, List<ClassFile> classes) {
		classes.add(generate(compiler));
	}

	/**
	 * 将对象的生成的类文件写入其目标目录。
	 *
	 * @param compiler  当前编译器实例
	 * @param cf  类文件
	 * @param buildDirectory  目标根目录
	 */
	protected void writeClassFile(Compiler compiler, ClassFile cf, File buildDirectory) {
		File classFile = new File(namespace.generate(buildDirectory), getObjectName() + ".class");
		try {
			// 生成类文件数据并写入类文件
			Files.write(cf.generate(), classFile);
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "类文件写入错误", e);
			compiler.error(Scanner.NO_POSITION, Error.errWriteError, classFile.getName());
		}
	}

	/*
	 * 生成一个类初始化程序，它将触发对象 加载() 事件，并初始化静态大小的数据成员数组。
	 */
	private void generateClassInitializer(ClassFile cf) {
		// xhwsd@qq.com 2021-5-22 为对象标识初始化<clinit>方法添加static标志，解决APK无法在高版本系统运行
		Method m = cf.newMethod((short) (Method.ACC_SYNTHETIC | Method.ACC_PUBLIC | Method.ACC_STATIC), "<clinit>", "()V");
		try {
			m.startCodeGeneration();

			// 初始化静态大小的数据成员
			for (DataMemberSymbol dataMember : dataMembers) {
				if (dataMember.getClass() == ObjectDataMemberSymbol.class) {
					dataMember.generateInitializer(m);
				}
			}

			// 触发对象加载事件
			if (objectLoadEventHandler != null) {
				m.generateInstrInvokestatic(getType().internalName(),
						objectLoadEventHandler.getName(), "()V");
			}

			m.generateInstrReturn();
			m.finishCodeGeneration();
		} catch (MethodTooBigException e) {
			Compiler.internalError();  // COV_NF_LINE
		}
	}

	/**
	 * 返回基础对象类型的内部名称。
	 *
	 * @param compiler  当前编译器实例
	 * @return  基础对象类型的内部名称
	 */
	public String getBaseObjectInternalName(Compiler compiler) {
		if (baseObject == null) {
			return "java/lang/Object";
		}

		// 我们需要在这里对窗口进行特殊处理（因为它是一个组件）。
		return isForm() ?
					compiler.getComponentImplementationInternalName(baseObject) :
					baseObject.getType().internalName();
	}

	/*
	 * 生成对象的Java类。
	 */
	private ClassFile generate(Compiler compiler) {
		short flags = ClassFile.ACC_PUBLIC;
		if (isInterface()) {
			flags |= ClassFile.ACC_INTERFACE | ClassFile.ACC_ABSTRACT;
		} else {
			flags |= ClassFile.ACC_SUPER;
		}

		String internalName = getType().internalName();
		String baseObjectInternalName = getBaseObjectInternalName(compiler);

		compiler.addClassfile(internalName);

		ClassFile cf = new ClassFile(flags, internalName, baseObjectInternalName);
		try {
			for (ObjectType interfaceType : interfaces) {
				cf.addInterfaceImplementation(interfaceType.internalName());
			}

			for (DataMemberSymbol dataMember : dataMembers) {
				dataMember.generate(cf);
			}

			generateClassInitializer(cf);

			for (FunctionSymbol function : functions) {
				function.generate(compiler, cf);
			}

			if (!isInterface) {
				constructor.generate(compiler, cf);
			}

			for (ObjectSymbol innerclass : innerclasses) {
				cf.addInnerClass(ClassFile.ACC_PUBLIC, innerclass.getName(),
						innerclass.getType().internalName());
			}

			if (outerclass != null) {
				cf.addOuterClass(ClassFile.ACC_PUBLIC, outerclass.getName(),
						outerclass.getType().internalName());
			}

			cf.getRuntimeVisibleAnnotationsAttribute().newAnnotation(
					'L' + RuntimeLoader.ANNOTATION_INTERNAL + "/SimpleObject;");

			cf.setSourceFile(getName() + Project.SOURCEFILE_EXTENSION);
		} catch (ConstantPoolOverflowException e) {
			compiler.error(Scanner.NO_POSITION, Error.errConstantPoolOverflow,
					internalName.replace('/', '.'));
		}

		return cf;
	}

	// SymbolWithType 实现

	public final Type getType() {
		return type;
	}

	public final void setType(Type type) {
		this.type = type;
	}
}

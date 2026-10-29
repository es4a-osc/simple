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

import simple.classfiles.ClassFile;
import simple.compiler.parser.Parser;
import simple.compiler.scanner.Scanner;
import simple.compiler.symbols.AliasSymbol;
import simple.compiler.symbols.NamespaceSymbol;
import simple.compiler.symbols.ObjectSymbol;
import simple.compiler.types.ObjectType;
import simple.compiler.util.Resource;
import simple.compiler.util.Signatures;
import simple.compiler.util.Toolchain;
import simple.util.Files;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

/**
 * Simple编译器的主要入口点。
 *
 * <p>为构建和部署Simple项目提供入口点。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public final class Compiler {

	// 文件名和位置
	private static final String ANDROID_HOME = System.getenv("ANDROID_HOME");
	private static final String SIMPLE_HOME = System.getenv("SIMPLE_HOME");
	// 可选扩展库根目录；未设置时编译器只使用Simple运行库。
	private static final String LIBRARIES_HOME = System.getenv("LIBRARIES_HOME");
	private static final String KEY_PASSWORD = System.getenv("KEY_PASSWORD");

	/* 运行时 */
	private static final String ANDROID_RUNTIME = ANDROID_HOME + "/platforms/android-" + Toolchain.API_LEVEL + "/android.jar";
	private static final String SIMPLE_ANDROID_RUNTIME = SIMPLE_HOME + "/SimpleAndroidRuntime.jar";

	// 签名APK文件的密钥库
	private static final String ANDROID_DEBUG_KEYSTORE = "android_debug.p12";

	/**
	 * 目标编译平台。
	 */
	public enum Platform {

		None(""),
		Android(SIMPLE_ANDROID_RUNTIME);

		// Simple运行时库
		private final String runtimeLibrary;

		Platform(String runtimeLibrary) {
			this.runtimeLibrary = runtimeLibrary;
		}

		public String getRuntimeLibrary() {
			return runtimeLibrary;
		}
	}

	/** Simple运行时的根包名称。所有运行时库包都是此包的子包。 */
	public static final String RUNTIME_ROOT_PACKAGE = "simple.runtime";
	public static final String RUNTIME_ROOT_INTERNAL = RUNTIME_ROOT_PACKAGE.replace('.', '/');
	/** Simple资源索引在应用包中的实际对象名称，避开Android标准资源类{@code R}。 */
	private static final String SIMPLE_RESOURCE_OBJECT_NAME = "SimpleResources";
	/** Simple代码访问当前项目资源索引时使用的全局名称。 */
	private static final String SIMPLE_RESOURCE_GLOBAL_NAME = "R";

	/** 运行时错误超类的内部名称 */
	public static final String RUNTIME_ERROR_INTERNAL_NAME = RUNTIME_ROOT_INTERNAL + "/errors/运行错误";

	// 日志记录支持
	private static final Logger LOG = Logger.getLogger(Compiler.class.getName());
	private static final Pattern MANIFEST_MACRO_PATTERN =
			Pattern.compile("\\$\\{([^}=]+)(?:=([^}]*))?\\}");

	// 编译器要使用的标准和错误流
	private PrintStream out;
	private PrintStream err;

	// 源文件的字符编码
	private String encoding;

	// 目标平台
	private Platform platform;

	// 正在编译的对象列表
	private List<ObjectSymbol> objects;

	// 生成的类文件列表
	private List<String> classfiles;

	// 编译错误和警告数
	private int errorCount;
	private int warningCount;

	// 全局（未命名）命名空间符号
	private NamespaceSymbol globalNamespaceSymbol;

	// 运行时错误类型
	private final ObjectType runtimeErrorType;

	// 文件索引到文件名映射
	private final List<String> filenameMap;

	// Map from object symbols of components needing Android permissions to the corresponding class
	// Note that this included components that may not be referenced by the currently compiling
	// package
	private final Map<ObjectSymbol, Class<?>> symbolToClassNeedingPermissionsMap;

	// Set of component classes referenced by the current package that require Android permissions
	private Set<Class<?>> classesNeedingPermissions;

	// 清单节点注解类按对象符号登记，解析时只收集项目实际引用的对象。
	private final Map<ObjectSymbol, Class<?>> symbolToClassNeedingManifestNodesMap;
	private Set<Class<?>> classesNeedingManifestNodes;

	// 运行时库加载器和分析器
	private RuntimeLoader runtimeLoader;

	// Simple项目使用服务集合
	private Set<String> simpleNeedingServices;

	// 当前项目实际引用的扩展库对象，用于只打包需要的扩展库。
	private Set<ObjectSymbol> simpleNeedingLibraryObjects;

	// 扩展库注解直接来自类文件，不保留必须先解析第三方类型的反射 Class。
	private final Map<ObjectSymbol, List<Map<String, String>>> libraryPermissions = new HashMap<ObjectSymbol, List<Map<String, String>>>();
	private final Map<ObjectSymbol, Map<String, String>> libraryManifestNodes = new HashMap<ObjectSymbol, Map<String, String>>();
	private final Set<ObjectSymbol> usedLibraryAnnotations = new LinkedHashSet<ObjectSymbol>();

	/**
	 * 保存扩展库对象的权限和清单注解，只有对象实际被使用时才参与生成。
	 *
	 * @param object 对象符号。
	 * @param permissions 可选的批量权限注解字段。
	 * @param singlePermissions 单项权限注解字段，包含展开后的重复注解。
	 * @param nodes 可选的清单注解字段。
	 */
	void addLibraryAnnotations(ObjectSymbol object, Map<String, String> permissions,
			List<Map<String, String>> singlePermissions, Map<String, String> nodes) {
		List<Map<String, String>> declarations = new ArrayList<Map<String, String>>(singlePermissions);
		if (permissions != null) {
			Map<String, String> group = new HashMap<String, String>(permissions);
			group.putIfAbsent("permissionNames", "");
			declarations.add(group);
		}
		if (!declarations.isEmpty()) {
			libraryPermissions.put(object, declarations);
		}
		if (nodes != null) {
			libraryManifestNodes.put(object, nodes);
		}
	}

	/**
	 * 记录当前项目实际引用的扩展库对象。
	 *
	 * <p>核心运行库对象没有扩展库目录，不会进入该集合。
	 *
	 * @param objectSymbol 已从类文件加载的Simple对象
	 */
	public void addSimpleNeedingLibraryObject(ObjectSymbol objectSymbol) {
		String internalName = objectSymbol.getNamespace().internalName() + objectSymbol.getObjectName();
		if (runtimeLoader.getExtensionLibraryDirectory(internalName) != null) {
			simpleNeedingLibraryObjects.add(objectSymbol);
		}
	}

	/*
	 * 按稳定路径顺序返回当前项目实际使用的扩展库目录。
	 */
	private List<File> getSimpleNeedingLibraryDirectories() {
		Set<File> directories = new LinkedHashSet<File>();
		for (ObjectSymbol objectSymbol : simpleNeedingLibraryObjects) {
			String internalName = objectSymbol.getNamespace().internalName()
					+ objectSymbol.getObjectName();
			File directory = runtimeLoader.getExtensionLibraryDirectory(internalName);
			if (directory != null) {
				directories.add(directory.getAbsoluteFile());
			}
		}
		List<File> result = new ArrayList<File>(directories);
		Collections.sort(result, new java.util.Comparator<File>() {
			@Override
			public int compare(File left, File right) {
				int comparison = left.getAbsolutePath().compareToIgnoreCase(right.getAbsolutePath());
				return comparison != 0 ? comparison
						: left.getAbsolutePath().compareTo(right.getAbsolutePath());
			}
		});
		return result;
	}

	private static boolean hasFiles(File directory) {
		if (!directory.isDirectory()) {
			return false;
		}
		File[] files = directory.listFiles();
		if (files == null) {
			return false;
		}
		for (File file : files) {
			if (file.isFile() || hasFiles(file)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Add a component class to the list of Simple component classes that require Android permissions.
	 *
	 * @param usesPermissionsClass the component class specifying the required permission.
	 * @param usesPermissionObjectSymbol the {@link ObjectSymbol} corresponding to usesPermissionClass
	 */
	public void addToPermissions(Class<?> usesPermissionsClass,
															 ObjectSymbol usesPermissionObjectSymbol) {
		symbolToClassNeedingPermissionsMap.put(usesPermissionObjectSymbol, usesPermissionsClass);
	}

	/**
	 * Check to see if this object symbol corresponds to a Simple component class that requires
	 * Android permissions and add it to the set of such classes.
	 *
	 * @param objectSymbol the object symbol to check
	 */
	public void checkForPermissions(ObjectSymbol objectSymbol) {
		if (libraryPermissions.containsKey(objectSymbol)) {
			usedLibraryAnnotations.add(objectSymbol);
		}
		if (symbolToClassNeedingPermissionsMap.containsKey(objectSymbol)) {
			classesNeedingPermissions.add(symbolToClassNeedingPermissionsMap.get(objectSymbol));
		}
	}

	/**
	 * 汇总实际引用对象的批量及单项权限，合并同名权限的适用范围。
	 *
	 * <p>运行库注解来自独立类加载器，通过其自身的注解类型反射读取；扩展库直接消费类文件字段。
	 *
	 * @return 已验证并按权限名统一的清单需求。
	 * @throws IOException 注解定义、权限名称或版本范围无效。
	 */
	private ManifestPermissions generatePermissions() throws IOException {
		ManifestPermissions permissions = new ManifestPermissions();
		Class<? extends Annotation> groupType = runtimeLoader.getAndroidUsesPermissions();
		Class<? extends Annotation> singleType = runtimeLoader.getAndroidUsesPermission();
		try {
			Method names = groupType.getMethod("permissionNames");
			Method name = singleType.getMethod("permissionName");
			Method maximum = singleType.getMethod("maxSdkVersion");
			for (Class<?> type : classesNeedingPermissions) {
				Annotation group = type.getAnnotation(groupType);
				if (group != null) {
					permissions.addNames((String) names.invoke(group));
				}
				for (Annotation single : type.getAnnotationsByType(singleType)) {
					permissions.add((String) name.invoke(single), (Integer) maximum.invoke(single));
				}
			}
		} catch (ReflectiveOperationException failure) {
			throw new IOException("读取运行库权限注解失败：" + failure.getMessage(), failure);
		}

		// 类文件中的注解仅在所属对象实际使用后验证，未引用的对象不增加权限或触发无关错误。
		for (ObjectSymbol object : usedLibraryAnnotations) {
			List<Map<String, String>> declarations = libraryPermissions.get(object);
			if (declarations != null) {
				for (Map<String, String> declaration : declarations) {
					permissions.addAnnotation(declaration);
				}
			}
		}
		return permissions;
	}

	/**
	 * 登记声明了清单节点的运行库对象。
	 *
	 * @param manifestNodesClass 声明清单节点的类
	 * @param objectSymbol 对应的Simple对象符号
	 */
	public void addToManifestNodes(Class<?> manifestNodesClass, ObjectSymbol objectSymbol) {
		symbolToClassNeedingManifestNodesMap.put(objectSymbol, manifestNodesClass);
	}

	/**
	 * 把当前项目实际引用对象声明的清单节点加入生成集合。
	 *
	 * @param objectSymbol 已解析的对象符号
	 */
	public void checkForManifestNodes(ObjectSymbol objectSymbol) {
		if (libraryManifestNodes.containsKey(objectSymbol)) {
			usedLibraryAnnotations.add(objectSymbol);
		}
		Class<?> manifestNodesClass = symbolToClassNeedingManifestNodesMap.get(objectSymbol);
		if (manifestNodesClass != null) {
			classesNeedingManifestNodes.add(manifestNodesClass);
		}
	}

	private static final class ManifestNodeGroups {
		private final Set<String> rootNodes = new LinkedHashSet<String>();
		private final Set<String> applicationNodes = new LinkedHashSet<String>();
		private final Set<String> activityNodes = new LinkedHashSet<String>();
		private final Set<String> intentFilterNodes = new LinkedHashSet<String>();
	}

	/*
	 * 读取实际使用对象上的ManifestNodes注解。注解类来自独立类加载器，必须通过反射访问。
	 */
	private ManifestNodeGroups generateManifestNodes(Project project) {
		ManifestNodeGroups nodes = new ManifestNodeGroups();
		Class<? extends Annotation> annotationClass = runtimeLoader.getAndroidManifestNodes();
		Method rootXmlMethod;
		Method applicationXmlMethod;
		Method activityXmlMethod;
		Method intentFilterXmlMethod;
		try {
			rootXmlMethod = annotationClass.getMethod("rootXml");
			applicationXmlMethod = annotationClass.getMethod("applicationXml");
			activityXmlMethod = annotationClass.getMethod("activityXml");
			intentFilterXmlMethod = annotationClass.getMethod("intentFilterXml");
		} catch (NoSuchMethodException e) {
			LOG.log(Level.SEVERE, "无法读取ManifestNodes注解的方法。", e);
			error(Scanner.NO_POSITION, "无法读取ManifestNodes注解");
			return nodes;
		}

		for (Class<?> manifestNodesClass : classesNeedingManifestNodes) {
			Annotation annotation = manifestNodesClass.getAnnotation(annotationClass);
			if (annotation == null) {
				error(Scanner.NO_POSITION, "类 %1 缺少ManifestNodes注解", manifestNodesClass.getName());
				continue;
			}
			try {
				String className = manifestNodesClass.getSimpleName();
				addManifestNode(nodes.rootNodes,
						(String) rootXmlMethod.invoke(annotation), project, className);
				addManifestNode(nodes.applicationNodes,
						(String) applicationXmlMethod.invoke(annotation), project, className);
				addManifestNode(nodes.activityNodes,
						(String) activityXmlMethod.invoke(annotation), project, className);
				addManifestNode(nodes.intentFilterNodes,
						(String) intentFilterXmlMethod.invoke(annotation), project, className);
			} catch (InvocationTargetException e) {
				LOG.log(Level.SEVERE, "无法读取类的ManifestNodes注解：" + manifestNodesClass.getName(), e);
				error(Scanner.NO_POSITION, "无法读取类 %1 的ManifestNodes注解", manifestNodesClass.getName());
			} catch (IllegalAccessException e) {
				LOG.log(Level.SEVERE, "无法读取类的ManifestNodes注解：" + manifestNodesClass.getName(), e);
				error(Scanner.NO_POSITION, "无法读取类 %1 的ManifestNodes注解", manifestNodesClass.getName());
			}
		}
		for (ObjectSymbol object : usedLibraryAnnotations) {
			Map<String, String> annotation = libraryManifestNodes.get(object);
			if (annotation != null) {
				String name = object.getObjectName();
				addManifestNode(nodes.rootNodes, annotation.get("rootXml"), project, name);
				addManifestNode(nodes.applicationNodes, annotation.get("applicationXml"), project, name);
				addManifestNode(nodes.activityNodes, annotation.get("activityXml"), project, name);
				addManifestNode(nodes.intentFilterNodes, annotation.get("intentFilterXml"), project, name);
			}
		}

		return nodes;
	}

	/**
	 * 展开节点宏并检查权限声明边界，再保存仍保持原文的 XML 片段。
	 *
	 * @param nodes 当前插入位置的节点集合。
	 * @param xml 注解中的 XML 片段。
	 * @param project 项目宏配置。
	 * @param className 声明节点的类型简名。
	 */
	private void addManifestNode(Set<String> nodes, String xml, Project project, String className) {
		if (xml != null && !xml.isEmpty()) {
			String expanded = expandManifestMacros(project, className, xml);
			try {
				ManifestPermissions.validateManifestNodes(expanded);
				nodes.add(expanded);
			} catch (IOException failure) {
				error(Scanner.NO_POSITION, "类 %1 的清单节点无效：%2", className, failure.getMessage());
			}
		}
	}

	/*
	 * 宏值可能包含美元符号或反斜杠，必须作为普通替换文本写入。
	 */
	private String expandManifestMacros(Project project, String className, String xml) {
		Matcher matcher = MANIFEST_MACRO_PATTERN.matcher(xml);
		StringBuffer result = new StringBuffer();
		while (matcher.find()) {
			String macroName = matcher.group(1);
			String macroValue = project.getMacroValue(className, macroName, matcher.group(2));
			if (macroValue == null) {
				error(Scanner.NO_POSITION, "缺少必要清单宏 %1.%2", className, macroName);
				macroValue = matcher.group(0);
			}
			matcher.appendReplacement(result, Matcher.quoteReplacement(macroValue));
		}
		matcher.appendTail(result);
		return result.toString();
	}

	/*
	 * 将收集并去重后的清单节点写入AndroidManifest.xml的指定位置。
	 */
	private static void writeManifestNodes(OutputStreamWriter out, Set<String> nodes,
			String indentation, String label) throws IOException {
		if (nodes.isEmpty()) {
			return;
		}
		out.write(indentation + "<!-- " + label + " Manifest Nodes Start -->\n");
		for (String node : nodes) {
			out.write(indentation + node);
			if (!node.endsWith("\n") && !node.endsWith("\r")) {
				out.write("\n");
			}
		}
		out.write(indentation + "<!-- " + label + " Manifest Nodes End -->\n");
	}

	/** 防止应用名称中的 XML 特殊字符破坏清单属性。 */
	private static String escapeXmlAttribute(String value) {
		StringBuilder escaped = new StringBuilder(value.length());
		for (int i = 0; i < value.length(); i++) {
			switch (value.charAt(i)) {
				case '&':
					escaped.append("&amp;");
					break;
				case '<':
					escaped.append("&lt;");
					break;
				case '>':
					escaped.append("&gt;");
					break;
				case '"':
					escaped.append("&quot;");
					break;
				case '\'':
					escaped.append("&apos;");
					break;
				default:
					escaped.append(value.charAt(i));
			}
		}
		return escaped.toString();
	}

	/**
	 * 根据项目属性及已解析的组件注解生成应用基础清单，供后续类库清单合并使用。
	 *
	 * @param compiler 当前编译器及诊断输出。
	 * @param project 当前项目。
	 * @param buildDirectory 清单输出目录。
	 * @param permissionsNeeded 当前已知的运行权限。
	 * @return 生成成功且未发现无效注解节点时返回 true。
	 */
	private static boolean writeAndroidManifest(Compiler compiler, Project project, File buildDirectory, ManifestPermissions permissionsNeeded) {
		// 从项目属性中取主窗口包名
		String mainForm = project.getMainForm();
		String appLabel = escapeXmlAttribute(project.getProjectName());
		File manifest = new File(buildDirectory, "AndroidManifest.xml");
		int errorCountBeforeManifestNodes = compiler.errorCount;
		ManifestNodeGroups manifestNodes = compiler.generateManifestNodes(project);
		if (compiler.errorCount > errorCountBeforeManifestNodes) {
			return false;
		}

		try {
			// 创建UTF8输出流
			OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(manifest), "UTF-8");
			out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");

			// 清单头
			out.write("<manifest " +
					"xmlns:android=\"http://schemas.android.com/apk/res/android\" " +
					// 应用包名
					"package=\"" + Signatures.getPackageName(mainForm) + "\" " +
					// 版本号，注意必须为整数
					"android:versionCode=\"" + project.getVersionCode() + "\" " +
					// 版本名
					"android:versionName=\"" + project.getVersionName() + "\">\n");

			// 应用对不同版本平台兼容性
			out.write("    <uses-sdk " +
					// 指定应用运行所需最低API级别
					"android:minSdkVersion=\"" + Toolchain.MIN_API_LEVEL + "\" " +
					// 指定应用的目标API级别，该属性添加于API4
					// 根据《移动应用软件高API等级预置与分发自律公约》自2019年5月1日起，
					// 新上架应用应基于Android 8.0（API等级26）及以上开发。
					"android:targetSdkVersion=\"" + Toolchain.API_LEVEL + "\" />\n");

			// 应用对屏幕大小支持机制，该节点添加于API4
			out.write("    <supports-screens " +
					// 是否支持小屏
					"android:smallScreens=\"true\" " +
					// 是否支持中屏
					"android:normalScreens=\"true\" " +
					// 是否支持大屏
					"android:largeScreens=\"true\" " +
					// 是否支持超大屏，该属性添加于API9
					"android:xlargeScreens=\"true\" " +
					// 是否支持多种不同密度
					"android:anyDensity=\"true\" />\n");

			// 使用权限
			out.write("    <!-- Uses Permission Start -->\n");
			for (Map.Entry<String, Integer> permission : permissionsNeeded.getPermissions().entrySet()) {
				out.write("    <uses-permission android:name=\"" + escapeXmlAttribute(permission.getKey()) + "\"");
				if (permission.getValue() > 0) {
					out.write(" android:maxSdkVersion=\"" + permission.getValue() + "\"");
				}
				out.write(" />\n");
			}
			out.write("    <!-- Uses Permission End -->\n");
			writeManifestNodes(out, manifestNodes.rootNodes, "    ", "Root");

			// 应用
			out.write("    <application " +
					// 应用包全名
					"android:name=\"" + Compiler.RUNTIME_ROOT_PACKAGE + ".android.MainApplication\" " +
					// 应用图标
					"android:icon=\"" + project.getIcon() + "\" " +
					// 应用名
					"android:label=\"" + appLabel + "\" " +
					// 目标 API 28 及以上默认禁止明文流量；显式允许 HTTP、ws:// 等既有网络功能。
					"android:usesCleartextTraffic=\"true\" " +
					// 硬件加速，该属性添加于API11
					"android:hardwareAccelerated=\"true\">\n");

			// 活动
			out.write("        <activity " +
					// 活动包全名
					"android:name=\"simple.runtime.android.MainActivity\" " +
					// 活动图标，引用res中的资源图标
					"android:icon=\"" + project.getIcon() + "\" " +
					// 活动名称
					"android:label=\"" + appLabel + "\" " +
					// 活动方向
					"android:screenOrientation=\"" + project.getOrientation() + "\" " +
					// 活动主题，缺省不设置该值使用系统默认主题
					(project.getTheme().isEmpty() ? "" : "android:theme=\"" + project.getTheme() + "\" ") +
					// 配置改变重启应用过滤器（如果不定义以下值，应用可能重启）：
					// keyboard - 键盘模式发生变化，例如：用户接入外部键盘输入。
					// keyboardHidden - 用户打开手机硬件键盘
					// navigation - 导航类型
					// orientation - 屏幕方向
					// screenSize - 屏幕尺寸，该属性添加于API13
					// smallestScreenSize - 物理屏幕尺寸，该属性值添加于API13
					"android:configChanges=\"keyboard|keyboardHidden|navigation|orientation|screenSize|smallestScreenSize\" " +
					// 活动与软键盘的交互模式，adjustPan：不改变窗口尺寸，而是自动平移窗口内容
					"android:windowSoftInputMode=\"adjustPan\" " +
					// 启动模式，singleTask：仅创建一个实例，且允许与其它活动组成部分
					"android:launchMode=\"singleTask\">\n");

			// 元数据 应用主窗口限制类名
			out.write("            <meta-data " +
					// 固定键名(运行库根包名.android.MainForm)
					"android:name=\"simple.runtime.android.MainForm\" " +
					// 主活动（运行库根包名.android.MainForm）启动后会读取该值，并设置该值（类名）为主窗口
					"android:value=\"" + mainForm + "\" />\n");
			writeManifestNodes(out, manifestNodes.activityNodes, "            ", "Activity");

			// 意图过滤器，指定该应用将接受那些意图广播
			out.write("            <intent-filter>\n");
			out.write("                <action android:name=\"android.intent.action.MAIN\" />\n");
			out.write("                <category android:name=\"android.intent.category.LAUNCHER\" />\n");
			out.write("                <category android:name=\"android.intent.category.DEFAULT\" />\n");
			writeManifestNodes(out, manifestNodes.intentFilterNodes, "                ", "Intent Filter");
			out.write("            </intent-filter>\n");
			out.write("        </activity>\n");

			// 服务
			out.write("        <!-- Service Start -->\n");
			for (String serviceName : compiler.simpleNeedingServices) {
				out.write("        <service android:name=\"" + serviceName + "\">\n");
				out.write("            <intent-filter>\n");
				out.write("                <action android:name=\"" + serviceName + "\" />\n");
				out.write("            </intent-filter>\n");
				out.write("        </service>\n");
			}
			out.append("        <!-- Service End -->\n");

			// max_aspect 属性表示 App 能够支持的最大屏幕比例，官方建议我们将该值设置为 2.1 或者更高的值。
			out.write("        <meta-data android:name=\"android.max_aspect\" android:value=\"2.1\" />\n");
			writeManifestNodes(out, manifestNodes.applicationNodes, "        ", "Application");
			out.write("    </application>\n");
			out.write("</manifest>\n");
			out.close();
			return true;
		} catch (IOException e) {
			compiler.error(Scanner.NO_POSITION, Error.errWriteError, manifest.toString());
			return false;
		}
	}

	/**
	 * 在资源链接前发现类库引用，避免应用主题和布局只能引用项目自身资源。
	 *
	 * <p>发现阶段使用独立语义树，允许尚未建立的资源名称产生诊断；其树与折叠结果
	 * 均不参与代码生成。两轮只共用本次构建读取的源码文本，语法树及语义状态互不复用。
	 *
	 * @param platform 目标平台。
	 * @param sourceTexts 本轮按项目单元顺序读取的只读源码快照。
	 * @param dependencyDirectory 本次依赖展开目录。
	 * @return 引用的扩展库目录。
	 */
	private static List<File> discoverLibraryDirectories(Platform platform,
			Map<Project.SourceDescriptor, String> sourceTexts, File dependencyDirectory) {
		try (PrintStream sink = new PrintStream(java.io.OutputStream.nullOutputStream())) {
			Compiler discovery = new Compiler(platform, sink, sink, dependencyDirectory);
			for (Map.Entry<Project.SourceDescriptor, String> entry : sourceTexts.entrySet()) {
				Project.SourceDescriptor source = entry.getKey();
				String fileName = source.getQualifiedName().replace('.', '/') + Project.SOURCEFILE_EXTENSION;
				new Parser(discovery, new Scanner(discovery, discovery.createFileIndex(fileName),
						entry.getValue()), source.getQualifiedName()).parse();
			}
			discovery.resolve();
			return discovery.getSimpleNeedingLibraryDirectories();
		}
	}

	/**
	 * 将指定对象以明确的名称公开到本次编译的全局符号表。
	 *
	 * <p>对象的实际限定名和最终类文件位置保持不变，只增加编译期名称入口，不会额外生成
	 * 或复制类文件。后续其它编译器提供的对象也可以复用此方法。
	 *
	 * @param compiler 当前编译器实例
	 * @param globalName 要提供给所有Simple单元使用的全局名称
	 * @param qualifiedName 要公开对象的完整限定名
	 * @return 登记成功返回{@code true}；全局名称已被占用时报告错误并返回{@code false}
	 */
	private static boolean exposeGlobalObject(Compiler compiler, String globalName, String qualifiedName) {
		ObjectSymbol objectSymbol = ObjectSymbol.getObjectSymbol(compiler, qualifiedName.replace('.', '/'));
		if (compiler.globalNamespaceSymbol.getScope().lookupShallow(globalName) != null) {
			compiler.error(Scanner.NO_POSITION, Error.errSymbolRedefinition, globalName);
			return false;
		}

		compiler.globalNamespaceSymbol.getScope().enterSymbol(new AliasSymbol(Scanner.NO_POSITION, globalName, objectSymbol));
		return true;
	}

	/**
	 * 每次构建从空类目录生成R类和Simple类，避免已改名或删除的旧类进入dex。
	 * 保留build/classes目录，只清理其中内容，并拒绝清理指向其它位置的目录链接。
	 */
	private static boolean prepareClassOutputDirectory(Compiler compiler, File buildDir) {
		Path buildPath = buildDir.toPath().toAbsolutePath().normalize();
		Path classesPath = buildPath.resolve("classes");
		try {
			if (java.nio.file.Files.exists(classesPath, LinkOption.NOFOLLOW_LINKS)) {
				if (!java.nio.file.Files.isDirectory(classesPath, LinkOption.NOFOLLOW_LINKS)
						|| java.nio.file.Files.isSymbolicLink(classesPath)) {
					throw new IOException("类输出目录不是普通的构建子目录");
				}
				java.nio.file.Files.walkFileTree(classesPath, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException {
						if (java.nio.file.Files.isSymbolicLink(directory)
								|| !directory.toAbsolutePath().normalize().startsWith(classesPath)) {
							throw new IOException("类输出目录包含指向其它位置的目录链接");
						}
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
						java.nio.file.Files.delete(file);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path directory, IOException failure) throws IOException {
						if (failure != null) {
							throw failure;
						}
						if (!directory.equals(classesPath)) {
							java.nio.file.Files.delete(directory);
						}
						return FileVisitResult.CONTINUE;
					}
				});
			} else {
				java.nio.file.Files.createDirectory(classesPath);
			}
			return true;
		} catch (IOException e) {
			compiler.error(Scanner.NO_POSITION, "无法准备类输出目录：" + classesPath + "（" + e.getMessage() + "）");
			return false;
		}
	}

	/**
	 * 内部错误，调用前应指示意外情况。
	 */
	public static void internalError() {
		throw new IllegalStateException("内部错误");
	}

	/**
	 * 构建一个Simple的项目。
	 *
	 * @param platform  要构建的目标平台
	 * @param project  要生成的项目
	 * @param out  要重定向到的标准输出流
	 * @param err  要重定向到的标准错误流
	 * @return  如果编译成功为{@code true}，否则为{@code false}
	 */
	public static boolean compile(Platform platform, Project project, PrintStream out, PrintStream err) {
		// 创建本次构建独立使用的编译器实例，并记录总耗时起点。
		long start = System.currentTimeMillis();
		final Compiler compiler = new Compiler(platform, out, err, new File(project.getBuildDirectory(), "dependencies"));
		if (compiler.errorCount > 0) {
			return false;
		}

		// 每个单元只读取一次；本轮发现与正式编译共用文本，各自建立语法树和符号状态。
		Map<Project.SourceDescriptor, String> sourceTexts = new LinkedHashMap<Project.SourceDescriptor, String>();
		for (Project.SourceDescriptor arg : project.getSources()) {
			// 编译器内部统一使用“限定名对应的相对路径”标识源码。
			String fileName = arg.getQualifiedName().replace('.', '/') + Project.SOURCEFILE_EXTENSION;
			out.println("正在编译单元：" + fileName);

			try {
				String sourceText = Files.read(arg.getFile(), compiler.encoding);
				sourceTexts.put(arg, sourceText);
				new Parser(compiler, new Scanner(compiler, compiler.createFileIndex(fileName),
						sourceText), arg.getQualifiedName()).parse();
			} catch (IOException ioe) {
				compiler.error(Scanner.NO_POSITION, Error.errReadError, arg.getFile().toString());
			}
		}

		// 源码读取或语法解析失败时，不再进入平台构建阶段。
		if (compiler.errorCount > 0) {
			compiler.out.println("编译发生错误计数：" + compiler.errorCount);
			return false;
		}

		switch (compiler.platform) {
		default:
			// compile当前只实现Android项目构建，其它平台属于调用错误。
			Compiler.internalError();
			return false; // 永远不会到这里...

		case Android:
			// 准备本次Android构建使用的根目录；类输出目录在生成类之前重建。
			File buildDir = Files.createDirectory(project.getBuildDirectory()).getAbsoluteFile().toPath().normalize().toFile();
			File classesDir = new File(buildDir, "classes");

			// 校验项目图标配置；Android图标必须使用@drawable/图标名格式。
			String iconPath = project.getIcon();
			Matcher iconMatcher = Pattern.compile("@drawable/(\\S+)").matcher(iconPath);
			if (!iconMatcher.find()) {
				compiler.error(Scanner.NO_POSITION, "属性配置项的应用图标 %1 无效", iconPath);
				return false;
			}

			// 项目尚未提供配置的图标文件时，写入编译器内置的缺省图标。
			File resDir = Files.createDirectory(project.getResDirectory()).getAbsoluteFile().toPath().normalize().toFile();
			File drawableDir = Files.createDirectory(resDir, "drawable");
			File iconFile = new File(drawableDir, iconMatcher.group(1) + ".png");
			if (!iconFile.exists()) {
				compiler.out.println("正在生成缺省图标：" + iconFile.getName());
				URL iconURL = Compiler.class.getResource("/simple/compiler/resources/simple.png");
				assert iconURL != null;
				try {
					BufferedImage icon = ImageIO.read(iconURL);
					ImageIO.write(icon, "png", iconFile);
				} catch (IOException ioe) {
					compiler.error(Scanner.NO_POSITION, Error.errWriteError, iconFile.toString());
					return false;
				}
			}

			// 集中定义后续资源链接、类生成和APK组装所需的输入输出位置。
			File manifestDirectory = Files.createDirectory(buildDir, "manifests");
			File manifestFile = new File(manifestDirectory, "AndroidManifest.xml");
			String applicationPackageName = Signatures.getPackageName(project.getMainForm());
			String androidResourceQualifiedName = applicationPackageName + ".R";
			String simpleResourceQualifiedName = applicationPackageName + "." + SIMPLE_RESOURCE_OBJECT_NAME;
			File resourceBuildDirectory = Files.createDirectory(buildDir, "res");
			File compiledResourcesFile = new File(resourceBuildDirectory, "resources.zip");
			File resourceSymbolsFile = new File(resourceBuildDirectory, "R.txt");
			File stableResourceIdsFile = new File(resourceBuildDirectory, "resource-ids.txt");
			File androidResourceSourceFile = new File(resourceBuildDirectory, androidResourceQualifiedName.replace('.', '/') + ".java");
			File assetsDir = Files.createDirectory(project.getAssetsDirectory()).getAbsoluteFile().toPath().normalize().toFile();
			File apFile = new File(resourceBuildDirectory, "resources.ap_");
			File apkDirectory = Files.createDirectory(buildDir, "apks");
			File apkFile = new File(apkDirectory, "signed.apk");
			File unsignedApkFile = new File(apkDirectory, "unsigned.apk");
			File alignedApkFile = new File(apkDirectory, "aligned.apk");

			// 先写入基础清单。此时Simple符号尚未解析，暂时还不能收集组件声明的权限和服务。
			out.println("正在生成清单：" + manifestFile.getName());
			if (!writeAndroidManifest(compiler, project, manifestDirectory, new ManifestPermissions())) {
				return false;
			}

			// 资源链接前先解析一份独立语义树，发现项目引用的扩展库。
			// 每个被引用的类库以自身classes.jar为整体打包，并携带其可选目录。
			List<File> libraryDirectories;
			List<File> compiledLibraryResourceFiles = new ArrayList<File>();
			List<File> assetsDirectories = new ArrayList<File>();
			List<File> dexInputFiles = new ArrayList<File>();
			List<File> dependencyFiles;
			List<LibraryArtifacts.AndroidLibrary> androidLibraries;
			try {
				libraryDirectories = discoverLibraryDirectories(platform, Collections.unmodifiableMap(sourceTexts),
						new File(buildDir, "dependencies"));
				dependencyFiles = compiler.runtimeLoader.getDependenciesForLibraries(libraryDirectories);
				androidLibraries = compiler.runtimeLoader.getAndroidLibrariesForLibraries(libraryDirectories);
			} catch (IOException failure) {
				compiler.error(Scanner.NO_POSITION, failure.getMessage());
				return false;
			}
			dexInputFiles.addAll(dependencyFiles);

			// Java 资源与 DEX 共用已选依赖；后续只追加项目实际使用的扩展库实现。
			List<File> resourceJars = new ArrayList<File>(dependencyFiles);
			List<File> nativeDirectories = new ArrayList<File>();
			File libraryResourceBuildDirectory = Files.createDirectory(resourceBuildDirectory, "libraries");

			for (int libraryIndex = 0; libraryIndex < libraryDirectories.size(); libraryIndex++) {
				File libraryDirectory = libraryDirectories.get(libraryIndex);
				File classesFile = new File(libraryDirectory, "classes.jar").getAbsoluteFile();
				if (!classesFile.isFile()) {
					compiler.error(Scanner.NO_POSITION, "扩展库缺少classes.jar：%1", libraryDirectory.toString());
					return false;
				}
				dexInputFiles.add(classesFile);
				resourceJars.add(classesFile);
			}

			// AAR 与扩展库本体共用 Android 输入收集流程，代码归档不混入这些目录。
			for (int libraryIndex = 0; libraryIndex < androidLibraries.size(); libraryIndex++) {
				File libraryDirectory = androidLibraries.get(libraryIndex).directory;
				File resourceDirectory = new File(libraryDirectory, "res");
				if (hasFiles(resourceDirectory)) {
					File compiledLibraryResources = new File(libraryResourceBuildDirectory, "library-" + libraryIndex + ".zip");
					out.println("正在编译类库资源：" + androidLibraries.get(libraryIndex).source.getName());
					if (!Toolchain.compileResources(buildDir, resourceDirectory, compiledLibraryResources, out, err)) {
						return false;
					}
					compiledLibraryResourceFiles.add(compiledLibraryResources);
				}
				File assets = new File(libraryDirectory, "assets");
				if (hasFiles(assets)) {
					assetsDirectories.add(assets);
				}
				File natives = new File(libraryDirectory, "jni");
				if (hasFiles(natives)) {
					nativeDirectories.add(natives);
				}
			}

			// 项目资产最后加入；项目资源在链接时单独作为覆盖输入。
			assetsDirectories.add(assetsDir);

			// 清单与资产副本只保留至 APK 组装完成，后续签名和发布使用已组装的产物。
			try (AndroidLibraries androidInputs = new AndroidLibraries(manifestDirectory)) {
				assetsDirectories = androidInputs.mergeAssets(assetsDirectories);
				androidInputs.mergeManifest(manifestFile, androidLibraries, project.getManifestPlaceholders(),
						applicationPackageName, out, err);

				// AAPT2第一阶段：把项目res目录编译成可重复参与链接的resources.zip。
				out.println("正在编译资源：" + compiledResourcesFile.getName());
				if (!Toolchain.compileResources(buildDir, resDir, compiledResourcesFile, System.out, System.err)) {
					internalError();
					return false;
				}

				// AAPT2首次链接：一次生成R.txt、标准R.java和稳定资源编号。
				// Simple 常量使用本次编号；最终链接刷新完整符号时复用同一组稳定编号。
				out.println("正在生成资源索引：" + resourceSymbolsFile.getName());
				if (!Toolchain.generateResourceIndex(buildDir, apFile, manifestFile,
						ANDROID_RUNTIME, assetsDirectories, resourceSymbolsFile, resourceBuildDirectory,
						stableResourceIdsFile, compiledLibraryResourceFiles, compiledResourcesFile, out, err)) {
					internalError();
					return false;
				}

				// 将R.txt转换为内存SimpleResources.simple，并以全局名称R提供给所有Simple单元。
				String resourceIndexSource;
				try {
					resourceIndexSource = Resource.toSimpleSource(resourceSymbolsFile);
					String resourceIndexFileName = simpleResourceQualifiedName.replace('.', '/') + Project.SOURCEFILE_EXTENSION;
					new Parser(compiler, new Scanner(compiler,
							compiler.createFileIndex(resourceIndexFileName), resourceIndexSource),
							simpleResourceQualifiedName).parse();
					// 实际对象名与全局名称分离，避免和Android标准R类或用户的Res单元冲突。
					if (!exposeGlobalObject(compiler, SIMPLE_RESOURCE_GLOBAL_NAME, simpleResourceQualifiedName)) {
						return false;
					}
				} catch (IOException ioe) {
					compiler.error(Scanner.NO_POSITION, ioe.getMessage());
					return false;
				}

				// 统一解析用户源码和SimpleResources.simple之间的类型、成员及引用关系。
				compiler.resolve();
				if (compiler.errorCount > 0) {
					compiler.out.println("编译发生错误计数：" + compiler.errorCount);
					return false;
				}

				if (!libraryDirectories.containsAll(compiler.getSimpleNeedingLibraryDirectories())) {
					compiler.error(Scanner.NO_POSITION, "资源准备与正式语义解析得到的扩展库引用不一致。");
					return false;
				}

				// 根据解析结果补齐权限和服务，再使用第一次保存的稳定资源编号生成最终资源包。
				if (!writeAndroidManifest(compiler, project, manifestDirectory, compiler.generatePermissions())) {
					return false;
				}
				try {
					androidInputs.mergeManifest(manifestFile, androidLibraries, project.getManifestPlaceholders(),
							applicationPackageName, out, err);
				} catch (IOException failure) {
					compiler.error(Scanner.NO_POSITION, failure.getMessage());
					return false;
				}
				out.println("正在打包资源：" + apFile.getName());
				if (!Toolchain.packageResources(buildDir, apFile, manifestFile,
						ANDROID_RUNTIME, assetsDirectories, stableResourceIdsFile,
						compiledLibraryResourceFiles, compiledResourcesFile, System.out, System.err)) {
					internalError();
					return false;
				}

				if (!prepareClassOutputDirectory(compiler, buildDir)) {
					return false;
				}

				// 编译AAPT2原生的分层R.java，供Android代码及第三方依赖访问R.class和R$*.class。
				out.println("正在编译Android资源索引：" + androidResourceQualifiedName);
				List<File> resourceSources;
				try {
					resourceSources = AndroidLibraries.generateResourceSources(androidLibraries,
							resourceSymbolsFile, applicationPackageName, resourceBuildDirectory, dexInputFiles);
					resourceSources.add(androidResourceSourceFile);
				} catch (IOException failure) {
					compiler.error(Scanner.NO_POSITION, failure.getMessage());
					return false;
				}
				if (!Toolchain.compileJava(buildDir, classesDir, resourceSources, out, err)) {
					internalError();
					return false;
				}

				// 生成全部Simple类；内存中的SimpleResources.simple会在这里生成平铺的SimpleResources.class。
				out.println("正在编译Simple资源索引：" + simpleResourceQualifiedName);
				compiler.generate(classesDir);

				// 统一转换项目类、资源索引及运行库；DEX 输出目录由本次构建独立管理。
				File dexDirectory = new File(buildDir, "dexs");
				dexInputFiles.add(0, new File(SIMPLE_ANDROID_RUNTIME).getAbsoluteFile());
				dexInputFiles.add(0, classesDir.getAbsoluteFile());
				try {
					Dependencies.validateClasses(dexInputFiles);
				} catch (IOException failure) {
					compiler.error(Scanner.NO_POSITION, failure.getMessage());
					return false;
				}
				List<File> dexFiles = Toolchain.generateDex(dexDirectory, dexInputFiles, new File(ANDROID_RUNTIME),
						Toolchain.MIN_API_LEVEL, System.out, System.err);
				if (dexFiles == null) {
					internalError();
					return false;
				}

				// 只打包本次 D8 返回的全部 DEX，不扫描构建目录中的历史文件。
				compiler.out.println("正在生成应用：" + unsignedApkFile.getName());
				if (!Toolchain.buildApplication(unsignedApkFile, apFile, dexFiles,
						resourceJars, nativeDirectories, System.err)) {
					internalError();
					return false;
				}
			} catch (IOException failure) {
				compiler.error(Scanner.NO_POSITION, failure.getMessage());
				return false;
			}

			// V2 签名保护 APK 的整体结构，所有对齐操作必须先于签名完成。
			out.println("正在对齐应用：" + alignedApkFile.getName());
			if (!Toolchain.alignApplication(unsignedApkFile, alignedApkFile, System.err)) {
				internalError();
				return false;
			}

			// 项目同时配置密钥库和KEY_PASSWORD时执行发布签名，否则使用自动生成的调试密钥。
			String signingKeystoreLocation;
			String signingPasswordSource;
			String signingAlias;
			String signingStoreType;
			String keyLocation = project.getKeystoreLocation();
			if (keyLocation != null && !keyLocation.isEmpty() && KEY_PASSWORD != null && !KEY_PASSWORD.isEmpty()) {
				// 继续使用项目原有的密钥和证书，从继承环境中读取发布口令。
				out.println("正在发布签名：" + apkFile.getName() + " -" + project.getKeystoreAlias());
				signingKeystoreLocation = keyLocation;
				signingPasswordSource = "env:KEY_PASSWORD";
				signingAlias = project.getKeystoreAlias();
				signingStoreType = null;
			} else {
				File keystore = new File(buildDir, ANDROID_DEBUG_KEYSTORE);
				if (!keystore.exists()) {
					// 调用 keytool.exe 生成调试密钥。
					out.println("正在生成调试密钥：" + ANDROID_DEBUG_KEYSTORE);
					if (!Toolchain.createDebugKeystore(keystore, System.err)) {
						internalError();
						return false;
					}
				}

				// 沿用已有调试密钥，不因更换签名工具而生成新证书。
				out.println("正在调试签名：" + apkFile.getName() + " -AndroidDebugKey");
				signingKeystoreLocation = keystore.getAbsolutePath();
				signingPasswordSource = "pass:android";
				signingAlias = "AndroidDebugKey";
				signingStoreType = "PKCS12";
			}

			// 签名直接写入最终应用路径；工具失败时立即中断，不再复制或单独校验。
			if (!Toolchain.signApplication(signingKeystoreLocation, signingPasswordSource,
					alignedApkFile, apkFile, signingAlias, signingStoreType, System.err)) {
				internalError();
				return false;
			}
			break;
		}

		// 所有平台构建步骤均成功后，输出总耗时并返回最终错误状态。
		out.println("构建完成，用时 " + ((System.currentTimeMillis() - start) / 1000.0) + " 秒");
		return compiler.errorCount == 0;
	}

	/**
	 * 为单元测试执行构建一个Simple项目。
	 *
	 * @param  project  要构建的项目
	 * @return  如果有任何错误，则返回{@code null}
	 */
	public static List<ClassFile> compileForUnitTesting(Project project) {
		// Create a new compiler instance for the compilation
		long start = System.currentTimeMillis();
		final Compiler compiler = new Compiler(Platform.Android, System.out, System.err);

		// Parse all source files
		for (Project.SourceDescriptor arg : project.getSources()) {
			final File file = arg.getFile();
			System.out.println("________编译 " + file.getPath());

			try {
				new Parser(compiler, new Scanner(compiler, compiler.createFileIndex(file.getAbsolutePath()),
						Files.read(file, compiler.encoding)), arg.getQualifiedName()).parse();
			} catch (IOException ioe) {
				compiler.error(Scanner.NO_POSITION, Error.errReadError, arg.getFile().toString());
			}
		}

		// Resolve symbol information in parse trees
		compiler.resolve();

		// Create and package class files if there were no compilation errors
		List<ClassFile> classes = null;
		if (compiler.errorCount > 0) {
			compiler.out.println("Compilation errors: " + compiler.errorCount);
		} else {
			// Create class files
			classes = new ArrayList<ClassFile>();
			compiler.generate(classes);
		}

		System.out.println("Build finished in " +
				((System.currentTimeMillis() - start) / 1000.0) + " seconds");

		return classes;
	}

	/**
	 * 创建一个新的Simple编译器。
	 *
	 * @param platform  platform to compile for
	 * @param out  stdout stream for compiler messages
	 * @param err  stderr stream for compiler messages
	 */
	public Compiler(Platform platform, PrintStream out, PrintStream err) {
		this(platform, out, err, null);
	}

	/**
	 * 创建编译器并指定本次依赖展开位置，不读取开发目录的 IDE 产物。
	 *
	 * @param platform 目标平台。
	 * @param out 普通诊断输出。
	 * @param err 错误输出。
	 * @param dependencyDirectory 项目构建目录中的依赖展开目录。
	 */
	private Compiler(Platform platform, PrintStream out, PrintStream err, File dependencyDirectory) {
		this.out = out;
		this.err = err;
		// xhwsd@qq.com 2021-5-22 指定源文件（.simple）字符编码
		encoding = "UTF-8";
		objects = new ArrayList<ObjectSymbol>();
		classfiles = new ArrayList<String>();
		globalNamespaceSymbol = new NamespaceSymbol();
		symbolToClassNeedingPermissionsMap = new HashMap<ObjectSymbol, Class<?>>();
		classesNeedingPermissions = new HashSet<Class<?>>();
		symbolToClassNeedingManifestNodesMap = new HashMap<ObjectSymbol, Class<?>>();
		classesNeedingManifestNodes = new LinkedHashSet<Class<?>>();

		// Simple项目使用服务（包名）集合
		simpleNeedingServices = new HashSet<String>();
		// Simple项目实际引用的扩展库对象集合
		simpleNeedingLibraryObjects = new HashSet<ObjectSymbol>();

		// 加载Simple的运行时库符号信息
		this.platform = platform;
		if (platform != Platform.None) {
			runtimeLoader = new RuntimeLoader(this, ANDROID_RUNTIME,
					platform.getRuntimeLibrary(), LIBRARIES_HOME, dependencyDirectory);
			runtimeLoader.loadSimpleObjects();
		}

		filenameMap = new ArrayList<String>();

		runtimeErrorType = (ObjectType) ObjectSymbol.getObjectSymbol(this,
					RUNTIME_ERROR_INTERNAL_NAME).getType();
	}

	/**
	 * Returns the path name of the file associated with the file index
	 *
	 * @param fileIndex  index of file to lookup
	 * @return  file name of file being looked up
	 */
	public String fileIndexToPath(int fileIndex) {
		return fileIndex == 0 ? "" : filenameMap.get(fileIndex - 1);
	}

	/**
	 * Returns a new file index for a file.
	 *
	 * @param path  file path
	 * @return  file index
	 */
	public int createFileIndex(String path) {
		filenameMap.add(path);
		return filenameMap.size();
	}

	/**
	 * Adds a new Simple object to the list compiled objects.
	 *
	 * @param objectSymbol  object being compiled
	 */
	public void addObject(ObjectSymbol objectSymbol) {
		objects.add(objectSymbol);
	}

	/**
	 * Adds the given class to the list of generated classes.
	 *
	 * @param internalName  generated class
	 */
	public void addClassfile(String internalName) {
		classfiles.add(internalName + ".class");
	}

	/**
	 * Returns the internal name of the implementation for the given component
	 * object type.
	 *
	 * @param type  component object type
	 * @return  internal name of component object
	 */
	public String getComponentImplementationInternalName(ObjectType type) {
		return runtimeLoader.getComponentImplementationInternalName(type);
	}

	/**
	 * 报告编译错误消息。
	 *
	 * @param position  源代码位置
	 * @param error  error message template (see {@link Error})
	 * @param params  parameters for error message template
	 */
	public void error(long position, String error, String... params) {
		errorCount++;
		new Error(position, error, params).print(this, err);
	}

	/**
	 * 报告编译警告消息。
	 *
	 * @param warning  警告信息
	 */
	public void warning(Warning warning) {
		warningCount++;
		warning.print(this, out);
	}

	/**
	 * 返回编译错误数。
	 *
	 * @return  编译错误计数
	 */
	public int getErrorCount() {
		return errorCount;
	}

	/**
	 * 返回编译警告数。
	 *
	 * @return  编译警告计数
	 */
	public int getWarningCount() {
		return warningCount;
	}

	/**
	 * 返回全局（未命名）命名空间的符号。
	 *
	 * @return  全局命名空间符号
	 */
	public NamespaceSymbol getGlobalNamespaceSymbol() {
		return globalNamespaceSymbol;
	}

	/**
	 * 返回运行时错误的基本对象类型。
	 *
	 * @return  运行时错误类型
	 */
	public ObjectType getRuntimeErrorType() {
		return runtimeErrorType;
	}

	/**
	 * 返回当前编译目标平台。
	 *
	 * @return  目标平台
	 */
	public Platform getPlatform() {
		return platform;
	}

	/**
	 * 触发编译的符号解析阶段。
	 *
	 * 注意：可视（public）仅用于测试！除非出于测试目的，否则不要从此类外部调用。
	 */
	public void resolve() {
		// 首先解决声明
		for (ObjectSymbol objectSymbol: objects) {
			objectSymbol.resolve(this, null);
		}

		// 既然已经解析了所有声明，我们也尝试解析函数体。
		// 分两步进行这样做的好处是，我们不必担心循环问题。
		for (ObjectSymbol objectSymbol: objects) {
			objectSymbol.resolveFunctionBodies(this);
		}
	}

	/*
	 * 触发类文件生成。
	 */
	private void generate(File buildDirectory) {
		for (ObjectSymbol objectSymbol: objects) {
			objectSymbol.generate(this, buildDirectory);
		}
	}

	/*
	 * 内存类文件生成中的触发器。用于单元测试。
	 */
	private void generate(List<ClassFile> classes) {
		for (ObjectSymbol objectSymbol: objects) {
			objectSymbol.generate(this, classes);
		}
	}

	/**
	 * 添加Simple项目需要服务。
	 *
	 * @param internalName 内部类名
	 */
	public void addSimpleNeedingService(String internalName) {
		// 将服务单元内部类名（simple/samples/test/MyService）转为服务类名（simple.samples.test.MyService）
		simpleNeedingServices.add(internalName.replace("/", "."));
	}
}

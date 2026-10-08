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

	// 运行时
	private static final String ANDROID_RUNTIME = ANDROID_HOME + "/platforms/android-26/android.jar";
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

	private static List<File> getJarFiles(File directory) {
		if (!directory.isDirectory()) {
			return Collections.emptyList();
		}
		File[] files = directory.listFiles();
		if (files == null) {
			return Collections.emptyList();
		}
		List<File> jars = new ArrayList<File>();
		for (File file : files) {
			if (file.isFile() && file.getName().toLowerCase().endsWith(".jar")) {
				jars.add(file.getAbsoluteFile());
			}
		}
		Collections.sort(jars, new java.util.Comparator<File>() {
			@Override
			public int compare(File left, File right) {
				int comparison = left.getName().compareToIgnoreCase(right.getName());
				return comparison != 0 ? comparison : left.getName().compareTo(right.getName());
			}
		});
		return jars;
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
		if (symbolToClassNeedingPermissionsMap.containsKey(objectSymbol)) {
			classesNeedingPermissions.add(symbolToClassNeedingPermissionsMap.get(objectSymbol));
		}
	}

	/*
	 * Generate the set of Android permissions needed by this package.
	 *
	 * Note that we do a bunch of reflection here rather than directly using the
	 * {@link UsesPermissions} class and its methods. This is because the {@link UsesPermissions}
	 * class that would be directly referenced here is actually a different class object than the one
	 * created for the component annotations. This is due to them being from different class
	 * loaders. Consequently, attempts to do things like 'getAnnotation(UsesPermissions.class)' or
	 * to cast the annotation objects to UsesPermissions all failed.
	 */
	private Set<String> generatePermissions() {
		Set<String> permissions = new HashSet<String>();
		final Class<? extends Annotation> usesPermissionAnnotationClass = runtimeLoader.getAndroidUsesPermission();
		java.lang.reflect.Method permissionNameMethod = null;
		try {
			permissionNameMethod = usesPermissionAnnotationClass.getMethod("permissionNames");
		} catch (NoSuchMethodException e) {
			LOG.log(Level.SEVERE, "Simple compiler doesn't know about UsesPermissions's permissionNames method.");
		}

		for (Class<?> classNeedingPermission : classesNeedingPermissions) {
			final Annotation usesPermissionsAnnotation = classNeedingPermission.getAnnotation(usesPermissionAnnotationClass);
			try {
				if (usesPermissionsAnnotation != null) {
					final String permissionsString =
							(String) permissionNameMethod.invoke(usesPermissionsAnnotation);
					if (permissionsString != null && permissionsString.length() > 0) {
						final String[] permissionStrings = permissionsString.split(",");
						for (String permissionString : permissionStrings) {
							permissions.add(permissionString.trim());
						}
					}
				} else {
					LOG.log(Level.SEVERE, "Class doesn't have UsesPermissions annotation: " + classNeedingPermission.getName());
				}
			} catch (InvocationTargetException e) {
				LOG.log(Level.SEVERE, "Simple compiler doesn't know how to deal with UsesPermissions annotation.");
			} catch (IllegalAccessException e) {
				LOG.log(Level.SEVERE, "Simple compiler doesn't know how to deal with UsesPermissions annotation.");
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
		return nodes;
	}

	private void addManifestNode(Set<String> nodes, String xml, Project project, String className) {
		if (xml != null && !xml.isEmpty()) {
			nodes.add(expandManifestMacros(project, className, xml));
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

	/*
	 * 创建部署构建的android应用程序所需的Androidmanifest.xml文件。
	 */
	private static boolean writeAndroidManifest(Compiler compiler, Project project, File buildDirectory, Set<String> permissionsNeeded) {
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
					// API14对应版本为Android 4.0
					"android:minSdkVersion=\"14\" " +
					// 指定应用的目标API级别，该属性添加于API4
					// 根据《移动应用软件高API等级预置与分发自律公约》自2019年5月1日起，
					// 新上架应用应基于Android 8.0（API等级26）及以上开发。
					// 目前API版本为26对应版本为Android 8.0
					// 锁定API26，开发将基于该版本做兼容处理
					"android:targetSdkVersion=\"26\" />\n");

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
			for (String permission : permissionsNeeded) {
				out.write("    <uses-permission android:name=\"" + permission + "\" />\n");
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
			out.write("                <action android:name=" +
					"\"android.intent.action.MAIN\" />\n");
			out.write("                <category android:name=" +
					"\"android.intent.category.LAUNCHER\" />\n");
			out.write("                <category android:name=" +
					"\"android.intent.category.DEFAULT\" />\n");
			writeManifestNodes(out, manifestNodes.intentFilterNodes, "                ",
					"Intent Filter");
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
			out.write("        <meta-data " +
					"android:name=\"android.max_aspect\" " +
					"android:value=\"2.1\" />\n");
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
		final Compiler compiler = new Compiler(platform, out, err);

		// 读取并解析项目中的全部Simple单元，先建立语法树和符号声明。
		for (Project.SourceDescriptor arg : project.getSources()) {
			// 编译器内部统一使用“限定名对应的相对路径”标识源码。
			String fileName = arg.getQualifiedName().replace('.', '/') + Project.SOURCEFILE_EXTENSION;
			out.println("正在编译单元：" + fileName);

			try {
				new Parser(compiler, new Scanner(compiler, compiler.createFileIndex(fileName),
						Files.read(arg.getFile(), compiler.encoding)), arg.getQualifiedName()).parse();
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
			File manifestFile = new File(buildDir, "AndroidManifest.xml");
			String applicationPackageName = Signatures.getPackageName(project.getMainForm());
			String androidResourceQualifiedName = applicationPackageName + ".R";
			String simpleResourceQualifiedName = applicationPackageName + "." + SIMPLE_RESOURCE_OBJECT_NAME;
			File resourceBuildDirectory = Files.createDirectory(buildDir, "res");
			File compiledResourcesFile = new File(resourceBuildDirectory, "resources.zip");
			File resourceSymbolsFile = new File(resourceBuildDirectory, "R.txt");
			File stableResourceIdsFile = new File(resourceBuildDirectory, "resource-ids.txt");
			File androidResourceSourceFile = new File(resourceBuildDirectory, androidResourceQualifiedName.replace('.', '/') + ".java");
			File assetsDir = Files.createDirectory(project.getAssetsDirectory()).getAbsoluteFile().toPath().normalize().toFile();
			File deployDir = Files.createDirectory(buildDir, "deploy");
			File apFile = new File(deployDir, project.getProjectName() + ".ap_");

			// 先写入基础清单。此时Simple符号尚未解析，暂时还不能收集组件声明的权限和服务。
			out.println("正在生成清单：" + manifestFile.getName());
			if (!writeAndroidManifest(compiler, project, buildDir, new HashSet<String>())) {
				return false;
			}

			// AAPT2第一阶段：把项目res目录编译成可重复参与链接的resources.zip。
			out.println("正在编译资源：" + compiledResourcesFile.getName());
			if (!Toolchain.compileResources(buildDir, resDir, compiledResourcesFile, System.out, System.err)) {
				internalError();
				return false;
			}

			// AAPT2首次链接：一次生成R.txt、标准R.java和稳定资源编号。
			// Simple索引与Android资源类共用本次编号，最终链接不再重复生成它们。
			out.println("正在生成资源索引：" + resourceSymbolsFile.getName());
			if (!Toolchain.generateResourceIndex(buildDir, apFile, manifestFile,
					ANDROID_RUNTIME, assetsDir, resourceSymbolsFile, resourceBuildDirectory,
					stableResourceIdsFile, compiledResourcesFile, System.out, System.err)) {
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

			// 解析完成后才能确定项目实际引用了哪些扩展库。
			// 每个被引用的类库以自身classes.jar为整体打包，并携带其可选目录。
			List<File> libraryDirectories = compiler.getSimpleNeedingLibraryDirectories();
			List<File> compiledLibraryResourceFiles = new ArrayList<File>();
			List<File> assetsDirectories = new ArrayList<File>();
			List<File> dexInputFiles = new ArrayList<File>();
			List<File> dependencyDirectories = new ArrayList<File>();
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

				File dependencyDirectory = new File(libraryDirectory, "libs").getAbsoluteFile();
				List<File> dependencyFiles = getJarFiles(dependencyDirectory);
				if (!dependencyFiles.isEmpty()) {
					dexInputFiles.addAll(dependencyFiles);
					dependencyDirectories.add(dependencyDirectory);
				}

				File resourceDirectory = new File(libraryDirectory, "res").getAbsoluteFile();
				if (hasFiles(resourceDirectory)) {
					File compiledLibraryResources = new File(libraryResourceBuildDirectory, "library-" + libraryIndex + ".zip");
					out.println("正在编译扩展库资源：" + libraryDirectory.getName());
					if (!Toolchain.compileResources(buildDir, resourceDirectory,
							compiledLibraryResources, System.out, System.err)) {
						internalError();
						return false;
					}
					compiledLibraryResourceFiles.add(compiledLibraryResources);
				}

				File libraryAssetsDirectory = new File(libraryDirectory, "assets").getAbsoluteFile();
				if (hasFiles(libraryAssetsDirectory)) {
					assetsDirectories.add(libraryAssetsDirectory);
				}

				File nativeDirectory = new File(libraryDirectory, "jni").getAbsoluteFile();
				if (hasFiles(nativeDirectory)) {
					nativeDirectories.add(nativeDirectory);
				}
			}
			// 项目资产最后加入；项目资源在链接时单独作为覆盖输入。
			assetsDirectories.add(assetsDir);

			// 根据解析结果补齐权限和服务，再使用第一次保存的稳定资源编号生成最终资源包。
			if (!writeAndroidManifest(compiler, project, buildDir, compiler.generatePermissions())) {
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
			if (!Toolchain.compileJava(buildDir, classesDir,
					androidResourceSourceFile, System.out, System.err)) {
				internalError();
				return false;
			}

			// 生成全部Simple类；内存中的SimpleResources.simple会在这里生成平铺的SimpleResources.class。
			out.println("正在编译Simple资源索引：" + simpleResourceQualifiedName);
			compiler.generate(classesDir);

			// 把项目类、标准R类、SimpleResources类和Simple Android运行库统一转换为classes.dex。
			File dexFile = new File(buildDir, "classes.dex");
			out.println("正在生成可执行类：" + dexFile.getName());
			dexInputFiles.add(0, new File(SIMPLE_ANDROID_RUNTIME).getAbsoluteFile());
			dexInputFiles.add(0, classesDir.getAbsoluteFile());
			if (!Toolchain.generateDex(dexFile, dexInputFiles, System.out, System.err)) {
				internalError();
				return false;
			}

			// 将最终资源包和classes.dex组装为尚未签名的APK。
			File apkFile = new File(deployDir, project.getProjectName() + ".apk");
			compiler.out.println("正在生成应用：" + apkFile.getName());
			if (!Toolchain.buildApplication(apkFile, apFile, dexFile,
					dependencyDirectories, nativeDirectories, System.err)) {
				internalError();
				return false;
			}

			// 项目同时配置密钥库和KEY_PASSWORD时执行发布签名，否则使用自动生成的调试密钥。
			String signingKeystoreLocation;
			String signingStorePassword;
			String signingAlias;
			String signingStoreType;
			String keyLocation = project.getKeystoreLocation();
			if (keyLocation != null && !keyLocation.isEmpty() && KEY_PASSWORD != null && !KEY_PASSWORD.isEmpty()) {
				// 调用 jarsigner.exe 使用发布密钥签名应用。
				out.println("正在发布签名：" + apkFile.getName() + " -" + project.getKeystoreAlias());
				signingKeystoreLocation = keyLocation;
				signingStorePassword = KEY_PASSWORD;
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

				// 调用 jarsigner.exe 使用调试密钥签名应用。
				out.println("正在调试签名：" + apkFile.getName() + " -AndroidDebugKey");
				signingKeystoreLocation = keystore.getAbsolutePath();
				signingStorePassword = "android";
				signingAlias = "AndroidDebugKey";
				signingStoreType = "PKCS12";
			}

			// 对已组装的APK执行最终签名，签名完成后才算Android构建结束。
			if (!Toolchain.signApplication(signingKeystoreLocation, signingStorePassword, apkFile, signingAlias, signingStoreType, System.err)) {
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
					platform.getRuntimeLibrary(), LIBRARIES_HOME);
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

package simple.compiler;

import junit.framework.TestCase;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.tools.ToolProvider;

/**
 * 在独立子进程中验证 Simple 源码、扩展库、原始 AAR 与 APK 的完整消费链路。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class CompilerAarBuildTest extends TestCase {

	/** 在两轮解析之间写入的故意无效文本，只用于验证本轮源码快照一致性。 */
	private static final String REWRITTEN_SOURCE = "本轮读取后故意改写的无效源码";

	/**
	 * 编译器从安装目录加载包装对象，使用 AAR 及内嵌 JAR，并为类库生成最终 R 类。
	 *
	 * @throws Exception 输入准备、编译、子进程或输出校验失败。
	 */
	public void testBuildFromInstalledAarWithResourceReference() throws Exception {
		File parent = new File("build/tests/aar");
		Files.createDirectories(parent.toPath());
		File root = Files.createTempDirectory(parent.toPath(), "完整 构建-").toFile().getCanonicalFile();
		File sources = new File(root, "java");
		File classes = new File(root, "java-classes");
		Files.createDirectories(classes.toPath());
		File r = write(sources, "fixture/aar/R.java", "package fixture.aar;\n\n"
				+ "/**\n * 仅用于编译生产者的占位资源类，不进入交付归档。\n * @author 树先生 xhwsd@qq.com\n */\n"
				+ "public final class R {\n\tpublic static final class string {\n\t\tpublic static int message = 0;\n\t}\n}\n");
		File helper = write(sources, "fixture/embedded/Helper.java", "package fixture.embedded;\n\n"
				+ "/**\n * AAR 内嵌 JAR 中的代码输入。\n * @author 树先生 xhwsd@qq.com\n */\n"
				+ "public final class Helper {\n\t/**\n\t * 原样返回传入编号。\n\t * @param value 资源编号。\n\t * @return 输入值。\n\t */\n"
				+ "\tpublic static int identity(int value) {\n\t\treturn value;\n\t}\n}\n");
		File feature = write(sources, "fixture/aar/Feature.java", "package fixture.aar;\n\n"
				+ "/**\n * 原始 AAR 的实现，必须使用消费者生成的 R 类。\n * @author 树先生 xhwsd@qq.com\n */\n"
				+ "public final class Feature {\n\t/**\n\t * 读取最终资源编号并调用内嵌 JAR。\n\t * @return 消息资源编号。\n\t */\n"
				+ "\tpublic static int value() {\n\t\treturn fixture.embedded.Helper.identity(R.string.message);\n\t}\n}\n");
		File wrapper = write(sources, "fixture/library/Dependency.java", "package fixture.library;\n\n"
				+ "/**\n * 第一份扩展库的 Simple 导出对象。\n * @author 树先生 xhwsd@qq.com\n */\n"
				+ "@simple.runtime.annotations.UsesPermissions(permissionNames=\"android.permission.ACCESS_NETWORK_STATE\")\n"
				+ "@simple.runtime.annotations.UsesPermission(\n\tpermissionName=\"fixture.permission.BOUNDED\",\n\tmaxSdkVersion=28\n)\n"
				+ "@simple.runtime.annotations.UsesPermission(permissionName=\"fixture.permission.SINGLE_DEFAULT\")\n"
				+ "@simple.runtime.annotations.ManifestNodes(applicationXml=\"<meta-data android:name='fixture.wrapper' android:value='declared'/>\")\n"
				+ "@simple.runtime.annotations.SimpleObject\npublic final class Dependency {\n"
				+ "\t/**\n\t * 访问共享 AAR 的资源。\n\t * @return 消息资源编号。\n\t */\n"
				+ "\t@simple.runtime.annotations.SimpleFunction\n\tpublic static int Value() {\n\t\treturn fixture.aar.Feature.value();\n\t}\n}\n");
		File extra = write(sources, "fixture/library/Extra.java", "package fixture.library;\n\n"
				+ "/**\n * 第二份扩展库的 Simple 导出对象，共享同一原始 AAR。\n * @author 树先生 xhwsd@qq.com\n */\n"
				+ "@simple.runtime.annotations.UsesPermission(\n\tpermissionName=\"fixture.permission.BOUNDED\",\n\tmaxSdkVersion=30\n)\n"
				+ "@simple.runtime.annotations.SimpleObject\npublic final class Extra {\n"
				+ "\t/**\n\t * 访问共享 AAR 的资源。\n\t * @return 消息资源编号。\n\t */\n"
				+ "\t@simple.runtime.annotations.SimpleFunction\n\tpublic static int Value() {\n\t\treturn fixture.aar.Feature.value();\n\t}\n}\n");
		assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, System.out, System.err,
				"-encoding", "UTF-8", "-classpath", System.getProperty("java.class.path"), "-d", classes.toString(),
				r.toString(), helper.toString(), feature.toString(), wrapper.toString(), extra.toString()));

		// 只交付实现类，不携带编译时占位 R；嵌入 JAR 也必须由消费者从 AAR 中取出。
		File installed = new File(root, "installed/library");
		writeJar(new File(installed, "classes.jar"), classes, "fixture/library/Dependency.class", "fixture/wrapper.txt");
		File code = new File(root, "aar-code.jar");
		File embedded = new File(root, "embedded.jar");
		writeJar(code, classes, "fixture/aar/Feature.class", "fixture/aar.txt");
		writeJar(embedded, classes, "fixture/embedded/Helper.class", "fixture/embedded.txt");
		File aar = new File(installed, "aars/feature.aar");
		Files.createDirectories(aar.getParentFile().toPath());
		// 编译后的完整目录不依赖 build；处理后的清单独立于作者输入，消费时只使用声明的清单。
		write(installed, "AndroidManifest.generated.xml", "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" package=\"fixture.library\">"
				+ "<uses-sdk android:minSdkVersion=\"24\"/><application><meta-data android:name=\"fixture.generated\" android:value=\"used\"/></application></manifest>");
		write(installed, "AndroidManifest.xml", "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" package=\"fixture.author\">"
				+ "<application><meta-data android:name=\"fixture.author\" android:value=\"unused\"/></application></manifest>");
		try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(aar.toPath()))) {
			entry(zip, "AndroidManifest.xml", "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" package=\"fixture.aar\"><uses-permission android:name=\"android.permission.INTERNET\"/><application android:extractNativeLibs=\"false\"/></manifest>".getBytes(StandardCharsets.UTF_8));
			entry(zip, "R.txt", "int string message 0x0\n".getBytes(StandardCharsets.UTF_8));
			entry(zip, "res/values/strings.xml", "<resources><string name=\"message\">dependency</string></resources>".getBytes(StandardCharsets.UTF_8));
			entry(zip, "assets/fixture.txt", "asset".getBytes(StandardCharsets.UTF_8));
			entry(zip, "jni/arm64-v8a/libfixture.so", new byte[] {1, 2, 3});
			entry(zip, "classes.jar", Files.readAllBytes(code.toPath()));
			entry(zip, "libs/helper.jar", Files.readAllBytes(embedded.toPath()));
		}
		// 新式交付只有本地原包和无线上依赖的原生声明，应用构建不应因此启动 Gradle。
		write(installed, "dependencies.json", "{\"schemaVersion\":2,\"namespace\":\"fixture.library\",\"java\":11,\"minSdk\":24,"
				+ "\"module\":\"dependencies.module\",\"manifest\":\"AndroidManifest.generated.xml\",\"files\":[{\"file\":\"aars/feature.aar\"}]}");
		write(installed, "dependencies.module", "{\"formatVersion\":\"1.1\",\"component\":{\"group\":\"es4a.extension\","
				+ "\"module\":\"fixture.library\",\"version\":\"1.0\"},\"variants\":[{\"name\":\"runtime\","
				+ "\"attributes\":{\"org.gradle.usage\":\"java-runtime\"}}]}");

		File second = new File(root, "installed/second");
		writeJar(new File(second, "classes.jar"), classes, "fixture/library/Extra.class", "fixture/second.txt");
		Files.createDirectories(new File(second, "aars").toPath());
		Files.copy(aar.toPath(), new File(second, "aars/feature.aar").toPath());
		// 未引用的库既缺第三方父类，又有损坏的依赖索引；两者都不能阻断本项目。
		File unused = new File(root, "installed/unused");
		Files.createDirectories(unused.toPath());
		ClassWriter writer = new ClassWriter(0);
		writer.visit(Opcodes.V11, Opcodes.ACC_PUBLIC, "unused/Feature", null, "missing/ThirdParty", null);
		writer.visitEnd();
		try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(new File(unused, "classes.jar").toPath()))) {
			entry(output, "unused/Feature.class", writer.toByteArray());
		}
		write(unused, "dependencies.json", "故意损坏的未使用依赖索引");

		File project = new File(root, "project");
		write(project, "project.properties", "main=fixture.app.Main\nname=AAR构建测试\nsource=./src\nres=./res\nassets=./assets\nbuild=./build\n");
		write(project, "res/values/strings.xml", "<resources><string name=\"from_dependency\">@string/message</string></resources>");
		write(project, "src/fixture/app/Main.simple", "事件 Main.初始化()\n\t断言真(fixture.library.Dependency.Value() = R.string_message)\n\t断言真(fixture.library.Extra.Value() = R.string_message)\n结束 事件\n\n$属性\n\t$资源 $窗口\n\t$定义 Main $为 窗口\n\t$结束 $定义\n$结束 $属性\n");
		File properties = new File(project, "project.properties");
		assertEquals(runCompiler(root, properties, new File(root, "installed"), false, null), 0);
		File build = new File(project, "build");
		File apk = new File(build, "apks/signed.apk");
		File keystore = new File(build, "android_debug.p12");
		byte[] originalKeystore = Files.readAllBytes(keystore.toPath());

		// 构建根目录只保留调试密钥，最终 APK 与各阶段 APK 统一归入 apks 目录。
		assertEquals(new HashSet<String>(Arrays.asList("android_debug.p12",
				"classes", "dependencies", "dexs", "res", "manifests", "apks")),
				new HashSet<String>(Arrays.asList(build.list())));
		for (String path : Arrays.asList("apks/unsigned.apk", "apks/aligned.apk", "apks/signed.apk",
				"dexs/classes.dex", "res/resources.ap_", "manifests/AndroidManifest.xml", "manifests/manifest-merger-report.txt")) {
			assertTrue(path, new File(build, path).isFile());
		}
		assertFalse(new File(build, "signed.apk").exists());
		String successfulOutput = Files.readString(new File(root, "compiler-0.log").toPath());
		assertTrue(successfulOutput.contains("构建完成"));
		assertTrue(successfulOutput.contains("正在生成 DEX：classes.dex"));
		assertTrue(successfulOutput.contains("正在生成应用：unsigned.apk"));
		assertTrue(successfulOutput.contains("正在对齐应用：aligned.apk"));
		assertTrue(successfulOutput.contains("正在调试签名：signed.apk -AndroidDebugKey"));
		assertFalse(successfulOutput.contains("正在生成应用：signed.apk"));
		assertFalse(successfulOutput.contains("正在对齐应用：signed.apk"));
		assertFalse(successfulOutput.contains("正在校验签名"));
		try (ZipFile zip = new ZipFile(apk)) {
			for (String path : Arrays.asList("classes.dex", "assets/fixture.txt", "lib/arm64-v8a/libfixture.so",
					"fixture/wrapper.txt", "fixture/second.txt", "fixture/aar.txt", "fixture/embedded.txt")) {
				assertNotNull(path, zip.getEntry(path));
			}
			assertEquals(ZipEntry.STORED, zip.getEntry("lib/arm64-v8a/libfixture.so").getMethod());
		}
		try (URLClassLoader loader = new URLClassLoader(new URL[] {new File(project, "build/classes").toURI().toURL()})) {
			int id = loader.loadClass("fixture.aar.R$string").getField("message").getInt(null);
			assertTrue(id != 0);
			assertEquals(id, loader.loadClass("fixture.app.R$string").getField("message").getInt(null));
		}
		String merged = Files.readString(new File(build, "manifests/AndroidManifest.xml").toPath());
		assertTrue(merged.contains("android.permission.INTERNET"));
		assertTrue(merged.contains("android.permission.ACCESS_NETWORK_STATE"));
		assertTrue(merged.contains("fixture.permission.SINGLE_DEFAULT"));
		int boundedPermission = merged.indexOf("fixture.permission.BOUNDED");
		assertTrue(boundedPermission >= 0);
		assertEquals(boundedPermission, merged.lastIndexOf("fixture.permission.BOUNDED"));
		String boundedElement = merged.substring(merged.lastIndexOf("<uses-permission", boundedPermission), merged.indexOf("/>", boundedPermission));
		assertTrue(boundedElement, boundedElement.contains("android:maxSdkVersion=\"30\""));
		assertTrue(merged.contains("fixture.wrapper"));
		assertTrue(merged.contains("fixture.generated"));
		assertFalse(merged.contains("fixture.author"));
		assertTrue(merged.contains("android:extractNativeLibs=\"false\""));

		// 初次读取后改写磁盘源码，两轮解析仍必须使用同一快照，不能漏掉原有扩展库。
		File source = new File(project, "src/fixture/app/Main.simple");
		String originalSource = Files.readString(source.toPath(), StandardCharsets.UTF_8);
		try {
			assertEquals(0, runCompiler(root, properties, new File(root, "installed"), true, null));
			assertEquals(REWRITTEN_SOURCE, Files.readString(source.toPath(), StandardCharsets.UTF_8));
		} finally {
			Files.writeString(source.toPath(), originalSource, StandardCharsets.UTF_8);
		}

		// 相同构建目录再次签名且 APK 变大时，不能被上次最终 APK 的长度截断。
		byte[] payload = new byte[32768];
		new Random(42).nextBytes(payload);
		File asset = new File(project, "assets/second-build.bin");
		Files.createDirectories(asset.getParentFile().toPath());
		Files.write(asset.toPath(), payload);
		assertEquals(0, runCompiler(root, properties, new File(root, "installed"), false, null));
		assertTrue(Arrays.equals(originalKeystore, Files.readAllBytes(keystore.toPath())));
		try (ZipFile zip = new ZipFile(apk)) {
			assertTrue(Arrays.equals(payload, zip.getInputStream(zip.getEntry("assets/second-build.bin")).readAllBytes()));
		}

		// 依赖发现阶段不能吞掉正式语义错误；签名前失败仍保留此前成功构建的 APK。
		byte[] previous = Files.readAllBytes(apk.toPath());
		write(project, "src/fixture/app/Main.simple", "事件 Main.初始化()\n\t断言真(fixture.library.Dependency.Value() = R.string_message)\n\t不存在的函数()\n结束 事件\n$属性\n\t$资源 $窗口\n\t$定义 Main $为 窗口\n\t$结束 $定义\n$结束 $属性\n");
		assertEquals(1, runCompiler(root, properties, new File(root, "installed"), false, null));
		assertTrue(Files.readString(new File(root, "compiler-1.log").toPath()).contains("不存在的函数"));
		assertTrue(Arrays.equals(previous, Files.readAllBytes(apk.toPath())));

		// 恢复有效源码，故意使用错误签名口令；必须返回失败且不能留下旧包或残缺包。
		Files.writeString(source.toPath(), originalSource, StandardCharsets.UTF_8);
		String originalProperties = Files.readString(properties.toPath(), StandardCharsets.UTF_8);
		Files.writeString(properties.toPath(), originalProperties + "key.location="
				+ keystore.getAbsolutePath().replace('\\', '/') + "\nkey.alias=AndroidDebugKey\n", StandardCharsets.UTF_8);
		assertEquals(1, runCompiler(root, properties, new File(root, "installed"), false, "incorrect-password"));
		String failedOutput = Files.readString(new File(root, "compiler-1.log").toPath());
		assertTrue(failedOutput.contains("正在发布签名"));
		assertFalse(failedOutput.contains("构建完成"));
		assertFalse(apk.exists());
		assertTrue(Arrays.equals(originalKeystore, Files.readAllBytes(keystore.toPath())));

		// 通用清单注解中的权限必须在进入 Android 清单合并前明确报错。
		Files.writeString(properties.toPath(), originalProperties, StandardCharsets.UTF_8);
		String originalWrapper = Files.readString(wrapper.toPath(), StandardCharsets.UTF_8);
		String invalidWrapper = originalWrapper.replace(
				"applicationXml=\"<meta-data android:name='fixture.wrapper' android:value='declared'/>\"",
				"rootXml=\"<uses-permission android:name='fixture.permission.INVALID'/>\"");
		assertFalse(originalWrapper.equals(invalidWrapper));
		Files.writeString(wrapper.toPath(), invalidWrapper, StandardCharsets.UTF_8);
		assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, System.out, System.err,
				"-encoding", "UTF-8", "-classpath", classes + File.pathSeparator + System.getProperty("java.class.path"),
				"-d", classes.toString(), wrapper.toString()));
		writeJar(new File(installed, "classes.jar"), classes, "fixture/library/Dependency.class", "fixture/wrapper.txt");
		assertEquals(1, runCompiler(root, properties, new File(root, "installed"), false, null));
		String invalidNodesOutput = Files.readString(new File(root, "compiler-1.log").toPath());
		assertTrue(invalidNodesOutput, invalidNodesOutput.contains("不能放入 @ManifestNodes"));
		assertFalse(invalidNodesOutput.contains("正在生成 DEX"));
		assertFalse(apk.exists());
	}

	/**
	 * 在独立进程中设置安装目录，避免 JVM 静态环境变量或类加载器影响测试结果。
	 *
	 * @param root 测试根目录。
	 * @param project 项目属性文件。
	 * @param libraries 安装后的扩展库根目录。
	 * @param rewriteSource 是否在初次读取后改写磁盘源码。
	 * @param signingPassword 发布签名口令；为 null 时清除继承的口令，使用调试签名。
	 * @return 编译器真实退出码。
	 * @throws Exception 进程无法执行或输出无法读取。
	 */
	private int runCompiler(File root, File project, File libraries, boolean rewriteSource, String signingPassword) throws Exception {
		String java = new File(System.getProperty("java.home"), "bin/java").toString();
		String entry = rewriteSource ? SourceSnapshotProbe.class.getName() : "simple.compiler.Main";
		ProcessBuilder builder = new ProcessBuilder(java, "-Dfile.encoding=UTF-8", "-classpath", System.getProperty("java.class.path"),
				entry, project.toString());
		if (rewriteSource) {
			// 正式入口只接受项目文件，隔离探针额外接收需要改写的源码路径。
			builder.command().add(new File(project.getParentFile(), "src/fixture/app/Main.simple").toString());
		}
		builder.environment().put("LIBRARIES_HOME", libraries.toString());
		if (signingPassword == null) {
			builder.environment().remove("KEY_PASSWORD");
		} else {
			builder.environment().put("KEY_PASSWORD", signingPassword);
		}
		File log = new File(root, "compiler-output.log");
		builder.redirectErrorStream(true);
		builder.redirectOutput(log);
		Process process = builder.start();
		if (!process.waitFor(60, TimeUnit.SECONDS)) {
			process.destroyForcibly();
			throw new IOException("等待编译器退出超时，请检查：" + log);
		}
		int result = process.exitValue();

		// 成功或语义失败都不能遗留本轮清单、资产临时输入，已发布 APK 另由用例核对。
		File[] generated = new File(project.getParentFile(), "build/manifests").listFiles();
		assertNotNull(generated);
		for (File child : generated) {
			assertFalse("编译退出后遗留临时输入：" + child, child.getName().startsWith("android-inputs-"));
		}

		byte[] output = Files.readAllBytes(log.toPath());
		Files.write(new File(root, "compiler-" + result + ".log").toPath(), output);
		if (result != 0) {
			System.out.println(new String(output, StandardCharsets.UTF_8));
		}
		return result;
	}

	/**
	 * 在独立测试进程中改写源文件，验证两轮解析不会混用不同时间的磁盘内容。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	public static final class SourceSnapshotProbe {

		/**
		 * 禁止实例化仅提供测试进程入口的类。
		 */
		private SourceSnapshotProbe() {
		}

		/**
		 * 在初次解析完成、扩展库发现开始前改写磁盘源码，再执行完整 APK 构建。
		 *
		 * @param arguments 项目属性文件与待改写源码的路径。
		 * @throws IOException 项目无法读取或未到达预期测试阶段。
		 */
		public static void main(String[] arguments) throws IOException {
			final File source = new File(arguments[1]);
			final boolean[] rewritten = {false};
			boolean compiled;
			try (PrintStream output = new PrintStream(System.out, true, "UTF-8") {
				/**
				 * 以构建阶段输出作为夹具触发点，不向编译器增加测试钩子或全局状态。
				 *
				 * @param message 当前编译消息。
				 * @throws IllegalStateException 测试源码无法改写。
				 */
				@Override
				public void println(String message) {
					super.println(message);
					if (!rewritten[0] && message != null && message.startsWith("正在生成清单：")) {
						try {
							Files.writeString(source.toPath(), REWRITTEN_SOURCE, StandardCharsets.UTF_8);
							rewritten[0] = true;
						} catch (IOException failure) {
							throw new IllegalStateException("测试源码无法改写：" + source, failure);
						}
					}
				}
			}) {
				compiled = Compiler.compile(Compiler.Platform.Android, new Project(new File(arguments[0])), output, System.err);
			}
			if (!rewritten[0]) {
				throw new IOException("源码快照测试未到达预期的改写阶段。");
			}
			System.exit(compiled ? 0 : 1);
		}
	}

	/**
	 * 将指定类及一个 Java 资源写入交付 JAR。
	 *
	 * @param file 输出归档。
	 * @param classes 类输出目录。
	 * @param name 类文件相对路径。
	 * @param resource Java 资源路径。
	 * @throws IOException 输入或归档无法读写。
	 */
	private void writeJar(File file, File classes, String name, String resource) throws IOException {
		Files.createDirectories(file.getParentFile().toPath());
		try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(file.toPath()))) {
			entry(output, name, Files.readAllBytes(new File(classes, name).toPath()));
			entry(output, resource, "resource".getBytes(StandardCharsets.UTF_8));
		}
	}

	/**
	 * 写入归档条目并关闭条目边界。
	 *
	 * @param zip 当前归档。
	 * @param name 相对路径。
	 * @param bytes 条目内容。
	 * @throws IOException 无法写入。
	 */
	private void entry(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
		zip.putNextEntry(new ZipEntry(name));
		zip.write(bytes);
		zip.closeEntry();
	}

	/**
	 * 在测试目录中创建 UTF-8 输入文件。
	 *
	 * @param root 输入根目录。
	 * @param name 相对文件名。
	 * @param text 文件内容。
	 * @return 实际写入路径。
	 * @throws IOException 目录或文件无法写入。
	 */
	private File write(File root, String name, String text) throws IOException {
		File file = new File(root, name);
		Files.createDirectories(file.getParentFile().toPath());
		Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
		return file;
	}
}

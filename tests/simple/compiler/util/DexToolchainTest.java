package simple.compiler.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import junit.framework.TestCase;

/**
 * 使用真实 JDK 与 D8 检查 Java 8 转换、中文路径、依赖输入和单 DEX 失败边界。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class DexToolchainTest extends TestCase {

	private Path root;
	private ByteArrayOutputStream diagnostics;

	/**
	 * 为每个 DEX 用例创建中文路径与独立诊断缓冲区。
	 *
	 * @throws Exception 生成目录创建失败。
	 */
	@Override
	protected void setUp() throws Exception {
		Path base = new File("build/tests/d8").toPath().toAbsolutePath();
		Files.createDirectories(base);
		root = Files.createTempDirectory(base, "中文 空格-");

		diagnostics = new ByteArrayOutputStream();
	}

	/**
	 * Lambda、方法引用及接口默认/静态方法转换为兼容最低 API 14 的 DEX。
	 *
	 * @throws Exception 测试源码或输出读取失败。
	 */
	public void testJava8FeaturesAndChineseDirectory() throws Exception {
		// 同时覆盖 Java 8 语法和类目录中的非程序文件。
		Path classes = compile("Sample", "package probe; interface Action {"
			+ "String get(); default String suffix() { return \"default-value\"; }"
			+ "static String prefix() { return \"static-value\"; }}"
			+ "public class Sample { static String value() { return \"method-ref\"; }"
			+ "public static String call() { Action a = () -> \"lambda-value\";"
			+ "Action b = Sample::value; return a.get() + b.get() + a.suffix() + Action.prefix(); }}");
		Files.write(classes.resolve("ignored.txt"), new byte[]{1, 2, 3});
		Path output = root.resolve("中文 输出.dex");

		boolean success = dex(output, Collections.singletonList(classes.toFile()));
		assertTrue(messages(), success);

		// 检查低版本 DEX 格式及转换后的内容，不依赖当前 JVM 执行结果。
		byte[] bytes = Files.readAllBytes(output);
		assertEquals("dex\n035\0", new String(bytes, 0, 8, StandardCharsets.US_ASCII));

		String content = new String(bytes, StandardCharsets.ISO_8859_1);
		assertTrue(content.contains("Lprobe/Sample;"));
		assertTrue(content.contains("lambda-value"));
		assertTrue(content.contains("default-value"));
		assertFalse(content.contains("Ljava/lang/invoke/LambdaMetafactory;"));
	}

	/**
	 * 扩展库 JAR 的类进入 DEX，普通资源仍留给后续 APK 组装处理。
	 *
	 * @throws Exception 测试 JAR 或输出读取失败。
	 */
	public void testJarInput() throws Exception {
		Path classes = compile("JarSample", "package probe; public class JarSample {"
			+ "public static int value() { return 7; }}");
		Path jar = jar(classes, "中文 依赖.jar");
		Path output = root.resolve("jar.dex");

		boolean success = dex(output, Collections.singletonList(jar.toFile()));
		assertTrue(messages(), success);

		String content = new String(Files.readAllBytes(output), StandardCharsets.ISO_8859_1);
		assertTrue(content.contains("Lprobe/JarSample;"));
		assertFalse(content.contains("resource-only-marker"));
	}

	/**
	 * D8 直接读取原 JAR 时只转换基础实现，不把多版本条目视为重复业务类。
	 *
	 * @throws Exception 测试类编译或归档读写失败。
	 */
	public void testVersionedJarClassesAreExcluded() throws Exception {
		Path classes = compile("Versioned", "package probe; public class Versioned {}");
		byte[] bytes = Files.readAllBytes(classes.resolve("probe/Versioned.class"));
		Path archive = root.resolve("versioned.jar");
		try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(archive))) {
			// 两份字节代表同一个类；版本条目若进入转换，将产生重复类错误。
			for (String name : new String[] {"probe/Versioned.class", "META-INF/versions/9/probe/Versioned.class"}) {
				output.putNextEntry(new JarEntry(name));
				output.write(bytes);
				output.closeEntry();
			}
		}

		assertTrue(messages(), dex(root.resolve("versioned.dex"), Collections.singletonList(archive.toFile())));
	}

	/**
	 * 损坏输入必须报告失败，不能覆盖上次成功的 DEX。
	 *
	 * @throws Exception 测试文件读写失败。
	 */
	public void testInvalidInputPreservesPreviousDex() throws Exception {
		Path invalid = root.resolve("bad.class");
		Files.write(invalid, new byte[]{0, 1, 2});

		// 预先写入旧结果，验证失败的转换不会将其覆盖。
		Path output = root.resolve("previous.dex");
		byte[] previous = new byte[]{3, 4, 5};
		Files.write(output, previous);

		assertFalse(dex(output, Collections.singletonList(invalid.toFile())));
		assertTrue(Arrays.equals(previous, Files.readAllBytes(output)));
		assertTrue(messages().contains("D8"));
	}

	/**
	 * 目录与依赖包含同名类时拒绝转换，不静默选取某个副本。
	 *
	 * @throws Exception 测试文件读写失败。
	 */
	public void testDuplicateClassesFail() throws Exception {
		Path classes = compile("Duplicate", "package probe; public class Duplicate {}");
		Path jar = jar(classes, "duplicate.jar");
		Path output = root.resolve("duplicate.dex");

		assertFalse(dex(output, Arrays.asList(classes.toFile(), jar.toFile())));
		assertFalse(Files.exists(output));
		assertTrue(messages().contains("Duplicate"));
	}

	/**
	 * 超过单 DEX 方法数上限时明确失败，不能丢弃额外 DEX 后继续打包。
	 *
	 * @throws Exception 测试源码或输出读取失败。
	 */
	public void testSingleDexLimit() throws Exception {
		Path classes = root.resolve("classes");
		List<Path> sources = new ArrayList<Path>();

		// 每个类分别满足 class 文件限制，合并后超过单 DEX 的方法数上限。
		for (String name : new String[]{"ManyA", "ManyB"}) {
			StringBuilder source = new StringBuilder("package probe; public class " + name + " {");
			for (int method = 0; method < 34000; method++) {
				source.append("public static void m").append(method).append("() {}");
			}
			sources.add(source(name, source.append('}').toString()));
		}

		javac(classes, sources);
		Path output = root.resolve("too-many.dex");

		assertFalse(dex(output, Collections.singletonList(classes.toFile())));
		assertFalse(Files.exists(output));
		assertTrue(messages().contains("D8"));
	}

	/**
	 * 运行实际工具链，将所有诊断保留到当前用例。
	 *
	 * @param output DEX 输出文件。
	 * @param inputs 目录或 JAR 输入。
	 * @return 转换是否成功。
	 * @throws Exception 诊断流创建失败。
	 */
	private boolean dex(Path output, List<File> inputs) throws Exception {
		PrintStream log = new PrintStream(diagnostics, true, "UTF-8");
		File platform = new File(System.getenv("ANDROID_HOME"), "platforms/android-30/android.jar");

		return Toolchain.generateDex(output.toFile(), inputs, platform, 14, log, log);
	}

	/**
	 * 保存并编译一个 Java 测试单元。
	 *
	 * @param name 单元类名。
	 * @param text Java 源码。
	 * @return 类目录。
	 * @throws Exception 文件创建或编译失败。
	 */
	private Path compile(String name, String text) throws Exception {
		Path classes = root.resolve("classes");
		javac(classes, Collections.singletonList(source(name, text)));
		return classes;
	}

	/**
	 * 在测试生成目录保存 Java 源码。
	 *
	 * @param name 单元类名。
	 * @param text Java 源码。
	 * @return 源文件路径。
	 * @throws Exception 文件写入失败。
	 */
	private Path source(String name, String text) throws Exception {
		Path file = root.resolve(name + ".java");
		Files.write(file, text.getBytes(StandardCharsets.UTF_8));
		return file;
	}

	/**
	 * 使用当前 JDK 编译真实的 Java 8 字节码。
	 *
	 * @param classes 输出类目录。
	 * @param sources 源文件列表。
	 * @throws Exception 输出目录创建失败。
	 */
	private void javac(Path classes, List<Path> sources) throws Exception {
		Files.createDirectories(classes);
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		assertNotNull("测试必须使用完整 JDK。", compiler);

		List<String> arguments = new ArrayList<String>(Arrays.asList(
			"-source", "1.8", "-target", "1.8", "-g", "-encoding", "UTF-8", "-d", classes.toString()));
		for (Path path : sources) {
			arguments.add(path.toString());
		}

		int exitCode = compiler.run(null, diagnostics, diagnostics, arguments.toArray(new String[0]));
		assertEquals(messages(), 0, exitCode);
	}

	/**
	 * 打包编译出的类及普通资源，模拟扩展库输入。
	 *
	 * @param classes 类目录。
	 * @param name JAR 文件名。
	 * @return JAR 路径。
	 * @throws Exception 文件枚举或写入失败。
	 */
	private Path jar(Path classes, String name) throws Exception {
		Path file = root.resolve(name);
		final List<Path> entries = new ArrayList<Path>();
		Files.walkFileTree(classes, new SimpleFileVisitor<Path>() {
			/**
			 * 收集测试类目录中的文件，供后续排序与归档。
			 *
			 * @param entry 当前遍历文件。
			 * @param attributes 当前文件属性。
			 * @return 继续遍历其它文件。
			 */
			@Override
			public FileVisitResult visitFile(Path entry, BasicFileAttributes attributes) {
				if (Files.isRegularFile(entry)) {
					entries.add(entry);
				}
				return FileVisitResult.CONTINUE;
			}
		});
		Collections.sort(entries);

		try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(file))) {
			for (Path entry : entries) {
				output.putNextEntry(new JarEntry(classes.relativize(entry).toString().replace('\\', '/')));
				Files.copy(entry, output);
				output.closeEntry();
			}

			// 资源标记用于断言普通资源没有混入 DEX。
			output.putNextEntry(new JarEntry("data.txt"));
			output.write("resource-only-marker".getBytes(StandardCharsets.US_ASCII));
			output.closeEntry();
		}

		return file;
	}

	/**
	 * 返回工具诊断文本。
	 *
	 * @return UTF-8 解码的诊断。
	 */
	private String messages() {
		return new String(diagnostics.toByteArray(), StandardCharsets.UTF_8);
	}

}

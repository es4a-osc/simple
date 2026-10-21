package simple.compiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import junit.framework.TestCase;

/**
 * 验证依赖选版、内容去重、模块识别及重复类诊断。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class DependenciesTest extends TestCase {
	private File root;

	/**
	 * 为每个用例创建独立的构建目录。
	 *
	 * @throws Exception 目录创建失败。
	 */
	@Override
	protected void setUp() throws Exception {
		File base = new File("build/tests/library-dependencies");
		Files.createDirectories(base.toPath());
		root = Files.createTempDirectory(base.toPath(), "case-").toFile().getAbsoluteFile();
	}

	/**
	 * 创建带一个条目的依赖 JAR；类条目使用真实的测试类字节码。
	 *
	 * @param path 相对测试根目录的 JAR 路径。
	 * @param entry 归档条目名。
	 * @param value 普通资源文本。
	 * @return 生成的 JAR 文件。
	 * @throws IOException 归档写入或测试类读取失败。
	 */
	private File jar(String path, String entry, String value) throws IOException {
		File file = new File(root, path);
		Files.createDirectories(file.getParentFile().toPath());
		try (JarOutputStream output = new JarOutputStream(new FileOutputStream(file))) {
			JarEntry item = new JarEntry(entry);
			item.setTime(0);
			output.putNextEntry(item);
			output.write(entry.endsWith(".class") ? fixtureClass() : value.getBytes(StandardCharsets.UTF_8));
			output.closeEntry();

			// 保持类内容有效，同时用独立资源区分需要检查冲突的不同 JAR。
			if (entry.endsWith(".class")) {
				output.putNextEntry(new JarEntry("fixture-marker.txt"));
				output.write(value.getBytes(StandardCharsets.UTF_8));
				output.closeEntry();
			}
		}
		return file;
	}

	/**
	 * 按给定的类库目录建立依赖选择结果。
	 *
	 * @param libraries 相对测试根目录的类库路径。
	 * @return 依赖选择结果。
	 * @throws IOException 依赖无法读取或版本冲突。
	 */
	private Dependencies select(String... libraries) throws IOException {
		File[] files = new File[libraries.length];
		for (int i = 0; i < libraries.length; i++) {
			files[i] = new File(root, libraries[i]);
		}
		return new Dependencies(Arrays.asList(files));
	}

	/**
	 * 数字版本按段比较，缺少尾段和长整数段均保持正确。
	 */
	public void testNumericVersionComparison() {
		assertTrue(Dependencies.compareVersion("3.10", "3.9") > 0);
		assertEquals(0, Dependencies.compareVersion("01.002", "1.2.0"));
		assertTrue(Dependencies.compareVersion("999999999999999999999.1", "99.9") > 0);
		assertTrue(Dependencies.compareVersion("1.0.1", "1") > 0);
	}

	/**
	 * 最高版本及其资源来源在加载和项目打包之间保持一致。
	 *
	 * @throws Exception 测试输入创建或校验失败。
	 */
	public void testSelectsHighestAndKeepsResourceFromSelectedJar() throws Exception {
		jar("a/libs/my-network-3.9.jar", "config.txt", "old");
		File highest = jar("b/libs/my-network-3.10.jar", "config.txt", "new");
		Dependencies dependencies = select("b", "a");
		assertEquals(Collections.singletonList(highest), dependencies.getFiles());
		assertEquals(Collections.singletonList(highest), dependencies.getFilesForLibraries(
				Collections.singletonList(new File(root, "a"))));
		try (JarFile selected = new JarFile(dependencies.getFiles().get(0))) {
			assertEquals('n', selected.getInputStream(selected.getJarEntry("config.txt")).read());
		}
		assertTrue(dependencies.getFilesForLibraries(Collections.singletonList(new File(root, "unused"))).isEmpty());
	}

	/**
	 * 非数字版本名称不参与自动选版，大小写 JAR 后缀仍可发现。
	 *
	 * @throws Exception 测试输入创建或校验失败。
	 */
	public void testOnlyDigitsAndSingleDotsFormVersion() throws Exception {
		String[] names = { "x-1.0-beta.jar", "x-1..2.jar", "x-.1.jar", "x-1..jar", "x-1e2.jar", "x.jar" };
		for (int i = 0; i < names.length; i++) {
			jar("a/libs/" + names[i], "value.txt", "value" + i);
		}
		jar("a/libs/x-1.jar", "value.txt", "one");
		File highest = jar("a/libs/x-2.JAR", "value.txt", "two");
		List<File> files = select("a").getFiles();
		assertEquals(names.length + 1, files.size());
		assertTrue(files.contains(highest));
		for (String name : names) {
			assertTrue(files.contains(new File(root, "a/libs/" + name)));
		}
	}

	/**
	 * 不同文件名的相同 JAR 按内容去重。
	 *
	 * @throws Exception 测试输入创建或校验失败。
	 */
	public void testDeduplicatesIdenticalJarWithDifferentNames() throws Exception {
		File original = jar("a/libs/network-1.jar", "config.txt", "same");
		File copy = new File(root, "b/libs/renamed.jar");
		Files.createDirectories(copy.getParentFile().toPath());
		Files.copy(original.toPath(), copy.toPath());
		Dependencies dependencies = select("a", "b");
		assertEquals(Collections.singletonList(original), dependencies.getFiles());
		assertEquals(Collections.singletonList(original), dependencies.getFilesForLibraries(
				Collections.singletonList(new File(root, "b"))));
	}

	/**
	 * 最高版本相同但内容不同时报告双方来源。
	 *
	 * @throws Exception 测试输入创建或校验失败。
	 */
	public void testEqualHighestVersionsWithDifferentContentsFail() throws Exception {
		File first = jar("a/libs/network-1.2.jar", "config.txt", "one");
		File second = jar("b/libs/network-1.2.0.jar", "config.txt", "two");
		try {
			select("a", "b");
			fail("必须报告相同版本的不同内容");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains(first.toString()));
			assertTrue(expected.getMessage().contains(second.toString()));
		}
	}

	/**
	 * 已淘汰的旧版本副本不触发最高版本内容冲突。
	 *
	 * @throws Exception 测试输入创建或校验失败。
	 */
	public void testExcludedOlderCopiesDoNotProduceFalseConflict() throws Exception {
		jar("a/libs/network-1.jar", "config.txt", "one");
		jar("b/libs/network-1.jar", "config.txt", "different old copy");
		File highest = jar("c/libs/network-2.jar", "config.txt", "two");
		assertEquals(Collections.singletonList(highest), select("a", "b", "c").getFiles());
	}

	/**
	 * 普通类重名仍然失败并报告两个来源。
	 *
	 * @throws Exception 测试输入创建或校验失败。
	 */
	public void testRemainingDuplicateClassNamesReportBothSources() throws Exception {
		File first = jar("a/libs/first.jar", "demo/Conflict.class", "one");
		File second = jar("b/libs/second.jar", "demo/Conflict.class", "two");
		try {
			Dependencies.validateClasses(select("a", "b").getFiles());
			fail("必须报告重复类");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("demo.Conflict"));
			assertTrue(expected.getMessage().contains(first.toString()));
			assertTrue(expected.getMessage().contains(second.toString()));
		}
	}

	/**
	 * 类目录与 JAR 之间的重名同样被拒绝。
	 *
	 * @throws Exception 测试输入创建或校验失败。
	 */
	public void testClassDirectoryAndJarConflict() throws Exception {
		File classes = new File(root, "classes");
		Files.createDirectories(new File(classes, "demo").toPath());
		Files.write(new File(classes, "demo/Conflict.class").toPath(), fixtureClass());
		File dependency = jar("a/libs/dep.jar", "demo/Conflict.class", "one");
		try {
			Dependencies.validateClasses(Arrays.asList(classes, dependency));
			fail("必须检查项目和依赖之间的重复类");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("demo.Conflict"));
		}
	}

	/**
	 * 损坏 JAR 的诊断包含原始文件路径。
	 *
	 * @throws Exception 测试输入创建或校验失败。
	 */
	public void testUnreadableJarHasPathInDiagnostic() throws Exception {
		File file = new File(root, "a/libs/broken.jar");
		Files.createDirectories(file.getParentFile().toPath());
		Files.write(file.toPath(), new byte[] { 1, 2, 3 });
		try {
			Dependencies.validateClasses(select("a").getFiles());
			fail("损坏 JAR 必须报告错误");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains(file.toString()));
		}
	}

	/**
	 * 加载器在反射分析前报告依赖重复类。
	 *
	 * @throws Exception 测试输入创建或校验失败。
	 */
	public void testRuntimeLoaderReportsConflictBeforeLoadingClasses() throws Exception {
		File runtime = new File(root, "runtime");
		Files.createDirectories(runtime.toPath());
		jar("libraries/a/classes.jar", "demo/A.class", "placeholder");
		File first = jar("libraries/a/libs/first.jar", "demo/Conflict.class", "one");
		File second = jar("libraries/a/libs/second.jar", "demo/Conflict.class", "two");
		ByteArrayOutputStream errors = new ByteArrayOutputStream();
		Compiler compiler = new Compiler(Compiler.Platform.None, System.out, new PrintStream(errors, true, "UTF-8"));
		RuntimeLoader loader = new RuntimeLoader(compiler,
				new File("build/runtime/android/classes").getAbsolutePath(), runtime.toString(),
				new File(root, "libraries").toString());
		loader.loadSimpleObjects();
		assertEquals(1, compiler.getErrorCount());
		assertTrue(errors.toString("UTF-8").contains(first.toString()));
		assertTrue(errors.toString("UTF-8").contains(second.toString()));
	}

	/**
	 * 项目打包复用加载阶段的全局依赖选择。
	 *
	 * @throws Exception 测试输入创建或校验失败。
	 */
	public void testRuntimeLoaderReusesSelectionForProjectDependencies() throws Exception {
		File runtime = new File(root, "runtime");
		Files.createDirectories(runtime.toPath());
		jar("libraries/a/classes.jar", "marker.txt", "a");
		jar("libraries/b/classes.jar", "marker.txt", "b");
		jar("libraries/a/libs/network-3.9.jar", "config.txt", "old");
		File highest = jar("libraries/b/libs/network-3.10.jar", "config.txt", "new");
		Compiler compiler = new Compiler(Compiler.Platform.None, System.out, System.err);
		RuntimeLoader loader = new RuntimeLoader(compiler,
				new File("build/runtime/android/classes").getAbsolutePath(), runtime.toString(),
				new File(root, "libraries").toString());
		loader.loadSimpleObjects();
		assertEquals(0, compiler.getErrorCount());
		assertEquals(Collections.singletonList(highest), loader.getDependenciesForLibraries(
				Collections.singletonList(new File(root, "libraries/a"))));
		assertTrue(loader.getDependenciesForLibraries(Collections.<File>emptyList()).isEmpty());
	}

	/**
	 * 两个模块描述不属于重复业务类，多版本目录也不参与基础类检查。
	 *
	 * @throws Exception 测试归档读写失败。
	 */
	public void testModuleDescriptorsAndVersionedEntriesAreExcluded() throws Exception {
		File first = moduleJar("first.jar", "module-info.class");
		File second = moduleJar("second.jar", "module-info.class");
		File versioned = jar("versioned.jar", "META-INF/versions/9/probe/Only.class", "unused");
		Dependencies.validateClasses(Arrays.asList(first, second, versioned));
	}

	/**
	 * 运行库加载器在反射前排除模块与多版本条目，不向 JVM 提交这些类型。
	 *
	 * @throws Exception 测试归档或运行库目录创建失败。
	 */
	public void testRuntimeLoaderSkipsModuleAndVersionedEntries() throws Exception {
		File runtime = new File(root, "runtime");
		Files.createDirectories(runtime.toPath());
		moduleJar("libraries/a/classes.jar", "module-info.class");
		moduleJar("libraries/b/classes.jar", "renamed.class");
		jar("libraries/c/classes.jar", "META-INF/versions/9/probe/Only.class", "unused");

		// 空运行库与仅含元数据的扩展库不应产生类反射错误。
		ByteArrayOutputStream errors = new ByteArrayOutputStream();
		Compiler compiler = new Compiler(Compiler.Platform.None, System.out, new PrintStream(errors, true, "UTF-8"));
		RuntimeLoader loader = new RuntimeLoader(compiler,
				new File("build/runtime/android/classes").getAbsolutePath(), runtime.toString(),
				new File(root, "libraries").toString());
		loader.loadSimpleObjects();
		assertEquals(errors.toString("UTF-8"), 0, compiler.getErrorCount());
	}

	/**
	 * 模块识别依据内容标志，不依赖外部文件名。
	 *
	 * @throws Exception 测试归档读写失败。
	 */
	public void testModuleFlagIsReadFromContent() throws Exception {
		Dependencies.validateClasses(Arrays.asList(moduleJar("one.jar", "renamed.class"),
				moduleJar("two.jar", "renamed.class")));
	}

	/**
	 * 截断的基础类必须报错，不能作为模块或普通元数据被静默忽略。
	 *
	 * @throws Exception 测试文件读写失败。
	 */
	public void testMalformedClassReportsEntry() throws Exception {
		File file = new File(root, "broken-class.jar");
		try (JarOutputStream output = new JarOutputStream(new FileOutputStream(file))) {
			output.putNextEntry(new JarEntry("probe/Broken.class"));
			output.write(new byte[]{0, 1});
			output.closeEntry();
		}

		try {
			Dependencies.validateClasses(Collections.singletonList(file));
			fail("损坏基础类必须报告错误");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("probe/Broken.class"));
		}
	}

	/**
	 * 读取真实测试类，供只检查类头的输入构造使用。
	 *
	 * @return 有效的普通 class 字节。
	 * @throws IOException 测试类资源读取失败。
	 */
	private byte[] fixtureClass() throws IOException {
		try (InputStream input = DependenciesTest.class.getResourceAsStream("DependenciesTest.class")) {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			for (int count; (count = input.read(buffer)) != -1;) {
				output.write(buffer, 0, count);
			}
			return output.toByteArray();
		}
	}

	/**
	 * 生成包含合法 Java 9 模块描述的归档，覆盖模块常量池和访问标志。
	 *
	 * @param name 输出 JAR 名称。
	 * @param entry 模块描述的归档路径。
	 * @return 输出 JAR。
	 * @throws IOException 测试归档写入失败。
	 */
	private File moduleJar(String name, String entry) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (DataOutputStream output = new DataOutputStream(bytes)) {
			// 常量池包含 module-info、Module 属性名、模块名称和 java.base。
			output.writeInt(0xCAFEBABE);
			output.writeShort(0);
			output.writeShort(53);
			output.writeShort(8);
			output.writeByte(1);
			output.writeUTF("module-info");
			output.writeByte(7);
			output.writeShort(1);
			output.writeByte(1);
			output.writeUTF("Module");
			output.writeByte(1);
			output.writeUTF("fixture.module");
			output.writeByte(19);
			output.writeShort(4);
			output.writeByte(1);
			output.writeUTF("java.base");
			output.writeByte(19);
			output.writeShort(6);

			// 模块描述无父类、字段或方法，Module 属性声明唯一的基础模块依赖。
			output.writeShort(0x8000);
			output.writeShort(2);
			for (int index = 0; index < 4; index++) {
				output.writeShort(0);
			}
			output.writeShort(1);
			output.writeShort(3);
			output.writeInt(22);
			output.writeShort(5);
			output.writeShort(0);
			output.writeShort(0);
			output.writeShort(1);
			output.writeShort(7);
			output.writeShort(0x8000);
			for (int index = 0; index < 5; index++) {
				output.writeShort(0);
			}
		}

		File file = new File(root, name);
		Files.createDirectories(file.getParentFile().toPath());
		try (JarOutputStream output = new JarOutputStream(new FileOutputStream(file))) {
			output.putNextEntry(new JarEntry(entry));
			output.write(bytes.toByteArray());
			output.closeEntry();
		}
		return file;
	}

}

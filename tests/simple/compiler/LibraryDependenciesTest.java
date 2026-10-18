package simple.compiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
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

/** 验证依赖文件名选版、内容去重、打包溯源和重复类诊断。 */
public final class LibraryDependenciesTest extends TestCase {
	private File root;

	@Override
	protected void setUp() throws Exception {
		File base = new File("build/tests/library-dependencies");
		Files.createDirectories(base.toPath());
		root = Files.createTempDirectory(base.toPath(), "case-").toFile().getAbsoluteFile();
	}

	private File jar(String path, String entry, String value) throws IOException {
		File file = new File(root, path);
		Files.createDirectories(file.getParentFile().toPath());
		try (JarOutputStream output = new JarOutputStream(new FileOutputStream(file))) {
			JarEntry item = new JarEntry(entry);
			item.setTime(0);
			output.putNextEntry(item);
			output.write(value.getBytes(StandardCharsets.UTF_8));
			output.closeEntry();
		}
		return file;
	}

	private LibraryDependencies select(String... libraries) throws IOException {
		File[] files = new File[libraries.length];
		for (int i = 0; i < libraries.length; i++) files[i] = new File(root, libraries[i]);
		return new LibraryDependencies(Arrays.asList(files));
	}

	public void testNumericVersionComparison() {
		assertTrue(LibraryDependencies.compareVersion("3.10", "3.9") > 0);
		assertEquals(0, LibraryDependencies.compareVersion("01.002", "1.2.0"));
		assertTrue(LibraryDependencies.compareVersion("999999999999999999999.1", "99.9") > 0);
		assertTrue(LibraryDependencies.compareVersion("1.0.1", "1") > 0);
	}

	public void testSelectsHighestAndKeepsResourceFromSelectedJar() throws Exception {
		jar("a/libs/my-network-3.9.jar", "config.txt", "old");
		File highest = jar("b/libs/my-network-3.10.jar", "config.txt", "new");
		LibraryDependencies dependencies = select("b", "a");
		assertEquals(Collections.singletonList(highest), dependencies.getFiles());
		assertEquals(Collections.singletonList(highest), dependencies.getFilesForLibraries(
				Collections.singletonList(new File(root, "a"))));
		try (JarFile selected = new JarFile(dependencies.getFiles().get(0))) {
			assertEquals('n', selected.getInputStream(selected.getJarEntry("config.txt")).read());
		}
		assertTrue(dependencies.getFilesForLibraries(Collections.singletonList(new File(root, "unused"))).isEmpty());
	}

	public void testOnlyDigitsAndSingleDotsFormVersion() throws Exception {
		String[] names = { "x-1.0-beta.jar", "x-1..2.jar", "x-.1.jar", "x-1..jar", "x-1e2.jar", "x.jar" };
		for (int i = 0; i < names.length; i++) jar("a/libs/" + names[i], "value.txt", "value" + i);
		jar("a/libs/x-1.jar", "value.txt", "one");
		File highest = jar("a/libs/x-2.JAR", "value.txt", "two");
		List<File> files = select("a").getFiles();
		assertEquals(names.length + 1, files.size());
		assertTrue(files.contains(highest));
		for (String name : names) assertTrue(files.contains(new File(root, "a/libs/" + name)));
	}

	public void testDeduplicatesIdenticalJarWithDifferentNames() throws Exception {
		File original = jar("a/libs/network-1.jar", "config.txt", "same");
		File copy = new File(root, "b/libs/renamed.jar");
		Files.createDirectories(copy.getParentFile().toPath());
		Files.copy(original.toPath(), copy.toPath());
		LibraryDependencies dependencies = select("a", "b");
		assertEquals(Collections.singletonList(original), dependencies.getFiles());
		assertEquals(Collections.singletonList(original), dependencies.getFilesForLibraries(
				Collections.singletonList(new File(root, "b"))));
	}

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

	public void testExcludedOlderCopiesDoNotProduceFalseConflict() throws Exception {
		jar("a/libs/network-1.jar", "config.txt", "one");
		jar("b/libs/network-1.jar", "config.txt", "different old copy");
		File highest = jar("c/libs/network-2.jar", "config.txt", "two");
		assertEquals(Collections.singletonList(highest), select("a", "b", "c").getFiles());
	}

	public void testRemainingDuplicateClassNamesReportBothSources() throws Exception {
		File first = jar("a/libs/first.jar", "demo/Conflict.class", "one");
		File second = jar("b/libs/second.jar", "demo/Conflict.class", "two");
		try {
			LibraryDependencies.validateClasses(select("a", "b").getFiles());
			fail("必须报告重复类");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("demo.Conflict"));
			assertTrue(expected.getMessage().contains(first.toString()));
			assertTrue(expected.getMessage().contains(second.toString()));
		}
	}

	public void testClassDirectoryAndJarConflict() throws Exception {
		File classes = new File(root, "classes");
		Files.createDirectories(new File(classes, "demo").toPath());
		Files.write(new File(classes, "demo/Conflict.class").toPath(), new byte[] { 1 });
		File dependency = jar("a/libs/dep.jar", "demo/Conflict.class", "one");
		try {
			LibraryDependencies.validateClasses(Arrays.asList(classes, dependency));
			fail("必须检查项目和依赖之间的重复类");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("demo.Conflict"));
		}
	}

	public void testUnreadableJarHasPathInDiagnostic() throws Exception {
		File file = new File(root, "a/libs/broken.jar");
		Files.createDirectories(file.getParentFile().toPath());
		Files.write(file.toPath(), new byte[] { 1, 2, 3 });
		try {
			LibraryDependencies.validateClasses(select("a").getFiles());
			fail("损坏 JAR 必须报告错误");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains(file.toString()));
		}
	}

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
}

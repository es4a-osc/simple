package simple.compiler;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 验证固定交付目录、AAR 展开、共享选择和失败诊断，不依赖仓库或个人 Gradle 缓存。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class LibraryArtifactsTest extends TestCase {

	private File root;

	/**
	 * 为当前用例创建独立的构建输入目录。
	 *
	 * @throws Exception 目录无法创建。
	 */
	@Override
	protected void setUp() throws Exception {
		File parent = new File("build/tests/aar");
		Files.createDirectories(parent.toPath());
		root = Files.createTempDirectory(parent.toPath(), "inputs-").toFile().getCanonicalFile();
	}

	/**
	 * 本地 AAR 的代码、内嵌 JAR 及 Android 附加目录均进入统一模型。
	 *
	 * @throws Exception 测试输入准备或读取失败。
	 */
	public void testLocalAarAndEmbeddedJar() throws Exception {
		File library = directory("local");
		File archive = new File(library, "aars/local.aar");
		writeAar(archive, "test.local", true);
		LibraryArtifacts artifacts = new LibraryArtifacts(Arrays.asList(library), new File(root, "expanded"));
		assertEquals(2, artifacts.getJars(Arrays.asList(library)).size());
		List<LibraryArtifacts.AndroidLibrary> inputs = artifacts.getAndroidLibraries(Arrays.asList(library));
		assertEquals(2, inputs.size());
		assertEquals("test.local", inputs.get(0).namespace);
		assertTrue(new File(inputs.get(0).directory, "res/values/strings.xml").isFile());
		assertTrue(new File(inputs.get(0).directory, "assets/data.txt").isFile());
		assertTrue(new File(inputs.get(0).directory, "jni/arm64-v8a/libtest.so").isFile());
	}

	/**
	 * 资源包名来自标准清单，不随目录改名变化，旧式生成清单不再参与选择。
	 *
	 * @throws Exception 测试文件无法写入或清单读取不符合预期。
	 */
	public void testNamespaceComesFromStandardManifest() throws Exception {
		File library = directory("任意目录名称");
		File manifest = new File(library, "AndroidManifest.xml");
		write(manifest, "<manifest package=\"test.portable\" />");
		LibraryArtifacts artifacts = new LibraryArtifacts(Collections.singletonList(library), new File(root, "expanded"));
		LibraryArtifacts.AndroidLibrary input = artifacts.getAndroidLibraries(Collections.singletonList(library)).get(0);
		assertEquals("test.portable", input.namespace);
		assertEquals(manifest, input.manifest);
		write(manifest, "<manifest />");
		try {
			artifacts.getAndroidLibraries(Collections.singletonList(library));
			fail("存在的清单不能缺少资源包名。");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("读取类库清单失败"));
		}
	}

	/**
	 * 全量依赖快照复用规范化后的根库请求，子集和不同顺序的查询仍保持原有含义。
	 *
	 * @throws Exception 测试归档无法创建或依赖选择失败。
	 */
	public void testDependencySnapshotPreservesRootSelectionAndOrder() throws Exception {
		File first = directory("snapshot-first");
		File second = directory("snapshot-second");
		File alpha = new File(first, "libs/alpha.jar");
		File beta = new File(second, "libs/beta.jar");
		writeJar(alpha, "alpha");
		writeJar(beta, "beta");
		List<File> roots = new ArrayList<File>(Arrays.asList(first, second));
		Dependencies dependencies = new Dependencies(roots, new File(root, "snapshot-expanded"));
		List<File> snapshot = dependencies.getFiles();
		assertEquals(Arrays.asList(alpha, beta), snapshot);

		// 路径别名和调用方列表变化不会破坏本轮快照，重复请求也不再重建结果。
		roots.clear();
		File alias = new File(first, "../" + first.getName());
		assertSame(snapshot, dependencies.getFilesForLibraries(Arrays.asList(alias, second)));
		assertSame(snapshot, dependencies.getFilesForLibraries(null));
		try {
			snapshot.clear();
			fail("调用方不能修改共享代码依赖快照。");
		} catch (UnsupportedOperationException expected) {
			assertEquals(2, snapshot.size());
		}

		// 根库顺序影响稳定输出，不能为了命中快照而把不同顺序或子集视为全量查询。
		assertEquals(Collections.singletonList(beta), dependencies.getFilesForLibraries(Collections.singletonList(second)));
		assertEquals(Arrays.asList(beta, alpha), dependencies.getFilesForLibraries(Arrays.asList(second, first)));
		assertTrue(dependencies.getFilesForLibraries(Collections.<File>emptyList()).isEmpty());
	}

	/**
	 * 两个扩展库交付同一依赖时按内容合并，筛选仍返回同一物理输入。
	 *
	 * @throws Exception 测试输入准备或读取失败。
	 */
	public void testSharedRepositoryAarIsExpandedOnce() throws Exception {
		File first = directory("first");
		File second = directory("second");
		File archive = new File(first, "aars/shared.aar");
		writeAar(archive, "test.shared", false);
		Files.createDirectories(new File(second, "aars").toPath());
		Files.copy(archive.toPath(), new File(second, "aars/shared.aar").toPath());
		LibraryArtifacts artifacts = new LibraryArtifacts(Arrays.asList(first, second), new File(root, "expanded"));
		assertEquals(artifacts.getJars(Arrays.asList(first)), artifacts.getJars(Arrays.asList(second)));
		assertEquals(1, artifacts.getJars(null).size());
		assertEquals(3, artifacts.getAndroidLibraries(Arrays.asList(first, second)).size());
	}

	/**
	 * 不同类库中同名的本地归档可能属于不同包，不能仅凭文件名认定版本冲突。
	 *
	 * @throws Exception 测试输入准备或读取失败。
	 */
	public void testUnrelatedLocalAarsWithSameFileName() throws Exception {
		File first = directory("local-first");
		File second = directory("local-second");
		writeAar(new File(first, "aars/library.aar"), "test.first", false);
		writeAar(new File(second, "aars/library.aar"), "test.second", false);
		LibraryArtifacts artifacts = new LibraryArtifacts(Arrays.asList(first, second), new File(root, "expanded"));
		assertEquals(2, artifacts.getJars(null).size());
	}

	/**
	 * 本地 JAR 使用包内 Maven 身份选版，其它独立依赖仍完整保留。
	 * 仓库传递关系及选版由 DependencyResolverTest 通过原生模块声明验证。
	 *
	 * @throws Exception 测试归档无法创建或依赖选择失败。
	 */
	public void testLocalMavenSelectionKeepsOtherDependencies() throws Exception {
		File first = directory("old");
		File second = directory("new");
		writeMavenJar(new File(first, "libs/old.jar"), "1.0", "old");
		writeMavenJar(new File(second, "libs/new.jar"), "1.10", "new");
		writeJar(new File(second, "libs/extra.jar"), "extra");
		List<File> chosen = new LibraryArtifacts(Arrays.asList(first, second), new File(root, "expanded")).getJars(null);
		assertEquals(Arrays.asList(new File(second, "libs/new.jar"), new File(second, "libs/extra.jar")), chosen);
	}

	/**
	 * 同坐标同版本的内容差异必须报错，不以先后顺序静默覆盖。
	 *
	 * @throws Exception 测试输入准备或读取失败。
	 */
	public void testSameCoordinateDifferentContentFails() throws Exception {
		File first = directory("conflict-a");
		File second = directory("conflict-b");
		writeJar(new File(first, "libs/dependency-1.0.jar"), "a");
		writeJar(new File(second, "libs/dependency-1.0.jar"), "b");
		try {
			new LibraryArtifacts(Arrays.asList(first, second), new File(root, "expanded")).getJars(null);
			fail("同版不同内容不应通过。");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("内容不同"));
		}
	}

	/**
	 * 固定目录只扫描第一层原包，源码附件及其它目录不成为运行依赖。
	 *
	 * @throws Exception 测试输入无法创建或扫描结果不符合预期。
	 */
	public void testFixedDirectoriesIgnoreSourcesAndOtherFiles() throws Exception {
		File library = directory("fixed-layout");
		File binary = new File(library, "libs/dependency.jar");
		writeJar(binary, "binary");
		write(new File(library, "libs/dependency-sources.jar"), "非运行归档");
		write(new File(library, "other.jar"), "不是约定目录");
		write(new File(library, "libs/nested/ignored.jar"), "不是第一层");
		assertEquals(Collections.singletonList(binary),
				new LibraryArtifacts(Arrays.asList(library), new File(root, "expanded")).getJars(null));
	}

	/**
	 * 固定目录不能通过目录链接引用交付外部归档，避免复制后依赖丢失。
	 *
	 * @throws Exception 输入准备或链接创建失败。
	 */
	public void testLocalArchiveCannotEscapeThroughDirectoryLink() throws Exception {
		File library = directory("linked-library");
		File outside = directory("outside");
		writeJar(new File(outside, "helper.jar"), "external");
		File link = new File(library, "libs");
		if (File.separatorChar == '\\') {
			Process process = new ProcessBuilder("cmd.exe", "/d", "/c", "mklink", "/J",
					link.getPath(), outside.getPath()).redirectErrorStream(true).start();
			byte[] output = process.getInputStream().readAllBytes();
			assertEquals(new String(output, StandardCharsets.UTF_8), 0, process.waitFor());
		} else {
			Files.createSymbolicLink(link.toPath(), outside.toPath());
		}
		try {
			new LibraryArtifacts(Arrays.asList(library), new File(root, "expanded"));
			fail("链接不能将运行依赖带出交付目录。");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("文件引用无效"));
		} finally {
			Files.delete(link.toPath());
		}
		assertTrue(new File(outside, "helper.jar").isFile());
	}

	/**
	 * 损坏 AAR 与 ZIP 越界条目必须在进入后续构建前被拒绝。
	 *
	 * @throws Exception 测试输入准备或读取失败。
	 */
	public void testCorruptAndEscapingAarFail() throws Exception {
		File library = directory("invalid-aar");
		File archive = new File(library, "aars/bad.aar");
		write(archive, "broken zip");
		try {
			new LibraryArtifacts(Arrays.asList(library), new File(root, "expanded"));
			fail("损坏归档不应通过。");
		} catch (IOException expected) {
			assertNotNull(expected.getMessage());
		}
		try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive.toPath()))) {
			entry(zip, "../outside.txt", new byte[] {1});
		}
		try {
			new LibraryArtifacts(Arrays.asList(library), new File(root, "expanded"));
			fail("归档越界不应通过。");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("无效或重复路径"));
		}
	}

	/**
	 * 扩展库与多个 AAR 的独立 JAR 进入同一候选池，正式版本胜过 rc，AAR 本体保持独立。
	 *
	 * @throws Exception 输入准备或选版失败。
	 */
	public void testIndependentJarsAcrossLibrariesAndAars() throws Exception {
		File first = directory("jar-first");
		File second = directory("jar-second");
		File third = directory("jar-third");
		writeJar(new File(first, "libs/shared-1.0.jar"), "old");
		File preview = new File(root, "shared-2.0-rc1.jar");
		File release = new File(root, "shared-2.0.jar");
		writeJar(preview, "preview");
		writeJar(release, "release");
		wrapEmbedded(new File(second, "aars/preview.aar"), "test.preview", preview);
		wrapEmbedded(new File(third, "aars/release.aar"), "test.release", release);
		LibraryArtifacts artifacts = new LibraryArtifacts(Arrays.asList(first, second, third), new File(root, "expanded"));
		List<File> jars = artifacts.getJars(Arrays.asList(first, second, third));
		assertEquals(1, jars.size());
		assertEquals("shared-2.0.jar", jars.get(0).getName());
	}

	/**
	 * 同名 JAR 可由唯一 Maven 元数据识别版本，来源文件名不参与坐标选版。
	 *
	 * @throws Exception 归档创建或选版失败。
	 */
	public void testEmbeddedMavenIdentityAndEqualVersionConflict() throws Exception {
		File first = directory("metadata-first");
		File second = directory("metadata-second");
		File old = new File(root, "old/helper.jar");
		File current = new File(root, "current/helper.jar");
		writeMavenJar(old, "1.0", "old");
		writeMavenJar(current, "2.0", "current");
		wrapEmbedded(new File(first, "aars/one.aar"), "test.one", old);
		wrapEmbedded(new File(second, "aars/two.aar"), "test.two", current);
		List<File> roots = Arrays.asList(first, second);
		List<File> selected = new LibraryArtifacts(roots, new File(root, "expanded")).getJars(roots);
		assertEquals(1, selected.size());
		assertEquals(LibraryArtifacts.digest(current), LibraryArtifacts.digest(selected.get(0)));
		writeMavenJar(old, "2.0", "different content");
		wrapEmbedded(new File(first, "aars/one.aar"), "test.one", old);
		try {
			new LibraryArtifacts(roots, new File(root, "expanded-conflict")).getJars(roots);
			fail("相同坐标版本的不同内容必须失败");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("同身份同版本"));
			assertTrue(expected.getMessage().contains("one.aar!/libs/helper.jar"));
			assertTrue(expected.getMessage().contains("two.aar!/libs/helper.jar"));
		}
	}

	/**
	 * 无版本的同名包无法安全选版，内容不同应保留给后续重复类检查。
	 *
	 * @throws Exception 测试归档无法创建或读取。
	 */
	public void testUnversionedIndependentJarsAreNotGuessed() throws Exception {
		File first = directory("unknown-first");
		File second = directory("unknown-second");
		writeJar(new File(first, "libs/utils.jar"), "one");
		writeJar(new File(second, "libs/utils.jar"), "two");
		List<File> roots = Arrays.asList(first, second);
		assertEquals(2, new LibraryArtifacts(roots, new File(root, "expanded")).getJars(roots).size());
	}

	/**
	 * 唯一模块元数据与版本文件名可以归一，分类制品不能被主制品当作同版冲突。
	 *
	 * @throws Exception 输入创建或身份识别失败。
	 */
	public void testMetadataAndFileNameShareIdentityAndClassifiersRemainSeparate() throws Exception {
		File first = directory("named-first");
		File second = directory("named-second");
		writeMavenJar(new File(first, "libs/helper.jar"), "1.0", "old");
		writeJar(new File(second, "libs/shared-2.0.jar"), "new");
		List<File> roots = Arrays.asList(first, second);
		List<File> jars = new LibraryArtifacts(roots, new File(root, "expanded")).getJars(roots);
		assertEquals(1, jars.size());
		assertEquals("shared-2.0.jar", jars.get(0).getName());
		File classified = directory("classified");
		writeJar(new File(classified, "libs/main.jar"), "main");
		writeJar(new File(classified, "libs/tests.jar"), "tests");
		assertEquals(2, new LibraryArtifacts(Arrays.asList(classified), new File(root, "classified-expanded")).getJars(null).size());
	}

	/**
	 * 缺少运行变体的发布元数据不能被当作无依赖库，从而把不完整依赖误报为成功。
	 *
	 * @throws Exception 测试描述无法写入。
	 */
	public void testNativeModuleRequiresRuntimeVariant() throws Exception {
		File library = directory("incomplete-module");
		write(new File(library, "AndroidManifest.xml"), "<manifest package=\"test.native_module\" />");
		write(new File(library, "dependencies.module"), "{\"formatVersion\":\"1.1\",\"component\":{\"group\":\"es4a.extension\","
				+ "\"module\":\"test.native_module\",\"version\":\"1.0\"},\"variants\":[]}");
		try {
			new LibraryArtifacts(Arrays.asList(library), new File(root, "expanded-incomplete"));
			fail("没有运行变体的描述必须失败");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("运行依赖变体"));
		}
	}

	/** 将独立 JAR 原样嵌入只含清单的 AAR，验证资源型 AAR 不要求 classes.jar。 */
	private void wrapEmbedded(File archive, String namespace, File embedded) throws IOException {
		Files.createDirectories(archive.getParentFile().toPath());
		try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive.toPath()))) {
			entry(zip, "AndroidManifest.xml", ("<manifest package=\"" + namespace + "\" />").getBytes(StandardCharsets.UTF_8));
			entry(zip, "libs/" + embedded.getName(), Files.readAllBytes(embedded.toPath()));
		}
	}

	/** 写入包含唯一 Maven 身份及可区分内容的 JAR。 */
	private void writeMavenJar(File file, String version, String content) throws IOException {
		Files.createDirectories(file.getParentFile().toPath());
		try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(file.toPath()))) {
			entry(output, "META-INF/maven/test/shared/pom.properties",
					("groupId=test\nartifactId=shared\nversion=" + version + "\n").getBytes(StandardCharsets.UTF_8));
			entry(output, "identity.txt", content.getBytes(StandardCharsets.UTF_8));
		}
	}

	/**
	 * 创建本用例中的独立交付目录。
	 *
	 * @param name 目录名称。
	 * @return 已创建的目录。
	 * @throws IOException 目录无法创建。
	 */
	private File directory(String name) throws IOException {
		File directory = new File(root, name);
		Files.createDirectories(directory.toPath());
		return directory;
	}

	/**
	 * 写入含资源、资产及原生库的测试 AAR，按需加入独立的内嵌 JAR。
	 *
	 * @param archive 输出 AAR。
	 * @param namespace 清单包名。
	 * @param embedded 是否包含内嵌 JAR。
	 * @throws IOException 归档无法写入。
	 */
	private void writeAar(File archive, String namespace, boolean embedded) throws IOException {
		Files.createDirectories(archive.getParentFile().toPath());
		File jar = new File(root, "fixture.jar");
		writeJar(jar, namespace);
		try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive.toPath()))) {
			entry(zip, "AndroidManifest.xml", ("<manifest package=\"" + namespace + "\" />").getBytes(StandardCharsets.UTF_8));
			entry(zip, "classes.jar", Files.readAllBytes(jar.toPath()));
			entry(zip, "res/values/strings.xml", "<resources><string name=\"message\">test</string></resources>".getBytes(StandardCharsets.UTF_8));
			entry(zip, "assets/data.txt", new byte[] {1});
			entry(zip, "jni/arm64-v8a/libtest.so", new byte[] {2});
			if (embedded) {
				writeJar(jar, "embedded");
				entry(zip, "libs/embedded.jar", Files.readAllBytes(jar.toPath()));
			}
		}
	}

	/**
	 * 写入可读取且内容可区分的 JAR。
	 *
	 * @param file 输出文件。
	 * @param text 内容身份。
	 * @throws IOException 归档无法写入。
	 */
	private void writeJar(File file, String text) throws IOException {
		Files.createDirectories(file.getParentFile().toPath());
		try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(file.toPath()))) {
			entry(output, "identity.txt", text.getBytes(StandardCharsets.UTF_8));
		}
	}

	/**
	 * 写入一个 ZIP 条目。
	 *
	 * @param output 当前归档。
	 * @param name 条目相对路径。
	 * @param bytes 内容。
	 * @throws IOException 条目无法写入。
	 */
	private void entry(ZipOutputStream output, String name, byte[] bytes) throws IOException {
		output.putNextEntry(new ZipEntry(name));
		output.write(bytes);
		output.closeEntry();
	}

	/**
	 * 写入测试输入，不依赖默认平台编码。
	 *
	 * @param file 输出文件。
	 * @param text UTF-8 内容。
	 * @throws IOException 内容无法写入。
	 */
	private void write(File file, String text) throws IOException {
		Files.createDirectories(file.getParentFile().toPath());
		Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
	}
}

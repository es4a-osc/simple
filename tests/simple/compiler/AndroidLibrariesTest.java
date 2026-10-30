package simple.compiler;

import junit.framework.TestCase;
import simple.compiler.util.Toolchain;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * 验证官方清单合并、资源符号重映射及真实 AAPT2 链接，不以文件存在代替内容校验。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class AndroidLibrariesTest extends TestCase {

	private File root;

	/** 当前用例独占的 Android 临时输入管理器。 */
	private AndroidLibraries android;

	private static final String ANDROID = " xmlns:android=\"http://schemas.android.com/apk/res/android\"";

	/**
	 * 为当前用例创建独立的构建输入目录。
	 *
	 * @throws Exception 目录无法创建。
	 */
	@Override
	protected void setUp() throws Exception {
		File parent = new File("build/tests/aar");
		Files.createDirectories(parent.toPath());
		root = Files.createTempDirectory(parent.toPath(), "android-").toFile().getCanonicalFile();
		android = new AndroidLibraries(root);
	}

	/**
	 * 无论验证成功或失败，都释放本用例创建的临时输入。
	 *
	 * @throws Exception 临时目录清理失败。
	 */
	@Override
	protected void tearDown() throws Exception {
		android.close();
	}

	/**
	 * 清单应合并权限和组件，展开相对类名及应用、用户占位符。
	 *
	 * @throws Exception 输入创建或合并失败。
	 */
	public void testManifestPermissionsComponentsAndPlaceholders() throws Exception {
		List<LibraryArtifacts.AndroidLibrary> libraries = library("manifest", "<manifest package=\"test.lib\"" + ANDROID + ">"
				+ "<uses-permission android:name=\"android.permission.INTERNET\"/>"
				+ "<application><provider android:name=\".Provider\" android:authorities=\"${applicationId}.${suffix}\" android:exported=\"false\"/>"
				+ "<meta-data android:name=\"fixture\" android:value=\"present\"/></application></manifest>", "");
		File manifest = applicationManifest();
		Map<String, Object> placeholders = new LinkedHashMap<String, Object>();
		placeholders.put("suffix", "provider");
		android.mergeManifest(manifest, libraries, placeholders, "test.app", System.out, System.err);
		String merged = Files.readString(manifest.toPath());
		assertTrue(merged.contains("android.permission.INTERNET"));
		assertTrue(merged.contains("test.lib.Provider"));
		assertTrue(merged.contains("test.app.provider"));
		assertTrue(merged.contains("present"));
	}

	/**
	 * 无法满足的最低 SDK 与未配置占位符必须导致真实合并失败。
	 *
	 * @throws Exception 输入创建失败。
	 */
	public void testManifestSdkAndMissingPlaceholderFail() throws Exception {
		String[] manifests = {
			"<manifest package=\"test.sdk\"" + ANDROID + "><uses-sdk android:minSdkVersion=\"25\"/></manifest>",
			"<manifest package=\"test.placeholder\"" + ANDROID + "><application><meta-data android:name=\"key\" android:value=\"${missing}\"/></application></manifest>"
		};
		for (int i = 0; i < manifests.length; i++) {
			List<LibraryArtifacts.AndroidLibrary> libraries = library("failure-" + i, manifests[i], "");
			try {
				android.mergeManifest(applicationManifest(), libraries, Collections.<String, Object>emptyMap(), "test.app", System.out, System.err);
				fail("不满足运行条件的清单不应合并成功。");
			} catch (IOException expected) {
				assertTrue(expected.getMessage().contains("清单合并失败"));
			}
		}
	}

	/**
	 * R 类必须使用最终普通资源编号，并保持 styleable 数组及索引相互对应。
	 *
	 * @throws Exception 符号生成、Java 编译或反射检查失败。
	 */
	public void testFinalResourceValuesAndStyleable() throws Exception {
		String symbols = "int attr flag 0x0\nint string message 0x0\n"
				+ "int[] styleable Widget { 0x01010000, 0x0 }\n"
				+ "int styleable Widget_android_theme 0\nint styleable Widget_flag 1\n";
		List<LibraryArtifacts.AndroidLibrary> libraries = library("symbols", "<manifest package=\"test.symbols\"/>", symbols);
		File finalSymbols = new File(root, "R.txt");
		write(finalSymbols, symbols.replace("int attr flag 0x0", "int attr flag 0x7f010001")
				.replace("int string message 0x0", "int string message 0x7f020001")
				.replace("0x01010000, 0x0", "0x01010000, 0x7f010001"));
		File sources = new File(root, "sources");
		Files.createDirectories(sources.toPath());
		List<File> generated = AndroidLibraries.generateResourceSources(libraries, finalSymbols, "test.app", sources,
				Collections.<File>emptyList());
		File classes = new File(root, "classes");
		Files.createDirectories(classes.toPath());
		assertTrue(Toolchain.compileJava(root, classes, generated, System.out, System.err));
		try (URLClassLoader loader = new URLClassLoader(new URL[] {classes.toURI().toURL()})) {
			assertEquals(0x7f020001, loader.loadClass("test.symbols.R$string").getField("message").getInt(null));
			Class<?> styleable = loader.loadClass("test.symbols.R$styleable");
			int index = styleable.getField("Widget_flag").getInt(null);
			assertEquals(1, index);
			assertEquals(0x7f010001, ((int[]) styleable.getField("Widget").get(null))[index]);
		}
	}

	/**
	 * 无法在最终资源表中找到的库符号必须明确失败，不能生成缺字段的 R 类。
	 *
	 * @throws Exception 输入创建失败。
	 */
	public void testMissingFinalResourceFails() throws Exception {
		List<LibraryArtifacts.AndroidLibrary> libraries = library("missing-symbol", "<manifest package=\"test.missing\"/>", "int string missing 0x0\n");
		write(new File(libraries.get(0).directory, "res/values/strings.xml"),
				"<resources><string name=\"missing\">required</string></resources>");
		File symbols = new File(root, "empty-R.txt");
		write(symbols, "");
		try {
			AndroidLibraries.generateResourceSources(libraries, symbols, "test.app", new File(root, "sources"),
					Collections.<File>emptyList());
			fail("缺失资源不应生成不完整的 R 类。");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("最终资源表缺少"));
		}
		libraries = library("missing-styleable", "<manifest package=\"test.styleable\"/>",
				"int[] styleable Widget { 0x0 }\nint styleable Widget_flag 0\n");
		write(new File(libraries.get(0).directory, "res/values/attrs.xml"),
				"<resources><declare-styleable name=\"Widget\"><attr name=\"flag\"/></declare-styleable></resources>");
		write(symbols, "int[] styleable Widget { 0x7f010001 }\nint styleable Widget_other 0\n");
		try {
			AndroidLibraries.generateResourceSources(libraries, symbols, "test.app", new File(root, "sources"),
					Collections.<File>emptyList());
			fail("缺失 styleable 子项不应生成不完整的 R 类。");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("最终资源表缺少"));
		}
	}

	/**
	 * 传递依赖换版后已经消失、且代码未引用的旧符号不阻断 R 类生成。
	 *
	 * @throws Exception 符号生成、编译或字段检查失败。
	 */
	public void testObsoleteTransitiveSymbolsAreOmitted() throws Exception {
		List<LibraryArtifacts.AndroidLibrary> libraries = library("obsolete", "<manifest package=\"test.old\"/>",
				"int color removed 0x0\nint string present 0x0\nint id shared 0x0\n"
				+ "int[] styleable Old { 0x0 }\nint styleable Old_flag 0\n");
		File symbols = new File(root, "final-R.txt");
		write(symbols, "int string present 0x7f010001\nint id shared 0x7f020001\n");
		// 不同布局使用同一 ID 是合法输入，不应被用于识别所有权的解析器误判为冲突。
		String layout = "<FrameLayout" + ANDROID + " android:id=\"@+id/shared\""
				+ " android:layout_width=\"match_parent\" android:layout_height=\"match_parent\"/>";
		write(new File(libraries.get(0).directory, "res/layout/first.xml"), layout);
		write(new File(libraries.get(0).directory, "res/layout/second.xml"), layout);
		File sources = new File(root, "sources");
		List<File> generated = AndroidLibraries.generateResourceSources(libraries, symbols, "test.app", sources,
				Collections.<File>emptyList());
		String text = Files.readString(generated.get(0).toPath());
		assertTrue(text.contains("present"));
		assertFalse(text.contains("removed"));
		assertFalse(text.contains("Old_flag"));
		File classes = new File(root, "classes");
		Files.createDirectories(classes.toPath());
		assertTrue(Toolchain.compileJava(root, classes, generated, System.out, System.err));
	}

	/**
	 * 普通资源及 styleable 即使来自传递依赖，只要字节码仍引用缺失字段就明确失败。
	 *
	 * @throws Exception 字节码夹具无法创建或缺失引用未正确诊断。
	 */
	public void testMissingReferencedTransitiveResourceFails() throws Exception {
		List<LibraryArtifacts.AndroidLibrary> libraries = library("referenced", "<manifest package=\"test.old\"/>",
				"int color removed 0x0\nint[] styleable Old { 0x0 }\nint styleable Old_flag 0\n");
		File symbols = new File(root, "final-R.txt");
		write(symbols, "");
		String[] owners = {"test/old/R$color", "test/old/R$styleable"};
		String[] fields = {"removed", "Old_flag"};
		for (int index = 0; index < owners.length; index++) {
			File jar = new File(root, "references-" + index + ".jar");
			ClassWriter writer = new ClassWriter(0);
			writer.visit(Opcodes.V11, Opcodes.ACC_PUBLIC, "test/consumer/UsesResource", null, "java/lang/Object", null);
			MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "value", "()I", null, null);
			method.visitCode();
			method.visitFieldInsn(Opcodes.GETSTATIC, owners[index], fields[index], "I");
			method.visitInsn(Opcodes.IRETURN);
			method.visitMaxs(1, 0);
			method.visitEnd();
			writer.visitEnd();
			try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(jar.toPath()))) {
				zip.putNextEntry(new ZipEntry("test/consumer/UsesResource.class"));
				zip.write(writer.toByteArray());
				zip.closeEntry();
			}
			try {
				AndroidLibraries.generateResourceSources(libraries, symbols, "test.app", new File(root, "sources"),
						Arrays.asList(jar));
				fail("代码实际使用的缺失字段必须报错。");
			} catch (IOException expected) {
				assertTrue(expected.getMessage().contains("代码引用了缺失"));
				assertTrue(expected.getMessage().contains(fields[index]));
				assertTrue(expected.getMessage().contains("UsesResource.class"));
			}
		}
	}

	/**
	 * 编译 SDK 要求超过当前平台时，在链接前报告制品条件不满足。
	 *
	 * @throws Exception 输入创建失败。
	 */
	public void testHigherCompileSdkMetadataFails() throws Exception {
		List<LibraryArtifacts.AndroidLibrary> libraries = library("compile-sdk", "<manifest package=\"test.compile\"/>", "");
		write(new File(libraries.get(0).directory, "META-INF/com/android/build/gradle/aar-metadata.properties"), "minCompileSdk=31\n");
		try {
			android.mergeManifest(applicationManifest(), libraries, Collections.<String, Object>emptyMap(), "test.app", System.out, System.err);
			fail("超出编译平台要求的 AAR 不应通过。");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("编译 API 30"));
		}
	}

	/**
	 * 同资源包名的两份扩展库清单必须分别合并，不能共用同一个临时文件。
	 *
	 * @throws Exception 输入创建或合并失败。
	 */
	public void testSeparateManifestsWithSameNamespace() throws Exception {
		File first = new File(root, "manifest-first");
		File second = new File(root, "manifest-second");
		write(new File(first, "AndroidManifest.xml"), "<manifest" + ANDROID + " package=\"test.shared\"><uses-permission android:name=\"android.permission.INTERNET\"/></manifest>");
		write(new File(second, "AndroidManifest.xml"), "<manifest" + ANDROID + " package=\"test.shared\"><uses-permission android:name=\"android.permission.VIBRATE\"/></manifest>");
		List<LibraryArtifacts.AndroidLibrary> libraries = new LibraryArtifacts(Arrays.asList(first, second), new File(root, "expanded"))
				.getAndroidLibraries(Arrays.asList(first, second));
		// 模型已从标准清单取得相同包名；移除输入 package，单独验证合并层的副本隔离。
		write(new File(first, "AndroidManifest.xml"), "<manifest" + ANDROID + "><uses-permission android:name=\"android.permission.INTERNET\"/></manifest>");
		write(new File(second, "AndroidManifest.xml"), "<manifest" + ANDROID + "><uses-permission android:name=\"android.permission.VIBRATE\"/></manifest>");
		File manifest = applicationManifest();
		android.mergeManifest(manifest, libraries, Collections.<String, Object>emptyMap(), "test.app", System.out, System.err);
		String merged = Files.readString(manifest.toPath());
		assertTrue(merged.contains("android.permission.INTERNET"));
		assertTrue(merged.contains("android.permission.VIBRATE"));

		// 第二次合并复用各自的清单副本，不重复解析、写入或创建新的副本目录。
		List<File> temporary = temporaryInputDirectories();
		assertEquals(1, temporary.size());
		File manifests = new File(temporary.get(0), "manifests");
		File[] copies = manifests.listFiles();
		assertNotNull(copies);
		assertEquals(2, copies.length);
		FileTime marker = FileTime.fromMillis(1234567890000L);
		for (File copy : copies) {
			Files.setLastModifiedTime(new File(copy, "AndroidManifest.xml").toPath(), marker);
		}
		android.mergeManifest(applicationManifest(), libraries, Collections.<String, Object>emptyMap(), "test.app", System.out, System.err);
		assertEquals(2, manifests.listFiles().length);
		for (File copy : copies) {
			assertEquals(marker, Files.getLastModifiedTime(new File(copy, "AndroidManifest.xml").toPath()));
		}

		// 关闭只移除实例拥有的副本，类库原清单、应用清单及构建目录继续保留。
		android.close();
		assertFalse(temporary.get(0).exists());
		assertTrue(new File(first, "AndroidManifest.xml").isFile());
		assertTrue(new File(second, "AndroidManifest.xml").isFile());
		assertTrue(manifest.isFile());
	}

	/**
	 * 两个依赖的资源与项目覆盖一起参与首次链接，最终链接保持已分配编号。
	 *
	 * @throws Exception 输入创建或外部工具执行失败。
	 */
	public void testMultipleResourceInputsAndStableIds() throws Exception {
		File first = new File(root, "first");
		File second = new File(root, "second");
		File project = new File(root, "project");
		write(new File(first, "values/strings.xml"), "<resources><string name=\"dependency\">first</string><string name=\"shared\">first</string></resources>");
		write(new File(second, "values/strings.xml"), "<resources><string name=\"shared\">second</string></resources>");
		write(new File(project, "values/strings.xml"), "<resources><string name=\"shared\">project</string><string name=\"reference\">@string/dependency</string></resources>");
		File a = new File(root, "a.zip");
		File b = new File(root, "b.zip");
		File p = new File(root, "p.zip");
		assertTrue(Toolchain.compileResources(root, first, a, System.out, System.err));
		assertTrue(Toolchain.compileResources(root, second, b, System.out, System.err));
		assertTrue(Toolchain.compileResources(root, project, p, System.out, System.err));
		File manifest = applicationManifest();
		File symbols = new File(root, "R.txt");
		File ids = new File(root, "ids.txt");
		File ap = new File(root, "resources.ap_");
		String android = System.getenv("ANDROID_HOME") + "/platforms/android-30/android.jar";
		assertTrue(Toolchain.generateResourceIndex(root, ap, manifest, android, Collections.<File>emptyList(),
				symbols, root, ids, Arrays.asList(a, b), p, System.out, System.err));
		String before = Files.readString(symbols.toPath());
		assertTrue(before.contains("string dependency"));
		assertTrue(Toolchain.packageResources(root, ap, manifest, android, Collections.<File>emptyList(),
				ids, Arrays.asList(a, b), p, System.out, System.err));
		assertEquals(before, Files.readString(symbols.toPath()));
	}

	/**
	 * 创建最小应用清单，明确本项目最低 API 与目标 API。
	 *
	 * @return 应用清单文件。
	 * @throws IOException 清单无法写入。
	 */
	private File applicationManifest() throws IOException {
		File manifest = new File(root, "AndroidManifest.xml");
		write(manifest, "<manifest package=\"test.app\"" + ANDROID + "><uses-sdk android:minSdkVersion=\"24\" android:targetSdkVersion=\"30\"/><application/></manifest>");
		return manifest;
	}

	/**
	 * 相同资产允许合并，依赖内容冲突必须失败，项目显式覆盖时使用项目内容。
	 *
	 * @throws Exception 测试输入或合并失败。
	 */
	public void testAssetDeduplicationConflictAndProjectOverride() throws Exception {
		File first = new File(root, "assets-first");
		File second = new File(root, "assets-second");
		File project = new File(root, "assets-project");
		Files.createDirectories(project.toPath());
		write(new File(first, "nested/data.txt"), "same");
		write(new File(second, "nested/data.txt"), "same");
		List<File> merged = android.mergeAssets(Arrays.asList(first, second, project));
		assertEquals("same", Files.readString(new File(merged.get(0), "nested/data.txt").toPath()));
		write(new File(second, "nested/data.txt"), "different");
		try {
			android.mergeAssets(Arrays.asList(first, second, project));
			fail("依赖资产冲突不应静默通过。");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("资产冲突"));
		}
		write(new File(project, "nested/data.txt"), "project");
		merged = android.mergeAssets(Arrays.asList(first, second, project));
		assertEquals("project", Files.readString(new File(merged.get(0), "nested/data.txt").toPath()));

		// 后续输入移除资产时，新结果必须不含旧文件，关闭时释放本轮所有合并副本。
		Files.delete(new File(first, "nested/data.txt").toPath());
		Files.delete(new File(second, "nested/data.txt").toPath());
		Files.delete(new File(project, "nested/data.txt").toPath());
		merged = android.mergeAssets(Arrays.asList(first, second, project));
		assertFalse(new File(merged.get(0), "nested/data.txt").exists());
		assertEquals(1, temporaryInputDirectories().size());
		android.close();
		assertTrue(temporaryInputDirectories().isEmpty());
		assertTrue(first.isDirectory());
		assertTrue(second.isDirectory());
		assertTrue(project.isDirectory());
	}

	/**
	 * 清单合并失败也应释放此前合并的资产，原始输入和诊断文件不受清理影响。
	 *
	 * @throws Exception 输入准备失败或失败边界不符合预期。
	 */
	public void testFailedManifestMergeReleasesOwnedAssets() throws Exception {
		File first = new File(root, "failed-assets");
		File project = new File(root, "failed-project");
		write(new File(first, "data.txt"), "original");
		Files.createDirectories(project.toPath());
		List<LibraryArtifacts.AndroidLibrary> libraries = library("failed-manifest", "<manifest package=\"test.failed\""
				+ ANDROID + "><application><meta-data android:name=\"key\" android:value=\"${missing}\"/></application></manifest>", "");
		File merged = null;
		try (AndroidLibraries inputs = new AndroidLibraries(root)) {
			merged = inputs.mergeAssets(Arrays.asList(first, project)).get(0);
			inputs.mergeManifest(applicationManifest(), libraries, Collections.<String, Object>emptyMap(), "test.app", System.out, System.err);
			fail("缺失占位符的清单必须使构建失败。");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("清单合并失败"));
		}
		assertNotNull(merged);
		assertFalse(merged.exists());
		assertTrue(temporaryInputDirectories().isEmpty());
		assertEquals("original", Files.readString(new File(first, "data.txt").toPath()));
	}

	/**
	 * 清理不能穿过目录链接删除其它输入，受阻后移除链接可以重试完成释放。
	 *
	 * @throws Exception 链接夹具创建失败或清理边界不符合预期。
	 */
	public void testOwnedCleanupDoesNotTraverseDirectoryLinks() throws Exception {
		File source = new File(root, "link-source");
		File project = new File(root, "link-project");
		File outside = new File(root, "outside-owned-inputs");
		write(new File(source, "data.txt"), "source");
		write(new File(outside, "keep.txt"), "keep");
		Files.createDirectories(project.toPath());
		File merged = android.mergeAssets(Arrays.asList(source, project)).get(0);
		File link = new File(merged, "linked");

		// Windows 使用不要求符号链接权限的目录联接，其它平台使用普通目录符号链接。
		if (File.separatorChar == '\\') {
			File log = Files.createTempFile("simple-junction-test-", ".log").toFile();
			Process process = new ProcessBuilder("cmd.exe", "/c", "mklink", "/J", link.getPath(), outside.getPath())
					.redirectErrorStream(true).redirectOutput(log).start();
			if (!process.waitFor(10, TimeUnit.SECONDS)) {
				process.destroyForcibly();
				throw new IOException("创建目录联接超时，日志：" + log);
			}
			assertEquals("目录联接创建失败，日志：" + log, 0, process.exitValue());
		} else {
			Files.createSymbolicLink(link.toPath(), outside.toPath());
		}

		try {
			android.close();
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("目录链接"));
		}
		assertEquals("keep", Files.readString(new File(outside, "keep.txt").toPath()));
		if (Files.exists(link.toPath(), LinkOption.NOFOLLOW_LINKS)) {
			Files.delete(link.toPath());
		}
		android.close();
		assertTrue(temporaryInputDirectories().isEmpty());
		assertEquals("source", Files.readString(new File(source, "data.txt").toPath()));
	}

	/**
	 * 收集本用例实际创建的 Android 输入工作目录，用于核对重复创建和释放。
	 *
	 * @return 自有临时根目录列表。
	 */
	private List<File> temporaryInputDirectories() {
		List<File> result = new ArrayList<File>();
		File[] children = root.listFiles();
		assertNotNull(children);
		for (File child : children) {
			if (child.getName().startsWith("android-inputs-")) {
				result.add(child);
			}
		}
		return result;
	}

	/**
	 * 创建一个包含清单和资源符号的纯资源 AAR，避免以 classes.jar 存在性限定合法 AAR。
	 *
	 * @param name 独立交付目录名。
	 * @param manifest 清单 XML。
	 * @param symbols R.txt 内容。
	 * @return 所选的 Android 类库输入。
	 * @throws IOException 归档无法写入或读取。
	 */
	private List<LibraryArtifacts.AndroidLibrary> library(String name, String manifest, String symbols) throws IOException {
		File directory = new File(root, name);
		File archive = new File(directory, "aars/library.aar");
		Files.createDirectories(archive.getParentFile().toPath());
		try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive.toPath()))) {
			zip.putNextEntry(new ZipEntry("AndroidManifest.xml"));
			zip.write(manifest.getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
			zip.putNextEntry(new ZipEntry("R.txt"));
			zip.write(symbols.getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
		}
		return new LibraryArtifacts(Arrays.asList(directory), new File(root, "expanded-" + name))
				.getAndroidLibraries(Arrays.asList(directory));
	}

	/**
	 * 以 UTF-8 写入一个独立的测试文件。
	 *
	 * @param file 输出文件。
	 * @param text 内容。
	 * @throws IOException 目录或文件无法写入。
	 */
	private void write(File file, String text) throws IOException {
		Files.createDirectories(file.getParentFile().toPath());
		Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
	}
}

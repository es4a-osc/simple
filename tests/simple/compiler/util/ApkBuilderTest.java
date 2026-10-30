package simple.compiler.util;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.util.zip.CRC32;

/**
 * 检查 AAR 代码归档中的 Java 资源与 JNI 文件确实进入 APK，并验证同路径内容冲突。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class ApkBuilderTest extends TestCase {

	/**
	 * 所有 DEX 按标准名字进入 APK；缺失主 DEX、断号或空清单不得覆盖成功归档。
	 *
	 * @throws Exception 测试文件创建、组装或读取失败。
	 */
	public void testAllDexFilesAndRejectIncompleteInput() throws Exception {
		File parent = new File("build/tests/apk");
		Files.createDirectories(parent.toPath());
		File root = Files.createTempDirectory(parent.toPath(), "multidex-").toFile();
		File resources = new File(root, "resources.ap_");
		try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(resources.toPath()))) {
			output.putNextEntry(new ZipEntry("AndroidManifest.xml"));
			output.write(new byte[] {1});
			output.closeEntry();
		}
		File first = new File(root, "classes.dex");
		File second = new File(root, "classes2.dex");
		File third = new File(root, "classes3.dex");
		Files.write(first.toPath(), new byte[] {2});
		Files.write(second.toPath(), new byte[] {3});
		Files.write(third.toPath(), new byte[] {4});
		File apk = new File(root, "application.apk");
		ApkBuilder.build(apk, resources, Arrays.asList(first, second, third),
				Collections.<File>emptyList(), Collections.<File>emptyList());
		try (ZipFile zip = new ZipFile(apk)) {
			assertEquals(4, zip.size());
			assertEquals(2, zip.getInputStream(zip.getEntry("classes.dex")).read());
			assertEquals(3, zip.getInputStream(zip.getEntry("classes2.dex")).read());
			assertEquals(4, zip.getInputStream(zip.getEntry("classes3.dex")).read());
		}
		byte[] previous = Files.readAllBytes(apk.toPath());
		for (List<File> invalid : Arrays.asList(Collections.<File>emptyList(),
				Arrays.asList(second), Arrays.asList(first, third), Arrays.asList(first, first))) {
			try {
				ApkBuilder.build(apk, resources, invalid, Collections.<File>emptyList(), Collections.<File>emptyList());
				fail("不完整或重复的 DEX 清单应被拒绝。");
			} catch (IOException expected) {
				assertTrue(Arrays.equals(previous, Files.readAllBytes(apk.toPath())));
			}
		}
	}

	/**
	 * 相同原生库只打包一次，Java 资源随代码归档补入；不同内容的同名原生库必须失败。
	 *
	 * @throws Exception 测试文件创建、组装或校验失败。
	 */
	public void testJavaResourcesAndNativeContentConflict() throws Exception {
		File parent = new File("build/tests/apk");
		Files.createDirectories(parent.toPath());
		File root = Files.createTempDirectory(parent.toPath(), "assembly-").toFile();
		File resources = new File(root, "resources.ap_");
		try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(resources.toPath()))) {
			output.putNextEntry(new ZipEntry("AndroidManifest.xml"));
			output.write(new byte[] {1});
			output.closeEntry();
		}
		File dex = new File(root, "classes.dex");
		Files.write(dex.toPath(), new byte[] {2});
		File jar = new File(root, "dependency.jar");
		try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar.toPath()))) {
			output.putNextEntry(new ZipEntry("fixture/data.txt"));
			output.write("java resource".getBytes(StandardCharsets.UTF_8));
			output.closeEntry();
		}
		File first = new File(root, "first/arm64-v8a/libfixture.so");
		File second = new File(root, "second/arm64-v8a/libfixture.so");
		Files.createDirectories(first.getParentFile().toPath());
		Files.createDirectories(second.getParentFile().toPath());
		Files.write(first.toPath(), new byte[] {3});
		Files.write(second.toPath(), new byte[] {3});
		File apk = new File(root, "application.apk");
		ApkBuilder.build(apk, resources, Arrays.asList(dex), Arrays.asList(jar), Arrays.asList(first.getParentFile().getParentFile(), second.getParentFile().getParentFile()));
		try (ZipFile zip = new ZipFile(apk)) {
			assertNotNull(zip.getEntry("fixture/data.txt"));
			assertEquals("java resource", new String(zip.getInputStream(zip.getEntry("fixture/data.txt")).readAllBytes(), StandardCharsets.UTF_8));
			assertNotNull(zip.getEntry("lib/arm64-v8a/libfixture.so"));
			assertEquals(ZipEntry.STORED, zip.getEntry("lib/arm64-v8a/libfixture.so").getMethod());
			assertEquals(4, zip.size());
		}
		Files.write(second.toPath(), new byte[] {4});
		try {
			ApkBuilder.build(apk, resources, Arrays.asList(dex), Collections.<File>emptyList(), Arrays.asList(first.getParentFile().getParentFile(), second.getParentFile().getParentFile()));
			fail("不同内容的同名原生库不应覆盖。");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("APK 条目冲突"));
		}
	}

	/**
	 * Java 资源与已有输入同名同内容时去重，服务和许可继续合并，未压缩条目保持原方式。
	 *
	 * @throws Exception 夹具准备、归档组装或内容校验失败。
	 */
	public void testJavaResourceMergeAndExistingEntryCompression() throws Exception {
		File root = Files.createTempDirectory("simple-apk-resources-").toFile();
		File resources = new File(root, "resources.ap_");
		byte[] shared = "shared resource".getBytes(StandardCharsets.UTF_8);
		try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(resources.toPath()))) {
			writeEntry(output, "AndroidManifest.xml", new byte[] {1}, false);
			writeEntry(output, "assets/shared.txt", shared, true);
		}
		File dex = new File(root, "classes.dex");
		Files.write(dex.toPath(), new byte[] {2});
		File nativeFile = new File(root, "jni/arm64-v8a/libfixture.so");
		Files.createDirectories(nativeFile.getParentFile().toPath());
		Files.write(nativeFile.toPath(), new byte[] {3});
		File first = new File(root, "first.jar");
		File second = new File(root, "second.jar");
		String service = "META-INF/services/fixture.Service";
		try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(first.toPath()))) {
			writeEntry(output, "assets/shared.txt", shared, false);
			writeEntry(output, "classes.dex", new byte[] {2}, false);
			writeEntry(output, "lib/arm64-v8a/libfixture.so", new byte[] {3}, false);
			writeEntry(output, service, "fixture.First\n".getBytes(StandardCharsets.UTF_8), false);
			writeEntry(output, "LICENSE", "first license".getBytes(StandardCharsets.UTF_8), false);
		}
		try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(second.toPath()))) {
			writeEntry(output, service, "# comment\nfixture.Second\nfixture.First\n".getBytes(StandardCharsets.UTF_8), false);
			writeEntry(output, "LICENSE", "second license".getBytes(StandardCharsets.UTF_8), false);
		}
		byte[] originalResources = Files.readAllBytes(resources.toPath());
		byte[] originalJar = Files.readAllBytes(first.toPath());

		File apk = new File(root, "application.apk");
		ApkBuilder.build(apk, resources, Arrays.asList(dex), Arrays.asList(first, second),
				Arrays.asList(nativeFile.getParentFile().getParentFile()));
		try (ZipFile zip = new ZipFile(apk)) {
			assertEquals(6, zip.size());
			assertEquals(ZipEntry.STORED, zip.getEntry("assets/shared.txt").getMethod());
			assertEquals(ZipEntry.STORED, zip.getEntry("lib/arm64-v8a/libfixture.so").getMethod());
			assertTrue(Arrays.equals(shared, readEntry(zip, "assets/shared.txt")));
			assertTrue(Arrays.equals(new byte[] {2}, readEntry(zip, "classes.dex")));
			assertEquals("fixture.First\nfixture.Second\n", new String(readEntry(zip, service), StandardCharsets.UTF_8));
			String license = new String(readEntry(zip, "LICENSE"), StandardCharsets.UTF_8);
			assertTrue(license.contains("first.jar") && license.contains("first license"));
			assertTrue(license.contains("second.jar") && license.contains("second license"));
		}
		assertTrue(Arrays.equals(originalResources, Files.readAllBytes(resources.toPath())));
		assertTrue(Arrays.equals(originalJar, Files.readAllBytes(first.toPath())));
	}

	/**
	 * Java 资源与 Android 资源、DEX 或原生库内容冲突时保留旧 APK，并清理本轮暂存文件。
	 *
	 * @throws Exception 夹具准备、冲突诊断或旧产物校验失败。
	 */
	public void testJavaResourceConflictsPreservePreviousApk() throws Exception {
		File root = Files.createTempDirectory("simple-apk-conflicts-").toFile();
		File resources = new File(root, "resources.ap_");
		try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(resources.toPath()))) {
			writeEntry(output, "AndroidManifest.xml", new byte[] {1}, false);
			writeEntry(output, "assets/shared.txt", new byte[] {4}, false);
		}
		File dex = new File(root, "classes.dex");
		Files.write(dex.toPath(), new byte[] {2});
		File nativeFile = new File(root, "jni/arm64-v8a/libfixture.so");
		Files.createDirectories(nativeFile.getParentFile().toPath());
		Files.write(nativeFile.toPath(), new byte[] {3});
		List<File> natives = Arrays.asList(nativeFile.getParentFile().getParentFile());
		File apk = new File(root, "application.apk");
		ApkBuilder.build(apk, resources, Arrays.asList(dex), Collections.<File>emptyList(), natives);
		byte[] previous = Files.readAllBytes(apk.toPath());
		File jar = new File(root, "conflict.jar");

		for (String name : Arrays.asList("assets/shared.txt", "classes.dex", "lib/arm64-v8a/libfixture.so")) {
			try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar.toPath()))) {
				writeEntry(output, name, new byte[] {9}, false);
			}
			try {
				ApkBuilder.build(apk, resources, Arrays.asList(dex), Arrays.asList(jar), natives);
				fail("不同内容的同名 Java 资源必须报错：" + name);
			} catch (IOException expected) {
				assertTrue(expected.getMessage().contains("JAR 资源与 APK 条目冲突：" + name));
				File origin = name.startsWith("assets/") ? resources : name.startsWith("lib/") ? nativeFile : dex;
				assertTrue(expected.getMessage().contains(origin.toString()));
				assertTrue(expected.getMessage().contains(jar.toString()));
			}
			assertTrue(Arrays.equals(previous, Files.readAllBytes(apk.toPath())));
			for (File file : root.listFiles()) {
				assertFalse(file.getName().startsWith("apk-build-"));
				assertFalse(file.getName().startsWith("jar-resources-"));
			}
		}
	}

	/**
	 * 写入指定压缩方式的测试条目，未压缩输入提供完整长度和 CRC。
	 *
	 * @param output 测试归档。
	 * @param name 条目路径。
	 * @param data 原始内容。
	 * @param stored 是否使用未压缩方式。
	 * @throws IOException 条目无法写入。
	 */
	private static void writeEntry(ZipOutputStream output, String name, byte[] data, boolean stored) throws IOException {
		ZipEntry entry = new ZipEntry(name);
		if (stored) {
			CRC32 crc = new CRC32();
			crc.update(data);
			entry.setMethod(ZipEntry.STORED);
			entry.setSize(data.length);
			entry.setCrc(crc.getValue());
		}
		output.putNextEntry(entry);
		output.write(data);
		output.closeEntry();
	}

	/**
	 * 读取测试归档中的一个条目，并及时释放其输入流。
	 *
	 * @param zip 已打开的归档。
	 * @param name 预期条目路径。
	 * @return 条目原始内容。
	 * @throws IOException 条目无法读取。
	 */
	private static byte[] readEntry(ZipFile zip, String name) throws IOException {
		try (InputStream input = zip.getInputStream(zip.getEntry(name))) {
			return input.readAllBytes();
		}
	}
}

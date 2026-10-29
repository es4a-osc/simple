package simple.compiler.util;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

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
}

package simple.compiler.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import junit.framework.TestCase;

/**
 * 验证不依赖旧 Android 工具的 APK 组装及失败时的输入、输出保护。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class ApkBuilderTest extends TestCase {

	private Path root;
	private File resources;
	private File dex;
	private File output;

	/**
	 * 创建包含中文和空格的独立目录，准备最小资源包及 DEX 输入。
	 *
	 * @throws Exception 文件或归档创建失败。
	 */
	@Override
	protected void setUp() throws Exception {
		Path base = new File("build/tests/apk-builder").toPath().toAbsolutePath();
		Files.createDirectories(base);
		root = Files.createTempDirectory(base, "中文 组装-");
		resources = archive("资源.ap_", "assets/中文 资产.txt", "asset", "resources.arsc", "table");
		dex = file("输入.dex", "dex");
		output = root.resolve("输出.apk").toFile();
	}

	/**
	 * 资源、固定名称的 DEX、服务文件与 ABI 原生库进入同一个未签名 APK。
	 *
	 * @throws Exception 测试文件读写或组装失败。
	 */
	public void testResourcesDexServicesAndNativeLibraries() throws Exception {
		File first = archive("first.jar", "META-INF/services/probe.Service", "probe.One\n");
		File second = archive("second.jar", "META-INF/services/probe.Service", "probe.Two\n");
		file("jni/arm64-v8a/libone.so", "one");
		file("jni/x86/libtwo.SO", "two");
		file("jni/arm64-v8a/gdbserver", "ignored");
		file("jni/arm64-v8a/readme.txt", "ignored");
		file("jni/arm64-v8a/.so", "ignored");
		file("jni/arm64-v8a/nested/libignored.so", "ignored");
		file("jni/root.so", "ignored");
		byte[] originalResources = Files.readAllBytes(resources.toPath());
		byte[] originalJar = Files.readAllBytes(first.toPath());

		ApkBuilder.build(output, resources, dex, Arrays.asList(first, second),
				Collections.singletonList(root.resolve("jni").toFile()));
		try (ZipFile zip = new ZipFile(output)) {
			assertEquals("asset", text(zip, "assets/中文 资产.txt"));
			assertEquals("table", text(zip, "resources.arsc"));
			assertEquals("dex", text(zip, "classes.dex"));
			assertEquals("one", text(zip, "lib/arm64-v8a/libone.so"));
			assertEquals("two", text(zip, "lib/x86/libtwo.SO"));
			assertEquals("probe.One\nprobe.Two\n", text(zip, "META-INF/services/probe.Service"));
			assertEquals(ZipEntry.STORED, zip.getEntry("resources.arsc").getMethod());
			assertEquals(ZipEntry.DEFLATED, zip.getEntry("lib/arm64-v8a/libone.so").getMethod());
			assertNull(zip.getEntry("输入.dex"));
			assertNull(zip.getEntry("lib/arm64-v8a/gdbserver"));
			assertNull(zip.getEntry("lib/arm64-v8a/readme.txt"));
			assertNull(zip.getEntry("lib/arm64-v8a/.so"));
			assertNull(zip.getEntry("lib/arm64-v8a/nested/libignored.so"));
			assertNull(zip.getEntry("lib/root.so"));
		}
		assertTrue(Arrays.equals(originalResources, Files.readAllBytes(resources.toPath())));
		assertTrue(Arrays.equals(originalJar, Files.readAllBytes(first.toPath())));
		assertEquals("dex", new String(Files.readAllBytes(dex.toPath()), StandardCharsets.UTF_8));
		assertNoTemporaryFiles();
	}

	/**
	 * 多个原生库来源写入同一归档路径时明确失败，不覆盖先前成功的 APK。
	 *
	 * @throws Exception 测试文件读写失败。
	 */
	public void testNativeConflictPreservesPreviousOutput() throws Exception {
		File first = file("first/arm64-v8a/libsame.so", "one");
		File second = file("second/arm64-v8a/libsame.so", "two");
		Files.write(output.toPath(), "previous".getBytes(StandardCharsets.UTF_8));
		try {
			ApkBuilder.build(output, resources, dex, Collections.<File>emptyList(), Arrays.asList(
					root.resolve("first").toFile(), root.resolve("second").toFile()));
			fail("重复原生库必须失败");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains(first.toString()));
			assertTrue(expected.getMessage().contains(second.toString()));
		}
		assertEquals("previous", new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8));
		assertNoTemporaryFiles();
	}

	/**
	 * Java 资源与基础归档冲突时，整个组装失败也不能替换原输出。
	 *
	 * @throws Exception 测试文件读写失败。
	 */
	public void testJavaResourceConflictPreservesPreviousOutput() throws Exception {
		File jar = archive("conflict.jar", "assets/中文 资产.txt", "different");
		Files.write(output.toPath(), "previous".getBytes(StandardCharsets.UTF_8));
		try {
			ApkBuilder.build(output, resources, dex, Collections.singletonList(jar), Collections.<File>emptyList());
			fail("冲突不能发布半成品");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("assets/中文 资产.txt"));
		}
		assertEquals("previous", new String(Files.readAllBytes(output.toPath()), StandardCharsets.UTF_8));
		assertNoTemporaryFiles();
	}

	/**
	 * 资源包损坏时给出来源，不发布新 APK 或遗留临时文件。
	 *
	 * @throws Exception 测试文件读写失败。
	 */
	public void testBrokenResourcesFail() throws Exception {
		Files.write(resources.toPath(), new byte[] {1, 2, 3});
		try {
			ApkBuilder.build(output, resources, dex, Collections.<File>emptyList(), Collections.<File>emptyList());
			fail("损坏资源包必须失败");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains(resources.toString()));
		}
		assertFalse(output.exists());
		assertNoTemporaryFiles();
	}

	/**
	 * 缺少 DEX 时必须失败，不能留下看似有效的资源 APK。
	 *
	 * @throws Exception 测试输入读取失败。
	 */
	public void testMissingDexFails() throws Exception {
		File missing = root.resolve("missing.dex").toFile();
		try {
			ApkBuilder.build(output, resources, missing, Collections.<File>emptyList(), Collections.<File>emptyList());
			fail("缺少 DEX 必须失败");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains(missing.toString()));
		}
		assertFalse(output.exists());
		assertNoTemporaryFiles();
	}

	/**
	 * 非规范的资源包条目必须拒绝，不能进入最终归档。
	 *
	 * @throws Exception 测试归档创建失败。
	 */
	public void testNonCanonicalResourcePathFails() throws Exception {
		File bad = archive("bad.ap_", "../outside.txt", "bad");
		try {
			ApkBuilder.build(output, bad, dex, Collections.<File>emptyList(), Collections.<File>emptyList());
			fail("不能接受回退路径");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("../outside.txt"));
		}
		assertFalse(output.exists());
		assertNoTemporaryFiles();
	}

	/**
	 * 调用方误把输入作为输出时，在创建临时归档之前拒绝操作。
	 *
	 * @throws Exception 测试输入读取失败。
	 */
	public void testOutputCannotOverwriteInput() throws Exception {
		byte[] before = Files.readAllBytes(resources.toPath());
		try {
			ApkBuilder.build(resources, resources, dex, Collections.<File>emptyList(), Collections.<File>emptyList());
			fail("输出不能覆盖输入");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains(resources.toString()));
		}
		assertTrue(Arrays.equals(before, Files.readAllBytes(resources.toPath())));
		assertNoTemporaryFiles();
	}

	/**
	 * 创建文本测试输入，并按需创建其上级目录。
	 *
	 * @param name 相对测试目录的路径。
	 * @param value UTF-8 文本内容。
	 * @return 已写入的文件。
	 * @throws IOException 目录或文件写入失败。
	 */
	private File file(String name, String value) throws IOException {
		Path path = root.resolve(name);
		Files.createDirectories(path.getParent());
		Files.write(path, value.getBytes(StandardCharsets.UTF_8));
		return path.toFile();
	}

	/**
	 * 创建测试归档，资源表使用未压缩方式，其它条目使用压缩方式。
	 *
	 * @param name 归档文件名。
	 * @param entries 成对的归档路径与内容。
	 * @return 已创建的归档。
	 * @throws IOException 归档写入失败。
	 */
	private File archive(String name, String... entries) throws IOException {
		File file = root.resolve(name).toFile();
		try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file.toPath()))) {
			for (int index = 0; index < entries.length; index += 2) {
				byte[] data = entries[index + 1].getBytes(StandardCharsets.UTF_8);
				ZipEntry entry = new ZipEntry(entries[index]);
				if (entries[index].equals("resources.arsc")) {
					CRC32 crc = new CRC32();
					crc.update(data);
					entry.setMethod(ZipEntry.STORED);
					entry.setSize(data.length);
					entry.setCrc(crc.getValue());
				}
				zip.putNextEntry(entry);
				zip.write(data);
				zip.closeEntry();
			}
		}
		return file;
	}

	/**
	 * 读取单个归档条目，便于验证组装后内容逐字保留。
	 *
	 * @param zip 已打开的归档。
	 * @param name 需要读取的条目。
	 * @return UTF-8 文本。
	 * @throws IOException 条目读取失败。
	 */
	private String text(ZipFile zip, String name) throws IOException {
		ZipEntry entry = zip.getEntry(name);
		assertNotNull(name, entry);
		try (InputStream input = zip.getInputStream(entry)) {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			for (int count; (count = input.read(buffer)) != -1;) {
				bytes.write(buffer, 0, count);
			}
			return bytes.toString("UTF-8");
		}
	}

	/**
	 * 检查组装及 Java 资源合并使用的临时文件均已清理。
	 */
	private void assertNoTemporaryFiles() {
		for (File file : root.toFile().listFiles()) {
			assertFalse(file.toString(), file.getName().startsWith("apk-build-"));
			assertFalse(file.toString(), file.getName().startsWith("jar-resources-"));
		}
	}
}

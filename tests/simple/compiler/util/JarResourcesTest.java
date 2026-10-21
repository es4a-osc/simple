package simple.compiler.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.ServiceLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import junit.framework.TestCase;

/**
 * 验证通用 Java 资源合并、服务注册、输入保护及真实 APK 组装入口。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class JarResourcesTest extends TestCase {

	private Path root;

	/**
	 * 为每个用例创建带中文和空格的独立构建目录。
	 *
	 * @throws Exception 目录创建失败。
	 */
	@Override
	protected void setUp() throws Exception {
		Path base = new File("build/tests/jar-resources").toPath().toAbsolutePath();
		Files.createDirectories(base);
		root = Files.createTempDirectory(base, "中文 资源-");
	}

	/**
	 * 排除 Maven 构建元数据，保留服务、运行资源和许可，且不修改原 JAR 或基础 APK 内容。
	 *
	 * @throws Exception 归档创建或校验失败。
	 */
	public void testServicesAndResourcesArePreserved() throws Exception {
		File first = jar("first.jar", "META-INF/services/probe.Service", "# providers\nprobe.One\n",
				"META-INF/runtime.properties", "enabled=true", "META-INF/LICENSE.txt", "first license",
				"META-INF/MANIFEST.MF", "old manifest", "META-INF/OLD.SF", "old signature",
				"META-INF/versions/9/probe/data.txt", "versioned", "probe/Only.class", "class",
				"META-INF/maven/probe/core/pom.properties", "version=1",
				"META-INF/maven/probe/core/pom.xml", "<project/>",
				"META-INF/maven-extra/pom.xml", "runtime-config");
		File second = jar("second.jar", "META-INF/services/probe.Service", "probe.One\r\nprobe.Two # more\r\n",
				"META-INF/LICENSE.txt", "second license", "probe/config.properties", "value=1",
				"META-INF/maven/probe/core/pom.properties", "version=2");
		byte[] original = Files.readAllBytes(first.toPath());
		File apk = apk();

		new JarResources(Arrays.asList(first, second)).addToApk(apk);
		assertEquals("probe.One\nprobe.Two\n", text(apk, "META-INF/services/probe.Service"));
		assertEquals("enabled=true", text(apk, "META-INF/runtime.properties"));
		assertEquals("runtime-config", text(apk, "META-INF/maven-extra/pom.xml"));
		assertEquals("value=1", text(apk, "probe/config.properties"));
		String license = text(apk, "META-INF/LICENSE.txt");
		assertTrue(license.contains("first license"));
		assertTrue(license.contains("second license"));
		assertTrue(license.contains("first.jar"));
		assertTrue(Arrays.equals(original, Files.readAllBytes(first.toPath())));

		// Maven 元数据在冲突检查前被排除；相邻目录资源和原生库规则不受影响。
		try (ZipFile zip = new ZipFile(apk)) {
			assertNull(zip.getEntry("META-INF/MANIFEST.MF"));
			assertNull(zip.getEntry("META-INF/OLD.SF"));
			assertNull(zip.getEntry("META-INF/maven/probe/core/pom.properties"));
			assertNull(zip.getEntry("META-INF/maven/probe/core/pom.xml"));
			assertNull(zip.getEntry("META-INF/versions/9/probe/data.txt"));
			assertNull(zip.getEntry("probe/Only.class"));
			assertEquals(ZipEntry.STORED, zip.getEntry("lib/test/libprobe.so").getMethod());
		}
		assertEquals("native-content", text(apk, "lib/test/libprobe.so"));
		assertEquals("dex-content", text(apk, "classes.dex"));
	}

	/**
	 * 内容相同的普通资源只保留一份，同名输入 JAR 也只处理一次。
	 *
	 * @throws Exception 测试归档读写失败。
	 */
	public void testIdenticalResourcesAreDeduplicated() throws Exception {
		File first = jar("first.jar", "probe/config.properties", "same");
		File second = jar("second.jar", "probe/config.properties", "same");
		File apk = apk();
		new JarResources(Arrays.asList(first, second, first)).addToApk(apk);
		assertEquals("same", text(apk, "probe/config.properties"));
	}

	/**
	 * 普通资源同名但内容不同时，明确报告双方 JAR，不按顺序覆盖。
	 *
	 * @throws Exception 测试归档创建失败。
	 */
	public void testConflictingResourcesFail() throws Exception {
		File first = jar("first.jar", "probe/config.properties", "one");
		File second = jar("second.jar", "probe/config.properties", "two");
		try {
			new JarResources(Arrays.asList(first, second));
			fail("必须报告普通资源冲突");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("first.jar"));
			assertTrue(expected.getMessage().contains("second.jar"));
			assertTrue(expected.getMessage().contains("probe/config.properties"));
		}
	}

	/**
	 * JAR 资源不能覆盖 APK 的既有条目，失败后保持原 APK 字节不变并清理副本。
	 *
	 * @throws Exception 测试文件读写失败。
	 */
	public void testApkConflictPreservesInput() throws Exception {
		File apk = apk();
		byte[] original = Files.readAllBytes(apk.toPath());
		File jar = jar("collision.jar", "classes.dex", "different");
		try {
			new JarResources(Collections.singletonList(jar)).addToApk(apk);
			fail("不能覆盖 APK 条目");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("classes.dex"));
		}
		assertTrue(Arrays.equals(original, Files.readAllBytes(apk.toPath())));
		assertEquals(0, root.toFile().listFiles(new java.io.FilenameFilter() {
			/**
			 * 查找本次 APK 合并使用的临时副本。
			 *
			 * @param directory 测试目录。
			 * @param name 文件名。
			 * @return 临时副本返回 {@code true}。
			 */
			@Override
			public boolean accept(File directory, String name) {
				return name.startsWith("jar-resources-");
			}
		}).length);
	}

	/**
	 * 无效服务提供者不是可屏蔽的提示，必须在组装前失败。
	 *
	 * @throws Exception 测试归档创建失败。
	 */
	public void testInvalidServiceConfigurationFails() throws Exception {
		File jar = jar("bad-service.jar", "META-INF/services/probe.Service", "not a class");
		try {
			new JarResources(Collections.singletonList(jar));
			fail("必须报告服务配置错误");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("bad-service.jar"));
		}
	}

	/**
	 * 资源路径不进行猜测修正，含回退路径的归档条目必须拒绝。
	 *
	 * @throws Exception 测试归档创建失败。
	 */
	public void testNonCanonicalPathFails() throws Exception {
		File jar = jar("bad-path.jar", "../config.txt", "bad");
		try {
			new JarResources(Collections.singletonList(jar));
			fail("必须拒绝非规范路径");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("../config.txt"));
		}
	}

	/**
	 * 从实际组装入口确认服务文件进入 APK，不只测试独立的合并辅助类。
	 *
	 * @throws Exception 测试输入和诊断创建失败。
	 */
	public void testBuildApplicationIncludesJavaResources() throws Exception {
		File resources = apk();
		File dex = root.resolve("classes.dex").toFile();
		Files.write(dex.toPath(), "dex-content".getBytes(StandardCharsets.UTF_8));
		File jar = jar("services.jar", "META-INF/services/probe.Service", "probe.One");
		File output = root.resolve("assembled.apk").toFile();
		ByteArrayOutputStream errors = new ByteArrayOutputStream();
		PrintStream log = new PrintStream(errors, true, "UTF-8");

		// 资源包不含 DEX 和原生库，避免用例本身制造重复条目。
		try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(resources.toPath()))) {
			zip.putNextEntry(new ZipEntry("assets/probe.txt"));
			zip.write("asset-content".getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
		}
		assertTrue(errors.toString("UTF-8"), Toolchain.buildApplication(output, resources, dex,
				Collections.singletonList(jar), Collections.<File>emptyList(), log));
		assertEquals("probe.One\n", text(output, "META-INF/services/probe.Service"));
		assertEquals("asset-content", text(output, "assets/probe.txt"));
		assertEquals("dex-content", text(output, "classes.dex"));
	}

	/**
	 * 验证 APK 中合并后的注册文件可以被标准 ServiceLoader 实际发现。
	 *
	 * @throws Exception 归档读写或服务实例化失败。
	 */
	public void testMergedServicesCanBeLoaded() throws Exception {
		String descriptor = "META-INF/services/" + ProbeService.class.getName();
		File first = jar("first.jar", descriptor, OneProvider.class.getName());
		File second = jar("second.jar", descriptor,
				OneProvider.class.getName() + "\n" + TwoProvider.class.getName());
		File apk = apk();
		new JarResources(Arrays.asList(first, second)).addToApk(apk);

		// 类型由父加载器提供，服务注册必须从本次生成的 APK 读取。
		try (URLClassLoader loader = new URLClassLoader(new URL[] {apk.toURI().toURL()},
				JarResourcesTest.class.getClassLoader())) {
			StringBuilder names = new StringBuilder();
			for (ProbeService service : ServiceLoader.load(ProbeService.class, loader)) {
				names.append(service.name()).append(',');
			}
			assertEquals("one,two,", names.toString());
		}
	}

	/**
	 * 供服务发现用例使用的最小接口，不绑定任何第三方日志或解析库。
	 */
	public interface ProbeService {

		/**
		 * 返回用例可以区分的提供者标识。
		 *
		 * @return 提供者标识。
		 */
		String name();
	}

	/**
	 * 第一份服务注册指向的公开实现。
	 */
	public static final class OneProvider implements ProbeService {

		/**
		 * 返回第一份实现的标识。
		 *
		 * @return 固定标识 one。
		 */
		@Override
		public String name() {
			return "one";
		}
	}

	/**
	 * 第二份服务注册指向的公开实现。
	 */
	public static final class TwoProvider implements ProbeService {

		/**
		 * 返回第二份实现的标识。
		 *
		 * @return 固定标识 two。
		 */
		@Override
		public String name() {
			return "two";
		}
	}

	/**
	 * 创建 UTF-8 测试 JAR，参数依次为条目名和内容。
	 *
	 * @param name JAR 文件名。
	 * @param entries 成对的条目名与文本。
	 * @return 测试 JAR。
	 * @throws IOException 归档写入失败。
	 */
	private File jar(String name, String... entries) throws IOException {
		File file = root.resolve(name).toFile();
		try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(file.toPath()))) {
			for (int index = 0; index < entries.length; index += 2) {
				output.putNextEntry(new JarEntry(entries[index]));
				output.write(entries[index + 1].getBytes(StandardCharsets.UTF_8));
				output.closeEntry();
			}
		}
		return file;
	}

	/**
	 * 创建包含 DEX 和未压缩原生库的 APK 归档，供资源合并测试使用。
	 *
	 * @return 未签名测试归档。
	 * @throws IOException 归档写入失败。
	 */
	private File apk() throws IOException {
		File file = root.resolve("input.apk").toFile();
		try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(file.toPath()))) {
			output.putNextEntry(new ZipEntry("classes.dex"));
			output.write("dex-content".getBytes(StandardCharsets.UTF_8));
			output.closeEntry();

			byte[] nativeData = "native-content".getBytes(StandardCharsets.UTF_8);
			CRC32 crc = new CRC32();
			crc.update(nativeData);
			ZipEntry nativeEntry = new ZipEntry("lib/test/libprobe.so");
			nativeEntry.setMethod(ZipEntry.STORED);
			nativeEntry.setSize(nativeData.length);
			nativeEntry.setCrc(crc.getValue());
			output.putNextEntry(nativeEntry);
			output.write(nativeData);
			output.closeEntry();
		}
		return file;
	}

	/**
	 * 读取测试归档中的单个资源文本。
	 *
	 * @param archive 测试归档。
	 * @param name 归档内路径。
	 * @return UTF-8 文本。
	 * @throws IOException 条目不存在或读取失败。
	 */
	private String text(File archive, String name) throws IOException {
		try (ZipFile zip = new ZipFile(archive)) {
			ZipEntry entry = zip.getEntry(name);
			assertNotNull(name, entry);
			try (java.io.InputStream input = zip.getInputStream(entry)) {
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				byte[] buffer = new byte[8192];
				for (int count; (count = input.read(buffer)) != -1;) {
					output.write(buffer, 0, count);
				}
				return output.toString("UTF-8");
			}
		}
	}
}

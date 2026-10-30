package simple.compiler;

import junit.framework.TestCase;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

/**
 * 验证统一准备的 Java 依赖可以独立加载，并与各平台分发保持一致。
 *
 * <p>AGP 的真实加载由 Gradle 依赖解析和类库构建测试验证；不从 SDK 旧交付借用依赖。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class ToolDistributionTest extends TestCase {

	/** 编译器直接使用的第三方入口类型。 */
	private static final String[] TYPES = {
		"com.android.manifmerger.ManifestMerger2", "com.android.ide.common.symbols.SymbolIo",
		"com.google.gson.Gson", "org.objectweb.asm.ClassReader", "org.apache.maven.artifact.versioning.ComparableVersion"
	};

	/**
	 * 同一轮分发必须携带与源码构建完全相同的依赖，ZIP 中也不能遗漏。
	 *
	 * @throws Exception 分发、归档或内容摘要无法读取。
	 */
	public void testDistributionContainsMatchingDependencies() throws Exception {
		File[] libraries = new File("libs").listFiles();
		assertNotNull("请先准备源码依赖", libraries);
		for (String platform : new String[] {"common", "windows", "linux", "mac"}) {
			File distribution = new File("dist", platform);
			assertFalse("不保留旧工具包布局", new File(distribution, "tools").exists());
			assertTrue(new File(distribution, "SimpleCompiler.jar").isFile());
			assertTrue(new File(distribution, "SimpleAndroidRuntime.jar").isFile());
			assertTrue(new File(distribution, "gradle/build.gradle").isFile());
			for (File jar : libraries) {
				if (jar.getName().endsWith(".jar")) {
					File delivered = new File(distribution, "libs/" + jar.getName());
					assertTrue("分发缺少依赖：" + delivered, delivered.isFile());
					assertEquals(LibraryArtifacts.digest(jar), LibraryArtifacts.digest(delivered));
				}
			}
		}
		for (File archive : new File("dist/zips").listFiles()) {
			try (ZipFile zip = new ZipFile(archive)) {
				for (File jar : libraries) {
					if (jar.getName().endsWith(".jar")) {
						assertNotNull("ZIP 缺少依赖：" + jar.getName(), zip.getEntry("libs/" + jar.getName()));
					}
				}
			}
		}
	}

	/**
	 * 只使用源码 libs 加载第三方入口，防止误用测试进程或 SDK 中的旧包。
	 *
	 * @throws Exception 独立类加载或公开方法签名无法解析。
	 */
	public void testDependenciesLoadFromSourceLibraries() throws Exception {
		List<URL> urls = new ArrayList<URL>();
		for (File jar : new File("libs").listFiles()) {
			if (jar.getName().endsWith(".jar")) {
				urls.add(jar.toURI().toURL());
			}
		}
		try (URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getPlatformClassLoader())) {
			for (String name : TYPES) {
				Class<?> type = Class.forName(name, true, loader);
				assertSame("不能从测试类路径借用依赖", loader, type.getClassLoader());
				assertTrue(type.getDeclaredMethods().length > 0);
			}
		}
	}

	/**
	 * AGP 和编译器共用统一选版结果，不同时携带已被替代的 Gson、ASM 核心版本。
	 */
	public void testUnifiedVersionsReplaceConflictingCandidates() {
		File libraries = new File("libs");
		assertTrue(new File(libraries, "gradle-7.4.2.jar").isFile());
		assertTrue(new File(libraries, "gson-2.10.1.jar").isFile());
		assertTrue(new File(libraries, "asm-9.7.1.jar").isFile());
		assertFalse(new File(libraries, "gson-2.8.9.jar").exists());
		assertFalse(new File(libraries, "asm-9.2.jar").exists());
	}
}

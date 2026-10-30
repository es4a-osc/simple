package simple.compiler;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import simple.compiler.util.MavenRepository;
import junit.framework.TestCase;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 从正式编译器 JAR 验证 SDK 外部脚本、Gradle 依赖规则和解析缓存。
 *
 * <p>依赖输入来自隔离的本地 Maven 仓库，测试不访问业务类库或修改 SDK 配置。
 * Gradle 与 AGP 使用 SDK 离线工具和空用户缓存，全程以离线模式解析。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class DependencyResolverTest extends TestCase {

	/** 本仓库维护的解析工程文件，分发与隔离 SDK 使用同一份源码。 */
	private static final String[] PROJECT_FILES = {"settings.gradle", "build.gradle", "gradle.properties"};

	/** 当前用例的生成目录。 */
	private File root;

	/** 系统临时目录中的隔离 SDK，仅携带配套解析脚本。 */
	private File sdk;

	/** 当前用例临时引用真实工具的目录链接，结束时仅删除链接本身。 */
	private File toolsLink;

	/** 系统临时目录中的空 Gradle 用户缓存，避免同步软件介入工具缓存。 */
	private File gradleUserHome;

	/** 用例专用的 Maven 仓库。 */
	private File repository;

	/** 模拟应用的依赖构建目录。 */
	private File work;

	/** 本次构建生成的正式编译器 JAR。 */
	private File compiler;

	/**
	 * 为每个用例建立独立的脚本和生成目录，保留中文及空格路径覆盖。
	 *
	 * @throws Exception 目录或工具链配置无法读取、创建。
	 */
	@Override
	protected void setUp() throws Exception {
		File parent = new File("build/tests/dependency-resolution");
		Files.createDirectories(parent.toPath());
		root = Files.createTempDirectory(parent.toPath(), "解析 中文 空格-").toFile().getCanonicalFile();
		sdk = Files.createTempDirectory("simple-sdk-中文 空格-").toFile().getCanonicalFile();
		gradleUserHome = Files.createTempDirectory("simple-gradle-home-").toFile().getCanonicalFile();
		repository = new File(root, "repository");
		work = new File(root, "application/build/dependencies");
		File distributionCompiler = new File("dist/common/SimpleCompiler.jar").getCanonicalFile();
		assertTrue("请先生成正式编译器 JAR", distributionCompiler.isFile());
		Files.createDirectories(new File(sdk, "simple").toPath());
		compiler = new File(sdk, "simple/SimpleCompiler.jar");
		Files.copy(distributionCompiler.toPath(), compiler.toPath());
		File libraries = new File(sdk, "simple/libs");
		Files.createDirectories(libraries.toPath());
		for (File jar : new File("libs").listFiles()) {
			if (jar.isFile() && jar.getName().endsWith(".jar")) {
				Files.copy(jar.toPath(), new File(libraries, jar.getName()).toPath());
			}
		}

		Files.createDirectories(repository.toPath());
		File project = new File(sdk, "simple/gradle");
		Files.createDirectories(project.toPath());
		for (String name : PROJECT_FILES) {
			Files.copy(new File("gradle/resolver", name).toPath(), new File(project, name).toPath());
		}
	}

	/**
	 * 结束用例时移除临时工具链接，避免构建目录清理误遍历真实 SDK。
	 *
	 * @throws Exception 链接无法移除。
	 */
	@Override
	protected void tearDown() throws Exception {
		try {
			if (toolsLink != null) {
				Files.deleteIfExists(toolsLink.toPath());
			}
		} finally {
			super.tearDown();
		}
	}

	/**
	 * 保留 SDK 固定 tools 目录契约，不复制整套 JDK、Gradle 或 Android 平台。
	 * 链接仅建在系统临时目录，不进入 Simple 构建目录或正式 SDK。
	 *
	 * @throws Exception 工具不存在或系统无法创建目录链接。
	 */
	private void linkSdkTools() throws Exception {
		File source = new File("../sdk/tools").getCanonicalFile();
		toolsLink = new File(sdk, "tools");
		if (File.separatorChar == '\\') {
			Process process = new ProcessBuilder("cmd.exe", "/d", "/c", "mklink", "/J",
					toolsLink.getPath(), source.getPath()).redirectErrorStream(true).start();
			byte[] output = process.getInputStream().readAllBytes();
			if (process.waitFor() != 0) {
				throw new IOException("无法创建测试工具链接：" + toolsLink + "\n"
						+ new String(output, StandardCharsets.UTF_8));
			}
		} else {
			Files.createSymbolicLink(toolsLink.toPath(), source.toPath());
		}
	}

	/**
	 * 正式 JAR 不嵌入脚本，各平台分发在编译器旁携带本仓库维护的解析工程。
	 *
	 * @throws Exception 分发无法读取或脚本内容不完整。
	 */
	public void testDistributionKeepsResolverScriptsOutsideJar() throws Exception {
		try (ZipFile zip = new ZipFile(compiler)) {
			assertNull(zip.getEntry("simple/compiler/resources/dependencies/"));
			for (String name : new String[] {"settings.template.gradle", "resolve.gradle", "gradle.properties"}) {
				assertNull(zip.getEntry("simple/compiler/resources/dependencies/" + name));
			}
		}
		for (String platform : new String[] {"common", "windows", "linux", "mac"}) {
			assertFalse(new File("dist/" + platform + "/gradle/toolchain.json").exists());
			for (String name : PROJECT_FILES) {
				File script = new File("dist/" + platform + "/gradle", name);
				assertTrue("分发缺少解析脚本：" + script, script.isFile());
				assertTrue(Arrays.equals(Files.readAllBytes(script.toPath()),
						Files.readAllBytes(new File("gradle/resolver", name).toPath())));
			}
		}
	}

	/**
	 * 无模块依赖时直接返回空结果，不要求 SDK 解析脚本存在。
	 *
	 * @throws Exception 解析操作意外失败。
	 */
	public void testLocalOnlyDependenciesDoNotPrepareGradleProject() throws Exception {
		JsonObject result = DependencyResolver.resolve(new ArrayList<File>(), work);
		assertEquals(0, result.getAsJsonArray("dependencies").size());
		assertFalse(work.exists());
		for (String name : PROJECT_FILES) {
			Files.delete(new File(sdk, "simple/gradle/" + name).toPath());
		}
		File library = writeLibrary("fixture.local", "[]", "[]");
		result = DependencyResolver.resolve(Arrays.asList(library), work);
		assertEquals(0, result.getAsJsonArray("dependencies").size());
		assertFalse(work.exists());
	}

	/**
	 * 模块声明必须与标准清单身份一致，缺失清单不能退回目录名。
	 *
	 * @throws Exception 测试输入无法准备。
	 */
	public void testModuleRequiresMatchingStandardManifest() throws Exception {
		File library = writeLibrary("fixture.identity", "[]", "[]");
		File manifest = new File(library, "AndroidManifest.xml");
		Files.writeString(manifest.toPath(), "<manifest package=\"fixture.other\" />", StandardCharsets.UTF_8);
		try {
			DependencyResolver.resolve(Arrays.asList(library), work);
			fail("模块和清单身份不同必须失败。");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("模块身份无效"));
		}
		Files.delete(manifest.toPath());
		try {
			DependencyResolver.resolve(Arrays.asList(library), work);
			fail("模块缺失标准清单必须失败。");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("AndroidManifest.xml"));
		}
		assertFalse(work.exists());
	}

	/**
	 * 无诊断时只输出中文进度；警告原文归档并提示，失败详情完整回显且不误报成功。
	 *
	 * @throws Exception 测试进程无法执行、输出内容丢失或失败未正确传播。
	 */
	public void testGradleOutputPreservesWarningsAndFailureDetails() throws Exception {
		File log = new File(root, "gradle.log");
		for (String mode : new String[] {"success", "warning", "failure"}) {
			ByteArrayOutputStream progress = new ByteArrayOutputStream();
			ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
			ProcessBuilder builder = new ProcessBuilder(
					new File(System.getProperty("java.home"), "bin/java").getPath(), "-Dfile.encoding=UTF-8",
					"-cp", System.getProperty("java.class.path"), OutputProbe.class.getName(), mode);
			try (PrintStream out = new PrintStream(progress, true, "UTF-8");
					PrintStream err = new PrintStream(diagnostics, true, "UTF-8")) {
				try {
					DependencyResolver.runGradle(builder, log, out, err);
					assertFalse("失败进程不能正常返回", "failure".equals(mode));
				} catch (IOException failure) {
					assertEquals("failure", mode);
					assertTrue(failure.getMessage().contains("退出码：7"));
					assertTrue(failure.getMessage().contains(log.getAbsolutePath()));
				}
			}

			String output = progress.toString("UTF-8");
			String diagnostic = diagnostics.toString("UTF-8");
			String complete = Files.readString(log.toPath(), StandardCharsets.UTF_8);
			assertEquals("正在准备依赖：Gradle" + System.lineSeparator(), output);
			if ("success".equals(mode)) {
				assertEquals("", diagnostic);
				assertEquals("", complete);
			} else {
				String expected = "warning".equals(mode) ? "测试警告：依赖选项需要注意" : "测试错误：依赖无法解析";
				assertTrue(complete, complete.contains(expected));
				assertTrue(diagnostic.contains(log.getName()));
				if ("failure".equals(mode)) {
					assertTrue(diagnostic, diagnostic.contains(expected));
					assertTrue(diagnostic.contains("失败上下文：中文与空格路径"));
					assertTrue(complete.contains("失败上下文：中文与空格路径"));
				} else {
					assertEquals("依赖准备提示：" + log.getName() + System.lineSeparator(), diagnostic);
				}
			}
		}
	}

	/**
	 * 离线消费原生元数据，并验证版本选择、排除、约束、缓存和外部脚本变化。
	 *
	 * @throws Exception 夹具创建、子进程执行或解析结果校验失败。
	 */
	public void testSdkResolverPreservesDependencyRulesAndCache() throws Exception {
		// 固定工具布局通过临时链接复用；隔离脚本只为本用例增加本地 Maven 仓库。
		linkSdkTools();
		File settings = new File(sdk, "simple/gradle/settings.gradle");
		String scriptText = Files.readString(settings.toPath(), StandardCharsets.UTF_8);
		scriptText += "\r\ndependencyResolutionManagement.repositories.maven { url = uri('"
				+ repository.toURI().toASCIIString() + "') }\r\n";
		Files.writeString(settings.toPath(), scriptText, StandardCharsets.UTF_8);
		// 本地模块模拟传递依赖，避免测试是否成功取决于线上仓库状态。
		String helper = "<dependencies><dependency><groupId>fixture</groupId><artifactId>helper</artifactId>"
				+ "<version>1.0</version></dependency></dependencies>";
		writeMavenModule("shared", "1.0", helper);
		writeMavenModule("shared", "2.0", helper);
		writeMavenModule("helper", "1.0", "");
		writeMavenModule("helper", "2.0", "");
		File first = writeLibrary("fixture.first", "[{\"group\":\"fixture\",\"module\":\"shared\","
				+ "\"version\":{\"requires\":\"1.0\"},\"excludes\":[{\"group\":\"fixture\",\"module\":\"helper\"}]}]", "[]");
		assertFalse("应用依赖解析不能要求扩展库开发工具", new File(sdk, "utils").exists());
		List<String> initialSdk = sdkSnapshot();
		String firstLog = runResolver(compiler, true, first);
		assertEquals("首次解析不得改写 SDK", initialSdk, sdkSnapshot());
		assertTrue(firstLog.contains("正在准备依赖：Gradle"));
		assertFalse(firstLog.contains("扩展库依赖准备完成"));
		assertFalse(firstLog.contains(":resolveApplicationDependencies"));
		assertFalse(firstLog.contains("BUILD SUCCESSFUL"));
		assertFalse(firstLog.contains("single-use Daemon"));
		assertFalse(firstLog.contains("For more on this"));
		assertFalse(firstLog.contains("[Incubating] Problems report"));
		File gradleLog = new File(work, "resolution/gradle.log");
		assertTrue(gradleLog.isFile());
		assertIds("fixture:shared:1.0");

		// 命中缓存时不启动 Gradle、不重写结果，但仍检查 SDK 脚本完整性。
		File result = new File(work, "resolution/resolved.json");
		FileTime marker = FileTime.fromMillis(1234567890000L);
		Files.setLastModifiedTime(result.toPath(), marker);
		Files.setLastModifiedTime(gradleLog.toPath(), marker);
		String logDigest = LibraryArtifacts.digest(gradleLog);
		String cachedLog = runResolver(compiler, true, first);
		assertEquals(marker, Files.getLastModifiedTime(result.toPath()));
		assertEquals(marker, Files.getLastModifiedTime(gradleLog.toPath()));
		assertEquals(logDigest, LibraryArtifacts.digest(gradleLog));
		assertFalse(cachedLog.contains("正在准备依赖：Gradle"));
		assertFalse(new File(work, "resolution/project").exists());
		assertEquals(initialSdk, sdkSnapshot());
		assertFalse(new File(sdk, "utils/dependencies").exists());
		assertFalse(new File(sdk, ".gradle").exists());

		// 制品摘要不符时必须重新取得有效结果，不能只比较声明指纹。
		JsonObject damaged = DependencyResolver.read(result);
		damaged.getAsJsonArray("dependencies").get(0).getAsJsonObject().addProperty("digest", "invalid");
		writeJson(result, damaged);
		assertTrue(runResolver(compiler, true, first).contains("正在准备依赖：Gradle"));
		assertIds("fixture:shared:1.0");

		// 多个根库同时参与选择；严格约束和不同依赖路径上的排除规则仍由 Gradle 处理。
		File second = writeLibrary("fixture.second", "[{\"group\":\"fixture\",\"module\":\"shared\","
				+ "\"version\":{\"requires\":\"2.0\"}}]", "[{\"group\":\"fixture\",\"module\":\"helper\","
				+ "\"version\":{\"strictly\":\"2.0\"}}]");
		runResolver(compiler, true, first, second);
		assertIds("fixture:helper:2.0", "fixture:shared:2.0");

		// 同版本重新发布声明、移除根库后，旧模块缓存不能继续影响当前选择。
		first = writeLibrary("fixture.first", "[{\"group\":\"fixture\",\"module\":\"helper\","
				+ "\"version\":{\"requires\":\"1.0\"}}]", "[]");
		runResolver(compiler, true, first);
		assertIds("fixture:helper:1.0");

		// 外部脚本变化必须使缓存失效；只修改隔离副本，正式 SDK 保持原样。
		String oldFingerprint = DependencyResolver.read(result).get("fingerprint").getAsString();
		File script = new File(sdk, "simple/gradle/gradle.properties");
		String updated = Files.readString(script.toPath(), StandardCharsets.UTF_8) + "\r\n# cache fingerprint test\r\n";
		Files.writeString(script.toPath(), updated, StandardCharsets.UTF_8);
		List<String> updatedSdk = sdkSnapshot();
		assertTrue(runResolver(compiler, true, first).contains("正在准备依赖：Gradle"));
		assertFalse(oldFingerprint.equals(DependencyResolver.read(result).get("fingerprint").getAsString()));
		assertEquals("重新解析不得改写 SDK", updatedSdk, sdkSnapshot());
		String previous = LibraryArtifacts.digest(result);

		// 有效缓存不能掩盖 SDK 脚本缺失，缺失时应在启动 Gradle 前明确报告文件路径。
		for (String name : PROJECT_FILES) {
			File missing = new File(sdk, "simple/gradle/" + name);
			byte[] original = Files.readAllBytes(missing.toPath());
			Files.delete(missing.toPath());
			String missingLog = runResolver(compiler, false, first);
			assertTrue(missingLog.contains("SDK 缺少依赖解析脚本"));
			assertTrue(missingLog.contains(missing.getCanonicalPath()));
			assertFalse(missingLog.contains("正在准备依赖：Gradle"));
			assertEquals(previous, LibraryArtifacts.digest(result));
			Files.write(missing.toPath(), original);
		}
		List<String> finalSdk = sdkSnapshot();

		// 不可满足的约束使本轮失败，并保留上一次完整结果供诊断。
		first = writeLibrary("fixture.first", "[{\"group\":\"fixture\",\"module\":\"helper\","
				+ "\"version\":{\"requires\":\"2.0\"}}]", "[]");
		second = writeLibrary("fixture.second", "[]", "[{\"group\":\"fixture\",\"module\":\"helper\","
				+ "\"version\":{\"strictly\":\"1.0\"}}]");
		String failureLog = runResolver(compiler, false, first, second);
		assertTrue(failureLog.contains("Gradle 退出码："));
		assertTrue(failureLog.contains(gradleLog.getCanonicalPath()));
		assertFalse(failureLog.contains("扩展库依赖准备完成"));
		assertTrue(Files.readString(gradleLog.toPath()).contains("fixture:helper"));
		assertEquals(previous, LibraryArtifacts.digest(result));
		assertEquals("失败解析不得改写 SDK", finalSdk, sdkSnapshot());
	}

	/**
	 * 两库和应用侧按文件互补，同一图中仍选择较高依赖版本；源目录保持只读。
	 *
	 * @throws Exception 夹具、解析或文件校验失败。
	 */
	public void testOfflineLibrariesAndApplicationRepositoryComplementEachOther() throws Exception {
		linkSdkTools();
		String helper = "<dependencies><dependency><groupId>fixture</groupId><artifactId>helper</artifactId>"
				+ "<version>1.0</version></dependency></dependencies>";
		writeMavenModule("shared", "1.0", helper);
		writeMavenModule("helper", "2.0", "");
		File first = writeLibrary("fixture.first", "[{\"group\":\"fixture\",\"module\":\"shared\","
				+ "\"version\":{\"requires\":\"1.0\"}}]", "[]");
		File second = writeLibrary("fixture.second", "[{\"group\":\"fixture\",\"module\":\"helper\","
				+ "\"version\":{\"requires\":\"2.0\"}}]", "[]");
		copyRepositoryFile("fixture/shared/1.0/shared-1.0.pom", new File(first, "dependencies"));
		copyRepositoryFile("fixture/shared/1.0/shared-1.0.jar", new File(second, "dependencies"));
		copyRepositoryFile("fixture/helper/2.0/helper-2.0.pom", new File(work, "repository"));
		copyRepositoryFile("fixture/helper/2.0/helper-2.0.jar", new File(second, "dependencies"));
		String before = MavenRepository.fingerprint(Arrays.asList(new File(first, "dependencies"), new File(second, "dependencies")));
		runResolver(compiler, true, first, second);
		assertIds("fixture:helper:2.0", "fixture:shared:1.0");
		assertEquals(before, MavenRepository.fingerprint(Arrays.asList(new File(first, "dependencies"), new File(second, "dependencies"))));
		for (JsonElement value : DependencyResolver.read(new File(work, "resolution/resolved.json")).getAsJsonArray("dependencies")) {
			assertTrue(value.getAsJsonObject().get("file").getAsString().startsWith(new File(work, "repository").getCanonicalPath()));
		}

		// 即使已有解析快照，来源中同坐标不同内容也必须在执行 Gradle 前拒绝。
		File conflict = new File(first, "dependencies/fixture/shared/1.0/shared-1.0.jar");
		Files.writeString(conflict.toPath(), "conflicting same coordinate");
		String failure = runResolver(compiler, false, first, second);
		assertTrue(failure.contains("离线依赖内容冲突"));
		assertFalse(failure.contains("正在准备依赖：Gradle"));
	}

	/**
	 * 首次离线缺项失败，联网仅从回环 HTTP 仓库补齐，之后空缓存仍能离线解析。
	 *
	 * @throws Exception HTTP 夹具、解析或持久仓库校验失败。
	 */
	public void testMissingArtifactDownloadsThenWorksWithEmptyOfflineCache() throws Exception {
		linkSdkTools();
		writeMavenModule("helper", "1.0", "");
		File first = writeLibrary("fixture.first", "[{\"group\":\"fixture\",\"module\":\"helper\","
				+ "\"version\":{\"requires\":\"1.0\"}}]", "[]");
		copyRepositoryFile("fixture/helper/1.0/helper-1.0.pom", new File(first, "dependencies"));
		final AtomicInteger requests = new AtomicInteger();
		HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				requests.incrementAndGet();
				File file = new File(repository, exchange.getRequestURI().getPath().substring(1)).getCanonicalFile();
				try {
					if (!file.toPath().startsWith(repository.getCanonicalFile().toPath()) || !file.isFile()) {
						exchange.sendResponseHeaders(404, -1);
						return;
					}
					if ("HEAD".equals(exchange.getRequestMethod())) {
						exchange.getResponseHeaders().add("Content-Length", Long.toString(file.length()));
						exchange.sendResponseHeaders(200, -1);
					} else {
						byte[] content = Files.readAllBytes(file.toPath());
						exchange.sendResponseHeaders(200, content.length);
						exchange.getResponseBody().write(content);
					}
				} finally {
					exchange.close();
				}
			}
		});
		server.start();
		try {
			String address = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
			File settings = new File(sdk, "simple/gradle/settings.gradle");
			String original = Files.readString(settings.toPath(), StandardCharsets.UTF_8);
			String text = original.replace("google()", "").replace("mavenCentral()", "")
					.replace("maven { url = uri('https://jitpack.io/') }", "");
			text += "\r\ndependencyResolutionManagement.repositories.maven { url = uri('" + address + "'); allowInsecureProtocol = true }\r\n";
			Files.writeString(settings.toPath(), text, StandardCharsets.UTF_8);
			String missing = runResolver(compiler, false, first);
			assertTrue(missing.contains("helper"));
			assertEquals("离线模式不能访问 HTTP", 0, requests.get());

			runResolver(compiler, true, false, first);
			assertTrue("联网必须实际补齐原包", requests.get() > 0);
			File stored = new File(work, "repository/fixture/helper/1.0/helper-1.0.jar");
			assertTrue(stored.isFile());
			assertTrue(new File(work, "repository/fixture/helper/1.0/helper-1.0.pom").isFile());
			int previous = requests.get();
			Files.delete(new File(work, "resolution/resolved.json").toPath());
			gradleUserHome = Files.createTempDirectory("simple-offline-empty-home-").toFile();
			runResolver(compiler, true, first);
			assertEquals("已有本地仓库时不访问 HTTP", previous, requests.get());
			assertIds("fixture:helper:1.0");

			// 同一个原生运行变体可有多个包，其中一个已有时仍须补齐其它包。
			writeMavenModule("helper", "2.0", "<!-- do_not_remove: published-with-gradle-metadata -->");
			File moduleDirectory = new File(repository, "fixture/helper/2.0");
			Files.copy(new File(moduleDirectory, "helper-2.0.jar").toPath(), new File(moduleDirectory, "helper-2.0-extra.jar").toPath());
			Files.writeString(new File(moduleDirectory, "helper-2.0.module").toPath(),
					"{\"formatVersion\":\"1.1\",\"component\":{\"group\":\"fixture\",\"module\":\"helper\",\"version\":\"2.0\"},"
					+ "\"variants\":[{\"name\":\"runtime\",\"attributes\":{\"org.gradle.usage\":\"java-runtime\"},\"files\":["
					+ "{\"name\":\"helper-2.0.jar\",\"url\":\"helper-2.0.jar\"},"
					+ "{\"name\":\"helper-2.0-extra.jar\",\"url\":\"helper-2.0-extra.jar\"}]}]}", StandardCharsets.UTF_8);
			writeLibrary("fixture.first", "[{\"group\":\"fixture\",\"module\":\"helper\",\"version\":{\"requires\":\"2.0\"}}]", "[]");
			for (String suffix : new String[] {"pom", "module", "jar"}) {
				copyRepositoryFile("fixture/helper/2.0/helper-2.0." + suffix, new File(first, "dependencies"));
			}
			runResolver(compiler, false, first);
			runResolver(compiler, true, false, first);
			assertTrue(new File(work, "repository/fixture/helper/2.0/helper-2.0-extra.jar").isFile());
			assertFalse("补齐不能改写随库仓库", new File(first, "dependencies/fixture/helper/2.0/helper-2.0-extra.jar").exists());
			Files.delete(new File(work, "resolution/resolved.json").toPath());
			gradleUserHome = Files.createTempDirectory("simple-offline-multiple-artifacts-").toFile();
			previous = requests.get();
			runResolver(compiler, true, first);
			assertEquals(previous, requests.get());
			assertIds("fixture:helper:2.0", "fixture:helper:2.0");
		} finally {
			server.stop(0);
		}
	}

	/**
	 * 复制一个夹具仓库文件到指定补充仓库，保留标准相对路径。
	 *
	 * @param relative Maven 仓库相对路径。
	 * @param destination 补充仓库目录。
	 * @throws IOException 文件无法复制。
	 */
	private void copyRepositoryFile(String relative, File destination) throws IOException {
		File target = new File(destination, relative);
		Files.createDirectories(target.toPath().getParent());
		Files.copy(new File(repository, relative).toPath(), target.toPath());
	}

	/**
	 * 原生变体约束不能退化为 POM 的宽松依赖，冲突也必须由 Gradle 正常拒绝。
	 *
	 * @throws Exception 夹具、离线解析或原生元数据保真校验失败。
	 */
	public void testOfflineNativeMetadataRetainsStrictConstraints() throws Exception {
		linkSdkTools();
		writeMavenModule("helper", "1.0", "");
		writeMavenModule("helper", "2.0", "");
		writeMavenModule("shared", "1.0", "<!-- do_not_remove: published-with-gradle-metadata -->"
				+ "<dependencies><dependency><groupId>fixture</groupId><artifactId>helper</artifactId>"
				+ "<version>2.0</version></dependency></dependencies>");
		File metadata = new File(repository, "fixture/shared/1.0/shared-1.0.module");
		String nativeMetadata = "{\"formatVersion\":\"1.1\",\"component\":{\"group\":\"fixture\",\"module\":\"shared\","
				+ "\"version\":\"1.0\"},\"variants\":[{\"name\":\"runtime\",\"attributes\":{\"org.gradle.usage\":\"java-runtime\"},"
				+ "\"dependencies\":[{\"group\":\"fixture\",\"module\":\"helper\",\"version\":{\"strictly\":\"1.0\"}}],"
				+ "\"files\":[{\"name\":\"shared-1.0.jar\",\"url\":\"shared-1.0.jar\"}]}]}";
		Files.writeString(metadata.toPath(), nativeMetadata, StandardCharsets.UTF_8);
		File first = writeLibrary("fixture.native", "[{\"group\":\"fixture\",\"module\":\"shared\","
				+ "\"version\":{\"requires\":\"1.0\"}}]", "[]");
		MavenRepository.merge(Arrays.asList(repository), new File(first, "dependencies"));
		runResolver(compiler, true, first);
		assertIds("fixture:helper:1.0", "fixture:shared:1.0");
		assertEquals(nativeMetadata, Files.readString(new File(work, "repository/fixture/shared/1.0/shared-1.0.module").toPath()));

		File second = writeLibrary("fixture.conflicting", "[{\"group\":\"fixture\",\"module\":\"helper\","
				+ "\"version\":{\"requires\":\"2.0\"}}]", "[]");
		String failure = runResolver(compiler, false, first, second);
		assertTrue(failure, failure.contains("strictly 1.0"));
		assertTrue(failure, failure.contains("2.0"));
	}

	/**
	 * 空缓存中保留父 POM 与导入 BOM，Gradle 才能正确解析没有显式版本的依赖。
	 *
	 * @throws Exception 仓库输入或离线解析不符合预期。
	 */
	public void testOfflinePomParentsAndBomRemainResolvable() throws Exception {
		linkSdkTools();
		writeMavenModule("helper", "2.0", "");
		writeMavenModule("bom", "1.0", "<packaging>pom</packaging><dependencyManagement><dependencies>"
				+ "<dependency><groupId>fixture</groupId><artifactId>helper</artifactId><version>2.0</version>"
				+ "</dependency></dependencies></dependencyManagement>");
		writeMavenModule("parent", "1.0", "<packaging>pom</packaging><properties><bom.version>1.0</bom.version></properties>");
		writeMavenModule("shared", "1.0", "<parent><groupId>fixture</groupId><artifactId>parent</artifactId>"
				+ "<version>1.0</version></parent><dependencyManagement><dependencies><dependency>"
				+ "<groupId>fixture</groupId><artifactId>bom</artifactId><version>${bom.version}</version>"
				+ "<scope>import</scope><type>pom</type></dependency></dependencies></dependencyManagement>"
				+ "<dependencies><dependency><groupId>fixture</groupId><artifactId>helper</artifactId></dependency></dependencies>");
		File library = writeLibrary("fixture.managed", "[{\"group\":\"fixture\",\"module\":\"shared\","
				+ "\"version\":{\"requires\":\"1.0\"}}]", "[]");
		MavenRepository.merge(Arrays.asList(repository), new File(library, "dependencies"));
		runResolver(compiler, true, library);
		assertIds("fixture:helper:2.0", "fixture:shared:1.0");
		assertTrue(new File(work, "repository/fixture/bom/1.0/bom-1.0.pom").isFile());
		assertTrue(new File(work, "repository/fixture/parent/1.0/parent-1.0.pom").isFile());
	}

	/**
	 * 启动只从交付 JAR 加载编译器的探针，隔离各次环境与类加载状态。
	 *
	 * @param jar 本轮使用的编译器 JAR。
	 * @param success 是否预期成功。
	 * @param libraries 本轮实际引用的扩展库。
	 * @return 子进程的 UTF-8 日志。
	 * @throws Exception 子进程超时、结果不符或日志无法读取。
	 */
	private String runResolver(File jar, boolean success, File... libraries) throws Exception {
		return runResolver(jar, success, true, libraries);
	}

	/**
	 * 使用明确的联网策略运行隔离解析探针。
	 *
	 * @param jar 本轮编译器 JAR。
	 * @param success 预期成功状态。
	 * @param offline 是否禁止 Gradle 访问远程仓库。
	 * @param libraries 本轮实际引用的库。
	 * @return 子进程诊断。
	 * @throws Exception 探针超时或结果不符合预期。
	 */
	private String runResolver(File jar, boolean success, boolean offline, File... libraries) throws Exception {
		String classPath = jar.getCanonicalPath() + File.pathSeparator
				+ System.getProperty("java.class.path") + File.pathSeparator
				+ new File(DependencyResolverTest.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath()
				+ File.pathSeparator + new File("third_party/junit-4.12.jar").getCanonicalPath();
		List<String> command = new ArrayList<String>(Arrays.asList(
				new File(System.getProperty("java.home"), "bin/java").getPath(), "-Dfile.encoding=UTF-8",
				"-cp", classPath, Probe.class.getName(), work.getCanonicalPath()));
		for (File library : libraries) {
			command.add(library.getCanonicalPath());
		}

		// 使用子进程的环境定位隔离 SDK，生产入口无需增加测试专用参数。
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.environment().put("ES4A_SDK_HOME", sdk.getCanonicalPath());
		builder.environment().put("ES4A_OFFLINE", offline ? "1" : "0");
		builder.environment().put("GRADLE_USER_HOME", gradleUserHome.getCanonicalPath());
		builder.environment().remove("ES4A_REFRESH_DEPENDENCIES");
		File log = Files.createTempFile("simple-dependency-resolution-", ".log").toFile();
		builder.redirectErrorStream(true);
		builder.redirectOutput(log);

		Process process = builder.start();
		if (!process.waitFor(120, TimeUnit.SECONDS)) {
			process.destroyForcibly();
			throw new IOException("依赖解析测试进程超时，日志：" + log);
		}

		String output = Files.readString(log.toPath(), StandardCharsets.UTF_8);
		assertEquals("解析状态不符合预期，日志：" + log + "\n" + output, success, process.exitValue() == 0);
		return output;
	}

	/**
	 * 核对完整依赖集合，防止错误版本或被移除的模块仍留在结果中。
	 *
	 * @param expected 预期的全部模块坐标。
	 * @throws IOException 解析结果无法读取。
	 */
	private void assertIds(String... expected) throws IOException {
		List<String> actual = new ArrayList<String>();
		JsonObject result = DependencyResolver.read(new File(work, "resolution/resolved.json"));
		for (JsonElement value : result.getAsJsonArray("dependencies")) {
			JsonObject artifact = value.getAsJsonObject();
			actual.add(artifact.get("id").getAsString());
			assertEquals(artifact.get("digest").getAsString(), LibraryArtifacts.digest(new File(artifact.get("file").getAsString())));
		}
		assertEquals(Arrays.asList(expected), actual);
	}

	/**
	 * 写入固定发布版本的原生声明，允许覆盖以模拟同版本重新发布。
	 *
	 * @param name 根库命名空间。
	 * @param dependencies 原生依赖数组的 JSON。
	 * @param constraints 原生约束数组的 JSON。
	 * @return 类库交付目录。
	 * @throws IOException 交付文件无法写入。
	 */
	private File writeLibrary(String name, String dependencies, String constraints) throws IOException {
		File library = new File(root, "libraries/" + name);
		JsonObject component = new JsonObject();
		component.addProperty("group", "es4a.extension");
		component.addProperty("module", name);
		component.addProperty("version", "1.0");
		JsonObject attributes = new JsonObject();
		attributes.addProperty("org.gradle.usage", "java-runtime");
		JsonObject variant = new JsonObject();
		variant.addProperty("name", "runtime");
		variant.add("attributes", attributes);
		variant.add("dependencies", JsonParser.parseString(dependencies));
		variant.add("dependencyConstraints", JsonParser.parseString(constraints));
		JsonArray variants = new JsonArray();
		variants.add(variant);
		JsonObject metadata = new JsonObject();
		metadata.addProperty("formatVersion", "1.1");
		metadata.add("component", component);
		metadata.add("variants", variants);
		writeJson(new File(library, "dependencies.module"), metadata);

		// 标准清单是资源身份的唯一来源，不依赖额外的文件索引。
		Files.writeString(new File(library, "AndroidManifest.xml").toPath(),
				"<manifest package=\"" + name + "\" />", StandardCharsets.UTF_8);
		return library;
	}

	/**
	 * 创建真实 Maven POM 与 JAR，使 Gradle 使用标准仓库逻辑解析传递依赖。
	 *
	 * @param name 模块名称。
	 * @param version 发布版本。
	 * @param dependencies POM 中的依赖 XML，空字符串表示无依赖。
	 * @throws IOException POM 或 JAR 无法写入。
	 */
	private void writeMavenModule(String name, String version, String dependencies) throws IOException {
		File directory = new File(repository, "fixture/" + name + "/" + version);
		Files.createDirectories(directory.toPath());
		String prefix = name + "-" + version;
		String pom = "<project><modelVersion>4.0.0</modelVersion><groupId>fixture</groupId><artifactId>"
				+ name + "</artifactId><version>" + version + "</version>" + dependencies + "</project>";
		Files.writeString(new File(directory, prefix + ".pom").toPath(), pom, StandardCharsets.UTF_8);
		try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(new File(directory, prefix + ".jar").toPath()))) {
			output.putNextEntry(new ZipEntry("identity.txt"));
			output.write(prefix.getBytes(StandardCharsets.UTF_8));
			output.closeEntry();
		}
	}

	/**
	 * 记录隔离 SDK 的全部目录、文件内容与修改时间，用于检查解析过程只读。
	 *
	 * @return 按路径稳定排列的 SDK 状态。
	 * @throws IOException 目录无法列出或文件无法读取。
	 */
	private List<String> sdkSnapshot() throws IOException {
		List<String> result = new ArrayList<String>();
		recordSdk(sdk, result);
		return result;
	}

	/**
	 * 递归收集 SDK 状态，不跟随配置中指向外部工具的文本路径。
	 *
	 * @param directory 当前目录。
	 * @param result 按路径收集的状态。
	 * @throws IOException SDK 目录无法读取。
	 */
	private void recordSdk(File directory, List<String> result) throws IOException {
		File[] files = directory.listFiles();
		if (files == null) {
			throw new IOException("无法读取测试 SDK 目录：" + directory);
		}
		Arrays.sort(files);
		for (File file : files) {
			String relative = sdk.toPath().relativize(file.toPath()).toString();
			if (file.equals(toolsLink)) {
				// 外部安装工具只记录链接节点，不将其内容纳入隔离 SDK 的写入快照。
				result.add(relative + " -> SDK tools");
				continue;
			}
			if (file.isDirectory()) {
				result.add(relative + "/");
				recordSdk(file, result);
			} else {
				result.add(relative + ":" + LibraryArtifacts.digest(file) + ":" + file.lastModified());
			}
		}
	}

	/**
	 * 以 UTF-8 保存测试描述，保持 Gradle 原生元数据的字段顺序。
	 *
	 * @param file 输出文件。
	 * @param value 待保存的 JSON 对象。
	 * @throws IOException 父目录或文件无法写入。
	 */
	private void writeJson(File file, JsonObject value) throws IOException {
		Files.createDirectories(file.getParentFile().toPath());
		Files.writeString(file.toPath(), new GsonBuilder().setPrettyPrinting().create().toJson(value), StandardCharsets.UTF_8);
	}

	/**
	 * 在独立 JVM 中产生确定的警告与错误，验证输出归档不依赖英文关键词或外部插件。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	public static final class OutputProbe {

		/**
		 * 禁止实例化仅提供测试进程入口的类。
		 */
		private OutputProbe() {
		}

		/**
		 * 按指定模式输出诊断；失败模式使用固定非零退出码。
		 *
		 * @param arguments 首项为 success、warning 或 failure。
		 */
		public static void main(String[] arguments) {
			if ("warning".equals(arguments[0])) {
				System.err.println("测试警告：依赖选项需要注意");
			} else if ("failure".equals(arguments[0])) {
				System.out.println("失败上下文：中文与空格路径");
				System.err.println("测试错误：依赖无法解析");
				System.exit(7);
			}
		}
	}

	/**
	 * 在独立 JVM 中访问交付 JAR 的包级入口，避免扩大编译器公开接口。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	public static final class Probe {

		/**
		 * 禁止实例化仅用作测试进程入口的类。
		 */
		private Probe() {
		}

		/**
		 * 按独立进程的环境配置解析指定库，异常使进程以非零状态退出。
		 *
		 * @param arguments 首项为应用依赖构建目录，其余为库目录。
		 * @throws IOException 依赖解析失败。
		 */
		public static void main(String[] arguments) throws IOException {
			List<File> libraries = new ArrayList<File>();
			for (int position = 1; position < arguments.length; position++) {
				libraries.add(new File(arguments[position]));
			}
			DependencyResolver.resolve(libraries, new File(arguments[0]));
		}
	}
}

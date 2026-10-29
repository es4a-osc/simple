package simple.compiler;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 从正式编译器 JAR 验证 SDK 外部脚本、Gradle 依赖规则和解析缓存。
 *
 * <p>依赖输入来自隔离的本地 Maven 仓库，测试不访问业务类库或修改 SDK 配置。
 * Gradle 与 AGP 使用构建编译器所需的 SDK 工具和已有缓存，全程以离线模式解析。
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
		repository = new File(root, "repository");
		work = new File(root, "application/build/dependencies");
		compiler = new File("dist/common/SimpleCompiler.jar").getCanonicalFile();
		assertTrue("请先生成正式编译器 JAR", compiler.isFile());

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
	 * 启动只从交付 JAR 加载编译器的探针，隔离各次环境与类加载状态。
	 *
	 * @param jar 本轮使用的编译器 JAR。
	 * @param success 是否预期成功。
	 * @param libraries 本轮实际引用的扩展库。
	 * @return 子进程的 UTF-8 日志。
	 * @throws Exception 子进程超时、结果不符或日志无法读取。
	 */
	private String runResolver(File jar, boolean success, File... libraries) throws Exception {
		String classPath = jar.getCanonicalPath() + File.pathSeparator
				+ new File(new File("dist/common/libs").getCanonicalFile(), "*").getPath() + File.pathSeparator
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
		builder.environment().put("ES4A_OFFLINE", "1");
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

		// 交付入口只引用本目录的原生声明，不携带任何解构后的依赖文件。
		JsonObject index = new JsonObject();
		index.addProperty("schemaVersion", 2);
		index.addProperty("namespace", name);
		index.addProperty("module", "dependencies.module");
		writeJson(new File(library, "dependencies.json"), index);
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

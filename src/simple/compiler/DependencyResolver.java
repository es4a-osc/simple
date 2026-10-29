package simple.compiler;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 在项目引用确定后调用 SDK 的 Gradle，消费扩展库原生模块声明并复用有效解析结果。
 *
 * <p>解析脚本由 SDK 的 simple/gradle 目录提供，编译器直接调用，不在应用目录复制脚本。
 * 模块选版、排除、约束及 Android 变体由 Gradle 与 SDK 已有 AGP 处理。
 * 归档始终引用解析结果中的路径，不猜测个人缓存的内部布局。
 *
 * @author 树先生 xhwsd@qq.com
 */
final class DependencyResolver {

	/** SDK 提供的依赖解析工程文件；内容共同参与解析缓存指纹。 */
	private static final String[] PROJECT_FILES = {
		"settings.gradle", "build.gradle", "gradle.properties"
	};

	/**
	 * 禁止实例化仅提供静态依赖解析操作的类。
	 */
	private DependencyResolver() {
	}

	/**
	 * 统一解析本轮所用模块。无新式模块声明时不启动 Gradle，纯本地库可离线消费。
	 *
	 * @param libraries 项目实际使用的扩展库。
	 * @param directory 项目的依赖构建目录。
	 * @return 已核对的解析结果；没有模块声明时返回空结果。
	 * @throws IOException 输入无效、工具失败、缓存缺失或进程被中断。
	 */
	static JsonObject resolve(List<File> libraries, File directory) throws IOException {
		// 先核对实际使用的类库声明；纯本地依赖不要求 SDK 或 Gradle 可用。
		JsonArray modules = new JsonArray();
		boolean runtimeModules = false;
		MessageDigest fingerprint = sha256();
		for (File library : libraries) {
			File index = new File(library, "dependencies.json");
			if (!index.isFile()) {
				continue;
			}
			JsonObject description = read(index);
			if (description.get("schemaVersion").getAsInt() != 2) {
				continue;
			}
			File metadata = contained(library, description.get("module").getAsString());
			runtimeModules |= hasRuntimeDependencies(metadata, description.get("namespace").getAsString());

			JsonObject module = new JsonObject();
			module.addProperty("file", metadata.getCanonicalPath());
			module.addProperty("namespace", description.get("namespace").getAsString());
			modules.add(module);
			fingerprint.update(index.getCanonicalPath().getBytes(StandardCharsets.UTF_8));
			fingerprint.update(Files.readAllBytes(index.toPath()));
			fingerprint.update(Files.readAllBytes(metadata.toPath()));
		}
		if (modules.size() == 0 || !runtimeModules) {
			JsonObject empty = new JsonObject();
			empty.add("dependencies", new JsonArray());
			empty.add("graph", new JsonArray());
			return empty;
		}

		// SDK 工具按固定目录定位；实际路径和外部脚本共同参与指纹。
		File sdk = sdkDirectory();
		File project = new File(sdk, "simple/gradle").getCanonicalFile();
		File javaHome = new File(sdk, "tools/jdk-11.0.32.1").getCanonicalFile();
		File gradle = new File(sdk, "tools/gradle-8.14.6").getCanonicalFile();
		File android = new File(sdk, "tools/android-sdk").getCanonicalFile();
		for (File tool : new File[] {javaHome, gradle, android}) {
			fingerprint.update(tool.getPath().getBytes(StandardCharsets.UTF_8));
		}
		for (String name : PROJECT_FILES) {
			File script = new File(project, name);
			if (!script.isFile()) {
				throw new IOException("SDK 缺少依赖解析脚本，请更新完整 SDK：" + script);
			}
			fingerprint.update(name.getBytes(StandardCharsets.UTF_8));
			fingerprint.update(Files.readAllBytes(script.toPath()));
		}
		String identity = hex(fingerprint.digest());

		// 只复用声明和归档内容都有效的结果；显式刷新时仍交给 Gradle 重新解析。
		File work = directory == null ? Files.createTempDirectory("simple-dependencies-").toFile()
				: new File(directory, "resolution").getCanonicalFile();
		Files.createDirectories(work.toPath());
		File output = new File(work, "resolved.json");
		boolean refresh = "1".equals(System.getenv("ES4A_REFRESH_DEPENDENCIES"));
		if (!refresh && valid(output, identity)) {
			return read(output);
		}

		// 执行工程位于 SDK；请求、结果和项目缓存全部进入应用构建目录，SDK 保持只读。
		JsonObject request = new JsonObject();
		request.addProperty("fingerprint", identity);
		request.addProperty("work", work.getCanonicalPath());
		request.addProperty("output", output.getCanonicalPath());
		request.add("modules", modules);
		File requestFile = new File(work, "request.json");
		Files.writeString(requestFile.toPath(), new GsonBuilder().setPrettyPrinting().create().toJson(request), StandardCharsets.UTF_8);

		// 沿用 SDK 随附工具；不读取另一套工具配置或扩展库开发工具的数据。
		List<String> command = new ArrayList<String>();
		command.add(new File(javaHome, "bin/java" + (File.separatorChar == '\\' ? ".exe" : "")).getPath());
		command.add("-Dfile.encoding=UTF-8");
		command.add("-Dsun.stdout.encoding=UTF-8");
		command.add("-Dsun.stderr.encoding=UTF-8");
		command.add("-cp");
		command.add(new File(gradle, "lib/*").getPath());
		command.add("org.gradle.launcher.GradleMain");
		command.add("--console=plain");
		// 由 Gradle 自身区分日志级别，保留警告和错误，不向用户输出内部任务与守护进程信息。
		command.add("--warn");
		command.add("--no-daemon");
		command.add("-p");
		command.add(project.getPath());
		command.add("--project-cache-dir");
		command.add(new File(work, "cache").getPath());
		command.add("-Prequest=" + requestFile.getPath());
		if ("1".equals(System.getenv("ES4A_OFFLINE"))) {
			command.add("--offline");
		}
		if (refresh) {
			command.add("--refresh-dependencies");
		}
		command.add("resolveApplicationDependencies");
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.environment().put("JAVA_HOME", javaHome.getPath());
		builder.environment().put("ANDROID_HOME", android.getPath());
		builder.environment().put("ANDROID_SDK_ROOT", android.getPath());

		File log = new File(work, "gradle.log");
		runGradle(builder, log, System.out, System.err);
		if (!valid(output, identity)) {
			throw new IOException("Gradle 未生成完整有效的应用依赖结果：" + output + "。完整日志：" + log);
		}
		return read(output);
	}

	/**
	 * 将 Gradle 诊断完整保存到日志，成功时提示警告，失败时回显详情，不继承终端输出。
	 *
	 * <p>调用方应使用 Gradle 的警告日志级别；此处不按文字内容过滤诊断。
	 * 成功返回仅表示进程退出码为零，解析结果仍须由调用方验证。
	 *
	 * @param builder 已配置命令与 SDK 环境的进程构造器。
	 * @param log 本轮 Gradle 诊断日志，实际启动时覆盖上次日志。
	 * @param out 中文进度摘要的输出目标。
	 * @param err 警告提示及失败详情的输出目标。
	 * @throws IOException 进程无法启动、执行失败、诊断无法读取或等待被中断。
	 */
	static void runGradle(ProcessBuilder builder, File log, PrintStream out, PrintStream err) throws IOException {
		// 合并后的输出直接写文件，避免大量诊断堵塞管道，也不把整份日志驻留内存。
		builder.redirectErrorStream(true);
		builder.redirectOutput(log);
		out.println("正在准备依赖：Gradle");
		Process process;
		try {
			process = builder.start();
		} catch (IOException failure) {
			throw new IOException("无法启动应用依赖解析：" + failure.getMessage() + "。完整日志：" + log, failure);
		}

		int code;
		try {
			// 应用构建不接受 Gradle 交互输入，中断时终止本轮进程并保留已写入的诊断。
			process.getOutputStream().close();
			code = process.waitFor();
		} catch (IOException failure) {
			process.destroyForcibly();
			throw new IOException("应用依赖解析输入关闭失败。完整日志：" + log, failure);
		} catch (InterruptedException failure) {
			process.destroyForcibly();
			Thread.currentThread().interrupt();
			throw new IOException("应用依赖解析已中断。完整日志：" + log, failure);
		}

		// 成功时的警告和报告原文留在日志，只提示入口；失败时完整回显，不按关键词截断。
		if (Files.size(log.toPath()) > 0) {
			if (code == 0) {
				err.println("依赖准备提示：" + log.getName());
			} else {
				err.println("Gradle 依赖解析失败详情：");
				try (BufferedReader reader = Files.newBufferedReader(log.toPath(), StandardCharsets.UTF_8)) {
					String line;
					while ((line = reader.readLine()) != null) {
						err.println(line);
					}
				}
				err.println("Gradle 完整日志：" + log.getAbsolutePath());
			}
		}
		if (code != 0) {
			throw new IOException("应用依赖解析失败，Gradle 退出码：" + code + "。完整日志：" + log.getAbsolutePath());
		}
	}

	/**
	 * 核对原生模块身份及交付边界，纯本地库不需要启动 Gradle。
	 *
	 * @param metadata Gradle 原生模块描述。
	 * @param namespace 交付描述中的资源命名空间。
	 * @return 是否存在运行模块依赖或约束。
	 * @throws IOException 描述不完整、身份不符或夹带制品引用。
	 */
	private static boolean hasRuntimeDependencies(File metadata, String namespace) throws IOException {
		try {
			// 先核对声明身份，避免将任意外部模块描述当作本地扩展库交付。
			JsonObject module = read(metadata);
			JsonObject component = module.getAsJsonObject("component");
			if (!"1.1".equals(module.get("formatVersion").getAsString())
					|| !"es4a.extension".equals(component.get("group").getAsString())
					|| !namespace.equals(component.get("module").getAsString())
					|| !namespace.matches("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+")
					|| !component.get("version").getAsString().matches("[A-Za-z0-9][A-Za-z0-9._+\\-]*")) {
				throw new IOException("扩展库原生模块身份无效：" + metadata);
			}

			// API 变体不参与 APK 运行依赖判断，所有变体都禁止夹带制品或转发地址。
			boolean required = false;
			boolean runtimeVariant = false;
			for (JsonElement value : module.getAsJsonArray("variants")) {
				JsonObject variant = value.getAsJsonObject();
				if ((variant.has("files") && variant.getAsJsonArray("files").size() != 0) || variant.has("available-at")) {
					throw new IOException("扩展库依赖描述不能包含制品或转发地址：" + metadata);
				}
				JsonObject attributes = variant.getAsJsonObject("attributes");
				if (attributes != null && attributes.has("org.gradle.usage")
						&& "java-runtime".equals(attributes.get("org.gradle.usage").getAsString())) {
					runtimeVariant = true;
					required |= (variant.has("dependencies") && variant.getAsJsonArray("dependencies").size() != 0)
							|| (variant.has("dependencyConstraints") && variant.getAsJsonArray("dependencyConstraints").size() != 0);
				}
			}
			if (!runtimeVariant) {
				throw new IOException("扩展库模块缺少运行依赖变体：" + metadata);
			}
			return required;
		} catch (RuntimeException failure) {
			throw new IOException("扩展库原生模块描述不完整：" + metadata, failure);
		}
	}

	/**
	 * 核对声明指纹与全部制品，损坏或过期的缓存只触发重新解析。
	 *
	 * @param file 已保存的解析结果。
	 * @param fingerprint 本轮声明、SDK 工具路径和解析脚本的指纹。
	 * @return 结果完整、指纹一致且所有制品校验通过时返回真。
	 */
	private static boolean valid(File file, String fingerprint) {
		if (!file.isFile()) {
			return false;
		}
		try {
			JsonObject result = read(file);
			if (!fingerprint.equals(result.get("fingerprint").getAsString())) {
				return false;
			}
			for (JsonElement value : result.getAsJsonArray("dependencies")) {
				JsonObject artifact = value.getAsJsonObject();
				File input = new File(artifact.get("file").getAsString());
				if (!input.isFile() || !LibraryArtifacts.digest(input).equals(artifact.get("digest").getAsString())) {
					return false;
				}
			}
			return true;
		} catch (IOException | RuntimeException failure) {
			return false;
		}
	}

	/**
	 * 核对类库交付中的相对文件引用，拒绝绝对路径、目录越界和缺失文件。
	 *
	 * @param root 已安装的类库目录。
	 * @param relative 相对于类库目录的文件路径。
	 * @return 已核对的规范化文件。
	 * @throws IOException 文件引用无效或规范化失败。
	 */
	static File contained(File root, String relative) throws IOException {
		File target = new File(root, relative).getCanonicalFile();
		if (new File(relative).isAbsolute() || !target.toPath().startsWith(root.getCanonicalFile().toPath()) || !target.isFile()) {
			throw new IOException("扩展库文件引用无效：" + relative + "\n来源：" + root);
		}
		return target;
	}

	/**
	 * 读取 UTF-8 JSON 对象，并将格式错误转换为带来源的构建诊断。
	 *
	 * @param file 待读取的描述文件。
	 * @return 已解析的 JSON 对象。
	 * @throws IOException 文件不可读或内容不是有效的 JSON 对象。
	 */
	static JsonObject read(File file) throws IOException {
		try {
			return JsonParser.parseString(Files.readString(file.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (RuntimeException failure) {
			throw new IOException("依赖描述格式无效：" + file, failure);
		}
	}

	/**
	 * 沿调用者提供的 SDK 或工具环境定位统一工具配置。
	 *
	 * @return 包含编译器配套 Gradle 工程的 SDK 根目录。
	 * @throws IOException 未找到配置或 SDK 路径无法规范化。
	 */
	private static File sdkDirectory() throws IOException {
		List<File> candidates = new ArrayList<File>();
		String explicit = System.getenv("ES4A_SDK_HOME");
		if (explicit != null && !explicit.isEmpty()) {
			candidates.add(new File(explicit));
		}
		String simple = System.getenv("SIMPLE_HOME");
		if (simple != null && !simple.isEmpty()) {
			candidates.add(new File(simple).getAbsoluteFile().getParentFile());
		}
		String android = System.getenv("ANDROID_HOME");
		if (android != null && !android.isEmpty()) {
			File tools = new File(android).getAbsoluteFile().getParentFile();
			if (tools != null && tools.getParentFile() != null) {
				candidates.add(tools.getParentFile());
			}
		}
		for (File candidate : candidates) {
			if (candidate != null && new File(candidate, "simple/gradle").isDirectory()) {
				return candidate.getCanonicalFile();
			}
		}
		throw new IOException("未找到 SDK 编译器配套目录 simple/gradle，请使用完整 SDK 的 project.ps1 编译。");
	}

	/**
	 * 获取受支持 JDK 必须提供的 SHA-256 摘要实现。
	 *
	 * @return 新建的摘要实例。
	 * @throws IllegalStateException 当前 JDK 缺少规定的摘要算法。
	 */
	private static MessageDigest sha256() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException failure) {
			throw new IllegalStateException(failure);
		}
	}

	/**
	 * 将摘要编码为不受默认区域设置影响的十六进制字符串。
	 *
	 * @param value 摘要字节。
	 * @return 小写十六进制字符串。
	 */
	private static String hex(byte[] value) {
		StringBuilder result = new StringBuilder();
		for (byte item : value) {
			result.append(String.format(Locale.ROOT, "%02x", item & 255));
		}
		return result.toString();
	}
}

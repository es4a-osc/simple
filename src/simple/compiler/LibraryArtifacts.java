package simple.compiler;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import simple.compiler.util.Toolchain;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 读取扩展库声明，按项目统一解析仓库依赖，并选择独立 JAR、展开 AAR。
 *
 * <p>加载器、资源处理和 APK 组装共用本对象的结果。坐标依赖按完整组织与名称分组，
 * 本地文件按内容去重；相同内容只展开一次，冲突不由目录枚举顺序决定。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class LibraryArtifacts {

	private static final Pattern VERSION = Pattern.compile("^(.+?)-([0-9][0-9A-Za-z._+\\-]*)\\.jar$", Pattern.CASE_INSENSITIVE);
	private final Map<File, List<Entry>> libraries = new LinkedHashMap<File, List<Entry>>();
	private final Map<File, String> namespaces = new HashMap<File, String>();
	private final Map<File, File> manifests = new HashMap<File, File>();
	private final Map<String, Entry> selected = new LinkedHashMap<String, Entry>();
	private final Map<String, List<String>> graph = new LinkedHashMap<String, List<String>>();
	private final Map<File, AndroidLibrary> androidLibraries = new LinkedHashMap<File, AndroidLibrary>();
	private final List<Entry> declarations = new ArrayList<Entry>();
	private final Map<String, Entry> resolvedJars = new HashMap<String, Entry>();
	private final File workDirectory;

	/**
	 * 表示一份参与资源、清单、资产及原生库处理的 Android 类库目录。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	public static final class AndroidLibrary {
		/** 已安装的扩展库目录或 AAR 展开目录。 */
		public final File directory;
		/** 用于生成 R 类的资源包名；无资源声明的旧式目录可以为空。 */
		public final String namespace;
		/** 原始 AAR 或扩展库目录，用于保留可定位的构建诊断。 */
		public final File source;
		/** 当前库实际使用的清单；生成清单与作者的输入文件保持独立。 */
		public final File manifest;

		/**
		 * 保存交付目录或 AAR 展开目录与对应资源包名。
		 *
		 * @param directory Android 类库目录。
		 * @param namespace 来自交付描述或清单的资源包名。
		 * @param source 原始归档或扩展库目录。
		 */
		private AndroidLibrary(File directory, String namespace, File source) {
			this(directory, namespace, source, new File(directory, "AndroidManifest.xml"));
		}

		/**
		 * 保存 Android 输入，并明确选择本库编译后清单或原始 AAR 清单。
		 *
		 * @param directory Android 类库目录。
		 * @param namespace 来自交付描述或清单的资源包名。
		 * @param source 原始归档或扩展库目录。
		 * @param manifest 当前构建使用的清单文件。
		 */
		private AndroidLibrary(File directory, String namespace, File source, File manifest) {
			this.directory = directory;
			this.namespace = namespace;
			this.source = source;
			this.manifest = manifest;
		}
	}

	/**
	 * 保存一次依赖声明及经过内容核对的文件身份。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	private static final class Entry {
		private final String id;
		private final String group;
		private final String version;
		private final File file;
		private final String digest;
		private final String source;

		/**
		 * 区分仓库坐标、本地文件与旧式 libs 文件名中的版本。
		 *
		 * @param id 交付描述中的依赖身份。
		 * @param file 已检查的原始归档。
		 * @throws IOException 文件无法读取。
		 */
		private Entry(String id, File file) throws IOException {
			this(id, file, "");
		}

		/** 同坐标的分类制品保持独立身份，不能与主制品相互覆盖。 */
		private Entry(String id, File file, String classifier) throws IOException {
			this.id = id;
			this.file = file;
			this.source = file.toString();
			this.digest = digest(file);
			String[] coordinate = id.split(":", -1);
			if (coordinate.length == 3 && !id.startsWith("file:")) {
				group = coordinate[0] + ":" + coordinate[1] + (classifier.isEmpty() ? "" : ":" + classifier)
					+ (file.getName().toLowerCase(Locale.ROOT).endsWith(".aar") ? "@aar" : "");
				version = coordinate[2];
			} else {
				Matcher matcher = VERSION.matcher(file.getName());
				boolean legacy = id.startsWith("legacy:") && matcher.matches();
				// 文件名不是仓库身份；无坐标文件仅按内容去重，不误合并同名的独立包。
				group = legacy ? "legacy:" + matcher.group(1) : "file:" + digest;
				version = legacy ? matcher.group(2) : "";
			}
		}

		/** 为解构后的独立 JAR 保留原始 AAR 来源，不重新计算已经核对的内容身份。 */
		private Entry(Entry entry, String source) {
			id = entry.id;
			group = entry.group;
			version = entry.version;
			file = entry.file;
			digest = entry.digest;
			this.source = source;
		}

	}

	/**
	 * 读取已安装扩展库；有描述时只消费描述中的运行依赖，没有描述时沿用本地目录约定。
	 *
	 * @param roots 已安装且含 classes.jar 的扩展库目录。
	 * @param workDirectory AAR 展开输出根目录；为空时使用系统临时目录。
	 * @throws IOException 描述无效、文件缺失、版本冲突或归档损坏。
	 */
	public LibraryArtifacts(List<File> roots, File workDirectory) throws IOException {
		this.workDirectory = workDirectory;
		for (File root : roots) {
			File directory = root.getCanonicalFile();
			List<Entry> entries = new ArrayList<Entry>();
			File index = new File(directory, "dependencies.json");
			if (index.isFile()) {
				readIndex(directory, index, entries);
			} else {
				for (File file : list(directory, "libs", ".jar")) {
					entries.add(new Entry("legacy:" + file.getName(), file));
				}
				for (File file : list(directory, "aars", ".aar")) {
					entries.add(new Entry("file:" + file.getName(), file));
				}
			}
			libraries.put(directory, entries);
			for (Entry entry : entries) {
				declarations.add(entry);
				choose(entry);
			}
		}

		// 只为调用者提供的项目所用库解析一次，由编译器内部统筹 Gradle，不推断缓存布局。
		JsonObject resolved = DependencyResolver.resolve(roots, workDirectory);
		List<Entry> externalEntries = new ArrayList<Entry>();
		for (JsonElement value : resolved.getAsJsonArray("dependencies")) {
			JsonObject item = value.getAsJsonObject();
			Entry entry = new Entry(item.get("id").getAsString(), new File(item.get("file").getAsString()),
					item.has("classifier") ? item.get("classifier").getAsString() : "");
			declarations.add(entry);
			choose(entry);
			if (entry.file.getName().toLowerCase(Locale.ROOT).endsWith(".jar")) resolvedJars.put(entry.group, entry);
			externalEntries.add(entry);
		}
		// 在线依赖在作者本地原包之前，本库目录最后加入，保留依赖在前、使用者在后的覆盖顺序。
		for (List<Entry> entries : libraries.values()) entries.addAll(0, externalEntries);
		for (JsonElement value : resolved.getAsJsonArray("graph")) {
			JsonObject item = value.getAsJsonObject();
			List<String> children = new ArrayList<String>();
			for (JsonElement child : item.getAsJsonArray("dependencies")) children.add(child.getAsString());
			graph.put(item.get("id").getAsString(), children);
		}

		// 只核对最终版本的副本，不因已经淘汰的旧版内容不同阻断选版。
		for (Entry entry : declarations) {
			Entry choice = selected.get(entry.group);
			boolean sameVersion = entry.version.equals(choice.version)
					|| (!entry.version.isEmpty() && !choice.version.isEmpty()
						&& Dependencies.compareVersion(entry.version, choice.version) == 0);
			if (sameVersion && !entry.digest.equals(choice.digest)) {
				throw new IOException("扩展库依赖版本相同但内容不同：\n" + choice.file + "\n" + entry.file);
			}
		}

		// 依赖关系先于资源覆盖顺序确定；相同内容的 AAR 只产生一个 Android 输入。
		for (Entry entry : selected.values()) {
			if (entry.file.getName().toLowerCase(Locale.ROOT).endsWith(".aar")) {
				File directory = unpack(entry);
				androidLibraries.put(entry.file, new AndroidLibrary(directory, AndroidLibraries.readNamespace(directory), entry.file));
			}
		}
	}

	/**
	 * 读取当前原生声明或旧式快照，拒绝越出交付目录的本地文件引用。
	 *
	 * @param directory 交付根目录。
	 * @param index 依赖描述。
	 * @param entries 当前库的声明列表。
	 * @throws IOException 结构、平台要求或文件引用无效。
	 */
	private void readIndex(File directory, File index, List<Entry> entries) throws IOException {
		try (Reader reader = Files.newBufferedReader(index.toPath(), StandardCharsets.UTF_8)) {
			JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
			int format = object.get("schemaVersion").getAsInt();
			if (format != 1 && format != 2) {
				throw new IOException("不支持的扩展库依赖描述版本：" + index);
			}
			if (object.get("minSdk").getAsInt() > Toolchain.MIN_API_LEVEL || object.get("java").getAsInt() > 11) {
				throw new IOException("扩展库要求高于当前 API 24 / Java 11 的运行环境：" + index);
			}
			namespaces.put(directory, object.get("namespace").getAsString());
			if (format == 2) {
				DependencyResolver.contained(directory, object.get("module").getAsString());
				if (object.has("manifest")) {
					manifests.put(directory, DependencyResolver.contained(directory, object.get("manifest").getAsString()));
				}
				for (JsonElement value : object.getAsJsonArray("files")) {
					String relative = value.getAsJsonObject().get("file").getAsString();
					File file = DependencyResolver.contained(directory, relative);
					String lower = file.getName().toLowerCase(Locale.ROOT);
					if ((!lower.endsWith(".jar") && !lower.endsWith(".aar")) || lower.endsWith("-sources.jar")) {
						throw new IOException("本地运行依赖必须是 JAR/AAR 原包：" + file);
					}
					entries.add(new Entry("file:" + file.getName(), file));
				}
				return;
			}

			for (JsonElement value : object.getAsJsonArray("dependencies")) {
				JsonObject item = value.getAsJsonObject();
				String relative = item.get("file").getAsString();
				File file = new File(directory, relative).getCanonicalFile();
				if (new File(relative).isAbsolute() || !file.toPath().startsWith(directory.toPath()) || !file.isFile()) {
					throw new IOException("扩展库依赖路径无效或文件缺失：" + relative + "\n" + index);
				}
				String lower = file.getName().toLowerCase(Locale.ROOT);
				if ((!lower.endsWith(".jar") && !lower.endsWith(".aar")) || lower.endsWith("-sources.jar")) {
					throw new IOException("扩展库运行依赖必须为 JAR 或 AAR：" + file);
				}
				entries.add(new Entry(item.get("id").getAsString(), file, item.has("classifier") ? item.get("classifier").getAsString() : ""));
			}
			JsonArray relationships = object.getAsJsonArray("graph");
			if (relationships != null) {
				for (JsonElement value : relationships) {
					JsonObject item = value.getAsJsonObject();
					List<String> children = new ArrayList<String>();
					for (JsonElement child : item.getAsJsonArray("dependencies")) {
						children.add(child.getAsString());
					}
					graph.put(item.get("id").getAsString(), children);
				}
			}
		} catch (RuntimeException failure) {
			throw new IOException("扩展库依赖描述无效：" + index + "\n" + failure.getMessage(), failure);
		}
	}

	/**
	 * 合并旧式快照中的同坐标候选；新式模块已由 Gradle 完成选版。
	 *
	 * @param entry 当前候选依赖。
	 * @throws IOException 无法安全选择依赖。
	 */
	private void choose(Entry entry) throws IOException {
		Entry previous = selected.get(entry.group);
		if (previous == null) {
			selected.put(entry.group, entry);
			return;
		}
		int comparison = 0;
		if (!entry.version.equals(previous.version)) {
			comparison = Dependencies.compareVersion(entry.version, previous.version);
		}
		if (comparison > 0) {
			selected.put(entry.group, entry);
		}
	}

	/**
	 * 返回全局或指定扩展库所选的原始依赖，依赖在使用者之前排序。
	 *
	 * @param roots 指定库目录；为空时返回全部已装配依赖。
	 * @return 已去重并排序的依赖声明。
	 * @throws IOException 路径无法规范化或依赖图存在环。
	 */
	private List<Entry> needed(List<File> roots) throws IOException {
		Set<Entry> candidates = new LinkedHashSet<Entry>();
		if (roots == null) {
			candidates.addAll(selected.values());
		} else {
			for (File root : roots) {
				List<Entry> entries = libraries.get(root.getCanonicalFile());
				if (entries != null) {
					for (Entry entry : entries) {
						candidates.add(selected.get(entry.group));
					}
				}
			}
		}
		Map<String, Entry> byId = new LinkedHashMap<String, Entry>();
		for (Entry entry : declarations) {
			byId.put(entry.id, selected.get(entry.group));
		}
		List<Entry> ordered = new ArrayList<Entry>();
		Set<Entry> visiting = new LinkedHashSet<Entry>();
		Set<Entry> visited = new LinkedHashSet<Entry>();
		for (Entry entry : candidates) {
			visit(entry, byId, visiting, visited, ordered);
		}
		return ordered;
	}

	/**
	 * 以深度优先顺序处理依赖边，避免资源覆盖次序依赖文件枚举结果。
	 *
	 * @param entry 当前节点。
	 * @param byId 本次参与的依赖身份。
	 * @param visiting 当前递归栈。
	 * @param visited 已完成节点。
	 * @param ordered 排序结果。
	 * @throws IOException 依赖关系存在环。
	 */
	private void visit(Entry entry, Map<String, Entry> byId, Set<Entry> visiting, Set<Entry> visited,
			List<Entry> ordered) throws IOException {
		if (visited.contains(entry)) {
			return;
		}
		if (!visiting.add(entry)) {
			throw new IOException("扩展库依赖关系存在环：" + entry.id);
		}
		List<String> children = graph.get(entry.id);
		if (children != null) {
			for (String child : children) {
				Entry dependency = byId.get(child);
				if (dependency != null) {
					visit(dependency, byId, visiting, visited, ordered);
				}
			}
		}
		visiting.remove(entry);
		visited.add(entry);
		ordered.add(entry);
	}

	/**
	 * 返回加载器、Java 资源及 D8 共用的 JAR 路径，展开 AAR 中的代码和内嵌 JAR。
	 *
	 * @param roots 指定库目录；为空时返回全局类路径。
	 * @return 按内容去重的 JAR 文件。
	 * @throws IOException 输入无法读取。
	 */
	public List<File> getJars(List<File> roots) throws IOException {
		Map<String, File> jars = new LinkedHashMap<String, File>();
		Map<String, Entry> independent = new LinkedHashMap<String, Entry>();
		List<Entry> candidates = new ArrayList<Entry>();
		for (Entry entry : needed(roots)) {
			AndroidLibrary android = androidLibraries.get(entry.file);
			if (android == null) {
				candidates.add(jarIdentity(entry.file, entry));
			} else {
				// classes.jar 是 AAR 本体，不能按这个通用文件名跨 AAR 选版。
				File classes = new File(android.directory, "classes.jar");
				if (classes.isFile()) jars.putIfAbsent(digest(classes), classes);
				for (File embedded : list(android.directory, "libs", ".jar")) {
					candidates.add(new Entry(jarIdentity(embedded, null), android.source + "!/libs/" + embedded.getName()));
				}
			}
		}
		// 名称回退只有在候选中对应唯一明确模块时才归一；不同组织的同名模块保持独立。
		Map<String, Set<String>> names = new HashMap<String, Set<String>>();
		for (Entry candidate : candidates) {
			String[] coordinate = candidate.id.split(":", -1);
			if (coordinate.length == 3 && candidate.group.equals(coordinate[0] + ":" + coordinate[1])) {
				Set<String> identities = names.get(coordinate[1]);
				if (identities == null) {
					identities = new LinkedHashSet<String>();
					names.put(coordinate[1], identities);
				}
				identities.add(candidate.group);
			}
		}
		for (int index = 0; index < candidates.size(); index++) {
			Entry candidate = candidates.get(index);
			if (candidate.group.startsWith("legacy:")) {
				Set<String> identities = names.get(candidate.group.substring("legacy:".length()));
				if (identities != null && identities.size() == 1) {
					candidate = new Entry(new Entry(identities.iterator().next() + ":" + candidate.version, candidate.file), candidate.source);
					candidates.set(index, candidate);
				}
			}
			// 不能在 Gradle 已处理约束后，以本地文件选版悄悄覆盖其模块结果。
			Entry resolved = resolvedJars.get(candidate.group);
			if (resolved != null && !resolved.digest.equals(candidate.digest)) {
				throw new IOException("本地 JAR 与 Gradle 选定模块冲突，请统一其声明或本地原包："
						+ candidate.id + " / " + resolved.id + "\n来源：" + candidate.source + "\n来源：" + resolved.source);
			}
		}

		for (Entry candidate : candidates) {
			Entry previous = independent.get(candidate.group);
			if (previous == null || new ComparableVersion(candidate.version).compareTo(new ComparableVersion(previous.version)) > 0) {
				independent.put(candidate.group, candidate);
			}
		}
		for (Entry candidate : candidates) {
			Entry choice = independent.get(candidate.group);
			if (new ComparableVersion(candidate.version).compareTo(new ComparableVersion(choice.version)) == 0
					&& !candidate.digest.equals(choice.digest)) {
				throw new IOException("独立 JAR 同身份同版本但内容不同：" + choice.id + "\n来源：" + choice.source + "\n来源：" + candidate.source);
			}
		}
		for (Entry choice : independent.values()) jars.putIfAbsent(choice.digest, choice.file);
		return new ArrayList<File>(jars.values());
	}

	/**
	 * 优先采用明确坐标，其次读取唯一的 Maven 身份，最后识别约定文件名。
	 *
	 * <p>含多个不同 Maven 身份的聚合包不推测归属；无可靠版本的文件只按内容去重。
	 * 元数据仅用于识别本地文件，不自动引入其 POM 中未声明的传递依赖。
	 */
	private static Entry jarIdentity(File file, Entry declared) throws IOException {
		if (declared != null && !declared.id.startsWith("file:") && !declared.id.startsWith("legacy:")) return declared;
		Set<String> identities = new LinkedHashSet<String>();
		try (JarFile jar = new JarFile(file)) {
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if (entry.getName().startsWith("META-INF/maven/") && entry.getName().endsWith("/pom.properties")) {
					Properties properties = new Properties();
					try (InputStream input = jar.getInputStream(entry)) { properties.load(input); }
					String group = properties.getProperty("groupId", "").trim();
					String name = properties.getProperty("artifactId", "").trim();
					String version = properties.getProperty("version", "").trim();
					if (!group.isEmpty() && !name.isEmpty() && !version.isEmpty()
							&& !group.contains(":") && !name.contains(":") && !version.contains(":")) {
						identities.add(group + ":" + name + ":" + version);
					}
				}
			}
		}
		if (identities.size() == 1) return new Entry(identities.iterator().next(), file);
		return new Entry((identities.isEmpty() ? "legacy:" : "file:") + file.getName(), file);
	}

	/**
	 * 返回依赖与扩展库本体的 Android 输入；低优先级依赖排在使用者之前。
	 *
	 * @param roots 项目实际引用的扩展库。
	 * @return 包含纯资源 AAR 的 Android 输入列表。
	 * @throws IOException 路径或清单无效。
	 */
	public List<AndroidLibrary> getAndroidLibraries(List<File> roots) throws IOException {
		Map<String, AndroidLibrary> result = new LinkedHashMap<String, AndroidLibrary>();
		for (Entry entry : needed(roots)) {
			AndroidLibrary library = androidLibraries.get(entry.file);
			if (library != null) {
				result.putIfAbsent(entry.digest, library);
			}
		}
		for (File root : roots) {
			File directory = root.getCanonicalFile();
			String namespace = namespaces.get(directory);
			if (namespace == null && new File(directory, "AndroidManifest.xml").isFile()) {
				namespace = AndroidLibraries.readNamespace(directory);
			}
			File manifest = manifests.getOrDefault(directory, new File(directory, "AndroidManifest.xml"));
			result.put(directory.toString(), new AndroidLibrary(directory, namespace, directory, manifest));
		}
		return new ArrayList<AndroidLibrary>(result.values());
	}

	/**
	 * 按原始内容身份展开 AAR，并校验每个 ZIP 条目始终位于构建目录中。
	 *
	 * @param entry 已选中的 AAR。
	 * @return 内容身份对应的展开目录。
	 * @throws IOException 归档损坏、包含越界或重复路径，或者缺少清单。
	 */
	private File unpack(Entry entry) throws IOException {
		File base = workDirectory;
		if (base == null) {
			base = Files.createTempDirectory("simple-aar-").toFile();
		}
		File directory = new File(base, entry.digest).getCanonicalFile();
		Files.createDirectories(directory.toPath());
		Set<String> names = new LinkedHashSet<String>();
		try (ZipFile zip = new ZipFile(entry.file)) {
			Enumeration<? extends ZipEntry> values = zip.entries();
			while (values.hasMoreElements()) {
				ZipEntry value = values.nextElement();
				File output = new File(directory, value.getName()).getCanonicalFile();
				if (!output.toPath().startsWith(directory.toPath()) || !names.add(output.toString())) {
					throw new IOException("AAR 包含无效或重复路径：" + value.getName() + "\n" + entry.file);
				}
				if (value.isDirectory()) {
					continue;
				}
				Files.createDirectories(output.getParentFile().toPath());
				try (InputStream input = zip.getInputStream(value)) {
					byte[] bytes = input.readAllBytes();
					// 相同内容不重复覆盖，避免同 JVM 中仍被类加载器打开的归档被改写。
					if (!output.isFile() || !Arrays.equals(bytes, Files.readAllBytes(output.toPath()))) {
						Files.write(output.toPath(), bytes);
					}
				}
			}
		}
		if (!new File(directory, "AndroidManifest.xml").isFile()) {
			throw new IOException("AAR 缺少 AndroidManifest.xml：" + entry.file);
		}
		return directory;
	}

	/**
	 * 收集约定目录第一层的归档，排除源码附件并保持稳定顺序。
	 *
	 * @param root 类库根目录。
	 * @param name 子目录名。
	 * @param extension 归档后缀。
	 * @return 规范化的归档列表。
	 * @throws IOException 目录无法枚举。
	 */
	private static List<File> list(File root, String name, String extension) throws IOException {
		File directory = new File(root, name);
		if (!directory.exists()) {
			return Collections.emptyList();
		}
		File[] children = directory.listFiles();
		if (children == null) {
			throw new IOException("无法读取依赖目录：" + directory);
		}
		Arrays.sort(children);
		List<File> result = new ArrayList<File>();
		for (File file : children) {
			String lower = file.getName().toLowerCase(Locale.ROOT);
			if (file.isFile() && lower.endsWith(extension) && !lower.endsWith("-sources.jar")) {
				result.add(file.getCanonicalFile());
			}
		}
		return result;
	}

	/**
	 * 计算归档内容身份，用于选择、展开目录及去重。
	 *
	 * @param file 原始归档或已展开的 JAR。
	 * @return SHA-256 十六进制摘要。
	 * @throws IOException 文件无法读取。
	 */
	static String digest(File file) throws IOException {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			try (InputStream input = Files.newInputStream(file.toPath())) {
				byte[] buffer = new byte[8192];
				for (int count; (count = input.read(buffer)) != -1;) {
					digest.update(buffer, 0, count);
				}
			}
			StringBuilder result = new StringBuilder();
			for (byte value : digest.digest()) {
				result.append(String.format(Locale.ROOT, "%02x", value & 255));
			}
			return result.toString();
		} catch (NoSuchAlgorithmException failure) {
			throw new IllegalStateException(failure);
		}
	}
}

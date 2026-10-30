package simple.compiler.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 处理可复制的 Maven 离线仓库，不解析依赖版本，也不直接访问网络。
 *
 * <p>Gradle 提供已选中的原始制品与原生描述文件，本类保留其内容、补齐父 POM 和 BOM，
 * 合并多个仓库并拒绝同一路径的不同内容。编译器和类库工具复用本类，不相互调用构建脚本。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class MavenRepository {

	/**
	 * 由调用方使用 Gradle 获取原始描述文件；不存在的可选格式返回空值。
	 */
	public interface MetadataSource {
		/**
		 * 取得指定坐标的原始 POM 或 Gradle Module Metadata。
		 *
		 * @param coordinate 组织、模块、版本组成的 Maven 坐标。
		 * @param extension pom 或 module。
		 * @return 描述文件，不存在时返回 null。
		 * @throws IOException 原始文件无法取得。
		 */
		File resolve(String coordinate, String extension) throws IOException;
	}

	/**
	 * 一个已由 Gradle 选中的原始运行制品。
	 */
	public static final class Artifact {
		/** Maven 模块坐标。 */
		public final String coordinate;
		/** 原始 JAR/AAR，不是 AGP 转换结果。 */
		public final File file;

		/**
		 * 创建不改变坐标或文件名的制品记录。
		 *
		 * @param coordinate Maven 模块坐标。
		 * @param file 原始文件。
		 */
		public Artifact(String coordinate, File file) {
			this.coordinate = coordinate;
			this.file = file;
		}
	}

	/**
	 * 禁止实例化只提供静态仓库操作的工具类。
	 */
	private MavenRepository() {
	}

	/**
	 * 对所有补充仓库的路径和实际文件内容取指纹，不使用个人缓存布局。
	 *
	 * @param repositories 实际参与本轮的仓库目录。
	 * @return 稳定的 SHA-256 指纹。
	 * @throws IOException 仓库内容不可读取或经过链接。
	 */
	public static String fingerprint(List<File> repositories) throws IOException {
		MessageDigest digest = digest();
		for (File repository : repositories) {
			digest.update(repository.getAbsolutePath().getBytes(StandardCharsets.UTF_8));
			for (File file : files(repository)) {
				digest.update(repository.toPath().relativize(file.toPath()).toString().getBytes(StandardCharsets.UTF_8));
				digest.update(Files.readAllBytes(file.toPath()));
			}
		}
		return hex(digest.digest());
	}

	/**
	 * 在独立暂存目录合并仓库，完成校验后同步目标文件，失败时恢复原目标内容。
	 *
	 * @param repositories 可互补的仓库目录；合并准备阶段只读，可包含目标仓库本身。
	 * @param destination 本轮拥有的合并仓库目录。
	 * @throws IOException 内容冲突、路径无效或写入失败。
	 */
	public static void merge(List<File> repositories, File destination) throws IOException {
		File pending = new File(destination.getAbsoluteFile().getParentFile(), ".repository-" + UUID.randomUUID());
		Files.createDirectories(pending.toPath());
		try {
			Map<String, File> origins = new LinkedHashMap<String, File>();
			for (File repository : repositories) {
				for (File source : files(repository)) {
					String relative = repository.toPath().relativize(source.toPath()).toString().replace('\\', '/');
					// 版本列表由合并后的实际目录重建，不以某个仓库的局部列表覆盖其它版本。
					if (source.getName().equals("maven-metadata.xml") || source.getName().startsWith("maven-metadata.xml.")) {
						continue;
					}
					File previous = origins.get(relative);
					if (previous != null && !Arrays.equals(Files.readAllBytes(previous.toPath()), Files.readAllBytes(source.toPath()))) {
						throw new IOException("离线依赖内容冲突：" + relative + "\n" + previous + "\n" + source);
					}
					origins.put(relative, source);
					copy(source, new File(pending, relative));
				}
			}
			// 带有 Gradle 重定向标记的 POM 不能冒充缺失的原生元数据。
			// 不把这份不完整描述注册给 Gradle；联网时仍可从后续仓库获取完整描述。
			for (File file : files(pending)) {
				if (file.getName().endsWith(".pom") && redirected(file) &&
						!new File(file.getParentFile(), stem(file) + ".module").isFile()) {
					Files.delete(file.toPath());
				}
			}
			versionLists(pending);
			append(pending, destination);
		} finally {
			remove(pending);
		}
	}

	/**
	 * 导出实际选中模块及原始描述；父 POM、导入 BOM 递归保留，声明内容不改写。
	 *
	 * @param destination 离线仓库目标。
	 * @param artifacts 实际选中的原始制品。
	 * @param components 实际选中模块，包括仅有描述的 BOM。
	 * @param source 通过 Gradle 取得原始元数据的适配器。
	 * @throws IOException 缺少必要描述、内容冲突或写入失败。
	 */
	public static void export(File destination, List<Artifact> artifacts, Collection<String> components,
			MetadataSource source) throws IOException {
		File pending = new File(destination.getAbsoluteFile().getParentFile(), ".export-" + UUID.randomUUID());
		Files.createDirectories(pending.toPath());
		try {
			Exporter exporter = new Exporter(pending, source);
			for (String component : components) {
				exporter.metadata(component);
			}
			for (Artifact artifact : artifacts) {
				exporter.metadata(artifact.coordinate);
				File directory = directory(pending, artifact.coordinate);
				File target = new File(directory, artifact.file.getName());
				copy(artifact.file, target);
				exporter.checkArtifact(directory, artifact.file);
			}
			// 保留已有其它版本；不允许同一坐标被新的不同内容静默覆盖。
			List<File> sources = new ArrayList<File>();
			sources.add(destination);
			sources.add(pending);
			// 合并入口已负责独立暂存、校验与失败恢复，不再另建已验证副本和第二次提交。
			merge(sources, destination);
		} finally {
			remove(pending);
		}
	}

	/**
	 * 向可能被 Gradle 使用的持久仓库提交新文件，不移动其根目录或已有原包。
	 *
	 * @param source 已完成冲突校验的完整仓库。
	 * @param destination 持久仓库。
	 * @throws IOException 写入失败；本轮新建文件和元数据修改会恢复。
	 */
	private static void append(File source, File destination) throws IOException {
		files(destination);
		List<File> created = new ArrayList<File>();
		Map<File, byte[]> previous = new LinkedHashMap<File, byte[]>();
		try {
			Set<String> incoming = new HashSet<String>();
			for (File input : files(source)) {
				incoming.add(source.toPath().relativize(input.toPath()).toString());
			}
			for (File old : files(destination)) {
				if (!incoming.contains(destination.toPath().relativize(old.toPath()).toString())) {
					previous.put(old, Files.readAllBytes(old.toPath()));
					Files.delete(old.toPath());
				}
			}
			for (File input : files(source)) {
				File output = new File(destination, source.toPath().relativize(input.toPath()).toString());
				byte[] content = Files.readAllBytes(input.toPath());
				if (output.isFile()) {
					byte[] old = Files.readAllBytes(output.toPath());
					if (Arrays.equals(old, content)) {
						continue;
					}
					previous.put(output, old);
				} else {
					created.add(output);
				}
				Files.createDirectories(output.toPath().getParent());
				Files.write(output.toPath(), content);
			}
		} catch (IOException failure) {
			for (File file : created) {
				Files.deleteIfExists(file.toPath());
			}
			for (Map.Entry<File, byte[]> item : previous.entrySet()) {
				Files.write(item.getKey().toPath(), item.getValue());
			}
			throw failure;
		}
	}

	/**
	 * 持有一次导出的父 POM、BOM 和原生变体转发递归状态。
	 */
	private static final class Exporter {
		private final File root;
		private final MetadataSource source;
		private final Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();
		private final Set<String> visiting = new HashSet<String>();

		/**
		 * 创建一次独立导出状态，不保存跨构建缓存。
		 *
		 * @param root 本轮暂存仓库。
		 * @param source Gradle 元数据读取器。
		 */
		private Exporter(File root, MetadataSource source) {
			this.root = root;
			this.source = source;
		}

		/**
		 * 保存模块原生描述并取得父 POM 继承后的属性。
		 *
		 * @param coordinate Maven 坐标。
		 * @return 用于定位父 POM 和导入 BOM 的有效属性。
		 * @throws IOException 元数据缺失、引用循环或 XML/JSON 无效。
		 */
		private Map<String, String> metadata(String coordinate) throws IOException {
			if (properties.containsKey(coordinate)) {
				return properties.get(coordinate);
			}
			if (!visiting.add(coordinate)) {
				throw new IOException("离线依赖元数据循环引用：" + coordinate);
			}
			try {
				File directory = directory(root, coordinate);
				String base = directory.getParentFile().getName() + "-" + directory.getName();
				File pom = source.resolve(coordinate, "pom");
				File module = source.resolve(coordinate, "module");
				if (pom == null && module == null) {
					throw new IOException("无法准备离线依赖描述：" + coordinate + "（需要原始 POM 或 .module）");
				}
				if (pom != null && redirected(pom) && module == null) {
					throw new IOException("缺少 POM 引用的 Gradle 元数据：" + coordinate + ".module");
				}
				if (module != null) {
					copy(module, new File(directory, base + ".module"));
				}
				Map<String, String> values = new LinkedHashMap<String, String>();
				String[] id = coordinate.split(":", -1);
				values.put("project.groupId", id[0]);
				values.put("project.artifactId", id[1]);
				values.put("project.version", id[2]);
				values.put("pom.groupId", id[0]);
				values.put("pom.version", id[2]);
				if (pom != null) {
					copy(pom, new File(directory, base + ".pom"));
					Element project = xml(pom).getDocumentElement();
					Map<String, String> local = new LinkedHashMap<String, String>(values);
					Element declared = child(project, "properties");
					if (declared != null) {
						for (Element property : children(declared)) {
							local.put(property.getLocalName(), property.getTextContent().trim());
						}
					}
					Element parent = child(project, "parent");
					if (parent != null) {
						String parentId = reference(parent, local);
						values.putAll(metadata(parentId));
						String[] parentParts = parentId.split(":", -1);
						local.put("project.parent.groupId", parentParts[0]);
						local.put("project.parent.artifactId", parentParts[1]);
						local.put("project.parent.version", parentParts[2]);
					}
					values.putAll(local);
					// 先登记当前属性，再遍历 BOM；合法 BOM 可能继承同一个父描述。
					properties.put(coordinate, values);
					Element management = child(project, "dependencyManagement");
					Element dependencies = management == null ? null : child(management, "dependencies");
					if (dependencies != null) {
						for (Element dependency : children(dependencies)) {
							if ("import".equals(text(dependency, "scope")) && "pom".equals(text(dependency, "type"))) {
								metadata(reference(dependency, values));
							}
						}
					}
				}
				properties.put(coordinate, values);
				// 多平台模块可将变体转发到另一坐标；保留原描述，并补齐目标描述而不展开重写。
				if (module != null) {
					try {
						JsonObject metadata = JsonParser.parseString(Files.readString(module.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
						for (JsonElement variant : metadata.getAsJsonArray("variants")) {
							JsonObject value = variant.getAsJsonObject();
							if (value.has("available-at")) {
								JsonObject target = value.getAsJsonObject("available-at");
								metadata(target.get("group").getAsString() + ":" + target.get("module").getAsString()
										+ ":" + target.get("version").getAsString());
							}
						}
					} catch (RuntimeException failure) {
						throw new IOException("Gradle 元数据无效：" + module, failure);
					}
				}
				return values;
			} finally {
				visiting.remove(coordinate);
			}
		}

		/**
		 * 核对原生元数据为当前制品声明的摘要，避免携带损坏的离线原包。
		 *
		 * @param directory 模块版本目录。
		 * @param artifact 已复制的原包。
		 * @throws IOException 摘要不符或原生元数据无效。
		 */
		private void checkArtifact(File directory, File artifact) throws IOException {
			File metadata = new File(directory, directory.getParentFile().getName() + "-" + directory.getName() + ".module");
			if (!metadata.isFile()) {
				return;
			}
			try {
				JsonObject module = JsonParser.parseString(Files.readString(metadata.toPath(), StandardCharsets.UTF_8)).getAsJsonObject();
				for (JsonElement variant : module.getAsJsonArray("variants")) {
					JsonObject value = variant.getAsJsonObject();
					if (!value.has("files")) {
						continue;
					}
					for (JsonElement entry : value.getAsJsonArray("files")) {
						JsonObject file = entry.getAsJsonObject();
						if (artifact.getName().equals(file.get("name").getAsString()) && file.has("sha256")) {
							String expected = file.get("sha256").getAsString();
							if (!expected.equals(hex(digest().digest(Files.readAllBytes(artifact.toPath()))))) {
								throw new IOException("离线依赖摘要不符：" + artifact);
							}
						}
					}
				}
			} catch (RuntimeException failure) {
				throw new IOException("Gradle 元数据无效：" + metadata, failure);
			}
		}
	}

	/**
	 * 将受限 Maven 坐标映射到标准仓库目录。
	 *
	 * @param root 仓库根目录。
	 * @param coordinate 组织、模块和版本。
	 * @return 模块版本目录。
	 * @throws IOException 坐标包含路径逃逸或非法字符。
	 */
	public static File directory(File root, String coordinate) throws IOException {
		String[] id = coordinate.split(":", -1);
		if (id.length != 3) {
			throw new IOException("Maven 坐标无效：" + coordinate);
		}
		for (String part : id) {
			if (!part.matches("[A-Za-z0-9_][A-Za-z0-9_.+\\-]*") || part.contains("..")) {
				throw new IOException("Maven 坐标无效：" + coordinate);
			}
		}
		return new File(root, id[0].replace('.', '/') + "/" + id[1] + "/" + id[2]);
	}

	/**
	 * 读取普通文件树，拒绝目录联接及符号链接，排序保证指纹稳定。
	 *
	 * @param root 可不存在的仓库目录。
	 * @return 所有普通文件。
	 * @throws IOException 仓库类型不正确或路径经过链接。
	 */
	private static List<File> files(File root) throws IOException {
		List<File> result = new ArrayList<File>();
		if (!root.exists()) {
			return result;
		}
		if (!root.isDirectory()) {
			throw new IOException("离线仓库不是目录：" + root);
		}
		Files.walkFileTree(root.toPath(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attributes) throws IOException {
				if (!path.toRealPath().equals(path.toAbsolutePath().normalize())) {
					throw new IOException("离线仓库不能经过链接：" + path);
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path path, BasicFileAttributes attributes) throws IOException {
				if (attributes.isSymbolicLink() || !attributes.isRegularFile()) {
					throw new IOException("离线仓库必须为普通文件：" + path);
				}
				result.add(path.toFile());
				return FileVisitResult.CONTINUE;
			}
		});
		Collections.sort(result);
		return result;
	}

	/**
	 * 删除本类拥有的暂存目录，不跟随链接。
	 *
	 * @param directory 暂存目录。
	 * @throws IOException 清理失败。
	 */
	private static void remove(File directory) throws IOException {
		List<File> entries = files(directory);
		if (!directory.exists()) {
			return;
		}
		for (File file : entries) {
			Files.delete(file.toPath());
		}
		Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult postVisitDirectory(Path path, IOException failure) throws IOException {
				if (failure != null) {
					throw failure;
				}
				Files.delete(path);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * 复制原始文件，不改写其内容。
	 *
	 * @param source 原始文件。
	 * @param destination 目标文件。
	 * @throws IOException 文件无效或复制失败。
	 */
	private static void copy(File source, File destination) throws IOException {
		if (!source.isFile() || source.getName().contains("..")) {
			throw new IOException("离线依赖文件无效：" + source);
		}
		Files.createDirectories(destination.toPath().getParent());
		Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}

	/**
	 * 重建固定版本目录列表，支持离线解析已准备的动态版本范围。
	 *
	 * @param root 合并仓库根目录。
	 * @throws IOException 描述文件无法读取或列表无法写入。
	 */
	private static void versionLists(File root) throws IOException {
		Map<File, Set<String>> modules = new HashMap<File, Set<String>>();
		for (File file : files(root)) {
			if (file.getName().endsWith(".pom") || file.getName().endsWith(".module")) {
				File version = file.getParentFile();
				String expected = version.getParentFile().getName() + "-" + version.getName();
				if (stem(file).equals(expected)) {
					Set<String> versions = modules.get(version.getParentFile());
					if (versions == null) {
						versions = new HashSet<String>();
						modules.put(version.getParentFile(), versions);
					}
					versions.add(version.getName());
				}
			}
		}
		for (Map.Entry<File, Set<String>> entry : modules.entrySet()) {
			List<String> versions = new ArrayList<String>(entry.getValue());
			Collections.sort(versions);
			String group = root.toPath().relativize(entry.getKey().getParentFile().toPath()).toString().replace(File.separatorChar, '.');
			StringBuilder value = new StringBuilder("<metadata><groupId>" + group + "</groupId><artifactId>"
					+ entry.getKey().getName() + "</artifactId><versioning><versions>");
			for (String version : versions) {
				value.append("<version>").append(version).append("</version>");
			}
			value.append("</versions></versioning></metadata>\r\n");
			Files.writeString(new File(entry.getKey(), "maven-metadata.xml").toPath(), value.toString(), StandardCharsets.UTF_8);
		}
	}

	/**
	 * 读取禁用外部实体和 DTD 的 Maven XML。
	 *
	 * @param file POM 文件。
	 * @return XML 文档。
	 * @throws IOException XML 无效。
	 */
	private static Document xml(File file) throws IOException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			return factory.newDocumentBuilder().parse(file);
		} catch (Exception failure) {
			throw new IOException("POM 无效：" + file, failure);
		}
	}

	/**
	 * 获取直接子元素，避免把传递依赖中的同名字段当作项目字段。
	 *
	 * @param element 父元素。
	 * @param name 子元素名称。
	 * @return 子元素或空值。
	 */
	private static Element child(Element element, String name) {
		for (Element item : children(element)) {
			if (name.equals(item.getLocalName())) {
				return item;
			}
		}
		return null;
	}

	/**
	 * 获取元素的直接子元素列表。
	 *
	 * @param element 父元素。
	 * @return 按原顺序排列的子元素。
	 */
	private static List<Element> children(Element element) {
		List<Element> result = new ArrayList<Element>();
		NodeList nodes = element.getChildNodes();
		for (int index = 0; index < nodes.getLength(); index++) {
			Node node = nodes.item(index);
			if (node instanceof Element) {
				result.add((Element) node);
			}
		}
		return result;
	}

	/**
	 * 取得一个直接字段的文本。
	 *
	 * @param element 父元素。
	 * @param name 字段名。
	 * @return 去除外围空白后的字段值。
	 */
	private static String text(Element element, String name) {
		Element value = child(element, name);
		return value == null ? "" : value.getTextContent().trim();
	}

	/**
	 * 用当前 POM 的有效属性定位父描述或导入 BOM。
	 *
	 * @param element 坐标元素。
	 * @param properties 当前有效属性。
	 * @return 已展开的 Maven 坐标。
	 * @throws IOException 属性无法展开。
	 */
	private static String reference(Element element, Map<String, String> properties) throws IOException {
		String value = text(element, "groupId") + ":" + text(element, "artifactId") + ":" + text(element, "version");
		for (int round = 0; round < properties.size() + 1; round++) {
			String previous = value;
			for (Map.Entry<String, String> property : properties.entrySet()) {
				value = value.replace("${" + property.getKey() + "}", property.getValue());
			}
			if (previous.equals(value)) {
				break;
			}
		}
		if (value.contains("${")) {
			throw new IOException("离线 POM 属性无法展开：" + value);
		}
		return value;
	}

	/**
	 * 判断 POM 是否要求保留原生 Gradle 元数据。
	 *
	 * @param pom 原始 POM。
	 * @return 是否包含上游发布重定向标记。
	 * @throws IOException 文件无法读取。
	 */
	private static boolean redirected(File pom) throws IOException {
		return Files.readString(pom.toPath(), StandardCharsets.UTF_8).contains("published-with-gradle-metadata");
	}

	/**
	 * 去掉文件的最后一个扩展名。
	 *
	 * @param file 文件。
	 * @return 不含最后扩展名的名称。
	 */
	private static String stem(File file) {
		return file.getName().substring(0, file.getName().lastIndexOf('.'));
	}

	/**
	 * 创建 JDK 必须提供的摘要实现。
	 *
	 * @return SHA-256 摘要。
	 */
	private static MessageDigest digest() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException failure) {
			throw new IllegalStateException(failure);
		}
	}

	/**
	 * 将摘要编码为十六进制字符串。
	 *
	 * @param value 摘要字节。
	 * @return 小写十六进制内容。
	 */
	private static String hex(byte[] value) {
		StringBuilder result = new StringBuilder();
		for (byte item : value) {
			result.append(String.format(Locale.ROOT, "%02x", item & 255));
		}
		return result.toString();
	}
}

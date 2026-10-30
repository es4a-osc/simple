package simple.compiler.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * 收集已选 JAR 的 Java 资源，供 APK 统一组装时写入。
 *
 * <p>服务注册按标准语法合并，普通资源冲突明确报错；不修改依赖 JAR，
 * 不负责 Android 资源链接、DEX 转换、原生库选择或签名。
 *
 * @author 树先生 xhwsd@qq.com
 */
final class JarResources {

	private static final String SERVICES = "META-INF/services/";

	// 使用路径排序，避免 JAR 内部条目顺序影响最终 APK。
	private final Map<String, Resource> resources = new TreeMap<String, Resource>();

	/**
	 * 在 APK 组装前读取资源并检查冲突，保留调用方已经确定的 JAR 顺序。
	 *
	 * @param jars 实际使用的扩展库及其已选依赖 JAR。
	 * @throws IOException JAR 损坏、资源冲突或服务文件无效。
	 */
	JarResources(List<File> jars) throws IOException {
		Map<String, List<Resource>> collected = new TreeMap<String, List<Resource>>();
		Set<File> seen = new LinkedHashSet<File>();

		// 同一输入只读取一次；保留完整来源，避免资源冲突被目录顺序掩盖。
		for (File file : jars) {
			if (!seen.add(file.getAbsoluteFile())) {
				continue;
			}
			try (JarFile jar = new JarFile(file)) {
				Enumeration<JarEntry> entries = jar.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					String name = entry.getName();
					if (entry.isDirectory() || !isResource(name)) {
						continue;
					}

					validatePath(name);
					try (InputStream input = jar.getInputStream(entry)) {
						add(collected, name, new Resource(file, read(input)));
					}
				}
			} catch (IOException failure) {
				throw new IOException("读取 JAR 资源失败：" + file + "\n" + failure.getMessage(), failure);
			}
		}

		// 提前完成文本检查与合并；有错误时不进入后续 APK 生成阶段。
		for (Map.Entry<String, List<Resource>> entry : collected.entrySet()) {
			String name = entry.getKey();
			List<Resource> values = entry.getValue();
			byte[] data = values.get(0).data;
			if (name.startsWith(SERVICES)) {
				data = mergeServices(name, values);
			} else if (values.size() > 1) {
				data = mergeNotices(name, values);
			}
			resources.put(name, new Resource(values.get(0).owner, data));
		}
	}

	/**
	 * 将 Java 资源写入正在组装的 APK，不创建另一份归档或接管输出流。
	 *
	 * <p>与已有条目同名时直接核对原始输入；相同内容保留已有条目的压缩方式，
	 * 不同内容报告双方来源。归档关闭和失败保护统一由组装入口处理。
	 *
	 * @param output 正在组装的归档。
	 * @param androidResources AAPT2 已链接的资源包。
	 * @param entries 已写入条目及其原始输入，资源包内条目以资源包文件为来源。
	 * @throws IOException 原始输入无法读取、资源冲突或写入失败。
	 */
	void writeTo(ZipOutputStream output, File androidResources, Map<String, File> entries) throws IOException {
		if (resources.isEmpty()) {
			return;
		}

		// 只有发生同名碰撞的内容需要重新读取，比较的是原输入，不读取正在写入的 APK。
		try (ZipFile linked = new ZipFile(androidResources)) {
			for (Map.Entry<String, Resource> entry : resources.entrySet()) {
				String name = entry.getKey();
				Resource resource = entry.getValue();
				File previous = entries.get(name);
				if (previous != null) {
					ZipEntry linkedEntry = previous.equals(androidResources) ? linked.getEntry(name) : null;
					try (InputStream input = linkedEntry == null ? Files.newInputStream(previous.toPath())
							: linked.getInputStream(linkedEntry)) {
						if (!Arrays.equals(read(input), resource.data)) {
							throw new IOException("JAR 资源与 APK 条目冲突：" + name
								+ "\n来源：" + previous + "\n来源：" + resource.owner);
						}
					}
					continue;
				}

				output.putNextEntry(new ZipEntry(name));
				output.write(resource.data);
				output.closeEntry();
			}
		}
	}

	/**
	 * 排除程序输入、源码、Maven 构建元数据及失效的归档元数据，保留其它 Java 资源。
	 *
	 * @param name JAR 条目路径。
	 * @return 需要参与 APK 资源合并时返回 {@code true}。
	 */
	private static boolean isResource(String name) {
		if (name.endsWith(".class") || name.endsWith(".java") || name.endsWith(".aidl")
				|| name.startsWith("META-INF/versions/")) {
			return false;
		}

		// Maven 构建元数据不参与 APK 运行，排除该目录但保留服务注册等其它资源。
		if (name.startsWith("META-INF/maven/")) {
			return false;
		}

		String upper = name.toUpperCase(Locale.ROOT);
		if (upper.equals("META-INF/MANIFEST.MF") || upper.equals("META-INF/INDEX.LIST")) {
			return false;
		}
		if (upper.startsWith("META-INF/")) {
			String file = upper.substring("META-INF/".length());
			if (file.indexOf('/') < 0 && (file.startsWith("SIG-") || file.endsWith(".SF")
					|| file.endsWith(".RSA") || file.endsWith(".DSA") || file.endsWith(".EC"))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 检查归档路径，拒绝非规范路径而不是静默重命名资源。
	 *
	 * @param name 归档中的资源路径。
	 * @throws IOException 路径为空、绝对路径或包含回退与非规范分隔。
	 */
	static void validatePath(String name) throws IOException {
		if (name.isEmpty() || name.startsWith("/") || name.indexOf('\\') >= 0 || name.indexOf(':') >= 0) {
			throw new IOException("无效归档资源路径：" + name);
		}
		for (String part : name.split("/", -1)) {
			if (part.isEmpty() || part.equals(".") || part.equals("..")) {
				throw new IOException("无效归档资源路径：" + name);
			}
		}
	}

	/**
	 * 对相同内容去重，只允许服务文件和文本许可声明进行多来源合并。
	 *
	 * @param collected 当前资源集合。
	 * @param name 资源归档路径。
	 * @param value 当前来源与内容。
	 * @throws IOException 普通资源同名但内容不同。
	 */
	private static void add(Map<String, List<Resource>> collected, String name, Resource value) throws IOException {
		List<Resource> values = collected.get(name);
		if (values == null) {
			values = new ArrayList<Resource>();
			collected.put(name, values);
		}
		for (Resource previous : values) {
			if (Arrays.equals(previous.data, value.data)) {
				return;
			}
			if (!name.startsWith(SERVICES) && !isNotice(name)) {
				throw new IOException("JAR 资源冲突：" + name + "\n来源：" + previous.owner
						+ "\n来源：" + value.owner);
			}
		}
		values.add(value);
	}

	/**
	 * 判断允许全文合并的许可和声明文件，避免把普通配置误当成文本声明。
	 *
	 * @param name 资源归档路径。
	 * @return 常见的文本许可或声明文件返回 {@code true}。
	 */
	private static boolean isNotice(String name) {
		String file = name.substring(name.lastIndexOf('/') + 1).toUpperCase(Locale.ROOT);
		return file.matches("(LICENSE|LICENCE|NOTICE|COPYING|DEPENDENCIES)(\\.(TXT|MD))?");
	}

	/**
	 * 按服务配置语法移除注释、空行并合并提供者，不替调用者选择服务实现。
	 *
	 * @param name 服务配置的归档路径。
	 * @param values 按输入顺序保存的来源和内容。
	 * @return UTF-8 编码、每行一个提供者的配置。
	 * @throws IOException 服务名称、提供者名称或 UTF-8 内容无效。
	 */
	private static byte[] mergeServices(String name, List<Resource> values) throws IOException {
		validateClassName(name.substring(SERVICES.length()), name);
		Set<String> providers = new LinkedHashSet<String>();
		for (Resource value : values) {
			for (String line : text(value, name).split("\\r\\n|\\n|\\r")) {
				int comment = line.indexOf('#');
				String provider = (comment < 0 ? line : line.substring(0, comment)).trim();
				if (!provider.isEmpty()) {
					validateClassName(provider, name + "（" + value.owner + "）");
					providers.add(provider);
				}
			}
		}

		StringBuilder merged = new StringBuilder();
		for (String provider : providers) {
			merged.append(provider).append('\n');
		}
		return merged.toString().getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * 校验服务类型和提供者的二进制类名，不加载或执行其中任何类型。
	 *
	 * @param name 点分隔的 Java 类名。
	 * @param origin 用于错误诊断的服务文件来源。
	 * @throws IOException 类名不符合 Java 标识符规则。
	 */
	private static void validateClassName(String name, String origin) throws IOException {
		for (String part : name.split("\\.", -1)) {
			if (part.isEmpty() || !Character.isJavaIdentifierStart(part.codePointAt(0))) {
				throw new IOException("无效服务类名：" + name + "\n来源：" + origin);
			}
			for (int index = Character.charCount(part.codePointAt(0)); index < part.length();) {
				int code = part.codePointAt(index);
				if (!Character.isJavaIdentifierPart(code)) {
					throw new IOException("无效服务类名：" + name + "\n来源：" + origin);
				}
				index += Character.charCount(code);
			}
		}
	}

	/**
	 * 合并不同来源的文本许可或声明，保留各段全文和原 JAR 文件名。
	 *
	 * @param name 声明文件的归档路径。
	 * @param values 内容不同的来源集合。
	 * @return 带来源分隔的 UTF-8 文本。
	 * @throws IOException 原声明不是有效 UTF-8 文本。
	 */
	private static byte[] mergeNotices(String name, List<Resource> values) throws IOException {
		StringBuilder merged = new StringBuilder();
		for (Resource value : values) {
			merged.append("--- ").append(value.owner.getName()).append(" ---\n");
			merged.append(text(value, name)).append("\n\n");
		}
		return merged.toString().getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * 严格读取 UTF-8 文本，避免合并时静默替换损坏字符。
	 *
	 * @param value 原始资源及来源。
	 * @param name 资源归档路径。
	 * @return 解码后的文本。
	 * @throws IOException 字节序列不是有效 UTF-8。
	 */
	private static String text(Resource value, String name) throws IOException {
		try {
			return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(value.data)).toString();
		} catch (CharacterCodingException failure) {
			throw new IOException("JAR 文本资源不是有效 UTF-8：" + name + "\n来源：" + value.owner, failure);
		}
	}

	/**
	 * 读取单个资源内容，输入流由调用方关闭。
	 *
	 * @param input 已打开的资源输入流。
	 * @return 原始资源字节。
	 * @throws IOException 资源读取失败。
	 */
	private static byte[] read(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		for (int count; (count = input.read(buffer)) != -1;) {
			output.write(buffer, 0, count);
		}
		return output.toByteArray();
	}

	/**
	 * 保存一个资源的原始内容和来源，供冲突诊断及合并使用。
	 */
	private static final class Resource {
		private final File owner;
		private final byte[] data;

		/**
		 * 建立资源内容与来源的对应关系。
		 *
		 * @param owner 原始 JAR 文件。
		 * @param data 原始资源字节。
		 */
		private Resource(File owner, byte[] data) {
			this.owner = owner;
			this.data = data;
		}
	}
}

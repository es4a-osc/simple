package simple.compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import simple.compiler.util.ClassFiles;

/**
 * 为一次编译选择扩展库依赖；加载、DEX 和 JAR 资源打包共用同一选择结果。
 *
 * <p>版本只取文件名末尾的数字段，不改写依赖，也不推断其二进制兼容性。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class Dependencies {

	// 只有文件名末尾的数字版本参与选版，预发布后缀等名称按普通依赖处理。
	private static final Pattern VERSIONED_JAR =
			Pattern.compile("^(.+)-([0-9]+(?:\\.[0-9]+)*)\\.jar$", Pattern.CASE_INSENSITIVE);

	// 路径排序保持输入稳定；忽略大小写相同时再按原始路径区分。
	private static final Comparator<File> FILE_ORDER = new Comparator<File>() {
		/**
		 * 按绝对路径比较两个依赖文件，使选择结果不依赖目录枚举顺序。
		 *
		 * @param left 左侧文件。
		 * @param right 右侧文件。
		 * @return 负数表示左侧在前，零表示路径相同，正数表示右侧在前。
		 */
		@Override
		public int compare(File left, File right) {
			int result = left.getAbsolutePath().compareToIgnoreCase(right.getAbsolutePath());
			return result != 0 ? result : left.getAbsolutePath().compareTo(right.getAbsolutePath());
		}
	};

	// 保存各类库原始依赖，供项目筛选时追溯到全局已选文件。
	private final Map<File, List<File>> libraryFiles = new LinkedHashMap<File, List<File>>();

	// 将每个候选路径映射到选版和内容去重后的最终路径。
	private final Map<File, File> selectedFiles = new HashMap<File, File>();

	// 全部已选依赖的只读列表，按首次出现的稳定顺序保留。
	private final List<File> files;

	/**
	 * 按已安装类库建立稳定的依赖选择快照。
	 *
	 * @param libraries 含 classes.jar 的类库目录。
	 * @throws IOException 读取失败或最高版本存在内容不同的副本。
	 */
	public Dependencies(List<File> libraries) throws IOException {
		// 固定类库顺序，再按依赖名称收集版本组和原始路径。
		List<File> orderedLibraries = new ArrayList<File>(libraries);
		Collections.sort(orderedLibraries, FILE_ORDER);

		Map<String, List<File>> groups = new LinkedHashMap<String, List<File>>();
		List<File> candidates = new ArrayList<File>();

		for (File library : orderedLibraries) {
			List<File> dependencies = listJars(new File(library, "libs"));
			libraryFiles.put(library.getAbsoluteFile(), dependencies);

			for (File file : dependencies) {
				candidates.add(file);
				Matcher matcher = VERSIONED_JAR.matcher(file.getName());

				if (matcher.matches()) {
					String name = matcher.group(1);
					List<File> group = groups.get(name);
					if (group == null) {
						group = new ArrayList<File>();
						groups.put(name, group);
					}
					group.add(file);
				} else {
					selectedFiles.put(file, file);
				}
			}
		}

		// 每组先确定最高数字版本，再核对同版本副本；不因已淘汰的旧版本产生冲突。
		Map<File, String> hashes = new HashMap<File, String>();
		for (List<File> group : groups.values()) {
			File highest = group.get(0);
			for (File file : group) {
				if (compareVersion(version(file), version(highest)) > 0) {
					highest = file;
				}
			}

			for (File file : group) {
				if (compareVersion(version(file), version(highest)) == 0
						&& !hash(file, hashes).equals(hash(highest, hashes))) {
					throw new IOException("扩展库依赖版本相同但内容不同：\n" + highest + "\n" + file);
				}

				selectedFiles.put(file, highest);
			}
		}

		// 相同内容的 JAR 只保留一份；原始路径仍映射到最终文件，供项目打包追溯。
		Map<String, File> byHash = new LinkedHashMap<String, File>();
		for (File candidate : candidates) {
			File selected = selectedFiles.get(candidate);
			String digest = hash(selected, hashes);
			File canonical = byHash.get(digest);

			if (canonical == null) {
				canonical = selected;
				byHash.put(digest, selected);
			}

			selectedFiles.put(candidate, canonical);
		}

		files = Collections.unmodifiableList(new ArrayList<File>(byHash.values()));
	}

	/**
	 * 返回加载器与后续打包共用的全部已选依赖。
	 *
	 * @return 按稳定顺序排列的只读依赖列表。
	 */
	public List<File> getFiles() {
		return files;
	}

	/**
	 * 只收集项目所需依赖组，但沿用加载器已经使用的版本和文件。
	 *
	 * @param libraries 项目实际引用的类库目录。
	 * @return 去重后的 JAR，不重新扫描 libs 或重新选版。
	 */
	public List<File> getFilesForLibraries(List<File> libraries) {
		// 复用全局选版结果，仅筛选项目需要的依赖，并保持首次引用顺序。
		Set<File> needed = new LinkedHashSet<File>();

		for (File library : libraries) {
			List<File> dependencies = libraryFiles.get(library.getAbsoluteFile());
			if (dependencies == null) {
				continue;
			}

			for (File dependency : dependencies) {
				needed.add(selectedFiles.get(dependency));
			}
		}

		return new ArrayList<File>(needed);
	}

	/**
	 * 从已经归入版本组的依赖文件名中提取数字版本。
	 *
	 * @param file 符合数字版本命名规则的依赖文件。
	 * @return 文件名末尾的数字版本字符串。
	 * @throws IllegalArgumentException 文件名不符合版本规则。
	 */
	private static String version(File file) {
		Matcher matcher = VERSIONED_JAR.matcher(file.getName());
		if (!matcher.matches()) {
			throw new IllegalArgumentException(file.toString());
		}

		return matcher.group(2);
	}

	/**
	 * 按数字段逐段比较版本，缺少的尾段按零处理。
	 *
	 * <p>使用任意精度整数，避免长版本段溢出，也不按字符串或浮点数比较。
	 *
	 * @param left 左侧数字版本。
	 * @param right 右侧数字版本。
	 * @return 负数表示左侧较旧，零表示等价，正数表示左侧较新。
	 */
	static int compareVersion(String left, String right) {
		String[] a = left.split("\\.");
		String[] b = right.split("\\.");

		for (int i = 0; i < Math.max(a.length, b.length); i++) {
			BigInteger x = i < a.length ? new BigInteger(a[i]) : BigInteger.ZERO;
			BigInteger y = i < b.length ? new BigInteger(b[i]) : BigInteger.ZERO;
			int result = x.compareTo(y);
			if (result != 0) {
				return result;
			}
		}

		return 0;
	}

	/**
	 * 按稳定顺序收集依赖目录第一层的 JAR，不递归扫描源码包等附加目录。
	 *
	 * @param directory 类库的依赖目录。
	 * @return JAR 绝对路径列表；目录不存在时返回空列表。
	 * @throws IOException 目录存在但无法枚举。
	 */
	private static List<File> listJars(File directory) throws IOException {
		if (!directory.exists()) {
			return Collections.emptyList();
		}

		File[] children = directory.listFiles();
		if (children == null) {
			throw new IOException("无法读取扩展库依赖目录：" + directory);
		}

		Arrays.sort(children, FILE_ORDER);
		List<File> result = new ArrayList<File>();
		for (File child : children) {
			if (child.isFile() && child.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".jar")) {
				result.add(child.getAbsoluteFile());
			}
		}

		return result;
	}

	/**
	 * 计算依赖内容的 SHA-256 标识，并复用本次选择过程中的已计算结果。
	 *
	 * @param file 要读取的依赖文件。
	 * @param hashes 本次选择使用的内容标识缓存。
	 * @return 内容摘要的字符串表示，仅用于内容相等性比较。
	 * @throws IOException 文件无法读取。
	 */
	private static String hash(File file, Map<File, String> hashes) throws IOException {
		String cached = hashes.get(file);
		if (cached != null) {
			return cached;
		}

		// 同一文件在版本冲突检查和最终去重之间只读取一次。
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			try (InputStream input = new FileInputStream(file)) {
				byte[] buffer = new byte[8192];
				for (int count; (count = input.read(buffer)) != -1;) {
					digest.update(buffer, 0, count);
				}
			}

			String result = Arrays.toString(digest.digest());
			hashes.put(file, result);
			return result;
		} catch (NoSuchAlgorithmException impossible) {
			throw new IllegalStateException(impossible);
		}
	}

	/**
	 * 在类加载或 DEX 处理之前定位剩余重复类，避免依赖目录顺序决定实际类型。
	 *
	 * @param inputs 已选 JAR 或类目录；不应包含用于编译的 Android 平台桩。
	 * @throws IOException 输入损坏或存在重复类。
	 */
	public static void validateClasses(List<File> inputs) throws IOException {
		Map<String, File> owners = new HashMap<String, File>();
		Set<File> seen = new LinkedHashSet<File>();

		// 同一路径只检查一次；仅登记基础普通类，模块描述不参与重名判断。
		for (File input : inputs) {
			File file = input.getAbsoluteFile();
			if (!seen.add(file)) {
				continue;
			}

			if (file.isDirectory()) {
				validateDirectory(file, file, owners);
			} else {
				try (JarFile jar = new JarFile(file)) {
					Enumeration<JarEntry> entries = jar.entries();
					while (entries.hasMoreElements()) {
						String name = entries.nextElement().getName();
						if (ClassFiles.isBaseClass(name)) {
							try (InputStream inputStream = jar.getInputStream(jar.getJarEntry(name))) {
								if (!ClassFiles.isModule(inputStream)) {
									rememberClass(name, file, owners);
								}
							} catch (IOException failure) {
								throw new IOException(name + "：" + failure.getMessage(), failure);
							}
						}
					}
				} catch (IOException failure) {
					throw new IOException("检查扩展库类文件失败：" + file + "\n" + failure.getMessage(), failure);
				}
			}
		}
	}

	/**
	 * 递归检查类目录，以相对根目录的路径统一登记类名。
	 *
	 * @param root 类路径根目录，也是冲突诊断中的来源目录。
	 * @param directory 当前递归访问的目录。
	 * @param owners 已发现类名与来源文件的映射。
	 * @throws IOException 目录无法读取或发现重复类。
	 */
	private static void validateDirectory(File root, File directory, Map<String, File> owners) throws IOException {
		File[] children = directory.listFiles();
		if (children == null) {
			throw new IOException("无法读取类目录：" + directory);
		}

		Arrays.sort(children, FILE_ORDER);
		for (File child : children) {
			if (child.isDirectory()) {
				validateDirectory(root, child, owners);
			} else if (child.getName().endsWith(".class")) {
				String name = root.toPath().relativize(child.toPath()).toString().replace(File.separatorChar, '/');
				if (ClassFiles.isBaseClass(name)) {
					try (InputStream input = new FileInputStream(child)) {
						if (!ClassFiles.isModule(input)) {
							rememberClass(name, root, owners);
						}
					} catch (IOException failure) {
						throw new IOException(child + "：" + failure.getMessage(), failure);
					}
				}
			}
		}
	}

	/**
	 * 登记类的来源，遇到同名类时报告双方路径。
	 *
	 * @param name 使用斜杠分隔、包含 class 后缀的相对路径。
	 * @param file 类所属的 JAR 或类路径根目录。
	 * @param owners 已发现类名与来源文件的映射。
	 * @throws IOException 已经登记过相同类名。
	 */
	private static void rememberClass(String name, File file, Map<String, File> owners) throws IOException {
		File previous = owners.put(name, file);
		if (previous != null) {
			throw new IOException("扩展库依赖冲突：" + name.substring(0, name.length() - 6).replace('/', '.')
					+ "\n来源：" + previous + "\n来源：" + file);
		}
	}
}

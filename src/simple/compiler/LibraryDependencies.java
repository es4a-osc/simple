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

/**
 * 为一次编译选择扩展库依赖；加载、DEX 和 JAR 资源打包共用同一选择结果。
 * 版本只取文件名末尾的数字段，不改写依赖，也不推断其二进制兼容性。
 */
public final class LibraryDependencies {
	private static final Pattern VERSIONED_JAR =
			Pattern.compile("^(.+)-([0-9]+(?:\\.[0-9]+)*)\\.jar$", Pattern.CASE_INSENSITIVE);
	private static final Comparator<File> FILE_ORDER = new Comparator<File>() {
		@Override
		public int compare(File left, File right) {
			int result = left.getAbsolutePath().compareToIgnoreCase(right.getAbsolutePath());
			return result != 0 ? result : left.getAbsolutePath().compareTo(right.getAbsolutePath());
		}
	};

	private final Map<File, List<File>> libraryFiles = new LinkedHashMap<File, List<File>>();
	private final Map<File, File> selectedFiles = new HashMap<File, File>();
	private final List<File> files;

	/**
	 * 按已安装类库建立稳定的依赖选择快照。
	 *
	 * @param libraries 含 classes.jar 的类库目录。
	 * @throws IOException 读取失败或最高版本存在内容不同的副本。
	 */
	public LibraryDependencies(List<File> libraries) throws IOException {
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

		Map<File, String> hashes = new HashMap<File, String>();
		for (List<File> group : groups.values()) {
			File highest = group.get(0);
			for (File file : group) {
				if (compareVersion(version(file), version(highest)) > 0) highest = file;
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

	/** @return 加载器使用的全部已选依赖。 */
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
		Set<File> needed = new LinkedHashSet<File>();
		for (File library : libraries) {
			List<File> dependencies = libraryFiles.get(library.getAbsoluteFile());
			if (dependencies == null) continue;
			for (File dependency : dependencies) needed.add(selectedFiles.get(dependency));
		}
		return new ArrayList<File>(needed);
	}

	private static String version(File file) {
		Matcher matcher = VERSIONED_JAR.matcher(file.getName());
		if (!matcher.matches()) throw new IllegalArgumentException(file.toString());
		return matcher.group(2);
	}

	/** 数字段逐段比较，缺少的尾段按零处理，不使用字符串大小或浮点数。 */
	static int compareVersion(String left, String right) {
		String[] a = left.split("\\.");
		String[] b = right.split("\\.");
		for (int i = 0; i < Math.max(a.length, b.length); i++) {
			BigInteger x = i < a.length ? new BigInteger(a[i]) : BigInteger.ZERO;
			BigInteger y = i < b.length ? new BigInteger(b[i]) : BigInteger.ZERO;
			int result = x.compareTo(y);
			if (result != 0) return result;
		}
		return 0;
	}

	private static List<File> listJars(File directory) throws IOException {
		if (!directory.exists()) return Collections.emptyList();
		File[] children = directory.listFiles();
		if (children == null) throw new IOException("无法读取扩展库依赖目录：" + directory);
		Arrays.sort(children, FILE_ORDER);
		List<File> result = new ArrayList<File>();
		for (File child : children) {
			if (child.isFile() && child.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".jar")) {
				result.add(child.getAbsoluteFile());
			}
		}
		return result;
	}

	private static String hash(File file, Map<File, String> hashes) throws IOException {
		String cached = hashes.get(file);
		if (cached != null) return cached;
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			try (InputStream input = new FileInputStream(file)) {
				byte[] buffer = new byte[8192];
				for (int count; (count = input.read(buffer)) != -1;) digest.update(buffer, 0, count);
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
		for (File input : inputs) {
			File file = input.getAbsoluteFile();
			if (!seen.add(file)) continue;
			if (file.isDirectory()) {
				validateDirectory(file, file, owners);
			} else {
				try (JarFile jar = new JarFile(file)) {
					Enumeration<JarEntry> entries = jar.entries();
					while (entries.hasMoreElements()) {
						String name = entries.nextElement().getName();
						if (name.endsWith(".class")) rememberClass(name, file, owners);
					}
				} catch (IOException failure) {
					throw new IOException("检查扩展库类文件失败：" + file + "\n" + failure.getMessage(), failure);
				}
			}
		}
	}

	private static void validateDirectory(File root, File directory, Map<String, File> owners) throws IOException {
		File[] children = directory.listFiles();
		if (children == null) throw new IOException("无法读取类目录：" + directory);
		Arrays.sort(children, FILE_ORDER);
		for (File child : children) {
			if (child.isDirectory()) {
				validateDirectory(root, child, owners);
			} else if (child.getName().endsWith(".class")) {
				String name = root.toPath().relativize(child.toPath()).toString().replace(File.separatorChar, '/');
				rememberClass(name, root, owners);
			}
		}
	}

	private static void rememberClass(String name, File file, Map<String, File> owners) throws IOException {
		File previous = owners.put(name, file);
		if (previous != null) {
			throw new IOException("扩展库依赖冲突：" + name.substring(0, name.length() - 6).replace('/', '.')
					+ "\n来源：" + previous + "\n来源：" + file);
		}
	}
}

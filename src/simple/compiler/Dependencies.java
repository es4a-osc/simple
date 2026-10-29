package simple.compiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.maven.artifact.versioning.ComparableVersion;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import simple.compiler.util.ClassFiles;

/**
 * 为一次编译选择扩展库依赖；加载、DEX 和 JAR 资源打包共用同一选择结果。
 *
 * <p>仓库模块由 SDK Gradle 解析；独立本地 JAR 共用身份及版本规则，不改写原包。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class Dependencies {

	/** 本轮依赖制品及其全局版本选择。 */
	private final LibraryArtifacts artifacts;

	/** 构造时的规范化根库顺序，不受调用方后续修改列表影响。 */
	private final List<File> libraries;

	/** 本轮根库顺序对应的只读代码依赖快照。 */
	private final List<File> files;

	/**
	 * 为不指定构建目录的调用者建立依赖选择快照。
	 *
	 * @param libraries 含 classes.jar 的已安装扩展库。
	 * @throws IOException 依赖描述、归档或版本选择无效。
	 */
	public Dependencies(List<File> libraries) throws IOException {
		this(libraries, null);
	}

	/**
	 * 统一读取 JAR 与 AAR，供加载器和后续构建复用同一选择结果。
	 *
	 * @param libraries 本项目实际使用的扩展库目录。
	 * @param workDirectory AAR 展开目录；为空时使用系统临时目录。
	 * @throws IOException 描述无效、输入缺失或依赖冲突。
	 */
	public Dependencies(List<File> libraries, File workDirectory) throws IOException {
		this.libraries = canonicalLibraries(libraries);
		artifacts = new LibraryArtifacts(this.libraries, workDirectory);
		files = Collections.unmodifiableList(artifacts.getJars(this.libraries));
	}

	/**
	 * 返回按本轮根库顺序装配、展开和去重的 JAR。
	 *
	 * @return 稳定排序的只读类路径。
	 */
	public List<File> getFiles() {
		return files;
	}

	/**
	 * 同一组有序根库复用本轮快照，其它查询按指定库及顺序筛选代码依赖。
	 *
	 * @param libraries 项目实际引用的扩展库；为 null 时使用本轮全部根库。
	 * @return 全量查询返回只读快照，其它查询返回所选依赖的 JAR。
	 * @throws IOException 路径无法规范化或子集依赖无法读取。
	 */
	public List<File> getFilesForLibraries(List<File> libraries) throws IOException {
		// 常规构建的资源与 DEX 请求使用同一批根库，无需重复读取归档和计算摘要。
		if (libraries == null || this.libraries.equals(canonicalLibraries(libraries))) {
			return files;
		}
		return artifacts.getJars(libraries);
	}

	/**
	 * 复制并规范化根库路径，保留输入顺序和重复项，不建立跨构建缓存。
	 *
	 * @param libraries 待核对的有序根库列表。
	 * @return 与调用方列表相互独立的只读路径列表。
	 * @throws IOException 路径无法规范化。
	 */
	private static List<File> canonicalLibraries(List<File> libraries) throws IOException {
		List<File> result = new ArrayList<File>();
		for (File library : libraries) {
			result.add(library.getCanonicalFile());
		}
		return Collections.unmodifiableList(result);
	}

	/**
	 * 返回项目所选的清单、资源、资产及原生库输入。
	 *
	 * @param libraries 项目实际引用的扩展库。
	 * @return 依赖在前、使用者在后的 Android 类库输入。
	 * @throws IOException 路径或清单无效。
	 */
	public List<LibraryArtifacts.AndroidLibrary> getAndroidLibraries(List<File> libraries) throws IOException {
		return artifacts.getAndroidLibraries(libraries);
	}

	/**
	 * 使用 Maven 成熟的版本比较实现，支持数字段和 rc、SNAPSHOT 等限定符。
	 *
	 * <p>所有独立本地 JAR 共用该规则，不在各个归档来源重复实现版本算法。
	 *
	 * @param left 左侧版本。
	 * @param right 右侧版本。
	 * @return 负数表示左侧较旧，零表示等价，正数表示左侧较新。
	 */
	static int compareVersion(String left, String right) {
		return new ComparableVersion(left).compareTo(new ComparableVersion(right));
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

		Arrays.sort(children);
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

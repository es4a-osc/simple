package simple.compiler;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import junit.framework.TestCase;
import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

/**
 * 验证运行库对象和语言特性示例的目录，各项目具有独立入口和 Android 包名。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class RuntimeExamplesTest extends TestCase {

	/**
	 * 与运行库对象示例并列维护的语言特性项目。
	 */
	private static final Set<String> LANGUAGE_EXAMPLES = Set.of("对象与接口", "命名空间与别名", "方法重载");

	/**
	 * 从运行库注解识别对象和组件，并纳入明确的语言特性项目，拒绝遗漏或残留目录。
	 *
	 * @throws Exception 运行库归档或导出类无法读取
	 */
	public void testDirectoriesMatchExportedObjects() throws Exception {
		Set<String> expected = new TreeSet<>(LANGUAGE_EXAMPLES);
		File runtime = new File(System.getenv("SIMPLE_HOME"), "SimpleAndroidRuntime.jar");
		try (JarFile archive = new JarFile(runtime)) {
			Enumeration<JarEntry> entries = archive.entries();
			while (entries.hasMoreElements()) {
				String entry = entries.nextElement().getName();
				if (!entry.startsWith("simple/runtime/") || !entry.endsWith(".class")
						|| entry.indexOf('$') >= 0 || entry.startsWith("simple/runtime/errors/")
						|| entry.startsWith("simple/runtime/components/impl/")) {
					continue;
				}

				String name = entry.substring(0, entry.length() - 6).replace('/', '.');
				Class<?> type = Class.forName(name, false, getClass().getClassLoader());
				if (!type.isAnnotationPresent(SimpleObject.class)) {
					continue;
				}
				boolean staticFunctions = false;
				for (Method method : type.getDeclaredMethods()) {
					if (Modifier.isStatic(method.getModifiers()) && method.isAnnotationPresent(SimpleFunction.class)) {
						staticFunctions = true;
					}
				}
				if (type.isAnnotationPresent(SimpleComponent.class)
						|| (!type.isInterface() && (!Modifier.isAbstract(type.getModifiers()) || staticFunctions))) {
					expected.add(type.getSimpleName());
				}
			}
		}

		Set<String> actual = new TreeSet<>();
		for (File project : projects(new File("examples"))) {
			actual.add(project.getParentFile().getName());
		}
		assertEquals(expected, actual);
	}

	/**
	 * 运行库项目名称与目录一致，所有示例入口存在，并使用独立的 Android 包名。
	 *
	 * @throws Exception 项目清单或源单元无法读取
	 */
	public void testIndependentProjectIdentity() throws Exception {
		Set<String> packages = new HashSet<>();
		for (File file : projects(new File("examples"))) {
			Project project = new Project(file);
			String name = file.getParentFile().getName();
			if (!LANGUAGE_EXAMPLES.contains(name)) {
				assertEquals(name, project.getProjectName());
			} else {
				assertNotNull(name, project.getProjectName());
				assertFalse(name, project.getProjectName().trim().isEmpty());
			}

			String main = project.getMainForm();
			assertNotNull(name, main);
			assertTrue(name, packages.add(main.substring(0, main.lastIndexOf('.'))));
			boolean found = false;
			for (Project.SourceDescriptor source : project.getSources()) {
				if (main.equals(source.getQualifiedName())) {
					found = true;
				}
			}
			assertTrue(name + "缺少主单元", found);
		}
	}

	/**
	 * 按目录名稳定列出指定分类中的真实项目，拒绝缺失或空的分类。
	 *
	 * @param directory 示例分类根目录
	 * @return 项目清单文件列表
	 */
	private List<File> projects(File directory) {
		File[] directories = directory.listFiles(File::isDirectory);
		assertNotNull(directory.toString(), directories);
		List<File> result = new ArrayList<>();
		for (File child : directories) {
			File file = new File(child, "project.properties");
			if (file.isFile()) {
				result.add(file);
			}
		}
		result.sort(Comparator.comparing(file -> file.getParentFile().getName()));
		assertFalse(directory.toString(), result.isEmpty());
		return result;
	}
}

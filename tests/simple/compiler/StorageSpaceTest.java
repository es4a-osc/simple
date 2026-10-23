package simple.compiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;

import junit.framework.TestCase;

import simple.compiler.parser.Parser;
import simple.compiler.scanner.Scanner;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.UsesPermissions;
import simple.runtime.文件操作;

/**
 * 验证存储空间查询重载、文件读写示例编译及无需新增权限的边界。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class StorageSpaceTest extends TestCase {

	/**
	 * 两个查询各提供默认与路径重载，均导出长整数返回值。
	 *
	 * @throws Exception 导出接口缺失。
	 */
	public void testExportedContract() throws Exception {
		assertTrue(文件操作.class.isAnnotationPresent(SimpleObject.class));
		assertFalse(文件操作.class.isAnnotationPresent(UsesPermissions.class));
		for (String name : new String[] { "取存储总空间", "取存储可用空间" }) {
			for (Class<?>[] parameters : new Class<?>[][] { {}, { String.class } }) {
				Method method = 文件操作.class.getMethod(name, parameters);
				assertTrue(method.isAnnotationPresent(SimpleFunction.class));
				assertTrue(java.lang.reflect.Modifier.isStatic(method.getModifiers()));
				assertEquals(long.class, method.getReturnType());
				assertFalse(method.isAnnotationPresent(UsesPermissions.class));
			}
		}
	}

	/**
	 * 编译文件读写完整示例，覆盖默认与路径版空间查询。
	 *
	 * @throws Exception 示例文件读取失败。
	 */
	public void testRealExampleCompiles() throws Exception {
		assertNotNull(Compiler.compileForUnitTesting(new Project(new File("examples/文件操作/project.properties"))));
	}

	/**
	 * 私有目录的存储空间查询不应为示例增加任何清单权限。
	 *
	 * @throws Exception 示例解析或权限集合读取失败。
	 */
	public void testLocalDataNeedsNoPermissions() throws Exception {
		ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
		PrintStream messages = new PrintStream(diagnostics, true, "UTF-8");
		Compiler compiler = new Compiler(Compiler.Platform.Android, messages, messages);
		Project project = new Project("examples/文件操作/project.properties", null);
		for (Project.SourceDescriptor descriptor : project.getSources()) {
			String source = new String(Files.readAllBytes(descriptor.getFile().toPath()), StandardCharsets.UTF_8);
			new Parser(compiler, new Scanner(compiler, source), descriptor.getQualifiedName()).parse();
		}
		if (compiler.getErrorCount() == 0) {
			compiler.resolve();
		}
		assertEquals(diagnostics.toString("UTF-8"), 0, compiler.getErrorCount());
		Method permissions = Compiler.class.getDeclaredMethod("generatePermissions");
		permissions.setAccessible(true);
		assertTrue(((Set<?>) permissions.invoke(compiler)).isEmpty());
	}
}

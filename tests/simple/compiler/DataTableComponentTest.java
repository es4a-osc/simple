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
import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.UsesPermissions;
import simple.runtime.components.可视组件;
import simple.runtime.components.数据表格;

/**
 * 验证数据表格的组件导出、真实示例编译及按需权限收集。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class DataTableComponentTest extends TestCase {

	/**
	 * 表格作为可视组件导出，包含行添加重载、属性读写和点击事件。
	 *
	 * @throws Exception 导出成员缺失。
	 */
	public void testExportedContract() throws Exception {
		assertTrue(数据表格.class.isAnnotationPresent(SimpleComponent.class));
		assertTrue(可视组件.class.isAssignableFrom(数据表格.class));
		assertFalse(数据表格.class.isAnnotationPresent(UsesPermissions.class));
		assertTrue(数据表格.class.getMethod("添加一行", String[].class)
			.isAnnotationPresent(SimpleFunction.class));
		assertTrue(数据表格.class.getMethod("添加一行", String[].class, int.class)
			.isAnnotationPresent(SimpleFunction.class));
		assertTrue(数据表格.class.getMethod("显示图片").isAnnotationPresent(SimpleProperty.class));
		assertTrue(数据表格.class.getMethod("显示图片", boolean.class).isAnnotationPresent(SimpleProperty.class));
		assertTrue(数据表格.class.getMethod("表格行被单击", int.class).isAnnotationPresent(SimpleEvent.class));
	}

	/**
	 * 编译完整示例，覆盖数据库查询、数组、事件、重载和 XML 属性生成结果。
	 *
	 * @throws Exception 示例文件读取失败。
	 */
	public void testRealExampleCompiles() throws Exception {
		assertNotNull(Compiler.compileForUnitTesting(new Project(new File("examples/数据表格/project.properties"))));
	}

	/**
	 * 示例只读取应用内部数据库及打包图片，不应请求存储或网络权限。
	 *
	 * @throws Exception 示例解析或权限集合读取失败。
	 */
	public void testLocalDataNeedsNoPermissions() throws Exception {
		ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
		PrintStream messages = new PrintStream(diagnostics, true, "UTF-8");
		Compiler compiler = new Compiler(Compiler.Platform.Android, messages, messages);
		Project project = new Project("examples/数据表格/project.properties", null);
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

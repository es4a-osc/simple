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
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.UsesPermissions;
import simple.runtime.components.可视组件;
import simple.runtime.components.弹出菜单;
import simple.runtime.components.组件;

/**
 * 验证弹出菜单的组件导出、原有接口、完整示例及无额外权限约定。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class PopupMenuComponentTest extends TestCase {

	/**
	 * 原有十个函数、两个事件和菜单资源属性按当前注解导出。
	 *
	 * @throws Exception 导出方法缺失。
	 */
	public void testExportedContract() throws Exception {
		assertTrue(弹出菜单.class.isAnnotationPresent(SimpleComponent.class));
		assertTrue(弹出菜单.class.isAnnotationPresent(SimpleObject.class));
		assertTrue(组件.class.isAssignableFrom(弹出菜单.class));
		assertFalse(弹出菜单.class.isAnnotationPresent(UsesPermissions.class));
		assertTrue(弹出菜单.class.getMethod("被选中", int.class).isAnnotationPresent(SimpleEvent.class));
		assertTrue(弹出菜单.class.getMethod("被关闭").isAnnotationPresent(SimpleEvent.class));
		assertTrue(弹出菜单.class.getMethod("菜单项").isAnnotationPresent(SimpleProperty.class));
		assertEquals("\"\"", 弹出菜单.class.getMethod("菜单项", String.class)
			.getAnnotation(SimpleProperty.class).initializer());

		String[] names = { "添加", "删除", "清空", "取分组", "置分组", "取排序", "置排序", "取父项", "置父项", "弹出" };
		Class<?>[][] parameters = {
			{ String.class }, { int.class }, {}, { int.class }, { int.class, int.class },
			{ int.class }, { int.class, int.class }, { int.class }, { int.class, int.class }, { 可视组件.class }
		};
		for (int index = 0; index < names.length; index++) {
			assertTrue(弹出菜单.class.getMethod(names[index], parameters[index]).isAnnotationPresent(SimpleFunction.class));
		}
		assertEquals(14, 弹出菜单.class.getDeclaredMethods().length);
	}

	/**
	 * 编译独立示例，覆盖全部方法、属性、事件和真实资源引用。
	 *
	 * @throws Exception 示例源码读取失败。
	 */
	public void testRealExampleCompiles() throws Exception {
		assertNotNull(Compiler.compileForUnitTesting(new Project(new File("examples/弹出菜单/project.properties"))));
	}

	/**
	 * 编译器从真实示例收集权限时不因弹出菜单增加任何权限。
	 *
	 * @throws Exception 源码解析或权限集合读取失败。
	 */
	public void testNoAdditionalPermissions() throws Exception {
		ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
		PrintStream messages = new PrintStream(diagnostics, true, "UTF-8");
		Compiler compiler = new Compiler(Compiler.Platform.Android, messages, messages);
		Project project = new Project("examples/弹出菜单/project.properties", null);
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

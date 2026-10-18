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
import simple.runtime.components.编辑框;
import simple.runtime.components.富文本框;

/**
 * 验证富文本框继承、区间接口与图片重载、真实示例编译及权限边界。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class RichTextComponentTest extends TestCase {

	/**
	 * 继承编辑框并导出格式方法、只读选区和受控链接事件。
	 *
	 * @throws Exception 导出签名缺失。
	 */
	public void testExportedContract() throws Exception {
		assertTrue(富文本框.class.isAnnotationPresent(SimpleComponent.class));
		assertTrue(编辑框.class.isAssignableFrom(富文本框.class));
		assertFalse(富文本框.class.isAnnotationPresent(UsesPermissions.class));
		assertTrue(富文本框.class.getMethod("置风格文本图片", String.class, int.class, int.class)
			.isAnnotationPresent(SimpleFunction.class));
		assertTrue(富文本框.class.getMethod("置风格文本图片", String.class,
			int.class, int.class, int.class, int.class).isAnnotationPresent(SimpleFunction.class));
		assertTrue(富文本框.class.getMethod("选区开始").isAnnotationPresent(SimpleProperty.class));
		assertTrue(富文本框.class.getMethod("选区结束").isAnnotationPresent(SimpleProperty.class));
		assertTrue(富文本框.class.getMethod("超链接被单击", String.class).isAnnotationPresent(SimpleEvent.class));
		assertTrue(富文本框.class.getMethod("删除文本", int.class, int.class)
			.isAnnotationPresent(SimpleFunction.class));
	}

	/**
	 * 编译完整示例，覆盖富文本格式、继承编辑方法、事件和图片重载。
	 *
	 * @throws Exception 示例文件读取失败。
	 */
	public void testRealExampleCompiles() throws Exception {
		assertNotNull(Compiler.compileForUnitTesting(new Project(new File("examples/富文本框/project.properties"))));
	}

	/**
	 * 示例只编辑文本及读取打包图片，不应请求存储或网络权限。
	 *
	 * @throws Exception 示例解析或权限集合读取失败。
	 */
	public void testLocalDataNeedsNoPermissions() throws Exception {
		ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
		PrintStream messages = new PrintStream(diagnostics, true, "UTF-8");
		Compiler compiler = new Compiler(Compiler.Platform.Android, messages, messages);
		Project project = new Project("examples/富文本框/project.properties", null);
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

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
import simple.runtime.剪贴板操作;

/**
 * 验证剪贴板方法导出、真实示例编译和无需额外权限的边界。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class ClipboardOperationsTest extends TestCase {

	/**
	 * 三个静态函数采用 Simple 文本与逻辑类型，且不导出为设计器组件。
	 *
	 * @throws Exception 导出签名缺失。
	 */
	public void testExportedContract() throws Exception {
		assertTrue(剪贴板操作.class.isAnnotationPresent(SimpleObject.class));
		Method write = 剪贴板操作.class.getMethod("置剪贴板文本", String.class);
		Method read = 剪贴板操作.class.getMethod("取剪贴板文本");
		Method hasText = 剪贴板操作.class.getMethod("剪贴板是否有文本");

		assertTrue(write.isAnnotationPresent(SimpleFunction.class));
		assertTrue(read.isAnnotationPresent(SimpleFunction.class));
		assertTrue(hasText.isAnnotationPresent(SimpleFunction.class));
		assertTrue(java.lang.reflect.Modifier.isStatic(write.getModifiers()));
		assertTrue(java.lang.reflect.Modifier.isStatic(read.getModifiers()));
		assertTrue(java.lang.reflect.Modifier.isStatic(hasText.getModifiers()));
		assertEquals(void.class, write.getReturnType());
		assertEquals(String.class, read.getReturnType());
		assertEquals(boolean.class, hasText.getReturnType());
		assertFalse(剪贴板操作.class.isAnnotationPresent(UsesPermissions.class));
	}

	/**
	 * 编译完整示例，覆盖三个全局函数、按钮事件和文本往返断言。
	 *
	 * @throws Exception 示例文件读取失败。
	 */
	public void testRealExampleCompiles() throws Exception {
		assertNotNull(Compiler.compileForUnitTesting(new Project(new File("examples/剪贴板操作/project.properties"))));
	}

	/**
	 * 剪贴板纯文本访问不应为示例增加任何清单权限。
	 *
	 * @throws Exception 示例解析或权限集合读取失败。
	 */
	public void testLocalDataNeedsNoPermissions() throws Exception {
		ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
		PrintStream messages = new PrintStream(diagnostics, true, "UTF-8");
		Compiler compiler = new Compiler(Compiler.Platform.Android, messages, messages);
		Project project = new Project("examples/剪贴板操作/project.properties", null);
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

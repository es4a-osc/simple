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
import simple.runtime.annotations.UsesPermissions;
import simple.runtime.components.音效播放器;

/**
 * 验证音效组件的导出签名、真实示例编译及按需权限收集。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class SoundEffectsComponentTest extends TestCase {

	/**
	 * 两种播放参数数量均可导出，成功与失败事件独立且不引入额外权限。
	 *
	 * @throws Exception 导出方法签名缺失。
	 */
	public void testExportedContract() throws Exception {
		assertTrue(音效播放器.class.isAnnotationPresent(SimpleComponent.class));
		assertFalse(音效播放器.class.isAnnotationPresent(UsesPermissions.class));
		assertTrue(音效播放器.class.getMethod("播放音效", int.class)
			.isAnnotationPresent(SimpleFunction.class));
		assertTrue(音效播放器.class.getMethod("播放音效", int.class, int.class)
			.isAnnotationPresent(SimpleFunction.class));
		assertTrue(音效播放器.class.getMethod("载入完毕", int.class)
			.isAnnotationPresent(SimpleEvent.class));
		assertTrue(音效播放器.class.getMethod("载入失败", int.class, int.class)
			.isAnnotationPresent(SimpleEvent.class));
	}

	/**
	 * 直接编译实际示例，覆盖重载调用、事件参数、控制流程及属性区。
	 *
	 * @throws Exception 项目读取失败。
	 */
	public void testRealExampleCompiles() throws Exception {
		assertNotNull(Compiler.compileForUnitTesting(
			new Project(new File("examples/音效播放/project.properties"))));
	}

	/**
	 * 播放 APK 自带音效的实际项目不应申请录音、存储或修改系统音量权限。
	 *
	 * @throws Exception 项目解析或权限集合读取失败。
	 */
	public void testAssetPlaybackRequiresNoPermission() throws Exception {
		ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
		PrintStream messages = new PrintStream(diagnostics, true, "UTF-8");
		Compiler compiler = new Compiler(Compiler.Platform.Android, messages, messages);

		for (Project.SourceDescriptor descriptor : new Project("examples/音效播放/project.properties", null).getSources()) {
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

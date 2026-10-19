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
import simple.runtime.annotations.ManifestNodes;
import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.UsesPermissions;
import simple.runtime.components.可视组件;
import simple.runtime.components.画板;

/**
 * 验证画板新增绘图接口、重载、真实示例编译及私有目录保存无需权限。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class CanvasComponentTest extends TestCase {

	/**
	 * 新增接口和重载均导出给 Simple，画笔常量与缺省值保持一致。
	 *
	 * @throws Exception 导出接口缺失。
	 */
	public void testExportedContract() throws Exception {
		assertTrue(画板.class.isAnnotationPresent(SimpleComponent.class));
		assertTrue(可视组件.class.isAssignableFrom(画板.class));
		assertFalse(画板.class.isAnnotationPresent(UsesPermissions.class));
		assertFalse(画板.class.isAnnotationPresent(ManifestNodes.class));
		assertTrue(画板.class.getMethod("画矩形", int.class, int.class, int.class, int.class).isAnnotationPresent(SimpleFunction.class));
		assertTrue(画板.class.getMethod("画圆角矩形", int.class, int.class, int.class, int.class, float.class).isAnnotationPresent(SimpleFunction.class));
		assertTrue(画板.class.getMethod("画弧形", int.class, int.class, int.class, int.class, float.class, float.class, boolean.class).isAnnotationPresent(SimpleFunction.class));
		assertTrue(画板.class.getMethod("画贝塞尔曲线", int.class, int.class, int.class, int.class, int.class, int.class).isAnnotationPresent(SimpleFunction.class));
		assertTrue(画板.class.getMethod("画文字", int.class, int.class, String.class).isAnnotationPresent(SimpleFunction.class));
		assertTrue(画板.class.getMethod("画图片", String.class, int.class, int.class).isAnnotationPresent(SimpleFunction.class));
		assertTrue(画板.class.getMethod("画图片", String.class, int.class, int.class, int.class, int.class).isAnnotationPresent(SimpleFunction.class));
		assertTrue(画板.class.getMethod("取点颜色", int.class, int.class).isAnnotationPresent(SimpleFunction.class));
		assertTrue(画板.class.getMethod("保存画面", String.class).isAnnotationPresent(SimpleFunction.class));
		assertTrue(画板.class.getMethod("保存画面", String.class, int.class).isAnnotationPresent(SimpleFunction.class));
		assertEquals(int.class, 画板.class.getMethod("取点颜色", int.class, int.class).getReturnType());
		assertEquals(0, 画板.画笔_描边);
		assertEquals(1, 画板.画笔_填充);
		assertEquals("1", 画板.class.getMethod("画笔类型", int.class).getAnnotation(SimpleProperty.class).initializer());
		assertTrue(画板.class.getMethod("字体大小", float.class).isAnnotationPresent(SimpleProperty.class));
	}

	/**
	 * 编译完整示例，覆盖图形、文字、图片、重载、取点、保存和错误分支。
	 *
	 * @throws Exception 示例文件读取失败。
	 */
	public void testRealExampleCompiles() throws Exception {
		assertNotNull(Compiler.compileForUnitTesting(new Project(new File("examples/画板绘图/project.properties"))));
	}

	/**
	 * 示例仅加载打包图片并写入私有目录，不应声明存储、网络或壁纸权限。
	 *
	 * @throws Exception 示例解析或权限集合读取失败。
	 */
	public void testLocalDataNeedsNoPermissions() throws Exception {
		ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
		PrintStream messages = new PrintStream(diagnostics, true, "UTF-8");
		Compiler compiler = new Compiler(Compiler.Platform.Android, messages, messages);
		Project project = new Project("examples/画板绘图/project.properties", null);
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

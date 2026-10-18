package simple.compiler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Set;

import junit.framework.TestCase;

import simple.compiler.parser.Parser;
import simple.compiler.scanner.Scanner;
import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.UsesPermissions;
import simple.runtime.components.文件选择器;

/**
 * 验证文档选择组件的导出、MIME 校验、流复制及真实项目的权限收集。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class FilePickerComponentTest extends TestCase {

	/**
	 * 系统选择不要求广泛存储权限，多选事件保留数组参数。
	 *
	 * @throws Exception 导出签名缺失。
	 */
	public void testExportedContract() throws Exception {
		assertTrue(文件选择器.class.isAnnotationPresent(SimpleComponent.class));
		assertFalse(文件选择器.class.isAnnotationPresent(UsesPermissions.class));
		assertTrue(文件选择器.class.getMethod("文件选择完毕", String[].class)
			.isAnnotationPresent(SimpleEvent.class));
		assertTrue(文件选择器.class.getMethod("选择文件", String.class, boolean.class, String.class)
			.isAnnotationPresent(SimpleFunction.class));
		assertEquals(String.class, 文件选择器.class.getMethod("复制到文件", String.class, String.class).getReturnType());
	}

	/**
	 * 接受标准 MIME、合法通配与大小写归一化，拒绝缺少类型或非法通配。
	 *
	 * @throws Exception 反射调用失败。
	 */
	public void testMimeValidation() throws Exception {
		Method validate = implementation().getDeclaredMethod("validateMimeType", String.class);
		validate.setAccessible(true);
		for (String type : new String[]{"text/plain", "image/*", "*/*", "application/vnd.ms-excel", "application/ld+json"}) {
			assertEquals(type, validate.invoke(null, type));
		}
		assertEquals("text/plain", validate.invoke(null, " TEXT/PLAIN "));
		for (String type : new String[]{null, "", "text", "*/xml", "text/plain/extra", "text /plain"}) {
			try {
				validate.invoke(null, type);
				fail("应拒绝无效 MIME：" + type);
			} catch (InvocationTargetException expected) {
				assertTrue(expected.getCause() instanceof IllegalArgumentException);
			}
		}
	}

	/**
	 * 多缓冲区与空输入均复制完整，输出流失败不会被静默当成成功。
	 *
	 * @throws Exception 反射或复制失败。
	 */
	public void testStreamingCopy() throws Exception {
		Method copy = implementation().getDeclaredMethod("copy", java.io.InputStream.class, OutputStream.class);
		copy.setAccessible(true);
		byte[] data = new byte[25001];
		for (int index = 0; index < data.length; index++) {
			data[index] = (byte) index;
		}
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		copy.invoke(null, new ByteArrayInputStream(data), output);
		assertTrue(Arrays.equals(data, output.toByteArray()));
		output.reset();
		copy.invoke(null, new ByteArrayInputStream(new byte[0]), output);
		assertEquals(0, output.size());

		OutputStream broken = new OutputStream() {
			@Override
			public void write(int value) throws IOException {
				throw new IOException("expected write failure");
			}
		};
		try {
			copy.invoke(null, new ByteArrayInputStream(data), broken);
			fail("写入失败应继续抛出");
		} catch (InvocationTargetException expected) {
			assertTrue(expected.getCause() instanceof IOException);
		}
	}

	/**
	 * 编译实际示例，覆盖多个选择器、数组事件、重载、XML 与私有路径衔接。
	 *
	 * @throws Exception 项目读取失败。
	 */
	public void testRealExampleCompiles() throws Exception {
		assertNotNull(Compiler.compileForUnitTesting(new Project(new File("examples/文件选择/project.properties"))));
	}

	/**
	 * 按实际项目解析结果核对权限，不将存储、管理全部文件或网络权限带入应用。
	 *
	 * @throws Exception 解析或读取权限集合失败。
	 */
	public void testExampleRequiresNoPermission() throws Exception {
		ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
		PrintStream messages = new PrintStream(diagnostics, true, "UTF-8");
		Compiler compiler = new Compiler(Compiler.Platform.Android, messages, messages);
		for (Project.SourceDescriptor descriptor : new Project("examples/文件选择/project.properties", null).getSources()) {
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

	/**
	 * 延迟加载实现类，纯 Java 规则测试不构造 Android 组件。
	 *
	 * @return 文档选择的 Android 实现类。
	 * @throws ClassNotFoundException 类未被构建。
	 */
	private Class<?> implementation() throws ClassNotFoundException {
		return Class.forName("simple.runtime.components.impl.android.文件选择器Impl");
	}
}

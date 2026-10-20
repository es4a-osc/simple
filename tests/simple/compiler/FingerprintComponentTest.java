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
import simple.runtime.components.指纹验证;
import simple.runtime.components.组件;

/**
 * 验证指纹组件的导出契约、真实示例编译和权限按引用收集。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class FingerprintComponentTest extends TestCase {

	/**
	 * 检查组件形态、事件签名及只读属性。
	 *
	 * @throws Exception 公开成员缺失。
	 */
	public void testExportedContract() throws Exception {
		assertTrue(指纹验证.class.isAnnotationPresent(SimpleComponent.class));
		assertTrue(组件.class.isAssignableFrom(指纹验证.class));
		for (String name : new String[]{"取可用状态", "开始验证", "取消验证", "打开设置"}) {
			assertTrue(指纹验证.class.getMethod(name).isAnnotationPresent(SimpleFunction.class));
		}
		for (String name : new String[]{"正在验证", "错误信息"}) {
			assertTrue(指纹验证.class.getMethod(name).isAnnotationPresent(SimpleProperty.class));
		}
		for (String name : new String[]{"验证成功", "验证未通过", "验证取消"}) {
			assertTrue(指纹验证.class.getMethod(name).isAnnotationPresent(SimpleEvent.class));
		}
		for (String name : new String[]{"验证提示", "验证错误"}) {
			assertTrue(指纹验证.class.getMethod(name, int.class, String.class).isAnnotationPresent(SimpleEvent.class));
		}
		assertEquals(0, 指纹验证.状态_可用);
		assertEquals(7, 指纹验证.错误_临时锁定);
		assertEquals(9, 指纹验证.错误_永久锁定);
	}

	/**
	 * 编译完整示例，覆盖查询、开始、取消、设置、销毁和全部结果事件。
	 *
	 * @throws Exception 示例文件读取失败。
	 */
	public void testRealExampleCompiles() throws Exception {
		assertNotNull(Compiler.compileForUnitTesting(new Project(new File("examples/指纹验证/project.properties"))));
	}

	/**
	 * 只在引用组件的项目中加入普通指纹权限，不要求相机或互联网权限。
	 *
	 * @throws Exception 示例解析或权限读取失败。
	 */
	public void testPermissionCollectedOnlyWhenUsed() throws Exception {
		assertEquals("android.permission.USE_FINGERPRINT",
			指纹验证.class.getAnnotation(UsesPermissions.class).permissionNames());
		Set<?> used = permissions("examples/指纹验证/project.properties");
		assertEquals(1, used.size());
		assertTrue(used.contains("android.permission.USE_FINGERPRINT"));
		assertFalse(permissions("examples/电池监视/project.properties").contains("android.permission.USE_FINGERPRINT"));
	}

	/**
	 * 外层组件不在字段或方法签名中暴露需要 API 23 的系统类型。
	 *
	 * @throws Exception 类文件读取失败。
	 */
	public void testApi23TypesAreIsolated() throws Exception {
		String name = "/simple/runtime/components/impl/android/指纹验证Impl.class";
		java.io.InputStream input = 指纹验证.class.getResourceAsStream(name);
		assertNotNull(input);
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			int size;
			while ((size = input.read(buffer)) != -1) {
				bytes.write(buffer, 0, size);
			}
			String constants = new String(bytes.toByteArray(), StandardCharsets.ISO_8859_1);
			assertFalse(constants.contains("android/hardware/fingerprint"));
			assertFalse(constants.contains("android/os/CancellationSignal"));
		} finally {
			input.close();
		}
	}

	/**
	 * 按真实项目源码解析和解析引用，读取编译器最终收集的权限集合。
	 *
	 * @param path 项目属性文件。
	 * @return 收集到的权限。
	 * @throws Exception 项目解析或权限生成失败。
	 */
	private Set<?> permissions(String path) throws Exception {
		ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
		PrintStream messages = new PrintStream(diagnostics, true, "UTF-8");
		Compiler compiler = new Compiler(Compiler.Platform.Android, messages, messages);
		Project project = new Project(path, null);
		for (Project.SourceDescriptor descriptor : project.getSources()) {
			String source = new String(Files.readAllBytes(descriptor.getFile().toPath()), StandardCharsets.UTF_8);
			new Parser(compiler, new Scanner(compiler, source), descriptor.getQualifiedName()).parse();
		}
		if (compiler.getErrorCount() == 0) {
			compiler.resolve();
		}
		assertEquals(diagnostics.toString("UTF-8"), 0, compiler.getErrorCount());
		Method method = Compiler.class.getDeclaredMethod("generatePermissions");
		method.setAccessible(true);
		return (Set<?>) method.invoke(compiler);
	}
}

package simple.compiler;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Set;

import junit.framework.TestCase;

import simple.compiler.parser.Parser;
import simple.compiler.scanner.Scanner;
import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.UsesPermissions;
import simple.runtime.components.音量管理器;

/**
 * 验证音量管理器的组件契约及实际项目的按需权限收集。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class VolumePermissionsTest extends TestCase {

	/**
	 * 实际使用音量管理器的示例只声明普通音频权限，不附带录音或勿扰访问权限。
	 *
	 * @throws Exception 读取项目或反射权限集合失败。
	 */
	public void testVolumeExampleDeclaresOnlyAudioSettingsPermission() throws Exception {
		assertEquals(Collections.singleton("android.permission.MODIFY_AUDIO_SETTINGS"),
			permissions("examples/音量管理器/project.properties"));
	}

	/**
	 * 未引用音量管理器的真实示例不会被加入普通音频权限。
	 *
	 * @throws Exception 读取项目或反射权限集合失败。
	 */
	public void testOtherExampleDoesNotRequestAudioSettingsPermission() throws Exception {
		assertFalse(permissions("examples/标签/project.properties")
			.contains("android.permission.MODIFY_AUDIO_SETTINGS"));
	}

	/**
	 * 四个导出方法属于非可视组件实例，权限不依赖调用者手写清单。
	 */
	public void testComponentContract() {
		assertTrue(音量管理器.class.isInterface());
		assertTrue(音量管理器.class.isAnnotationPresent(SimpleComponent.class));
		assertEquals("android.permission.MODIFY_AUDIO_SETTINGS",
			音量管理器.class.getAnnotation(UsesPermissions.class).permissionNames());

		int count = 0;

		for (Method method : 音量管理器.class.getDeclaredMethods()) {
			if (method.isAnnotationPresent(SimpleFunction.class)) {
				assertFalse(Modifier.isStatic(method.getModifiers()));
				count++;
			}
		}

		assertEquals(4, count);
	}


	/**
	 * 系统权限拒绝会转换为既有运行错误，使 Simple 的错误分支能够处理。
	 *
	 * @throws Exception 反射实现类或错误转换失败。
	 */
	public void testSystemDenialRemainsCatchableBySimple() throws Exception {
		Class<?> implementation = Class.forName("simple.runtime.components.impl.android.音量管理器Impl");
		Method translate = implementation.getDeclaredMethod("operationError", String.class, RuntimeException.class);
		translate.setAccessible(true);
		SecurityException denied = new SecurityException("notification policy access denied");
		simple.runtime.errors.运行错误 error = (simple.runtime.errors.运行错误)
			translate.invoke(null, "设置音量失败", denied);

		assertSame(denied, error.getCause());
		assertSame(error, simple.runtime.errors.运行错误.convertToRuntimeError(error));
	}

	/**
	 * 解析实际项目全部单元，再读取编译器为此项目收集的权限集合。
	 *
	 * @param path 项目清单路径，相对于 Simple 仓库目录。
	 * @return 编译器收集的 Android 权限。
	 * @throws Exception 项目读取、编译器初始化或反射失败。
	 */
	private Set<?> permissions(String path) throws Exception {
		ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
		PrintStream messages = new PrintStream(diagnostics, true, "UTF-8");
		Compiler compiler = new Compiler(Compiler.Platform.Android, messages, messages);

		for (Project.SourceDescriptor descriptor : new Project(path, null).getSources()) {
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

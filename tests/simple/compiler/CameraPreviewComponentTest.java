package simple.compiler;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

import junit.framework.TestCase;

import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.components.摄像框;
import simple.runtime.components.impl.android.摄像框Impl;

/**
 * 验证摄像框的方向、尺寸、文件保护约定及真实 Simple 示例。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class CameraPreviewComponentTest extends TestCase {

	/**
	 * 前后置在四种显示方向下都应得到明确、独立的像素旋转角。
	 *
	 * @throws Exception 计算方法不可访问
	 */
	public void testFrontAndBackRotation() throws Exception {
		Method rotation = helper("imageRotation", int.class, int.class, boolean.class);
		int[][] back = {{0, 270, 180, 90}, {90, 0, 270, 180}, {180, 90, 0, 270}, {270, 180, 90, 0}};
		int[][] front = {{0, 90, 180, 270}, {90, 180, 270, 0}, {180, 270, 0, 90}, {270, 0, 90, 180}};
		for (int sensor = 0; sensor < 4; sensor++) {
			for (int display = 0; display < 4; display++) {
				assertEquals(back[sensor][display], rotation.invoke(null, sensor * 90, display, false));
				assertEquals(front[sensor][display], rotation.invoke(null, sensor * 90, display, true));
			}
		}
	}

	/**
	 * 尺寸比较必须同时考虑两条边、比例及大数，不因固定竖屏假设而失真。
	 *
	 * @throws Exception 计算方法不可访问
	 */
	public void testSizeMatchingAndOverflow() throws Exception {
		Method score = helper("sizeScore", int.class, int.class, int.class, int.class);
		assertEquals(0d, (Double) score.invoke(null, 640, 480, 480, 640), 0d);
		assertEquals(0d, (Double) score.invoke(null, 480, 640, 480, 640), 0d);
		double proportional = (Double) score.invoke(null, 1280, 960, 480, 640);
		double distorted = (Double) score.invoke(null, 1280, 720, 480, 640);
		assertTrue(proportional < distorted);
		assertTrue(Double.isFinite((Double) score.invoke(null, Integer.MAX_VALUE, Integer.MAX_VALUE, 480, 640)));
		assertTrue(Double.isInfinite((Double) score.invoke(null, 0, 480, 480, 640)));
	}

	/**
	 * 目标冲突、空文件和相对路径必须失败，不能修改原有文件。
	 *
	 * @throws Exception 文件操作或反射失败
	 */
	public void testFileDeliveryProtectsExistingContent() throws Exception {
		File directory = Files.createTempDirectory("simple-preview-test-").toFile();
		File target = new File(directory, "photo.jpg");
		File staged = new File(directory, ".photo.tmp");
		Method output = helper("outputFile", String.class);
		Method publish = helper("publishFile", File.class, File.class);
		try {
			Files.write(target.toPath(), "original".getBytes(StandardCharsets.UTF_8));
			Files.write(staged.toPath(), "captured".getBytes(StandardCharsets.UTF_8));
			expectIo(output, target.getAbsolutePath());
			expectIo(output, "relative.jpg");
			expectIo(publish, staged, target);
			assertEquals("original", new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8));
			assertTrue(staged.isFile());
			assertTrue(target.delete());
			Files.write(staged.toPath(), new byte[0]);
			expectIo(publish, staged, target);
			assertFalse(target.exists());
			Files.write(staged.toPath(), "captured".getBytes(StandardCharsets.UTF_8));
			publish.invoke(null, staged, target);
			assertFalse(staged.exists());
			assertEquals("captured", new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8));
		} finally {
			Files.deleteIfExists(staged.toPath());
			Files.deleteIfExists(target.toPath());
			Files.deleteIfExists(directory.toPath());
		}
	}

	/**
	 * 示例必须能够引用新事件和录像签名，不能保留无效格式参数。
	 *
	 * @throws Exception 导出成员或示例编译失败
	 */
	public void testContractAndRealExampleCompile() throws Exception {
		assertTrue(摄像框.class.getMethod("错误信息").isAnnotationPresent(SimpleProperty.class));
		assertTrue(摄像框.class.getMethod("操作失败", String.class, String.class).isAnnotationPresent(SimpleEvent.class));
		assertTrue(摄像框.class.getMethod("预览就绪").isAnnotationPresent(SimpleEvent.class));
		assertTrue(摄像框.class.getMethod("录像完毕", String.class).isAnnotationPresent(SimpleEvent.class));
		assertEquals(void.class, 摄像框.class.getMethod("开始录像", String.class, int.class, boolean.class).getReturnType());
		assertNotNull(Compiler.compileForUnitTesting(new Project(new File("examples/摄像框/project.properties"))));
	}

	/**
	 * 获取无需相机硬件的内部纯规则。
	 *
	 * @param name 方法名
	 * @param types 参数类型
	 * @return 可访问的方法
	 * @throws Exception 方法不存在
	 */
	private static Method helper(String name, Class<?>... types) throws Exception {
		Method method = 摄像框Impl.class.getDeclaredMethod(name, types);
		method.setAccessible(true);
		return method;
	}

	/**
	 * 断言一次受保护文件操作明确失败。
	 *
	 * @param method 目标方法
	 * @param arguments 实参
	 * @throws Exception 反射异常或断言失败
	 */
	private void expectIo(Method method, Object... arguments) throws Exception {
		try {
			method.invoke(null, arguments);
			fail("应拒绝该文件操作");
		} catch (InvocationTargetException failure) {
			assertTrue(failure.getCause() instanceof IOException);
		}
	}
}

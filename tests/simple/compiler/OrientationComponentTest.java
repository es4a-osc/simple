package simple.compiler;

import java.io.File;
import java.lang.reflect.Method;

import junit.framework.TestCase;

import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.components.方向传感器;
import simple.runtime.components.impl.android.方向传感器Impl;

/**
 * 验证方向传感器的角度约定、倾斜范围、导出状态和真实示例。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class OrientationComponentTest extends TestCase {

	/**
	 * 新增状态与事件必须能被真实 Simple 项目引用。
	 *
	 * @throws Exception 成员不存在或示例无法读取
	 */
	public void testContractAndRealExampleCompile() throws Exception {
		for (String name : new String[]{"可用", "启用", "正在监听", "已有方向", "错误信息"}) {
			assertTrue(方向传感器.class.getMethod(name).isAnnotationPresent(SimpleProperty.class));
		}
		assertTrue(方向传感器.class.getMethod("监听状态改变", boolean.class, String.class)
			.isAnnotationPresent(SimpleEvent.class));
		assertNotNull(Compiler.compileForUnitTesting(new Project(new File("examples/方向传感器/project.properties"))));
	}

	/**
	 * 水平四个方位与绕固定轴倾斜时的正负号、范围不能随实现路径改变。
	 *
	 * @throws Exception 方向计算方法不可访问
	 */
	public void testCardinalHeadingsAndLegacyAngleRanges() throws Exception {
		checkAngles(new float[]{1, 0, 0, 0, 1, 0, 0, 0, 1}, 0, 0, 0);
		checkAngles(new float[]{0, 1, 0, -1, 0, 0, 0, 0, 1}, 90, 0, 0);
		checkAngles(new float[]{-1, 0, 0, 0, -1, 0, 0, 0, 1}, 180, 0, 0);
		checkAngles(new float[]{0, -1, 0, 1, 0, 0, 0, 0, 1}, 270, 0, 0);

		for (int degrees : new int[]{-120, -30, 30, 120}) {
			float cosine = (float) Math.cos(Math.toRadians(degrees));
			float sine = (float) Math.sin(Math.toRadians(degrees));
			checkAngles(new float[]{1, 0, 0, 0, cosine, -sine, 0, sine, cosine},
				Math.abs(degrees) > 90 ? 180 : 0, -degrees, 0);
		}
		for (int degrees : new int[]{-30, 30}) {
			float cosine = (float) Math.cos(Math.toRadians(degrees));
			float sine = (float) Math.sin(Math.toRadians(degrees));
			checkAngles(new float[]{cosine, 0, sine, 0, 1, 0, -sine, 0, cosine},
				0, 0, -degrees);
		}
	}

	/**
	 * 无效矩阵不能伪装成有效零角度，负方位归一化后也不能舍入为 360。
	 *
	 * @throws Exception 计算方法不可访问
	 */
	public void testInvalidMatrixAndHeadingWrap() throws Exception {
		Method matrix = calculation("anglesFromMatrix", float[].class, float[].class);
		assertEquals(Boolean.FALSE, matrix.invoke(null,
			new float[]{Float.NaN, 0, 0, 0, 1, 0, 0, 0, 1}, new float[3]));
		assertEquals(Boolean.FALSE, matrix.invoke(null,
			new float[]{1, 0, 0, 0, Float.POSITIVE_INFINITY, 0, 0, 0, 1}, new float[3]));
		Method normalize = calculation("normalizeYaw", float.class);
		for (float value : new float[]{-720, -360, -90, -0.000001f, 0, 359.999f, 360, 720}) {
			float normalized = (Float) normalize.invoke(null, value);
			assertTrue("方位必须在 [0,360)：" + normalized, normalized >= 0 && normalized < 360);
		}
		assertEquals(270f, (Float) normalize.invoke(null, -90f), 0.0001f);
	}

	/**
	 * 正负姿态对称，超出 90 度的俯仰也必须保持倾斜程度在 [0,1]。
	 *
	 * @throws Exception 倾斜计算方法不可访问
	 */
	public void testTiltMagnitudeIsSymmetricAndBounded() throws Exception {
		Method method = calculation("tiltMagnitude", float.class, float.class);
		for (int pitch = -180; pitch <= 180; pitch += 15) {
			for (int roll = -90; roll <= 90; roll += 15) {
				float value = (Float) method.invoke(null, (float) pitch, (float) roll);
				float opposite = (Float) method.invoke(null, (float) -pitch, (float) -roll);
				assertTrue(value >= 0 && value <= 1);
				assertEquals(value, opposite, 0.00001f);
			}
		}
		assertEquals(0f, (Float) method.invoke(null, 0f, 0f), 0.00001f);
		assertEquals(1f, (Float) method.invoke(null, -180f, -90f), 0.00001f);
	}

	/**
	 * 检查一个独立构造的旋转矩阵对应的角度。
	 *
	 * @param matrix 九元素旋转矩阵
	 * @param yaw 期望方位
	 * @param pitch 期望俯仰
	 * @param roll 期望翻滚
	 * @throws Exception 计算方法不可访问
	 */
	private void checkAngles(float[] matrix, float yaw, float pitch, float roll) throws Exception {
		float[] angles = new float[3];
		assertEquals(Boolean.TRUE, calculation("anglesFromMatrix", float[].class, float[].class)
			.invoke(null, matrix, angles));
		assertEquals(yaw, angles[0], 0.0001f);
		assertEquals(pitch, angles[1], 0.0001f);
		assertEquals(roll, angles[2], 0.0001f);
	}

	/**
	 * 定位不调用 Android 服务的纯计算方法，直接验证生产实现。
	 *
	 * @param name 方法名
	 * @param parameters 参数类型
	 * @return 可调用的计算方法
	 * @throws Exception 方法不存在或无法访问
	 */
	private Method calculation(String name, Class<?>... parameters) throws Exception {
		Method method = 方向传感器Impl.class.getDeclaredMethod(name, parameters);
		method.setAccessible(true);
		return method;
	}
}

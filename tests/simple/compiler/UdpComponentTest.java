package simple.compiler;

import java.io.File;
import junit.framework.TestCase;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.components.UDP数据报;

/**
 * 验证 UDP 异步导出契约及完整示例的编译。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class UdpComponentTest extends TestCase {

	/**
	 * 调用受理、实际绑定及发送完成必须分别提供结果。
	 *
	 * @throws Exception 成员缺失或类型错误
	 */
	public void testAsynchronousContract() throws Exception {
		assertEquals(boolean.class, UDP数据报.class.getMethod("开始监听", int.class).getReturnType());
		assertEquals(boolean.class, UDP数据报.class.getMethod("发送数据", byte[].class, String.class, int.class).getReturnType());
		assertEquals(boolean.class, UDP数据报.class.getMethod("置接收数据包大小", int.class).getReturnType());
		assertTrue(UDP数据报.class.getMethod("启动完毕", boolean.class, int.class).isAnnotationPresent(SimpleEvent.class));
		assertTrue(UDP数据报.class.getMethod("发送完毕", int.class, String.class, int.class).isAnnotationPresent(SimpleEvent.class));
		assertTrue(UDP数据报.class.getMethod("监听端口").isAnnotationPresent(SimpleProperty.class));
		assertTrue(UDP数据报.class.getMethod("操作失败", String.class, String.class).isAnnotationPresent(SimpleEvent.class));
	}

	/**
	 * 真实示例必须能调用新增属性、事件和返回值。
	 *
	 * @throws Exception 示例读取或编译失败
	 */
	public void testRealExampleCompiles() throws Exception {
		assertNotNull(Compiler.compileForUnitTesting(new Project(new File("examples/UDP数据报/project.properties"))));
	}
}

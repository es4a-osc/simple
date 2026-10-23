package simple.compiler;

import java.io.File;
import junit.framework.TestCase;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.components.TCP客户端;
import simple.runtime.components.TCP服务端;

/**
 * 验证 TCP 两端的异步导出契约及现有完整示例编译。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class TcpComponentTest extends TestCase {

	/**
	 * 受理结果、监听结果和发送完成必须能够被 Simple 调用方区分。
	 *
	 * @throws Exception 成员不存在或类型不符
	 */
	public void testAsynchronousContract() throws Exception {
		assertEquals(boolean.class, TCP客户端.class.getMethod("连接服务器", String.class, int.class, int.class).getReturnType());
		assertEquals(boolean.class, TCP客户端.class.getMethod("发送数据", byte[].class).getReturnType());
		assertEquals(boolean.class, TCP服务端.class.getMethod("启动服务器", int.class).getReturnType());
		assertEquals(boolean.class, TCP服务端.class.getMethod("发送数据", byte[].class, String.class, int.class).getReturnType());
		assertTrue(TCP服务端.class.getMethod("启动完毕", boolean.class, int.class).isAnnotationPresent(SimpleEvent.class));
		assertTrue(TCP客户端.class.getMethod("发送完毕", int.class).isAnnotationPresent(SimpleEvent.class));
		for (Class<?> type : new Class<?>[]{TCP客户端.class, TCP服务端.class}) {
			assertTrue(type.getMethod("错误信息").isAnnotationPresent(SimpleProperty.class));
			assertTrue(type.getMethod("操作失败", String.class, String.class).isAnnotationPresent(SimpleEvent.class));
		}
	}

	/**
	 * 两个真实示例必须能引用新属性、事件和函数返回值。
	 *
	 * @throws Exception 示例读取或编译失败
	 */
	public void testBothRealExamplesCompile() throws Exception {
		for (String name : new String[]{"TCP服务端", "TCP客户端"}) {
			assertNotNull(Compiler.compileForUnitTesting(new Project(new File("examples/" + name + "/project.properties"))));
		}
	}
}

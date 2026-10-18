package simple.runtime;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.UUID;

import junit.framework.TestCase;

import simple.runtime.annotations.SimpleFunction;

/**
 * 验证程序标识生成的导出签名、标准格式及 UUID 版本。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class ProgramOperationsTest extends TestCase {

	/**
	 * 标识生成以无参数静态函数导出，并返回文本。
	 *
	 * @throws Exception 导出方法缺失。
	 */
	public void testGuidExportedContract() throws Exception {
		Method method = 程序操作.class.getMethod("生成GUID");
		assertTrue(method.isAnnotationPresent(SimpleFunction.class));
		assertTrue(Modifier.isStatic(method.getModifiers()));
		assertEquals(String.class, method.getReturnType());
	}

	/**
	 * 多次调用均返回带连字符的小写标准 UUID，采用随机版本与标准变体。
	 */
	public void testGuidFormatAndVersion() {
		for (int index = 0; index < 1000; index++) {
			String text = 程序操作.生成GUID();
			assertTrue(text, text.matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"));

			UUID identifier = UUID.fromString(text);
			assertEquals(text, identifier.toString());
			assertEquals(4, identifier.version());
			assertEquals(2, identifier.variant());
		}
	}
}

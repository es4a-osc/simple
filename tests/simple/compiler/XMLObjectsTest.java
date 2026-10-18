package simple.compiler;

import java.io.File;
import java.lang.reflect.Method;

import junit.framework.TestCase;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.collections.XML生成器;
import simple.runtime.collections.XML解析器;
import simple.runtime.errors.XML错误;
import simple.runtime.errors.运行错误;

/**
 * 验证 XML 对象导出、纯 Java 状态校验和真实 Simple 示例编译。
 *
 * <p>Android XML 引擎不在宿主 android.jar 桩上执行，往返与格式验证由示例 APK 自检覆盖。</p>
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class XMLObjectsTest extends TestCase {

	/**
	 * XML 类型是独立普通对象，重载按参数数量区分。
	 *
	 * @throws Exception 导出签名缺失。
	 */
	public void testExportedContract() throws Exception {
		assertFalse(XML解析器.class.isAnnotationPresent(SimpleComponent.class));
		assertFalse(XML生成器.class.isAnnotationPresent(SimpleComponent.class));
		assertTrue(XML解析器.class.getMethod("解析字节集", byte[].class)
			.isAnnotationPresent(SimpleFunction.class));
		assertTrue(XML解析器.class.getMethod("解析字节集", byte[].class, String.class)
			.isAnnotationPresent(SimpleFunction.class));
		assertTrue(XML生成器.class.getMethod("设置属性", String.class, String.class, String.class)
			.isAnnotationPresent(SimpleFunction.class));
		assertEquals(byte[].class, XML生成器.class.getMethod("导出字节集").getReturnType());
		assertEquals(0, XML解析器.节点_文档开始);
		assertEquals(1, XML解析器.节点_文档结束);
	}

	/**
	 * 未初始化时给出可捕获错误，重置清空错误而不创建 Android 解析器。
	 */
	public void testUnsetStateAndReset() {
		XML解析器 reader = new XML解析器();
		XML生成器 writer = new XML生成器();
		try {
			reader.读取下一个();
			fail("未设置输入应失败");
		} catch (XML错误 expected) {
			assertSame(expected, 运行错误.convertToRuntimeError(expected));
			assertEquals(expected.getMessage(), reader.取错误信息());
		}
		try {
			writer.导出字节集();
			fail("未完成文档不得导出");
		} catch (XML错误 expected) {
			assertEquals(expected.getMessage(), writer.取错误信息());
		}

		reader.重置();
		writer.重置();
		assertEquals("", reader.取错误信息());
		assertEquals("", writer.取错误信息());
	}

	/**
	 * 空输入引用和不存在的字符集在创建系统 XML 引擎前被明确拒绝。
	 */
	public void testInputAndCharsetValidation() {
		XML解析器 reader = new XML解析器();
		try {
			reader.解析文本(null);
			fail("空引用应失败");
		} catch (XML错误 expected) {
			assertTrue(reader.取错误信息().contains("空引用"));
		}
		try {
			reader.解析字节集(new byte[0], "ES4A-no-such-charset");
			fail("不存在的编码应失败");
		} catch (XML错误 expected) {
			assertNotNull(expected.getCause());
		}

		XML生成器 writer = new XML生成器();
		try {
			writer.开始文档("ES4A-no-such-charset");
			fail("不存在的编码应失败");
		} catch (XML错误 expected) {
			assertTrue(writer.取错误信息().contains("开始 XML 文档失败"));
		}
	}

	/**
	 * 名称校验支持中文和组合字符，拒绝冒号、非法首字符与未配对代理项。
	 *
	 * @throws Exception 名称校验反射失败。
	 */
	public void testNamespaceLocalNames() throws Exception {
		Method isName = XML生成器.class.getDeclaredMethod("isName", String.class);
		isName.setAccessible(true);
		for (String name : new String[]{"根", "_item-1.2", "a\u0301", "\uD83D\uDE00"}) {
			assertEquals(name, Boolean.TRUE, isName.invoke(null, name));
		}
		for (String name : new String[]{"", "1根", "a b", "p:root", "\uD800", "a/"}) {
			assertEquals(name, Boolean.FALSE, isName.invoke(null, name));
		}
	}

	/**
	 * 编译真实示例，覆盖所有公开方法、常量、重载和 XML错误 分支。
	 *
	 * @throws Exception 项目读取失败。
	 */
	public void testRealExampleCompiles() throws Exception {
		assertNotNull(Compiler.compileForUnitTesting(new Project(new File("examples/XML处理/project.properties"))));
	}
}

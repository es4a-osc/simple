package simple.classfiles;

import junit.framework.TestCase;

/**
 * 通过真实 JVM 类加载与执行核对字符串常量的 Modified UTF-8 编码和长度边界。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class ConstantPoolTest extends TestCase {

	/**
	 * ASCII、中文、NUL、表情及单独的代理代码单元都须保持原有 UTF-16 内容。
	 *
	 * @throws Exception 生成的类无法加载、执行或返回值不符。
	 */
	public void testStringConstantsPreserveUtf16Contents() throws Exception {
		String[] values = {"", "ASCII", "中文", "\u0000", "A\u0000B", "😀", "中文😀", "\uD83D", "\uDE00"};
		for (String value : values) {
			assertEquals(value, roundTrip(value));
		}
	}

	/**
	 * 65535 字节限制按 Modified UTF-8 编码计算，不能按字符数或普通 UTF-8 长度计算。
	 *
	 * @throws Exception 合法边界字符串无法生成或执行。
	 */
	public void testEncodedByteLimitAcceptsBoundaryAndRejectsOverflow() throws Exception {
		String[] valid = {"a".repeat(65535), "\u0000".repeat(32767) + "a",
				"中".repeat(21845), "😀".repeat(10922) + "abc"};
		for (String value : valid) {
			assertEquals(value, roundTrip(value));
		}
		String[] invalid = {"a".repeat(65536), "\u0000".repeat(32768),
				"中".repeat(21846), "😀".repeat(10923)};
		for (String value : invalid) {
			try {
				roundTrip(value);
				fail("超出 Modified UTF-8 字节上限的常量必须报错");
			} catch (ConstantPoolOverflowException expected) {
				assertTrue(expected.getMessage().contains("utf8 constant too long"));
			}
		}
	}

	/**
	 * 生成返回指定常量的静态方法，加载到独立 JVM 类加载器并读取实际返回值。
	 *
	 * @param value 要核对的字符串。
	 * @return JVM 执行生成方法后返回的字符串。
	 * @throws Exception 方法无法生成、类无法加载或执行失败。
	 */
	private static String roundTrip(String value) throws Exception {
		ClassFile file = new ClassFile(ClassFile.ACC_PUBLIC, "fixture/UnicodeConstant", "java/lang/Object");
		Method method = file.newMethod((short) (Method.ACC_PUBLIC | Method.ACC_STATIC), "value", "()Ljava/lang/String;");
		method.startCodeGeneration();
		method.generateInstrLdc(value);
		method.generateInstrAreturn();
		method.finishCodeGeneration();
		Class<?> generated = new GeneratedClassLoader().define(file.generate());
		return (String) generated.getMethod("value").invoke(null);
	}

	/**
	 * 为每个用例隔离同名生成类，不使用文件或应用类加载缓存。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	private static final class GeneratedClassLoader extends ClassLoader {

		/**
		 * 使用测试类的加载器作为父加载器。
		 */
		private GeneratedClassLoader() {
			super(ConstantPoolTest.class.getClassLoader());
		}

		/**
		 * 将真实生成的 class 字节交给 JVM 解析和校验。
		 *
		 * @param data 生成类的完整字节。
		 * @return 已定义的类。
		 */
		private Class<?> define(byte[] data) {
			return defineClass(null, data, 0, data.length);
		}
	}
}

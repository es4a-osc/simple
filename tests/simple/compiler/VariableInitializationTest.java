package simple.compiler;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import junit.framework.TestCase;
import simple.compiler.parser.Parser;
import simple.compiler.scanner.Scanner;

/**
 * 验证变量声明初值的诊断边界；运行结果与 JVM 验证另由 Simple 冒烟工程覆盖。
 *
 * @author 树先生 xhwsd@qq.com
 */
public class VariableInitializationTest extends TestCase {

	/**
	 * 解析并检查对象单元中的声明，确认编译器拒绝输入并按需报告指定诊断。
	 *
	 * @param body  对象单元的用户代码，不包含属性区
	 * @param diagnostic  诊断中必须包含的文本；为 {@code null} 时仅检查存在错误
	 * @throws Exception  初始化测试环境或解析源码失败时抛出
	 */
	private void rejects(String body, String diagnostic) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		PrintStream stream = new PrintStream(output, true, "UTF-8");
		Compiler compiler = new Compiler(Compiler.Platform.None, stream, stream);
		Parser parser = new Parser(compiler, new Scanner(compiler,
				body + "\n$属性\n$资源 $对象\n$结束 $属性\n"), "test.Initializers");
		parser.parse();
		compiler.resolve();
		assertTrue(output.toString("UTF-8"), compiler.getErrorCount() > 0);
		if (diagnostic != null) {
			assertTrue(output.toString("UTF-8"), output.toString("UTF-8").contains(diagnostic));
		}
	}

	/**
	 * 验证局部变量初值不能读取正在声明的变量自身。
	 *
	 * @throws Exception  初始化测试环境或解析源码失败时抛出
	 */
	public void testRejectsLocalSelfReference() throws Exception {
		rejects("过程 Run()\n变量 value 为 整数型 = value + 1\n结束 过程\n", "value");
	}

	/**
	 * 验证局部变量初值不能读取后续声明的局部变量。
	 *
	 * @throws Exception  初始化测试环境或解析源码失败时抛出
	 */
	public void testRejectsLocalForwardReference() throws Exception {
		rejects("过程 Run()\n变量 first 为 整数型 = later\n变量 later 为 整数型 = 1\n结束 过程\n", "later");
	}

	/**
	 * 验证成员数组声明不能同时指定分配尺寸和初始化表达式。
	 *
	 * @throws Exception  初始化测试环境或解析源码失败时抛出
	 */
	public void testRejectsSizedArrayInitializer() throws Exception {
		rejects("变量 values 为 整数型(3) = 创建 整数型(3)\n", "指定数组尺寸");
	}

	/**
	 * 验证局部数组声明不能同时指定分配尺寸和初始化表达式。
	 *
	 * @throws Exception  初始化测试环境或解析源码失败时抛出
	 */
	public void testRejectsLocalSizedArrayInitializer() throws Exception {
		rejects("过程 Run()\n变量 values 为 整数型(3) = 创建 整数型(3)\n结束 过程\n", "指定数组尺寸");
	}

	/**
	 * 验证静态初值不能在没有接收对象的情况下读取实例成员。
	 *
	 * @throws Exception  初始化测试环境或解析源码失败时抛出
	 */
	public void testRejectsInstanceReadInStaticInitializer() throws Exception {
		rejects("变量 member 为 整数型\n静态 变量 value 为 整数型 = member\n", null);
	}

	/**
	 * 验证静态初值不能使用本对象。
	 *
	 * @throws Exception  初始化测试环境或解析源码失败时抛出
	 */
	public void testRejectsThisInStaticInitializer() throws Exception {
		rejects("静态 变量 value 为 对象 = 本对象\n", null);
	}

	/**
	 * 验证没有返回值的过程调用不能作为变量初值。
	 *
	 * @throws Exception  初始化测试环境或解析源码失败时抛出
	 */
	public void testRejectsProcedureAsInitialValue() throws Exception {
		rejects("变量 value 为 整数型 = Run()\n静态 过程 Run()\n结束 过程\n", null);
	}

	/**
	 * 验证元素类型不兼容的数组不能作为声明数组的初值。
	 *
	 * @throws Exception  初始化测试环境或解析源码失败时抛出
	 */
	public void testRejectsArrayTypeMismatch() throws Exception {
		rejects("变量 values 为 整数型() = 创建 文本型(3)\n", null);
	}
}

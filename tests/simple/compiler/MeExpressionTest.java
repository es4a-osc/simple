package simple.compiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import junit.framework.TestCase;
import simple.compiler.Compiler.Platform;
import simple.compiler.parser.Parser;
import simple.compiler.scanner.Scanner;

/**
 * 验证显式本对象在静态环境中提前报错，实例事件、函数、过程及属性访问器保持合法。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class MeExpressionTest extends TestCase {

	public void testRejectsMeInLoadEvent() throws Exception {
		assertRejected("事件 Check.加载()\r\n\t保存实例 = 本对象\r\n结束 事件\r\n");
	}

	public void testRejectsQualifiedMeInLoadEvent() throws Exception {
		assertRejected("事件 Check.加载()\r\n\t静态计数 = 本对象.成_计数\r\n结束 事件\r\n");
	}

	public void testRejectsMeInStaticProcedure() throws Exception {
		assertRejected("静态 过程 Save()\r\n\t保存实例 = 本对象\r\n结束 过程\r\n");
	}

	public void testRejectsMeInStaticFunction() throws Exception {
		assertRejected("静态 函数 Get() 为 对象\r\n\tGet = 本对象\r\n结束 函数\r\n");
	}

	public void testRejectsMeWhenStaticParameterOccupiesLocalZero() throws Exception {
		assertRejected("静态 过程 Save(值 为 整数型)\r\n\t保存实例 = 本对象\r\n结束 过程\r\n");
	}

	public void testAllowsMeInInitializeEvent() throws Exception {
		assertAllowed("事件 Check.初始化()\r\n\t保存实例 = 本对象\r\n结束 事件\r\n");
	}

	public void testAllowsMeInInstanceProcedure() throws Exception {
		assertAllowed("过程 Save()\r\n\t保存实例 = 本对象\r\n结束 过程\r\n");
	}

	public void testAllowsMeInInstanceFunction() throws Exception {
		assertAllowed("函数 Get() 为 对象\r\n\tGet = 本对象\r\n结束 函数\r\n");
	}

	public void testAllowsMeInPropertyAccessors() throws Exception {
		assertAllowed("属性 Current 为 对象\r\n"
				+ "\t获取\r\n\t\tCurrent = 本对象\r\n\t结束 获取\r\n"
				+ "\t设置\r\n\t\t保存实例 = 本对象\r\n\t结束 设置\r\n结束 属性\r\n");
	}

	public void testAllowsStaticMembersInLoadEvent() throws Exception {
		assertAllowed("事件 Check.加载()\r\n\t静态计数 = 100\r\n结束 事件\r\n");
	}

	private void assertRejected(String body) throws Exception {
		ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
		Compiler compiler = resolve(body, diagnostics);
		String text = diagnostics.toString("UTF-8");
		assertEquals(text, 1, compiler.getErrorCount());
		assertTrue(text, text.contains("在非实例成员中使用实例成员 '本对象'"));
	}

	private void assertAllowed(String body) throws Exception {
		ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
		Compiler compiler = resolve(body, diagnostics);
		assertEquals(diagnostics.toString("UTF-8"), 0, compiler.getErrorCount());
	}

	/**
	 * 使用当前 Android 平台加载运行库，解析并检查指定的测试成员。
	 *
	 * @param body 测试对象中的用户代码。
	 * @param diagnostics 接收编译诊断的缓冲区。
	 * @return 完成解析和语义检查的编译器。
	 * @throws Exception 诊断流初始化或运行库加载失败。
	 */
	private Compiler resolve(String body, ByteArrayOutputStream diagnostics) throws Exception {
		PrintStream messages = new PrintStream(diagnostics, true, "UTF-8");
		Compiler compiler = new Compiler(Platform.None, messages, messages);
		RuntimeLoader loader = new RuntimeLoader(compiler,
				new File(System.getenv("ANDROID_HOME"), "platforms/android-30/android.jar").getAbsolutePath(),
				new File("build/runtime/android/classes").getAbsolutePath(), null);
		loader.loadSimpleObjects();
		String source = "静态 变量 保存实例 为 对象\r\n"
				+ "静态 变量 静态计数 为 整数型\r\n变量 成_计数 为 整数型\r\n\r\n"
				+ body + "\r\n$属性\r\n\t$资源 $对象\r\n$结束 $属性\r\n";
		Parser parser = new Parser(compiler, new Scanner(compiler, source), "simple.compiler.me.Check");
		parser.parse();
		compiler.resolve();
		return compiler;
	}
}

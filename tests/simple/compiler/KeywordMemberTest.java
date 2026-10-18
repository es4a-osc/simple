package simple.compiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import simple.classfiles.ClassFile;
import simple.compiler.Compiler.Platform;
import simple.compiler.parser.Parser;
import simple.compiler.scanner.Scanner;
import simple.compiler.symbols.NamespaceSymbol;
import simple.compiler.symbols.ObjectSymbol;

/** 验证关键字成员访问、原有语法边界以及实际生成字节码的执行结果。 */
public final class KeywordMemberTest extends TestCase {
	private static final String TYPE = "simple.runtime.testing.KeywordMemberObject";
	private ByteArrayOutputStream diagnostics;

	private Compiler parse(String source) throws Exception {
		diagnostics = new ByteArrayOutputStream();
		PrintStream messages = new PrintStream(diagnostics, true, "UTF-8");
		Compiler compiler = new Compiler(Platform.Android, messages, messages);
		new RuntimeLoader(compiler, new File("build/runtime/android/classes").getAbsolutePath(),
				new File("build/tests/classes").getAbsolutePath(), null).loadSimpleObjects();
		NamespaceSymbol.getNamespaceSymbol(compiler, "KeywordNamespace/结束");
		Parser parser = new Parser(compiler, new Scanner(compiler, source),
				"simple.compiler.keywords.Check");
		parser.parse();
		if (compiler.getErrorCount() == 0) compiler.resolve();
		return compiler;
	}

	private String unit(String code) {
		return code + "\n$属性\n\t$资源 $对象\n$结束 $属性\n";
	}

	private void assertRejected(String code, String message) throws Exception {
		Compiler compiler = parse(unit(code));
		String text = diagnostics.toString("UTF-8");
		assertTrue(text, compiler.getErrorCount() > 0);
		assertTrue(text, text.contains(message));
	}

	public void testExecutesKeywordMembersAndKeepsLikeOperator() throws Exception {
		String code = "静态 函数 Run() 为 整数型\n"
				+ "\t变量 目标 为 " + TYPE + "\n\t目标 = 创建 " + TYPE + "\n"
				+ "\t目标.结束 = 3\n\t目标.真 = 4\n\t目标.假 = 5\n\t目标.设置 = 6\n"
				+ "\t目标.下级 = 目标\n"
				+ "\t变量 文本 为 文本型\n\t文本 = 目标.匹配(\"abc\")\n"
				+ "\t如果 文本 <> \"abc\" 则 退出 函数\n"
				+ "\t如果 目标.匹配() <> \"default\" 则 退出 函数\n"
				+ "\t变量 动态 为 变体型\n\t动态 = 目标\n"
				+ "\t如果 动态.结束 <> 3 则 退出 函数\n"
				+ "\t变量 匹配结果 为 逻辑型\n\t匹配结果 = 文本 匹配 \"a.*\"\n\t如果 匹配结果 则\n"
				+ "\t\tRun = 目标.下级.结束 + (目标).真 + (目标).假 + 目标.设置 + "
				+ TYPE + ".空 + " + TYPE + ".执行()\n\t结束 如果\n结束 函数\n";
		Compiler compiler = parse(unit(code));
		assertEquals(diagnostics.toString("UTF-8"), 0, compiler.getErrorCount());
		final List<ClassFile> classes = new ArrayList<ClassFile>();
		ObjectSymbol.getObjectSymbol(compiler, "simple/compiler/keywords/Check").generate(compiler, classes);
		ClassLoader loader = new ClassLoader(getClass().getClassLoader()) {
			@Override
			protected Class<?> findClass(String name) throws ClassNotFoundException {
				for (ClassFile file : classes) {
					if (file.getName().equals(name)) {
						byte[] bytes = file.generate();
						return defineClass(name, bytes, 0, bytes.length);
					}
				}
				throw new ClassNotFoundException(name);
			}
		};
		assertEquals(33, loader.loadClass("simple.compiler.keywords.Check").getMethod("Run").invoke(null));
	}

	public void testRejectsInvalidMemberTokens() throws Exception {
		for (String member : new String[] {"123", "\"匹配\"", "+", "%", ""}) {
			assertRejected("过程 Run()\n\t变量 目标 为 " + TYPE + "\n\t目标." + member
					+ "\n结束 过程\n", "<标识符>");
		}
	}

	public void testKeepsKeywordDeclarationsReserved() throws Exception {
		assertRejected("变量 匹配 为 整数型\n", "<标识符>");
		assertRejected("过程 匹配()\n结束 过程\n", "<标识符>");
		assertRejected("事件 目标.结束()\n结束 事件\n", "<标识符>");
		assertRejected("变量 目标 为 simple.runtime.结束\n", "<标识符>");
		assertRejected("别名 目标 = simple.runtime.结束\n", "<标识符>");
	}

	public void testRejectsKeywordNamespaceInExpression() throws Exception {
		assertRejected("过程 Run()\n\tKeywordNamespace.结束.目标()\n结束 过程\n", "<标识符>");
	}

	public void testKeepsMemberAndArgumentChecks() throws Exception {
		assertRejected("过程 Run()\n\t变量 目标 为 " + TYPE
				+ "\n\t目标.否则()\n结束 过程\n", "否则");
		assertRejected("过程 Run()\n\t变量 目标 为 " + TYPE
				+ "\n\t变量 参数 为 变体型(1)\n\t目标.匹配(参数)\n结束 过程\n", "文本型");
		assertRejected("过程 Run()\n\t" + TYPE + ".匹配(\"abc\")\n结束 过程\n", "实例成员");
	}
}

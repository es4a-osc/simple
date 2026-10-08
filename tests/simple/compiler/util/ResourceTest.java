package simple.compiler.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import junit.framework.TestCase;
import simple.compiler.Compiler;
import simple.compiler.Compiler.Platform;
import simple.compiler.parser.Parser;
import simple.compiler.scanner.Scanner;
import simple.util.Files;

/**
 * 测试{@link Resource}生成的资源索引源码。
 *
 * @author 树先生 xhwsd@qq.com
 */
public class ResourceTest extends TestCase {

	private static final String ENCODING = "UTF-8";

	/**
	 * 验证普通资源编号会转换为保持原顺序的Simple常量，并跳过暂不支持的styleable。
	 */
	public void testToSimpleSource() throws Exception {
		File symbolFile = createSymbolFile(
				"int drawable es4a 0x7f010000\n"
				+ "int drawable icon 0x7f010001\r\n"
				+ "\r\n"
				+ "int[] styleable TestView { 0x7f010000 }\n"
				+ "int styleable TestView_icon 0\n"
				+ "int id account 0x7f020000\n");
		String source = Resource.toSimpleSource(symbolFile);

		assertEquals(
				"' 此单元由编译器根据 R.txt 自动生成，请勿修改。\r\n"
				+ "' 资源常量名称格式：资源类型_资源名称。\r\n"
				+ "\r\n"
				+ "常量 drawable_es4a 为 整数型 = &H7F010000\r\n"
				+ "常量 drawable_icon 为 整数型 = &H7F010001\r\n"
				+ "常量 id_account 为 整数型 = &H7F020000\r\n"
				+ "\r\n"
				+ "$属性\r\n"
				+ "\t$资源 $对象\r\n"
				+ "$结束 $属性\r\n",
				source);

		// 生成结果必须是编译器能够直接解析和解析符号的Simple源码。
		Compiler compiler = new Compiler(Platform.None, System.out, System.err);
		new Parser(compiler, new Scanner(compiler, source), "test.SimpleResources").parse();
		compiler.resolve();
		assertEquals(0, compiler.getErrorCount());
	}

	/**
	 * 验证生成内容能够写入指定文件。
	 */
	public void testWriteSimpleSource() throws Exception {
		File symbolFile = createSymbolFile("int string app_name 0x7f050000\n");
		File temporaryDirectory = java.nio.file.Files.createTempDirectory("Resource").toFile();
		File outputDirectory = new File(temporaryDirectory, "generated");
		File simpleFile = new File(outputDirectory, "SimpleResources.simple");
		temporaryDirectory.deleteOnExit();
		outputDirectory.deleteOnExit();
		simpleFile.deleteOnExit();

		Resource.writeSimpleSource(symbolFile, simpleFile);

		assertTrue(outputDirectory.isDirectory());
		assertEquals(Resource.toSimpleSource(symbolFile),
				Files.read(simpleFile, ENCODING));
	}

	/**
	 * 验证无法识别的符号不会被静默丢弃，并报告对应的R.txt行号。
	 */
	public void testRejectUnknownSymbol() throws Exception {
		File symbolFile = createSymbolFile(
				"int drawable icon 0x7f010001\n"
				+ "unknown drawable error 0x7f010002\n");

		try {
			Resource.toSimpleSource(symbolFile);
			fail("无法识别的资源符号应该导致转换失败");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("第 2 行"));
		}
	}

	private static File createSymbolFile(String contents) throws IOException {
		File file = File.createTempFile("Resource", ".txt");
		file.deleteOnExit();
		Files.write(contents.getBytes(Charset.forName(ENCODING)), file);
		return file;
	}
}

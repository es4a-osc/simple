package simple.compiler.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import simple.compiler.scanner.TokenKind;
import simple.util.Files;

/**
 * Android资源索引辅助功能。
 *
 * <p>读取AAPT2生成的{@code R.txt}，将其中的资源编号转换为Simple资源常量源码。
 * 生成的源码可以直接交给Simple编译器，也可以写入指定的{@code SimpleResources.simple}文件。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class Resource {

	/** 读取{@code R.txt}和写出{@code SimpleResources.simple}时使用的文本编码。 */
	private static final String ENCODING = "UTF-8";

	/** 生成的{@code SimpleResources.simple}统一使用项目约定的Windows换行。 */
	private static final String NEW_LINE = "\r\n";

	/** 匹配{@code int 资源类型 资源名称 0x资源编号}格式的普通资源符号。 */
	private static final Pattern RESOURCE_SYMBOL = Pattern.compile("^int\\s+(\\S+)\\s+(\\S+)\\s+0[xX]([0-9a-fA-F]+)\\s*$");

	/** 匹配当前不转换的{@code styleable}数组和数组索引。 */
	private static final Pattern STYLEABLE_SYMBOL = Pattern.compile("^(?:int\\[\\]|int)\\s+styleable\\s+.*$");

	private Resource() {
	}

	/**
	 * 读取AAPT2生成的{@code R.txt}并转换为Simple源码字符串。
	 *
	 * <p>资源名称按“资源类型_资源名称”展开，例如{@code drawable_icon}。资源编号不重新
	 * 分配，只把AAPT2的{@code 0x7f010001}格式转换为Simple的{@code &H7F010001}格式。
	 * 当前不生成{@code styleable}数组及其数组索引。
	 *
	 * @param symbolFile AAPT2生成的{@code R.txt}文件
	 * @return 可直接交给Simple编译器解析的{@code SimpleResources.simple}源码
	 * @throws IOException 读取失败或遇到无法识别的资源符号时抛出
	 */
	public static String toSimpleSource(File symbolFile) throws IOException {
		String symbols = Files.read(symbolFile, ENCODING);
		StringBuilder source = new StringBuilder();
		// 关键字统一引用TokenKind，避免生成器重复维护Simple语法文本。
		source.append(TokenKind.TOK_COMMENT).append(" 此单元由编译器根据 R.txt 自动生成，请勿修改。").append(NEW_LINE);
		source.append(TokenKind.TOK_COMMENT).append(" 资源常量名称格式：资源类型_资源名称。").append(NEW_LINE);
		source.append(NEW_LINE);

		BufferedReader reader = new BufferedReader(new StringReader(symbols));
		String line;
		int lineNumber = 0;
		while ((line = reader.readLine()) != null) {
			lineNumber++;
			String trimmedLine = line.trim();
			if (trimmedLine.length() == 0 || STYLEABLE_SYMBOL.matcher(trimmedLine).matches()) {
				continue;
			}

			Matcher matcher = RESOURCE_SYMBOL.matcher(trimmedLine);
			if (!matcher.matches()) {
				throw new IOException("无法识别 R.txt 第 " + lineNumber + " 行：" + line);
			}

			source.append(TokenKind.TOK_CONST).append(' ')
					.append(matcher.group(1)).append('_').append(matcher.group(2))
					.append(' ').append(TokenKind.TOK_AS).append(' ')
					.append(TokenKind.TOK_INTEGER).append(" = &H")
					.append(matcher.group(3).toUpperCase(Locale.US))
					.append(NEW_LINE);
		}

		// Simple对象单元必须带有属性区，内存源码也遵守相同语法。
		source.append(NEW_LINE);
		source.append(TokenKind.TOK_$PROPERTIES).append(NEW_LINE);
		source.append('\t').append(TokenKind.TOK_$SOURCE).append(' ').append(TokenKind.TOK_$OBJECT).append(NEW_LINE);
		source.append(TokenKind.TOK_$END).append(' ').append(TokenKind.TOK_$PROPERTIES).append(NEW_LINE);
		return source.toString();
	}

	/**
	 * 把AAPT2生成的{@code R.txt}转换并写入指定的{@code SimpleResources.simple}文件。
	 *
	 * <p>目标文件的父目录不存在时会自动创建，已有文件会被覆盖，输出编码为UTF-8。
	 *
	 * @param symbolFile AAPT2生成的{@code R.txt}文件
	 * @param simpleFile 要生成的{@code SimpleResources.simple}文件
	 * @throws IOException 读取、转换或写入失败时抛出
	 */
	public static void writeSimpleSource(File symbolFile, File simpleFile) throws IOException {
		File parentDirectory = simpleFile.getAbsoluteFile().getParentFile();
		if (parentDirectory != null && !parentDirectory.exists() && !parentDirectory.mkdirs()) {
			throw new IOException("无法创建目录：" + parentDirectory);
		}
		Files.write(toSimpleSource(symbolFile).getBytes(Charset.forName(ENCODING)), simpleFile);
	}
}

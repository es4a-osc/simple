package simple.runtime;

import android.util.Base64;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;

/**
 * 提供 Unicode 转义、URL 参数、Base64、十六进制及文本与字节数组的编码转换。
 *
 * <p>未指定字符集时使用 UTF-8。URL 接口处理表单或查询参数值，不用于编码完整网址。
 * Base64 使用标准字母表，不属于加密。空输入返回空结果；除十六进制接口的宽松约定外，空引用和无效编码参数会抛出错误。</p>
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 编码转换 {

	/**
	 * 静态方法集，不创建实例。
	 */
	private 编码转换() {
	}

	/* Unicode 转义 */

	/**
	 * 解析四位 Unicode 转义，以及制表、回车、换行和换页转义。
	 *
	 * <p>保留原接口规则：其它转义去掉反斜杠，保留其后的字符。
	 * 不完整的转义或非十六进制 Unicode 转义会抛出错误。</p>
	 *
	 * @param text 待解析的文本。
	 * @return 解析后的文本。
	 */
	@SimpleFunction
	public static String UCS2解析(String text) {
		requireText(text);
		StringBuilder result = new StringBuilder(text.length());

		for (int index = 0; index < text.length();) {
			char value = text.charAt(index++);
			if (value != '\\') {
				result.append(value);
				continue;
			}

			if (index == text.length()) {
				throw new IllegalArgumentException("文本末尾的转义不完整。");
			}

			value = text.charAt(index++);

			// 四位十六进制还原一个 UTF-16 代码单元，代理对按原顺序保留。
			if (value == 'u') {
				if (text.length() - index < 4) {
					throw new IllegalArgumentException("Unicode 转义必须包含四位十六进制数字。");
				}

				int decoded = 0;

				for (int digitIndex = 0; digitIndex < 4; digitIndex++) {
					char digit = text.charAt(index++);
					int number;

					if (digit >= '0' && digit <= '9') {
						number = digit - '0';
					} else if (digit >= 'a' && digit <= 'f') {
						number = digit - 'a' + 10;
					} else if (digit >= 'A' && digit <= 'F') {
						number = digit - 'A' + 10;
					} else {
						throw new IllegalArgumentException("Unicode 转义含有非十六进制字符。");
					}

					decoded = (decoded << 4) | number;
				}

				result.append((char) decoded);
			} else {
				// 普通转义转换控制字符；未知转义沿用原规则保留其后的字符。
				switch (value) {
					case 't':
						value = '\t';
						break;
					case 'r':
						value = '\r';
						break;
					case 'n':
						value = '\n';
						break;
					case 'f':
						value = '\f';
						break;
					default:
						break;
				}

				result.append(value);
			}
		}

		return result.toString();
	}

	/**
	 * 将每个 UTF-16 代码单元编码为四位小写十六进制 Unicode 转义。
	 *
	 * <p>补足前导零；表情等补充字符由两个代理代码单元分别编码，可用 UCS2解析还原。</p>
	 *
	 * @param text 待编码的文本。
	 * @return Unicode 转义文本。
	 */
	@SimpleFunction
	public static String UCS2编码(String text) {
		requireText(text);
		StringBuilder result = new StringBuilder();

		for (int index = 0; index < text.length(); index++) {
			char value = text.charAt(index);
			result.append("\\u");

			for (int shift = 12; shift >= 0; shift -= 4) {
				result.append(Character.forDigit((value >> shift) & 0xF, 16));
			}
		}

		return result.toString();
	}

	/* URL 参数编码 */

	/**
	 * 使用 UTF-8 编码 URL 参数值。
	 *
	 * @param text 参数原文。
	 * @return 编码后的参数值。
	 */
	@SimpleFunction
	public static String URL编码(String text) {
		return URL编码(text, "UTF-8");
	}

	/**
	 * 使用指定字符集编码 URL 参数值，空格转为加号，原加号转为百分号转义。
	 *
	 * @param text 参数原文，不是完整网址。
	 * @param encoding 字符集名称，例如 UTF-8 或 GBK。
	 * @return 编码后的参数值。
	 */
	@SimpleFunction
	public static String URL编码(String text, String encoding) {
		requireText(text);
		Charset charset = getCharset(encoding);

		try {
			return URLEncoder.encode(text, charset.name());
		} catch (UnsupportedEncodingException exception) {
			throw new IllegalArgumentException("不支持的字符集：" + encoding, exception);
		}
	}

	/**
	 * 使用 UTF-8 解码 URL 参数值。
	 *
	 * @param text 已编码的参数值。
	 * @return 解码后的参数原文。
	 */
	@SimpleFunction
	public static String URL解码(String text) {
		return URL解码(text, "UTF-8");
	}

	/**
	 * 使用指定字符集解码 URL 参数值，将加号还原为空格。
	 *
	 * <p>不完整或无效的百分号转义会抛出错误。</p>
	 *
	 * @param text 已编码的参数值。
	 * @param encoding 字符集名称，例如 UTF-8 或 GBK。
	 * @return 解码后的参数原文。
	 */
	@SimpleFunction
	public static String URL解码(String text, String encoding) {
		requireText(text);
		Charset charset = getCharset(encoding);

		try {
			return URLDecoder.decode(text, charset.name());
		} catch (UnsupportedEncodingException exception) {
			throw new IllegalArgumentException("不支持的字符集：" + encoding, exception);
		}
	}

	/* Base64 与文本字节转换 */

	/**
	 * 将字节数组编码为标准 Base64 文本，保留末尾填充且不插入换行。
	 *
	 * @param data 待编码的字节数组。
	 * @return Base64 文本。
	 */
	@SimpleFunction
	public static String Base64编码(byte[] data) {
		if (data == null) {
			throw new IllegalArgumentException("字节数组不能是空引用。");
		}

		return Base64.encodeToString(data, Base64.NO_WRAP);
	}

	/**
	 * 按 Android 标准 Base64 规则解码为字节数组。
	 *
	 * <p>允许换行及省略末尾填充，填充数量错误会抛出错误。
	 * 此接口不使用 URL 安全字母表，也不用于严格校验输入文本。</p>
	 *
	 * @param text Base64 文本。
	 * @return 解码后的字节数组。
	 */
	@SimpleFunction
	public static byte[] Base64解码(String text) {
		requireText(text);
		return Base64.decode(text, Base64.DEFAULT);
	}

	/**
	 * 使用 UTF-8 将文本转换为字节数组。
	 *
	 * @param text 待转换的文本。
	 * @return UTF-8 字节数组。
	 */
	@SimpleFunction
	public static byte[] 文本到字节(String text) {
		return 文本到字节(text, "UTF-8");
	}

	/**
	 * 使用指定字符集将文本转换为字节数组。
	 *
	 * <p>无法表示的字符按该字符集的默认替换规则处理。</p>
	 *
	 * @param text 待转换的文本。
	 * @param encoding 字符集名称，例如 UTF-8 或 GBK。
	 * @return 转换后的字节数组。
	 */
	@SimpleFunction
	public static byte[] 文本到字节(String text, String encoding) {
		requireText(text);
		return text.getBytes(getCharset(encoding));
	}

	/**
	 * 使用 UTF-8 将字节数组转换为文本。
	 *
	 * @param data 待转换的字节数组。
	 * @return 解码后的文本。
	 */
	@SimpleFunction
	public static String 字节到文本(byte[] data) {
		return 字节到文本(data, "UTF-8");
	}

	/**
	 * 使用指定字符集将字节数组转换为文本。
	 *
	 * <p>无效的字节序列按该字符集的默认替换规则处理。</p>
	 *
	 * @param data 待转换的字节数组。
	 * @param encoding 字符集名称，例如 UTF-8 或 GBK。
	 * @return 解码后的文本。
	 */
	@SimpleFunction
	public static String 字节到文本(byte[] data, String encoding) {
		if (data == null) {
			throw new IllegalArgumentException("字节数组不能是空引用。");
		}

		return new String(data, getCharset(encoding));
	}

	/* 十六进制字节编码 */

	/**
	 * 将字节数组编码为不带分隔符的大写十六进制文本。
	 *
	 * @param data 待编码的字节数组。
	 * @return 每个字节对应两个字符并保留前导零；空引用、空数组或处理失败返回空文本。
	 */
	@SimpleFunction
	public static String 字节集到十六进制(byte[] data) {
		if (data == null || data.length == 0) {
			return "";
		}

		try {
			char[] alphabet = "0123456789ABCDEF".toCharArray();
			char[] result = new char[data.length * 2];

			for (int index = 0; index < data.length; index++) {
				int value = data[index] & 0xFF;
				result[index * 2] = alphabet[value >>> 4];
				result[index * 2 + 1] = alphabet[value & 0xF];
			}

			return new String(result);
		} catch (Exception exception) {
			return "";
		}
	}

	/**
	 * 每两个字符还原一个字节，沿用既有宽松十六进制解析规则。
	 *
	 * <p>有效输入应只含 0-9、A-F 或 a-f，不含前缀和分隔符。
	 * 奇数长度会忽略最后一个字符；非法字符仍按低四位映射，不报告格式错误。
	 * 此接口不能用于校验输入，调用者需要严格格式时应先检查内容。</p>
	 *
	 * @param text 十六进制文本，大小写均可。
	 * @return 解析后的字节数组；空引用、空文本或处理失败返回空数组。
	 */
	@SimpleFunction
	public static byte[] 十六进制到字节集(String text) {
		if (text == null || text.length() == 0) {
			return new byte[0];
		}

		try {
			byte[] result = new byte[text.length() / 2];

			// 不跳过空白和分隔符，保留成对读取及末尾截断约定。
			for (int index = 0; index < result.length; index++) {
				int first = hexNibble(text.charAt(index * 2));
				int second = hexNibble(text.charAt(index * 2 + 1));
				result[index] = (byte) ((first << 4) | second);
			}

			return result;
		} catch (Exception exception) {
			return new byte[0];
		}
	}

	/**
	 * 按既有规则映射一个字符，不额外增加十六进制格式校验。
	 *
	 * @param value 待映射的字符。
	 * @return 0 至 15 的低四位数值。
	 */
	private static int hexNibble(char value) {
		if (value >= 'a') {
			return (value - 'a' + 10) & 0xF;
		}
		if (value >= 'A') {
			return (value - 'A' + 10) & 0xF;
		}

		return (value - '0') & 0xF;
	}

	/* 参数检查 */

	/**
	 * 拒绝空引用，保留空文本作为有效输入。
	 *
	 * @param text 待检查的文本。
	 */
	private static void requireText(String text) {
		if (text == null) {
			throw new IllegalArgumentException("文本不能是空引用。");
		}
	}

	/**
	 * 解析字符集名称，由 Charset 报告非法名称或未支持的字符集。
	 *
	 * @param encoding 字符集名称。
	 * @return 可用的字符集。
	 */
	private static Charset getCharset(String encoding) {
		if (encoding == null || encoding.length() == 0) {
			throw new IllegalArgumentException("字符集名称不能为空。");
		}

		return Charset.forName(encoding);
	}

	/**
	 * 解码旧协议中的 Base64 内容，保留跳过无效字符和补位的处理顺序。
	 *
	 * <p>仅供包内加密协议解析使用，不改变公开 Base64解码 的规则；
	 * 截断输入仍可能抛出索引错误，由对应协议接口转换为原有失败返回值。</p>
	 *
	 * @param text 旧协议保存的 Base64 内容。
	 * @return 已解析的字节数组。
	 */
	static byte[] decodeLegacyBase64(String text) {
		int remainder = text.length() % 4;
		if (remainder == 2) {
			text += "==";
		} else if (remainder == 3) {
			text += "=";
		}

		byte[] data = text.getBytes(Charset.forName("US-ASCII"));
		java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream();
		int position = 0;

		// 首两位跳过不在字母表中的字符，保留原解析器的截断边界。
		while (position < data.length) {
			int first;
			do {
				first = legacyBase64Value(data[position++]);
			} while (position < data.length && first == -1);
			if (first == -1) {
				break;
			}

			int second;
			do {
				second = legacyBase64Value(data[position++]);
			} while (position < data.length && second == -1);
			if (second == -1) {
				break;
			}

			result.write((first << 2) | ((second & 48) >>> 4));

			// 第三、四位遇到补位符即返回，不校验其后是否仍有字符。
			int third;
			do {
				third = data[position++];
				if (third == '=') {
					return result.toByteArray();
				}

				third = legacyBase64Value(third);
			} while (position < data.length && third == -1);
			if (third == -1) {
				break;
			}

			result.write(((second & 15) << 4) | ((third & 60) >>> 2));
			int fourth;
			do {
				fourth = data[position++];
				if (fourth == '=') {
					return result.toByteArray();
				}

				fourth = legacyBase64Value(fourth);
			} while (position < data.length && fourth == -1);
			if (fourth == -1) {
				break;
			}

			result.write(((third & 3) << 6) | fourth);
		}

		return result.toByteArray();
	}

	/**
	 * 返回旧协议标准 Base64 字母表中的数值。
	 *
	 * @param value ASCII 字节。
	 * @return 六位数值；字母表外的字符返回 -1。
	 */
	private static int legacyBase64Value(int value) {
		if (value >= 'A' && value <= 'Z') {
			return value - 'A';
		}
		if (value >= 'a' && value <= 'z') {
			return value - 'a' + 26;
		}
		if (value >= '0' && value <= '9') {
			return value - '0' + 52;
		}

		return value == '+' ? 62 : value == '/' ? 63 : -1;
	}
}

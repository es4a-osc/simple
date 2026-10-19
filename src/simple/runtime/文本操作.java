/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simple.runtime;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.helpers.ConvHelpers;
import simple.runtime.parameters.StringReferenceParameter;
import simple.runtime.variants.Variant;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提供字符代码转换、文本查找、截取、拆分连接、传址修改及格式化。
 *
 * <p>索引从 0 开始，长度按 UTF-16 代码单元计算。传址接口直接修改传入变量；
 * 空格修剪仅处理普通空格，不移除制表符或其它空白字符。</p>
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 文本操作 {

	/**
	 * 静态方法集，不创建实例。
	 */
	private 文本操作() {
	}

	/* 字符与代码 */

	/**
	 * 返回给定字符串第一个字符的unicode值。
	 *
	 * @param str 用于转换第一个字符的字符串
	 * @return {@code str}第一个字符的unicode值
	 */
	@SimpleFunction
	public static int 代码(String str) {
		try {
			return str.charAt(0);
		} catch (IndexOutOfBoundsException e) {
			// 将在运行时转换为Simple的运行时错误
			throw new IllegalArgumentException("String length for Asc() must be 1 or greater");
		}
	}

	/**
	 * 返回给定unicode值的字符串。
	 *
	 * @param value 要转换为字符串的unicode值
	 * @return 由给定的unicode值组成的字符串
	 */
	@SimpleFunction
	public static String 字符(int value) {
		return Character.toString((char)value);
	}

	/* 长度与比较 */

	/**
	 * 返回文本中的 UTF-16 代码单元数，补充字符占两个单元。
	 *
	 * @param text 待计数文本。
	 * @return 文本长度。
	 */
	@SimpleFunction
	public static int 取字符数(String text) {
		return text.length();
	}

	/**
	 * 返回文本按 UTF-8 编码后的字节数。
	 *
	 * @param text 待计数文本。
	 * @return UTF-8 字节数。
	 */
	@SimpleFunction
	public static int 取文本字节数(String text) {
		return 取文本字节数(text, "UTF-8");
	}

	/**
	 * 返回文本按指定字符集编码后的字节数。
	 *
	 * @param text 待计数文本。
	 * @param encoding 字符集名称，例如 UTF-8 或 GBK。
	 * @return 编码后的字节数。
	 */
	@SimpleFunction
	public static int 取文本字节数(String text, String encoding) {
		return 编码转换.文本到字节(text, encoding).length;
	}

	/**
	 * 按 UTF-16 字典顺序比较两个文本。
	 *
	 * @param first 第一个文本。
	 * @param second 第二个文本。
	 * @return 相等为 0，第一个较小为负数，较大为正数。
	 */
	@SimpleFunction
	public static int 文本比较(String first, String second) {
		return first.compareTo(second);
	}

	/* 查找与截取 */

	/**
	 * 从文本开头向后查找字面内容。
	 *
	 * @param text 被查找文本。
	 * @param find 要查找的原文。
	 * @return 首次匹配的索引，未找到返回 -1。
	 */
	@SimpleFunction
	public static int 寻找文本(String text, String find) {
		return 寻找文本(text, find, 0);
	}

	/**
	 * 从指定位置向后查找文本。
	 *
	 * @param text 被查找文本。
	 * @param find 要查找的文本。
	 * @param start 起始索引，允许等于文本长度。
	 * @return 首次匹配的索引，未找到返回 -1。
	 */
	@SimpleFunction
	public static int 寻找文本(String text, String find, int start) {
		checkIndex(start, text.length());
		return text.indexOf(find, start);
	}

	/**
	 * 从文本末尾向前查找字面内容。
	 *
	 * @param text 被查找文本。
	 * @param find 要查找的原文。
	 * @return 最后匹配的索引，未找到返回 -1。
	 */
	@SimpleFunction
	public static int 倒找文本(String text, String find) {
		return 倒找文本(text, find, text.length());
	}

	/**
	 * 从指定位置向前查找文本。
	 *
	 * @param text 被查找文本。
	 * @param find 要查找的文本。
	 * @param start 起始索引，允许等于文本长度。
	 * @return 最后匹配的索引，未找到返回 -1。
	 */
	@SimpleFunction
	public static int 倒找文本(String text, String find, int start) {
		checkIndex(start, text.length());
		return text.lastIndexOf(find, start);
	}

	/**
	 * 从文本开头截取指定数量的 UTF-16 代码单元。
	 *
	 * @param text 原文本。
	 * @param length 数量，不得为负，超出可用长度时截取到末尾。
	 * @return 截取的文本。
	 */
	@SimpleFunction
	public static String 取文本左边(String text, int length) {
		return substring(text, 0, length);
	}

	/**
	 * 从文本末尾截取指定数量的 UTF-16 代码单元。
	 *
	 * @param text 原文本。
	 * @param length 数量，不得为负，超出可用长度时返回全文。
	 * @return 截取的文本。
	 */
	@SimpleFunction
	public static String 取文本右边(String text, int length) {
		checkLength(length);
		return substring(text, Math.max(0, text.length() - length), length);
	}

	/**
	 * 从指定位置截取到文本末尾，索引仍从 0 开始。
	 *
	 * @param text 原文本。
	 * @param start 起始索引，允许等于文本长度。
	 * @return 截取的文本；起点位于末尾时为空文本。
	 */
	@SimpleFunction
	public static String 取文本中间(String text, int start) {
		return 取文本中间(text, start, Integer.MAX_VALUE);
	}

	/**
	 * 从指定位置截取文本。
	 *
	 * @param text 原文本。
	 * @param start 起始索引，允许等于文本长度。
	 * @param length 数量，不得为负，超出剩余长度时截取到末尾。
	 * @return 截取的文本。
	 */
	@SimpleFunction
	public static String 取文本中间(String text, int start, int length) {
		return substring(text, start, length);
	}

	/* 传址修改 */

	/**
	 * 按设备默认区域设置将传入变量转换为小写。
	 *
	 * @param text 待修改的文本变量。
	 */
	@SimpleFunction
	public static void 到小写(StringReferenceParameter text) {
		text.set(text.get().toLowerCase());
	}

	/**
	 * 按设备默认区域设置将传入变量转换为大写。
	 *
	 * @param text 待修改的文本变量。
	 */
	@SimpleFunction
	public static void 到大写(StringReferenceParameter text) {
		text.set(text.get().toUpperCase());
	}

	/**
	 * 删除传入变量首尾的普通空格。
	 *
	 * @param text 待修改的文本变量。
	 */
	@SimpleFunction
	public static void 删首尾空(StringReferenceParameter text) {
		删首空(text);
		删尾空(text);
	}

	/**
	 * 删除传入变量开头的普通空格。
	 *
	 * @param text 待修改的文本变量。
	 */
	@SimpleFunction
	public static void 删首空(StringReferenceParameter text) {
		String value = text.get();
		int start = 0;

		while (start < value.length() && value.charAt(start) == ' ') {
			start++;
		}

		if (start > 0) {
			text.set(value.substring(start));
		}
	}

	/**
	 * 删除传入变量末尾的普通空格，全空格文本变为空文本。
	 *
	 * @param text 待修改的文本变量。
	 */
	@SimpleFunction
	public static void 删尾空(StringReferenceParameter text) {
		String value = text.get();
		int end = value.length();

		while (end > 0 && value.charAt(end - 1) == ' ') {
			end--;
		}

		if (end < value.length()) {
			text.set(value.substring(0, end));
		}
	}

	/**
	 * 从文本开头替换全部不重叠的字面匹配，直接修改传入变量。
	 *
	 * @param text 待修改的文本变量。
	 * @param find 要查找的原文。
	 * @param replacement 替换内容。
	 */
	@SimpleFunction
	public static void 子文本替换(StringReferenceParameter text, String find, String replacement) {
		子文本替换(text, find, replacement, 0, -1);
	}

	/**
	 * 从指定位置替换全部不重叠的字面匹配，直接修改传入变量。
	 *
	 * @param text 待修改的文本变量。
	 * @param find 要查找的原文。
	 * @param replacement 替换内容。
	 * @param start 起始索引，之前的文本保持原样。
	 */
	@SimpleFunction
	public static void 子文本替换(StringReferenceParameter text, String find, String replacement, int start) {
		子文本替换(text, find, replacement, start, -1);
	}

	/**
	 * 从指定位置替换原文本中不重叠的字面匹配，直接修改传入变量。
	 *
	 * <p>查找和替换内容中的正则符号均按原文处理；新写入内容不再次参与匹配。
	 * 查找内容为空时匹配原文本各位置之间的边界。</p>
	 *
	 * @param text 待修改的文本变量。
	 * @param find 要查找的原文。
	 * @param replacement 替换内容。
	 * @param start 起始索引，之前的文本保持原样。
	 * @param count 替换次数，-1 表示全部，0 或其它负数不替换。
	 */
	@SimpleFunction
	public static void 子文本替换(StringReferenceParameter text, String find, String replacement,
		int start, int count) {
		String value = text.get();

		checkIndex(start, value.length());
		if (find == null || replacement == null) {
			throw new NullPointerException();
		}

		if (count != -1 && count <= 0) {
			return;
		}

		// 匹配原文本，并转义替换值，避免反斜杠、美元符号和新增内容影响后续替换。
		Matcher matcher = Pattern.compile(Pattern.quote(find)).matcher(value.substring(start));
		StringBuffer result = new StringBuffer();
		String literal = Matcher.quoteReplacement(replacement);
		int replaced = 0;

		while ((count == -1 || replaced < count) && matcher.find()) {
			matcher.appendReplacement(result, literal);
			replaced++;
		}

		matcher.appendTail(result);
		text.set(value.substring(0, start) + result);
	}

	/**
	 * 将指定区间替换为新文本，直接修改传入变量。
	 *
	 * @param text 待修改的文本变量。
	 * @param start 起始索引，允许等于文本长度。
	 * @param length 原区间长度，不得为负，超出剩余长度时替换到末尾；0 表示插入。
	 * @param replacement 新文本。
	 */
	@SimpleFunction
	public static void 替换文本区间(StringReferenceParameter text, int start, int length, String replacement) {
		String value = text.get();
		checkIndex(start, value.length());
		checkLength(length);
		if (replacement == null) {
			throw new NullPointerException();
		}

		int end = start + Math.min(length, value.length() - start);
		text.set(value.substring(0, start) + replacement + value.substring(end));
	}

	/**
	 * 翻转传入文本，保留有效代理对内部的先后关系。
	 *
	 * @param text 待修改的文本变量。
	 */
	@SimpleFunction
	public static void 翻转文本(StringReferenceParameter text) {
		text.set(new StringBuilder(text.get()).reverse().toString());
	}

	/* 标记提取 */

	/**
	 * 提取首对左右标记之间的文本，标记本身不包含在结果中。
	 *
	 * @param text 原文本。
	 * @param left 左标记，不得为空。
	 * @param right 右标记，不得为空。
	 * @return 首个匹配内容，没有完整标记对时返回空文本。
	 */
	@SimpleFunction
	public static String 取标记间文本(String text, String left, String right) {
		checkMarkers(left, right);
		int start = text.indexOf(left);
		if (start < 0) {
			return "";
		}

		start += left.length();
		int end = text.indexOf(right, start);
		return end < 0 ? "" : text.substring(start, end);
	}

	/**
	 * 按原文顺序提取不重叠标记对之间的全部文本，不处理嵌套标记。
	 *
	 * @param text 原文本。
	 * @param left 左标记，不得为空，按字面内容匹配。
	 * @param right 右标记，不得为空，按字面内容匹配。
	 * @return 匹配内容数组，保留空内容，没有完整标记对时返回空数组。
	 */
	@SimpleFunction
	public static String[] 取全部标记间文本(String text, String left, String right) {
		checkMarkers(left, right);
		ArrayList<String> result = new ArrayList<String>();
		int position = 0;

		while (position < text.length()) {
			int start = text.indexOf(left, position);
			if (start < 0) {
				break;
			}

			start += left.length();
			int end = text.indexOf(right, start);
			if (end < 0) {
				break;
			}

			result.add(text.substring(start, end));
			position = end + right.length();
		}

		return result.toArray(new String[result.size()]);
	}

	/* 文本拆分与连接 */

	/**
	 * 使用字面分隔文本分割全部内容，保留末尾空成员。
	 *
	 * @param text 原文本。
	 * @param separator 字面分隔文本；空文本按 UTF-16 边界分割。
	 * @return 分割结果。
	 */
	@SimpleFunction
	public static String[] 分割文本(String text, String separator) {
		return 分割文本(text, separator, Integer.MAX_VALUE);
	}

	/**
	 * 使用字面分隔文本分割内容，并限制返回成员数。
	 *
	 * @param text 原文本。
	 * @param separator 字面分隔文本，不是正则表达式。
	 * @param count 最大成员数，必须大于 0；达到上限后剩余内容作为最后一项。
	 * @return 分割结果，保留末尾空成员；空分隔文本沿用 Java 的 UTF-16 边界规则。
	 */
	@SimpleFunction
	public static String[] 分割文本(String text, String separator, int count) {
		if (separator == null) {
			throw new NullPointerException();
		}

		if (count <= 0) {
			throw new IllegalArgumentException("分割文本的最大成员数必须大于 0。");
		}

		return text.split(Pattern.quote(separator), count);
	}

	/**
	 * 使用指定分隔文本连接数组成员。
	 *
	 * @param array 文本数组。
	 * @param separator 成员间的分隔文本，不得为空引用，允许空文本。
	 * @return 连接结果，空数组返回空文本；空引用成员沿用 Java 文本转换规则。
	 */
	@SimpleFunction
	public static String 连接数组文本(String[] array, String separator) {
		if (separator == null) {
			throw new NullPointerException();
		}

		StringBuilder result = new StringBuilder();

		for (int index = 0; index < array.length; index++) {
			if (index > 0) {
				result.append(separator);
			}

			result.append(array[index]);
		}

		return result.toString();
	}

	/* 文本格式化 */

	/**
	 * 使用 Java 格式说明符和参数生成文本，区域设置沿用设备默认值。
	 *
	 * @param format 格式文本。
	 * @param args 参数或参数数组。
	 * @return 格式化结果。
	 */
	@SimpleFunction
	public static String 格式化文本(String format, Variant args) {
		return String.format(format, ConvHelpers.variant2objects(args));
	}

	/**
	 * 按 DecimalFormat 格式将双精度数值转换为显示文本。
	 *
	 * <p>格式中的 0 表示必填数字，# 表示可选数字，逗号启用分组，
	 * 小数点指定小数位；也支持百分比、科学计数和正负数子格式。
	 * 格式语法固定，输出的数字、分隔符等符号随设备默认格式区域设置变化。</p>
	 *
	 * <p>沿用 HALF_EVEN 舍入，恰好位于中点时向偶数取舍，例如格式 0 下
	 * 2.5 得到 2、3.5 得到 4。输入保留 double 的二进制浮点误差，
	 * 不保证十进制金额精度；精确金额文本应使用{@link #格式化金额(String)}。
	 * 负零、NaN 和无穷大按 DecimalFormat 的规则及区域符号显示。</p>
	 *
	 * @param pattern 格式文本，例如 0000、#,##0.00、0% 或 0.00E0。
	 * @param value 待格式化的双精度数值。
	 * @return 按给定格式和默认格式区域设置生成的文本。
	 * @throws IllegalArgumentException 格式文本不符合 DecimalFormat 语法。
	 * @throws NullPointerException 格式文本为空引用。
	 */
	@SimpleFunction
	public static String 格式化数值(String pattern, double value) {
		return new DecimalFormat(pattern).format(value);
	}

	/**
	 * 将十进制金额文本按 HALF_UP 舍入为两位小数，不经过二进制浮点转换。
	 *
	 * <p>输出固定使用小数点，不添加千分位或货币符号；支持正负号和科学计数，
	 * 不自动去除首尾空白。恰好位于中间值时向远离零的方向舍入，
	 * 例如 1.005 得到 1.01，-1.005 得到 -1.01；结果为零时不保留负号。</p>
	 *
	 * @param amount 金额文本，例如 12.3。
	 * @return 固定保留两位小数的普通十进制文本，例如 12.30。
	 * @throws NumberFormatException 金额不是有效十进制文本，不接受分组分隔符、货币符号或首尾空白。
	 * @throws NullPointerException 金额为空引用。
	 * @throws ArithmeticException 数值规模超出可处理范围。
	 */
	@SimpleFunction
	public static String 格式化金额(String amount) {
		return new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP).toPlainString();
	}

	/**
	 * 将科学计数或普通十进制文本展开为不带指数的普通十进制文本。
	 *
	 * @param value 数值文本，例如 1.23E+5 或 1E-3。
	 * @return 不使用科学计数的文本，分别如 123000 和 0.001；不主动删除小数末尾零。
	 * @throws NumberFormatException 文本不是有效十进制数。
	 */
	@SimpleFunction
	public static String 科学转普通计数(String value) {
		return new BigDecimal(value).toPlainString();
	}

	/* 内部边界检查 */

	/**
	 * 检查起始位置，允许使用文本末尾的位置。
	 *
	 * @param index 待检查索引。
	 * @param maximum 文本长度。
	 */
	private static void checkIndex(int index, int maximum) {
		if (index < 0 || index > maximum) {
			throw new IllegalArgumentException("文本索引超出范围：" + index);
		}
	}

	/**
	 * 检查非负长度。
	 *
	 * @param length 待检查长度。
	 */
	private static void checkLength(int length) {
		if (length < 0) {
			throw new IllegalArgumentException("文本长度不得为负数。");
		}
	}

	/**
	 * 按剩余长度截取文本，避免起始位置与长度相加溢出。
	 *
	 * @param text 原文本。
	 * @param start 起始位置。
	 * @param length 截取长度。
	 * @return 截取结果。
	 */
	private static String substring(String text, int start, int length) {
		checkIndex(start, text.length());
		checkLength(length);
		return text.substring(start, start + Math.min(length, text.length() - start));
	}

	/**
	 * 检查字面标记，空标记会使匹配规则不明确。
	 *
	 * @param left 左标记。
	 * @param right 右标记。
	 */
	private static void checkMarkers(String left, String right) {
		if (left == null || right == null || left.length() == 0 || right.length() == 0) {
			throw new IllegalArgumentException("左右标记不能为空。");
		}
	}
}

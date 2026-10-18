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

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提供文本查找、截取、传址修改及标记间内容提取。
 *
 * <p>索引从 0 开始，长度按 UTF-16 代码单元计算。传址接口直接修改传入变量；
 * 空格修剪仅处理普通空格，不移除制表符或其它空白字符。</p>
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 文本操作 {

	private 文本操作() {
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

	/* 标记提取与格式化 */

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

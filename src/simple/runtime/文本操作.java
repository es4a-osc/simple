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

/**
 * 实现各种与字符串相关的运行时函数。
 *
 * @author Herbert Czymontek
 */
@SimpleObject
public final class 文本操作 {

	private 文本操作() {
	}

	/*
	 * 检验索引；验证字符串索引参数是否在字符串边界内。
	 */
	private static void checkIndex(String function, int index, int max) {
		if (index < 0 || index > max) {
			throw new IllegalArgumentException("Index for " + function + "() out of range. " +
					"Should be between 0 and " + max + ", but is " + index);
		}
	}

	/*
	 * 检验长度；验证字符串长度参数是否为正数。
	 */
	private static void checkLength(String function, int len) {
		if (len < 0) {
			throw new IllegalArgumentException("Length for " + function + "() out of range. " +
					"Should be greater than 0, but is " + len);
		}
	}

	/*
	 * 取子字符串；提取子字符串的辅助方法。
	 */
	private static String mid(String function, String str, int start, int len) {
		checkIndex(function, start, str.length());
		checkLength(function, len);

		int end = start + len;
		if (end > str.length()) {
			end = str.length();
		}

		return str.substring(start, end);
	}

	/**
	 * 从给定文本寻找指定文本。
	 *
	 * @param str1  被寻找的文本
	 * @param str2  要寻找的文本
	 * @param start  寻找在开始索引{@code str1}
	 * @return  在{@code str1}中找到{@code str2}的索引，
	 *          如果在{@code str1}中找不到{@code str2}则为负值
	 */
	@SimpleFunction
	public static int 寻找文本(String str1, String str2, int start) {
		checkIndex("InStr", start, str1.length());

		return str1.indexOf(str2, start);
	}

	/**
	 * 从给定文本末尾倒序寻找指定文本。
	 *
	 * @param str1  被寻找的文本
	 * @param str2  要寻找的文本
	 * @param start  在{@code str1}中寻找开始索引
	 * @return  在{@code str1}中找到{@code str2}的索引，
	 *          如果在{@code str1}中找不到{@code str2}则为负值
	 */
	@SimpleFunction
	public static int 倒找文本(String str1, String str2, int start) {
		checkIndex("InStrRev", start, str1.length());

		return str1.lastIndexOf(str2, start);
	}

	/**
	 * 将给定文本全部转换为小写。
	 *
	 * @param str  要转换为小写的文本
	 */
	@SimpleFunction
	public static void 到小写(StringReferenceParameter str) {
		str.set(str.get().toLowerCase());
	}

	/**
	 * 将给定的文本全部转换为大写。
	 *
	 * @param str  要转换为大写文本
	 */
	@SimpleFunction
	public static void 到大写(StringReferenceParameter str) {
		str.set(str.get().toUpperCase());
	}

	/**
	 * 从给定文本的开头返回指定数的子文本。
	 *
	 * @param str  从中返回子文本的给定文本
	 * @param len  要返回的文本数
	 * @return  给定文本的子文本
	 */
	@SimpleFunction
	public static String 取文本左边(String str, int len) {
		return mid("Left", str, 0, len);
	}

	/**
	 * 从给定文本的末尾返回指定数目的子文本。
	 *
	 * @param str  从中返回子文本的给定文本
	 * @param len  要返回的文本数
	 * @return  给定文本的子文本
	 */
	@SimpleFunction
	public static String 取文本右边(String str, int len) {
		int start = str.length() - len;
		return mid("Right", str, start < 0 ? 0 : start, len);
	}

	/**
	 * 从给定索引开始，返回给定文本中的指定文本数。
	 *
	 * @param str  返回子文本的给定文本
	 * @param start  在{@code str}中开始索引
	 * @param len  要返回的字符数
	 * @return  给定文本的子文本
	 */
	@SimpleFunction
	public static String 取文本中间(String str, int start, int len) {
		return mid("Mid", str, start, len);
	}

	/**
	 * 返回给定文本中的字符数。
	 *
	 * @param str  获得长度的文本
	 * @return  给定文本的字符数
	 */
	@SimpleFunction
	public static int 取字符数(String str) {
		return str.length();
	}

	/**
	 * 从给定文本中删除开头和末尾的空格。
	 *
	 * @param str  要修剪的文本
	 */
	@SimpleFunction
	public static void 删首尾空(StringReferenceParameter str) {
		删首空(str);
		删尾空(str);
	}

	/**
	 * 从给定文本中删除开头的空格。
	 *
	 * @param str  要修剪的文本
	 */
	@SimpleFunction
	public static void 删首空(StringReferenceParameter str) {
		char[] chars = str.get().toCharArray();
		int count = 0;
		for (int i = 0; i < chars.length && chars[i] == ' '; i++, count++) {
		}

		if (count > 0) {
			str.set(new String(chars, count, chars.length - count));
		}
	}

	/**
	 * 从给定文本中删除末尾的空格。
	 *
	 * @param str  要修剪的文本
	 */
	@SimpleFunction
	public static void 删尾空(StringReferenceParameter str) {
		char[] chars = str.get().toCharArray();
		int count = 0;
		for (int i = chars.length - 1; i > 0 && chars[i] == ' '; i--, count++) {
		}

		if (count > 0) {
			str.set(new String(chars, 0, chars.length - count));
		}
	}

	/**
	 * 将给定文本中指定子文本替换为指定文本。
	 *
	 * @param str  要修改的文本
	 * @param find  要查找的文本
	 * @param replace  子文本替换的文本
	 * @param start  {@code str}中的起始索引
	 * @param count  执行替换的次数（-1 替换所有）
	 */
	@SimpleFunction
	public static void 子文本替换(StringReferenceParameter str, String find, String replace,
			int start, int count) {
		String s = str.get();

		checkIndex("Replace", start, s.length());
		if (find == null || replace == null) {
			// None of the following code minds find or replace being null, but we do
			throw new NullPointerException();
		}

		// Escape reg expr characters
		find = "\\Q" + find + "\\E";

		String s1 = s.substring(0, start);
		String s2 = s.substring(start);

		if (count == -1) {
			s2 = s2.replaceAll(find, replace);
		} else {
			while (--count >= 0) {
				s2 = s2.replaceFirst(find, replace);
			}
		}

		str.set(s1 + s2);
	}

	/**
	 * 按字典顺序比较两个给定的文本。
	 *
	 * @param str1  第一个比较文本
	 * @param str2  第二个比较文本
	 * @return  如果文本相等则为0；
	 *          如果{@code str2}在{@code str1}之后为负数；
	 *          如果{@code str1}在{@code str2}之后为正数。
	 */
	@SimpleFunction
	public static int 文本比较(String str1, String str2) {
		return str1.compareTo(str2);
	}

	/**
	 * 反转给定的文本。
	 *
	 * @param str  要反转的文本
	 */
	@SimpleFunction
	public static void 翻转文本(StringReferenceParameter str) {
		str.set(new StringBuffer(str.get()).reverse().toString());
	}

	/* 扩展 */

	/**
	 * 使用指定的格式文本和参数返回一个格式化文本。
	 *
	 * <p>详细信息请参考有关{@code String.format(String, Object...)}文档。
	 *
	 * @param format 格式文本
	 * @param args 格式文本中由格式说明符引用的参数，可以是数组
	 * @return 一个格式化文本
	 */
	@SimpleFunction
	public static String 格式化文本(String format, Variant args) {
		/*
		JAVA字符串格式化-String.format()的使用：
		https://www.cnblogs.com/Dhouse/p/7776780.html
		*/
		return String.format(format, ConvHelpers.variant2objects(args));
	}
}

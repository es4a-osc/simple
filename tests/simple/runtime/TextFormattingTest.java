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

import java.text.DecimalFormatSymbols;
import java.util.Locale;

import junit.framework.TestCase;

/**
 * 核对文本操作中数值与金额格式化的区域、舍入、精度及异常约定。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class TextFormattingTest extends TestCase {

	/**
	 * 验证补零、可选小数位、分组、百分比、科学计数和正负子格式。
	 */
	public void testFormatNumberPatterns() {
		Locale original = Locale.getDefault(Locale.Category.FORMAT);
		try {
			Locale.setDefault(Locale.Category.FORMAT, Locale.US);
			assertEquals("0042", 文本操作.格式化数值("0000", 42));
			assertEquals("1,234.50", 文本操作.格式化数值("#,##0.00", 1234.5));
			assertEquals("12.3", 文本操作.格式化数值("0.##", 12.3));
			assertEquals("13%", 文本操作.格式化数值("0%", 0.1251));
			assertEquals("12.5‰", 文本操作.格式化数值("0.0‰", 0.0125));
			assertEquals("1.23E4", 文本操作.格式化数值("0.00E0", 12300));
			assertEquals("(12.50)", 文本操作.格式化数值("0.00;(0.00)", -12.5));
			assertEquals("编号0042", 文本操作.格式化数值("'编号'0000", 42));
		} finally {
			Locale.setDefault(Locale.Category.FORMAT, original);
		}
	}

	/**
	 * 使用可精确表示的中点验证 HALF_EVEN，不与金额的 HALF_UP 混淆。
	 */
	public void testFormatNumberRounding() {
		Locale original = Locale.getDefault(Locale.Category.FORMAT);
		try {
			Locale.setDefault(Locale.Category.FORMAT, Locale.US);
			assertEquals("2", 文本操作.格式化数值("0", 2.5));
			assertEquals("4", 文本操作.格式化数值("0", 3.5));
			assertEquals("-2", 文本操作.格式化数值("0", -2.5));
			assertEquals("-4", 文本操作.格式化数值("0", -3.5));
			assertEquals("1.12", 文本操作.格式化数值("0.00", 1.125));
			assertEquals("1.38", 文本操作.格式化数值("0.00", 1.375));
			assertEquals("-0.00", 文本操作.格式化数值("0.00", -0.0));
		} finally {
			Locale.setDefault(Locale.Category.FORMAT, original);
		}
	}

	/**
	 * 验证格式语法不变，输出分隔符随默认格式区域变化且不被上次调用缓存。
	 */
	public void testFormatNumberLocale() {
		Locale original = Locale.getDefault(Locale.Category.FORMAT);
		try {
			Locale.setDefault(Locale.Category.FORMAT, Locale.US);
			assertEquals("1,234.50", 文本操作.格式化数值("#,##0.00", 1234.5));
			Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY);
			assertEquals("1.234,50", 文本操作.格式化数值("#,##0.00", 1234.5));
			assertEquals("1234.50", 文本操作.格式化金额("1234.5"));
		} finally {
			Locale.setDefault(Locale.Category.FORMAT, original);
		}
	}

	/**
	 * 验证 double 已丢失的整数精度不会被格式化恢复。
	 */
	public void testFormatNumberPrecision() {
		Locale original = Locale.getDefault(Locale.Category.FORMAT);
		try {
			Locale.setDefault(Locale.Category.FORMAT, Locale.US);
			assertEquals("9007199254740992", 文本操作.格式化数值("0", (double) 9007199254740993L));
			assertEquals("9007199254740993.00", 文本操作.格式化金额("9007199254740993"));
		} finally {
			Locale.setDefault(Locale.Category.FORMAT, original);
		}
	}

	/**
	 * 验证非有限数值沿用格式化器的符号显示，不额外限制输入。
	 */
	public void testFormatNumberSpecialValues() {
		Locale original = Locale.getDefault(Locale.Category.FORMAT);
		try {
			Locale.setDefault(Locale.Category.FORMAT, Locale.US);
			// NaN 的文字因平台符号数据而异，按当前区域的实际符号校验。
			DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
			assertEquals(symbols.getNaN(), 文本操作.格式化数值("0.00", Double.NaN));
			assertEquals(symbols.getInfinity(), 文本操作.格式化数值("0.00", Double.POSITIVE_INFINITY));
			assertEquals(symbols.getMinusSign() + symbols.getInfinity(),
					文本操作.格式化数值("0.00", Double.NEGATIVE_INFINITY));
		} finally {
			Locale.setDefault(Locale.Category.FORMAT, original);
		}
	}

	/**
	 * 验证非法格式和空引用保留 Java 异常，交由 Simple 统一转换。
	 */
	public void testFormatNumberInvalidPattern() {
		for (String pattern : new String[] {"0.0.0", "0E", "'未闭合", "#0#", "0%%"}) {
			try {
				文本操作.格式化数值(pattern, 1);
				fail("非法格式应被拒绝：" + pattern);
			} catch (IllegalArgumentException expected) {
			}
		}

		try {
			文本操作.格式化数值(null, 1);
			fail("空引用应被拒绝");
		} catch (NullPointerException expected) {
		}
	}

	/**
	 * 验证固定小数位和十进制中点舍入，避免先转为 double 造成误差。
	 */
	public void testFormatAmountRounding() {
		assertEquals("12.00", 文本操作.格式化金额("12"));
		assertEquals("12.30", 文本操作.格式化金额("12.3"));
		assertEquals("12.34", 文本操作.格式化金额("12.344"));
		assertEquals("12.35", 文本操作.格式化金额("12.345"));
		assertEquals("1.01", 文本操作.格式化金额("1.005"));
		assertEquals("2.68", 文本操作.格式化金额("2.675"));
		assertEquals("13.00", 文本操作.格式化金额("12.999"));
	}

	/**
	 * 验证负数的中点舍入和零的规范表示。
	 */
	public void testFormatAmountSignAndZero() {
		assertEquals("-1.01", 文本操作.格式化金额("-1.005"));
		assertEquals("-0.01", 文本操作.格式化金额("-0.005"));
		assertEquals("0.00", 文本操作.格式化金额("-0.004"));
		assertEquals("0.00", 文本操作.格式化金额("-0.00"));
		assertEquals("0.00", 文本操作.格式化金额("0"));
		assertEquals("12.30", 文本操作.格式化金额("+12.3"));
	}

	/**
	 * 验证超过 double 精确整数范围的金额、科学计数和前导零。
	 */
	public void testFormatAmountPrecision() {
		assertEquals("9007199254740993.01", 文本操作.格式化金额("9007199254740993.005"));
		assertEquals("123456789012345678901234567890.12",
				文本操作.格式化金额("123456789012345678901234567890.12"));
		assertEquals("1000.00", 文本操作.格式化金额("1E3"));
		assertEquals("0.00", 文本操作.格式化金额("1E-3"));
		assertEquals("1.20", 文本操作.格式化金额("0001.20"));
	}

	/**
	 * 验证输出不随设备区域设置改变，并恢复测试前的默认区域。
	 */
	public void testFormatAmountLocale() {
		Locale original = Locale.getDefault();
		try {
			Locale.setDefault(Locale.GERMANY);
			assertEquals("1234.50", 文本操作.格式化金额("1234.5"));
		} finally {
			Locale.setDefault(original);
		}
	}

	/**
	 * 验证非法输入不会伪装成零，空引用仍按既有 Java 错误转换约定处理。
	 */
	public void testFormatAmountInvalidInput() {
		for (String invalid : new String[] {"", " ", " 1", "1 ", "NaN", "Infinity", "1,23", "1,234.56", "￥12", "abc"}) {
			try {
				文本操作.格式化金额(invalid);
				fail("非法金额应被拒绝：" + invalid);
			} catch (NumberFormatException expected) {
			}
		}

		try {
			文本操作.格式化金额(null);
			fail("空引用应被拒绝");
		} catch (NullPointerException expected) {
		}
	}

	/**
	 * 非法文本不被修剪或当成零。
	 */
	public void testInvalidNumberText() {
		for (String value : new String[] {"", " 1", "1 ", "1,000", "NaN", "Infinity", "abc"}) {
			try {
				文本操作.科学转普通计数(value);
				fail("应拒绝数值文本：" + value);
			} catch (NumberFormatException expected) {
				// 不把解析失败变成成功结果。
			}
		}
	}

}

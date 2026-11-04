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

import java.util.Arrays;

import simple.runtime.parameters.StringReferenceParameter;
import simple.runtime.variants.ArrayVariant;
import simple.runtime.variants.BooleanVariant;
import simple.runtime.variants.DoubleVariant;
import simple.runtime.variants.IntegerVariant;
import simple.runtime.variants.StringVariant;
import simple.runtime.variants.Variant;

import java.util.List;
import java.util.Locale;

import junit.framework.TestCase;

/**
 * Tests for {@link 文本操作}.
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public class StringsTest extends TestCase {

	/**
	 * 创建指定名称的运行库用例。
	 *
	 * @param testName 用例名称
	 */
	public StringsTest(String testName) {
		super(testName);
	}

	/**
	 * Tests {@link 文本操作#寻找文本(String, String, int)}.
	 */
	public void testInStr() {
		// Check that if the first string argument is null that a NullPointerException will be thrown.
		// Note that if the function is called from Simple code that the compiler will generate
		// exception handlers to convert NullPointerExceptions into the equivalent runtime error
		try {
			文本操作.寻找文本(null, "", 0);
			fail();
		} catch (NullPointerException expected) {
		}

		// Check that if the second string argument is null that a NullPointerException will be thrown
		try {
			文本操作.寻找文本("", null, 0);
			fail();
		} catch (NullPointerException expected) {
		}

		// Check that if the start index argument is out of range that an IllegalArgumentException
		// will be thrown (Simple runtime error equivalent of IllegalArgumentError)
		try {
			文本操作.寻找文本("", "", -1);
			fail();
		} catch (IllegalArgumentException expected) {
		}
		try {
			文本操作.寻找文本("", "", 1);
			fail();
		} catch (IllegalArgumentException expected) {
		}

		// Empty string is always found at the start index position
		assertEquals(0, 文本操作.寻找文本("", "", 0));
		assertEquals(0, 文本操作.寻找文本("foo", "", 0));
		assertEquals(1, 文本操作.寻找文本("foo", "", 1));

		// Check that a negative value will be returned if the second string cannot be found within the
		// first
		assertTrue(文本操作.寻找文本("foo", "bar", 0) < 0);
		assertTrue(文本操作.寻找文本("foobar", "bar", 5) < 0);

		// Check that the first occurrence of the search string is found if the search starting index
		// is less than start index of the first occurrence
		assertEquals(0, 文本操作.寻找文本("foofoo", "foo", 0));

		// Check that the second occurrence of the search string is found if the search starting index
		// is greater than start index of the first occurrence
		assertEquals(3, 文本操作.寻找文本("foofoo", "foo", 1));
	}

	/**
	 * Tests {@link 文本操作#倒找文本(String, String, int)}.
	 */
	public void testInStrRev() {
		// Check that if the first string argument is null that a NullPointerException will be thrown
		try {
			文本操作.倒找文本(null, "", 0);
			fail();
		} catch (NullPointerException expected) {
		}

		// Check that if the second string argument is null that a NullPointerException will be thrown
		try {
			文本操作.倒找文本("", null, 0);
			fail();
		} catch (NullPointerException expected) {
		}

		// Check that if the start index argument is out of range that an IllegalArgumentException
		// will be thrown
		try {
			文本操作.倒找文本("", "", -1);
			fail();
		} catch (IllegalArgumentException expected) {
		}

		try {
			文本操作.倒找文本("", "", 1);
			fail();
		} catch (IllegalArgumentException expected) {
		}

		// Empty string is always found at the start index position
		assertEquals(0, 文本操作.倒找文本("", "", 0));
		assertEquals(0, 文本操作.倒找文本("foo", "", 0));
		assertEquals(1, 文本操作.倒找文本("foo", "", 1));

		// Check that a negative value will be returned if the second string cannot be found within the
		// first
		assertTrue(文本操作.倒找文本("foo", "bar", 0) < 0);
		assertTrue(文本操作.倒找文本("foobar", "bar", 2) < 0);

		// Check that the first occurrence of the search string is found if the search starting index
		// is less than start index of the first occurrence
		assertEquals(3, 文本操作.倒找文本("foofoo", "foo", 3));

		// Check that the second occurrence of the search string is found if the search starting index
		// is greater than start index of the first occurrence
		assertEquals(0, 文本操作.倒找文本("foofoo", "foo", 2));
	}

	/**
	 * Tests {@link 文本操作#到小写(StringReferenceParameter)}.
	 */
	public void testLCase() {
		StringReferenceParameter strRef = new StringReferenceParameter(null);
		// Check that if the string argument is null that a NullPointerException will be thrown
		try {
			文本操作.到小写(strRef);
			fail();
		} catch (NullPointerException expected) {
		}

		// Empty string
		strRef.set("");
		文本操作.到小写(strRef);
		assertEquals("", strRef.get());

		// All UPPER should become all lower
		strRef.set("FOO");
		文本操作.到小写(strRef);
		assertEquals("foo", strRef.get());
	}

	/**
	 * Tests {@link 文本操作#到大写(StringReferenceParameter)}.
	 */
	public void testUCase() {
		StringReferenceParameter strRef = new StringReferenceParameter(null);
		// Check that if the string argument is null that a NullPointerException will be thrown
		try {
			文本操作.到大写(strRef);
			fail();
		} catch (NullPointerException expected) {
		}

		// Empty string
		strRef.set("");
		文本操作.到大写(strRef);
		assertEquals("", strRef.get());

		// All lower should become all UPPER
		strRef.set("foo");
		文本操作.到大写(strRef);
		assertEquals("FOO", strRef.get());
	}

	/**
	 * Tests {@link 文本操作#取文本左边(String, int)}.
	 */
	public void testLeft() {
		// Check that if the string argument is null that a NullPointerException will be thrown
		try {
			文本操作.取文本左边(null, 0);
			fail();
		} catch (NullPointerException expected) {
		}

		// Length argument out of range (negative or greater than string length)
		try {
			文本操作.取文本左边("foo", -2);
			fail();
		} catch (IllegalArgumentException expected) {
		}

		// Left of empty string
		assertEquals("", 文本操作.取文本左边("", 0));
		assertEquals("", 文本操作.取文本左边("foo", 0));

		// Left of string
		assertEquals("fo", 文本操作.取文本左边("foo", 2));
		assertEquals("foo", 文本操作.取文本左边("foo", 20));
	}

	/**
	 * Tests {@link 文本操作#取文本右边(String, int)}.
	 */
	public void testRight() {
		// Check that if the string argument is null that a NullPointerException will be thrown
		try {
			文本操作.取文本右边(null, 0);
			fail();
		} catch (NullPointerException expected) {
		}

		// Length argument out of range (negative or greater than string length)
		try {
			文本操作.取文本右边("foo", -2);
			fail();
		} catch (IllegalArgumentException expected) {
		}

		// Right of empty string
		assertEquals("", 文本操作.取文本右边("", 0));
		assertEquals("", 文本操作.取文本右边("foo", 0));

		// Right of string
		assertEquals("oo", 文本操作.取文本右边("foo", 2));
		assertEquals("foo", 文本操作.取文本右边("foo", 20));
	}

	/**
	 * Tests {@link 文本操作#取文本中间(String, int, int)}.
	 */
	public void testMid() {
		// Check that if the string argument is null that a NullPointerException will be thrown
		try {
			文本操作.取文本中间(null, 0, 0);
			fail();
		} catch (NullPointerException expected) {
		}

		// Check that if the start index argument is out of range that an IllegalArgumentException
		// will be thrown
		try {
			文本操作.取文本中间("", -1, 0);
			fail();
		} catch (IllegalArgumentException expected) {
		}
		try {
			文本操作.取文本中间("", 1, 0);
			fail();
		} catch (IllegalArgumentException expected) {
		}

		// Length argument out of range (negative or greater than string length)
		try {
			文本操作.取文本中间("foo", 0, -2);
			fail();
		} catch (IllegalArgumentException expected) {
		}

		// Mid of empty string
		assertEquals("", 文本操作.取文本中间("", 0, 0));
		assertEquals("", 文本操作.取文本中间("", 0, 10));

		// Mid of string
		assertEquals("", 文本操作.取文本中间("foo", 1, 0));
		assertEquals("oo", 文本操作.取文本中间("foo", 1, 2));
		assertEquals("oo", 文本操作.取文本中间("foo", 1, 20));
	}

	/**
	 * Tests {@link 文本操作#取字符数(String)}.
	 */
	public void testLen() {
		// Check that if the string argument is null that a NullPointerException will be thrown
		try {
			文本操作.取字符数(null);
			fail();
		} catch (NullPointerException expected) {
		}

		// Length of strings
		assertEquals(0, 文本操作.取字符数(""));
		assertEquals(3, 文本操作.取字符数("foo"));
	}

	/**
	 * Tests {@link 文本操作#删尾空(StringReferenceParameter)}.
	 */
	public void testRTrim() {
		StringReferenceParameter strRef = new StringReferenceParameter(null);

		// Check that if the string argument is null that a NullPointerException will be thrown
		try {
			文本操作.删尾空(strRef);
			fail();
		} catch (NullPointerException expected) {
		}

		// Trim spaces of strings
		strRef.set("");
		文本操作.删尾空(strRef);
		assertEquals("", strRef.get());

		strRef.set("  foo bar  ");
		文本操作.删尾空(strRef);
		assertEquals("  foo bar", strRef.get());
	}

	/**
	 * Tests {@link 文本操作#删首空(StringReferenceParameter)}.
	 */
	public void testLTrim() {
		StringReferenceParameter strRef = new StringReferenceParameter(null);

		// Check that if the string argument is null that a NullPointerException will be thrown
		try {
			文本操作.删首空(strRef);
			fail();
		} catch (NullPointerException expected) {
		}

		// Trim spaces of strings
		strRef.set("");
		文本操作.删首空(strRef);
		assertEquals("", strRef.get());

		strRef.set("  foo bar  ");
		文本操作.删首空(strRef);
		assertEquals("foo bar  ", strRef.get());
	}

	/**
	 * Tests {@link 文本操作#删首尾空(StringReferenceParameter)}.
	 */
	public void testTrim() {
		StringReferenceParameter strRef = new StringReferenceParameter(null);

		// Check that if the string argument is null that a NullPointerException will be thrown
		try {
			文本操作.删首尾空(strRef);
			fail();
		} catch (NullPointerException expected) {
		}

		// Trim spaces of strings
		strRef.set("");
		文本操作.删首尾空(strRef);
		assertEquals("", strRef.get());

		strRef.set("  foo bar  ");
		文本操作.删首尾空(strRef);
		assertEquals("foo bar", strRef.get());
	}

	/**
	 * Tests {@link 文本操作#子文本替换(StringReferenceParameter, String, String, int, int)}.
	 */
	public void testReplace() {
		StringReferenceParameter strRef = new StringReferenceParameter(null);

		// Check that if any of the string arguments are null that a NullPointerException will be
		// thrown
		try {
			文本操作.子文本替换(strRef, "", "", 0, 0);
			fail();
		} catch (NullPointerException expected) {
		}

		strRef.set("");
		try {
			文本操作.子文本替换(strRef, null, "", 0, 0);
			fail();
		} catch (NullPointerException expected) {
		}

		try {
			文本操作.子文本替换(strRef, "", null, 0, 0);
			fail();
		} catch (NullPointerException expected) {
		}

		// Check that if the start index argument is out of range that an IllegalArgumentException
		// will be thrown
		try {
			文本操作.子文本替换(strRef, "", "", -1, 0);
			fail();
		} catch (IllegalArgumentException expected) {
		}
		try {
			文本操作.子文本替换(strRef, "", "", 1, 0);
			fail();
		} catch (IllegalArgumentException expected) {
		}

		// Some fun with empty strings
		strRef.set("");
		文本操作.子文本替换(strRef, "", "", 0, -1);
		assertEquals("", strRef.get());

		strRef.set("");
		文本操作.子文本替换(strRef, "", "", 0, 0);
		assertEquals("", strRef.get());

		strRef.set("");
		文本操作.子文本替换(strRef, "", "", 0, 10);
		assertEquals("", strRef.get());

		strRef.set("foo");
		文本操作.子文本替换(strRef, "", "", 0, -1);
		assertEquals("foo", strRef.get());

		strRef.set("foo");
		文本操作.子文本替换(strRef, "", "", 0, 1);
		assertEquals("foo", strRef.get());

		strRef.set("foo");
		文本操作.子文本替换(strRef, "", "", 1, 1);
		assertEquals("foo", strRef.get());

		strRef.set("foo");
		文本操作.子文本替换(strRef, "", "f", 2, 2);
		// 空查找文本匹配原有边界，新写入的 f 不应再次参与匹配。
		assertEquals("fofof", strRef.get());

		// Make sure reg expr characters in the find expression are ignored
		strRef.set("f.o.o");
		文本操作.子文本替换(strRef, ".", "!", 2, 1);
		assertEquals("f.o!o", strRef.get());

		// Enough special cases, now some easy replacements
		strRef.set("foo");
		文本操作.子文本替换(strRef, "o", "i", 0, -1);
		assertEquals("fii", strRef.get());

		strRef.set("foo");
		文本操作.子文本替换(strRef, "o", "i", 0, 0);
		assertEquals("foo", strRef.get());

		strRef.set("foo");
		文本操作.子文本替换(strRef, "o", "i", 0, 1);
		assertEquals("fio", strRef.get());

		strRef.set("foo");
		文本操作.子文本替换(strRef, "o", "i", 0, 10);
		assertEquals("fii", strRef.get());

		strRef.set("foo");
		文本操作.子文本替换(strRef, "o", "i", 2, -1);
		assertEquals("foi", strRef.get());

		strRef.set("foo");
		文本操作.子文本替换(strRef, "o", "i", 2, 0);
		assertEquals("foo", strRef.get());

		strRef.set("foo");
		文本操作.子文本替换(strRef, "o", "i", 2, 1);
		assertEquals("foi", strRef.get());

		strRef.set("foo");
		文本操作.子文本替换(strRef, "o", "i", 2, 10);
		assertEquals("foi", strRef.get());
	}

	/**
	 * Tests {@link 文本操作#文本比较(String, String)}.
	 */
	public void testStrComp() {
		// Check that if either of the string arguments is null that a NullPointerException will be
		// thrown
		try {
			文本操作.文本比较(null, "");
			fail();
		} catch (NullPointerException expected) {
		}
		try {
			文本操作.文本比较("", null);
			fail();
		} catch (NullPointerException expected) {
		}

		// Empty strings
		assertTrue(文本操作.文本比较("", "") == 0);
		assertTrue(文本操作.文本比较("foo", "") > 0);
		assertTrue(文本操作.文本比较("", "foo") < 0);

		// Other strings
		assertTrue(文本操作.文本比较("foo", "foo") == 0);
		assertTrue(文本操作.文本比较("fooo", "foo") > 0);
		assertTrue(文本操作.文本比较("foo", "bar") > 0);
		assertTrue(文本操作.文本比较("foo", "fooo") < 0);
		assertTrue(文本操作.文本比较("bar", "foo") < 0);
	}

	/**
	 * Tests {@link 文本操作#翻转文本(StringReferenceParameter)}.
	 */
	public void testStrReverse() {
		// Check that if the string argument is null that a NullPointerException will be thrown
		StringReferenceParameter strRef = new StringReferenceParameter(null);
		try {
			文本操作.翻转文本(strRef);
			fail();
		} catch (NullPointerException expected) {
		}

		// Trim spaces of strings
		strRef.set("");
		文本操作.翻转文本(strRef);
		assertEquals("", strRef.get());

		strRef.set("foo");
		文本操作.翻转文本(strRef);
		assertEquals("oof", strRef.get());
	}

	/**
	 * Tests {@link 文本操作#格式化文本(String, Variant)}。
	 */
	public void testStrFormat() {
		// 当输入的字符串为 null 检查抛出 NullPointerException
		try {
			文本操作.格式化文本(null, null);
			fail();
		} catch (NullPointerException expected) {
		}

		// 参数是空是，不继续格式化
		//assertEquals(文本操作.格式化文本("文本型=%s", null), String.format("文本型=%s", null));

		// 多个任意类型参数
		Object [] objects = new Object []{true, 123456, 123.456, "文本"};
		Variant arrayVariant = ArrayVariant.getArrayVariant(objects);
		assertEquals(文本操作.格式化文本("逻辑型=%b 整数型=%d 小数型=%f 文本型=%s", arrayVariant),
				String.format(Locale.CHINA, "逻辑型=%b 整数型=%d 小数型=%f 文本型=%s", objects));

		// 单个任意类型参数
		Variant booleanVariant = BooleanVariant.getBooleanVariant(true);
		assertEquals(文本操作.格式化文本("逻辑型=%b", booleanVariant),
				String.format(Locale.CHINA, "逻辑型=%b", true));

		Variant integerVariant = IntegerVariant.getIntegerVariant(123456);
		assertEquals(文本操作.格式化文本("整数型=%d", integerVariant),
			String.format(Locale.CHINA, "整数型=%d", 123456));

		Variant doubleVariant = DoubleVariant.getDoubleVariant(123.456);
		assertEquals(文本操作.格式化文本("小数型=%f", doubleVariant),
				String.format(Locale.CHINA, "小数型=%f", 123.456));

		Variant stringVariant = StringVariant.getStringVariant("文本");
		assertEquals(文本操作.格式化文本("文本型=%s", stringVariant),
				String.format(Locale.CHINA, "文本型=%s", "文本"));

	}

	/**
	 * Tests {@link 文本操作#代码(String)}.
	 */
	public void testAsc() {
		// Check that if the argument is null that a NullPointerException will be thrown.
		// Note that if the function is called from Simple code that the compiler will generate
		// exception handlers to convert NullPointerExceptions into the equivalent runtime error
		try {
			文本操作.代码(null);
			fail();
		} catch (NullPointerException expected) {
		}

		// Empty string
		try {
			文本操作.代码("");
			fail();
		} catch (IllegalArgumentException expected) {
		}

		// Other strings
		assertEquals(48, 文本操作.代码("0"));
		assertEquals(48, 文本操作.代码("0123"));
	}

	/**
	 * Tests {@link 文本操作#字符(int)}.
	 */
	public void testChr() {
		assertEquals("0", 文本操作.字符(48));
	}

	/**
	 * Tests {@link 文本操作#分割文本(String, String, int)}.
	 */
	public void testSplit() {
		// Check that if the array argument is null that a NullPointerException will be thrown.
		try {
			文本操作.分割文本(null, "", 1);
			fail();
		} catch (NullPointerException expected) {
		}

		// Check that if the string argument is null that a NullPointerException will be thrown.
		try {
			文本操作.分割文本("", null, 1);
			fail();
		} catch (NullPointerException expected) {
		}

		// Check that if the count argument less or equal to 0 that an  IllegalArgumentException will
		// be thrown.
		try {
			文本操作.分割文本("", "", Integer.MIN_VALUE);
			fail();
		} catch (IllegalArgumentException expected) {
		}

		try {
			文本操作.分割文本("", "", 0);
			fail();
		} catch (IllegalArgumentException expected) {
		}

		// Some regular splitting
		String[] result = 文本操作.分割文本("", "", 1);
		assertEquals(1, result.length);
		assertEquals("", result[0]);

		result = 文本操作.分割文本("", "", Integer.MAX_VALUE);
		assertEquals(1, result.length);
		assertEquals("", result[0]);

		result = 文本操作.分割文本("", "foo", 1);
		assertEquals(1, result.length);
		assertEquals("", result[0]);

		result = 文本操作.分割文本("", "foo", Integer.MAX_VALUE);
		assertEquals(1, result.length);
		assertEquals("", result[0]);

		result = 文本操作.分割文本("foo", "", 1);
		assertEquals(1, result.length);
		assertEquals("foo", result[0]);

		result = 文本操作.分割文本("foo", "", 3);
		assertEquals(3, result.length);
		// JDK1.7及以下版本运行结果为 ["","f", "oo"]
		// assertContentsInOrder(java.util.Arrays.asList(result), "", "f", "oo");
		// JDK1.8及以上版本运行结果为 ["f", "o", "o"]
		assertContentsInOrder(java.util.Arrays.asList(result), "f", "o", "o");

		result = 文本操作.分割文本("foo", "", Integer.MAX_VALUE);
		// JDK1.7及以下版本运行结果为 ["","f", "o", "o", ""]
		//assertEquals(5, result.length);
		// JDK1.8及以上版本运行结果为 ["f", "o", "o", ""]
		assertEquals(4, result.length);
		// JDK1.7及以下版本运行结果为 ["","f", "o", "o", ""]
		//assertContentsInOrder(java.util.Arrays.asList(result), "", "f", "o", "o", "");
		// JDK1.8及以上版本运行结果为 ["f", "o", "o", ""]
		assertContentsInOrder(java.util.Arrays.asList(result), "f", "o", "o", "");

		result = 文本操作.分割文本("foobarfoobarfoo", "foo", 1);
		assertEquals(1, result.length);
		assertEquals("foobarfoobarfoo", result[0]);

		result = 文本操作.分割文本("foobarfoobarfoo", "foo", 3);
		assertEquals(3, result.length);
		assertContentsInOrder(java.util.Arrays.asList(result), "", "bar", "barfoo");

		result = 文本操作.分割文本("foobarfoobarfoo", "foo", Integer.MAX_VALUE);
		assertEquals(4, result.length);
		assertContentsInOrder(java.util.Arrays.asList(result), "", "bar", "bar", "");

		result = 文本操作.分割文本("barfoobarfoobar", "foo", 1);
		assertEquals(1, result.length);
		assertEquals("barfoobarfoobar", result[0]);

		result = 文本操作.分割文本("barfoobarfoobar", "foo", 2);
		assertEquals(2, result.length);
		assertContentsInOrder(java.util.Arrays.asList(result), "bar", "barfoobar");

		result = 文本操作.分割文本("barfoobarfoobar", "foo", Integer.MAX_VALUE);
		assertEquals(3, result.length);
		assertContentsInOrder(java.util.Arrays.asList(result), "bar", "bar", "bar");
	}

	/**
	 * Tests {@link 文本操作#连接数组文本(String[], String)}.
	 */
	public void testJoin() {
		// Check that if the array argument is null that a NullPointerException will be thrown.
		try {
			文本操作.连接数组文本(null, "");
			fail();
		} catch (NullPointerException expected) {
		}

		// Check that if the string argument is null that a NullPointerException will be thrown.
		try {
			文本操作.连接数组文本(new String[0], null);
			fail();
		} catch (NullPointerException expected) {
		}

		// Some regular joining
		assertEquals("foo", 文本操作.连接数组文本(new String[] { "foo" }, ", "));
		assertEquals("foo, bar", 文本操作.连接数组文本(new String[] { "foo", "bar" }, ", "));
	}

	/**
	 * 按顺序核对分割文本得到的全部成员。
	 *
	 * @param <T> 成员类型。
	 * @param list 实际成员列表。
	 * @param contents 期望成员。
	 */
	@SuppressWarnings("unchecked")
	private <T> void assertContentsInOrder(List<T> list, T... contents) {
		assertEquals(list.size(), contents.length);

		for (int i = 0; i < contents.length; i++) {
			assertEquals(list.get(i), contents[i]);
		}
	}

	/**
	 * 省略截取长度时保留全部后缀，并继续拒绝越界起点。
	 */
	public void testTextSuffixBounds() {
		assertEquals("中文", 文本操作.取文本中间("A中文", 1));
		assertEquals("", 文本操作.取文本中间("abc", 3));
		assertEquals("", 文本操作.取文本中间("", 0));
		rejectInvalidRange(new Runnable() {
			@Override
			public void run() {
				文本操作.取文本中间("abc", -1);
			}
		});
		rejectInvalidRange(new Runnable() {
			@Override
			public void run() {
				文本操作.取文本中间("abc", 4);
			}
		});
	}

	/**
	 * 确认原完整接口的参数校验仍由便捷入口保留。
	 *
	 * @param action 应拒绝无效参数的操作。
	 */
	private static void rejectInvalidRange(Runnable action) {
		try {
			action.run();
			fail("无效参数未被拒绝");
		} catch (IllegalArgumentException expected) {
			// 保留原有参数错误约定。
		}
	}

	/**
	 * 省略查找起点时覆盖首尾、空匹配及未命中的情况。
	 */
	public void testDefaultSearchBounds() {
		assertEquals(0, 文本操作.寻找文本("ababa", "aba"));
		assertEquals(2, 文本操作.倒找文本("ababa", "aba"));
		assertEquals(-1, 文本操作.寻找文本("abc", "x"));
		assertEquals(-1, 文本操作.倒找文本("abc", "x"));
		assertEquals(0, 文本操作.寻找文本("", ""));
		assertEquals(0, 文本操作.倒找文本("", ""));
		assertEquals(3, 文本操作.倒找文本("abc", ""));
	}

	/**
	 * 省略次数时替换全部字面匹配，指定起点前的原文保持不变。
	 */
	public void testDefaultReplacementRange() {
		StringReferenceParameter text = new StringReferenceParameter("a.a.a");
		文本操作.子文本替换(text, ".", "$1\\x");
		assertEquals("a$1\\xa$1\\xa", text.get());

		text.set("a.a.a");
		文本操作.子文本替换(text, ".", "!", 2);
		assertEquals("a.a!a", text.get());
		文本操作.子文本替换(text, "a", "x", text.get().length());
		assertEquals("a.a!a", text.get());
		rejectSequenceArgument(new Runnable() {
			@Override
			public void run() {
				文本操作.子文本替换(text, "a", "x", -1);
			}
		});
		assertEquals("a.a!a", text.get());

		text.set("aa");
		文本操作.子文本替换(text, "a", "aa");
		assertEquals("aaaa", text.get());
	}

	/**

	 * 截取允许空区间和超长数量，不允许负长度或越界起点。

	 */
	public void testTextBounds() {
		assertEquals("bc", 文本操作.取文本中间("abc", 1, Integer.MAX_VALUE));
		assertEquals("abc", 文本操作.取文本左边("abc", Integer.MAX_VALUE));
		assertEquals("abc", 文本操作.取文本右边("abc", Integer.MAX_VALUE));
		assertEquals("", 文本操作.取文本中间("abc", 3, 100));
		assertEquals(2, 文本操作.取字符数("\uD83D\uDE00"));
		rejectSequenceArgument(new Runnable() {
			@Override
			public void run() {
				文本操作.取文本中间("abc", -1, 1);
			}
		});
		rejectSequenceArgument(new Runnable() {
			@Override
			public void run() {
				文本操作.取文本中间("abc", 4, 1);
			}
		});
		rejectSequenceArgument(new Runnable() {
			@Override
			public void run() {
				文本操作.取文本右边("abc", -1);
			}
		});
	}

	/**

	 * 单个空格和全空格应完全删除，制表符维持既有语义。

	 */
	public void testTrimming() {
		StringReferenceParameter text = new StringReferenceParameter(" ");
		文本操作.删尾空(text);
		assertEquals("", text.get());
		text.set("   ");
		文本操作.删尾空(text);
		assertEquals("", text.get());
		text.set(" \t ");
		文本操作.删首尾空(text);
		assertEquals("\t", text.get());
	}

	/**

	 * 查找与替换均为字面内容，只匹配原文，不再次匹配新写入内容。

	 */
	public void testLiteralReplacement() {
		StringReferenceParameter text = new StringReferenceParameter("a.a.a");
		文本操作.子文本替换(text, ".", "$1\\x", 0, 2);
		assertEquals("a$1\\xa$1\\xa", text.get());
		text.set("x\\Ex\\E");
		文本操作.子文本替换(text, "\\E", "$", 0, -1);
		assertEquals("x$x$", text.get());
		text.set("aa");
		文本操作.子文本替换(text, "a", "aa", 0, 2);
		assertEquals("aaaa", text.get());
		text.set("abc");
		文本操作.子文本替换(text, "", ".", 1, 2);
		assertEquals("a.b.c", text.get());
		文本操作.子文本替换(text, ".", "x", 0, 0);
		assertEquals("a.b.c", text.get());
	}

	/**

	 * 按起点与长度替换区间，末尾零长度表示追加。

	 */
	public void testRangeReplacement() {
		StringReferenceParameter text = new StringReferenceParameter("abc");
		文本操作.替换文本区间(text, 1, 1, "XY");
		assertEquals("aXYc", text.get());
		文本操作.替换文本区间(text, 4, 0, "!");
		assertEquals("aXYc!", text.get());
		文本操作.替换文本区间(text, 1, Integer.MAX_VALUE, "");
		assertEquals("a", text.get());
		rejectSequenceArgument(new Runnable() {
			@Override
			public void run() {
				文本操作.替换文本区间(text, 2, 0, "x");
			}
		});
		assertEquals("a", text.get());
	}

	/**

	 * 长度区分 UTF-16 单元和明确字符集的字节数。

	 */
	public void testEncodedLength() {
		assertEquals(2, 文本操作.取字符数("中文"));
		assertEquals(6, 文本操作.取文本字节数("中文"));
		assertEquals(4, 文本操作.取文本字节数("中文", "GBK"));
		assertEquals(0, 文本操作.取文本字节数(""));
		rejectSequenceArgument(new Runnable() {
			@Override
			public void run() {
				文本操作.取文本字节数("中文", "NO-SUCH-CHARSET");
			}
		});
	}

	/**

	 * 标记为字面内容，保留跨行及空内容，不递归处理嵌套。

	 */
	public void testMarkerExtraction() {
		String text = "前[一\n二][][末]后";
		assertEquals("一\n二", 文本操作.取标记间文本(text, "[", "]"));
		assertTrue(Arrays.equals(new String[]{"一\n二", "", "末"}, 文本操作.取全部标记间文本(text, "[", "]")));
		assertEquals("", 文本操作.取标记间文本("缺少[右边", "[", "]"));
		assertEquals(0, 文本操作.取全部标记间文本("缺少[右边", "[", "]").length);
		assertEquals("x", 文本操作.取标记间文本("\\Ex$", "\\E", "$"));
		assertEquals("a", 文本操作.取标记间文本("|a|", "|", "|"));
		rejectSequenceArgument(new Runnable() {
			@Override
			public void run() {
				文本操作.取标记间文本("abc", "", "]");
			}
		});
	}

	/**
	 * 文本分割和连接保留字面分隔符及空项。
	 */
	public void testTextSplitAndJoinBoundaries() {
		assertTrue(Arrays.equals(new String[]{"a", "", ""}, 文本操作.分割文本("a||", "|")));
		assertTrue(Arrays.equals(new String[]{"a", "b"}, 文本操作.分割文本("a\\Eb", "\\E")));
		assertTrue(Arrays.equals(new String[]{"a", "b|c"}, 文本操作.分割文本("a|b|c", "|", 2)));
		assertEquals("a|b", 文本操作.连接数组文本(new String[]{"a", "b"}, "|"));
	}

	/**
	 * 检查无效参数确实被拒绝。
	 *
	 * @param action 应抛出参数错误的操作。
	 */
	private void rejectSequenceArgument(Runnable action) {
		try {
			action.run();
		} catch (IllegalArgumentException expected) {
			return;
		}
		fail("无效参数未被拒绝。");
	}

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

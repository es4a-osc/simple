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


import simple.runtime.文本操作;
import simple.runtime.parameters.StringReferenceParameter;
import simple.runtime.variants.ArrayVariant;
import simple.runtime.variants.BooleanVariant;
import simple.runtime.variants.DoubleVariant;
import simple.runtime.variants.IntegerVariant;
import simple.runtime.variants.StringVariant;
import simple.runtime.variants.Variant;

import java.util.Locale;

import junit.framework.TestCase;

/**
 * Tests for {@link 文本操作}.
 *
 * @author Herbert Czymontek
 */
public class StringsTest extends TestCase {

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
		assertEquals("foffo", strRef.get());

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
	 * Tests {@link 文本操作#格式化文本(String, String)}。
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
}

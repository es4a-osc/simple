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

import simple.runtime.数组操作;
import simple.runtime.variants.ArrayVariant;
import simple.runtime.variants.Variant;

import java.util.List;

import junit.framework.TestCase;

/**
 * Tests for {@link 数组操作}.
 *
 * @author Herbert Czymontek
 */
public class ArraysTest extends TestCase {

	public ArraysTest(String testName) {
		super(testName);
	}

	/**
	 * Tests {@link 数组操作#过滤数组文本(String[], String, boolean)}.
	 */
	public void testFilter() {
		// Check that if the array argument is null that a NullPointerException will be thrown.
		// Note that if the function is called from Simple code that the compiler will generate
		// exception handlers to convert NullPointerExceptions into the equivalent runtime error
		try {
			数组操作.过滤数组文本(null, "", false);
			fail();
		} catch (NullPointerException expected) {
		}

		// Check that if the string argument is null that a NullPointerException will be thrown.
		try {
			数组操作.过滤数组文本(new String[0], null, false);
			fail();
		} catch (NullPointerException expected) {
		}

		// Some regular filtering
		String[] array = new String[] { "foo", "foobar", "bar", "foo" };

		String[] includeResult = 数组操作.过滤数组文本(array, "foo", true);
		assertEquals(2, includeResult.length);
		assertContentsAnyOrder(java.util.Arrays.asList(includeResult), "foo", "foobar");

		String[] excludeResult = 数组操作.过滤数组文本(array, "foo", false);
		assertEquals(1, excludeResult.length);
		assertEquals("bar", excludeResult[0]);
	}

	/**
	 * Tests {@link 数组操作#连接数组文本(String[], String)}.
	 */
	public void testJoin() {
		// Check that if the array argument is null that a NullPointerException will be thrown.
		try {
			数组操作.连接数组文本(null, "");
			fail();
		} catch (NullPointerException expected) {
		}

		// Check that if the string argument is null that a NullPointerException will be thrown.
		try {
			数组操作.连接数组文本(new String[0], null);
			fail();
		} catch (NullPointerException expected) {
		}

		// Some regular joining
		assertEquals("foo", 数组操作.连接数组文本(new String[] { "foo" }, ", "));
		assertEquals("foo, bar", 数组操作.连接数组文本(new String[] { "foo", "bar" }, ", "));
	}

	/**
	 * Tests {@link 数组操作#分割文本(String, String, int)}.
	 */
	public void testSplit() {
		// Check that if the array argument is null that a NullPointerException will be thrown.
		try {
			数组操作.分割文本(null, "", 1);
			fail();
		} catch (NullPointerException expected) {
		}

		// Check that if the string argument is null that a NullPointerException will be thrown.
		try {
			数组操作.分割文本("", null, 1);
			fail();
		} catch (NullPointerException expected) {
		}

		// Check that if the count argument less or equal to 0 that an  IllegalArgumentException will
		// be thrown.
		try {
			数组操作.分割文本("", "", Integer.MIN_VALUE);
			fail();
		} catch (IllegalArgumentException expected) {
		}

		try {
			数组操作.分割文本("", "", 0);
			fail();
		} catch (IllegalArgumentException expected) {
		}

		// Some regular splitting
		String[] result = 数组操作.分割文本("", "", 1);
		assertEquals(1, result.length);
		assertEquals("", result[0]);

		result = 数组操作.分割文本("", "", Integer.MAX_VALUE);
		assertEquals(1, result.length);
		assertEquals("", result[0]);

		result = 数组操作.分割文本("", "foo", 1);
		assertEquals(1, result.length);
		assertEquals("", result[0]);

		result = 数组操作.分割文本("", "foo", Integer.MAX_VALUE);
		assertEquals(1, result.length);
		assertEquals("", result[0]);

		result = 数组操作.分割文本("foo", "", 1);
		assertEquals(1, result.length);
		assertEquals("foo", result[0]);

		result = 数组操作.分割文本("foo", "", 3);
		assertEquals(3, result.length);
		// JDK1.7及以下版本运行结果为 ["","f", "oo"]
		// assertContentsInOrder(java.util.Arrays.asList(result), "", "f", "oo");
		// JDK1.8及以上版本运行结果为 ["f", "o", "o"]
		assertContentsInOrder(java.util.Arrays.asList(result), "f", "o", "o");

		result = 数组操作.分割文本("foo", "", Integer.MAX_VALUE);
		// JDK1.7及以下版本运行结果为 ["","f", "o", "o", ""]
		//assertEquals(5, result.length);
		// JDK1.8及以上版本运行结果为 ["f", "o", "o", ""]
		assertEquals(4, result.length);
		// JDK1.7及以下版本运行结果为 ["","f", "o", "o", ""]
		//assertContentsInOrder(java.util.Arrays.asList(result), "", "f", "o", "o", "");
		// JDK1.8及以上版本运行结果为 ["f", "o", "o", ""]
		assertContentsInOrder(java.util.Arrays.asList(result), "f", "o", "o", "");

		result = 数组操作.分割文本("foobarfoobarfoo", "foo", 1);
		assertEquals(1, result.length);
		assertEquals("foobarfoobarfoo", result[0]);

		result = 数组操作.分割文本("foobarfoobarfoo", "foo", 3);
		assertEquals(3, result.length);
		assertContentsInOrder(java.util.Arrays.asList(result), "", "bar", "barfoo");

		result = 数组操作.分割文本("foobarfoobarfoo", "foo", Integer.MAX_VALUE);
		assertEquals(4, result.length);
		assertContentsInOrder(java.util.Arrays.asList(result), "", "bar", "bar", "");

		result = 数组操作.分割文本("barfoobarfoobar", "foo", 1);
		assertEquals(1, result.length);
		assertEquals("barfoobarfoobar", result[0]);

		result = 数组操作.分割文本("barfoobarfoobar", "foo", 2);
		assertEquals(2, result.length);
		assertContentsInOrder(java.util.Arrays.asList(result), "bar", "barfoobar");

		result = 数组操作.分割文本("barfoobarfoobar", "foo", Integer.MAX_VALUE);
		assertEquals(3, result.length);
		assertContentsInOrder(java.util.Arrays.asList(result), "bar", "bar", "bar");
	}

	/**
	 * Tests {@link 数组操作#取数组下标(Variant, int)}.
	 */
	public void testUBound() {
		// Check that if the array argument is null that a NullPointerException will be thrown.
		try {
			数组操作.取数组下标(null, 1);
			fail();
		} catch (NullPointerException expected) {
		}

		final int FIRST_DIMENSION = 3;
		final int SECOND_DIMENSION = 5;
		Variant singleDimArray = ArrayVariant.getArrayVariant(new int[FIRST_DIMENSION]);
		Variant multiDimArray =
				ArrayVariant.getArrayVariant(new int[FIRST_DIMENSION][SECOND_DIMENSION]);

		// Negative dimension
		try {
			数组操作.取数组下标(singleDimArray, -1);
			fail();
		} catch (IllegalArgumentException expected) {
		}

		// Zero dimension
		try {
			数组操作.取数组下标(singleDimArray, 0);
			fail();
		} catch (IllegalArgumentException expected) {
		}

		// Dimension too large
		try {
			数组操作.取数组下标(singleDimArray, FIRST_DIMENSION + 1);
			fail();
		} catch (IllegalArgumentException expected) {
		}

		// Single-dimension array
		assertEquals(FIRST_DIMENSION, 数组操作.取数组下标(singleDimArray, 1));

		// Multi-dimension array
		assertEquals(FIRST_DIMENSION, 数组操作.取数组下标(singleDimArray, 1));
		assertEquals(FIRST_DIMENSION, 数组操作.取数组下标(multiDimArray, 1));
		assertEquals(SECOND_DIMENSION, 数组操作.取数组下标(multiDimArray, 2));
	}

	// 解决 [unchecked] 参数化 vararg 类型T的堆可能已受污染
	@SuppressWarnings("unchecked")
	private <T> void assertContentsAnyOrder(List<T> list, T... contents) {
		assertEquals(list.size(), contents.length);

		for (T element : contents) {
			assertTrue(list.contains(element));
		}
	}

	// 解决 [unchecked] 参数化 vararg 类型T的堆可能已受污染
	@SuppressWarnings("unchecked")
	private <T> void assertContentsInOrder(List<T> list, T... contents) {
		assertEquals(list.size(), contents.length);

		for (int i = 0; i < contents.length; i++) {
			assertEquals(list.get(i), contents[i]);
		}
	}
}

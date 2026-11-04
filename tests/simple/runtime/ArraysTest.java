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

import simple.runtime.variants.StringVariant;

import simple.runtime.variants.SingleVariant;

import simple.runtime.variants.ShortVariant;

import simple.runtime.variants.ObjectVariant;

import simple.runtime.variants.LongVariant;

import simple.runtime.variants.IntegerVariant;

import simple.runtime.variants.DoubleVariant;

import simple.runtime.variants.ByteVariant;

import simple.runtime.variants.BooleanVariant;

import java.lang.reflect.Array;

import java.util.Arrays;

import simple.runtime.variants.ArrayVariant;
import simple.runtime.variants.Variant;

import java.util.List;

import junit.framework.TestCase;

/**
 * Tests for {@link 数组操作}.
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public class ArraysTest extends TestCase {

	/**
	 * 创建指定名称的运行库用例。
	 *
	 * @param testName 用例名称
	 */
	public ArraysTest(String testName) {
		super(testName);
	}

	/**
	 * 数组成员数只统计第一维，包括空数组与不等长的二维数组。
	 */
	public void testArrayMemberCountUsesFirstDimension() {
		assertEquals(0, 数组操作.取数组成员数(ArrayVariant.getArrayVariant(new int[0])));
		assertEquals(3, 数组操作.取数组成员数(ArrayVariant.getArrayVariant(new String[]{"甲", null, "乙"})));
		assertEquals(2, 数组操作.取数组成员数(ArrayVariant.getArrayVariant(new int[][]{{1, 2, 3}, {4}})));
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

	/**
	 * 默认过滤保留包含项，维持去重顺序，完整接口仍可排除匹配项。
	 */
	public void testFilterIncludesByDefault() {
		String[] source = new String[]{"beta", "alpha", "beta", "gamma"};
		assertTrue(Arrays.equals(new String[]{"beta", "alpha", "gamma"}, 数组操作.过滤数组文本(source, "a")));
		assertTrue(Arrays.equals(new String[]{"beta"}, 数组操作.过滤数组文本(source, "et")));
		assertEquals(0, 数组操作.过滤数组文本(source, "absent").length);
		assertTrue(Arrays.equals(new String[]{"alpha", "gamma"}, 数组操作.过滤数组文本(source, "et", false)));
		assertEquals(4, source.length);
	}

	/**

	 * 追加后保持全部 Simple 基本元素类型和原数组内容。

	 */
	public void testArrayAppendTypes() {
		Object[][] cases = {
			{new boolean[]{true}, BooleanVariant.getBooleanVariant(false), boolean[].class},
			{new byte[]{1}, ByteVariant.getByteVariant((byte) 2), byte[].class},
			{new short[]{1}, ShortVariant.getShortVariant((short) 2), short[].class},
			{new int[]{1}, IntegerVariant.getIntegerVariant(2), int[].class},
			{new long[]{1}, LongVariant.getLongVariant(2), long[].class},
			{new float[]{1}, SingleVariant.getSingleVariant(2), float[].class},
			{new double[]{1}, DoubleVariant.getDoubleVariant(2), double[].class},
			{new String[]{"a"}, StringVariant.getStringVariant("b"), String[].class},
			{new Object[]{"a"}, ObjectVariant.getObjectVariant("b"), Object[].class}
		};
		for (Object[] item : cases) {
			Object source = item[0];
			Variant result = 数组操作.加入数组成员(ArrayVariant.getArrayVariant(source), (Variant) item[1]);
			assertNotNull(result);
			assertEquals(item[2], result.getArray().getClass());
			assertEquals(2, Array.getLength(result.getArray()));
			assertEquals(1, Array.getLength(source));
			assertEquals(Array.get(source, 0), Array.get(result.getArray(), 0));
		}
		int[][] nested = {{1}};
		Variant result = 数组操作.加入数组成员(ArrayVariant.getArrayVariant(nested), ArrayVariant.getArrayVariant(new int[]{2}));
		assertTrue(Arrays.deepEquals(new int[][]{{1}, {2}}, (int[][]) result.getArray()));
		assertNull(数组操作.加入数组成员(ArrayVariant.getArrayVariant(new int[0]), ObjectVariant.getObjectVariant(new Object())));
	}

	/**

	 * 可插入空数组、首部、中间和末尾，失败约定继续返回空。

	 */
	public void testArrayInsertion() {
		Variant empty = ArrayVariant.getArrayVariant(new int[0]);
		assertTrue(Arrays.equals(new int[]{7}, (int[]) 数组操作.插入数组成员(empty, IntegerVariant.getIntegerVariant(7), 0).getArray()));
		int[] source = {1, 3};
		for (int index = 0; index <= source.length; index++) {
			int[] expected = index == 0 ? new int[]{2, 1, 3} : index == 1 ? new int[]{1, 2, 3} : new int[]{1, 3, 2};
			assertTrue(Arrays.equals(expected, (int[]) 数组操作.插入数组成员(ArrayVariant.getArrayVariant(source), IntegerVariant.getIntegerVariant(2), index).getArray()));
		}
		assertTrue(Arrays.equals(new int[]{1, 3}, source));
		assertNull(数组操作.插入数组成员(empty, IntegerVariant.getIntegerVariant(7), -1));
		assertNull(数组操作.插入数组成员(empty, IntegerVariant.getIntegerVariant(7), 1));
	}

	/**

	 * 删除首尾及唯一成员，原数组保持不变。

	 */
	public void testArrayRemoval() {
		Variant source = ArrayVariant.getArrayVariant(new String[]{"a", "b", "c"});
		assertTrue(Arrays.equals(new String[]{"b", "c"}, (String[]) 数组操作.删除数组成员(source, 0).getArray()));
		assertTrue(Arrays.equals(new String[]{"a", "b"}, (String[]) 数组操作.删除数组成员(source, 2).getArray()));
		assertEquals(3, Array.getLength(source.getArray()));
		assertEquals(0, Array.getLength(数组操作.删除数组成员(ArrayVariant.getArrayVariant(new byte[]{1}), 0).getArray()));
		assertNull(数组操作.删除数组成员(source, 3));
		assertNull(数组操作.删除数组成员(source, -1));
	}

	/**

	 * 同数组重叠复制、数组合并都保留预期顺序。

	 */
	public void testCopyAndMerge() {
		int[] source = {1, 2, 3, 4};
		Variant wrapped = ArrayVariant.getArrayVariant(source);
		数组操作.复制数组成员(wrapped, 0, wrapped, 1, 3);
		assertTrue(Arrays.equals(new int[]{1, 1, 2, 3}, source));
		assertTrue(Arrays.equals(new String[]{"a", "b"}, 数组操作.合并文本数组(new String[]{"a"}, new String[]{"b"})));
	}

	/**

	 * 排序直接修改并返回原数组，文本按字典顺序处理。

	 */
	public void testSorting() {
		int[] numbers = {3, -1, 3, 0};
		assertSame(numbers, 数组操作.数组排序(numbers));
		assertTrue(Arrays.equals(new int[]{-1, 0, 3, 3}, numbers));
		String[] words = {"b", "10", "2", "a"};
		assertSame(words, 数组操作.文本数组排序(words));
		assertTrue(Arrays.equals(new String[]{"10", "2", "a", "b"}, words));
		String[] invalid = {"z", null, "a"};
		try {
			数组操作.文本数组排序(invalid);
			fail();
		} catch (NullPointerException expected) {
			assertTrue(Arrays.equals(new String[]{"z", null, "a"}, invalid));
		}
	}

	/**
	 * 文本数组过滤保留首次出现的匹配项。
	 */
	public void testTextArrayFilterDeduplicates() {
		assertTrue(Arrays.equals(new String[]{"foo", "foobar"}, 数组操作.过滤数组文本(new String[]{"foo", "foobar", "foo"}, "foo", true)));
	}
}

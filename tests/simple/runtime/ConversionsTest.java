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

import java.util.Arrays;

import java.util.Calendar;
import java.util.GregorianCalendar;

import simple.runtime.helpers.ConvHelpers;
import simple.runtime.variants.DateVariant;
import simple.runtime.variants.ArrayVariant;
import simple.runtime.variants.BooleanVariant;
import simple.runtime.variants.ByteVariant;
import simple.runtime.variants.IntegerVariant;
import simple.runtime.variants.LongVariant;
import simple.runtime.variants.ObjectVariant;
import simple.runtime.variants.StringVariant;
import simple.runtime.variants.UninitializedVariant;
import simple.runtime.variants.Variant;

import junit.framework.TestCase;

/**
 * Tests for {@link 数据转换}.
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public class ConversionsTest extends TestCase {

	/**
	 * 为 JUnit 创建指定名称的转换用例。
	 *
	 * @param testName 用例名称
	 */
	public ConversionsTest(String testName) {
		super(testName);
	}

	/**
	 * 直接验证导出转换函数的数值、符号截断及文本结果。
	 */
	public void testExportedScalarConversions() {
		// 使用固定值核对各种公开入口，不通过隐式转换代替函数调用。
		Variant integer = IntegerVariant.getIntegerVariant(257);
		assertTrue(数据转换.到逻辑(integer));
		assertFalse(数据转换.到逻辑(IntegerVariant.getIntegerVariant(0)));
		assertEquals((byte) 1, 数据转换.到字节(integer));
		assertEquals((short) 257, 数据转换.到短整数(integer));
		assertEquals(257, 数据转换.到整数(integer));
		assertEquals(257L, 数据转换.到长整数(integer));
		assertEquals(257.0f, 数据转换.到单精度小数(integer), 0.0f);
		assertEquals(257.0, 数据转换.到双精度小数(integer), 0.0);
		assertEquals("257", 数据转换.到文本(integer));

		// 中文文本及精确长整数不应被中间双精度小数截断。
		assertEquals("中文", 数据转换.到文本(StringVariant.getStringVariant("中文")));
		assertEquals(9007199254740993L,
			数据转换.到长整数(LongVariant.getLongVariant(9007199254740993L)));
		assertEquals(-1, 数据转换.到整数(ByteVariant.getByteVariant((byte) -1)));
		assertEquals(-1, 数据转换.到整数(BooleanVariant.getBooleanVariant(true)));
	}

	/**
	 * 不支持的变体类型使用接口约定的失败返回值，不抛出转换错误。
	 */
	public void testUnsupportedScalarConversions() {
		Variant unsupported = ObjectVariant.getObjectVariant(new Object());
		assertFalse(数据转换.到逻辑(unsupported));
		assertEquals((byte) 0, 数据转换.到字节(unsupported));
		assertEquals((short) 0, 数据转换.到短整数(unsupported));
		assertEquals(0, 数据转换.到整数(unsupported));
		assertEquals(0L, 数据转换.到长整数(unsupported));
		assertEquals(0.0f, 数据转换.到单精度小数(unsupported), 0.0f);
		assertEquals(0.0, 数据转换.到双精度小数(unsupported), 0.0);
		assertEquals("", 数据转换.到文本(unsupported));
	}

	/**
	 * 对象转换保留引用身份和基本类型包装，空值仍为空。
	 */
	public void testExportedObjectConversions() {
		Object object = new Object();
		assertSame(object, 数据转换.到对象(ObjectVariant.getObjectVariant(object)));
		assertEquals(Integer.valueOf(42), 数据转换.到对象(IntegerVariant.getIntegerVariant(42)));
		assertEquals(Boolean.TRUE, 数据转换.到对象(BooleanVariant.getBooleanVariant(true)));
		assertNull(数据转换.到对象(null));
		assertNull(数据转换.到对象(UninitializedVariant.getUninitializedVariant()));

		// 数组导出保留原数组；对象数组转换负责装箱及解包变体成员。
		int[] numbers = {1, 2};
		assertSame(numbers, 数据转换.到对象(ArrayVariant.getArrayVariant(numbers)));
		Object[] boxed = 数据转换.到对象数组(ArrayVariant.getArrayVariant(numbers));
		assertEquals(2, boxed.length);
		assertEquals(Integer.valueOf(1), boxed[0]);
		Variant[] mixed = {IntegerVariant.getIntegerVariant(7), StringVariant.getStringVariant("甲")};
		Object[] values = 数据转换.到对象数组(ArrayVariant.getArrayVariant(mixed));
		assertEquals(Integer.valueOf(7), values[0]);
		assertEquals("甲", values[1]);
		assertEquals(1, 数据转换.到对象数组(IntegerVariant.getIntegerVariant(9)).length);
		assertNull(数据转换.到对象数组(null));
	}

	/**
	 * 验证{@link Calendar}实现类可以转换为日期变体。
	 */
	public void testCalendarVariantConversion() {
		Calendar calendar = new GregorianCalendar(2026, Calendar.SEPTEMBER, 3, 16, 30, 0);
		Variant value = ConvHelpers.object2variant(calendar);

		assertTrue(value instanceof DateVariant);
		assertSame(calendar, value.getDate());
	}

	/**
	 * 整数转换使用四字节小端，不混同长整数的大端布局。
	 */
	public void testIntegerLittleEndian() {
		assertTrue(Arrays.equals(new byte[] {0x78, 0x56, 0x34, 0x12},
			数据转换.整数到字节集(0x12345678)));
		assertEquals(0x12345678, 数据转换.字节集到整数(new byte[] {0x78, 0x56, 0x34, 0x12, 0x00}));

		for (int value : new int[] {0, -1, 1, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
			assertEquals(value, 数据转换.字节集到整数(数据转换.整数到字节集(value)));
		}
	}

	/**
	 * 长整数转换使用八字节大端，保留最高位符号。
	 */
	public void testLongBigEndian() {
		assertTrue(Arrays.equals(new byte[] {1, 2, 3, 4, 5, 6, 7, 8},
			数据转换.长整数到字节集(0x0102030405060708L)));

		for (long value : new long[] {0, -1, 1, Long.MIN_VALUE, Long.MAX_VALUE}) {
			assertEquals(value, 数据转换.字节集到长整数(数据转换.长整数到字节集(value)));
		}

		assertEquals(0x0102030405060708L,
			数据转换.字节集到长整数(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 99}));
	}

	/**
	 * 不足固定长度的数组不会越界，也不按可用字节补零解释。
	 */
	public void testShortAndNullArrays() {
		assertEquals(0, 数据转换.字节集到整数(null));
		assertEquals(0L, 数据转换.字节集到长整数(null));
		assertEquals(0.0, 数据转换.字节集到双精度小数(null), 0.0);

		for (int length = 0; length < 8; length++) {
			byte[] bytes = new byte[length];
			Arrays.fill(bytes, (byte) 1);
			if (length < 4) {
				assertEquals(0, 数据转换.字节集到整数(bytes));
			}
			assertEquals(0L, 数据转换.字节集到长整数(bytes));
			assertEquals(0.0, 数据转换.字节集到双精度小数(bytes), 0.0);
		}
	}

	/**
	 * 双精度小数以大端 IEEE 754 表示，不经文本转换。
	 */
	public void testDoubleBitPatterns() {
		assertEquals(0x3FF0000000000000L, java.nio.ByteBuffer.wrap(数据转换.双精度小数到字节集(1.0)).getLong());
		assertEquals(0x8000000000000000L, java.nio.ByteBuffer.wrap(数据转换.双精度小数到字节集(-0.0)).getLong());
		assertEquals(0x7FF0000000000000L, java.nio.ByteBuffer.wrap(数据转换.双精度小数到字节集(Double.POSITIVE_INFINITY)).getLong());
		assertEquals(0xFFF0000000000000L, java.nio.ByteBuffer.wrap(数据转换.双精度小数到字节集(Double.NEGATIVE_INFINITY)).getLong());

		for (double value : new double[] {0.0, -0.0, -123.25, Double.MIN_VALUE, Double.MAX_VALUE,
				Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
			double actual = 数据转换.字节集到双精度小数(数据转换.双精度小数到字节集(value));
			assertEquals(Double.doubleToRawLongBits(value), Double.doubleToRawLongBits(actual));
		}
	}

	/**
	 * 编码规范化 NaN，解码允许 NaN，并忽略第八字节之后的内容。
	 */
	public void testNaNAndTrailingBytes() {
		double nonCanonical = Double.longBitsToDouble(0x7FF8000000000001L);
		assertEquals(0x7FF8000000000000L, java.nio.ByteBuffer.wrap(数据转换.双精度小数到字节集(nonCanonical)).getLong());
		assertTrue(Double.isNaN(数据转换.字节集到双精度小数(new byte[]{(byte) 0x7F, (byte) 0xF8, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01})));
		assertEquals(1.0, 数据转换.字节集到双精度小数(new byte[]{(byte) 0x3F, (byte) 0xF0, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFF}), 0.0);
	}

	/**
	 * 所有单字节均可无符号解释，不改变现有有符号类型转换。
	 */
	public void testUnsignedByteRange() {
		for (int value = 0; value < 256; value++) {
			assertEquals(value, 数据转换.取字节无符号值((byte) value));
		}
		assertEquals(-1, 数据转换.到整数(simple.runtime.variants.ByteVariant.getByteVariant((byte) -1)));
	}

	/**
	 * 已有文本转整数规则不因补充进制转换而改变。
	 */
	public void testExistingTextConversionPreserved() {
		assertEquals(1, 数据转换.到整数(StringVariant.getStringVariant("1.5")));
		assertEquals(0, 数据转换.到整数(StringVariant.getStringVariant("bad")));
	}
}

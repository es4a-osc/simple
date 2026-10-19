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


import simple.runtime.数值运算;
import simple.runtime.errors.转换错误;
import simple.runtime.variants.BooleanVariant;
import simple.runtime.variants.DoubleVariant;
import simple.runtime.variants.IntegerVariant;
import simple.runtime.variants.ObjectVariant;
import simple.runtime.variants.SingleVariant;
import simple.runtime.variants.StringVariant;

import junit.framework.TestCase;

/**
 * Tests for {@link 数值运算}.
 *
 * @author Herbert Czymontek
 */
public class MathTest extends TestCase {

	public MathTest(String testName) {
		super(testName);
	}

	/**
	 * Tests {@link 数值运算#取绝对值(simple.runtime.variants.Variant)}.
	 */
	public void testAbs() {
		// Variants containing numeric value
		assertEquals(123, 数值运算.取绝对值(IntegerVariant.getIntegerVariant(123)).getInteger());
		assertEquals(123, 数值运算.取绝对值(IntegerVariant.getIntegerVariant(-123)).getInteger());

		assertEquals(123.456, 数值运算.取绝对值(DoubleVariant.getDoubleVariant(123.456)).getDouble());
		assertEquals(123.456, 数值运算.取绝对值(DoubleVariant.getDoubleVariant(-123.456)).getDouble());

		// Non-numeric variants
		try {
			assertEquals("", 数值运算.取绝对值(StringVariant.getStringVariant("")));
			fail();
		} catch (转换错误 expected) {
		}
	}

	/**
	 * 验证反正弦的弧度范围、定义域和负零行为。
	 */
	public void testAsin() {
		assertEquals(-数值运算.PI / 2, 数值运算.求反正弦(-1), 0.0);
		assertEquals(0.0, 数值运算.求反正弦(0), 0.0);
		assertEquals(数值运算.PI / 2, 数值运算.求反正弦(1), 0.0);
		assertEquals(数值运算.PI / 6, 数值运算.求反正弦(0.5), 1.0e-15);
		assertEquals(Double.doubleToRawLongBits(-0.0),
				Double.doubleToRawLongBits(数值运算.求反正弦(-0.0)));

		// 越界、无穷大和 NaN 返回 NaN，不改为抛出异常。
		assertTrue(Double.isNaN(数值运算.求反正弦(1.0000000000000002)));
		assertTrue(Double.isNaN(数值运算.求反正弦(-1.0000000000000002)));
		assertTrue(Double.isNaN(数值运算.求反正弦(Double.POSITIVE_INFINITY)));
		assertTrue(Double.isNaN(数值运算.求反正弦(Double.NaN)));
	}

	/**
	 * 验证反余弦的弧度范围和定义域边界。
	 */
	public void testAcos() {
		assertEquals(数值运算.PI, 数值运算.求反余弦(-1), 0.0);
		assertEquals(数值运算.PI / 2, 数值运算.求反余弦(0), 0.0);
		assertEquals(0.0, 数值运算.求反余弦(1), 0.0);
		assertEquals(数值运算.PI / 3, 数值运算.求反余弦(0.5), 1.0e-15);

		assertTrue(Double.isNaN(数值运算.求反余弦(1.0000000000000002)));
		assertTrue(Double.isNaN(数值运算.求反余弦(-1.0000000000000002)));
		assertTrue(Double.isNaN(数值运算.求反余弦(Double.NEGATIVE_INFINITY)));
		assertTrue(Double.isNaN(数值运算.求反余弦(Double.NaN)));
	}

	/**
	 * Tests {@link 数值运算#求反正切(double)}.
	 */
	public void testAtn() {
		// The trigonometric (and some other mathematical) functions are just wrappers around their
		// Java runtime equivalent. Nevertheless we do some limited testing to ensure basic
		// functionality.
		assertEquals(java.lang.Math.atan(0), 数值运算.求反正切(0));
		assertEquals(java.lang.Math.atan(数值运算.PI / 4), 数值运算.求反正切(数值运算.PI / 4));
		assertEquals(java.lang.Math.atan(数值运算.PI / 2), 数值运算.求反正切(数值运算.PI / 2));
		assertEquals(java.lang.Math.atan(3 * (数值运算.PI / 4)), 数值运算.求反正切(3 * (数值运算.PI / 4)));
		assertEquals(java.lang.Math.atan(数值运算.PI), 数值运算.求反正切(数值运算.PI));
	}

	/**
	 * Tests {@link 数值运算#求余弦(double)}.
	 */
	public void testCos() {
		assertEquals(java.lang.Math.cos(0), 数值运算.求余弦(0));
		assertEquals(java.lang.Math.cos(数值运算.PI / 4), 数值运算.求余弦(数值运算.PI / 4));
		assertEquals(java.lang.Math.cos(数值运算.PI / 2), 数值运算.求余弦(数值运算.PI / 2));
		assertEquals(java.lang.Math.cos(3 * (数值运算.PI / 4)), 数值运算.求余弦(3 * (数值运算.PI / 4)));
		assertEquals(java.lang.Math.cos(数值运算.PI), 数值运算.求余弦(数值运算.PI));
	}

	/**
	 * Tests {@link 数值运算#角度转弧度(double)}.
	 */
	public void testDegreesToRadians() {
		assertEquals(java.lang.Math.toRadians(0), 数值运算.角度转弧度(0));
		assertEquals(java.lang.Math.toRadians(45), 数值运算.角度转弧度(45));
		assertEquals(java.lang.Math.toRadians(90), 数值运算.角度转弧度(90));
		assertEquals(java.lang.Math.toRadians(135), 数值运算.角度转弧度(135));
		assertEquals(java.lang.Math.toRadians(180), 数值运算.角度转弧度(180));
	}

	/**
	 * Tests {@link 数值运算#弧度转角度(double)}.
	 */
	public void testRadiansToDegrees() {
		assertEquals(java.lang.Math.toDegrees(0), 数值运算.弧度转角度(0));
		assertEquals(java.lang.Math.toDegrees(数值运算.PI / 4),
								 数值运算.弧度转角度(数值运算.PI / 4));
		assertEquals(java.lang.Math.toDegrees(数值运算.PI / 2),
								 数值运算.弧度转角度(数值运算.PI / 2));
		assertEquals(java.lang.Math.toDegrees(3 * (数值运算.PI / 4)),
								 数值运算.弧度转角度(3 * (数值运算.PI / 4)));
		assertEquals(java.lang.Math.toDegrees(数值运算.PI),
								 数值运算.弧度转角度(数值运算.PI));
	}

	/**
	 * Tests {@link 数值运算#求反对数(double)}.
	 */
	public void testExp() {
		assertEquals(java.lang.Math.exp(0), 数值运算.求反对数(0));
		assertEquals(java.lang.Math.exp(1), 数值运算.求反对数(1));
		assertEquals(java.lang.Math.exp(10), 数值运算.求反对数(10));
	}

	/**
	 * Tests {@link 数值运算#取整数(simple.runtime.variants.Variant)}.
	 */
	public void testInt() {
		assertEquals(0, 数值运算.取整数(BooleanVariant.getBooleanVariant(false)));
		assertEquals(-1, 数值运算.取整数(BooleanVariant.getBooleanVariant(true)));

		assertEquals(-10, 数值运算.取整数(IntegerVariant.getIntegerVariant(-10)));
		assertEquals(0, 数值运算.取整数(IntegerVariant.getIntegerVariant(0)));
		assertEquals(10, 数值运算.取整数(IntegerVariant.getIntegerVariant(10)));

		assertEquals(-10, 数值运算.取整数(SingleVariant.getSingleVariant(-10.123f)));
		assertEquals(0, 数值运算.取整数(SingleVariant.getSingleVariant(-0.123f)));
		assertEquals(0, 数值运算.取整数(SingleVariant.getSingleVariant(0.123f)));
		assertEquals(10, 数值运算.取整数(SingleVariant.getSingleVariant(10.123f)));

		assertEquals(-10, 数值运算.取整数(DoubleVariant.getDoubleVariant(-10.123)));
		assertEquals(0, 数值运算.取整数(DoubleVariant.getDoubleVariant(-0.123)));
		assertEquals(0, 数值运算.取整数(DoubleVariant.getDoubleVariant(0.123)));
		assertEquals(10, 数值运算.取整数(DoubleVariant.getDoubleVariant(10.123)));

		assertEquals(-10, 数值运算.取整数(StringVariant.getStringVariant("-10")));
		assertEquals(0, 数值运算.取整数(StringVariant.getStringVariant("0")));
		assertEquals(10, 数值运算.取整数(StringVariant.getStringVariant("10")));

		try {
			数值运算.取整数(StringVariant.getStringVariant("abc"));
			fail();
		} catch (转换错误 expected) {
		}

		try {
			数值运算.取整数(ObjectVariant.getObjectVariant(null));
			fail();
		} catch (转换错误 expected) {
		}
	}

	/**
	 * 验证十进制中点按 HALF_UP 舍入，包括负数、进位和正零结果。
	 */
	public void testRoundHalfUp() {
		assertEquals(1.01, 数值运算.四舍五入(1.005, 2), 0.0);
		assertEquals(-1.01, 数值运算.四舍五入(-1.005, 2), 0.0);
		assertEquals(2.68, 数值运算.四舍五入(2.675, 2), 0.0);
		assertEquals(100.0, 数值运算.四舍五入(99.999, 2), 0.0);
		assertEquals(2.0, 数值运算.四舍五入(1.5, 0), 0.0);
		assertEquals(-2.0, 数值运算.四舍五入(-1.5, 0), 0.0);
		assertEquals(Double.doubleToRawLongBits(0.0),
				Double.doubleToRawLongBits(数值运算.四舍五入(-0.004, 2)));
		assertEquals(Double.doubleToRawLongBits(0.0),
				Double.doubleToRawLongBits(数值运算.四舍五入(-0.0, 2)));
	}

	/**
	 * 验证负位数按十位、百位舍入，以及最终双精度溢出的行为。
	 */
	public void testRoundNegativeScale() {
		assertEquals(150.0, 数值运算.四舍五入(145, -1), 0.0);
		assertEquals(-150.0, 数值运算.四舍五入(-145, -1), 0.0);
		assertEquals(1200.0, 数值运算.四舍五入(1234, -2), 0.0);
		assertEquals(0.0, 数值运算.四舍五入(4, -1), 0.0);
		assertEquals(Double.POSITIVE_INFINITY, 数值运算.四舍五入(Double.MAX_VALUE, -308), 0.0);
	}

	/**
	 * 验证非有限输入和无法处理的小数位规模明确报错。
	 */
	public void testRoundInvalidValues() {
		for (double value : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
			try {
				数值运算.四舍五入(value, 2);
				fail("NaN 和无穷大不能进行十进制舍入");
			} catch (NumberFormatException expected) {
			}
		}

		try {
			数值运算.四舍五入(1, Integer.MIN_VALUE);
			fail("无法处理的小数位规模应报错");
		} catch (ArithmeticException expected) {
		}
	}

	/**
	 * Tests {@link 数值运算#求自然对数(double)}.
	 */
	public void testLog() {
		assertEquals(java.lang.Math.log(0), 数值运算.求自然对数(0));
		assertEquals(java.lang.Math.log(1), 数值运算.求自然对数(1));
		assertEquals(java.lang.Math.log(10), 数值运算.求自然对数(10));
		assertEquals(java.lang.Math.log(数值运算.E), 数值运算.求自然对数(数值运算.E));
	}

	/**
	 * Tests {@link 数值运算#取最大值(simple.runtime.variants.Variant,
	 *   simple.runtime.variants.Variant)}.
	 */
	public void testMax() {
		assertFalse(数值运算.取最大值(BooleanVariant.getBooleanVariant(false),
				BooleanVariant.getBooleanVariant(false)).getBoolean());
		assertTrue(数值运算.取最大值(BooleanVariant.getBooleanVariant(true),
				BooleanVariant.getBooleanVariant(true)).getBoolean());
		assertFalse(数值运算.取最大值(BooleanVariant.getBooleanVariant(true),
				BooleanVariant.getBooleanVariant(false)).getBoolean());
		assertFalse(数值运算.取最大值(BooleanVariant.getBooleanVariant(false),
				BooleanVariant.getBooleanVariant(true)).getBoolean());

		assertEquals(-10, 数值运算.取最大值(IntegerVariant.getIntegerVariant(-10),
				IntegerVariant.getIntegerVariant(-10)).getInteger());
		assertEquals(10, 数值运算.取最大值(IntegerVariant.getIntegerVariant(-10),
				IntegerVariant.getIntegerVariant(10)).getInteger());
		assertEquals(10, 数值运算.取最大值(IntegerVariant.getIntegerVariant(10),
				IntegerVariant.getIntegerVariant(-10)).getInteger());
		assertEquals(10, 数值运算.取最大值(IntegerVariant.getIntegerVariant(10),
				IntegerVariant.getIntegerVariant(10)).getInteger());

		assertEquals(-10.123, 数值运算.取最大值(DoubleVariant.getDoubleVariant(-10.123),
				DoubleVariant.getDoubleVariant(-10.123)).getDouble());
		assertEquals(10.123, 数值运算.取最大值(DoubleVariant.getDoubleVariant(-10.123),
				DoubleVariant.getDoubleVariant(10.123)).getDouble());
		assertEquals(10.123, 数值运算.取最大值(DoubleVariant.getDoubleVariant(10.123),
				DoubleVariant.getDoubleVariant(-10.123)).getDouble());
		assertEquals(10.123, 数值运算.取最大值(DoubleVariant.getDoubleVariant(10.123),
				DoubleVariant.getDoubleVariant(10.123)).getDouble());

		assertEquals(-10.123, 数值运算.取最大值(StringVariant.getStringVariant("-10.123"),
				StringVariant.getStringVariant("-10.123")).getDouble());
		assertEquals(10.123, 数值运算.取最大值(StringVariant.getStringVariant("-10.123"),
				StringVariant.getStringVariant("10.123")).getDouble());
		assertEquals(10.123, 数值运算.取最大值(StringVariant.getStringVariant("10.123"),
				StringVariant.getStringVariant("-10.123")).getDouble());
		assertEquals(10.123, 数值运算.取最大值(StringVariant.getStringVariant("10.123"),
				StringVariant.getStringVariant("10.123")).getDouble());

		try {
			数值运算.取最大值(StringVariant.getStringVariant("abc"), StringVariant.getStringVariant("def"));
			fail();
		} catch (转换错误 expected) {
		}

		try {
			数值运算.取最大值(IntegerVariant.getIntegerVariant(10), StringVariant.getStringVariant("def"));
			fail();
		} catch (转换错误 expected) {
		}

		try {
			数值运算.取最大值(StringVariant.getStringVariant("abc"), IntegerVariant.getIntegerVariant(10));
			fail();
		} catch (转换错误 expected) {
		}

		try {
			数值运算.取最大值(IntegerVariant.getIntegerVariant(10), ObjectVariant.getObjectVariant(null));
			fail();
		} catch (转换错误 expected) {
		}
	}

	/**
	 * Tests {@link 数值运算#取最小值(simple.runtime.variants.Variant,
	 *   simple.runtime.variants.Variant)}.
	 */
	public void testMin() {
		assertFalse(数值运算.取最小值(BooleanVariant.getBooleanVariant(false),
				BooleanVariant.getBooleanVariant(false)).getBoolean());
		assertTrue(数值运算.取最小值(BooleanVariant.getBooleanVariant(true),
				BooleanVariant.getBooleanVariant(true)).getBoolean());
		assertTrue(数值运算.取最小值(BooleanVariant.getBooleanVariant(true),
				BooleanVariant.getBooleanVariant(false)).getBoolean());
		assertTrue(数值运算.取最小值(BooleanVariant.getBooleanVariant(false),
				BooleanVariant.getBooleanVariant(true)).getBoolean());

		assertEquals(-10, 数值运算.取最小值(IntegerVariant.getIntegerVariant(-10),
				IntegerVariant.getIntegerVariant(-10)).getInteger());
		assertEquals(-10, 数值运算.取最小值(IntegerVariant.getIntegerVariant(-10),
				IntegerVariant.getIntegerVariant(10)).getInteger());
		assertEquals(-10, 数值运算.取最小值(IntegerVariant.getIntegerVariant(10),
				IntegerVariant.getIntegerVariant(-10)).getInteger());
		assertEquals(10, 数值运算.取最小值(IntegerVariant.getIntegerVariant(10),
				IntegerVariant.getIntegerVariant(10)).getInteger());

		assertEquals(-10.123, 数值运算.取最小值(DoubleVariant.getDoubleVariant(-10.123),
				DoubleVariant.getDoubleVariant(-10.123)).getDouble());
		assertEquals(-10.123, 数值运算.取最小值(DoubleVariant.getDoubleVariant(-10.123),
				DoubleVariant.getDoubleVariant(10.123)).getDouble());
		assertEquals(-10.123, 数值运算.取最小值(DoubleVariant.getDoubleVariant(10.123),
				DoubleVariant.getDoubleVariant(-10.123)).getDouble());
		assertEquals(10.123, 数值运算.取最小值(DoubleVariant.getDoubleVariant(10.123),
				DoubleVariant.getDoubleVariant(10.123)).getDouble());

		assertEquals(-10.123, 数值运算.取最小值(StringVariant.getStringVariant("-10.123"),
				StringVariant.getStringVariant("-10.123")).getDouble());
		assertEquals(-10.123, 数值运算.取最小值(StringVariant.getStringVariant("-10.123"),
				StringVariant.getStringVariant("10.123")).getDouble());
		assertEquals(-10.123, 数值运算.取最小值(StringVariant.getStringVariant("10.123"),
				StringVariant.getStringVariant("-10.123")).getDouble());
		assertEquals(10.123, 数值运算.取最小值(StringVariant.getStringVariant("10.123"),
				StringVariant.getStringVariant("10.123")).getDouble());

		try {
			数值运算.取最小值(StringVariant.getStringVariant("abc"), StringVariant.getStringVariant("def"));
			fail();
		} catch (转换错误 expected) {
		}

		try {
			数值运算.取最小值(IntegerVariant.getIntegerVariant(10), StringVariant.getStringVariant("def"));
			fail();
		} catch (转换错误 expected) {
		}

		try {
			数值运算.取最小值(StringVariant.getStringVariant("abc"), IntegerVariant.getIntegerVariant(10));
			fail();
		} catch (转换错误 expected) {
		}

		try {
			数值运算.取最小值(IntegerVariant.getIntegerVariant(10), ObjectVariant.getObjectVariant(null));
			fail();
		} catch (转换错误 expected) {
		}
	}

	/**
	 * Tests {@link 数值运算#取随机数()}.
	 */
	public void testRnd() {
		// That's pretty hard to test. Nevertheless we want to at least invoke the function to make
		// sure it doesn't crash.
		数值运算.取随机数();
	}

	/**
	 * 验证单点闭区间包含零、负数及两个整数极值。
	 */
	public void testRndRangeSingleValue() {
		assertEquals(0, 数值运算.取随机数(0, 0));
		assertEquals(-7, 数值运算.取随机数(-7, -7));
		assertEquals(Integer.MIN_VALUE, 数值运算.取随机数(Integer.MIN_VALUE, Integer.MIN_VALUE));
		assertEquals(Integer.MAX_VALUE, 数值运算.取随机数(Integer.MAX_VALUE, Integer.MAX_VALUE));
	}

	/**
	 * 验证负区间、跨零区间、窄边界区间和超过整数宽度的区间不会溢出。
	 */
	public void testRndRangeBounds() {
		int[][] ranges = {
			{-7, -3}, {-3, 4}, {0, 6},
			{Integer.MIN_VALUE, Integer.MIN_VALUE + 1},
			{Integer.MAX_VALUE - 1, Integer.MAX_VALUE},
			{Integer.MIN_VALUE, -1}, {0, Integer.MAX_VALUE},
			{Integer.MIN_VALUE, Integer.MAX_VALUE},
			{Integer.MIN_VALUE + 1, Integer.MAX_VALUE},
			{Integer.MIN_VALUE, Integer.MAX_VALUE - 1}
		};
		for (int[] range : ranges) {
			for (int i = 0; i < 200; i++) {
				int value = 数值运算.取随机数(range[0], range[1]);
				assertTrue(value >= range[0]);
				assertTrue(value <= range[1]);
			}
		}
	}

	/**
	 * 验证反向范围明确报错，不交换端点或返回特殊数值。
	 */
	public void testRndRangeInvalidBounds() {
		try {
			数值运算.取随机数(1, 0);
			fail("最小值大于最大值时应报错");
		} catch (IllegalArgumentException expected) {
		}

		try {
			数值运算.取随机数(Integer.MAX_VALUE, Integer.MIN_VALUE);
			fail("反向极值范围应报错");
		} catch (IllegalArgumentException expected) {
		}
	}

	/**
	 * Tests {@link 数值运算#取符号(double)}.
	 */
	public void testSgn() {
		assertEquals(-1, 数值运算.取符号(-10.123));
		assertEquals(0, 数值运算.取符号(0));
		assertEquals(1, 数值运算.取符号(10.123));
	}

	/**
	 * Tests {@link 数值运算#求正弦(double)}.
	 */
	public void testSin() {
		assertEquals(java.lang.Math.sin(0), 数值运算.求正弦(0));
		assertEquals(java.lang.Math.sin(数值运算.PI / 4), 数值运算.求正弦(数值运算.PI / 4));
		assertEquals(java.lang.Math.sin(数值运算.PI / 2), 数值运算.求正弦(数值运算.PI / 2));
		assertEquals(java.lang.Math.sin(3 * (数值运算.PI / 4)), 数值运算.求正弦(3 * (数值运算.PI / 4)));
		assertEquals(java.lang.Math.sin(数值运算.PI), 数值运算.求正弦(数值运算.PI));
	}

	/**
	 * Tests {@link 数值运算#求平方根(double)}.
	 */
	public void testSqr() {
		assertEquals(java.lang.Math.sqrt(0), 数值运算.求平方根(0));
		assertEquals(java.lang.Math.sqrt(2), 数值运算.求平方根(2));
		assertEquals(java.lang.Math.sqrt(4), 数值运算.求平方根(4));
		assertEquals(java.lang.Math.sqrt(10), 数值运算.求平方根(10));
	}

	/**
	 * 验证立方根支持负数、极大极小值及特殊浮点值。
	 */
	public void testCbrt() {
		assertEquals(3.0, 数值运算.求立方根(27), 0.0);
		assertEquals(-2.0, 数值运算.求立方根(-8), 0.0);
		assertEquals(2.0e100, 数值运算.求立方根(8.0e300), 1.0e85);
		assertEquals(2.0e-100, 数值运算.求立方根(8.0e-300), 1.0e-115);
		assertTrue(数值运算.求立方根(Double.MIN_VALUE) > 0);

		assertEquals(Double.doubleToRawLongBits(-0.0),
				Double.doubleToRawLongBits(数值运算.求立方根(-0.0)));
		assertEquals(Double.POSITIVE_INFINITY, 数值运算.求立方根(Double.POSITIVE_INFINITY), 0.0);
		assertEquals(Double.NEGATIVE_INFINITY, 数值运算.求立方根(Double.NEGATIVE_INFINITY), 0.0);
		assertTrue(Double.isNaN(数值运算.求立方根(Double.NaN)));
	}

	/**
	 * 验证斜边运算不会因中间平方产生不必要的溢出或下溢。
	 */
	public void testHypot() {
		assertEquals(5.0, 数值运算.求斜边(3, 4), 0.0);
		assertEquals(5.0, 数值运算.求斜边(-3, -4), 0.0);
		assertEquals(5.0e200, 数值运算.求斜边(3.0e200, 4.0e200), 1.0e186);
		assertEquals(5.0e-200, 数值运算.求斜边(3.0e-200, 4.0e-200), 1.0e-214);
		assertEquals(Double.MAX_VALUE, 数值运算.求斜边(Double.MAX_VALUE, 0), 0.0);
		assertEquals(Double.doubleToRawLongBits(0.0),
				Double.doubleToRawLongBits(数值运算.求斜边(-0.0, -0.0)));

		// 无穷大优先于 NaN；仅最终结果超出双精度范围时返回无穷大。
		assertEquals(Double.POSITIVE_INFINITY, 数值运算.求斜边(Double.NaN, Double.POSITIVE_INFINITY), 0.0);
		assertEquals(Double.POSITIVE_INFINITY, 数值运算.求斜边(Double.NEGATIVE_INFINITY, Double.NaN), 0.0);
		assertTrue(Double.isNaN(数值运算.求斜边(Double.NaN, 1)));
		assertTrue(Double.isNaN(数值运算.求斜边(1, Double.NaN)));
		assertEquals(Double.POSITIVE_INFINITY, 数值运算.求斜边(Double.MAX_VALUE, Double.MAX_VALUE), 0.0);
	}

	/**
	 * Tests {@link 数值运算#求正切(double)}.
	 */
	public void testTan() {
		assertEquals(java.lang.Math.tan(0), 数值运算.求正切(0));
		assertEquals(java.lang.Math.tan(数值运算.PI / 4), 数值运算.求正切(数值运算.PI / 4));
		assertEquals(java.lang.Math.tan(数值运算.PI / 2), 数值运算.求正切(数值运算.PI / 2));
		assertEquals(java.lang.Math.tan(3 * (数值运算.PI / 4)), 数值运算.求正切(3 * (数值运算.PI / 4)));
		assertEquals(java.lang.Math.tan(数值运算.PI), 数值运算.求正切(数值运算.PI));
	}
}

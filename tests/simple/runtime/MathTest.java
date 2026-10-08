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


import simple.runtime.算术运算;
import simple.runtime.errors.转换错误;
import simple.runtime.variants.BooleanVariant;
import simple.runtime.variants.DoubleVariant;
import simple.runtime.variants.IntegerVariant;
import simple.runtime.variants.ObjectVariant;
import simple.runtime.variants.SingleVariant;
import simple.runtime.variants.StringVariant;

import junit.framework.TestCase;

/**
 * Tests for {@link 算术运算}.
 *
 * @author Herbert Czymontek
 */
public class MathTest extends TestCase {

	public MathTest(String testName) {
		super(testName);
	}

	/**
	 * Tests {@link 算术运算#取绝对值(simple.runtime.variants.Variant)}.
	 */
	public void testAbs() {
		// Variants containing numeric value
		assertEquals(123, 算术运算.取绝对值(IntegerVariant.getIntegerVariant(123)).getInteger());
		assertEquals(123, 算术运算.取绝对值(IntegerVariant.getIntegerVariant(-123)).getInteger());

		assertEquals(123.456, 算术运算.取绝对值(DoubleVariant.getDoubleVariant(123.456)).getDouble());
		assertEquals(123.456, 算术运算.取绝对值(DoubleVariant.getDoubleVariant(-123.456)).getDouble());

		// Non-numeric variants
		try {
			assertEquals("", 算术运算.取绝对值(StringVariant.getStringVariant("")));
			fail();
		} catch (转换错误 expected) {
		}
	}

	/**
	 * Tests {@link 算术运算#求反正切(double)}.
	 */
	public void testAtn() {
		// The trigonometric (and some other mathematical) functions are just wrappers around their
		// Java runtime equivalent. Nevertheless we do some limited testing to ensure basic
		// functionality.
		assertEquals(java.lang.Math.atan(0), 算术运算.求反正切(0));
		assertEquals(java.lang.Math.atan(算术运算.PI / 4), 算术运算.求反正切(算术运算.PI / 4));
		assertEquals(java.lang.Math.atan(算术运算.PI / 2), 算术运算.求反正切(算术运算.PI / 2));
		assertEquals(java.lang.Math.atan(3 * (算术运算.PI / 4)), 算术运算.求反正切(3 * (算术运算.PI / 4)));
		assertEquals(java.lang.Math.atan(算术运算.PI), 算术运算.求反正切(算术运算.PI));
	}

	/**
	 * Tests {@link 算术运算#求余弦(double)}.
	 */
	public void testCos() {
		assertEquals(java.lang.Math.cos(0), 算术运算.求余弦(0));
		assertEquals(java.lang.Math.cos(算术运算.PI / 4), 算术运算.求余弦(算术运算.PI / 4));
		assertEquals(java.lang.Math.cos(算术运算.PI / 2), 算术运算.求余弦(算术运算.PI / 2));
		assertEquals(java.lang.Math.cos(3 * (算术运算.PI / 4)), 算术运算.求余弦(3 * (算术运算.PI / 4)));
		assertEquals(java.lang.Math.cos(算术运算.PI), 算术运算.求余弦(算术运算.PI));
	}

	/**
	 * Tests {@link 算术运算#角度转弧度(double)}.
	 */
	public void testDegreesToRadians() {
		assertEquals(java.lang.Math.toRadians(0), 算术运算.角度转弧度(0));
		assertEquals(java.lang.Math.toRadians(45), 算术运算.角度转弧度(45));
		assertEquals(java.lang.Math.toRadians(90), 算术运算.角度转弧度(90));
		assertEquals(java.lang.Math.toRadians(135), 算术运算.角度转弧度(135));
		assertEquals(java.lang.Math.toRadians(180), 算术运算.角度转弧度(180));
	}

	/**
	 * Tests {@link 算术运算#弧度转角度(double)}.
	 */
	public void testRadiansToDegrees() {
		assertEquals(java.lang.Math.toDegrees(0), 算术运算.弧度转角度(0));
		assertEquals(java.lang.Math.toDegrees(算术运算.PI / 4),
								 算术运算.弧度转角度(算术运算.PI / 4));
		assertEquals(java.lang.Math.toDegrees(算术运算.PI / 2),
								 算术运算.弧度转角度(算术运算.PI / 2));
		assertEquals(java.lang.Math.toDegrees(3 * (算术运算.PI / 4)),
								 算术运算.弧度转角度(3 * (算术运算.PI / 4)));
		assertEquals(java.lang.Math.toDegrees(算术运算.PI),
								 算术运算.弧度转角度(算术运算.PI));
	}

	/**
	 * Tests {@link 算术运算#求反对数(double)}.
	 */
	public void testExp() {
		assertEquals(java.lang.Math.exp(0), 算术运算.求反对数(0));
		assertEquals(java.lang.Math.exp(1), 算术运算.求反对数(1));
		assertEquals(java.lang.Math.exp(10), 算术运算.求反对数(10));
	}

	/**
	 * Tests {@link 算术运算#取整数(simple.runtime.variants.Variant)}.
	 */
	public void testInt() {
		assertEquals(0, 算术运算.取整数(BooleanVariant.getBooleanVariant(false)));
		assertEquals(-1, 算术运算.取整数(BooleanVariant.getBooleanVariant(true)));

		assertEquals(-10, 算术运算.取整数(IntegerVariant.getIntegerVariant(-10)));
		assertEquals(0, 算术运算.取整数(IntegerVariant.getIntegerVariant(0)));
		assertEquals(10, 算术运算.取整数(IntegerVariant.getIntegerVariant(10)));

		assertEquals(-10, 算术运算.取整数(SingleVariant.getSingleVariant(-10.123f)));
		assertEquals(0, 算术运算.取整数(SingleVariant.getSingleVariant(-0.123f)));
		assertEquals(0, 算术运算.取整数(SingleVariant.getSingleVariant(0.123f)));
		assertEquals(10, 算术运算.取整数(SingleVariant.getSingleVariant(10.123f)));

		assertEquals(-10, 算术运算.取整数(DoubleVariant.getDoubleVariant(-10.123)));
		assertEquals(0, 算术运算.取整数(DoubleVariant.getDoubleVariant(-0.123)));
		assertEquals(0, 算术运算.取整数(DoubleVariant.getDoubleVariant(0.123)));
		assertEquals(10, 算术运算.取整数(DoubleVariant.getDoubleVariant(10.123)));

		assertEquals(-10, 算术运算.取整数(StringVariant.getStringVariant("-10")));
		assertEquals(0, 算术运算.取整数(StringVariant.getStringVariant("0")));
		assertEquals(10, 算术运算.取整数(StringVariant.getStringVariant("10")));

		try {
			算术运算.取整数(StringVariant.getStringVariant("abc"));
			fail();
		} catch (转换错误 expected) {
		}

		try {
			算术运算.取整数(ObjectVariant.getObjectVariant(null));
			fail();
		} catch (转换错误 expected) {
		}
	}

	/**
	 * Tests {@link 算术运算#求自然对数(double)}.
	 */
	public void testLog() {
		assertEquals(java.lang.Math.log(0), 算术运算.求自然对数(0));
		assertEquals(java.lang.Math.log(1), 算术运算.求自然对数(1));
		assertEquals(java.lang.Math.log(10), 算术运算.求自然对数(10));
		assertEquals(java.lang.Math.log(算术运算.E), 算术运算.求自然对数(算术运算.E));
	}

	/**
	 * Tests {@link 算术运算#取最大值(simple.runtime.variants.Variant,
	 *   simple.runtime.variants.Variant)}.
	 */
	public void testMax() {
		assertFalse(算术运算.取最大值(BooleanVariant.getBooleanVariant(false),
				BooleanVariant.getBooleanVariant(false)).getBoolean());
		assertTrue(算术运算.取最大值(BooleanVariant.getBooleanVariant(true),
				BooleanVariant.getBooleanVariant(true)).getBoolean());
		assertFalse(算术运算.取最大值(BooleanVariant.getBooleanVariant(true),
				BooleanVariant.getBooleanVariant(false)).getBoolean());
		assertFalse(算术运算.取最大值(BooleanVariant.getBooleanVariant(false),
				BooleanVariant.getBooleanVariant(true)).getBoolean());

		assertEquals(-10, 算术运算.取最大值(IntegerVariant.getIntegerVariant(-10),
				IntegerVariant.getIntegerVariant(-10)).getInteger());
		assertEquals(10, 算术运算.取最大值(IntegerVariant.getIntegerVariant(-10),
				IntegerVariant.getIntegerVariant(10)).getInteger());
		assertEquals(10, 算术运算.取最大值(IntegerVariant.getIntegerVariant(10),
				IntegerVariant.getIntegerVariant(-10)).getInteger());
		assertEquals(10, 算术运算.取最大值(IntegerVariant.getIntegerVariant(10),
				IntegerVariant.getIntegerVariant(10)).getInteger());

		assertEquals(-10.123, 算术运算.取最大值(DoubleVariant.getDoubleVariant(-10.123),
				DoubleVariant.getDoubleVariant(-10.123)).getDouble());
		assertEquals(10.123, 算术运算.取最大值(DoubleVariant.getDoubleVariant(-10.123),
				DoubleVariant.getDoubleVariant(10.123)).getDouble());
		assertEquals(10.123, 算术运算.取最大值(DoubleVariant.getDoubleVariant(10.123),
				DoubleVariant.getDoubleVariant(-10.123)).getDouble());
		assertEquals(10.123, 算术运算.取最大值(DoubleVariant.getDoubleVariant(10.123),
				DoubleVariant.getDoubleVariant(10.123)).getDouble());

		assertEquals(-10.123, 算术运算.取最大值(StringVariant.getStringVariant("-10.123"),
				StringVariant.getStringVariant("-10.123")).getDouble());
		assertEquals(10.123, 算术运算.取最大值(StringVariant.getStringVariant("-10.123"),
				StringVariant.getStringVariant("10.123")).getDouble());
		assertEquals(10.123, 算术运算.取最大值(StringVariant.getStringVariant("10.123"),
				StringVariant.getStringVariant("-10.123")).getDouble());
		assertEquals(10.123, 算术运算.取最大值(StringVariant.getStringVariant("10.123"),
				StringVariant.getStringVariant("10.123")).getDouble());

		try {
			算术运算.取最大值(StringVariant.getStringVariant("abc"), StringVariant.getStringVariant("def"));
			fail();
		} catch (转换错误 expected) {
		}

		try {
			算术运算.取最大值(IntegerVariant.getIntegerVariant(10), StringVariant.getStringVariant("def"));
			fail();
		} catch (转换错误 expected) {
		}

		try {
			算术运算.取最大值(StringVariant.getStringVariant("abc"), IntegerVariant.getIntegerVariant(10));
			fail();
		} catch (转换错误 expected) {
		}

		try {
			算术运算.取最大值(IntegerVariant.getIntegerVariant(10), ObjectVariant.getObjectVariant(null));
			fail();
		} catch (转换错误 expected) {
		}
	}

	/**
	 * Tests {@link 算术运算#取最小值(simple.runtime.variants.Variant,
	 *   simple.runtime.variants.Variant)}.
	 */
	public void testMin() {
		assertFalse(算术运算.取最小值(BooleanVariant.getBooleanVariant(false),
				BooleanVariant.getBooleanVariant(false)).getBoolean());
		assertTrue(算术运算.取最小值(BooleanVariant.getBooleanVariant(true),
				BooleanVariant.getBooleanVariant(true)).getBoolean());
		assertTrue(算术运算.取最小值(BooleanVariant.getBooleanVariant(true),
				BooleanVariant.getBooleanVariant(false)).getBoolean());
		assertTrue(算术运算.取最小值(BooleanVariant.getBooleanVariant(false),
				BooleanVariant.getBooleanVariant(true)).getBoolean());

		assertEquals(-10, 算术运算.取最小值(IntegerVariant.getIntegerVariant(-10),
				IntegerVariant.getIntegerVariant(-10)).getInteger());
		assertEquals(-10, 算术运算.取最小值(IntegerVariant.getIntegerVariant(-10),
				IntegerVariant.getIntegerVariant(10)).getInteger());
		assertEquals(-10, 算术运算.取最小值(IntegerVariant.getIntegerVariant(10),
				IntegerVariant.getIntegerVariant(-10)).getInteger());
		assertEquals(10, 算术运算.取最小值(IntegerVariant.getIntegerVariant(10),
				IntegerVariant.getIntegerVariant(10)).getInteger());

		assertEquals(-10.123, 算术运算.取最小值(DoubleVariant.getDoubleVariant(-10.123),
				DoubleVariant.getDoubleVariant(-10.123)).getDouble());
		assertEquals(-10.123, 算术运算.取最小值(DoubleVariant.getDoubleVariant(-10.123),
				DoubleVariant.getDoubleVariant(10.123)).getDouble());
		assertEquals(-10.123, 算术运算.取最小值(DoubleVariant.getDoubleVariant(10.123),
				DoubleVariant.getDoubleVariant(-10.123)).getDouble());
		assertEquals(10.123, 算术运算.取最小值(DoubleVariant.getDoubleVariant(10.123),
				DoubleVariant.getDoubleVariant(10.123)).getDouble());

		assertEquals(-10.123, 算术运算.取最小值(StringVariant.getStringVariant("-10.123"),
				StringVariant.getStringVariant("-10.123")).getDouble());
		assertEquals(-10.123, 算术运算.取最小值(StringVariant.getStringVariant("-10.123"),
				StringVariant.getStringVariant("10.123")).getDouble());
		assertEquals(-10.123, 算术运算.取最小值(StringVariant.getStringVariant("10.123"),
				StringVariant.getStringVariant("-10.123")).getDouble());
		assertEquals(10.123, 算术运算.取最小值(StringVariant.getStringVariant("10.123"),
				StringVariant.getStringVariant("10.123")).getDouble());

		try {
			算术运算.取最小值(StringVariant.getStringVariant("abc"), StringVariant.getStringVariant("def"));
			fail();
		} catch (转换错误 expected) {
		}

		try {
			算术运算.取最小值(IntegerVariant.getIntegerVariant(10), StringVariant.getStringVariant("def"));
			fail();
		} catch (转换错误 expected) {
		}

		try {
			算术运算.取最小值(StringVariant.getStringVariant("abc"), IntegerVariant.getIntegerVariant(10));
			fail();
		} catch (转换错误 expected) {
		}

		try {
			算术运算.取最小值(IntegerVariant.getIntegerVariant(10), ObjectVariant.getObjectVariant(null));
			fail();
		} catch (转换错误 expected) {
		}
	}

	/**
	 * Tests {@link 算术运算#取随机数()}.
	 */
	public void testRnd() {
		// That's pretty hard to test. Nevertheless we want to at least invoke the function to make
		// sure it doesn't crash.
		算术运算.取随机数();
	}

	/**
	 * Tests {@link 算术运算#取符号(double)}.
	 */
	public void testSgn() {
		assertEquals(-1, 算术运算.取符号(-10.123));
		assertEquals(0, 算术运算.取符号(0));
		assertEquals(1, 算术运算.取符号(10.123));
	}

	/**
	 * Tests {@link 算术运算#求正弦(double)}.
	 */
	public void testSin() {
		assertEquals(java.lang.Math.sin(0), 算术运算.求正弦(0));
		assertEquals(java.lang.Math.sin(算术运算.PI / 4), 算术运算.求正弦(算术运算.PI / 4));
		assertEquals(java.lang.Math.sin(算术运算.PI / 2), 算术运算.求正弦(算术运算.PI / 2));
		assertEquals(java.lang.Math.sin(3 * (算术运算.PI / 4)), 算术运算.求正弦(3 * (算术运算.PI / 4)));
		assertEquals(java.lang.Math.sin(算术运算.PI), 算术运算.求正弦(算术运算.PI));
	}

	/**
	 * Tests {@link 算术运算#求平方根(double)}.
	 */
	public void testSqr() {
		assertEquals(java.lang.Math.sqrt(0), 算术运算.求平方根(0));
		assertEquals(java.lang.Math.sqrt(2), 算术运算.求平方根(2));
		assertEquals(java.lang.Math.sqrt(4), 算术运算.求平方根(4));
		assertEquals(java.lang.Math.sqrt(10), 算术运算.求平方根(10));
	}

	/**
	 * Tests {@link 算术运算#求正切(double)}.
	 */
	public void testTan() {
		assertEquals(java.lang.Math.tan(0), 算术运算.求正切(0));
		assertEquals(java.lang.Math.tan(算术运算.PI / 4), 算术运算.求正切(算术运算.PI / 4));
		assertEquals(java.lang.Math.tan(算术运算.PI / 2), 算术运算.求正切(算术运算.PI / 2));
		assertEquals(java.lang.Math.tan(3 * (算术运算.PI / 4)), 算术运算.求正切(3 * (算术运算.PI / 4)));
		assertEquals(java.lang.Math.tan(算术运算.PI), 算术运算.求正切(算术运算.PI));
	}
}

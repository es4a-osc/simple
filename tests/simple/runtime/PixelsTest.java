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

import junit.framework.TestCase;
import simple.runtime.errors.转换错误;
import simple.runtime.variants.ByteVariant;
import simple.runtime.variants.DoubleVariant;
import simple.runtime.variants.IntegerVariant;
import simple.runtime.variants.LongVariant;
import simple.runtime.variants.ShortVariant;
import simple.runtime.variants.StringVariant;

/**
 * Tests for {@link 像素转换#解析像素(simple.runtime.variants.Variant)}.
 */
public final class PixelsTest extends TestCase {

	public void testIntegralVariants() {
		assertEquals(0, 像素转换.解析像素(ByteVariant.getByteVariant((byte) 0)));
		assertEquals(Short.MAX_VALUE, 像素转换.解析像素(ShortVariant.getShortVariant(Short.MAX_VALUE)));
		assertEquals(Integer.MIN_VALUE, 像素转换.解析像素(IntegerVariant.getIntegerVariant(Integer.MIN_VALUE)));
		assertEquals(Integer.MAX_VALUE, 像素转换.解析像素(LongVariant.getLongVariant(Integer.MAX_VALUE)));
	}

	public void testAbsolutePixelText() {
		assertEquals(10, 像素转换.解析像素(StringVariant.getStringVariant("10")));
		assertEquals(-12, 像素转换.解析像素(StringVariant.getStringVariant(" -12PX ")));
	}

	public void testRejectsOutOfRangeLong() {
		assertConversionError(LongVariant.getLongVariant((long) Integer.MAX_VALUE + 1));
		assertConversionError(LongVariant.getLongVariant((long) Integer.MIN_VALUE - 1));
	}

	public void testRejectsNonIntegralNumber() {
		assertConversionError(DoubleVariant.getDoubleVariant(1.5));
	}

	private static void assertConversionError(simple.runtime.variants.Variant value) {
		try {
			像素转换.解析像素(value);
			fail();
		} catch (转换错误 expected) {
		}
	}
}

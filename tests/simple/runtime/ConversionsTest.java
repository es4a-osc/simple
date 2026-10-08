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

import java.util.Calendar;
import java.util.GregorianCalendar;

import simple.runtime.数据转换;
import simple.runtime.errors.转换错误;
import simple.runtime.helpers.ConvHelpers;
import simple.runtime.variants.DateVariant;
import simple.runtime.variants.DoubleVariant;
import simple.runtime.variants.IntegerVariant;
import simple.runtime.variants.StringVariant;
import simple.runtime.variants.Variant;

import junit.framework.TestCase;

/**
 * Tests for {@link 数据转换}.
 *
 * @author Herbert Czymontek
 */
public class ConversionsTest extends TestCase {

	public ConversionsTest(String testName) {
		super(testName);
	}

	/**
	 * Tests {@link 数据转换#代码(String)}.
	 */
	public void testAsc() {
		// Check that if the argument is null that a NullPointerException will be thrown.
		// Note that if the function is called from Simple code that the compiler will generate
		// exception handlers to convert NullPointerExceptions into the equivalent runtime error
		try {
			数据转换.代码(null);
			fail();
		} catch (NullPointerException expected) {
		}

		// Empty string
		try {
			数据转换.代码("");
			fail();
		} catch (IllegalArgumentException expected) {
		}

		// Other strings
		assertEquals(48, 数据转换.代码("0"));
		assertEquals(48, 数据转换.代码("0123"));
	}

	/**
	 * Tests {@link 数据转换#字符(int)}.
	 */
	public void testChr() {
		assertEquals("0", 数据转换.字符(48));
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
	 * Tests {@link 数据转换#十六进制(simple.runtime.variants.Variant)}.
	 */
	public void testHex() {
		// Variants containing numeric value
		assertEquals("123ABC", 数据转换.十六进制(IntegerVariant.getIntegerVariant(0x123ABC)));
		assertEquals("FFFFFFFFFFFFFFFF", 数据转换.十六进制(IntegerVariant.getIntegerVariant(-1)));
		assertEquals("A", 数据转换.十六进制(DoubleVariant.getDoubleVariant(10.3)));

		// Non-numeric variants
		try {
			assertEquals("", 数据转换.十六进制(StringVariant.getStringVariant("")));
			fail();
		} catch (转换错误 expected) {
		}
	}
}

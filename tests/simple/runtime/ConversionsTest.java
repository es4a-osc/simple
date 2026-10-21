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

import simple.runtime.helpers.ConvHelpers;
import simple.runtime.variants.DateVariant;
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
	 * 验证{@link Calendar}实现类可以转换为日期变体。
	 */
	public void testCalendarVariantConversion() {
		Calendar calendar = new GregorianCalendar(2026, Calendar.SEPTEMBER, 3, 16, 30, 0);
		Variant value = ConvHelpers.object2variant(calendar);

		assertTrue(value instanceof DateVariant);
		assertSame(calendar, value.getDate());
	}
}

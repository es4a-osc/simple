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

import simple.runtime.日期时间操作;

import junit.framework.TestCase;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Tests for {@link 日期时间操作}.
 *
 * @author Herbert Czymontek
 */
public class DatesTest extends TestCase {

	public DatesTest(String testName) {
		super(testName);
	}

	/**
	 * Tests {@link 日期时间操作#增减时间(Calendar, int, int)}.
	 */
	public void testDateAdd() {
		GregorianCalendar date = new GregorianCalendar(2004, Calendar.FEBRUARY, 29, 20, 01, 30);
		日期时间操作.增减时间(date, 日期时间操作.日期_年, 1);
		assertEquals(2005, date.get(GregorianCalendar.YEAR));
		assertEquals(28, date.get(GregorianCalendar.DAY_OF_MONTH));
		assertEquals(Calendar.FEBRUARY, date.get(GregorianCalendar.MONTH));

		date = new GregorianCalendar(2004, Calendar.FEBRUARY, 29, 20, 01, 30);
		日期时间操作.增减时间(date, 日期时间操作.日期_年, -1);
		assertEquals(2003, date.get(GregorianCalendar.YEAR));
		assertEquals(28, date.get(GregorianCalendar.DAY_OF_MONTH));
		assertEquals(Calendar.FEBRUARY, date.get(GregorianCalendar.MONTH));

		date = new GregorianCalendar(2004, Calendar.DECEMBER, 29, 20, 01, 30);
		日期时间操作.增减时间(date, 日期时间操作.日期_月, 1);
		assertEquals(2005, date.get(GregorianCalendar.YEAR));
		assertEquals(29, date.get(GregorianCalendar.DAY_OF_MONTH));
		assertEquals(Calendar.JANUARY, date.get(GregorianCalendar.MONTH));

		date = new GregorianCalendar(2004, Calendar.JANUARY, 29, 20, 01, 30);
		日期时间操作.增减时间(date, 日期时间操作.日期_月, -1);
		assertEquals(2003, date.get(GregorianCalendar.YEAR));
		assertEquals(29, date.get(GregorianCalendar.DAY_OF_MONTH));
		assertEquals(Calendar.DECEMBER, date.get(GregorianCalendar.MONTH));

		date = new GregorianCalendar(2100, Calendar.FEBRUARY, 28, 20, 01, 30);
		日期时间操作.增减时间(date, 日期时间操作.日期_天, 1);
		assertEquals(2100, date.get(GregorianCalendar.YEAR));
		assertEquals(1, date.get(GregorianCalendar.DAY_OF_MONTH));
		assertEquals(Calendar.MARCH, date.get(GregorianCalendar.MONTH));

		date = new GregorianCalendar(2100, Calendar.MARCH, 1, 20, 01, 30);
		日期时间操作.增减时间(date, 日期时间操作.日期_天, -1);
		assertEquals(2100, date.get(GregorianCalendar.YEAR));
		assertEquals(28, date.get(GregorianCalendar.DAY_OF_MONTH));
		assertEquals(Calendar.FEBRUARY, date.get(GregorianCalendar.MONTH));

		date = new GregorianCalendar(2100, Calendar.FEBRUARY, 28, 23, 01, 30);
		日期时间操作.增减时间(date, 日期时间操作.日期_时, 1);
		assertEquals(2100, date.get(GregorianCalendar.YEAR));
		assertEquals(1, date.get(GregorianCalendar.DAY_OF_MONTH));
		assertEquals(Calendar.MARCH, date.get(GregorianCalendar.MONTH));
		assertEquals(0, date.get(GregorianCalendar.HOUR_OF_DAY));

		date = new GregorianCalendar(2100, Calendar.MARCH, 1, 0, 01, 30);
		日期时间操作.增减时间(date, 日期时间操作.日期_时, -1);
		assertEquals(2100, date.get(GregorianCalendar.YEAR));
		assertEquals(28, date.get(GregorianCalendar.DAY_OF_MONTH));
		assertEquals(Calendar.FEBRUARY, date.get(GregorianCalendar.MONTH));
		assertEquals(23, date.get(GregorianCalendar.HOUR_OF_DAY));

		date = new GregorianCalendar(2100, Calendar.FEBRUARY, 28, 23, 59, 30);
		日期时间操作.增减时间(date, 日期时间操作.日期_分, 1);
		assertEquals(2100, date.get(GregorianCalendar.YEAR));
		assertEquals(1, date.get(GregorianCalendar.DAY_OF_MONTH));
		assertEquals(Calendar.MARCH, date.get(GregorianCalendar.MONTH));
		assertEquals(0, date.get(GregorianCalendar.HOUR_OF_DAY));
		assertEquals(0, date.get(GregorianCalendar.MINUTE));

		date = new GregorianCalendar(2100, Calendar.MARCH, 1, 0, 0, 30);
		日期时间操作.增减时间(date, 日期时间操作.日期_分, -1);
		assertEquals(2100, date.get(GregorianCalendar.YEAR));
		assertEquals(28, date.get(GregorianCalendar.DAY_OF_MONTH));
		assertEquals(Calendar.FEBRUARY, date.get(GregorianCalendar.MONTH));
		assertEquals(23, date.get(GregorianCalendar.HOUR_OF_DAY));
		assertEquals(59, date.get(GregorianCalendar.MINUTE));

		date = new GregorianCalendar(2100, Calendar.FEBRUARY, 28, 23, 59, 59);
		日期时间操作.增减时间(date, 日期时间操作.日期_秒, 1);
		assertEquals(2100, date.get(GregorianCalendar.YEAR));
		assertEquals(1, date.get(GregorianCalendar.DAY_OF_MONTH));
		assertEquals(Calendar.MARCH, date.get(GregorianCalendar.MONTH));
		assertEquals(0, date.get(GregorianCalendar.HOUR_OF_DAY));
		assertEquals(0, date.get(GregorianCalendar.MINUTE));
		assertEquals(0, date.get(GregorianCalendar.SECOND));

		date = new GregorianCalendar(2100, Calendar.MARCH, 1, 0, 0, 0);
		日期时间操作.增减时间(date, 日期时间操作.日期_秒, -1);
		assertEquals(2100, date.get(GregorianCalendar.YEAR));
		assertEquals(28, date.get(GregorianCalendar.DAY_OF_MONTH));
		assertEquals(Calendar.FEBRUARY, date.get(GregorianCalendar.MONTH));
		assertEquals(23, date.get(GregorianCalendar.HOUR_OF_DAY));
		assertEquals(59, date.get(GregorianCalendar.MINUTE));
		assertEquals(59, date.get(GregorianCalendar.SECOND));
	}

	/**
	 * Tests {@link 日期时间操作#日期值(String)}.
	 */
	public void testDateValue() {
		// Check NullPointerException is thrown when the input string is null
		try {
			日期时间操作.日期值(null);
			fail();
		} catch (NullPointerException expected) {
		}

		// Empty string is an illegal argument
		try {
			日期时间操作.日期值("");
			fail();
		} catch (IllegalArgumentException expected) {
		}

		// Another illegal argument
		try {
			日期时间操作.日期值("abc");
			fail();
		} catch (IllegalArgumentException expected) {
		}

		// Input string with extra characters at the end is OK
		日期时间操作.日期值("2008/04/21 abc");
		日期时间操作.日期值("2008/04/21 09:29:48 abc");

		// Date only input string
		Calendar date = 日期时间操作.日期值("2008/04/21");
		assertEquals(2008, date.get(GregorianCalendar.YEAR));
		assertEquals(21, date.get(GregorianCalendar.DAY_OF_MONTH));
		assertEquals(Calendar.APRIL, date.get(GregorianCalendar.MONTH));
		assertEquals(0, date.get(GregorianCalendar.HOUR_OF_DAY));
		assertEquals(0, date.get(GregorianCalendar.MINUTE));
		assertEquals(0, date.get(GregorianCalendar.SECOND));

		// Date and time input string
		date = 日期时间操作.日期值("2008/04/21 09:29:48");
		assertEquals(2008, date.get(GregorianCalendar.YEAR));
		assertEquals(21, date.get(GregorianCalendar.DAY_OF_MONTH));
		assertEquals(Calendar.APRIL, date.get(GregorianCalendar.MONTH));
		assertEquals(9, date.get(GregorianCalendar.HOUR_OF_DAY));
		assertEquals(29, date.get(GregorianCalendar.MINUTE));
		assertEquals(48, date.get(GregorianCalendar.SECOND));
	}

	/**
	 * Tests {@link 日期时间操作#取日(Calendar)}.
	 */
	public void testDay() {
		Calendar date = 日期时间操作.日期值("2008/04/21 09:29:48");
		assertEquals(21, 日期时间操作.取日(date));
	}

	/**
	 * Tests {@link 日期时间操作#格式化时间(Calendar)}.
	 */
	public void testFormatDate() {
		Calendar date = 日期时间操作.日期值("2008/04/21 09:29:48");

		// 日期和时间输入字符串
		assertTrue(日期时间操作.格式化时间(date, "yyyy/MM/dd HH:mm:ss").startsWith("2008/04/21 09:29:48"));
		assertTrue(日期时间操作.格式化时间(date, "yyyy-MM-dd HH:mm:ss").startsWith("2008-04-21 09:29:48"));
		assertTrue(日期时间操作.格式化时间(date, "yyyy.MM.dd HH:mm:ss").startsWith("2008.04.21 09:29:48"));
		assertTrue(日期时间操作.格式化时间(date, "yyyy年MM月dd日HH时mm分ss秒").startsWith("2008年04月21日09时29分48秒"));
		assertTrue(日期时间操作.格式化时间(date, "yyyyMMddHHmmss").startsWith("20080421092948"));

		// 只输入日期字符串
		assertTrue(日期时间操作.格式化时间(date, "HH:mm:ss").startsWith("09:29:48"));
		assertTrue(日期时间操作.格式化时间(date, "HH-mm-ss").startsWith("09-29-48"));
		assertTrue(日期时间操作.格式化时间(date, "HH.mm.ss").startsWith("09.29.48"));
		assertTrue(日期时间操作.格式化时间(date, "HH时mm分ss秒").startsWith("09时29分48秒"));
	}

	/**
	 * Tests {@link 日期时间操作#取小时(Calendar)}.
	 */
	public void testHour() {
		Calendar date = 日期时间操作.日期值("2008/04/21 09:29:48");
		assertEquals(9, 日期时间操作.取小时(date));
	}

	/**
	 * Tests {@link 日期时间操作#取分钟(Calendar)}.
	 */
	public void testMinute() {
		Calendar date = 日期时间操作.日期值("2008/04/21 09:29:48");
		assertEquals(29, 日期时间操作.取分钟(date));
	}

	/**
	 * Tests {@link 日期时间操作#取月份(Calendar)}.
	 */
	public void testMonth() {
		Calendar date = 日期时间操作.日期值("2008/04/21 09:29:48");
		assertEquals(日期时间操作.月份_四月, 日期时间操作.取月份(date));
	}

	/**
	 * Tests {@link 日期时间操作#取月份名(Calendar)}.
	 */
	public void testMonthName() {
		Calendar date = 日期时间操作.日期值("2008/04/21 09:29:48");
		assertEquals("四月", 日期时间操作.取月份名(date));
	}

	/**
	 * Tests {@link 日期时间操作#取现行时间()}.
	 */
	public void testNow() {
		// Not much to test here, just making sure this doesn't crash
		assertNotNull(日期时间操作.取现行时间());
	}

	/**
	 * Tests {@link 日期时间操作#取秒(Calendar)}.
	 */
	public void testSecond() {
		Calendar date = 日期时间操作.日期值("2008/04/21 09:29:48");
		assertEquals(48, 日期时间操作.取秒(date));
	}

	/**
	 * Tests {@link 日期时间操作#取现行时间戳()}.
	 */
	public void testTimer() {
		final long sleepInterval = 1000;
		long start = 日期时间操作.取现行时间戳();
		try {
			Thread.sleep(sleepInterval);
		} catch (InterruptedException e) {
			fail();
		}
		long end = 日期时间操作.取现行时间戳();

		assertTrue(end - start >= sleepInterval);
	}

	/**
	 * Tests {@link 日期时间操作#取星期(Calendar)}.
	 */
	public void testWeekday() {
		Calendar date = 日期时间操作.日期值("2008/04/21 09:29:48");
		assertEquals(日期时间操作.星期_星期一, 日期时间操作.取星期(date));
	}

	/**
	 * Tests {@link 日期时间操作#取星期名(Calendar)}.
	 */
	public void testWeekdayName() {
		Calendar date = 日期时间操作.日期值("2008/04/21 09:29:48");
		assertEquals("星期一", 日期时间操作.取星期名(date));
	}

	/**
	 * Tests {@link 日期时间操作#取年(Calendar)}.
	 */
	public void testYear() {
		Calendar date = 日期时间操作.日期值("2008/04/21 09:29:48");
		assertEquals(2008, 日期时间操作.取年(date));
	}

	/** 扩展 **/

	/**
	 * 测试{@link 日期时间操作#文本到时间(String, String, String)}.
	 */
	public void testStringtoDate() {


		// 日期和时间输入字符串
		Calendar date = 日期时间操作.日期值("2008/04/21 09:29:48");
		assertEquals(日期时间操作.文本到时间("2008/04/21 09:29:48", "yyyy/MM/dd HH:mm:ss", 日期时间操作.时区_上海).getTimeInMillis(), date.getTimeInMillis());
		assertEquals(日期时间操作.文本到时间("2008-04-21 09:29:48", "yyyy-MM-dd HH:mm:ss", 日期时间操作.时区_上海).getTimeInMillis(), date.getTimeInMillis());
		assertEquals(日期时间操作.文本到时间("2008.04.21 09:29:48", "yyyy.MM.dd HH:mm:ss", 日期时间操作.时区_上海).getTimeInMillis(), date.getTimeInMillis());
		assertEquals(日期时间操作.文本到时间("2008年04月21日09时29分48秒", "yyyy年MM月dd日HH时mm分ss秒", 日期时间操作.时区_上海).getTimeInMillis(), date.getTimeInMillis());
		assertEquals(日期时间操作.文本到时间("20080421092948", "yyyyMMddHHmmss", 日期时间操作.时区_上海).getTimeInMillis(), date.getTimeInMillis());

		// 只输入日期字符串
		Calendar time = 日期时间操作.日期值("2008/04/21");
		assertEquals(日期时间操作.文本到时间("2008/04/21", "yyyy/MM/dd", 日期时间操作.时区_上海).getTimeInMillis(), time.getTimeInMillis());
		assertEquals(日期时间操作.文本到时间("2008-04-21", "yyyy-MM-dd", 日期时间操作.时区_上海).getTimeInMillis(), time.getTimeInMillis());
		assertEquals(日期时间操作.文本到时间("2008.04.21", "yyyy.MM.dd", 日期时间操作.时区_上海).getTimeInMillis(), time.getTimeInMillis());
		assertEquals(日期时间操作.文本到时间("2008年04月21日", "yyyy年MM月dd日", 日期时间操作.时区_上海).getTimeInMillis(), time.getTimeInMillis());
		assertEquals(日期时间操作.文本到时间("20080421", "yyyyMMdd", 日期时间操作.时区_上海).getTimeInMillis(), time.getTimeInMillis());
	}

	/**
	 * 测试{@link 日期时间操作#取时间间隔(Calendar, Calendar, int)}。
	 */
	public void testGetDateInterval() {
		// 测试年
		Calendar date1 = 日期时间操作.日期值("2008/04/21 09:29:48");
		Calendar date2 = 日期时间操作.日期值("2020/04/21 09:29:48");
		assertEquals(日期时间操作.取时间间隔(date1, date2, 日期时间操作.日期_年), 12);

		// 测试月
		date1 = 日期时间操作.日期值("2008/04/21 09:29:48");
		date2 = 日期时间操作.日期值("2008/12/21 09:29:48");
		assertEquals(日期时间操作.取时间间隔(date1, date2, 日期时间操作.日期_月), 8);

		// 测试天
		date1 = 日期时间操作.日期值("2008/04/21 09:29:48");
		date2 = 日期时间操作.日期值("2008/04/28 09:29:48");
		assertEquals(日期时间操作.取时间间隔(date1, date2, 日期时间操作.日期_天), 7);

		// 测试时
		date1 = 日期时间操作.日期值("2008/04/21 09:29:48");
		date2 = 日期时间操作.日期值("2008/04/21 22:29:48");
		assertEquals(日期时间操作.取时间间隔(date1, date2, 日期时间操作.日期_时), 13);

		// 测试分
		date1 = 日期时间操作.日期值("2008/04/21 09:29:48");
		date2 = 日期时间操作.日期值("2008/04/21 09:40:48");
		assertEquals(日期时间操作.取时间间隔(date1, date2, 日期时间操作.日期_分), 11);

		// 测试秒
		date1 = 日期时间操作.日期值("2008/04/21 09:29:48");
		date2 = 日期时间操作.日期值("2008/04/21 09:40:48");
		assertEquals(日期时间操作.取时间间隔(date1, date2, 日期时间操作.日期_秒), 11 * 60);
	}

	/**
	 * 测试{@link 日期时间操作#时间戳到时间(long)}。
	 */
	public void testTimestampToTimeZoneData() {
		long timestamp = 1580673335443L;
		Calendar calendar = 日期时间操作.时间戳到时间(timestamp);
		// 感觉这测试意义不大！
		assertEquals(calendar.getTimeInMillis(), timestamp);
	}

	/**
	 * 测试{@link 日期时间操作#到时间戳(Calendar)}。
	 */
	public void testToTimestamp() {
		Calendar date = 日期时间操作.日期值("2008/04/21 09:29:48");
		assertEquals(日期时间操作.到时间戳(date), date.getTimeInMillis());
	}

	/**
	 * 测试{@link 日期时间操作#取时区()}。
	 */
	public void testGetTimestamp() {
		assertNotNull(日期时间操作.取时区());
	}

	/**
	 * 测试{@link 日期时间操作#取时区时间(String)}。
	 */
	public void testTimeZoneNow() {
		// 标准时间（格林尼治）
		assertNotNull(日期时间操作.取时区时间(日期时间操作.时区_格林尼治));
	}

	/**
	 * 测试{@link 日期时间操作#取时间时区(Calendar)}。
	 */
	public void testGetDateTimestamp() {
		Calendar date = 日期时间操作.取现行时间();
		assertEquals(日期时间操作.取时间时区(date), date.getTimeZone().getID());
	}

	/**
	 * 测试{@link 日期时间操作#置时间时区(Calendar, String)}。
	 */
	public void testsetDateTimestamp() {
		Calendar date = 日期时间操作.取现行时间();

		日期时间操作.置时间时区(date, 日期时间操作.时区_格林尼治);
		assertEquals(date.getTimeZone().getID(), 日期时间操作.时区_格林尼治);
	}

}

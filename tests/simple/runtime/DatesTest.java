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

import simple.runtime.annotations.SimpleFunction;

import java.util.TimeZone;

import java.util.Locale;

import junit.framework.TestCase;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Tests for {@link 日期时间操作}.
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public class DatesTest extends TestCase {

	private Locale previousLocale;
	private TimeZone previousTimeZone;

	/**
	 * 创建指定名称的运行库用例。
	 *
	 * @param testName 用例名称
	 */
	public DatesTest(String testName) {
		super(testName);
	}

	/**
	 * 为每个日期用例固定区域与时区，并保存宿主状态。
	 */
	@Override
	protected void setUp() {
		previousLocale = Locale.getDefault();
		previousTimeZone = TimeZone.getDefault();
		Locale.setDefault(Locale.US);
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	/**
	 * 恢复宿主区域与时区，避免影响其它测试。
	 */
	@Override
	protected void tearDown() {
		Locale.setDefault(previousLocale);
		TimeZone.setDefault(previousTimeZone);
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
		// 名称应随实际区域变化，不固定为运行测试时的宿主语言。
		Locale.setDefault(Locale.US);
		assertEquals("April", 日期时间操作.取月份名(date));
		Locale.setDefault(Locale.SIMPLIFIED_CHINESE);
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
		// 同一日期分别验证英文和中文区域，夹具负责恢复全局状态。
		Locale.setDefault(Locale.US);
		assertEquals("Monday", 日期时间操作.取星期名(date));
		Locale.setDefault(Locale.SIMPLIFIED_CHINESE);
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


		// 使用独立日历定义上海时刻，避免把默认时区和显式上海时区混为一谈。
		Calendar date = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
		date.clear();
		date.set(2008, Calendar.APRIL, 21, 9, 29, 48);
		assertEquals(日期时间操作.文本到时间("2008/04/21 09:29:48", "yyyy/MM/dd HH:mm:ss", 日期时间操作.时区_上海).getTimeInMillis(), date.getTimeInMillis());
		assertEquals(日期时间操作.文本到时间("2008-04-21 09:29:48", "yyyy-MM-dd HH:mm:ss", 日期时间操作.时区_上海).getTimeInMillis(), date.getTimeInMillis());
		assertEquals(日期时间操作.文本到时间("2008.04.21 09:29:48", "yyyy.MM.dd HH:mm:ss", 日期时间操作.时区_上海).getTimeInMillis(), date.getTimeInMillis());
		assertEquals(日期时间操作.文本到时间("2008年04月21日09时29分48秒", "yyyy年MM月dd日HH时mm分ss秒", 日期时间操作.时区_上海).getTimeInMillis(), date.getTimeInMillis());
		assertEquals(日期时间操作.文本到时间("20080421092948", "yyyyMMddHHmmss", 日期时间操作.时区_上海).getTimeInMillis(), date.getTimeInMillis());

		// 只输入日期时，时刻应为上海当地零点。
		Calendar time = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
		time.clear();
		time.set(2008, Calendar.APRIL, 21, 0, 0, 0);
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

	/**
	 * 默认时区在调用时读取，不能缓存初次创建的时区。
	 */
	public void testParsingUsesCurrentDefaultTimeZone() {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
		Calendar shanghai = 日期时间操作.文本到时间("2026-10-18 12:30:45", "yyyy-MM-dd HH:mm:ss");
		assertEquals("Asia/Shanghai", shanghai.getTimeZone().getID());
		assertEquals(utc("2026-10-18 04:30:45").getTimeInMillis(), shanghai.getTimeInMillis());

		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		Calendar utc = 日期时间操作.文本到时间("2026-10-18 12:30:45", "yyyy-MM-dd HH:mm:ss");
		assertEquals("UTC", utc.getTimeZone().getID());
		assertEquals(8L * 60 * 60 * 1000, utc.getTimeInMillis() - shanghai.getTimeInMillis());
	}

	/**
	 * 无参数日期和时间使用固定分隔符，显式分隔符重载继续可用。
	 */
	public void testDefaultDateAndTimeSeparators() {
		assertTrue(日期时间操作.取现行日期文本().matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"));
		assertTrue(日期时间操作.取现行时间文本().matches("[0-9]{2}:[0-9]{2}:[0-9]{2}"));
		assertTrue(日期时间操作.取现行日期文本("/").matches("[0-9]{4}/[0-9]{2}/[0-9]{2}"));
		assertTrue(日期时间操作.取现行时间文本("").matches("[0-9]{6}"));
	}

	/**
	 * 运行库加载器依赖导出注解，不能仅有 public Java 方法。
	 */
	public void testIntervalExported() throws Exception {
		assertTrue(日期时间操作.class.getMethod("取时间间隔",
			Calendar.class, Calendar.class, int.class).isAnnotationPresent(SimpleFunction.class));
	}

	/**
	 * 保持月份从零、星期天从一开始的既有编号。
	 */
	public void testMonthAndWeekdayConventions() {
		assertEquals(0, 日期时间操作.取月份(utc("2026-01-01 00:00:00")));
		assertEquals(11, 日期时间操作.取月份(utc("2026-12-31 00:00:00")));
		assertEquals(日期时间操作.星期_星期天, 日期时间操作.取星期(utc("2026-10-18 00:00:00")));
	}

	/**
	 * 星期按七天折算，负间隔向零截断且不修改输入。
	 */
	public void testWeekIntervals() {
		Calendar begin = utc("2026-10-01 00:00:00");
		Calendar end = utc("2026-10-15 00:00:00");
		long beginBefore = begin.getTimeInMillis();
		long endBefore = end.getTimeInMillis();

		assertEquals(2L, 日期时间操作.取时间间隔(begin, end, 日期时间操作.日期_星期));
		assertEquals(-2L, 日期时间操作.取时间间隔(end, begin, 日期时间操作.日期_星期));
		assertEquals(0L, 日期时间操作.取时间间隔(begin, utc("2026-09-25 00:00:00"), 日期时间操作.日期_星期));
		assertEquals(beginBefore, begin.getTimeInMillis());
		assertEquals(endBefore, end.getTimeInMillis());
	}

	/**
	 * 原有年月折算保持固定天数，不变为日历字段差。
	 */
	public void testFixedYearAndMonthIntervals() {
		Calendar begin = utc("2026-01-31 00:00:00");
		assertEquals(0L, 日期时间操作.取时间间隔(begin, utc("2026-02-28 00:00:00"), 日期时间操作.日期_月));
		assertEquals(1L, 日期时间操作.取时间间隔(begin, utc("2026-03-02 00:00:00"), 日期时间操作.日期_月));
		assertEquals(1L, 日期时间操作.取时间间隔(utc("2024-01-01 00:00:00"),
			utc("2024-12-31 00:00:00"), 日期时间操作.日期_年));
	}

	/**
	 * 夏令时日历上的一天可能只有二十三小时，天间隔仍按二十四小时折算。
	 */
	public void testDaylightSavingElapsedTime() {
		Calendar begin = 日期时间操作.文本到时间("2026-03-08 00:00:00", "yyyy-MM-dd HH:mm:ss", "America/New_York");
		Calendar end = 日期时间操作.文本到时间("2026-03-09 00:00:00", "yyyy-MM-dd HH:mm:ss", "America/New_York");
		assertEquals(23L, 日期时间操作.取时间间隔(begin, end, 日期时间操作.日期_时));
		assertEquals(0L, 日期时间操作.取时间间隔(begin, end, 日期时间操作.日期_天));
	}

	/**
	 * 不支持的间隔单位继续报错，不隐式当成毫秒。
	 */
	public void testUnsupportedInterval() {
		try {
			日期时间操作.取时间间隔(utc("2026-01-01 00:00:00"), utc("2026-01-02 00:00:00"), Calendar.MILLISECOND);
			fail("应拒绝未支持的间隔单位。");
		} catch (IllegalArgumentException expected) {
			// 保持已有异常边界。
		}
	}

	/**
	 * 更换对象时区只改变显示，不改变实际时间点。
	 */
	public void testTimeZoneKeepsTimestamp() {
		Calendar date = utc("2026-10-16 04:30:45");
		long timestamp = date.getTimeInMillis();
		日期时间操作.置时间时区(date, "Asia/Shanghai");
		assertEquals(timestamp, date.getTimeInMillis());
		assertEquals("2026-10-16 12:30:45", 日期时间操作.格式化时间(date, "yyyy-MM-dd HH:mm:ss"));
	}

	/**
	 * 越界日期及尾随文本继续按宽松模式解析。
	 */
	public void testLenientParsingPreserved() {
		Calendar date = 日期时间操作.文本到时间("2026-02-30 trailing", "yyyy-MM-dd", "UTC");
		assertEquals("2026-03-02", 日期时间操作.格式化时间(date, "yyyy-MM-dd"));
		assertEquals("2026-10-16", 日期时间操作.格式化时间(日期时间操作.日期值("2026-10-16 trailing"), "yyyy-MM-dd"));
	}

	/**
	 * 数值时间戳始终按毫秒处理，零和负数也可往返。
	 */
	public void testNumericTimestampUnitPreserved() {
		for (long timestamp : new long[] {0L, -1L, 1609459200123L}) {
			assertEquals(timestamp, 日期时间操作.到时间戳(日期时间操作.时间戳到时间(timestamp)));
		}
	}

	/**
	 * 十位秒与十三位毫秒文本可表示同一时刻，并按设备默认时区显示。
	 */
	public void testTimestampTextUnitsAndZone() {
		assertEquals("2021-01-01 00:00:00", 日期时间操作.时间戳到时间文本("1609459200"));
		assertEquals("2021-01-01 00:00:00", 日期时间操作.时间戳到时间文本("1609459200123"));
		assertEquals("1970-01-01 00:00:00", 日期时间操作.时间戳到时间文本("0000000000"));

		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
		assertEquals("2021-01-01 08:00:00", 日期时间操作.时间戳到时间文本("1609459200"));
	}

	/**
	 * 空值和不支持的长度返回空文本；正确长度但含非数字时抛出数值异常。
	 */
	public void testTimestampTextFailures() {
		assertEquals("", 日期时间操作.时间戳到时间文本(null));
		assertEquals("", 日期时间操作.时间戳到时间文本(""));
		assertEquals("", 日期时间操作.时间戳到时间文本("123"));
		try {
			日期时间操作.时间戳到时间文本("abcdefghij");
			fail("应拒绝不能解析的长整数。");
		} catch (NumberFormatException expected) {
			// 与便捷接口的数值解析约定一致。
		}
	}

	/**
	 * 分隔符作为普通文本处理，支持空文本及日期模式中的特殊字符。
	 */
	public void testCurrentTextSeparators() {
		Calendar before = 日期时间操作.取现行时间();
		String date = 日期时间操作.取现行日期文本("-");
		Calendar after = 日期时间操作.取现行时间();
		assertTrue(date.equals(日期时间操作.格式化时间(before, "yyyy-MM-dd"))
			|| date.equals(日期时间操作.格式化时间(after, "yyyy-MM-dd")));

		assertTrue(日期时间操作.取现行日期文本("").matches("[0-9]{8}"));
		assertTrue(日期时间操作.取现行日期文本("'M").matches("[0-9]{4}'M[0-9]{2}'M[0-9]{2}"));
		assertTrue(日期时间操作.取现行时间文本(":").matches("[0-9]{2}:[0-9]{2}:[0-9]{2}"));
		assertTrue(日期时间操作.取现行时间文本("").matches("[0-9]{6}"));
		assertTrue(日期时间操作.取现行时间文本("H'").matches("[0-9]{2}H'[0-9]{2}H'[0-9]{2}"));
	}

	/**
	 * 默认格式使用对象自身时区，不能偷偷切换到设备默认时区。
	 */
	public void testDateFormatKeepsObjectTimeZone() {
		Calendar value = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
		value.setTimeInMillis(0);
		assertEquals("1970-01-01 08:00:00", 日期时间操作.格式化时间(value));
		value.setTimeZone(TimeZone.getTimeZone("UTC"));
		assertEquals("1970-01-01 00:00:00", 日期时间操作.格式化时间(value));
		assertEquals("1970", 日期时间操作.格式化时间(value, "yyyy"));
	}

	/**
	 * 以明确的时区创建固定测试向量。
	 *
	 * @param text 固定日期时间文本。
	 * @return UTC 日期时间。
	 */
	private static Calendar utc(String text) {
		return 日期时间操作.文本到时间(text, "yyyy-MM-dd HH:mm:ss", "UTC");
	}
}

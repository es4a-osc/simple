package simple.runtime;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import junit.framework.TestCase;

import simple.runtime.annotations.SimpleFunction;

/**
 * 验证时间间隔导出、便捷文本接口及既有日期与时区约定。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class DatesCompatibilityTest extends TestCase {

	private Locale previousLocale;
	private TimeZone previousTimeZone;

	@Override
	protected void setUp() {
		previousLocale = Locale.getDefault();
		previousTimeZone = TimeZone.getDefault();
		Locale.setDefault(Locale.US);
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	@Override
	protected void tearDown() {
		Locale.setDefault(previousLocale);
		TimeZone.setDefault(previousTimeZone);
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
	 * 以明确的时区创建固定测试向量。
	 *
	 * @param text 固定日期时间文本。
	 * @return UTC 日期时间。
	 */
	private static Calendar utc(String text) {
		return 日期时间操作.文本到时间(text, "yyyy-MM-dd HH:mm:ss", "UTC");
	}
}

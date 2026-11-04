package simple.runtime.helpers;

import java.time.Instant;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import junit.framework.TestCase;

import simple.runtime.errors.转换错误;

/**
 * 日期、UTC 文本与 Unix 毫秒时间戳转换的纯 Java 回归测试。
 *
 * @author 树先生 xhwsd@qq.com
 */
public class ConvHelpersTest extends TestCase {

	/**
	 * 全范围长整数时间戳往返不丢失毫秒，并使用 UTC 时区。
	 */
	public void testTimestampRoundTrip() {
		long[] timestamps = {Long.MIN_VALUE, -1L, 0L, 1L, 948585600123L, Long.MAX_VALUE};
		for (long timestamp : timestamps) {
			Calendar date = ConvHelpers.long2date(timestamp);
			assertEquals(timestamp, date.getTimeInMillis());
			assertEquals(timestamp, ConvHelpers.date2long(date));
			assertEquals("UTC", date.getTimeZone().getID());
		}
	}

	/**
	 * 输出固定三位毫秒的 UTC 文本，保留原对象的时间戳和时区。
	 */
	public void testDateFormattingPreservesInput() {
		Calendar date = new GregorianCalendar(TimeZone.getTimeZone("Asia/Shanghai"), Locale.ROOT);
		long timestamp = Instant.parse("2000-01-23T00:09:10.123Z").toEpochMilli();
		date.setTimeInMillis(timestamp);

		assertEquals("2000-01-23T00:09:10.123Z", ConvHelpers.date2string(date));
		assertEquals(timestamp, date.getTimeInMillis());
		assertEquals("Asia/Shanghai", date.getTimeZone().getID());
	}

	/**
	 * 纪元前后的一毫秒与零毫秒按固定精度输出。
	 */
	public void testEpochTextPrecision() {
		assertEquals("1969-12-31T23:59:59.999Z", ConvHelpers.date2string(ConvHelpers.long2date(-1L)));
		assertEquals("1970-01-01T00:00:00.000Z", ConvHelpers.date2string(ConvHelpers.long2date(0L)));
		assertEquals("1970-01-01T00:00:00.001Z", ConvHelpers.date2string(ConvHelpers.long2date(1L)));
	}

	/**
	 * 纯日期使用 UTC 零点，无时区的日期时间也按 UTC 解释。
	 */
	public void testDateOnlyAndUnspecifiedZone() {
		assertEquals("2000-01-23T00:00:00.000Z", ConvHelpers.date2string(ConvHelpers.string2date("2000-01-23")));
		assertEquals("2000-01-23T12:34:56.000Z",
				ConvHelpers.date2string(ConvHelpers.string2date("2000-01-23T12:34:56")));
		assertEquals("2000-01-23T12:34:56.789Z",
				ConvHelpers.date2string(ConvHelpers.string2date("2000-01-23T12:34:56.789")));
	}

	/**
	 * 正负时区偏移均转换到同一 UTC 时间点。
	 */
	public void testOffsetDateTimeParsing() {
		long expected = Instant.parse("2000-01-23T00:00:00.123Z").toEpochMilli();
		String[] values = {
			"2000-01-23T00:00:00.123Z",
			"2000-01-23T08:00:00.123+08:00",
			"2000-01-22T18:30:00.123-05:30"
		};
		for (String value : values) {
			Calendar date = ConvHelpers.string2date(value);
			assertEquals(expected, ConvHelpers.date2long(date));
			assertEquals("UTC", date.getTimeZone().getID());
		}
		assertEquals(expected - 123L, ConvHelpers.date2long(ConvHelpers.string2date("2000-01-23T08:00:00+08:00")));
	}

	/**
	 * 一至三位小数秒分别代表十分之一秒、百分之一秒和毫秒。
	 */
	public void testFractionalSeconds() {
		assertEquals(100L, ConvHelpers.date2long(ConvHelpers.string2date("1970-01-01T00:00:00.1Z")));
		assertEquals(120L, ConvHelpers.date2long(ConvHelpers.string2date("1970-01-01T00:00:00.12Z")));
		assertEquals(123L, ConvHelpers.date2long(ConvHelpers.string2date("1970-01-01T00:00:00.123Z")));
	}

	/**
	 * 闰日与历史日期采用同一公历规则，避开默认历法切换缺口。
	 */
	public void testGregorianDates() {
		String[] values = {"0001-01-01", "1500-03-01", "1582-10-10", "2000-02-29", "9999-12-31"};
		for (String value : values) {
			long expected = Instant.parse(value + "T00:00:00Z").toEpochMilli();
			Calendar date = ConvHelpers.string2date(value);
			assertEquals(expected, ConvHelpers.date2long(date));
			assertEquals(value + "T00:00:00.000Z", ConvHelpers.date2string(date));
		}
	}

	/**
	 * 更换设备默认语言和时区后，转换结果仍保持拉丁数字和 UTC。
	 */
	public void testLocaleAndTimeZoneIndependence() {
		Locale originalLocale = Locale.getDefault();
		TimeZone originalZone = TimeZone.getDefault();
		try {
			Locale.setDefault(new Locale("th", "TH"));
			TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Apia"));
			Calendar date = Calendar.getInstance();
			date.setTimeInMillis(0L);
			assertEquals("1970-01-01T00:00:00.000Z", ConvHelpers.date2string(date));
			assertEquals(0L, ConvHelpers.date2long(ConvHelpers.string2date("1970-01-01")));
			assertEquals("2011-12-30T00:00:00.000Z", ConvHelpers.date2string(ConvHelpers.string2date("2011-12-30")));
		} finally {
			Locale.setDefault(originalLocale);
			TimeZone.setDefault(originalZone);
		}
	}

	/**
	 * 日期无效、时区无效、尾随内容及超过毫秒的精度均被拒绝。
	 */
	public void testInvalidDateText() {
		String[] values = {
			null, "", "not-a-date", "0000-01-01", "1900-02-29", "2001-02-29", "2000-13-01",
			"2000-01-23T24:00:00Z", "2000-01-23T00:60:00Z", "2000-01-23T00:00:60Z",
			"2000-01-23T00:00:00.1234Z", "2000-01-23T00:00:00.Z",
			"2000-01-23T00:00:00+25:00", "2000-01-23suffix", "2000-01-23T00:00:00Zsuffix"
		};
		for (String value : values) {
			try {
				ConvHelpers.string2date(value);
				fail("应拒绝无效日期文本：" + value);
			} catch (转换错误 expected) {
				// 无效输入统一报告转换失败。
			}
		}
	}

	/**
	 * 空日期不能被当成纪元或其它有效日期。
	 */
	public void testNullDate() {
		try {
			ConvHelpers.date2long(null);
			fail("空日期应转换失败");
		} catch (转换错误 expected) {
			// 空引用不能产生有效时间戳。
		}
		try {
			ConvHelpers.date2string(null);
			fail("空日期应转换失败");
		} catch (转换错误 expected) {
			// 空引用不能产生有效日期文本。
		}
	}

	/**
	 * 固定四位公元年份格式无法表示的时间点明确转换失败。
	 */
	public void testTextYearRange() {
		long[] timestamps = {Long.MIN_VALUE, Long.MAX_VALUE};
		for (long timestamp : timestamps) {
			try {
				ConvHelpers.date2string(ConvHelpers.long2date(timestamp));
				fail("超出四位公元年份的时间点应转换失败");
			} catch (转换错误 expected) {
				// 时间戳转换仍支持完整 long 范围，文本格式具有明确年份范围。
			}
		}
	}
}

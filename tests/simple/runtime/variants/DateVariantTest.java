package simple.runtime.variants;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import junit.framework.TestCase;

import simple.runtime.errors.转换错误;
import simple.runtime.数据转换;

/**
 * 日期变体的日期、UTC 文本和毫秒时间戳转换测试。
 *
 * @author 树先生 xhwsd@qq.com
 */
public class DateVariantTest extends TestCase {

	/**
	 * 日期变体按同一时间点输出 UTC 文本和长整数，保留输入对象。
	 */
	public void testTimestampAndUtcText() {
		Calendar date = new GregorianCalendar(TimeZone.getTimeZone("Asia/Shanghai"), Locale.ROOT);
		date.setTimeInMillis(123L);
		Variant value = DateVariant.getDateVariant(date);

		assertEquals(123L, value.getLong());
		assertEquals("1970-01-01T00:00:00.123Z", value.getString());
		assertSame(date, value.getDate());
		assertEquals(123L, date.getTimeInMillis());
		assertEquals("Asia/Shanghai", date.getTimeZone().getID());
	}

	/**
	 * 公开的数据转换入口使用新增的日期变体转换。
	 */
	public void testExportedConversions() {
		Calendar date = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ROOT);
		date.setTimeInMillis(1234567890123L);
		Variant value = DateVariant.getDateVariant(date);

		assertEquals(1234567890123L, 数据转换.到长整数(value));
		assertEquals("2009-02-13T23:31:30.123Z", 数据转换.到文本(value));
	}

	/**
	 * 日期比较继续按时间点进行，不受原始时区影响。
	 */
	public void testComparison() {
		Calendar left = new GregorianCalendar(TimeZone.getTimeZone("Asia/Shanghai"), Locale.ROOT);
		Calendar right = new GregorianCalendar(TimeZone.getTimeZone("America/New_York"), Locale.ROOT);
		left.setTimeInMillis(123L);
		right.setTimeInMillis(123L);
		Variant value = DateVariant.getDateVariant(left);

		assertTrue(value.identical(DateVariant.getDateVariant(right)));
		right.setTimeInMillis(124L);
		assertTrue(value.cmp(DateVariant.getDateVariant(right)) < 0);
	}

	/**
	 * 未支持的整数转换仍报告失败，避免截断毫秒时间戳。
	 */
	public void testIntegerConversionRemainsUnsupported() {
		Calendar date = new GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.ROOT);
		date.setTimeInMillis(0L);
		try {
			DateVariant.getDateVariant(date).getInteger();
			fail("日期变体不应隐式截断到32位整数");
		} catch (转换错误 expected) {
			// 时间戳使用长整数型。
		}
	}

	/**
	 * 空日期仍可按日期读取，转文本或时间戳时统一失败。
	 */
	public void testNullDate() {
		Variant value = DateVariant.getDateVariant(null);
		assertNull(value.getDate());
		try {
			value.getLong();
			fail("空日期不应产生时间戳");
		} catch (转换错误 expected) {
			// 沿用辅助转换方法的空值约定。
		}
		try {
			value.getString();
			fail("空日期不应产生日期文本");
		} catch (转换错误 expected) {
			// 沿用辅助转换方法的空值约定。
		}
	}
}

package simple.runtime.variants;

import java.util.Calendar;

import junit.framework.TestCase;

/**
 * 长整数变体的毫秒时间戳与日期转换测试。
 *
 * @author 树先生 xhwsd@qq.com
 */
public class LongVariantTest extends TestCase {

	/**
	 * 完整长整数范围按 Unix 毫秒时间戳转换，往返不丢失精度。
	 */
	public void testDateConversion() {
		long[] values = {Long.MIN_VALUE, -1L, 0L, 1L, 1234567890123L, Long.MAX_VALUE};
		for (long value : values) {
			Calendar date = LongVariant.getLongVariant(value).getDate();
			assertEquals(value, date.getTimeInMillis());
			assertEquals("UTC", date.getTimeZone().getID());
			assertEquals(value, DateVariant.getDateVariant(date).getLong());
		}
	}

	/**
	 * 每次日期转换返回独立对象，修改结果不影响长整数变体。
	 */
	public void testIndependentDateResults() {
		Variant value = LongVariant.getLongVariant(123L);
		Calendar first = value.getDate();
		first.setTimeInMillis(456L);
		Calendar second = value.getDate();

		assertNotSame(first, second);
		assertEquals(123L, second.getTimeInMillis());
		assertEquals(123L, value.getLong());
	}

	/**
	 * 原有长整数转文本仍输出十进制数值。
	 */
	public void testExistingStringConversion() {
		assertEquals("1234567890123", LongVariant.getLongVariant(1234567890123L).getString());
		assertEquals("-1", LongVariant.getLongVariant(-1L).getString());
	}
}

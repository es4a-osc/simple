package simple.runtime.variants;

import java.util.Calendar;

import junit.framework.TestCase;

import simple.runtime.errors.转换错误;

/**
 * 文本变体的日期解析与原有标量转换测试。
 *
 * @author 树先生 xhwsd@qq.com
 */
public class StringVariantTest extends TestCase {

	/**
	 * 纯日期解析为 UTC 零点，并可通过日期变体输出规范文本。
	 */
	public void testDateOnlyConversion() {
		Variant value = StringVariant.getStringVariant("2000-01-23");
		Calendar date = value.getDate();

		assertEquals("UTC", date.getTimeZone().getID());
		assertEquals("2000-01-23T00:00:00.000Z", DateVariant.getDateVariant(date).getString());
		assertEquals("2000-01-23", value.getString());
	}

	/**
	 * 带偏移和小数秒的文本按同一毫秒时间点转换。
	 */
	public void testOffsetAndFractionConversion() {
		String[] values = {"1970-01-01T00:00:00.123Z", "1970-01-01T08:00:00.123+08:00"};
		for (String value : values) {
			Calendar date = StringVariant.getStringVariant(value).getDate();
			assertEquals(123L, DateVariant.getDateVariant(date).getLong());
		}
		assertEquals(100L, StringVariant.getStringVariant("1970-01-01T00:00:00.1Z").getDate().getTimeInMillis());
	}

	/**
	 * 无效日期与数值文本不会被误当成有效日期时间。
	 */
	public void testInvalidDateConversion() {
		String[] values = {null, "", "0", "2001-02-29", "2000-01-23suffix"};
		for (String value : values) {
			try {
				StringVariant.getStringVariant(value).getDate();
				fail("应拒绝无效日期文本：" + value);
			} catch (转换错误 expected) {
				// 与已有日期解析辅助方法保持同一失败约定。
			}
		}
	}

	/**
	 * 原有文本、整数和长整数转换继续使用原规则。
	 */
	public void testExistingConversions() {
		Variant value = StringVariant.getStringVariant("42");
		assertEquals("42", value.getString());
		assertEquals(42, value.getInteger());
		assertEquals(42L, value.getLong());
	}
}

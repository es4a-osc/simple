package simple.runtime;

import java.util.Arrays;

import junit.framework.TestCase;

/**
 * 验证高精度文本运算、舍入规则、余数符号及异常边界。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class BigNumbersTest extends TestCase {

	/**
	 * 十进制小数相加保持精确，不经过 double。
	 */
	public void testExactDecimalAddition() {
		assertEquals("0.3", 大数运算.大数相加("0.1", "0.2"));
		assertEquals("9007199254740994", 大数运算.大数相加("9007199254740993", "1"));
	}

	/**
	 * 运算不受长整数范围限制，连续进位可跨越数百位。
	 */
	public void testLargeIntegerCarry() {
		char[] digits = new char[300];
		Arrays.fill(digits, '9');
		char[] zeros = new char[300];
		Arrays.fill(zeros, '0');
		assertEquals("1" + new String(zeros), 大数运算.大数相加(new String(digits), "1"));
	}

	/**
	 * 保留十进制小数位，不主动规范化为最短文本。
	 */
	public void testScalePreserved() {
		assertEquals("3.50", 大数运算.大数相加("1.20", "2.3"));
		assertEquals("2.400", 大数运算.大数相乘("1.20", "2.0"));
		assertEquals("0.00", 文本操作.科学转普通计数("-0.00"));
	}

	/**
	 * 相减及相乘支持带符号数值和超出长整数范围的结果。
	 */
	public void testSubtractAndMultiply() {
		assertEquals("-0.10", 大数运算.大数相减("0.20", "0.3"));
		assertEquals("10000000000000000000000000000000000000000",
			大数运算.大数相乘("100000000000000000000", "100000000000000000000"));
		assertEquals("-6", 大数运算.大数相乘("-2", "3"));
	}

	/**
	 * HALF_UP 在正负中间值上均向远离零方向舍入。
	 */
	public void testDivisionHalfUp() {
		assertEquals("0.333333", 大数运算.大数相除("1", "3", 6));
		assertEquals("1.01", 大数运算.大数相除("1.005", "1", 2));
		assertEquals("-1.01", 大数运算.大数相除("-1.005", "1", 2));
		assertEquals("0.50", 大数运算.大数相除("1", "2", 2));
	}

	/**
	 * 零及负小数位数保持原有整数、十位和百位舍入含义。
	 */
	public void testDivisionZeroAndNegativeScale() {
		assertEquals("3", 大数运算.大数相除("5", "2", 0));
		assertEquals("1300", 大数运算.大数相除("1250", "1", -2));
		assertEquals("-1300", 大数运算.大数相除("-1250", "1", -2));
	}

	/**
	 * 比较看实际数值，忽略末尾零，支持超过浮点精确范围的相邻整数。
	 */
	public void testNumericComparison() {
		assertEquals(0, 大数运算.大数比较("1.0", "1.00"));
		assertEquals(-1, 大数运算.大数比较("9007199254740992", "9007199254740993"));
		assertEquals(1, 大数运算.大数比较("-0.01", "-0.02"));
	}

	/**
	 * 余数保留被除数符号，而不是统一转为非负数。
	 */
	public void testRemainderSign() {
		assertEquals("-2", 大数运算.大数求余("-5", "3"));
		assertEquals("2", 大数运算.大数求余("5", "-3"));
		assertEquals("0.50", 大数运算.大数求余("5.50", "2.5"));
	}

	/**
	 * 科学计数可用于所有运算，文本展开不带指数。
	 */
	public void testScientificNotation() {
		assertEquals("123000", 文本操作.科学转普通计数("1.23E+5"));
		assertEquals("0.001", 文本操作.科学转普通计数("1E-3"));
		assertEquals("1000.001", 大数运算.大数相加("1E+3", "1E-3"));
	}

	/**
	 * 除零及求余除零保留算术异常。
	 */
	public void testDivisionByZero() {
		try {
			大数运算.大数相除("1", "0.00", 2);
			fail("应拒绝除零。");
		} catch (ArithmeticException expected) {
			// 保持数值运算的错误边界。
		}

		try {
			大数运算.大数求余("1", "0");
			fail("应拒绝求余除零。");
		} catch (ArithmeticException expected) {
			// 余数运算也不得伪装成零。
		}
	}

	/**
	 * 空引用不被隐式当成空文本或零。
	 */
	public void testNullInput() {
		try {
			大数运算.大数相加(null, "1");
			fail("应拒绝空引用。");
		} catch (NullPointerException expected) {
			// 沿用 BigDecimal 文本构造的异常约定。
		}
	}
}

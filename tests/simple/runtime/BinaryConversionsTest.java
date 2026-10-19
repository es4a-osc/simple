package simple.runtime;

import java.util.Arrays;

import junit.framework.TestCase;

import simple.runtime.variants.StringVariant;

/**
 * 核对进制文本、字节序、IEEE 754 位模式及宽松十六进制解析约定。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class BinaryConversionsTest extends TestCase {

	/**
	 * 整数转换使用四字节小端，不混同长整数的大端布局。
	 */
	public void testIntegerLittleEndian() {
		assertTrue(Arrays.equals(new byte[] {0x78, 0x56, 0x34, 0x12},
			数据转换.整数到字节集(0x12345678)));
		assertEquals(0x12345678, 数据转换.字节集到整数(new byte[] {0x78, 0x56, 0x34, 0x12, 0x00}));

		for (int value : new int[] {0, -1, 1, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
			assertEquals(value, 数据转换.字节集到整数(数据转换.整数到字节集(value)));
		}
	}

	/**
	 * 长整数转换使用八字节大端，保留最高位符号。
	 */
	public void testLongBigEndian() {
		assertTrue(Arrays.equals(new byte[] {1, 2, 3, 4, 5, 6, 7, 8},
			数据转换.长整数到字节集(0x0102030405060708L)));

		for (long value : new long[] {0, -1, 1, Long.MIN_VALUE, Long.MAX_VALUE}) {
			assertEquals(value, 数据转换.字节集到长整数(数据转换.长整数到字节集(value)));
		}

		assertEquals(0x0102030405060708L,
			数据转换.字节集到长整数(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 99}));
	}

	/**
	 * 不足固定长度的数组不会越界，也不按可用字节补零解释。
	 */
	public void testShortAndNullArrays() {
		assertEquals(0, 数据转换.字节集到整数(null));
		assertEquals(0L, 数据转换.字节集到长整数(null));
		assertEquals(0.0, 数据转换.字节集到双精度小数(null), 0.0);

		for (int length = 0; length < 8; length++) {
			byte[] bytes = new byte[length];
			Arrays.fill(bytes, (byte) 1);
			if (length < 4) {
				assertEquals(0, 数据转换.字节集到整数(bytes));
			}
			assertEquals(0L, 数据转换.字节集到长整数(bytes));
			assertEquals(0.0, 数据转换.字节集到双精度小数(bytes), 0.0);
		}
	}

	/**
	 * 双精度小数以大端 IEEE 754 表示，不经文本转换。
	 */
	public void testDoubleBitPatterns() {
		assertEquals("3FF0000000000000", 编码转换.字节集到十六进制(数据转换.双精度小数到字节集(1.0)));
		assertEquals("8000000000000000", 编码转换.字节集到十六进制(数据转换.双精度小数到字节集(-0.0)));
		assertEquals("7FF0000000000000", 编码转换.字节集到十六进制(数据转换.双精度小数到字节集(Double.POSITIVE_INFINITY)));
		assertEquals("FFF0000000000000", 编码转换.字节集到十六进制(数据转换.双精度小数到字节集(Double.NEGATIVE_INFINITY)));

		for (double value : new double[] {0.0, -0.0, -123.25, Double.MIN_VALUE, Double.MAX_VALUE,
				Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
			double actual = 数据转换.字节集到双精度小数(数据转换.双精度小数到字节集(value));
			assertEquals(Double.doubleToRawLongBits(value), Double.doubleToRawLongBits(actual));
		}
	}

	/**
	 * 编码规范化 NaN，解码允许 NaN，并忽略第八字节之后的内容。
	 */
	public void testNaNAndTrailingBytes() {
		double nonCanonical = Double.longBitsToDouble(0x7FF8000000000001L);
		assertEquals("7FF8000000000000", 编码转换.字节集到十六进制(数据转换.双精度小数到字节集(nonCanonical)));
		assertTrue(Double.isNaN(数据转换.字节集到双精度小数(编码转换.十六进制到字节集("7FF8000000000001"))));
		assertEquals(1.0, 数据转换.字节集到双精度小数(编码转换.十六进制到字节集("3FF0000000000000FF")), 0.0);
	}

	/**
	 * 所有单字节均可无符号解释，不改变现有有符号类型转换。
	 */
	public void testUnsignedByteRange() {
		for (int value = 0; value < 256; value++) {
			assertEquals(value, 数据转换.取字节无符号值((byte) value));
		}
		assertEquals(-1, 数据转换.到整数(simple.runtime.variants.ByteVariant.getByteVariant((byte) -1)));
	}

	/**
	 * 全部 256 个字节可编码和还原，十六进制文本保留前导零。
	 */
	public void testHexadecimalByteRoundTrip() {
		byte[] bytes = new byte[256];
		for (int index = 0; index < bytes.length; index++) {
			bytes[index] = (byte) index;
		}

		String text = 编码转换.字节集到十六进制(bytes);
		assertEquals(512, text.length());
		assertTrue(text.startsWith("00010203"));
		assertTrue(text.endsWith("FCFDFEFF"));
		assertTrue(Arrays.equals(bytes, 编码转换.十六进制到字节集(text)));
		assertTrue(Arrays.equals(bytes, 编码转换.十六进制到字节集(text.toLowerCase(java.util.Locale.ROOT))));
	}

	/**
	 * 十六进制的空输入保持既有空结果约定。
	 */
	public void testHexadecimalEmptyInputs() {
		assertEquals("", 编码转换.字节集到十六进制(null));
		assertEquals("", 编码转换.字节集到十六进制(new byte[0]));
		assertEquals(0, 编码转换.十六进制到字节集(null).length);
		assertEquals(0, 编码转换.十六进制到字节集("").length);
	}

	/**
	 * 宽松解析不会自动变成格式校验器，奇数末位和非法字符的行为保持稳定。
	 */
	public void testPermissiveHexadecimalParsing() {
		assertTrue(Arrays.equals(new byte[] {0x12, (byte) 0xAB}, 编码转换.十六进制到字节集("12ABC")));
		assertEquals(0, 编码转换.十六进制到字节集("F").length);
		assertTrue(Arrays.equals(new byte[] {0}, 编码转换.十六进制到字节集("GG")));
		assertTrue(Arrays.equals(new byte[] {(byte) 0xFF}, 编码转换.十六进制到字节集("//")));
		assertTrue(Arrays.equals(new byte[] {0}, 编码转换.十六进制到字节集("  ")));
	}

	/**
	 * 已有文本转整数规则不因补充进制转换而改变。
	 */
	public void testExistingTextConversionPreserved() {
		assertEquals(1, 数据转换.到整数(StringVariant.getStringVariant("1.5")));
		assertEquals(0, 数据转换.到整数(StringVariant.getStringVariant("bad")));
		assertEquals("2A", 进制转换.十六进制(simple.runtime.variants.IntegerVariant.getIntegerVariant(42)));
	}
}

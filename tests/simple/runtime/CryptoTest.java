package simple.runtime;

import java.nio.charset.Charset;
import java.util.Arrays;

import junit.framework.TestCase;

/**
 * 验证摘要固定向量、旧加密格式及明确保留的失败返回约定。
 *
 * <p>JVM 用例不调用 Android Base64 编码；涉及编码的完整往返在加密操作示例中验证。</p>
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class CryptoTest extends TestCase {

	/**
	 * MD5 保持大写十六进制输出，空数组也有确定摘要。
	 */
	public void testMd5Vectors() {
		assertEquals("900150983CD24FB0D6963F7D28E17F72", 加密操作.取MD5值(ascii("abc")));
		assertEquals("D41D8CD98F00B204E9800998ECF8427E", 加密操作.取MD5值(new byte[0]));
	}

	/**
	 * 文本接口符合标准 RC4 向量，并能还原十六进制密文。
	 */
	public void testRc4TextVector() {
		assertEquals("BBF316E8D940AF0AD3", 加密操作.RC4加密("Plaintext", "Key"));
		assertEquals("Plaintext", 加密操作.RC4解密("BBF316E8D940AF0AD3", "Key"));
		assertEquals("1021BF0420", 加密操作.RC4加密("pedia", "Wiki"));
	}

	/**
	 * 中文明文和密钥继续使用 GBK，不改为 UTF-8。
	 */
	public void testRc4ChineseEncoding() {
		assertEquals("3BE61CD8", 加密操作.RC4加密("中文", "secret"));
		assertEquals("中文", 加密操作.RC4解密("3BE61CD8", "secret"));
	}

	/**
	 * 字节加密返回十六进制 ASCII；字节解密接收原始密文。
	 */
	public void testRc4ByteFormat() {
		byte[] plain = ascii("Plaintext");
		byte[] encrypted = 加密操作.RC4加密2(plain, "Key");
		assertTrue(Arrays.equals(ascii("BBF316E8D940AF0AD3"), encrypted));
		assertEquals(plain.length * 2, encrypted.length);

		byte[] raw = {(byte) 0xBB, (byte) 0xF3, 0x16, (byte) 0xE8, (byte) 0xD9,
			0x40, (byte) 0xAF, 0x0A, (byte) 0xD3};
		assertTrue(Arrays.equals(plain, 加密操作.RC4解密2(raw, "Key")));
		assertFalse(Arrays.equals(plain, 加密操作.RC4解密2(encrypted, "Key")));
	}

	/**
	 * 字节接口不得改写输入数组，所有字节值都参与处理。
	 */
	public void testRc4InputPreserved() {
		byte[] data = new byte[256];
		for (int index = 0; index < data.length; index++) {
			data[index] = (byte) index;
		}

		byte[] before = data.clone();
		assertEquals(512, 加密操作.RC4加密2(data, "中文密钥").length);
		assertTrue(Arrays.equals(before, data));
		加密操作.RC4解密2(data, "中文密钥");
		assertTrue(Arrays.equals(before, data));
	}

	/**
	 * 空引用按原接口返回空结果，不引入新的公开异常。
	 */
	public void testRc4NullAndEmpty() {
		assertEquals("", 加密操作.RC4加密(null, "Key"));
		assertEquals("", 加密操作.RC4解密(null, "Key"));
		assertEquals("", 加密操作.RC4加密("", "Key"));
		assertEquals(0, 加密操作.RC4加密2(null, "Key").length);
		assertEquals(0, 加密操作.RC4解密2(null, "Key").length);
	}

	/**
	 * 文本解密保留忽略奇数长度末位的处理。
	 */
	public void testRc4OddHexTail() {
		assertEquals("Plaintext", 加密操作.RC4解密("BBF316E8D940AF0AD3F", "Key"));
	}

	/**
	 * 已有 DES 密文继续按固定 IV 和 GBK 解密。
	 */
	public void testDesDecodeVectors() {
		assertEquals("abc", 加密操作.DES解密("U+H0s6Ybcig=", "12345678"));
		assertEquals("中文", 加密操作.DES解密("E9SrmGhEaTQ=", "12345678"));
		assertEquals("", 加密操作.DES解密("U+H0s6Ybcig=", "short"));
		assertEquals("", 加密操作.DES解密(null, "12345678"));
	}

	/**
	 * 固定的既有 Authcode 密文可按 UTF-8 还原中文。
	 */
	public void testAuthcodeDecodeVector() {
		assertEquals("中文", 加密操作.Authcode解密(
			"44ueDg231UQZwCuvK7mQhVaqK2Od7lZO94UrhihxQYrBiz0=", "secret"));
	}

	/**
	 * 校验失败和解析异常的返回值保持区别，不把“2”统一改成空文本。
	 */
	public void testAuthcodeFailureConventions() {
		assertEquals("2", 加密操作.Authcode解密("", "secret"));
		assertEquals("2", 加密操作.Authcode解密("abcdAAAA", "secret"));
		assertEquals("", 加密操作.Authcode解密("abcdA", "secret"));
		assertEquals("", 加密操作.Authcode解密(null, "secret"));
	}

	/**
	 * 测试向量使用确定的 ASCII 字节。
	 *
	 * @param text 向量文本。
	 * @return ASCII 字节数组。
	 */
	private static byte[] ascii(String text) {
		return text.getBytes(Charset.forName("US-ASCII"));
	}
}

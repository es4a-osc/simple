package simple.runtime;

import junit.framework.TestCase;

/**
 * 验证六位 RGB 与八位 ARGB 颜色文本解析，避免有效格式误入失败分支。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class ColorTest extends TestCase {

	/**
	 * 六位格式补齐不透明通道，并返回完整的 ARGB 值。
	 */
	public void testRgbDefaultsToOpaque() {
		assertEquals(0xFF336699, 颜色操作.取颜色("#336699"));
		assertEquals(0xFF000000, 颜色操作.取颜色("#000000"));
		assertEquals(0xFFFFFFFF, 颜色操作.取颜色("#FFFFFF"));
	}

	/**
	 * 八位格式保留全透明、半透明和不透明通道。
	 */
	public void testArgbPreservesAlpha() {
		assertEquals(0x00336699, 颜色操作.取颜色("#00336699"));
		assertEquals(0x80336699, 颜色操作.取颜色("#80336699"));
		assertEquals(0xFF336699, 颜色操作.取颜色("#FF336699"));
	}

	/**
	 * 十六进制字母大小写不影响解析结果。
	 */
	public void testHexLetterCase() {
		assertEquals(0xFFAABBCC, 颜色操作.取颜色("#aAbBcC"));
		assertEquals(0x80AABBCC, 颜色操作.取颜色("#80aAbBcC"));
	}

	/**
	 * 同一 RGB 值与显式不透明 ARGB 值完全一致。
	 */
	public void testEquivalentOpaqueFormats() {
		assertEquals(颜色操作.取颜色("#FF336699"), 颜色操作.取颜色("#336699"));
	}

	/**
	 * 不支持的格式继续抛出参数异常，不转为零或透明色。
	 */
	public void testUnsupportedFormats() {
		assertInvalid("336699");
		assertInvalid("#369");
		assertInvalid("#1234567");
		assertInvalid("#123456789");
		assertInvalid("#GG6699");
	}

	/**
	 * 核对无效输入的既有异常约定。
	 *
	 * @param text 颜色文本。
	 */
	private static void assertInvalid(String text) {
		try {
			颜色操作.取颜色(text);
			fail("应拒绝颜色文本：" + text);
		} catch (IllegalArgumentException expected) {
			// 数值解析错误也属于参数异常，不约束具体异常消息。
		}
	}
}

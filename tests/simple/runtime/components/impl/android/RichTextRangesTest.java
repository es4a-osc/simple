package simple.runtime.components.impl.android;

import java.util.Arrays;

import junit.framework.TestCase;

import simple.runtime.errors.索引超出界限错误;

/**
 * 验证局部格式切分、半开索引及段落边界，避免覆盖区间外格式。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class RichTextRangesTest extends TestCase {

	/**
	 * 空区间和末尾位置合法，反向、负数及越界均拒绝。
	 */
	public void testRangeValidation() {
		RichTextRanges.check(0, 0, 0);
		RichTextRanges.check(6, 0, 6);
		RichTextRanges.check(6, 6, 6);
		invalid(6, -1, 2);
		invalid(6, 3, 2);
		invalid(6, 0, 7);
	}

	/**
	 * 清除中间样式时保留左右两段，完全覆盖时不留下片段。
	 */
	public void testPartialRemovalKeepsOutsideStyles() {
		parts(new int[][]{{0, 2}, {4, 6}}, RichTextRanges.remaining(0, 6, 2, 4));
		parts(new int[][]{{0, 2}}, RichTextRanges.remaining(0, 6, 2, 8));
		parts(new int[][]{{4, 6}}, RichTextRanges.remaining(0, 6, 0, 4));
		parts(new int[0][], RichTextRanges.remaining(2, 4, 0, 6));
		parts(new int[][]{{2, 4}}, RichTextRanges.remaining(2, 4, 4, 6));
		parts(new int[][]{{2, 4}}, RichTextRanges.remaining(2, 4, 0, 2));
		parts(new int[][]{{0, 6}}, RichTextRanges.remaining(0, 6, 3, 3));
	}

	/**
	 * 枚举相交、包含和相邻区间，确认保留片段恰好覆盖未操作字符。
	 */
	public void testAllRemainingFragmentsPreserveExactlyUnchangedCharacters() {
		for (int a = 0; a < 8; a++) {
			for (int b = a + 1; b <= 8; b++) {
				for (int start = 0; start <= 8; start++) {
					for (int end = start; end <= 8; end++) {
						int[][] remaining = RichTextRanges.remaining(a, b, start, end);
						for (int index = 0; index < 8; index++) {
							int coverage = 0;
							for (int[] part : remaining) {
								assertTrue(part[0] < part[1]);
								if (part[0] <= index && index < part[1]) {
									coverage++;
								}
							}
							boolean expected = a <= index && index < b
								&& !(start <= index && index < end);
							assertEquals(expected ? 1 : 0, coverage);
						}
					}
				}
			}
		}
	}

	/**
	 * 项目符扩展为完整段落，排除终点所在的下一段不受影响。
	 */
	public void testParagraphRanges() {
		parts(new int[][]{{0, 4}}, RichTextRanges.paragraphs("abc\ndef\nghi", 1, 4));
		parts(new int[][]{{0, 4}, {4, 8}}, RichTextRanges.paragraphs("abc\ndef\nghi", 2, 5));
		parts(new int[][]{{8, 11}}, RichTextRanges.paragraphs("abc\ndef\nghi", 8, 11));
		parts(new int[][]{{0, 1}, {1, 2}}, RichTextRanges.paragraphs("\n\n", 0, 2));
		parts(new int[][]{{0, 4}}, RichTextRanges.paragraphs("abc\n", 0, 4));
		parts(new int[0][], RichTextRanges.paragraphs("abc", 3, 3));
		parts(new int[0][], RichTextRanges.paragraphs("", 0, 0));
	}

	/**
	 * 补充字符占两个 UTF-16 单元，末尾无换行的段落仍完整返回。
	 */
	public void testUtf16Positions() {
		String text = "A\uD83D\uDE00B\n末段";
		RichTextRanges.check(text.length(), 1, 3);
		assertEquals("😀", text.substring(1, 3));
		parts(new int[][]{{0, 5}}, RichTextRanges.paragraphs(text, 1, 3));
		parts(new int[][]{{5, 7}}, RichTextRanges.paragraphs(text, 5, 7));
	}

	private void invalid(int length, int start, int end) {
		try {
			RichTextRanges.check(length, start, end);
			fail("应拒绝非法区间");
		} catch (索引超出界限错误 expected) {
			// 统一为 Simple 可捕获的索引错误。
		}
	}

	private void parts(int[][] expected, int[][] actual) {
		assertTrue(Arrays.deepToString(actual), Arrays.deepEquals(expected, actual));
	}
}

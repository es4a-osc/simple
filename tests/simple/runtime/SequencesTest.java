package simple.runtime;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Random;

import junit.framework.TestCase;

import simple.runtime.collections.集合;
import simple.runtime.parameters.StringReferenceParameter;
import simple.runtime.variants.ArrayVariant;
import simple.runtime.variants.BooleanVariant;
import simple.runtime.variants.ByteVariant;
import simple.runtime.variants.DoubleVariant;
import simple.runtime.variants.IntegerVariant;
import simple.runtime.variants.LongVariant;
import simple.runtime.variants.ObjectVariant;
import simple.runtime.variants.ShortVariant;
import simple.runtime.variants.SingleVariant;
import simple.runtime.variants.StringVariant;
import simple.runtime.variants.Variant;

/**
 * 覆盖文本、数组和字节集的边界、元素类型及输入数据保持。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class SequencesTest extends TestCase {

	/**
	 * 省略查找起点时覆盖首尾、空匹配及未命中的情况。
	 */
	public void testDefaultSearchBounds() {
		assertEquals(0, 文本操作.寻找文本("ababa", "aba"));
		assertEquals(2, 文本操作.倒找文本("ababa", "aba"));
		assertEquals(-1, 文本操作.寻找文本("abc", "x"));
		assertEquals(-1, 文本操作.倒找文本("abc", "x"));
		assertEquals(0, 文本操作.寻找文本("", ""));
		assertEquals(0, 文本操作.倒找文本("", ""));
		assertEquals(3, 文本操作.倒找文本("abc", ""));
	}

	/**
	 * 省略次数时替换全部字面匹配，指定起点前的原文保持不变。
	 */
	public void testDefaultReplacementRange() {
		StringReferenceParameter text = new StringReferenceParameter("a.a.a");
		文本操作.子文本替换(text, ".", "$1\\x");
		assertEquals("a$1\\xa$1\\xa", text.get());

		text.set("a.a.a");
		文本操作.子文本替换(text, ".", "!", 2);
		assertEquals("a.a!a", text.get());
		文本操作.子文本替换(text, "a", "x", text.get().length());
		assertEquals("a.a!a", text.get());
		invalid(() -> 文本操作.子文本替换(text, "a", "x", -1));
		assertEquals("a.a!a", text.get());

		text.set("aa");
		文本操作.子文本替换(text, "a", "aa");
		assertEquals("aaaa", text.get());
	}

	/** 截取允许空区间和超长数量，不允许负长度或越界起点。 */
	public void testTextBounds() {
		assertEquals("bc", 文本操作.取文本中间("abc", 1, Integer.MAX_VALUE));
		assertEquals("abc", 文本操作.取文本左边("abc", Integer.MAX_VALUE));
		assertEquals("abc", 文本操作.取文本右边("abc", Integer.MAX_VALUE));
		assertEquals("", 文本操作.取文本中间("abc", 3, 100));
		assertEquals(2, 文本操作.取字符数("\uD83D\uDE00"));
		invalid(() -> 文本操作.取文本中间("abc", -1, 1));
		invalid(() -> 文本操作.取文本中间("abc", 4, 1));
		invalid(() -> 文本操作.取文本右边("abc", -1));
	}

	/** 单个空格和全空格应完全删除，制表符维持既有语义。 */
	public void testTrimming() {
		StringReferenceParameter text = new StringReferenceParameter(" ");
		文本操作.删尾空(text);
		assertEquals("", text.get());
		text.set("   ");
		文本操作.删尾空(text);
		assertEquals("", text.get());
		text.set(" \t ");
		文本操作.删首尾空(text);
		assertEquals("\t", text.get());
	}

	/** 查找与替换均为字面内容，只匹配原文，不再次匹配新写入内容。 */
	public void testLiteralReplacement() {
		StringReferenceParameter text = new StringReferenceParameter("a.a.a");
		文本操作.子文本替换(text, ".", "$1\\x", 0, 2);
		assertEquals("a$1\\xa$1\\xa", text.get());
		text.set("x\\Ex\\E");
		文本操作.子文本替换(text, "\\E", "$", 0, -1);
		assertEquals("x$x$", text.get());
		text.set("aa");
		文本操作.子文本替换(text, "a", "aa", 0, 2);
		assertEquals("aaaa", text.get());
		text.set("abc");
		文本操作.子文本替换(text, "", ".", 1, 2);
		assertEquals("a.b.c", text.get());
		文本操作.子文本替换(text, ".", "x", 0, 0);
		assertEquals("a.b.c", text.get());
	}

	/** 按起点与长度替换区间，末尾零长度表示追加。 */
	public void testRangeReplacement() {
		StringReferenceParameter text = new StringReferenceParameter("abc");
		文本操作.替换文本区间(text, 1, 1, "XY");
		assertEquals("aXYc", text.get());
		文本操作.替换文本区间(text, 4, 0, "!");
		assertEquals("aXYc!", text.get());
		文本操作.替换文本区间(text, 1, Integer.MAX_VALUE, "");
		assertEquals("a", text.get());
		invalid(() -> 文本操作.替换文本区间(text, 2, 0, "x"));
		assertEquals("a", text.get());
	}

	/** 长度区分 UTF-16 单元和明确字符集的字节数。 */
	public void testEncodedLength() {
		assertEquals(2, 文本操作.取字符数("中文"));
		assertEquals(6, 文本操作.取文本字节数("中文"));
		assertEquals(4, 文本操作.取文本字节数("中文", "GBK"));
		assertEquals(0, 文本操作.取文本字节数(""));
		invalid(() -> 文本操作.取文本字节数("中文", "NO-SUCH-CHARSET"));
	}

	/** 标记为字面内容，保留跨行及空内容，不递归处理嵌套。 */
	public void testMarkerExtraction() {
		String text = "前[一\n二][][末]后";
		assertEquals("一\n二", 文本操作.取标记间文本(text, "[", "]"));
		assertTrue(Arrays.equals(new String[]{"一\n二", "", "末"}, 文本操作.取全部标记间文本(text, "[", "]")));
		assertEquals("", 文本操作.取标记间文本("缺少[右边", "[", "]"));
		assertEquals(0, 文本操作.取全部标记间文本("缺少[右边", "[", "]").length);
		assertEquals("x", 文本操作.取标记间文本("\\Ex$", "\\E", "$"));
		assertEquals("a", 文本操作.取标记间文本("|a|", "|", "|"));
		invalid(() -> 文本操作.取标记间文本("abc", "", "]"));
	}

	/** 追加后保持全部 Simple 基本元素类型和原数组内容。 */
	public void testArrayAppendTypes() {
		Object[][] cases = {
			{new boolean[]{true}, BooleanVariant.getBooleanVariant(false), boolean[].class},
			{new byte[]{1}, ByteVariant.getByteVariant((byte) 2), byte[].class},
			{new short[]{1}, ShortVariant.getShortVariant((short) 2), short[].class},
			{new int[]{1}, IntegerVariant.getIntegerVariant(2), int[].class},
			{new long[]{1}, LongVariant.getLongVariant(2), long[].class},
			{new float[]{1}, SingleVariant.getSingleVariant(2), float[].class},
			{new double[]{1}, DoubleVariant.getDoubleVariant(2), double[].class},
			{new String[]{"a"}, StringVariant.getStringVariant("b"), String[].class},
			{new Object[]{"a"}, ObjectVariant.getObjectVariant("b"), Object[].class}
		};
		for (Object[] item : cases) {
			Object source = item[0];
			Variant result = 数组操作.加入数组成员(ArrayVariant.getArrayVariant(source), (Variant) item[1]);
			assertNotNull(result);
			assertEquals(item[2], result.getArray().getClass());
			assertEquals(2, Array.getLength(result.getArray()));
			assertEquals(1, Array.getLength(source));
			assertEquals(Array.get(source, 0), Array.get(result.getArray(), 0));
		}
		int[][] nested = {{1}};
		Variant result = 数组操作.加入数组成员(ArrayVariant.getArrayVariant(nested), ArrayVariant.getArrayVariant(new int[]{2}));
		assertTrue(Arrays.deepEquals(new int[][]{{1}, {2}}, (int[][]) result.getArray()));
		assertNull(数组操作.加入数组成员(ArrayVariant.getArrayVariant(new int[0]), ObjectVariant.getObjectVariant(new Object())));
	}

	/** 可插入空数组、首部、中间和末尾，失败约定继续返回空。 */
	public void testArrayInsertion() {
		Variant empty = ArrayVariant.getArrayVariant(new int[0]);
		assertTrue(Arrays.equals(new int[]{7}, (int[]) 数组操作.插入数组成员(empty, IntegerVariant.getIntegerVariant(7), 0).getArray()));
		int[] source = {1, 3};
		for (int index = 0; index <= source.length; index++) {
			int[] expected = index == 0 ? new int[]{2, 1, 3} : index == 1 ? new int[]{1, 2, 3} : new int[]{1, 3, 2};
			assertTrue(Arrays.equals(expected, (int[]) 数组操作.插入数组成员(ArrayVariant.getArrayVariant(source), IntegerVariant.getIntegerVariant(2), index).getArray()));
		}
		assertTrue(Arrays.equals(new int[]{1, 3}, source));
		assertNull(数组操作.插入数组成员(empty, IntegerVariant.getIntegerVariant(7), -1));
		assertNull(数组操作.插入数组成员(empty, IntegerVariant.getIntegerVariant(7), 1));
	}

	/** 删除首尾及唯一成员，原数组保持不变。 */
	public void testArrayRemoval() {
		Variant source = ArrayVariant.getArrayVariant(new String[]{"a", "b", "c"});
		assertTrue(Arrays.equals(new String[]{"b", "c"}, (String[]) 数组操作.删除数组成员(source, 0).getArray()));
		assertTrue(Arrays.equals(new String[]{"a", "b"}, (String[]) 数组操作.删除数组成员(source, 2).getArray()));
		assertEquals(3, Array.getLength(source.getArray()));
		assertEquals(0, Array.getLength(数组操作.删除数组成员(ArrayVariant.getArrayVariant(new byte[]{1}), 0).getArray()));
		assertNull(数组操作.删除数组成员(source, 3));
		assertNull(数组操作.删除数组成员(source, -1));
	}

	/** 同数组重叠复制、数组合并都保留预期顺序。 */
	public void testCopyAndMerge() {
		int[] source = {1, 2, 3, 4};
		Variant wrapped = ArrayVariant.getArrayVariant(source);
		数组操作.复制数组成员(wrapped, 0, wrapped, 1, 3);
		assertTrue(Arrays.equals(new int[]{1, 1, 2, 3}, source));
		assertTrue(Arrays.equals(new byte[]{1, 2}, 数组操作.合并字节数组(new byte[]{1}, new byte[]{2})));
		assertTrue(Arrays.equals(new String[]{"a", "b"}, 数组操作.合并文本数组(new String[]{"a"}, new String[]{"b"})));
	}

	/** 排序直接修改并返回原数组，文本按字典顺序处理。 */
	public void testSorting() {
		int[] numbers = {3, -1, 3, 0};
		assertSame(numbers, 数组操作.数组排序(numbers));
		assertTrue(Arrays.equals(new int[]{-1, 0, 3, 3}, numbers));
		String[] words = {"b", "10", "2", "a"};
		assertSame(words, 数组操作.文本数组排序(words));
		assertTrue(Arrays.equals(new String[]{"10", "2", "a", "b"}, words));
		String[] invalid = {"z", null, "a"};
		try {
			数组操作.文本数组排序(invalid);
			fail();
		} catch (NullPointerException expected) {
			assertTrue(Arrays.equals(new String[]{"z", null, "a"}, invalid));
		}
	}

	/** 文本过滤去重且有序，分隔符中的正则转义按字面匹配。 */
	public void testTextArrayOperations() {
		assertTrue(Arrays.equals(new String[]{"foo", "foobar"}, 数组操作.过滤数组文本(new String[]{"foo", "foobar", "foo"}, "foo", true)));
		assertTrue(Arrays.equals(new String[]{"a", "", ""}, 数组操作.分割文本("a||", "|")));
		assertTrue(Arrays.equals(new String[]{"a", "b"}, 数组操作.分割文本("a\\Eb", "\\E")));
		assertTrue(Arrays.equals(new String[]{"a", "b|c"}, 数组操作.分割文本("a|b|c", "|", 2)));
		assertEquals("a|b", 数组操作.连接数组文本(new String[]{"a", "b"}, "|"));
	}

	/** 截取返回独立数组，支持空内容和超长数量。 */
	public void testByteSlices() {
		byte[] source = {0, 1, -1, 3};
		assertEquals(4, 字节集操作.取字节集长度(source));
		byte[] left = 字节集操作.取字节集左边(source, 2);
		assertTrue(Arrays.equals(new byte[]{0, 1}, left));
		left[0] = 99;
		assertEquals(0, source[0]);
		assertTrue(Arrays.equals(new byte[]{-1, 3}, 字节集操作.取字节集右边(source, 2)));
		assertTrue(Arrays.equals(new byte[]{1, -1, 3}, 字节集操作.取字节集中间(source, 1, Integer.MAX_VALUE)));
		assertTrue(Arrays.equals(source, 字节集操作.取字节集右边(source, Integer.MAX_VALUE)));
		assertEquals(0, 字节集操作.取字节集中间(source, 4, 100).length);
		assertEquals(0, 字节集操作.取字节集左边(new byte[0], 1).length);
	}

	/** 合并副本不会与原数据共享字节存储。 */
	public void testByteMerge() {
		byte[] source = {1};
		byte[] result = 字节集操作.合并字节集(source, new byte[]{2});
		result[0] = 9;
		assertEquals(1, source[0]);
		assertTrue(Arrays.equals(new byte[]{9, 2}, result));
	}

	/** 首尾、连续分隔符和末尾不完整匹配均有明确结果。 */
	public void testByteSplitBoundaries() {
		assertParts(new byte[][]{{}, {1}, {}, {}}, 字节集操作.分割字节集(new byte[]{0, 1, 0, 0}, new byte[]{0}));
		assertParts(new byte[][]{{}}, 字节集操作.分割字节集(new byte[0], new byte[]{0}));
		assertParts(new byte[][]{{1, 2, 1}}, 字节集操作.分割字节集(new byte[]{1, 2, 1}, new byte[]{1, 2, 3}));
		assertParts(new byte[][]{{}, {1}}, 字节集操作.分割字节集(new byte[]{1, 1, 1}, new byte[]{1, 1}));
		byte[] source = {1, 2};
		集合 parts = 字节集操作.分割字节集(source, new byte[]{3});
		((byte[]) parts.项目(0).getArray())[0] = 9;
		assertEquals(1, source[0]);
	}

	/** 随机字节分割后用同一分隔符连接，应逐字节还原原数据。 */
	public void testByteSplitRoundTrip() {
		Random random = new Random(20261017);
		for (int round = 0; round < 200; round++) {
			byte[] source = new byte[random.nextInt(100)];
			byte[] separator = new byte[1 + random.nextInt(5)];
			for (int index = 0; index < source.length; index++) {
				source[index] = (byte) random.nextInt(4);
			}
			for (int index = 0; index < separator.length; index++) {
				separator[index] = (byte) random.nextInt(4);
			}
			集合 parts = 字节集操作.分割字节集(source, separator);
			byte[] joined = new byte[0];
			for (int index = 0; index < parts.计数(); index++) {
				if (index > 0) {
					joined = 字节集操作.合并字节集(joined, separator);
				}
				joined = 字节集操作.合并字节集(joined, (byte[]) parts.项目(index).getArray());
			}
			assertTrue(Arrays.equals(source, joined));
		}
	}

	/** 无效字节参数应抛出可转换为 Simple 错误的参数异常。 */
	public void testByteInvalidArguments() {
		invalid(() -> 字节集操作.取字节集长度(null));
		invalid(() -> 字节集操作.取字节集左边(new byte[]{1}, -1));
		invalid(() -> 字节集操作.取字节集中间(new byte[]{1}, 2, 0));
		invalid(() -> 字节集操作.分割字节集(new byte[]{1}, new byte[0]));
		invalid(() -> 字节集操作.分割字节集(new byte[]{1}, null));
	}

	/**
	 * 核对分割片段的数量和字节内容。
	 *
	 * @param expected 期望片段。
	 * @param actual 实际片段集合。
	 */
	private void assertParts(byte[][] expected, 集合 actual) {
		assertEquals(expected.length, actual.计数());
		for (int index = 0; index < expected.length; index++) {
			assertTrue(Arrays.equals(expected[index], (byte[]) actual.项目(index).getArray()));
		}
	}

	/**
	 * 检查无效参数确实被拒绝。
	 *
	 * @param action 应抛出参数错误的操作。
	 */
	private void invalid(Runnable action) {
		try {
			action.run();
		} catch (IllegalArgumentException expected) {
			return;
		}
		fail("无效参数未被拒绝。");
	}
}

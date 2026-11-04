package simple.runtime;

import java.util.Arrays;
import java.util.Random;
import junit.framework.TestCase;
import simple.runtime.collections.集合;


/**
 * 验证字节集操作的截取、合并、分割、复制隔离和边界。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class ByteSequencesTest extends TestCase {

	/**
	 * 字节后缀是独立副本，不因省略长度而返回原数组。
	 */
	public void testByteSuffixCopiesData() {
		byte[] source = new byte[]{1, 2, 3};
		byte[] suffix = 字节集操作.取字节集中间(source, 1);
		assertTrue(Arrays.equals(new byte[]{2, 3}, suffix));
		suffix[0] = 9;
		assertEquals(2, source[1]);
		assertEquals(0, 字节集操作.取字节集中间(source, source.length).length);
		assertEquals(0, 字节集操作.取字节集中间(new byte[0], 0).length);
		rejectInvalidRange(new Runnable() {
			@Override
			public void run() {
				字节集操作.取字节集中间(source, -1);
			}
		});
		rejectInvalidRange(new Runnable() {
			@Override
			public void run() {
				字节集操作.取字节集中间(source, 4);
			}
		});
	}

	/**
	 * 确认原完整接口的参数校验仍由便捷入口保留。
	 *
	 * @param action 应拒绝无效参数的操作。
	 */
	private static void rejectInvalidRange(Runnable action) {
		try {
			action.run();
			fail("无效参数未被拒绝");
		} catch (IllegalArgumentException expected) {
			// 保留原有参数错误约定。
		}
	}

	/**

	 * 截取返回独立数组，支持空内容和超长数量。

	 */
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

	/**

	 * 合并副本不会与原数据共享字节存储。

	 */
	public void testByteMerge() {
		byte[] source = {1};
		byte[] result = 字节集操作.合并字节集(source, new byte[]{2});
		result[0] = 9;
		assertEquals(1, source[0]);
		assertTrue(Arrays.equals(new byte[]{9, 2}, result));
		assertTrue(Arrays.equals(new byte[]{1}, 字节集操作.合并字节集(new byte[0], source)));
		assertNotSame(source, 字节集操作.合并字节集(source, new byte[0]));
		assertEquals(0, 字节集操作.合并字节集(new byte[0], new byte[0]).length);
	}

	/**

	 * 首尾、连续分隔符和末尾不完整匹配均有明确结果。

	 */
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

	/**

	 * 随机字节分割后用同一分隔符连接，应逐字节还原原数据。

	 */
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

	/**

	 * 无效字节参数应抛出可转换为 Simple 错误的参数异常。

	 */
	public void testByteInvalidArguments() {
		invalid(new Runnable() {
			@Override
			public void run() {
				字节集操作.取字节集长度(null);
			}
		});
		invalid(new Runnable() {
			@Override
			public void run() {
				字节集操作.合并字节集(null, new byte[0]);
			}
		});
		invalid(new Runnable() {
			@Override
			public void run() {
				字节集操作.合并字节集(new byte[0], null);
			}
		});
		invalid(new Runnable() {
			@Override
			public void run() {
				字节集操作.取字节集左边(new byte[]{1}, -1);
			}
		});
		invalid(new Runnable() {
			@Override
			public void run() {
				字节集操作.取字节集中间(new byte[]{1}, 2, 0);
			}
		});
		invalid(new Runnable() {
			@Override
			public void run() {
				字节集操作.分割字节集(new byte[]{1}, new byte[0]);
			}
		});
		invalid(new Runnable() {
			@Override
			public void run() {
				字节集操作.分割字节集(new byte[]{1}, null);
			}
		});
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

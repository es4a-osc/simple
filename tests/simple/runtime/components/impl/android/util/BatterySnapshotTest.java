package simple.runtime.components.impl.android.util;

import junit.framework.TestCase;

/**
 * 验证系统电池测量值的量程、单位、未知值和变化判断。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class BatterySnapshotTest extends TestCase {

	/**
	 * 量程并非总是 100，百分比向下取整，温度和电压保留正确单位。
	 */
	public void testScaleAndUnits() {
		BatterySnapshot state = new BatterySnapshot(true, 2, 127, 200, 357, 4215, 1, 2);
		assertTrue(state.available);
		assertEquals(63, state.level);
		assertEquals(35.7, state.temperature, 0.000001);
		assertEquals(4.215, state.voltage, 0.000001);
		assertEquals(-12.3, new BatterySnapshot(true, 3, 0, 100, -123, 3000, 0, 2).temperature, 0.000001);
	}

	/**
	 * 无电池、缺失测量和无效刻度不能伪装为有效的零值。
	 */
	public void testUnavailableMeasurements() {
		BatterySnapshot absent = new BatterySnapshot(false, 1, 50, 100, 0, 4000, 0, 1);
		assertFalse(absent.available);
		assertEquals(-1, absent.level);
		assertTrue(Double.isNaN(absent.temperature));
		assertTrue(Double.isNaN(absent.voltage));

		for (int[] pair : new int[][]{{1, 0}, {-1, 100}, {101, 100}}) {
			BatterySnapshot invalid = new BatterySnapshot(true, 1, pair[0], pair[1], Integer.MIN_VALUE, 0, -1, 1);
			assertFalse(invalid.available);
			assertEquals(-1, invalid.level);
			assertTrue(Double.isNaN(invalid.temperature));
			assertTrue(Double.isNaN(invalid.voltage));
		}
	}

	/**
	 * 百分比换算使用长整数中间值，不因大刻度溢出，零和满电均有效。
	 */
	public void testPercentageBoundaries() {
		assertEquals(100, new BatterySnapshot(true, 5, Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 1, 1, 2).level);
		assertEquals(0, new BatterySnapshot(true, 3, 0, 100, 0, 1, 0, 2).level);
		assertEquals(99, new BatterySnapshot(true, 2, Integer.MAX_VALUE - 1, Integer.MAX_VALUE, 0, 1, 1, 2).level);
	}

	/**
	 * 重复结果不重复报告；电压、健康或电源变化也需要通知，未知 NaN 可去重。
	 */
	public void testChangeDetection() {
		BatterySnapshot first = new BatterySnapshot(true, 2, 50, 100, 350, 4000, 1, 2);
		assertFalse(first.sameValues(null));
		assertTrue(first.sameValues(new BatterySnapshot(true, 2, 100, 200, 350, 4000, 1, 2)));
		int[][] changes = {{3, 50, 350, 4000, 1, 2}, {2, 51, 350, 4000, 1, 2},
			{2, 50, 351, 4000, 1, 2}, {2, 50, 350, 4001, 1, 2},
			{2, 50, 350, 4000, 2, 2}, {2, 50, 350, 4000, 1, 3}};
		for (int[] values : changes) {
			assertFalse(first.sameValues(new BatterySnapshot(true, values[0], values[1], 100,
				values[2], values[3], values[4], values[5])));
		}

		BatterySnapshot unknown = new BatterySnapshot(false, 1, -1, 0, Integer.MIN_VALUE, -1, -1, 1);
		assertTrue(unknown.sameValues(new BatterySnapshot(false, 1, -1, 0, Integer.MIN_VALUE, -1, -1, 1)));
	}
}

package simple.runtime.components.impl.android;

import junit.framework.TestCase;

import simple.runtime.errors.索引超出界限错误;

/**
 * 验证数据表格的索引、行属性、输入隔离及失败时的数据保持。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class DataTableModelTest extends TestCase {

	/**
	 * 表头不计入数据行，删除后剩余行的内容与属性一起前移。
	 */
	public void testRowsKeepTheirOwnMetadataAfterRemoval() {
		DataTableModel model = new DataTableModel();
		model.setHeaders(new String[]{"姓名", "数量"});
		model.addRow(new String[]{"甲", "1"}, 10);
		model.addRow(new String[]{"乙", "2"}, 20);
		model.setRowTag(0, "first");
		model.setRowTag(1, "业务+|*|+标记");
		model.removeRow(0);

		assertEquals(1, model.getRowCount());
		assertEquals(2, model.getColumnCount());
		assertEquals("乙", model.getCell(0, 0));
		assertEquals(20, model.getRowColor(0));
		assertEquals("业务+|*|+标记", model.getRowTag(0));
		model.setRowColor(0, 30);
		assertEquals(30, model.getRowColor(0));
	}

	/**
	 * 输入数组和读回的表头均为独立副本，外部修改不能悄悄改变表格。
	 */
	public void testInputArraysAreNotShared() {
		DataTableModel model = new DataTableModel();
		String[] headers = {"名称"};
		String[] values = {"原内容"};
		model.setHeaders(headers);
		model.addRow(values, 0);
		headers[0] = "外部表头";
		values[0] = "外部内容";
		model.getHeaders()[0] = "读回修改";
		assertEquals("名称", model.getHeaders()[0]);
		assertEquals("原内容", model.getCell(0, 0));
	}

	/**
	 * 列数冲突先失败，已有数据、表头和列数保持不变。
	 */
	public void testColumnMismatchPreservesData() {
		DataTableModel model = new DataTableModel();
		model.setHeaders(new String[]{"名称", "数量"});
		model.addRow(new String[]{"原内容", "1"}, 0);
		invalid(() -> model.addRow(new String[]{"缺列"}, 1));
		invalid(() -> model.setHeaders(new String[]{"错误表头"}));
		assertEquals(1, model.getRowCount());
		assertEquals(2, model.getColumnCount());
		assertEquals("名称", model.getHeaders()[0]);
		assertEquals("原内容", model.getCell(0, 0));
	}

	/**
	 * 所有行列访问都使用数据索引，负数和末尾索引不能访问表头或其它行。
	 */
	public void testInvalidIndicesDoNotChangeExistingRow() {
		DataTableModel model = new DataTableModel();
		model.addRow(new String[]{"保留"}, 7);
		outOfBounds(() -> model.getCell(-1, 0));
		outOfBounds(() -> model.getCell(1, 0));
		outOfBounds(() -> model.getCell(0, -1));
		outOfBounds(() -> model.setCell(0, 1, "越界"));
		outOfBounds(() -> model.removeRow(1));
		outOfBounds(() -> model.setRowColor(-1, 0));
		outOfBounds(() -> model.getRowColor(1));
		outOfBounds(() -> model.setRowTag(1, "越界"));
		outOfBounds(() -> model.getRowTag(-1));
		assertEquals("保留", model.getCell(0, 0));
		assertEquals(7, model.getRowColor(0));
	}

	/**
	 * 清空会重置表头和列数，允许用另一种列结构重新填充。
	 */
	public void testClearAllowsAnotherSchema() {
		DataTableModel model = new DataTableModel();
		model.setHeaders(new String[]{"第一列", "第二列"});
		model.addRow(new String[]{"甲", "乙"}, 0);
		model.clear();
		model.clear();
		assertNull(model.getHeaders());
		assertEquals(0, model.getRowCount());
		assertEquals(0, model.getColumnCount());
		model.addRow(new String[]{"新列"}, 0);
		assertEquals(1, model.getColumnCount());
		assertEquals("新列", model.getCell(0, 0));
	}

	/**
	 * 空数组拒绝，数组中的空成员和空标记统一为可显示的空文本。
	 */
	public void testNullAndEmptyInputs() {
		DataTableModel model = new DataTableModel();
		invalid(() -> model.setHeaders(null));
		invalid(() -> model.addRow(new String[0], 0));
		model.setHeaders(new String[]{null});
		model.addRow(new String[]{null}, 0);
		model.setCell(0, 0, null);
		model.setRowTag(0, null);
		assertEquals("", model.getHeaders()[0]);
		assertEquals("", model.getCell(0, 0));
		assertEquals("", model.getRowTag(0));
	}

	private void invalid(Runnable action) {
		try {
			action.run();
			fail("应拒绝不合法的行列结构");
		} catch (IllegalArgumentException expected) {
			// 预期失败，调用者继续检查已有数据。
		}
	}

	private void outOfBounds(Runnable action) {
		try {
			action.run();
			fail("应报告数据索引越界");
		} catch (索引超出界限错误 expected) {
			// 不允许底层视图偏移掩盖索引错误。
		}
	}
}

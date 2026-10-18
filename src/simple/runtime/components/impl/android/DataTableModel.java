package simple.runtime.components.impl.android;

import java.util.ArrayList;
import java.util.List;

import simple.runtime.errors.索引超出界限错误;

/**
 * 保存数据表格的表头、行内容、背景色和行标记，不依赖 Android 视图。
 *
 * <p>行列索引从 0 开始，表头不计入数据行；先校验再修改，输入数组不会被共享。
 *
 * @author 树先生 xhwsd@qq.com
 */
final class DataTableModel {

	private String[] headers;
	private int columns;
	private final List<Row> rows = new ArrayList<Row>();

	/* 表头与行列结构 */

	/**
	 * 复制并设置表头，已有数据时要求列数一致。
	 *
	 * @param values 输入值数组
	 */
	void setHeaders(String[] values) {
		String[] copy = copyValues(values);
		if (!rows.isEmpty() && copy.length != columns) {
			throw new IllegalArgumentException("已有数据时，表头列数必须与数据列数一致");
		}

		headers = copy;
		columns = copy.length;
	}

	/**
	 * 返回表头副本；尚未设置表头时返回空引用。
	 *
	 * @return 表头副本；未设置时为 {@code null}
	 */
	String[] getHeaders() {
		return headers == null ? null : headers.clone();
	}

	/**
	 * 复制输入单元格并追加一行，要求与现有列数一致。
	 *
	 * @param values 输入值数组
	 * @param color ARGB 颜色值
	 */
	void addRow(String[] values, int color) {
		String[] copy = copyValues(values);
		if (columns != 0 && copy.length != columns) {
			throw new IllegalArgumentException("数据列数必须与表格列数一致");
		}

		rows.add(new Row(copy, color));
		columns = copy.length;
	}

	/**
	 * 删除指定数据行，行索引从 0 开始。
	 *
	 * @param row 行索引，从 0 开始
	 */
	void removeRow(int row) {
		checkRow(row);
		rows.remove(row);
	}

	/**
	 * 清空当前保存的内容与关联状态。
	 */
	void clear() {
		rows.clear();
		headers = null;
		columns = 0;
	}

	/**
	 * 返回数据行数，不包含表头。
	 *
	 * @return 数据行数，不包含表头
	 */
	int getRowCount() {
		return rows.size();
	}

	/**
	 * 返回当前列数。
	 *
	 * @return 列数
	 */
	int getColumnCount() {
		return columns;
	}

	/* 内容与行属性 */

	/**
	 * 读取指定单元格文本，行列索引从 0 开始。
	 *
	 * @param row 行索引，从 0 开始
	 * @param column 列索引，从 0 开始
	 * @return 指定单元格的文本
	 */
	String getCell(int row, int column) {
		checkCell(row, column);
		return rows.get(row).values[column];
	}

	/**
	 * 设置指定单元格文本，空引用保存为空文本。
	 *
	 * @param row 行索引，从 0 开始
	 * @param column 列索引，从 0 开始
	 * @param value 新的单元格文本；空引用按空文本保存。
	 */
	void setCell(int row, int column, String value) {
		checkCell(row, column);
		rows.get(row).values[column] = value == null ? "" : value;
	}

	/**
	 * 读取指定数据行的背景色。
	 *
	 * @param row 行索引，从 0 开始
	 * @return 指定行的 ARGB 背景色
	 */
	int getRowColor(int row) {
		checkRow(row);
		return rows.get(row).color;
	}

	/**
	 * 设置指定数据行的背景色。
	 *
	 * @param row 行索引，从 0 开始
	 * @param color ARGB 颜色值
	 */
	void setRowColor(int row, int color) {
		checkRow(row);
		rows.get(row).color = color;
	}

	/**
	 * 读取指定数据行的附加标记。
	 *
	 * @param row 行索引，从 0 开始
	 * @return 指定行的附加标记
	 */
	String getRowTag(int row) {
		checkRow(row);
		return rows.get(row).tag;
	}

	/**
	 * 设置指定数据行的附加标记，空引用保存为空文本。
	 *
	 * @param row 行索引，从 0 开始
	 * @param tag 附加标记或 NFC 标签
	 */
	void setRowTag(int row, String tag) {
		checkRow(row);
		rows.get(row).tag = tag == null ? "" : tag;
	}

	/* 输入校验 */

	/**
	 * 复制单元格数组并将其中的空引用规范为空文本。
	 *
	 * @param values 输入值数组
	 * @return 独立的数组副本，空单元格已转换为空文本
	 */
	private String[] copyValues(String[] values) {
		if (values == null || values.length == 0) {
			throw new IllegalArgumentException("表头或数据必须至少包含一列");
		}

		String[] copy = values.clone();

		for (int index = 0; index < copy.length; index++) {
			if (copy[index] == null) {
				copy[index] = "";
			}
		}

		return copy;
	}

	/**
	 * 检查数据行索引是否在有效范围内。
	 *
	 * @param row 行索引，从 0 开始
	 */
	private void checkRow(int row) {
		if (row < 0 || row >= rows.size()) {
			throw new 索引超出界限错误();
		}
	}

	/**
	 * 检查数据行和列索引是否在有效范围内。
	 *
	 * @param row 行索引，从 0 开始
	 * @param column 列索引，从 0 开始
	 */
	private void checkCell(int row, int column) {
		checkRow(row);
		if (column < 0 || column >= columns) {
			throw new 索引超出界限错误();
		}
	}

	/**
	 * 一行数据的内容与展示属性；标记独立保存，不依赖文本分隔符。
	 */
	private static final class Row {

		private final String[] values;
		private int color;
		private String tag = "";

		/**
		 * 保存一行独立单元格数据及其初始背景色。
		 *
		 * @param values 输入值数组
		 * @param color ARGB 颜色值
		 */
		private Row(String[] values, int color) {
			this.values = values;
			this.color = color;
		}
	}
}

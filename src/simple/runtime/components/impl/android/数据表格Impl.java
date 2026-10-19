package simple.runtime.components.impl.android;

import java.io.IOException;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import simple.runtime.android.MainActivity;
import simple.runtime.components.数据表格;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.android.util.DataTableModel;
import simple.runtime.components.impl.android.util.ImageUtil;
import simple.runtime.components.impl.android.util.ViewUtil;
import simple.runtime.errors.文件未存在错误;
import simple.runtime.events.EventDispatcher;

/**
 * 使用原生表格及双向滚动视图展示数据，数据索引与视图位置独立。
 *
 * <p>该组件不复用表格布局容器，也不接收 Simple 子组件；所有操作在界面线程完成。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class 数据表格Impl extends 视图组件 implements 数据表格 {

	/* 数据与视图 */

	private DataTableModel model;
	private TableLayout table;
	private int appliedStretchColumn;
	private boolean shortened;

	/* 表头与单元格样式 */

	private int headerBackgroundColor;
	private int headerTextColor;
	private float headerTextSize;
	private int maxCharacters;
	private int dividerColor;
	private int dividerWidth;
	private int stretchColumn;
	private boolean showImages;
	private int cellTextColor;
	private float cellTextSize;

	/**
	 * 创建数据表格，使用容器所在界面的上下文。
	 *
	 * @param container 容纳组件的窗口或布局容器。
	 */
	public 数据表格Impl(组件容器 container) {
		super(container);
	}

	/**
	 * 建立纵向滚动、横向滚动和表格视图，并初始化组件状态。
	 *
	 * @return 交由父容器管理的外层滚动视图。
	 */
	@Override
	protected View createView() {
		// 父类构造期间会调用此方法，状态在这里建立，避免被字段初始化覆盖。
		model = new DataTableModel();
		appliedStretchColumn = -1;
		headerBackgroundColor = 0xFFE8EEF7;
		headerTextColor = 0xFF000000;
		headerTextSize = 14;
		maxCharacters = 10;
		dividerColor = 0xFFD0D7DE;
		dividerWidth = 1;
		stretchColumn = -1;
		showImages = false;
		cellTextColor = 0xFF000000;
		cellTextSize = 14;
		table = new TableLayout(MainActivity.getContext());
		HorizontalScrollView horizontal = new HorizontalScrollView(MainActivity.getContext());
		horizontal.setFillViewport(true);
		horizontal.addView(table, new HorizontalScrollView.LayoutParams(-1, -2));
		ScrollView vertical = new ScrollView(MainActivity.getContext());
		vertical.addView(horizontal, new ScrollView.LayoutParams(-1, -2));
		return vertical;
	}

	/* 组件事件 */

	/**
	 * 派发数据行点击事件。
	 *
	 * @param row 从 0 开始的数据行索引。
	 */
	@Override
	public void 表格行被单击(int row) {
		EventDispatcher.dispatchEvent(this, "表格行被单击", row);
	}

	/* 组件属性 */

	/**
	 * 取得表头背景的 ARGB 颜色。
	 *
	 * @return 表头背景的 ARGB 颜色。
	 */
	@Override
	public int 表头背景颜色() {
		return headerBackgroundColor;
	}

	/**
	 * 设置表头背景的 ARGB 颜色。
	 *
	 * @param color 表头背景的 ARGB 颜色。
	 */
	@Override
	public void 表头背景颜色(int color) {
		headerBackgroundColor = color;
		rebuild();
	}

	/**
	 * 取得表头文字的 ARGB 颜色。
	 *
	 * @return 表头文字的 ARGB 颜色。
	 */
	@Override
	public int 表头字体颜色() {
		return headerTextColor;
	}

	/**
	 * 设置表头文字的 ARGB 颜色。
	 *
	 * @param color 表头文字的 ARGB 颜色。
	 */
	@Override
	public void 表头字体颜色(int color) {
		headerTextColor = color;
		rebuild();
	}

	/**
	 * 取得表头文字大小，单位 SP，必须为有限正数。
	 *
	 * @return 表头文字大小，单位 SP，必须为有限正数。
	 */
	@Override
	public float 表头字体大小() {
		return headerTextSize;
	}

	/**
	 * 设置表头文字大小，单位 SP，必须为有限正数。
	 *
	 * @param size 表头文字大小，单位 SP，必须为有限正数。
	 * @throws IllegalArgumentException 数值不在允许范围内。
	 */
	@Override
	public void 表头字体大小(float size) {
		if (Float.isNaN(size) || Float.isInfinite(size) || size <= 0) {
			throw new IllegalArgumentException("字体大小必须为有限正数");
		}

		headerTextSize = size;
		rebuild();
	}

	/**
	 * 取得文字单元格最大宽度，单位 em，必须大于 0；实际宽度受列拉伸影响。
	 *
	 * @return 文字单元格最大宽度，单位 em，必须大于 0；实际宽度受列拉伸影响。
	 */
	@Override
	public int 最大字长() {
		return maxCharacters;
	}

	/**
	 * 设置文字单元格最大宽度，单位 em，必须大于 0；实际宽度受列拉伸影响。
	 *
	 * @param length 文字单元格最大宽度，单位 em，必须大于 0；实际宽度受列拉伸影响。
	 * @throws IllegalArgumentException 数值不在允许范围内。
	 */
	@Override
	public void 最大字长(int length) {
		if (length <= 0) {
			throw new IllegalArgumentException("最大字长必须大于 0");
		}

		maxCharacters = length;
		rebuild();
	}

	/**
	 * 取得单元格边框的 ARGB 颜色。
	 *
	 * @return 单元格边框的 ARGB 颜色。
	 */
	@Override
	public int 分割线颜色() {
		return dividerColor;
	}

	/**
	 * 设置单元格边框的 ARGB 颜色。
	 *
	 * @param color 单元格边框的 ARGB 颜色。
	 */
	@Override
	public void 分割线颜色(int color) {
		dividerColor = color;
		rebuild();
	}

	/**
	 * 取得单元格边框宽度，单位 DIP；0 隐藏边框，不能为负数。
	 *
	 * @return 单元格边框宽度，单位 DIP；0 隐藏边框，不能为负数。
	 */
	@Override
	public int 分割线宽度() {
		return dividerWidth;
	}

	/**
	 * 设置单元格边框宽度，单位 DIP；0 隐藏边框，不能为负数。
	 *
	 * @param width 单元格边框宽度，单位 DIP；0 隐藏边框，不能为负数。
	 * @throws IllegalArgumentException 数值不在允许范围内。
	 */
	@Override
	public void 分割线宽度(int width) {
		if (width < 0) {
			throw new IllegalArgumentException("分割线宽度不能为负数");
		}

		dividerWidth = width;
		rebuild();
	}

	/**
	 * 取得填满剩余宽度的列索引，从 0 开始；-1 不拉伸，可在添加数据前设置。
	 *
	 * @return 填满剩余宽度的列索引，从 0 开始；-1 不拉伸，可在添加数据前设置。
	 */
	@Override
	public int 拉伸列() {
		return stretchColumn;
	}

	/**
	 * 设置填满剩余宽度的列索引，从 0 开始；-1 不拉伸，可在添加数据前设置。
	 *
	 * @param column 填满剩余宽度的列索引，从 0 开始；-1 不拉伸，可在添加数据前设置。
	 * @throws IllegalArgumentException 数值不在允许范围内。
	 */
	@Override
	public void 拉伸列(int column) {
		if (column < -1) {
			throw new IllegalArgumentException("拉伸列必须为 -1 或非负索引");
		}

		stretchColumn = column;
		rebuild();
	}

	/**
	 * 取得是否将数据行的第一列显示为本地图片；表头仍为文本，不下载网络图片。
	 *
	 * @return 是否将数据行的第一列显示为本地图片；表头仍为文本，不下载网络图片。
	 */
	@Override
	public boolean 显示图片() {
		return showImages;
	}

	/**
	 * 设置是否将数据行的第一列显示为本地图片；表头仍为文本，不下载网络图片。
	 *
	 * @param enabled 是否将数据行的第一列显示为本地图片；表头仍为文本，不下载网络图片。
	 * @throws simple.runtime.errors.文件未存在错误 图片首列引用的本地图片无法读取。
	 */
	@Override
	public void 显示图片(boolean enabled) {
		if (enabled) {
			for (int row = 0; row < model.getRowCount(); row++) {
				loadImage(model.getCell(row, 0));
			}
		}
		showImages = enabled;
		rebuild();
	}

	/**
	 * 取得数据单元格文字的 ARGB 颜色。
	 *
	 * @return 数据单元格文字的 ARGB 颜色。
	 */
	@Override
	public int 单元格字体颜色() {
		return cellTextColor;
	}

	/**
	 * 设置数据单元格文字的 ARGB 颜色。
	 *
	 * @param color 数据单元格文字的 ARGB 颜色。
	 */
	@Override
	public void 单元格字体颜色(int color) {
		cellTextColor = color;
		rebuild();
	}

	/**
	 * 取得数据单元格文字大小，单位 SP，必须为有限正数。
	 *
	 * @return 数据单元格文字大小，单位 SP，必须为有限正数。
	 */
	@Override
	public float 单元格字体大小() {
		return cellTextSize;
	}

	/**
	 * 设置数据单元格文字大小，单位 SP，必须为有限正数。
	 *
	 * @param size 数据单元格文字大小，单位 SP，必须为有限正数。
	 * @throws IllegalArgumentException 数值不在允许范围内。
	 */
	@Override
	public void 单元格字体大小(float size) {
		if (Float.isNaN(size) || Float.isInfinite(size) || size <= 0) {
			throw new IllegalArgumentException("字体大小必须为有限正数");
		}

		cellTextSize = size;
		rebuild();
	}

	/* 组件方法 */

	/**
	 * 设置或替换表头，不将表头计入数据行。
	 *
	 * @param values 至少一列的表头文本；已有数据时，列数必须一致。
	 * @throws IllegalArgumentException 数组为空或列数不一致。
	 */
	@Override
	public void 添加表头(String[] values) {
		model.setHeaders(values);
		rebuild();
	}

	/**
	 * 以白色背景追加一行数据。
	 *
	 * @param values 与表格列数一致的文本数组；显示图片时首项为图片标识。
	 * @throws IllegalArgumentException 数组为空或列数不一致。
	 * @throws simple.runtime.errors.文件未存在错误 图片首列引用的本地图片无法读取。
	 */
	@Override
	public void 添加一行(String[] values) {
		添加一行(values, Color.WHITE);
	}

	/**
	 * 以指定背景色追加一行数据。
	 *
	 * @param values 与表格列数一致的文本数组；未设置表头时由首行确定列数。
	 * @param color 该行的 ARGB 背景色。
	 * @throws IllegalArgumentException 数组为空或列数不一致。
	 * @throws simple.runtime.errors.文件未存在错误 图片首列引用的本地图片无法读取。
	 */
	@Override
	public void 添加一行(String[] values, int color) {
		if (showImages && values != null && values.length > 0) {
			loadImage(values[0]);
		}
		model.addRow(values, color);
		rebuild();
	}

	/**
	 * 删除指定数据行，后续行的内容、背景色和标记一起前移。
	 *
	 * @param row 从 0 开始的数据行索引，不包含表头。
	 * @throws simple.runtime.errors.索引超出界限错误 行或列索引越界。
	 */
	@Override
	public void 删除一行(int row) {
		model.removeRow(row);
		rebuild();
	}

	/**
	 * 清空全部数据和表头，行列数归零；保留样式设置。
	 */
	@Override
	public void 清空表格() {
		model.clear();
		rebuild();
	}

	/**
	 * 取得数据行数，不包含表头。
	 *
	 * @return 数据行数。
	 */
	@Override
	public int 取行数() {
		return model.getRowCount();
	}

	/**
	 * 取得列数，由表头或第一行确定。
	 *
	 * @return 列数，清空后为 0。
	 */
	@Override
	public int 取列数() {
		return model.getColumnCount();
	}

	/**
	 * 读取单元格原始文本，图片列返回图片标识。
	 *
	 * @param row 从 0 开始的数据行索引。
	 * @param column 从 0 开始的列索引。
	 * @return 单元格文本。
	 * @throws simple.runtime.errors.索引超出界限错误 行或列索引越界。
	 */
	@Override
	public String 取单元格内容(int row, int column) {
		return model.getCell(row, column);
	}

	/**
	 * 更新指定单元格并刷新显示。
	 *
	 * @param row 从 0 开始的数据行索引。
	 * @param column 从 0 开始的列索引。
	 * @param value 新的文本，空引用按空文本处理；图片列须为有效本地图片。
	 * @throws simple.runtime.errors.索引超出界限错误 行或列索引越界。
	 * @throws simple.runtime.errors.文件未存在错误 图片首列引用的本地图片无法读取。
	 */
	@Override
	public void 置单元格内容(int row, int column, String value) {
		// 先验证索引和图片，失败时保留原来的内容。
		model.getCell(row, column);
		if (showImages && column == 0) {
			loadImage(value);
		}
		model.setCell(row, column, value);
		rebuild();
	}

	/**
	 * 设置一行的背景色。
	 *
	 * @param row 从 0 开始的数据行索引。
	 * @param color 该行的 ARGB 背景色。
	 * @throws simple.runtime.errors.索引超出界限错误 行或列索引越界。
	 */
	@Override
	public void 置表格行背景色(int row, int color) {
		model.setRowColor(row, color);
		rebuild();
	}

	/**
	 * 设置内容超宽时的显示方式，同时刷新已有行。
	 *
	 * @param enabled 真为单行省略显示，假为允许文字换行。
	 */
	@Override
	public void 置缩略显示内容(boolean enabled) {
		shortened = enabled;
		rebuild();
	}

	/**
	 * 为一行保存独立业务标记，不改变显示内容。
	 *
	 * @param row 从 0 开始的数据行索引。
	 * @param tag 业务标记，空引用按空文本处理。
	 * @throws simple.runtime.errors.索引超出界限错误 行或列索引越界。
	 */
	@Override
	public void 置表格行标记(int row, String tag) {
		model.setRowTag(row, tag);
	}

	/**
	 * 读取一行的业务标记。
	 *
	 * @param row 从 0 开始的数据行索引。
	 * @return 业务标记，未设置时为空文本。
	 * @throws simple.runtime.errors.索引超出界限错误 行或列索引越界。
	 */
	@Override
	public String 取表格行标记(int row) {
		return model.getRowTag(row);
	}

	/* 视图构建 */

	/**
	 * 按数据模型重建表格；样式修改不会重置业务内容、行标记或背景色。
	 */
	private void rebuild() {
		table.removeAllViews();
		if (appliedStretchColumn >= 0) {
			table.setColumnStretchable(appliedStretchColumn, false);
		}
		appliedStretchColumn = stretchColumn;
		if (stretchColumn >= 0) {
			table.setColumnStretchable(stretchColumn, true);
		}

		String[] headers = model.getHeaders();
		if (headers != null) {
			TableRow header = new TableRow(MainActivity.getContext());

			for (String text : headers) {
				header.addView(createText(text, true, headerBackgroundColor));
			}
			table.addView(header);
		}

		for (int row = 0; row < model.getRowCount(); row++) {
			TableRow line = new TableRow(MainActivity.getContext());
			line.setTag(Integer.valueOf(row));
			line.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View view) {
					表格行被单击(((Integer) view.getTag()).intValue());
				}
			});

			int color = model.getRowColor(row);

			for (int column = 0; column < model.getColumnCount(); column++) {
				String value = model.getCell(row, column);
				line.addView(showImages && column == 0
					? createImage(value, color) : createText(value, false, color));
			}
			table.addView(line);
		}
	}

	/**
	 * 创建文本单元格，保留原始数据，仅改变可视换行和缩略方式。
	 *
	 * @param text 待处理文本
	 * @param header 请求或响应头
	 * @param color ARGB 颜色值
	 * @return 已设置文字样式的标签
	 */
	private TextView createText(String text, boolean header, int color) {
		TextView view = new TextView(MainActivity.getContext());
		view.setText(text);
		view.setTextColor(header ? headerTextColor : cellTextColor);
		view.setTextSize(header ? headerTextSize : cellTextSize);
		view.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
		view.setMaxEms(maxCharacters);
		view.setSingleLine(shortened);
		view.setEllipsize(shortened ? TextUtils.TruncateAt.END : null);
		if (header) {
			view.setTypeface(null, Typeface.BOLD);
		}
		decorate(view, color);
		view.setLayoutParams(new TableRow.LayoutParams(-2, -1));
		return view;
	}

	/**
	 * 创建第一列的本地图片单元格，空标识显示空白。
	 *
	 * @param image 图片资源路径
	 * @param color ARGB 颜色值
	 * @return 已设置图片内容的视图
	 */
	private ImageView createImage(String image, int color) {
		ImageView view = new ImageView(MainActivity.getContext());
		view.setImageDrawable(loadImage(image));
		view.setScaleType(ImageView.ScaleType.FIT_CENTER);
		view.setContentDescription(image == null ? "" : image);
		decorate(view, color);
		view.setLayoutParams(new TableRow.LayoutParams(dp(56), dp(48)));
		return view;
	}

	/**
	 * 为单元格设置背景、边框和统一内边距。
	 *
	 * @param view 目标原生视图
	 * @param color ARGB 颜色值
	 */
	private void decorate(View view, int color) {
		GradientDrawable background = new GradientDrawable();
		background.setColor(color);
		background.setStroke(dp(dividerWidth), dividerColor);
		ViewUtil.setBackgroundDrawable(view, background);
		int padding = dp(8);
		view.setPadding(padding, padding, padding, padding);
		view.setMinimumHeight(dp(40));
	}

	/**
	 * 沿用运行库的资源、资产和文件图片规则，不创建联网或存储权限。
	 *
	 * @param image 图片资源路径
	 * @return 载入的图片对象
	 */
	private Drawable loadImage(String image) {
		try {
			return ImageUtil.getDrawable(image);
		} catch (IOException exception) {
			throw new 文件未存在错误(image);
		}
	}

	/**
	 * 将组件内部的 DIP 尺寸转换为当前设备像素。
	 *
	 * @param value 需要换算的 DIP 长度。
	 * @return 换算后的像素值
	 */
	private int dp(int value) {
		return Math.round(value * table.getResources().getDisplayMetrics().density);
	}
}

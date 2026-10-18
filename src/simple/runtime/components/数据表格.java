package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;

/**
 * 展示带表头的行列数据，支持本地图片列、单元格修改及行点击。
 *
 * <p>行列索引从 0 开始，表头不计入数据行；越界抛出索引超出界限错误。
 * 在界面线程操作，适用于中小规模数据展示，不提供编辑器、排序或分页。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 数据表格 extends 可视组件 {

	/* 组件事件 */

	/**
	 * 数据行被单击时触发，表头不触发该事件。
	 *
	 * @param row 从 0 开始的数据行索引。
	 */
	@SimpleEvent
	void 表格行被单击(int row);

	/* 组件属性 */

	/**
	 * 取得表头背景的 ARGB 颜色。
	 *
	 * @return 表头背景的 ARGB 颜色。
	 */
	@SimpleProperty
	int 表头背景颜色();

	/**
	 * 设置表头背景的 ARGB 颜色。
	 *
	 * @param color 表头背景的 ARGB 颜色。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_COLOR,
		initializer = "&HFFE8EEF7"
	)
	void 表头背景颜色(int color);

	/**
	 * 取得表头文字的 ARGB 颜色。
	 *
	 * @return 表头文字的 ARGB 颜色。
	 */
	@SimpleProperty
	int 表头字体颜色();

	/**
	 * 设置表头文字的 ARGB 颜色。
	 *
	 * @param color 表头文字的 ARGB 颜色。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_COLOR,
		initializer = "&HFF000000"
	)
	void 表头字体颜色(int color);

	/**
	 * 取得表头文字大小，单位 SP，必须为有限正数。
	 *
	 * @return 表头文字大小，单位 SP，必须为有限正数。
	 */
	@SimpleProperty
	float 表头字体大小();

	/**
	 * 设置表头文字大小，单位 SP，必须为有限正数。
	 *
	 * @param size 表头文字大小，单位 SP，必须为有限正数。
	 * @throws IllegalArgumentException 数值不在允许范围内。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_SINGLE,
		initializer = "14"
	)
	void 表头字体大小(float size);

	/**
	 * 取得文字单元格最大宽度，单位 em，必须大于 0；实际宽度受列拉伸影响。
	 *
	 * @return 文字单元格最大宽度，单位 em，必须大于 0；实际宽度受列拉伸影响。
	 */
	@SimpleProperty
	int 最大字长();

	/**
	 * 设置文字单元格最大宽度，单位 em，必须大于 0；实际宽度受列拉伸影响。
	 *
	 * @param length 文字单元格最大宽度，单位 em，必须大于 0；实际宽度受列拉伸影响。
	 * @throws IllegalArgumentException 数值不在允许范围内。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER,
		initializer = "10"
	)
	void 最大字长(int length);

	/**
	 * 取得单元格边框的 ARGB 颜色。
	 *
	 * @return 单元格边框的 ARGB 颜色。
	 */
	@SimpleProperty
	int 分割线颜色();

	/**
	 * 设置单元格边框的 ARGB 颜色。
	 *
	 * @param color 单元格边框的 ARGB 颜色。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_COLOR,
		initializer = "&HFFD0D7DE"
	)
	void 分割线颜色(int color);

	/**
	 * 取得单元格边框宽度，单位 DIP；0 隐藏边框，不能为负数。
	 *
	 * @return 单元格边框宽度，单位 DIP；0 隐藏边框，不能为负数。
	 */
	@SimpleProperty
	int 分割线宽度();

	/**
	 * 设置单元格边框宽度，单位 DIP；0 隐藏边框，不能为负数。
	 *
	 * @param width 单元格边框宽度，单位 DIP；0 隐藏边框，不能为负数。
	 * @throws IllegalArgumentException 数值不在允许范围内。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER,
		initializer = "1"
	)
	void 分割线宽度(int width);

	/**
	 * 取得填满剩余宽度的列索引，从 0 开始；-1 不拉伸，可在添加数据前设置。
	 *
	 * @return 填满剩余宽度的列索引，从 0 开始；-1 不拉伸，可在添加数据前设置。
	 */
	@SimpleProperty
	int 拉伸列();

	/**
	 * 设置填满剩余宽度的列索引，从 0 开始；-1 不拉伸，可在添加数据前设置。
	 *
	 * @param column 填满剩余宽度的列索引，从 0 开始；-1 不拉伸，可在添加数据前设置。
	 * @throws IllegalArgumentException 数值不在允许范围内。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER,
		initializer = "-1"
	)
	void 拉伸列(int column);

	/**
	 * 取得是否将数据行的第一列显示为本地图片；表头仍为文本，不下载网络图片。
	 *
	 * @return 是否将数据行的第一列显示为本地图片；表头仍为文本，不下载网络图片。
	 */
	@SimpleProperty
	boolean 显示图片();

	/**
	 * 设置是否将数据行的第一列显示为本地图片；表头仍为文本，不下载网络图片。
	 *
	 * @param enabled 是否将数据行的第一列显示为本地图片；表头仍为文本，不下载网络图片。
	 * @throws simple.runtime.errors.文件未存在错误 图片首列引用的本地图片无法读取。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "False"
	)
	void 显示图片(boolean enabled);

	/**
	 * 取得数据单元格文字的 ARGB 颜色。
	 *
	 * @return 数据单元格文字的 ARGB 颜色。
	 */
	@SimpleProperty
	int 单元格字体颜色();

	/**
	 * 设置数据单元格文字的 ARGB 颜色。
	 *
	 * @param color 数据单元格文字的 ARGB 颜色。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_COLOR,
		initializer = "&HFF000000"
	)
	void 单元格字体颜色(int color);

	/**
	 * 取得数据单元格文字大小，单位 SP，必须为有限正数。
	 *
	 * @return 数据单元格文字大小，单位 SP，必须为有限正数。
	 */
	@SimpleProperty
	float 单元格字体大小();

	/**
	 * 设置数据单元格文字大小，单位 SP，必须为有限正数。
	 *
	 * @param size 数据单元格文字大小，单位 SP，必须为有限正数。
	 * @throws IllegalArgumentException 数值不在允许范围内。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_SINGLE,
		initializer = "14"
	)
	void 单元格字体大小(float size);

	/* 组件方法 */

	/**
	 * 设置或替换表头，不将表头计入数据行。
	 *
	 * @param values 至少一列的表头文本；已有数据时，列数必须一致。
	 * @throws IllegalArgumentException 数组为空或列数不一致。
	 */
	@SimpleFunction
	void 添加表头(String[] values);

	/**
	 * 以白色背景追加一行数据。
	 *
	 * @param values 与表格列数一致的文本数组；显示图片时首项为图片标识。
	 * @throws IllegalArgumentException 数组为空或列数不一致。
	 * @throws simple.runtime.errors.文件未存在错误 图片首列引用的本地图片无法读取。
	 */
	@SimpleFunction
	void 添加一行(String[] values);

	/**
	 * 以指定背景色追加一行数据。
	 *
	 * @param values 与表格列数一致的文本数组；未设置表头时由首行确定列数。
	 * @param color 该行的 ARGB 背景色。
	 * @throws IllegalArgumentException 数组为空或列数不一致。
	 * @throws simple.runtime.errors.文件未存在错误 图片首列引用的本地图片无法读取。
	 */
	@SimpleFunction
	void 添加一行(String[] values, int color);

	/**
	 * 删除指定数据行，后续行的内容、背景色和标记一起前移。
	 *
	 * @param row 从 0 开始的数据行索引，不包含表头。
	 * @throws simple.runtime.errors.索引超出界限错误 行或列索引越界。
	 */
	@SimpleFunction
	void 删除一行(int row);

	/**
	 * 清空全部数据和表头，行列数归零；保留样式设置。
	 */
	@SimpleFunction
	void 清空表格();

	/**
	 * 取得数据行数，不包含表头。
	 *
	 * @return 数据行数。
	 */
	@SimpleFunction
	int 取行数();

	/**
	 * 取得列数，由表头或第一行确定。
	 *
	 * @return 列数，清空后为 0。
	 */
	@SimpleFunction
	int 取列数();

	/**
	 * 读取单元格原始文本，图片列返回图片标识。
	 *
	 * @param row 从 0 开始的数据行索引。
	 * @param column 从 0 开始的列索引。
	 * @return 单元格文本。
	 * @throws simple.runtime.errors.索引超出界限错误 行或列索引越界。
	 */
	@SimpleFunction
	String 取单元格内容(int row, int column);

	/**
	 * 更新指定单元格并刷新显示。
	 *
	 * @param row 从 0 开始的数据行索引。
	 * @param column 从 0 开始的列索引。
	 * @param value 新的文本，空引用按空文本处理；图片列须为有效本地图片。
	 * @throws simple.runtime.errors.索引超出界限错误 行或列索引越界。
	 * @throws simple.runtime.errors.文件未存在错误 图片首列引用的本地图片无法读取。
	 */
	@SimpleFunction
	void 置单元格内容(int row, int column, String value);

	/**
	 * 设置一行的背景色。
	 *
	 * @param row 从 0 开始的数据行索引。
	 * @param color 该行的 ARGB 背景色。
	 * @throws simple.runtime.errors.索引超出界限错误 行或列索引越界。
	 */
	@SimpleFunction
	void 置表格行背景色(int row, int color);

	/**
	 * 设置内容超宽时的显示方式，同时刷新已有行。
	 *
	 * @param enabled 真为单行省略显示，假为允许文字换行。
	 */
	@SimpleFunction
	void 置缩略显示内容(boolean enabled);

	/**
	 * 为一行保存独立业务标记，不改变显示内容。
	 *
	 * @param row 从 0 开始的数据行索引。
	 * @param tag 业务标记，空引用按空文本处理。
	 * @throws simple.runtime.errors.索引超出界限错误 行或列索引越界。
	 */
	@SimpleFunction
	void 置表格行标记(int row, String tag);

	/**
	 * 读取一行的业务标记。
	 *
	 * @param row 从 0 开始的数据行索引。
	 * @return 业务标记，未设置时为空文本。
	 * @throws simple.runtime.errors.索引超出界限错误 行或列索引越界。
	 */
	@SimpleFunction
	String 取表格行标记(int row);
}

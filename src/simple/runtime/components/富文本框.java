package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleDataElement;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;

/**
 * 可编辑的富文本框，支持字符区间格式、段落项目符、本地图片和链接事件。
 *
 * <p>格式直接作用于当前可编辑内容；索引从 0 开始，使用 UTF-16 半开区间。
 * 光标、输入法、基础文本操作和文本改变事件继承编辑框。内容为带格式的文本，非 HTML。
 * 在界面线程操作，读取文本属性只返回纯文本，不包含格式。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 富文本框 extends 编辑框 {

	/* 组件常量 */

	/**
	 * 正常字体样式。
	 */
	@SimpleDataElement
	int 字体样式_正常 = 1;

	/**
	 * 粗体字体样式。
	 */
	@SimpleDataElement
	int 字体样式_粗体 = 2;

	/**
	 * 斜体字体样式。
	 */
	@SimpleDataElement
	int 字体样式_斜体 = 3;

	/**
	 * 粗斜体字体样式。
	 */
	@SimpleDataElement
	int 字体样式_粗斜体 = 4;

	/* 组件事件 */

	/**
	 * 链接可点击为真时，点击链接派发此事件。
	 *
	 * @param url 链接保存的网址或业务标识。
	 */
	@SimpleEvent
	void 超链接被单击(String url);

	/* 组件属性 */

	/**
	 * 取得选区起点；没有选区时为光标位置，尚未定位时为 -1。
	 *
	 * @return UTF-16 起点。
	 */
	@SimpleProperty
	int 选区开始();

	/**
	 * 取得选区终点，该位置不包含在选区内。
	 *
	 * @return UTF-16 终点，尚未定位时为 -1。
	 */
	@SimpleProperty
	int 选区结束();

	/**
	 * 取得是否允许触摸激活文本中的链接。
	 *
	 * @return 真为允许链接点击。
	 */
	@SimpleProperty
	boolean 链接可点击();

	/**
	 * 切换链接点击；关闭时触摸链接文字用于定位编辑光标。
	 *
	 * @param enabled 是否允许链接触发事件，默认假。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "False"
	)
	void 链接可点击(boolean enabled);

	/* 组件方法 */

	/**
	 * 替换当前内容并清除原格式，新内容立即可编辑。
	 *
	 * @param text 新的纯文本，空引用按空文本处理。
	 */
	@SimpleFunction
	void 创建风格文本(String text);

	/**
	 * 在当前内容末尾追加文本，保留已有格式。
	 *
	 * @param text 追加的纯文本，空引用按空文本处理。
	 */
	@SimpleFunction
	void 追加风格文本(String text);

	/**
	 * 设置指定字符区间的文字颜色。
	 *
	 * @param color 文字的 ARGB 颜色。
	 * @param start 从 0 开始的 UTF-16 起点，包含该位置。
	 * @param end UTF-16 终点，不包含该位置，允许等于文本长度。
	 * @throws simple.runtime.errors.索引超出界限错误 字符区间越界或起点大于终点。
	 */
	@SimpleFunction
	void 置风格文本前景色(int color, int start, int end);

	/**
	 * 设置指定字符区间的背景颜色。
	 *
	 * @param color 文字背景的 ARGB 颜色。
	 * @param start 从 0 开始的 UTF-16 起点，包含该位置。
	 * @param end UTF-16 终点，不包含该位置，允许等于文本长度。
	 * @throws simple.runtime.errors.索引超出界限错误 字符区间越界或起点大于终点。
	 */
	@SimpleFunction
	void 置风格文本背景色(int color, int start, int end);

	/**
	 * 设置指定字符区间的字号。
	 *
	 * @param size 大于 0 的字号，单位 SP。
	 * @param start 从 0 开始的 UTF-16 起点，包含该位置。
	 * @param end UTF-16 终点，不包含该位置，允许等于文本长度。
	 * @throws simple.runtime.errors.索引超出界限错误 字符区间越界或起点大于终点。
	 * @throws IllegalArgumentException 字号不是正数。
	 */
	@SimpleFunction
	void 置风格文本字体大小(int size, int start, int end);

	/**
	 * 设置指定字符区间的字体样式。
	 *
	 * @param style 使用字体样式_正常、字体样式_粗体、字体样式_斜体或字体样式_粗斜体。
	 * @param start 从 0 开始的 UTF-16 起点，包含该位置。
	 * @param end UTF-16 终点，不包含该位置，允许等于文本长度。
	 * @throws simple.runtime.errors.索引超出界限错误 字符区间越界或起点大于终点。
	 * @throws IllegalArgumentException 字体样式不在 1～4 内。
	 */
	@SimpleFunction
	void 置风格文本字体样式(int style, int start, int end);

	/**
	 * 为指定字符区间设置下划线。
	 *
	 * @param start 从 0 开始的 UTF-16 起点，包含该位置。
	 * @param end UTF-16 终点，不包含该位置，允许等于文本长度。
	 * @throws simple.runtime.errors.索引超出界限错误 字符区间越界或起点大于终点。
	 */
	@SimpleFunction
	void 置风格文本下划线(int start, int end);

	/**
	 * 为指定字符区间设置删除线。
	 *
	 * @param start 从 0 开始的 UTF-16 起点，包含该位置。
	 * @param end UTF-16 终点，不包含该位置，允许等于文本长度。
	 * @throws simple.runtime.errors.索引超出界限错误 字符区间越界或起点大于终点。
	 */
	@SimpleFunction
	void 置风格文本删除线(int start, int end);

	/**
	 * 为指定字符区间设置上标。
	 *
	 * @param start 从 0 开始的 UTF-16 起点，包含该位置。
	 * @param end UTF-16 终点，不包含该位置，允许等于文本长度。
	 * @throws simple.runtime.errors.索引超出界限错误 字符区间越界或起点大于终点。
	 */
	@SimpleFunction
	void 置风格文本上标(int start, int end);

	/**
	 * 为指定字符区间设置下标。
	 *
	 * @param start 从 0 开始的 UTF-16 起点，包含该位置。
	 * @param end UTF-16 终点，不包含该位置，允许等于文本长度。
	 * @throws simple.runtime.errors.索引超出界限错误 字符区间越界或起点大于终点。
	 */
	@SimpleFunction
	void 置风格文本下标(int start, int end);

	/**
	 * 将指定字符区间设置为链接。
	 *
	 * @param url 非空链接标识；点击仅派发事件，不自动打开外部应用。
	 * @param start 从 0 开始的 UTF-16 起点，包含该位置。
	 * @param end UTF-16 终点，不包含该位置，允许等于文本长度。
	 * @throws simple.runtime.errors.索引超出界限错误 字符区间越界或起点大于终点。
	 * @throws IllegalArgumentException 链接标识为空。
	 */
	@SimpleFunction
	void 置风格文本超链接(String url, int start, int end);

	/**
	 * 为选中内容覆盖的完整段落添加项目符。
	 *
	 * @param color 项目符的 ARGB 颜色。
	 * @param start 从 0 开始的 UTF-16 起点，包含该位置。
	 * @param end UTF-16 终点，不包含该位置，允许等于文本长度。
	 * @throws simple.runtime.errors.索引超出界限错误 字符区间越界或起点大于终点。
	 */
	@SimpleFunction
	void 置风格文本项目符(int color, int start, int end);

	/**
	 * 以 24 DIP 方形图片显示非空字符区间，原始文本仍可读取。
	 *
	 * @param image 资源、资产或可访问文件的本地图片标识。
	 * @param start 从 0 开始的 UTF-16 起点，包含该位置。
	 * @param end UTF-16 终点，不包含该位置，允许等于文本长度。
	 * @throws simple.runtime.errors.索引超出界限错误 字符区间越界或起点大于终点。
	 * @throws IllegalArgumentException 尺寸不是正数或区间为空。
	 * @throws simple.runtime.errors.文件未存在错误 图片无法读取。
	 */
	@SimpleFunction
	void 置风格文本图片(String image, int start, int end);

	/**
	 * 以指定尺寸的图片显示非空字符区间。
	 *
	 * @param image 资源、资产或可访问文件的本地图片标识。
	 * @param width 大于 0 的图片显示宽度，单位 DIP。
	 * @param height 大于 0 的图片显示高度，单位 DIP。
	 * @param start 从 0 开始的 UTF-16 起点，包含该位置。
	 * @param end UTF-16 终点，不包含该位置，允许等于文本长度。
	 * @throws simple.runtime.errors.索引超出界限错误 字符区间越界或起点大于终点。
	 * @throws IllegalArgumentException 尺寸不是正数或区间为空。
	 * @throws simple.runtime.errors.文件未存在错误 图片无法读取。
	 */
	@SimpleFunction
	void 置风格文本图片(String image, int width, int height, int start, int end);

	/**
	 * 清除区间内的文字格式和链接，保留图片及区间外格式；项目符按完整段落移除。
	 *
	 * @param start 从 0 开始的 UTF-16 起点，包含该位置。
	 * @param end UTF-16 终点，不包含该位置，允许等于文本长度。
	 * @throws simple.runtime.errors.索引超出界限错误 字符区间越界或起点大于终点。
	 */
	@SimpleFunction
	void 清除文本格式(int start, int end);

	/**
	 * 取得当前选区的纯文本。
	 *
	 * @return 没有选区时返回空文本。
	 */
	@SimpleFunction
	String 取选中文本();
}

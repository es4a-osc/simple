package simple.runtime.components.impl.android;

import java.io.IOException;

import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Spanned;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.LinkMovementMethod;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.EditText;

import simple.runtime.components.富文本框;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.android.util.ImageUtil;
import simple.runtime.errors.文件未存在错误;
import simple.runtime.events.EventDispatcher;

/**
 * 在编辑框的同一可编辑文本上维护格式，复用基础编辑、输入法和文本改变事件。
 *
 * <p>字符格式按半开区间替换；局部修改拆分已有格式并保留区间外部分。
 * 链接只回调应用，图片只读取本地来源。所有调用须位于界面线程。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class 富文本框Impl extends 编辑框Impl implements 富文本框 {

	private boolean linksEnabled;

	/**
	 * 创建可编辑的富文本框。
	 *
	 * @param container 容纳此组件的容器。
	 */
	public 富文本框Impl(组件容器 container) {
		super(container);
		链接可点击(false);
	}

	/* 组件事件 */

	/**
	 * 将链接标识交给应用处理，不启动外部应用。
	 *
	 * @param url 被点击链接的标识。
	 */
	@Override
	public void 超链接被单击(String url) {
		EventDispatcher.dispatchEvent(this, "超链接被单击", url);
	}

	/* 组件属性 */

	/**
	 * 取得当前选区起点或光标位置。
	 *
	 * @return UTF-16 索引，尚未定位时为 -1。
	 */
	@Override
	public int 选区开始() {
		return editor().getSelectionStart();
	}

	/**
	 * 取得当前选区的排除终点。
	 *
	 * @return UTF-16 索引，尚未定位时为 -1。
	 */
	@Override
	public int 选区结束() {
		return editor().getSelectionEnd();
	}

	/**
	 * 取得链接触摸开关。
	 *
	 * @return 是否派发链接点击。
	 */
	@Override
	public boolean 链接可点击() {
		return linksEnabled;
	}

	/**
	 * 切换链接触摸模式；关闭后恢复通常的光标定位。
	 *
	 * @param enabled 是否启用链接点击。
	 */
	@Override
	public void 链接可点击(boolean enabled) {
		linksEnabled = enabled;
		editor().setLinksClickable(enabled);
		editor().setMovementMethod(enabled
			? LinkMovementMethod.getInstance() : ArrowKeyMovementMethod.getInstance());
	}

	/* 组件方法 */

	/**
	 * 替换当前内容并清除原格式，新内容立即可编辑。
	 *
	 * @param text 新的纯文本，空引用按空文本处理。
	 */
	@Override
	public void 创建风格文本(String text) {
		editor().setText(text == null ? "" : text);
	}

	/**
	 * 在当前内容末尾追加文本，保留已有格式。
	 *
	 * @param text 追加的纯文本，空引用按空文本处理。
	 */
	@Override
	public void 追加风格文本(String text) {
		content().append(text == null ? "" : text);
	}

	/**
	 * 设置指定字符区间的文字颜色。
	 *
	 * @param color 文字的 ARGB 颜色。
	 * @param start 从 0 开始的 UTF-16 起点，包含该位置。
	 * @param end UTF-16 终点，不包含该位置，允许等于文本长度。
	 * @throws simple.runtime.errors.索引超出界限错误 字符区间越界或起点大于终点。
	 */
	@Override
	public void 置风格文本前景色(int color, int start, int end) {
		replace(new ForegroundColorSpan(color), ForegroundColorSpan.class, start, end);
	}

	/**
	 * 设置指定字符区间的背景颜色。
	 *
	 * @param color 文字背景的 ARGB 颜色。
	 * @param start 从 0 开始的 UTF-16 起点，包含该位置。
	 * @param end UTF-16 终点，不包含该位置，允许等于文本长度。
	 * @throws simple.runtime.errors.索引超出界限错误 字符区间越界或起点大于终点。
	 */
	@Override
	public void 置风格文本背景色(int color, int start, int end) {
		replace(new BackgroundColorSpan(color), BackgroundColorSpan.class, start, end);
	}

	/**
	 * 设置指定字符区间的字号。
	 *
	 * @param size 大于 0 的字号，单位 SP。
	 * @param start 从 0 开始的 UTF-16 起点，包含该位置。
	 * @param end UTF-16 终点，不包含该位置，允许等于文本长度。
	 * @throws simple.runtime.errors.索引超出界限错误 字符区间越界或起点大于终点。
	 * @throws IllegalArgumentException 字号不是正数。
	 */
	@Override
	public void 置风格文本字体大小(int size, int start, int end) {
		if (size <= 0) {
			throw new IllegalArgumentException("字号必须大于 0");
		}

		int pixels = Math.max(1, Math.round(size * editor().getResources().getDisplayMetrics().scaledDensity));
		replace(new AbsoluteSizeSpan(pixels), AbsoluteSizeSpan.class, start, end);
	}

	/**
	 * 设置指定字符区间的字体样式。
	 *
	 * @param style 使用字体样式_正常、字体样式_粗体、字体样式_斜体或字体样式_粗斜体。
	 * @param start 从 0 开始的 UTF-16 起点，包含该位置。
	 * @param end UTF-16 终点，不包含该位置，允许等于文本长度。
	 * @throws simple.runtime.errors.索引超出界限错误 字符区间越界或起点大于终点。
	 * @throws IllegalArgumentException 字体样式不在 1～4 内。
	 */
	@Override
	public void 置风格文本字体样式(int style, int start, int end) {
		if (style < 字体样式_正常 || style > 字体样式_粗斜体) {
			throw new IllegalArgumentException("未知字体样式");
		}
		replace(new StyleSpan(style - 字体样式_正常), StyleSpan.class, start, end);
	}

	/**
	 * 为指定字符区间设置下划线。
	 *
	 * @param start 从 0 开始的 UTF-16 起点，包含该位置。
	 * @param end UTF-16 终点，不包含该位置，允许等于文本长度。
	 * @throws simple.runtime.errors.索引超出界限错误 字符区间越界或起点大于终点。
	 */
	@Override
	public void 置风格文本下划线(int start, int end) {
		replace(new UnderlineSpan(), UnderlineSpan.class, start, end);
	}

	/**
	 * 为指定字符区间设置删除线。
	 *
	 * @param start 从 0 开始的 UTF-16 起点，包含该位置。
	 * @param end UTF-16 终点，不包含该位置，允许等于文本长度。
	 * @throws simple.runtime.errors.索引超出界限错误 字符区间越界或起点大于终点。
	 */
	@Override
	public void 置风格文本删除线(int start, int end) {
		replace(new StrikethroughSpan(), StrikethroughSpan.class, start, end);
	}

	/**
	 * 为指定字符区间设置上标。
	 *
	 * @param start 从 0 开始的 UTF-16 起点，包含该位置。
	 * @param end UTF-16 终点，不包含该位置，允许等于文本长度。
	 * @throws simple.runtime.errors.索引超出界限错误 字符区间越界或起点大于终点。
	 */
	@Override
	public void 置风格文本上标(int start, int end) {
		check(start, end);
		remove(SubscriptSpan.class, start, end);
		replace(new SuperscriptSpan(), SuperscriptSpan.class, start, end);
	}

	/**
	 * 为指定字符区间设置下标。
	 *
	 * @param start 从 0 开始的 UTF-16 起点，包含该位置。
	 * @param end UTF-16 终点，不包含该位置，允许等于文本长度。
	 * @throws simple.runtime.errors.索引超出界限错误 字符区间越界或起点大于终点。
	 */
	@Override
	public void 置风格文本下标(int start, int end) {
		check(start, end);
		remove(SuperscriptSpan.class, start, end);
		replace(new SubscriptSpan(), SubscriptSpan.class, start, end);
	}

	/**
	 * 将指定字符区间设置为链接。
	 *
	 * @param url 非空链接标识；点击仅派发事件，不自动打开外部应用。
	 * @param start 从 0 开始的 UTF-16 起点，包含该位置。
	 * @param end UTF-16 终点，不包含该位置，允许等于文本长度。
	 * @throws simple.runtime.errors.索引超出界限错误 字符区间越界或起点大于终点。
	 * @throws IllegalArgumentException 链接标识为空。
	 */
	@Override
	public void 置风格文本超链接(String url, int start, int end) {
		if (url == null || url.length() == 0) {
			throw new IllegalArgumentException("链接标识不能为空");
		}
		replace(new EventLink(url), URLSpan.class, start, end);
	}

	/**
	 * 为选中内容覆盖的完整段落添加项目符。
	 *
	 * @param color 项目符的 ARGB 颜色。
	 * @param start 从 0 开始的 UTF-16 起点，包含该位置。
	 * @param end UTF-16 终点，不包含该位置，允许等于文本长度。
	 * @throws simple.runtime.errors.索引超出界限错误 字符区间越界或起点大于终点。
	 */
	@Override
	public void 置风格文本项目符(int color, int start, int end) {
		for (int[] range : RichTextRanges.paragraphs(content(), start, end)) {
			for (BulletSpan span : content().getSpans(range[0], range[1], BulletSpan.class)) {
				if (overlaps(span, range[0], range[1])) {
					content().removeSpan(span);
				}
			}
			content().setSpan(new BulletSpan(dp(8), color), range[0], range[1], Spanned.SPAN_PARAGRAPH);
		}
	}

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
	@Override
	public void 置风格文本图片(String image, int start, int end) {
		置风格文本图片(image, 24, 24, start, end);
	}

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
	@Override
	public void 置风格文本图片(String image, int width, int height, int start, int end) {
		check(start, end);
		if (start == end || width <= 0 || height <= 0) {
			throw new IllegalArgumentException("图片需要非空区间及正数尺寸");
		}

		// 先读取图片，失败时保留原内容和格式。图片区间重叠时整体替换旧图片。
		Drawable drawable;

		try {
			drawable = ImageUtil.getDrawable(image);
		} catch (IOException exception) {
			throw new 文件未存在错误(image == null ? "" : image);
		}

		if (drawable == null) {
			throw new 文件未存在错误(image == null ? "" : image);
		}

		drawable.setBounds(0, 0, dp(width), dp(height));
		for (ImageSpan span : content().getSpans(start, end, ImageSpan.class)) {
			if (overlaps(span, start, end)) {
				content().removeSpan(span);
			}
		}
		content().setSpan(new ImageSpan(drawable, image, ImageSpan.ALIGN_BASELINE), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
	}

	/**
	 * 清除区间内的文字格式和链接，保留图片及区间外格式；项目符按完整段落移除。
	 *
	 * @param start 从 0 开始的 UTF-16 起点，包含该位置。
	 * @param end UTF-16 终点，不包含该位置，允许等于文本长度。
	 * @throws simple.runtime.errors.索引超出界限错误 字符区间越界或起点大于终点。
	 */
	@Override
	public void 清除文本格式(int start, int end) {
		check(start, end);
		if (start == end) {
			return;
		}

		for (CharacterStyle span : content().getSpans(start, end, CharacterStyle.class)) {
			// 图片保留；不触碰输入法的组合标记、选择标记和文本观察器。
			if (!(span.getUnderlying() instanceof ImageSpan)) {
				removePart(span, start, end);
			}
		}
		for (int[] range : RichTextRanges.paragraphs(content(), start, end)) {
			for (BulletSpan span : content().getSpans(range[0], range[1], BulletSpan.class)) {
				if (overlaps(span, range[0], range[1])) {
					content().removeSpan(span);
				}
			}
		}
	}

	/**
	 * 取得当前选区的纯文本。
	 *
	 * @return 没有选区时返回空文本。
	 */
	@Override
	public String 取选中文本() {
		int start = 选区开始();
		int end = 选区结束();
		if (start < 0 || end < 0 || start == end) {
			return "";
		}

		return content().subSequence(Math.min(start, end), Math.max(start, end)).toString();
	}

	/* 可编辑文本与区间格式 */

	/**
	 * 取得富文本框的原生编辑控件。
	 *
	 * @return 富文本编辑控件
	 */
	private EditText editor() {
		return (EditText) getView();
	}

	/**
	 * 取得当前可编辑的富文本内容。
	 *
	 * @return 可编辑的文本与样式
	 */
	private Editable content() {
		return editor().getText();
	}

	/**
	 * 将 DIP 长度换算为像素。
	 *
	 * @param value 需要换算的 DIP 长度。
	 * @return 换算后的像素值
	 */
	private int dp(int value) {
		return Math.max(1, Math.round(value * editor().getResources().getDisplayMetrics().density));
	}

	/**
	 * 校验半开样式区间是否位于当前文本内。
	 *
	 * @param start 区间起始位置，包含该位置
	 * @param end 区间结束位置，不包含该位置
	 */
	private void check(int start, int end) {
		RichTextRanges.check(content().length(), start, end);
	}

	/**
	 * 判断样式与指定文本区间是否重叠。
	 *
	 * @param span 文本样式对象
	 * @param start 区间起始位置，包含该位置
	 * @param end 区间结束位置，不包含该位置
	 * @return 样式与指定文本区间重叠时为 {@code true}，否则为 {@code false}
	 */
	private boolean overlaps(Object span, int start, int end) {
		return content().getSpanStart(span) < end && content().getSpanEnd(span) > start;
	}

	/**
	 * 替换指定区间内同类样式，保留区间外的样式片段。
	 *
	 * @param style 要应用的样式
	 * @param kind 操作或数据类型
	 * @param start 区间起始位置，包含该位置
	 * @param end 区间结束位置，不包含该位置
	 */
	private void replace(CharacterStyle style, Class<? extends CharacterStyle> kind, int start, int end) {
		check(start, end);
		if (start == end) {
			return;
		}

		remove(kind, start, end);
		content().setSpan(style, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
	}

	/**
	 * 移除指定区间内的同类样式。
	 *
	 * @param kind 操作或数据类型
	 * @param start 区间起始位置，包含该位置
	 * @param end 区间结束位置，不包含该位置
	 */
	private void remove(Class<? extends CharacterStyle> kind, int start, int end) {
		if (start == end) {
			return;
		}
		for (CharacterStyle span : content().getSpans(start, end, CharacterStyle.class)) {
			if (kind.isInstance(span.getUnderlying())) {
				removePart(span, start, end);
			}
		}
	}

	/**
	 * 裁剪与指定区间重叠的样式片段。
	 *
	 * @param span 文本样式对象
	 * @param start 区间起始位置，包含该位置
	 * @param end 区间结束位置，不包含该位置
	 */
	private void removePart(CharacterStyle span, int start, int end) {
		if (!overlaps(span, start, end)) {
			return;
		}

		int[][] parts = RichTextRanges.remaining(content().getSpanStart(span),
			content().getSpanEnd(span), start, end);
		content().removeSpan(span);
		for (int[] part : parts) {
			content().setSpan(copy(span.getUnderlying()), part[0], part[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
	}

	/**
	 * 复制已有样式的属性。
	 *
	 * <p>每段使用独立对象；链接拆分后仍须派发本组件事件。
	 *
	 * @param span 文本样式对象
	 * @return 保留原样式属性的新样式对象
	 */
	private CharacterStyle copy(CharacterStyle span) {
		if (span instanceof URLSpan) {
			return new EventLink(((URLSpan) span).getURL());
		}
		if (span instanceof ForegroundColorSpan) {
			return new ForegroundColorSpan(((ForegroundColorSpan) span).getForegroundColor());
		}
		if (span instanceof BackgroundColorSpan) {
			return new BackgroundColorSpan(((BackgroundColorSpan) span).getBackgroundColor());
		}
		if (span instanceof AbsoluteSizeSpan) {
			AbsoluteSizeSpan size = (AbsoluteSizeSpan) span;
			return new AbsoluteSizeSpan(size.getSize(), size.getDip());
		}
		if (span instanceof StyleSpan) {
			return new StyleSpan(((StyleSpan) span).getStyle());
		}
		if (span instanceof UnderlineSpan) {
			return new UnderlineSpan();
		}
		if (span instanceof StrikethroughSpan) {
			return new StrikethroughSpan();
		}
		if (span instanceof SuperscriptSpan) {
			return new SuperscriptSpan();
		}
		if (span instanceof SubscriptSpan) {
			return new SubscriptSpan();
		}

		return CharacterStyle.wrap(span);
	}

	/* 应用内链接 */

	/**
	 * 将富文本链接点击转发为组件事件的可点击样式。
	 */
	private final class EventLink extends URLSpan {

		/**
		 * 保存可在点击事件中返回的链接地址。
		 *
		 * @param url 目标地址
		 */
		EventLink(String url) {
			super(url);
		}

		@Override
		public void onClick(View widget) {
			if (linksEnabled) {
				超链接被单击(getURL());
			}
		}
	}
}

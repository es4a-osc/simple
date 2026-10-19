package simple.runtime;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import simple.runtime.android.MainActivity;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

/**
 * 系统剪贴板的纯文本读写函数。
 *
 * <p>不申请额外权限。读取受 Android 焦点与隐私规则限制；空结果不代表剪贴板一定为空。
 * 只读取第一项现成文本，不加载 URI、图片或意图，也不监听后台变化。</p>
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 剪贴板操作 {

	/**
	 * 静态方法集，不创建实例。
	 */
	private 剪贴板操作() {
	}

	/* 文本读写 */

	/**
	 * 用一项纯文本替换系统剪贴板的当前内容。
	 *
	 * <p>空文本会写入一个空文本项目，不等同于清除剪贴板。写入内容可能被其他应用读取，
	 * 不应把密码等敏感信息作为普通复制文本。</p>
	 *
	 * @param text 要复制的文本；为 null 时按空文本处理。
	 * @throws IllegalStateException 当前应用上下文或剪贴板服务不可用。
	 * @throws SecurityException 系统拒绝本次写入。
	 */
	@SimpleFunction
	public static void 置剪贴板文本(String text) {
		ClipboardManager clipboard = getClipboard();
		clipboard.setPrimaryClip(ClipData.newPlainText("Simple", text == null ? "" : text));
	}

	/**
	 * 读取剪贴板第一项现成文本，保留空白、换行和 Unicode 字符。
	 *
	 * <p>没有项目、第一项不含文本或系统限制读取时返回空文本；不将 URI、图片或意图转换为文本。
	 * 应由用户在应用具有输入焦点时主动触发，后台读取可能得不到内容。
	 * 系统可能显示读取提示；本方法不记录或缓存读取结果。</p>
	 *
	 * @return 第一项文本；没有可读取的文本时返回空文本。
	 * @throws IllegalStateException 当前应用上下文或剪贴板服务不可用。
	 */
	@SimpleFunction
	public static String 取剪贴板文本() {
		ClipboardManager clipboard = getClipboard();

		// 只处理系统明确允许读取的当前内容，不探测其他来源。
		try {
			ClipData clip = clipboard.getPrimaryClip();
			if (clip == null || clip.getItemCount() == 0) {
				return "";
			}

			CharSequence text = clip.getItemAt(0).getText();
			return text == null ? "" : text.toString();
		} catch (SecurityException e) {
			return "";
		}
	}

	/**
	 * 判断剪贴板第一项是否有当前可读取的非空文本。
	 *
	 * <p>空格与换行也属于文本。该判断会读取内容，受与取剪贴板文本相同的系统限制；
	 * 返回假不能区分空内容、非文本项目和系统限制，两次调用之间内容也可能变化。</p>
	 *
	 * @return 可读取的第一项文本长度大于零时返回真，否则返回假。
	 * @throws IllegalStateException 当前应用上下文或剪贴板服务不可用。
	 */
	@SimpleFunction
	public static boolean 剪贴板是否有文本() {
		return !取剪贴板文本().isEmpty();
	}

	/* 系统服务 */

	/**
	 * 每次按当前应用获取服务，不保存 Activity 或剪贴板内容。
	 *
	 * @return 系统剪贴板服务
	 */
	private static ClipboardManager getClipboard() {
		Context context = MainActivity.getContext();
		if (context == null) {
			throw new IllegalStateException("当前应用上下文不可用");
		}

		ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
		if (clipboard == null) {
			throw new IllegalStateException("系统剪贴板服务不可用");
		}

		return clipboard;
	}
}

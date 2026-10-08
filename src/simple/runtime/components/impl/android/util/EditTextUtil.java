package simple.runtime.components.impl.android.util;

import simple.runtime.android.MainActivity;
import simple.runtime.components.组件;

import android.content.Context;
import android.text.InputType;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class EditTextUtil {

	public static boolean isEnabled(EditText editText) {
		return editText.isEnabled();
	}

	public static void setEnabled(EditText editText, boolean enabled) {
		editText.setEnabled(enabled);
		editText.setFocusable(enabled);
		editText.setFocusableInTouchMode(enabled);
		editText.invalidate();
	}

	public static String getHintText(EditText editText) {
		return editText.getHint().toString();
	}

	public static void setHintText(EditText editText, String hint) {
		editText.setHint(hint);
		editText.invalidate();
	}

	public static void setHintTextColor(EditText editText, int color) {
		editText.setHintTextColor(color);
		editText.invalidate();
	}

	public static boolean isCursorVisible(EditText editText) {
		return editText.isCursorVisible();
	}

	public static void setCursorVisible(EditText editText, boolean visible) {
		editText.setCursorVisible(visible);
	}

	/**
	 * 设置输入模式。
	 *
	 * <p>参考文档：
	 * {@link https://www.jianshu.com/p/7d896821b413}
	 * {@link https://developer.android.google.cn/reference/android/text/InputType.html}
	 *
	 * @param textview 文本视图。
	 * @param inputModel 当前输入模式，可选值请参考{@link 组件#输入模式_文本}等常量。
	 */
	public static void setInputModel(EditText editText, int inputModel) {
		// 保持多行输入
		int inputType = editText.getInputType() & (InputType.TYPE_MASK_CLASS | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
		switch (inputModel) {
			default:
				throw new IllegalArgumentException();
			case 组件.输入模式_文本:
				inputType |= InputType.TYPE_CLASS_TEXT;
				break;
			case 组件.输入模式_数字:
				inputType |= InputType.TYPE_CLASS_NUMBER;
				break;
			case 组件.输入模式_邮箱:
				inputType |= InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
				break;
			case 组件.输入模式_密码:
				inputType |= InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
				break;
			case 组件.输入模式_链接:
				inputType |= InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI;
				break;
		}
		// 保持光标位置
		int index = editText.getSelectionStart();
		editText.setInputType(inputType);
		editText.setSelection(index);
		editText.invalidate();
	}

	/**
	 * 是否为多行输入。
	 *
	 * @param editText 编辑框实例。
	 * @return 多行为{@code true}，单行为{@code false}。
	 */
	public static boolean isMultilineInput(EditText editText) {
		int multilineType = InputType.TYPE_MASK_CLASS | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
		return (editText.getInputType() & multilineType) == multilineType;
	}

	/**
	 * 设置是否多行输入。
	 *
	 * @param editText 编辑框实例。
	 * @param isMultiline 多行为{@code true}，单行为{@code false}。
	 */
	public static void setMultilineInput(EditText editText, boolean isMultiline) {
		setInputType(editText, isMultiline, InputType.TYPE_MASK_CLASS | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
	}

	/**
	 * 设置输入类型。
	 *
	 * @param editText 编辑框实例。
	 * @param set {@code true}为设置输入类型，{@code false}为移除输入类型
	 * @param inputType 输入类型。
	 */
	public static void setInputType(EditText editText, boolean set, int inputType) {
		// 移除类型
		int type = editText.getInputType() & ~inputType;
		if (set) {
			// 加入类型
			type |= inputType;
		}
		// 保持光标位置
		int index = editText.getSelectionStart();
		editText.setInputType(type);
		editText.setSelection(index);
		editText.invalidate();
	}

	/**
	 * 获取光标开始位置。
	 *
	 * @param editText 编辑框对象。
	 * @return 开始位置。
	 */
	public static int getSelectionStart(EditText editText) {
		return editText.getSelectionStart();
	}

	/**
	 * 设置光标开始位置。
	 *
	 * @param editText 编辑框对象。
	 * @param location 开始位置。
	 */
	public static void setSelection(EditText editText, int location) {
		editText.setSelection(location);
	}

	/**
	 * 获取光标结束位置。
	 *
	 * @param editText 编辑框对象。
	 * @return 结束位置。
	 */
	public static int getSelectionEnd(EditText editText) {
		return editText.getSelectionEnd();
	}

	/**
	 * 设置光标选择文本。
	 *
	 * @param editText 编辑框对象。
	 * @param start 开始位置。
	 * @param stop 停止位置。
	 */
	public static void setSelection(EditText editText, int start, int stop) {
		editText.setSelection(start, stop);
	}

	/**
	 * 设置光标选择所有文本。
	 *
	 * @param editText 编辑框对象。
	 */
	public static void selectAll(EditText editText) {
		editText.selectAll();
	}

	/**
	 * 向编辑框中追加文本。
	 *
	 * @param editText 编辑框对象。
	 * @param text 追加文本。
	 */
	public static void appendText(EditText editText, String text) {
		editText.getText().append(text);
	}

	/**
	 * 向编辑框中指定位置插入文本。
	 *
	 * @param editText 编辑框对象。
	 * @param location 插入位置。
	 * @param text 插入文本。
	 */
	public static void insertText(EditText editText, int location, String text) {
		editText.getText().insert(location, text);
	}

	/**
	 * 删除指定范围的文本。
	 *
	 * @param editText 编辑框对象。
	 * @param start 开始位置。
	 * @param stop 停止位置。
	 */
	public static void deleteText(EditText editText, int start, int stop) {
		editText.getText().delete(start, stop);
	}

	/**
	 * 显示输入法。
	 *
	 * @param editText 编辑框对象。
	 * @param isForced
	 */
	public static void showSoftInput(EditText editText, boolean isForced) {
		InputMethodManager imm = (InputMethodManager) MainActivity.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		// 显示输入法
		imm.showSoftInput(editText, isForced ? InputMethodManager.SHOW_FORCED : 0);
	}

	/**
	 * 隐藏输入法 。
	 *
	 * @param editText 编辑框对象。
	 * @param isAlways
	 */
	public static void hideSoftInput(EditText editText, boolean isAlways) {
		InputMethodManager imm = (InputMethodManager) MainActivity.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		// 隐藏输入法
		imm.hideSoftInputFromWindow(editText.getWindowToken(), isAlways ? InputMethodManager.HIDE_NOT_ALWAYS : 0);
	}

}

package simple.runtime;

import java.util.Calendar;

import simple.runtime.android.MainActivity;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.parameters.DateReferenceParameter;
import simple.runtime.components.窗口;
import simple.runtime.components.组件;
import simple.runtime.components.impl.android.窗口Impl;
import simple.runtime.components.impl.android.util.EditTextUtil;

import android.widget.EditText;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;

/**
 * 实现各对话框。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 对话框操作 {

	// 当前 Toast 对象
	private static Toast toast;

	/**
	 * 静态方法集，不创建实例。
	 */
	private 对话框操作() {
	}

	/**
	 * 显示一个无焦点的提示框。
	 *
	 * @param text 提示文本。
	 */
	@SimpleFunction
	public static void 弹出提示框(String text) {
		Toast.makeText(MainActivity.getContext(), text, Toast.LENGTH_LONG).show();
	}

	/**
	 * 立即显示一个无焦点的提示框。
	 *
	 * @param text 提示文本。
	 */
	@SimpleFunction
	public static void 显示提示框(String text) {
		if (toast != null) {
			// 注销之前显示的那条信息
			toast.cancel();
		}

		toast = Toast.makeText(MainActivity.getContext(), text, Toast.LENGTH_LONG);
		toast.show();
	}

	/**
	 * 显示使用“确认”按钮的提示对话框。
	 *
	 * @param title 对话框标题。
	 * @param message 对话框信息。
	 */
	@SimpleFunction
	public static void 弹出确认框(String title, String message) {
		弹出确认框(title, message, "确认");
	}

	/**
	 * 弹出具备确认按钮的对话框。
	 *
	 * @param title 对话框的标题。
	 * @param message 对话框的信息。
	 * @param btnOK 确认按钮的标题。
	 */
	@SimpleFunction
	public static void 弹出确认框(String title, String message, String btnOK) {
		// 结构并显示对话框
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.getContext());
		builder.setCancelable(false);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setPositiveButton(btnOK, null);
		builder.show();
	}

	/**
	 * 显示使用“是”和“否”按钮的对话框，沿用阻塞等待约定。
	 *
	 * <p>不能在 Simple 的错误处理语句中调用。</p>
	 *
	 * @param title 对话框标题。
	 * @param message 对话框信息。
	 * @return 选择是返回真，选择否返回假。
	 */
	@SimpleFunction
	public static boolean 弹出是否框(String title, String message) {
		return 弹出是否框(title, message, "是", "否");
	}

	/**
	 * 弹出具备是否按钮的对话框。
	 *
	 * <p>注意会堵塞等待结果，无法在{@code 位于 错误}语句中使用。
	 *
	 * @param title 对话框的标题。
	 * @param message 对话框的信息。
	 * @param btnOK 是按钮的标题。
	 * @param btnNO 否按钮的标题。
	 * @return 点击是返回{@code true}，否则返回{@code false}。
	 */
	@SimpleFunction
	public static boolean 弹出是否框(String title, String message, String btnOK, String btnNO) {
		// 创建一个堵塞等待
		WaitEnd wait = new WaitEnd();

		// 结构并显示对话框
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.getContext());
		builder.setCancelable(false);
		builder.setTitle(title);
		builder.setMessage(message);
		builder.setPositiveButton(btnOK, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				wait.end(true);
			}
		});
		builder.setNegativeButton(btnNO, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				wait.end(false);
			}
		});
		builder.show();

		// 开始等待
		return (boolean) wait.start();
	}

	/**
	 * 显示单选列表，初始不选中任何项目，沿用确认与取消约定。
	 *
	 * <p>不能在 Simple 的错误处理语句中调用。</p>
	 *
	 * @param title 对话框标题。
	 * @param items 可选项目文本。
	 * @return 确认返回所选索引；取消或未选择返回 -1。
	 */
	@SimpleFunction
	public static int 弹出单选框(String title, String[] items) {
		return 弹出单选框(title, items, -1);
	}

	/**
	 * 弹出具备单选列表的对话框。
	 *
	 * <p>注意会堵塞等待结果，无法在{@code 位于 错误}语句中使用。
	 *
	 * @param title 对话框的标题。
	 * @param items 单选项目文本。
	 * @param checkedItem 选择项目索引。
	 * @return 返回选中项目索引。
	 */
	@SimpleFunction
	public static int 弹出单选框(String title, String[] items, int checkedItem) {
		// 创建一个等待
		WaitEnd wait = new WaitEnd();
		// 初始缺省值
		wait.expand = checkedItem;

		// 构造并显示对话框
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.getContext());
		builder.setCancelable(false);
		builder.setTitle(title);
		builder.setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				wait.expand = which;
			}
		});
		builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				wait.end(true);
			}
		});
		builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				wait.end(false);
			}
		});
		builder.show();

		// 开始等待
		if ((boolean) wait.start()) {
			return (int) wait.expand;
		} else {
			return -1;
		}
	}

	/**
	 * 显示多选列表，初始项目全部不选中，沿用确认与取消约定。
	 *
	 * <p>不能在 Simple 的错误处理语句中调用。</p>
	 *
	 * @param title 对话框标题。
	 * @param items 可选项目文本。
	 * @return 确认返回各项选中状态，取消返回空引用。
	 */
	@SimpleFunction
	public static boolean[] 弹出多选框(String title, String[] items) {
		return 弹出多选框(title, items, new boolean[items.length]);
	}

	/**
	 * 弹出具备多选列表的对话框。
	 *
	 * <p>注意会堵塞等待结果，无法在{@code 位于 错误}语句中使用。
	 *
	 * @param title 对话框的标题。
	 * @param items 多选项目文本。
	 * @param checkedItems 初始项目选中状态。
	 * @return 返回项目项目选中状态。
	 */
	@SimpleFunction
	public static boolean[] 弹出多选框(String title, String[] items, boolean[] checkedItems) {
		// 创建一个等待
		WaitEnd wait = new WaitEnd();

		// 构造并显示对话框
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.getContext());
		builder.setCancelable(false);
		builder.setTitle(title);
		builder.setMultiChoiceItems(items, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				checkedItems[which] = isChecked;
			}
		});
		builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				wait.end(true);
			}
		});
		builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				wait.end(false);
			}
		});
		builder.show();

		// 开始等待
		if ((boolean) wait.start()) {
			return checkedItems;
		} else {
			return null;
		}
	}

	/**
	 * 显示单行文本输入框，使用文本输入模式。
	 *
	 * <p>沿用完整方法的阻塞等待约定。</p>
	 *
	 * @param title 对话框标题。
	 * @param hint 输入框提示。
	 * @return 确认返回输入内容，取消返回空引用。
	 */
	@SimpleFunction
	public static String 弹出输入框(String title, String hint) {
		return 弹出输入框(title, hint, false, 组件.输入模式_文本);
	}

	/**
	 * 显示文本输入框，使用文本输入模式并显式指定是否允许多行。
	 *
	 * <p>沿用完整方法的阻塞等待约定。</p>
	 *
	 * @param title 对话框标题。
	 * @param hint 输入框提示。
	 * @param multiline 是否允许多行输入。
	 * @return 确认返回输入内容，取消返回空引用。
	 */
	@SimpleFunction
	public static String 弹出输入框(String title, String hint, boolean multiline) {
		return 弹出输入框(title, hint, multiline, 组件.输入模式_文本);
	}

	/**
	 * 弹出具备输入框的对话框。
	 *
	 * @param title 对话框的标题。
	 * @param hint 编辑框提示文本。
	 * @param multiline 编辑框是否允许多行输入。
	 * @param model 编辑框输入模式，可选值请参考{@link 组件#输入模式_文本}等常量。
	 * @return 确认返回输入文本，否则返回{@code null}。
	 */
	@SimpleFunction
	public static String 弹出输入框(String title, String hint, boolean multiline, int model) {
		// 创建一个编辑框
		EditText edit = new EditText(MainActivity.getContext());
		edit.setFocusable(true);
		edit.setHint(hint);
		EditTextUtil.setMultilineInput(edit, multiline);
		EditTextUtil.setInputModel(edit, model);

		// 创建一个线性布局
		LinearLayout layout = new LinearLayout(MainActivity.getContext());
		layout.setPadding(像素转换.到绝对像素(10), 像素转换.到绝对像素(5),
				像素转换.到绝对像素(10), 像素转换.到绝对像素(5));
		// 设置布局方向垂直
		layout.setOrientation(LinearLayout.VERTICAL);
		// 将编辑框添加到布局中
		layout.addView(edit, new LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

		// 创建一个等待
		WaitEnd wait = new WaitEnd();

		// 结构并显示对话框
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.getContext());
		builder.setCancelable(false);
		builder.setTitle(title);
		builder.setView(layout);
		builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				wait.end(true);
			}
		});
		builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				wait.end(false);
			}
		});
		builder.show();

		// 开始等待
		if((boolean) wait.start()) {
			return edit.getText().toString();
		} else {
			return null;
		}
	}

	/* 日期与时间选择 */

	/**
	 * 显示日期选择框，以设备当前日期时间作为初值。
	 *
	 * <p>须在界面线程调用，沿用对话框等待返回的约定，不在错误处理语句中调用。</p>
	 *
	 * @param title 对话框标题。
	 * @param result 接收确认结果的日期时间变量，仅确认时被替换；可与初值使用同一变量。
	 * @return 确认返回真并写入新对象，保留初始时间和时区；取消返回假，结果变量不变。
	 */
	@SimpleFunction
	public static boolean 弹出日期选择框(String title, DateReferenceParameter result) {
		return 弹出日期选择框(title, Calendar.getInstance(), result);
	}

	/**
	 * 显示日期选择框，以指定日期时间的年月日作为初值。
	 *
	 * <p>不修改初值，确认后仅替换年月日，保留时间和时区；
	 * 日期字段沿用 Calendar 的月份编号，一月为 0。
	 * 须在界面线程调用，不在错误处理语句中调用。</p>
	 *
	 * @param title 对话框标题。
	 * @param initial 初始日期时间，不得为空。
	 * @param result 接收确认结果的日期时间变量，仅确认时被替换；可与初值使用同一变量。
	 * @return 确认返回真并写入新对象；取消按钮、返回键或框外点击返回假，结果变量不变。
	 * @throws IllegalArgumentException 初值或结果参数为空，初值无效或超出平台日期选择器范围。
	 * @throws IllegalStateException 当前不是界面线程，或没有可用活动。
	 */
	@SimpleFunction
	public static boolean 弹出日期选择框(String title, Calendar initial, DateReferenceParameter result) {
		Calendar value = copyPickerInitial(initial);
		if (result == null) {
			throw new IllegalArgumentException("结果参数不得为空");
		}

		MainActivity activity = requirePickerActivity();
		PickerWait wait = new PickerWait();
		DatePickerDialog dialog = new DatePickerDialog(activity, null,
			value.get(Calendar.YEAR), value.get(Calendar.MONTH), value.get(Calendar.DAY_OF_MONTH));
		DatePicker picker = dialog.getDatePicker();
		if (picker.getYear() != value.get(Calendar.YEAR)
			|| picker.getMonth() != value.get(Calendar.MONTH)
			|| picker.getDayOfMonth() != value.get(Calendar.DAY_OF_MONTH)) {
			throw new IllegalArgumentException("初始日期超出平台日期选择器范围");
		}

		// 仅确认按钮读取日期，避免旧平台在关闭时回调导致取消被误判。
		dialog.setTitle(title);
		dialog.setButton(DialogInterface.BUTTON_POSITIVE, "确认", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface source, int which) {
				picker.clearFocus();
				wait.selected = selectPickerDate(value, picker.getYear(),
					picker.getMonth(), picker.getDayOfMonth());
				wait.confirmed = true;
			}
		});
		return commitPickerResult(wait.showAndWait(dialog), result);
	}

	/**
	 * 显示时间选择框，以设备当前时间和系统 12／24 小时制作为初值。
	 *
	 * <p>须在界面线程调用，不在错误处理语句中调用。</p>
	 *
	 * @param title 对话框标题。
	 * @param result 接收确认结果的日期时间变量，仅确认时被替换；可与初值使用同一变量。
	 * @return 确认返回真并写入新对象，保留日期和时区，秒及毫秒归零；取消返回假，结果变量不变。
	 */
	@SimpleFunction
	public static boolean 弹出时间选择框(String title, DateReferenceParameter result) {
		return 弹出时间选择框(title, Calendar.getInstance(), result);
	}

	/**
	 * 显示时间选择框，使用指定初值及系统 12／24 小时制。
	 *
	 * <p>不修改初值；须在界面线程调用，不在错误处理语句中调用。</p>
	 *
	 * @param title 对话框标题。
	 * @param initial 初始日期时间，不得为空。
	 * @param result 接收确认结果的日期时间变量，仅确认时被替换；可与初值使用同一变量。
	 * @return 确认返回真并写入新对象，保留日期和时区，秒及毫秒归零；取消返回假，结果变量不变。
	 * @throws IllegalArgumentException 初值或结果参数为空，或初值无效。
	 * @throws IllegalStateException 当前不是界面线程，或没有可用活动。
	 */
	@SimpleFunction
	public static boolean 弹出时间选择框(String title, Calendar initial, DateReferenceParameter result) {
		Calendar value = copyPickerInitial(initial);
		return 弹出时间选择框(title, value,
			android.text.format.DateFormat.is24HourFormat(requirePickerActivity()), result);
	}

	/**
	 * 显示时间选择框，显式指定初值及 12／24 小时制。
	 *
	 * <p>确认时保留初值的日期和时区，秒及毫秒归零，不修改初值。
	 * 12 小时制只影响界面显示，返回对象的小时仍可按 0–23 读取。
	 * 须在界面线程调用，不在错误处理语句中调用。</p>
	 *
	 * @param title 对话框标题。
	 * @param initial 初始日期时间，不得为空。
	 * @param is24Hour 真表示 24 小时制，假表示 12 小时制。
	 * @param result 接收确认结果的日期时间变量，仅确认时被替换；可与初值使用同一变量。
	 * @return 确认返回真并写入新对象；取消按钮、返回键或框外点击返回假，结果变量不变。
	 * @throws IllegalArgumentException 初值或结果参数为空，或初值无效。
	 * @throws IllegalStateException 当前不是界面线程，或没有可用活动。
	 */
	@SimpleFunction
	public static boolean 弹出时间选择框(String title, Calendar initial,
		boolean is24Hour, DateReferenceParameter result) {
		Calendar value = copyPickerInitial(initial);
		if (result == null) {
			throw new IllegalArgumentException("结果参数不得为空");
		}

		MainActivity activity = requirePickerActivity();
		PickerWait wait = new PickerWait();
		TimePickerDialog dialog = new TimePickerDialog(activity, new TimePickerDialog.OnTimeSetListener() {

			@Override
			public void onTimeSet(TimePicker picker, int hour, int minute) {
				// 部分旧平台在 onStop 也调用监听器，只有明确确认才接受结果。
				if (wait.confirmed) {
					wait.selected = selectPickerTime(value, hour, minute);
				}
			}
		}, value.get(Calendar.HOUR_OF_DAY), value.get(Calendar.MINUTE), is24Hour) {

			@Override
			public void onClick(DialogInterface source, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					wait.confirmed = true;
					// show() 会重设确认按钮监听，须在原生提交入口标记确认并提交编辑值。
					android.view.View focused = getCurrentFocus();
					if (focused != null) {
						focused.clearFocus();
					}
				}
				super.onClick(source, which);
			}
		};

		dialog.setTitle(title);
		dialog.setButton(DialogInterface.BUTTON_POSITIVE, "确认", dialog);
		return commitPickerResult(wait.showAndWait(dialog), result);
	}

	/**
	 * 弹出具备进度圈的对话框。
	 *
	 * @param title 对话框的标题。
	 * @param message 对话框的信息。
	 * @return 进度框对象实例。
	 */
	@SimpleFunction
	public static Object 弹出进度框(String title, String message) {
		// 创建进度圈
		ProgressBar progress = new ProgressBar(MainActivity.getContext());
		// 不确定进度，无限转圈圈吧！
		progress.setIndeterminate(false);

		// 创建标签
		TextView text = new TextView(MainActivity.getContext());
		text.setGravity(Gravity.CENTER_VERTICAL);
		text.setTextColor(组件.颜色_黑);
		text.setText(message);

		// 创建线性布局
		LinearLayout layout = new LinearLayout(MainActivity.getContext());
		layout.setPadding(像素转换.到绝对像素(15), 像素转换.到绝对像素(20),
				像素转换.到绝对像素(15), 像素转换.到绝对像素(20));

		// 将进度圈添加到布局中
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
		params.rightMargin = 像素转换.到绝对像素(15);
		layout.addView(progress, params);

		// 将标签添加到布局中
		layout.addView(text, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

		// 结构并显示对话框
		AlertDialog dialog = new AlertDialog.Builder(MainActivity.getContext()).create();
		// 将标签的标识设置为对话框对象哈希码，用于后续便捷查找标签设置文本。
		text.setId(dialog.hashCode());
		dialog.setCancelable(false);
		dialog.setTitle(title);
		dialog.setView(layout);
		dialog.show();
		return dialog;
	}

	/**
	 * 设置进度框的信息。
	 *
	 * @param object 进度框对象实例。
	 * @param message 对话框的信息。
	 */
	@SimpleFunction
	public static void 置进度框信息(Object object, String message) {
		if (object instanceof AlertDialog) {
			AlertDialog dialog = (AlertDialog) object;
			// 用对话框对象哈希码为视图标识去查找标签
			TextView text = dialog.findViewById(dialog.hashCode());
			text.setText(message);
		} else {
			throw new IllegalArgumentException("进度框对象错误，设置信息失败");
		}
	}

	/**
	 * 关闭进度框。
	 *
	 * @param object 进度框对象实例。
	 */
	@SimpleFunction
	public static void 关闭进度框(Object object) {
		if (object instanceof AlertDialog) {
			((AlertDialog) object).dismiss();
		} else {
			throw new IllegalArgumentException("进度框对象错误，关闭进度框失败");
		}
	}

	/**
	 * 显示嵌入窗口的对话框，按钮为“确认”和“取消”。
	 *
	 * <p>不能在 Simple 的错误处理语句中调用。</p>
	 *
	 * @param title 对话框标题。
	 * @param form 嵌入对话框的窗口对象。
	 * @return 确认返回真，取消返回假。
	 */
	@SimpleFunction
	public static boolean 弹出对话框(String title, 窗口 form) {
		return 弹出对话框(title, form, "确认", "取消");
	}

	/**
	 * 弹出嵌入窗口的对话框。
	 *
	 * <p>注意会堵塞等待结果，无法在{@code 位于 错误}语句中使用。
	 *
	 * @param title 对话框的标题。
	 * @param form 窗口对象。
	 * @param btnOK 是按钮标题。
	 * @param btnNO 否按钮标题。
	 * @return 点击是返回{@code true}，否则返回{@code false}。
	 */
	@SimpleFunction
	public static boolean 弹出对话框(String title, 窗口 form, String btnOK, String btnNO) {
		// 创建一个等待
		WaitEnd wait = new WaitEnd();

		// 构造并显示对话框
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.getContext());
		builder.setCancelable(false);
		builder.setTitle(title);
		builder.setView(((窗口Impl) form).getView());
		builder.setPositiveButton(btnOK, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				wait.end(true);
			}
		});
		builder.setNegativeButton(btnNO, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				wait.end(false);
			}
		});
		builder.show();

		// 开始等待
		return (boolean) wait.start();
	}

	/* 日期时间选择内部状态 */

	/**
	 * 复制并验证初值，避免读取日历字段时规范化调用者的对象。
	 *
	 * @param initial 初始日期时间
	 * @return 与输入对象相互独立的日期时间副本
	 */
	private static Calendar copyPickerInitial(Calendar initial) {
		if (initial == null) {
			throw new IllegalArgumentException("初始日期时间不得为空");
		}

		Calendar value = (Calendar) initial.clone();
		value.getTimeInMillis();
		return value;
	}

	/**
	 * 替换年月日前先回到月初，避免月末跨月规范化影响所选日期。
	 *
	 * @param initial 初始日期时间
	 * @param year 年份
	 * @param month 月份
	 * @param day 日期
	 * @return 更新了年月日的日期时间
	 */
	private static Calendar selectPickerDate(Calendar initial, int year, int month, int day) {
		Calendar value = copyPickerInitial(initial);
		value.set(Calendar.DAY_OF_MONTH, 1);
		value.set(year, month, day);
		return value;
	}

	/**
	 * 时间选择精确到分钟，保留初值日期及其时区。
	 *
	 * @param initial 初始日期时间
	 * @param hour 小时
	 * @param minute 分钟
	 * @return 更新了时分并清零秒和毫秒的日期时间
	 */
	private static Calendar selectPickerTime(Calendar initial, int hour, int minute) {
		Calendar value = copyPickerInitial(initial);
		value.set(Calendar.HOUR_OF_DAY, hour);
		value.set(Calendar.MINUTE, minute);
		value.set(Calendar.SECOND, 0);
		value.set(Calendar.MILLISECOND, 0);
		return value;
	}

	/**
	 * 只有确认结果才替换输出，取消时保持原变量及其引用不变。
	 *
	 * @param selected 是否选中
	 * @param result 输出结果引用
	 * @return 选择结果非空且已写入输出参数时为真；取消时为假并保留原输出值。
	 */
	private static boolean commitPickerResult(Calendar selected, DateReferenceParameter result) {
		if (selected == null) {
			return false;
		}

		result.set(selected);
		return true;
	}

	/**
	 * 原生对话框仅在仍可用的界面活动上创建。
	 *
	 * @return 可用于显示选择框的当前活动
	 */
	private static MainActivity requirePickerActivity() {
		if (Looper.myLooper() != Looper.getMainLooper()) {
			throw new IllegalStateException("日期时间选择框须在界面线程调用");
		}

		MainActivity activity = MainActivity.getContext();
		if (activity == null || activity.isFinishing()
			|| (android.os.Build.VERSION.SDK_INT >= 17 && activity.isDestroyed())) {
			throw new IllegalStateException("没有可用的界面活动");
		}

		return activity;
	}

	/**
	 * 每次选择独立等待关闭，只捕获自身退出信号，避免吞掉界面回调中的其它异常。
	 */
	private static final class PickerWait implements DialogInterface.OnDismissListener,
		DialogInterface.OnCancelListener {

		private final RuntimeException exitSignal = new RuntimeException();
		private final Handler handler = new Handler(Looper.getMainLooper());
		private final Runnable exit = new Runnable() {

			@Override
			public void run() {
				throw exitSignal;
			}
		};
		private Calendar selected;
		private boolean confirmed;
		private boolean finished;

		@Override
		public void onCancel(DialogInterface dialog) {
			confirmed = false;
			selected = null;
		}

		@Override
		public void onDismiss(DialogInterface dialog) {
			if (!finished) {
				finished = true;
				handler.post(exit);
			}
		}

		/**
		 * 确认、取消和返回均先关闭对话框，再恢复调用方。
		 *
		 * @param dialog 当前弹窗
		 * @return showAndWait回调
		 */
		private Calendar showAndWait(AlertDialog dialog) {
			dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface source, int which) {
					onCancel(source);
				}
			});
			dialog.setCanceledOnTouchOutside(true);
			dialog.setOnCancelListener(this);
			dialog.setOnDismissListener(this);

			try {
				dialog.show();
				if (!finished) {
					Looper.loop();
				}
			} catch (RuntimeException error) {
				if (error != exitSignal) {
					throw error;
				}
			} finally {
				finished = true;
				handler.removeCallbacks(exit);
				dialog.setOnDismissListener(null);
				dialog.setOnCancelListener(null);
				dialog.dismiss();
			}

			return confirmed ? selected : null;
		}
	}

	/**
	 * 堵塞等待类，主要用于等待对话框结束。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	private static class WaitEnd {
		// 结果
		public Object result = null;
		// 扩展
		public Object expand = null;

		// 处理器，显式绑定当前线程的消息循环，保持原有等待与退出逻辑。
		private Handler handler = new Handler(Looper.myLooper()) {
			@Override
			public void handleMessage(Message mesg) {
				// 收到任意消息抛出异常
				throw new RuntimeException();
			}
		};

		/**
		 * 开始。
		 *
		 * @return 结果。
		 */
		public Object start() {
			try {
				Looper.getMainLooper();
				// 进入循环等待
				Looper.loop();
			} catch (RuntimeException e) {
				// 使用异常退出循环
			}
			return result;
		}

		/**
		 * 等待。
		 */
		public void end() {
			// 给消息处理器发送个消息
			handler.sendMessage(handler.obtainMessage());
		}

		/**
		 * 等待。
		 *
		 * @param result 结果。
		 */
		public void end(Object result) {
			this.result = result;
			end();
		}
	}
}

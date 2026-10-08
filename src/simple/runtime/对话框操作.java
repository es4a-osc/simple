package simple.runtime;

import simple.runtime.android.MainActivity;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.components.窗口;
import simple.runtime.components.组件;
import simple.runtime.components.impl.android.窗口Impl;
import simple.runtime.components.impl.android.util.EditTextUtil;

import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
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
			throw new IllegalArgumentException("[对话框.置进度框信息] 进度框对象错误，设置信息失败");
		}
	}

	/**
	 * 关闭进度框。
	 *
	 * @param dialog 进度框对象实例。
	 */
	@SimpleFunction
	public static void 关闭进度框(Object object) {
		if (object instanceof AlertDialog) {
			((AlertDialog) object).dismiss();
		} else {
			throw new IllegalArgumentException("[对话框.关闭进度框] 进度框对象错误，关闭进度框失败");
		}
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

		// 处理器
		private Handler handler = new Handler() {
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

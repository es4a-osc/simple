package simple.runtime.components.impl.android;

import simple.runtime.components.水平进度条;
import simple.runtime.components.impl.android.util.ViewUtil;
import simple.runtime.components.组件容器;
import simple.runtime.android.MainActivity;

import android.R;
import android.view.View;
import android.widget.ProgressBar;

import java.lang.reflect.Field;

public class 水平进度条Impl extends 进度视图组件 implements 水平进度条 {

	public 水平进度条Impl(组件容器 container) {
		super(container);
	}

	@Override
	protected View createView() {
		// ProgressBar https://developer.android.google.cn/reference/android/widget/ProgressBar.html
		// R.attr.progressBarStyleHorizontal 水平进度条样式。
		ProgressBar view = new ProgressBar(MainActivity.getContext(), null, R.attr.progressBarStyleHorizontal);
		// 可停留焦点
		ViewUtil.setFocusable(view, true);
		return view;
	}

	// 水平进度条 实现

	@Override
	public boolean 循环模式() {
		return ((ProgressBar) getView()).isIndeterminate();
	}

	@Override
	public void 循环模式(boolean indeterminate) {
		((ProgressBar) getView()).setIndeterminate(indeterminate);
	}

	@Override
	public int 最大高度() {
		// 详细信息请参考 最大高度(int)
		try {
			ProgressBar seekBar = (ProgressBar) getView();
			Class<?> superClass = seekBar.getClass();
			Field mMaxHeight = superClass.getDeclaredField("mMaxHeight");
			mMaxHeight.setAccessible(true);
			return (int) mMaxHeight.get(seekBar);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public void 最大高度(int maxHeight) {
		// SeekBar 继承至 ProgressBar 的 setMaxHeight() 方法需要API29
		// 通过反射调用 ProgressBar 的 android:maxHeigh 属性
		// 参考文档 https://www.jianshu.com/p/413792ed6960
		try {
			ProgressBar progressBar = (ProgressBar) getView();
			Class<?> superClass = progressBar.getClass();
			Field mMaxHeight = superClass.getDeclaredField("mMaxHeight");
			mMaxHeight.setAccessible(true);
			mMaxHeight.set(progressBar, maxHeight);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public int 最小高度() {
		// 详细信息请参考 最大高度(int)
		try {
			ProgressBar progressBar = (ProgressBar) getView();
			Class<?> superClass = progressBar.getClass();
			Field mMinHeight = superClass.getDeclaredField("mMinHeight");
			mMinHeight.setAccessible(true);
			return (int) mMinHeight.get(progressBar);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public void 最小高度(int minHeight) {
		// 详细信息请参考 最大高度(int)
		try {
			ProgressBar progressBar = (ProgressBar) getView();
			Class<?> superClass = progressBar.getClass();
			Field mMinHeight = superClass.getDeclaredField("mMinHeight");
			mMinHeight.setAccessible(true);
			mMinHeight.set(progressBar, minHeight);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

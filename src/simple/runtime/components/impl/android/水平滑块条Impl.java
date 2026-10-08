package simple.runtime.components.impl.android;

import simple.runtime.components.组件容器;
import simple.runtime.components.impl.android.util.ImageUtil;
import simple.runtime.components.impl.android.util.ViewUtil;
import simple.runtime.errors.文件未存在错误;
import simple.runtime.components.水平滑块条;
import simple.runtime.android.MainActivity;
import simple.runtime.events.EventDispatcher;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.SeekBar;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * 水平滑块条实现
 *
 * @author 树先生 xhwsd@qq.com
 */
public class 水平滑块条Impl extends 进度视图组件 implements 水平滑块条 {

	public 水平滑块条Impl(组件容器 container) {
		super(container);
	}

	@Override
	protected View createView() {
		// SeekBar https://developer.android.google.cn/reference/android/widget/SeekBar
		SeekBar view = new SeekBar(MainActivity.getContext());

		// 可停留焦点
		ViewUtil.setFocusable(view, true);
		view.setThumbOffset(0);
		view.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStartTrackingTouch(SeekBar seekbar) {
				开始拖动();
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekbar) {
				停止拖动();
			}

			@Override
			public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {
				位置被改变(progress);
			}
		});
		return view;
	}

	// 水平滑块条 实现

	@Override
	public void 开始拖动() {
		EventDispatcher.dispatchEvent(this, "开始拖动");
	}

	@Override
	public void 停止拖动() {
		EventDispatcher.dispatchEvent(this, "停止拖动");
	}

	@Override
	public void 位置被改变(int progress) {
		EventDispatcher.dispatchEvent(this, "位置被改变", progress);
	}

	@Override
	public int 最大高度() {
		// 详细信息请参考 最大高度(int)
		try {
			SeekBar seekBar = (SeekBar) getView();
			Class<?> superClass = seekBar.getClass().getSuperclass().getSuperclass();
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
			SeekBar seekBar = (SeekBar) getView();
			// 注意这里继承关系 SeekBar -> AbsSeekBar -> ProgressBar
			Class<?> superClass = seekBar.getClass().getSuperclass().getSuperclass();
			Field mMaxHeight = superClass.getDeclaredField("mMaxHeight");
			mMaxHeight.setAccessible(true);
			mMaxHeight.set(seekBar, maxHeight);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public int 最小高度() {
		// 详细信息请参考 最大高度(int)
		try {
			SeekBar seekBar = (SeekBar) getView();
			Class<?> superClass = seekBar.getClass().getSuperclass().getSuperclass();
			Field mMinHeight = superClass.getDeclaredField("mMinHeight");
			mMinHeight.setAccessible(true);
			return (int) mMinHeight.get(seekBar);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public void 最小高度(int minHeight) {
		// 详细信息请参考 最大高度(int)
		try {
			SeekBar seekBar = (SeekBar) getView();
			Class<?> superClass = seekBar.getClass().getSuperclass().getSuperclass();
			Field mMinHeight = superClass.getDeclaredField("mMinHeight");
			mMinHeight.setAccessible(true);
			mMinHeight.set(seekBar, minHeight);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void 滑块图片(String image) {
		try {
			Drawable drawable = ImageUtil.getDrawable(image);
			if (drawable != null) {
				((SeekBar) getView()).setThumb(drawable);
			}
		} catch(IOException e) {
			throw new 文件未存在错误(image);
		}
	}
}

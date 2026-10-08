package simple.runtime.components.impl.android;

import simple.runtime.components.组件容器;
import simple.runtime.components.进度组件;
import simple.runtime.components.impl.android.util.ImageUtil;
import simple.runtime.errors.文件未存在错误;

import java.io.IOException;

import android.graphics.drawable.Drawable;
import android.widget.ProgressBar;

/**
 * 进度视图组件实现。
 *
 * @author 树先生 xhwsd@qq.com
 */
public abstract class 进度视图组件 extends 视图组件 implements 进度组件 {

	/**
	 * 创建一个新的进度视图组件。
	 *
	 * @param container 容纳组件的容器（不可为{@code null}，对于不可见的组件必须是窗口）
	 */
	protected 进度视图组件(组件容器 container) {
		super(container);
	}

	// 进度组件 实现

	@Override
	public int 最大位置() {
		return ((ProgressBar) getView()).getMax();
	}

	@Override
	public void 最大位置(int max) {
		((ProgressBar) getView()).setMax(max);
	}

	@Override
	public int 位置() {
		return ((ProgressBar) getView()).getProgress();
	}

	@Override
	public void 位置(int progress) {
		((ProgressBar) getView()).setProgress(progress);
	}

	@Override
	public void 进度图片(String image) {
		try {
			Drawable drawable = ImageUtil.getDrawable(image);
			if (drawable != null) {
				((ProgressBar) getView()).setProgressDrawable(drawable);
			}
		} catch(IOException e) {
			throw new 文件未存在错误(image);
		}
	}

}

package simple.runtime.components.impl.android;

import simple.runtime.components.垂直滑块条;
import simple.runtime.components.组件容器;
import simple.runtime.android.MainActivity;
import simple.runtime.events.EventDispatcher;
import simple.runtime.components.impl.android.util.VerticalSeekBar;
import simple.runtime.components.impl.android.util.ViewUtil;

import android.view.View;
import android.widget.SeekBar;

/**
 * 垂直滑块条的实现。
 *
 * @author 树先生 xhwsd@qq.com
 */
public class 垂直滑块条Impl extends 进度视图组件 implements 垂直滑块条 {

	public 垂直滑块条Impl(组件容器 container) {
		super(container);
	}

	@Override
	protected View createView() {
		VerticalSeekBar view = new VerticalSeekBar(MainActivity.getContext());

		// 可通过方向键获得焦点
		ViewUtil.setFocusable(view, true);
		view.setThumbOffset(0);

		// 滑块条改变监听器
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

	// 垂直滑块条 实现

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
}

package simple.runtime.components.impl.android;

import simple.runtime.components.组件容器;
import simple.runtime.components.水平滚动框;
import simple.runtime.components.组件;
import simple.runtime.components.布局;
import simple.runtime.android.MainActivity;
import simple.runtime.events.EventDispatcher;

import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

/**
 * 水平滚动框实现。
 *
 * @author 树先生 xhwsd@qq.com
 */
public class 水平滚动框Impl extends 视图组件 implements 水平滚动框, 视图组件容器 {

	/**
	 * 创建一个新的水平滚动框组件。
	 *
	 * @param container 容器，组件将被放置在于
	 */
	public 水平滚动框Impl(组件容器 container) {
		super(container);

	}

	@Override
	protected View createView() {
		// HorizontalScrollView https://developer.android.google.cn/reference/android/widget/HorizontalScrollView
		HorizontalScrollView view = new HorizontalScrollView(MainActivity.getContext()) {
			@Override
			protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
				super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
				被滚动(scrollX, clampedX);
			}
		};
		return view;
	}

	@Override
	public ViewGroup getLayoutManager() {
		return (ViewGroup) getView();
	}

	@Override
	public void addComponent(组件 component) {
		// 判断下组件是否是视图组件
		if ((component instanceof 视图组件)) {
			// 把可视组件接口转为实现接口视图组件，并取视图组件的视图
			View child = ((视图组件) component).getView();
			HorizontalScrollView view = (HorizontalScrollView) getView();
			view.addView(child, new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT));
		}
	}

	@Override
	public 布局 getLayout() {
		return null;
	}

	// 水平滚动框 实现

	@Override
	public void 被滚动(int scrollX, boolean clampedX) {
		EventDispatcher.dispatchEvent(this, "被滚动", scrollX, clampedX);
	}

	@Override
	public boolean 启用滚动条() {
		HorizontalScrollView view = (HorizontalScrollView) getView();
		return view.isVerticalScrollBarEnabled();
	}

	@Override
	public void 启用滚动条(boolean isEnabled) {
		HorizontalScrollView view = (HorizontalScrollView) getView();
		view.setVerticalScrollBarEnabled(isEnabled);
	}

	@Override
	public int 最大滚动值() {
		HorizontalScrollView scrollView = (HorizontalScrollView) getView();
		return scrollView.getMaxScrollAmount();
	}

	@Override
	public void 滚动(int x) {
		HorizontalScrollView scrollView = (HorizontalScrollView) getView();
		scrollView.smoothScrollTo(x, 0);
	}

	@Override
	public void 滚动到左() {
		HorizontalScrollView view = (HorizontalScrollView) getView();
		Handler handler = new Handler();
		handler.post(new Runnable() {
			public void run() {
				view.fullScroll(33);
			}
		});
	}

	@Override
	public void 滚动到右() {
		HorizontalScrollView view = (HorizontalScrollView) getView();
		Handler handler = new Handler();
		handler.post(new Runnable() {
			public void run() {
				view.fullScroll(130);
			}
		});
	}
}

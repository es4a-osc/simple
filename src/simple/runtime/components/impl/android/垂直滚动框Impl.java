package simple.runtime.components.impl.android;

import simple.runtime.components.组件容器;
import simple.runtime.android.MainActivity;
import simple.runtime.components.垂直滚动框;
import simple.runtime.components.组件;
import simple.runtime.components.布局;
import simple.runtime.events.EventDispatcher;

import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

/**
 * 垂直滚动框实现。
 *
 * @author 树先生 xhwsd@qq.com
 */
public class 垂直滚动框Impl extends 视图组件 implements 垂直滚动框, 视图组件容器 {

	/**
	 * 创建一个新的垂直滚动框组件。
	 *
	 * @param container 容器，组件将被放置在于
	 */
	public 垂直滚动框Impl(组件容器 container) {
		super(container);
	}

	@Override
	protected View createView() {
		// ScrollView https://developer.android.google.cn/reference/android/widget/ScrollView
		ScrollView view = new ScrollView(MainActivity.getContext()) {
			@Override
			protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
				super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
				被滚动(scrollY, clampedY);
			}
		};
		return view;
	}

	@Override
	public void addComponent(组件 component) {
		// 判断下组件是否是视图组件
		if ((component instanceof 视图组件)) {
			// 把可视组件接口转为实现接口视图组件，并取视图组件的视图
			View child = ((视图组件) component).getView();
			ScrollView view = (ScrollView) getView();
			view.addView(child, new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT));
		}

	}

	@Override
	public 布局 getLayout() {
		return null;
	}

	@Override
	public ViewGroup getLayoutManager() {
		return (ViewGroup) getView();
	}

	/* 垂直滚动框 实现 */

	@Override
	public void 被滚动(int scrollY, boolean clampedY) {
		EventDispatcher.dispatchEvent(this, "被滚动", scrollY, clampedY);
	}

	@Override
	public boolean 启用滚动条() {
		ScrollView view = (ScrollView) getView();
		return view.isVerticalScrollBarEnabled();
	}

	@Override
	public void 启用滚动条(boolean isEnabled) {
		ScrollView view = (ScrollView) getView();
		view.setVerticalScrollBarEnabled(isEnabled);
	}

	@Override
	public int 最大滚动值() {
		ScrollView view = (ScrollView) getView();
		return view.getMaxScrollAmount();
	}

	@Override
	public void 滚动(int y) {
		ScrollView view = (ScrollView) getView();
		view.smoothScrollTo(0, y);
	}

	@Override
	public void 滚动到顶() {
		ScrollView view = (ScrollView) getView();
		Handler handler = new Handler();
		handler.post(new Runnable() {
			public void run() {
				view.fullScroll(33);
			}
		});
	}

	@Override
	public void 滚动到底() {
		ScrollView view = (ScrollView) getView();
		Handler handler = new Handler();
		handler.post(new Runnable() {
			public void run() {
				view.fullScroll(130);
			}
		});
	}
}

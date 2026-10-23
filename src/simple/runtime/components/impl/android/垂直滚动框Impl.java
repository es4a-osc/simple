package simple.runtime.components.impl.android;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import simple.runtime.android.MainActivity;
import simple.runtime.components.布局;
import simple.runtime.components.组件;
import simple.runtime.components.组件容器;
import simple.runtime.components.垂直滚动框;
import simple.runtime.events.EventDispatcher;

/**
 * 提供垂直滚动、实际滚动范围和对应方向的滚动条。
 *
 * @author 树先生 xhwsd@qq.com
 */
public class 垂直滚动框Impl extends 视图组件 implements 垂直滚动框, 视图组件容器 {

	// -1 表示布局完成后的末端；排队期间多次请求只执行最后一次。
	private Integer pendingPosition;
	private final Runnable scrollAction = this::performScroll;
	private boolean destroyed;

	/**
	 * 创建只容纳一个直接可视子组件的垂直滚动框。
	 *
	 * @param container 所属组件容器
	 */
	public 垂直滚动框Impl(组件容器 container) {
		super(container);
	}

	@Override
	protected View createView() {
		return new ScrollView(MainActivity.getContext()) {
			@Override
			protected void onScrollChanged(int x, int y, int oldX, int oldY) {
				super.onScrollChanged(x, y, oldX, oldY);
				if (!destroyed && y != oldY) {
					int range = scrollRange(this);
					被滚动(y, y <= 0 || y >= range);
				}
			}

			@Override
			protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
				super.onLayout(changed, left, top, right, bottom);
				// 初次布局或内容大小改变后，再使用实际尺寸处理排队中的滚动请求。
				if (!destroyed && pendingPosition != null) {
					removeCallbacks(scrollAction);
					post(scrollAction);
				}
			}
		};
	}

	/* 组件事件 */

	@Override
	public void 被滚动(int position, boolean atEdge) {
		EventDispatcher.dispatchEvent(this, "被滚动", position, atEdge);
	}

	/* 组件属性 */

	@Override
	public boolean 启用滚动条() {
		return getView().isVerticalScrollBarEnabled();
	}

	@Override
	public void 启用滚动条(boolean enabled) {
		getView().setVerticalScrollBarEnabled(enabled);
	}

	@Override
	public int 最大滚动值() {
		return scrollRange((ScrollView) getView());
	}

	/* 组件方法 */

	@Override
	public void 滚动(int position) {
		requestScroll(Math.max(0, position));
	}

	@Override
	public void 滚动到顶() {
		requestScroll(0);
	}

	@Override
	public void 滚动到底() {
		requestScroll(-1);
	}

	@Override
	public void 销毁() {
		if (destroyed) {
			return;
		}
		destroyed = true;
		pendingPosition = null;
		getView().removeCallbacks(scrollAction);
		super.销毁();
	}

	/* 内部接口与平台回调 */

	@Override
	public ViewGroup getLayoutManager() {
		return (ViewGroup) getView();
	}

	@Override
	public void addComponent(组件 component) {
		if (component instanceof 视图组件) {
			View child = ((视图组件) component).getView();
			((ScrollView) getView()).addView(child, new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT
			));
		}
	}

	@Override
	public 布局 getLayout() {
		return null;
	}

	/**
	 * 通过视图队列安排滚动；视图尚未附着时由系统在附着后执行。
	 *
	 * @param position 非负像素位置；-1 表示当前内容末端
	 */
	private void requestScroll(int position) {
		if (destroyed) {
			return;
		}
		pendingPosition = position;
		View view = getView();
		view.removeCallbacks(scrollAction);
		view.post(scrollAction);
	}

	/**
	 * 在布局已完成时执行最后一次请求，目标限制在当前实际可滚动范围内。
	 */
	private void performScroll() {
		if (destroyed || pendingPosition == null) {
			return;
		}
		ScrollView view = (ScrollView) getView();
		if (view.isLayoutRequested() || view.getWidth() == 0 || view.getHeight() == 0) {
			return;
		}
		int range = scrollRange(view);
		int position = pendingPosition < 0 ? range : Math.min(pendingPosition, range);
		pendingPosition = null;
		view.smoothScrollTo(0, position);
	}

	/**
	 * 按平台滚动范围计算内容超出视口的高度，包含容器填充造成的可视区域变化。
	 *
	 * @param view 原生滚动视图
	 * @return 最大像素偏移；没有内容、尚未布局或内容未超出视口时为 0
	 */
	private static int scrollRange(ScrollView view) {
		if (view.getChildCount() == 0 || view.getHeight() == 0) {
			return 0;
		}
		int viewport = view.getHeight() - view.getPaddingTop() - view.getPaddingBottom();
		return Math.max(0, view.getChildAt(0).getHeight() - viewport);
	}
}

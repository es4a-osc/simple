package simple.runtime.components.impl.android;

import simple.runtime.components.组件容器;
import simple.runtime.components.列表组件;

import android.view.View;
import android.widget.AbsListView;

/**
 * 列表视图组件实现。
 *
 * @author 树先生 xhwsd@qq.com
 */
public abstract class 列表视图组件 extends 视图组件 implements 列表组件 {

	/**
	 * 创建一个新的视图组件。
	 *
	 * @param container 容纳组件的容器（不可为{@code null}，对于不可见的组件必须是窗口）
	 */
	protected 列表视图组件(组件容器 container) {
		super(container);

		View view = getView();
		if (view instanceof AbsListView) {
			view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
				@Override
				public void onViewAttachedToWindow(View view) {
					// 原生列表重新挂载时会标记数据待更新，补一次布局以恢复表项交互。
					view.requestLayout();
				}

				@Override
				public void onViewDetachedFromWindow(View view) {
				}
			});
		}
	}

	/* 进度组件 实现　*/

}

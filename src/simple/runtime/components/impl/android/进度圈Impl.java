package simple.runtime.components.impl.android;

import simple.runtime.components.进度圈;
import simple.runtime.components.impl.android.util.ViewUtil;
import simple.runtime.components.组件容器;
import simple.runtime.android.MainActivity;

import android.view.View;
import android.widget.ProgressBar;

public class 进度圈Impl extends 视图组件 implements 进度圈 {

	public 进度圈Impl(组件容器 container) {
		super(container);
	}

	@Override
	protected View createView() {
		ProgressBar view = new ProgressBar(MainActivity.getContext());

		// 可停留焦点
		ViewUtil.setFocusable(view, true);

		// 不确定进度，无限转圈圈吧！
		view.setIndeterminate(false);
		return view;
	}
}

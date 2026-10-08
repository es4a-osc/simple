package simple.runtime.components.impl.android;

import simple.runtime.android.MainActivity;
import simple.runtime.components.绝对布局;
import simple.runtime.components.impl.android.util.ViewUtil;

/**
 * 绝对布局的实现。
 * 
 * 注意：绝对布局已在API3版本中废弃。
 * 
 * @author 树先生 xhwsd@qq.com
 */
public class 绝对布局Impl extends 布局Impl implements 绝对布局 {
	/*
	AbsoluteLayout
	https://developer.android.google.cn/reference/android/widget/AbsoluteLayout

	AbsoluteLayout.LayoutParams
	https://developer.android.google.cn/reference/android/widget/AbsoluteLayout.LayoutParams
	*/

	@SuppressWarnings("deprecation")
	绝对布局Impl(视图组件容器 container) {
		super(new android.widget.AbsoluteLayout(MainActivity.getContext()), container);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void addComponent(视图组件 component) {
		getLayoutManager().addView(component.getView(), new android.widget.AbsoluteLayout.LayoutParams(
			android.widget.AbsoluteLayout.LayoutParams.WRAP_CONTENT,
			android.widget.AbsoluteLayout.LayoutParams.WRAP_CONTENT,
			component.左边().getInteger(),
			component.顶边().getInteger()
		));
	}

	/**
	 * 将组件放入其列和行中。
	 *
	 * @param component 准备好放入其列和行中的组件
	 */
	@SuppressWarnings("deprecation")
	public void placeComponent(视图组件 component) {
		component.getView().setLayoutParams(new android.widget.AbsoluteLayout.LayoutParams(
			ViewUtil.simpleToAndroidLength(component.宽度().getInteger()),
			ViewUtil.simpleToAndroidLength(component.高度().getInteger()),
			component.左边().getInteger(),
			component.顶边().getInteger()
		));
	}

	/**
	 * 删除组件
	 * 
	 * @param component 组件
	 */
	public void removeComponent(视图组件 component) {
		getLayoutManager().removeView(component.getView());  
	}
}

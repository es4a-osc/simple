package simple.runtime.components.impl.android;

import simple.runtime.android.MainActivity;
import simple.runtime.components.相对布局;
import simple.runtime.components.组件;

import android.os.Build;
import android.widget.RelativeLayout;

/**
 * 用于相对同级或父级组件放置组件的相对布局。
 * 
 * @author 树先生 xhwsd@qq.com
 */
public class 相对布局Impl extends 布局Impl implements 相对布局 {
	/*
	RelativeLayout
	https://developer.android.google.cn/reference/android/widget/RelativeLayout

	RelativeLayout.LayoutParams
	https://developer.android.google.cn/reference/android/widget/RelativeLayout.LayoutParams

	菜鸟 RelativeLayout(相对布局)
	https://www.runoob.com/w3cnote/android-tutorial-relativelayout.html
	*/

	/**
	 * 创建一个新的线性布局。
	 *
	 * @param container 视图容器
	 */
	相对布局Impl(视图组件容器 container) {
		super(new RelativeLayout(MainActivity.getContext()), container);
	}

	@Override
	public void addComponent(视图组件 component) {
		getLayoutManager().addView(component.getView(), new RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.WRAP_CONTENT,
			RelativeLayout.LayoutParams.WRAP_CONTENT
		));
	}

	/**
	 * 添加子组件的布局规则
	 *
	 * @param component 子组件
	 * @param verb 布局规则
	 */
	public void addComponentRule(视图组件 component, int verb) {
		((RelativeLayout.LayoutParams) component.getView().getLayoutParams()).addRule(compatibleRule(verb));
		component.getView().requestLayout();
	}

	/**
	 * 添加子组件的布局规则
	 *
	 * @param component 子组件
	 * @param verb 布局规则
	 * @param anchor 规则锚点
	 */
	public void addComponentRule(视图组件 component, int verb, int anchor) {
		((RelativeLayout.LayoutParams) component.getView().getLayoutParams()).addRule(compatibleRule(verb), anchor);
		component.getView().requestLayout();
	}

	/**
	 * 删除子组件的布局规则
	 *
	 * @param component 子组件
	 * @param verb 布局规则
	 */
	public void removeComponentRule(视图组件 component, int verb) {
		// API17兼容处理
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
			((RelativeLayout.LayoutParams) component.getView().getLayoutParams()).addRule(compatibleRule(verb), 0);
		} else {
			// 添加于API17：LayoutParams.removeRule(int) 
			((RelativeLayout.LayoutParams) component.getView().getLayoutParams()).removeRule(verb);
		}
		
		// 更新布局
		component.getView().requestLayout();
	}

	/**
	 * 取子组件的布局规则瞄点
	 *
	 * @param component 子组件
	 * @return 成功返回规则瞄点，失败返回0
	 */
	public int getComponentAnchor(视图组件 component, int verb) {
		// API23兼容处理
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			int[] rules = ((RelativeLayout.LayoutParams) component.getView().getLayoutParams()).getRules();
			return (verb >= 0 && verb < rules.length) ? rules[verb] : 0;
		} else {
			// 添加于API23：LayoutParams.getRule(int) 
			return ((RelativeLayout.LayoutParams) component.getView().getLayoutParams()).getRule(verb);
		}
	}

	// API 16 及以下没有“开始/结束”规则，按从左到右的布局转换。
	private static int compatibleRule(int verb) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			return verb;
		}

		switch (verb) {
			case 组件.布局_规则_开始: return RelativeLayout.LEFT_OF;
			case 组件.布局_规则_结束: return RelativeLayout.RIGHT_OF;
			case 组件.布局_规则_对齐_开始: return RelativeLayout.ALIGN_LEFT;
			case 组件.布局_规则_对齐_结束: return RelativeLayout.ALIGN_RIGHT;
			case 组件.布局_规则_对齐_父_开始: return RelativeLayout.ALIGN_PARENT_LEFT;
			case 组件.布局_规则_对齐_父_结束: return RelativeLayout.ALIGN_PARENT_RIGHT;
			default: return verb;
		}
	}
}

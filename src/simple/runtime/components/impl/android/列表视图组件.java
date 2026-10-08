package simple.runtime.components.impl.android;

import simple.runtime.components.组件容器;
import simple.runtime.components.列表组件;

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
	}

	/* 进度组件 实现　*/

}

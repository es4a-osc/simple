/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simple.runtime.components.impl.android;

import simple.runtime.components.布局;
import simple.runtime.components.组件;
import simple.runtime.components.组件容器;
import simple.runtime.components.面板;
import simple.runtime.components.impl.android.util.ViewUtil;
import simple.runtime.variants.ObjectVariant;
import simple.runtime.variants.Variant;
import simple.runtime.日志输出;

import android.view.View;
import android.view.ViewGroup;

/**
 * 可放置在窗口或其他容器中的容器。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public class 面板Impl extends 视图组件 implements 面板, 视图组件容器 {

	// 布局
	protected 布局Impl viewLayout;
	// 当这是真时，尝试更改布局属性会导致错误
	private boolean layoutFixed;
	// 布局属性的支持
	private Variant layout;
	// 背景颜色属性的支持
	private int backgroundColor;

	/**
	 * 创建一个新的面板组件。
	 *
	 * @param container  容器、组件将放入
	 */
	public 面板Impl(组件容器 container) {
		super(container);

		layoutFixed = false;
	}

	@Override
	protected View createView() {
		return null;
	}

	/* 面板 实现 */

	@Override
	public int 背景颜色() {
		return backgroundColor;
	}

	@Override
	public void 背景颜色(int argb) {
		backgroundColor = argb;
		
		// 由于Simple属性的排序，尚未设置ViewLayout。
		if (viewLayout != null) {
			viewLayout.getLayoutManager().setBackgroundColor(argb);
		}
	}

	@Override
	public Variant 布局() {
		return layout;
	}

	@Override
	public void 布局(Variant layoutType) {
		// 避免相同布局重复实例化等一系列操作
		if (getLayoutType() == layoutType.getInteger()) {
			日志输出.debug("(%d) 避免重复实例化布局；新布局类型=%d", hashCode(), layoutType.getInteger());
			return;
		}

		if (layoutFixed) {
			throw new IllegalStateException("容器已加入子组件，布局属性无法更改");
		}

		// 记录原布局
		ViewGroup layoutManager = null;
		if (viewLayout != null) {
			layoutManager = viewLayout.getLayoutManager();
		}

		// 实例化对应布局
		switch (layoutType.getInteger()) {
			default:
				throw new IllegalArgumentException("未知布局");
			case 组件.布局_线性:
				viewLayout = new 线性布局Impl(this);
				break;
			case 组件.布局_表格:
				viewLayout = new 表格布局Impl(this);
				break;
			case 组件.布局_单帧:
				viewLayout = new 单帧布局Impl(this);
				break;
			case 组件.布局_相对:
				viewLayout = new 相对布局Impl(this);
				break;
      		case 组件.布局_绝对:
        		viewLayout = new 绝对布局Impl(this);
				break;
		}

		日志输出.debug("(%d) 实例化新布局；新布局类型=%d", hashCode(), layoutType.getInteger());

		// 将布局包装为变体对象
		layout = ObjectVariant.getObjectVariant(viewLayout);

		// 恢复原布局相关参数
		if (layoutManager != null) {
			// 先把原布局移出父级布局
			ViewUtil.removeView(layoutManager);

			// 迁移视图标识
			getView().setId(layoutManager.getId());
			layoutManager.setId(View.NO_ID);

			// 迁移可绘制布局
			ViewUtil.setBackgroundDrawable(getView(), layoutManager.getBackground());
			layoutManager.setBackground(null);
		} else {
			// 受Simple属性排版影响，viewLayout 可能尚未设置。
			// 面板的背景颜色属性有初始值，所以这里给新建的视图初始属性。
			背景颜色(backgroundColor);
		}

		// 这样意味着，你每修改一次布局就添加一次自身。这里是把自身添加到组件中。
		addToContainer();

		// 恢复布局参数，注意布局参数仅在添加到布局后才效
		if (layoutManager != null) {
			ViewGroup.LayoutParams layoutParams = layoutManager.getLayoutParams();
			if (layoutParams != null) {
				viewLayout.getLayoutManager().setLayoutParams(layoutParams);
			}
		}
	}

	protected void addToContainer() {
		// 无法在ViewComponent构造函数中提前添加此组件 - 现在执行它
		getComponentContainer().addComponent(this);
	}

	/**
	 * 取当前布局类型
	 * 
	 * @return 成功返回布局类型，失败返回0
	 */
	protected int getLayoutType() {
		if (viewLayout instanceof 线性布局Impl) {
			return  组件.布局_线性;
		} else if (viewLayout instanceof 表格布局Impl) {
			return 组件.布局_表格;
		} else if (viewLayout instanceof 单帧布局Impl) {
			return 组件.布局_单帧;
		} else if (viewLayout instanceof 相对布局Impl) {
			return 组件.布局_相对;
		} else if (viewLayout instanceof 绝对布局Impl) {
			return 组件.布局_绝对;
		} else {
			return 0;
		}
	}

	/* 组件容器 实现 */

	@Override
	public ViewGroup getLayoutManager() {
		return viewLayout.getLayoutManager();
	}

	@Override
	public void addComponent(组件 component) {
		// 现在已添加组件，防止布局更改
		layoutFixed = true;

		viewLayout.addComponent((视图组件) component);
	}

	@Override
	public 布局 getLayout() {
		return viewLayout;
	}

	/* 视图组件 实现 */

	@Override
	public View getView() {
		return viewLayout == null ? null : viewLayout.getLayoutManager();
	}
}

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

import simple.runtime.android.MainActivity;
import simple.runtime.components.组件;
import simple.runtime.components.线性布局;
import simple.runtime.components.impl.android.util.ViewUtil;

import android.widget.RadioGroup;

/**
 * 用于水平或垂直放置组件的线性布局。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public final class 线性布局Impl extends 布局Impl implements 线性布局 {
	/*
	RadioGroup
	https://developer.android.google.cn/reference/android/widget/RadioGroup
	
	RadioGroup.LayoutParams
	https://developer.android.google.cn/reference/android/widget/RadioGroup.LayoutParams
	*/

	// 内容对齐属性支持
	protected int justification;

	/**
	 * 创建一个新的线性布局。
	 *
	 * @param container  视图容器
	 */
	线性布局Impl(视图组件容器 container) {
		// 单选组布局（android.widget.RadioGroup），该布局是继承至线性布局（android.widget.LinearLayout）
		// 使用该布局而未直接使用线性布局的原因是为了让Simple中的线性布局兼容单选框情况。
		super(new RadioGroup(MainActivity.getContext()), container);

		justification = 组件.对齐_上;
	}

	@Override
	public void addComponent(视图组件 component) {
		getLayoutManager().addView(component.getView(), new RadioGroup.LayoutParams(
			RadioGroup.LayoutParams.WRAP_CONTENT,
			RadioGroup.LayoutParams.WRAP_CONTENT
		));
	}

	/* 线性布局 实现 */

	@Override
	public int 方向() {
		return ((RadioGroup) getLayoutManager()).getOrientation() == RadioGroup.HORIZONTAL ?
				组件.布局_方向_水平 : 组件.布局_方向_垂直;
	}

	@Override
	public void 方向(int newOrientation) {
		((RadioGroup) getLayoutManager()).setOrientation(
			newOrientation == 组件.布局_方向_水平 ? RadioGroup.HORIZONTAL : RadioGroup.VERTICAL
		);
	}

	@Override
	public int 内容对齐() {
		return justification;
	}

	@Override
	public void 内容对齐(int justification) {
		// 注意这里是设置其内容（子视图）相对自身的对齐方式
		this.justification = justification;
		
		((RadioGroup) getLayoutManager()).setGravity(ViewUtil.simpleToAndroidGravity(justification));
	}

	@Override
	public boolean 基线对齐() {
		return ((RadioGroup) getLayoutManager()).isBaselineAligned();
	}

	@Override
	public void 基线对齐(boolean baselineAligned) {
		((RadioGroup) getLayoutManager()).setBaselineAligned(baselineAligned);
	}

	@Override
	public float 权重总和() {
		return ((RadioGroup) getLayoutManager()).getWeightSum();
	}

	@Override
	public void 权重总和(float weight) {
		((RadioGroup) getLayoutManager()).setWeightSum(weight);
	}
}

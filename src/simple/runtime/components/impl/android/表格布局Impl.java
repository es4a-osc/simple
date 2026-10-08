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
import simple.runtime.components.表格布局;
import simple.runtime.components.可视组件;
import simple.runtime.components.组件;
import simple.runtime.components.impl.android.util.ViewUtil;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.TableRow;
import android.widget.TableLayout;

/**
 * 一个允许将子组件放置在表格窗体中的布局组件。
 *
 * @author David Foster
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public class 表格布局Impl extends 布局Impl implements 表格布局 {
	/*
	TableLayout
	https://developer.android.google.cn/reference/android/widget/TableLayout

	TableLayout.LayoutParams
	https://developer.android.google.cn/reference/android/widget/TableLayout.LayoutParams
	*/

	// 表格布局的行和列数
	private int rows;
	private int columns;

	// 指示表大小无法再更改
	private boolean fixed;

	private int justification;

	/**
	 * 创建一个新的表格布局。
	 *
	 * @param container  视图容器
	 */
	表格布局Impl(视图组件容器 container) {
		// 先创建表格布局
		super(new TableLayout(MainActivity.getContext()), container);

		justification = 组件.对齐_上;

		// 取表格布局
		TableLayout layoutManager = (TableLayout) getLayoutManager();
		// 设置所有列可拉伸
		layoutManager.setStretchAllColumns(true);
	}

	/**
	 * {@inheritDoc}
	 *
	 * Note that table layout uses {@link #placeComponent(视图组件)} to
	 * actually add the component. This is necessary because you need to set both
	 * the {@link 可视组件#列()} and {@link 可视组件#行()}
	 * properties before the component can be put into the layout.
	 */
	@Override
	public void addComponent(视图组件 component) {
	}

	/**
	 * 将组件放入其列和行中。
	 *
	 * @param component  准备好放入其列和行中的组件
	 */
	public void placeComponent(视图组件 component) {
		ensureTableInitialized();

		int column = component.列();
		int row = component.行();
		if (0 <= column && column < columns && 0 <= row && row < rows) {
			// 取布局管理器（其实就是表格布局的父类）
			ViewGroup layoutManager = getLayoutManager();
			// 取指定表格行，然后添加视图
			((TableRow) layoutManager.getChildAt(row)).addView(component.getView(), new TableRow.LayoutParams(column));
		}
	}

	/**
	 * 确保表已初始化
	 */
	private void ensureTableInitialized() {
		if (!fixed) {
			// 添加第一个组件后，无法再调整表的大小
			fixed = true;
			// 取布局管理器（其实就是表格布局的父类）
			ViewGroup layoutManager = getLayoutManager();
			// 取上下文
			Context context = layoutManager.getContext();
			// 按行循环
			for (int row = 0; row < rows; row++) {
				// 添加表格行
				layoutManager.addView(new TableRow(context), new TableLayout.LayoutParams(
					// 表格行宽度适应内容
					ViewGroup.LayoutParams.WRAP_CONTENT,
					// 表格行高度适应内容
					ViewGroup.LayoutParams.WRAP_CONTENT
				));
			}
		}
	}

	/* 表格布局 实现 */

	@Override
	public void 行数(int newRows) {
		rows = newRows;
	}

	@Override
	public void 列数(int newColumns) {
		columns = newColumns;
	}

	/* 自定义扩展 */

	@Override
	public int 内容对齐() {
		return justification;
	}

	@Override
	public void 内容对齐(int justification) {
		this.justification = justification;
		TableLayout layoutManager = (TableLayout) getLayoutManager();
		layoutManager.setGravity(ViewUtil.simpleToAndroidGravity(justification));
	}

	@Override
	public boolean 所有列可收缩() {
		return ((TableLayout) getLayoutManager()).isShrinkAllColumns();
	}

	@Override
	public void 所有列可收缩(boolean shrinkAllColumns) {
		((TableLayout) getLayoutManager()).setShrinkAllColumns(shrinkAllColumns);
	}

	@Override
	public boolean 所有列可拉伸() {
		return ((TableLayout) getLayoutManager()).isStretchAllColumns();
	}

	@Override
	public void 所有列可拉伸(boolean stretchAllColumns) {
		((TableLayout) getLayoutManager()).setStretchAllColumns(stretchAllColumns);
	}

	@Override
	public boolean 列是否折叠(int columnIndex) {
		return ((TableLayout) getLayoutManager()).isColumnCollapsed(columnIndex);
	}

	@Override
	public void 置列折叠(int columnIndex, boolean isCollapsed) {
		((TableLayout) getLayoutManager()).setColumnCollapsed(columnIndex, isCollapsed);
	}

	@Override
	public boolean 列是否可收缩(int columnIndex) {
		return ((TableLayout) getLayoutManager()).isColumnShrinkable(columnIndex);
	}

	@Override
	public void 置列可收缩(int columnIndex, boolean isShrinkable) {
		((TableLayout) getLayoutManager()).setColumnShrinkable(columnIndex, isShrinkable);
	}

	@Override
	public boolean 列是否可拉伸(int columnIndex) {
		return ((TableLayout) getLayoutManager()).isColumnStretchable(columnIndex);
	}

	@Override
	public void 置列可拉伸(int columnIndex, boolean isStretchable) {
		((TableLayout) getLayoutManager()).setColumnStretchable(columnIndex, isStretchable);
	}
}

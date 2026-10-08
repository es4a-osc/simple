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

package simple.runtime.components;

import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.SimpleFunction;

/**
 * 一个允许子组件以表格形式放置的布局组件。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public interface 表格布局 extends 布局 {
	/**
	 * 行数属性设置器方法。
	 *
	 * @param rows 此布局中的行数
	 */
	@SimpleProperty
	void 行数(int rows);

	/**
	 * 列数属性设置器方法。
	 *
	 * @param cols 此布局中的列数
	 */
	@SimpleProperty
	void 列数(int cols);

	/* 自定义扩展 */

	/**
	 * 内容对齐属性获取器方法。
	 *
	 * @return 其中之一{@link 组件#对齐_左}、
	 *          {@link 组件#对齐_水平居中}或
	 *          {@link 组件#对齐_右}
	 */
	@SimpleProperty
	int 内容对齐();

	/**
	 * 内容对齐属性设置器方法。
	 *
	 * @param justification 其中之一{@link 组件#对齐_左}、
	 *                       {@link 组件#对齐_水平居中}或
	 *                       {@link 组件#对齐_右}
	 */
	@SimpleProperty()
	void 内容对齐(int justification);

	/**
	 * 所有列可收缩获取器方法。
	 */
	@SimpleProperty
	boolean 所有列可收缩();

	/**
	 * 所有列可收缩设置器方法。
	 * 
	 * @param shrinkAllColumns 可收缩所有列
	 */
	@SimpleProperty
	void 所有列可收缩(boolean shrinkAllColumns);

	/**
	 * 所有列可拉伸获取器方法。
	 */
	@SimpleProperty
	boolean 所有列可拉伸();

	/**
	 * 所有列可拉伸设置器方法。
	 * 
	 * @param stretchAllColumns 可拉伸所有列
	 */
	@SimpleProperty
	void 所有列可拉伸(boolean stretchAllColumns);

	/**
	 * 检查列是否折叠
	 * 
	 * @param columnIndex 列索引，从0开始。
	 * @return 折叠返回真，否则返回假
	 */
	@SimpleFunction
	boolean 列是否折叠(int columnIndex);

	/**
	 * 设置列是否折叠
	 * 
	 * @param columnIndex 列索引，从0开始。
	 * @param isCollapsed 是否折叠
	 */
	@SimpleFunction
	void 置列折叠(int columnIndex, boolean isCollapsed);

	/**
	 * 检验列是否可收缩
	 * 
	 * @param columnIndex 列索引，从0开始。
	 * @return 可收缩返回真，否则返回假
	 */
	@SimpleFunction
	boolean 列是否可收缩(int columnIndex);

	/**
	 * 设置列是否可收缩
	 * 
	 * @param columnIndex 列索引，从0开始。
	 * @param isShrinkable 是否可收缩
	 */
	@SimpleFunction
	void 置列可收缩(int columnIndex, boolean isShrinkable);

	/**
	 * 检验列是否可拉伸
	 * 
	 * @param columnIndex 列索引，从0开始。
	 * @return 可拉伸返回真，否则返回假
	 */
	@SimpleFunction
	boolean 列是否可拉伸(int columnIndex);

	/**
	 * 设置列是否可拉伸
	 * 
	 * @param columnIndex 列索引，从0开始。
	 * @param isShrinkable 是否可拉伸
	 */
	@SimpleFunction
	void 置列可拉伸(int columnIndex, boolean isStretchable);
}

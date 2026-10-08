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

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;

/**
 * 基于文本的Simple组件（如按钮，标签，文本框等）。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public interface 文本组件 extends 可视组件 {

	/**
	 * 字体粗体属性获取器方法。
	 *
	 * @return  {@code true}表示粗体，{@code false}正常。
	 */
	@SimpleProperty
	boolean 字体加粗();

	/**
	 * 字体粗体属性设置器方法。
	 *
	 * @param bold   {@code true}表示粗体，{@code false}正常。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "False"
	)
	void 字体加粗(boolean bold);

	/**
	 * 字体斜体属性获取器方法。
	 *
	 * @return  {@code true}表示倾斜，{@code false}正常。
	 */
	@SimpleProperty
	boolean 字体倾斜();

	/**
	 * 字体倾斜属性设置器方法。
	 *
	 * @param italic  {@code true}表示倾斜，{@code false}正常。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "False"
	)
	void 字体倾斜(boolean italic);

	/**
	 * 字体大小属性获取器方法。
	 *
	 * @return  字体大小（以像素为单位）
	 */
	@SimpleProperty
	float 字体大小();

	/**
	 * 字体大小属性设置器方法。
	 *
	 * @param size  字体大小（以缩放像素为单位）
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_SINGLE,
		initializer = 组件.字体_默认_大小 + ""
	)
	void 字体大小(float size);

	/**
	 * 字体类型属性获取器方法。
	 *
	 * @return  其中之一{@link 组件#字体类型_默认}、
	 *          {@link 组件#字体类型_衬线}、
	 *          {@link 组件#字体类型_无衬线}或
	 *          {@link 组件#字体类型_等宽}
	 */
	@SimpleProperty
	int 字体类型();

	/**
	 * 字体类型属性设置器方法。
	 *
	 * @param typeface  其中之一{@link 组件#字体类型_默认}、
	 *                  {@link 组件#字体类型_衬线}、
	 *                  {@link 组件#字体类型_无衬线}或
	 *                  {@link 组件#字体类型_等宽}
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_TYPEFACE,
		initializer = 组件.字体类型_默认 + ""
	)
	void 字体类型(int typeface);

	/**
	 * 内容对齐属性获取器方法。
	 *
	 * @return  其中之一{@link 组件#对齐_左}、
	 *          {@link 组件#对齐_水平居中}或
	 *          {@link 组件#对齐_右}
	 */
	@SimpleProperty
	int 内容对齐();

	/**
	 * 内容对齐属性设置器方法。
	 *
	 * @param justification  其中之一{@link 组件#对齐_左}、
	 *                       {@link 组件#对齐_水平居中}或
	 *                       {@link 组件#对齐_右}
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_TEXTJUSTIFICATION,
		initializer = 组件.对齐_左 + ""
	)
	void 内容对齐(int justification);

	/**
	 * 文本属性获取器方法。
	 *
	 * @return  文本框内容
	 */
	@SimpleProperty
	String 文本();

	/**
	 * 文本属性设置器方法：设置文本框的内容
	 *
	 * @param newtext  文本框中的新文本
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_STRING,
		initializer = "\"\""
	)
	void 文本(String newtext);

	/**
	 * 文本颜色属性获取器方法。
	 *
	 * @return  带有alpha的文本RGB颜色
	 */
	@SimpleProperty
	int 文本颜色();

	/**
	 * 文本颜色属性设置器方法。
	 *
	 * @param argb  带有alpha的文本RGB颜色
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_COLOR,
		initializer = 组件.颜色_黑 + ""
	)
	void 文本颜色(int argb);

	/**
	 * 单行模式属性获取器方法。
	 *
	 * @return 已单行模式返回{@code true}，未单行模式{@code false}。
	 */
	@SimpleProperty
	boolean 单行模式();

	/**
	 * 单行模式属性设置器方法。
	 *
	 * @param singleLine 单行模式返回{@code true}，否则{@code false}。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "False"
	)
	void 单行模式(boolean singleLine);

	/**
	 * 设置左边图标。
	 *
	 * @param image 图片，支持{@code 资源标识}、{@code @drawable/资源名}、
	 * 				{@code 资产文件名}、{@code /外部文件名}。
	 * @param width 绘制宽度，单位像素。
	 * @param height 绘制高度，单位像素。
	 */
	@SimpleFunction
	void 置左边图标(String image, int width, int height);

	/**
	 * 设置顶边图标。
	 *
	 * @param image 图片，支持{@code 资源标识}、{@code @drawable/资源名}、
	 * 				{@code 资产文件名}、{@code /外部文件名}。
	 * @param width 绘制宽度，单位像素。
	 * @param height 绘制高度，单位像素。
	 */
	@SimpleFunction
	void 置顶边图标(String image, int width, int height);

	/**
	 * 设置右边图标。
	 *
	 * @param image 图片，支持{@code 资源标识}、{@code @drawable/资源名}、
	 * 				{@code 资产文件名}、{@code /外部文件名}。
	 * @param width 绘制宽度，单位像素。
	 * @param height 绘制高度，单位像素。
	 */
	@SimpleFunction
	void 置右边图标(String image, int width, int height);

	/**
	 * 设置底边图标。
	 *
	 * @param image 图片，支持{@code 资源标识}、{@code @drawable/资源名}、
	 * 				{@code 资产文件名}、{@code /外部文件名}。
	 * @param width 绘制宽度，单位像素。
	 * @param height 绘制高度，单位像素。
	 */
	@SimpleFunction
	void 置底边图标(String image, int width, int height);

	/**
	 * 设置图标的填充距离。
	 *
	 * @param padding 填充距离。
	 */
	@SimpleFunction
	void 置图标填充(int padding);
}

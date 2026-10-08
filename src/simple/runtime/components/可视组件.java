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
import simple.runtime.variants.Variant;

/**
 * 可见的Simple组件。
 *
 * @author Herbert Czymontek
 */
@SimpleObject
public interface 可视组件 extends 组件 {

	/* 可视组件 基础属性定义 */

	/**
	 * 标识属性获取方法。
	 *
	 * @return 返回当前可视组件的标识，注意{@code -1}为无标识
	 */
	@SimpleProperty
	int 标识();

	/**
	 * 标识属性设置方法。
	 *
	 * @param 标识 设置可视组件的标识，注意{@code -1}为无标识
	 */
	@SimpleProperty
	void 标识(int id);

	/**
	 * 可视的获取属性方法。
	 *
	 * @return 返回组件的可视状态。
	 */
	@SimpleProperty
	boolean 可视();

	/**
	 * 可视的属性设置方法。
	 *
	 * @param visible 欲设置组件的可视状态。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "True"
	)
	void 可视(boolean visible);

	/**
	 * 宽度属性获取器方法。
	 *
	 * @return 布局所使用的宽度属性
	 */
	@SimpleProperty
	Variant 宽度();

	/**
	 * 宽度属性设置器方法。
	 *
	 * @param width 布局所使用的宽度属性，兼容解析单位像素字符串
	 */
	@SimpleProperty
	void 宽度(Variant width);

	/**
	 * 高度属性获取器方法。
	 *
	 * @return 布局所使用的高度属性
	 */
	@SimpleProperty
	Variant 高度();

	/**
	 * 高度属性设置器方法。
	 *
	 * @param height 布局所使用的高度属性，兼容解析单位像素字符串
	 */
	void 高度(Variant height);

	/* 边距相关，该布局参数支持所有布局 */

	/**
	 * 左边距属性获取方法。
	 *
	 * @return 左边距
	 */
	@SimpleProperty
	Variant 左边距();

	/**
	 * 左边距充属性设置方法。
	 *
	 * @param leftMargin 左边距
	 */
	@SimpleProperty
	void 左边距(Variant leftMargin);

	/**
	 * 顶边距属性获取方法。
	 *
	 * @return 顶边距
	 */
	@SimpleProperty
	Variant 顶边距();

	/**
	 * 顶边距充属性设置方法。
	 *
	 * @param topMargin 顶边距
	 */
	@SimpleProperty
	void 顶边距(Variant topMargin);

	/**
	 * 右边距属性获取方法。
	 *
	 * @return 右边距
	*/
	@SimpleProperty
	Variant 右边距();

	/**
	 * 右边距充属性设置方法。
	 *
	 * @param rightMargin 右边距
	 */
	@SimpleProperty
	void 右边距(Variant rightMargin);

	/**
	 * 底边距属性获取方法。
	 *
	 * @return 底边距
	 */
	@SimpleProperty
	Variant 底边距();

	/**
	 * 底边距属性设置方法。
	 *
	 * @param bottomMargin 底边距
	 */
	@SimpleProperty
	void 底边距(Variant bottomMargin);

	/**
	 * 设置组件的边距。
	 *
	 * @param leftMargin 左边距
	 * @param topMargin 顶边距
	 * @param rightMargin 右边距
	 * @param bottomMargin 底边距
	 */
	@SimpleFunction
	void 置边距(Variant leftMargin, Variant topMargin, Variant rightMargin, Variant bottomMargin);

	/* 填充相关，该布局参数支持所有布局 */

	/**
	 * 左填充属性获取方法。
	 *
	 * @return 左填充
	 */
	@SimpleProperty
	Variant 左填充();

	/**
	 * 左填充属性设置器方法。
	 *
	 * @param leftPadding 左填充
	 */
	@SimpleProperty
	void 左填充(Variant leftPadding);

	/**
	 * 顶填充属性获取器方法。
	 *
	 * @return 顶填充
	 */
	@SimpleProperty
	Variant 顶填充();

	/**
	 * 顶填充属性设置器方法。
	 *
	 * @param topPadding 顶填充
	 */
	@SimpleProperty
	void 顶填充(Variant topPadding);

	/**
	 * 右填充属性获取器方法。
	 *
	 * @return 右填充
	 */
	@SimpleProperty
	Variant 右填充();

	/**
	 * 右填充属性设置器方法。
	 *
	 * @param rightPadding 右填充
	 */
	@SimpleProperty
	void 右填充(Variant rightPadding);

	/**
	 * 底填充属性获取器方法。
	 *
	 * @return 底填充
	 */
	@SimpleProperty
	Variant 底填充();

	/**
	 * 底填充属性设置器方法。
	 *
	 * @param bottomPadding 底填充
	 */
	@SimpleProperty
	void 底填充(Variant bottomPadding);

	/**
	 * 设置组件的填充。
	 *
	 * @param leftPadding 左填充
	 * @param topPadding 顶填充
	 * @param rightPadding 右填充
	 * @param bottomPadding 底填充
	 */
	@SimpleFunction
	void 置填充(Variant leftPadding, Variant topPadding, Variant rightPadding, Variant bottomPadding);

	/* 背景相关 */

	/**
	 * 背景颜色属性获取器方法。
	 *
	 * @return  具有Alpha的背景RGB颜色
	 */
	@SimpleProperty
	int 背景颜色();

	/**
	 * 背景颜色属性设置器方法。
	 *
	 * @param argb  具有Alpha的背景RGB颜色
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_COLOR
	)
	void 背景颜色(int argb);

	/**
	 * 背景图片属性获取器方法。
	 *
	 * @return 图片路径或图片标识
	 */
	@SimpleProperty
	String 背景图片();

	/**
	 * 背景图片属性设置器方法。
	 *
	 * @param image 图片，支持{@code 资源标识}、{@code @drawable/资源名}、
	 * 				{@code 资产文件名}、{@code /外部文件名}
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_ASSET,
		initializer = "\"\""
	)
	void 背景图片(String image);
	
	/* 对齐相关，该布局参数仅支持：线性布局、单帧布局、表格布局 */

	/**
	 * 取自身在容器内容对齐属性获取方法。
	 *
	 * <p>仅限位于{@code 线性布局}、{@code 单帧布局}、{@code 表格布局}中时可用。
	 *
	 * @return	返回值请参考{@link 组件#对齐_左}等
	 */
	@SimpleProperty
	int 对齐();

	/**
	 * 设置自身在容器对齐属性设置方法。
	 *
	 * <p>仅限位于{@code 线性布局}、{@code 单帧布局}、{@code 表格布局}中时可用。
	 *
	 * @param justification	可选值请参考{@link 组件#对齐_左}等
	 */
	@SimpleProperty
	void 对齐(int justification);

	/* 权重相关，该布局参数仅支持：线性布局 */

	/**
	 * 权重属性获取方法。
	 *
	 * <p>仅限位于{@code 线性布局}中时可用。
	 *
	 * @return 权重
	 */
	@SimpleProperty
	float 权重();

	/**
	 * 权重属性设置方法：简单可以把该属性理解为占比。
	 *
	 * <p>仅限位于{@code 线性布局}中时可用。
	 *
	 * <p>注意该属性值受{@link 线性布局#权重总和(float)}、
	 * {@link 可视组件#宽度(int)}或{@link 可视组件#高度(int)}影响
	 *
	 * @param weight 权重
	 */
	@SimpleProperty
	void 权重(float weight);

	/* 行和列相关，该布局参数仅支持：表格布局 */

	/**
	 * 行属性获取器方法。
	 *
	 * @return  表布局使用的行
	 */
	@SimpleProperty
	int 行();

	/**
	 * 行属性设置器方法。
	 *
	 * @param row 表布局使用的行
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER,
		initializer = 组件.布局_无_行 + ""
	)
	void 行(int row);

	/**
	 * 列属性获取器方法。
	 *
	 * @return  表布局所使用的列
	 */
	@SimpleProperty
	int 列();

	/**
	 * 列属性设置器方法。
	 *
	 * @param column  表布局所使用的列
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER,
		initializer = 组件.布局_无_列 + ""
	)
	void 列(int column);

	/* 规则 定义，该布局参数仅支持：相对布局 */
	
	/**
	 * 为当前视图添加相对规则。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * 
	 * @param verb 相对规则，可选值请参考常量{@link 组件#布局_规则_左}等
	 */
	@SimpleFunction
	void 添加规则(int verb);

	/**
	 * 为当前视图添加相对规则。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * 
	 * @param verb 相对规则，可选值请参考常量{@link 组件#布局_规则_左}等
	 * @param anchor 瞄点值，可选值为{@link 组件#布局_规则_真}或视图标识
	 */
	@SimpleFunction
	void 添加规则(int verb, int anchor);

	/**
	 * 为当前视图删除相对规则。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * 
	 * @param verb 相对规则。
	 */
	@SimpleFunction
	void 删除规则(int verb);

	/**
	 * 为当前视图添加相对规则。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * 
	 * @param anchor 相对规则，可选值请参考常量{@link 组件#布局_规则_左}等
	 * @return 瞄点值，返回值为{@link 组件#布局_规则_真}或视图标识
	 */
	@SimpleFunction
	int 取瞄点(int verb);

	/**
	 * 位于左边属性获取方法：位于某个视图的左边。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.LEFT_OF}
	 * 
	 * @return 视图标识
	 */
	@SimpleProperty
	int 位于左边();
	
	/**
	 * 位于左边属性设置方法：位于某个视图的左边。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.LEFT_OF}
	 * 
	 * @param id 视图标识
	 */
	@SimpleProperty
	void 位于左边(int id);

	/**
	 * 位于左边属性获取方法：位于某个视图的顶边。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ABOVE}
	 * 
	 * @return 视图标识
	 */
	@SimpleProperty
	int 位于顶边();
	
	/**
	 * 位于顶边属性设置方法：位于某个视图的顶边。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ABOVE}
	 * 
	 * @param id 视图标识
	 */
	@SimpleProperty
	void 位于顶边(int id);

	/**
	 * 位于右边属性获取方法：位于某个视图的右边。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.RIGHT_OF}
	 * 
	 * @return 视图标识。
	 */
	@SimpleProperty
	int 位于右边();

	/**
	 * 位于右边属性设置方法：位于某个视图的右边。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ABOVE}
	 * 
	 * @param id 视图标识。
	 */
	@SimpleProperty
	void 位于右边(int id);

	/**
	 * 位于底边属性获取方法：位于某个视图的底边。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.BELOW}
	 * 
	 * @return 视图标识。
	 */
	@SimpleProperty
	int 位于底边();
	
	/**
	 * 位于底边属性设置方法：位于某个视图的底边。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ABOVE}
	 * 
	 * @param id 视图标识。
	 */
	@SimpleProperty
	void 位于底边(int id);
	
	/**
	 * 对齐基线属性获取方法：与某个视图的基线对齐。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_BASELINE}
	 * 
	 * @return 视图标识。
	 */
	@SimpleProperty
	int 对齐基线();
	
	/**
	 * 对齐基线属性设置方法：与某个视图的基线对齐。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_BASELINE}
	 * 
	 * @param id 视图标识。
	 */
	@SimpleProperty
	void 对齐基线(int id);

	/**
	 * 对齐左边属性获取方法：与某个视图的左边对齐。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_LEFT}
	 * 
	 * @return 视图标识。
	 */
	@SimpleProperty
	int 对齐左边();
	
	/**
	 * 对齐左边属性设置方法：与某个视图的左边对齐。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_LEFT}
	 * 
	 * @param id 视图标识。
	 */
	@SimpleProperty
	void 对齐左边(int id);

	/**
	 * 对齐顶边属性获取方法：与某个视图的顶边对齐。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_TOP}
	 * 
	 * @return 视图标识。
	 */
	@SimpleProperty
	int 对齐顶边();
	
	/**
	 * 对齐顶边属性设置方法：与某个视图的顶边对齐。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_LEFT}
	 * 
	 * @param id 视图标识。
	 */
	@SimpleProperty
	void 对齐顶边(int id);

	/**
	 * 对齐右边属性获取方法：与某个视图的右边对齐。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_RIGHT}
	 * 
	 * @return 视图标识。
	 */
	@SimpleProperty
	int 对齐右边();
	
	/**
	 * 对齐右边属性设置方法：与某个视图的右边对齐。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_RIGHT}
	 * 
	 * @param id 视图标识。
	 */
	@SimpleProperty
	void 对齐右边(int id);

	/**
	 * 对齐底边属性获取方法：与某个视图的底边对齐。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_BOTTOM}
	 * 
	 * @return 视图标识。
	 */
	@SimpleProperty
	int 对齐底边();

	/**
	 * 对齐底边属性设置方法：与某个视图的底边对齐。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_BOTTOM}
	 * 
	 * @param id 视图标识。
	 */
	@SimpleProperty
	void 对齐底边(int id);

	/**
	 * 对齐父左边属性获取方法：停靠于父级视图的左边。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_PARENT_LEFT}
	 * 
	 * @return {@code true}为开启，{@code false}为关闭。
	 */
	@SimpleProperty
	boolean 对齐父左边();
	
	/**
	 * 对齐父左边属性设置方法：停靠于父级视图的左边。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_PARENT_LEFT}
	 * 
	 * @param enable {@code true}为开启，{@code false}为关闭。
	 */
	@SimpleProperty
	void 对齐父左边(boolean enable);

	/**
	 * 对齐父顶边属性获取方法：停靠于父级视图的顶边。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_PARENT_TOP}
	 * 
	 * @return {@code true}为开启，{@code false}为关闭。
	 */
	@SimpleProperty
	boolean 对齐父顶边();
	
	/**
	 * 对齐父左边属性设置方法：停靠于父级视图的顶边。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_PARENT_TOP}
	 * 
	 * @param enable {@code true}为开启，{@code false}为关闭。
	 */
	@SimpleProperty
	void 对齐父顶边(boolean enable);

	/**
	 * 对齐父右边属性获取方法：停靠于父级视图的右边。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_PARENT_RIGHT}
	 * 
	 * @return {@code true}为开启，{@code false}为关闭。
	 */
	@SimpleProperty
	boolean 对齐父右边();
	
	/**
	 * 对齐父右边属性设置方法：停靠于父级视图的右边。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_PARENT_RIGHT}
	 * 
	 * @param enable {@code true}为开启，{@code false}为关闭。
	 */
	@SimpleProperty
	void 对齐父右边(boolean enable);

	/**
	 * 对齐父底边属性获取方法：停靠于父级视图的底边。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_PARENT_BOTTOM}
	 * 
	 * @return {@code true}为开启，{@code false}为关闭。
	 */
	@SimpleProperty
	boolean 对齐父底边();
	
	/**
	 * 对齐父底边属性设置方法：停靠于父级视图的底边。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_PARENT_BOTTOM}
	 * 
	 * @param enable {@code true}为开启，{@code false}为关闭。
	 */
	@SimpleProperty
	void 对齐父底边(boolean enable);

	/**
	 * 居中于父属性获取方法：位于父级视图的中间。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.CENTER_IN_PARENT}
	 * 
	 * @return {@code true}为开启，{@code false}为关闭。
	 */
	@SimpleProperty
	boolean 居中于父();
	
	/**
	 * 居中于父属性设置方法：位于父级视图的中间。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.CENTER_IN_PARENT}
	 * 
	 * @param enable {@code true}为开启，{@code false}为关闭。
	 */
	@SimpleProperty
	void 居中于父(boolean enable);

	/**
	 * 居中水平属性获取方法：位于父级视图的水平中间。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.CENTER_HORIZONTAL}
	 * 
	 * @return {@code true}为开启，{@code false}为关闭。
	 */
	@SimpleProperty
	boolean 居中水平();

	/**
	 * 居中水平属性设置方法：位于父级视图的水平中间。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.CENTER_HORIZONTAL}
	 * 
	 * @param enable {@code true}为开启，{@code false}为关闭。
	 */
	@SimpleProperty
	void 居中水平(boolean enable);
	
	/**
	 * 居中垂直属性获取方法：位于父级视图的垂直中间。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.CENTER_VERTICAL}
	 * 
	 * @return {@code true}为开启，{@code false}为关闭。
	 */
	@SimpleProperty
	boolean 居中垂直();
	
	/**
	 * 居中垂直属性设置方法：位于父级视图的垂直中间。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.CENTER_VERTICAL}
	 * 
	 * @param enable {@code true}为开启，{@code false}为关闭。
	 */
	@SimpleProperty
	void 居中垂直(boolean enable);

	/**
	 * 位于开始属性获取方法：位于某个视图的开始。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.START_OF}
	 * 
	 * @return 视图标识
	 */
	@SimpleProperty
	int 位于开始();
	
	/**
	 * 位于开始属性设置方法：位于某个视图的开始。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.START_OF}
	 * 
	 * @param id 视图标识
	 */
	@SimpleProperty
	void 位于开始(int id);

	/**
	 * 位于结束属性获取方法：位于某个视图的结束。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.END_OF}
	 * 
	 * @return 视图标识
	 */
	@SimpleProperty
	int 位于结束();
	
	/**
	 * 位于结束属性设置方法：位于某个视图的结束。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.START_OF}
	 * 
	 * @param id 视图标识
	 */
	@SimpleProperty
	void 位于结束(int id);

	/**
	 * 对齐开始属性获取方法：与某个视图开始对齐。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_START}
	 * 
	 * @return 视图标识
	 */
	@SimpleProperty
	int 对齐开始();
	
	/**
	 * 对齐开始属性设置方法：与某个视图开始对齐。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_START}
	 * 
	 * @param id 视图标识
	 */
	@SimpleProperty
	void 对齐开始(int id);

	/**
	 * 对齐结束属性获取方法：与某个视图结束对齐。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_END}
	 * 
	 * @return 视图标识
	 */
	@SimpleProperty
	int 对齐结束();
	
	/**
	 * 对齐结束属性设置方法：与某个视图结束对齐。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_END}
	 * 
	 * @param id 视图标识
	 */
	@SimpleProperty
	void 对齐结束(int id);

	/**
	 * 对齐父开始属性获取方法：位于父级视图的开始。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_PARENT_START}
	 * 
	 * @return {@code true}为开启，{@code false}为关闭。
	 */
	@SimpleProperty
	boolean 对齐父开始();

	/**
	 * 对齐父开始属性设置方法：位于父级视图的开始。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_PARENT_START}
	 * 
	 * @param enable {@code true}为开启，{@code false}为关闭。
	 */
	@SimpleProperty
	void 对齐父开始(boolean enable);

	/**
	 * 对齐父结束属性获取方法：位于父级视图的结束。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_PARENT_END}
	 * 
	 * @return {@code true}为开启，{@code false}为关闭。
	 */
	@SimpleProperty
	boolean 对齐父结束();
	
	/**
	 * 对齐父结束属性设置方法：位于父级视图的结束。
	 * 
	 * <p>仅限位于{@code 相对布局}中时可用。
	 * <p>{@code RelativeLayout.ALIGN_PARENT_END}
	 * 
	 * @param enable {@code true}为开启，{@code false}为关闭。
	 */
	@SimpleProperty
	void 对齐父结束(boolean enable);

	/* 左边和顶边等相关，该布局参数仅支持：绝对布局 */
	
	/**
	 * 相对父级容器左边位置。
	 * 仅在组件位于绝对布局时生效。
	 * 
	 * @return 返回绝对像素（px）
	 */
	@SimpleProperty
	Variant 左边();

	/**
	 * 相对父级容器左边位置。
	 * 仅在组件位于绝对布局时生效。
	 * 
	 * @param left 左边，兼容解析单位像素字符串
	 */
	@SimpleProperty
	void 左边(Variant left);

	/**
	 * 相对父级容器顶边位置。
	 * 仅在组件位于绝对布局时生效。
	 * 
	 * @return 返回绝对像素（px）
	 */
	@SimpleProperty
	Variant 顶边();

	/**
	 * 相对父级容器顶边位置。
	 * 仅在组件位于绝对布局时生效。
	 * 
	 * @param top 顶部，兼容解析单位像素字符串
	 */
	@SimpleProperty
	void 顶边(Variant top);

	/**
	 * 设置组件的填充。
	 *
	 * @param left 左边，兼容解析单位像素字符串
	 * @param top 顶边，兼容解析单位像素字符串
	 * @param width 宽度，兼容解析单位像素字符串
	 * @param height 高度，兼容解析单位像素字符串
	 */
	@SimpleFunction
	void 移动(Variant left, Variant top, Variant width, Variant height);
}

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

import simple.runtime.annotations.SimpleDataElement;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;

/**
 * 用于显示图像和动画的组件。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwd@qq.com
 */
@SimpleObject
public interface 图片组件 extends 可视组件 {

	/**
	 * 拉伸图片（不按比例）以填充视图的宽高。
	 *
	 * <p>{@code ImageView.ScaleType.FIT_XY}
	 */
	@SimpleDataElement
	static final int 显示方式_缩放图片 = 1;

	/**
	 * 当原图宽高或等于视图的宽高时，按原图大小居中显示；
	 * 反之将原图缩放至View的宽高居中显示。
	 *
	 * <p>{@code ImageView.ScaleType.CENTER_INSIDE}
	 */
	@SimpleDataElement
	static final int 显示方式_图片居中 = 2;

	/**
	 * 按比例拉伸图片，拉伸后图片的高度为视图的高度，且显示在View的中间。
	 *
	 * <p>{@code ImageView.ScaleType.FIT_CENTER}
	 */
	@SimpleDataElement
	static final int 显示方式_居中缩放 = 3;

	/**
	 * 图片属性设置方法。
	 *
	 * @param image 图片标识，支持{@code 资源标识}、{@code @drawable/资源名}、
	 * 				{@code 资产文件名}、{@code /外部文件名}。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_ASSET,
		initializer = "\"\""
	)
	void 图片(String image);

	/**
	 * 保持宽高比属性获取方法。
	 *
	 * @return 是否保持宽高比。
	 */
	@SimpleProperty
	boolean 保持宽高比();

	/**
	 * 保持宽高比属性设置方法。
	 *
	 * <p>需要与{@link 图片框#最大宽度(int)}、{@link 图片框#最大高度(int)}一起使用，
	 * 否则单独使用没有效果。
	 *
	 * @param adjustViewBounds 是否保持宽高比。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN
	)
	void 保持宽高比(boolean adjustViewBounds);

	/**
	 * 最大宽度属性获取方法。
	 *
	 * @return 最大宽度。
	 */
	@SimpleProperty
	int 最大宽度();

	/**
	 * 最大宽度属性获取方法。
	 *
	 * <p>单独使用无效，需要与{@link 图片框#保持宽高比(boolean)}一起使用。
	 * <p>如果想设置图片固定大小，又想保持图片宽高比需要如下设置：
	 * <p>1.{@code 图片框.保持宽高比(true)}。
	 * <p>2.设置{@code 图片框.最大宽度(int)}、{@code 图片框.最大高度(int)}。
	 * <p>3.设置{@code 可视组件.宽度(长度_适应内容)}和{@code 可视组件.高度(长度_适应内容)}。
	 *
	 * @param maxWidth 最大宽度。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER
	)
	void 最大宽度(int maxWidth);

	/**
	 * 最大高度属性获取方法。
	 *
	 * @return 最大高度。
	 */
	@SimpleProperty
	int 最大高度();

	/**
	 * 最大高度属性获取方法。
	 *
	 * <p>详细信息请参考{@link 图片框#最大宽度(int)}属性。
	 *
	 * @param maxHeight 最大高度。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER
	)
	void 最大高度(int maxHeight);

	/**
	 * 显示方式属性获取方法。
	 *
	 * @return 返回显示方式，返回值请参考{@link 图片框#显示方式_缩放图片}等常量。
	 */
	@SimpleProperty
	int 显示方式();

	/**
	 * 显示方式属性获取方法。
	 *
	 * @param scaleType 设置显示方式，可选值请参考{@link 图片框#显示方式_缩放图片}等常量。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER
	)
	void 显示方式(int scaleType);
}

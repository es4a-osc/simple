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

import simple.runtime.components.组件容器;
import simple.runtime.components.文本组件;
import simple.runtime.components.impl.android.util.ImageUtil;
import simple.runtime.components.impl.android.util.TextViewUtil;
import simple.runtime.errors.文件未存在错误;

import java.io.IOException;

import android.widget.TextView;

/**
 * 具有Android文本视图的所有组件的基础基类。
 *
 * @author Herbert Czymontek
 * @author Sharon Perl
 * @author 树先生 xhwsd@qq.com
 */
public abstract class 文本视图组件 extends 视图组件 implements 文本组件 {
	/*
	android.widget.TextView：
	https://developer.android.google.cn/reference/android/widget/TextView
	*/

	// 支持文本对齐
	private int justification;
	// 支持字体类型
	private int fontTypeface;
	// 支持文本颜色
	private int textColor;
	// 是否单行模式
	private boolean singleLine;

	/**
	 * 创建一个新的文本框基础组件
	 * @param container  容纳组件的容器（不可为{@code null})
	 */
	protected 文本视图组件(组件容器 container) {
		super(container);
	}

	@Override
	public boolean 字体加粗() {
		return TextViewUtil.isFontBold((TextView) getView());
	}

	@Override
	public void 字体加粗(boolean bold) {
		TextViewUtil.setFontBold((TextView) getView(), bold);
	}

	@Override
	public boolean 字体倾斜() {
		return TextViewUtil.isFontItalic((TextView) getView());
	}

	@Override
	public void 字体倾斜(boolean italic) {
		TextViewUtil.setFontItalic((TextView) getView(), italic);
	}

	@Override
	public float 字体大小() {
		return TextViewUtil.getFontSize((TextView) getView());
	}

	@Override
	public void 字体大小(float size) {
		TextViewUtil.setFontSize((TextView) getView(), size);
	}

	@Override
	public int 字体类型() {
		return fontTypeface;
	}

	@Override
	public void 字体类型(int typeface) {
		fontTypeface = typeface;
		TextViewUtil.setFontTypeface((TextView) getView(), typeface);
	}

	@Override
	public int 内容对齐() {
		return justification;
	}

	@Override
	public void 内容对齐(int newJustification) {
		justification = newJustification;
		TextViewUtil.setJustification((TextView) getView(), newJustification);
	}

	@Override
	public String 文本() {
		return TextViewUtil.getText((TextView) getView());
	}

	@Override
	public void 文本(String newtext) {
		TextViewUtil.setText((TextView) getView(), newtext);
	}

	@Override
	public int 文本颜色() {
		return textColor;
	}

	@Override
	public void 文本颜色(int argb) {
		textColor = argb;
		TextViewUtil.setTextColor((TextView) getView(), argb);
	}

	@Override
	public boolean 单行模式() {
		return singleLine;
	}

	@Override
	public void 单行模式(boolean singleLine) {
		this.singleLine = singleLine;
		//TextViewUtil.setSingleLine((TextView) getView(), singleLine);
	}

	@Override
	public void 置左边图标(String image, int width, int height) {
		try {
			TextViewUtil.setCompoundDrawableLeft((TextView) getView(),
					ImageUtil.getDrawable(image), width, height);
		} catch (IOException e) {
			throw new 文件未存在错误(image);
		}
	}

	@Override
	public void 置顶边图标(String image, int width, int height) {
		try {
			TextViewUtil.setCompoundDrawableTop((TextView) getView(),
					ImageUtil.getDrawable(image), width, height);
		} catch (IOException e) {
			throw new 文件未存在错误(image);
		}
	}

	@Override
	public void 置右边图标(String image, int width, int height) {
		try {
			TextViewUtil.setCompoundDrawableRight((TextView) getView(),
					ImageUtil.getDrawable(image), width, height);
		} catch (IOException e) {
			throw new 文件未存在错误(image);
		}
	}

	@Override
	public void 置底边图标(String image, int width, int height) {
		try {
			TextViewUtil.setCompoundDrawableBottom((TextView) getView(),
					ImageUtil.getDrawable(image), width, height);
		} catch (IOException e) {
			throw new 文件未存在错误(image);
		}
	}

	@Override
	public void 置图标填充(int padding) {
		TextViewUtil.setCompoundDrawablePadding((TextView) getView(), padding);
	}
}

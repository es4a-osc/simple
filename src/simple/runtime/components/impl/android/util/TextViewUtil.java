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

package simple.runtime.components.impl.android.util;

import simple.runtime.android.MainActivity;
import simple.runtime.components.组件;

import java.io.IOException;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.widget.TextView;

/**
 * 用于操作{@link TextView}对象的辅助方法。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public class TextViewUtil {

	private TextViewUtil() {
	}

	/**
	 * {@link TextView}对齐设置器。
	 *
	 * @param textview   文本视图实例
	 * @param justification  其中之一{@link 组件#对齐_左}、
	 *                       {@link 组件#对齐_水平居中}或
	 *                       {@link 组件#对齐_右}等
	 */
	public static void setJustification(TextView textview, int justification) {
		textview.setGravity(ViewUtil.simpleToAndroidGravity(justification));
		textview.invalidate();
	}

	/**
	 * 返回{@link TextView}的字体是否加粗。
	 *
	 * @param textview   文本视图实例
	 * @return  {@code true}表示粗体，{@code false}正常
	 */
	public static boolean isFontBold(TextView textview) {
		return textview.getTypeface().isBold();
	}

	/**
	 * 设置{@link TextView}的字体粗细。
	 *
	 * @param textview   文本视图实例
	 * @param bold  {@code true}表示粗体，{@code false}正常
	 */
	public static void setFontBold(TextView textview, boolean bold) {
		setFontStyle(textview, bold, Typeface.BOLD);
	}

	/**
	 * 返回{@link TextView}的字体样式。
	 *
	 * @param textview   文本视图实例
	 * @return  {@code true}表示斜体，{@code false}正常
	 */
	public static boolean isFontItalic(TextView textview) {
		return textview.getTypeface().isItalic();
	}

	/**
	 * 设置{@link TextView}的字体样式。
	 *
	 * @param textview   文本视图实例
	 * @param italic  {@code true}表示斜体，{@code false}正常
	 */
	public static void setFontItalic(TextView textview, boolean italic) {
		setFontStyle(textview, italic, Typeface.ITALIC);
	}

	/**
	 * 设置或清除{@link TextView}的当前字体的样式。
	 *
	 * @param textview 文本视图实例
	 * @param set {@code true}为设置，{@code false}为清除
	 * @param styleMask 欲设置或清除的字体样式
	 */
	private static void setFontStyle(TextView textview, boolean set, int styleMask) {
		// 取原样式
		Typeface tf = textview.getTypeface();
		if (tf == null) {
			tf = Typeface.DEFAULT;
		}

		// 按位取反，移出
		int style = tf.getStyle() & ~styleMask;
		if (set) {
			// 按位或，加入
			style |= styleMask;
		}

		// 设置样式
		textview.setTypeface(Typeface.create(tf, style));
		textview.invalidate();
	}

	/**
	 * 返回{@link TextView}的字体大小。
	 *
	 * @param textview   文本视图实例
	 * @return  字体大小（以像素为单位）
	 */
	public static float getFontSize(TextView textview) {
		return textview.getTextSize();
	}

	/**
	 * 设置{@link TextView}的字体大小。
	 *
	 * @param textview   文本视图实例
	 * @param size  字体大小（以缩放像素为单位）
	 */
	public static void setFontSize(TextView textview, float size) {
		textview.setTextSize(size);
		textview.invalidate();
	}

	/**
	 * 设置{@link TextView}的字体类型。
	 *
	 * @param textview   文本视图实例
	 * @param typeface  其中之一@link 组件#字体类型_默认}、
	 *                  {@link 组件#字体类型_衬线}、
	 *                  {@link 组件#字体类型_无衬线}或
	 *                  {@link 组件#字体类型_等宽}
	 */
	public static void setFontTypeface(TextView textview, int typeface) {
		Typeface tf;
		switch (typeface) {
			default:
				throw new IllegalArgumentException();

			case 组件.字体类型_默认:
				tf = Typeface.DEFAULT;
				break;

			case 组件.字体类型_衬线:
				tf = Typeface.SERIF;
				break;

			case 组件.字体类型_无衬线:
				tf = Typeface.SANS_SERIF;
				break;

			case 组件.字体类型_等宽:
				tf = Typeface.MONOSPACE;
				break;
		}
		textview.setTypeface(tf);
		textview.invalidate();
	}

	/**
	 * 设置文本视图的自定义字体。
	 *
	 * @param textview 文本视图
	 * @param path 字体路径，可以为{@code null}或空文本。
	 * @throws IOException
	 */
	public static void setCustomFont(TextView textview, String path) throws IOException {
		// 载入新字体集
		Typeface tf = null;
		if (path != null && !path.isEmpty()) {
			try {
				if (path.charAt(0) == '/') {
					// 外部路径
					tf = Typeface.createFromFile(path);
				} else {
					// 资产路径
					tf = Typeface.createFromAsset(MainActivity.getContext().getAssets(), path);
				}
			} catch (Exception e) {
				throw new IOException(path);
			}
		}
		// 设置字体
		textview.setTypeface(tf);
		textview.invalidate();
	}

	/**
	 * 返回{@link TextView}的文本。
	 *
	 * @param textview   文本视图实例
	 * @return  文本视图中显示的文本
	 */
	public static String getText(TextView textview) {
		return textview.getText().toString();
	}

	/**
	 * 设置{@link TextView}的文本。
	 *
	 * @param textview   文本视图实例
	 * @param text  要显示的新文本
	 */
	public static void setText(TextView textview, String text) {
		textview.setText(text);
		textview.invalidate();
	}

	/**
	 * 设置{@link TextView}的文本颜色。
	 *
	 * @param textview   文本视图实例
	 * @param argb  带alpha的文本RGB颜色
	 */
	public static void setTextColor(TextView textview, int argb) {
		textview.setTextColor(argb);
		textview.invalidate();
	}

	/**
	 * 设置文本视图内容字母是否全部自动转为大写。
	 *
	 * @param textview 文本视图
	 * @param allCaps 是否所有自动转大写
	 */
	public static void setAllCaps(TextView textview, boolean allCaps) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)  {
			// 添加于API14：TextView.setAllCaps(boolean)
			textview.setAllCaps(allCaps);
			textview.invalidate();
		}
		// 小于API14的SDK中主题样式默认不会将按钮标题设置为自动将字母转大写的
	}

	/**
	 * 设置可绘制对象在文本的左边。
	 *
	 * @param textView 文本视图
	 * @param drawable 可绘制对象，可为{@code null}。
	 * @param width 绘制宽度，单位像素。
	 * @param height 绘制高度，单位像素。
	 */
	public static void setCompoundDrawableLeft(TextView textView, Drawable drawable, int width, int height) {
		// API要求需要选设置 Drawable.setBounds
		if (drawable != null) drawable.setBounds(0, 0, width, height);
		// 为支持设置单个位置的图标，这里需要将其他位置的图标出
		Drawable[] drawables = textView.getCompoundDrawables();
		textView.setCompoundDrawables(drawable, drawables[1], drawables[2], drawables[3]);
		textView.invalidate();
	}

	/**
	 * 设置可绘制对象在文本的顶边。
	 *
	 * @param textView 文本视图
	 * @param drawable 可绘制对象，可为{@code null}。
	 * @param width 绘制宽度，单位像素。
	 * @param height 绘制高度，单位像素。
	 */
	public static void setCompoundDrawableTop(TextView textView, Drawable drawable, int width, int height) {
		if (drawable != null) drawable.setBounds(0, 0, width, height);
		Drawable[] drawables = textView.getCompoundDrawables();
		textView.setCompoundDrawables(drawables[0], drawable, drawables[2], drawables[3]);
		textView.invalidate();
	}

	/**
	 * 设置可绘制对象在文本的右边。
	 *
	 * @param textView 文本视图
	 * @param drawable 可绘制对象，可为{@code null}。
	 * @param width 绘制宽度，单位像素。
	 * @param height 绘制高度，单位像素。
	 */
	public static void setCompoundDrawableRight(TextView textView, Drawable drawable, int width, int height) {
		if (drawable != null) drawable.setBounds(0, 0, width, height);
		Drawable[] drawables = textView.getCompoundDrawables();
		textView.setCompoundDrawables(drawables[0], drawables[1], drawable, drawables[3]);
		textView.invalidate();
	}

	/**
	 * 设置可绘制对象在文本的底边。
	 *
	 * @param textView 文本视图
	 * @param drawable 可绘制对象，可为{@code null}。
	 * @param width 绘制宽度，单位像素。
	 * @param height 绘制高度，单位像素。
	 */
	public static void setCompoundDrawableBottom(TextView textView, Drawable drawable, int width, int height) {
		if (drawable != null) drawable.setBounds(0, 0, width, height);
		Drawable[] drawables = textView.getCompoundDrawables();
		textView.setCompoundDrawables(drawables[0], drawables[1], drawables[2], drawable);
		textView.invalidate();
	}

	/**
	 * 设置可绘制对象的填充距离。
	 * @param textView 文本视图
	 * @param padding 填充距离。
	 */
	public static void setCompoundDrawablePadding(TextView textView, int padding) {
		textView.setCompoundDrawablePadding(padding);
		textView.invalidate();
	}

	/**
	 * 设置文本是否单行模式。
	 *
	 * @param editText 文本视图对象。
	 * @param singleLine 是否单行模式。
	 */
	public static void setSingleLine(TextView textView, boolean singleLine) {
		textView.setSingleLine(singleLine);
		textView.invalidate();
	}

	/**
	 * 设置此TextView的行间距。除最后一行之外的每一行的高度都要乘以mult，并加上add。
	 *
	 * @param textView 文本视图对象。
	 * @param add 加数。
	 */
	public void setLineSpacingAdd(TextView textView, float add) {
		// 设置此TextView的行间距。除最后一行之外的每一行的高度都要乘以mult，并加上add。
		textView.setLineSpacing(add, textView.getLineSpacingMultiplier());
		textView.invalidate();
	}

	/**
	 * 设置此TextView的行间距。除最后一行之外的每一行的高度都要乘以mult，并加上add。
	 *
	 * @param textView 文本视图对象。
	 * @param mult 乘数。
	 */
	public void setLineSpacingMult(TextView textView, float mult) {
		// 设置此TextView的行间距。除最后一行之外的每一行的高度都要乘以mult，并加上add。
		textView.setLineSpacing(textView.getLineSpacingExtra(), mult);
		textView.invalidate();
	}
}

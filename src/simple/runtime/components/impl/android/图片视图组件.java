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
import simple.runtime.components.图片组件;
import simple.runtime.components.impl.android.util.ImageUtil;
import simple.runtime.errors.文件未存在错误;

import java.io.IOException;

import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/**
 * 用于显示图像和动画的组件
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public abstract class 图片视图组件 extends 视图组件 implements 图片组件 {
	/*
	 * android.widget.ImageView：
	 * https://developer.android.google.cn/reference/android/widget/ImageView
	 *
	 * android.widget.ImageView.ScaleType：
	 * https://developer.android.google.cn/reference/android/widget/ImageView.ScaleType.html
	 *
	 * ImageView属性：
	 * https://www.cnblogs.com/heimianshusheng/p/5017343.html
	 */

	// 图像的显示方式
	private int scaleType;

	/**
	 * 创建一个新的视图组件。
	 *
	 * @param container 容纳组件的容器（不可为 {@code null}，对于不可见的组件必须是窗口）
	 */
	protected 图片视图组件(组件容器 container) {
		super(container);
	}

	/* 图片视图组件 属性实现 */

	@Override
	public void 图片(String image) {
		try {
			Drawable drawable = ImageUtil.getDrawable(image);
			if (drawable != null) {
				((ImageView) getView()).setImageDrawable(drawable);
			}
		} catch (IOException e) {
			// 该异常尽可能在该方法中抛出，用于告诉用户是该方法发生异常
			throw new 文件未存在错误(image);
		}
	}

	public boolean 保持宽高比() {
		return ((ImageView) getView()).getAdjustViewBounds();
	}

	public void 保持宽高比(boolean adjustViewBounds) {
		((ImageView) getView()).setAdjustViewBounds(adjustViewBounds);
	}

	public int 最大宽度() {
		return ((ImageView) getView()).getMaxWidth();
	}

	public void 最大宽度(int maxWidth) {
		((ImageView) getView()).setMaxWidth(maxWidth);
	}

	public int 最大高度() {
		return ((ImageView) getView()).getMaxHeight();
	}

	public void 最大高度(int maxHeight) {
		((ImageView) getView()).setMaxHeight(maxHeight);
	}

	public int 显示方式() {
		return scaleType;
	}

	public void 显示方式(int scaleType) {
		ImageView view = (ImageView) getView();
		// ImageView.ScaleType
		// https://developer.android.google.cn/reference/android/widget/ImageView.ScaleType.html
		if (scaleType == 显示方式_缩放图片) {
			view.setScaleType(ImageView.ScaleType.FIT_XY);
		} else if (scaleType == 显示方式_图片居中) {
			view.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		} else if(scaleType == 显示方式_居中缩放) {
			view.setScaleType(ImageView.ScaleType.FIT_CENTER);
		} else {
			throw new IllegalArgumentException();
		}

		// 正确才保存
		this.scaleType = scaleType;
	}
}

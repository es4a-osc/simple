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

import simple.runtime.components.组件;

import java.util.concurrent.atomic.AtomicInteger;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.LinearLayout;

/**
 * 用于操作{@link View}对象的辅助方法。
 *
 * @author David Foster
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public final class ViewUtil {
	/*
	android.view.View 视图
	https://developer.android.google.cn/reference/android/view/View

	ViewGroup.LayoutParams 布局参数：最基础的布局参数
	https://developer.android.google.cn/reference/android/view/ViewGroup.LayoutParams

	ViewGroup.MarginLayoutParams 边距布局参数：是 ViewGroup.LayoutParams 的子类，扩展出了边距属性
	https://developer.android.google.cn/reference/android/view/ViewGroup.MarginLayoutParams

	LinearLayout 线性布局
	https://developer.android.google.cn/reference/android/widget/LinearLayout

	LinearLayout.LayoutParams 线性布局参数：是 ViewGroup.MarginLayoutParams 的子类，扩展出了线性布局属性
	https://developer.android.google.cn/reference/android/widget/LinearLayout.LayoutParams
	*/

	// 注意这里必须使用静态(static)修饰变量！所有调用该sNextGeneratedId变量，都使用同一个"内存对象"。
	private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

	private ViewUtil() {
	}

	/* 标识 */

	/**
	 * 生成视图标识符。
	 *
	 * @return 视图标识符。
	 */
	public static int generateViewId() {
		// API17兼容处理
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
			// 该代码可能与 R.id 冲突
			for (;;) {
				final int result = sNextGeneratedId.get();
				// aapt生成的id具有高字节非零；夹到其下的范围。
				int newValue = result + 1;
				if (newValue > 0x00FFFFFF) newValue = 1; // 转到1，而不是0。
				if (sNextGeneratedId.compareAndSet(result, newValue)) {
					return result;
				}
			}
		} else {
			// 添加于API17：View.generateViewId()
			return View.generateViewId();
		}
	}

	/**
	 * 返回视图的标识符。
	 *
	 * @param view 视图实例。
	 * @return 有效标识正整数，无效标识为{@code -1}。
	 */
	public static int getId(View view) {
		// View.NO_ID = -1
		return view.getId();
	}

	/**
	 * 设置视图的标识符。标识符不必在此视图的层次结构中是唯一的。标识符应为正数。
	 *
	 * @param view 视图实例。
	 * @param id 用于标识视图的数字。
	 */
	public static void setId(View view, int id) {
		view.setId(id);
	}

	/**
	 * 懒惰的分配视图标识符。
	 *
	 * @param view 视图实例。
	 * @return 视图标识。
	 */
	public static int allocationId(View view) {
		int id = view.getId();
		if (id == View.NO_ID) {
			id = generateViewId();
			view.setId(id);
		}
		return id;
	}

	/* 宽高 */

	/**
	 * 返回{@link View}的布局宽度。
	 *
	 * @param view 视图实例。
	 * @return 布局宽度。
	 */
	public static int getWidth(View view) {
		return view.getWidth();
	}

	/**
	 * 设置{@link View}的布局宽度。
	 *
	 * @param view 视图实例。
	 * @param width 布局宽度。
	 */
	public static void setWidth(View view, int width) {
		view.getLayoutParams().width = simpleToAndroidLength(width);
	}

	/**
	 * 返回{@link View}的布局高度。
	 *
	 * @param view 视图实例
	 * @return 布局高度
	 */
	public static int getHeight(View view) {
		return view.getHeight();
	}

	/**
	 * 设置{@link View}的布局高度。
	 *
	 * @param view 视图实例
	 * @param height 布局高度
	 */
	public static void setHeight(View view, int height) {
		view.getLayoutParams().height = simpleToAndroidLength(height);
	}

	/**
	 * 将Simple的长度转为Android长度。
	 *
	 * @param simpleLength Simple长度。
	 * @return Android长度。
	 */
	public static int simpleToAndroidLength(int simpleLength) {
		switch (simpleLength) {
			case 组件.长度_适应内容:
				return ViewGroup.LayoutParams.WRAP_CONTENT;
			case 组件.长度_匹配父级:
				// ViewGroup.LayoutParams.FILL_PARENT 在API8被 ViewGroup.LayoutParams.MATCH_PARENT 代替
				return ViewGroup.LayoutParams.MATCH_PARENT;
			default:
				return simpleLength;
		}
	}

	/* 可视 */

	/**
	 * 判断视图是否可视。
	 *
	 * @param view 视图实例。
	 * @return 可视返回{@code true}，非可视返回{@cdoe false}。
	 */
	public static boolean isVisible(View view) {
		return view.getVisibility() == View.VISIBLE;
	}

	/**
	 * 置视图是否可视。
	 *
	 * @param view 视图实例。
	 * @param visiable 是否可视。
	 */
	public static void setVisible(View view, boolean visible) {
		view.setVisibility(visible == true ? View.VISIBLE : View.GONE);
	}

	/* 焦点 */

	/**
	 * 返回视图当前是否可以获得焦点。
	 *
	 * @param view 视图实例。
	 * @return 可获得焦点返回{@code true}，否则返回{@code false}。
	 */
	public static boolean isFocusable(View view) {
		return view.isFocusable();
	}

	/**
	 * 设置视图是否可以通过方向键获得焦点。
	 *
	 * @param view 视图实例。
	 * @param focusable 为{@code true}可获得焦点，不可获得焦点为{@code false}。
	 */
	public static void setFocusable(View view, boolean focusable) {
		view.setFocusable(focusable);
	}

	/**
	 * 请求视图获得焦点。
	 *
	 * @param view 视图实例。
	 * @return 获得焦点返回{@cdoe trur}，否则返回{@code false}。
	 */
	public static boolean requestFocus(View view) {
		return view.requestFocus();
	}

	/**
	 * 清除视图焦点。
	 *
	 * @param view 视图实例。
	 */
	public static void clearFocus(View view) {
		view.clearFocus();
	}

	/**
	 * 返回视图是否可以触摸能够获得焦点。
	 *
	 * @param view 视图实例。
	 * @return 可获得焦点返回{@code true}，否则返回{@code false}。
	 */
	public static boolean isFocusableInTouchMode(View view) {
		return view.isFocusableInTouchMode();
	}

	/**
	 * 设置视图是否可以触摸能够获得焦点。
	 *
	 * <p>通俗易懂的说就是：
	 * <p>触摸先不触发点击事件，而是先让其获得焦点；
	 * <p>如果有焦点了，再点击才触发点击事件。
	 *
	 * @param view 视图实例。
	 * @param focusableInTouchMode 为{@code true}可获得焦点，不可获得焦点为{@code false}。
	 */
	public static void setFocusableInTouchMode(View view, boolean focusableInTouchMode) {
		view.setFocusableInTouchMode(focusableInTouchMode);
	}

	/**
	 * 设置视图获得触摸焦点。
	 *
	 * @param view 视图实例。
	 */
	public static void requestFocusFromTouch(View view) {
		view.requestFocusFromTouch();
	}

	/* 移除 */

	/**
	 * 将视图从父级布局中移除。
	 *
	 * @param view 视图实例。
	 */
	public static void removeView(View view) {
		ViewGroup group = (ViewGroup) view.getParent();
		if (group != null) {
			group.removeView(view);
		}
	}

	/* 启用 */

	/**
	 * 返回视图启用状态。
	 *
	 * @param view 视图实例
	 * @return {@code true}为启用，{@code false}为禁用
	 */
	public static boolean isEnabled(View view) {
		return view.isEnabled();
	}

	/**
	 * 设置视图启用状态。
	 *
	 * @param view 视图实例
	 * @param enabled {@code true}为启用，{@code false}为禁用
	 */
	public static void setEnabled(View view, boolean enabled) {
		view.setEnabled(enabled);
		view.invalidate();
	}

	/* 背景 */

	/**
	 * 设置视图的背景颜色。
	 *
	 * @param view 视图实例。
	 * @param color 背景的颜色。
	 */
	public static void setBackgroundColor(View view, int color) {
		view.setBackgroundColor(color);
		view.invalidate();
	}

	/**
	 * 设置视图的背景可绘制对象。
	 *
	 * @param view 视图实例。
	 * @param drawable 可绘制实例。
	 */
	@SuppressWarnings("deprecation")
	public static void setBackgroundDrawable(View view, Drawable drawable) {
		// API16兼容处理
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
			// 弃用于API16：View.setBackgroundDrawable(Drawable)
			view.setBackgroundDrawable(drawable);
		} else {
			// 添加于API16：View.setBackground(Drawable)
			view.setBackground(drawable);
		}
		view.invalidate();
	}

	/* 填充 */

	/**
	 * 取视图的左填充。
	 *
	 * @param view 视图实例。
	 * @return 左填充。
	 */
	public static int getLeftPadding(View view) {
		return view.getPaddingLeft();
	}

	/**
	 * 置视图的左填充。
	 *
	 * @param view 视图实例。
	 * @param leftPadding 左填充。
	 */
	public static void setLeftPadding(View view, int leftPadding) {
		view.setPadding(leftPadding, view.getPaddingTop(), view.getPaddingRight(), view.getPaddingBottom());
	}

	/**
	 * 取视图的顶填充。
	 *
	 * @param view 视图实例。
	 * @return 顶填充。
	 */
	public static int getTopPadding(View view) {
		return view.getPaddingTop();
	}

	/**
	 * 置视图的顶填充。
	 *
	 * @param view 视图实例。
	 * @param topPadding 顶填充。
	 */
	public static void setTopPadding(View view, int topPadding) {
		view.setPadding(view.getPaddingLeft(), topPadding, view.getPaddingRight(), view.getPaddingBottom());
	}

	/**
	 * 取视图的右填充。
	 *
	 * @param view 视图实例。
	 * @return 右填充。
	 */
	public static int getRightPadding(View view) {
		return view.getPaddingRight();
	}

	/**
	 * 置视图的右填充。
	 *
	 * @param view 视图实例。
	 * @param rightPadding 右填充。
	 */
	public static void setRightPadding(View view, int rightPadding) {
		view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), rightPadding, view.getPaddingBottom());
	}

	/**
	 * 取视图的底填充。
	 *
	 * @param view 视图实例。
	 * @return 底填充。
	 */
	public static int getBottomPadding(View view) {
		return view.getPaddingBottom();
	}

	/**
	 * 置视图的底填充。
	 *
	 * @param view 视图实例。
	 * @param bottomPadding	底填充。
	 */
	public static void setBottomPadding(View view, int bottomPadding) {
		view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), bottomPadding);
	}

	/**
	 * 设置视图的填充
	 *
	 * @param view 视图实例。
	 * @param leftPadding 左填充。
	 * @param topPadding 顶填充。
	 * @param rightPadding 右填充。
	 * @param bottomPadding	底填充。
	 */
	public static void setPadding(View view, int leftPadding, int topPadding, int rightPadding, int bottomPadding) {
		view.setPadding(leftPadding, topPadding, rightPadding, bottomPadding);
	}

	/* 边距 */

	/**
	 * 是否具有边距布局参数
	 *
	 * @param view 视图实例。
	 * @return 有返回{@code true}，无返回{@code false}。
	 */
	public static boolean hasMargin(View view) {
		ViewGroup.LayoutParams params = view.getLayoutParams();
		return params instanceof MarginLayoutParams;
	}

	/**
	 * 取视图左边距
	 *
	 * @param view 视图实例。
	 * @return 成功返回左边距，失败返回{@code 0}。
	 */
	public static int getLeftMargin(View view) {
		ViewGroup.LayoutParams params = view.getLayoutParams();
		if(params instanceof MarginLayoutParams) {
			return ((MarginLayoutParams) params).leftMargin;
		}
		return 0;
	}

	/**
	 * 置视图左边距。
	 *
	 * @param view 视图实例。
	 * @param leftMargin 左边距。
	 * @return 成功返回{@code true}，失败返回{@code false}。
	 */
	public static boolean setLeftMargin(View view, int leftMargin) {
		ViewGroup.LayoutParams params = view.getLayoutParams();
		if(params instanceof MarginLayoutParams) {
			((MarginLayoutParams) params).leftMargin = leftMargin;
			view.requestLayout();
			return true;
		}
		return false;
	}

	/**
	 * 取视图顶边距。
	 *
	 * @param view 视图实例。
	 * @return 成功返回顶边距，失败返回{@code 0}。
	 */
	public static int getTopMargin(View view) {
		ViewGroup.LayoutParams params = view.getLayoutParams();
		if(params instanceof MarginLayoutParams) {
			return ((MarginLayoutParams) params).topMargin;
		}
		return 0;
	}

	/**
	 * 置视图顶边距。
	 *
	 * @param view 视图实例。
	 * @param topMargin	顶边距。
	 * @return 成功返回{@code true}，失败返回{@code false}。
	 */
	public static boolean setTopMargin(View view, int topMargin) {
		ViewGroup.LayoutParams params = view.getLayoutParams();
		if(params instanceof MarginLayoutParams) {
			((MarginLayoutParams) params).topMargin = topMargin;
			view.requestLayout();
			return true;
		}
		return false;
	}

	/**
	 * 取视图右边距。
	 *
	 * @param view 视图实例。
	 * @return 成功返回右边距，失败返回{@code 0}。
	 */
	public static int getRightMargin(View view) {
		ViewGroup.LayoutParams params = view.getLayoutParams();
		if(params instanceof MarginLayoutParams) {
			return ((MarginLayoutParams) params).rightMargin;
		}
		return 0;
	}

	/**
	 * 置视图右边距。
	 *
	 * @param view	视图实例。
	 * @param rightMargin 右边距。
	 * @return 成功返回{@code true}，失败返回{@code false}。
	 */
	public static boolean setRightMargin(View view, int rightMargin) {
		ViewGroup.LayoutParams params = view.getLayoutParams();
		if(params instanceof MarginLayoutParams) {
			((MarginLayoutParams) params).rightMargin = rightMargin;
			view.requestLayout();
			return true;
		}
		return false;
	}

	/**
	 * 取视图底边距。
	 *
	 * @param view 视图实例。
	 * @return 成功返回底边距，失败返回{@code 0}。
	 */
	public static int getBottomMargin(View view) {
		ViewGroup.LayoutParams params = view.getLayoutParams();
		if(params instanceof MarginLayoutParams) {
			return ((MarginLayoutParams) params).bottomMargin;
		}
		return 0;
	}

	/**
	 * 置视图底边距。
	 *
	 * @param view 视图实例。
	 * @param bottomMargin 底边距。
	 * @return 成功返回{@code true}，失败返回{@code false}。
	 */
	public static boolean setBottomMargin(View view, int bottomMargin) {
		ViewGroup.LayoutParams params = view.getLayoutParams();
		if (params instanceof MarginLayoutParams) {
			((MarginLayoutParams) params).bottomMargin = bottomMargin;
			view.requestLayout();
			return true;
		}
		return false;
	}

	/**
	 * 置视图外边距。
	 *
	 * @param view 视图实例。
	 * @param leftMargin 左边距。
	 * @param topMargin	顶边距。
	 * @param rightMargin 右边距。
	 * @param bottomMargin 底边距。
	 * @return 成功返回{@code true}，失败返回{@code false}。
	 */
	public static boolean setMargin(View view, int leftMargin, int topMargin, int rightMargin, int bottomMargin) {
		ViewGroup.LayoutParams params = view.getLayoutParams();
		if (params instanceof MarginLayoutParams) {
			((MarginLayoutParams) params).setMargins(leftMargin, topMargin, rightMargin, bottomMargin);
			view.requestLayout();
			return true;
		}
		return false;
	}

	/* 重力（其实就是对齐） */

	/**
	 * 将Simple的对齐转Android的重力。
	 *
	 * @param simpleJustification Simple对齐
	 * @return Android重力（{@code android.view.Gravity}）
	 */
	public static int simpleToAndroidGravity(int simpleJustification) {
		switch (simpleJustification) {
			// 仅水平
			case 组件.对齐_左:
				return Gravity.LEFT;
			case 组件.对齐_水平居中:
				return Gravity.CENTER_HORIZONTAL;
			case 组件.对齐_右:
				return Gravity.RIGHT;
			// 仅垂直
			case 组件.对齐_上:
				return Gravity.TOP;
			case 组件.对齐_垂直居中:
				return Gravity.CENTER_VERTICAL;
			case 组件.对齐_下:
				return Gravity.BOTTOM;
			// 水平靠左，垂直*
			case 组件.对齐_左上:
				return Gravity.LEFT + Gravity.TOP;
			case 组件.对齐_左中:
				return Gravity.LEFT + Gravity.CENTER_VERTICAL;
			case 组件.对齐_左下:
				return Gravity.LEFT + Gravity.BOTTOM;
			// 水平居中，垂直*
			case 组件.对齐_中上:
				return Gravity.CENTER_HORIZONTAL + Gravity.TOP;
			case 组件.对齐_居中:
				return Gravity.CENTER_HORIZONTAL + Gravity.CENTER_VERTICAL;
			case 组件.对齐_中下:
				return Gravity.CENTER_HORIZONTAL + Gravity.BOTTOM;
			// 水平靠右，垂直*
			case 组件.对齐_右上:
				return Gravity.RIGHT + Gravity.TOP;
			case 组件.对齐_右中:
				return Gravity.RIGHT + Gravity.CENTER_VERTICAL;
			case 组件.对齐_右下:
				return Gravity.RIGHT + Gravity.BOTTOM;
			default:
				throw new IllegalArgumentException();
		}
	}
	
	/**
	 * 是否具有线性布局参数
	 * 
	 * @param view 视图
	 * @return 有返回{@code true}，无返回{@code false}
	 */
	public static boolean hasLinearLayout(View view) {
		ViewGroup.LayoutParams params = view.getLayoutParams();
		return params instanceof LinearLayout.LayoutParams;
	}

	/**
	 * 获取子视图的对齐，仅在视图位于线性布局（及子类）中时生效
	 *
	 * @param view 视图
	 * @return 成功返回重力，否则返回{@code 0}
	 */
	public static int getGravity(View view) {
		ViewGroup.LayoutParams params = view.getLayoutParams();
		if (params instanceof LinearLayout.LayoutParams) {
			return ((LinearLayout.LayoutParams) params).gravity;
		}
		return Gravity.NO_GRAVITY;
	}

	/**
	 * 设置子视图的对齐，仅在视图位于线性布局（及子类）中时生效
	 *
	 * @param view 视图
	 * @param gravity 对齐
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	public static boolean setGravity(View view, int gravity) {
		ViewGroup.LayoutParams params = view.getLayoutParams();
		if (params instanceof LinearLayout.LayoutParams) {
			((LinearLayout.LayoutParams) params).gravity = gravity;
			view.requestLayout();
			return true;
		}
		return  false;
	}

	/* 权重 */

	/**
	 * 获取子视图的权重，仅在视图位于线性布局（及子类）中时生效
	 *
	 * @param view 视图
	 * @return 成功返回权重，否则返回{@code 0}
	 */
	public static float getWeight(View view) {
		ViewGroup.LayoutParams params = view.getLayoutParams();
		if (params instanceof LinearLayout.LayoutParams) {
			return ((LinearLayout.LayoutParams) params).weight;
		}
		return 0;
	}

	/**
	 * 设置子视图的权重，仅在视图位于线性布局（及子类）中时生效
	 *
	 * @param view 视图
	 * @param weight 权重
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	public static boolean setWeight(View view, float weight) {
		ViewGroup.LayoutParams params = view.getLayoutParams();
		if (params instanceof LinearLayout.LayoutParams) {
			((LinearLayout.LayoutParams) params).weight = weight;
			view.requestLayout();
			return true;
		}
		return false;
	}
}

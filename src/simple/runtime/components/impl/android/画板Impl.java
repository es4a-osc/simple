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
import simple.runtime.components.画板;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.android.util.PaintUtil;
import simple.runtime.events.EventDispatcher;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Android实现Simple的画板组件。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public final class 画板Impl extends 视图组件 implements 画板 {

	// 颜色
	private Paint paintColor;
	private Paint backgroundColor;

	private int paintstyle;

	/**
	 * 创建一个新的画板组件。
	 *
	 * @param container  容纳组件的容器（不可为{@code null}）
	 */
	public 画板Impl(组件容器 container) {
		super(container);
	}

	@Override
	protected View createView() {
		CanvasView view = new CanvasView(MainActivity.getContext());

		// 初始化颜色
		backgroundColor = new Paint();
		paintColor = new Paint();
		paintColor.setStrokeWidth(1);

		return view;
	}

	// 画板 实现

	@Override
	public void 被按下(int x, int y) {
		EventDispatcher.dispatchEvent(this, "被按下", x, y);
	}

	@Override
	public void 被弹起(int x, int y) {
		EventDispatcher.dispatchEvent(this, "被弹起", x, y);
	}

	@Override
	public void 触摸移动(int lastX, int lastY, int currX, int currY) {
		EventDispatcher.dispatchEvent(this, "触摸移动", lastX, lastY, currX, currY);
	}

		@Override
		public void 触摸手势(int direction) {
				EventDispatcher.dispatchEvent(this, "触摸手势", direction);
	}

	@Override
	public int 画笔颜色() {
		return PaintUtil.extractARGB(paintColor);
	}

	@Override
	public void 画笔颜色(int argb) {
		PaintUtil.changePaint(paintColor, argb);
	}

	@Override
	public float 画笔粗细() {
		return paintColor.getStrokeWidth();
	}

	@Override
	public void 画笔粗细(float size) {
		if (size <= 0) {
			paintColor.setStrokeWidth(1.0F);
		} else {
			paintColor.setStrokeWidth(size);
		}
	}

	public int 画笔类型() {
		return paintstyle;
	}

	public void 画笔类型(int style) {
		if (style == 1) {
			paintColor.setStyle(Paint.Style.FILL);
		} else {
			paintColor.setStyle(Paint.Style.STROKE);
		}
		paintstyle = style;
	}

	public float 字体大小() {
		return paintColor.getTextSize();
	}

	public void 字体大小(float size) {
		paintColor.setTextSize(size);
	}

	@Override
	public void 清空() {
		CanvasView view = (CanvasView) getView();
		view.bitmap.eraseColor(Color.TRANSPARENT);
		view.invalidate();
	}

	@Override
	public void 画点(int x, int y) {
		CanvasView view = (CanvasView) getView();
		view.canvas.drawPoint(x, y, paintColor);
		view.invalidate();
	}

	@Override
	public void 画圆(int x, int y, float r) {
		CanvasView view = (CanvasView) getView();
		view.canvas.drawCircle(x, y, r, paintColor);
		view.invalidate();
	}

	@Override
	public void 画直线(int x1, int y1, int x2, int y2) {
		CanvasView view = (CanvasView) getView();
		view.canvas.drawLine(x1, y1, x2, y2, paintColor);
		view.invalidate();
	}

	public void 画贝塞尔曲线(int qx, int qy, int fx, int fy, int zx, int zy) {
		Path path = new Path();
		path.moveTo(qx, qy);
		path.quadTo(fx, fy, zx, zy);
		CanvasView view = (CanvasView) getView();
		view.canvas.drawPath(path, paintColor);
		view.invalidate();
	}

		public void 画矩形(int x, int y, int w, int h) {
				CanvasView view = (CanvasView) getView();
				Rect dst = new Rect(x, y, x + w, y + h);
				view.canvas.drawRect(dst, paintColor);
				view.invalidate();
		}

		public void 画圆角矩形(int x, int y, int w, int h, float r) {
				CanvasView view = (CanvasView) getView();
				RectF dst = new RectF(x, y, x + w, y + h);
				view.canvas.drawRoundRect(dst, r, r, paintColor);
				view.invalidate();
	}

		public void 画弧形(int x, int y, int w, int h, float startAngle, float sweepAngle, boolean useCenter) {
		CanvasView view = (CanvasView) getView();
		RectF dst = new RectF(x, y, x + w, y + h);
		view.canvas.drawArc(dst, startAngle, sweepAngle, useCenter, paintColor);
		view.invalidate();
	}

	@Override
	public int 背景颜色() {
		return PaintUtil.extractARGB(backgroundColor);
	}

	@Override
	public void 背景颜色(int argb) {
		PaintUtil.changePaint(backgroundColor, argb);
		CanvasView view = (CanvasView) getView();
		view.canvas.drawPaint(backgroundColor);
		view.invalidate();
	}

	/**
	 * Panel that can be drawn on. This overrides
	 * {@link View#onDraw(Canvas)} but not
	 * {@link View#onTouchEvent(MotionEvent)},
	 * which should be overridden in any subclass that should handle touch events.
	 */
	private class CanvasView extends View {

		protected final Bitmap bitmap;
		protected final Canvas canvas;
		private int lastX, lastY, currX, currY;
		private GestureDetector gestureDetector;

		@SuppressWarnings("deprecation")
		public CanvasView(Context context) {
			super(context);
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
				// 弃用于API15：Context.getWallpaperDesiredMinimumWidth()
				bitmap = Bitmap.createBitmap(context.getWallpaperDesiredMinimumWidth(),
						context.getWallpaperDesiredMinimumWidth(), Bitmap.Config.ARGB_8888);
			} else {
				WallpaperManager wm = WallpaperManager.getInstance(context);
				bitmap = Bitmap.createBitmap(wm.getDesiredMinimumWidth(),
						wm.getDesiredMinimumWidth(), Bitmap.Config.ARGB_8888);
			}
			canvas = new Canvas(bitmap);

			// 手势检测器
			gestureDetector = new GestureDetector(MainActivity.getContext(),
					new GestureDetector.SimpleOnGestureListener() {

				@Override
				public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
					int direction;
					int deltaX = (int) (e1.getRawX() - e2.getRawX());
					int deltaY = (int) (e1.getRawY() - e2.getRawY());
					if (Math.abs(deltaX) > Math.abs(deltaY)) {
						// 水平移动
						direction = deltaX > 0 ? 触摸_左滑 : 触摸_右滑;
					} else {
						// 垂直移动
						direction = deltaY > 0 ? 触摸_上滑 : 触摸_下滑;
					}
					触摸手势(direction);
					return true;
				}

				@Override
				public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
					int direction;
					if (Math.abs(distanceX) > Math.abs(distanceY)) {
						// 水平移动
						direction = distanceX > 0 ? 触摸_左移 : 触摸_右移;
					} else {
						// 垂直移动
						direction = distanceY > 0 ? 触摸_上移 : 触摸_下移;
					}
					触摸手势(direction);
					return true;
				}

				@Override
				public boolean onSingleTapConfirmed(MotionEvent e) {
					触摸手势(触摸_单击);
					return true;
				}

				@Override
				public boolean onDoubleTap(MotionEvent e) {
					触摸手势(触摸_双击);
					return true;
				}
			});
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			lastX = currX;
			lastY = currY;
			currX = (int) event.getX();
			currY = (int) event.getY();

			// 判断触摸动作
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					lastX = currX;
					lastY = currY;
					被按下(currX, currY);
					break;
					case MotionEvent.ACTION_UP:
					lastX = currX;
					lastY = currY;
					被弹起(currX, currY);
					break;
				case MotionEvent.ACTION_MOVE:
					触摸移动(lastX, lastY, currX, currY);
					break;
			}
			gestureDetector.onTouchEvent(event);
			return true; // 消耗掉这个事件
		}

		@Override
		protected void onDraw(Canvas c) {
			if (bitmap != null) {
				c.drawBitmap(bitmap, 0, 0, null);
			}
		}
	}
}

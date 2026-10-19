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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import simple.runtime.android.MainActivity;
import simple.runtime.components.画板;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.android.util.PaintUtil;
import simple.runtime.errors.文件读写错误;
import simple.runtime.errors.未初始化实例错误;
import simple.runtime.errors.索引超出界限错误;
import simple.runtime.events.EventDispatcher;
import simple.runtime.variants.Variant;

/**
 * 在软件位图中持久绘制，通过原生视图显示；所有坐标与尺寸使用 px。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public final class 画板Impl extends 视图组件 implements 画板 {

	private Paint paintColor;
	private int backgroundColor;
	private int paintStyle;
	private final MainActivity activity;
	private final MainActivity.OnDestroyListener destroyListener;
	private boolean destroyed;

	/**
	 * 创建画板并登记宿主销毁回调，显式销毁时同时移除原生视图。
	 *
	 * @param container 容纳画板的组件容器。
	 */
	public 画板Impl(组件容器 container) {
		super(container);
		activity = MainActivity.getContext();
		destroyListener = new MainActivity.OnDestroyListener() {
			@Override
			public void onDestroy() {
				release(false);
			}
		};
		activity.addOnDestroyListener(destroyListener);
	}

	@Override
	protected View createView() {
		// 此方法由父类构造器调用，画笔须在创建视图前就绪。
		paintColor = new Paint(Paint.ANTI_ALIAS_FLAG);
		paintColor.setStrokeWidth(1.0F);
		paintColor.setTextSize(16.0F);
		paintColor.setStyle(Paint.Style.FILL);
		paintStyle = 画笔_填充;
		return new CanvasView(MainActivity.getContext());
	}

	/* 组件事件 */

	@Override
	public void 被按下(int x, int y) {
		if (!destroyed) {
			EventDispatcher.dispatchEvent(this, "被按下", x, y);
		}
	}

	@Override
	public void 被弹起(int x, int y) {
		if (!destroyed) {
			EventDispatcher.dispatchEvent(this, "被弹起", x, y);
		}
	}

	@Override
	public void 触摸移动(int lastX, int lastY, int currX, int currY) {
		if (!destroyed) {
			EventDispatcher.dispatchEvent(this, "触摸移动", lastX, lastY, currX, currY);
		}
	}

	@Override
	public void 触摸手势(int direction) {
		if (!destroyed) {
			EventDispatcher.dispatchEvent(this, "触摸手势", direction);
		}
	}

	/* 组件属性 */

	/**
	 * 更新宽度后主动请求布局，使画面缓冲随实际视图尺寸同步。
	 *
	 * @param value 沿用可视组件的像素单位和长度解析规则。
	 */
	@Override
	public void 宽度(Variant value) {
		requireView();
		super.宽度(value);
		getView().requestLayout();
	}

	/**
	 * 更新高度后主动请求布局，不只修改布局参数中的数值。
	 *
	 * @param value 沿用可视组件的像素单位和长度解析规则。
	 */
	@Override
	public void 高度(Variant value) {
		requireView();
		super.高度(value);
		getView().requestLayout();
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
		requireFinite(size);
		paintColor.setStrokeWidth(size <= 0 ? 1.0F : size);
	}

	@Override
	public int 画笔类型() {
		return paintStyle;
	}

	@Override
	public void 画笔类型(int style) {
		if (style != 画笔_描边 && style != 画笔_填充) {
			throw new IllegalArgumentException("画笔类型必须为画笔_描边或画笔_填充");
		}

		paintColor.setStyle(style == 画笔_填充 ? Paint.Style.FILL : Paint.Style.STROKE);
		paintStyle = style;
	}

	@Override
	public float 字体大小() {
		return paintColor.getTextSize();
	}

	@Override
	public void 字体大小(float size) {
		requireFinite(size);
		if (size <= 0) {
			throw new IllegalArgumentException("字体大小必须为正数");
		}
		paintColor.setTextSize(size);
	}

	@Override
	public int 背景颜色() {
		return backgroundColor;
	}

	@Override
	public void 背景颜色(int argb) {
		backgroundColor = argb;
		CanvasView view = requireView();
		if (view.bitmap != null) {
			view.bitmap.eraseColor(argb);
		}
		view.invalidate();
	}

	/* 图形与文字绘制 */

	@Override
	public void 清空() {
		CanvasView view = requireView();
		if (view.bitmap != null) {
			view.bitmap.eraseColor(backgroundColor);
		}
		view.invalidate();
	}

	@Override
	public void 画点(int x, int y) {
		CanvasView view = requireView();
		view.drawingCanvas().drawPoint(x, y, paintColor);
		view.invalidate();
	}

	@Override
	public void 画圆(int x, int y, float radius) {
		requireFinite(radius);
		if (radius < 0) {
			throw new IllegalArgumentException("圆半径不得为负数");
		}
		CanvasView view = requireView();
		view.drawingCanvas().drawCircle(x, y, radius, paintColor);
		view.invalidate();
	}

	@Override
	public void 画直线(int x1, int y1, int x2, int y2) {
		CanvasView view = requireView();
		view.drawingCanvas().drawLine(x1, y1, x2, y2, paintColor);
		view.invalidate();
	}

	@Override
	public void 画矩形(int x, int y, int width, int height) {
		RectF bounds = rectangle(x, y, width, height);
		CanvasView view = requireView();
		view.drawingCanvas().drawRect(bounds, paintColor);
		view.invalidate();
	}

	@Override
	public void 画圆角矩形(int x, int y, int width, int height, float radius) {
		RectF bounds = rectangle(x, y, width, height);
		requireFinite(radius);
		if (radius < 0) {
			throw new IllegalArgumentException("圆角半径不得为负数");
		}
		CanvasView view = requireView();
		view.drawingCanvas().drawRoundRect(bounds, radius, radius, paintColor);
		view.invalidate();
	}

	@Override
	public void 画弧形(int x, int y, int width, int height,
			float startAngle, float sweepAngle, boolean useCenter) {
		RectF bounds = rectangle(x, y, width, height);
		requireFinite(startAngle);
		requireFinite(sweepAngle);
		CanvasView view = requireView();
		view.drawingCanvas().drawArc(bounds, startAngle, sweepAngle, useCenter, paintColor);
		view.invalidate();
	}

	@Override
	public void 画贝塞尔曲线(int startX, int startY, int controlX, int controlY, int endX, int endY) {
		Path path = new Path();
		path.moveTo(startX, startY);
		path.quadTo(controlX, controlY, endX, endY);

		CanvasView view = requireView();
		view.drawingCanvas().drawPath(path, paintColor);
		view.invalidate();
	}

	@Override
	public void 画文字(int x, int y, String text) {
		if (text == null) {
			throw new IllegalArgumentException("文字不得为空引用");
		}
		CanvasView view = requireView();
		view.drawingCanvas().drawText(text, x, y, paintColor);
		view.invalidate();
	}

	/* 图片载入、像素读取与保存 */

	@Override
	public void 画图片(String path, int x, int y) {
		CanvasView view = requireView();
		Bitmap image = decodeImage(path);
		try {
			view.drawingCanvas().drawBitmap(image, x, y, null);
			view.invalidate();
		} finally {
			// 图片已复制到软件画布，不再持有解码后的临时位图。
			image.recycle();
		}
	}

	@Override
	public void 画图片(String path, int x, int y, int width, int height) {
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException("目标图片宽高必须为正数");
		}
		RectF bounds = rectangle(x, y, width, height);
		CanvasView view = requireView();
		Bitmap image = decodeImage(path);
		try {
			Paint imagePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
			view.drawingCanvas().drawBitmap(image, null, bounds, imagePaint);
			view.invalidate();
		} finally {
			image.recycle();
		}
	}

	@Override
	public int 取点颜色(int x, int y) {
		Bitmap image = requireView().laidOutBitmap();
		if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) {
			throw new 索引超出界限错误();
		}
		return image.getPixel(x, y);
	}

	@Override
	public void 保存画面(String path) {
		保存画面(path, 图片格式_PNG);
	}

	@Override
	public void 保存画面(String path, int format) {
		if (format != 图片格式_PNG && format != 图片格式_JPEG) {
			throw new IllegalArgumentException("图片格式必须为图片格式_PNG或图片格式_JPEG");
		}
		requirePath(path);
		Bitmap image = requireView().laidOutBitmap();
		File target = new File(path);
		if (!target.isAbsolute()) {
			target = new File(activity.getFilesDir(), path);
		}

		Bitmap flattened = null;
		FileOutputStream output = null;
		try {
			// JPEG 没有透明通道，显式铺白，避免透明区域被编码成黑色。
			if (format == 图片格式_JPEG) {
				flattened = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(flattened);
				canvas.drawColor(Color.WHITE);
				canvas.drawBitmap(image, 0, 0, null);
				image = flattened;
			}

			output = new FileOutputStream(target);
			Bitmap.CompressFormat encoding = format == 图片格式_PNG
				? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
			if (!image.compress(encoding, 100, output)) {
				throw new IOException("图片编码失败");
			}
			output.close();
			output = null;
		} catch (IOException | SecurityException exception) {
			throw fileError("保存画面失败：" + path, exception);
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException ignored) {
					// 保留写入或编码阶段的原始异常。
				}
			}
			if (flattened != null) {
				flattened.recycle();
			}
		}
	}

	@Override
	public void 销毁() {
		release(true);
	}

	/* 内部校验与资源管理 */

	/**
	 * 取得仍可使用的画板，拒绝销毁后的绘制和读取。
	 *
	 * @return 画板原生视图。
	 */
	private CanvasView requireView() {
		if (destroyed) {
			throw new 未初始化实例错误();
		}
		return (CanvasView) getView();
	}

	/**
	 * 校验尺寸并使用浮点中间值计算右下边界，避免整数加法溢出。
	 *
	 * @param x 左边横坐标。
	 * @param y 顶边纵坐标。
	 * @param width 非负宽度。
	 * @param height 非负高度。
	 * @return 以 px 表示的包围矩形。
	 */
	private static RectF rectangle(int x, int y, int width, int height) {
		if (width < 0 || height < 0) {
			throw new IllegalArgumentException("图形宽高不得为负数");
		}
		return new RectF(x, y, (float) x + width, (float) y + height);
	}

	/**
	 * 拒绝 NaN 和无穷数，避免无效数值进入原生绘制。
	 *
	 * @param value 待检查的浮点数。
	 */
	private static void requireFinite(float value) {
		if (Float.isNaN(value) || Float.isInfinite(value)) {
			throw new IllegalArgumentException("绘图数值必须为有限数");
		}
	}

	/**
	 * 校验本地路径，不把网址、资源标识或 content URI 当作文件名。
	 *
	 * @param path 本地路径或资产文件名。
	 */
	private static void requirePath(String path) {
		if (path == null || path.trim().isEmpty() || path.contains("://") || path.startsWith("@")) {
			throw new IllegalArgumentException("请提供本地文件路径或资产文件名");
		}
	}

	/**
	 * 解码图片的真实像素，不使用资源密度缩放。
	 *
	 * @param path 资产文件名或绝对文件路径。
	 * @return 调用方在软件绘制完成后释放的临时位图。
	 */
	private Bitmap decodeImage(String path) {
		requirePath(path);
		InputStream input = null;
		try {
			input = new File(path).isAbsolute() ? new FileInputStream(path) : activity.getAssets().open(path);
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inScaled = false;
			Bitmap image = BitmapFactory.decodeStream(input, null, options);
			if (image == null) {
				throw new IOException("内容不是可解码的图片");
			}
			return image;
		} catch (IOException | SecurityException exception) {
			throw fileError("载入图片失败：" + path, exception);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException ignored) {
					// 只读流关闭失败不覆盖解码结果或原始异常。
				}
			}
		}
	}

	/**
	 * 转换为 Simple 可捕获的文件错误并保留底层原因。
	 *
	 * @param message 操作和路径说明。
	 * @param cause 原始读写异常。
	 * @return 运行库文件错误。
	 */
	private static 文件读写错误 fileError(String message, Exception cause) {
		文件读写错误 error = new 文件读写错误(message);
		error.initCause(cause);
		return error;
	}

	/**
	 * 释放组件持有的画面；宿主遍历销毁监听器时不修改其集合。
	 *
	 * @param removeListener 是否注销宿主销毁监听。
	 */
	private void release(boolean removeListener) {
		if (destroyed) {
			return;
		}
		destroyed = true;
		if (removeListener) {
			activity.removeOnDestroyListener(destroyListener);
		}
		super.销毁();
		((CanvasView) getView()).releaseBitmap();
	}

	/* 原生视图、画面尺寸与触摸事件 */

	/**
	 * 按实际布局尺寸保存画面，尺寸变化时原样复制左上交集区域。
	 */
	private final class CanvasView extends View {

		private Bitmap bitmap;
		private Canvas canvas;
		private final GestureDetector gestureDetector;
		private int lastX;
		private int lastY;
		private int currX;
		private int currY;

		/**
		 * 创建触摸与手势接收视图，画面在绘制或布局时按需分配。
		 *
		 * @param context 当前 Activity 上下文。
		 */
		CanvasView(Context context) {
			super(context);
			gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
				@Override
				public boolean onFling(MotionEvent first, MotionEvent last, float velocityX, float velocityY) {
					if (first == null || last == null) {
						return false;
					}
					float deltaX = first.getRawX() - last.getRawX();
					float deltaY = first.getRawY() - last.getRawY();
					触摸手势(Math.abs(deltaX) > Math.abs(deltaY)
						? (deltaX > 0 ? 触摸_左滑 : 触摸_右滑)
						: (deltaY > 0 ? 触摸_上滑 : 触摸_下滑));
					return true;
				}

				@Override
				public boolean onScroll(MotionEvent first, MotionEvent last, float distanceX, float distanceY) {
					触摸手势(Math.abs(distanceX) > Math.abs(distanceY)
						? (distanceX > 0 ? 触摸_左移 : 触摸_右移)
						: (distanceY > 0 ? 触摸_上移 : 触摸_下移));
					return true;
				}

				@Override
				public boolean onSingleTapConfirmed(MotionEvent event) {
					触摸手势(触摸_单击);
					return true;
				}

				@Override
				public boolean onDoubleTap(MotionEvent event) {
					触摸手势(触摸_双击);
					return true;
				}
			});
		}

		/**
		 * 提供绘图缓冲；布局前的绘制暂用屏幕大小，布局完成后按实际尺寸保留内容。
		 *
		 * @return 当前软件画布。
		 */
		Canvas drawingCanvas() {
			int width = getWidth() > 0 ? getWidth() : getResources().getDisplayMetrics().widthPixels;
			int height = getHeight() > 0 ? getHeight() : getResources().getDisplayMetrics().heightPixels;
			ensureBitmap(Math.max(1, width), Math.max(1, height));
			return canvas;
		}

		/**
		 * 取实际布局大小的完整画面，防止把临时尺寸误当成最终保存尺寸。
		 *
		 * @return 当前完整画面。
		 */
		Bitmap laidOutBitmap() {
			if (getWidth() <= 0 || getHeight() <= 0) {
				throw new 未初始化实例错误();
			}
			ensureBitmap(getWidth(), getHeight());
			return bitmap;
		}

		/**
		 * 新建或调整缓冲，复制使用 SRC 以保留透明像素，避免重复混合背景。
		 *
		 * @param width 新画面宽度，必须为正数。
		 * @param height 新画面高度，必须为正数。
		 */
		private void ensureBitmap(int width, int height) {
			if (destroyed || (bitmap != null && bitmap.getWidth() == width && bitmap.getHeight() == height)) {
				return;
			}

			Bitmap next = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			next.eraseColor(backgroundColor);
			Canvas nextCanvas = new Canvas(next);
			if (bitmap != null) {
				Paint copyPaint = new Paint();
				copyPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
				nextCanvas.drawBitmap(bitmap, 0, 0, copyPaint);
			}
			bitmap = next;
			canvas = nextCanvas;
			// 旧帧可能仍被显示列表引用，交由系统回收，不提前 recycle。
		}

		/**
		 * 断开软件画布与位图引用，显示管线仍持有的旧帧由系统安全释放。
		 */
		void releaseBitmap() {
			canvas = null;
			bitmap = null;
			invalidate();
		}

		@Override
		protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
			super.onSizeChanged(width, height, oldWidth, oldHeight);
			if (width > 0 && height > 0) {
				ensureBitmap(width, height);
			}
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			if (destroyed) {
				return false;
			}
			lastX = currX;
			lastY = currY;
			currX = (int) event.getX();
			currY = (int) event.getY();

			switch (event.getActionMasked()) {
				case MotionEvent.ACTION_DOWN:
					lastX = currX;
					lastY = currY;
					被按下(currX, currY);
					break;
				case MotionEvent.ACTION_UP:
					被弹起(currX, currY);
					break;
				case MotionEvent.ACTION_MOVE:
					触摸移动(lastX, lastY, currX, currY);
					break;
			}
			if (!destroyed) {
				gestureDetector.onTouchEvent(event);
			}
			return true;
		}

		@Override
		protected void onDraw(Canvas destination) {
			super.onDraw(destination);
			if (!destroyed && getWidth() > 0 && getHeight() > 0) {
				ensureBitmap(getWidth(), getHeight());
				destination.drawBitmap(bitmap, 0, 0, null);
			}
		}
	}
}

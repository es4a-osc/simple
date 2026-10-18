package simple.runtime.components.impl.android.util;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

/**
 * 以垂直方向测量、绘制和处理触摸的滑块。
 */
public class VerticalSeekBar extends SeekBar {

	private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener;

	public VerticalSeekBar(Context context) {
		super(context);
	}

	public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public VerticalSeekBar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(h, w, oldh, oldw);
	}

	protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(heightMeasureSpec, widthMeasureSpec);
		setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
	}

	protected void onDraw(Canvas c) {
		c.rotate(-90);
		c.translate(-getHeight(), 0);
		onSizeChanged(getWidth(), getHeight(), 0, 0);
		super.onDraw(c);
	}

	public boolean onTouchEvent(MotionEvent event) {
		if (!isEnabled()) {
			return false;
		}

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				setPressed(true);
				onStartTrackingTouch();
				break;
			case MotionEvent.ACTION_MOVE:
				attemptClaimDrag();
				setProgress(getMax() - (int)(getMax() * event.getY() / getHeight()));
				onProgressRefresh();
				break;
			case MotionEvent.ACTION_UP:
				int i = 0;
				i = getMax() - (int)(getMax() * event.getY() / getHeight());
				setProgress(i);
				onSizeChanged(getWidth(), getHeight(), 0, 0);

				onStopTrackingTouch();
				setPressed(false);
				break;
			case MotionEvent.ACTION_CANCEL:
				onStopTrackingTouch();
				setPressed(false);
		}
		return true;
	}

	public void setOnSeekBarChangeListener(SeekBar.OnSeekBarChangeListener l) {
		mOnSeekBarChangeListener = l;
	}

	/**
	 * 将当前滑块位置通知监听器。
	 */
	public void onProgressRefresh() {
		if (mOnSeekBarChangeListener != null) {
			mOnSeekBarChangeListener.onProgressChanged(this, getProgress(), true);
		}
	}

	/**
	 * 通知监听器开始拖动滑块。
	 */
	public void onStartTrackingTouch() {
		if (mOnSeekBarChangeListener != null) {
			mOnSeekBarChangeListener.onStartTrackingTouch(this);
		}
	}

	/**
	 * 通知监听器结束拖动滑块。
	 */
	public void onStopTrackingTouch() {
		if (mOnSeekBarChangeListener != null) {
			mOnSeekBarChangeListener.onStopTrackingTouch(this);
		}
	}

	/**
	 * 在滑块拖动期间请求父容器保留触摸事件。
	 */
	private void attemptClaimDrag() {
		if (getParent() != null) {
			getParent().requestDisallowInterceptTouchEvent(true);
		}
	}
}

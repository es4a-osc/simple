package simple.runtime.components.impl.android;

import simple.runtime.android.MainActivity;
import simple.runtime.components.组件容器;
import simple.runtime.components.视频播放器;
import simple.runtime.events.EventDispatcher;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.VideoView;

/**
 * 视频播放器组件实现。
 *
 * @author 小刀(siyu1840)
 * @author 树先生 xhwsd@qq.com
 */
public class 视频播放器Impl extends 视图组件 implements 视频播放器 {

	private VideoView videoView;
	private MediaController mediaController;

	public 视频播放器Impl(组件容器 container) {
		super(container);
	}

	// 视频播放器 实现

	@Override
	public void 播放完毕() {
		EventDispatcher.dispatchEvent(this, "播放完毕");
	}

	@Override
	public void 播放视频(String path) {
		videoView.setVideoPath(path);
		videoView.start();
	}

	@Override
	public void 暂停播放() {
		videoView.pause();
	}

	@Override
	public void 继续播放() {
		videoView.start();
	}

	@Override
	public void 停止播放() {
		videoView.stopPlayback();
	}

	@Override
	public int 取视频时长() {
		return videoView.getDuration();
	}

	@Override
	public int 取播放位置() {
		return videoView.getCurrentPosition();
	}

	@Override
	public void 置播放位置(int value) {
		videoView.seekTo(value);
	}

	@Override
	public boolean 取播放状态() {
		return videoView.isPlaying();
	}

	@Override
	public int 取缓冲进度() {
		return videoView.getBufferPercentage();
	}

	@Override
	public void 置控制条状态(boolean visiable) {
		if (visiable) {
			if (mediaController == null) {
				mediaController = new MediaController(MainActivity.getContext());
				mediaController.setAnchorView(videoView);
				videoView.setMediaController(mediaController);
			}
			mediaController.setVisibility(0);
		} else if (mediaController != null) {
			mediaController.setVisibility(4);
		}
	}

	// 视图组件 相关方法
	@Override
	protected View createView() {
		videoView = new CustomVideoView(MainActivity.getContext());
		videoView.setFocusable(true);
		videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				播放完毕();
			}
		});

		LinearLayout layout = new LinearLayout(MainActivity.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setHorizontalGravity(1);
		layout.setBackgroundColor(颜色_黑);
		LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT);
		layout.addView(videoView, param);
		layout.setFocusable(true);
		return layout;
	}

	/**
	 * VideoView
	 * https://developer.android.google.cn/reference/android/widget/VideoView
	 */
	protected class CustomVideoView extends VideoView {
		private int mVideoWidth;
		private int mVideoHeight;

		public CustomVideoView(Context context) {
			super(context);
		}

		public CustomVideoView(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		public CustomVideoView(Context context, AttributeSet attrs, int defStyle) {
			super(context, attrs, defStyle);
		}

		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			int width = getDefaultSize(mVideoWidth, widthMeasureSpec);
			int height = getDefaultSize(mVideoHeight, heightMeasureSpec);
			setMeasuredDimension(width, height);
		}
	}
}

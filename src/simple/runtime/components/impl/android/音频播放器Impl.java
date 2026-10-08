package simple.runtime.components.impl.android;

import simple.runtime.android.MainActivity;
import simple.runtime.components.音频播放器;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;

import android.media.MediaPlayer;
import android.content.res.AssetFileDescriptor;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;

/**
 * 音频播放器实现类
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class 音频播放器Impl extends 组件Impl implements 音频播放器 {

	private MediaPlayer mp;

	/**
	 * 创建新的音频播放器组件。
	 *
	 * @param container 容纳组件的容器（不可为 {@code null}，对于不可见的组件必须是窗口）
	 */
	public 音频播放器Impl(组件容器 container) {
		super(container);

		// MediaPlayer https://developer.android.google.cn/reference/android/media/MediaPlayer
		// MediaPlayer 解析文档 https://blog.csdn.net/shulianghan/article/details/38487967
		// 媒体播放器
		mp = new MediaPlayer();

		// 注册一个在异步操作过程中发生错误回调的监听器;
		mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
			/**
			 * 调用以指示错误。
			 *
			 * @param mp    错误所属的媒体播放器
			 * @param what  信息或警告的类型。
			 * @param extra 一个额外的代码，特定于信息。通常依赖于实现。
			 * @return 如果方法处理错误，则返回true;否则返回false。返回false或根本没有OnErrorListener将导致调用OnCompletionListener。
			 */
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				错误通知(what, extra);
				return true;
			}
		});

		// 注册一个当有信息或者警告出现就会回调的监听器;
		mp.setOnInfoListener(new MediaPlayer.OnInfoListener() {
			/**
			 * 如果方法处理信息，则为True，否则为false。返回false或根本没有OnInfoListener将导致信息被丢弃。
			 *
			 * @param mp    MediaPlayer的信息属于。
			 * @param what  信息或警告的类型。
			 * @param extra 一个额外的代码，特定于信息。通常依赖于实现。
			 * @return 如果方法处理信息，则为True，否则为false。返回false或根本没有OnInfoListener将导致信息被丢弃。
			 */
			@Override
			public boolean onInfo(MediaPlayer mp, int what, int extra) {
				信息通知(what, extra);
				return true;
			}
		});

		// 注册一个在网络缓冲数据流发生变化后回调的监听器;
		mp.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
			/**
			 * 被调用以更新缓冲通过渐进式HTTP下载接收的媒体流的状态。 接收的缓冲百分比表示已缓冲或播放了多少内容。
			 * 例如，当已播放一半内容时，缓冲更新为80％表示要播放的下一个30％的内容已被缓冲。
			 *
			 * @param mp      更新所属的MediaPlayer
			 * @param percent 到目前为止已缓冲或播放的内容的百分比（0-100）
			 */
			@Override
			public void onBufferingUpdate(MediaPlayer mp, int percent) {
				缓冲更新(percent);
			}
		});

		// 注册一个当媒体资源准备播放时回调的监听器;
		mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			/**
			 * 媒体源准备好播放时调用的回调的接口定义。
			 *
			 * @param mp 准备播放的MediaPlayer
			 */
			@Override
			public void onPrepared(MediaPlayer mp) {
				播放就绪();
			}
		});

		// 注册一个搜寻操作完成后回调的监听器;
		mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
			/**
			 * 调用以指示搜索操作的完成。
			 *
			 * @param mp 发出搜索操作的媒体播放器
			 */
			@Override
			public void onSeekComplete(MediaPlayer mp) {
				位置改变();
			}
		});

		// 注册一个在媒体资源播放完毕之后回调的播放事件;
		mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			/**
			 * 在播放期间到达媒体源结尾时调用。
			 *
			 * @param mp 到达文件末尾的MediaPlayer
			 */
			@Override
			public void onCompletion(MediaPlayer mp) {
				播放结束();
			}
		});
	}

	/* 音频播放器 实现 */

	@Override
	public void 错误通知(int what, int extra) {
		EventDispatcher.dispatchEvent(this, "错误通知", what, extra);
	}

	@Override
	public void 信息通知(int what, int extra) {
		EventDispatcher.dispatchEvent(this, "信息通知", what, extra);
	}

	@Override
	public void 缓冲更新(int percent) {
		EventDispatcher.dispatchEvent(this, "缓冲更新", percent);
	}

	@Override
	public void 播放就绪() {
		EventDispatcher.dispatchEvent(this, "播放就绪");
	}

	@Override
	public void 位置改变() {
		EventDispatcher.dispatchEvent(this, "位置改变");
	}

	@Override
	public void 播放结束() {
		EventDispatcher.dispatchEvent(this, "播放结束");
	}

	@Override
	public int 时长() {
		return mp.getDuration();
	}

	@Override
	public int 位置() {
		return mp.getCurrentPosition();
	}

	@Override
	public void 位置(int value) {
		try {
			mp.seekTo(value); // 寻求指定的时间位置。
		} catch (IllegalStateException e) {
		}
	}

	@Override
	public boolean 循环() {
		/*
		方法名称 : public boolean isLooping ();
		方法解析 : 检查 MediaPlayer 是否在循环播放, 如果是 返回 true, 不是的话 返回 false;
		方法的有效状态和无效状态 : 在任何状态都有效, 包括在 Error 状态的时候;
		*/
		return mp.isLooping();
	}

	@Override
	public void 循环(boolean looping) {
		/*
		方法名称 : public void setLooping (boolean looping); 方法解析 : 设置播放器是否循环播放;
		方法的有效状态和无效状态 : 
		-- 有效状态 : Idle, Initialized, Prepared, Started, Paused, Stopped, PlaybackCompleted, 在以上状态调用该方法不会改变当前状态;
		-- 无效状态 : Error 状态, 在 Error 状态调用该方法, 会进入 Error 状态中;
		*/
		mp.setLooping(looping); // 将播放器设置为循环或非循环。
	}

	/* 音频播放器 方法实现 */

	@Override
	public boolean 打开(String mFile) {

		/*
		方法名称 : public void reset ();
		方法解析 : 重置 MediaPlayer 到 Idle 状态, 此时还没有设置数据源, 如果想要播放媒体资源, 需要设置数据源 和 调用 prepare()方法;
		方法的有效状态和无效状态 : 在任何状态下 调用 release() 方法都可以;
		Error 状态转换 : 当 MediaPlayer 出现一些错误如 格式错误, 分辨率过高等原因, 播放器引擎 就会调用 OnErrorListener.onError()方法;
		-- 进入 Error 状态 : 客户端调用 OnErrorListener.onError() 方法, 会进入 Error 状态;
		-- 离开 Error 状态 : 如果想要使用进入 Error 状态的 MediaPlayer, 可以使用 reset() 方法进入 Idle 状态;
		*/
		mp.reset();

		try {
			if (mFile.startsWith("/") && new File(mFile).exists()) {
				mp.setDataSource(mFile);
			} else if (mFile.indexOf("://") != -1) {
				mp.setDataSource(mFile);
			} else {
				AssetFileDescriptor aFD = MainActivity.getContext().getAssets().openFd(mFile);
				FileDescriptor fileDescriptor = aFD.getFileDescriptor();
				mp.setDataSource(fileDescriptor, aFD.getStartOffset(), aFD.getLength());
				aFD.close();
			}
		} catch (IllegalArgumentException e) {
			return false;
		} catch (SecurityException e) {
			return false;
		} catch (IllegalStateException e) {
			return false;
		} catch (IOException e) {
			return false;
		}

		try {
			/*
			方法名称 : public void prepareAsync (); 方法作用 : 异步的方法, 让播放器处于准备状态;
			-- 调用时机 : 设置完数据源 和 播放载体 之后调用该方法;
			-- 适用情况 : 对于 流媒体 来说, 调用该方法立即返回, 要比 阻塞等待缓冲足够的数据在播放比较好;
			方法的有效状态和无效状态 : 只有在 Initialized 和 Stopped 状态中会
			-- 有效状态 : Initialized, Stopped, 在以上状态调用该方法不会改变 MediaPlayer 状态;
			-- 无效状态 : Idle, Prepared, Started, Paused, PlaybackCompleted, Error 状态, 在 Error 状态调用该方法, 会进入 Error 状态中;
			*/
			mp.prepareAsync();
			return true;
		} catch (IllegalStateException e) {
			return false;
		}
	}

	@Override
	public boolean 播放() {
		try {
			mp.start();
			return true;
		} catch (IllegalStateException e) {
			return false;
		}
	}

	@Override
	public boolean 暂停() {
		/*
		方法名称 : public void pause ();
		方法解析 : 暂停播放, 如果想要恢复播放的话, 调用 start() 方法;
		方法的有效状态和无效状态 : 只有在 Started 和 Paused 状态有效, Started 状态调用该方法进入 Paused 状态, Paused 状态调用该方法不起作用;
		-- 有效状态 : Started, Paused, 在以上状态调用该方法不会改变 MediaPlayer 状态;
		-- 无效状态 : Error , Idle, Initialized, Prepared, Stopped, PlaybackCompleted 状态, 在 Error 状态调用该方法, 会进入 Error 状态, 抛出 IllegalStateException;
		*/
		try {
			mp.pause();
			return true;
		} catch (IllegalStateException e) {
			return false;
		}

	}

	@Override
	public boolean 停止() {
		try {
			mp.stop();
			return true;
		} catch (IllegalStateException e) {
			return false;
		}
	}

	@Override
	public boolean 是否正在播放() {
		/*
		方法名称 : public boolean isPlaying ();
		方法解析 : 检查 MediaPlayer 是否正在播放;
		方法的有效状态和无效状态 : 除 Error 以外的所有状态, 都可以查看 MediaPlayer 是否在播放;
		-- 有效状态 : Idle, Initialized, Prepared, Started, Paused, Stopped, PlaybackCompleted, 在以上状态调用该方法不会改变 MediaPlayer 状态;
		-- 无效状态 : Error 状态, 在 Error 状态调用该方法, 会进入 Error 状态, 抛出 IllegalStateException;
		*/
		return mp.isPlaying();
	}

	@Override
	public void 置音量(float Right, float Left) {
		mp.setVolume(Right, Left);
	}
}

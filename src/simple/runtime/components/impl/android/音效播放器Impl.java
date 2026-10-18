package simple.runtime.components.impl.android;

import android.annotation.TargetApi;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import simple.runtime.android.MainActivity;
import simple.runtime.components.组件容器;
import simple.runtime.components.音效播放器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;

/**
 * 使用 SoundPool 预加载短音效，协调异步载入回调与播放器释放。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class 音效播放器Impl extends 组件Impl implements 音效播放器 {

	private final MainActivity activity;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private final Object lifecycle = new Object();
	private final MainActivity.OnDestroyListener destroyListener;

	// 只跟踪尚可使用的音效，不累计短播放流编号，避免高频播放造成集合增长。
	private final Map<Integer, Boolean> sounds = new HashMap<Integer, Boolean>();
	private SoundPool pool;
	private boolean destroyed;

	/**
	 * 创建组件并登记 Activity 销毁回调，不提前占用音频资源。
	 *
	 * @param container 容纳本组件的窗口，不得为空。
	 */
	public 音效播放器Impl(组件容器 container) {
		super(container);
		activity = MainActivity.getContext();
		destroyListener = new MainActivity.OnDestroyListener() {

			@Override
			public void onDestroy() {
				// Activity 正在遍历监听器，此时不能修改监听器集合。
				dispose(false);
			}
		};
		activity.addOnDestroyListener(destroyListener);
	}

	/* 组件事件 */

	/**
	 * 派发主线程载入成功事件。
	 *
	 * @param soundId 已经载入成功的音效编号。
	 */
	@Override
	public void 载入完毕(int soundId) {
		EventDispatcher.dispatchEvent(this, "载入完毕", soundId);
	}

	/**
	 * 派发主线程异步载入失败事件。
	 *
	 * @param soundId 本次载入的音效编号。
	 * @param status 系统返回的非零失败状态。
	 */
	@Override
	public void 载入失败(int soundId, int status) {
		EventDispatcher.dispatchEvent(this, "载入失败", soundId, status);
	}

	/* 组件方法 */

	/**
	 * 重建音效池，旧音效及播放流全部失效。
	 *
	 * @param maxStreams 最大并发播放数，必须大于 0。
	 */
	@Override
	public void 初始化播放器(int maxStreams) {
		if (maxStreams < 1) {
			throw new IllegalArgumentException("最大并发播放数必须大于 0");
		}

		synchronized (lifecycle) {
			if (destroyed) {
				throw new IllegalStateException("音效播放器组件已销毁");
			}

			releasePool();
			pool = createPool(maxStreams);
			pool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {

				@Override
				public void onLoadComplete(final SoundPool source, final int soundId, final int status) {
					// 统一回到主线程；池身份和音效表共同拒绝释放、重建或卸载后的旧回调。
					handler.post(new Runnable() {

						@Override
						public void run() {
							dispatchLoad(source, soundId, status);
						}
					});
				}
			});
		}
	}

	/**
	 * 打开资产或绝对路径并提交异步解码，文件描述符在提交后立即关闭。
	 *
	 * @param source 资产文件名或可访问的绝对文件路径。
	 * @return 正音效编号；同步打开失败或系统拒绝载入时为 -1。
	 */
	@Override
	public int 载入音效(String source) {
		synchronized (lifecycle) {
			SoundPool current = requirePool();
			if (source == null || source.length() == 0) {
				return -1;
			}

			int soundId = 0;

			try {
				if (source.startsWith("/")) {
					soundId = current.load(source, 1);
				} else {
					AssetFileDescriptor descriptor = activity.getAssets().openFd(source);
					try {
						soundId = current.load(descriptor, 1);
					} finally {
						descriptor.close();
					}
				}

				if (soundId <= 0) {
					return -1;
				}

				sounds.put(soundId, false);
				return soundId;
			} catch (IOException | SecurityException error) {
				// 描述符关闭失败时也回收已经提交的音效，避免留下不可追踪的编号。
				if (soundId > 0) {
					current.unload(soundId);
				}
				return -1;
			}
		}
	}

	/**
	 * 单次播放已经就绪的音效。
	 *
	 * @param soundId 当前播放器返回的音效编号。
	 * @return 正播放编号；未就绪或播放失败时为 0。
	 */
	@Override
	public int 播放音效(int soundId) {
		return 播放音效(soundId, 0);
	}

	/**
	 * 使用媒体音量、相同左右声道和正常速率播放音效。
	 *
	 * @param soundId 已经载入成功的音效编号。
	 * @param loopCount 额外重复次数，-1 持续循环，0 播放一次。
	 * @return 正播放编号；未就绪、无效编号或系统拒绝播放时为 0。
	 */
	@Override
	public int 播放音效(int soundId, int loopCount) {
		if (loopCount < -1) {
			throw new IllegalArgumentException("音效循环次数不能小于 -1");
		}

		synchronized (lifecycle) {
			SoundPool current = requirePool();
			if (!Boolean.TRUE.equals(sounds.get(soundId))) {
				return 0;
			}

			return current.play(soundId, 1.0f, 1.0f, 1, loopCount, 1.0f);
		}
	}

	/**
	 * 暂停一个播放流，不改变音效载入状态。
	 *
	 * @param streamId 播放编号，无效或已结束的编号由系统忽略。
	 */
	@Override
	public void 暂停播放(int streamId) {
		synchronized (lifecycle) {
			requirePool().pause(streamId);
		}
	}

	/**
	 * 继续一个已暂停的播放流。
	 *
	 * @param streamId 播放编号，无效或已结束的编号由系统忽略。
	 */
	@Override
	public void 继续播放(int streamId) {
		synchronized (lifecycle) {
			requirePool().resume(streamId);
		}
	}

	/**
	 * 停止一个播放流，再次播放需重新取得播放编号。
	 *
	 * @param streamId 播放编号，无效或已结束的编号由系统忽略。
	 */
	@Override
	public void 停止播放(int streamId) {
		synchronized (lifecycle) {
			requirePool().stop(streamId);
		}
	}

	/**
	 * 卸载音效并取消它尚未派发的载入结果。
	 *
	 * @param soundId 音效编号，不是播放编号。
	 * @return 系统确认卸载返回真，否则返回假。
	 */
	@Override
	public boolean 卸载音效(int soundId) {
		synchronized (lifecycle) {
			SoundPool current = requirePool();
			if (!sounds.containsKey(soundId) || !current.unload(soundId)) {
				return false;
			}

			sounds.remove(soundId);
			return true;
		}
	}

	/**
	 * 释放音频资源及回调，保留组件以便稍后重新初始化。
	 */
	@Override
	public void 释放播放器() {
		synchronized (lifecycle) {
			releasePool();
		}
	}

	/* 生命周期 */

	/**
	 * 释放资源、移除 Activity 监听器并注销组件事件，重复调用无副作用。
	 */
	@Override
	public void 销毁() {
		dispose(true);
	}

	/**
	 * 在生命周期锁内校验播放器状态。
	 *
	 * @return 当前仍可使用的音效池。
	 */
	private SoundPool requirePool() {
		if (destroyed || pool == null) {
			throw new IllegalStateException(destroyed ? "音效播放器组件已销毁" : "请先初始化音效播放器");
		}
		return pool;
	}

	/**
	 * 在生命周期锁内断开旧池及其回调，再释放原生资源。
	 */
	private void releasePool() {
		SoundPool previous = pool;
		pool = null;
		sounds.clear();
		handler.removeCallbacksAndMessages(null);

		if (previous != null) {
			previous.setOnLoadCompleteListener(null);
			previous.release();
		}
	}

	/**
	 * 对当前池的载入结果更新状态并派发主线程事件。
	 *
	 * @param source 产生回调的音效池。
	 * @param soundId 载入音效编号。
	 * @param status 解码结果，0 为成功。
	 */
	private void dispatchLoad(SoundPool source, int soundId, int status) {
		synchronized (lifecycle) {
			if (destroyed || source != pool || !sounds.containsKey(soundId)) {
				return;
			}

			// 与释放保持同一状态边界；事件处理器可重入调用本组件的方法。
			if (status == 0) {
				sounds.put(soundId, true);
				载入完毕(soundId);
			} else {
				sounds.remove(soundId);
				载入失败(soundId, status);
			}
		}
	}

	/**
	 * 销毁时释放音效池；Activity 遍历监听器时不修改该监听器列表。
	 *
	 * @param removeListener 是否主动移除 Activity 监听器。
	 */
	private void dispose(boolean removeListener) {
		synchronized (lifecycle) {
			if (destroyed) {
				return;
			}

			destroyed = true;
			releasePool();
		}

		if (removeListener) {
			activity.removeOnDestroyListener(destroyListener);
		}
		super.销毁();
	}

	/* 系统版本兼容 */

	/**
	 * 在支持构建器的系统使用媒体音效属性，旧系统继续使用媒体音量通道。
	 *
	 * @param maxStreams 最大并发播放数。
	 * @return 新音效池。
	 */
	@SuppressWarnings("deprecation")
	private static SoundPool createPool(int maxStreams) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			return Api21.create(maxStreams);
		}
		return new SoundPool(maxStreams, AudioManager.STREAM_MUSIC, 0);
	}

	/**
	 * 隔离 API 21 新类型，避免旧系统提前解析 AudioAttributes。
	 */
	private static final class Api21 {

		private Api21() {
		}

		/**
		 * 创建支持音频属性的音效池。
		 *
		 * @param maxStreams 最大并发播放数。
		 * @return 新音效池。
		 */
		@TargetApi(Build.VERSION_CODES.LOLLIPOP)
		static SoundPool create(int maxStreams) {
			AudioAttributes attributes = new AudioAttributes.Builder()
				.setUsage(AudioAttributes.USAGE_GAME)
				.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
				.build();
			return new SoundPool.Builder().setMaxStreams(maxStreams).setAudioAttributes(attributes).build();
		}
	}
}

package simple.runtime.components.impl.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import simple.runtime.android.MainActivity;
import simple.runtime.components.组件容器;
import simple.runtime.components.语音处理器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 协调系统识别结果、朗读引擎异步初始化和宿主销毁时的资源释放。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class 语音处理器Impl extends 组件Impl implements 语音处理器,
		MainActivity.OnActivityResultListener, MainActivity.OnDestroyListener {

	// 宿主直接继承 Activity，使用独立的正整数区间，不受 Fragment 的 16 位请求号约束。
	private static final AtomicInteger NEXT_REQUEST = new AtomicInteger(0x50000000);
	private final MainActivity activity;
	private final Handler handler;
	private TextToSpeech speech;
	private long speechGeneration;
	private boolean speechReady;
	private int recognitionRequest;
	private boolean listenersRegistered;
	private boolean destroyed;
	private String pendingText;
	private String lastError = "";

	/**
	 * 保存宿主并延后登记监听，避免在宿主事件遍历期间修改监听集合。
	 *
	 * @param container 所属组件容器
	 */
	public 语音处理器Impl(组件容器 container) {
		super(container);
		activity = MainActivity.getContext();
		handler = new Handler(Looper.getMainLooper());
		handler.post(this::registerListeners);
	}

	/* 组件事件 */

	@Override
	public void 语音识别完毕(String result) {
		EventDispatcher.dispatchEvent(this, "语音识别完毕", result);
	}

	@Override
	public void 语音识别取消() {
		EventDispatcher.dispatchEvent(this, "语音识别取消");
	}

	@Override
	public void 语音朗读就绪() {
		EventDispatcher.dispatchEvent(this, "语音朗读就绪");
	}

	@Override
	public void 操作失败(String operation, String message) {
		EventDispatcher.dispatchEvent(this, "操作失败", operation, message);
	}

	/* 组件属性 */

	@Override
	public boolean 正在识别() {
		requireMainThread();
		return recognitionRequest != 0;
	}

	@Override
	public boolean 朗读就绪() {
		requireMainThread();
		return speechReady;
	}

	@Override
	public String 错误信息() {
		requireMainThread();
		return lastError;
	}

	/* 组件方法 */

	@Override
	public boolean 是否支持语音识别() {
		requireMainThread();
		if (destroyed || hostDestroyed()) {
			return false;
		}
		try {
			return !activity.getPackageManager().queryIntentActivities(
				new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0).isEmpty();
		} catch (RuntimeException failure) {
			return false;
		}
	}

	@Override
	public boolean 语音识别() {
		requireMainThread();
		if (!canOperate()) {
			return false;
		}
		if (!activity.isInForeground()) {
			return reject("应用未在前台，不能打开语音识别界面。");
		}
		if (recognitionRequest != 0) {
			return reject("已有语音识别正在进行。");
		}

		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "请开始说话");
		intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
		int request = NEXT_REQUEST.getAndIncrement();
		if (request <= 0) {
			return reject("语音识别请求标识已耗尽。");
		}
		recognitionRequest = request;
		try {
			// API 30 的平台 Activity 此接口未标记弃用，沿用宿主现有的结果分发约定。
			activity.startActivityForResult(intent, request);
			lastError = "";
			return true;
		} catch (ActivityNotFoundException failure) {
			recognitionRequest = 0;
			return reject("设备没有可用的语音识别应用。");
		} catch (RuntimeException failure) {
			recognitionRequest = 0;
			return reject(errorMessage(failure));
		}
	}

	@Override
	public boolean 初始化语音朗读() {
		requireMainThread();
		if (!canOperate()) {
			return false;
		}
		if (speech != null) {
			lastError = "";
			return true;
		}

		final long generation = ++speechGeneration;
		try {
			speech = new TextToSpeech(activity, status -> {
				// 引擎可能在构造期间或后台线程通知；排队后字段已赋值，并能校验释放与重建。
				handler.post(() -> finishInitialization(generation, status));
			});
			lastError = "";
			return true;
		} catch (RuntimeException failure) {
			speechGeneration++;
			speech = null;
			pendingText = null;
			return reject(errorMessage(failure));
		}
	}

	@Override
	public boolean 语音朗读(String text) {
		requireMainThread();
		if (!canOperate()) {
			return false;
		}
		if (text == null || text.trim().length() == 0) {
			return reject("朗读文本不能为空。");
		}
		if (Build.VERSION.SDK_INT >= 18 && text.length() > Api18.maxInputLength()) {
			return reject("朗读文本超过系统允许的输入长度。");
		}
		if (!speechReady) {
			// 与 QUEUE_FLUSH 一致，初始化期间只保留最后一次请求。
			pendingText = text;
			return 初始化语音朗读();
		}
		return submitSpeech(text);
	}

	@Override
	public void 释放语音朗读() {
		requireMainThread();
		releaseSpeech();
	}

	@Override
	public void 销毁() {
		requireMainThread();
		if (destroyed) {
			return;
		}
		destroyed = true;
		recognitionRequest = 0;
		handler.removeCallbacksAndMessages(null);
		releaseSpeech();

		// 结果与销毁事件均可能正在遍历宿主集合，移除操作统一延后。
		handler.post(() -> {
			if (listenersRegistered) {
				activity.removeOnActivityResultListener(this);
				activity.removeOnDestroyListener(this);
				listenersRegistered = false;
			}
		});
		super.销毁();
	}

	/* 内部接口与平台回调 */

	@Override
	public void onDestroy() {
		销毁();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (destroyed || recognitionRequest == 0 || requestCode != recognitionRequest) {
			return;
		}
		recognitionRequest = 0;

		// 先消费请求再排队通知，事件中再次识别或销毁不会修改宿主正在遍历的监听集合。
		handler.post(() -> {
			if (destroyed) {
				return;
			}
			if (hostDestroyed()) {
				销毁();
				return;
			}
			if (resultCode == Activity.RESULT_CANCELED) {
				lastError = "";
				语音识别取消();
				return;
			}
			if (resultCode != Activity.RESULT_OK || data == null) {
				fail("语音识别", "语音识别未返回有效结果。");
				return;
			}
			ArrayList<String> results;
			try {
				results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			} catch (RuntimeException failure) {
				fail("语音识别", errorMessage(failure));
				return;
			}
			// 列表是同一段语音的候选文本，不能拼成多段识别内容。
			if (results == null || results.isEmpty() || results.get(0) == null
					|| results.get(0).trim().length() == 0) {
				fail("语音识别", "语音识别未返回有效文本。");
				return;
			}
			lastError = "";
			语音识别完毕(results.get(0));
		});
	}

	/**
	 * 在宿主事件遍历结束后登记结果与销毁监听。
	 */
	private void registerListeners() {
		if (destroyed || listenersRegistered) {
			return;
		}
		if (hostDestroyed()) {
			销毁();
			return;
		}
		activity.addOnActivityResultListener(this);
		activity.addOnDestroyListener(this);
		listenersRegistered = true;
	}

	/**
	 * 校验初始化回调归属，准备默认语言并提交暂存的最后一次朗读请求。
	 *
	 * @param generation 引擎代次
	 * @param status 系统初始化结果
	 */
	private void finishInitialization(long generation, int status) {
		if (destroyed || generation != speechGeneration || speech == null) {
			return;
		}
		if (hostDestroyed()) {
			销毁();
			return;
		}

		String failure = null;
		try {
			if (status != TextToSpeech.SUCCESS) {
				failure = "语音朗读引擎初始化失败，请检查已安装的引擎。";
			} else {
				int language = speech.setLanguage(Locale.getDefault());
				if (language < TextToSpeech.LANG_AVAILABLE) {
					failure = language == TextToSpeech.LANG_MISSING_DATA
						? "缺少系统默认语言的朗读数据。" : "朗读引擎不支持系统默认语言。";
				}
			}
		} catch (RuntimeException error) {
			failure = errorMessage(error);
		}
		if (failure != null) {
			releaseSpeech();
			fail("初始化语音朗读", failure);
			return;
		}

		speechReady = true;
		String text = pendingText;
		pendingText = null;
		if (text != null && !submitSpeech(text)) {
			fail("语音朗读", lastError);
			// 失败事件允许释放或重建引擎，不能向新引擎派发旧代次的就绪事件。
			if (destroyed || generation != speechGeneration || speech == null) {
				return;
			}
		}
		语音朗读就绪();
	}

	/**
	 * 按设备版本提交朗读，检查引擎是否受理，而非推断实际发声成功。
	 *
	 * @param text 已校验的朗读文本
	 * @return 系统接受朗读请求时为真
	 */
	private boolean submitSpeech(String text) {
		try {
			int result = Build.VERSION.SDK_INT >= 21
				? Api21.speak(speech, text) : speakLegacy(speech, text);
			if (result != TextToSpeech.SUCCESS) {
				return reject("朗读引擎未受理请求。");
			}
			lastError = "";
			return true;
		} catch (RuntimeException failure) {
			return reject(errorMessage(failure));
		}
	}

	/**
	 * 先使旧初始化回调失效，再清除请求并尽力释放引擎。
	 */
	private void releaseSpeech() {
		speechGeneration++;
		TextToSpeech previous = speech;
		speech = null;
		speechReady = false;
		pendingText = null;
		if (previous == null) {
			return;
		}

		try {
			previous.stop();
		} catch (RuntimeException failure) {
			Log.w("语音处理器", "停止朗读失败。", failure);
		} finally {
			try {
				previous.shutdown();
			} catch (RuntimeException failure) {
				Log.w("语音处理器", "释放朗读引擎失败。", failure);
			}
		}
	}

	/**
	 * 拒绝销毁后的请求及正在关闭的宿主，避免隐式重建引擎。
	 *
	 * @return 可以继续执行请求时为真
	 */
	private boolean canOperate() {
		if (destroyed) {
			return reject("语音处理器已经销毁。");
		}
		if (hostDestroyed()) {
			销毁();
			return reject("宿主已经销毁。");
		}
		if (activity.isFinishing()) {
			return reject("宿主正在关闭。");
		}
		return true;
	}

	/**
	 * 在支持宿主销毁查询的系统上检查实际状态，低版本依靠已注册的销毁监听。
	 *
	 * @return 宿主已经销毁时为真
	 */
	private boolean hostDestroyed() {
		return Build.VERSION.SDK_INT >= 17 && activity.isDestroyed();
	}

	/**
	 * 保存同步拒绝原因，不派发异步失败事件。
	 *
	 * @param message 拒绝原因
	 * @return 固定返回假
	 */
	private boolean reject(String message) {
		lastError = message;
		return false;
	}

	/**
	 * 保存已受理操作的失败原因并通知调用者。
	 *
	 * @param operation 操作名称
	 * @param message 失败原因
	 */
	private void fail(String operation, String message) {
		lastError = message;
		操作失败(operation, message);
	}

	/**
	 * 为没有说明的系统异常补充可读文本。
	 *
	 * @param failure 系统调用异常
	 * @return 异常说明
	 */
	private static String errorMessage(RuntimeException failure) {
		String message = failure.getMessage();
		return message == null || message.length() == 0 ? failure.getClass().getSimpleName() : message;
	}

	/**
	 * 拒绝后台线程直接修改组件状态。
	 *
	 * @throws IllegalStateException 当前线程不是主线程
	 */
	private static void requireMainThread() {
		if (Looper.myLooper() != Looper.getMainLooper()) {
			throw new IllegalStateException("语音处理器必须在主线程调用。");
		}
	}

	/**
	 * API 21 以下没有带 Bundle 和请求标识的朗读重载，继续调用该版本可用的旧接口。
	 *
	 * @param speech 已就绪的朗读引擎
	 * @param text 朗读文本
	 * @return 引擎受理结果
	 */
	@SuppressWarnings("deprecation")
	private static int speakLegacy(TextToSpeech speech, String text) {
		return speech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
	}

	/**
	 * 隔离 API 18 起提供的朗读输入长度查询。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	@TargetApi(18)
	private static final class Api18 {

		/**
		 * 返回系统允许的单次朗读文本长度。
		 *
		 * @return 最大字符数
		 */
		static int maxInputLength() {
			return TextToSpeech.getMaxSpeechInputLength();
		}
	}

	/**
	 * 隔离 API 21 起提供的新朗读重载，低版本不会执行其方法体。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	@TargetApi(21)
	private static final class Api21 {

		/**
		 * 使用 Bundle 重载提交替换当前队列的朗读请求。
		 *
		 * @param speech 已就绪的朗读引擎
		 * @param text 朗读文本
		 * @return 引擎受理结果
		 */
		static int speak(TextToSpeech speech, String text) {
			return speech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "simple-speech");
		}
	}
}

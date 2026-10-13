package simple.runtime.components.impl.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;

import simple.runtime.android.MainActivity;
import simple.runtime.components.组件容器;
import simple.runtime.components.语音处理器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 系统语音识别和朗读实现，协调朗读引擎的异步初始化及释放。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class 语音处理器Impl extends 组件Impl implements 语音处理器, MainActivity.OnActivityResultListener {

	private static final AtomicInteger NEXT_REQUEST = new AtomicInteger(0x6000);
	private final int recognitionRequest = NEXT_REQUEST.getAndIncrement();
	private final MainActivity activity;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private TextToSpeech speech;
	private int speechGeneration;
	private boolean speechReady;
	private boolean recognitionPending;
	private boolean destroyed;
	private String pendingText;

	public 语音处理器Impl(组件容器 container) {
		super(container);

		if (recognitionRequest > 0x6fff) {
			throw new IllegalStateException("语音处理器请求标识已耗尽");
		}

		activity = MainActivity.getContext();

		activity.addOnActivityResultListener(this);
	}

	/* 组件事件 */

	@Override
	public void 语音识别完毕(String result) {
		EventDispatcher.dispatchEvent(this, "语音识别完毕", result);
	}

	/* 组件方法 */

	@Override
	public boolean 是否已安装语音引擎() {
		// 保留原函数含义：检查系统是否有可接收语音识别请求的应用。
		return !activity.getPackageManager().queryIntentActivities(
				new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0).isEmpty();
	}

	@Override
	public void 语音识别() {
		if (destroyed || recognitionPending) {
			return;
		}

		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "请开始说话");

		// 防止重复启动；取消或异常后解除等待，以便下一次识别。
		recognitionPending = true;

		try {
			activity.startActivityForResult(intent, recognitionRequest);
		} catch (RuntimeException error) {
			recognitionPending = false;
			throw error;
		}
	}

	@Override
	public void 初始化语音朗读() {
		if (destroyed || speech != null) {
			return;
		}

		final int generation = ++speechGeneration;
		speech = new TextToSpeech(activity, new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(final int status) {
				// 排队处理，确保字段已赋值；释放后到达的旧初始化回调不得影响新引擎。
				handler.post(new Runnable() {
					@Override
					public void run() {
						if (destroyed || generation != speechGeneration || speech == null) {
							return;
						}

						if (status != TextToSpeech.SUCCESS) {
							释放语音朗读();
							return;
						}

						int language = speech.setLanguage(Locale.getDefault());
						speechReady = language != TextToSpeech.LANG_MISSING_DATA
								&& language != TextToSpeech.LANG_NOT_SUPPORTED;
						if (!speechReady) {
							释放语音朗读();
						} else if (pendingText != null) {
							String text = pendingText;
							pendingText = null;
							语音朗读(text);
						}
					}
				});
			}
		});
	}

	@Override
	public void 语音朗读(String text) {
		if (destroyed) {
			return;
		}

		if (!speechReady) {
			// 与 QUEUE_FLUSH 一致，只保留初始化期间最后一次朗读请求。
			pendingText = text;
			初始化语音朗读();
			return;
		}

		speech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
	}

	@Override
	public void 释放语音朗读() {
		// 先使旧回调失效、清空引用，再调用引擎释放，后续请求可创建新引擎。
		speechGeneration++;
		TextToSpeech previous = speech;
		speech = null;
		speechReady = false;
		pendingText = null;

		if (previous != null) {
			try {
				previous.stop();
			} finally {
				previous.shutdown();
			}
		}
	}

	@Override
	public void 销毁() {
		destroyed = true;
		handler.removeCallbacksAndMessages(null);

		recognitionPending = false;

		activity.removeOnActivityResultListener(this);

		释放语音朗读();

		super.销毁();
	}

	/* 内部接口与平台回调 */

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (destroyed || !recognitionPending || requestCode != recognitionRequest) {
			return;
		}

		recognitionPending = false;
		if (resultCode != Activity.RESULT_OK || data == null) {
			return;
		}

		ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
		if (results == null) {
			return;
		}

		StringBuilder text = new StringBuilder();
		for (String result : results) {
			if (result == null) {
				continue;
			}

			if (text.length() > 0) {
				text.append('\n');
			}

			text.append(result);
		}

		语音识别完毕(text.toString());
	}
}

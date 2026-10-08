package simple.runtime.components.impl.android;

import simple.runtime.components.网页浏览框;
import simple.runtime.components.组件容器;
import simple.runtime.android.MainActivity;
import simple.runtime.events.EventDispatcher;
import simple.runtime.parameters.BooleanReferenceParameter;
import simple.runtime.parameters.IntegerReferenceParameter;
import simple.runtime.parameters.StringReferenceParameter;

import android.graphics.Bitmap;
import android.view.View;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.os.Build;

/**
 * Android实现Simple的网页浏览框组件。
 *
 * @author 树先生 xhwd@qq.com
 */
public class 网页浏览框Impl extends 视图组件 implements 网页浏览框 {

	private GestureDetector gestureDetector;

	public 网页浏览框Impl(组件容器 container) {
		super(container);
	}

	@Override
	protected View createView() {
		// 手势识别器
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
				return false;
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
				return false;
			}

			@Override
			public boolean onSingleTapConfirmed(MotionEvent e) {
				触摸手势(触摸_单击);
				return false;
			}

			@Override
			public boolean onDoubleTap(MotionEvent e) {
				触摸手势(触摸_双击);
				return false;
			}
		});

		// WebView https://developer.android.google.cn/reference/android/webkit/WebView
		// WebView全面解析 https://www.jianshu.com/p/3e0136c9e748
		WebView view = new WebView(MainActivity.getContext()) {
			/*
			 * 调度触摸事件
			 */
			@Override
			public boolean dispatchTouchEvent(MotionEvent ev) {
				gestureDetector.onTouchEvent(ev);
				return super.dispatchTouchEvent(ev);
			}
		};

		// WebViewClient https://developer.android.google.cn/reference/android/webkit/WebViewClient
		view.setWebViewClient(new WebViewClient() {
			/*
			 * 即将跳转地址
			 * 返回true，代表开发者已经对url进行了处理，WebView就不会再对这个url进行加载了。
			 * 弃用于API24：WebViewClient.shouldOverrideUrlLoading(WebView, String)
			 */
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				// API24兼容处理
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
					BooleanReferenceParameter shield = new BooleanReferenceParameter(false);
					即将跳转(url, shield);
					if (shield.get() == false) {
						view.loadUrl(url);
					}
				}
				return true;
			}

			/*
			 * 即将跳转地址
			 * 返回true，代表开发者已经对url进行了处理，WebView就不会再对这个url进行加载了。
			 * 添加于API24：WebViewClient.shouldOverrideUrlLoading(WebView, WebResourceRequest)
			 */
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
				// API24兼容处理
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
					String url = request.getUrl().toString();
					BooleanReferenceParameter shield = new BooleanReferenceParameter(false);
					即将跳转(url, shield);
					if (shield.get() == false) {
						view.loadUrl(url);
					}
				}
				return true;
			}

			/*
			 * 页面载入开始
			 */
			@Override
			public void onPageStarted(WebView view, String url, Bitmap facicon) {
				super.onPageStarted(view, url, facicon);
				开始载入(url);
			}

			/*
			 * 页面载入完成
			 */
			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				载入完毕(url);
			}

			/*
			 * 收到错误
			 * 弃用于API23：WebViewClient.onReceivedError(WebView, errorCode, description, failingUrl)
			 */
			@Override
			@SuppressWarnings("deprecation")
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				super.onReceivedError(view, errorCode, description, failingUrl);

				// API23兼容处理
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
					载入失败(errorCode, failingUrl);
				}
			}

			/*
			 * 收到错误
			 * 添加于API23：WebViewClient.onReceivedError(WebView, WebResourceRequest, WebResourceError)
			 */
			@Override
			public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
				super.onReceivedError(view, request, error);

				// API23兼容处理
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					载入失败(error.getErrorCode(), request.getUrl().toString());
				}
			}
		});

		// WebChromeClient https://developer.android.google.cn/reference/android/webkit/WebChromeClient
		view.setWebChromeClient(new WebChromeClient() {
			/*
			 * 页面载入进度
			 */
			@Override
			public void onProgressChanged(WebView view, int newProgress) {
				super.onProgressChanged(view, newProgress);
				
				进度改变(newProgress);
			}

			/*
			 * 显示 javascript 警报对话框
			 * 返回{@code true}表示不弹出系统的提示框，返回{@code false}表示弹出
			 */
			@Override
			public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
				// 部分机型只会弹出一次提示框，调用此方法即可解决此问题。
				BooleanReferenceParameter shield = new BooleanReferenceParameter(false);
				弹出警告框(url, message, shield);

				if (shield.get()) {
					result.cancel();
					return true;
				}
				return super.onJsAlert(view, url, message, result);
			}

			/*
			 * 网页弹出确认框时触发此方法。
			 * 返回{@code true}表示不弹出系统的提示框，返回{@code false}表示弹出
			 */
			@Override
			public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
				// confirm表示确认，cancel表示取消。
				IntegerReferenceParameter take = new IntegerReferenceParameter(处理方式_忽略);
				弹出确认框(url, message, take);

				switch (take.get()) {
					case 处理方式_取消:
						result.cancel();
						return true;
					case 处理方式_确认:
						result.confirm();
						return true;
					default:
						// 处理方式_忽略 或其它
						return super.onJsConfirm(view, url, message, result);
				}
			}

			/*
			 * 网页弹出输入框时触发此方法。
			 * 返回{@code true}表示不弹出系统的提示框，返回{@code false}表示弹出
			 */
			@Override
			public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
				// confirm表示确认（并返回值），cancel表示取消。
				IntegerReferenceParameter take = new IntegerReferenceParameter(处理方式_忽略);
				StringReferenceParameter value = new StringReferenceParameter(defaultValue);
				弹出输入框(url, message, value, take);

				switch (take.get()) {
					case 处理方式_取消:
						result.cancel();
						return true;
					case 处理方式_确认:
						result.confirm(value.get());
						return true;
					default:
						// 处理方式_忽略 或其它
						return super.onJsPrompt(view, url, message, value.get(), result);
				}
			}
		});

		// 下载监听器
		view.setDownloadListener(new DownloadListener() {
			/*
			 * 开始下载文件
			 */
			@Override
			public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
				开始下载(url, contentLength);
			}
		});

		view.setFocusable(true);
		view.setLongClickable(true);
		view.requestFocus();
		// 当浏览框被触摸时获取焦点，这样就可以弹出输入法软键盘了
		view.requestFocusFromTouch();
		WebSettings settings = view.getSettings();
		// 5.1以上默认禁止了https和http混用，以下方式是开启
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			// 允许从任何来源加载内容，即使起源是不安全的；
			settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
		}
		return view;
	}

	/*
	 * Web应用接口
	 */
	private class WebAppInterface {

		// 当前注册的接口名称
		String name;

		public WebAppInterface(String name) {
			this.name = name;
		}

		@JavascriptInterface
		public void call(String param) {
			MainActivity.getContext().runOnUiThread(new Runnable() {
				public void run() {
					接口调用(name, param);
				}
			});
		}
	}

	// 网页浏览框 实现

	@Override
	public void 触摸手势(int direction) {
		EventDispatcher.dispatchEvent(this, "触摸手势", direction);
	}

	@Override
	public void 即将跳转(String url, BooleanReferenceParameter shield) {
		EventDispatcher.dispatchEvent(this, "即将跳转", url, shield);
	}

	@Override
	public void 开始载入(String url) {
		EventDispatcher.dispatchEvent(this, "开始载入", url);
	}

	@Override
	public void 载入完毕(String url) {
		EventDispatcher.dispatchEvent(this, "载入完毕", url);
	}

	@Override
	public void 载入失败(int errorCode, String failingUrl) {
		EventDispatcher.dispatchEvent(this, "载入失败", errorCode, failingUrl);
	}

	@Override
	public void 进度改变(int newProgress) {
		EventDispatcher.dispatchEvent(this, "进度改变", newProgress);
	}

	@Override
	public void 弹出警告框(String url, String message, BooleanReferenceParameter shield) {
		EventDispatcher.dispatchEvent(this, "弹出警告框", url, message, shield);
	}

	@Override
	public void 弹出确认框(String url, String message, IntegerReferenceParameter take) {
		EventDispatcher.dispatchEvent(this, "弹出确认框", url, message, take);
	}

	@Override
	public void 弹出输入框(String url, String message, StringReferenceParameter value, IntegerReferenceParameter take) {
		EventDispatcher.dispatchEvent(this, "弹出输入框", url, value, take);
	}

	@Override
	public void 开始下载(String url, long contentLength) {
		EventDispatcher.dispatchEvent(this, "开始下载", url, contentLength);
	}

	@Override
	public void 接口调用(String name, String param) {
		EventDispatcher.dispatchEvent(this, "接口调用", name, param);
	}

	@Override
	public void 接口返回(String script, String result) {
		EventDispatcher.dispatchEvent(this, "接口返回", script, result);
	}

	@Override
	public void 地址(String url) {
		加载(url);
	}

	@Override
	public String 地址() {
		return ((WebView) getView()).getUrl();
	}

	@Override
	public String 标题() {
		return ((WebView) getView()).getTitle();
	}

	@Override
	public int 进度() {
		return ((WebView) getView()).getProgress();
	}

	@Override
	public boolean 支持缩放() {
		return ((WebView) getView()).getSettings().supportZoom();
	}

	@Override
	public void 支持缩放(boolean support) {
		((WebView) getView()).getSettings().setSupportZoom(support);
	}

	@Override
	public boolean 启用缩放按钮() {
		return ((WebView) getView()).getSettings().getBuiltInZoomControls();
	}

	@Override
	public void 启用缩放按钮(boolean enabled) {
		((WebView) getView()).getSettings().setBuiltInZoomControls(enabled);
	}

	@Override
	public int 缓存模式() {
		int mode = ((WebView) getView()).getSettings().getCacheMode();
		// 弃用于API17：WebSettings.LOAD_NORMAL；被 WebSettings.LOAD_DEFAULT 代替
		return mode == 0 ? 缓存模式_默认 : mode;
	}

	@Override
	public void 缓存模式(int mode) {
		((WebView) getView()).getSettings().setCacheMode(mode);
	}

	@Override
	public String UserAgent() {
		return ((WebView) getView()).getSettings().getUserAgentString();
	}

	@Override
	public void UserAgent(String ua) {
		WebSettings settings = ((WebView) getView()).getSettings();
		if (ua != null && !ua.isEmpty()) {
			settings.setUserAgentString(ua);
		} else {
			settings.setUserAgentString(null);
		}
	}

	@Override
	public boolean 启用JS() {
		return ((WebView) getView()).getSettings().getJavaScriptEnabled();
	}

	@Override
	public void 启用JS(boolean flag) {
		((WebView) getView()).getSettings().setJavaScriptEnabled(flag);
	}

	@Override
	public void 加载(String url) {
		try {
			((WebView) getView()).loadUrl(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void 刷新() {
		((WebView) getView()).reload();
	}

	@Override
	public void 停止() {
		((WebView) getView()).stopLoading();
	}

	@Override
	public void 后退() {
		((WebView) getView()).goBack();
	}

	@Override
	public void 前进() {
		((WebView) getView()).goForward();
	}

	@Override
	public boolean 可否后退() {
		return ((WebView) getView()).canGoBack();
	}

	@Override
	public boolean 可否前进() {
		return ((WebView) getView()).canGoForward();
	}

	@Override
	public void 注册接口(String name) {
		WebView view = (WebView) getView();
		WebSettings settings = view.getSettings();
		settings.setJavaScriptEnabled(true);
		settings.setDefaultTextEncodingName("UTF-8");
		// 构造传入接口名，将用于后续事件
		view.addJavascriptInterface(new WebAppInterface(name), name);
	}

	@Override
	public boolean 调用接口(String script, boolean isReturn) {
		WebView view = (WebView) getView();
		WebSettings settings = view.getSettings();
		settings.setJavaScriptEnabled(true);
		settings.setDefaultTextEncodingName("UTF-8");

		// API19兼容处理
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			ValueCallback<String> callback = null;
			if (isReturn) {
				callback = new ValueCallback<String>() {
					@Override
					public void onReceiveValue(String result) {
						接口返回(script, result);
					}
				};
			}
			// 添加于API19：WebView.evaluateJavascript(String, ValueCallback<String>)
			view.evaluateJavascript(script, callback);
			return true;
		} else if (!isReturn) {
			view.loadUrl(script);
			return true;
		}
		return false;
	}
}

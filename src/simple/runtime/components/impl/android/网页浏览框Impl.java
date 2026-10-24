package simple.runtime.components.impl.android;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.LinkedHashMap;
import java.util.Map;

import simple.runtime.android.MainActivity;
import simple.runtime.components.网页浏览框;
import simple.runtime.components.组件容器;
import simple.runtime.events.EventDispatcher;
import simple.runtime.parameters.BooleanReferenceParameter;
import simple.runtime.parameters.IntegerReferenceParameter;
import simple.runtime.parameters.StringReferenceParameter;

/**
 * 承载网页、导航和脚本交互，按宿主生命周期暂停、恢复并释放 WebView。
 *
 * @author 树先生 xhwsd@qq.com
 */
public class 网页浏览框Impl extends 视图组件 implements 网页浏览框,
		MainActivity.OnPauseListener, MainActivity.OnResumeListener, MainActivity.OnDestroyListener {

	// createView() 在父类构造期间执行，这些字段不能用子类字段初始化器覆盖。
	private MainActivity activity;
	private WebView browser;
	private GestureDetector gestureDetector;
	private Handler handler;
	private Map<String, WebAppInterface> interfaces;
	private volatile boolean destroyed;
	private boolean released;
	private volatile long documentGeneration;
	private String lastUrl;
	private String lastError;
	private JsResult activeDialog;

	/**
	 * 创建浏览框并订阅宿主生命周期；必须在主线程创建。
	 *
	 * @param container 放置浏览框的组件容器
	 */
	public 网页浏览框Impl(组件容器 container) {
		super(container);
		activity.addOnPauseListener(this);
		activity.addOnResumeListener(this);
		activity.addOnDestroyListener(this);
		if (!activity.isInForeground()) {
			browser.onPause();
		}
	}

	@Override
	protected View createView() {
		requireMainThread();
		activity = MainActivity.getContext();
		if (activity == null) {
			throw new IllegalStateException("网页浏览框需要有效的主活动");
		}
		handler = new Handler(Looper.getMainLooper());
		interfaces = new LinkedHashMap<>();
		lastUrl = "";
		lastError = "";
		gestureDetector = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onDown(MotionEvent event) {
				return true;
			}

			@Override
			public boolean onFling(MotionEvent first, MotionEvent last, float velocityX, float velocityY) {
				if (first != null && last != null) {
					float deltaX = first.getRawX() - last.getRawX();
					float deltaY = first.getRawY() - last.getRawY();
					触摸手势(Math.abs(deltaX) > Math.abs(deltaY)
							? (deltaX > 0 ? 触摸_左滑 : 触摸_右滑)
							: (deltaY > 0 ? 触摸_上滑 : 触摸_下滑));
				}
				return false;
			}

			@Override
			public boolean onScroll(MotionEvent first, MotionEvent last, float distanceX, float distanceY) {
				触摸手势(Math.abs(distanceX) > Math.abs(distanceY)
						? (distanceX > 0 ? 触摸_左移 : 触摸_右移)
						: (distanceY > 0 ? 触摸_上移 : 触摸_下移));
				return false;
			}

			@Override
			public boolean onSingleTapConfirmed(MotionEvent event) {
				触摸手势(触摸_单击);
				return false;
			}

			@Override
			public boolean onDoubleTap(MotionEvent event) {
				触摸手势(触摸_双击);
				return false;
			}
		});

		browser = new WebView(activity) {
			@Override
			public boolean dispatchTouchEvent(MotionEvent event) {
				if (destroyed) {
					return false;
				}
				gestureDetector.onTouchEvent(event);
				return super.dispatchTouchEvent(event);
			}
		};
		// API 26 类型仅在该版本分支创建，API 24、25 沿用普通客户端。
		browser.setWebViewClient(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Api26Client() : new BrowserClient());
		browser.setWebChromeClient(new ScriptClient());
		browser.setDownloadListener(new DownloadListener() {
			@Override
			public void onDownloadStart(String url, String userAgent, String disposition, String mimeType, long length) {
				开始下载(url, length);
			}
		});

		WebSettings settings = browser.getSettings();
		settings.setDefaultTextEncodingName("UTF-8");
		settings.setBuiltInZoomControls(true);
		settings.setDisplayZoomControls(false);
		// 编译目标为 API 30；这些设置在项目最低 API 24 上也已可用。
		settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
		browser.setFocusable(true);
		browser.setFocusableInTouchMode(true);
		return browser;
	}

	/* 组件事件：同步参考参数必须在主线程就地处理。 */

	@Override
	public void 触摸手势(int direction) {
		if (!destroyed) {
			EventDispatcher.dispatchEvent(this, "触摸手势", direction);
		}
	}

	@Override
	public void 即将跳转(String url, BooleanReferenceParameter shield) {
		if (!destroyed) {
			EventDispatcher.dispatchEvent(this, "即将跳转", url, shield);
		}
	}

	@Override
	public void 开始载入(String url) {
		if (!destroyed) {
			EventDispatcher.dispatchEvent(this, "开始载入", url);
		}
	}

	@Override
	public void 载入完毕(String url) {
		if (!destroyed) {
			EventDispatcher.dispatchEvent(this, "载入完毕", url);
		}
	}

	@Override
	public void 载入失败(int errorCode, String failingUrl) {
		if (!destroyed) {
			EventDispatcher.dispatchEvent(this, "载入失败", errorCode, failingUrl);
		}
	}

	@Override
	public void HTTP错误(int statusCode, String url) {
		if (!destroyed) {
			EventDispatcher.dispatchEvent(this, "HTTP错误", statusCode, url);
		}
	}

	@Override
	public void 进度改变(int progress) {
		if (!destroyed) {
			EventDispatcher.dispatchEvent(this, "进度改变", progress);
		}
	}

	@Override
	public void 弹出警告框(String url, String message, BooleanReferenceParameter shield) {
		if (!destroyed) {
			EventDispatcher.dispatchEvent(this, "弹出警告框", url, message, shield);
		}
	}

	@Override
	public void 弹出确认框(String url, String message, IntegerReferenceParameter take) {
		if (!destroyed) {
			EventDispatcher.dispatchEvent(this, "弹出确认框", url, message, take);
		}
	}

	@Override
	public void 弹出输入框(String url, String message, StringReferenceParameter value, IntegerReferenceParameter take) {
		if (!destroyed) {
			EventDispatcher.dispatchEvent(this, "弹出输入框", url, message, value, take);
		}
	}

	@Override
	public void 开始下载(String url, long contentLength) {
		if (!destroyed) {
			EventDispatcher.dispatchEvent(this, "开始下载", url, contentLength);
		}
	}

	@Override
	public void 接口调用(String name, String param) {
		if (!destroyed) {
			EventDispatcher.dispatchEvent(this, "接口调用", name, param);
		}
	}

	@Override
	public void 接口返回(String script, String result) {
		if (!destroyed) {
			EventDispatcher.dispatchEvent(this, "接口返回", script, result);
		}
	}

	/* 组件属性 */

	@Override
	public String 地址() {
		String value = requireBrowser().getUrl();
		return value == null ? "" : value;
	}

	@Override
	public void 地址(String url) {
		requireBrowser();
		// 设计器默认空地址只表示尚未加载页面。
		if (url != null && !url.trim().isEmpty()) {
			加载(url);
		}
	}

	@Override
	public String 标题() {
		String value = requireBrowser().getTitle();
		return value == null ? "" : value;
	}

	@Override
	public int 进度() {
		return requireBrowser().getProgress();
	}

	@Override
	public boolean 支持缩放() {
		return requireBrowser().getSettings().supportZoom();
	}

	@Override
	public void 支持缩放(boolean support) {
		requireBrowser().getSettings().setSupportZoom(support);
	}

	@Override
	public boolean 启用缩放按钮() {
		return requireBrowser().getSettings().getDisplayZoomControls();
	}

	@Override
	public void 启用缩放按钮(boolean enabled) {
		requireBrowser().getSettings().setDisplayZoomControls(enabled);
	}

	@Override
	public int 缓存模式() {
		int mode = requireBrowser().getSettings().getCacheMode();
		// 兼容平台可能返回的旧 LOAD_NORMAL 数值，不引用已弃用常量。
		return mode == 0 ? 缓存模式_默认 : mode;
	}

	@Override
	public void 缓存模式(int mode) {
		if (mode != 缓存模式_默认 && mode != 缓存模式_优先缓存
				&& mode != 缓存模式_仅网络 && mode != 缓存模式_仅缓存) {
			throw new IllegalArgumentException("未知的网页缓存模式：" + mode);
		}
		requireBrowser().getSettings().setCacheMode(mode);
	}

	@Override
	public String UserAgent() {
		return requireBrowser().getSettings().getUserAgentString();
	}

	@Override
	public void UserAgent(String value) {
		requireBrowser().getSettings().setUserAgentString(value == null || value.isEmpty() ? null : value);
	}

	@Override
	public boolean 启用JS() {
		return requireBrowser().getSettings().getJavaScriptEnabled();
	}

	@Override
	public void 启用JS(boolean enabled) {
		requireBrowser().getSettings().setJavaScriptEnabled(enabled);
		if (!enabled) {
			documentGeneration++;
		}
	}

	@Override
	public boolean 允许混合内容() {
		return requireBrowser().getSettings().getMixedContentMode() == WebSettings.MIXED_CONTENT_ALWAYS_ALLOW;
	}

	@Override
	public void 允许混合内容(boolean allow) {
		requireBrowser().getSettings().setMixedContentMode(allow
				? WebSettings.MIXED_CONTENT_ALWAYS_ALLOW : WebSettings.MIXED_CONTENT_NEVER_ALLOW);
	}

	@Override
	public boolean 已销毁() {
		return destroyed;
	}

	@Override
	public String 错误信息() {
		requireMainThread();
		return lastError;
	}

	/* 组件方法 */

	@Override
	public void 加载(String url) {
		WebView view = requireBrowser();
		if (url == null || url.trim().isEmpty()) {
			throw new IllegalArgumentException("加载网址不能为空");
		}
		documentGeneration++;
		lastError = "";
		lastUrl = url;
		view.loadUrl(url);
	}

	@Override
	public void 刷新() {
		WebView view = requireBrowser();
		documentGeneration++;
		lastError = "";
		view.reload();
	}

	@Override
	public void 停止() {
		requireBrowser().stopLoading();
	}

	@Override
	public void 后退() {
		WebView view = requireBrowser();
		if (view.canGoBack()) {
			documentGeneration++;
			view.goBack();
		}
	}

	@Override
	public void 前进() {
		WebView view = requireBrowser();
		if (view.canGoForward()) {
			documentGeneration++;
			view.goForward();
		}
	}

	@Override
	public boolean 可否后退() {
		return requireBrowser().canGoBack();
	}

	@Override
	public boolean 可否前进() {
		return requireBrowser().canGoForward();
	}

	@Override
	public void 注册接口(String name) {
		WebView view = requireBrowser();
		requireInterfaceName(name);
		WebAppInterface bridge = new WebAppInterface(name);
		view.getSettings().setJavaScriptEnabled(true);
		view.addJavascriptInterface(bridge, name);
		interfaces.put(name, bridge);
	}

	@Override
	public boolean 移除接口(String name) {
		requireMainThread();
		requireInterfaceName(name);
		if (destroyed || !interfaces.containsKey(name)) {
			return false;
		}
		browser.removeJavascriptInterface(name);
		interfaces.remove(name);
		return true;
	}

	@Override
	public boolean 调用接口(String script) {
		return 调用接口(script, false);
	}

	@Override
	public boolean 调用接口(final String script, boolean withResult) {
		requireMainThread();
		if (destroyed) {
			lastError = "网页浏览框已销毁";
			return false;
		}
		String source = normalizeScript(script);
		if (source.isEmpty()) {
			lastError = "执行脚本不能为空";
			return false;
		}
		try {
			browser.getSettings().setJavaScriptEnabled(true);
			final long generation = documentGeneration;
			if (Build.VERSION.SDK_INT >= 19) {
				browser.evaluateJavascript(source, withResult ? new ValueCallback<String>() {
					@Override
					public void onReceiveValue(String result) {
						if (!destroyed && generation == documentGeneration) {
							接口返回(script, result == null ? "null" : result);
						}
					}
				} : null);
			} else if (!withResult) {
				// 旧接口只执行 javascript: URL，没有等价的异步结果回调。
				browser.loadUrl("javascript:" + source);
			} else {
				lastError = "当前系统不支持脚本返回结果";
				return false;
			}
			lastError = "";
			return true;
		} catch (RuntimeException failure) {
			lastError = failure.toString();
			return false;
		}
	}

	@Override
	public void 销毁() {
		requireMainThread();
		if (!destroyed) {
			dispose(false);
		}
	}

	/* 宿主生命周期 */

	@Override
	public void onPause() {
		if (!destroyed) {
			browser.onPause();
		}
	}

	@Override
	public void onResume() {
		if (!destroyed) {
			browser.onResume();
		}
	}

	@Override
	public void onDestroy() {
		销毁();
	}

	/**
	 * 获取仍然有效的浏览器，并拒绝错误线程或销毁后的原生操作。
	 *
	 * @return 当前浏览器
	 * @throws IllegalStateException 不在主线程或组件已销毁
	 */
	private WebView requireBrowser() {
		requireMainThread();
		if (destroyed) {
			throw new IllegalStateException("网页浏览框已销毁");
		}
		return browser;
	}

	/**
	 * 检查当前线程，避免将同步参考参数或 WebView 操作异步转发后伪造结果。
	 *
	 * @throws IllegalStateException 当前不在主线程
	 */
	private static void requireMainThread() {
		if (Looper.myLooper() != Looper.getMainLooper()) {
			throw new IllegalStateException("网页浏览框必须在主线程操作");
		}
	}

	/**
	 * 拒绝没有实际名称的脚本接口。
	 *
	 * @param name 脚本接口名
	 * @throws IllegalArgumentException 名称为空或只有空白
	 */
	private static void requireInterfaceName(String name) {
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("脚本接口名称不能为空");
		}
	}

	/**
	 * 将可选 javascript: 前缀统一还原为脚本源码，供新旧执行接口使用。
	 *
	 * @param script 脚本源码或带前缀的脚本
	 * @return 规范化后的源码；无内容时为空
	 */
	private static String normalizeScript(String script) {
		String source = script == null ? "" : script.trim();
		if (source.regionMatches(true, 0, "javascript:", 0, 11)) {
			source = source.substring(11).trim();
		}
		return source;
	}

	/**
	 * 同步询问调用者是否取消导航；不取消时由 WebView 继续原请求。
	 *
	 * @param url 导航目标
	 * @return 是否取消原请求
	 */
	private boolean interceptNavigation(String url) {
		if (destroyed) {
			return true;
		}
		BooleanReferenceParameter shield = new BooleanReferenceParameter(false);
		即将跳转(url, shield);
		return destroyed || shield.get();
	}

	/**
	 * 清理当前组件的浏览器和回调，重复执行不会重复释放。
	 *
	 * @param rendererGone 渲染进程是否已退出；此时不再访问失效浏览器的设置或页面
	 */
	private void dispose(boolean rendererGone) {
		if (released) {
			return;
		}
		released = true;
		destroyed = true;
		documentGeneration++;
		handler.removeCallbacksAndMessages(null);
		WebView view = browser;
		try {
			if (activeDialog != null) {
				activeDialog.cancel();
				activeDialog = null;
			}
			if (!rendererGone) {
				view.stopLoading();
				for (String name : interfaces.keySet()) {
					view.removeJavascriptInterface(name);
				}
				view.setWebViewClient(null);
				view.setWebChromeClient(null);
				view.setDownloadListener(null);
			}
		} finally {
			interfaces.clear();
			activity.removeOnPauseListener(this);
			activity.removeOnResumeListener(this);
			activity.removeOnDestroyListener(this);
			activity = null;
			gestureDetector = null;
			try {
				// 先从父视图移除并注销事件，再释放 WebView 的原生资源。
				super.销毁();
			} finally {
				browser = null;
				view.destroy();
			}
		}
	}

	/**
	 * 将主页面导航与加载结果转换为组件事件，保留必要的旧版回调。
	 */
	private class BrowserClient extends WebViewClient {

		/**
		 * 创建当前组件的页面客户端。
		 */
		BrowserClient() {
			super();
		}

		@Deprecated
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			// API 24 以前只能收到字符串回调；新系统若回退到此入口也不能无条件吞掉导航。
			return interceptNavigation(url);
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
			return request.isForMainFrame() && interceptNavigation(request.getUrl().toString());
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap icon) {
			if (!destroyed) {
				documentGeneration++;
				lastUrl = url;
				lastError = "";
				开始载入(url);
			}
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			载入完毕(url);
		}

		@Deprecated
		@Override
		public void onReceivedError(WebView view, int code, String description, String url) {
			// API 23 以前只有旧回调；新回调自行处理，避免主页面错误重复通知。
			if (!destroyed && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
				lastError = description == null ? "页面加载失败" : description;
				载入失败(code, url);
			}
		}

		@Override
		public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
			if (!destroyed && request.isForMainFrame()) {
				lastError = String.valueOf(error.getDescription());
				载入失败(error.getErrorCode(), request.getUrl().toString());
			}
		}

		@Override
		public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
			if (!destroyed && request.isForMainFrame()) {
				lastError = "HTTP " + response.getStatusCode();
				HTTP错误(response.getStatusCode(), request.getUrl().toString());
			}
		}
	}

	/**
	 * 隔离 API 26 新增的渲染进程退出类型，失效后销毁对应组件并报告终止错误。
	 */
	private final class Api26Client extends BrowserClient {

		/**
		 * 创建支持渲染进程退出通知的客户端。
		 */
		Api26Client() {
			super();
		}

		@Override
		public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
			if (!destroyed) {
				destroyed = true;
				lastError = detail.didCrash() ? "网页渲染进程崩溃，组件已销毁" : "网页渲染进程退出，组件已销毁";
				try {
					// 先使原生操作失效，再报告终止事件；注销事件必须在通知完成之后。
					EventDispatcher.dispatchEvent(网页浏览框Impl.this, "载入失败", ERROR_UNKNOWN, lastUrl);
				} finally {
					dispose(true);
				}
			}
			return true;
		}
	}

	/**
	 * 将脚本对话框和进度转换为同步组件事件，未接管的对话框沿用系统默认界面。
	 */
	private final class ScriptClient extends WebChromeClient {

		/**
		 * 创建脚本界面回调客户端。
		 */
		ScriptClient() {
			super();
		}

		@Override
		public void onProgressChanged(WebView view, int progress) {
			进度改变(progress);
		}

		@Override
		public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
			if (destroyed) {
				result.cancel();
				return true;
			}
			activeDialog = result;
			try {
				BooleanReferenceParameter shield = new BooleanReferenceParameter(false);
				弹出警告框(url, message, shield);
				if (destroyed) {
					return true;
				}
				if (shield.get()) {
					// alert 没有取消返回值，确认即可抑制界面并继续脚本。
					result.confirm();
					return true;
				}
				return false;
			} catch (RuntimeException | Error failure) {
				result.cancel();
				throw failure;
			} finally {
				activeDialog = null;
			}
		}

		@Override
		public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
			if (destroyed) {
				result.cancel();
				return true;
			}
			activeDialog = result;
			try {
				IntegerReferenceParameter take = new IntegerReferenceParameter(处理方式_忽略);
				弹出确认框(url, message, take);
				if (destroyed) {
					return true;
				}
				if (take.get() == 处理方式_确认) {
					result.confirm();
					return true;
				}
				if (take.get() == 处理方式_取消) {
					result.cancel();
					return true;
				}
				return false;
			} catch (RuntimeException | Error failure) {
				result.cancel();
				throw failure;
			} finally {
				activeDialog = null;
			}
		}

		@Override
		public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
			if (destroyed) {
				result.cancel();
				return true;
			}
			activeDialog = result;
			try {
				IntegerReferenceParameter take = new IntegerReferenceParameter(处理方式_忽略);
				StringReferenceParameter value = new StringReferenceParameter(defaultValue);
				弹出输入框(url, message, value, take);
				if (destroyed) {
					return true;
				}
				if (take.get() == 处理方式_确认) {
					result.confirm(value.get());
					return true;
				}
				if (take.get() == 处理方式_取消) {
					result.cancel();
					return true;
				}
				return false;
			} catch (RuntimeException | Error failure) {
				result.cancel();
				throw failure;
			} finally {
				activeDialog = null;
			}
		}
	}

	/**
	 * 只暴露带 JavascriptInterface 标记的调用入口，将桥接线程消息交给主线程。
	 */
	private final class WebAppInterface {
		private final String name;

		/**
		 * 保存接口名，供替换、移除和回调过滤核对对象身份。
		 *
		 * @param name 注册名
		 */
		WebAppInterface(String name) {
			this.name = name;
		}

		/**
		 * 异步发送脚本消息；已移除、替换、切换页面或销毁后的排队消息不再派发。
		 *
		 * @param param 脚本消息
		 */
		@JavascriptInterface
		public void call(final String param) {
			if (destroyed) {
				return;
			}
			final long generation = documentGeneration;
			handler.post(new Runnable() {
				@Override
				public void run() {
					if (!destroyed && generation == documentGeneration && interfaces.get(name) == WebAppInterface.this) {
						接口调用(name, param);
					}
				}
			});
		}
	}
}

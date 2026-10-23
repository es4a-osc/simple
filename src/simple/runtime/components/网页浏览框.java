package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleDataElement;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.UsesPermissions;
import simple.runtime.parameters.BooleanReferenceParameter;
import simple.runtime.parameters.IntegerReferenceParameter;
import simple.runtime.parameters.StringReferenceParameter;

/**
 * 承载网页与 JavaScript 交互的可视组件，需要系统提供可用的 WebView。
 *
 * <p>除已销毁状态查询外，调用与事件均在主线程执行。宿主销毁时自动释放，
 * 销毁后仅可查询已销毁和错误信息、重复销毁，以及调用返回失败的脚本接口。
 * 注册的脚本接口可被页面及其子框架访问，只应注册到可信内容。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
@UsesPermissions(permissionNames =
	// 联网加载网页；组件不提供定位授权或网页定位回调。
	"android.permission.INTERNET"
)
public interface 网页浏览框 extends 可视组件 {
	/**
	 * 按平台默认策略使用网页缓存。
	 *
	 * <p>{@code LOAD_DEFAULT}
	 */
	@SimpleDataElement
	public static final int 缓存模式_默认 = -1;

	/**
	 * 优先使用已有网页缓存。
	 *
	 * <p>{@code LOAD_CACHE_ELSE_NETWORK}
	 */
	@SimpleDataElement
	public static final int 缓存模式_优先缓存 = 1;

	/**
	 * 仅从网络加载网页内容。
	 *
	 * <p>{@code LOAD_NO_CACHE}
	 */
	@SimpleDataElement
	public static final int 缓存模式_仅网络 = 2;

	/**
	 * 仅从网页缓存加载内容。
	 *
	 * <p>{@code LOAD_CACHE_ONLY}
	 */
	@SimpleDataElement
	public static final int 缓存模式_仅缓存 = 3;

	/**
	 * 保持组件默认处理方式。
	 */
	@SimpleDataElement
	public static final int 处理方式_忽略 = 0;

	/**
	 * 取消本次网页脚本对话框请求。
	 */
	@SimpleDataElement
	public static final int 处理方式_取消 = 1;

	/**
	 * 确认本次网页脚本对话框请求。
	 */
	@SimpleDataElement
	public static final int 处理方式_确认 = 2;

	/* 组件事件 */

	/**
	 * 触摸手势事件处理方法。
	 *
	 * @param direction 触摸，可选值请参考{@link 组件#触摸_单击}等常量。
	 */
	@SimpleEvent
	void 触摸手势(int direction);

	/**
	 * 主页面由网页链接、脚本或重定向发起导航时触发，可同步屏蔽本次导航。
	 *
	 * <p>主动调用加载不会保证触发本事件，POST 等部分导航也不会经过此回调。
	 *
	 * @param url 即将跳转的网址。
	 * @param shield 是否屏蔽，缺省为{@code false}。
	 */
	@SimpleEvent
	void 即将跳转(String url, BooleanReferenceParameter shield);

	/**
	 * 主页面开始载入时触发。
	 *
	 * @param url 开始载入的网址。
	 */
	@SimpleEvent
	void 开始载入(String url);

	/**
	 * 主页面完成本轮载入时触发，不代表全部子资源成功或页面没有 HTTP 错误。
	 *
	 * @param url 载入完毕的网址。
	 */
	@SimpleEvent
	void 载入完毕(String url);

	/**
	 * 主页面网络或资源加载失败时触发，子资源失败不触发。
	 *
	 * <p>HTTP 状态错误通过 HTTP错误报告；渲染进程退出时先标记组件销毁，
	 * 再以未知错误码报告本事件，具体原因见错误信息。该终止通知需要 API 26 及以上。
	 *
	 * @param errorCode WebView 错误码；渲染进程退出使用未知错误码 -1
	 * @param failingUrl 失败的网址。
	 */
	@SimpleEvent
	void 载入失败(int errorCode, String failingUrl);

	/**
	 * 进度改变事件处理方法。
	 *
	 * @param newProgress 当前进度。
	 */
	@SimpleEvent
	void 进度改变(int newProgress);

	/**
	 * 网页请求 alert 警告框时触发；屏蔽后不显示界面，脚本继续执行。
	 *
	 * @param url 网址。
	 * @param message 警告信息。
	 * @param shield 是否屏蔽。
	 */
	@SimpleEvent
	void 弹出警告框(String url, String message, BooleanReferenceParameter shield);

	/**
	 * 网页请求 confirm 确认框时触发；忽略使用系统界面，确认返回真，取消返回假。
	 *
	 * @param url 网址。
	 * @param message 警告信息。
	 * @param handle 处理方式；可选值请参考{@link 网页浏览框#处理方式_忽略}。
	 */
	@SimpleEvent
	void 弹出确认框(String url, String message, IntegerReferenceParameter handle);

	/**
	 * 网页请求 prompt 输入框时触发；忽略使用系统界面，确认返回参考参数文本，取消返回 null。
	 *
	 * @param url 网址。
	 * @param message 警告信息。
	 * @param value 输入内容。
	 * @param handle 处理方式；可选值请参考{@link 网页浏览框#处理方式_忽略}。
	 */
	@SimpleEvent
	void 弹出输入框(String url, String message, StringReferenceParameter value, IntegerReferenceParameter handle);

	/**
	 * 网页请求下载时触发，本组件只通知下载地址，不自动保存文件。
	 *
	 * @param url 开始下载网址。
	 * @param contentLength 字节数；未知时为 -1
	 */
	@SimpleEvent
	void 开始下载(String url, long contentLength);

	/**
	 * 接口触发事件处理方法。
	 *
	 * <p>脚本调用已注册接口对象后在主线程触发；接口移除、替换或组件销毁后不再派发排队消息。
	 *
	 * @param name 被调用的接口对象名。
	 * @param param 传递的参数内容。
	 */
	@SimpleEvent
	void 接口调用(String name, String param);

	/**
	 * 接口返回事件处理方法。
	 *
	 * <p>请求脚本结果后异步触发，页面切换或组件销毁后不再派发旧页面的排队结果。
	 *
	 * @param script 调用的JS脚本。
	 * @param result WebView 返回的 JSON 文本；字符串含 JSON 引号，空结果通常为 null 文本
	 */
	@SimpleEvent
	void 接口返回(String script, String result);

	/* 组件属性 */

	/**
	 * 地址属性获取方法。
	 *
	 * <p>可以载入asset中的html，如{@code file:///android_asset/index.html}
	 *
	 * @return 当前网址；尚无网址时为空文本。
	 */
	@SimpleProperty
	String 地址();

	/**
	 * 地址属性设置方法。
	 *
	 * @param url 当前网址；空文本不发起加载
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_STRING,
		initializer = "\"\""
	)
	void 地址(String url);

	/**
	 * 标题属性获取方法。
	 *
	 * @return 网页标题；尚无标题时为空文本。
	 */
	@SimpleProperty
	String 标题();

	/**
	 * 进度属性获取方法。
	 *
	 * @return 返回当前网页浏览器的进度。
	 */
	@SimpleProperty
	int 进度();

	/**
	 * 支持缩放属性获取方法。
	 *
	 * @return 返回当前网页浏览器是否支持缩放。
	 */
	@SimpleProperty
	boolean 支持缩放();

	/**
	 * 支持缩放属性获取方法。
	 *
	 * @param support 设置当前网页浏览器是否支持缩放。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "True"
	)
	void 支持缩放(boolean support);

	/**
	 * 启用缩放按钮属性获取方法。
	 *
	 * @return 返回当前网页浏览器是否显示内置缩放按钮；与支持缩放独立，关闭按钮不关闭双指缩放。
	 */
	@SimpleProperty
	boolean 启用缩放按钮();

	/**
	 * 启用缩放按钮属性设置方法。
	 *
	 * @param enabled 设置当前网页浏览器是否显示内置缩放按钮；与支持缩放独立，关闭按钮不关闭双指缩放。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "False"
	)
	void 启用缩放按钮(boolean enabled);

	/**
	 * 缓存模式属性获取方法。
	 *
	 * @return 返回当前网页浏览器的缓存模式，可选值请参考{@link 网页浏览框#缓存模式_默认}等常量。
	 */
	@SimpleProperty
	int 缓存模式();

	/**
	 * 缓存模式属性设置方法。
	 *
	 * @param mode 设置当前网页浏览器的缓存模式，
	 * 可选值请参考{@link 网页浏览框#缓存模式_默认}等常量。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER,
		initializer = 缓存模式_默认 + ""
	)
	void 缓存模式(int mode);

	/**
	 * UserAgent属性获取方法。
	 *
	 * @return 返回当前网页浏览器的UserAgent。
	 */
	@SimpleProperty
	String UserAgent();

	/**
	 * UserAgent属性获取方法。
	 *
	 * @param ua 设置当前网页浏览器的UserAgent。
	 */
	@SimpleProperty
	void UserAgent(String ua);

	/**
	 * 启用JS属性获取方法。
	 *
	 * @return 返回当前网页浏览器是否启用JS。
	 */
	@SimpleProperty
	boolean 启用JS();

	/**
	 * 启用JS属性获取方法。
	 *
	 * @param flag 设置当前网页浏览器是否启用JS。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "True"
	)
	void 启用JS(boolean flag);

	/* 组件方法 */

	/**
	 * 加载指定网址。
	 *
	 * <p>可以载入asset中的html，如{@code file:///android_asset/index.html}
	 *
	 * @param url 非空网址，支持 file:///android_asset/ 下的本地网页
	 * @throws IllegalArgumentException 网址为空或只有空白
	 * @throws IllegalStateException 当前不在主线程或组件已销毁
	 */
	@SimpleFunction
	void 加载(String url);

	/**
	 * 刷新当前页面
	 */
	@SimpleFunction
	void 刷新();

	/**
	 * 停止加载。
	 */
	@SimpleFunction
	void 停止();

	/**
	 * 后退前一页面。
	 */
	@SimpleFunction
	void 后退();

	/**
	 * 前进后一页面。
	 */
	@SimpleFunction
	void 前进();

	/**
	 * 检验是否可以后退。
	 *
	 * @return 可后退返回{@code true}，否则返回{@code false}。
	 */
	@SimpleFunction
	boolean 可否后退();

	/**
	 * 检验是否可以前进。
	 *
	 * @return 可前进返回{@code true}，否则返回{@code false}。
	 */
	@SimpleFunction
	boolean 可否前进();

	/**
	 * 注册供网页调用的脚本接口，会开启 JavaScript；同名注册替换原接口。
	 *
	 * <p>应在加载页面前注册，注册后的对象在下一次页面载入时对脚本生效。
	 *
	 * <p>JS中调用格式为{@code 接口对象名.call(param)}，
	 * 将触发{@link 网页浏览框#接口调用(String, String)}事件。
	 *
	 * @param name 非空接口对象名
	 * @throws IllegalArgumentException 名称为空或只有空白
	 */
	@SimpleFunction
	void 注册接口(String name);

	/**
	 * 执行脚本且不请求接口返回事件，会开启 JavaScript。
	 *
	 * @param script JavaScript 源码，可带 javascript: 前缀
	 * @return 已发起执行返回真，否则返回假，不表示脚本业务成功。
	 */
	@SimpleFunction
	boolean 调用接口(String script);

	/**
	 * 异步执行 JavaScript 源码，会开启 JavaScript。
	 *
	 * <p>应在页面载入后调用；请求返回结果时异步触发接口返回。
	 * 返回真仅表示已提交执行，不表示脚本语法或业务执行成功。
	 *
	 * @param script 要执行的JavaScript。
	 * @param isReturn 是否有返回值。
	 * @return 已提交执行返回真；脚本为空、组件已销毁或提交失败返回假，原因见错误信息
	 */
	@SimpleFunction
	boolean 调用接口(String script, boolean isReturn);

	/**
	 * 主页面收到 HTTP 错误响应时触发；子资源的 HTTP 错误不触发。
	 *
	 * <p>服务器错误页面仍可能显示并触发载入完毕，不将 HTTP 状态码混作 WebView 错误码。
	 *
	 * @param statusCode HTTP 状态码，例如 404 或 500
	 * @param url 主页面网址
	 */
	@SimpleEvent
	void HTTP错误(int statusCode, String url);

	/**
	 * 是否允许 HTTPS 页面加载 HTTP 混合内容，默认为假。
	 *
	 * @return 是否允许混合内容
	 */
	@SimpleProperty
	boolean 允许混合内容();

	/**
	 * 设置是否允许 HTTPS 页面加载 HTTP 混合内容。
	 *
	 * @param allow 是否允许，仍受系统网络策略限制
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "False"
	)
	void 允许混合内容(boolean allow);

	/**
	 * 查询组件是否已经释放；可在任意线程查询。
	 *
	 * @return 是否已销毁
	 */
	@SimpleProperty
	boolean 已销毁();

	/**
	 * 最近主页面加载、渲染进程退出或脚本提交失败的说明，销毁后仍可读取。
	 *
	 * <p>开始新页面载入或成功提交脚本时清空，查询不修改。
	 *
	 * @return 错误说明；尚无错误时为空文本
	 */
	@SimpleProperty
	String 错误信息();

	/**
	 * 移除已注册的脚本接口，并立即停止其排队消息通知。
	 *
	 * <p>页面中的 JavaScript 对象在下次载入后才消失。
	 *
	 * @param name 接口对象名
	 * @return 原接口存在且已移除返回真；未注册或组件已销毁返回假
	 * @throws IllegalArgumentException 名称为空或只有空白
	 */
	@SimpleFunction
	boolean 移除接口(String name);
}

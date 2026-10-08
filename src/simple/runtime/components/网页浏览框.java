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
 * Simple的浏览器组件。
 *
 * @author 树先生 xhwd@qq.com
 */
@SimpleComponent
@SimpleObject
@UsesPermissions(permissionNames =
								// 允许应用程序打开网络套接字。
								"android.permission.INTERNET," +
								// 允许应用程序访问精确的位置。
								"android.permission.ACCESS_FINE_LOCATION," +
								// 允许应用程序访问大致的位置。
								"android.permission.ACCESS_COARSE_LOCATION")
public interface 网页浏览框 extends 可视组件 {

	@SimpleDataElement
	public static final int 缓存模式_默认 = -1; // LOAD_DEFAULT
	@SimpleDataElement
	public static final int 缓存模式_优先缓存 = 1; // LOAD_CACHE_ELSE_NETWORK
	@SimpleDataElement
	public static final int 缓存模式_仅网络 = 2; // LOAD_NO_CACHE
	@SimpleDataElement
	public static final int 缓存模式_仅缓存 = 3; // LOAD_CACHE_ONLY

	@SimpleDataElement
	public static final int 处理方式_忽略 = 0;
	@SimpleDataElement
	public static final int 处理方式_取消 = 1;
	@SimpleDataElement
	public static final int 处理方式_确认 = 2;

	/**
	 * 触摸手势事件处理方法。
	 *
	 * @param direction 触摸，可选值请参考{@link 组件#触摸_单击}等常量。
	 */
	@SimpleEvent
	void 触摸手势(int direction);

	/**
	 * 即将跳转事件处理方法。
	 *
	 * @param url 即将跳转的网址。
	 * @param shield 是否屏蔽，缺省为{@code false}。
	 */
	@SimpleEvent
	void 即将跳转(String url, BooleanReferenceParameter shield);

	/**
	 * 开始载入事件处理方法。
	 *
	 * @param url 开始载入的网址。
	 */
	@SimpleEvent
	void 开始载入(String url);

	/**
	 * 载入完毕事件处理方法。
	 *
	 * @param url 载入完毕的网址。
	 */
	@SimpleEvent
	void 载入完毕(String url);

	/**
	 * 载入失败事件处理方法。
	 *
	 * @param errorCode 失败的代码
	 * @param url 失败的网址。
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
	 * 弹出警告框事件处理方法。
	 *
	 * @param url 网址。
	 * @param message 警告信息。
	 * @param shield 是否屏蔽。
	 */
	@SimpleEvent
	void 弹出警告框(String url, String message, BooleanReferenceParameter shield);

	/**
	 * 弹出确认框事件处理方法。
	 *
	 * @param url 网址。
	 * @param message 警告信息。
	 * @param handle 处理方式；可选值请参考{@link 网页浏览框#处理方式_忽略}。
	 */
	@SimpleEvent
	void 弹出确认框(String url, String message, IntegerReferenceParameter handle);

	/**
	 * 弹出输入框事件处理方法。
	 *
	 * @param url 网址。
	 * @param message 警告信息。
	 * @param value 输入内容。
	 * @param handle 处理方式；可选值请参考{@link 网页浏览框#处理方式_忽略}。
	 */
	@SimpleEvent
	void 弹出输入框(String url, String message, StringReferenceParameter value, IntegerReferenceParameter handle);

	/**
	 * 开始下载事件处理方法。
	 *
	 * @param url 开始下载网址。
	 * @param contentLength 内容长度。
	 */
	@SimpleEvent
	void 开始下载(String url, long contentLength);

	/**
	 * 接口触发事件处理方法。
	 *
	 * <p>JS脚本中调用已注册的接口对象触发该事件。
	 *
	 * @param name 被调用的接口对象名。
	 * @param param 传递的参数内容。
	 */
	@SimpleEvent
	void 接口调用(String name, String param);

	/**
	 * 接口返回事件处理方法。
	 *
	 * <p>调用JS脚本中函数后返回结果触发触发该事件。
	 *
	 * @param script 调用的JS脚本。
	 * @param result 函数返回结果。
	 */
	@SimpleEvent
	void 接口返回(String script, String result);

	// 网页浏览框 属性定义

	/**
	 * 地址属性获取方法。
	 *
	 * <p>可以载入asset中的html，如{@code file:///android_asset/index.html}
	 *
	 * @return 返回当前网页浏览器的地址。
	 */
	@SimpleProperty
	String 地址();

	/**
	 * 地址属性设置方法。
	 *
	 * @param url 设置当前网页浏览器的地址。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_STRING,
		initializer = "\"\""
	)
	void 地址(String url);

	/**
	 * 标题属性获取方法。
	 *
	 * @return 返回当前网页浏览器的标题。
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
	 * @return 返回当前网页浏览器是否启用缩放按钮。
	 */
	@SimpleProperty
	boolean 启用缩放按钮();

	/**
	 * 启用缩放按钮属性设置方法。
	 *
	 * @param enabled 设置当前网页浏览器是否启用缩放按钮。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "False"
	)
	void 启用缩放按钮(boolean enabled);

	/**
	 * 缓存模式属性获取方法。
	 *
	 * @return 返回当前网页浏览器的缓存模式，可选值请参考{@link 网页浏览框#载入方式_默认}等常量。
	 */
	@SimpleProperty
	int 缓存模式();

	/**
	 * 缓存模式属性设置方法。
	 *
	 * @param mode 设置当前网页浏览器的缓存模式，
	 * 			可选值请参考{@link 网页浏览框#载入方式_默认}等常量。
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

	/**
	 * 加载指定网址。
	 *
	 * <p>可以载入asset中的html，如{@code file:///android_asset/index.html}
	 *
	 * @param url 加载网址。
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
	 * 向浏览器中注册JS接口，会开启JS。
	 *
	 * <p>JS中调用格式为{@code 接口对象名.call(param)}，
	 * 将触发{@link 网页浏览框#调用接口(name, param)}事件。
	 *
	 * @param name 接口对象名
	 */
	@SimpleFunction
	void 注册接口(String name);

	/**
	 * 调用JS中的函数，会开启JS。
	 *
	 * <p>注意仅API19及以上版本才可有返回值，
	 * 成功将触发{@link 网页浏览框#接口返回(script, result)}事件。
	 *
	 * @param script 要执行的JavaScript。
	 * @param isReturn 是否有返回值。
	 * @return 成功返回{@code true}，否则返回{@code false}。
	 */
	@SimpleFunction
	boolean 调用接口(String script, boolean isReturn);
}

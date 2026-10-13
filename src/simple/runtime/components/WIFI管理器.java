package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.UsesPermissions;

/**
 * Wi-Fi 网络、扫描与热点管理组件；系统权限和 Android 版本可能限制网络修改功能。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
@SimpleComponent
@UsesPermissions(permissionNames =
	// 允许应用程序访问 Wi-Fi 网络状态。
	"android.permission.ACCESS_WIFI_STATE," +
	// 允许应用程序访问网络连接状态。
	"android.permission.ACCESS_NETWORK_STATE," +
	// 允许应用程序改变 Wi-Fi 连接状态。
	"android.permission.CHANGE_WIFI_STATE," +
	// 允许应用程序改变网络连接状态。
	"android.permission.CHANGE_NETWORK_STATE," +
	// 允许应用程序使用唤醒锁，防止处理器休眠或屏幕变暗。
	"android.permission.WAKE_LOCK," +
	// 允许应用程序访问精确的位置。
	"android.permission.ACCESS_FINE_LOCATION," +
	// 允许应用程序访问大致的位置。
	"android.permission.ACCESS_COARSE_LOCATION"
)
public interface WIFI管理器 extends 组件 {

	/* 组件事件 */

	@SimpleEvent
	void 扫描完毕();

	@SimpleEvent
	void 开启状态改变(boolean enabled);

	/**
	 * 状态：1 已连接，2 连接中，3 已断开，4 断开中，5 暂停，6 未知。
	 */
	@SimpleEvent
	void 网络状态改变(int state);

	@SimpleEvent
	void 密码错误();

	/* 组件方法 */

	/**
	 * 每项为“名称|*|网络标识”。
	 */
	@SimpleFunction
	String[] 取已保存网络列表();

	@SimpleFunction
	int 取已保存网络(String ssid);

	@SimpleFunction
	void 打开WIFI设置();

	@SimpleFunction
	boolean 置WIFI状态(boolean enabled);

	@SimpleFunction
	boolean 是否已开启();

	/**
	 * 旧系统静态 IPv4 配置，前缀固定为 24；依赖系统隐藏接口支持。
	 */
	@SimpleFunction
	void 置静态IP(String ssid, String ip, String gateway, String DNS);

	/**
	 * 连接指定网络；禁用其它为真时同时禁用其它已保存网络。
	 */
	@SimpleFunction
	boolean 置指定WIFI状态(int networkId, boolean disableOthers);

	/**
	 * 返回系统状态：0 当前连接，1 禁用或不存在，2 启用。
	 */
	@SimpleFunction
	int 取连接状态(int networkId);

	@SimpleFunction
	boolean 开始扫描();

	/**
	 * 以“名称*地址*加密信息*信号*频率”返回结果，网络之间以 / 分隔。
	 */
	@SimpleFunction
	String 获取扫描结果();

	/**
	 * 添加网络并返回标识；类型：1 开放，2 WEP，3 WPA。
	 */
	@SimpleFunction
	int 添加网络(String ssid, String password, int type);

	@SimpleFunction
	boolean 删除网络(int networkId);

	@SimpleFunction
	void 锁定WIFI();

	@SimpleFunction
	void 解锁WIFI();

	@SimpleFunction
	void 创建热点(String ssid, String password);

	@SimpleFunction
	void 关闭热点();

	@SimpleFunction
	String 取当前BSSID();

	@SimpleFunction
	String 取当前SSID();

	@SimpleFunction
	int 取当前networkid();

	@SimpleFunction
	String 取内网IP();

	@SimpleFunction
	String 取MAC地址();

	@SimpleFunction
	int 取当前信号强度();

	@SimpleFunction
	int 取当前连接速度();

	@SimpleFunction
	String 取网关();

	@SimpleFunction
	String 取网络前缀();

	@SimpleFunction
	String 取DNS1();

	@SimpleFunction
	String 取DNS2();
}

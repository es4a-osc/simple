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

	/**
	 * 本组件请求的扫描结果就绪时触发。
	 */
	@SimpleEvent
	void 扫描完毕();

	/**
	 * Wi-Fi 开启状态变化时触发。
	 *
	 * @param enabled 是否启用
	 */
	@SimpleEvent
	void 开启状态改变(boolean enabled);

	/**
	 * 状态：1 已连接，2 连接中，3 已断开，4 断开中，5 暂停，6 未知。
	 *
	 * @param state 当前状态
	 */
	@SimpleEvent
	void 网络状态改变(int state);

	/**
	 * 网络认证失败时触发。
	 */
	@SimpleEvent
	void 密码错误();

	/* 组件方法 */

	/**
	 * 每项为“名称|*|网络标识”。
	 *
	 * @return 每项为“名称|*|网络标识”
	 */
	@SimpleFunction
	String[] 取已保存网络列表();

	/**
	 * 返回匹配网络的标识，未找到时返回 -1。
	 *
	 * @param ssid 无线网络名称
	 * @return 匹配网络的标识，未找到时返回 -1
	 */
	@SimpleFunction
	int 取已保存网络(String ssid);

	/**
	 * 打开系统 Wi-Fi 设置界面。
	 */
	@SimpleFunction
	void 打开WIFI设置();

	/**
	 * 请求开启或关闭 Wi-Fi，返回系统是否接受请求。
	 *
	 * @param enabled 是否启用
	 * @return 操作成功时为 {@code true}，失败时为 {@code false}
	 */
	@SimpleFunction
	boolean 置WIFI状态(boolean enabled);

	/**
	 * 返回 Wi-Fi 是否已开启。
	 *
	 * @return 当前“是否已开启”状态
	 */
	@SimpleFunction
	boolean 是否已开启();

	/**
	 * 旧系统静态 IPv4 配置，前缀固定为 24；依赖系统隐藏接口支持。
	 *
	 * @param ssid 无线网络名称
	 * @param ip 目标 IP 地址
	 * @param gateway 网关地址
	 * @param DNS DNS 服务器地址
	 */
	@SimpleFunction
	void 置静态IP(String ssid, String ip, String gateway, String DNS);

	/**
	 * 连接指定网络；禁用其它为真时同时禁用其它已保存网络。
	 *
	 * @param networkId 网络配置编号
	 * @param disableOthers 是否禁用其它网络
	 * @return 操作成功时为 {@code true}，失败时为 {@code false}
	 */
	@SimpleFunction
	boolean 置指定WIFI状态(int networkId, boolean disableOthers);

	/**
	 * 返回系统状态：0 当前连接，1 禁用或不存在，2 启用。
	 *
	 * @param networkId 网络配置编号
	 * @return 系统状态：0 当前连接，1 禁用或不存在，2 启用
	 */
	@SimpleFunction
	int 取连接状态(int networkId);

	/**
	 * 请求扫描周围网络；返回是否成功启动扫描。
	 *
	 * @return 操作成功时为 {@code true}，失败时为 {@code false}
	 */
	@SimpleFunction
	boolean 开始扫描();

	/**
	 * 以“名称*地址*加密信息*信号*频率”返回结果，网络之间以 / 分隔。
	 *
	 * @return 以“名称*地址*加密信息*信号*频率”返回结果，网络之间以 / 分隔
	 */
	@SimpleFunction
	String 获取扫描结果();

	/**
	 * 添加网络并返回标识；类型：1 开放，2 WEP，3 WPA。
	 *
	 * @param ssid 无线网络名称
	 * @param password 认证密码
	 * @param type 1 为开放网络，2 为 WEP，3 为 WPA。
	 * @return 添加网络并返回标识；类型：1 开放，2 WEP，3 WPA
	 */
	@SimpleFunction
	int 添加网络(String ssid, String password, int type);

	/**
	 * 删除指定的已保存网络，返回是否成功。
	 *
	 * @param networkId 网络配置编号
	 * @return 操作成功时为 {@code true}，失败时为 {@code false}
	 */
	@SimpleFunction
	boolean 删除网络(int networkId);

	/**
	 * 获取本组件的 Wi-Fi 唤醒锁。
	 */
	@SimpleFunction
	void 锁定WIFI();

	/**
	 * 释放本组件的 Wi-Fi 唤醒锁。
	 */
	@SimpleFunction
	void 解锁WIFI();

	/**
	 * 按名称和密码启动热点；依赖系统隐藏接口支持。
	 *
	 * @param ssid 无线网络名称
	 * @param password 认证密码
	 */
	@SimpleFunction
	void 创建热点(String ssid, String password);

	/**
	 * 关闭热点；依赖系统隐藏接口支持。
	 */
	@SimpleFunction
	void 关闭热点();

	/**
	 * 返回当前连接的接入点地址。
	 *
	 * @return 当前连接的接入点地址
	 */
	@SimpleFunction
	String 取当前BSSID();

	/**
	 * 返回当前连接的网络名称。
	 *
	 * @return 当前连接的网络名称
	 */
	@SimpleFunction
	String 取当前SSID();

	/**
	 * 返回当前连接的网络标识。
	 *
	 * @return 当前连接的网络标识
	 */
	@SimpleFunction
	int 取当前networkid();

	/**
	 * 返回当前设备的内网 IPv4 地址。
	 *
	 * @return 当前设备的内网 IPv4 地址
	 */
	@SimpleFunction
	String 取内网IP();

	/**
	 * 返回系统提供的设备 Wi-Fi 地址。
	 *
	 * @return 系统提供的设备 Wi-Fi 地址
	 */
	@SimpleFunction
	String 取MAC地址();

	/**
	 * 返回 0～4 的信号档位；没有连接信息时为 0。
	 *
	 * @return  0～4 的信号档位；没有连接信息时为 0
	 */
	@SimpleFunction
	int 取当前信号强度();

	/**
	 * 返回系统提供的连接速度，单位为 Mbps。
	 *
	 * @return 系统提供的连接速度，单位为 Mbps
	 */
	@SimpleFunction
	int 取当前连接速度();

	/**
	 * 返回 DHCP 提供的网关地址。
	 *
	 * @return  DHCP 提供的网关地址
	 */
	@SimpleFunction
	String 取网关();

	/**
	 * 返回 DHCP 提供的子网掩码地址。
	 *
	 * @return  DHCP 提供的子网掩码地址
	 */
	@SimpleFunction
	String 取网络前缀();

	/**
	 * 返回 DHCP 提供的首选域名服务器地址。
	 *
	 * @return  DHCP 提供的首选域名服务器地址
	 */
	@SimpleFunction
	String 取DNS1();

	/**
	 * 返回 DHCP 提供的备用域名服务器地址。
	 *
	 * @return  DHCP 提供的备用域名服务器地址
	 */
	@SimpleFunction
	String 取DNS2();
}

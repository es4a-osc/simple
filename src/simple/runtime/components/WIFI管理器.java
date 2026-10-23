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
	 * 等待扫描期间，收到系统扫描结束通知时触发。
	 *
	 * <p>通知可能来自系统或其它应用的扫描；结果未更新时，读取到的仍可能是缓存。
	 *
	 * @param updated 本次扫描是否成功更新结果
	 */
	@SimpleEvent
	void 扫描完毕(boolean updated);

	/**
	 * Wi-Fi 开启状态变化时触发。
	 *
	 * @param enabled 是否已开启
	 */
	@SimpleEvent
	void 开启状态改变(boolean enabled);

	/**
	 * Wi-Fi 连接状态变化时触发。
	 *
	 * @param state 1 已连接，2 连接中，3 已断开，4 断开中，5 暂停，6 未知
	 */
	@SimpleEvent
	void 网络状态改变(int state);

	/**
	 * 系统报告 Wi-Fi 密码认证错误时触发。
	 *
	 * <p>Android 9（API 28）及以上系统不保证提供此通知。
	 */
	@SimpleEvent
	void 密码错误();

	/**
	 * 返回“添加网络”的最终结果；结果码不是系统网络标识。
	 *
	 * @param ssid 本次请求的网络名称
	 * @param result 0 成功，1 失败，2 已存在，-1 用户取消
	 */
	@SimpleEvent
	void 网络添加结果(String ssid, int result);

	/**
	 * 本地热点已就绪时触发，提供系统生成的实际连接信息。
	 *
	 * @param ssid 实际热点名称
	 * @param password 实际热点密码；开放热点为空字符串
	 */
	@SimpleEvent
	void 本地热点创建成功(String ssid, String password);

	/**
	 * 本地热点创建失败时触发。
	 *
	 * @param reason 1 无可用信道，2 通用错误，3 模式冲突，4 系统禁止共享；权限异常或系统不支持时为 2
	 */
	@SimpleEvent
	void 本地热点创建失败(int reason);

	/* 组件方法 */

	/**
	 * 返回本应用可以读取的已保存网络，每项为“名称|*|网络标识”。
	 *
	 * <p>Android 10（API 29）及以上普通应用读取受限，空列表不表示设备没有保存网络。
	 *
	 * @return 可读取的配置列表；无可读配置时为空数组
	 */
	@SimpleFunction
	String[] 取已保存网络列表();

	/**
	 * 返回指定已保存网络的系统标识；未找到或不可读取时返回 -1。
	 *
	 * <p>Android 10（API 29）及以上普通应用读取受限。
	 *
	 * @param ssid 网络名称
	 * @return 系统网络标识；未找到或不可读取时为 -1
	 */
	@SimpleFunction
	int 取已保存网络标识(String ssid);

	/**
	 * 打开系统 Wi-Fi 控制界面。
	 */
	@SimpleFunction
	void 打开WIFI设置();

	/**
	 * 请求开启或关闭 Wi-Fi，返回系统是否接受请求。
	 *
	 * <p>Android 10（API 29）及以上普通应用不能直接开关 Wi-Fi；可通过“打开WIFI设置”由用户操作。
	 *
	 * @param enabled 是否开启
	 * @return 系统接受请求时为真，否则为假；实际状态变化通过“开启状态改变”通知
	 */
	@SimpleFunction
	boolean 置WIFI状态(boolean enabled);

	/**
	 * 返回 Wi-Fi 当前是否已开启。
	 *
	 * @return 已开启时为真，否则为假
	 */
	@SimpleFunction
	boolean 是否已开启();

	/**
	 * 设置指定已保存网络的静态 IPv4，返回系统是否接受完整配置。
	 *
	 * <p>需要系统允许读取并修改网络配置及静态地址设置；Android 10（API 29）及以上普通应用不可用。
	 *
	 * @param ssid 网络名称
	 * @param ip 点分十进制 IPv4 地址
	 * @param prefixLength 网络前缀长度，范围为 0～32
	 * @param gateway 点分十进制 IPv4 网关
	 * @param DNS 点分十进制 IPv4 DNS 地址
	 * @return 配置已受理时为真；受限、参数无效或不可修改时为假
	 */
	@SimpleFunction
	boolean 置静态IP(String ssid, String ip, int prefixLength, String gateway, String DNS);

	/**
	 * 连接指定的已保存网络，返回系统是否接受请求。
	 *
	 * <p>Android 10（API 29）及以上普通应用不可用，需要系统允许管理网络配置。
	 *
	 * @param networkId 系统网络标识
	 * @param disableOthers 是否同时禁用其它已保存网络
	 * @return 系统接受请求时为真，否则为假
	 */
	@SimpleFunction
	boolean 置指定WIFI状态(int networkId, boolean disableOthers);

	/**
	 * 返回指定系统网络标识对应的状态。
	 *
	 * <p>配置不可读取时不能据此判断该网络实际是否已禁用。
	 *
	 * @param networkId 系统网络标识
	 * @return 0 当前连接，1 禁用、不存在或不可读取，2 启用
	 */
	@SimpleFunction
	int 取连接状态(int networkId);

	/**
	 * 请求扫描周围网络，返回系统是否接受请求。
	 *
	 * <p>需要定位权限及系统要求的定位服务状态，受扫描频率限制；已受理请求的结束通知通过“扫描完毕”返回。
	 *
	 * @return 已受理时为真；不可用、拒绝或已有等待中的扫描时为假
	 */
	@SimpleFunction
	boolean 开始扫描();

	/**
	 * 返回当前可读取的扫描结果，各项以 / 分隔，每项为“名称*地址*加密信息*信号*频率”。
	 *
	 * <p>没有新结果时可能返回系统缓存；读取需要定位权限及系统要求的定位服务状态。
	 *
	 * @return 序列化的扫描结果；无可读结果时为空字符串
	 */
	@SimpleFunction
	String 获取扫描结果();

	/**
	 * 异步请求添加已保存网络，返回是否受理；最终通过“网络添加结果”事件返回。
	 *
	 * <p>须在主线程调用，同一实例同时处理一个请求。部分系统需要用户确认；Android 10（API 29）普通应用不可用，WEP 和原始十六进制 PSK 只支持 API 30 以下系统。返回假时不触发结果事件。
	 *
	 * @param ssid 网络名称
	 * @param password 开放网络忽略；WPA/WPA2 密码，API 30 及以上为 8～63 个 ASCII 字符
	 * @param type 1 开放，2 WEP，3 WPA/WPA2（API 30 及以上仅 WPA2）
	 * @return 请求已受理时为真；不可用、忙碌或参数不支持时为假
	 */
	@SimpleFunction
	boolean 添加网络(String ssid, String password, int type);

	/**
	 * 删除指定的已保存网络，返回是否成功。
	 *
	 * <p>Android 10（API 29）及以上普通应用不可用，需要系统允许管理网络配置。
	 *
	 * @param networkId 系统网络标识
	 * @return 删除成功时为真，否则为假
	 */
	@SimpleFunction
	boolean 删除网络(int networkId);

	/**
	 * 获取本组件的 Wi-Fi 锁，持有期间可能增加耗电。
	 *
	 * <p>组件销毁时自动释放；此锁不能阻止用户主动关闭 Wi-Fi。
	 */
	@SimpleFunction
	void 锁定WIFI();

	/**
	 * 释放本组件持有的 Wi-Fi 锁，重复调用不产生额外操作。
	 */
	@SimpleFunction
	void 解锁WIFI();

	/**
	 * 按指定名称和密码请求创建系统热点，返回系统是否接受请求。
	 *
	 * <p>须在主线程调用，需要系统授予热点管理权限，普通应用可能被拒绝。返回真不表示热点已经就绪；此方法不触发本地热点的结果事件。
	 *
	 * @param ssid 热点名称
	 * @param password 8～63 个 ASCII 字符；空字符串或空引用表示开放热点
	 * @return 系统接受请求时为真，否则为假
	 */
	@SimpleFunction
	boolean 创建热点(String ssid, String password);

	/**
	 * 请求创建系统生成名称和密码的本地热点，不提供互联网共享。
	 *
	 * <p>需要 Android 8.0（API 26）及以上系统、定位权限及系统要求的定位服务状态，同一应用只能有一个活动请求；结果通过“本地热点创建成功”或“本地热点创建失败”返回。
	 */
	@SimpleFunction
	void 创建本地热点();

	/**
	 * 关闭当前实例的本地热点，或请求关闭系统热点，返回关闭操作是否成功受理。
	 *
	 * <p>须在主线程调用；关闭系统热点需要相应系统管理权限。本地热点尚在创建时会标记取消，等待系统结果后释放资源，此时返回假；不影响其它应用持有的资源。
	 *
	 * @return 本地资源释放调用已完成或系统接受关闭请求时为真，否则为假
	 */
	@SimpleFunction
	boolean 关闭热点();

	/**
	 * 返回当前连接的接入点 MAC 地址；缺失、不可读取或为系统占位地址时返回空字符串。
	 *
	 * @return 可读取的 BSSID；无可用地址时为空字符串
	 */
	@SimpleFunction
	String 取当前BSSID();

	/**
	 * 返回当前连接的网络名称；缺失或系统不允许读取时返回空字符串。
	 *
	 * @return 实际 SSID；无法读取时为空字符串
	 */
	@SimpleFunction
	String 取当前SSID();

	/**
	 * 返回当前 Wi-Fi 连接的系统网络标识；无连接或不可读取时返回 -1。
	 *
	 * @return 系统网络标识；不可读取时为 -1
	 */
	@SimpleFunction
	int 取当前网络标识();

	/**
	 * 返回当前 Wi-Fi 连接的内网 IPv4 地址；无地址时返回空字符串。
	 *
	 * @return 点分十进制 IPv4 地址；无地址时为空字符串
	 */
	@SimpleFunction
	String 取内网IP();

	/**
	 * 返回系统允许读取的设备 Wi-Fi MAC 地址；缺失、不可读取或为系统占位地址时返回空字符串。
	 *
	 * @return 可读取的设备 MAC 地址；无可用地址时为空字符串
	 */
	@SimpleFunction
	String 取MAC地址();

	/**
	 * 返回当前 Wi-Fi 信号的 0～4 档位。
	 *
	 * @return 0 最弱，4 最强；无连接信息时为 0
	 */
	@SimpleFunction
	int 取当前信号强度();

	/**
	 * 返回系统报告的当前 Wi-Fi 链路速度，单位为 Mbps。
	 *
	 * @return 链路速度；无连接信息时为 0
	 */
	@SimpleFunction
	int 取当前连接速度();

	/**
	 * 返回 DHCP 提供的 IPv4 网关地址；无 DHCP 信息时返回空字符串。
	 *
	 * @return 点分十进制网关地址；无信息时为空字符串
	 */
	@SimpleFunction
	String 取网关();

	/**
	 * 返回 DHCP 提供的 IPv4 子网掩码；无 DHCP 信息时返回空字符串。
	 *
	 * @return 点分十进制子网掩码；无信息时为空字符串
	 */
	@SimpleFunction
	String 取子网掩码();

	/**
	 * 返回 DHCP 提供的首选 IPv4 DNS 地址；无 DHCP 信息时返回空字符串。
	 *
	 * @return 点分十进制 DNS 地址；无信息时为空字符串
	 */
	@SimpleFunction
	String 取DNS1();

	/**
	 * 返回 DHCP 提供的备用 IPv4 DNS 地址；无 DHCP 信息时返回空字符串。
	 *
	 * @return 点分十进制 DNS 地址；无信息时为空字符串
	 */
	@SimpleFunction
	String 取DNS2();
}

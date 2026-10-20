package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.UsesPermissions;
import simple.runtime.collections.集合;

/**
 * 经典蓝牙设备发现与 RFCOMM 通信组件，不提供 BLE 通信。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
@SimpleComponent
@UsesPermissions(permissionNames =
	// 允许应用程序连接已配对的蓝牙设备。
	"android.permission.BLUETOOTH," +
	// 允许应用程序发现并配对蓝牙设备。
	"android.permission.BLUETOOTH_ADMIN," +
	// 允许应用程序访问精确的位置。
	"android.permission.ACCESS_FINE_LOCATION," +
	// 允许应用程序访问大致的位置。
	"android.permission.ACCESS_COARSE_LOCATION"
)
public interface 蓝牙通信器 extends 组件 {

	/* 组件事件 */

	/**
	 * 结果：1 开启成功，2 开启取消或失败，3 可发现设置成功，4 可发现设置取消或失败。
	 *
	 * @param result 1 开启成功，2 开启取消或失败，3 可发现设置成功，4 可发现设置取消或失败。
	 */
	@SimpleEvent
	void 蓝牙设置完毕(int result);

	/**
	 * 发现设备时触发，返回名称、地址及配对状态。
	 *
	 * @param name 要操作的名称
	 * @param address 资源或服务器地址
	 * @param bonded 设备是否已配对
	 */
	@SimpleEvent
	void 发现设备(String name, String address, boolean bonded);

	/**
	 * 成功时模式 1 为主动连接、2 为接收连接；失败时模式为 0。
	 *
	 * @param success 本次操作是否成功
	 * @param name 要操作的名称
	 * @param address 资源或服务器地址
	 * @param mode 1 为主动连接，2 为接收连接，失败时为 0。
	 */
	@SimpleEvent
	void 连接完毕(boolean success, String name, String address, int mode);

	/**
	 * 返回本次读取的数据块；字节流不保证一次事件对应一条完整业务消息。
	 *
	 * @param data 本次传入的数据
	 * @param name 要操作的名称
	 * @param address 资源或服务器地址
	 */
	@SimpleEvent
	void 收到数据(byte[] data, String name, String address);

	/**
	 * 数据成功写入连接后触发。
	 *
	 * @param data 本次传入的数据
	 * @param name 要操作的名称
	 * @param address 资源或服务器地址
	 */
	@SimpleEvent
	void 发送完毕(byte[] data, String name, String address);

	/**
	 * 当前连接断开时触发。
	 */
	@SimpleEvent
	void 连接断开();

	/* 组件方法 */

	/**
	 * 打开系统蓝牙设置界面。
	 */
	@SimpleFunction
	void 打开蓝牙设置();

	/**
	 * 返回设备是否具有蓝牙适配器。
	 *
	 * @return 当前“是否存在”状态
	 */
	@SimpleFunction
	boolean 是否存在();

	/**
	 * 返回蓝牙是否已开启。
	 *
	 * @return 当前“是否已开启”状态
	 */
	@SimpleFunction
	boolean 是否已开启();

	/**
	 * 请求用户开启蓝牙。
	 */
	@SimpleFunction
	void 开启蓝牙();

	/**
	 * 请求关闭蓝牙。
	 */
	@SimpleFunction
	void 关闭蓝牙();

	/**
	 * 请求让其它设备发现本设备。
	 */
	@SimpleFunction
	void 置可被发现();

	/**
	 * 模式 1 为聊天模式并监听连接；其它值使用串口服务进行主动连接。
	 *
	 * @param mode 1 为聊天模式，其它值为串口模式。
	 */
	@SimpleFunction
	void 置工作模式(int mode);

	/**
	 * 设置本组件后续连接的服务 UUID；空文本恢复当前模式的默认值。
	 *
	 * @param uuid 服务或特征 UUID
	 */
	@SimpleFunction
	void 置UUID(String uuid);

	/**
	 * 设置本机蓝牙名称，返回是否成功。
	 *
	 * @param name 要操作的名称
	 * @return 操作成功时为 {@code true}，失败时为 {@code false}
	 */
	@SimpleFunction
	boolean 置本机名称(String name);

	/**
	 * 返回本机蓝牙名称。
	 *
	 * @return 本机蓝牙名称
	 */
	@SimpleFunction
	String 取本机名称();

	/**
	 * 返回系统提供的本机蓝牙地址。
	 *
	 * @return 系统提供的本机蓝牙地址
	 */
	@SimpleFunction
	String 取本机地址();

	/**
	 * 返回已配对设备集合，每项为包含名称和地址的文本数组。
	 *
	 * @return 已配对设备集合，每项为包含名称和地址的文本数组
	 */
	@SimpleFunction
	集合 取已配对设备();

	/**
	 * 开始发现附近的经典蓝牙设备。
	 */
	@SimpleFunction
	void 搜索设备();

	/**
	 * 取消设备发现。
	 */
	@SimpleFunction
	void 停止搜索();

	/**
	 * 按地址发起 RFCOMM 连接。
	 *
	 * @param address 资源或服务器地址
	 */
	@SimpleFunction
	void 连接设备(String address);

	/**
	 * 通过当前连接发送字节数组。
	 *
	 * @param data 本次传入的数据
	 */
	@SimpleFunction
	void 发送数据(byte[] data);

	/**
	 * 关闭当前连接并停止连接任务。
	 */
	@SimpleFunction
	void 断开连接();

	/**
	 * 返回当前连接的设备名称。
	 *
	 * @return 当前连接的设备名称
	 */
	@SimpleFunction
	String 取已连接设备名称();

	/**
	 * 返回当前连接的设备地址。
	 *
	 * @return 当前连接的设备地址
	 */
	@SimpleFunction
	String 取已连接设备地址();
}

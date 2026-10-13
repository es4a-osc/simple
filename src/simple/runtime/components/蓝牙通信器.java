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
	 */
	@SimpleEvent
	void 蓝牙设置完毕(int result);

	@SimpleEvent
	void 发现设备(String name, String address, boolean bonded);

	/**
	 * 成功时模式 1 为主动连接、2 为接收连接；失败时模式为 0。
	 */
	@SimpleEvent
	void 连接完毕(boolean success, String name, String address, int mode);

	/**
	 * 返回本次读取的数据块；字节流不保证一次事件对应一条完整业务消息。
	 */
	@SimpleEvent
	void 收到数据(byte[] data, String name, String address);

	@SimpleEvent
	void 发送完毕(byte[] data, String name, String address);

	@SimpleEvent
	void 连接断开();

	/* 组件方法 */

	@SimpleFunction
	void 打开蓝牙设置();

	@SimpleFunction
	boolean 是否存在();

	@SimpleFunction
	boolean 是否已开启();

	@SimpleFunction
	void 开启蓝牙();

	@SimpleFunction
	void 关闭蓝牙();

	@SimpleFunction
	void 置可被发现();

	/**
	 * 模式 1 为聊天模式并监听连接；其它值使用串口服务进行主动连接。
	 */
	@SimpleFunction
	void 置工作模式(int mode);

	/**
	 * 设置本组件后续连接的服务 UUID；空文本恢复当前模式的默认值。
	 */
	@SimpleFunction
	void 置UUID(String uuid);

	@SimpleFunction
	boolean 置本机名称(String name);

	@SimpleFunction
	String 取本机名称();

	@SimpleFunction
	String 取本机地址();

	@SimpleFunction
	集合 取已配对设备();

	@SimpleFunction
	void 搜索设备();

	@SimpleFunction
	void 停止搜索();

	@SimpleFunction
	void 连接设备(String address);

	@SimpleFunction
	void 发送数据(byte[] data);

	@SimpleFunction
	void 断开连接();

	@SimpleFunction
	String 取已连接设备名称();

	@SimpleFunction
	String 取已连接设备地址();
}

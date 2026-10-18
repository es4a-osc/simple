package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.UsesPermissions;

/**
 * 绑定 UDP 端口接收数据报，使用同一 Socket 定向发送或发送广播。
 */
@SimpleComponent
@SimpleObject
@UsesPermissions(
	permissionNames =
		// 允许建立网络连接和绑定本地监听端口。
		"android.permission.INTERNET"
)
public interface UDP数据报 extends 组件 {

	/* 组件事件 */

	/**
	 * 收到一个 UDP 数据报时，通过组件创建线程的消息队列派发。
	 *
	 * @param data 字节数组。
	 * @param address 对端 IP 地址。
	 * @param port 对端端口号。
	 */
	@SimpleEvent
	void 收到数据(byte[] data, String address, int port);

	/* 组件方法 */

	/**
	 * 设置后续接收的缓冲区大小；超过缓冲区容量的数据报会被截断。
	 *
	 * @param size 接收缓冲区大小，单位字节，初始为 1024。
	 */
	@SimpleFunction
	void 置接收数据包大小(int size);

	/**
	 * 在后台线程绑定 UDP 端口；方法返回不代表绑定已经成功。
	 *
	 * @param port 本地监听端口号；0 表示由系统分配。
	 */
	@SimpleFunction
	void 开始监听(int port);

	/**
	 * 使用监听 Socket 同步发送数据报；须先开始监听，应在工作线程调用。
	 *
	 * @param data 字节数组。
	 * @param address 目标 IP 地址或主机名；空文本使用广播地址 255.255.255.255。
	 * @param port 对端端口号。
	 */
	@SimpleFunction
	void 发送数据(byte[] data, String address, int port);

	/**
	 * 清除监听标记、关闭 UDP Socket 并中断监听线程。
	 */
	@SimpleFunction
	void 停止监听();
}

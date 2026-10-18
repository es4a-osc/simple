package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.UsesPermissions;

/**
 * 监听 TCP 端口，接收客户连接并按地址和端口收发字节流。
 */
@SimpleComponent
@SimpleObject
@UsesPermissions(
	permissionNames =
		// 允许建立网络连接和绑定本地监听端口。
		"android.permission.INTERNET"
)
public interface TCP服务端 extends 组件 {

	/* 组件事件 */

	/**
	 * 接受客户连接后，通过组件创建线程的消息队列派发。
	 *
	 * @param address 对端 IP 地址。
	 * @param port 对端端口号。
	 */
	@SimpleEvent
	void 客户进入(String address, int port);

	/**
	 * 读取到客户发送的字节时触发；一次事件不保证对应一条完整消息。
	 *
	 * @param data 字节数组。
	 * @param address 对端 IP 地址。
	 * @param port 对端端口号。
	 */
	@SimpleEvent
	void 收到数据(byte[] data, String address, int port);

	/**
	 * 客户离开通知事件，派发行为沿用原组件实现。
	 *
	 * @param address 对端 IP 地址。
	 * @param port 对端端口号。
	 */
	@SimpleEvent
	void 客户离开(String address, int port);

	/* 组件方法 */

	/**
	 * 设置后续读取使用的缓冲区大小，不添加分包或组包协议。
	 *
	 * @param size 接收缓冲区大小，单位字节，初始为 1024；TCP 不以此划分消息边界。
	 */
	@SimpleFunction
	void 置接收数据包大小(int size);

	/**
	 * 在后台线程绑定并监听端口；方法返回不代表绑定已经成功。
	 *
	 * @param port 本地监听端口号；0 表示由系统分配。
	 */
	@SimpleFunction
	void 启动服务器(int port);

	/**
	 * 同步向地址和端口匹配的已连接客户发送字节；应在工作线程调用。
	 *
	 * @param data 字节数组。
	 * @param address 对端 IP 地址。
	 * @param port 对端端口号。
	 */
	@SimpleFunction
	void 发送数据(byte[] data, String address, int port);

	/**
	 * 关闭监听 Socket 并清空客户列表，已有客户连接的清理行为沿用原组件。
	 */
	@SimpleFunction
	void 关闭服务器();
}

package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.UsesPermissions;

/**
 * 异步连接 TCP 服务端，通过事件接收字节流并同步发送字节。
 */
@SimpleComponent
@SimpleObject
@UsesPermissions(
	permissionNames =
		// 允许建立网络连接和绑定本地监听端口。
		"android.permission.INTERNET"
)
public interface TCP客户端 extends 组件 {

	/* 组件事件 */

	/**
	 * 后台连接尝试结束后，通过组件创建线程的消息队列派发。
	 *
	 * @param result 连接成功为真，连接失败为假。
	 */
	@SimpleEvent
	void 连接完毕(boolean result);

	/**
	 * 读取到服务端发送的字节时触发；一次事件不保证对应一条完整消息。
	 *
	 * @param data 字节数组。
	 */
	@SimpleEvent
	void 收到数据(byte[] data);

	/**
	 * 连接读写异常产生断开通知时触发，正常结束和主动关闭行为沿用原组件。
	 */
	@SimpleEvent
	void 连接断开();

	/* 组件方法 */

	/**
	 * 设置读取缓冲区大小，并用于之后连接的 Socket 收发缓冲区设置。
	 *
	 * @param size 接收缓冲区大小，单位字节，初始为 1024；TCP 不以此划分消息边界。
	 */
	@SimpleFunction
	void 置接收数据包大小(int size);

	/**
	 * 在后台线程连接服务端，结果通过连接完毕事件通知。
	 *
	 * @param address 服务端 IP 地址或主机名。
	 * @param port 对端端口号。
	 * @param timeout 连接超时时间，单位毫秒；0 表示不限制等待时间。
	 */
	@SimpleFunction
	void 连接服务器(String address, int port, int timeout);

	/**
	 * 通过发送 TCP 紧急数据探测连接，沿用原组件的探测方式。
	 *
	 * @return 紧急数据发送未抛出 IO 异常时返回真，不表示对端应用已处理数据。
	 */
	@SimpleFunction
	boolean 取连接状态();

	/**
	 * 同步发送字节并刷新输出流；应在连接成功后由工作线程调用。
	 *
	 * @param data 字节数组。
	 */
	@SimpleFunction
	void 发送数据(byte[] data);

	/**
	 * 设置停止标记，关闭连接并中断连接和接收线程，关闭顺序沿用原组件。
	 */
	@SimpleFunction
	void 断开连接();
}

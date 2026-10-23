package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.UsesPermissions;

/**
 * 异步连接 TCP 服务端并收发原始字节流；全部事件在主线程交付。
 *
 * <p>组件销毁或宿主退出时自动释放网络资源。TCP 不提供消息边界；应用协议自行组包。
 * 主动关闭、重新连接或重新启动后，旧连接尚未交付的回调和发送将被取消。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
@UsesPermissions(permissionNames =
	// 允许连接 TCP 服务端并收发网络数据。
	"android.permission.INTERNET"
)
public interface TCP客户端 extends 组件 {

	/* 组件事件 */

	/**
	 * 连接尝试完成时触发；主动取消或被新连接替换的尝试不再回调。
	 *
	 * @param result 本次是否成功建立连接
	 */
	@SimpleEvent
	void 连接完毕(boolean result);

	/**
	 * 收到字节流片段时触发；一次事件不保证对应一条完整消息。
	 *
	 * @param data 独立的接收数据
	 */
	@SimpleEvent
	void 收到数据(byte[] data);

	/**
	 * 已建立的连接正常结束、异常中断或主动断开时触发一次；重连替换旧连接不另行通知。
	 */
	@SimpleEvent
	void 连接断开();

	/**
	 * 本次数据已写入本地 Socket 输出流时触发，不表示对端应用已经收到或处理。
	 *
	 * @param length 本次写出的字节数
	 */
	@SimpleEvent
	void 发送完毕(int length);

	/**
	 * 操作未受理或异步执行失败时触发；错误信息同时更新。
	 *
	 * @param operation 失败操作
	 * @param message 失败原因
	 */
	@SimpleEvent
	void 操作失败(String operation, String message);

	/* 组件属性 */

	/**
	 * 判断当前是否仍在尝试连接。
	 *
	 * @return 当前连接尝试尚未完成时为真
	 */
	@SimpleProperty
	boolean 正在连接();

	/**
	 * 获取最近一次操作失败的原因。
	 *
	 * @return 错误文本；新连接或发送请求前清空
	 */
	@SimpleProperty
	String 错误信息();

	/* 组件方法 */

	/**
	 * 设置应用层读取缓冲区，不改变 TCP 消息边界；正在阻塞的读取结束后采用新值。
	 *
	 * @param size 缓冲区大小，1～1048576 字节，初始为 1024
	 * @return 设置成功为真，参数无效或已销毁为假
	 */
	@SimpleFunction
	boolean 置接收数据包大小(int size);

	/**
	 * 在后台连接服务端并替换旧连接，结果以连接完毕事件为准。
	 *
	 * @param address 服务端 IP 或主机名
	 * @param port 服务端端口，1～65535
	 * @param timeout 连接超时毫秒，0 表示无限；不包含系统 DNS 查询耗时
	 * @return 连接请求受理为真，不代表已经连接成功
	 */
	@SimpleFunction
	boolean 连接服务器(String address, int port, int timeout);

	/**
	 * 查询本地已知状态，不发送探测数据；对端断网可能尚未被系统检测。
	 *
	 * @return 连接已建立且尚未检测到断开时为真
	 */
	@SimpleFunction
	boolean 取连接状态();

	/**
	 * 复制数据并异步排队发送，按入队顺序写出；队列满时拒绝，结果由发送完毕或操作失败事件报告。
	 *
	 * @param data 非空字节集，单次及未完成发送总量最多 4 MiB，最多 128 项待发送
	 * @return 成功入队为真，不表示对端已经收到
	 */
	@SimpleFunction
	boolean 发送数据(byte[] data);

	/**
	 * 取消正在连接的请求，或关闭已建立的连接；未完成发送及旧连接排队回调会被取消。
	 */
	@SimpleFunction
	void 断开连接();
}

package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.UsesPermissions;

/**
 * 异步监听 TCP 端口并按客户 IP 和端口收发原始字节流；全部事件在主线程交付。最多同时接入 64 个客户。
 *
 * <p>组件销毁或宿主退出时自动释放网络资源。TCP 不提供消息边界；应用协议自行组包。
 * 主动关闭、重新连接或重新启动后，旧连接尚未交付的回调和发送将被取消。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
@UsesPermissions(permissionNames =
	// 允许监听 TCP 端口并与客户端通信。
	"android.permission.INTERNET"
)
public interface TCP服务端 extends 组件 {

	/* 组件事件 */

	/**
	 * 监听尝试完成时触发；端口绑定成功后才报告成功。
	 *
	 * @param result 是否成功监听
	 * @param port 实际监听端口；失败时为 0
	 */
	@SimpleEvent
	void 启动完毕(boolean result, int port);

	/**
	 * 客户连接建立时触发。
	 *
	 * @param address 客户 IP 地址
	 * @param port 客户端口
	 */
	@SimpleEvent
	void 客户进入(String address, int port);

	/**
	 * 收到客户的字节流片段时触发；一次事件不保证对应一条完整消息。
	 *
	 * @param data 独立的接收数据
	 * @param address 客户 IP 地址
	 * @param port 客户端口
	 */
	@SimpleEvent
	void 收到数据(byte[] data, String address, int port);

	/**
	 * 监听期间某个客户正常结束或异常断开时触发一次；关闭整个服务器时以服务器关闭事件统一通知。
	 *
	 * @param address 客户 IP 地址
	 * @param port 客户端口
	 */
	@SimpleEvent
	void 客户离开(String address, int port);

	/**
	 * 已成功启动的监听结束并关闭全部客户连接后触发；启动中取消不产生此事件。
	 */
	@SimpleEvent
	void 服务器关闭();

	/**
	 * 本次数据已写入本地 Socket 输出流时触发，不表示客户应用已经收到或处理。
	 *
	 * @param length 本次写出的字节数
	 * @param address 客户 IP 地址
	 * @param port 客户端口
	 */
	@SimpleEvent
	void 发送完毕(int length, String address, int port);

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
	 * 判断是否已经成功绑定且仍在监听。
	 *
	 * @return 当前正在监听时为真
	 */
	@SimpleProperty
	boolean 正在监听();

	/**
	 * 获取系统实际绑定的监听端口。
	 *
	 * @return 实际端口，未监听时为 0
	 */
	@SimpleProperty
	int 监听端口();

	/**
	 * 获取当前已连接客户的数量。
	 *
	 * @return 当前连接数
	 */
	@SimpleProperty
	int 客户数量();

	/**
	 * 获取最近一次操作失败的原因。
	 *
	 * @return 错误文本；新启动或发送请求前清空
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
	 * 在后台绑定并监听端口，结果以启动完毕事件为准；已有启动请求或监听时拒绝重复启动。
	 *
	 * @param port 监听端口，0～65535；0 表示由系统分配
	 * @return 启动请求受理为真，不代表已经监听成功
	 */
	@SimpleFunction
	boolean 启动服务器(int port);

	/**
	 * 向匹配的客户复制并异步排队发送，按入队顺序写出；结果由发送完毕或操作失败事件报告。
	 *
	 * @param data 非空字节集，单次及未完成发送总量最多 4 MiB，最多 128 项待发送
	 * @param address 客户进入事件给出的 IP 地址，不进行主机名解析
	 * @param port 客户进入事件给出的端口
	 * @return 成功入队为真，找不到客户或队列满时为假
	 */
	@SimpleFunction
	boolean 发送数据(byte[] data, String address, int port);

	/**
	 * 取消启动或关闭监听和全部客户连接，取消未完成发送及旧监听的排队回调。
	 */
	@SimpleFunction
	void 关闭服务器();
}

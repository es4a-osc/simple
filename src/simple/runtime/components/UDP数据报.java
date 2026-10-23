package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.UsesPermissions;

/**
 * 异步绑定 UDP 端口，使用同一 Socket 收发独立数据报；全部事件在主线程交付。
 *
 * <p>UDP 不保证送达、顺序或去重，本组件不自动重发。接收处理积压时可能丢包。
 * 组件销毁或宿主退出时自动释放网络资源；不承诺后台服务保活。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
@UsesPermissions(permissionNames =
	// 允许绑定 UDP 端口并收发数据报。
	"android.permission.INTERNET"
)
public interface UDP数据报 extends 组件 {

	/* 组件事件 */

	/**
	 * 监听尝试完成时触发；主动取消的尝试不再回调。
	 *
	 * @param result 是否成功绑定
	 * @param port 实际监听端口，失败时为 0
	 */
	@SimpleEvent
	void 启动完毕(boolean result, int port);

	/**
	 * 收到一个完整数据报时触发；不会合并相邻数据报。
	 *
	 * @param data 独立的数据数组，可为零字节
	 * @param address 来源 IP 地址
	 * @param port 来源端口
	 */
	@SimpleEvent
	void 收到数据(byte[] data, String address, int port);

	/**
	 * 数据报已交给本地网络栈时触发，不表示对端已经收到。
	 *
	 * @param length 数据报字节数
	 * @param address 实际目标 IP
	 * @param port 目标端口
	 */
	@SimpleEvent
	void 发送完毕(int length, String address, int port);

	/**
	 * 成功启动的监听主动停止或异常结束时触发一次；销毁时不再派发事件。
	 */
	@SimpleEvent
	void 监听停止();

	/**
	 * 操作未受理或异步失败时触发；错误信息同时更新。
	 *
	 * @param operation 失败操作
	 * @param message 失败原因
	 */
	@SimpleEvent
	void 操作失败(String operation, String message);

	/* 组件属性 */

	/**
	 * 判断本地端口是否已成功绑定且尚未停止。
	 *
	 * @return 正在监听时为真
	 */
	@SimpleProperty
	boolean 正在监听();

	/**
	 * 获取当前实际绑定的本地端口。
	 *
	 * @return 尚未启动或已停止时为 0
	 */
	@SimpleProperty
	int 监听端口();

	/**
	 * 获取最近一次操作失败的原因。
	 *
	 * @return 错误文本；新启动或发送请求前清空
	 */
	@SimpleProperty
	String 错误信息();

	/* 组件方法 */

	/**
	 * 设置完整数据报的接收上限；超限时整包丢弃并触发操作失败。正在阻塞的接收结束后采用新值。
	 *
	 * @param size 上限为 1～65507 字节，初始 1024
	 * @return 设置成功为真，参数无效或已销毁为假
	 */
	@SimpleFunction
	boolean 置接收数据包大小(int size);

	/**
	 * 在后台绑定端口，结果以启动完毕事件为准；正在启动或监听时拒绝重复启动。
	 *
	 * @param port 本地端口为 0～65535，0 表示系统分配
	 * @return 请求受理为真，不代表已经绑定成功
	 */
	@SimpleFunction
	boolean 开始监听(int port);

	/**
	 * 复制数据并异步排队发送；须先成功监听。最多 128 项未完成发送，总量最多 4 MiB，超限拒绝。
	 *
	 * @param data 0～65507 字节，不允许空引用
	 * @param address 目标 IP 或主机名；空文本使用广播地址 255.255.255.255，广播是否可达取决于网络环境
	 * @param port 目标端口为 1～65535
	 * @return 入队成功为真；完成或失败通过事件报告
	 */
	@SimpleFunction
	boolean 发送数据(byte[] data, String address, int port);

	/**
	 * 取消启动或停止监听，关闭 Socket；未完成发送和旧会话尚未交付的回调会被取消。
	 */
	@SimpleFunction
	void 停止监听();
}

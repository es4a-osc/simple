package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.UsesPermissions;
import simple.runtime.collections.哈希表;

/**
 * 异步 HTTP/HTTPS 客户端，提供普通请求及流式文件提交。
 *
 * <p>配置和执行方法在主线程调用，每个实例同时受理一个请求，事件在主线程通知。
 * 请求头和超时持续有效，提交正文仅供下一次已受理请求使用。
 * 组件显式销毁或宿主 Activity 销毁后停止回调；进入后台或切换窗口不自动取消。</p>
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
@SimpleComponent
@UsesPermissions(permissionNames =
	"android.permission.INTERNET"
)
public interface HTTP客户端 extends 组件 {

	/* 组件事件 */

	/**
	 * 请求正文写出进度，约每 200 毫秒通知；不表示服务器已处理内容。
	 *
	 * @param sent 已写出字节数，文件表单包含字段和边界。
	 * @param total 正文总字节数；未知时为 -1。
	 */
	@SimpleEvent
	void 发送进度(long sent, long total);

	/**
	 * 响应正文读取进度，约每 200 毫秒通知。
	 *
	 * @param received 已读入字节数。
	 * @param total 响应总字节数；未知或压缩响应时为 -1。
	 */
	@SimpleEvent
	void 接收进度(long received, long total);

	/**
	 * 完整响应已保存，响应属性可读取；允许在此事件中开始下一请求。
	 *
	 * @param status HTTP 状态码，包括 3xx、4xx、5xx；业务成功需由应用判断。
	 */
	@SimpleEvent
	void 请求完成(int status);

	/**
	 * 已受理请求发生网络、文件或响应大小错误时触发。
	 *
	 * @param message 错误说明，不包含请求头及提交正文。
	 */
	@SimpleEvent
	void 请求失败(String message);

	/**
	 * 主动取消赢得终止状态时触发一次，不保证撤回服务器已接收的数据。
	 */
	@SimpleEvent
	void 请求取消();

	/* 组件属性 */

	/**
	 * 返回后续请求的连接超时。
	 *
	 * @return 毫秒数，默认为 10000，0 表示无限等待。
	 */
	@SimpleProperty
	int 连接超时();

	/**
	 * 设置后续请求的连接超时，不改变当前任务。
	 *
	 * @param value 非负毫秒数。
	 * @throws IllegalArgumentException 参数为负数。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER,
		initializer = "10000"
	)
	void 连接超时(int value);

	/**
	 * 返回后续请求的读取超时，此值不是写入或整体请求超时。
	 *
	 * @return 毫秒数，默认为 30000，0 表示无限等待。
	 */
	@SimpleProperty
	int 读入超时();

	/**
	 * 设置后续请求等待响应数据的超时。
	 *
	 * @param value 非负毫秒数。
	 * @throws IllegalArgumentException 参数为负数。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER,
		initializer = "30000"
	)
	void 读入超时(int value);

	/**
	 * 返回后续请求允许缓存的最大响应正文大小。
	 *
	 * @return 字节数，默认为 8388608；不限制流式提交文件的大小。
	 */
	@SimpleProperty
	int 响应大小上限();

	/**
	 * 设置响应正文上限，超过时通过请求失败通知，避免无限占用内存。
	 *
	 * @param value 正整数字节数。
	 * @throws IllegalArgumentException 参数小于 1。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER,
		initializer = "8388608"
	)
	void 响应大小上限(int value);

	/**
	 * 查询是否仍有任务或终止通知等待处理。
	 *
	 * @return 当前实例忙碌时为真。
	 */
	@SimpleProperty
	boolean 正在请求();

	/**
	 * 返回最近结束请求的 HTTP 状态。
	 *
	 * @return 完成请求的状态码；失败、取消或没有结果时为 0。
	 */
	@SimpleProperty
	int 响应代码();

	/**
	 * 返回最近完成请求的正文文本；发送下一请求时保留到新的终止通知。
	 *
	 * @return 按响应字符集解码的文本；缺失或不支持的字符集按 UTF-8 解码，无结果时为空文本。
	 */
	@SimpleProperty
	String 响应文本();

	/**
	 * 返回最近完成请求的原始正文副本。
	 *
	 * @return 响应字节集；无结果时为空字节集。
	 */
	@SimpleProperty
	byte[] 响应数据();

	/**
	 * 返回最近操作或失败的说明。
	 *
	 * @return 成功受理或完成时为空文本；取消时为取消说明。
	 */
	@SimpleProperty
	String 错误信息();

	/* 组件方法 */

	/**
	 * 设置后续请求的请求头，同名头按不区分大小写的规则替换。
	 *
	 * @param name 非空 HTTP 头名称。
	 * @param value 请求头值，不得包含换行。
	 * @throws IllegalArgumentException 名称或值无效，或试图设置由正文管理的类型、长度与传输编码头。
	 */
	@SimpleFunction
	void 置请求头(String name, String value);

	/**
	 * 清除后续请求的全部自定义请求头，不影响已开始的任务。
	 */
	@SimpleFunction
	void 清空请求头();

	/**
	 * 用 UTF-8 文本替换待提交正文，默认类型为 text/plain; charset=UTF-8。
	 *
	 * @param text 完整文本。
	 */
	@SimpleFunction
	void 置提交文本(String text);

	/**
	 * 用指定 MIME 类型的 UTF-8 文本替换待提交正文。
	 *
	 * @param text 完整文本。
	 * @param type MIME 类型，不含字符集参数，例如 application/json；组件添加 UTF-8 字符集。
	 * @throws IllegalArgumentException 内容或 MIME 类型无效。
	 */
	@SimpleFunction
	void 置提交文本(String text, String type);

	/**
	 * 复制字节集作为下一请求正文，类型为 application/octet-stream。
	 *
	 * @param data 完整字节内容；大文件优先使用文件提交。
	 * @throws IllegalArgumentException 数据为空引用。
	 */
	@SimpleFunction
	void 置提交数据(byte[] data);

	/**
	 * 将字段快照编码为 UTF-8 的 application/x-www-form-urlencoded 正文。
	 *
	 * @param fields 表单字段，值转换为文本；不保留传入哈希表的可变引用。
	 * @throws IllegalArgumentException 参数为空引用。
	 */
	@SimpleFunction
	void 置提交表单(哈希表 fields);

	/**
	 * 配置原始文件正文，实际打开和读取在工作线程执行。
	 *
	 * @param address 本地路径或已获读权限的 content URI；相对路径以应用内部文件目录为基准。
	 * @throws IllegalArgumentException 地址为空。
	 */
	@SimpleFunction
	void 置提交文件(String address);

	/**
	 * 配置单文件 multipart 表单，不附加其它字段。
	 *
	 * @param name 服务器接收文件的字段名，例如 file。
	 * @param address 本地路径或已获读权限的 content URI。
	 * @throws IllegalArgumentException 字段名或地址无效。
	 */
	@SimpleFunction
	void 置提交文件表单(String name, String address);

	/**
	 * 配置一个文件和附加文本字段组成的 multipart 表单，替换之前的正文。
	 *
	 * @param name 文件字段名。
	 * @param address 文件路径或 content URI。
	 * @param fields 附加字段快照。
	 * @throws IllegalArgumentException 字段、地址或附加参数无效。
	 */
	@SimpleFunction
	void 置提交文件表单(String name, String address, 哈希表 fields);

	/**
	 * 清除待提交正文；不同正文设置方法相互替换，不叠加。
	 */
	@SimpleFunction
	void 清空提交内容();

	/**
	 * 异步发送 GET 请求；如已配置正文则拒绝受理。
	 *
	 * @param address HTTP/HTTPS 完整网址。
	 * @return 已受理为真；忙碌、地址或正文配置错误时为假，原因见错误信息。
	 */
	@SimpleFunction
	boolean 发送请求(String address);

	/**
	 * 固定参数快照并异步发送，不自动跟随重定向或重试。
	 *
	 * @param method GET、POST、PUT、DELETE、HEAD 或 OPTIONS，不区分大小写。
	 * @param address HTTP/HTTPS 完整网址。
	 * @return 已受理为真，此时清除待提交正文；拒绝受理不触发终止事件，也不打断当前请求。
	 */
	@SimpleFunction
	boolean 发送请求(String method, String address);

	/**
	 * 尝试取消当前任务；阻塞关闭在后台处理，不保证立即断开网络或撤回服务器数据。
	 *
	 * @return 取消获得终止权时为真；无任务或任务已确定结束时为假。
	 */
	@SimpleFunction
	boolean 取消请求();

	/**
	 * 读取最近完成请求的响应头，同名多值以逗号及空格连接。
	 *
	 * @param name 响应头名称，不区分大小写。
	 * @return 响应头值，不存在或无完成结果时为空文本。
	 */
	@SimpleFunction
	String 取响应头(String name);
}

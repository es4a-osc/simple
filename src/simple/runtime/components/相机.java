package simple.runtime.components;

import simple.runtime.annotations.ManifestNodes;
import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.UsesPermissions;

/**
 * 调用系统拍摄、媒体选择和裁剪界面，并控制设备的持续照明。
 *
 * <p>异步请求须在主线程调用；同一实例同时处理一个任务。
 * 返回假表示未受理且不触发结果事件，原因通过错误信息读取。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
@SimpleComponent
@UsesPermissions(permissionNames =
	// 系统拍摄和持续照明需要相机权限；媒体选择使用用户授予的单项访问权限。
	"android.permission.CAMERA"
)
@ManifestNodes(rootXml =
	// 旧系统的临时拍摄输出需要共享存储写入权限，新系统不再请求它。
	"<uses-permission android:name=\"android.permission.WRITE_EXTERNAL_STORAGE\" android:maxSdkVersion=\"28\" />" +
	"<uses-feature android:name=\"android.hardware.camera\" android:required=\"false\" />" +
	"<uses-feature android:name=\"android.hardware.camera.autofocus\" android:required=\"false\" />"
)
public interface 相机 extends 组件 {

	/* 组件事件 */

	/**
	 * 拍照结果已完整保存到指定文件后触发。
	 *
	 * @param path 实际保存的绝对文件路径
	 */
	@SimpleEvent
	void 拍摄完毕(String path);

	/**
	 * 录像结果已完整保存到指定文件后触发。
	 *
	 * @param path 实际保存的绝对文件路径
	 */
	@SimpleEvent
	void 录像完毕(String path);

	/**
	 * 选择的照片或视频已复制到应用缓存后触发。
	 *
	 * @param path 缓存文件的绝对路径；需长期保留时应另行复制
	 */
	@SimpleEvent
	void 浏览完毕(String path);

	/**
	 * 裁剪结果已完整保存到应用缓存后触发；不另行触发浏览完毕。
	 *
	 * @param path 裁剪结果的绝对路径；需长期保留时应另行复制
	 */
	@SimpleEvent
	void 剪裁完毕(String path);

	/**
	 * 用户取消已受理的拍摄、选择或裁剪任务时触发。
	 *
	 * @param operation 拍照、录像、选择照片、选择视频或裁剪照片
	 */
	@SimpleEvent
	void 操作取消(String operation);

	/**
	 * 已受理的任务启动失败、结果无效或保存失败时触发。
	 *
	 * @param operation 失败的操作名称
	 * @param message 失败原因
	 */
	@SimpleEvent
	void 操作失败(String operation, String message);

	/**
	 * 系统已为共享存储中的文件更新媒体索引时触发。
	 *
	 * @param path 被扫描文件的绝对路径
	 * @param uri 系统返回的媒体内容 URI
	 */
	@SimpleEvent
	void 相册更新完毕(String path, String uri);

	/* 组件属性 */

	/**
	 * 返回最近一次未受理或异步失败的原因。
	 *
	 * @return 错误说明；最近一次请求正常受理时为空字符串
	 */
	@SimpleProperty
	String 错误信息();

	/**
	 * 判断是否仍在等待系统界面、复制媒体或更新媒体索引。
	 *
	 * @return 存在未结束的任务时为真
	 */
	@SimpleProperty
	boolean 正在处理();

	/* 组件方法 */

	/**
	 * 请求系统拍照，成功后将完整结果保存到指定文件。
	 *
	 * <p>需要相机权限及可处理拍照的系统应用；Android 9（API 28）及以下还需要外部存储写入权限。
	 * 取消、失败或空结果不会覆盖原文件。
	 *
	 * @param path 可写的绝对文件路径，不能是目录或内容 URI
	 * @return 请求已受理时为真；最终通过拍摄完毕、操作取消或操作失败通知
	 */
	@SimpleFunction
	boolean 开始拍照(String path);

	/**
	 * 请求系统录像，成功后将完整结果保存到指定文件。
	 *
	 * <p>需要相机权限及可处理录像的系统应用；Android 9（API 28）及以下还需要外部存储写入权限。
	 * 取消、失败或空结果不会覆盖原文件。
	 *
	 * @param path 可写的绝对文件路径，不能是目录或内容 URI
	 * @param quality 0 为低质量，1 为高质量
	 * @return 请求已受理时为真；最终通过录像完毕、操作取消或操作失败通知
	 */
	@SimpleFunction
	boolean 开始录像(String path, int quality);

	/**
	 * 请求扫描共享存储中系统可访问的媒体文件，更新媒体索引。
	 *
	 * <p>不会把应用私有文件复制到公共相册；私有目录不支持此操作。
	 *
	 * @param path 系统媒体服务可访问的共享存储绝对文件路径
	 * @return 请求已受理时为真；最终通过相册更新完毕或操作失败通知
	 */
	@SimpleFunction
	boolean 更新系统相册(String path);

	/**
	 * 打开系统文档界面选择一张照片，选择成功后复制到应用缓存。
	 *
	 * @return 请求已受理时为真；最终通过浏览完毕、操作取消或操作失败通知
	 */
	@SimpleFunction
	boolean 浏览相册();

	/**
	 * 打开系统文档界面选择一个视频，选择成功后复制到应用缓存。
	 *
	 * @return 请求已受理时为真；最终通过浏览完毕、操作取消或操作失败通知
	 */
	@SimpleFunction
	boolean 浏览相册视频();

	/**
	 * 选择照片后调用设备提供的裁剪应用，将结果保存到应用缓存。
	 *
	 * <p>Android 不保证安装裁剪应用；缺少处理程序会通过操作失败通知。
	 * Android 9（API 28）及以下还需要外部存储写入权限。
	 *
	 * @return 请求已受理时为真；最终通过剪裁完毕、操作取消或操作失败通知
	 */
	@SimpleFunction
	boolean 浏览相册并剪裁();

	/**
	 * 读取照片方向中的旋转角度，不执行镜像或修改照片。
	 *
	 * @param path 可读取的绝对文件路径
	 * @return 0、90、180 或 270 度；文件无效或无法读取时为 0
	 */
	@SimpleFunction
	int 取照片旋转角度(String path);

	/**
	 * 判断设备是否提供带闪光灯的摄像头。
	 *
	 * @return 存在闪光灯硬件时为真，不表示当前可以点亮
	 */
	@SimpleFunction
	boolean 是否存在闪光灯();

	/**
	 * 请求开启持续照明；需要相机权限及当前可用的闪光灯硬件。
	 *
	 * <p>退出前台或组件销毁时自动请求关闭；受其它相机任务占用时可能无法开启。
	 *
	 * @return 系统接受请求时为真；失败原因通过错误信息读取
	 */
	@SimpleFunction
	boolean 开启闪光灯();

	/**
	 * 请求关闭本实例开启的持续照明，重复调用不产生额外请求。
	 *
	 * @return 关闭请求完成或本实例没有开启照明时为真；失败原因通过错误信息读取
	 */
	@SimpleFunction
	boolean 关闭闪光灯();
}

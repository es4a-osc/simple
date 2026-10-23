package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.UsesPermissions;

/**
 * 嵌入窗口的摄像头预览组件，支持拍照和录像。
 *
 * <p>拍摄需相机权限，有声录像还需麦克风权限。文件输出使用尚不存在的可写绝对路径，
 * 建议使用应用私有目录；共享存储受系统访问范围和权限限制。
 * 预览就绪后才能拍摄；照片和录像以完成事件为准，错误由操作失败事件及错误信息报告。
 * 应用离开前台、表面销毁或关闭摄像头时取消未完成的拍摄并丢弃未交付的录像，
 * 返回前台后需重新开启。所有操作在主线程调用。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
@SimpleComponent
@UsesPermissions(permissionNames =
	// 允许使用摄像头进行预览、拍照和录像。
	"android.permission.CAMERA," +
	// 允许录制音频，用于有声录像。
	"android.permission.RECORD_AUDIO," +
	// 写入旧系统共享存储需要此权限；新系统仍受存储访问范围限制。
	"android.permission.WRITE_EXTERNAL_STORAGE"
)
public interface 摄像框 extends 可视组件 {

	/* 组件事件 */

	/**
	 * 自动对焦完成时触发，参数表示是否成功。
	 *
	 * @param success 本次操作是否成功
	 */
	@SimpleEvent
	void 对焦完毕(boolean success);

	/**
	 * 摄像头预览被单击时触发。
	 */
	@SimpleEvent
	void 被单击();

	/**
	 * 照片保存完成时触发，返回文件路径。
	 *
	 * @param path 已保存文件的绝对路径
	 */
	@SimpleEvent
	void 拍照完毕(String path);

	/**
	 * 照片编码完成时触发，返回照片字节数组。
	 *
	 * @param data 方向已校正的 JPEG 或 PNG 编码数据
	 */
	@SimpleEvent
	void 拍照完毕2(byte[] data);

	/**
	 * 按短边×长边逐行报告支持的预览尺寸。
	 *
	 * @param sizes 相机支持的尺寸列表
	 */
	@SimpleEvent
	void 获得预览尺寸列表(String sizes);

	/**
	 * 按短边×长边逐行报告支持的照片尺寸。
	 *
	 * @param sizes 相机支持的尺寸列表
	 */
	@SimpleEvent
	void 获得照片尺寸列表(String sizes);

	/**
	 * 摄像头和预览会话可接受拍摄操作时触发。
	 */
	@SimpleEvent
	void 预览就绪();

	/**
	 * 录像器实际开始录制后触发。
	 */
	@SimpleEvent
	void 录像开始();

	/**
	 * 显式停止录像且文件成功保存后触发。
	 *
	 * @param path 已保存 MP4 文件的绝对路径
	 */
	@SimpleEvent
	void 录像完毕(String path);

	/**
	 * 操作未完成时触发，不会同时报告该操作成功。
	 *
	 * @param operation 失败的操作
	 * @param message 错误原因
	 */
	@SimpleEvent
	void 操作失败(String operation, String message);

	/* 组件属性 */

	/**
	 * 获取最近一次操作失败的原因。
	 *
	 * @return 错误信息；新拍摄请求前清空
	 */
	@SimpleProperty
	String 错误信息();

	/* 组件方法 */

	/**
	 * 请求打开摄像头；类型：1 前置，2 后置；按请求宽高选择近似尺寸，就绪后触发预览就绪。
	 *
	 * @param type 1 为前置，2 为后置。
	 * @param width 请求的预览宽度。
	 * @param height 请求的预览高度。
	 */
	@SimpleFunction
	void 开启摄像头(int type, int width, int height);

	/**
	 * 请求自动对焦。
	 */
	@SimpleFunction
	void 开始对焦();

	/**
	 * 保存像素方向正确的照片；格式：1 JPEG，2 PNG；质量范围 0～100（PNG 为无损编码，忽略质量）。
	 *
	 * @param path 尚不存在的可写绝对文件路径
	 * @param format 1 为 JPEG，2 为 PNG。
	 * @param quality 编码质量，范围 0～100。
	 */
	@SimpleFunction
	void 拍摄照片(String path, int format, int quality);

	/**
	 * 拍摄并由拍照完毕2返回编码字节；格式和质量规则与拍摄照片一致。
	 *
	 * @param format 1 为 JPEG，2 为 PNG。
	 * @param quality 编码质量，范围 0～100。
	 */
	@SimpleFunction
	void 拍摄照片2(int format, int quality);

	/**
	 * 录制 MP4 视频；质量 1～7 为最低、240p、288p、480p、720p、1080p、最高（不超过 1080p），选择设备支持的近似档位。
	 *
	 * @param path 尚不存在的可写绝对文件路径
	 * @param quality 1～7 录像质量档位
	 * @param sound 是否录制声音。
	 */
	@SimpleFunction
	void 开始录像(String path, int quality, boolean sound);

	/**
	 * 停止录像；正常完成后触发录像完毕，过短或未开始的录像报告失败。
	 */
	@SimpleFunction
	void 停止录像();

	/**
	 * 开启当前摄像头的持续照明模式。
	 */
	@SimpleFunction
	void 开启闪光灯();

	/**
	 * 关闭当前摄像头的持续照明模式。
	 */
	@SimpleFunction
	void 关闭闪光灯();

	/**
	 * 关闭预览并释放资源，取消未交付的照片和录像，不覆盖已有文件。
	 */
	@SimpleFunction
	void 关闭摄像头();
}

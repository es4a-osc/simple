package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.UsesPermissions;

/**
 * 嵌入窗口的摄像头预览组件，支持拍照和录像。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
@SimpleComponent
@UsesPermissions(permissionNames =
	// 允许应用程序录制音频。
	"android.permission.RECORD_AUDIO," +
	// 允许应用程序访问摄像头。
	"android.permission.CAMERA," +
	// 声明应用程序访问闪光灯的权限。
	"android.permission.FLASHLIGHT," +
	// 允许应用程序读取外部存储。
	"android.permission.READ_EXTERNAL_STORAGE," +
	// 允许应用程序写入外部存储。
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
	 * @param path 文件或目录路径
	 */
	@SimpleEvent
	void 拍照完毕(String path);

	/**
	 * 照片编码完成时触发，返回照片字节数组。
	 *
	 * @param data 本次传入的数据
	 */
	@SimpleEvent
	void 拍照完毕2(byte[] data);

	/**
	 * 按高×宽逐行报告支持的预览尺寸。
	 *
	 * @param sizes 相机支持的尺寸列表
	 */
	@SimpleEvent
	void 获得预览尺寸列表(String sizes);

	/**
	 * 按高×宽逐行报告支持的照片尺寸。
	 *
	 * @param sizes 相机支持的尺寸列表
	 */
	@SimpleEvent
	void 获得照片尺寸列表(String sizes);

	/* 组件方法 */

	/**
	 * 类型：1 前置，2 后置；宽高用于选择竖屏拍摄和预览尺寸。
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
	 * 保存照片；格式：1 JPEG，2 PNG；质量范围 0～100。
	 *
	 * @param path 文件或目录路径
	 * @param format 1 为 JPEG，2 为 PNG。
	 * @param quality 编码质量，范围 0～100。
	 */
	@SimpleFunction
	void 拍摄照片(String path, int format, int quality);

	/**
	 * 返回照片字节；格式和质量规则与拍摄照片一致。
	 *
	 * @param format 1 为 JPEG，2 为 PNG。
	 * @param quality 编码质量，范围 0～100。
	 */
	@SimpleFunction
	void 拍摄照片2(int format, int quality);

	/**
	 * 质量 1～7：LOW、QVGA、CIF、480P、720P、1080P、HIGH。格式保留兼容，输出由设备档位决定。
	 *
	 * @param path 文件或目录路径
	 * @param format 保留兼容参数，输出格式由设备档位决定。
	 * @param quality 1～7：LOW、QVGA、CIF、480P、720P、1080P、HIGH。
	 * @param sound 是否录制声音。
	 */
	@SimpleFunction
	void 开始录像(String path, int format, int quality, boolean sound);

	/**
	 * 停止录像并释放录制资源。
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
	 * 关闭摄像头预览并释放拍摄资源。
	 */
	@SimpleFunction
	void 关闭摄像头();
}

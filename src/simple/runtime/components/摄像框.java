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

	@SimpleEvent
	void 对焦完毕(boolean success);

	@SimpleEvent
	void 被单击();

	@SimpleEvent
	void 拍照完毕(String path);

	@SimpleEvent
	void 拍照完毕2(byte[] data);

	/**
	 * 按高×宽逐行报告支持的预览尺寸。
	 */
	@SimpleEvent
	void 获得预览尺寸列表(String sizes);

	/**
	 * 按高×宽逐行报告支持的照片尺寸。
	 */
	@SimpleEvent
	void 获得照片尺寸列表(String sizes);

	/* 组件方法 */

	/**
	 * 类型：1 前置，2 后置；宽高用于选择竖屏拍摄和预览尺寸。
	 */
	@SimpleFunction
	void 开启摄像头(int type, int width, int height);

	@SimpleFunction
	void 开始对焦();

	/**
	 * 保存照片；格式：1 JPEG，2 PNG；质量范围 0～100。
	 */
	@SimpleFunction
	void 拍摄照片(String path, int format, int quality);

	/**
	 * 返回照片字节；格式和质量规则与拍摄照片一致。
	 */
	@SimpleFunction
	void 拍摄照片2(int format, int quality);

	/**
	 * 质量 1～7：LOW、QVGA、CIF、480P、720P、1080P、HIGH。格式保留兼容，输出由设备档位决定。
	 */
	@SimpleFunction
	void 开始录像(String path, int format, int quality, boolean sound);

	@SimpleFunction
	void 停止录像();

	@SimpleFunction
	void 开启闪光灯();

	@SimpleFunction
	void 关闭闪光灯();

	@SimpleFunction
	void 关闭摄像头();
}

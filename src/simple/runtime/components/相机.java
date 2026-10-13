package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.UsesPermissions;

/**
 * 调用系统相机和相册，并提供闪光灯控制的非可见组件。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
@SimpleComponent
@UsesPermissions(permissionNames =
	// 声明应用程序访问闪光灯的权限。
	"android.permission.FLASHLIGHT," +
	// 允许应用程序访问摄像头。
	"android.permission.CAMERA," +
	// 允许应用程序读取外部存储。
	"android.permission.READ_EXTERNAL_STORAGE," +
	// 允许应用程序写入外部存储。
	"android.permission.WRITE_EXTERNAL_STORAGE"
)
public interface 相机 extends 组件 {

	/* 组件事件 */

	@SimpleEvent
	void 拍摄完毕();

	@SimpleEvent
	void 录像完毕();

	/**
	 * 返回文件路径；媒体提供器未提供路径时复制到应用缓存。
	 */
	@SimpleEvent
	void 浏览完毕(String path);

	@SimpleEvent
	void 剪裁完毕(String path);

	/* 组件方法 */

	/**
	 * 调用系统相机，将结果复制到指定文件路径。
	 */
	@SimpleFunction
	void 开始拍照(String path);

	/**
	 * 调用系统录像；质量 0 为低质量，1 为高质量。
	 */
	@SimpleFunction
	void 开始录像(String path, int quality);

	@SimpleFunction
	void 更新系统相册(String path);

	@SimpleFunction
	void 浏览相册();

	@SimpleFunction
	void 浏览相册视频();

	/**
	 * 依赖设备提供裁剪应用；结果保存在应用缓存，通过剪裁完毕返回路径。
	 */
	@SimpleFunction
	void 浏览相册并剪裁();

	@SimpleFunction
	int 取照片旋转角度(String path);

	@SimpleFunction
	boolean 是否存在闪光灯();

	@SimpleFunction
	void 开启闪光灯();

	@SimpleFunction
	void 关闭闪光灯();
}

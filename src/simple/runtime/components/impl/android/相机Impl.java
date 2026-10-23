package simple.runtime.components.impl.android;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;
import android.webkit.MimeTypeMap;

import simple.runtime.android.MainActivity;
import simple.runtime.components.相机;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 使用系统媒体界面处理拍摄与选择，在后台复制结果，并管理持续照明。
 *
 * <p>最低支持 API 24；API 29 起裁剪输入先以待定项准备，完整后再交给外部应用。
 * 每个任务持有独立状态，清理完成后才分发终止事件，销毁后拒绝迟到结果。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class 相机Impl extends 组件Impl implements 相机,
		MainActivity.OnActivityResultListener, MainActivity.OnPauseListener {

	// 宿主直接继承 Activity，不使用 Fragment 的 16 位请求号编码；独立区间隔离其它组件。
	private static final AtomicInteger NEXT_REQUEST = new AtomicInteger(0x40000000);
	private final MainActivity activity;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private final CameraManager cameraManager;
	private final ExecutorService worker = Executors.newSingleThreadExecutor();
	private volatile Request pending;
	private volatile String lastError = "";
	private volatile boolean destroyed;
	private boolean listenersRegistered;
	private String torchId;

	/**
	 * 取得宿主和相机服务；监听器在首次操作时延后注册。
	 *
	 * @param container 所属组件容器
	 */
	public 相机Impl(组件容器 container) {
		super(container);
		activity = MainActivity.getContext();
		cameraManager = (CameraManager) activity.getSystemService(Activity.CAMERA_SERVICE);
	}

	/* 组件事件 */

	/**
	 * 分发完整照片文件的保存结果。
	 *
	 * @param path 实际保存路径
	 */
	@Override
	public void 拍摄完毕(String path) {
		EventDispatcher.dispatchEvent(this, "拍摄完毕", path);
	}

	/**
	 * 分发完整录像文件的保存结果。
	 *
	 * @param path 实际保存路径
	 */
	@Override
	public void 录像完毕(String path) {
		EventDispatcher.dispatchEvent(this, "录像完毕", path);
	}

	/**
	 * 分发复制到缓存的选择结果。
	 *
	 * @param path 缓存文件路径
	 */
	@Override
	public void 浏览完毕(String path) {
		EventDispatcher.dispatchEvent(this, "浏览完毕", path);
	}

	/**
	 * 分发裁剪后的缓存文件。
	 *
	 * @param path 裁剪结果路径
	 */
	@Override
	public void 剪裁完毕(String path) {
		EventDispatcher.dispatchEvent(this, "剪裁完毕", path);
	}

	/**
	 * 分发用户取消结果。
	 *
	 * @param operation 操作名称
	 */
	@Override
	public void 操作取消(String operation) {
		EventDispatcher.dispatchEvent(this, "操作取消", operation);
	}

	/**
	 * 分发已受理任务的失败原因。
	 *
	 * @param operation 操作名称
	 * @param message 失败说明
	 */
	@Override
	public void 操作失败(String operation, String message) {
		EventDispatcher.dispatchEvent(this, "操作失败", operation, message);
	}

	/**
	 * 分发媒体索引的真实更新结果。
	 *
	 * @param path 被扫描的文件
	 * @param uri 系统返回的内容 URI
	 */
	@Override
	public void 相册更新完毕(String path, String uri) {
		EventDispatcher.dispatchEvent(this, "相册更新完毕", path, uri);
	}

	/* 组件属性 */

	/**
	 * 读取最近的请求拒绝或任务失败原因。
	 *
	 * @return 错误说明；无错误时为空字符串
	 */
	@Override
	public String 错误信息() {
		return lastError;
	}

	/**
	 * 判断本实例是否仍持有未终止任务。
	 *
	 * @return 正在等待或处理结果时为真
	 */
	@Override
	public boolean 正在处理() {
		return !destroyed && pending != null;
	}

	/* 组件方法 */

	/**
	 * 检查输出路径后排队启动系统拍照。
	 *
	 * @param path 绝对文件路径
	 * @return 请求是否已进入队列
	 */
	@Override
	public boolean 开始拍照(String path) {
		return capture(path, false, 1);
	}

	/**
	 * 检查质量参数和输出路径后排队启动系统录像。
	 *
	 * @param path 绝对文件路径
	 * @param quality 0 为低质量，1 为高质量
	 * @return 请求是否已进入队列
	 */
	@Override
	public boolean 开始录像(String path, int quality) {
		if (quality != 0 && quality != 1) {
			return reject("录像质量只能为 0 或 1");
		}
		return capture(path, true, quality);
	}

	/**
	 * 扫描系统可访问的共享文件，原生回调返回 URI 后再报告成功。
	 *
	 * @param path 共享存储中的文件路径
	 * @return 系统扫描请求是否受理
	 */
	@Override
	public boolean 更新系统相册(String path) {
		if (!ready()) {
			return false;
		}
		final Request request;
		try {
			File file = checkedFile(path);
			String dataRoot = new File(activity.getApplicationInfo().dataDir).getCanonicalPath() + File.separator;
			String canonical = file.getCanonicalPath();
			if (!file.isFile() || !file.canRead() || canonical.startsWith(dataRoot)
					|| canonical.contains("/Android/data/") || canonical.contains("/Android/obb/")) {
				return reject("需要系统媒体服务可读取的共享存储文件，不能扫描应用私有目录");
			}
			request = new Request("更新相册", false, false, file);
		} catch (IOException | RuntimeException error) {
			return reject(errorMessage(error));
		}
		if (!reserve(request)) {
			return false;
		}
		try {
			MediaScannerConnection.scanFile(activity, new String[] {request.destination.getPath()}, null,
					(scannedPath, uri) -> postResult(request, () -> {
						if (uri == null) {
							fail(request, "系统没有为该文件建立媒体索引");
						} else {
							pending = null;
							lastError = "";
							cleanup(request, true);
							相册更新完毕(scannedPath, uri.toString());
						}
					}));
			return true;
		} catch (RuntimeException error) {
			pending = null;
			cleanup(request, false);
			return reject(errorMessage(error));
		}
	}

	/**
	 * 使用 API 19 起可用的文档选择协议读取一张照片，不依赖媒体数据库路径列。
	 *
	 * @return 选择请求是否已受理
	 */
	@Override
	public boolean 浏览相册() {
		return select(false, false);
	}

	/**
	 * 选择一个视频并在后台复制成可持续读取的缓存文件。
	 *
	 * @return 选择请求是否已受理
	 */
	@Override
	public boolean 浏览相册视频() {
		return select(true, false);
	}

	/**
	 * 选择照片后继续裁剪任务，两个系统阶段共用同一任务但使用不同请求号。
	 *
	 * @return 首阶段选择请求是否已受理
	 */
	@Override
	public boolean 浏览相册并剪裁() {
		return select(false, true);
	}

	/**
	 * 读取 EXIF 的旋转部分；镜像状态不在整数角度中表达。
	 *
	 * @param path 本地文件路径
	 * @return 0、90、180 或 270 度；不可读时为 0
	 */
	@Override
	public int 取照片旋转角度(String path) {
		if (path == null || path.isEmpty()) {
			return 0;
		}
		try {
			int orientation = new ExifInterface(path).getAttributeInt(
					ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
			switch (orientation) {
				case ExifInterface.ORIENTATION_ROTATE_90:
				case ExifInterface.ORIENTATION_TRANSVERSE:
					return 90;
				case ExifInterface.ORIENTATION_ROTATE_180:
				case ExifInterface.ORIENTATION_FLIP_VERTICAL:
					return 180;
				case ExifInterface.ORIENTATION_ROTATE_270:
				case ExifInterface.ORIENTATION_TRANSPOSE:
					return 270;
				default:
					return 0;
			}
		} catch (IOException | RuntimeException error) {
			return 0;
		}
	}

	/**
	 * 通过实际摄像头特征判断闪光灯能力，不把硬件声明当成可点亮结果。
	 *
	 * @return 存在带闪光灯的摄像头时为真
	 */
	@Override
	public boolean 是否存在闪光灯() {
		try {
			return findFlashCamera() != null;
		} catch (CameraAccessException | RuntimeException error) {
			return false;
		}
	}

	/**
	 * 使用 API 23 起的独立照明入口；本项目最低 API 24，无需占用旧 Camera 预览。
	 *
	 * @return 系统接受开启请求时为真
	 */
	@Override
	public boolean 开启闪光灯() {
		if (!mainThread() || destroyed || activity.isFinishing() || activity.isDestroyed()) {
			return reject("组件已销毁、宿主不可用或当前不在主线程");
		}
		try {
			String id = torchId == null ? findFlashCamera() : torchId;
			if (id == null) {
				return reject("设备没有可用的闪光灯摄像头");
			}
			cameraManager.setTorchMode(id, true);
			torchId = id;
			lastError = "";
			// 注册延后执行，避免事件处理程序重入时修改宿主正在遍历的监听集合。
			if (!handler.post(this::ensureListeners)) {
				stopTorch();
				return reject("无法登记照明的生命周期监听");
			}
			return true;
		} catch (CameraAccessException | RuntimeException error) {
			return reject(errorMessage(error));
		}
	}

	/**
	 * 只在本实例记录了开启请求时调用关闭接口。
	 *
	 * @return 关闭调用完成或本实例未开启照明时为真
	 */
	@Override
	public boolean 关闭闪光灯() {
		if (!mainThread()) {
			return reject("关闭闪光灯须在主线程调用");
		}
		lastError = "";
		return stopTorch();
	}

	/**
	 * 宿主退出前台时关闭持续照明，不取消仍在系统界面中的拍摄任务。
	 */
	@Override
	public void onPause() {
		stopTorch();
	}

	/**
	 * 立即使任务失效、停止后台复制，再延后注销监听并清理本实例资源。
	 */
	@Override
	public synchronized void 销毁() {
		if (destroyed) {
			return;
		}
		destroyed = true;
		Request previous = pending;
		pending = null;
		if (previous != null) {
			previous.cancelled = true;
		}
		worker.shutdownNow();
		Runnable dispose = () -> {
			cleanup(previous, false);
			stopTorch();
			if (listenersRegistered) {
				listenersRegistered = false;
				activity.removeOnActivityResultListener(this);
				activity.removeOnPauseListener(this);
			}
			super.销毁();
		};
		if (!handler.post(dispose)) {
			dispose.run();
		}
	}

	/**
	 * 处理本任务当前阶段的真实系统结果，重复或过期返回不会再次处理。
	 *
	 * @param requestCode 系统请求号
	 * @param resultCode 系统结果码
	 * @param data 选择结果；拍摄结果可为空
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Request request = pending;
		if (!active(request) || request.code == 0 || request.processing || requestCode != request.code) {
			return;
		}
		if (resultCode == Activity.RESULT_CANCELED) {
			pending = null;
			lastError = "";
			cleanup(request, false);
			操作取消(request.operation);
			return;
		}
		if (resultCode != Activity.RESULT_OK) {
			fail(request, "系统返回了未成功完成的操作结果");
			return;
		}
		request.processing = true;
		if (request.output != null) {
			execute(request, () -> {
				copyToFile(request, request.output, request.destination);
				postResult(request, () -> complete(request, request.destination));
			});
			return;
		}

		Uri selected = data == null ? null : data.getData();
		if (selected == null && data != null && data.getClipData() != null
				&& data.getClipData().getItemCount() == 1) {
			selected = data.getClipData().getItemAt(0).getUri();
		}
		if (selected == null || !"content".equals(selected.getScheme())) {
			fail(request, "系统未提供可读取的媒体内容 URI");
			return;
		}
		final Uri source = selected;
		if (request.crop) {
			execute(request, () -> prepareCrop(request, source));
		} else {
			execute(request, () -> {
				File result = newCacheFile(request, extension(source, request.video));
				copyToFile(request, source, result);
				postResult(request, () -> complete(request, result));
			});
		}
	}

	/* 内部辅助方法 */

	/**
	 * 校验请求共用条件，不改动正在进行的任务。
	 *
	 * @return 实例空闲、宿主有效且位于主线程时为真
	 */
	private boolean ready() {
		if (!mainThread() || destroyed || activity.isFinishing() || activity.isDestroyed()) {
			return reject("组件已销毁、宿主不可用或当前不在主线程");
		}
		if (pending != null) {
			return reject("上一项相机操作尚未结束");
		}
		lastError = "";
		return true;
	}

	/**
	 * 判断调用线程是否为宿主主线程。
	 *
	 * @return 当前位于主线程时为真
	 */
	private boolean mainThread() {
		return Looper.myLooper() == handler.getLooper();
	}

	/**
	 * 记录未受理原因，不伪造异步结果事件。
	 *
	 * @param message 拒绝原因
	 * @return 始终为假
	 */
	private boolean reject(String message) {
		lastError = message;
		return false;
	}

	/**
	 * 统一异常的可读说明，同时保留异常类型，便于判断权限或处理程序缺失。
	 *
	 * @param error 实际异常
	 * @return 非空错误说明
	 */
	private static String errorMessage(Exception error) {
		return error.getClass().getSimpleName() + "：" + String.valueOf(error.getMessage());
	}

	/**
	 * 在真正发起系统任务前登记监听；调用方保证在主线程中执行。
	 */
	private void ensureListeners() {
		if (!destroyed && !listenersRegistered) {
			activity.addOnActivityResultListener(this);
			activity.addOnPauseListener(this);
			listenersRegistered = true;
		}
	}

	/**
	 * 准备独立输出状态并排队发起拍摄，受理失败时回收已创建的临时项。
	 *
	 * @param path 目标文件路径
	 * @param video 是否录像
	 * @param quality 录像质量
	 * @return 请求是否已受理
	 */
	private boolean capture(String path, boolean video, int quality) {
		if (!ready()) {
			return false;
		}
		Request request = null;
		try {
			request = new Request(video ? "录像" : "拍照", video, false, checkedFile(path));
			createMedia(request, true, video ? "video/mp4" : "image/jpeg");
			Intent intent = new Intent(video ? MediaStore.ACTION_VIDEO_CAPTURE : MediaStore.ACTION_IMAGE_CAPTURE);
			if (video) {
				intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, quality);
			}
			return enqueue(request, withOutput(intent, request.output));
		} catch (IOException | RuntimeException error) {
			cleanup(request, false);
			return reject(errorMessage(error));
		}
	}

	/**
	 * 使用系统单文件选择协议，不直接请求访问整座媒体库。
	 *
	 * @param video 是否选择视频
	 * @param crop 是否接着裁剪
	 * @return 请求是否已受理
	 */
	private boolean select(boolean video, boolean crop) {
		if (!ready()) {
			return false;
		}
		Request request = new Request(crop ? "裁剪照片" : video ? "选择视频" : "选择照片", video, crop, null);
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType(video ? "video/*" : "image/*")
				.addCategory(Intent.CATEGORY_OPENABLE);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		return enqueue(request, intent);
	}

	/**
	 * 使请求进入主线程队列；事件重入时也不会立即修改宿主监听集合。
	 *
	 * @param request 独立任务状态
	 * @param intent 将要启动的系统意图
	 * @return 请求是否已排队
	 */
	private boolean enqueue(Request request, Intent intent) {
		if (!reserve(request)) {
			cleanup(request, false);
			return false;
		}
		if (!handler.post(() -> launch(request, intent))) {
			pending = null;
			cleanup(request, false);
			return reject("主线程未接受相机请求");
		}
		return true;
	}

	/**
	 * 与跨线程销毁互斥地登记任务，避免检查空闲后又覆盖已销毁状态。
	 *
	 * @param request 待登记任务
	 * @return 实例未销毁且仍空闲时为真
	 */
	private synchronized boolean reserve(Request request) {
		if (destroyed || pending != null) {
			return reject("组件已销毁或已有未结束的任务");
		}
		pending = request;
		return true;
	}

	/**
	 * 启动当前阶段，处理程序缺失或权限异常通过统一失败事件通知。
	 *
	 * @param request 当前任务
	 * @param intent 当前阶段的系统意图
	 */
	private void launch(Request request, Intent intent) {
		if (!active(request)) {
			cleanup(request, false);
			return;
		}
		try {
			ensureListeners();
			request.code = NEXT_REQUEST.getAndIncrement();
			if (request.code < 0) {
				throw new IllegalStateException("相机请求号已耗尽");
			}
			request.processing = false;
			// 待定媒体仅允许所有者读写；即使授予 URI 权限也不能直接交给外部相机或裁剪。
			makeExternal(request.input);
			activity.startActivityForResult(intent, request.code);
		} catch (IOException | RuntimeException error) {
			fail(request, errorMessage(error));
		}
	}

	/**
	 * 检查绝对路径并拒绝目录；不在请求阶段截断用户的已有文件。
	 *
	 * @param path 用户指定的文件路径
	 * @return 规范化后的绝对文件
	 * @throws IOException 无法规范化路径时
	 * @throws IllegalArgumentException 路径缺失、非绝对路径或指向目录时
	 */
	private static File checkedFile(String path) throws IOException {
		if (path == null || path.isEmpty()) {
			throw new IllegalArgumentException("文件路径不能为空");
		}
		File file = new File(path);
		if (!file.isAbsolute() || file.isDirectory()) {
			throw new IllegalArgumentException("需要绝对文件路径，不能是目录或内容 URI");
		}
		return file.getCanonicalFile();
	}

	/**
	 * 创建外部应用可写的临时媒体项；仅待复制的裁剪输入使用 API 29 的待定状态。
	 *
	 * @param request 资源所属任务
	 * @param output 是否作为最终拍摄输出；否则作为裁剪的独立输入副本
	 * @param mime 实际内容类型
	 * @return 新建的临时媒体 URI
	 * @throws IOException 任务失效或系统未创建媒体项时
	 */
	private Uri createMedia(Request request, boolean output, String mime) throws IOException {
		checkActive(request);
		ContentValues values = new ContentValues();
		String suffix = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
		values.put(MediaStore.MediaColumns.DISPLAY_NAME,
				"simple-camera-" + System.nanoTime() + "." + (suffix == null ? "jpg" : suffix));
		values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
		// 输出必须直接允许外部应用写入，不能将空输出从待定切换为公开，否则扫描可能移除空项。
		if (!output && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			values.put(MediaStore.MediaColumns.IS_PENDING, 1);
		}
		Uri uri = activity.getContentResolver().insert(request.video
				? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
		if (uri == null) {
			throw new IOException("系统未创建临时拍摄输出");
		}
		synchronized (request) {
			if (destroyed || request.cancelled) {
				deleteMedia(uri);
				throw new IOException("相机任务已失效");
			}
			if (output) {
				request.output = uri;
			} else {
				request.input = uri;
			}
		}
		return uri;
	}

	/**
	 * 解除已准备媒体的待定状态，允许获得 URI 授权的外部应用实际读写。
	 *
	 * <p>API 24～28 没有待定列，保留原生插入和授权路径；临时项在任务终止时删除。
	 *
	 * @param uri 本任务拥有的临时媒体，可为空
	 * @throws IOException 系统未能解除待定状态时
	 */
	private void makeExternal(Uri uri) throws IOException {
		if (uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			ContentValues values = new ContentValues();
			values.put(MediaStore.MediaColumns.IS_PENDING, 0);
			if (activity.getContentResolver().update(uri, values, null, null) != 1) {
				throw new IOException("系统未允许外部应用访问临时媒体");
			}
		}
	}

	/**
	 * 传递内容 URI 和临时读写授权，不暴露 file URI。
	 *
	 * @param intent 拍摄或裁剪意图
	 * @param output 本应用拥有的输出 URI
	 * @return 带有输出位置和授权的意图
	 */
	private static Intent withOutput(Intent intent, Uri output) {
		intent.putExtra(MediaStore.EXTRA_OUTPUT, output);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		intent.setClipData(ClipData.newRawUri("相机临时输出", output));
		return intent;
	}

	/**
	 * 复制用户选择的照片为独立裁剪输入，避免把原照片的写权限授予裁剪应用。
	 *
	 * @param request 裁剪任务
	 * @param source 用户选择且已获得读取授权的 URI
	 * @throws IOException 读取、复制或创建临时输出失败时
	 */
	private void prepareCrop(Request request, Uri source) throws IOException {
		String mime = activity.getContentResolver().getType(source);
		Uri input = createMedia(request, false, mime == null ? "image/jpeg" : mime);
		try (InputStream stream = openInput(request, source);
				OutputStream output = activity.getContentResolver().openOutputStream(input, "w")) {
			copyStream(request, stream, output);
		} finally {
			forgetInput(request);
		}
		File destination = newCacheFile(request, "jpg");
		request.destination = destination;
		Uri output = createMedia(request, true, "image/jpeg");
		Intent crop = new Intent("com.android.camera.action.CROP").setDataAndType(input, "image/*");
		crop.putExtra("scale", true);
		crop.putExtra("return-data", false);
		postResult(request, () -> launch(request, withOutput(crop, output)));
	}

	/**
	 * 根据真实 MIME 类型选择安全扩展名，不假定选择的照片一定为 JPEG。
	 *
	 * @param source 选择结果 URI
	 * @param video 是否选择视频
	 * @return 不含路径字符的扩展名
	 */
	private String extension(Uri source, boolean video) {
		String mime = activity.getContentResolver().getType(source);
		String value = mime == null ? null : MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
		return value != null && value.matches("[a-zA-Z0-9]{1,10}") ? value : video ? "mp4" : "jpg";
	}

	/**
	 * 为当前任务创建缓存文件，销毁或失败时仍由任务持有并清理。
	 *
	 * @param request 当前任务
	 * @param extension 已校验的扩展名
	 * @return 缓存文件
	 * @throws IOException 缓存目录不可写或任务已失效时
	 */
	private File newCacheFile(Request request, String extension) throws IOException {
		checkActive(request);
		File directory = new File(activity.getCacheDir(), "simple-camera");
		if (!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory()) {
			throw new IOException("无法创建相机缓存目录");
		}
		File file = File.createTempFile("media-", "." + extension, directory);
		synchronized (request) {
			request.cacheFile = file;
			checkActive(request);
		}
		return file;
	}

	/**
	 * 在工作线程复制完整结果；空流、读取失败或任务取消时恢复原目标文件。
	 *
	 * @param request 当前任务
	 * @param source 内容 URI
	 * @param destination 目标文件
	 * @throws IOException 读取、写入、任务取消或空结果时
	 */
	private void copyToFile(Request request, Uri source, File destination) throws IOException {
		checkActive(request);
		File parent = destination.getParentFile();
		if (parent != null && !parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) {
			throw new IOException("无法创建输出目录");
		}
		// 暂存文件与目标位于同一目录；全部读写、同步和关闭成功后才原子替换。
		File staging = File.createTempFile(".simple-camera-", ".tmp", parent);
		try {
			try (InputStream input = openInput(request, source);
					FileOutputStream output = new FileOutputStream(staging)) {
				copyStream(request, input, output);
				output.getFD().sync();
			}
			checkActive(request);
			// API 21 起可用，覆盖项目全部支持版本；重命名失败必须抛出，不能报告保存成功。
			Os.rename(staging.getPath(), destination.getPath());
		} catch (ErrnoException error) {
			throw new IOException("无法提交媒体文件", error);
		} finally {
			forgetInput(request);
			if (staging.exists() && !staging.delete()) {
				Log.w("相机", "无法删除媒体暂存文件：" + staging.getName());
			}
		}
	}

	/**
	 * 跟踪当前读取流，组件销毁时可主动关闭阻塞中的媒体读取。
	 *
	 * @param request 当前任务
	 * @param uri 输入内容 URI
	 * @return 已登记的输入流
	 * @throws IOException 媒体不可读或任务已失效时
	 */
	private InputStream openInput(Request request, Uri uri) throws IOException {
		InputStream input = activity.getContentResolver().openInputStream(uri);
		if (input == null) {
			throw new IOException("系统未提供可用的媒体流");
		}
		synchronized (request) {
			if (destroyed || request.cancelled) {
				closeStream(input);
				throw new IOException("相机任务已失效");
			}
			request.openInput = input;
		}
		return input;
	}

	/**
	 * 输入流由读写阶段关闭后移除任务引用。
	 *
	 * @param request 当前任务
	 */
	private void forgetInput(Request request) {
		synchronized (request) {
			request.openInput = null;
		}
	}

	/**
	 * 在清理路径关闭未结束的读取，关闭异常不阻断其它资源回收。
	 *
	 * @param stream 需要关闭的流，可为空
	 */
	private static void closeStream(Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException | RuntimeException error) {
				Log.w("相机", "关闭媒体读取流失败", error);
			}
		}
	}

	/**
	 * 流式复制媒体，定期检查取消状态；不把零字节输出当作成功。
	 *
	 * @param request 当前任务
	 * @param input 可读取的输入流
	 * @param output 可写入的输出流
	 * @throws IOException 输入输出不可用、空内容、复制失败或任务取消时
	 */
	private void copyStream(Request request, InputStream input, OutputStream output) throws IOException {
		if (input == null || output == null) {
			throw new IOException("系统未提供可用的媒体流");
		}
		long length = 0;
		byte[] buffer = new byte[32768];
		int count;
		while ((count = input.read(buffer)) != -1) {
			checkActive(request);
			output.write(buffer, 0, count);
			length += count;
		}
		checkActive(request);
		if (length == 0) {
			throw new IOException("系统返回了空媒体文件");
		}
	}

	/**
	 * 在独立工作线程完成媒体复制，失败结果统一回到主线程。
	 *
	 * @param request 当前任务
	 * @param work 需要执行的输入输出工作
	 */
	private void execute(Request request, MediaWork work) {
		try {
			worker.execute(() -> {
				try {
					work.run();
				} catch (IOException | RuntimeException error) {
					postResult(request, () -> fail(request, errorMessage(error)));
				} finally {
					if (destroyed || request.cancelled) {
						cleanup(request, false);
					}
				}
			});
		} catch (RuntimeException error) {
			fail(request, errorMessage(error));
		}
	}

	/**
	 * 仅给仍有效的任务派发主线程处理，排队期间销毁也会清理临时资源。
	 *
	 * @param request 结果所属任务
	 * @param completion 主线程处理动作
	 */
	private void postResult(Request request, Runnable completion) {
		if (!handler.post(() -> {
			if (active(request)) {
				completion.run();
			} else {
				cleanup(request, false);
			}
		})) {
			cleanup(request, false);
		}
	}

	/**
	 * 清除任务等待状态并转交缓存所有权，之后才调用用户的成功事件。
	 *
	 * @param request 已完成的任务
	 * @param file 实际结果文件
	 */
	private void complete(Request request, File file) {
		pending = null;
		lastError = "";
		cleanup(request, true);
		if (request.crop) {
			剪裁完毕(file.getAbsolutePath());
		} else if (request.destination == null) {
			浏览完毕(file.getAbsolutePath());
		} else if (request.video) {
			录像完毕(file.getAbsolutePath());
		} else {
			拍摄完毕(file.getAbsolutePath());
		}
	}

	/**
	 * 结束当前任务并报告一次可观察的失败，保留调用者原有文件。
	 *
	 * @param request 失败任务
	 * @param message 失败原因
	 */
	private void fail(Request request, String message) {
		if (active(request)) {
			pending = null;
			lastError = message;
			cleanup(request, false);
			Log.e("相机", request.operation + "失败：" + message);
			操作失败(request.operation, message);
		}
	}

	/**
	 * 判断异步结果是否仍归当前有效任务所有。
	 *
	 * @param request 待检查任务
	 * @return 当前实例仍等待该任务时为真
	 */
	private boolean active(Request request) {
		return request != null && !destroyed && !request.cancelled && pending == request;
	}

	/**
	 * 在后台输入输出期间检查销毁和中断。
	 *
	 * @param request 当前任务
	 * @throws IOException 任务已销毁或被中断时
	 */
	private void checkActive(Request request) throws IOException {
		if (destroyed || request.cancelled || Thread.currentThread().isInterrupted()) {
			throw new IOException("相机任务已取消或组件已销毁");
		}
	}

	/**
	 * 释放任务拥有的媒体行与授权；缓存仅在成功转交后保留，用户目标文件从不在此删除。
	 *
	 * @param request 待清理任务，可为空
	 * @param keepCache 是否将成功缓存文件的所有权转交调用者
	 */
	private void cleanup(Request request, boolean keepCache) {
		if (request == null) {
			return;
		}
		Uri output;
		Uri input;
		File cache;
		InputStream stream;
		synchronized (request) {
			request.cancelled = true;
			output = request.output;
			input = request.input;
			request.output = null;
			request.input = null;
			cache = request.cacheFile;
			stream = request.openInput;
			request.openInput = null;
			if (keepCache) {
				request.cacheFile = null;
			}
		}
		closeStream(stream);
		deleteMedia(output);
		deleteMedia(input);
		if (!keepCache && cache != null && cache.exists() && !cache.delete()) {
			Log.w("相机", "无法删除未交付的缓存文件：" + cache.getName());
		}
	}

	/**
	 * 尝试回收本应用拥有的临时媒体项，不删除用户选择的原始资源。
	 *
	 * @param uri 临时内容 URI，可为空
	 */
	private void deleteMedia(Uri uri) {
		if (uri != null) {
			try {
				activity.revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
			} catch (RuntimeException error) {
				Log.w("相机", "无法撤销临时媒体授权", error);
			}
			try {
				activity.getContentResolver().delete(uri, null, null);
			} catch (RuntimeException error) {
				Log.w("相机", "无法清理临时媒体项", error);
			}
		}
	}

	/**
	 * 优先选择后置且带闪光灯的摄像头，其次使用其它带闪光灯的摄像头。
	 *
	 * @return 摄像头标识；没有可用特征时为空
	 * @throws CameraAccessException 系统无法读取摄像头列表或特征时
	 */
	private String findFlashCamera() throws CameraAccessException {
		if (cameraManager == null) {
			return null;
		}
		String fallback = null;
		for (String id : cameraManager.getCameraIdList()) {
			CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
			if (Boolean.TRUE.equals(characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE))) {
				if (Integer.valueOf(CameraCharacteristics.LENS_FACING_BACK)
						.equals(characteristics.get(CameraCharacteristics.LENS_FACING))) {
					return id;
				}
				fallback = id;
			}
		}
		return fallback;
	}

	/**
	 * 请求关闭本实例记录的持续照明；失败时保留标识以便再次释放。
	 *
	 * @return 关闭调用完成或无持有标识时为真
	 */
	private boolean stopTorch() {
		if (torchId == null) {
			return true;
		}
		try {
			cameraManager.setTorchMode(torchId, false);
			torchId = null;
			return true;
		} catch (CameraAccessException | RuntimeException error) {
			Log.w("相机", "关闭闪光灯失败", error);
			return reject(errorMessage(error));
		}
	}

	/**
	 * 保存一次拍摄、选择或媒体扫描的独立状态。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	private static final class Request {
		private final String operation;
		private final boolean video;
		private final boolean crop;
		private volatile boolean cancelled;
		private int code;
		private boolean processing;
		private File destination;
		private Uri output;
		private Uri input;
		private File cacheFile;
		private InputStream openInput;

		/**
		 * 创建任务，不提前申请系统资源。
		 *
		 * @param operation 操作名称
		 * @param video 是否处理视频
		 * @param crop 是否执行裁剪
		 * @param destination 用户目标文件，选择任务为空
		 */
		private Request(String operation, boolean video, boolean crop, File destination) {
			this.operation = operation;
			this.video = video;
			this.crop = crop;
			this.destination = destination;
		}
	}

	/**
	 * 可报告输入输出失败的后台工作。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	private interface MediaWork {
		/**
		 * 执行一次媒体处理。
		 *
		 * @throws IOException 读取、复制或任务取消时
		 */
		void run() throws IOException;
	}
}

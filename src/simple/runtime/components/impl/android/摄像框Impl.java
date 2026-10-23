package simple.runtime.components.impl.android;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.util.Size;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import simple.runtime.android.MainActivity;
import simple.runtime.components.摄像框;
import simple.runtime.components.组件;
import simple.runtime.components.组件容器;
import simple.runtime.events.EventDispatcher;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 摄像头预览与拍摄实现，协调 Surface、Camera2 和 MediaRecorder 的资源生命周期。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class 摄像框Impl extends 视图组件 implements 摄像框,
		MainActivity.OnPauseListener, MainActivity.OnDestroyListener {

	private LinearLayout layout;
	private volatile CameraView cameraView;
	private int cameraFacing;
	private int requestedWidth;
	private int requestedHeight;
	private volatile boolean flashlight;
	private volatile boolean destroyed;
	private final MainActivity activity;
	private final Handler handler;
	private final ExecutorService encoder;
	private final HandlerThread cameraThread;
	private final Handler cameraHandler;
	private int openingCameras;
	private final java.util.concurrent.atomic.AtomicBoolean processing = new java.util.concurrent.atomic.AtomicBoolean();
	private boolean attached;
	private volatile String lastError = "";

	/**
	 * 创建预览容器，并延后登记宿主回调，避免修改正在分发的监听集合。
	 *
	 * @param container 所属组件容器
	 */
	public 摄像框Impl(组件容器 container) {
		super(container);
		activity = MainActivity.getContext();
		handler = new Handler(Looper.getMainLooper());
		encoder = Executors.newSingleThreadExecutor();
		cameraThread = new HandlerThread("SimpleCamera");
		cameraThread.start();
		cameraHandler = new Handler(cameraThread.getLooper());
		handler.post(() -> {
			if (!destroyed) {
				activity.addOnPauseListener(this);
				activity.addOnDestroyListener(this);
				attached = true;
			}
		});
	}

	@Override
	protected View createView() {
		// 基类构造期间调用，此处只创建视图，不读取子类初始化字段。
		layout = new LinearLayout(MainActivity.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setGravity(Gravity.CENTER);
		layout.setBackgroundColor(组件.颜色_黑);
		return layout;
	}

	/* 组件事件 */

	@Override
	public void 对焦完毕(boolean success) {
		EventDispatcher.dispatchEvent(this, "对焦完毕", success);
	}

	@Override
	public void 被单击() {
		EventDispatcher.dispatchEvent(this, "被单击");
	}

	@Override
	public void 拍照完毕(String path) {
		EventDispatcher.dispatchEvent(this, "拍照完毕", path);
	}

	@Override
	public void 拍照完毕2(byte[] data) {
		EventDispatcher.dispatchEvent(this, "拍照完毕2", data);
	}

	@Override
	public void 获得预览尺寸列表(String sizes) {
		EventDispatcher.dispatchEvent(this, "获得预览尺寸列表", sizes);
	}

	@Override
	public void 获得照片尺寸列表(String sizes) {
		EventDispatcher.dispatchEvent(this, "获得照片尺寸列表", sizes);
	}

	@Override
	public void 预览就绪() {
		EventDispatcher.dispatchEvent(this, "预览就绪");
	}

	@Override
	public void 录像开始() {
		EventDispatcher.dispatchEvent(this, "录像开始");
	}

	@Override
	public void 录像完毕(String path) {
		EventDispatcher.dispatchEvent(this, "录像完毕", path);
	}

	@Override
	public void 操作失败(String operation, String message) {
		EventDispatcher.dispatchEvent(this, "操作失败", operation, message);
	}

	@Override
	public String 错误信息() {
		return lastError;
	}

	/* 组件方法 */

	@Override
	public void 开启摄像头(int type, int width, int height) {
		requireMainThread();
		lastError = "";
		if (destroyed || !activity.isInForeground() || activity.isFinishing()) {
			fail("开启摄像头", "组件已销毁或应用未在前台");
			return;
		}
		if (Build.VERSION.SDK_INT >= 23 && activity.checkSelfPermission("android.permission.CAMERA")
				!= PackageManager.PERMISSION_GRANTED) {
			fail("开启摄像头", "未授予相机权限");
			return;
		}
		if ((type != 1 && type != 2) || width <= 0 || height <= 0) {
			fail("开启摄像头", "摄像头类型应为 1（前置）或 2（后置），宽高必须大于零");
			return;
		}

		关闭摄像头();
		cameraFacing = type == 1 ? 1 : 0;
		requestedWidth = width;
		requestedHeight = height;

		cameraView = new ModernCameraView(activity);

		layout.addView(cameraView, new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		cameraView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View view) {
				被单击();
			}
		});
	}

	@Override
	public void 开始对焦() {
		requireMainThread();
		lastError = "";
		if (cameraView != null) {
			try {
				cameraView.focus();
			} catch (RuntimeException failure) {
				fail("开始对焦", failure.toString());
			}
		} else {
			fail("开始对焦", "摄像头尚未开启");
		}
	}

	@Override
	public void 拍摄照片(String path, int format, int quality) {
		requireMainThread();
		lastError = "";
		if (cameraView != null) {
			try {
				cameraView.takePicture(path, format, quality, false);
			} catch (RuntimeException failure) {
				fail("拍摄照片", failure.toString());
			}
		} else {
			fail("拍摄照片", "摄像头尚未开启");
		}
	}

	@Override
	public void 拍摄照片2(int format, int quality) {
		requireMainThread();
		lastError = "";
		if (cameraView != null) {
			try {
				cameraView.takePicture(null, format, quality, true);
			} catch (RuntimeException failure) {
				fail("拍摄照片", failure.toString());
			}
		} else {
			fail("拍摄照片", "摄像头尚未开启");
		}
	}

	@Override
	public void 开始录像(String path, int quality, boolean sound) {
		requireMainThread();
		lastError = "";
		if (cameraView != null) {
			try {
				cameraView.takeVideo(path, quality, sound);
			} catch (RuntimeException failure) {
				fail("开始录像", failure.toString());
			}
		} else {
			fail("开始录像", "摄像头尚未开启");
		}
	}

	@Override
	public void 停止录像() {
		requireMainThread();
		if (cameraView != null) {
			try {
				cameraView.stopVideo();
			} catch (RuntimeException failure) {
				fail("停止录像", failure.toString());
			}
		}
	}

	@Override
	public void 开启闪光灯() {
		requireMainThread();
		lastError = "";
		if (cameraView != null) {
			try {
				cameraView.applyFlashlight(true);
			} catch (RuntimeException failure) {
				fail("设置闪光灯", failure.toString());
			}
		} else {
			flashlight = false;
			fail("设置闪光灯", "摄像头尚未开启");
		}
	}

	@Override
	public void 关闭闪光灯() {
		requireMainThread();
		lastError = "";
		if (cameraView != null) {
			try {
				cameraView.applyFlashlight(false);
			} catch (RuntimeException failure) {
				fail("设置闪光灯", failure.toString());
			}
		} else {
			flashlight = false;
			fail("设置闪光灯", "摄像头尚未开启");
		}
	}

	@Override
	public void 关闭摄像头() {
		requireMainThread();
		flashlight = false;
		CameraView previous = cameraView;
		cameraView = null;
		if (previous != null) {
			previous.close();
			layout.removeView(previous);
		}
	}

	@Override
	public void 销毁() {
		requireMainThread();
		if (destroyed) {
			return;
		}
		destroyed = true;
		关闭摄像头();
		encoder.shutdownNow();
		cameraHandler.post(this::retireCameraThread);
		// 编码结果仍需回到主线程清理临时文件，不能清空这些待处理回调。
		handler.post(() -> {
			if (attached) {
				activity.removeOnPauseListener(this);
				activity.removeOnDestroyListener(this);
				attached = false;
			}
		});
		super.销毁();
	}

	@Override
	public void onPause() {
		关闭摄像头();
	}

	@Override
	public void onDestroy() {
		销毁();
	}

	/* 内部辅助方法 */

	/**
	 * 销毁后等待尚未回调的打开请求结束，再停止线程，避免丢失迟到设备的关闭回调。
	 */
	private void retireCameraThread() {
		if (destroyed && openingCameras == 0) {
			cameraThread.quitSafely();
		}
	}

	/**
	 * 拒绝跨线程操作视图和相机状态。
	 */
	private static void requireMainThread() {
		if (Looper.myLooper() != Looper.getMainLooper()) {
			throw new IllegalStateException("摄像框只能在主线程调用");
		}
	}

	/**
	 * 记录错误并在当前调用栈结束后通知，避免资源清理期间事件重入。
	 *
	 * @param operation 失败操作
	 * @param message 原因
	 */
	private void fail(String operation, String message) {
		lastError = message;
		Log.w("摄像框", operation + "：" + message);
		emit(cameraView, "操作失败", operation, message);
	}

	/**
	 * 将后台结果转回主线程，并过滤已关闭或已替换后端的迟到通知。
	 *
	 * @param owner 发起通知的预览，可为空
	 * @param event 事件名称
	 * @param arguments 事件参数
	 */
	private void emit(CameraView owner, String event, Object... arguments) {
		handler.post(() -> {
			if (!destroyed && owner == cameraView) {
				EventDispatcher.dispatchEvent(this, event, arguments);
			}
		});
	}

	/**
	 * 从当前 Activity 所在显示器读取旋转角，不使用默认显示器猜测多屏位置。
	 *
	 * @return Surface 旋转常量
	 */
	private int displayRotation() {
		if (Build.VERSION.SDK_INT >= 30) {
			android.view.Display display = activity.getDisplay();
			return display == null ? Surface.ROTATION_0 : display.getRotation();
		}
		return legacyDisplayRotation();
	}

	/**
	 * API 30 以下尚无 Context.getDisplay，保留默认显示器入口。
	 *
	 * @return Surface 旋转常量
	 */
	@SuppressWarnings("deprecation")
	private int legacyDisplayRotation() {
		return activity.getWindowManager().getDefaultDisplay().getRotation();
	}

	/**
	 * 统一前后置照片和录像的顺时针旋转角。
	 *
	 * @param sensor 传感器安装角
	 * @param rotation Surface 旋转常量
	 * @param front 是否前置
	 * @return 0、90、180 或 270 度
	 */
	private static int imageRotation(int sensor, int rotation, boolean front) {
		int degrees = rotation == Surface.ROTATION_90 ? 90 : rotation == Surface.ROTATION_180 ? 180
				: rotation == Surface.ROTATION_270 ? 270 : 0;
		return (sensor + (front ? degrees : -degrees) + 360) % 360;
	}

	/**
	 * 在编码线程旋转和压缩照片；交付前再次确认拍摄所属的预览没有被关闭或替换。
	 *
	 * @param owner 发起拍摄的预览
	 * @param data 未旋转 JPEG 数据
	 * @param rotation 顺时针旋转角
	 * @param path 输出路径；返回字节时为空
	 * @param format 1 JPEG、2 PNG
	 * @param quality 压缩质量
	 * @param returnBytes 是否交付字节
	 */
	private void processPicture(CameraView owner, byte[] data, int rotation, String path,
			int format, int quality, boolean returnBytes) {
		if (destroyed || cameraView != owner || !processing.compareAndSet(false, true)) {
			return;
		}
		try {
			encoder.execute(() -> {
				File staged = null;
				try {
					byte[] encoded = encodePicture(data, rotation, format, quality);
					if (destroyed || cameraView != owner || Thread.currentThread().isInterrupted()) {
						handler.post(() -> processing.set(false));
						return;
					}
					File target = null;
					if (!returnBytes) {
						target = outputFile(path);
						staged = File.createTempFile(".simple-photo-", ".tmp", target.getParentFile());
						try (FileOutputStream stream = new FileOutputStream(staged)) {
							stream.write(encoded);
							stream.getFD().sync();
						}
					}
					final File pending = staged;
					final File destination = target;
					handler.post(() -> {
						processing.set(false);
						if (destroyed || cameraView != owner) {
							discard(pending);
							return;
						}
						try {
							if (returnBytes) {
								拍照完毕2(encoded);
							} else {
								publishFile(pending, destination);
								拍照完毕(destination.getAbsolutePath());
							}
						} catch (IOException failure) {
							discard(pending);
							fail("拍摄照片", failure.getMessage());
						}
					});
				} catch (IOException | RuntimeException failure) {
					discard(staged);
					handler.post(() -> {
						processing.set(false);
						if (!destroyed && cameraView == owner) {
							fail("拍摄照片", failure.toString());
						}
					});
				}
		});
		} catch (java.util.concurrent.RejectedExecutionException stopped) {
			processing.set(false);
		}
	}

	/**
	 * 将相机原始 JPEG 转为像素方向正确的目标格式，不依赖读取端识别 EXIF。
	 *
	 * @param data 原始 JPEG
	 * @param rotation 顺时针角度
	 * @param format 1 JPEG、2 PNG
	 * @param quality 编码质量
	 * @return 旋转后编码的数据
	 * @throws IOException 解码或编码失败
	 */
	private static byte[] encodePicture(byte[] data, int rotation, int format, int quality) throws IOException {
		Bitmap source = BitmapFactory.decodeByteArray(data, 0, data.length);
		if (source == null) {
			throw new IOException("无法解码摄像头照片");
		}
		Bitmap rotated = null;
		try {
			Matrix matrix = new Matrix();
			matrix.setRotate(rotation);
			rotated = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			if (!rotated.compress(format == 1 ? Bitmap.CompressFormat.JPEG : Bitmap.CompressFormat.PNG,
					quality, output)) {
				throw new IOException("无法编码照片");
			}
			return output.toByteArray();
		} finally {
			if (rotated != null && rotated != source) {
				rotated.recycle();
			}
			source.recycle();
		}
	}

	/**
	 * 将已完成的临时文件交付到尚不存在的目标路径。
	 *
	 * @param staged 同目录临时文件
	 * @param target 输出目标
	 * @throws IOException 文件为空、目标冲突或移动失败
	 */
	private static void publishFile(File staged, File target) throws IOException {
		if (staged == null || staged.length() == 0 || target.exists()) {
			throw new IOException("输出文件为空或目标已存在");
		}
		if (Build.VERSION.SDK_INT >= 26) {
			java.nio.file.Files.move(staged.toPath(), target.toPath());
		} else if (!staged.renameTo(target)) {
			throw new IOException("无法交付文件");
		}
	}

	/**
	 * 清理本组件创建的临时文件，不删除调用方原有文件。
	 *
	 * @param staged 可为空的临时文件
	 */
	private static void discard(File staged) {
		if (staged != null && staged.exists() && !staged.delete()) {
			Log.w("摄像框", "无法清理临时文件：" + staged);
		}
	}

	/**
	 * 统一表面生命周期、异步拍摄约束和等比显示。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	private abstract class CameraView extends SurfaceView implements SurfaceHolder.Callback {

		final SurfaceHolder holder;
		volatile int bufferWidth;
		volatile int bufferHeight;
		volatile int sensorOrientation;

		/**
		 * 创建并登记预览表面；相机只在表面可用后打开。
		 *
		 * @param context 宿主上下文
		 */
		CameraView(Context context) {
			super(context);
			holder = getHolder();
			holder.addCallback(this);
			setKeepScreenOn(true);
		}

		/**
		 * 更新预览缓冲区的尺寸，并按安装角和屏幕方向保持比例。
		 *
		 * @param width 缓冲区宽度
		 * @param height 缓冲区高度
		 * @param sensor 传感器安装角
		 */
		final void fit(int width, int height, int sensor) {
			bufferWidth = width;
			bufferHeight = height;
			sensorOrientation = sensor;
			post(this::requestLayout);
		}

		@Override
		protected void onMeasure(int widthSpec, int heightSpec) {
			super.onMeasure(widthSpec, heightSpec);
			int width = getMeasuredWidth();
			int height = getMeasuredHeight();
			if (bufferWidth > 0 && bufferHeight > 0 && width > 0 && height > 0) {
				boolean swapped = imageRotation(sensorOrientation, displayRotation(), cameraFacing == 1) % 180 != 0;
				float x = (float) width / (swapped ? bufferHeight : bufferWidth);
				float y = (float) height / (swapped ? bufferWidth : bufferHeight);
				float scale = Math.min(x, y);
				setScaleX(scale / x);
				setScaleY(scale / y);
			}
		}

		/**
		 * 请求一次自动对焦，结果由对焦事件报告。
		 */
		abstract void focus();

		/**
		 * 请求照片，禁止在录制和前次照片未完成时重复提交。
		 *
		 * @param path 目标文件
		 * @param format 1 JPEG、2 PNG
		 * @param quality 0～100
		 * @param returnBytes 是否返回字节
		 */
		abstract void takePicture(String path, int format, int quality, boolean returnBytes);

		/**
		 * 开始 MP4 录像。
		 *
		 * @param path 目标文件
		 * @param quality 1～7 录像档位
		 * @param sound 是否收音
		 */
		abstract void takeVideo(String path, int quality, boolean sound);

		/**
		 * 停止录像，成功交付后触发录像完毕。
		 */
		abstract void stopVideo();

		/**
		 * 应用持续照明设置。
		 *
		 * @param enabled 是否开启
		 */
		abstract void applyFlashlight(boolean enabled);

		/**
		 * 取消待处理操作并释放录像器、会话和相机。
		 */
		abstract void close();
	}

	/**
	 * 解析并准备拍摄输出文件。
	 *
	 * @param path 尚不存在的可写绝对文件路径
	 * @return 已经校验的输出文件
	 * @throws IOException 访问文件、流或网络资源失败时
	 */
	private static File outputFile(String path) throws IOException {
		if (path == null || path.isEmpty()) {
			throw new IOException("输出路径不能为空");
		}

		File file = new File(path);
		if (!file.isAbsolute() || file.exists()) {
			throw new IOException("必须使用尚不存在的绝对文件路径");
		}
		File parent = file.getAbsoluteFile().getParentFile();

		if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
			throw new IOException("无法创建输出目录");
		}

		return file;
	}

	/**
	 * Camera2 预览与录制后端；原生相机状态归专用线程，界面事件与编码分别排队。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	private final class ModernCameraView extends CameraView {

		private CameraManager manager;
		private final Handler handler = cameraHandler;
		private CameraCharacteristics characteristics;
		private volatile CameraDevice camera;
		private CameraCaptureSession session;
		private ImageReader reader;
		private CaptureRequest.Builder repeating;
		private MediaRecorder recorder;
		private File videoTarget;
		private File videoTemp;
		private volatile boolean closed;
		private volatile boolean ready;
		private boolean recording;
		private boolean picturePending;
		private boolean focusing;
		private boolean reported;
		private boolean opening;
		private volatile int revision;
		private int pictureRotation;
		private int photoFormat;
		private int photoQuality;
		private String photoPath;
		private boolean photoBytes;
		private String cameraId;
		private Size previewSize;
		private Size pictureSize;
		private Runnable timeout;
		private Runnable focusTimeout;
		private Runnable photoTimeout;
		private int continuousMode = CaptureRequest.CONTROL_AF_MODE_OFF;

		/**
		 * 保存相机服务；表面创建前不申请硬件。
		 *
		 * @param context 宿主上下文
		 */
		ModernCameraView(Context context) {
			super(context);
			// 相机服务发现也可能阻塞，延后到专用线程执行。
		}

		/**
		 * 只为本次预览交付错误，不能让旧线程污染新预览的错误状态。
		 *
		 * @param operation 失败操作
		 * @param message 原因
		 */
		private void reportFailure(String operation, String message) {
			摄像框Impl.this.handler.post(() -> {
				if (!destroyed && cameraView == this) {
					lastError = message;
					Log.w("摄像框", operation + "：" + message);
					操作失败(operation, message);
				}
			});
		}

		/**
		 * 判断异步回调是否仍属于当前预览。
		 *
		 * @return 当前实例尚未关闭且仍为活动预览
		 */
		private boolean current() {
			return !closed && !destroyed && cameraView == this;
		}

		@Override
		public void surfaceCreated(SurfaceHolder surface) {
			if (Looper.myLooper() != handler.getLooper()) {
				摄像框Impl.this.handler.postDelayed(() -> {
					if (current() && camera == null) {
						fatal("打开摄像头超时");
					}
				}, 15000);
				handler.post(() -> surfaceCreated(surface));
				return;
			}
			if (!current()) {
				return;
			}
			try {
				manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
				if (manager == null) {
					throw new IOException("系统没有相机服务");
				}
				int facing = cameraFacing == 1 ? CameraCharacteristics.LENS_FACING_FRONT
						: CameraCharacteristics.LENS_FACING_BACK;
				for (String id : manager.getCameraIdList()) {
					if (!current()) {
						return;
					}
					CameraCharacteristics info = manager.getCameraCharacteristics(id);
					if (!current()) {
						return;
					}
					Integer lens = info.get(CameraCharacteristics.LENS_FACING);
					if (lens != null && lens == facing) {
						cameraId = id;
						characteristics = info;
						break;
					}
				}
				if (!current()) {
					return;
				}
				if (cameraId == null) {
					throw new IOException("设备没有所选摄像头");
				}
				StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
				if (map == null) {
					throw new IOException("设备没有报告相机输出能力");
				}
				Size[] previews = map.getOutputSizes(SurfaceHolder.class);
				Size[] pictures = map.getOutputSizes(ImageFormat.JPEG);
				previewSize = chooseSize(previews, requestedWidth, requestedHeight);
				pictureSize = chooseSize(pictures, requestedWidth, requestedHeight);
				Integer orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
				sensorOrientation = orientation == null ? 0 : orientation;
				pictureRotation = imageRotation(sensorOrientation, displayRotation(), cameraFacing == 1);
				fit(previewSize.getWidth(), previewSize.getHeight(), sensorOrientation);
				post(() -> {
					if (current()) {
						holder.setFixedSize(previewSize.getWidth(), previewSize.getHeight());
					}
				});
				int[] modes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
				if (contains(modes, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)) {
					continuousMode = CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
				} else if (contains(modes, CaptureRequest.CONTROL_AF_MODE_AUTO)) {
					continuousMode = CaptureRequest.CONTROL_AF_MODE_AUTO;
				}
				reader = ImageReader.newInstance(pictureSize.getWidth(), pictureSize.getHeight(), ImageFormat.JPEG, 2);
				reader.setOnImageAvailableListener(this::receiveImage, handler);
				CameraDevice.StateCallback callback = new CameraDevice.StateCallback() {
					@Override
					public void onOpened(CameraDevice opened) {
						finishOpening();
						cancelTimeout();
						if (!current()) {
							opened.close();
							return;
						}
						camera = opened;
						configureSession(false);
					}

					@Override
					public void onDisconnected(CameraDevice disconnected) {
						finishOpening();
						disconnected.close();
						if (current()) {
							fatal("摄像头已断开");
						}
					}

					@Override
					public void onError(CameraDevice failed, int error) {
						finishOpening();
						failed.close();
						if (current()) {
							fatal("摄像头错误：" + error);
						}
					}
				};
				timeout = () -> {
					if (current()) {
						fatal("打开摄像头超时");
					}
				};
				摄像框Impl.this.handler.postDelayed(timeout, 15000);
				if (!current()) {
					return;
				}
				opening = true;
				++openingCameras;
				if (Build.VERSION.SDK_INT >= 28) {
					manager.openCamera(cameraId, command -> handler.post(command), callback);
				} else {
					manager.openCamera(cameraId, callback, handler);
				}
			} catch (CameraAccessException | IOException | RuntimeException failure) {
				finishOpening();
				fatal(failure.toString());
			}
		}

		/**
		 * 将打开请求计数归还一次，销毁后的迟到回调仍须有线程接收并关闭设备。
		 */
		private void finishOpening() {
			if (opening) {
				opening = false;
				--openingCameras;
			}
			retireCameraThread();
		}

		@Override
		public void surfaceChanged(SurfaceHolder surface, int format, int width, int height) {
			requestLayout();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder surface) {
			// 一次表面对应一次后端；迟到的打开、会话和图像回调不能重新占用相机。
			close();
		}

		/**
		 * 建立预览或录像输出组合；用序号拒绝被停止或被新会话替换的回调。
		 *
		 * @param video 是否建立录像会话
		 */
		private void configureSession(boolean video) {
			if (!current() || camera == null) {
				return;
			}
			ready = false;
			final int expected = ++revision;
			if (session != null) {
				session.close();
				session = null;
			}
			try {
				repeating = camera.createCaptureRequest(video ? CameraDevice.TEMPLATE_RECORD : CameraDevice.TEMPLATE_PREVIEW);
				repeating.addTarget(holder.getSurface());
				if (video) {
					repeating.addTarget(recorder.getSurface());
				}
				repeating.set(CaptureRequest.CONTROL_AF_MODE, continuousMode);
				repeating.set(CaptureRequest.FLASH_MODE, flashlight ? CaptureRequest.FLASH_MODE_TORCH : CaptureRequest.FLASH_MODE_OFF);
				List<Surface> outputs = Arrays.asList(holder.getSurface(), video ? recorder.getSurface() : reader.getSurface());
				CameraCaptureSession.StateCallback callback = new CameraCaptureSession.StateCallback() {
					@Override
					public void onConfigured(CameraCaptureSession configured) {
						if (!current() || revision != expected) {
							configured.close();
							return;
						}
						cancelTimeout();
						session = configured;
						try {
							configured.setRepeatingRequest(repeating.build(), null, handler);
							ready = true;
							if (video) {
								recorder.start();
								recording = true;
								emit(ModernCameraView.this, "录像开始");
							} else if (!reported) {
								reported = true;
								reportSizesAndReady();
							}
						} catch (CameraAccessException | RuntimeException failure) {
							fatal(failure.toString());
						}
					}

					@Override
					public void onConfigureFailed(CameraCaptureSession failed) {
						failed.close();
						if (current() && revision == expected) {
							fatal("设备不支持当前相机输出组合");
						}
					}
				};
				cancelTimeout();
				timeout = () -> {
					if (current() && revision == expected) {
						fatal("配置相机会话超时");
					}
				};
				摄像框Impl.this.handler.postDelayed(timeout, 15000);
				if (Build.VERSION.SDK_INT >= 28) {
					createModernSession(camera, outputs, callback);
				} else {
					createLegacySession(camera, outputs, callback);
				}
			} catch (CameraAccessException | RuntimeException failure) {
				fatal(failure.toString());
			}
		}

		/**
		 * 完成初始化后报告能力；每个事件之后重新核对所有者，允许事件关闭或切换相机。
		 */
		private void reportSizesAndReady() {
			StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
			emit(this, "获得照片尺寸列表", modernSizeText(map.getOutputSizes(ImageFormat.JPEG)));
			if (current()) {
				emit(this, "获得预览尺寸列表", modernSizeText(map.getOutputSizes(SurfaceHolder.class)));
			}
			if (current()) {
				emit(this, "预览就绪");
			}
		}

		/**
		 * 使用 API 28 引入的会话配置替代 API 30 弃用的旧入口。
		 *
		 * @param device 当前相机
		 * @param surfaces 输出表面
		 * @param callback 会话结果
		 * @throws CameraAccessException 相机访问失败
		 */
		private void createModernSession(CameraDevice device, List<Surface> surfaces,
				CameraCaptureSession.StateCallback callback) throws CameraAccessException {
			List<OutputConfiguration> outputs = new ArrayList<>();
			for (Surface surface : surfaces) {
				outputs.add(new OutputConfiguration(surface));
			}
			device.createCaptureSession(new SessionConfiguration(SessionConfiguration.SESSION_REGULAR,
					outputs, command -> handler.post(command), callback));
		}

		/**
		 * API 24～27 没有 SessionConfiguration，必须通过旧入口建立实际会话。
		 *
		 * @param device 当前相机
		 * @param surfaces 输出表面
		 * @param callback 会话结果
		 * @throws CameraAccessException 相机访问失败
		 */
		@SuppressWarnings("deprecation")
		private void createLegacySession(CameraDevice device, List<Surface> surfaces,
				CameraCaptureSession.StateCallback callback) throws CameraAccessException {
			device.createCaptureSession(surfaces, callback, handler);
		}

		@Override
		void focus() {
			if (Looper.myLooper() != handler.getLooper()) {
				if (!current() || !ready) {
					reportFailure("开始对焦", "预览尚未就绪");
					return;
				}
				handler.post(() -> focus());
				return;
			}
			if (!available("开始对焦") || recorder != null || picturePending || processing.get() || focusing) {
				if (ready && (recorder != null || picturePending || processing.get() || focusing)) {
					reportFailure("开始对焦", "正在执行其他拍摄操作");
				}
				return;
			}
			if (!contains(characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES),
					CaptureRequest.CONTROL_AF_MODE_AUTO)) {
				emit(this, "对焦完毕", false);
				return;
			}
			try {
				focusing = true;
				repeating.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
				repeating.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
				session.capture(repeating.build(), null, handler);
				repeating.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
				session.setRepeatingRequest(repeating.build(), new CameraCaptureSession.CaptureCallback() {
					@Override
					public void onCaptureCompleted(CameraCaptureSession captured, CaptureRequest request, TotalCaptureResult result) {
						if (!current() || captured != session || !focusing) {
							return;
						}
						Integer state = result.get(CaptureResult.CONTROL_AF_STATE);
						if (state != null && (state == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
								|| state == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED)) {
							finishFocus(state == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED);
						}
					}
				}, handler);
				focusTimeout = () -> {
					if (current() && focusing) {
						finishFocus(false);
					}
				};
				handler.postDelayed(focusTimeout, 5000);
			} catch (CameraAccessException | RuntimeException failure) {
				focusing = false;
				if (focusTimeout != null) {
					handler.removeCallbacks(focusTimeout);
				}
				reportFailure("开始对焦", failure.toString());
			}
		}

		/**
		 * 恢复连续对焦后交付本次结果，不能让单次对焦永久改变预览模式。
		 *
		 * @param success 是否对焦成功
		 */
		private void finishFocus(boolean success) {
			focusing = false;
			if (focusTimeout != null) {
				handler.removeCallbacks(focusTimeout);
			}
			try {
				repeating.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
				session.capture(repeating.build(), null, handler);
				repeating.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
				repeating.set(CaptureRequest.CONTROL_AF_MODE, continuousMode);
				session.setRepeatingRequest(repeating.build(), null, handler);
				emit(this, "对焦完毕", success);
			} catch (CameraAccessException | RuntimeException failure) {
				reportFailure("开始对焦", failure.toString());
			}
		}

		@Override
		void takePicture(String path, int format, int quality, boolean returnBytes) {
			if (Looper.myLooper() != handler.getLooper()) {
				if (!current() || !ready) {
					reportFailure("拍摄照片", "预览尚未就绪");
					return;
				}
				handler.post(() -> takePicture(path, format, quality, returnBytes));
				return;
			}
			if (!available("拍摄照片")) {
				return;
			}
			if (recorder != null || picturePending || processing.get() || focusing) {
				reportFailure("拍摄照片", "正在执行其他拍摄操作");
				return;
			}
			if ((format != 1 && format != 2) || quality < 0 || quality > 100) {
				reportFailure("拍摄照片", "格式应为 1（JPEG）或 2（PNG），质量范围为 0～100");
				return;
			}
			try {
				if (!returnBytes) {
					try {
						outputFile(path);
					} catch (IOException failure) {
						reportFailure("拍摄照片", failure.getMessage());
						return;
					}
				}
				picturePending = true;
				photoPath = path;
				photoFormat = format;
				photoQuality = quality;
				photoBytes = returnBytes;
				pictureRotation = imageRotation(sensorOrientation, displayRotation(), cameraFacing == 1);
				CaptureRequest.Builder shot = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
				shot.addTarget(reader.getSurface());
				// 固定为传感器原始方向，随后统一旋转实际像素，避免不同厂商 EXIF 策略导致二次旋转。
				shot.set(CaptureRequest.JPEG_ORIENTATION, 0);
				shot.set(CaptureRequest.JPEG_QUALITY, (byte) 100);
				shot.set(CaptureRequest.CONTROL_AF_MODE, continuousMode);
				shot.set(CaptureRequest.FLASH_MODE, flashlight ? CaptureRequest.FLASH_MODE_TORCH : CaptureRequest.FLASH_MODE_OFF);
				session.capture(shot.build(), new CameraCaptureSession.CaptureCallback() {
					@Override
					public void onCaptureFailed(CameraCaptureSession captured, CaptureRequest request, CaptureFailure failure) {
						if (current() && captured == session && picturePending) {
							picturePending = false;
							if (photoTimeout != null) {
								handler.removeCallbacks(photoTimeout);
							}
							fatal("相机没有生成照片：" + failure.getReason());
						}
					}
				}, handler);
				photoTimeout = () -> {
					if (current() && picturePending) {
						// 超时后关闭整个后端，不能把迟到图像配给后续照片请求。
						fatal("等待照片超时");
					}
				};
				handler.postDelayed(photoTimeout, 15000);
			} catch (CameraAccessException | RuntimeException failure) {
				picturePending = false;
				fatal("拍摄照片失败：" + failure);
			}
		}

		/**
		 * 立即复制 JPEG 并关闭 Image，避免编码期间占满相机缓冲区。
		 *
		 * @param source 图像接收器
		 */
		private void receiveImage(ImageReader source) {
			try (Image image = source.acquireNextImage()) {
				if (image == null || !current() || source != reader || !picturePending) {
					return;
				}
				java.nio.ByteBuffer buffer = image.getPlanes()[0].getBuffer();
				byte[] data = new byte[buffer.remaining()];
				buffer.get(data);
				picturePending = false;
				if (photoTimeout != null) {
					handler.removeCallbacks(photoTimeout);
				}
				processPicture(this, data, pictureRotation, photoPath, photoFormat, photoQuality, photoBytes);
			} catch (RuntimeException failure) {
				if (current()) {
					picturePending = false;
					reportFailure("拍摄照片", failure.toString());
				}
			}
		}

		@Override
		void takeVideo(String path, int quality, boolean sound) {
			if (Looper.myLooper() != handler.getLooper()) {
				if (!current() || !ready) {
					reportFailure("开始录像", "预览尚未就绪");
					return;
				}
				handler.post(() -> takeVideo(path, quality, sound));
				return;
			}
			if (!available("开始录像")) {
				return;
			}
			if (recorder != null || picturePending || processing.get() || focusing) {
				reportFailure("开始录像", "正在执行其他拍摄操作");
				return;
			}
			if (quality < 1 || quality > 7) {
				reportFailure("开始录像", "录像质量必须为 1～7");
				return;
			}
			if (sound && Build.VERSION.SDK_INT >= 23
					&& activity.checkSelfPermission("android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) {
				reportFailure("开始录像", "有声录像需要麦克风权限");
				return;
			}
			try {
				videoTarget = outputFile(path);
				videoTemp = File.createTempFile(".simple-video-", ".mp4", videoTarget.getParentFile());
				StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
				int[] heights = {0, 240, 288, 480, 720, 1080, 1080};
				Size size = chooseVideoSize(map.getOutputSizes(MediaRecorder.class), quality, heights[quality - 1]);
				recorder = new MediaRecorder();
				if (sound) {
					recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
				}
				recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
				recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
				recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
				recorder.setVideoSize(size.getWidth(), size.getHeight());
				recorder.setVideoFrameRate(30);
				recorder.setVideoEncodingBitRate(Math.max(500000, size.getWidth() * size.getHeight() * 5));
				if (sound) {
					recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
					recorder.setAudioEncodingBitRate(128000);
					recorder.setAudioChannels(1);
					recorder.setAudioSamplingRate(44100);
				}
				recorder.setOrientationHint(imageRotation(sensorOrientation, displayRotation(), cameraFacing == 1));
				recorder.setOutputFile(videoTemp.getAbsolutePath());
				recorder.setOnErrorListener((failed, what, extra) -> {
					if (current() && recorder == failed) {
						fatal("录像器错误：" + what + "/" + extra);
					}
				});
				recorder.prepare();
				configureSession(true);
			} catch (IOException | RuntimeException failure) {
				releaseRecorder(false);
				reportFailure("开始录像", failure.toString());
			}
		}

		@Override
		void stopVideo() {
			if (Looper.myLooper() != handler.getLooper()) {
				handler.post(() -> stopVideo());
				return;
			}
			if (!current() || recorder == null) {
				return;
			}
			++revision;
			ready = false;
			cancelTimeout();
			if (session != null) {
				session.close();
				session = null;
			}
			releaseRecorder(true);
			configureSession(false);
		}

		/**
		 * 停止并释放录像器；显式停止且文件正常才发布，关闭或失败时清理临时文件。
		 *
		 * @param publish 是否交付有效录像
		 */
		private void releaseRecorder(boolean publish) {
			MediaRecorder previous = recorder;
			recorder = null;
			boolean wasRecording = recording;
			recording = false;
			boolean complete = false;
			try {
				if (previous != null && wasRecording) {
					previous.stop();
					complete = true;
				}
			} catch (RuntimeException failure) {
				if (publish) {
					reportFailure("停止录像", "录像过短或编码失败：" + failure);
				}
			} finally {
				if (previous != null) {
					try {
						previous.release();
					} catch (RuntimeException failure) {
						Log.w("摄像框", "释放录像器失败", failure);
					}
				}
			}
			File staged = videoTemp;
			File target = videoTarget;
			videoTemp = null;
			videoTarget = null;
			if (publish && complete && current()) {
				// 文件提交与关闭都在主线程排序，避免关闭之后才落下未通知的视频。
				摄像框Impl.this.handler.post(() -> {
					if (!current()) {
						discard(staged);
						return;
					}
					try {
						publishFile(staged, target);
						录像完毕(target.getAbsolutePath());
					} catch (IOException failure) {
						discard(staged);
						reportFailure("停止录像", failure.getMessage());
					}
				});
				return;
			} else if (publish && !wasRecording) {
				reportFailure("停止录像", "录像尚未开始，已取消准备");
			}
			discard(staged);
		}

		@Override
		void applyFlashlight(boolean enabled) {
			if (Looper.myLooper() != handler.getLooper()) {
				if (!current() || !ready) {
					reportFailure("设置闪光灯", "预览尚未就绪");
					return;
				}
				handler.post(() -> applyFlashlight(enabled));
				return;
			}
			if (!available("设置闪光灯")) {
				return;
			}
			if (enabled && !Boolean.TRUE.equals(characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE))) {
				reportFailure("开启闪光灯", "所选摄像头没有闪光灯");
				return;
			}
			if (focusing || picturePending) {
				reportFailure("设置闪光灯", "正在对焦或拍照");
				return;
			}
			try {
				repeating.set(CaptureRequest.FLASH_MODE, enabled ? CaptureRequest.FLASH_MODE_TORCH : CaptureRequest.FLASH_MODE_OFF);
				session.setRepeatingRequest(repeating.build(), null, handler);
				flashlight = enabled;
			} catch (CameraAccessException | RuntimeException failure) {
				reportFailure("设置闪光灯", failure.toString());
			}
		}

		/**
		 * 检查预览会话；请求不得以没有异常冒充已受理。
		 *
		 * @param operation 操作名称
		 * @return 会话可接受请求
		 */
		private boolean available(String operation) {
			if (!current() || !ready || session == null) {
				reportFailure(operation, "预览尚未就绪");
				return false;
			}
			return true;
		}

		/**
		 * 先释放全部资源，再异步报告不可继续的相机错误。
		 *
		 * @param message 错误原因
		 */
		private void fatal(String message) {
			close();
			reportFailure("摄像头", message);
		}

		/**
		 * 撤销当前打开或会话配置的超时任务。
		 */
		private void cancelTimeout() {
			if (timeout != null) {
				摄像框Impl.this.handler.removeCallbacks(timeout);
				timeout = null;
			}
		}

		@Override
		void close() {
			if (closed) {
				return;
			}
			closed = true;
			ready = false;
			post(() -> holder.removeCallback(this));
			if (Looper.myLooper() == handler.getLooper()) {
				releaseResources();
			} else {
				handler.post(this::releaseResources);
			}
		}

		/**
		 * 在相机线程按会话、录像器、相机、图像缓冲区的顺序释放。
		 */
		private void releaseResources() {
			++revision;
			cancelTimeout();
			if (focusTimeout != null) {
				handler.removeCallbacks(focusTimeout);
			}
			if (photoTimeout != null) {
				handler.removeCallbacks(photoTimeout);
			}
			try {
				if (session != null) {
					session.close();
				}
			} catch (RuntimeException failure) {
				Log.w("摄像框", "关闭相机会话失败", failure);
			} finally {
				session = null;
				releaseRecorder(false);
				try {
					if (camera != null) {
						camera.close();
					}
				} catch (RuntimeException failure) {
					Log.w("摄像框", "关闭摄像头失败", failure);
				} finally {
					camera = null;
					if (reader != null) {
						reader.close();
						reader = null;
					}
				}
			}
			retireCameraThread();
		}

		/**
		 * 检查设备能力数组中的可用模式。
		 *
		 * @param values 能力值，可为空
		 * @param expected 目标模式
		 * @return 是否包含目标模式
		 */
		private boolean contains(int[] values, int expected) {
			if (values != null) {
				for (int value : values) {
					if (value == expected) {
						return true;
					}
				}
			}
			return false;
		}

		/**
		 * 按长短边比例、像素面积距离选择尺寸，不假设设备传感器固定为横屏。
		 *
		 * @param sizes 支持的尺寸
		 * @param width 请求宽度
		 * @param height 请求高度
		 * @return 最接近请求的尺寸
		 * @throws IOException 未报告可用尺寸
		 */
		private Size chooseSize(Size[] sizes, int width, int height) throws IOException {
			if (sizes == null || sizes.length == 0) {
				throw new IOException("设备未报告可用输出尺寸");
			}
			Size best = null;
			double score = Double.POSITIVE_INFINITY;
			for (Size size : sizes) {
				double candidate = sizeScore(size.getWidth(), size.getHeight(), width, height);
				if (candidate < score) {
					best = size;
					score = candidate;
				}
			}
			return best;
		}

		/**
		 * 在设备录像输出中选择档位，超出常规编码范围的分辨率不自动采用。
		 *
		 * @param sizes 支持的录像尺寸
		 * @param quality 1 最低、7 最高，其余为期望档位
		 * @param height 期望短边
		 * @return 设备支持且不超过 1080p 的近似档位
		 * @throws IOException 没有可用录像尺寸
		 */
		private Size chooseVideoSize(Size[] sizes, int quality, int height) throws IOException {
			if (sizes == null || sizes.length == 0) {
				throw new IOException("设备不支持录像输出");
			}
			Size best = null;
			double distance = Double.POSITIVE_INFINITY;
			for (Size size : sizes) {
				if (size.getWidth() > 1920 || size.getHeight() > 1080) {
					continue;
				}
				double candidate = quality == 1 ? (double) size.getWidth() * size.getHeight()
						: quality == 7 ? -(double) size.getWidth() * size.getHeight()
						: Math.abs(size.getHeight() - height) * 1000000d + size.getWidth();
				if (candidate < distance) {
					best = size;
					distance = candidate;
				}
			}
			if (best == null) {
				throw new IOException("没有可用的常规录像分辨率");
			}
			return best;
		}

		/**
		 * 将相机输出尺寸转换为逐行的短边×长边文本，便于竖屏请求。
		 *
		 * @param sizes 设备支持的输出尺寸
		 * @return 按面积升序排列的尺寸列表
		 */
		private String modernSizeText(Size[] sizes) {
			if (sizes == null) {
				return "";
			}
			List<Size> ordered = new ArrayList<>(Arrays.asList(sizes));
			Collections.sort(ordered, (first, second) -> Long.compare(
					(long) first.getWidth() * first.getHeight(), (long) second.getWidth() * second.getHeight()));
			StringBuilder result = new StringBuilder();
			for (Size size : ordered) {
				if (result.length() > 0) {
					result.append('\n');
				}
				result.append(Math.min(size.getWidth(), size.getHeight())).append('×')
						.append(Math.max(size.getWidth(), size.getHeight()));
			}
			return result.toString();
		}
	}

	/**
	 * 评价长短边比例及面积偏差，避免只比较一条边而选错分辨率。
	 *
	 * @param width 候选宽度
	 * @param height 候选高度
	 * @param requestedWidth 请求宽度
	 * @param requestedHeight 请求高度
	 * @return 越小越接近；非法尺寸返回正无穷
	 */
	private static double sizeScore(int width, int height, int requestedWidth, int requestedHeight) {
		if (width <= 0 || height <= 0 || requestedWidth <= 0 || requestedHeight <= 0) {
			return Double.POSITIVE_INFINITY;
		}
		double ratio = (double) Math.max(width, height) / Math.min(width, height);
		double requestedRatio = (double) Math.max(requestedWidth, requestedHeight) / Math.min(requestedWidth, requestedHeight);
		double area = (double) width * height / ((double) requestedWidth * requestedHeight);
		return 10 * Math.abs(Math.log(ratio / requestedRatio)) + Math.abs(Math.log(area));
	}
}

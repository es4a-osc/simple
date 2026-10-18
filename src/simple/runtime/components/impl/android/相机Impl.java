package simple.runtime.components.impl.android;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.AtomicFile;

import simple.runtime.android.MainActivity;
import simple.runtime.components.相机;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 系统相机与相册适配；使用内容 URI 传递拍摄结果，不向其它应用暴露 file URI。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class 相机Impl extends 组件Impl implements 相机, MainActivity.OnActivityResultListener {

	private static final AtomicInteger NEXT_REQUEST = new AtomicInteger(0x5000);

	// 分配互不重复的实例请求，避免多个相机组件接收同一次结果。
	private final int takePictureRequest = NEXT_REQUEST.getAndAdd(4);
	private final int checkPictureRequest = takePictureRequest + 1;
	private final int takeVideoRequest = takePictureRequest + 2;
	private final int cropPictureRequest = takePictureRequest + 3;
	private final MainActivity activity;
	private Camera flashlightCamera;
	private SurfaceTexture flashlightSurface;
	private int pendingRequest;
	private Uri pendingOutput;
	private String outputPath;
	private boolean cropRequested;
	private boolean videoRequested;
	private boolean destroyed;

	public 相机Impl(组件容器 container) {
		super(container);

		if (cropPictureRequest > 0x5fff) {
			throw new IllegalStateException("相机组件请求标识已耗尽");
		}

		activity = MainActivity.getContext();

		activity.addOnActivityResultListener(this);
	}

	/* 组件事件 */

	@Override
	public void 拍摄完毕() {
		EventDispatcher.dispatchEvent(this, "拍摄完毕");
	}

	@Override
	public void 录像完毕() {
		EventDispatcher.dispatchEvent(this, "录像完毕");
	}

	@Override
	public void 浏览完毕(String path) {
		EventDispatcher.dispatchEvent(this, "浏览完毕", path);
	}

	@Override
	public void 剪裁完毕(String path) {
		EventDispatcher.dispatchEvent(this, "剪裁完毕", path);
	}

	/* 组件方法 */

	@Override
	public void 开始拍照(String path) {
		prepareOutput(path, false);
		launch(withOutput(new Intent(MediaStore.ACTION_IMAGE_CAPTURE)), takePictureRequest);
	}

	@Override
	public void 开始录像(String path, int quality) {
		prepareOutput(path, true);
		Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, quality);
		launch(withOutput(intent), takeVideoRequest);
	}

	@Override
	public void 更新系统相册(String path) {
		MediaScannerConnection.scanFile(activity, new String[] { path }, null, null);
	}

	@Override
	public void 浏览相册() {
		selectMedia(false, false);
	}

	@Override
	public void 浏览相册视频() {
		selectMedia(true, false);
	}

	@Override
	public void 浏览相册并剪裁() {
		selectMedia(false, true);
	}

	@Override
	public int 取照片旋转角度(String path) {
		try {
			int orientation = new ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

			switch (orientation) {
				case ExifInterface.ORIENTATION_ROTATE_180: return 180;
				case ExifInterface.ORIENTATION_ROTATE_90: return 90;
				case ExifInterface.ORIENTATION_ROTATE_270: return 270;
				default: return 0;
			}
		} catch (IOException error) {
			return 0;
		}
	}

	@Override
	public boolean 是否存在闪光灯() {
		return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
	}

	@Override
	public void 开启闪光灯() {
		if (destroyed || flashlightCamera != null || !是否存在闪光灯()) {
			return;
		}

		try {
			flashlightCamera = Camera.open();
			Camera.Parameters parameters = flashlightCamera.getParameters();
			List<String> modes = parameters.getSupportedFlashModes();

			if (modes == null || !modes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
				关闭闪光灯();
				return;
			}

			flashlightSurface = new SurfaceTexture(0);
			flashlightCamera.setPreviewTexture(flashlightSurface);
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
			flashlightCamera.setParameters(parameters);
			flashlightCamera.startPreview();
		} catch (IOException | RuntimeException error) {
			关闭闪光灯();
			Log.e("相机", "开启闪光灯失败", error);
		}
	}

	@Override
	public void 关闭闪光灯() {
		Camera previous = flashlightCamera;
		flashlightCamera = null;

		try {
			if (previous != null) {
				try {
					previous.stopPreview();
				} catch (RuntimeException error) {
					Log.w("相机", "停止闪光灯预览失败", error);
				} finally {
					previous.release();
				}
			}
		} finally {
			if (flashlightSurface != null) {
				flashlightSurface.release();
				flashlightSurface = null;
			}
		}
	}

	@Override
	public void 销毁() {
		destroyed = true;
		pendingRequest = 0;
		activity.removeOnActivityResultListener(this);
		clearOutput();
		关闭闪光灯();

		super.销毁();
	}

	/* 内部接口与平台回调 */

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (destroyed || requestCode != pendingRequest || pendingRequest == 0) {
			return;
		}

		pendingRequest = 0;
		if (resultCode != Activity.RESULT_OK) {
			clearOutput();
			return;
		}

		if (requestCode == takePictureRequest || requestCode == takeVideoRequest || requestCode == cropPictureRequest) {
			String completedPath = outputPath;

			try {
				copyContent(pendingOutput, new File(completedPath));
			} catch (IOException | RuntimeException error) {
				Log.e("相机", "保存拍摄结果失败", error);
				return;
			} finally {
				clearOutput();
			}

			// 业务事件在清理完成后分发，事件处理程序可以发起下一次拍摄。
			if (requestCode == takePictureRequest) {
				拍摄完毕();
			} else if (requestCode == takeVideoRequest) {
				录像完毕();
			} else {
				剪裁完毕(completedPath);
			}

			return;
		}

		if (data == null || data.getData() == null) {
			return;
		}

		String path;

		try {
			Uri selected = data.getData();
			path = resolvePath(selected);
			if (cropRequested) {
				File cropFile = File.createTempFile("simple-crop-", ".jpg", activity.getCacheDir());
				prepareOutput(cropFile.getAbsolutePath(), false);
				Intent crop = new Intent("com.android.camera.action.CROP");
				crop.setDataAndType(selected, "image/*");
				crop.putExtra("scale", true);
				launch(withOutput(crop), cropPictureRequest);
			}
		} catch (IOException | RuntimeException error) {
			clearOutput();
			Log.e("相机", "处理相册结果失败", error);
			return;
		}

		浏览完毕(path);
	}

	/* 内部辅助方法 */

	/**
	 * 启动指定请求对应的系统活动。
	 *
	 * @param intent 系统意图
	 * @param requestCode 发起操作时使用的请求编号
	 */
	private void launch(Intent intent, int requestCode) {
		if (destroyed) {
			throw new IllegalStateException("相机组件已销毁");
		}

		if (pendingRequest != 0) {
			throw new IllegalStateException("上一项相机操作尚未完成");
		}

		// 只接收本组件发出的任务结果，取消和启动失败也清理临时输出。
		pendingRequest = requestCode;

		try {
			activity.startActivityForResult(intent, requestCode);
		} catch (RuntimeException error) {
			pendingRequest = 0;
			clearOutput();
			throw error;
		}
	}

	/**
	 * 准备照片或视频的输出位置。
	 *
	 * @param path 文件或目录路径
	 * @param video 是否处理视频
	 */
	private void prepareOutput(String path, boolean video) {
		if (destroyed) {
			throw new IllegalStateException("相机组件已销毁");
		}

		if (pendingRequest != 0) {
			throw new IllegalStateException("上一项相机操作尚未完成");
		}

		File file = new File(path);
		if (path.isEmpty()) {
			throw new IllegalArgumentException("输出路径不能为空");
		}

		// 系统相机先写临时媒体项，结果成功后再复制到用户指定路径并删除临时项。
		ContentValues values = new ContentValues();
		values.put(MediaStore.MediaColumns.DISPLAY_NAME, file.getName());
		values.put(MediaStore.MediaColumns.MIME_TYPE, video ? "video/mp4" : "image/jpeg");
		pendingOutput = activity.getContentResolver().insert(video
				? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
		if (pendingOutput == null) {
			throw new IllegalStateException("无法创建拍摄输出");
		}

		outputPath = path;
	}

	/**
	 * 为拍摄意图附加输出位置和访问授权。
	 *
	 * @param intent 系统意图
	 * @return 带有输出位置和授权的意图
	 */
	private Intent withOutput(Intent intent) {
		intent.putExtra(MediaStore.EXTRA_OUTPUT, pendingOutput);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

		intent.setClipData(android.content.ClipData.newRawUri("拍摄输出", pendingOutput));
		return intent;
	}

	/**
	 * 打开系统媒体选择界面。
	 *
	 * @param video 是否处理视频
	 * @param crop 是否请求裁剪
	 */
	private void selectMedia(boolean video, boolean crop) {
		if (pendingRequest != 0) {
			throw new IllegalStateException("上一项相机操作尚未完成");
		}

		videoRequested = video;
		cropRequested = crop;
		launch(new Intent(Intent.ACTION_PICK, video
				? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI), checkPictureRequest);
	}

	/**
	 * 解析媒体 URI 可使用的本地路径。
	 *
	 * @param uri 内容地址
	 * @return 解析得到的媒体文件路径
	 * @throws IOException 访问文件、流或网络资源失败时
	 */
	private String resolvePath(Uri uri) throws IOException {
		if ("file".equals(uri.getScheme())) {
			return uri.getPath();
		}

		// 旧媒体提供器能返回文件路径；新提供器缺少 _data 时复制到应用缓存。
		try (Cursor cursor = activity.getContentResolver().query(uri,
				new String[] { MediaStore.MediaColumns.DATA }, null, null, null)) {
			if (cursor != null && cursor.moveToFirst()) {
				int column = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
				if (column >= 0) {
					String path = cursor.getString(column);
					if (path != null && !path.isEmpty() && new File(path).canRead()) {
						return path;
					}
				}
			}
		} catch (IllegalArgumentException error) {
			Log.d("相机", "媒体提供器未提供文件路径，改用内容流");
		}

		File file = File.createTempFile("simple-media-", videoRequested ? ".mp4" : ".jpg", activity.getCacheDir());
		copyContent(uri, file);
		return file.getAbsolutePath();
	}

	/**
	 * 将内容 URI 的数据复制到目标文件。
	 *
	 * @param uri 内容地址
	 * @param file 本次操作的文件
	 * @throws IOException 访问文件、流或网络资源失败时
	 */
	private void copyContent(Uri uri, File file) throws IOException {
		File parent = file.getAbsoluteFile().getParentFile();
		if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
			throw new IOException("无法创建输出目录");
		}

		try (InputStream input = activity.getContentResolver().openInputStream(uri)) {
			if (input == null) {
				throw new IOException("无法读取拍摄结果");
			}

			// 内容流读取或写入失败时恢复已有文件，不留下被截断的目标文件。
			AtomicFile target = new AtomicFile(file);
			FileOutputStream output = target.startWrite();

			try {
				byte[] buffer = new byte[8192];
				int size;

				while ((size = input.read(buffer)) != -1) {
					output.write(buffer, 0, size);
				}

				target.finishWrite(output);
			} catch (IOException | RuntimeException error) {
				target.failWrite(output);
				throw error;
			}
		}
	}

	/**
	 * 清除本次拍摄输出状态。
	 */
	private void clearOutput() {
		Uri previous = pendingOutput;
		pendingOutput = null;
		outputPath = null;

		if (previous != null) {
			try {
				activity.getContentResolver().delete(previous, null, null);
			} catch (RuntimeException error) {
				Log.w("相机", "清理临时拍摄结果失败", error);
			}
		}
	}
}

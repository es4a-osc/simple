package simple.runtime.components.impl.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Log;
import android.util.AtomicFile;
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
import java.util.Comparator;
import java.util.List;

/**
 * 摄像头预览与拍摄实现，协调 Surface、Camera 和 MediaRecorder 的资源生命周期。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class 摄像框Impl extends 视图组件 implements 摄像框 {

	private LinearLayout layout;
	private CameraView cameraView;
	private int cameraFacing;
	private int requestedWidth;
	private int requestedHeight;
	private boolean flashlight;
	private boolean destroyed;

	public 摄像框Impl(组件容器 container) {
		super(container);
	}

	@Override
	protected View createView() {
		// 基类构造期间调用，此处只创建视图，不读取子类初始化字段。
		layout = new LinearLayout(MainActivity.getContext());
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setGravity(Gravity.CENTER_HORIZONTAL);
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

	/* 组件方法 */

	@Override
	public void 开启摄像头(int type, int width, int height) {
		if (destroyed) {
			return;
		}

		if ((type != 1 && type != 2) || width <= 0 || height <= 0) {
			throw new IllegalArgumentException("摄像头类型应为 1（前置）或 2（后置），宽高必须大于零");
		}

		关闭摄像头();
		cameraFacing = type == 1 ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
		requestedWidth = width;
		requestedHeight = height;

		cameraView = new CameraView(MainActivity.getContext());

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
		if (cameraView != null) {
			cameraView.focus();
		}
	}

	@Override
	public void 拍摄照片(String path, int format, int quality) {
		if (cameraView != null) {
			cameraView.takePicture(path, format, quality, false);
		}
	}

	@Override
	public void 拍摄照片2(int format, int quality) {
		if (cameraView != null) {
			cameraView.takePicture(null, format, quality, true);
		}
	}

	@Override
	public void 开始录像(String path, int format, int quality, boolean sound) {
		if (cameraView != null) {
			// 兼容原接口：格式参数保留，实际容器与编码由设备录像档位决定。
			cameraView.takeVideo(path, quality, sound);
		}
	}

	@Override
	public void 停止录像() {
		if (cameraView != null) {
			cameraView.stopVideo();
		}
	}

	@Override
	public void 开启闪光灯() {
		flashlight = true;
		if (cameraView != null) {
			cameraView.applyFlashlight();
		}
	}

	@Override
	public void 关闭闪光灯() {
		flashlight = false;
		if (cameraView != null) {
			cameraView.applyFlashlight();
		}
	}

	@Override
	public void 关闭摄像头() {
		CameraView previous = cameraView;
		cameraView = null;
		if (previous != null) {
			previous.close();
			layout.removeView(previous);
		}
	}

	@Override
	public void 销毁() {
		destroyed = true;
		关闭摄像头();

		super.销毁();
	}

	/* 内部辅助方法 */

	private static File outputFile(String path) throws IOException {
		if (path == null || path.isEmpty()) {
			throw new IOException("输出路径不能为空");
		}

		File file = new File(path);
		File parent = file.getAbsoluteFile().getParentFile();
		if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
			throw new IOException("无法创建输出目录");
		}

		return file;
	}

	private static List<Camera.Size> sortedSizes(List<Camera.Size> sizes) {
		List<Camera.Size> result = sizes == null ? new ArrayList<Camera.Size>() : new ArrayList<Camera.Size>(sizes);
		Collections.sort(result, new Comparator<Camera.Size>() {
			@Override
			public int compare(Camera.Size first, Camera.Size second) {
				int width = Integer.compare(first.width, second.width);
				return width == 0 ? Integer.compare(first.height, second.height) : width;
			}
		});
		return result;
	}

	private Camera.Size selectSize(List<Camera.Size> sizes) {
		Camera.Size selected = null;
		for (Camera.Size size : sizes) {
			if (size.height == requestedWidth && size.width == requestedHeight) {
				return size;
			}

			if (size.height <= requestedWidth) {
				selected = size;
			}
		}

		return selected;
	}

	private static String sizeText(List<Camera.Size> sizes) {
		StringBuilder text = new StringBuilder();
		for (Camera.Size size : sizes) {
			if (text.length() > 0) {
				text.append('\n');
			}

			// 保留原组件按竖屏“高×宽”报告尺寸的约定。
			text.append(size.height).append('×').append(size.width);
		}

		return text.toString();
	}

	private final class CameraView extends SurfaceView implements SurfaceHolder.Callback {

		private final SurfaceHolder holder;
		private Camera camera;
		private MediaRecorder recorder;
		private int cameraId = -1;
		private int pictureRotation;
		private boolean closed;
		private boolean picturePending;
		private boolean recording;
		private boolean sizesReported;

		CameraView(Context context) {
			super(context);
			holder = getHolder();
			holder.addCallback(this);
			setKeepScreenOn(true);
		}

		private boolean active() {
			return !closed && cameraView == this && camera != null;
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			if (closed || cameraView != this) {
				return;
			}

			// 只打开符合前置／后置选择的一个摄像头，不把“缺少前置”误当成后置。
			Camera.CameraInfo info = new Camera.CameraInfo();
			for (int id = 0; id < Camera.getNumberOfCameras(); id++) {
				Camera.getCameraInfo(id, info);
				if (info.facing == cameraFacing) {
					cameraId = id;
					break;
				}
			}

			if (cameraId < 0) {
				return;
			}

			try {
				camera = Camera.open(cameraId);
				camera.setPreviewDisplay(holder);
				int rotation = MainActivity.getContext().getWindowManager().getDefaultDisplay().getRotation();
				int degrees = rotation == Surface.ROTATION_90 ? 90 : rotation == Surface.ROTATION_180 ? 180
						: rotation == Surface.ROTATION_270 ? 270 : 0;
				pictureRotation = info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT
						? (info.orientation + degrees) % 360 : (info.orientation - degrees + 360) % 360;
				camera.setDisplayOrientation(info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT
						? (360 - pictureRotation) % 360 : pictureRotation);
				configureCamera();
			} catch (IOException | RuntimeException error) {
				releaseCamera();
				Log.e("摄像框", "开启摄像头失败", error);
			}
		}

		private void configureCamera() {
			Camera.Parameters parameters = camera.getParameters();
			parameters.setPictureFormat(ImageFormat.JPEG);
			List<Camera.Size> pictures = sortedSizes(parameters.getSupportedPictureSizes());
			List<Camera.Size> previews = sortedSizes(parameters.getSupportedPreviewSizes());
			Camera.Size picture = selectSize(pictures);
			Camera.Size preview = selectSize(previews);
			if (picture != null) {
				parameters.setPictureSize(picture.width, picture.height);
			}

			if (preview != null) {
				parameters.setPreviewSize(preview.width, preview.height);
			}

			List<String> focusModes = parameters.getSupportedFocusModes();
			if (cameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK && focusModes != null
					&& focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
				parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
			}

			setFlashMode(parameters);
			camera.setParameters(parameters);
			camera.startPreview();

			if (!sizesReported) {
				sizesReported = true;
				获得照片尺寸列表(sizeText(pictures));
				if (active()) {
					获得预览尺寸列表(sizeText(previews));
				}
			}
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			// 预览配置已在 Surface 创建时完成；无需反复触发对焦或重写拍照状态。
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			releaseCamera();
		}

		private void setFlashMode(Camera.Parameters parameters) {
			List<String> modes = parameters.getSupportedFlashModes();
			String mode = flashlight ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF;
			if (modes != null && modes.contains(mode)) {
				parameters.setFlashMode(mode);
			}
		}

		void applyFlashlight() {
			if (active() && recorder == null) {
				Camera.Parameters parameters = camera.getParameters();

				setFlashMode(parameters);
				camera.setParameters(parameters);
			}
		}

		void focus() {
			if (!active() || recorder != null || picturePending) {
				return;
			}

			Camera.Parameters parameters = camera.getParameters();
			List<String> modes = parameters.getSupportedFocusModes();
			if (modes == null || !modes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
				对焦完毕(false);
				return;
			}

			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
			camera.setParameters(parameters);
			camera.autoFocus(new Camera.AutoFocusCallback() {
				@Override
				public void onAutoFocus(boolean success, Camera focusedCamera) {
					if (active() && camera == focusedCamera) {
						focusedCamera.cancelAutoFocus();
						对焦完毕(success);
					}
				}
			});
		}

		void takePicture(final String path, int format, final int quality, final boolean returnBytes) {
			if (!active() || recorder != null || picturePending) {
				return;
			}

			if ((format != 1 && format != 2) || quality < 0 || quality > 100) {
				throw new IllegalArgumentException("照片格式应为 1（JPEG）或 2（PNG），质量范围为 0～100");
			}

			final Bitmap.CompressFormat compression = format == 1 ? Bitmap.CompressFormat.JPEG : Bitmap.CompressFormat.PNG;

			// 一次拍照固定输出参数；关闭或重开后返回的旧相机回调会被拒绝。
			picturePending = true;

			try {
				camera.takePicture(null, null, new Camera.PictureCallback() {
					@Override
					public void onPictureTaken(byte[] data, Camera capturedCamera) {
						if (!active() || camera != capturedCamera) {
							return;
						}

						byte[] encoded;

						try {
							encoded = encodePicture(data, compression, quality);
							if (!returnBytes) {
								File file = outputFile(path);
								AtomicFile target = new AtomicFile(file);
								FileOutputStream output = target.startWrite();

								try {
									output.write(encoded);
									target.finishWrite(output);
								} catch (IOException | RuntimeException error) {
									target.failWrite(output);
									throw error;
								}
							}
						} catch (IOException | RuntimeException error) {
							Log.e("摄像框", "保存照片失败", error);
							picturePending = false;
							resumePreview();
							return;
						}

						picturePending = false;
						resumePreview();

						// 资源操作完成后再通知，事件中的关闭或重开摄像头不会破坏后续操作。
						if (returnBytes) {
							拍照完毕2(encoded);
						} else {
							拍照完毕(path);
						}
					}
				});
			} catch (RuntimeException error) {
				picturePending = false;
				throw error;
			}
		}

		private byte[] encodePicture(byte[] data, Bitmap.CompressFormat format, int quality) throws IOException {
			Bitmap source = BitmapFactory.decodeByteArray(data, 0, data.length);
			if (source == null) {
				throw new IOException("无法解码摄像头照片");
			}

			Bitmap rotated = null;

			try {
				Matrix matrix = new Matrix();
				matrix.setRotate(pictureRotation);
				rotated = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				if (!rotated.compress(format, quality, output)) {
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

		private void resumePreview() {
			if (active() && recorder == null) {
				try {
					camera.startPreview();
				} catch (RuntimeException error) {
					Log.w("摄像框", "恢复预览失败", error);
				}
			}
		}

		void takeVideo(String path, int quality, boolean sound) {
			if (!active() || recorder != null || picturePending) {
				return;
			}

			int[] qualities = { CamcorderProfile.QUALITY_LOW, CamcorderProfile.QUALITY_QVGA,
					CamcorderProfile.QUALITY_CIF, CamcorderProfile.QUALITY_480P, CamcorderProfile.QUALITY_720P,
					CamcorderProfile.QUALITY_1080P, CamcorderProfile.QUALITY_HIGH };
			int profileQuality = quality >= 1 && quality <= qualities.length ? qualities[quality - 1] : CamcorderProfile.QUALITY_HIGH;
			if (!CamcorderProfile.hasProfile(cameraId, profileQuality)) {
				profileQuality = CamcorderProfile.QUALITY_HIGH;
			}

			try {
				File file = outputFile(path);
				CamcorderProfile profile = CamcorderProfile.get(cameraId, profileQuality);

				// 录像期间 Camera 交给 MediaRecorder；准备失败和停止后都释放并重新锁定。
				recorder = new MediaRecorder();
				camera.unlock();
				recorder.setCamera(camera);
				if (sound) {
					recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
				}

				recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
				recorder.setOutputFormat(profile.fileFormat);
				recorder.setVideoEncoder(profile.videoCodec);
				recorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
				recorder.setVideoFrameRate(profile.videoFrameRate);
				recorder.setVideoEncodingBitRate(profile.videoBitRate);
				if (sound) {
					recorder.setAudioEncoder(profile.audioCodec);
					recorder.setAudioEncodingBitRate(profile.audioBitRate);
					recorder.setAudioChannels(profile.audioChannels);
					recorder.setAudioSamplingRate(profile.audioSampleRate);
				}

				recorder.setOrientationHint(pictureRotation);
				recorder.setOutputFile(file.getAbsolutePath());
				recorder.setPreviewDisplay(holder.getSurface());
				recorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
					@Override
					public void onError(MediaRecorder failed, int what, int extra) {
						if (recorder == failed) {
							stopVideo();
						}
					}
				});
				recorder.prepare();
				recorder.start();
				recording = true;
			} catch (IOException | RuntimeException error) {
				stopVideo();
				Log.e("摄像框", "开始录像失败", error);
			}
		}

		void stopVideo() {
			MediaRecorder previous = recorder;
			recorder = null;
			boolean wasRecording = recording;
			recording = false;
			if (previous == null) {
				return;
			}

			try {
				if (wasRecording) {
					previous.stop();
				}
			} catch (RuntimeException error) {
				Log.w("摄像框", "停止录像失败", error);
			} finally {
				try {
					previous.reset();
				} catch (RuntimeException error) {
					Log.w("摄像框", "重置录像器失败", error);
				} finally {
					previous.release();
				}

				if (camera != null) {
					try {
						camera.lock();
						resumePreview();
					} catch (RuntimeException error) {
						Log.w("摄像框", "恢复摄像头失败", error);
					}
				}
			}
		}

		// Surface 销毁和显式关闭共享同一释放顺序：先录像器，后摄像头。
		private void releaseCamera() {
			stopVideo();
			Camera previous = camera;
			camera = null;
			picturePending = false;
			if (previous != null) {
				try {
					previous.stopPreview();
				} catch (RuntimeException error) {
					Log.w("摄像框", "停止预览失败", error);
				} finally {
					previous.release();
				}
			}
		}

		void close() {
			closed = true;
			holder.removeCallback(this);
			releaseCamera();
		}
	}
}

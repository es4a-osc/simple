package simple.runtime.collections;

import android.media.MediaRecorder;

import simple.runtime.日志输出;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.UsesPermissions;
import simple.runtime.errors.文件读写错误;

import java.io.File;
import java.io.IOException;

/**
 * 麦克风录音对象；停止及启动失败时均释放系统录音资源。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
@UsesPermissions(
	permissionNames =
		// 录制音频。
		"android.permission.RECORD_AUDIO," +
		// 兼容写入旧版系统的共享存储路径；应用私有路径不需要此权限。
		"android.permission.WRITE_EXTERNAL_STORAGE"
)
public final class 录音器 {

	private MediaRecorder recorder;
	private File outputFile;
	private int audioSource = MediaRecorder.AudioSource.DEFAULT;

	/* 对象方法 */

	/**
	 * 设置下一次录音的音频来源，默认使用系统默认来源。
	 *
	 * @param source Android MediaRecorder.AudioSource 值，1 为麦克风
	 */
	@SimpleFunction
	public void 置录音源(int source) {
		audioSource = source;
	}

	/**
	 * 开始录音，输出路径已有的文件会被覆盖；当前录音须先停止。
	 *
	 * @param path 可写的录音文件路径，缺少的父目录会自动创建
	 */
	@SimpleFunction
	public void 开始录音(String path) {
		if (recorder != null) {
			throw new IllegalArgumentException("请先停止当前录音");
		}

		File file = createFile(path);
		MediaRecorder next = new MediaRecorder();

		try {
			next.setAudioSource(audioSource);
			next.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
			next.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
			next.setOutputFile(file.getAbsolutePath());
			next.prepare();
			next.start();

			// 只有启动成功的录音器才交给停止方法管理。
			recorder = next;
			outputFile = file;
		} catch (IOException | RuntimeException error) {
			releaseRecorder(next);
			文件读写错误 failure = new 文件读写错误("启动录音失败：" + error.getMessage());
			failure.initCause(error);
			throw failure;
		}
	}

	/**
	 * 停止录音并释放资源；未开始或已经停止时不执行操作。
	 * 录音过短等原因导致停止失败时，删除无效输出并报告错误。
	 */
	@SimpleFunction
	public void 停止录音() {
		MediaRecorder current = recorder;
		File file = outputFile;

		if (current == null) {
			return;
		}

		// 先清空状态，失败后仍可重新开始或重复停止。
		recorder = null;
		outputFile = null;

		try {
			current.stop();
		} catch (RuntimeException error) {
			if (file != null && file.isFile() && !file.delete()) {
				日志输出.warning("无法删除无效录音：%s", file.getAbsolutePath());
			}

			文件读写错误 failure = new 文件读写错误("停止录音失败：" + error.getMessage());
			failure.initCause(error);
			throw failure;
		} finally {
			releaseRecorder(current);
		}
	}

	/* 内部辅助 */

	/**
	 * 检查输出文件与父目录；文件系统失败直接交给 Simple 错误分支处理。
	 */
	private File createFile(String path) {
		if (path == null || path.trim().isEmpty()) {
			throw new IllegalArgumentException("录音路径不能为空");
		}

		try {
			File file = new File(path).getAbsoluteFile();
			File parent = file.getParentFile();

			if (parent != null && !parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) {
				throw new IOException("无法创建录音目录：" + parent);
			}

			if (!file.exists() && !file.createNewFile()) {
				throw new IOException("无法创建录音文件：" + file);
			}

			if (!file.isFile() || !file.canWrite()) {
				throw new IOException("录音文件不可写：" + file);
			}

			return file;
		} catch (IOException | SecurityException error) {
			文件读写错误 failure = new 文件读写错误("创建录音文件失败：" + error.getMessage());
			failure.initCause(error);
			throw failure;
		}
	}

	/**
	 * 释放失败只记录日志，避免覆盖启动或停止时的原始错误。
	 */
	private void releaseRecorder(MediaRecorder current) {
		try {
			current.release();
		} catch (RuntimeException error) {
			日志输出.warning("释放录音器失败：%s", error.getMessage());
		}
	}
}

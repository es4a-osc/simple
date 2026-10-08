package simple.runtime.collections;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.UsesPermissions;

import java.io.File;
import java.io.IOException;

import android.media.MediaRecorder;

@SimpleObject
@UsesPermissions(permissionNames =
		// 录制音频
		"android.permission.RECORD_AUDIO," +
		// 写入外部存储器
		"android.permission.WRITE_EXTERNAL_STORAG")
public final class 录音器 {

	private MediaRecorder mr;
	private int audioSource;

	private File createFile(String path) {
		try {
			File file = new File(path);
			if (!file.exists()) {
				File parent = file.getParentFile();
				if (!parent.exists())
					parent.mkdirs();
				file.createNewFile();
			} else {
				file.delete();
			}
			return file;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@SimpleFunction
	public void 置录音源(int value) {
		audioSource = value;
	}

	@SimpleFunction
	public void 开始录音(String path) {
		mr = new MediaRecorder();
		File file = createFile(path);
		mr.setAudioSource(audioSource);
		mr.setOutputFormat(0);
		mr.setAudioEncoder(0);
		mr.setOutputFile(file.getAbsolutePath());
		try {
			mr.prepare();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		mr.start();
	}

	@SimpleFunction
	public void 停止录音() {
		if (mr != null) {
			mr.stop();
			mr.release();
			mr = null;
		}
	}
}

package simple.runtime.components.impl.android.util;

import java.io.File;
import java.io.IOException;

import android.graphics.drawable.Drawable;

/**
 * 文件辅助方法。
 *
 * @author 树先生 xhwsd@qq.com
 */
public class FileUtil {

	/**
	 * 检验指定路径是否是符合文件路径。
	 *
	 * @param path 外部文件路径，格式为{@code /文件名}。
	 * @return 符合返回{@code true}，否则返回{@code false}。
	 */
	public static boolean isFilePath(String path) {
		return (path.charAt(0) == '/' && path.length() > 1);
	}

	/**
	 * 取指定文件名可绘制对象。
	 *
	 * @param fileName 外部文件名，格式为{@code /文件名}。
	 * @return 可绘制对象。
	 * @throws IOException
	 */
	public static Drawable getFileDrawable(String fileName) throws IOException {
		File file = new File(fileName);
		// 文件是否存在且非文件目录
		if (file.exists() && !file.isDirectory()) {
			return Drawable.createFromPath(fileName);
		}
		// 没找限定资源，抛出异常
		throw new IOException(fileName);
	}
}

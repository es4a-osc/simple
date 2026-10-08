package simple.runtime.components.impl.android.util;

import simple.runtime.android.MainActivity;

import android.graphics.drawable.Drawable;

import java.io.IOException;

/**
 * 资产相关辅助方法。
 *
 * @author 树先生 xhwsd@qq.com
 */
public class AssetsUtil {
	/*
	AssetManager：
	{@link https://developer.android.google.cn/reference/android/content/res/AssetManager}
	*/

	/**
	 * 检验指定路径是否是符合资产路径。
	 *
	 * @param path 资产路径，格式为{@code 文件名}。
	 * @return 符合返回{@code true}，否则返回{@code false}。
	 */
	public static boolean isAssetsPath(String path) {
		return path.charAt(0) != '/';
	}

	/**
	 * 取指定资产路径可绘制对象。
	 *
	 * @param fileName 资产文件名，格式为{@code 文件名}。
	 * @return 可绘制对象。
	 * @throws IOException
	 */
	public static Drawable getAssetsDrawable(String fileName) throws IOException {
		return Drawable.createFromStream(MainActivity.getContext().getAssets().open(fileName), fileName);
	}

	/**
	 * 返回给定路径上所有资产的 String 数组。
	 *
	 * @param fileName 资产文件名，格式为{@code 文件名}。
	 * @return 字符串数组。
	 * @throws IOException
	 */
	public static String[] getAssetsList(String fileName) throws IOException {
		return MainActivity.getContext().getAssets().list(fileName);
	}
}

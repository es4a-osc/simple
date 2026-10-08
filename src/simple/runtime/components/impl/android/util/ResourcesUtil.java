package simple.runtime.components.impl.android.util;

import simple.runtime.android.MainActivity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Build;

/**
 * 资源相关辅助方法。
 *
 * @author 树先生 xhwsd@qq.com
 */
public class ResourcesUtil {
	/*
	android.content.res.Resources：
	https://developer.android.google.cn/reference/android/content/res/Resources
	*/

	private static final String PREFIX_DRAWABLE = "@drawable/";
	private static final Pattern PATTERN_RES = Pattern.compile("@(\\S+)/(\\S+)");

	/**
	 * 检验指定路径是否是符合可绘制资源路径。
	 *
	 * @param path 资源路径，格式为{@code @drawable/可绘资源名}。
	 * @return 符合返回{@code true}，否则返回{@code false}。
	 */
	public static boolean isDrawablePath(String path) {
		return (path.startsWith(PREFIX_DRAWABLE) && path.length() > PREFIX_DRAWABLE.length());
	}

	/**
	 * 取指定资源路径的可绘制对象。
	 *
	 * @param path 资源路径，格式为{@code @drawable/可绘资源名}。
	 * @return 可绘制对象。
	 * @throws NotFoundException
	 */
	public static Drawable getResourcesDrawable(String path) throws NotFoundException {
		MainActivity context = MainActivity.getContext();
		Resources resources = context.getResources();
		int resid = getResourcesId(resources, path, context.getPackageName());
		if (resid > 0) {
			return getResourcesDrawable(resources, resid);
		}
		throw new NotFoundException(path);
	}

	/**
	 * 取指定资源标识的可绘制对象。
	 *
	 * @param resid 资源标识。
	 * @return 可绘制对象。
	 * @throws NotFoundException
	 */
	public static Drawable getResourcesDrawable(int resid) throws NotFoundException {
		return getResourcesDrawable(MainActivity.getContext().getResources(), resid);
	}

	/**
	 * 取指定资源标识的可绘制对象。
	 *
	 * @param resources 资源对象。
	 * @param resid 资源标识。
	 * @return 返回可绘制对象。
	 * @throws NotFoundException
	 */
	@SuppressWarnings("deprecation") // 忽略该方法调用已弃用的API
	public static Drawable getResourcesDrawable(Resources resources, int resid) throws NotFoundException {
		// API21兼容处理
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			// 需要API21：getDrawable(int, Theme)
			return resources.getDrawable(resid, null);
		} else {
			// API22弃用：getDrawable(int)
			return resources.getDrawable(resid);
		}
	}

	/**
	 * 取指定资源名称及类型的资源标识。
	 *
	 * @param path 资源路径，格式为{@code @类型名/资源名}。
	 * @return 成功返回资源标识，否则返回{@code 0}。
	 */
	public static int getResourcesId(String path) {
		MainActivity context = MainActivity.getContext();
		return getResourcesId(context.getResources(), path, context.getPackageName());
	}

	/**
	 * 取指定资源名称及类型的资源标识。
	 *
	 * <p>R.layout.main其对应关系：layout是资源类型，main是资源名称。
	 *
	 * @param name 资源名称，例如：icon、button1
	 * @param type 资源类型，例如：drawable、layout、id、string、anim、color。
	 * @return 成功返回资源标识，否则返回{@code 0}。
	 */
	public static int getResourcesId(String name, String type) {
		MainActivity context = MainActivity.getContext();
		return getResourcesId(context.getResources(), name, type, context.getPackageName());
	}

	/**
	 * 取指定资源路径的资源标识。
	 *
	 * @param resources 资源对象。
	 * @param path 资源路径，格式为{@code @类型名/资源名}。
	 * @param packageName 包名。
	 * @return 成功返回资源标识，否则返回{@code 0}。
	 */
	public static int getResourcesId(Resources resources, String path, String packageName) {
		Matcher matcher = PATTERN_RES.matcher(path);
		if (matcher.find()) {
			return getResourcesId(resources, matcher.group(2), matcher.group(1), packageName);
		}
		return 0;
	}

	/**
	 * 取指定资源名称及类型的资源标识。
	 *
	 * <p>R.layout.main其对应关系：layout是资源类型，main是资源名称。
	 *
	 * @param resources 资源对象。
	 * @param name 资源名称，例如：icon、button1
	 * @param type 资源类型，例如：drawable、layout、id、string、anim、color。
	 * @param packageName 包名。
	 * @return 成功返回资源标识，否则返回{@code 0}。
	 */
	public static int getResourcesId(Resources resources, String name, String type, String packageName) {
		return resources.getIdentifier(name, type, packageName);
	}

	/**
	 * 取指定资源索引的资源名称。
	 *
	 * @param resid 资源标识。
	 * @return 成功返回资源名称。
	 * @throws NotFoundException
	 */
	public static String getResourceName(int resid) throws NotFoundException {
		return (MainActivity.getContext().getResources()).getResourceEntryName(resid);
	}

	/**
	 * 取指定资源索引的资源类型名称。
	 *
	 * @param resid 资源标识。
	 * @return 成功返回资源类型名称。
	 * @throws NotFoundException
	 */
	public static String getResourceTypeName(int resid) throws NotFoundException {
		return MainActivity.getContext().getResources().getResourceTypeName(resid);
	}

	/**
	 * 获取R.String类型下的对应文本，一般用于多国语言配置。
	 *
	 * @param resid 资源标识。
	 * @return 成功返回取资源文本，否则返回{@code null}
	 */
	public static String getResourcesText(int resid) throws NotFoundException {
		return MainActivity.getContext().getResources().getText(resid).toString();
	}
}

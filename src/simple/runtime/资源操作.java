package simple.runtime;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.components.impl.android.util.ResourcesUtil;

import android.content.res.Resources.NotFoundException;

/**
 * 资源（res）相关函数。
 *
 * @author 树先生 xhwsd@qq.con
 */
@SimpleObject
public final class 资源操作 {

	/**
	 * 取指定资源路径的资源标识。
	 *
	 * @param path 资源路径，格式为{@code @defType/name}。
	 * @return 成功返回资源标识，否则返回{@code 0}。
	 */
	@SimpleFunction
	public static int 到资源标识(String path) {
		return ResourcesUtil.getResourcesId(path);
	}

	/**
	 *  取指定资源名称及类型的资源标识。
	 *
	 * <p>R.layout.main其对应关系：layout是资源类型，main是资源名称。
	 *
	 * @param name 资源名称，例如：icon、button1
	 * @param type 资源类型，例如：drawable、layout、id、string、anim、color。
	 * @return 成功返回资源标识，否则返回{@code 0}。
	 */
	@SimpleFunction
	public static int 取资源标识(String name, String type) {
		return ResourcesUtil.getResourcesId(name, type);
	}

	/**
	 * 取指定资源标识的资源类型名称。
	 *
	 * @param resid 资源标识。
	 * @return 成功返回资源类型名称，否则返回空文本。
	 */
	@SimpleFunction
	public static String 取资源类型(int resid) {
		try {
			return ResourcesUtil.getResourceTypeName(resid);
		} catch (NotFoundException e) {
			return "";
		}
	}

	/**
	 * 取指定资源标识的资源名称。
	 *
	 * @param resid 资源标识。
	 * @return 成功返回资源名称，否则返回空文本。
	 */
	@SimpleFunction
	public static String 取资源名称(int resid) {
		try {
			return ResourcesUtil.getResourceName(resid);
		} catch (NotFoundException e) {
			return "";
		}
	}

	/**
	 * 取指定资源标识对应的文本，一般用于多国语言配置。
	 *
	 * @param resid 资源标识。
	 * @return 成功返回资源文本，否则返回空文本。
	 */
	@SimpleFunction
	public static String 取资源文本(int resid) {
		try {
			return ResourcesUtil.getResourcesText(resid);
		} catch (NotFoundException e) {
			return "";
		}
	}

	/**
	 * 检验指定资源标识的资源是否存在。
	 *
	 * @param resid 资源索引。
	 * @return 存在返回{@code true}，否则返回{@code false}。
	 */
	@SimpleFunction
	public static boolean 资源是否存在(int resid) {
		return 取资源类型(resid).isEmpty() == false;
	}
}

package simple.runtime;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.collections.JSON值;

/**
 * JSON相关函数。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class JSON操作 {

	/**
	 * 创建新JSON值空。
	 *
	 * @return JSON值空实例。
	 */
	@SimpleFunction
	public static JSON值 创建JSON空() {
		return new JSON值(JSON值.值类型_空);
	}

	/**
	 * 创建新JSON值对象。
	 *
	 * @return JSON值对象实例。
	 */
	@SimpleFunction
	public static JSON值 创建JSON对象() {
		return new JSON值(JSON值.值类型_对象);
	}

	/**
	 * 创建新JSON值对象。
	 *
	 * @return JSON值数组实例。
	 */
	@SimpleFunction
	public static JSON值 创建JSON数组() {
		return new JSON值(JSON值.值类型_数组);
	}

	/**
	 * 创建新JSON值对象。
	 *
	 * @param text JSON文本。
	 * @return 新对象实例。
	 */
	@SimpleFunction
	public static JSON值 解析JSON文本(String text) {
		return new JSON值(text);
	}

	/**
	 * 格式化JSON文本。
	 *
	 * @param text JSON文本。
	 * @param indentSpaces 缩进空格数
	 * @return 格式化后的JSON文本。
	 */
	@SimpleFunction
	public static String 格式化JSON文本(String text, int indentSpaces) {
		JSON值 json = new JSON值(text);
		return json.到文本(indentSpaces);
	}
}

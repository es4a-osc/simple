package simple.runtime;

import android.graphics.Color;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

/**
 * 颜色相关函数。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 颜色操作 {

	/**
	 * 解析颜色文本, 返回对应的颜色值。
	 *
	 * <pre>
	 *     支持的格式:
	 *     #RRGGBB
	 *     #AARRGGBB
	 * </pre>
	 *
	 * @param color argb/rgb颜色文本。
	 * @return argb/rgb颜色值。
	 */
	@SimpleFunction
	public static int 取颜色(String color) {
		if (color.charAt(0) == '#') {
			// Use a long to avoid rollovers on #ffXXXXXX
			long value = Long.parseLong(color.substring(1), 16);
			if (color.length() == 7) {
				// 设置透明度
				value |= 0x00000000ff000000;
			} else if (color.length() == 9) {
				return (int) value;
			}
		}
		throw new IllegalArgumentException("未知颜色");
	}

	/**
	 * 将透明度、红色、绿色、蓝色转为ARGB颜色值。
	 *
	 * @param a 透明度。
	 * @param r 红色。
	 * @param g 绿色。
	 * @param b 蓝色。
	 * @return ARGB颜色值。
	 */
	@SimpleFunction
	public static int 到颜色(int a, int r, int g, int b) {
		return Color.argb(a, r, g, b);
	}

	/**
	 * 取ARGB颜色值中的透明度。
	 *
	 * @param color ARGB颜色值
	 * @return 透明度。
	 */
	@SimpleFunction
	public static int 取透明度(int color) {
		return Color.alpha(color);
	}

	/**
	 * 取ARGB颜色值中的红色值。
	 *
	 * @param color ARGB颜色值
	 * @return 红色值。
	 */
	@SimpleFunction
	public static int 取红色(int color) {
		return Color.red(color);
	}

	/**
	 * 取ARGB颜色值中的绿色值。
	 *
	 * @param color ARGB颜色值
	 * @return 绿色值。
	 */
	@SimpleFunction
	public static int 取绿色(int color) {
		return Color.green(color);
	}

	/**
	 * 取ARGB颜色值中的蓝色值。
	 *
	 * @param color ARGB颜色值
	 * @return 蓝色值。
	 */
	@SimpleFunction
	public static int 取蓝色(int color) {
		return Color.blue(color);
	}
}

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
	 * 静态方法集，不创建实例。
	 */
	private 颜色操作() {
	}

	/**
	 * 将 #RRGGBB 或 #AARRGGBB 文本解析为 ARGB 颜色值。
	 *
	 * <p>六位格式默认完全不透明；八位格式保留指定的透明度，十六进制字母不区分大小写。</p>
	 *
	 * @param color #RRGGBB 或 #AARRGGBB 格式的颜色文本。
	 * @return 32 位 ARGB 颜色值，最高位为 1 时整数值为负数。
	 * @throws IllegalArgumentException 颜色格式或十六进制数值无效。
	 */
	@SimpleFunction
	public static int 取颜色(String color) {
		if (color.charAt(0) == '#') {
			// 先按 long 解析，允许最高位为 1 的八位 ARGB 值。
			long value = Long.parseLong(color.substring(1), 16);
			if (color.length() == 7) {
				// 六位 RGB 未指定透明度，补齐为完全不透明。
				value |= 0x00000000ff000000;
				return (int) value;
			} else if (color.length() == 9) {
				return (int) value;
			}
		}

		throw new IllegalArgumentException("未知颜色");
	}

	/**
	 * 将红、绿、蓝分量合成为完全不透明的 ARGB 颜色值。
	 *
	 * @param r 红色分量，范围 0–255。
	 * @param g 绿色分量，范围 0–255。
	 * @param b 蓝色分量，范围 0–255。
	 * @return 透明度为 255 的颜色值，不校正超出范围的颜色分量。
	 */
	@SimpleFunction
	public static int 到颜色(int r, int g, int b) {
		return 到颜色(255, r, g, b);
	}

	/**
	 * 将透明度、红色、绿色、蓝色分量合成为 ARGB 颜色值。
	 *
	 * @param a 透明度，范围 0–255；0 为完全透明，255 为完全不透明。
	 * @param r 红色分量，范围 0–255。
	 * @param g 绿色分量，范围 0–255。
	 * @param b 蓝色分量，范围 0–255。
	 * @return 32 位 ARGB 颜色值，不对超出范围的分量进行校正。
	 */
	@SimpleFunction
	public static int 到颜色(int a, int r, int g, int b) {
		return Color.argb(a, r, g, b);
	}

	/**
	 * 读取 ARGB 颜色值中的透明度分量。
	 *
	 * @param color 32 位 ARGB 颜色值。
	 * @return 透明度，范围 0–255；0 为完全透明，255 为完全不透明。
	 */
	@SimpleFunction
	public static int 取透明度(int color) {
		return Color.alpha(color);
	}

	/**
	 * 读取 ARGB 颜色值中的红色分量。
	 *
	 * @param color 32 位 ARGB 颜色值。
	 * @return 红色分量，范围 0–255。
	 */
	@SimpleFunction
	public static int 取红色(int color) {
		return Color.red(color);
	}

	/**
	 * 读取 ARGB 颜色值中的绿色分量。
	 *
	 * @param color 32 位 ARGB 颜色值。
	 * @return 绿色分量，范围 0–255。
	 */
	@SimpleFunction
	public static int 取绿色(int color) {
		return Color.green(color);
	}

	/**
	 * 读取 ARGB 颜色值中的蓝色分量。
	 *
	 * @param color 32 位 ARGB 颜色值。
	 * @return 蓝色分量，范围 0–255。
	 */
	@SimpleFunction
	public static int 取蓝色(int color) {
		return Color.blue(color);
	}
}

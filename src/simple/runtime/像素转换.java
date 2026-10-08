package simple.runtime;

import android.util.TypedValue;

import simple.runtime.android.MainActivity;
import simple.runtime.annotations.SimpleDataElement;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.errors.转换错误;
import simple.runtime.variants.ByteVariant;
import simple.runtime.variants.IntegerVariant;
import simple.runtime.variants.LongVariant;
import simple.runtime.variants.ShortVariant;
import simple.runtime.variants.StringVariant;
import simple.runtime.variants.Variant;

import java.util.Locale;

/**
 * 像素相关函数。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 像素转换 {
	/*
	参考来源：
	https://www.cnblogs.com/xilinch/p/4444833.html

	TypedValue：
	https://developer.android.google.cn/reference/android/util/TypedValue
	*/

	@SimpleDataElement
	public static final int 像素_绝对 = TypedValue.COMPLEX_UNIT_PX;
	@SimpleDataElement
	public static final int 像素_相对 = TypedValue.COMPLEX_UNIT_DIP;
	@SimpleDataElement
	public static final int 像素_缩放 = TypedValue.COMPLEX_UNIT_SP;

	private 像素转换() {
	}

	/**
	 * 将指定类型的像素值转换为绝对像素（px）值。
	 *
	 * @param value 像素值
	 * @param unit 类型，可选值钱请参考{@link 像素转换#像素_像素_绝对}等
	 * @return  绝对像素（dp）值
	 */
	@SimpleFunction
	public static int 转绝对像素(float value, int unit) {
		return (int) (TypedValue.applyDimension(unit, value,
				MainActivity.getContext().getResources().getDisplayMetrics()));
	}

	/**
	 * 将相对像素（dip）值转换为绝对像素（px）值
	 *
	 * @param dipValue  相对像素（dp）
	 * @return  绝对像素（dp）值
	 */
	@SimpleFunction
	public static int 到绝对像素(float dipValue) {
		return (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue,
				MainActivity.getContext().getResources().getDisplayMetrics()));
	}

	/**
	 * 将绝对像素（px）值转换为相对像素（dp）值。
	 *
	 * @param pxValue 绝对像素（px）值
	 * @return 相对像素（dip）值
	 */
	@SimpleFunction
	public static int 到相对像素(float pxValue) {
		// 取设备屏幕的密度
		final float scale = MainActivity.getContext().getResources().getDisplayMetrics().density;
		return (int) (pxValue / scale + 0.5f);
	}

	/**
	 * 将绝对像素（px）值转换为缩放像素（sp）值。
	 *
	 * @param pxValue 绝对像素（px）值
	 * @return  缩放像素（sp）值
	 */
	@SimpleFunction
	public static int 到缩放像素(float pxValue) {
		// 取设备屏幕的缩放密度
		final float fontScale = MainActivity.getContext().getResources().getDisplayMetrics().scaledDensity;
		return (int) (pxValue / fontScale + 0.5f);
	}

	/**
	 * 将整数或带单位的文本解析为绝对像素（px）。
	 *
	 * <p>整数直接视为绝对像素。文本支持纯数值及{@code px}、{@code dp}、{@code dip}、
	 * {@code sp}后缀；纯数值和{@code px}直接视为绝对像素，{@code dp}和{@code dip}按屏幕
	 * 密度换算，{@code sp}按屏幕缩放密度换算。单位不区分大小写。
	 *
	 * @param value 整数，或形如{@code "10"}、{@code "10px"}、
	 *              {@code "10dp"}、{@code "10dip"}、{@code "10sp"}的文本
	 * @return 换算后的绝对像素（px）
	 * @throws 转换错误 值不是整数或文本，或者文本中的数值、单位无效
	 */
	@SimpleFunction
	public static int 解析像素(Variant value) {
		// Simple数字字面量会选择能容纳当前值的最小整数类型，因此必须接受所有整数变体。
		if (value instanceof ByteVariant || value instanceof ShortVariant || value instanceof IntegerVariant) {
			return value.getInteger();
		}
		if (value instanceof LongVariant) {
			long integer = value.getLong();
			if (integer < Integer.MIN_VALUE || integer > Integer.MAX_VALUE) {
				throw new 转换错误();
			}
			return (int) integer;
		}

		// 仅文本允许携带单位，避免其它变体被隐式转换后产生歧义。
		if (!(value instanceof StringVariant)) {
			throw new 转换错误();
		}

		String text = value.getString();
		if (text == null) {
			throw new 转换错误();
		}

		// 统一清理空白和单位大小写，再从末尾识别单位。
		text = text.trim().toLowerCase(Locale.US);
		int unit = TypedValue.COMPLEX_UNIT_PX;
		String numberText = text;
		if (text.endsWith("dip") || text.endsWith("dp")) {
			unit = TypedValue.COMPLEX_UNIT_DIP;
			numberText = text.substring(0, text.length() - (text.endsWith("dip") ? 3 : 2)).trim();
		} else if (text.endsWith("px") || text.endsWith("sp")) {
			unit = text.endsWith("px")
					? TypedValue.COMPLEX_UNIT_PX
					: TypedValue.COMPLEX_UNIT_SP;
			numberText = text.substring(0, text.length() - 2).trim();
		}

		try {
			float number = Float.parseFloat(numberText);
			if (Float.isNaN(number) || Float.isInfinite(number)) {
				throw new 转换错误();
			}

			// 绝对像素不依赖屏幕密度，也不要求MainActivity已经提供Context。
			if (unit == TypedValue.COMPLEX_UNIT_PX) {
				if (number > Integer.MAX_VALUE || number < Integer.MIN_VALUE) {
					throw new 转换错误();
				}
				return (int) number;
			}

			// dp、dip和sp必须结合当前设备的显示指标换算为Android实际使用的px。
			float absolutePixels = TypedValue.applyDimension(unit, number,
					MainActivity.getContext().getResources().getDisplayMetrics());
			if (Float.isNaN(absolutePixels) || Float.isInfinite(absolutePixels) ||
					absolutePixels > Integer.MAX_VALUE || absolutePixels < Integer.MIN_VALUE) {
				throw new 转换错误();
			}
			return (int) absolutePixels;
		} catch (NumberFormatException e) {
			throw new 转换错误();
		}
	}
}

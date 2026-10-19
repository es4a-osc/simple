/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simple.runtime;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.Random;

import simple.runtime.annotations.SimpleDataElement;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.variants.IntegerVariant;
import simple.runtime.variants.Variant;

/**
 * 实现各种数学运行时函数。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 数值运算 {

	private final static Random randomGenerator = new SecureRandom();

	/* 数学常量 */

	/**
	 * 欧拉常量。
	 */
	@SimpleDataElement
	public static final double E = java.lang.Math.E;

	/**
	 * 圆周率 Pi 常量。
	 */
	@SimpleDataElement
	public static final double PI = java.lang.Math.PI;

	/**
	 * 静态方法集，不创建实例。
	 */
	private 数值运算() {  // COV_NF_LINE
	}                 // COV_NF_LINE

	/* 取值与比较 */

	/**
	 * 返回给定值的绝对值。
	 *
	 * @param v  值
	 * @return  绝对值
	 */
	@SimpleFunction
	public static Variant 取绝对值(Variant v) {
		if (v.cmp(IntegerVariant.getIntegerVariant(0)) < 0) {
			v = v.mul(IntegerVariant.getIntegerVariant(-1));
		}

		return v;
	}

	/**
	 * 指示给定值的符号。
	 *
	 * @param v 值
	 * @return 1 表示正值，0 表示零，-1 表示负值
	 */
	@SimpleFunction
	public static int 取符号(double v) {
		return (int)java.lang.Math.signum(v);
	}

	/**
	 * 返回两个值中的较大值。
	 *
	 * @param v1 第一值
	 * @param v2 第二值
	 * @return {@code v1}和{@code v2}的较大值
	 */
	@SimpleFunction
	public static Variant 取最大值(Variant v1, Variant v2) {
		// 需要确保变量包含数值：如果没有，调用 {@code getDouble()}将导致运行时错误
		v1.getDouble();
		v2.getDouble();

		return v1.cmp(v2) < 0 ? v2 : v1;
	}

	/**
	 * 返回两个值中的较小值。
	 *
	 * @param v1 第一值
	 * @param v2 第二值
	 * @return {@code v1}和{@code v2}的较小值
	 */
	@SimpleFunction
	public static Variant 取最小值(Variant v1, Variant v2) {
		// 需要确保变量包含数值：如果没有，调用{@code getDouble()}将导致运行时错误
		v1.getDouble();
		v2.getDouble();

		return v1.cmp(v2) >= 0 ? v2 : v1;
	}

	/* 取整与舍入 */

	/**
	 * 返回给定数字的整数部分。
	 *
	 * @param v 值
	 * @return {@code v}的整数部分
	 */
	@SimpleFunction
	public static long 取整数(Variant v) {
		return v.getLong();
	}

	/**
	 * 按指定小数位数进行 HALF_UP 舍入，中点向远离零的方向取舍。
	 *
	 * <p>例如 1.005 保留两位得到 1.01，-1.005 得到 -1.01；
	 * 位数为零时舍入到整数，负位数表示舍入到十位、百位等。
	 * 按输入双精度数的十进制表示舍入，结果仍可能有二进制浮点误差；
	 * 舍入结果为零时返回正零。需要精确金额文本时可使用{@link 文本操作#格式化金额(String)}。</p>
	 *
	 * @param value 待舍入的有限双精度数值。
	 * @param scale 要保留的小数位数，允许负数。
	 * @return 舍入后的双精度数值；最终结果超出双精度范围时返回相应符号的无穷大。
	 * @throws NumberFormatException 数值为 NaN 或无穷大。
	 * @throws ArithmeticException 数值规模超出可处理范围。
	 */
	@SimpleFunction
	public static double 四舍五入(double value, int scale) {
		return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
	}

	/* 开方、指数与对数 */

	/**
	 * 返回给定值的平方根。
	 *
	 * @param v 值
	 * @return {@code v}的平方根
	 */
	@SimpleFunction
	public static double 求平方根(double v) {
		return java.lang.Math.sqrt(v);
	}

	/**
	 * 求给定值的实数立方根，支持负数。
	 *
	 * @param v 要求立方根的数值。
	 * @return 实数立方根；输入为 NaN 时返回 NaN，无穷大或零保留其符号。
	 */
	@SimpleFunction
	public static double 求立方根(double v) {
		return java.lang.Math.cbrt(v);
	}

	/**
	 * 将 e（euler 的常数）返回到给定值的幂。
	 *
	 * @param v 值
	 * @return e to the power of {@code v}
	 */
	@SimpleFunction
	public static double 求反对数(double v) {
		return java.lang.Math.exp(v);
	}

	/**
	 * 返回给定数字的自然对数。
	 *
	 * @param v 值
	 * @return {@code v}的自然对数
	 */
	@SimpleFunction
	public static double 求自然对数(double v) {
		return java.lang.Math.log(v);
	}

	/* 角度换算 */

	/**
	 * 将以度为单位的角度转换为以弧度表示的近似值。
	 *
	 * @param d 以度为单位的值
	 * @return 弧度逼近{@code d}度
	 */
	@SimpleFunction
	public static double 角度转弧度(double d) {
		return java.lang.Math.toRadians(d);
	}

	/**
	 * 将以弧度为单位的角度转换为以度为单位的近似值。
	 *
	 * @param r 以弧度表示的值
	 * @return 度数逼近{@code r}​​弧度
	 */
	@SimpleFunction
	public static double 弧度转角度(double r) {
		return java.lang.Math.toDegrees(r);
	}

	/* 三角函数与几何计算 */

	/**
	 * 返回给定值的正弦。
	 *
	 * @param v 值
	 * @return {@code v}的正弦
	 */
	@SimpleFunction
	public static double 求正弦(double v) {
		return java.lang.Math.sin(v);
	}

	/**
	 * 返回给定值的余弦值。
	 *
	 * @param v 值
	 * @return {@code v}的余弦
	 */
	@SimpleFunction
	public static double 求余弦(double v) {
		return java.lang.Math.cos(v);
	}

	/**
	 * 返回给定值的正切值。
	 *
	 * @param v 值
	 * @return {@code v}的正切
	 */
	@SimpleFunction
	public static double 求正切(double v) {
		return java.lang.Math.tan(v);
	}

	/**
	 * 根据正弦比值求反正弦。
	 *
	 * @param v 正弦比值，有效范围为 [-1, 1]。
	 * @return 反正弦值，单位为弧度，范围为 [-π/2, π/2]；输入越界或为 NaN 时返回 NaN，保留零的符号。
	 */
	@SimpleFunction
	public static double 求反正弦(double v) {
		return java.lang.Math.asin(v);
	}

	/**
	 * 根据余弦比值求反余弦。
	 *
	 * @param v 余弦比值，有效范围为 [-1, 1]。
	 * @return 反余弦值，单位为弧度，范围为 [0, π]；输入越界或为 NaN 时返回 NaN。
	 */
	@SimpleFunction
	public static double 求反余弦(double v) {
		return java.lang.Math.acos(v);
	}

	/**
	 * 根据正切比值求反正切。
	 *
	 * @param v 正切比值。
	 * @return 反正切值，单位为弧度，范围为 [-π/2, π/2]。
	 */
	@SimpleFunction
	public static double 求反正切(double v) {
		return java.lang.Math.atan(v);
	}

	/**
	 * 根据纵坐标 y 和横坐标 x 求四象限反正切，参数顺序为 y、x。
	 *
	 * @param y 纵坐标，第一个参数。
	 * @param x 横坐标，第二个参数。
	 * @return 点 (x, y) 相对 X 轴正方向的方向角，单位为弧度，范围为 [-π, π]。
	 */
	@SimpleFunction
	public static double 求反正切2(double y, double x) {
		return java.lang.Math.atan2(y, x);
	}

	/**
	 * 根据两条直角边的数值求斜边长度，避免中间平方运算导致不必要的溢出或下溢。
	 *
	 * @param x 第一条直角边的数值，负数按绝对值处理。
	 * @param y 第二条直角边的数值，负数按绝对值处理。
	 * @return 非负斜边长度；任一参数为无穷大时返回正无穷大，否则任一参数为 NaN 时返回 NaN。
	 *         两个参数均为零时返回正零，最终结果超出双精度范围时返回正无穷大。
	 */
	@SimpleFunction
	public static double 求斜边(double x, double y) {
		return java.lang.Math.hypot(x, y);
	}

	/* 随机数 */

	/**
	 * 返回介于 0.0（含）和 1.0（不含）之间的随机数。
	 *
	 * @return 随机数（介于 0.0 和 1.0 之间）
	 */
	@SimpleFunction
	public static double 取随机数() {
		return randomGenerator.nextDouble();
	}

	/**
	 * 返回指定闭区间内均匀选取的随机整数，允许负数和完整整数范围。
	 *
	 * @param minimum 最小值，包含该值。
	 * @param maximum 最大值，包含该值；不得小于最小值。
	 * @return 位于 [minimum, maximum] 的整数；上下限相同时直接返回该值。
	 * @throws IllegalArgumentException 最小值大于最大值。
	 */
	@SimpleFunction
	public static int 取随机数(int minimum, int maximum) {
		if (minimum > maximum) {
			throw new IllegalArgumentException("最小值不得大于最大值");
		}
		if (minimum == maximum) {
			return minimum;
		}

		// 使用长整数计算闭区间大小，避免端点之差或加一溢出。
		long range = (long) maximum - minimum + 1;
		if (range <= Integer.MAX_VALUE) {
			return minimum + randomGenerator.nextInt((int) range);
		}

		// 宽区间至少覆盖一半整数，直接筛选可避免取模偏差。
		int value;
		do {
			value = randomGenerator.nextInt();
		} while (value < minimum || value > maximum);
		return value;
	}
}

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

import java.util.Random;

import simple.runtime.annotations.SimpleDataElement;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.variants.IntegerVariant;
import simple.runtime.variants.Variant;

import java.security.SecureRandom;

/**
 * 实现各种数学运行时函数。
 *
 * @author Herbert Czymontek
 */
@SimpleObject
public final class 算术运算 {

	private final static Random randomGenerator = new SecureRandom();

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

	private 算术运算() {  // COV_NF_LINE
	}                 // COV_NF_LINE

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
	 * 返回给定值的反正切值。
	 *
	 * @param v  值
	 * @return  {@code v}的反正切值
	 */
	@SimpleFunction
	public static double 求反正切(double v) {
		return java.lang.Math.atan(v);
	}

 /**
	* 返回从直角坐标（x，y）到极坐标（r，theta>）的转换的角度θ。
	*
	* @param   y  纵坐标
	* @param   x  横坐标
	* @return  极坐标中点（r，theta）的 theta 分量，对应于笛卡尔坐标系中的点（x，y）
	*/
	@SimpleFunction
	public static double 求反正切2(double y, double x) {
		return java.lang.Math.atan2(y,x);
	}

	/**
	 * 返回给定值的余弦值。
	 *
	 * @param v 值
	 * @return  {@code v}的余弦
	 */
	@SimpleFunction
	public static double 求余弦(double v) {
		return java.lang.Math.cos(v);
	}

	/**
	 * 将 e（euler 的常数）返回到给定值的幂。
	 *
	 * @param v  值
	 * @return  e to the power of {@code v}
	 */
	@SimpleFunction
	public static double 求反对数(double v) {
		return java.lang.Math.exp(v);
	}

	/**
	 * 返回给定数字的整数部分。
	 *
	 * @param v  值
	 * @return  {@code v}的整数部分
	 */
	@SimpleFunction
	public static long 取整数(Variant v) {
		return v.getLong();
	}

	/**
	 * 返回给定数字的自然对数。
	 *
	 * @param v  值
	 * @return  {@code v}的自然对数
	 */
	@SimpleFunction
	public static double 求自然对数(double v) {
		return java.lang.Math.log(v);
	}

	/**
	 * 返回两个值中的较大值。
	 *
	 * @param v1  第一值
	 * @param v2  第二值
	 * @return  {@code v1}和{@code v2}的较大值
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
	 * @param v1  第一值
	 * @param v2  第二值
	 * @return  {@code v1}和{@code v2}的较小值
	 */
	@SimpleFunction
	public static Variant 取最小值(Variant v1, Variant v2) {
		// 需要确保变量包含数值：如果没有，调用{@code getDouble()}将导致运行时错误
		v1.getDouble();
		v2.getDouble();

		return v1.cmp(v2) >= 0 ? v2 : v1;
	}

	/**
	 *  返回介于 0.0（含）和 1.0（不含）之间的随机数。
	 *
	 * @return   随机数（介于 0.0 和 1.0 之间）
	 */
	@SimpleFunction
	public static double 取随机数() {
		return randomGenerator.nextDouble();
	}

	/**
	 * 返回给定值的正弦。
	 *
	 * @param v  值
	 * @return  {@code v}的正弦
	 */
	@SimpleFunction
	public static double 求正弦(double v) {
		return java.lang.Math.sin(v);
	}

	/**
	 * 指示给定值的符号。
	 *
	 * @param v  值
	 * @return  1 表示正值，0 表示零，-1 表示负值
	 */
	@SimpleFunction
	public static int 取符号(double v) {
		return (int)java.lang.Math.signum(v);
	}

	/**
	 * 返回给定值的平方根。
	 *
	 * @param v  值
	 * @return  {@code v}的平方根
	 */
	@SimpleFunction
	public static double 求平方根(double v) {
		return java.lang.Math.sqrt(v);
	}

	/**
	 * 返回给定值的正切值。
	 *
	 * @param v  值
	 * @return  {@code v}的正切
	 */
	@SimpleFunction
	public static double 求正切(double v) {
		return java.lang.Math.tan(v);
	}

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
}

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


import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.errors.转换错误;
import simple.runtime.helpers.ConvHelpers;
import simple.runtime.variants.Variant;

/**
 * 实现各种与转换相关的运行时函数。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 数据转换 {

	/**
	 * 静态方法集，不创建实例。
	 */
	private 数据转换() {
	}

	/**
	 * 将值转为逻辑值。
	 *
	 * @param value 值。
	 * @return 成功返回逻辑值，否则返回{@code false}。
	 */
	@SimpleFunction
	public static boolean 到逻辑(Variant value) {
		try {
			return value.getBoolean();
		} catch (转换错误 e) {
			return false;
		}
	}

	/**
	 * 将值转为字节值。
	 *
	 * @param value 值。
	 * @return 成功返回字节值，否则返回{@code 0}。
	 */
	@SimpleFunction
	public static byte 到字节(Variant value) {
		try {
			return value.getByte();
		} catch (转换错误 e) {
			return 0;
		}
	}

	/**
	 * 将值转为短整数值。
	 *
	 * @param value 值。
	 * @return 成功返回短整数值，否则返回{@code 0}。
	 */
	@SimpleFunction
	public static short 到短整数(Variant value) {
		try {
			return value.getShort();
		} catch (转换错误 e) {
			return 0;
		}
	}

	/**
	 * 将值转为整数值。
	 *
	 * @param value 值。
	 * @return 成功返回整数值，否则返回{@code 0}。
	 */
	@SimpleFunction
	public static int 到整数(Variant value) {
		try {
			return value.getInteger();
		} catch (转换错误 e) {
			return 0;
		}
	}

	/**
	 * 将值转为长整数值。
	 *
	 * @param value 值。
	 * @return 成功返回长整数值，否则返回{@code 0}。
	 */
	@SimpleFunction
	public static long 到长整数(Variant value) {
		try {
			return value.getLong();
		} catch (转换错误 e) {
			return 0;
		}
	}

	/**
	 * 将值转为单精度小数值。
	 *
	 * @param value 值。
	 * @return 成功返回单精度小数值，否则返回{@code 0}。
	 */
	@SimpleFunction
	public static float 到单精度小数(Variant value) {
		try {
			return value.getSingle();
		} catch (转换错误 e) {
			return 0;
		}
	}

	/**
	 * 将值转为双精度小数值。
	 *
	 * @param value 值。
	 * @return 成功返回双精度小数值，否则返回{@code 0}。
	 */
	@SimpleFunction
	public static double 到双精度小数(Variant value) {
		try {
			return value.getDouble();
		} catch (转换错误 e) {
			return 0;
		}
	}

	/**
	 * 将值转为文本值。
	 *
	 * @param value 值。
	 * @return 成功返回文本，否则返回空文本。
	 */
	@SimpleFunction
	public static String 到文本(Variant value) {
		try {
			return value.getString();
		} catch (转换错误 e) {
			return "";
		}
	}

	/**
	 * 将值转为对象值。
	 *
	 * @param value 值。
	 * @return 成功返回对象，否则返回空。
	 */
	@SimpleFunction
	public static Object 到对象(Variant value) {
		return ConvHelpers.variant2object(value);
	}

	/**
	 * 将值转为对象数组值。
	 *
	 * @param value 值。
	 * @return 成功返回对象数组，否则返回空。
	 */
	@SimpleFunction
	public static Object[] 到对象数组(Variant value) {
		return ConvHelpers.variant2objects(value);
	}

	/* 数值与字节集：整数小端，长整数和双精度小数大端 */

	/**
	 * 将整数按小端字节序转换为四字节数组，低位字节在前。
	 *
	 * @param value 待转换的 32 位整数。
	 * @return 固定四字节数组，例如十六进制 12345678 转为 78 56 34 12。
	 */
	@SimpleFunction
	public static byte[] 整数到字节集(int value) {
		return new byte[] {
			(byte) value,
			(byte) (value >> 8),
			(byte) (value >> 16),
			(byte) (value >>> 24)
		};
	}

	/**
	 * 将前四个字节按小端字节序还原为整数。
	 *
	 * @param data 小端字节数组，多余字节不参与转换。
	 * @return 32 位有符号整数；空引用或不足四字节返回 0。
	 */
	@SimpleFunction
	public static int 字节集到整数(byte[] data) {
		if (data == null || data.length < 4) {
			return 0;
		}

		return (data[0] & 0xFF)
			| ((data[1] & 0xFF) << 8)
			| ((data[2] & 0xFF) << 16)
			| ((data[3] & 0xFF) << 24);
	}

	/**
	 * 将长整数按大端字节序转换为八字节数组，高位字节在前。
	 *
	 * @param value 待转换的 64 位长整数。
	 * @return 固定八字节数组；与整数到字节集的小端顺序不同。
	 */
	@SimpleFunction
	public static byte[] 长整数到字节集(long value) {
		byte[] result = new byte[8];

		// 从最高八位开始提取，固定保留全部八个字节。
		for (int index = 0; index < result.length; index++) {
			result[index] = (byte) (value >>> (56 - index * 8));
		}

		return result;
	}

	/**
	 * 将前八个字节按大端字节序还原为长整数。
	 *
	 * @param data 大端字节数组，多余字节不参与转换。
	 * @return 64 位有符号长整数；空引用或不足八字节返回 0。
	 */
	@SimpleFunction
	public static long 字节集到长整数(byte[] data) {
		if (data == null || data.length < 8) {
			return 0;
		}

		long result = 0;

		for (int index = 0; index < 8; index++) {
			result = (result << 8) | (data[index] & 0xFFL);
		}

		return result;
	}

	/**
	 * 将双精度小数的 IEEE 754 位模式按大端字节序转换为八字节数组。
	 *
	 * <p>转换的是数值的二进制表示，不是十进制文本的字符编码。
	 * 保留正负零和无穷；NaN 使用 Double.doubleToLongBits 的规范位模式。</p>
	 *
	 * @param value 待转换的双精度小数。
	 * @return 固定八字节数组，高位字节在前。
	 */
	@SimpleFunction
	public static byte[] 双精度小数到字节集(double value) {
		return 长整数到字节集(Double.doubleToLongBits(value));
	}

	/**
	 * 将前八个大端字节按 IEEE 754 位模式还原为双精度小数。
	 *
	 * @param data 大端字节数组，多余字节不参与转换。
	 * @return 双精度小数；允许正负零、无穷及 NaN，空引用或不足八字节返回 0。
	 */
	@SimpleFunction
	public static double 字节集到双精度小数(byte[] data) {
		return Double.longBitsToDouble(字节集到长整数(data));
	}

	/**
	 * 取得单个字节按无符号方式解释后的数值，返回普通整数型。
	 *
	 * @param value 待转换的单个字节。
	 * @return 0 至 255 的整数，例如字节 -1 返回 255；不改变现有到整数的有符号转换。
	 */
	@SimpleFunction
	public static int 取字节无符号值(byte value) {
		return value & 0xFF;
	}
}

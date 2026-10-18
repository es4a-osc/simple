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

import java.util.Locale;

/**
 * 实现各种与转换相关的运行时函数。
 *
 * @author Herbert Czymontek
 */
@SimpleObject
public final class 数据转换 {

	private 数据转换() {
	}

	/**
	 * 返回给定字符串第一个字符的unicode值。
	 *
	 * @param str  用于转换第一个字符的字符串
	 * @return  {@code str}第一个字符的unicode值
	 */
	@SimpleFunction
	public static int 代码(String str) {
		try {
			return str.charAt(0);
		} catch (IndexOutOfBoundsException e) {
			// 将在运行时转换为Simple的运行时错误
			throw new IllegalArgumentException("String length for Asc() must be 1 or greater");
		}
	}

	/**
	 * 返回给定unicode值的字符串。
	 *
	 * @param value  要转换为字符串的unicode值
	 * @return  由给定的unicode值组成的字符串
	 */
	@SimpleFunction
	public static String 字符(int value) {
		return Character.toString((char)value);
	}

	/**
	 * 返回包含给定值的十六进制值的字符串。
	 *
	 * <p>如果给定值不是整数，那么将使用其整数部分。
	 *
	 * @param v  值
	 * @return  字符串，为{@code v}的十六进制值
	 */
	@SimpleFunction
	public static String 十六进制(Variant v) {
		return Long.toHexString(v.getLong()).toUpperCase(Locale.ENGLISH);
	}

	/* 扩展 */

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

	/* 进制文本转换 */

	/**
	 * 将十六进制文本解析为有符号长整数。
	 *
	 * <p>允许正负号，不接受 0x 前缀、分隔符或首尾空白。
	 * 超出长整数范围的文本不按无符号补码解释，例如 FFFFFFFFFFFFFFFF 返回 0。</p>
	 *
	 * @param text 十六进制数值文本。
	 * @return 解析结果；空引用、空文本、非法内容或溢出返回 0。
	 */
	@SimpleFunction
	public static long 十六进制到长整数(String text) {
		try {
			return Long.parseLong(text, 16);
		} catch (Exception exception) {
			return 0;
		}
	}

	/**
	 * 将整数转换为不带前缀的二进制文本。
	 *
	 * @param value 待转换的 32 位整数。
	 * @return 不补前导零的二进制文本；0 返回 0，负数返回 32 位补码。
	 */
	@SimpleFunction
	public static String 整数到二进制(int value) {
		return Integer.toBinaryString(value);
	}

	/**
	 * 将二进制文本解析为有符号长整数。
	 *
	 * <p>允许正负号，不接受 0b 前缀、分隔符或首尾空白。
	 * 最高位为 1 的文本仍按正数解析，不自动作为有符号补码。</p>
	 *
	 * @param text 二进制数值文本。
	 * @return 解析结果；空引用、空文本、非法内容或溢出返回 0。
	 */
	@SimpleFunction
	public static long 二进制到长整数(String text) {
		try {
			return Long.parseLong(text, 2);
		} catch (Exception exception) {
			return 0;
		}
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
	 * 将单个有符号字节解释为 0 至 255 的无符号整数。
	 *
	 * @param value 待转换的单个字节。
	 * @return 0 至 255 的整数，例如字节 -1 返回 255；不改变现有到整数的有符号转换。
	 */
	@SimpleFunction
	public static int 字节到无符号整数(byte value) {
		return value & 0xFF;
	}
}

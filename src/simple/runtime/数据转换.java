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

	/** 扩展 **/

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
}

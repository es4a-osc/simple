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

import java.util.Locale;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.errors.转换错误;
import simple.runtime.variants.Variant;

/**
 * 数值的二进制、十六进制文本表示与解析。
 *
 * <p>不处理字节集编码或字节序；数值输出的补码规则与输入的有符号解析
 * 分别由各方法约定，不能将所有负数输出直接解析为原值。</p>
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 进制转换 {

	/**
	 * 静态方法集，不创建实例。
	 */
	private 进制转换() {
	}

	/* 十六进制表示与解析 */

	/**
	 * 将变体转换为长整数，再生成不带前缀的大写十六进制文本。
	 *
	 * <p>小数沿用变体到长整数的截断规则；负数按 64 位补码输出，
	 * 例如 -1 得到 FFFFFFFFFFFFFFFF，不添加正负号或前导零。</p>
	 *
	 * @param v 可转换为长整数的变体值。
	 * @return 大写十六进制文本；0 返回 0。
	 * @throws 转换错误 变体不能转换为长整数。
	 * @throws NullPointerException 变体为空引用。
	 */
	@SimpleFunction
	public static String 十六进制(Variant v) {
		return Long.toHexString(v.getLong()).toUpperCase(Locale.ENGLISH);
	}

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

	/* 二进制表示与解析 */

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
}

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

import junit.framework.TestCase;

import simple.runtime.errors.转换错误;
import simple.runtime.variants.DoubleVariant;
import simple.runtime.variants.IntegerVariant;
import simple.runtime.variants.StringVariant;

/**
 * 核对进制转换的数值表示、带符号解析、补码及失败返回约定。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public final class RadixConversionsTest extends TestCase {

	/**
	 * 验证{@link 进制转换#十六进制(simple.runtime.variants.Variant)}的变体转换及负数补码。
	 */
	public void testHex() {
		// 可转换的数值沿用变体到长整数规则。
		assertEquals("123ABC", 进制转换.十六进制(IntegerVariant.getIntegerVariant(0x123ABC)));
		assertEquals("FFFFFFFFFFFFFFFF", 进制转换.十六进制(IntegerVariant.getIntegerVariant(-1)));
		assertEquals("A", 进制转换.十六进制(DoubleVariant.getDoubleVariant(10.3)));

		// 无法转换为数值的变体保留原有转换错误。
		try {
			assertEquals("", 进制转换.十六进制(StringVariant.getStringVariant("")));
			fail();
		} catch (转换错误 expected) {
		}
	}

	/**
	 * 十六进制解析支持带符号数值及长整数边界。
	 */
	public void testHexadecimalNumbers() {
		assertEquals(255L, 进制转换.十六进制到长整数("fF"));
		assertEquals(-255L, 进制转换.十六进制到长整数("-FF"));
		assertEquals(255L, 进制转换.十六进制到长整数("+FF"));
		assertEquals(Long.MAX_VALUE, 进制转换.十六进制到长整数("7FFFFFFFFFFFFFFF"));
		assertEquals(Long.MIN_VALUE, 进制转换.十六进制到长整数("-8000000000000000"));
	}

	/**
	 * 数值解析失败仍返回零，不自动去除前缀或空白。
	 */
	public void testInvalidHexadecimalNumbers() {
		for (String text : new String[] {null, "", "0xFF", " FF", "FF ", "G", "FFFFFFFFFFFFFFFF"}) {
			assertEquals(0L, 进制转换.十六进制到长整数(text));
		}
	}

	/**
	 * 二进制输出按 32 位整数补码，解析按带符号数值处理。
	 */
	public void testBinaryNumberRepresentation() {
		assertEquals("0", 进制转换.整数到二进制(0));
		assertEquals("1010", 进制转换.整数到二进制(10));
		assertEquals("11111111111111111111111111111111", 进制转换.整数到二进制(-1));
		assertEquals(4294967295L, 进制转换.二进制到长整数(进制转换.整数到二进制(-1)));
		assertEquals(-10L, 进制转换.二进制到长整数("-1010"));
		assertEquals(10L, 进制转换.二进制到长整数("+1010"));
	}

	/**
	 * 二进制输入的格式和范围不合法时返回零。
	 */
	public void testInvalidBinaryNumbers() {
		for (String text : new String[] {null, "", "0b10", " 10", "10 ", "102",
				"1000000000000000000000000000000000000000000000000000000000000000"}) {
			assertEquals(0L, 进制转换.二进制到长整数(text));
		}
	}
}

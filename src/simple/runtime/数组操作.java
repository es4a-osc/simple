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
import simple.runtime.variants.ArrayVariant;
import simple.runtime.variants.IntegerVariant;
import simple.runtime.variants.Variant;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 提供数组计数、复制、合并、成员增删及文本数组处理。
 *
 * <p>成员索引从 0 开始，维度编号从 1 开始。增删成员返回相同元素类型的新数组，
 * 不改变原数组；排序和复制直接修改目标数组。</p>
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 数组操作 {

	private 数组操作() {
	}

	/* 计数与复制 */

	/**
	 * 返回指定维度的成员数，沿用既有名称，不是最后一个成员的索引。
	 *
	 * @param array 原数组。
	 * @param dimension 维度编号，从 1 开始；更深维度沿第一个成员读取。
	 * @return 该维度的成员数；空外层数组无法读取更深维度时抛出错误。
	 */
	@SimpleFunction
	public static int 取数组下标(Variant array, int dimension) {
		if (dimension <= 0) {
			throw new IllegalArgumentException("数组维度必须从 1 开始。");
		}

		Object result = array.getArray();

		while (--dimension > 0) {
			result = Array.get(result, 0);
		}

		return Array.getLength(result);
	}

	/**
	 * 返回数组第一维的成员数。
	 *
	 * @param array 原数组。
	 * @return 成员数，空数组返回 0。
	 */
	@SimpleFunction
	public static int 取数组成员数(Variant array) {
		return 取数组下标(array, 1);
	}

	/**
	 * 从源数组复制指定数量的成员到目标数组，支持同一数组中的重叠区间。
	 *
	 * @param source 源数组。
	 * @param sourceIndex 源起始索引。
	 * @param target 目标数组，直接修改其内容。
	 * @param targetIndex 目标起始索引。
	 * @param length 复制数量，不得为负；类型不兼容或区间越界时抛出错误。
	 */
	@SimpleFunction
	public static void 复制数组成员(Variant source, int sourceIndex, Variant target, int targetIndex, int length) {
		System.arraycopy(source.getArray(), sourceIndex, target.getArray(), targetIndex, length);
	}

	/* 合并与成员增删 */

	/**
	 * 按先后顺序合并两个字节数组，不修改输入数组。
	 *
	 * @param first 第一个字节数组。
	 * @param second 第二个字节数组。
	 * @return 独立的新数组。
	 */
	@SimpleFunction
	public static byte[] 合并字节数组(byte[] first, byte[] second) {
		byte[] result = new byte[combinedLength(first.length, second.length)];
		System.arraycopy(first, 0, result, 0, first.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	/**
	 * 按先后顺序合并两个文本数组，不修改输入数组。
	 *
	 * @param first 第一个文本数组。
	 * @param second 第二个文本数组。
	 * @return 独立的新数组。
	 */
	@SimpleFunction
	public static String[] 合并文本数组(String[] first, String[] second) {
		String[] result = new String[combinedLength(first.length, second.length)];
		System.arraycopy(first, 0, result, 0, first.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	/**
	 * 在指定位置插入成员，保留原数组元素类型。
	 *
	 * @param array 原数组。
	 * @param value 新成员，按原元素类型执行 Simple 转换。
	 * @param index 插入位置，从 0 到成员数；等于成员数表示追加。
	 * @return 新数组；沿用既有失败约定，索引无效、类型不兼容或空引用时返回空。
	 */
	@SimpleFunction
	public static Variant 插入数组成员(Variant array, Variant value, int index) {
		try {
			return insertMember(array.getArray(), value, index);
		} catch (IllegalArgumentException | NullPointerException | 转换错误 exception) {
			return null;
		}
	}

	/**
	 * 在数组末尾追加成员，不修改原数组。
	 *
	 * @param array 原数组。
	 * @param value 新成员，按原元素类型执行 Simple 转换。
	 * @return 新数组；类型不兼容或空引用时返回空。
	 */
	@SimpleFunction
	public static Variant 加入数组成员(Variant array, Variant value) {
		try {
			Object source = array.getArray();
			return insertMember(source, value, Array.getLength(source));
		} catch (IllegalArgumentException | NullPointerException | 转换错误 exception) {
			return null;
		}
	}

	/**
	 * 删除指定索引的一个成员，保留其余成员的顺序和元素类型。
	 *
	 * @param array 原数组。
	 * @param index 待删除成员的索引。
	 * @return 新数组，不修改原数组；索引无效、不是数组或空引用时返回空。
	 */
	@SimpleFunction
	public static Variant 删除数组成员(Variant array, int index) {
		try {
			Object source = array.getArray();
			int length = Array.getLength(source);

			if (index < 0 || index >= length) {
				return null;
			}

			Object result = Array.newInstance(source.getClass().getComponentType(), length - 1);
			System.arraycopy(source, 0, result, 0, index);
			System.arraycopy(source, index + 1, result, index, length - index - 1);
			return ArrayVariant.getArrayVariant(result);
		} catch (IllegalArgumentException | NullPointerException | 转换错误 exception) {
			return null;
		}
	}

	/* 排序 */

	/**
	 * 将整数数组按升序原地排序。
	 *
	 * @param array 待排序数组，不得为空引用。
	 * @return 原数组，内容已排序。
	 */
	@SimpleFunction
	public static int[] 数组排序(int[] array) {
		Arrays.sort(array);
		return array;
	}

	/**
	 * 将文本数组按 UTF-16 字典顺序原地排序，不进行拼音或数值排序。
	 *
	 * @param array 待排序数组，数组及其成员不得为空引用。
	 * @return 原数组，内容已排序。
	 */
	@SimpleFunction
	public static String[] 文本数组排序(String[] array) {
		// 先检查全部成员，避免空引用导致排序中途失败并留下部分修改。
		for (String value : array) {
			if (value == null) {
				throw new NullPointerException();
			}
		}

		Arrays.sort(array);
		return array;
	}

	/* 文本数组处理 */

	/**
	 * 保留包含指定字面文本的成员，沿用去重及首次出现顺序。
	 *
	 * @param array 待筛选的文本数组。
	 * @param find 要包含的字面文本。
	 * @return 筛选得到的新数组。
	 */
	@SimpleFunction
	public static String[] 过滤数组文本(String[] array, String find) {
		return 过滤数组文本(array, find, true);
	}

	/**
	 * 按子文本包含关系过滤，并沿用既有去重规则。
	 *
	 * @param array 原文本数组，成员不得为空引用。
	 * @param find 字面子文本。
	 * @param include 真表示保留匹配项，假表示排除匹配项。
	 * @return 去重后的新数组，按成员首次出现的顺序排列。
	 */
	@SimpleFunction
	public static String[] 过滤数组文本(String[] array, String find, boolean include) {
		if (find == null) {
			throw new NullPointerException();
		}

		// 使用有序集合去重，结果仍按原成员首次出现的顺序排列。
		Set<String> result = new LinkedHashSet<String>();

		for (String value : array) {
			if (value.contains(find) == include) {
				result.add(value);
			}
		}

		return result.toArray(new String[result.size()]);
	}

	/**
	 * 使用指定分隔文本连接数组成员。
	 *
	 * @param array 文本数组。
	 * @param separator 成员间的分隔文本，不得为空引用，允许空文本。
	 * @return 连接结果，空数组返回空文本；空引用成员沿用 Java 文本转换规则。
	 */
	@SimpleFunction
	public static String 连接数组文本(String[] array, String separator) {
		if (separator == null) {
			throw new NullPointerException();
		}

		StringBuilder result = new StringBuilder();

		for (int index = 0; index < array.length; index++) {
			if (index > 0) {
				result.append(separator);
			}

			result.append(array[index]);
		}

		return result.toString();
	}

	/**
	 * 使用字面分隔文本分割全部内容，保留末尾空成员。
	 *
	 * @param text 原文本。
	 * @param separator 字面分隔文本；空文本按 UTF-16 边界分割。
	 * @return 分割结果。
	 */
	@SimpleFunction
	public static String[] 分割文本(String text, String separator) {
		return 分割文本(text, separator, Integer.MAX_VALUE);
	}

	/**
	 * 使用字面分隔文本分割内容，并限制返回成员数。
	 *
	 * @param text 原文本。
	 * @param separator 字面分隔文本，不是正则表达式。
	 * @param count 最大成员数，必须大于 0；达到上限后剩余内容作为最后一项。
	 * @return 分割结果，保留末尾空成员；空分隔文本沿用 Java 的 UTF-16 边界规则。
	 */
	@SimpleFunction
	public static String[] 分割文本(String text, String separator, int count) {
		if (separator == null) {
			throw new NullPointerException();
		}

		if (count <= 0) {
			throw new IllegalArgumentException("分割文本的最大成员数必须大于 0。");
		}

		return text.split(Pattern.quote(separator), count);
	}

	/* 内部辅助 */

	/**
	 * 检查合并长度，避免整数溢出。
	 *
	 * @param first 第一部分长度。
	 * @param second 第二部分长度。
	 * @return 合并长度。
	 */
	private static int combinedLength(int first, int second) {
		if (first > Integer.MAX_VALUE - second) {
			throw new IllegalArgumentException("合并后的数组过长。");
		}

		return first + second;
	}

	/**
	 * 复制数组并写入一个新成员，复用运行库已有的 Variant 类型转换。
	 *
	 * @param source 原始 Java 数组。
	 * @param value 新成员。
	 * @param index 插入位置。
	 * @return 包装后的新数组。
	 */
	private static Variant insertMember(Object source, Variant value, int index) {
		int length = Array.getLength(source);
		if (index < 0 || index > length) {
			throw new IllegalArgumentException("插入位置超出数组范围。");
		}

		Class<?> component = source.getClass().getComponentType();
		Object result = Array.newInstance(component, combinedLength(length, 1));

		System.arraycopy(source, 0, result, 0, index);
		System.arraycopy(source, index, result, index + 1, length - index);
		ArrayVariant output = ArrayVariant.getArrayVariant(result);
		if (component.isArray()) {
			Array.set(result, index, value.getArray());
		} else {
			output.array(new Variant[]{IntegerVariant.getIntegerVariant(index)}, value);
		}

		return output;
	}
}

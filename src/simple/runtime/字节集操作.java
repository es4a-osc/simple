package simple.runtime;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.collections.集合;
import simple.runtime.variants.ArrayVariant;

import java.util.Arrays;

/**
 * 提供字节数组的合并、分割、截取和计数。
 *
 * <p>索引从 0 开始，数量以字节计。返回的字节片段均为独立数组，不修改输入数据；
 * 文本和字节的字符集转换由编码转换提供。</p>
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 字节集操作 {

	/**
	 * 静态方法集，不创建实例。
	 */
	private 字节集操作() {
	}

	/* 长度与合并 */

	/**
	 * 返回字节集长度。
	 *
	 * @param data 字节数组，不得为空引用。
	 * @return 字节数，空数组返回 0。
	 */
	@SimpleFunction
	public static int 取字节集长度(byte[] data) {
		requireData(data);
		return data.length;
	}

	/**
	 * 按先后顺序合并两个字节集。
	 *
	 * @param first 第一个字节集。
	 * @param second 第二个字节集。
	 * @return 独立的新字节数组。
	 */
	@SimpleFunction
	public static byte[] 合并字节集(byte[] first, byte[] second) {
		requireData(first);
		requireData(second);
		// 保留合并长度的溢出检查，避免分配错误长度的数组。
		if (first.length > Integer.MAX_VALUE - second.length) {
			throw new IllegalArgumentException("合并后的数组过长。");
		}

		byte[] result = new byte[first.length + second.length];
		System.arraycopy(first, 0, result, 0, first.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	/* 截取 */

	/**
	 * 从开头截取指定数量的字节。
	 *
	 * @param data 原字节集。
	 * @param length 数量，不得为负，超出原长度时返回全部字节的副本。
	 * @return 新字节数组。
	 */
	@SimpleFunction
	public static byte[] 取字节集左边(byte[] data, int length) {
		return 取字节集中间(data, 0, length);
	}

	/**
	 * 从末尾截取指定数量的字节。
	 *
	 * @param data 原字节集。
	 * @param length 数量，不得为负，超出原长度时返回全部字节的副本。
	 * @return 新字节数组。
	 */
	@SimpleFunction
	public static byte[] 取字节集右边(byte[] data, int length) {
		requireData(data);
		checkLength(length);
		return 取字节集中间(data, Math.max(0, data.length - length), length);
	}

	/**
	 * 从指定位置截取到字节集末尾，不改变原数组。
	 *
	 * @param data 原字节集。
	 * @param start 起始索引，允许等于字节集长度。
	 * @return 新字节数组；起点位于末尾时为空数组。
	 */
	@SimpleFunction
	public static byte[] 取字节集中间(byte[] data, int start) {
		return 取字节集中间(data, start, Integer.MAX_VALUE);
	}

	/**
	 * 从指定位置截取字节，不足指定数量时截取到末尾。
	 *
	 * @param data 原字节集。
	 * @param start 起始索引，允许等于字节集长度。
	 * @param length 数量，不得为负。
	 * @return 新字节数组，长度为 0 或从末尾开始时返回空数组。
	 */
	@SimpleFunction
	public static byte[] 取字节集中间(byte[] data, int start, int length) {
		requireData(data);
		if (start < 0 || start > data.length) {
			throw new IllegalArgumentException("字节集索引超出范围。");
		}

		checkLength(length);
		int end = start + Math.min(length, data.length - start);
		return Arrays.copyOfRange(data, start, end);
	}

	/* 分割 */

	/**
	 * 使用不重叠的字节序列分割字节集。
	 *
	 * <p>保留首尾和连续分隔符产生的空片段；空原数据得到一个空片段。
	 * 集合的每个项目都是一个字节数组，未找到分隔符时包含原数据的独立副本。</p>
	 *
	 * @param data 原字节集。
	 * @param separator 分隔字节序列，不得为空引用或空数组。
	 * @return 按原顺序排列的字节数组集合。
	 */
	@SimpleFunction
	public static 集合 分割字节集(byte[] data, byte[] separator) {
		requireData(data);
		requireData(separator);
		if (separator.length == 0) {
			throw new IllegalArgumentException("分隔字节序列不能为空。");
		}

		集合 result = new 集合();
		int start = 0;
		int position = 0;

		// 仅在完整分隔符仍可放下的位置比较，避免末尾的部分匹配越界。
		while (position <= data.length - separator.length) {
			int matched = 0;

			while (matched < separator.length && data[position + matched] == separator[matched]) {
				matched++;
			}

			if (matched == separator.length) {
				result.加入(ArrayVariant.getArrayVariant(Arrays.copyOfRange(data, start, position)));
				position += separator.length;
				start = position;
			} else {
				position++;
			}
		}

		// 末段单独加入，保留末尾分隔符产生的空片段。
		result.加入(ArrayVariant.getArrayVariant(Arrays.copyOfRange(data, start, data.length)));
		return result;
	}

	/* 内部检查 */

	/**
	 * 区分空字节数组和无数据的空引用。
	 *
	 * @param data 待检查的字节数组。
	 */
	private static void requireData(byte[] data) {
		if (data == null) {
			throw new IllegalArgumentException("字节集不能是空引用。");
		}
	}

	/**
	 * 检查截取数量。
	 *
	 * @param length 字节数量。
	 */
	private static void checkLength(int length) {
		if (length < 0) {
			throw new IllegalArgumentException("字节数量不得为负数。");
		}
	}
}

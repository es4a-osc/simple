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
import simple.runtime.variants.ArrayVariant;
import simple.runtime.variants.Variant;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * 实现各种阵列相关的运行时函数。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 数组操作 {

	private 数组操作() {  // COV_NF_LINE
	}                   // COV_NF_LINE

	/**
	 * 过滤数组的文本内容。
	 *
	 * @param array 要搜索的数组
	 * @param str 要在数组中搜索的子文本
	 * @param include 如果{@code true}则在结果中包含匹配的子文本，否则排除它们
	 * @return 包含（非）匹配数组项的数组
	 */
	@SimpleFunction
	public static String[] 过滤数组文本(String[] array, String str, boolean include) {
		if (str == null) {
			throw new NullPointerException();
		}

		Set<String> result = new HashSet<String>();
		for (String a : array) {
			if (a.contains(str) == include) {
				result.add(a);
			}
		}
		return result.toArray(new String[result.size()]);
	}

	/**
	 * 将数组成员追加到单个文本中。
	 *
	 * @param array 包含要连接文本的数组
	 * @param separator 在数组成员之间连接文本
	 * @return 包含连接数组成员的文本
	 */
	@SimpleFunction
	public static String 连接数组文本(String[] array, String separator) {
		if (separator == null) {
			throw new NullPointerException();
		}

		StringBuilder sb = new StringBuilder();
		String sep = "";
		for (String a : array) {
		 sb.append(sep).append(a);
		 sep = separator;
		}
		return sb.toString();
	}

	/**
	 * 查找指定文本来分割文本。
	 *
	 * @param str 欲分割的文本
	 * @param separator 欲查找的分割文本
	 * @param count 分割次数
	 * @return 包含分割后的文本数组
	 */
	@SimpleFunction
	public static String[] 分割文本(String str, String separator, int count) {
		if (separator == null) {
			throw new NullPointerException();
		}

		if (count <= 0) {
			throw new IllegalArgumentException("Count for Split() out of range. Should greater than 0.");
		}

		return str.split("\\Q" + separator + "\\E", count);
	}

	/**
	 * 返回数组维度的大小。
	 *
	 * @param array 请求其大小的数组
	 * @param dim 维度（第一个维度为1，依此类推）
	 * @return 数组维度的大小
	 */
	@SimpleFunction
	public static int 取数组下标(Variant array, int dim) {
		if (dim <= 0) {
			throw new IllegalArgumentException("Dimension for UBound() out of range. " +
					"Should be greater than 0.");
		}

		Object arr = array.getArray();
		while (--dim > 0) {
			arr = Array.get(arr, 0);
		}

		return Array.getLength(arr);
	}

	/* 扩展 */

	/**
	 * 取数组第一个维度的成员数。
	 *
	 * @param array 欲取成员的数组。
	 * @return 数组维度的大小
	 */
	@SimpleFunction
	public static int 取数组成员数(Variant array) {
		return 取数组下标(array, 1);
	}

	/**
	 * 将2个字节数组合并为一个
	 * 
	 * @param array1
	 * @param array2
	 * @return 新数组
	 */
	@SimpleFunction
	public static byte[] 合并字节数组(byte[] array1, byte[] array2) {
		byte[] array3 = new byte[array1.length + array2.length];
		System.arraycopy(array1, 0, array3, 0, array1.length);
		System.arraycopy(array2, 0, array3, array1.length, array2.length);
		return array3;
	}

	/**
	 * 将2个文本数组合并为一个
	 * 
	 * @param array1
	 * @param array2
	 * @return 新数组
	 */
	@SimpleFunction
	public static String[] 合并文本数组(String[] array1, String[] array2) {
		String[] array3 = new String[array1.length + array2.length];
		System.arraycopy(array1, 0, array3, 0, array1.length);
		System.arraycopy(array2, 0, array3, array1.length, array2.length);
		return array3;
	}

	/**
	 * 从源数组复制成员到目标数组成员
	 * 
	 * @param src 源数组
	 * @param srcPos 源数组中的起始位置，从0开始。
	 * @param dest 目标数组
	 * @param destPos 目标数组中的起始位置，从0开始。
	 * @param length 要复制的数组成员的数量，注意确保从位置拥有的拥有的成员数。
	 * @return 新数组
	 */
	@SimpleFunction
	public static void 复制数组成员(Variant src, int srcPos, Variant dest, int destPos, int length) {
		Object srcArray = src.getArray();
		Object destArray = dest.getArray();
		System.arraycopy(srcArray, srcPos, destArray, destPos, length);
	}

	/**
	 * 在数组指定位置插入成员
	 * 
	 * @param a 数组
	 * @param data 成员数据
	 * @param pos 插入位置
	 * @return 成功返回新数组，失败返回空
	 */
	@SimpleFunction
	public static Variant 插入数组成员(Variant a, Variant data, int pos) {
		int i = 0;
		ArrayList<Object> list = new ArrayList<Object>();
		try {
			// 将源数组转为列表
			Object arr = a.getArray();
			while (i < Array.getLength(arr)) {
				Object arr1 = Array.get(arr, i);
				// 插入成员
				if (i == pos) {
					list.add(data.getObject());
				}

				// 添加成员
				list.add(arr1);
				i++;
			}

			// 新建数组同类的对象数组
			Object objarr = Array.newInstance(
				Class.forName(arr.getClass().getComponentType().toString()),
				list.size()
			);
			if (objarr == null) {
				return null;
			}

			// 把列表成员设置到对象数组
			i = 0;
			Object[] tarr = list.toArray();
			while (i < Array.getLength(objarr)) {
				Array.set(objarr, i, tarr[i]);
				i++;
			}
			
			return ArrayVariant.getArrayVariant(objarr);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 在数组末尾添加成员
	 * 
	 * @param a 数组
	 * @param data 数据
	 * @return 成功返回新数组，失败返回空
	 */
	@SimpleFunction
	public static Variant 加入数组成员(Variant a, Variant data) {
		int i = 0;
		ArrayList<Object> list = new ArrayList<Object>();
		try {
			// 将源数组转为列表
			Object arr = a.getArray();
			while (i < Array.getLength(arr)) {
				Object arr1 = Array.get(arr, i);
				list.add(arr1);
				i++;
			}
			// 将数字添加到末尾
			list.add(data.getObject());
			
			// 新建数组同类的对象数组
			Object objarr = Array.newInstance(
				Class.forName(arr.getClass().getComponentType().toString()),
				list.size()
			);
			if (objarr == null) {
				return null;
			}

			// 把列表成员设置到对象数组
			i = 0;
			Object[] tarr = list.toArray();
			while (i < Array.getLength(objarr)) {
				Array.set(objarr, i, tarr[i]);
				i++;
			}
			
			return ArrayVariant.getArrayVariant(objarr);
		} catch (Exception e) {
			return null;
		}
	}
}

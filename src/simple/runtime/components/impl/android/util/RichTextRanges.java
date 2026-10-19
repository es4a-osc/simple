package simple.runtime.components.impl.android.util;

import java.util.ArrayList;
import java.util.List;

import simple.runtime.errors.索引超出界限错误;

/**
 * 校验富文本框的半开区间，并计算清除格式后的剩余片段及完整段落。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class RichTextRanges {

	private RichTextRanges() {
	}

	/* 字符区间 */

	/**
	 * 校验半开文本区间是否位于给定长度内。
	 *
	 * @param length 处理的数据长度
	 * @param start 区间起始位置，包含该位置
	 * @param end 区间结束位置，不包含该位置
	 */
	public static void check(int length, int start, int end) {
		if (start < 0 || end < start || end > length) {
			throw new 索引超出界限错误();
		}
	}

	/**
	 * 计算移除指定区间后仍需保留的样式片段。
	 *
	 * @param spanStart 样式原始起始位置
	 * @param spanEnd 样式原始结束位置
	 * @param start 区间起始位置，包含该位置
	 * @param end 区间结束位置，不包含该位置
	 * @return 需要保留的样式区间
	 */
	public static int[][] remaining(int spanStart, int spanEnd, int start, int end) {
		if (start == end || spanEnd <= start || spanStart >= end) {
			return new int[][]{{spanStart, spanEnd}};
		}

		List<int[]> result = new ArrayList<int[]>();
		if (spanStart < start) {
			result.add(new int[]{spanStart, start});
		}
		if (spanEnd > end) {
			result.add(new int[]{end, spanEnd});
		}
		return result.toArray(new int[result.size()][]);
	}

	/* 段落边界 */

	/**
	 * 将文本区间扩展到完整段落边界。
	 *
	 * @param text 待处理文本
	 * @param start 区间起始位置，包含该位置
	 * @param end 区间结束位置，不包含该位置
	 * @return 覆盖所选文本的完整段落区间；每项为起止索引，空选择返回空数组。
	 */
	public static int[][] paragraphs(CharSequence text, int start, int end) {
		check(text.length(), start, end);
		List<int[]> result = new ArrayList<int[]>();
		if (start == end) {
			return new int[0][];
		}

		// 项目符作用于完整段落，结束换行包含在段落中，末尾不额外创建空段落。
		int position = start;
		while (position > 0 && text.charAt(position - 1) != '\n') {
			position--;
		}
		while (position < end) {
			int next = position;
			while (next < text.length() && text.charAt(next) != '\n') {
				next++;
			}

			if (next < text.length()) {
				next++;
			}
			result.add(new int[]{position, next});
			position = next;
		}

		return result.toArray(new int[result.size()][]);
	}
}

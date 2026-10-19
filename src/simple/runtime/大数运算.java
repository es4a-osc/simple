package simple.runtime;

import java.math.BigDecimal;
import java.math.RoundingMode;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

/**
 * 以文本输入输出执行高精度十进制运算，避免先转换为普通浮点数造成精度损失。
 *
 * <p>数值文本支持正负号、小数和科学计数，不自动去除首尾空白。
 * 结果使用普通十进制形式，不主动删除末尾零；比较返回整数。
 * 非法数值抛出数值格式异常，空引用抛出空指针异常，除零抛出算术异常，
 * 不把失败伪装成零或空文本。精度与可处理规模受 BigDecimal 及可用内存限制。</p>
 *
 * <p>现有 Simple 错误转换尚不支持算术异常。Simple 代码在相除或求余前，
 * 应使用大数比较检查除数是否为零，不依赖错误处理分支接住除零。</p>
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 大数运算 {

	/**
	 * 静态方法集，不创建实例。
	 */
	private 大数运算() {
	}

	/* 四则运算 */

	/**
	 * 精确计算两个十进制数之和。
	 *
	 * @param first 第一个数值文本，例如 0.1。
	 * @param second 第二个数值文本，例如 0.2。
	 * @return 普通十进制结果，保留运算产生的小数位，例如 1.20 加 2.3 得到 3.50。
	 * @throws NumberFormatException 任一文本不是有效十进制数。
	 */
	@SimpleFunction
	public static String 大数相加(String first, String second) {
		return new BigDecimal(first).add(new BigDecimal(second)).toPlainString();
	}

	/**
	 * 精确计算第一个十进制数减去第二个数。
	 *
	 * @param first 被减数文本。
	 * @param second 减数文本。
	 * @return 普通十进制差值，允许负数，不主动删除末尾零。
	 * @throws NumberFormatException 任一文本不是有效十进制数。
	 */
	@SimpleFunction
	public static String 大数相减(String first, String second) {
		return new BigDecimal(first).subtract(new BigDecimal(second)).toPlainString();
	}

	/**
	 * 精确计算两个十进制数的乘积。
	 *
	 * @param first 第一个因数文本。
	 * @param second 第二个因数文本。
	 * @return 普通十进制乘积，保留运算产生的小数位。
	 * @throws NumberFormatException 任一文本不是有效十进制数。
	 */
	@SimpleFunction
	public static String 大数相乘(String first, String second) {
		return new BigDecimal(first).multiply(new BigDecimal(second)).toPlainString();
	}

	/**
	 * 按指定小数位数计算商，使用 HALF_UP 舍入。
	 *
	 * <p>恰好位于中间值时向远离零的方向舍入，例如 -1.005 保留两位得到 -1.01。
	 * 位数为 0 时舍入到整数，位数为负数时舍入到十位、百位等。</p>
	 *
	 * @param first 被除数文本。
	 * @param second 除数文本，不得为零。
	 * @param scale 结果小数位数，允许负数，表示按十位、百位等舍入。
	 * @return 普通十进制商，非负位数下保留指定的小数位和末尾零。
	 * @throws NumberFormatException 任一文本不是有效十进制数。
	 * @throws ArithmeticException 除数为零或指定规模的运算无法完成。
	 */
	@SimpleFunction
	public static String 大数相除(String first, String second, int scale) {
		return new BigDecimal(first).divide(new BigDecimal(second), scale, RoundingMode.HALF_UP).toPlainString();
	}

	/* 比较与求余 */

	/**
	 * 比较两个十进制数的数值，不以文本长度或小数位数决定大小。
	 *
	 * @param first 第一个数值文本。
	 * @param second 第二个数值文本。
	 * @return 小于返回 -1，数值相等返回 0，大于返回 1；1.0 与 1.00 相等。
	 * @throws NumberFormatException 任一文本不是有效十进制数。
	 */
	@SimpleFunction
	public static int 大数比较(String first, String second) {
		return new BigDecimal(first).compareTo(new BigDecimal(second));
	}

	/**
	 * 计算第一个数除以第二个数所得的余数，不执行非负模运算。
	 *
	 * @param first 被除数文本。
	 * @param second 除数文本，不得为零。
	 * @return 普通十进制余数；非零余数与被除数同号，例如 -5 除以 3 的余数为 -2。
	 * @throws NumberFormatException 任一文本不是有效十进制数。
	 * @throws ArithmeticException 除数为零或运算无法完成。
	 */
	@SimpleFunction
	public static String 大数求余(String first, String second) {
		return new BigDecimal(first).remainder(new BigDecimal(second)).toPlainString();
	}
}

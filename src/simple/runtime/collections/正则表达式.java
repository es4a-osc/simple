package simple.runtime.collections;

import simple.runtime.annotations.SimpleDataElement;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.regex.Matcher;

/**
 * 正则表达式
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public class 正则表达式 {

	@SimpleDataElement
	public static final int 匹配_规范等价 = Pattern.CANON_EQ;
	@SimpleDataElement
	public static final int 匹配_不区分大小写 = Pattern.CASE_INSENSITIVE;
	@SimpleDataElement
	public static final int 匹配_允许空白注释 = Pattern.COMMENTS;

	/**
	 * 默认情况下, 正则表达式中点(.)不会匹配换行符, 设置了Pattern.DOTALL模式, 才会匹配所有字符包括换行符。
	 */
	@SimpleDataElement
	public static final int 匹配_DOTALL模式	= Pattern.DOTALL;
	@SimpleDataElement
	public static final int 匹配_字面值解析 = Pattern.LITERAL;

	/**
	 * 正则表达式中出现了^或者$, 默认只会匹配第一行. 设置了Pattern.MULTILINE模式,会匹配所有行。
	 */
	@SimpleDataElement
	public static final int 匹配_多行模式 = Pattern.MULTILINE;
	@SimpleDataElement
	public static final int 匹配_UNICODE_CASE = Pattern.UNICODE_CASE;
	@SimpleDataElement
	public static final int 匹配_UNIX_LINES = Pattern.UNIX_LINES;

	private Pattern pattern;
	private Matcher matcher;

	/**
	 * 将给定的正则表达式编译到模式中。
	 *
	 * @param regex 要编译的表达式
	 * @param flags 匹配标志，可选组合值：{@link #匹配_规范等价} 、{@link #匹配_不区分大小写}、
	 * 				{@link #匹配_允许空白注释}、{@link #匹配_DOTALL模式}、{@link #匹配_字面值解析}、
	 * 				{@link #匹配_多行模式}、{@link #匹配_UNICODE_CASE}、{@link #匹配_UNIX_LINES}
	 * @throws 非法参数错误 表达式或匹配标志无效
	 */
	@SimpleFunction
	public void 编译(String regex, int flags) {
		try {
			pattern = Pattern.compile(regex, flags);
		} catch (PatternSyntaxException e) {
			// 如果表达式的语法无效
			throw new IllegalArgumentException("表达式 " + regex + " 无效"); // 非法参数错误
		} catch (IllegalArgumentException e) {
			// 如果在 flags 中设置与定义的匹配标志不对应的位值
			throw new IllegalArgumentException("匹配标志 " + flags + " 无效"); // 非法参数错误
		}
	}

	/**
	 * 用正则表达式对指定文本进行分割。
	 *
	 * @param text 待分割文本
	 * @return 返回分割后的文本数组
	 * @throws 未初始化实例错误 未编译表达式
	 */
	@SimpleFunction
	public String[] 分割(String text) {
		if (pattern == null) throw new NullPointerException("未编译表达式");
		return pattern.split(text);
	}

	/**
	 * 创建匹配给定输入与此模式的匹配器。
	 *
	 * @param text 待匹配文本
	 * @throws 未初始化实例错误 未编译表达式
	 */
	@SimpleFunction
	public void 匹配(String input) {
		if (pattern == null) throw new NullPointerException("未编译表达式");
		matcher = pattern.matcher(input);
	}

	/**
	 * 替换模式与给定替换字符串相匹配的输入序列的每个子序列。
	 *
	 * @param replacement 替换字符串
	 * @return 返回替换后的文本
	 * @throws 未初始化实例错误 未匹配表达式
	 */
	@SimpleFunction
	public String 替换(String replacement) {
		if (matcher == null) throw new NullPointerException("未匹配表达式");
		return matcher.replaceAll(replacement);
	}

	/**
	 * 调用本命令寻找下一个匹配文本
	 *
	 * @return 如果找到则返回真，未找到则返回假。
	 * @throws 未初始化实例错误 未匹配表达式
	 */
	@SimpleFunction
	public boolean 匹配下一个() {
		if (matcher == null) throw new NullPointerException("未匹配表达式");
		return matcher.find();
	}

	/**
	 * 调用本命令取出匹配到的文本。
	 *
	 * @return 返回匹配到的文本
	 * @throws 未初始化实例错误 未匹配表达式
	 */
	@SimpleFunction
	public String 取匹配文本() {
		if (matcher == null) throw new NullPointerException("未匹配表达式");
		return matcher.group();
	}

	/**
	 * 调用本命令取出匹配到的指定索引的子文本
	 *
	 * @param group 子文本索引是从1开始的。
	 * @return 返回指定索引的子文本
	 * @throws 未初始化实例错误 未匹配表达式
	 */
	@SimpleFunction
	public String 取子匹配文本(int group) {
		if (matcher == null) throw new NullPointerException("未匹配表达式");
		return matcher.group(group);
	}

	/**
	 * 调用本命令取出匹配到的子文本的数量。
	 *
	 * @return 子文本的数量
	 * @throws 未初始化实例错误 未匹配表达式
	 */
	@SimpleFunction
	public int 取子匹配数() {
		if (matcher == null) throw new NullPointerException("未匹配表达式");
		return matcher.groupCount();
	}

	/**
	 * 调用本命令取出匹配到的文本在整个文本中开始的位置。
	 *
	 * @return 开始位置
	 * @throws 未初始化实例错误 未匹配表达式
	 */
	@SimpleFunction
	public int 取开始位置() {
		if (matcher == null) throw new NullPointerException("未匹配表达式");
		return matcher.start();
	}

	/**
	 * 调用本命令取出匹配到的文本在整个文本中结束的位置。
	 *
	 * @return 结束位置
	 * @throws 未初始化实例错误 未匹配表达式
	 */
	@SimpleFunction
	public int 取结束位置() {
		if (matcher == null) throw new NullPointerException("未匹配表达式");
		return matcher.end();
	}
}

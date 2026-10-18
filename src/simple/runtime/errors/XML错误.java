package simple.runtime.errors;

import simple.runtime.annotations.SimpleObject;

/**
 * XML 输入、遍历或生成失败，可由 Simple 错误分支捕获。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SuppressWarnings("serial")
@SimpleObject
public final class XML错误 extends 运行错误 {

	/**
	 * 保存本次 XML 操作的错误原因。
	 *
	 * @param message 错误原因。
	 */
	public XML错误(String message) {
		super(message);
	}
}

package simple.runtime.errors;

import simple.runtime.annotations.SimpleObject;

/**
 * SQLite 数据库的路径、连接或 SQL 操作失败，可由 Simple 错误分支捕获。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SuppressWarnings("serial")
@SimpleObject
public final class 数据库错误 extends 运行错误 {

	/**
	 * 创建包含失败原因的数据库错误。
	 *
	 * @param message 失败原因。
	 */
	public 数据库错误(String message) {
		super(message);
	}
}

package simple.runtime;

import java.util.UUID;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

/**
 * 程序标识生成与当前单元信息。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 其它操作 {

	/*
	 * 0. dalvik.system.VMStack
	 * 1. java.lang.Thread
	 * 2. simple.runtime.其它操作 <--- 当前类
	 * 3. simple.samples.test.主窗口 <--- 上级类
	 * 4. java.lang.reflect.Method
	 * 5. java.lang.reflect.Method
	 * 6. simple.runtime.events.EventDispatcher$EventHandlerClosure
	 * ...
	 */
	private static final int CURRENT_INDEX = 3;

	/**
	 * 静态方法集，不创建实例。
	 */
	private 其它操作() {
	}

	/* 标识生成 */

	/**
	 * 生成随机的第 4 版 UUID（GUID）。
	 *
	 * <p>可用作记录或任务标识，重复概率极低，但不保证绝对唯一。
	 * 每次调用独立生成，不代表设备身份，也不是递增编号。</p>
	 *
	 * @return 36 个字符的小写十六进制文本，包含连字符，格式为 8-4-4-4-12。
	 */
	@SimpleFunction
	public static String 生成GUID() {
		return UUID.randomUUID().toString();
	}

	/* 单元信息 */

	/**
	 * 返回当前单元名。
	 *
	 * @return 成功返回单元名，否则返回空文本。
	 */
	@SimpleFunction
	public static String 取单元名() {
		// 注意必须直接调用 Thread.getStackTrace() 方法，否则会造成索引错误
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		if (ste.length > CURRENT_INDEX) {
			// 取全类名
			String className = ste[CURRENT_INDEX].getClassName();
			return className.substring(className.lastIndexOf(".") + 1);
		}

		return "";
	}

	/**
	 * 返回当前单元类名（限定名）。
	 *
	 * @return 成功返回单元类名，否则返回空文本。
	 */
	@SimpleFunction
	public static String 取单元类名() {
		// 必须直接调用 getStackTrace() 方法，否则会造成索引错误
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		if (ste.length > CURRENT_INDEX) {
			// 取全类名
			return ste[CURRENT_INDEX].getClassName();
		}

		return "";
	}

}

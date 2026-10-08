package simple.runtime;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.collections.线程;
import simple.runtime.variants.Variant;

import android.os.Looper;

/**
 * 线程相关函数。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 线程操作 {

	/**
	 * 创建新线程对象。
	 *
	 * @param unit 单元对象实例。
	 * @param name 回调过程名。
	 * @return 新对象实例。
	 */
	@SimpleFunction
	public static 线程 创建线程(Object unit, String name) {
		return 创建线程(unit, name, null);
	}

	/**
	 * 创建新线程对象。
	 *
	 * @param unit 单元对象实例。
	 * @param name 回调过程名。
	 * @param args 传递给过程的参数；成员与参数对应，没有参数可传递{@code null}或没有成员的数组。
	 * @return 新对象实例。
	 */
	@SimpleFunction
	public static 线程 创建线程(Object unit, String name, Variant[] args) {
		return new 线程(unit, name, args);
	}

	/**
	 * 创建并启动线程。
	 *
	 * @param unit 单元对象实例。
	 * @param name 回调过程名。
	 */
	@SimpleFunction
	public static 线程 启动线程(Object unit, String name) {
		return 启动线程(unit, name, null);
	}

	/**
	 * 创建并启动线程。
	 *
	 * @param unit 单元对象实例。
	 * @param name 回调过程名。
	 * @param args 传递给过程的参数；成员与参数对应，没有参数可传递{@code null}或没有成员的数组。
	 */
	@SimpleFunction
	public static 线程 启动线程(Object unit, String name, Variant[] args) {
		线程 thread = new 线程(unit, name, args);
		thread.启动();
		return thread;
	}

	/**
	 * 返回当前执行线程的线程标识。
	 *
	 * @return 线程标识。
	 */
	@SimpleFunction
	public static long 取线程标识() {
		return Thread.currentThread().getId();
	}

	/**
	 * 使当前线程延时指定时长，不推荐在主线程（窗口单元）下调用。
	 *
	 * @param millis 延时时间，单位为毫秒。
	 * @return 成功返回{@code true}，否则（中断状态）返回{@code false}。
	 */
	@SimpleFunction
	public static boolean 延时(long millis) {
		try {
			Thread.sleep(millis);
			return true;
		} catch (InterruptedException e) {
			// 如果任何线程中断了当前线程。当抛出该异常时，当前线程的中断状态被清除。
			日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[线程.延时] 线程已被中断；线程标识=" + 取线程标识());
		}
		return false;
	}

	/**
	 * 检测并清除当前线程中断状态。
	 *
	 * @return 成功清除返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public static boolean 清除线程中断() {
		return Thread.interrupted();
	}

	/**
	 * 判断当前线程是否UI线程
	 *
	 * @return 执行过程在主线程为返回{@code true}，否则返回{@code false}
	 */
	public static boolean 是否为主线程() {
		return Thread.currentThread() == Looper.getMainLooper().getThread();
	}
}

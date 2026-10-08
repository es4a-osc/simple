package simple.runtime.collections;

import simple.runtime.程序操作;
import simple.runtime.日志输出;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.SimpleDataElement;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.variants.Variant;

/**
 * 线程对象。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public class 线程 {
	@SimpleDataElement
	static final int 状态_新建 = 0; // Thread.State.NEW
	@SimpleDataElement
	static final int 状态_可运行 = 1; // Thread.State.Runnable
	@SimpleDataElement
	static final int 状态_阻塞 = 2; // Thread.State.BLOCKED
	@SimpleDataElement
	static final int 状态_无限等待 = 3; // Thread.State.WAITING
	@SimpleDataElement
	static final int 状态_限时等待 = 4; // Thread.State.TIMED_WAITING
	@SimpleDataElement
	static final int 状态_结束 = 5; // Thread.State.TERMINATED

	// 线程实例
	private Thread thread;

	/**
	 * 构造新{@code Thread}的线程对象。
	 *
	 * <p>将回调{@code 线程运行()}方法。
	 */
	public 线程() {
		thread = new Thread() {
			@Override
			public void run() {
				线程运行();
			}
		};
	}

	/**
	 * 构造指定回调的线程对象。
	 *
	 * <p>将回调指定单元过程。
	 *
	 * @param unit 单元对象实例。
	 * @param name 回调过程名。
	 * @param args 传递给过程的参数；成员与参数对应，没有参数可传递{@code null}或没有成员的数组。
	 */
	public 线程(Object unit, String name, Variant[] args) {
		thread = new Thread() {
			@Override
			public void run() {
				程序操作.调用方法(unit, name, args);
			}
		};
	}

	/**
	 * 取{@code Thread}实例。
	 *
	 * @return {@code Thread}实例。
	 */
	public Thread getThread() {
		return thread;
	}

	/**
	 * 线程启动时执行该方法，继承单元请覆写该{@code 过程}。
	 */
	@SimpleFunction
	public void 线程运行() {
		日志输出.输出警告(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/线程.线程运行] 继承线程的对象单元未覆写该过程；单元类名=" + getClass().getName());
	}

	/**
	 * 状态属性获取方法。
	 *
	 * @return 返回值请参考{@link 线程#状态_新建}等
	 */
	@SimpleProperty
	public int 状态() {
		Thread.State state = thread.getState();
		if (state == Thread.State.NEW) {
			return 状态_新建;
		} else if (state == Thread.State.RUNNABLE) {
			return 状态_可运行;
		} else if (state == Thread.State.BLOCKED) {
			return 状态_阻塞;
		} else if (state == Thread.State.WAITING) {
			return 状态_无限等待;
		} else if (state == Thread.State.TIMED_WAITING) {
			return 状态_限时等待;
		} else {
			return 状态_结束;
		}
	}

	/**
	 * 标识属性获取方法。
	 *
	 * @return 线程标识
	 */
	@SimpleProperty
	public long 标识() {
		return thread.getId();
	}

	/**
	 * 名称属性获取方法。
	 *
	 * @return 线程名称
	 */
	@SimpleProperty
	public String 名称() {
		return thread.getName();
	}

	/**
	 * 名称属性获取方法。
	 *
	 * @return 线程名称
	 */
	@SimpleProperty
	public void 名称(String name) {
		try {
			thread.setName(name);
		} catch (SecurityException e) {
			日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/线程.名称] 线程无法被修改；线程标识=" + thread.getId());
		}
	}

	/**
	 * 启动该线程开始执行，每个线程仅可启动一次。
	 *
	 * @return 成功返回{@code true}，失败返回{@code false}。
	 */
	@SimpleFunction
	public boolean 启动() {
		// 检验线程是否已启动
		if (thread.getState() == Thread.State.NEW) {
			thread.start();
			return true;
		} else {
			日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/线程.启动] 非新建状态；线程标识=" + thread.getId());
			return false;
		}
	}

	/**
	 * 堵塞当前操作线程，并等待该线程执行完毕。
	 *
	 * @param millis 以毫秒为单位的等待时间，为 0 意味着要一直等下去。
	 * @return 成功返回{@code true}，否则返回{@code false}。
	 * @throws 非法参数错误  millis为负数
	 */
	@SimpleFunction
	public boolean 参与(long millis) {
		try {
			thread.join(millis);
			return true;
		} catch (InterruptedException e) {
			// 如果任何线程中断了当前线程。当抛出该异常时，当前线程的中断状态 被清除。
			日志输出.输出错误(日志输出.MODULE_NAME_RTL,	"[" + hashCode() + "/线程.参与] 线程已被中断；线程标识=" + thread.getId());
		}
		return false;
	}

	/**
	 * 使线程等待。
	 *
	 * @param timeout 要等待的最长时间（以毫秒为单位），为 0 意味着要一直等下去。
	 * @return 成功返回{@code true}，否则返回{@code false}。
	 * @throws 非法参数错误 timeout为负数
	 */
	@SimpleFunction
	public boolean 等待(long timeout) {
		try {
			thread.wait(timeout);
			return true;
		} catch (IllegalMonitorStateException e) {
			// 如果当前线程不是此对象监视器的所有者。
			日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/线程.等待] 非该对象监视器所有；线程标识=" + thread.getId());
		} catch (InterruptedException e) {
			// 如果任何线程中断了当前线程。当抛出该异常时，当前线程的中断状态 被清除。
			日志输出.输出错误(日志输出.MODULE_NAME_RTL,	"[" + hashCode() + "/线程.等待] 线程已被中断；线程标识=" + thread.getId());
		}
		return false;
	}

	/**
	 * 标记该线程为中断状态。
	 */
	@SimpleFunction
	public void 中断() {
		thread.interrupt();
	}

	/**
	 * 检测该线程是否中断状态。
	 *
	 * @return 中断状态返回{@code true}，否则返回{@code false}。
	 */
	@SimpleFunction
	public boolean 是否中断() {
		return thread.isInterrupted();
	}

	/**
	 * 强制终止正在执行的该线程。
	 *
	 * <p>注意：该方法已弃用，非安全优雅的退出。
	 *
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	@SuppressWarnings("deprecation") // 忽略该方法调用已弃用的API
	public boolean 终止() {
		try {
			// Thread.stop() 已过时
			thread.stop();
			return true;
		} catch (SecurityException e) {
			日志输出.输出错误(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/线程.终止] 线程无法被修改；线程标识=" + thread.getId());
		}
		return false;
	}
}

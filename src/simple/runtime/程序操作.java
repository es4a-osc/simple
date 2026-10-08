package simple.runtime;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.variants.ObjectVariant;
import simple.runtime.variants.Variant;

/**
 * 程序相关函数
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 程序操作 {

	/*
	 * 0. dalvik.system.VMStack
	 * 1. java.lang.Thread
	 * 2. simple.runtime.其它 <--- 当前类
	 * 3. simple.samples.test.主窗口 <--- 上级类
	 * 4. java.lang.reflect.Method
	 * 5. java.lang.reflect.Method
	 * 6. simple.runtime.events.EventDispatcher$EventHandlerClosure
	 * ...
	 */
	private static final int CURRENT_INDEX = 3;

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

	/**
	 * 取指定对象的对象名。
	 *
	 * @param object 对象实例。
	 * @return 对象名。
	 */
	@SimpleFunction
	public static String 取对象名(Object object) {
		return object.getClass().getSimpleName();
	}

	/**
	 * 取指定对象的类名。
	 *
	 * @param object 对象实例。
	 * @return 类名。
	 */
	@SimpleFunction
	public static String 取对象类名(Object object) {
		return object.getClass().getName();
	}

	/**
	 * 取指定对象的哈希码。
	 *
	 * @param object 对象实例。
	 * @return 对象哈希码。
	 */
	@SimpleFunction
	public static int 取对象哈希码(Object object) {
		return object.hashCode();
	}

	/**
	 * 创建指定限定类名的对象
	 *
	 * @param className 单元的完全限定类名。
	 * @return 成功返回对象实例，否则返回{@code null}。
	 */
	@SimpleFunction
	public static Object 创建对象(String className) {
		try {
			// 通过完全限定类名查找类。实例化该类，返回对象
			return Class.forName(className).newInstance();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 调用对象的方法。
	 *
	 * @param unit 单元对象实例。
	 * @param name 对象方法名，函数名或过程名。
	 * @return 返回函数结果或过程为{@code null}。
	 */
	@SimpleFunction
	public static Variant 调用方法(Object unit, String name) {
		return 调用方法(unit, name, null);
	}

	/**
	 * 调用对象的方法。
	 *
	 * @param unit 单元对象实例。
	 * @param name 对象方法名，函数名或过程名。
	 * @param args 传递给方法的参数；成员与参数对应，没有参数可传递{@code null}或没有成员的数组。
	 * @return 返回函数结果或过程为{@code null}。
	 */
	@SimpleFunction
	public static Variant 调用方法(Object unit, String name, Variant[] args) {
		return ObjectVariant.getObjectVariant(unit).function(name, args == null ? new Variant[0] : args);
	}

	/**
	 * 取对象的成员值。
	 *
	 * @param unit 单元对象实例。
	 * @param name 对象成员名。
	 * @return 成员值。
	 */
	@SimpleFunction
	public static Variant 取成员值(Object unit, String name) {
		return ObjectVariant.getObjectVariant(unit).dataMember(name);
	}

	/**
	 * 置对象的成员值。
	 *
	 * @param unit 单元对象实例。
	 * @param name 对象成员名。
	 * @param variant 成员值。
	 */
	@SimpleFunction
	public static void 置成员值(Object unit, String name, Variant variant) {
		ObjectVariant.getObjectVariant(unit).dataMember(name, variant);
	}
}

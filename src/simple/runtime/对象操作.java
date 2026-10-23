package simple.runtime;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.variants.ObjectVariant;
import simple.runtime.variants.Variant;

/**
 * 对象信息、实例创建及反射访问。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 对象操作 {

	/**
	 * 静态方法集，不创建实例。
	 */
	private 对象操作() {
	}

	/* 对象信息 */

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

	/* 对象创建 */

	/**
	 * 通过可访问的无参数构造方法创建指定类的对象。
	 *
	 * @param className 类的完全限定名称。
	 * @return 成功返回对象实例；类不存在、不能实例化、构造方法不可访问或抛出异常时返回{@code null}。
	 * @throws Error 类初始化或构造方法发生错误时，继续抛出原错误。
	 */
	@SimpleFunction
	public static Object 创建对象(String className) {
		try {
			Class<?> type = Class.forName(className);
			Constructor<?> constructor = type.getDeclaredConstructor();

			// 部分 Android 实现未拒绝私有构造，显式保持旧入口的访问边界；不使用 setAccessible。
			if (type != 对象操作.class) {
				int modifiers = constructor.getModifiers();
				if (Modifier.isPrivate(modifiers)) {
					return null;
				}
				if (!Modifier.isPublic(type.getModifiers()) || !Modifier.isPublic(modifiers)) {
					Package targetPackage = type.getPackage();
					if (targetPackage == null || type.getClassLoader() != 对象操作.class.getClassLoader()
							|| !targetPackage.getName().equals(对象操作.class.getPackage().getName())) {
						return null;
					}
				}
			}
			return constructor.newInstance();
		} catch (InvocationTargetException e) {
			// 新反射调用会包装构造方法的失败，不能因此把原本向外传播的 Error 当作普通异常吞掉。
			Throwable cause = e.getCause();
			if (cause instanceof Error) {
				throw (Error) cause;
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	/* 方法调用 */

	/**
	 * 调用对象的方法。
	 *
	 * @param unit 单元对象实例。
	 * @param name 对象方法名，函数名或过程名。
	 * @return 函数返回对应的变体值；过程返回未初始化变体。
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
	 * @return 函数返回对应的变体值；过程返回未初始化变体。
	 */
	@SimpleFunction
	public static Variant 调用方法(Object unit, String name, Variant[] args) {
		return ObjectVariant.getObjectVariant(unit).function(name, args == null ? new Variant[0] : args);
	}

	/* 成员读写 */

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

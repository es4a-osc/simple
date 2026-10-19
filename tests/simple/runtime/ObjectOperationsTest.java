package simple.runtime;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import junit.framework.TestCase;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.collections.线程;
import simple.runtime.errors.未知标识符错误;
import simple.runtime.variants.IntegerVariant;
import simple.runtime.variants.Variant;

/**
 * 验证对象操作迁移后的反射访问、失败约定及线程回调。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class ObjectOperationsTest extends TestCase {

	/**
	 * 对象接口只由新方法集导出，程序接口保留原有归属。
	 */
	public void testExportedOwnership() {
		assertTrue(对象操作.class.isAnnotationPresent(SimpleObject.class));
		int count = 0;
		for (Method method : 对象操作.class.getDeclaredMethods()) {
			assertTrue(method.isAnnotationPresent(SimpleFunction.class));
			assertTrue(Modifier.isPublic(method.getModifiers()));
			assertTrue(Modifier.isStatic(method.getModifiers()));
			count++;
		}
		assertEquals(8, count);

		for (Method method : 其它操作.class.getDeclaredMethods()) {
			String name = method.getName();
			assertTrue(name, name.equals("生成GUID") || name.equals("取单元名") || name.equals("取单元类名"));
		}
	}

	/**
	 * 根据限定类名创建实例，并读取其真实类型与哈希码。
	 */
	public void testCreationAndInformation() {
		Object object = 对象操作.创建对象(Probe.class.getName());
		assertTrue(object instanceof Probe);
		assertEquals("Probe", 对象操作.取对象名(object));
		assertEquals(Probe.class.getName(), 对象操作.取对象类名(object));
		assertEquals(object.hashCode(), 对象操作.取对象哈希码(object));

		assertNull(对象操作.创建对象("missing.Object"));
		assertNull(对象操作.创建对象("java.lang.Integer"));
	}

	/**
	 * 无参数重载、空参数数组和带参数调用沿用同一反射规则。
	 */
	public void testMethodCalls() {
		Probe object = new Probe();
		assertEquals(7, 对象操作.调用方法(object, "read").getInteger());
		assertEquals(7, 对象操作.调用方法(object, "read", null).getInteger());
		assertEquals(7, 对象操作.调用方法(object, "read", new Variant[0]).getInteger());
		assertEquals(12, 对象操作.调用方法(object, "add", new Variant[] {
			IntegerVariant.getIntegerVariant(5)
		}).getInteger());
	}

	/**
	 * 优先访问公开字段，字段缺失时回退到同名属性方法。
	 */
	public void testFieldsAndProperties() {
		Probe object = new Probe();
		assertEquals(7, 对象操作.取成员值(object, "value").getInteger());
		对象操作.置成员值(object, "value", IntegerVariant.getIntegerVariant(21));
		assertEquals(21, object.value);

		对象操作.置成员值(object, "property", IntegerVariant.getIntegerVariant(34));
		assertEquals(34, 对象操作.取成员值(object, "property").getInteger());
	}

	/**
	 * 找不到方法或成员时仍抛出运行库的未知标识符错误。
	 */
	public void testMissingMembers() {
		Probe object = new Probe();
		try {
			对象操作.调用方法(object, "missing");
			fail("缺失方法应报错");
		} catch (未知标识符错误 expected) {
			// 迁移不改变错误类型。
		}

		try {
			对象操作.取成员值(object, "missing");
			fail("缺失成员应报错");
		} catch (未知标识符错误 expected) {
			// 字段和属性均不存在时沿用相同错误。
		}
	}

	/**
	 * 内部线程回调通过新方法集调用目标，参数与结果保持不变。
	 *
	 * @throws InterruptedException 等待线程结束时被中断。
	 */
	public void testThreadCallback() throws InterruptedException {
		Probe object = new Probe();
		Thread worker = new 线程(object, "add", new Variant[] {
			IntegerVariant.getIntegerVariant(5)
		}).getThread();
		worker.start();
		worker.join(3000);
		assertFalse("回调线程应结束", worker.isAlive());
		assertEquals(12, object.value);
	}

	/**
	 * 为反射测试提供公开字段、普通方法和同名属性方法。
	 */
	public static final class Probe {

		/** 测试成员的初始值。 */
		public int value = 7;

		/**
		 * 创建可通过无参数构造实例化的对象。
		 */
		public Probe() {
		}

		/**
		 * 读取当前值。
		 *
		 * @return 当前成员值。
		 */
		public int read() {
			return value;
		}

		/**
		 * 增加当前值，供反射参数转换和线程回调验证。
		 *
		 * @param increment 增量。
		 * @return 增加后的成员值。
		 */
		public int add(int increment) {
			value += increment;
			return value;
		}

		/**
		 * 通过属性方法读取当前值。
		 *
		 * @return 当前成员值。
		 */
		public int property() {
			return value;
		}

		/**
		 * 通过属性方法写入当前值。
		 *
		 * @param newValue 新成员值。
		 */
		public void property(int newValue) {
			value = newValue;
		}
	}
}

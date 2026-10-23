package simple.runtime;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import junit.framework.TestCase;
import simple.compiler.Compiler;
import simple.compiler.Project;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.collections.线程;
import simple.runtime.errors.未知标识符错误;
import simple.runtime.variants.IntegerVariant;
import simple.runtime.variants.Variant;

/**
 * 验证对象信息、反射创建的访问与失败边界、成员访问及线程回调。
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
	 * 缺少可访问的无参构造、不能实例化或类名无效时返回空，不绕过访问控制。
	 */
	public void testCreationRejectsUnavailableConstructors() {
		assertNull(对象操作.创建对象(null));
		assertNull(对象操作.创建对象(""));
		assertNull(对象操作.创建对象(PrivateConstructor.class.getName()));
		assertNull(对象操作.创建对象(其它操作.class.getName()));
		assertNull(对象操作.创建对象(AbstractObject.class.getName()));
		assertNull(对象操作.创建对象(Runnable.class.getName()));
	}

	/**
	 * 调用者自身的私有构造原本即可访问，不能将所有私有构造一概拒绝。
	 */
	public void testCreationAllowsOwnPrivateConstructor() {
		assertTrue(对象操作.创建对象(对象操作.class.getName()) instanceof 对象操作);
	}

	/**
	 * 原来可访问的同包无参构造仍能调用，不错误地收窄为仅支持公开构造。
	 */
	public void testCreationAllowsPackageConstructor() {
		assertTrue(对象操作.创建对象(PackageConstructor.class.getName()) instanceof PackageConstructor);
	}

	/**
	 * 构造方法抛出的受检异常和运行时异常均转换为创建失败。
	 */
	public void testConstructorExceptionsReturnNull() {
		assertNull(对象操作.创建对象(CheckedFailure.class.getName()));
		assertNull(对象操作.创建对象(UncheckedFailure.class.getName()));
	}

	/**
	 * 构造方法中的 Error 不因反射包装而被误当作普通异常吞掉。
	 */
	public void testConstructorErrorIsPreserved() {
		try {
			对象操作.创建对象(ConstructorError.class.getName());
			fail("构造错误必须继续抛出");
		} catch (AssertionError expected) {
			assertSame(ConstructorError.FAILURE, expected);
		}
	}

	/**
	 * 类初始化失败应继续抛出原错误，不被创建失败的空值掩盖。
	 */
	public void testInitializerErrorIsPreserved() {
		try {
			对象操作.创建对象(InitializerError.class.getName());
			fail("类初始化错误必须继续抛出");
		} catch (ExceptionInInitializerError expected) {
			assertEquals("初始化失败", expected.getCause().getMessage());
		}
	}

	/**
	 * 用现有程序信息示例验证反射创建、失败返回值和实例方法调用。
	 *
	 * @throws IOException 示例项目无法读取
	 */
	public void testReflectionExampleCompiles() throws IOException {
		assertNotNull(Compiler.compileForUnitTesting(new Project(new File("examples/对象操作/project.properties"))));
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
	 * 验证私有构造不会被强制开放。
	 */
	public static final class PrivateConstructor {

		/**
		 * 仅供类内部创建。
		 */
		private PrivateConstructor() {
		}
	}

	/**
	 * 验证同包可访问的非公开构造仍受支持。
	 */
	static final class PackageConstructor {

		/**
		 * 允许运行库同包调用。
		 */
		PackageConstructor() {
		}
	}

	/**
	 * 验证抽象类型不能被实例化。
	 */
	public abstract static class AbstractObject {

		/**
		 * 存在无参构造也不能直接创建抽象类型。
		 */
		public AbstractObject() {
		}
	}

	/**
	 * 验证构造方法受检异常的失败返回值。
	 */
	public static final class CheckedFailure {

		/**
		 * 模拟构造过程中读取失败。
		 *
		 * @throws IOException 固定的构造失败
		 */
		public CheckedFailure() throws IOException {
			throw new IOException("构造失败");
		}
	}

	/**
	 * 验证构造方法运行时异常的失败返回值。
	 */
	public static final class UncheckedFailure {

		/**
		 * 模拟构造过程拒绝当前状态。
		 *
		 * @throws IllegalStateException 固定的构造失败
		 */
		public UncheckedFailure() {
			throw new IllegalStateException("构造失败");
		}
	}

	/**
	 * 验证 Error 保留原实例，而非返回空或抛出反射包装。
	 */
	public static final class ConstructorError {
		static final AssertionError FAILURE = new AssertionError("构造错误");

		/**
		 * 抛出可核对对象身份的错误。
		 *
		 * @throws AssertionError 固定的构造错误
		 */
		public ConstructorError() {
			throw FAILURE;
		}
	}

	/**
	 * 验证类初始化阶段的错误继续向外传播。
	 */
	public static final class InitializerError {
		static {
			if (Boolean.parseBoolean("true")) {
				throw new IllegalStateException("初始化失败");
			}
		}

		/**
		 * 类初始化失败时不会执行。
		 */
		public InitializerError() {
		}
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

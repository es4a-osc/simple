package simple.compiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Set;
import java.util.TimeZone;

import junit.framework.TestCase;

import simple.compiler.parser.Parser;
import simple.compiler.scanner.Scanner;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.UsesPermissions;
import simple.runtime.parameters.DateReferenceParameter;
import simple.runtime.对话框操作;

/**
 * 验证日期时间选择导出、完整示例及日历副本与精度边界。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class DateTimePickerTest extends TestCase {

	/**
	 * 两个日期重载及三个时间重载均返回确认状态，并传址输出日期时间，无需权限。
	 *
	 * @throws Exception 导出方法缺失。
	 */
	public void testExportedContract() throws Exception {
		Class<?>[][] signatures = {
			{ String.class, DateReferenceParameter.class },
			{ String.class, Calendar.class, DateReferenceParameter.class },
			{ String.class, Calendar.class, boolean.class, DateReferenceParameter.class }
		};
		for (String name : new String[] { "弹出日期选择框", "弹出时间选择框" }) {
			int count = name.equals("弹出日期选择框") ? 2 : 3;
			for (int index = 0; index < count; index++) {
				Method method = 对话框操作.class.getMethod(name, signatures[index]);
				assertTrue(method.isAnnotationPresent(SimpleFunction.class));
				assertTrue(Modifier.isStatic(method.getModifiers()));
				assertEquals(boolean.class, method.getReturnType());
				assertFalse(method.isAnnotationPresent(UsesPermissions.class));
			}
		}
		assertFalse(对话框操作.class.isAnnotationPresent(UsesPermissions.class));
	}

	/**
	 * 完整示例编译覆盖五个重载、取消状态分支和日期时间传址操作。
	 *
	 * @throws Exception 示例文件读取失败。
	 */
	public void testRealExampleCompiles() throws Exception {
		assertNotNull(Compiler.compileForUnitTesting(new Project(new File("examples/对话框/project.properties"))));
	}

	/**
	 * 选择日期时间不应使完整示例增加清单权限。
	 *
	 * @throws Exception 示例解析或权限集合读取失败。
	 */
	public void testExampleNeedsNoPermissions() throws Exception {
		ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
		PrintStream messages = new PrintStream(diagnostics, true, "UTF-8");
		Compiler compiler = new Compiler(Compiler.Platform.Android, messages, messages);
		Project project = new Project("examples/对话框/project.properties", null);
		for (Project.SourceDescriptor descriptor : project.getSources()) {
			String source = new String(Files.readAllBytes(descriptor.getFile().toPath()), StandardCharsets.UTF_8);
			new Parser(compiler, new Scanner(compiler, source), descriptor.getQualifiedName()).parse();
		}
		if (compiler.getErrorCount() == 0) {
			compiler.resolve();
		}
		assertEquals(diagnostics.toString("UTF-8"), 0, compiler.getErrorCount());

		Method permissions = Compiler.class.getDeclaredMethod("generatePermissions");
		permissions.setAccessible(true);
		assertTrue(((Set<?>) permissions.invoke(compiler)).isEmpty());
	}

	/**
	 * 从月末选取闰日时保留时间、毫秒及原时区，且不修改传入日历。
	 *
	 * @throws Exception 内部日期转换调用失败。
	 */
	public void testDateSelectionPreservesTimeAndInput() throws Exception {
		Calendar initial = initial();
		long before = initial.getTimeInMillis();
		Method method = helper("selectPickerDate", Calendar.class, int.class, int.class, int.class);
		Calendar selected = (Calendar) method.invoke(null, initial, 2024, Calendar.FEBRUARY, 29);

		assertNotSame(initial, selected);
		assertEquals(before, initial.getTimeInMillis());
		assertEquals(2024, selected.get(Calendar.YEAR));
		assertEquals(Calendar.FEBRUARY, selected.get(Calendar.MONTH));
		assertEquals(29, selected.get(Calendar.DAY_OF_MONTH));
		assertEquals(23, selected.get(Calendar.HOUR_OF_DAY));
		assertEquals(45, selected.get(Calendar.MINUTE));
		assertEquals(56, selected.get(Calendar.SECOND));
		assertEquals(789, selected.get(Calendar.MILLISECOND));
		assertEquals("Asia/Shanghai", selected.getTimeZone().getID());
	}

	/**
	 * 跨年选择不会因原月末的天数而偏移到其它月份。
	 *
	 * @throws Exception 内部日期转换调用失败。
	 */
	public void testDateSelectionAcrossYear() throws Exception {
		Calendar selected = (Calendar) helper("selectPickerDate", Calendar.class,
			int.class, int.class, int.class).invoke(null, initial(), 2025, Calendar.JANUARY, 1);
		assertEquals(2025, selected.get(Calendar.YEAR));
		assertEquals(Calendar.JANUARY, selected.get(Calendar.MONTH));
		assertEquals(1, selected.get(Calendar.DAY_OF_MONTH));
	}

	/**
	 * 凌晨和深夜均保留日期、时区，秒与毫秒归零，初值保持不变。
	 *
	 * @throws Exception 内部时间转换调用失败。
	 */
	public void testTimeSelectionPrecisionAndInput() throws Exception {
		Calendar initial = initial();
		long before = initial.getTimeInMillis();
		Method method = helper("selectPickerTime", Calendar.class, int.class, int.class);
		for (int hour : new int[] { 0, 12, 23 }) {
			Calendar selected = (Calendar) method.invoke(null, initial, hour, 59);
			assertNotSame(initial, selected);
			assertEquals(before, initial.getTimeInMillis());
			assertEquals(initial.get(Calendar.YEAR), selected.get(Calendar.YEAR));
			assertEquals(initial.get(Calendar.MONTH), selected.get(Calendar.MONTH));
			assertEquals(initial.get(Calendar.DAY_OF_MONTH), selected.get(Calendar.DAY_OF_MONTH));
			assertEquals(hour, selected.get(Calendar.HOUR_OF_DAY));
			assertEquals(59, selected.get(Calendar.MINUTE));
			assertEquals(0, selected.get(Calendar.SECOND));
			assertEquals(0, selected.get(Calendar.MILLISECOND));
			assertEquals(initial.getTimeZone().getID(), selected.getTimeZone().getID());
		}
	}

	/**
	 * 严格日历中的无效初值和空初值必须拒绝，不能悄悄改成当前日期。
	 *
	 * @throws Exception 内部校验方法缺失。
	 */
	public void testInvalidInitialRejected() throws Exception {
		Calendar invalid = initial();
		invalid.set(Calendar.MONTH, Calendar.FEBRUARY);
		invalid.set(Calendar.DAY_OF_MONTH, 30);
		for (Calendar value : new Calendar[] { null, invalid }) {
			try {
				helper("copyPickerInitial", Calendar.class).invoke(null, value);
				fail("无效初值应被拒绝");
			} catch (InvocationTargetException error) {
				assertTrue(error.getCause() instanceof IllegalArgumentException);
			}
		}
	}

	/**
	 * 取消保留输出变量原引用和所有字段，包括尚未初始化的输出。
	 *
	 * @throws Exception 内部结果提交方法调用失败。
	 */
	public void testCancelPreservesOutput() throws Exception {
		Method method = helper("commitPickerResult", Calendar.class, DateReferenceParameter.class);
		Calendar original = initial();
		long before = original.getTimeInMillis();
		DateReferenceParameter output = new DateReferenceParameter(original);
		assertEquals(false, method.invoke(null, null, output));
		assertSame(original, output.get());
		assertEquals(before, original.getTimeInMillis());

		output = new DateReferenceParameter(null);
		assertEquals(false, method.invoke(null, null, output));
		assertNull(output.get());
	}

	/**
	 * 确认仅替换输出引用，不修改其原对象，支持初值与输出使用同一变量。
	 *
	 * @throws Exception 内部结果提交方法调用失败。
	 */
	public void testConfirmReplacesOutput() throws Exception {
		Calendar original = initial();
		long before = original.getTimeInMillis();
		DateReferenceParameter output = new DateReferenceParameter(original);
		Calendar selected = (Calendar) helper("selectPickerTime", Calendar.class,
			int.class, int.class).invoke(null, original, 0, 5);

		Method method = helper("commitPickerResult", Calendar.class, DateReferenceParameter.class);
		assertEquals(true, method.invoke(null, selected, output));
		assertSame(selected, output.get());
		assertNotSame(original, output.get());
		assertEquals(before, original.getTimeInMillis());
	}

	/** 返回带毫秒和明确时区的严格月末初值。 */
	private Calendar initial() {
		Calendar value = new GregorianCalendar(TimeZone.getTimeZone("Asia/Shanghai"));
		value.clear();
		value.set(2024, Calendar.JANUARY, 31, 23, 45, 56);
		value.set(Calendar.MILLISECOND, 789);
		value.setLenient(false);
		return value;
	}

	/** 仅调用日期转换纯逻辑，不在主机测试中创建 Android 对话框。 */
	private Method helper(String name, Class<?>... parameters) throws Exception {
		Method method = 对话框操作.class.getDeclaredMethod(name, parameters);
		method.setAccessible(true);
		return method;
	}
}

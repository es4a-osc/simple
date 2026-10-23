package simple.compiler;

import android.app.Service;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import simple.classfiles.ClassFile;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.collections.意图;
import simple.runtime.collections.服务;

/**
 * 验证系统实例化契约、服务过程覆写和两个真实服务项目的生成字节码。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class ServiceUnitTest extends TestCase {

	/**
	 * 服务基类必须保留公开无参构造和可继承性，不能被改成普通组件或抽象调度入口。
	 *
	 * @throws Exception 构造方法不存在
	 */
	public void testSystemInstantiationContract() throws Exception {
		assertEquals(Service.class, 服务.class.getSuperclass());
		assertTrue(Modifier.isPublic(服务.class.getConstructor().getModifiers()));
		assertFalse(Modifier.isFinal(服务.class.getModifiers()));
		assertFalse(Modifier.isAbstract(服务.class.getModifiers()));
	}

	/**
	 * 三个服务过程必须仍可被 Simple 单元以原名称和签名覆写。
	 *
	 * @throws Exception 服务成员缺失
	 */
	public void testLifecycleOverrideContract() throws Exception {
		for (Method method : new Method[]{
				服务.class.getMethod("服务创建"),
				服务.class.getMethod("服务处理", 意图.class),
				服务.class.getMethod("服务销毁")}) {
			assertTrue(method.isAnnotationPresent(SimpleFunction.class));
			assertEquals(void.class, method.getReturnType());
			assertFalse(Modifier.isFinal(method.getModifiers()));
		}
	}

	/**
	 * 后台服务与定时任务示例必须生成系统能加载的服务子类及公开无参构造。
	 *
	 * @throws Exception 项目读取、编译或生成类验证失败
	 */
	public void testBothServiceProjectsGenerateValidSubclasses() throws Exception {
		for (String[] project : new String[][]{
				{"服务", "simple.examples.service.任务服务"},
				{"定时任务", "simple.examples.tasks.任务服务"}}) {
			List<ClassFile> classes = Compiler.compileForUnitTesting(new Project(new File(
					"examples/" + project[0] + "/project.properties")));
			assertNotNull(classes);
			Map<String, byte[]> generated = new HashMap<>();
			for (ClassFile file : classes) {
				generated.put(file.getName(), file.generate());
			}
			ClassLoader loader = new ClassLoader(getClass().getClassLoader()) {
				@Override
				protected Class<?> findClass(String name) throws ClassNotFoundException {
					byte[] bytes = generated.get(name);
					if (bytes == null) {
						throw new ClassNotFoundException(name);
					}
					return defineClass(name, bytes, 0, bytes.length);
				}
			};
			Class<?> type = Class.forName(project[1], false, loader);
			assertEquals(服务.class, type.getSuperclass());
			assertTrue(Modifier.isPublic(type.getConstructor().getModifiers()));
			assertEquals(void.class, type.getDeclaredMethod("服务处理", 意图.class).getReturnType());
		}
	}
}

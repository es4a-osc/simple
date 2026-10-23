package simple.compiler;

import java.io.File;
import junit.framework.TestCase;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.UsesPermissions;
import simple.runtime.components.邮箱选择器;

/**
 * 验证邮箱选择器的权限、选择结果、输入属性和现有完整示例。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class EmailSelectorComponentTest extends TestCase {

	/**
	 * 建议查询只需要读取联系人，不能额外申请写入通讯录权限。
	 */
	public void testReadOnlyContactPermission() {
		UsesPermissions permissions = 邮箱选择器.class.getAnnotation(UsesPermissions.class);
		assertNotNull(permissions);
		assertEquals("android.permission.READ_CONTACTS", permissions.permissionNames());
	}

	/**
	 * 选择结果与错误必须可观察，单行默认值应与实际输入设计一致。
	 *
	 * @throws Exception 导出成员缺失
	 */
	public void testSelectionAndInputContract() throws Exception {
		assertTrue(邮箱选择器.class.getMethod("选择完毕", String.class, String.class).isAnnotationPresent(SimpleEvent.class));
		assertTrue(邮箱选择器.class.getMethod("操作失败", String.class).isAnnotationPresent(SimpleEvent.class));
		assertTrue(邮箱选择器.class.getMethod("错误信息").isAnnotationPresent(SimpleProperty.class));
		assertEquals("True", 邮箱选择器.class.getMethod("单行模式", boolean.class)
				.getAnnotation(SimpleProperty.class).initializer());
		assertEquals(boolean.class, 邮箱选择器.class.getMethod("启用").getReturnType());
	}

	/**
	 * 没有姓名或系统使用邮箱作为显示名时，应返回纯邮箱且不添加尖括号。
	 *
	 * @throws Exception 候选构造或访问失败
	 */
	public void testUnnamedCandidateIsBareAddress() throws Exception {
		Class<?> type = Class.forName("simple.runtime.components.impl.android.util.EmailAddressAdapter$Item");
		java.lang.reflect.Constructor<?> constructor = type.getDeclaredConstructor(String.class, String.class);
		constructor.setAccessible(true);
		String address = "example+tag@example.invalid";
		for (String name : new String[]{"", address}) {
			Object item = constructor.newInstance(name, address);
			assertEquals(address, item.toString());
			assertEquals("", type.getField("name").get(item));
		}
	}

	/**
	 * 邮箱选择器独立示例必须能调用更新后的组件。
	 *
	 * @throws Exception 示例读取或编译失败
	 */
	public void testExistingInputExampleCompiles() throws Exception {
		assertNotNull(Compiler.compileForUnitTesting(new Project(new File("examples/邮箱选择器/project.properties"))));
	}
}

package simple.compiler;

import java.io.File;
import java.lang.reflect.Method;
import junit.framework.TestCase;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.UsesPermissions;
import simple.runtime.components.网页浏览框;
import simple.runtime.components.impl.android.网页浏览框Impl;

/**
 * 验证网页浏览框的权限、交互契约、脚本格式与完整离线示例。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class WebViewComponentTest extends TestCase {

	/**
	 * 组件只申请实际使用的联网权限，新能力必须有对应导出标记。
	 *
	 * @throws Exception 导出成员缺失
	 */
	public void testPermissionsAndExportedMembers() throws Exception {
		assertEquals("android.permission.INTERNET", 网页浏览框.class.getAnnotation(UsesPermissions.class).permissionNames());
		assertTrue(网页浏览框.class.getMethod("HTTP错误", int.class, String.class).isAnnotationPresent(SimpleEvent.class));
		assertTrue(网页浏览框.class.getMethod("移除接口", String.class).isAnnotationPresent(SimpleFunction.class));
		assertEquals(boolean.class, 网页浏览框.class.getMethod("移除接口", String.class).getReturnType());
		assertTrue(网页浏览框.class.getMethod("错误信息").isAnnotationPresent(SimpleProperty.class));
		assertTrue(网页浏览框.class.getMethod("已销毁").isAnnotationPresent(SimpleProperty.class));
	}

	/**
	 * 支持缩放、显示按钮与混合内容应具有独立且明确的默认值。
	 *
	 * @throws Exception 属性不存在
	 */
	public void testIndependentDefaultSettings() throws Exception {
		assertEquals("True", 网页浏览框.class.getMethod("支持缩放", boolean.class).getAnnotation(SimpleProperty.class).initializer());
		assertEquals("False", 网页浏览框.class.getMethod("启用缩放按钮", boolean.class).getAnnotation(SimpleProperty.class).initializer());
		assertEquals("False", 网页浏览框.class.getMethod("允许混合内容", boolean.class).getAnnotation(SimpleProperty.class).initializer());
	}

	/**
	 * 新旧脚本入口应保留真实源码，只移除最前面的可选协议前缀。
	 *
	 * @throws Exception 辅助方法访问或执行失败
	 */
	public void testScriptInputPreservesJavaScript() throws Exception {
		Method normalize = 网页浏览框Impl.class.getDeclaredMethod("normalizeScript", String.class);
		normalize.setAccessible(true);
		assertEquals("", normalize.invoke(null, new Object[]{null}));
		assertEquals("", normalize.invoke(null, " javascript: "));
		assertEquals("sum2Java(1,2)", normalize.invoke(null, " JavaScript: sum2Java(1,2) "));
		assertEquals("var value = 'javascript:内容'; value", normalize.invoke(null, "var value = 'javascript:内容'; value"));
	}

	/**
	 * 用现有网页交互完整项目验证新增事件、属性、接口移除及脚本重载。
	 *
	 * @throws Exception 项目读取或编译失败
	 */
	public void testExistingOfflineExampleCompiles() throws Exception {
		assertNotNull(Compiler.compileForUnitTesting(new Project(new File("examples/网页浏览框/project.properties"))));
	}
}

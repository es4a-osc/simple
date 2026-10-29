package simple.compiler;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;
import simple.runtime.annotations.UsesPermission;

/**
 * 验证权限范围合并、单项注解及其重复容器，以及清单节点的权限声明边界。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class ManifestPermissionsTest extends TestCase {

	/**
	 * 无版本上限的需求优先于有限范围，两种声明顺序得到相同结果。
	 *
	 * @throws IOException 测试中的有效权限被拒绝。
	 */
	public void testUnboundedPermissionCoversBothOrders() throws IOException {
		for (int first : new int[] {0, 28}) {
			ManifestPermissions permissions = new ManifestPermissions();
			permissions.add("android.permission.WRITE_EXTERNAL_STORAGE", first);
			permissions.add("android.permission.WRITE_EXTERNAL_STORAGE", first == 0 ? 28 : 0);
			assertEquals(1, permissions.getPermissions().size());
			assertEquals(Integer.valueOf(0), permissions.getPermissions().get("android.permission.WRITE_EXTERNAL_STORAGE"));
		}
	}

	/**
	 * 全部声明都有上限时覆盖最高需求，批量权限及缺省单项权限仍不设上限。
	 *
	 * @throws IOException 测试中的有效注解被拒绝。
	 */
	public void testBoundedAndDefaultAnnotations() throws IOException {
		ManifestPermissions permissions = new ManifestPermissions();
		for (int maximum : new int[] {30, 28, 29, 30}) {
			permissions.add("fixture.permission.BOUNDED", maximum);
		}
		Map<String, String> single = new HashMap<String, String>();
		single.put("permissionName", "fixture.permission.DEFAULT");
		permissions.addAnnotation(single);
		permissions.addNames("android.permission.INTERNET, android.permission.INTERNET, ");
		assertEquals(3, permissions.getPermissions().size());
		assertEquals(Integer.valueOf(30), permissions.getPermissions().get("fixture.permission.BOUNDED"));
		assertEquals(Integer.valueOf(0), permissions.getPermissions().get("fixture.permission.DEFAULT"));
		assertEquals(Integer.valueOf(0), permissions.getPermissions().get("android.permission.INTERNET"));
	}

	/**
	 * 负数、无效名称及非整数上限不能静默变为不受限权限。
	 *
	 * @throws IOException 意外发生其它权限处理错误。
	 */
	public void testInvalidDeclarationsAreRejected() throws IOException {
		String[][] values = {{"", "0"}, {"a,b", "28"}, {"a b", "0"}, {"fixture.permission.TEST", "-1"},
				{"fixture.permission.TEST", "invalid"}};
		for (String[] value : values) {
			Map<String, String> fields = new HashMap<String, String>();
			fields.put("permissionName", value[0]);
			fields.put("maxSdkVersion", value[1]);
			ManifestPermissions permissions = new ManifestPermissions();
			try {
				permissions.addAnnotation(fields);
				fail("无效权限未被拒绝：" + fields);
			} catch (IOException expected) {
				assertTrue(permissions.getPermissions().isEmpty());
			}
		}
	}

	/**
	 * 普通节点和注释可以保留，所有权限节点必须走权限注解。
	 *
	 * @throws IOException 有效的非权限节点被拒绝。
	 */
	public void testManifestNodesRejectPermissionElements() throws IOException {
		ManifestPermissions.validateManifestNodes("<!-- uses-permission -->"
				+ "<uses-feature android:name=\"android.hardware.camera\" android:required=\"false\" />"
				+ "<meta-data android:name=\"description\" android:value=\"uses-permission\" />");
		for (String tag : new String[] {"uses-permission", "uses-permission-sdk-23", "uses-permission-sdk-m"}) {
			try {
				ManifestPermissions.validateManifestNodes("<" + tag + " android:name=\"android.permission.CAMERA\" />");
				fail("通用清单注解接受了权限节点：" + tag);
			} catch (IOException expected) {
				assertTrue(expected.getMessage().contains("@UsesPermission"));
			}
		}
	}

	/**
	 * 独立类加载器仍能展开重复注解容器，省略版本字段时读取默认值。
	 *
	 * @throws Exception 注解类型、容器或反射访问失败。
	 */
	public void testRepeatablePermissionsAcrossClassLoaders() throws Exception {
		URL tests = PermissionFixture.class.getProtectionDomain().getCodeSource().getLocation();
		URL runtime = UsesPermission.class.getProtectionDomain().getCodeSource().getLocation();
		try (URLClassLoader loader = new URLClassLoader(new URL[] {tests, runtime}, null)) {
			Class<? extends Annotation> type = loader.loadClass(UsesPermission.class.getName()).asSubclass(Annotation.class);
			Annotation[] values = loader.loadClass(PermissionFixture.class.getName()).getAnnotationsByType(type);
			assertEquals(2, values.length);
			assertEquals("android.permission.CAMERA", type.getMethod("permissionName").invoke(values[0]));
			assertEquals(0, type.getMethod("maxSdkVersion").invoke(values[0]));
			assertEquals(28, type.getMethod("maxSdkVersion").invoke(values[1]));
		}
	}

	/**
	 * 由 Java 自动生成容器的重复权限测试输入。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	@UsesPermission(permissionName = "android.permission.CAMERA")
	@UsesPermission(
		permissionName = "android.permission.WRITE_EXTERNAL_STORAGE",
		maxSdkVersion = 28
	)
	public static final class PermissionFixture {

		/**
		 * 禁止实例化只作为注解载体的测试类型。
		 */
		private PermissionFixture() {
		}
	}
}

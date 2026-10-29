package simple.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明公开对象或组件需要的一项 Android 权限，可在同一类型上重复使用。
 *
 * <p>此注解只负责清单声明，危险权限仍须在运行时取得用户授权。
 *
 * @author 树先生 xhwsd@qq.com
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(UsesPermission.List.class)
public @interface UsesPermission {

	/**
	 * 获取单项 Android 权限名称，不接受逗号分隔的权限列表。
	 *
	 * @return 非空的权限名称。
	 */
	String permissionName();

	/**
	 * 获取这项权限适用的最高 Android API 等级。
	 *
	 * @return 正数表示最高 API 等级，0 表示不设上限；不接受负数。
	 */
	int maxSdkVersion() default 0;

	/**
	 * 保存同一类型上的多项权限声明，由 Java 编译器处理重复注解时使用。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface List {

		/**
		 * 获取该类型声明的全部单项权限。
		 *
		 * @return 按源码声明顺序保存的权限注解。
		 */
		UsesPermission[] value();
	}
}

package simple.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明类库对象实际被项目引用时需要加入Android清单的XML节点。
 *
 * @author 树先生 xhwsd@qq.com
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ManifestNodes {

	/**
	 * 插入到{@code /manifest}节点下的XML
	 *
	 * <p>XML内容支持{@code ${宏名}}和{@code ${宏名=缺省值}}标签，编译时会替换为{@code project.properties}中{@code 类简名.宏名}对应的值。
	 *
	 * @return XML内容
	 */
	String rootXml() default "";

	/**
	 * 插入到{@code /manifest/application}节点下的XML
	 *
	 * <p>XML内容支持{@code ${宏名}}和{@code ${宏名=缺省值}}标签，编译时会替换为{@code project.properties}中{@code 类简名.宏名}对应的值。
	 *
	 * @return XML内容
	 */
	String applicationXml() default "";

	/**
	 * 插入到{@code /manifest/application/activity}节点下的XML
	 *
	 * <p>XML内容支持{@code ${宏名}}和{@code ${宏名=缺省值}}标签，编译时会替换为{@code project.properties}中{@code 类简名.宏名}对应值。
	 *
	 * @return XML内容
	 */
	String activityXml() default "";

	/**
	 * 插入到{@code /manifest/application/activity/intent-filter}节点下的XML
	 *
	 * <p>XML内容支持{@code ${宏名}}和{@code ${宏名=缺省值}}标签，编译时会替换为{@code project.properties}中{@code 类简名.宏名}对应值。
	 *
	 * @return XML内容
	 */
	String intentFilterXml() default "";
}

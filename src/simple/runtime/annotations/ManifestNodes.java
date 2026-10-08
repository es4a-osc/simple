/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simple.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明类库对象实际被项目引用时需要加入Android清单的XML节点。
 *
 * <p>XML支持{@code ${宏名}}和{@code ${宏名=缺省值}}。编译器从
 * {@code project.properties}读取“类简名.宏名”对应的值。
 *
 * @author 树先生 xhwsd@qq.com
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ManifestNodes {

	/** @return 插入到{@code /manifest}下的XML */
	String rootXml() default "";

	/** @return 插入到{@code /manifest/application}下的XML */
	String applicationXml() default "";

	/** @return 插入到主{@code activity}下的XML */
	String activityXml() default "";

	/** @return 插入到主{@code activity/intent-filter}下的XML */
	String intentFilterXml() default "";
}

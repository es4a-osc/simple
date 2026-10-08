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

package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.UsesPermissions;

/**
 * Simple的电话组件。
 *
 * <p>允许访问手机功能。
 *
 * @author Herbert Czymontek
 */
@SimpleComponent
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.CALL_PHONE")
public interface 电话 extends 组件 {

	/**
	 * 可用的属性获取方法（只读属性）。
	 *
	 * @return {@code true}表示电话功能可用，{@code false}表示电话功能不可用
	 */
	@SimpleProperty
	boolean 可用();

	/**
	 * 呼叫给定电话号码的电话。
	 *
	 * @param phoneNumber  仅数字形式的电话号码（无空格、破折号等）
	 */
	@SimpleFunction
	void 呼叫(String phoneNumber);

	/**
	 * 振动手机。
	 *
	 * @param duration  持续时间（以毫秒为单位）
	 */
	@SimpleFunction
	void 振动(int duration);
}

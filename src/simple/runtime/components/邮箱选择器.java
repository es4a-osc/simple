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
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.UsesPermissions;

/**
 * 输入邮箱，并从本机通讯录按姓名或邮箱地址显示自动补全候选。
 *
 * <p>输入至少一个字符，最多显示 50 个去重后的邮箱候选；建议功能需要读取联系人权限，
 * 未授权时仍可手动输入。全部操作和事件在主线程执行。
 * 失去焦点、停用或离开前台时取消查询并清除候选，保留当前文本；销毁时释放查询资源。
 *
 * @author Herbert Czymontek
 * @author Sharon Perl
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
@UsesPermissions(permissionNames =
	// 允许读取联系人姓名和邮箱，用于自动补全候选。
	"android.permission.READ_CONTACTS"
)
public interface 邮箱选择器 extends 文本组件 {

	/* 组件事件 */

	/**
	 * 输入框获得焦点时触发；不会修改当前文本。
	 */
	@SimpleEvent
	void 获得焦点();

	/**
	 * 输入框失去焦点时触发；取消建议查询，保留当前文本。
	 */
	@SimpleEvent
	void 失去焦点();

	/**
	 * 用户选中一个邮箱候选后触发；文本已更新为带姓名的邮箱格式，无姓名时为邮箱地址。
	 *
	 * @param name 联系人显示名
	 * @param address 选中的邮箱地址
	 */
	@SimpleEvent
	void 选择完毕(String name, String address);

	/**
	 * 有效建议查询因权限或联系人提供者错误而失败时触发；没有匹配项不视为失败。
	 *
	 * @param message 失败原因
	 */
	@SimpleEvent
	void 操作失败(String message);

	/* 组件属性 */

	/**
	 * 获取是否允许输入和显示邮箱建议。
	 *
	 * @return 启用时为真，组件销毁后为假
	 */
	@SimpleProperty
	boolean 启用();

	/**
	 * 设置是否允许输入和显示邮箱建议；停用时取消查询并保留文本。
	 *
	 * @param enabled 是否启用
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "True"
	)
	void 启用(boolean enabled);

	/**
	 * 获取邮箱输入框是否使用单行显示。
	 *
	 * @return 单行时为真
	 */
	@SimpleProperty
	boolean 单行模式();

	/**
	 * 设置邮箱输入框是否使用单行显示，初始为真。
	 *
	 * @param singleLine 是否单行
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "True"
	)
	void 单行模式(boolean singleLine);

	/**
	 * 获取最近一次有效建议查询的失败原因。
	 *
	 * @return 无错误或查询成功后为空文本
	 */
	@SimpleProperty
	String 错误信息();
}

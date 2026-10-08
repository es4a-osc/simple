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
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.parameters.BooleanReferenceParameter;

/**
 * Simple的文本框组件。
 *
 * @author Herbert Czymontek
 */
@SimpleComponent
@SimpleObject
public interface 编辑框 extends 文本组件 {

	/**
	 * 默认获得焦点事件处理器。
	 */
	@SimpleEvent
	void 获得焦点();

	/**
	 * 默认失去焦点事件处理器。
	 */
	@SimpleEvent
	void 失去焦点();

	/**
	 * 默认文本改变事件处理器。
	 *
	 * <p>注意参数{@code accept}暂时无效
	 *
	 * @param text 要验证的文本
	 * @param accept 设置为{@code false}以拒绝文本输入，默认值为{@code true}。
	 */
	@SimpleEvent
	void 文本改变(String text, BooleanReferenceParameter accept);

	/**
	 * 启用属性获取器方法。
	 *
	 * @return {@code true}表示已启用，{@code false}表示已禁用
	 */
	@SimpleProperty
	boolean 启用();

	/**
	 * 启用属性设置器方法。
	 *
	 * @param enabled {@code true}表示已启用，{@code false}表示已禁用
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "True"
	)
	void 启用(boolean enabled);

	/**
	 * 提示属性获取器方法。
	 *
	 * @return  提示文本
	 */
	@SimpleProperty
	String 提示文本();

	/**
	 * 提示属性设置器方法。
	 *
	 * @param hint  提示文本
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_STRING,
		initializer = "\"\""
	)
	void 提示文本(String hint);

	/**
	 * 提示颜色属性获取方法。
	 *
	 * @return 带透明度的文本RGB颜色。
	 */
	@SimpleProperty
	int 提示颜色();

	/**
	 * 提示颜色属性设置方法。
	 *
	 * @param color 带透明度的文本RGB颜色。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_COLOR
	)
	void 提示颜色(int color);

	/**
	 * 光标可视属性获取方法。
	 *
	 * @return 光标是否可视，可视返回{@code true}，隐藏返回{@code false}。
	 */
	@SimpleProperty
	boolean 光标可视();

	/**
	 * 光标可视属性设置方法。
	 *
	 * @param visible 光标是否可视，可视返回{@code true}，隐藏返回{@code false}。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "True"
	)
	void 光标可视(boolean visible);

	/**
	 * 光标位置属性获取方法。
	 *
	 * @return 光标位置。
	 */
	@SimpleProperty
	int 光标位置();

	/**
	 * 光标位置属性设置方法。
	 *
	 * @param location 光标位置。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER,
		initializer = "0"
	)
	void 光标位置(int location);

	/**
	 * 输入模式属性获取方法。
	 *
	 * @return 当前输入模式，可选值请参考{@link 组件#输入模式_文本}等常量。
	 */
	@SimpleProperty
	int 输入模式();

	/**
	 * 输入模式属性设置方法。
	 *
	 * @param inputModel 当前输入模式，可选值请参考{@link 组件#输入模式_文本}等常量。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER,
		initializer = 组件.输入模式_文本 + ""
	)
	void 输入模式(int inputModel);

	/**
	 * 使其获得焦点。
	 *
	 * @return 获得焦点返回{@cdoe true}，否则返回{@code false}。
	 */
	@SimpleFunction
	boolean 获取焦点();

	/**
	 * 使其清除焦点。
	 */
	@SimpleFunction
	void 清除焦点();

	/**
	 * 在内容末尾加入文本。
	 * @param text 加入文本。
	 */
	@SimpleFunction
	void 加入文本(String text);

	/**
	 * 插入文本到指定位置。
	 *
	 * @param location 插入位置。
	 * @param text 插入文本。
	 */
	@SimpleFunction
	void 插入文本(int location, String text);

	/**
	 * 删除指定文本。
	 *
	 * @param start 起始位置。
	 * @param stop 结束位置。
	 */
	@SimpleFunction
	void 删除文本(int start, int stop);

	/**
	 * 选中指点文本。
	 *
	 * @param start 开始位置。
	 * @param stop 停止位置。
	 */
	@SimpleFunction
	void 选中文本(int start, int stop);

	/**
	 * 选中所有文本。
	 */
	@SimpleFunction
	void 全选文本();

	/**
	 * 以该组件为瞄点显示输入法。
	 */
	@SimpleFunction
	void 显示输入法();

	/**
	 * 隐藏输入法。
	 */
	@SimpleFunction
	void 隐藏输入法();
}

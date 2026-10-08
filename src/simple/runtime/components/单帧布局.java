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

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

/**
 * Simple的单帧布局组件。
 *
 * @author David Foster
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public interface 单帧布局 extends 布局 {

	/**
	 * 将指点窗口添加到布局。
	 *
	 * @param form 窗口实例。
	 */
	@SimpleFunction
	void 添加(窗口 form);
}

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
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.variants.Variant;

/**
 * 可放置在窗口或其他容器中的容器。
 *
 * @author Herbert Czymontek
 */
@SimpleComponent
@SimpleObject
public interface 面板 extends 可视组件, 组件容器 {

	/**
	 * 布局属性获取器。
	 *
	 * @return  布局实例
	 */
	@SimpleProperty
	Variant 布局();

	/**
	 * 布局属性设置器：我们为将常量转换为布局对象实例的属性为属性分配一个布局。
	 *
	 * @param layoutType  其中之一{@link 组件#布局_线性}，
	 *                    {@link 组件#布局_表格}或
	 *                    {@link 组件#布局_单帧}
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_LAYOUT,
		initializer = 组件.布局_线性 + ""
	)
	void 布局(Variant layoutType);

	// 覆盖属性默认值

	@Override
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_COLOR,
		initializer = 组件.颜色_白 + ""
	)
	void 背景颜色(int color);
}

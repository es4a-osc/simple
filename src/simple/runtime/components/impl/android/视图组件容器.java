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

package simple.runtime.components.impl.android;

import android.view.ViewGroup;
import simple.runtime.components.组件容器;

/**
 * 可以包含其它组件的组件需要实现此接口。
 *
 * @author Herbert Czymontek
 */
public interface 视图组件容器 extends 组件容器 {

	/**
	 * 返回容器的布局管理器。
	 *
	 * @return 布局管理器
	 */
	ViewGroup getLayoutManager();
}

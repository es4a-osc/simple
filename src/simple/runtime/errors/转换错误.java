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

package simple.runtime.errors;

import simple.runtime.annotations.SimpleObject;

/**
 * 运行时错误，指示将类型的值转换为另一种类型的值的尝试失败，
 * 例如将字符串“foo”转换为整数，但也从基本类型转换为没有关系的派生类型。
 *
 * @author Herbert Czymontek
 */
@SuppressWarnings("serial")
@SimpleObject
public final class 转换错误 extends 运行错误 {
}

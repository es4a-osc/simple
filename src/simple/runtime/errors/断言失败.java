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

import simple.runtime.断言操作;
import simple.runtime.annotations.SimpleObject;

/**
 * 运行时错误，指示断言失败。
 *
 * <p>由 {@link 断言操作} 中的功能引起。
 *
 * @author Herbert Czymontek
 */
@SuppressWarnings("serial")
@SimpleObject
public final class 断言失败 extends 运行错误 {
}

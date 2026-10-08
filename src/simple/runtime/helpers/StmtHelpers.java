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

package simple.runtime.helpers;

import simple.runtime.数组操作;
import simple.runtime.collections.集合;
import simple.runtime.variants.ArrayVariant;
import simple.runtime.variants.IntegerVariant;
import simple.runtime.variants.ObjectVariant;
import simple.runtime.variants.Variant;

/**
 * 用于太复杂而无法内联的语句的辅助方法。
 *
 * @author Herbert Czymontek
 */
public final class StmtHelpers {

	private StmtHelpers() {  // COV_NF_LINE
	}                        // COV_NF_LINE

	/**
	 * 返回给定变量中集合或数组的元素计数。
	 *
	 * @param v  包含集合或数组的变量
	 * @return  元素计数
	 */
	public static int forEachCount(Variant v) {
		if (v instanceof ArrayVariant) {
			return 数组操作.取数组下标(v, 1);
		} else {
			// Might cause a class cast exception which will be converted to a runtime error by Simple
			// exception handlers
			return ((集合) ((ObjectVariant) v).getObject()).计数();
		}
	}

	/**
	 * 从给定变量的集合或数组中返回具有给定索引的元素。
	 *
	 * @param v  包含集合或数组的变量
	 * @param index  元素索引
	 * @return  元素
	 */
	public static Variant forEachItem(Variant v, int index) {
		if (v instanceof ArrayVariant) {
			return ((ArrayVariant) v).array(new Variant[] { IntegerVariant.getIntegerVariant(index) });
		} else {
			// Will not cause any exception because forEachCount() would have already thrown it
			return ((集合) ((ObjectVariant) v).getObject()).项目(index);
		}
	}
}

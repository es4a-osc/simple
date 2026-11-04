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

package simple.runtime.variants;

import java.util.Calendar;

import simple.runtime.helpers.ConvHelpers;

/**
 * Date variant implementation.
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public final class DateVariant extends Variant {

	// Date value
	private Calendar value;

	/**
	 * Factory method for creating date variants.
	 *
	 * @param value  date
	 * @return  new date variant
	 */
	public static final DateVariant getDateVariant(Calendar value) {
		return new DateVariant(value);
	}

	/**
	 * Creates a new date variant.
	 */
	private DateVariant(Calendar value) {
		super(VARIANT_DATE);
		this.value = value;
	}

	@Override
	public long getLong() {
		// 取日期时间对应的 Unix 毫秒时间戳。
		return ConvHelpers.date2long(value);
	}

	@Override
	public String getString() {
		// 取带三位毫秒和 Z 标记的 ISO 8601 UTC 日期时间文本。
		return ConvHelpers.date2string(value);
	}

	@Override
	public Calendar getDate() {
		return value;
	}

	@Override
	public boolean identical(Variant rightOp) {
		return cmp(rightOp) == 0;
	}

	@Override
	public int cmp(Variant rightOp) {
		if (rightOp.getKind() != VARIANT_DATE) {
			// Will cause a runtime error
			return super.cmp(rightOp);
		}

		return value.compareTo(rightOp.getDate());
	}
}

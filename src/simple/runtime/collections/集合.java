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

package simple.runtime.collections;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.errors.索引超出界限错误;
import simple.runtime.variants.Variant;

import java.util.ArrayList;

/**
 * 集合是一组有序的项目。与所有成员必须具有相同数据类型的数组不同，集合不具有该限制。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 集合 {
	/*
	关联类：
	simple.compiler.statements.ForEachStatement
	*/

	// 集合元素的容器
	private ArrayList<Variant> list;

	/**
	 * 构造新{@code ArrayList}的集合对象。
	 */
	public 集合() {
		list = new ArrayList<Variant>();
	}

	/**
	 * 构造指定{@code ArrayList}的集合对象
	 *
	 * @param list {@code ArrayList}实例。
	 */
	public 集合(ArrayList<Variant> list) {
		this.list = list;
	}

	/**
	 * 取{@code ArrayList}实例。
	 *
	 * @return {@code ArrayList}实例。
	 */
	public ArrayList<Variant> getList() {
		return list;
	}

	/**
	 * 从集合中删除所有项目。
	 */
	@SimpleFunction
	public void 清空() {
		list.clear();
	}

	/**
	 * 将新项添加到集合中。
	 *
	 * @param item 要添加的项目
	 */
	@SimpleFunction
	public void 加入(Variant item) {
		list.add(item);
	}

	/**
	 * 返回指定位置的项目。
	 *
	 * @param index 欲插入项目的位置
	 * @param item 项目添加
	 * @throws 索引超出界限错误
	 */
	@SimpleFunction
	public void 插入(int index, Variant item) {
		try {
			list.add(index, item);
		} catch (IndexOutOfBoundsException e) {
			throw new 索引超出界限错误();
		}
	}

	/**
	 * 返回指定位置的项目。
	 *
	 * @param index  项目位置
	 * @return  项目
	 * @throws 索引超出界限错误
	 */
	@SimpleFunction
	public Variant 项目(int index) {
		/*
		 * 关联代码：
		 * {@link simple.compiler.statements.ForEachStatement#generate(simple.classfiles.Method)}
		 * {@link simple.runtime.helpers.StmtHelpers#forEachItem(Variant, int)}
		 */
		try {
			return list.get(index);
		} catch (IndexOutOfBoundsException e) {
			throw new 索引超出界限错误();
		}
	}

	/**
	 * 重新设置指点位置的项目值。
	 *
	 * @param index  项目位置
	 * @param Variant  新项目值
	 * @throws 索引超出界限错误
	 */
	@SimpleFunction
	public void 替换(int index, Variant item) {
		try {
			list.set(index, item);
		} catch (IndexOutOfBoundsException e) {
			throw new 索引超出界限错误();
		}
	}

	/**
	 * 返回集合中的项目数。
	 *
	 * @return  集合中的项目数
	 */
	@SimpleProperty
	public int 计数() {
		/*
		关联代码：
		{@link simple.compiler.statements.ForEachStatement#generate(simple.classfiles.Method)}
		{@link simple.runtime.helpers.StmtHelpers#forEachCount(Variant)}
		*/
		return list.size();
	}

	/**
	 * 从集合中移除指点位置的项。
	 *
	 * @param index  项目位置
	 * @throws 索引超出界限错误
	 */
	@SimpleFunction
	public void 删除(int index) {
		try {
			list.remove(index);
		} catch (IndexOutOfBoundsException e) {
			throw new 索引超出界限错误();
		}
	}

	/**
	 * 将指点集合并入到当前集合。
	 *
	 * @param collection 集合
	 */
	@SimpleFunction
	public void 并入(集合 collection) {
		list.addAll(collection.getList());
	}

	/**
	 * 将数组并入到当前集合。
	 *
	 * @param array 数组。
	 */
	@SimpleFunction
	public void 并入数组(Variant[] array) {
		for (Variant value : array) {
			list.add(value);
		}
	}

	/**
	 * 将集合转为数组。
	 *
	 * @return 返回变体数组。
	 */
	@SimpleFunction
	public Variant[] 到数组() {
		return list.toArray(new Variant[0]);
	}

	/**
	 * 将集合内的项目顺序打乱。
	 */
	@SimpleFunction
	public void 打乱() {
		java.util.Collections.shuffle(list);
	}
}

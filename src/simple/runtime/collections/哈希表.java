package simple.runtime.collections;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.variants.ObjectVariant;
import simple.runtime.variants.Variant;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Simple的哈希表对象
 *
 * 内部使用{@link java.util.HashMap}实现
 * 注意：项目是无序的，并非线程安全
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 哈希表 {

	private HashMap<String, Variant> map;
	private Iterator<Map.Entry<String, Variant>> iterator;
	private Map.Entry<String, Variant> entry;

	/**
	 * 构造新{@code HashMap}的哈希表对象。
	 */
	public 哈希表() {
		map = new HashMap<String, Variant>();
	}

	/**
	 * 构造指定{@code HashMap}的哈希表对象。
	 *
	 * @param map {@code HashMap}实例。
	 */
	public 哈希表(HashMap<String, Variant> map) {
		this.map = map;
	}

	/**
	 * 取{@code HashMap}实例。
	 *
	 * @return {@code HashMap}实例。
	 */
	public HashMap<String, Variant> getMap() {
		return map;
	}

	/**
	 * 清除所有键值对。
	 */
	@SimpleFunction
	public void 清除() {
		map.clear();
	}

	/**
	 * 加入键值对。
	 *
	 * @param key 键名
	 * @param value 值
	 */
	@SimpleFunction
	public void 加入(String key, Variant value) {
		map.put(key, value);
	}

	/**
	 * 取指定键的值。
	 *
	 * @param key 键名
	 * @return 成功返回值，否则返回{@code null}
	 */
	@SimpleFunction
	public Variant 取值(String key) {
		Variant value = map.get(key);
		// 注意这里：创建了个空 ObjectVariant
		if (value == null) {
			value = ObjectVariant.getObjectVariant(null);
		}
		return value;
	}

	/**
	 * 取项目数。
	 *
	 * @return 项目数
	 */
	@SimpleProperty
	public int 计数() {
		return map.size();
	}

	/**
	 * 删除指定键。
	 *
	 * @param key 键名
	 */
	@SimpleFunction
	public void 删除(String key) {
		map.remove(key);
	}

	/**
	 * 检验是否包含指定键。
	 *
	 * @param key 键
	 * @return 已包含返回{@code true}，未包含返回{@code false}
	 */
	@SimpleFunction
	public boolean 是否包含键(String key) {
		return map.containsKey(key);
	}

	/**
	 * 取所有键。
	 *
	 * @return 所有键
	 */
	@SimpleFunction
	public String[] 取所有键() {
		return map.keySet().toArray(new String[0]);
	}

	/**
	 * 取所有值。
	 *
	 * @return 所有值
	 */
	@SimpleFunction
	public Variant[] 取所有值() {
		return map.values().toArray(new Variant[0]);
	}

	/**
	 * 迭代初始化，迭代将到起始。
	 */
	@SimpleFunction
	public void 迭代初始() {
		iterator = map.entrySet().iterator();
	}

	/**
	 * 迭代到下个。
	 *
	 * @return 成功返回{@code true}，失败返回{@code false}
	 */
	@SimpleFunction
	public boolean 迭代下个() {
		// TODO 检验迭代器是否为实例
		if (iterator.hasNext()) {
			entry = iterator.next();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 删除迭代后项目
	 */
	@SimpleFunction
	public void 迭代删除() {
		// TODO 检验迭代器是否为实例
		iterator.remove();
	}

	/**
	 * 取迭代后的键。
	 *
	 * @return 键
	 */
	@SimpleFunction
	public String 迭代取键() {
		// TODO 检验元素是否为实例
		return entry.getKey();
	}

	/**
	 * 取迭代后的值。
	 *
	 * @return 值
	 */
	@SimpleFunction
	public Variant 迭代取值() {
		// TODO 检验元素是否为实例
		return entry.getValue();
	}
}

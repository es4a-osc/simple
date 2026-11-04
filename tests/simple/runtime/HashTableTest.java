package simple.runtime;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;
import simple.runtime.collections.哈希表;
import simple.runtime.collections.集合;
import simple.runtime.errors.索引超出界限错误;
import simple.runtime.variants.IntegerVariant;
import simple.runtime.variants.Variant;

/**
 * 验证哈希表的键值、缺失值、快照与迭代删除。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class HashTableTest extends TestCase {

	/**
	 * 哈希表覆盖同名键、缺失值、删除和独立导出数组。
	 */
	public void testHashTableKeysValuesAndMissingValues() {
		哈希表 values = new 哈希表();
		assertEquals(0, values.计数());
		assertNull(values.取值("不存在").getObject());
		values.加入("甲", number(1));
		values.加入("乙", number(2));
		values.加入("甲", number(3));
		assertEquals(2, values.计数());
		assertEquals(3, values.取值("甲").getInteger());
		assertTrue(values.是否包含键("甲"));
		assertEquals(new HashSet<String>(Arrays.asList("甲", "乙")),
			new HashSet<String>(Arrays.asList(values.取所有键())));
		Variant[] exported = values.取所有值();
		assertEquals(2, exported.length);
		exported[0] = number(99);
		assertEquals(3, values.取值("甲").getInteger());

		values.删除("不存在");
		values.删除("乙");
		assertFalse(values.是否包含键("乙"));
		values.清除();
		values.清除();
		assertEquals(0, values.计数());
	}

	/**
	 * 迭代遍历不依赖哈希顺序，并通过迭代器删除当前项。
	 */
	public void testHashTableIterationAndRemoval() {
		哈希表 values = new 哈希表();
		values.加入("甲", number(1));
		values.加入("乙", number(2));
		values.迭代初始();
		Set<String> seen = new HashSet<String>();
		int sum = 0;
		while (values.迭代下个()) {
			seen.add(values.迭代取键());
			sum += values.迭代取值().getInteger();
			values.迭代删除();
		}
		assertEquals(2, seen.size());
		assertEquals(3, sum);
		assertEquals(0, values.计数());
		assertFalse(values.迭代下个());
		values.迭代初始();
		assertFalse(values.迭代下个());
	}

	/**
	 * 创建测试使用的整数变体。
	 *
	 * @param value 整数值
	 * @return 对应的变体
	 */
	private static Variant number(int value) {
		return IntegerVariant.getIntegerVariant(value);
	}
}

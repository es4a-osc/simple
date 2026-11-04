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
 * 验证集合的增删改、顺序、边界与导出数组。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class CollectionTest extends TestCase {

	/**
	 * 集合增删改、插入和合并保持顺序，导出数组不会成为内部存储。
	 */
	public void testCollectionMutationsAndArrayIsolation() {
		集合 values = new 集合();
		assertEquals(0, values.计数());
		values.加入(number(1));
		values.插入(0, number(2));
		values.替换(1, number(3));
		集合 other = new 集合();
		other.加入(number(4));
		values.并入(other);
		Variant[] source = {number(5), number(6)};
		values.并入数组(source);
		source[0] = number(99);
		assertEquals(5, values.计数());
		assertEquals(2, values.项目(0).getInteger());
		assertEquals(5, values.项目(3).getInteger());

		Variant[] copy = values.到数组();
		copy[0] = number(88);
		assertEquals(2, values.项目(0).getInteger());
		values.删除(1);
		assertEquals(4, values.计数());
		assertEquals(4, values.项目(1).getInteger());
		values.清空();
		values.清空();
		assertEquals(0, values.到数组().length);
	}

	/**
	 * 洗牌保留全部成员，不要求随机结果必须改变排列。
	 */
	public void testShufflePreservesMembers() {
		集合 values = new 集合();
		values.打乱();
		for (int index = 0; index < 20; index++) {
			values.加入(number(index));
		}

		values.打乱();
		Set<Integer> actual = new HashSet<Integer>();
		for (Variant value : values.到数组()) {
			actual.add(value.getInteger());
		}
		assertEquals(20, actual.size());
		for (int index = 0; index < 20; index++) {
			assertTrue(actual.contains(index));
		}
	}

	/**
	 * 四种索引操作均拒绝负数及超出范围的索引，失败后原数据保持。
	 */
	public void testCollectionInvalidIndicesAreAtomic() {
		集合 values = new 集合();
		values.加入(number(7));
		for (int operation = 0; operation < 4; operation++) {
			for (int index : new int[]{-1, 2}) {
				try {
					switch (operation) {
						case 0:
							values.项目(index);
							break;
						case 1:
							values.删除(index);
							break;
						case 2:
							values.替换(index, number(0));
							break;
						default: values.插入(index, number(0)); break;
					}
					fail("索引操作应失败：" + operation + "/" + index);
				} catch (索引超出界限错误 expected) {
					assertEquals(1, values.计数());
					assertEquals(7, values.项目(0).getInteger());
				}
			}
		}
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

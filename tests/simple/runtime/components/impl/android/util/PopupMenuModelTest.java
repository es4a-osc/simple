package simple.runtime.components.impl.android.util;

import junit.framework.TestCase;

import simple.runtime.errors.索引超出界限错误;

/**
 * 验证菜单项标识、父子关系、分组排序和失败时的状态保持。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class PopupMenuModelTest extends TestCase {

	/**
	 * 标题相同的项目具有不同标识，删除或清空后旧标识不会被复用。
	 */
	public void testIdsRemainStableAndAreNotReused() {
		PopupMenuModel model = new PopupMenuModel();
		int first = model.add("同名");
		int second = model.add("同名");
		assertTrue(first > 0);
		assertTrue(second > first);
		model.remove(first);
		assertEquals(second, model.children(0).get(0).id);
		model.clear();
		assertTrue(model.add("新项目") > second);
	}

	/**
	 * 项目分组和排序不串项，返回列表的修改不会影响模型。
	 */
	public void testMetadataAndListIsolation() {
		PopupMenuModel model = new PopupMenuModel();
		int first = model.add("甲");
		int second = model.add("乙");
		model.setGroup(first, 3);
		model.setOrder(second, 7);
		assertEquals(3, model.getGroup(first));
		assertEquals(0, model.getGroup(second));
		assertEquals(0, model.getOrder(first));
		assertEquals(7, model.getOrder(second));
		assertEquals("甲", model.children(0).get(0).title);
		assertEquals(7, model.children(0).get(1).getOrder());
		model.children(0).clear();
		assertEquals(2, model.children(0).size());
	}

	/**
	 * 拒绝父子循环并保持原关系，允许子项重新移到顶层。
	 */
	public void testCycleRejectionIsAtomic() {
		PopupMenuModel model = new PopupMenuModel();
		int parent = model.add("父");
		int child = model.add("子");
		int grandchild = model.add("孙");
		model.setParent(child, parent);
		model.setParent(grandchild, child);
		for (int candidate : new int[] { parent, child, grandchild }) {
			try {
				model.setParent(parent, candidate);
				fail("父项不能指向自身或后代");
			} catch (IllegalArgumentException expected) {
				assertEquals(0, model.getParent(parent));
				assertEquals(parent, model.getParent(child));
			}
		}

		model.setParent(child, 0);
		assertEquals(2, model.children(0).size());
		assertEquals(child, model.getParent(grandchild));
	}

	/**
	 * 删除父项同时清理后代，其他分支及标识保持不变。
	 */
	public void testRemovalClearsOnlyTheSelectedSubtree() {
		PopupMenuModel model = new PopupMenuModel();
		int parent = model.add("父");
		int child = model.add("子");
		int grandchild = model.add("孙");
		int other = model.add("其他分支");
		model.setParent(child, parent);
		model.setParent(grandchild, child);
		model.remove(parent);
		model.remove(parent);
		assertTrue(model.children(parent).isEmpty());
		assertTrue(model.children(child).isEmpty());
		assertEquals(1, model.children(0).size());
		assertEquals(other, model.children(0).get(0).id);
	}

	/**
	 * 缺失父项和空标题不能留下半次修改。
	 */
	public void testInvalidInputPreservesExistingItems() {
		PopupMenuModel model = new PopupMenuModel();
		int id = model.add("已有");
		try {
			model.setParent(id, id + 100);
			fail("缺失父项应报错");
		} catch (索引超出界限错误 expected) {
			assertEquals(0, model.getParent(id));
		}

		try {
			model.add(null);
			fail("空引用标题应报错");
		} catch (IllegalArgumentException expected) {
			assertEquals(1, model.children(0).size());
		}

		try {
			model.getGroup(id + 100);
			fail("缺失项目应报错");
		} catch (索引超出界限错误 expected) {
			assertEquals("已有", model.children(0).get(0).title);
		}
	}
}

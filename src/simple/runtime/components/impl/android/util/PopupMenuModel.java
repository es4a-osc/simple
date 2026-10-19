package simple.runtime.components.impl.android.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import simple.runtime.errors.索引超出界限错误;

/**
 * 保存动态菜单项、分组、排序和无环父子关系，不依赖 Android 视图。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class PopupMenuModel {

	private final Map<Integer, Item> items = new LinkedHashMap<Integer, Item>();
	private long nextId = 1;

	/**
	 * 创建空菜单，0 保留为顶层标识。
	 */
	public PopupMenuModel() {
	}

	/**
	 * 保存单项数据；标识和标题固定，关系只能通过模型修改。
	 */
	public static final class Item {

		/** 菜单项的正整数标识。 */
		public final int id;
		/** 菜单项标题。 */
		public final String title;
		private int group;
		private int order;
		private int parent;

		/**
		 * 创建具有稳定标识的顶层菜单项。
		 *
		 * @param id 项目标识。
		 * @param title 项目标题。
		 */
		private Item(int id, String title) {
			this.id = id;
			this.title = title;
		}

		/**
		 * 返回分组标识。
		 *
		 * @return 分组标识，0 表示未分组。
		 */
		public int getGroup() {
			return group;
		}

		/**
		 * 返回 Android 菜单排序值。
		 *
		 * @return 排序值。
		 */
		public int getOrder() {
			return order;
		}
	}

	/* 菜单项目 */

	/**
	 * 添加顶层菜单项，不复用已发出的标识。
	 *
	 * @param title 标题，不得为空引用。
	 * @return 新菜单项标识。
	 * @throws IllegalArgumentException 标题为空引用。
	 * @throws IllegalStateException 正整数标识已耗尽。
	 */
	public int add(String title) {
		if (title == null) {
			throw new IllegalArgumentException("菜单项标题不得为空引用");
		}
		if (nextId > Integer.MAX_VALUE) {
			throw new IllegalStateException("菜单项标识已耗尽");
		}

		int id = (int) nextId++;
		items.put(id, new Item(id, title));
		return id;
	}

	/**
	 * 删除菜单项及其后代，不存在的标识不作处理。
	 *
	 * @param id 菜单项标识。
	 */
	public void remove(int id) {
		if (!items.containsKey(id)) {
			return;
		}

		for (Item child : children(id)) {
			remove(child.id);
		}
		items.remove(id);
	}

	/**
	 * 清空菜单项，保留标识序列，避免旧回调标识与新项目混淆。
	 */
	public void clear() {
		items.clear();
	}

	/**
	 * 按添加次序返回指定父项的直接子项列表。
	 *
	 * @param parent 父项标识，0 表示顶层。
	 * @return 独立列表；排序值由 Android 菜单在显示时处理。
	 */
	public List<Item> children(int parent) {
		List<Item> result = new ArrayList<Item>();
		for (Item item : items.values()) {
			if (item.parent == parent) {
				result.add(item);
			}
		}
		return result;
	}

	/* 分组与排序 */

	/**
	 * 读取菜单项分组。
	 *
	 * @param id 菜单项标识。
	 * @return 分组标识。
	 * @throws 索引超出界限错误 菜单项不存在。
	 */
	public int getGroup(int id) {
		return requireItem(id).group;
	}

	/**
	 * 更新菜单项分组。
	 *
	 * @param id 菜单项标识。
	 * @param group 分组标识。
	 * @throws 索引超出界限错误 菜单项不存在。
	 */
	public void setGroup(int id, int group) {
		requireItem(id).group = group;
	}

	/**
	 * 读取菜单项排序值。
	 *
	 * @param id 菜单项标识。
	 * @return 排序值。
	 * @throws 索引超出界限错误 菜单项不存在。
	 */
	public int getOrder(int id) {
		return requireItem(id).order;
	}

	/**
	 * 更新菜单项排序值。
	 *
	 * @param id 菜单项标识。
	 * @param order Android 菜单排序值。
	 * @throws 索引超出界限错误 菜单项不存在。
	 */
	public void setOrder(int id, int order) {
		requireItem(id).order = order;
	}

	/* 父子关系 */

	/**
	 * 读取菜单项父项。
	 *
	 * @param id 菜单项标识。
	 * @return 父项标识，0 表示顶层。
	 * @throws 索引超出界限错误 菜单项不存在。
	 */
	public int getParent(int id) {
		return requireItem(id).parent;
	}

	/**
	 * 校验父子关系后重新挂接菜单项，失败时保留原关系。
	 *
	 * @param id 菜单项标识。
	 * @param parent 父项标识，0 表示顶层。
	 * @throws 索引超出界限错误 项目或非零父项不存在。
	 * @throws IllegalArgumentException 父项是自身或自身的后代。
	 */
	public void setParent(int id, int parent) {
		Item item = requireItem(id);
		for (int ancestor = parent; ancestor != 0; ancestor = requireItem(ancestor).parent) {
			if (ancestor == id) {
				throw new IllegalArgumentException("菜单父子关系不能形成循环");
			}
		}
		item.parent = parent;
	}

	/**
	 * 读取已存在的项目，不允许把缺失项目误当作默认值。
	 *
	 * @param id 菜单项标识。
	 * @return 已存在的菜单项。
	 * @throws 索引超出界限错误 菜单项不存在。
	 */
	private Item requireItem(int id) {
		Item item = items.get(id);
		if (item == null) {
			throw new 索引超出界限错误();
		}
		return item;
	}
}

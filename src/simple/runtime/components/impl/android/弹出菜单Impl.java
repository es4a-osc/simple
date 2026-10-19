package simple.runtime.components.impl.android;

import android.view.Menu;
import android.view.View;
import android.widget.PopupMenu;

import simple.runtime.android.MainActivity;
import simple.runtime.components.可视组件;
import simple.runtime.components.弹出菜单;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.components.impl.android.util.PopupMenuModel;
import simple.runtime.components.impl.android.util.ResourcesUtil;
import simple.runtime.errors.文件未存在错误;
import simple.runtime.events.EventDispatcher;

/**
 * 将菜单资源或动态父子菜单投影为 Android PopupMenu，管理显示与销毁回调。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class 弹出菜单Impl extends 组件Impl implements 弹出菜单 {

	private final MainActivity activity;
	private final PopupMenuModel model = new PopupMenuModel();
	private final MainActivity.OnDestroyListener destroyListener;
	private String menuRes = "";
	private PopupMenu popup;
	private boolean destroyed;

	/**
	 * 创建非可视组件并登记当前 Activity 的销毁监听。
	 *
	 * @param container 容纳本组件的窗口。
	 */
	public 弹出菜单Impl(组件容器 container) {
		super(container);
		activity = MainActivity.getContext();
		destroyListener = new MainActivity.OnDestroyListener() {

			@Override
			public void onDestroy() {
				dispose(false);
			}
		};
		activity.addOnDestroyListener(destroyListener);
	}

	/* 组件事件 */

	@Override
	public void 被选中(int itemId) {
		EventDispatcher.dispatchEvent(this, "被选中", itemId);
	}

	@Override
	public void 被关闭() {
		EventDispatcher.dispatchEvent(this, "被关闭");
	}

	/* 菜单数据 */

	@Override
	public String 菜单项() {
		return menuRes;
	}

	@Override
	public void 菜单项(String menuRes) {
		requireAlive();
		String source = menuRes == null ? "" : menuRes;
		if (!source.isEmpty()) {
			if (!source.startsWith("@menu/") || ResourcesUtil.getResourcesId(source) == 0) {
				throw new 文件未存在错误(source);
			}
			model.clear();
		}
		this.menuRes = source;
	}

	@Override
	public int 添加(String title) {
		requireAlive();
		int id = model.add(title);
		menuRes = "";
		return id;
	}

	@Override
	public void 删除(int itemId) {
		requireAlive();
		model.remove(itemId);
	}

	@Override
	public void 清空() {
		requireAlive();
		model.clear();
	}

	@Override
	public int 取分组(int itemId) {
		requireAlive();
		return model.getGroup(itemId);
	}

	@Override
	public void 置分组(int itemId, int groupId) {
		requireAlive();
		model.setGroup(itemId, groupId);
	}

	@Override
	public int 取排序(int itemId) {
		requireAlive();
		return model.getOrder(itemId);
	}

	@Override
	public void 置排序(int itemId, int order) {
		requireAlive();
		model.setOrder(itemId, order);
	}

	@Override
	public int 取父项(int itemId) {
		requireAlive();
		return model.getParent(itemId);
	}

	@Override
	public void 置父项(int itemId, int parentId) {
		requireAlive();
		model.setParent(itemId, parentId);
	}

	/* 菜单显示 */

	@Override
	public void 弹出(可视组件 visibleComponent) {
		requireAlive();
		if (!(visibleComponent instanceof 视图组件)) {
			throw new IllegalArgumentException("弹出菜单需要可视组件作为锚点");
		}

		View anchor = ((视图组件) visibleComponent).getView();
		if (activity.isFinishing() || anchor.getWindowToken() == null) {
			throw new IllegalStateException("菜单锚点尚未附着到有效窗口");
		}
		if (anchor.getContext() != activity) {
			throw new IllegalArgumentException("菜单锚点不属于当前 Activity");
		}

		// 先关闭旧菜单，再建立本轮快照；关闭回调可能销毁组件。
		if (popup != null) {
			popup.dismiss();
		}
		requireAlive();
		final PopupMenu current = new PopupMenu(activity, anchor);
		if (menuRes.isEmpty()) {
			addItems(current.getMenu(), 0);
		} else {
			current.inflate(ResourcesUtil.getResourcesId(menuRes));
		}

		// 空菜单不建立没有可见项目的弹出窗口，避免拦截后续界面点击。
		if (current.getMenu().size() == 0) {
			return;
		}

		current.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(android.view.MenuItem item) {
				if (!destroyed && popup == current) {
					被选中(item.getItemId());
				}
				return true;
			}
		});
		current.setOnDismissListener(new PopupMenu.OnDismissListener() {

			@Override
			public void onDismiss(PopupMenu menu) {
				if (popup == current) {
					popup = null;
					if (!destroyed) {
						被关闭();
					}
				}
			}
		});
		popup = current;
		current.show();
	}

	@Override
	public void 销毁() {
		dispose(true);
	}

	/* 内部辅助方法 */

	/**
	 * 按父子关系递归生成菜单，排序由 Android 根据每项排序值处理。
	 *
	 * @param menu 接收菜单项的系统菜单。
	 * @param parent 父项标识，0 表示顶层。
	 */
	private void addItems(Menu menu, int parent) {
		for (PopupMenuModel.Item item : model.children(parent)) {
			if (model.children(item.id).isEmpty()) {
				menu.add(item.getGroup(), item.id, item.getOrder(), item.title);
			} else {
				addItems(menu.addSubMenu(item.getGroup(), item.id, item.getOrder(), item.title), item.id);
			}
		}
	}

	/**
	 * 拒绝销毁后的组件操作。
	 *
	 * @throws IllegalStateException 组件已销毁。
	 */
	private void requireAlive() {
		if (destroyed) {
			throw new IllegalStateException("弹出菜单组件已销毁");
		}
	}

	/**
	 * 停止菜单回调并释放菜单与项目数据，可重复调用。
	 *
	 * @param unregister 是否移除 Activity 监听；Activity 正在遍历时不移除。
	 */
	private void dispose(boolean unregister) {
		if (destroyed) {
			return;
		}
		destroyed = true;
		PopupMenu current = popup;
		popup = null;
		if (current != null) {
			current.setOnMenuItemClickListener(null);
			current.setOnDismissListener(null);
			current.dismiss();
		}
		model.clear();
		if (unregister) {
			activity.removeOnDestroyListener(destroyListener);
		}
		super.销毁();
	}
}

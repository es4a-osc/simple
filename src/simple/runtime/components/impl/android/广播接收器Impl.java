package simple.runtime.components.impl.android;

import simple.runtime.android.MainActivity;
import simple.runtime.collections.意图;
import simple.runtime.components.广播接收器;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;

import java.util.LinkedHashSet;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;

/**
 * 广播接收器的实现。
 *
 * @author 树先生 xhwsd@qq.com
 */
public class 广播接收器Impl extends 组件Impl implements 广播接收器 {

	private final BroadcastReceiver receiver;
	private final Set<String> actions = new LinkedHashSet<String>();
	private boolean registered;

	public 广播接收器Impl(组件容器 container) {
		super(container);

		// 创建接收器
		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				收到广播(intent.getAction(), new 意图(intent));
			}
		};
	}

	/* 广播接收器 实现 */

	@Override
	public void 销毁() {
		unregisterReceiver();
		actions.clear();

		super.销毁();
	}

	@Override
	public void 收到广播(String action, 意图 intent) {
		EventDispatcher.dispatchEvent(this, "收到广播", action, intent);
	}

	@Override
	public int 动作计数() {
		return actions.size();
	}

	@Override
	public String[] 所有动作() {
		return actions.toArray(new String[actions.size()]);
	}

	@Override
	public void 注册动作(String action) {
		if (action == null || action.isEmpty() || actions.contains(action)) {
			return;
		}

		unregisterReceiver();
		actions.add(action);
		registerReceiver();
	}

	@Override
	public void 注销动作(String action) {
		if (action == null || !actions.contains(action)) {
			return;
		}

		unregisterReceiver();
		actions.remove(action);
		registerReceiver();
	}

	@Override
	public boolean 具有动作(String action) {
		return action != null && actions.contains(action);
	}

	/* 内部辅助方法 */

	/**
	 * 注销接收器。
	 */
	private void unregisterReceiver() {
		if (registered) {
			MainActivity.getContext().unregisterReceiver(receiver);
			registered = false;
		}
	}

	/**
	 * 注册接收器。
	 */
	private void registerReceiver() {
		if (actions.isEmpty()) {
			return;
		}

		IntentFilter filter = new IntentFilter();
		for (String action : actions) {
			filter.addAction(action);
		}

		MainActivity.getContext().registerReceiver(receiver, filter);
		registered = true;
	}
}

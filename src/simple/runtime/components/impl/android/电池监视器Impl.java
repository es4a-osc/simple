package simple.runtime.components.impl.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import simple.runtime.android.MainActivity;
import simple.runtime.components.电池监视器;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.components.impl.android.util.BatterySnapshot;
import simple.runtime.events.EventDispatcher;

/**
 * 动态接收系统电池广播，维护一致快照并随组件或 Activity 销毁释放监听。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class 电池监视器Impl extends 组件Impl implements 电池监视器 {

	private final MainActivity activity;
	private final BroadcastReceiver receiver;
	private final MainActivity.OnDestroyListener destroyListener;
	private BatterySnapshot snapshot;
	private boolean enabled = true;
	private boolean initialized;
	private boolean registered;
	private boolean reported;
	private boolean destroyed;

	/**
	 * 取得当前粘性广播；事件监听延后到组件初始化，避免提前分派事件。
	 *
	 * @param container 容纳此组件的窗口。
	 */
	public 电池监视器Impl(组件容器 container) {
		super(container);
		activity = MainActivity.getContext();
		snapshot = decode(activity.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)));

		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (!destroyed && enabled && registered
						&& intent != null && Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
					updateSnapshot(intent);
				}
			}
		};
		destroyListener = new MainActivity.OnDestroyListener() {
			@Override
			public void onDestroy() {
				release(false);
			}
		};
		activity.addOnDestroyListener(destroyListener);
	}

	/* 组件事件 */

	@Override
	public void 初始化() {
		if (destroyed || initialized) {
			return;
		}

		initialized = true;
		super.初始化();
		if (enabled && !destroyed) {
			startListening();
		}
	}

	@Override
	public void 状态改变(int status, int level, double temperature) {
		if (!destroyed && enabled) {
			EventDispatcher.dispatchEvent(this, "状态改变", status, level, temperature);
		}
	}

	/* 组件属性 */

	@Override
	public boolean 是否可用() {
		return snapshot.available;
	}

	@Override
	public boolean 启用() {
		return enabled;
	}

	@Override
	public void 启用(boolean enabled) {
		if (destroyed) {
			return;
		}

		this.enabled = enabled;
		if (!enabled) {
			stopListening();
		} else if (initialized) {
			startListening();
		}
	}

	/* 组件方法 */

	@Override
	public int 取电池状态() {
		return snapshot.status;
	}

	@Override
	public int 取电池电量() {
		return snapshot.level;
	}

	@Override
	public double 取电池温度() {
		return snapshot.temperature;
	}

	@Override
	public double 取电池电压() {
		return snapshot.voltage;
	}

	@Override
	public String 取充电方式() {
		switch (snapshot.plugged) {
			case 0:
				return "未连接";
			case BatteryManager.BATTERY_PLUGGED_AC:
				return "交流电";
			case BatteryManager.BATTERY_PLUGGED_USB:
				return "USB";
			case BatteryManager.BATTERY_PLUGGED_WIRELESS:
				return "无线";
			default:
				return snapshot.plugged < 0 ? "未知" : "其他";
		}
	}

	@Override
	public String 取健康状态() {
		switch (snapshot.health) {
			case BatteryManager.BATTERY_HEALTH_GOOD:
				return "良好";
			case BatteryManager.BATTERY_HEALTH_OVERHEAT:
				return "过热";
			case BatteryManager.BATTERY_HEALTH_DEAD:
				return "失效";
			case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
				return "过压";
			case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
				return "故障";
			case BatteryManager.BATTERY_HEALTH_COLD:
				return "过冷";
			default:
				return "未知";
		}
	}

	@Override
	public void 销毁() {
		release(true);
	}

	/* 监听与资源释放 */

	/**
	 * 注册受系统保护的电池广播；当前结果立即可读，首次结果只报告一次。
	 */
	private void startListening() {
		if (registered) {
			return;
		}

		reported = false;
		Intent current = activity.registerReceiver(receiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		registered = true;
		if (current != null) {
			updateSnapshot(current);
		}
	}

	/**
	 * 停止广播接收并保留快照；已排队的回调由接收器状态检查丢弃。
	 */
	private void stopListening() {
		if (registered) {
			registered = false;
			activity.unregisterReceiver(receiver);
		}
	}

	/**
	 * 原子替换快照，事件处理程序读取到的是同一条广播的全部数据。
	 *
	 * @param intent 系统电池广播。
	 */
	private void updateSnapshot(Intent intent) {
		BatterySnapshot next = decode(intent);
		boolean changed = !reported || !next.sameValues(snapshot);
		snapshot = next;
		reported = true;

		if (changed) {
			状态改变(snapshot.status, snapshot.level, snapshot.temperature);
		}
	}

	/**
	 * 释放组件；Activity 正在遍历销毁监听器时不修改该列表。
	 *
	 * @param removeListener 是否从 Activity 注销销毁回调。
	 */
	private void release(boolean removeListener) {
		if (destroyed) {
			return;
		}

		destroyed = true;
		enabled = false;
		stopListening();
		if (removeListener) {
			activity.removeOnDestroyListener(destroyListener);
		}
		super.销毁();
	}

	/**
	 * 按系统声明的整数类型读取测量值，缺失数据保留明确的未知标记。
	 *
	 * @param intent 当前粘性广播，允许为空。
	 * @return 已统一单位的快照。
	 */
	private static BatterySnapshot decode(Intent intent) {
		if (intent == null) {
			return new BatterySnapshot(false, 状态_未知, -1, 0, Integer.MIN_VALUE, -1, -1,
				BatteryManager.BATTERY_HEALTH_UNKNOWN);
		}

		return new BatterySnapshot(intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false),
			intent.getIntExtra(BatteryManager.EXTRA_STATUS, 状态_未知),
			intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
			intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0),
			intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE),
			intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1),
			intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1),
			intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN));
	}
}

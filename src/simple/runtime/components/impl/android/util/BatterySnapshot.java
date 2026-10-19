package simple.runtime.components.impl.android.util;

/**
 * 同一条电池广播的不可变快照，集中处理量程和单位换算。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class BatterySnapshot {

	public final boolean available;
	public final int status;
	public final int level;
	public final double temperature;
	public final double voltage;
	public final int plugged;
	public final int health;

	/**
	 * 将系统整数测量值转换为公开读取结果，不依赖 Android 环境。
	 *
	 * @param present 系统是否报告存在电池。
	 * @param status 系统充电状态。
	 * @param level 当前电量刻度。
	 * @param scale 满电刻度。
	 * @param temperature 十分之一摄氏度，缺失时为整数最小值。
	 * @param voltage 毫伏，非正数表示未报告有效值。
	 * @param plugged 外部电源类型，负数表示未知。
	 * @param health 系统健康状态。
	 */
	public BatterySnapshot(boolean present, int status, int level, int scale,
			int temperature, int voltage, int plugged, int health) {
		available = present && scale > 0 && level >= 0 && level <= scale;
		this.status = status;
		this.level = available ? (int) ((long) level * 100 / scale) : -1;
		this.temperature = present && temperature != Integer.MIN_VALUE
			? temperature / 10.0 : Double.NaN;
		this.voltage = present && voltage > 0 ? voltage / 1000.0 : Double.NaN;
		this.plugged = plugged;
		this.health = health;
	}

	/**
	 * 比较全部公开数据，忽略不影响读取结果的原始刻度差异。
	 *
	 * @param previous 前一次快照。
	 * @return 公开结果完全相同时为真，两个未知浮点数也视为相同。
	 */
	public boolean sameValues(BatterySnapshot previous) {
		return previous != null && available == previous.available
			&& status == previous.status && level == previous.level
			&& Double.compare(temperature, previous.temperature) == 0
			&& Double.compare(voltage, previous.voltage) == 0
			&& plugged == previous.plugged && health == previous.health;
	}
}

package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleDataElement;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;

/**
 * 读取系统电池信息并监听变化的非可视组件，无需额外权限。
 *
 * <p>初始化后默认启用；停止监听时保留最后一次结果，重新启用时读取最新状态。
 * 信息来自系统电池广播，更新频率由系统决定，不代表持续后台运行。</p>
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
@SimpleComponent
public interface 电池监视器 extends 组件 {

	/* 电池状态常量 */

	/**
	 * 系统未报告明确的充电状态。
	 */
	@SimpleDataElement
	int 状态_未知 = 1;

	/**
	 * 电池正在充电。
	 */
	@SimpleDataElement
	int 状态_充电中 = 2;

	/**
	 * 电池正在放电。
	 */
	@SimpleDataElement
	int 状态_放电中 = 3;

	/**
	 * 电池当前未充电，可能仍连接外部电源。
	 */
	@SimpleDataElement
	int 状态_未充电 = 4;

	/**
	 * 电池已充满。
	 */
	@SimpleDataElement
	int 状态_已充满 = 5;

	/* 组件事件 */

	/**
	 * 启用后首次取得状态，或任一公开电池信息变化时触发。
	 *
	 * <p>事件发生前已更新全部读取结果；电压、充电方式和健康状态可通过对应方法读取。
	 * 停用或销毁后不再触发。</p>
	 *
	 * @param status 电池状态，使用本组件的状态常量。
	 * @param level 电量百分比，未知或无电池时为 -1。
	 * @param temperature 摄氏温度，系统未报告时为 NaN。
	 */
	@SimpleEvent
	void 状态改变(int status, int level, double temperature);

	/* 组件属性 */

	/**
	 * 判断是否已取得存在电池且电量量程有效的系统信息。
	 *
	 * @return 信息可用时返回真，不代表所有可选数据都有值。
	 */
	@SimpleProperty
	boolean 是否可用();

	/**
	 * 读取电池监听的启用状态。
	 *
	 * @return 当前启用状态；销毁后为假。
	 */
	@SimpleProperty
	boolean 启用();

	/**
	 * 启用或停止监听，重复设置不重复注册。
	 *
	 * <p>启用时读取当前系统状态并报告首次结果；停用后读取方法保留最后结果。
	 * 已销毁组件不能重新启用。</p>
	 *
	 * @param enabled 真为启用，假为停止监听。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "True"
	)
	void 启用(boolean enabled);

	/* 组件方法 */

	/**
	 * 读取最后取得的电池状态。
	 *
	 * @return 本组件的状态常量；未取得信息时为状态_未知。
	 */
	@SimpleFunction
	int 取电池状态();

	/**
	 * 读取按系统量程换算的电量百分比，小数部分舍去。
	 *
	 * @return 0 至 100；未知、无电池或量程无效时为 -1。
	 */
	@SimpleFunction
	int 取电池电量();

	/**
	 * 读取电池温度，单位为摄氏度，保留系统提供的一位小数。
	 *
	 * @return 摄氏温度；系统未报告时为 NaN，不用零度代替未知值。
	 */
	@SimpleFunction
	double 取电池温度();

	/**
	 * 读取电池电压，单位为伏。
	 *
	 * @return 毫伏换算后的电压；系统未报告有效电压时为 NaN。
	 */
	@SimpleFunction
	double 取电池电压();

	/**
	 * 读取外部电源类型，不以电池是否正在充电推断连接情况。
	 *
	 * @return 未连接、交流电、USB、无线、其他或未知。
	 */
	@SimpleFunction
	String 取充电方式();

	/**
	 * 读取系统报告的电池健康状态。
	 *
	 * @return 良好、过热、失效、过压、故障、过冷或未知。
	 */
	@SimpleFunction
	String 取健康状态();
}

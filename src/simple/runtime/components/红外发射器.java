package simple.runtime.components;

import simple.runtime.annotations.ManifestNodes;
import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.UsesPermissions;

/**
 * 使用设备内置红外发射器发送遥控信号的不可视组件。
 * Android 4.4（API 19）及以上系统且具有红外发射硬件时可用。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
@SimpleComponent
@UsesPermissions(
	permissionNames =
		// 允许应用使用设备内置的红外发射器。
		"android.permission.TRANSMIT_IR"
)
@ManifestNodes(
	rootXml =
		"<uses-feature" +
		" android:name=\"android.hardware.consumerir\"" +
		" android:required=\"false\"" +
		" />"
)
public interface 红外发射器 extends 组件 {

	/* 组件方法 */

	/**
	 * 判断当前设备是否具有可用的红外发射器。
	 *
	 * @return 系统和硬件均支持时返回真，否则返回假。
	 */
	@SimpleFunction
	boolean 是否支持红外();

	/**
	 * 获取设备支持的载波频率范围，频率单位为 Hz。
	 *
	 * @return 文本数组，每项格式为“最小频率-最大频率”，包含两个端点；
	 * 无红外发射器或系统未返回频率信息时返回空数组。
	 */
	@SimpleFunction
	String[] 取发射器支持频率();

	/**
	 * 同步发送红外脉冲，依次交替发射和停顿；方法返回时发送调用已结束。
	 * 当前设备不支持红外时不发射信号。
	 *
	 * @param frequency 载波频率，单位 Hz，须为正数并应位于设备支持的范围内。
	 * @param pattern 依次交替的发射、停顿时长，单位微秒；数组不能为空，
	 * 每项须为正数，总时长不得超过 2 秒。
	 * @throws IllegalArgumentException 频率或脉冲参数无效。
	 */
	@SimpleFunction
	void 发射信号(int frequency, int[] pattern);
}

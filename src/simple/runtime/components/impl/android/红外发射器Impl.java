package simple.runtime.components.impl.android;

import simple.runtime.android.MainActivity;
import simple.runtime.components.红外发射器;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.ConsumerIrManager;
import android.os.Build;

/**
 * Android 内置红外发射器实现，保留原类库的频率范围文本格式。
 *
 * @author 树先生 xhwsd@qq.com
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public final class 红外发射器Impl extends 组件Impl implements 红外发射器 {

	// Android 红外服务允许的单次脉冲总时长，单位微秒。
	private static final long MAX_PATTERN_DURATION = 2000000L;

	// 低版本系统或未提供红外服务时为空。
	private final ConsumerIrManager infraredManager;

	/**
	 * 创建不可视组件，并取得支持当前系统版本的红外服务。
	 *
	 * @param container 容纳组件的窗口。
	 */
	public 红外发射器Impl(组件容器 container) {
		super(container);

		// 仅在 API 19 及以上调用红外服务，避免旧系统进入不支持的 API。
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			infraredManager = (ConsumerIrManager) MainActivity.getContext()
				.getSystemService(Context.CONSUMER_IR_SERVICE);
		} else {
			infraredManager = null;
		}
	}

	/* 组件方法 */

	@Override
	public boolean 是否支持红外() {
		return infraredManager != null && infraredManager.hasIrEmitter();
	}

	@Override
	public String[] 取发射器支持频率() {
		if (!是否支持红外()) {
			return new String[0];
		}

		// 系统与红外服务通信失败时可以返回 null，统一转换为空数组。
		ConsumerIrManager.CarrierFrequencyRange[] ranges = infraredManager.getCarrierFrequencies();
		if (ranges == null || ranges.length == 0) {
			return new String[0];
		}

		// 保留设备返回顺序和原有“最小频率-最大频率”接口格式。
		String[] frequencies = new String[ranges.length];
		for (int index = 0; index < ranges.length; index++) {
			ConsumerIrManager.CarrierFrequencyRange range = ranges[index];
			frequencies[index] = range.getMinFrequency() + "-" + range.getMaxFrequency();
		}

		return frequencies;
	}

	@Override
	public void 发射信号(int frequency, int[] pattern) {
		if (frequency <= 0) {
			throw new IllegalArgumentException("红外载波频率必须为正数");
		}
		if (pattern == null || pattern.length == 0) {
			throw new IllegalArgumentException("红外脉冲数组不能为空");
		}

		// 校验与发送使用同一份快照，避免调用方修改数组绕过参数检查。
		int[] signal = pattern.clone();
		validatePattern(signal);

		if (!是否支持红外()) {
			return;
		}

		// Android 此调用同步执行，不创建后台线程或改变原接口的调用时序。
		infraredManager.transmit(frequency, signal);
	}

	/* 内部辅助方法 */

	/**
	 * 校验脉冲分段及总时长，使用长整数累加以避免整数溢出。
	 *
	 * @param pattern 已复制的脉冲数组。
	 * @throws IllegalArgumentException 存在非正时长或总时长超过 2 秒。
	 */
	private static void validatePattern(int[] pattern) {
		long totalDuration = 0;
		for (int duration : pattern) {
			if (duration <= 0) {
				throw new IllegalArgumentException("红外脉冲时长必须为正数");
			}

			totalDuration += duration;
			if (totalDuration > MAX_PATTERN_DURATION) {
				throw new IllegalArgumentException("红外脉冲总时长不能超过 2 秒");
			}
		}
	}
}

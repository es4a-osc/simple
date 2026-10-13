package simple.runtime.components.impl.android;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import simple.runtime.android.MainActivity;
import simple.runtime.components.组件容器;
import simple.runtime.components.距离传感器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;

/**
 * Android 距离传感器实现，负责监听启停、距离读数及组件销毁时的资源释放。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class 距离传感器Impl extends 组件Impl implements 距离传感器, SensorEventListener {

	private final SensorManager sensorManager;
	private final Sensor sensor;

	private int samplingPeriod = SensorManager.SENSOR_DELAY_NORMAL;
	private int minimumDistance = 1;
	private float distance;
	private boolean hasReading;
	private boolean listening;
	private boolean destroyed;

	public 距离传感器Impl(组件容器 container) {
		super(container);

		sensorManager = (SensorManager) MainActivity.getContext().getSystemService(Context.SENSOR_SERVICE);
		sensor = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

		// 保留原组件创建后即开始监听的行为，属性初始化再次启用时不会重复注册。
		可用(true);
	}

	/* 组件事件 */

	@Override
	public void 距离改变(float distance) {
		EventDispatcher.dispatchEvent(this, "距离改变", distance);
	}

	@Override
	public void 物体靠近() {
		EventDispatcher.dispatchEvent(this, "物体靠近");
	}

	@Override
	public void 物体离开() {
		EventDispatcher.dispatchEvent(this, "物体离开");
	}

	/* 组件属性 */

	@Override
	public float 距离() {
		return distance;
	}

	@Override
	public boolean 可用() {
		return listening;
	}

	@Override
	public void 可用(boolean enable) {
		if (destroyed || sensor == null || listening == enable) {
			return;
		}

		if (enable) {
			listening = sensorManager.registerListener(this, sensor, samplingPeriod);
		} else {
			// 先关闭回调入口，避免取消监听后已排队的读数继续触发事件。
			listening = false;
			sensorManager.unregisterListener(this);
		}
	}

	@Override
	public int 最小距离() {
		return minimumDistance;
	}

	@Override
	public void 最小距离(int distance) {
		if (distance < 0) {
			throw new IllegalArgumentException("最小距离不能小于零");
		}

		minimumDistance = distance;
	}

	@Override
	public int 检测间隔() {
		return samplingPeriod;
	}

	@Override
	public void 检测间隔(int interval) {
		if (interval < 0) {
			throw new IllegalArgumentException("检测间隔不能小于零");
		}

		if (samplingPeriod == interval) {
			return;
		}

		boolean restart = listening;
		可用(false);
		samplingPeriod = interval;

		if (restart) {
			可用(true);
		}
	}

	/* 组件方法 */

	@Override
	public void 销毁() {
		可用(false);
		destroyed = true;

		super.销毁();
	}

	/* 内部接口与平台回调 */

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (!listening || event.sensor.getType() != Sensor.TYPE_PROXIMITY || event.values.length == 0) {
			return;
		}

		float newDistance = event.values[0];
		if (Float.isNaN(newDistance) || Float.isInfinite(newDistance) || newDistance < 0) {
			return;
		}

		boolean changed = !hasReading || distance != newDistance;

		// 先更新状态，事件处理程序读取距离时才能取得本次读数；首个零读数也必须报告。
		distance = newDistance;
		hasReading = true;

		if (newDistance < minimumDistance) {
			物体靠近();
		} else {
			物体离开();
		}

		if (changed) {
			距离改变(newDistance);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// 距离传感器不使用精度变化通知。
	}
}

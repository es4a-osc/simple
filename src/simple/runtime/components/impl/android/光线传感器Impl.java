package simple.runtime.components.impl.android;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import simple.runtime.android.MainActivity;
import simple.runtime.components.光线传感器;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;

/**
 * Android 环境光传感器实现；设备无对应传感器时不注册监听。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class 光线传感器Impl extends 组件Impl implements 光线传感器, SensorEventListener {

	private final SensorManager sensorManager;
	private final Sensor sensor;
	private boolean listening;

	public 光线传感器Impl(组件容器 container) {
		super(container);

		sensorManager = (SensorManager) MainActivity.getContext().getSystemService(Context.SENSOR_SERVICE);
		sensor = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

		// 硬件存在才注册；注册失败或销毁后不再分发传感器回调。
		if (sensor != null) {
			listening = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
		}
	}

	/* 组件事件 */

	@Override
	public void 光线改变(float light) {
		EventDispatcher.dispatchEvent(this, "光线改变", light);
	}

	/* 组件属性 */

	@Override
	public boolean 是否有效() {
		return sensor != null;
	}

	/* 组件方法 */

	@Override
	public void 销毁() {
		if (listening) {
			listening = false;
			sensorManager.unregisterListener(this);
		}

		super.销毁();
	}

	/* 内部接口与平台回调 */

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (listening && event.sensor.getType() == Sensor.TYPE_LIGHT && event.values.length > 0) {
			float light = event.values[0];
			if (!Float.isNaN(light) && !Float.isInfinite(light) && light >= 0) {
				光线改变(light);
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// 环境照度只使用数值读数。
	}
}

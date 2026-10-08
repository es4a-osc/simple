/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simple.runtime.components.impl.android;

import simple.runtime.android.MainActivity;
import simple.runtime.components.方向传感器;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;

import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Sensor that can measure absolute orientation in 3 dimensions.
 *
 * @author Herbert Czymontek
 * @author Ellen Spertus
 */
public final class 方向传感器Impl extends 组件Impl implements 方向传感器, SensorEventListener {

	private final SensorManager sensors;

	// 支持的属性
	private boolean enabled;
	private float yaw;
	private float pitch;
	private float roll;

	/**
	 * 创建一个新的方向传感器组件。
	 *
	 * @param container  包含组件的容器（对于不可见的组件不可以为{@code null}，类似容器必须是窗口）
	 */
	public 方向传感器Impl(组件容器 container) {
		super(container);

		sensors = (SensorManager) MainActivity.getContext().getSystemService(Context.SENSOR_SERVICE);
		sensors.registerListener(this, sensors.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_GAME);
	}

	/* OrientationSensor 实现 */

	@Override
	public void 方向改变(float newYaw, float newPitch, float newRoll) {
		EventDispatcher.dispatchEvent(this, "方向改变", newYaw, newPitch, newRoll);
	}

	@Override
	public boolean 可用() {
		List<Sensor> sensorList = sensors.getSensorList(Sensor.TYPE_ORIENTATION);
		return sensorList != null && !sensorList.isEmpty();
	}

	@Override
	public boolean 启用() {
		return enabled;
	}

	@Override
	public void 启用(boolean enable) {
		enabled = enable;
	}

	@Override
	public float 俯仰角() {
		return pitch;
	}

	@Override
	public float 翻滚角() {
		return roll;
	}

	@Override
	public float 偏航角() {
		return yaw;
	}

	@Override
	public float 倾斜角() {
		return (float) Math.toDegrees(Math.atan2(pitch, roll));
	}

	@Override
	public float Magnitude() {
		// Limit pitch and roll to 90; otherwise, the phone is upside down.
		// If the device is upside-down, it can range from -180 to 180.
		// We restrict it to the range [-90, 90].
		final int MAX_VALUE = 90;

		return (float) (Math.sqrt(Math.pow(Math.min(MAX_VALUE, pitch), 2) +
				Math.pow(Math.min(MAX_VALUE, roll), 2)) / (MAX_VALUE * Math.sqrt(2)));
	}

	// SensorEventListener 实现

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ORIENTATION && enabled) {
			yaw = event.values[0];
			pitch = event.values[1];
			roll = event.values[2];
			方向改变(yaw, pitch, roll);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}
}

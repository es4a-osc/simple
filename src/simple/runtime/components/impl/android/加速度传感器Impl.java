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

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import simple.runtime.android.MainActivity;
import simple.runtime.components.加速度传感器;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 用于测量加速度3个维度的传感器。
 *
 * @author Herbert Czymontek
 */
public final class 加速度传感器Impl extends 组件Impl
		implements 加速度传感器, SensorEventListener {

	// Skake threshold - derived by trial
	private final static double SHAKE_THRESHOLD = 8.0;

	// Cache for shake detection
	private final static int SENSOR_CACHE_SIZE = 10;
	private final Queue<Float> X_CACHE;
	private final Queue<Float> Y_CACHE;
	private final Queue<Float> Z_CACHE;

	// Backing for sensor values
	private float xAccel;
	private float yAccel;
	private float zAccel;

	// Accelerometer sensor
	private final Sensor sensor;

	// Indicates whether the timer is running or not
	private boolean enabled;

	/**
	 * Creates a new OrientationSensor component.
	 *
	 * @param container  container which will hold the component (must not be
	 *                   {@code null}, for non-visible component, like this one
	 *                   must be the form)
	 */
	public 加速度传感器Impl(组件容器 container) {
		super(container);

		X_CACHE = new LinkedList<Float>();
		Y_CACHE = new LinkedList<Float>();
		Z_CACHE = new LinkedList<Float>();

		SensorManager sensors =
				(SensorManager) MainActivity.getContext().getSystemService(Context.SENSOR_SERVICE);
		sensor = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		if (sensor != null) {
			sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);
		}
	}

	// AccelerometerSensor implementation

	/**
	 * {@inheritDoc}
	 *
	 * <p>From the Android documentation:
	 * <p>"All values are in SI units (m/s^2) and measure the acceleration applied
	 * to the phone minus the force of gravity.
	 * <br>values[0]: Acceleration minus Gx on the x-axis
	 * <br>values[1]: Acceleration minus Gy on the y-axis
	 * <br>values[2]: Acceleration minus Gz on the z-axis "
	 */
	@Override
	public void 加速度改变(float xAccelNew, float yAccelNew, float zAccelNew) {
		xAccel = xAccelNew;
		yAccel = yAccelNew;
		zAccel = zAccelNew;

		addToSensorCache(X_CACHE, xAccelNew);
		addToSensorCache(Y_CACHE, yAccelNew);
		addToSensorCache(Z_CACHE, zAccelNew);

		if (isShaking(X_CACHE, xAccelNew) ||
				isShaking(Y_CACHE, yAccelNew) ||
				isShaking(Z_CACHE, zAccelNew)) {
			摇晃();
		}

		EventDispatcher.dispatchEvent(this, "加速度改变", xAccelNew, yAccelNew, zAccelNew);
	}

	@Override
	public void 摇晃() {
		EventDispatcher.dispatchEvent(this, "摇晃");
	}

	@Override
	public boolean 可用() {
		return sensor != null;
//    List<Sensor> sensorList = sensors.getSensorList(Sensor.TYPE_ACCELEROMETER);
//    return sensorList != null && !sensorList.isEmpty();
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
	public float X加速度() {
		return xAccel;
	}

	@Override
	public float Y加速度() {
		return yAccel;
	}

	@Override
	public float Z加速度() {
		return zAccel;
	}

	/*
	 * Updating sensor cache, replacing oldest values.
	 */
	private void addToSensorCache(Queue<Float> cache, float value) {
		if (cache.size() >= SENSOR_CACHE_SIZE) {
			cache.remove();
		}
		cache.add(value);
	}

	/*
	 * Indicates whether there was a sudden, unusual movement.
	 */
	// TODO: maybe this can be improved
	//       see http://www.utdallas.edu/~rxb023100/pubs/Accelerometer_WBSN.pdf
	private boolean isShaking(Queue<Float> cache, float currentValue) {
		float average = 0;
		for (float value : cache) {
			average += value;
		}

		average /= cache.size();

		return Math.abs(average - currentValue) > SHAKE_THRESHOLD;
	}

	// SensorEventListener implementation

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && enabled) {
			xAccel = event.values[0];
			yAccel = event.values[1];
			zAccel = event.values[2];
			加速度改变(xAccel, yAccel, zAccel);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor s, int accuracy) {
	}
}

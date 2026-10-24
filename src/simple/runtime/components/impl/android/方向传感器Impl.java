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

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import simple.runtime.android.MainActivity;
import simple.runtime.components.方向传感器;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;

/**
 * 在前台选择可用的绝对方向来源，统一角度约定并管理监听生命周期。
 *
 * <p>优先使用磁北旋转矢量，其次使用加速度与地磁合成；旧方向传感器仅作为能力回退。
 * 普通旋转矢量优先提供陀螺仪融合，地磁旋转矢量作为另一种绝对方向来源；不使用缺少磁北参考的游戏旋转矢量。
 * 每次注册使用独立监听对象，停用或重启后丢弃旧监听的迟到数据。
 *
 * @author Herbert Czymontek
 * @author Ellen Spertus
 * @author 树先生 xhwsd@qq.com
 */
public final class 方向传感器Impl extends 组件Impl implements 方向传感器,
		MainActivity.OnResumeListener, MainActivity.OnPauseListener, MainActivity.OnDestroyListener {

	private static final int MODE_VECTOR = 1;
	private static final int MODE_COMBINED = 2;
	private static final int MODE_LEGACY = 3;
	private final MainActivity activity;
	private final SensorManager sensors;
	private final Handler handler;
	private DirectionListener listener;
	private boolean enabled = true;
	private boolean attached;
	private boolean refreshQueued;
	private boolean destroyed;
	private boolean hasDirection;
	private float yaw;
	private float pitch;
	private float roll;
	private String lastError = "";
	private String reportedState = "";

	/**
	 * 保存宿主和传感器服务，延后登记监听，避免构造时触发事件或修改宿主监听集合。
	 *
	 * @param container 所属组件容器
	 */
	public 方向传感器Impl(组件容器 container) {
		super(container);
		activity = MainActivity.getContext();
		sensors = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
		handler = new Handler(Looper.getMainLooper());
		queueRefresh();
	}

	/* 组件事件 */

	@Override
	public void 方向改变(float newYaw, float newPitch, float newRoll) {
		EventDispatcher.dispatchEvent(this, "方向改变", newYaw, newPitch, newRoll);
	}

	@Override
	public void 监听状态改变(boolean listening, String message) {
		EventDispatcher.dispatchEvent(this, "监听状态改变", listening, message);
	}

	/* 组件属性 */

	@Override
	public boolean 可用() {
		requireMainThread();
		if (destroyed || sensors == null || hostDestroyed()) {
			return false;
		}
		try {
			return (Build.VERSION.SDK_INT >= 9 && sensor(Sensor.TYPE_ROTATION_VECTOR) != null)
				|| (Build.VERSION.SDK_INT >= 19 && sensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) != null)
				|| (sensor(Sensor.TYPE_ACCELEROMETER) != null && sensor(Sensor.TYPE_MAGNETIC_FIELD) != null)
				|| legacySensor(sensors) != null;
		} catch (RuntimeException failure) {
			return false;
		}
	}

	@Override
	public boolean 启用() {
		requireMainThread();
		return enabled && !destroyed;
	}

	@Override
	public void 启用(boolean value) {
		requireMainThread();
		if (destroyed) {
			return;
		}
		enabled = value;
		if (!value) {
			stopListening();
		}
		queueRefresh();
	}

	@Override
	public boolean 正在监听() {
		requireMainThread();
		return !destroyed && enabled && listener != null && activity.isInForeground()
			&& !activity.isFinishing() && !hostDestroyed();
	}

	@Override
	public boolean 已有方向() {
		requireMainThread();
		return !destroyed && hasDirection;
	}

	@Override
	public String 错误信息() {
		requireMainThread();
		return lastError;
	}

	@Override
	public float 俯仰角() {
		requireMainThread();
		return pitch;
	}

	@Override
	public float 翻滚角() {
		requireMainThread();
		return roll;
	}

	@Override
	public float 偏航角() {
		requireMainThread();
		return yaw;
	}

	@Override
	public float 倾斜角() {
		requireMainThread();
		return (float) Math.toDegrees(Math.atan2(pitch, roll));
	}

	@Override
	public float Magnitude() {
		requireMainThread();
		return tiltMagnitude(pitch, roll);
	}

	@Override
	public void 销毁() {
		requireMainThread();
		if (destroyed) {
			return;
		}
		destroyed = true;
		enabled = false;
		handler.removeCallbacksAndMessages(null);
		stopListening();
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (attached) {
					activity.removeOnResumeListener(方向传感器Impl.this);
					activity.removeOnPauseListener(方向传感器Impl.this);
					activity.removeOnDestroyListener(方向传感器Impl.this);
					attached = false;
				}
			}
		});
		super.销毁();
	}

	/* 宿主回调 */

	@Override
	public void onResume() {
		queueRefresh();
	}

	@Override
	public void onPause() {
		stopListening();
		publishState("应用已离开前台");
	}

	@Override
	public void onDestroy() {
		销毁();
	}

	/* 内部辅助方法 */

	/**
	 * 合并排队的能力检查，属性初始化和事件重入期间不直接登记宿主监听。
	 */
	private void queueRefresh() {
		if (!destroyed && !refreshQueued) {
			refreshQueued = true;
			handler.post(new Runnable() {
				@Override
				public void run() {
					refreshQueued = false;
					refresh();
				}
			});
		}
	}

	/**
	 * 根据宿主状态、系统版本和实际注册结果选择可用的方向来源。
	 */
	private void refresh() {
		if (destroyed) {
			return;
		}
		if (hostDestroyed()) {
			销毁();
			return;
		}
		if (!attached) {
			activity.addOnResumeListener(this);
			activity.addOnPauseListener(this);
			activity.addOnDestroyListener(this);
			attached = true;
		}
		if (!enabled || !activity.isInForeground() || activity.isFinishing()) {
			stopListening();
			publishState(!enabled ? "已停用" : "应用未在前台或宿主正在关闭");
			return;
		}
		if (listener != null) {
			return;
		}
		if (sensors == null) {
			lastError = "系统没有提供传感器服务。";
			publishState(lastError);
			return;
		}

		lastError = "";
		boolean available = false;
		try {
			if (Build.VERSION.SDK_INT >= 9) {
				Sensor vector = sensor(Sensor.TYPE_ROTATION_VECTOR);
				available |= vector != null;
				if (startListening(vector, null, MODE_VECTOR)) {
					return;
				}
			}
			if (Build.VERSION.SDK_INT >= 19) {
				Sensor vector = sensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
				available |= vector != null;
				if (startListening(vector, null, MODE_VECTOR)) {
					return;
				}
			}
			Sensor accelerometer = sensor(Sensor.TYPE_ACCELEROMETER);
			Sensor magnetic = sensor(Sensor.TYPE_MAGNETIC_FIELD);
			available |= accelerometer != null && magnetic != null;
			if (accelerometer != null && magnetic != null
					&& startListening(accelerometer, magnetic, MODE_COMBINED)) {
				return;
			}

			// 缺少新来源或注册失败时，仍给只提供旧方向传感器的系统保留实际可用路径。
			Sensor legacy = legacySensor(sensors);
			available |= legacy != null;
			if (startListening(legacy, null, MODE_LEGACY)) {
				return;
			}
		} catch (RuntimeException failure) {
			lastError = errorMessage(failure);
		}
		if (lastError.length() == 0) {
			lastError = available ? "方向传感器监听注册失败。" : "设备没有可用的磁北方向传感器。";
		}
		publishState(lastError);
	}

	/**
	 * 读取指定的非弃用传感器类型。
	 *
	 * @param type 系统支持的传感器类型
	 * @return 默认传感器，不存在时为空
	 */
	private Sensor sensor(int type) {
		return sensors.getDefaultSensor(type);
	}

	/**
	 * 完整注册一个来源；组合来源任一注册失败时立即清除已登记部分。
	 *
	 * @param first 主传感器，缺失时不尝试
	 * @param second 合成方向需要的地磁传感器，其他模式为空
	 * @param mode 计算模式
	 * @return 全部注册成功时为真
	 */
	private boolean startListening(Sensor first, Sensor second, int mode) {
		if (first == null) {
			return false;
		}
		DirectionListener candidate = new DirectionListener(first, second, mode);
		listener = candidate;
		try {
			if (sensors.registerListener(candidate, first, SensorManager.SENSOR_DELAY_GAME, handler)
					&& (second == null || sensors.registerListener(candidate, second, SensorManager.SENSOR_DELAY_GAME, handler))) {
				lastError = "";
				publishState("正在等待有效方向");
				return true;
			}
		} catch (RuntimeException failure) {
			lastError = errorMessage(failure);
		}
		stopListening();
		return false;
	}

	/**
	 * 先使旧监听失效，再注销系统订阅，避免排队回调更新当前结果。
	 */
	private void stopListening() {
		DirectionListener previous = listener;
		listener = null;
		if (sensors != null && previous != null) {
			try {
				sensors.unregisterListener(previous);
			} catch (RuntimeException failure) {
				Log.w("方向传感器", "注销方向监听失败。", failure);
			}
		}
	}

	/**
	 * 接收已校验的角度，在通知之前更新全部属性，并防止状态事件重入后派发旧方向。
	 *
	 * @param source 结果所属的监听
	 * @param angles 磁北方位、俯仰和翻滚角，单位度
	 */
	private void accept(DirectionListener source, float[] angles) {
		if (!active(source)) {
			return;
		}
		yaw = normalizeYaw(angles[0]);
		pitch = angles[1];
		roll = angles[2];
		hasDirection = true;
		lastError = "";
		publishState("正在接收有效方向");
		if (active(source)) {
			方向改变(yaw, pitch, roll);
		}
	}

	/**
	 * 保留最近有效结果，向调用者说明当前样本不能用于确定方向。
	 *
	 * @param source 当前监听
	 */
	private void invalidData(DirectionListener source) {
		if (active(source)) {
			lastError = "当前传感器数据无法确定有效方向，等待后续数据。";
			publishState(lastError);
		}
	}

	/**
	 * 判断回调是否仍属于当前前台监听。
	 *
	 * @param source 回调所属监听
	 * @return 可以继续处理时为真
	 */
	private boolean active(DirectionListener source) {
		return !destroyed && enabled && listener == source && activity.isInForeground()
			&& !activity.isFinishing() && !hostDestroyed();
	}

	/**
	 * 只通知变化后的监听和数据状态，防止无效样本造成重复事件。
	 *
	 * @param message 状态或错误说明
	 */
	private void publishState(String message) {
		if (destroyed) {
			return;
		}
		boolean listening = 正在监听();
		String state = listening + ":" + message;
		if (!state.equals(reportedState)) {
			reportedState = state;
			监听状态改变(listening, message);
		}
	}

	/**
	 * 检查宿主是否已经销毁，较低版本通过宿主销毁回调处理。
	 *
	 * @return 宿主已销毁时为真
	 */
	private boolean hostDestroyed() {
		return Build.VERSION.SDK_INT >= 17 && activity.isDestroyed();
	}

	/**
	 * 只提供旧方向传感器时仍需使用该接口；此类型从 API 15 起弃用。
	 *
	 * @param manager 系统传感器服务
	 * @return 旧方向传感器，不存在时为空
	 */
	@SuppressWarnings("deprecation")
	private static Sensor legacySensor(SensorManager manager) {
		return manager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
	}

	/**
	 * 从设备到世界的旋转矩阵提取既定角度，保持俯仰 [-180,180]、翻滚 [-90,90]。
	 *
	 * <p>getOrientation 的俯仰与翻滚分解范围、翻滚正方向不同，不能直接把三项结果改成度数后返回。
	 * 方位仍取设备 Y 轴相对磁北的方向，坐标不随显示方向重映射。
	 *
	 * @param matrix 九元素行优先旋转矩阵
	 * @param angles 三元素输出数组，单位度
	 * @return 矩阵有效时为真
	 */
	private static boolean anglesFromMatrix(float[] matrix, float[] angles) {
		for (float value : matrix) {
			if (!finite(value)) {
				return false;
			}
		}
		angles[0] = normalizeYaw((float) Math.toDegrees(Math.atan2(matrix[1], matrix[4])));
		angles[1] = (float) Math.toDegrees(Math.atan2(-matrix[7], matrix[8]));
		angles[2] = (float) Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, matrix[6]))));
		return true;
	}

	/**
	 * 将方位归一化到 [0,360)。
	 *
	 * @param value 有限角度，单位度
	 * @return 归一化的方位角
	 */
	private static float normalizeYaw(float value) {
		float normalized = value % 360;
		if (normalized < 0) {
			normalized += 360;
		}
		return normalized >= 360 || normalized == 0 ? 0 : normalized;
	}

	/**
	 * 对正负俯仰和翻滚一视同仁地计算倾斜程度。
	 *
	 * @param pitch 俯仰角，单位度
	 * @param roll 翻滚角，单位度
	 * @return 范围 [0,1] 的归一化长度
	 */
	private static float tiltMagnitude(float pitch, float roll) {
		double x = Math.min(90, Math.abs(pitch)) / 90.0;
		double y = Math.min(90, Math.abs(roll)) / 90.0;
		return (float) Math.min(1, Math.sqrt((x * x + y * y) / 2));
	}

	/**
	 * 检查数值是否有限，不引入低版本没有的 isFinite 接口。
	 *
	 * @param value 待检查值
	 * @return 不是 NaN 或无穷值时为真
	 */
	private static boolean finite(float value) {
		return !Float.isNaN(value) && !Float.isInfinite(value);
	}

	/**
	 * 为没有说明的系统异常补充可读文本。
	 *
	 * @param failure 系统异常
	 * @return 异常说明
	 */
	private static String errorMessage(RuntimeException failure) {
		String message = failure.getMessage();
		return message == null || message.length() == 0 ? failure.getClass().getSimpleName() : message;
	}

	/**
	 * 拒绝后台线程直接操作组件状态。
	 *
	 * @throws IllegalStateException 当前线程不是主线程
	 */
	private static void requireMainThread() {
		if (Looper.myLooper() != Looper.getMainLooper()) {
			throw new IllegalStateException("方向传感器必须在主线程调用。");
		}
	}

	/**
	 * 保存一轮注册的传感器与合成缓冲，隔离停用前的数据及系统复用的事件数组。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	private final class DirectionListener implements SensorEventListener {

		private final Sensor first;
		private final Sensor second;
		private final int mode;
		private final float[] acceleration = new float[3];
		private final float[] magnetic = new float[3];
		private final float[] quaternion = new float[4];
		private final float[] matrix = new float[9];
		private final float[] angles = new float[3];
		private boolean hasAcceleration;
		private boolean hasMagnetic;

		/**
		 * 创建本轮独立的监听与计算状态。
		 *
		 * @param first 主传感器
		 * @param second 合成模式的地磁传感器，其他模式为空
		 * @param mode 计算模式
		 */
		DirectionListener(Sensor first, Sensor second, int mode) {
			this.first = first;
			this.second = second;
			this.mode = mode;
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (!active(this) || (event.sensor != first && event.sensor != second)) {
				return;
			}
			float[] values = event.values;
			if (values.length < 3 || !finite(values[0]) || !finite(values[1]) || !finite(values[2])) {
				if (mode == MODE_COMBINED) {
					if (event.sensor == first) {
						hasAcceleration = false;
					} else {
						hasMagnetic = false;
					}
				}
				invalidData(this);
				return;
			}

			if (mode == MODE_LEGACY) {
				if (Math.abs(values[1]) > 180 || Math.abs(values[2]) > 90) {
					invalidData(this);
					return;
				}
				System.arraycopy(values, 0, angles, 0, 3);
				accept(this, angles);
				return;
			}
			if (mode == MODE_VECTOR) {
				if (!prepareQuaternion(values)) {
					invalidData(this);
					return;
				}
				Api9.rotationMatrix(matrix, quaternion);
			} else {
				if (event.sensor == first) {
					System.arraycopy(values, 0, acceleration, 0, 3);
					hasAcceleration = true;
				} else {
					System.arraycopy(values, 0, magnetic, 0, 3);
					hasMagnetic = true;
				}
				if (!hasAcceleration || !hasMagnetic) {
					return;
				}
				if (!SensorManager.getRotationMatrix(matrix, null, acceleration, magnetic)) {
					invalidData(this);
					return;
				}
			}
			if (anglesFromMatrix(matrix, angles)) {
				accept(this, angles);
			} else {
				invalidData(this);
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// 数据仍按有限值与矩阵有效性校验，不将精度状态当作实际测得的方向。
		}

		/**
		 * 复制并归一化旋转四元数，兼容早期仅有三个分量的系统数据。
		 *
		 * @param values 系统旋转矢量，前三项已经过有限值检查
		 * @return 四元数有效时为真
		 */
		private boolean prepareQuaternion(float[] values) {
			double sum = values[0] * (double) values[0] + values[1] * (double) values[1]
				+ values[2] * (double) values[2];
			float w;
			if (values.length >= 4) {
				w = values[3];
				if (!finite(w)) {
					return false;
				}
			} else {
				if (sum > 1.000001) {
					return false;
				}
				w = (float) Math.sqrt(Math.max(0, 1 - sum));
			}
			double norm = Math.sqrt(sum + w * (double) w);
			if (norm < 0.000001) {
				return false;
			}
			quaternion[0] = (float) (values[0] / norm);
			quaternion[1] = (float) (values[1] / norm);
			quaternion[2] = (float) (values[2] / norm);
			quaternion[3] = (float) (w / norm);
			return true;
		}
	}

	/**
	 * 隔离 API 9 起提供的旋转矢量转矩阵入口，低版本使用实际可用的合成或旧传感器路径。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	@TargetApi(9)
	private static final class Api9 {

		/**
		 * 将已经归一化的旋转四元数转换为旋转矩阵。
		 *
		 * @param matrix 九元素输出矩阵
		 * @param quaternion 四元素单位四元数
		 */
		static void rotationMatrix(float[] matrix, float[] quaternion) {
			SensorManager.getRotationMatrixFromVector(matrix, quaternion);
		}
	}
}

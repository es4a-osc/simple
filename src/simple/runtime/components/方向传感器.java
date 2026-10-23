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

package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;

/**
 * 在应用前台测量设备相对磁北的方向与倾斜，不需要运行时权限。
 *
 * <p>需要可提供磁北参考的传感器或传感器组合，操作须在主线程进行。
 * 设备自然方向下，X 轴向右、Y 轴向上、Z 轴朝屏幕外；
 * 坐标轴不随应用横竖屏切换。磁场干扰、快速运动和接近竖直的姿态可能降低方向稳定性。
 * 停用或退出前台后停止监听，保留最近有效结果；宿主销毁时自动释放。
 *
 * @author Herbert Czymontek
 * @author Ellen Spertus
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 方向传感器 extends 传感器组件 {

	/* 组件事件 */

	/**
	 * 收到有效方向后触发，相关属性在事件前更新。
	 *
	 * @param yaw 磁北方位角，单位度，范围 [0, 360)；北为 0，东为 90，南为 180，西为 270
	 * @param pitch 绕 X 轴的俯仰角，单位度，范围 [-180, 180]；Z 轴向 Y 轴倾斜时为正
	 * @param roll 绕 Y 轴的翻滚角，单位度，范围 [-90, 90]；X 轴向 Z 轴倾斜时为正
	 */
	@SimpleEvent
	void 方向改变(float yaw, float pitch, float roll);

	/**
	 * 监听开始、停止或数据有效状态改变时触发，不重复通知相同状态。
	 *
	 * @param listening 是否已经注册方向监听，不表示已经取得有效方向
	 * @param message 当前状态或失败原因
	 */
	@SimpleEvent
	void 监听状态改变(boolean listening, String message);

	/* 组件属性 */

	/**
	 * 检查设备是否提供可以测量磁北方向的传感器组合。
	 *
	 * @return 组件未销毁且具备所需硬件或系统服务时为真，不保证注册成功或当前数据有效
	 */
	@SimpleProperty
	boolean 可用();

	/**
	 * 读取请求的启用状态。
	 *
	 * @return 希望在应用前台监听方向时为真，不表示监听成功或已经取得有效方向
	 */
	@SimpleProperty
	boolean 启用();

	/**
	 * 设置是否在应用前台监听方向，默认启用。
	 *
	 * <p>停用会立即注销；重新启用或返回前台时重新检查设备能力。失败见错误信息和监听状态改变。
	 *
	 * @param enabled 真为启用，假为停用
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "True"
	)
	void 启用(boolean enabled);

	/**
	 * 判断是否已经注册且仍在前台接收传感器数据。
	 *
	 * @return 正在等待或接收方向时为真，不表示当前数据有效
	 */
	@SimpleProperty
	boolean 正在监听();

	/**
	 * 判断是否已经取得过有效方向。
	 *
	 * @return 有最近有效结果时为真；停用后仍保留，销毁后为假
	 */
	@SimpleProperty
	boolean 已有方向();

	/**
	 * 读取最近一次注册失败或无效数据的说明。
	 *
	 * @return 错误说明；重新注册成功或收到有效数据后清空，能力查询不修改此值
	 */
	@SimpleProperty
	String 错误信息();

	/**
	 * 返回最近一次有效俯仰角，停用后保留。
	 *
	 * @return 单位度，范围 [-180, 180]；尚无有效方向时为 0，应检查已有方向
	 */
	@SimpleProperty
	float 俯仰角();

	/**
	 * 返回最近一次有效翻滚角，停用后保留。
	 *
	 * @return 单位度，范围 [-90, 90]；尚无有效方向时为 0，应检查已有方向
	 */
	@SimpleProperty
	float 翻滚角();

	/**
	 * 返回最近一次有效磁北方位角，不是真北方向。
	 *
	 * @return 单位度，范围 [0, 360)；尚无有效方向时为 0，应检查已有方向
	 */
	@SimpleProperty
	float 偏航角();

	/**
	 * 以翻滚角为横坐标、俯仰角为纵坐标，返回倾斜方向。
	 *
	 * @return 单位度，范围 [-180, 180]；水平时为 0，应检查已有方向
	 */
	@SimpleProperty
	float 倾斜角();

	/**
	 * 返回按俯仰角和翻滚角计算的归一化倾斜程度，不表示传感器精度。
	 *
	 * <p>两个角度的绝对值各限制到 90 度，再计算归一化长度。
	 *
	 * @return 范围 [0, 1]；尚无有效方向时为 0，应检查已有方向
	 */
	@SimpleProperty
	float Magnitude();
}

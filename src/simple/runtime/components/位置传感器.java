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
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.UsesPermissions;

/**
 * 在应用前台接收位置，并异步查询位置对应的地址。
 *
 * <p>需要精确位置或大致位置权限以及已开启的定位服务；仅有大致位置权限时使用网络定位。
 * 按 60 秒、5 米作为更新请求条件，实际更新频率由系统决定。
 * 停用或退出前台后停止定位，最近一次结果仍保留；操作须在主线程调用。
 *
 * @author Ellen Spertus
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
@SimpleComponent
@UsesPermissions(permissionNames =
	// 允许获取精确位置。
	"android.permission.ACCESS_FINE_LOCATION," +
	// 允许获取大致位置。
	"android.permission.ACCESS_COARSE_LOCATION"
)
public interface 位置传感器 extends 传感器组件 {

	/* 组件事件 */

	/**
	 * 收到新的有效位置后触发，相关属性在事件前更新。
	 *
	 * @param latitude 纬度，范围为 -90～90 度
	 * @param longitude 经度，范围为 -180～180 度
	 * @param altitude 海拔，单位米；具备海拔为假时为 0
	 */
	@SimpleEvent
	void 位置改变(double latitude, double longitude, double altitude);

	/**
	 * 定位监听开始、停止或提供者可用状态改变时触发。
	 *
	 * @param listening 是否正在等待定位结果，不表示已经取得位置
	 * @param message 当前状态说明
	 */
	@SimpleEvent
	void 定位状态改变(boolean listening, String message);

	/**
	 * 请求的地址查询完成后触发；坐标对应发起查询时的位置。
	 *
	 * @param latitude 查询时的纬度
	 * @param longitude 查询时的经度
	 * @param address 地址文本；系统没有匹配地址时为空
	 */
	@SimpleEvent
	void 地址查询完毕(double latitude, double longitude, String address);

	/**
	 * 定位注册、权限检查或已受理的地址查询失败时触发。
	 *
	 * @param operation 定位或查询地址
	 * @param message 失败原因
	 */
	@SimpleEvent
	void 操作失败(String operation, String message);

	/* 组件属性 */

	/**
	 * 判断本实例是否已收到有效经纬度。
	 *
	 * @return 已有位置结果时为真；停用后仍保留，不表示当前正在定位
	 */
	@SimpleProperty
	boolean 是否可用();

	/**
	 * 判断最近一次位置是否附带海拔。
	 *
	 * @return 最近一次结果有海拔时为真
	 */
	@SimpleProperty
	boolean 具备海拔();

	/**
	 * 判断最近一次位置是否附带水平精度。
	 *
	 * @return 最近一次结果有水平精度时为真，精度为 0 也可能是有效值
	 */
	@SimpleProperty
	boolean 具备精度();

	/**
	 * 读取最近一次位置的经度。
	 *
	 * @return 经度，单位度；尚无位置时为 0，是否有效应检查是否可用
	 */
	@SimpleProperty
	double 经度();

	/**
	 * 读取最近一次位置的纬度。
	 *
	 * @return 纬度，单位度；尚无位置时为 0，是否有效应检查是否可用
	 */
	@SimpleProperty
	double 纬度();

	/**
	 * 读取最近一次位置的海拔，不沿用更早结果的海拔。
	 *
	 * @return 海拔，单位米；尚无海拔时为 0，是否有效应检查具备海拔
	 */
	@SimpleProperty
	double 海拔();

	/**
	 * 读取最近一次位置的水平精度，不以提供者的精度等级代替实际测量。
	 *
	 * @return 估计误差半径，单位米；尚无精度时为 0，是否有效应检查具备精度
	 */
	@SimpleProperty
	double 精度();

	/**
	 * 读取请求的启用状态。
	 *
	 * @return 希望在应用前台接收位置时为真，不表示权限、服务或定位结果可用
	 */
	@SimpleProperty
	boolean 启用();

	/**
	 * 设置前台定位的启用状态；授权或服务恢复后自动重试。
	 *
	 * @param enabled 真为启用，假为停用；失败通过操作失败通知
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "True"
	)
	void 启用(boolean enabled);

	/**
	 * 判断是否已登记至少一个可用提供者的定位监听。
	 *
	 * @return 正在前台等待定位时为真，不表示已得到位置
	 */
	@SimpleProperty
	boolean 正在定位();

	/**
	 * 读取最近一次未受理或操作失败的原因。
	 *
	 * @return 错误说明；正常启动定位或受理地址查询后为空
	 */
	@SimpleProperty
	String 错误信息();

	/**
	 * 读取最近一次地址查询缓存，不执行网络或磁盘查询。
	 *
	 * @return 与当前经纬度对应的已查询地址；尚未查询或坐标改变后为空
	 */
	@SimpleProperty
	String 当前地址();

	/**
	 * 在后台查询最近一次位置的地址，界面线程不会等待查询完成。
	 *
	 * <p>需要先取得位置，并且系统提供地址查询服务；网络或服务故障可能导致失败。
	 * 查询期间再次请求会被拒绝，组件销毁后不再报告结果。
	 *
	 * @return 已受理时为真，之后通过地址查询完毕或操作失败通知；未受理时为假，原因见错误信息
	 */
	@SimpleFunction
	boolean 请求地址();
}

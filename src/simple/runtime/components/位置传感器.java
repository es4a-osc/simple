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
import simple.runtime.annotations.UsesPermissions;

/**
 * 可提供经度、纬度和海拔信息的传感器。
 *
 * @author Ellen Spertus
 */
@SimpleObject
@SimpleComponent
@UsesPermissions(permissionNames =
	// 允许请求精确位置；部分蓝牙扫描和无线网络信息也受此权限约束。
	"android.permission.ACCESS_FINE_LOCATION," +
	// 允许请求大致位置。
	"android.permission.ACCESS_COARSE_LOCATION," +
	// 保留旧版模拟位置权限声明，是否可用仍受系统策略限制。
	"android.permission.ACCESS_MOCK_LOCATION," +
	// 允许调用定位提供者的附加命令。
	"android.permission.ACCESS_LOCATION_EXTRA_COMMANDS"
)
public interface 位置传感器  extends 传感器组件 {

	/* 组件事件 */

	/**
	 * 指示已检测到新位置。
	 *
	 * @param latitude 纬度
	 * @param longitude 经度
	 * @param altitude 海拔高度
	 */
	@SimpleEvent
	void 位置改变(double latitude, double longitude, double altitude);

	/* 组件属性 */

	/**
	 * 指示设备是否具有位置传感器。
	 *
	 * @return 当前“是否可用”状态
	 */
	@SimpleProperty
	boolean 是否可用();

	/**
	 * 指示海拔信息是否可用。
	 *
	 * @return 当前“具备海拔”状态
	 */
	@SimpleProperty
	boolean 具备海拔();

	/**
	 * 指示有关位置精度的信息是否可用。
	 *
	 * @return 当前“具备精度”状态
	 */
	@SimpleProperty
	boolean 具备精度();

	/**
	 * 最新的可用经度值。如果没有值可用，将返回{@code 0}。
	 *
	 * @return 最新的可用经度值。如果没有值可用，将返回0
	 */
	@SimpleProperty
	double 经度();

	/**
	 * 最新可用的纬度值。如果没有值可用，将返回{@code 0}。
	 *
	 * @return 最新可用的纬度值。如果没有值可用，将返回0
	 */
	@SimpleProperty
	double 纬度();

	/**
	 * 最近可用的海拔值，以米为单位。如果没有值可用，将返回{@code 0}。
	 *
	 * @return 最近可用的海拔值，以米为单位。如果没有值可用，将返回0
	 */
	@SimpleProperty
	double 海拔();

	/**
	 * 最新的精度测量，以米为单位。如果没有可用的值，则返回{@code 0}。
	 *
	 * @return 最新的精度测量，以米为单位。如果没有可用的值，则返回0
	 */
	@SimpleProperty
	double 精度();

	/**
	 * 启用属性获取器。
	 *
	 * @return {@code true}表示传感器生成事件，{@code false}表示它不生成事件
	 */
	@SimpleProperty
	boolean 启用();

	/**
	 * 启用属性设置器。
	 *
	 * @param enabled {@code true}表示传感器生成事件，{@code false}表示它不生成事件
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_BOOLEAN,
		initializer = "True"
	)
	void 启用(boolean enabled);

	/**
	 * 提供当前地址或空字符串的文本表示形式。
	 *
	 * @return 提供当前地址或空字符串的文本表示形式
	 */
	@SimpleProperty
	String 当前地址();
}

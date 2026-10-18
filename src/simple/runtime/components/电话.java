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
import simple.runtime.collections.集合;

/**
 * 访问设备电话、SIM 卡、通话记录和振动功能。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
@UsesPermissions(
	permissionNames =
		// 直接拨打电话；仅打开拨号界面不需要此权限。
		"android.permission.CALL_PHONE," +
		// 控制设备振动。
		"android.permission.VIBRATE," +
		// 查询、监听通话状态，并兼容旧系统的本机号码读取。
		"android.permission.READ_PHONE_STATE," +
		// 读取 SIM 卡提供的本机号码，号码可能为空。
		"android.permission.READ_PHONE_NUMBERS," +
		// 读取通话记录和来电号码，受系统及安装来源限制。
		"android.permission.READ_CALL_LOG," +
		// 删除指定通话记录，受系统及安装来源限制。
		"android.permission.WRITE_CALL_LOG"
)
public interface 电话 extends 组件 {

	/* 组件事件 */

	/**
	 * 开始监听后通知当前通话状态及后续变化。
	 *
	 * <p>状态 0 表示空闲，1 表示响铃，2 表示摘机（包括拨号和通话），不代表对方已接听。
	 * 来电号码可能因权限、运营商或系统限制为空。</p>
	 *
	 * @param state 系统通话状态。
	 * @param phoneNumber 系统提供的来电号码，不可获取时为空文本。
	 */
	@SimpleEvent
	void 通话状态改变(int state, String phoneNumber);

	/* 组件属性 */

	/**
	 * 判断设备是否声明电话硬件且能取得电话服务。
	 *
	 * @return 具备电话能力时返回真，不表示 SIM 卡就绪、权限已授予或呼叫一定成功。
	 */
	@SimpleProperty
	boolean 可用();

	/**
	 * 判断此组件是否正在监听通话状态。
	 *
	 * @return 正在监听时返回真。
	 */
	@SimpleProperty
	boolean 监听中();

	/**
	 * 查询默认电话服务的通话状态，需要电话状态权限。
	 *
	 * @return 0 表示空闲，1 表示响铃，2 表示摘机；无电话能力时返回 -1。
	 */
	@SimpleProperty
	int 通话状态();

	/* 组件方法 */

	/**
	 * 直接拨打指定号码，需要呼叫权限，可能产生费用。
	 *
	 * @param phoneNumber 电话号码，不得为空。
	 */
	@SimpleFunction
	void 呼叫(String phoneNumber);

	/**
	 * 打开系统拨号界面，由用户确认拨打，不需要呼叫权限。
	 *
	 * @param phoneNumber 预填号码，空文本表示不预填。
	 */
	@SimpleFunction
	void 启动拨号界面(String phoneNumber);

	/**
	 * 控制设备振动，需要振动权限。
	 *
	 * @param duration 持续毫秒数，0 表示停止振动，不得为负数。
	 */
	@SimpleFunction
	void 振动(int duration);

	/**
	 * 在主线程开始监听通话状态，需要电话状态权限，重复调用不重复注册。
	 */
	@SimpleFunction
	void 开始监听();

	/**
	 * 停止监听，未开始时无需处理；停止后不再派发旧监听器的回调。
	 */
	@SimpleFunction
	void 停止监听();

	/**
	 * 查询默认 SIM 卡状态，不申请电话权限。
	 *
	 * @return 系统状态：0 未知、1 无卡、2 需要 PIN、3 需要 PUK、4 网络锁、5 就绪等。
	 */
	@SimpleFunction
	int 取SIM卡状态();

	/**
	 * 查询指定卡槽的 SIM 卡状态，需要 Android 8.0 或更高版本。
	 *
	 * @param slotIndex 卡槽索引，从 0 开始，不是订阅编号。
	 * @return 与默认 SIM 卡查询一致的系统状态值。
	 */
	@SimpleFunction
	int 取SIM卡状态(int slotIndex);

	/**
	 * 读取默认订阅的本机号码，需要读取号码权限；旧目标版本也可使用电话状态权限。
	 *
	 * @return SIM 卡提供的号码，未写入或系统不提供时为空文本，不以设备标识代替号码。
	 */
	@SimpleFunction
	String 取本机号码();

	/**
	 * 同步读取最近最多 100 条通话记录，需要 Android 4.1 及读取通话记录权限。
	 *
	 * <p>每项为文本数组：[记录编号、号码、缓存联系人名、类型、时间戳毫秒、时长秒]。
	 * 类型采用系统值，如 1 来电、2 去电、3 未接；读取权限受系统及安装来源限制。</p>
	 *
	 * @return 按时间及编号倒序排列的记录，没有记录时返回空集合。
	 */
	@SimpleFunction
	集合 取通话记录();

	/**
	 * 按记录编号删除一条通话记录，需要 Android 4.1 及写入通话记录权限。
	 *
	 * @param recordId 查询结果中的正整数记录编号，不是电话号码。
	 * @return 确实删除记录时返回真，记录不存在时返回假。
	 */
	@SimpleFunction
	boolean 删除通话记录(String recordId);
}

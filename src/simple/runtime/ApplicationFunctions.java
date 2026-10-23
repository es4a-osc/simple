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

package simple.runtime;

import simple.runtime.collections.意图;
import simple.runtime.components.窗口;
import simple.runtime.variants.Variant;

/**
 * 应用程序相关功能的接口。Simple程序员无法访问。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public interface ApplicationFunctions {

	/**
	 * 创建一个包含给定标题的新菜单项。
	 *
	 * <p>标题还将用于标识菜单事件处理程序中的菜单项。
	 *
	 * @param caption 菜单项标题
	 */
	void addMenuItem(String caption);

	/**
	 * 显示其它窗口。
	 *
	 * @param form 要显示的窗口
	 */
	void switchForm(窗口 form);

	/**
	 * 终止此应用程序，结束主活动及当前应用进程。
	 */
	void terminate();

	/**
	 * 检索以前存储的首选项的值（即使从同一程序的先前部分检索）。
	 *
	 * @param name 用于在以下存储值的名称
	 * @return 与名称关联的值
	 */
	Variant getPreference(String name);

	/**
	 * 将给定值存储在给定名称下。可以随时使用给定名称检索该值（即使在程序的后续运行中）。
	 *
	 * @param name 要在以下存储值的名称
	 * @param value 要存储的值（必须是原始值，不允许使用对象）
	 */
	void storePreference(String name, Variant value);

	/**
	 * 取当前应用包名。
	 *
	 * @return 返回此应用程序的包的名称
	 */
	String getPackageName();

	/**
	 * 获取应用程序名称
	 *
	 * @return 当前应用的显示名称
	 */
	String getAppName();

	/**
	 * 获取应用程序版本名称信息
	 *
	 * @return 当前应用的版本名称
	 */
	String getVersionName();

	/**
	 * 读取当前应用清单中 versionCode 指定的整数版本号。
	 *
	 * @return 当前应用的版本号；读取失败时返回 0。
	 */
	int getVersionCode();

	/**
	 * 启动指定服务。
	 *
	 * @param intent 意图实例。
	 * @return 请求已受理返回真；服务不存在、启动受限、意图无效或权限不足返回假。
	 */
	boolean serviceStart(意图 intent);

	/**
	 * 请求停止指定服务。
	 *
	 * <p>Simple 服务单元会在队列处理完毕后自动停止；停止请求不保证当前操作已经退出。
	 *
	 * @param intent 意图实例。
	 * @return 正在运行的匹配服务已收到停止请求时返回真；未运行或请求失败返回假。
	 */
	boolean serviceStop(意图 intent);

	/**
	 * 发送广播消息。
	 *
	 * @param intent 意图实例。
	 */
	void broadcastSend(意图 intent);

	/**
	 * 取当前应用的显示区域宽度。
	 *
	 * <p>左右若有系统导航栏或刘海，不包含其占用区域。
	 *
	 * @return 宽度，单位像素（px）
	 */
	int getDisplayWidth();

	/**
	 * 取当前应用的显示区域高度。
	 *
	 * <p>含系统状态栏和应用标题栏，不含上下系统导航栏及刘海占用区域。
	 *
	 * @return 高度，单位像素（px）
	 */
	int getDisplayHeight();

	/**
	 * 取当前应用的显示密度比例。
	 *
	 * @return 密度比例
	 */
	double getDisplayDensity();

	/**
	 * 取系统状态栏高度，不受是否隐藏影响。
	 *
	 * @return 高度，单位像素（px）
	 */
	int getStatusBarHeight();

	/**
	 *  取当前应用实际内容宽度
	 *
	 * <p>左右若有系统导航栏，不包含其占用区域。
	 *
	 * @return 宽度，单位像素（px）
	 */
	int getContentWidth();

	/**
	 *  取当前应用实际内容高度
	 *
	 * <p>不含系统状态栏、应用标题栏和底部系统导航栏。
	 *
	 * @return 高度，单位像素（px）
	 */
	int getContentHeight();

	/**
	 * 是否为竖屏
	 *
	 * @return 竖屏为真，否则为假
	 */
	boolean isPortraitOrientation();

	/**
	 * 是否在前台
	 *
	 * @return 在前台为真，否则为假
	 */
	boolean isInForeground();

	/**
	 * 返回桌面
	 */
	void returnDesktop();

	/**
	 * 返回应用
	 */
	void returnApp();
}

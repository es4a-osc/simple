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
	 * 终止此应用程序。
	 */
	void finish();

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
	 */
	String getAppName();

	/**
	 * 获取应用程序版本名称信息
	 *
	 * @return 当前应用的版本名称
	 */
	String getVersionName();

	/**
	 * 获取应用程序版本名称信息
	 *
	 * @return 当前应用的版本名称
	 */
	long getVersionCode();

	/**
	 * 启动指定服务。
	 *
	 * @param intent 意图实例。
	 * @return 成功返回{@code true}，否则返回{@code false}。
	 */
	boolean serviceStart(意图 intent);

	/**
	 * 立刻停止指定服务。
	 *
	 * <p>注意：服务在任务执行完成后自行结束自己，而不需要外部去调用停止服务。
	 *
	 * @param intent 意图实例。
	 * @return 成功返回{@code true}，否则返回{@code false}。
	 */
	boolean serviceStop(意图 intent);

	/**
	 * 发送广播消息。
	 *
	 * @param intent 意图实例。
	 */
	void broadcastSend(意图 intent);

	/**
	 * 取屏幕宽度
	 *
	 * @return 宽度，单位像素（px）
	 */
	int getScreenWidth();

	/**
	 * 取屏幕高度
	 *
	 * @return 高度，单位像素（px）
	 */
	int getScreenHeight();

	/**
	 * 取屏幕密度
	 *
	 * @return 密度
	 */
  	double getScreenDensity();

	/**
	 * 取状态栏高度
	 *
	 * @return 高度，单位像素（px）
	 */
	int getStatusBarHeight();

	/**
	 * 取内容宽度
	 *
	 * @return 宽度，单位像素（px）
	 */
	int getContentWidth();

	/**
	 * 取内容高度
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

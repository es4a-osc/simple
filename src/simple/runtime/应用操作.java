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

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.collections.意图;
import simple.runtime.components.窗口;
import simple.runtime.variants.Variant;

/**
 * 实现各种应用程序相关的运行时函数。
 *
 * @author Herbert Czymontek
 */
@SimpleObject
public abstract class 应用操作 {

	private static ApplicationFunctions applicationFunctions;

	private 应用操作() {
	}

	/**
	 * 初始化应用程序库的应用程序功能
	 *
	 * @param functions 应用函数的实现
	 */
	public static void initialize(ApplicationFunctions functions) {
		applicationFunctions = functions;
	}

	/**
	 * 创建一个包含给定标题的新菜单项
	 *
	 * <p>该标题还将用于识别菜单事件处理程序中的菜单项
	 *
	 * @param caption 菜单项标题
	 */
	@SimpleFunction
	public static void AddMenuItem(String caption) {
		applicationFunctions.addMenuItem(caption);
	}

	/**
	 * 显示其它窗口
	 *
	 * @param form 要显示的窗口
	 */
	@SimpleFunction
	public static void 切换窗口(窗口 form) {
		applicationFunctions.switchForm(form);
	}

	/**
	 * 终止此应用程序
	 */
	@SimpleFunction
	public static void 终止() {
		// Simple核心关键字已使用“结束”。
		applicationFunctions.finish();
	}

	/**
	 * 检索以前存储的首选项的值（即使从同一程序的先前部分检索）。
	 *
	 * @param name  用于在以下存储值的名称
	 * @return  与名称关联的值
	 */
	@SimpleFunction
	public static Variant 读取设置(String name) {
		return applicationFunctions.getPreference(name);
	}

	/**
	 * 将给定值存储在给定名称下，可以随时使用给定名称检索该值（即使在程序的后续运行中）
	 *
	 * @param name 要在以下存储值的名称
	 * @param value 要存储的值（必须是原始值，不允许使用对象）
	 */
	@SimpleFunction
	public static void 保存设置(String name, Variant value) {
		applicationFunctions.storePreference(name, value);
	}

	/**
	 * 返回当前应用程序的包名
	 *
	 * @return 应用包名
	 */
	@SimpleFunction
	public static String 取包名() {
		return applicationFunctions.getPackageName();
	}

	/**
	 * 获取应用程序名称
	 */
	@SimpleFunction
	public static String 取应用名() {
		return applicationFunctions.getAppName();
	}

	/**
	 * 获取应用程序版本名称信息
	 *
	 * @return 当前应用的版本名称
	 */
	@SimpleFunction
	public static String 取版本名() {
		return applicationFunctions.getVersionName();
	}

	/**
	 * 获取应用程序版本名称信息
	 *
	 * @return 当前应用的版本号
	 */
	@SimpleFunction
	public static long 取版本号() {
		return applicationFunctions.getVersionCode();
	}

	/**
	 * 启动指定服务
	 *
	 * @param intent 意图实例
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public static boolean 启动服务(意图 intent) {
		return applicationFunctions.serviceStart(intent);
	}

	/**
	 * 立刻停止指定服务。
	 *
	 * <p>注意：服务在任务执行完成后自行结束自己，不需要外部去调用停止服务
	 *
	 * @param intent 意图动作。
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public static boolean 停止服务(意图 intent) {
		return applicationFunctions.serviceStop(intent);
	}

	/**
	 * 发送广播消息
	 *
	 * @param intent 意图动作
	 */
	@SimpleFunction
	public static void 发送广播(意图 intent) {
		applicationFunctions.broadcastSend(intent);
	}

	/**
	 * 取屏幕宽度
	 *
	 * @return 宽度，单位像素（px）
	 */
	@SimpleFunction
	public static int 取屏幕宽度() {
		return applicationFunctions.getScreenWidth();
	}

	/**
	 * 取屏幕高度
	 *
	 * @return 高度，单位像素（px）
	 */
	@SimpleFunction
	public static int 取屏幕高度() {
		return applicationFunctions.getScreenHeight();
	}

	/**
	 * 取屏幕密度
	 *
	 * @return 密度
	 */
	@SimpleFunction
  	public static double 取屏幕密度() {
		return applicationFunctions.getScreenDensity();
	}

	/**
	 * 取状态栏高度
	 *
	 * @return 高度，单位像素（px）
	 */
	@SimpleFunction
	public static int 取状态栏高度() {
		return applicationFunctions.getStatusBarHeight();
	}

	/**
	 * 取内容宽度
	 *
	 * @return 宽度，单位像素（px）
	 */
	@SimpleFunction
	public static int 取内容宽度() {
		return applicationFunctions.getContentWidth();
	}

	/**
	 * 取内容高度
	 *
	 * @return 高度，单位像素（px）
	 */
	@SimpleFunction
	public static int 取内容高度() {
		return applicationFunctions.getContentHeight();
	}

	/**
	 * 是否为竖屏
	 *
	 * @return 竖屏为{@code true}，否则为{@code false}
	 */
	@SimpleFunction
	public static boolean 是否为竖屏() {
		return applicationFunctions.isPortraitOrientation();
	}

	/**
	 * 是否在前台
	 *
	 * @return 在前台为{@code true}，否则为{@code false}
	 */
	@SimpleFunction
	public static boolean 是否在前台() {
		return applicationFunctions.isInForeground();
	}

	/**
	 * 返回桌面
	 */
	@SimpleFunction
	public static void 返回桌面() {
		applicationFunctions.returnDesktop();
	}

	/**
	 * 返回应用
	 */
	@SimpleFunction
	public static void 返回应用() {
		applicationFunctions.returnApp();
	}
}

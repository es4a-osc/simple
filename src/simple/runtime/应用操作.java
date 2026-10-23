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
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public abstract class 应用操作 {

	private static ApplicationFunctions applicationFunctions;

	/**
	 * 静态方法集，不创建实例。
	 */
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
	 * 终止此应用程序，结束主活动及当前应用进程。
	 */
	@SimpleFunction
	public static void 终止() {
		// Simple核心关键字已使用“结束”。
		applicationFunctions.terminate();
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
	 * @return 成功返回包名
	 */
	@SimpleFunction
	public static String 取包名() {
		return applicationFunctions.getPackageName();
	}

	/**
	 * 获取应用程序名称
	 *
	 * @return 成功返回名称，否则返回空字符串
	 */
	@SimpleFunction
	public static String 取应用名() {
		return applicationFunctions.getAppName();
	}

	/**
	 * 获取应用程序版本名称
	 *
	 * @return 成功返回版本名称，否则返回空字符串
	 */
	@SimpleFunction
	public static String 取版本名() {
		return applicationFunctions.getVersionName();
	}

	/**
	 * 获取应用程序版本编号
	 *
	 * @return 成功版本编号，否则返回0
	 */
	@SimpleFunction
	public static int 取版本号() {
		return applicationFunctions.getVersionCode();
	}

	/**
	 * 请求启动指定服务，返回真仅表示系统受理，不表示任务处理完成。
	 *
	 * <p>API 26 及以上受普通服务后台执行限制；启动还需满足目标服务的权限要求。
	 *
	 * @param intent 启动意图
	 * @return 请求已受理返回真；服务不存在、启动受限、意图无效或权限不足返回假
	 */
	@SimpleFunction
	public static boolean 启动服务(意图 intent) {
		return applicationFunctions.serviceStart(intent);
	}

	/**
	 * 请求停止指定服务；正在执行的任务需要响应中断后退出。
	 *
	 * @param intent 服务意图
	 * @return 正在运行的匹配服务已收到停止请求时返回真；未运行或请求失败返回假
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
	 * 取当前应用的显示区域宽度。
	 *
	 * <p>左右若有系统导航栏或刘海，不包含其占用区域。
	 *
	 * @return 宽度，单位像素（px）
	 */
	@SimpleFunction
	public static int 取显示宽度() {
		return applicationFunctions.getDisplayWidth();
	}

	/**
	 * 取当前应用的显示区域高度。
	 *
	 * <p>含系统状态栏和应用标题栏，不含上下系统导航栏及刘海占用区域。
	 *
	 * @return 高度，单位像素（px）
	 */
	@SimpleFunction
	public static int 取显示高度() {
		return applicationFunctions.getDisplayHeight();
	}

	/**
	 * 取当前应用的显示密度比例。
	 *
	 * @return 密度比例
	 */
	@SimpleFunction
	public static double 取显示密度() {
		return applicationFunctions.getDisplayDensity();
	}

	/**
	 * 取系统状态栏高度，不受是否隐藏影响。
	 *
	 * @return 高度，单位像素（px）
	 */
	@SimpleFunction
	public static int 取状态栏高度() {
		return applicationFunctions.getStatusBarHeight();
	}

	/**
	 * 取当前应用实际内容宽度
	 *
	 * <p>不含系统状态栏、应用标题栏和底部系统导航栏。
	 *
	 * @return 宽度，单位像素（px）
	 */
	@SimpleFunction
	public static int 取内容宽度() {
		return applicationFunctions.getContentWidth();
	}

	/**
	 * 取当前应用实际内容高度
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

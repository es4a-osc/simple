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

import java.lang.String;

/**
 * 日志记录相关运行时函数的实现。
 *
 * @author Herbert Czymontek
 */
@SimpleObject
public final class 日志输出 {

	// Simple运行库的模块名称
	public static final String MODULE_NAME_RTL = "Simple运行库";

	// 缺省的模块名称
	protected static String defaulModuleNamet = "ES4A";

	private static LogFunctions logFunctions;

	private 日志输出() {
	}

	/**
	 * 初始化日志记录。
	 *
	 * @param functions 日志功能的实现
	 */
	public static void initialize(LogFunctions functions) {
		logFunctions = functions;
	}

	/**
	 * 取堆栈类方法名
	 * 
	 * @param index
	 * @return 堆栈类方法名
	 */
	private static String getClassMethod(int index) {
		StackTraceElement current = new Throwable().getStackTrace()[index];
		String className = current.getClassName();
		className = className.substring(className.lastIndexOf('.') + 1);
		String methodName = current.getMethodName();
		return String.format("%s.%s", className, methodName);
	}

	/**
	 * 供运行库输出错误。
	 *
	 * @param format 格式文本
	 * @param args 参数值
	 */
	public static void error(String format, Object... args) {
		if (logFunctions != null) {
			logFunctions.error(MODULE_NAME_RTL, "[" + getClassMethod(2) + "] " + String.format(format, args));
		}
	}

	/**
	 * 供运行库输出警告。
	 *
	 * @param format 格式文本
	 * @param args 参数值
	 */
	public static void warning(String format, Object... args) {
		if (logFunctions != null) {
			logFunctions.warning(MODULE_NAME_RTL, "[" + getClassMethod(2) + "] " + String.format(format, args));
		}
	}

	/**
	 * 供运行库输出信息。
	 *
	 * @param format 格式文本
	 * @param args 参数值
	 */
	public static void info(String format, Object... args) {
		if (logFunctions != null) {
			logFunctions.info(MODULE_NAME_RTL, "[" + getClassMethod(2) + "] " + String.format(format, args));
		}
	}
	
	/**
	 * 供运行库输出调试。
	 *
	 * @param format 格式文本
	 * @param args 参数值
	 */
	public static void debug(String format, Object... args) {
		if (logFunctions != null) {
			logFunctions.debug(MODULE_NAME_RTL, "[" + getClassMethod(2) + "] " + String.format(format, args));
		}
	}

	/**
	 * 置输出缺省模型名称。
	 * 
	 * @param moduleName 模块名称
	 */
	@SimpleFunction
	public static void 置输出模块名(String moduleName)
	{
		defaulModuleNamet = moduleName;
	}

	/**
	 * 取输出缺省模块名称。
	 * 
	 * @return 模块名称
	 */
	@SimpleFunction
	public static String 取输出模块名()
	{
		return defaulModuleNamet;
	}

	/**
	 * 记录错误消息。
	 *
	 * @param moduleName 报告消息的模块的名称（例如“ES4A”）
	 * @param message 要记录的日志
	 */
	@SimpleFunction
	public static void 输出错误(String moduleName, String message) {
		logFunctions.error(moduleName, message);
	}

	/**
	 * 记录错误消息。
	 *
	 * @param message  要记录的日志
	 */
	@SimpleFunction
	public static void 输出错误(String message) {
		输出错误(defaulModuleNamet, message);
	}

	/**
	 * 记录警告消息。
	 *
	 * @param moduleName 报告消息的模块的名称（例如“ES4A”）
	 * @param message 要记录的日志
	 */
	@SimpleFunction
	public static void 输出警告(String moduleName, String message) {
		logFunctions.warning(moduleName, message);
	}

	/**
	 * 记录警告消息。
	 * 
	 * @param message 要记录的日志
	 */
	@SimpleFunction
	public static void 输出警告(String message) {
		输出警告(defaulModuleNamet, message);
	}
	
	/**
	 * 记录信息消息。
	 *
	 * @param moduleName 报告消息的模块的名称（例如“ES4A”）
	 * @param message 要记录的日志
	 */
	@SimpleFunction
	public static void 输出信息(String moduleName, String message) {
		logFunctions.info(moduleName, message);
	}
	/**
	 * 记录信息消息。
	 *
	 * @param message 要记录的日志
	 */
	@SimpleFunction
	public static void 输出信息(String message) {
		输出信息(defaulModuleNamet, message);
	}

	/**
	 * 记录调试消息。
	 *
	 * @param moduleName 报告消息的模块的名称（例如“ES4A”）
	 * @param message 要记录的日志
	 */
	@SimpleFunction
	public static void 输出调试(String moduleName, String message) {
		logFunctions.debug(moduleName, message);
	}

	/**
	 * 记录调试消息。
	 * 
	 * @param message 要记录的日志
	 */
	@SimpleFunction
	public static void 输出调试(String message) {
		输出调试(defaulModuleNamet, message);
	}
}

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

/**
 * 用于日志相关函数的接口。
 *
 * <p>Simple程序员无法访问。
 *
 * @author Herbert Czymontek
 */
public interface LogFunctions {

	/**
	 * 日志错误消息。
	 *
	 * @param moduleName  报告消息的模块的名称（例如“Simple运行库”）
	 * @param message  日志文本
	 */
	void error(String moduleName, String message);

	/**
	 * 记录警告消息。
	 *
	 * @param moduleName  报告消息的模块的名称（例如“Simple运行库”）
	 * @param message  日志文本
	 */
	void warning(String moduleName, String message);

	/**
	 * 记录信息消息。
	 *
	 * @param moduleName  报告消息的模块的名称（例如“Simple运行库”）
	 * @param message  日志文本
	 */
	void info(String moduleName, String message);

	/**
	 * 记录调试消息。
	 *
	 * @param moduleName  报告消息的模块的名称（例如“Simple运行库”）
	 * @param message  日志文本
	 */
	void debug(String moduleName, String message);
}

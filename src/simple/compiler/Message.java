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

package simple.compiler;

import simple.compiler.scanner.Scanner;

import java.io.PrintStream;

/**
 * This is the superclass for messages (like errors and warnings) reported by
 * the compiler.
 *
 * <p>Each message is created from a template. The template can have parameters
 * which consist of a percent sign followed by a parameter number. Parameter
 * numbers start with 1. During message construction the message template
 * parameters are substituted with the actual message parameters.
 *
 * @author Herbert Czymontek
 */
public abstract class Message {
	// 由扫描器编码的源位置。
	private final long position;

	// 实际消息
	private final String message;

	/**
	 * 创建一条新消息。
	 *
	 * @param position  源位置
	 * @param message  消息模板
	 * @param params  消息的参数
	 */
	protected Message(long position, String message, String... params) {
		this.position = position;
		this.message = applyParameters(message, params);
	}

	/**
	 * 返回消息种类，例如错误消息的“错误”。
	 *
	 * @return  消息种类
	 */
	protected abstract String messageKind();

	/**
	 * 打印消息。
	 *
	 * @param compiler  当前编译器实例
	 * @param out  要打印到的输出流
	 */
	public final void print(Compiler compiler, PrintStream out) {
		String filename = compiler.fileIndexToPath(Scanner.getFileIndex(position));
		String decodedPosition = filename + '：';
		if (position != Scanner.NO_POSITION) {
			decodedPosition += Scanner.getLine(position) + "：";
		}
		out.println(decodedPosition + messageKind() + "：" + message);
	}

	/**
	 * 本土化已命名的消息，或者如果没有可用的本土化，则使用默认消息对其进行初始化。
	 *
	 * @param msgName  消息名称
	 * @param defaultMessage   默认消息
	 * @return  本土化消息
	 */
	public final static String localize(String msgName, String defaultMessage) {
		// TODO: read localized messages from a properties file
		return defaultMessage;
	}

	/*
	 * 应用参数；将任何参数应用于消息模板中的变量。
	 */
	private static String applyParameters(String message, String... params) {
		int paramCnt = params.length;
		for (int i = 0; i < paramCnt; i++) {
			message = applyParameter(message, i + 1, params[i]);
		}
		return message;
	}

	/*
	 * 应用参数；将参数应用于消息中的变量。
	 */
	private static String applyParameter(String message, int param, String str) {
		return message.replaceAll("%" + param, str);
	}
}

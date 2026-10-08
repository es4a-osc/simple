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

package simple.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 用于命令执行和 I/O 重定向的实用程序类。
 *
 * @author Herbert Czymontek
 */
public final class Execution {

	// 日志记录支持
	private static final Logger LOG = Logger.getLogger(Execution.class.getName());

	/*
	 * 用于标准输出流和错误输出流重定向的输入流处理程序。
	 */
	private static class RedirectStreamHandler extends Thread {
		// 要重定向的流
		private final InputStream input;
		private final PrintWriter output;

		RedirectStreamHandler(PrintWriter output, InputStream input) {
			this.input = Preconditions.checkNotNull(input);
			this.output = Preconditions.checkNotNull(output);
			start();
		}

		@Override
		public void run() {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(input));
				String line;
				while ((line = reader.readLine()) != null) {
						output.println(line);
				}
			} catch (IOException ioe) {
				// 可以忽略...
				LOG.log(Level.WARNING, "____I/O 重定向失败：", ioe);
			}
		}
	}

	private Execution() {
	}

	/**
	 * 将两个字符串列表的内容合并为一个字符串数组。
	 *
	 * @param list1  第一个字符串列表
	 * @param list2  第二个字符串列表
	 * @return  组合字符串数组
	 */
	public static String[] combineIntoStringArray(List<String> list1, List<String> list2) {
		List<String> combinedList = new ArrayList<String>();
		combinedList.addAll(list1);
		combinedList.addAll(list2);
		return combinedList.toArray(new String[combinedList.size()]);
	}

	/**
	 * 在命令shell中执行命令。
	 *
	 * @param workingDir  命令的工作目录
	 * @param command  要执行的命令及其参数
	 * @param out  重定向到的标准输出流
	 * @param err  重定向到的错误输出流
	 * @return  如果命令成功，则为{@code true}，否则为{@code false}
	 */
	public static boolean execute(File workingDir, String[] command, PrintStream out,
			PrintStream err) {
		//LOG.log(Level.INFO, "____执行 " + Strings.join(" ", command));

		try {
			Process process = Runtime.getRuntime().exec(command, null, workingDir);
			// 支持标准输出为空
			if (out != null) {
				new RedirectStreamHandler(new PrintWriter(out, true), process.getInputStream());
			}
			// 支持错误输出为空
			if (err != null) {
				new RedirectStreamHandler(new PrintWriter(err, true), process.getErrorStream());
			}
			return process.waitFor() == 0;
		} catch (Exception e) {
			LOG.log(Level.WARNING, "____执行失败：", e);
			return false;
		}
	}
}

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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * 用于命令执行和 I/O 重定向的实用程序类。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public final class Execution {

	/**
	 * 用于标准输出流和错误输出流重定向的输入流处理程序。
	 */
	private static class RedirectStreamHandler extends Thread {
		// 要重定向的流
		private final InputStream input;
		private final PrintWriter output;
		private final Charset charset;
		private IOException failure;

		/**
		 * 按调用方指定的工具输出编码转发输入流。
		 *
		 * @param output 接收解码后文本的输出。
		 * @param input 外部工具的标准输出或错误输出。
		 * @param charset 外部工具实际使用的输出编码。
		 */
		RedirectStreamHandler(PrintWriter output, InputStream input, Charset charset) {
			this.input = Preconditions.checkNotNull(input);
			this.output = output;
			this.charset = Preconditions.checkNotNull(charset);
		}

		/**
		 * 持续排空管道；不显示的输出也必须读取，避免外部工具因管道写满而阻塞。
		 */
		@Override
		public void run() {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, charset))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (output != null) {
						output.println(line);
					}
				}
				if (output != null && output.checkError()) {
					failure = new IOException("无法转发工具输出。");
				}
			} catch (IOException exception) {
				failure = exception;
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
	 * 执行命令，沿用 JVM 默认编码读取工具输出。
	 *
	 * @param workingDir  命令的工作目录
	 * @param command  要执行的命令及其参数
	 * @param out  重定向到的标准输出流
	 * @param err  重定向到的错误输出流
	 * @return  如果命令成功，则为{@code true}，否则为{@code false}
	 */
	public static boolean execute(File workingDir, String[] command, PrintStream out, PrintStream err) {
		return execute(workingDir, command, out, err, Charset.defaultCharset());
	}

	/**
	 * 执行命令，按指定编码读取标准输出和错误输出。
	 *
	 * <p>仅影响子进程输出的解码，不改变命令参数、终端编码或输出流的编码。
	 *
	 * @param workingDir 命令工作目录；为 {@code null} 时沿用当前目录。
	 * @param command 命令及其参数。
	 * @param out 标准输出的转发目标；为 {@code null} 时不转发。
	 * @param err 错误输出的转发目标；为 {@code null} 时不转发。
	 * @param charset 外部工具实际使用的输出编码。
	 * @return 命令退出码为零时返回 {@code true}，执行失败时返回 {@code false}。
	 */
	public static boolean execute(File workingDir, String[] command, PrintStream out, PrintStream err, Charset charset) {
		Process process = null;
		RedirectStreamHandler standard = null;
		RedirectStreamHandler errors = null;
		try {
			process = new ProcessBuilder(command).directory(workingDir).start();
			standard = new RedirectStreamHandler(
					out == null ? null : new PrintWriter(out, true), process.getInputStream(), charset);
			errors = new RedirectStreamHandler(
					err == null ? null : new PrintWriter(err, true), process.getErrorStream(), charset);
			standard.start();
			errors.start();
			// 此入口不提供交互输入，立即关闭标准输入，让等待 EOF 的工具正常结束。
			process.getOutputStream().close();

			// 子进程退出不代表日志已转发完；返回前等待两路输出都读到 EOF。
			int exitCode = process.waitFor();
			standard.join();
			errors.join();
			if (standard.failure != null || errors.failure != null) {
				throw standard.failure != null ? standard.failure : errors.failure;
			}
			return exitCode == 0;
		} catch (InterruptedException failure) {
			stopProcess(process, standard, errors);
			Thread.currentThread().interrupt();
			if (err != null) {
				err.println("工具执行已中断。");
			}
			return false;
		} catch (IOException failure) {
			stopProcess(process, standard, errors);
			if (err != null) {
				err.println("工具执行失败：" + failure.getMessage());
			}
			return false;
		}
	}

	/**
	 * 失败或取消时结束子进程并回收输出线程，保留调用线程原有及等待期间收到的中断。
	 *
	 * @param process 已启动的子进程，可以为空。
	 * @param standard 普通输出线程，可以为空。
	 * @param errors 错误输出线程，可以为空。
	 */
	private static void stopProcess(Process process, Thread standard, Thread errors) {
		if (process == null) {
			return;
		}
		process.destroyForcibly();
		boolean interrupted = Thread.interrupted();
		for (Thread reader : new Thread[] {standard, errors}) {
			if (reader == null) {
				continue;
			}
			while (reader.isAlive()) {
				try {
					reader.join();
				} catch (InterruptedException failure) {
					interrupted = true;
				}
			}
		}
		if (interrupted) {
			Thread.currentThread().interrupt();
		}
	}
}

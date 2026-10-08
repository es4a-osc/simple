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

import simple.compiler.Compiler.Platform;

import java.io.IOException;

/**
 * Main entry point for the command line version of the Simple compiler.
 *
 * @author Herbert Czymontek
 */
public final class Main {

	// COV_NF_START

	private Main() {
	}

	/**
	 * Main entry point.
	 *
	 * @param args  command line arguments
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Usage: simplec <projectfile>");
		} else {
			try {
				boolean successful = Compiler.compile(Platform.Android, new Project(args[0], null), System.out, System.err);
				
				// 编译成功退出码为 0 xhwsd@qq.com 2026-9-4
				if (successful) {
					System.exit(0);
					return;
				}
			} catch (IOException ioe) {
				System.err.println("Cannot read project file '" + args[0] + "'");
			}
		}

		// 编译失败退出码为 1 xhwsd@qq.com 2026-9-4
		System.exit(1);
	}

	// COV_NF_END
}

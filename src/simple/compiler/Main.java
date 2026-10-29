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

import java.io.File;
import java.io.IOException;

/**
 * 编译器命令行入口，同时为项目工具提供构建目录查询。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public final class Main {

	// COV_NF_START

	/**
	 * 禁止实例化仅提供命令行入口的类。
	 */
	private Main() {
	}

	/**
	 * 编译指定项目，或仅使用项目模型读取构建目录，不创建任何生成物。
	 *
	 * @param args 项目文件，或 {@code --build-directory <项目文件>}。
	 */
	public static void main(String[] args) {
		boolean directoryOnly = args.length == 2 && "--build-directory".equals(args[0]);
		if (args.length != 1 && !directoryOnly) {
			System.err.println("用法：simplec <项目文件>");
			System.err.println("查询构建目录：simplec --build-directory <项目文件>");
		} else {
			String projectFile = args[directoryOnly ? 1 : 0];
			try {
				// 两种入口共用 Java Properties 读取规则，脚本无需复制配置解析及路径语义。
				Project project = new Project(new File(projectFile).getAbsoluteFile());
				boolean successful;
				if (directoryOnly) {
					System.out.println(project.getBuildDirectory().getCanonicalPath());
					successful = true;
				} else {
					successful = Compiler.compile(Platform.Android, project, System.out, System.err);
				}

				// 查询或构建成功时返回零，失败时统一返回非零退出码。
				if (successful) {
					System.exit(0);
					return;
				}
			} catch (IOException | IllegalArgumentException failure) {
				System.err.println("无法读取项目文件：" + projectFile + "（" + failure.getMessage() + "）");
			}
		}

		// 参数、项目配置或编译阶段任一步失败都不能报告成功。
		System.exit(1);
	}

	// COV_NF_END
}

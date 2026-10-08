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

package simple.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.regex.Pattern;

import simple.compiler.util.Signatures;

/**
 * Creates a new Simple project.
 *
 * @author Herbert Czymontek
 */
public class ProjectCreator {

	private static void usage() {
		System.err.println("Usage: newsimpleproject qualified-form-name\n" +
											 "           e.g. simpleproject com.yourdomain.Test");
		System.exit(1);
	}

	private static void fatal(String msg) {
		System.err.println("Error: " + msg);
		System.exit(-1);
	}

	private static void createDirectories(File dir) throws IOException {
		if (!dir.mkdirs()) {
			throw new IOException("cannot create directories " + dir);
		}
	}

	private static void createTextFile(File dir, String name, String content) throws IOException {
		File file = new File(dir, name);
		file.createNewFile();
		Writer output = new BufferedWriter(new FileWriter(file));
		try {
			output.write(content);
		} finally {
			output.close();
		}
	}

	/**
	 * Main entry point.
	 *
	 * @param args  command line arguments
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			usage();
		}

		// Check qualified form name
		String qualifiedFormName = args[0];
		if (!Pattern.matches("^(([a-z])+.)+[A-Z]([A-Za-z])+$", qualifiedFormName)) {
			fatal("malformed qualified form name - must be a valid Java class name, " +
					"e.g. com.yourdomain.Test");
		}

		// Get form name components
		String formName = Signatures.getClassName(qualifiedFormName);
		String packageName = Signatures.getPackageName(qualifiedFormName);

		// 使用simpleproject目录和src目录创建项目根目录
		File projectDir = new File(formName);
		File srcDir = new File(formName + "/src/" + packageName.replace('.', '/'));
		File assetsDir = new File(formName + "/assets");
		File resDir = new File(formName + "/res");
		try {
			createDirectories(projectDir);
			createDirectories(srcDir);
			createDirectories(assetsDir);
			createDirectories(resDir);

		} catch (IOException e) {
			fatal("无法创建目录");
		}

		try {
			// 创建项目文件
			createTextFile(projectDir, "project.properties",
					"main=" + qualifiedFormName + "\n" +
					"name=" + formName + "\n" +
					"assets=./assets\n" +
					"res=./res\n" +
					"source=./src\n" +
					"build=./build\n");

			// 创建窗口源文件
			createTextFile(srcDir, formName + ".simple",
					"$属性\n" +
					"$资源 $窗口\n" +
					"$定义 " + formName + " $为 窗口\n" +
					"布局 = 1\n" +
					"布局.方向 = 1\n" +
					"$结束 $定义\n" +
					"$结束 $属性\n");

		} catch (IOException e) {
			fatal("无法创建源文件");
		}
	}
}

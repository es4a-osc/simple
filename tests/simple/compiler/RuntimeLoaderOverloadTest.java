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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import junit.framework.TestCase;
import simple.compiler.Compiler.Platform;
import simple.compiler.parser.Parser;
import simple.compiler.scanner.Scanner;
import simple.compiler.symbols.FunctionSymbol;
import simple.compiler.symbols.ObjectSymbol;
import simple.compiler.symbols.Symbol;

/**
 * 验证运行库中的同名@SimpleFunction方法不会在加载时丢失。
 */
public final class RuntimeLoaderOverloadTest extends TestCase {
	private static final String TEST_RUNTIME_CLASS =
			"simple/runtime/OverloadedRuntimeObject.class";

	public void testLoadsFunctionsWithDifferentParameterCounts() {
		Compiler compiler = new Compiler(Platform.None, System.out, System.err);
		RuntimeLoader loader = new RuntimeLoader(compiler,
				new File("build/runtime/android/classes").getAbsolutePath(),
				new File("build/tests/classes").getAbsolutePath(), null);

		loader.loadSimpleObjects();

		ObjectSymbol object = ObjectSymbol.getObjectSymbol(compiler,
				"simple/runtime/OverloadedRuntimeObject");
		Symbol symbol = object.getScope().lookupShallow("calculate");
		assertTrue(symbol instanceof FunctionSymbol);

		FunctionSymbol function = (FunctionSymbol) symbol;
		assertNotNull(function.getOverload(0));
		assertNotNull(function.getOverload(1));
		Symbol globalFunction = compiler.getGlobalNamespaceSymbol().getScope()
				.lookupShallow("calculate");
		assertSame(function, globalFunction);

		// 使用全局函数入口解析两个不同参数个数的Simple调用。
		Scanner scanner = new Scanner(compiler,
				"静态 过程 Run()\n"
				+ "  变量 result 为 整数型\n"
				+ "  result = calculate()\n"
				+ "  result = calculate(1)\n"
				+ "结束 过程\n"
				+ "\n"
				+ "$属性\n"
				+ "  $资源 $对象\n"
				+ "$结束 $属性\n");
		Parser parser = new Parser(compiler, scanner, "simple.compiler.runtimecall.CallSite");
		parser.parse();
		compiler.resolve();

		assertEquals(0, compiler.getErrorCount());
	}

	public void testLoadsOptionalExtensionLibraryDirectory() throws Exception {
		File librariesDirectory = new File("build/tests/extension-libraries").getAbsoluteFile();
		deleteRecursively(librariesDirectory);
		File runtimeDirectory = new File("build/tests/empty-runtime").getAbsoluteFile();
		deleteRecursively(runtimeDirectory);
		assertTrue(runtimeDirectory.mkdirs());
		File libraryDirectory = new File(librariesDirectory, "example.library");
		assertTrue(libraryDirectory.mkdirs());
		// 同一根目录允许保留尚未生成classes.jar的开发中类库。
		File unfinishedSource = new File(librariesDirectory, "unfinished.library/src");
		assertTrue(unfinishedSource.mkdirs());

		// 使用已经编译的测试运行库类生成固定结构的扩展库classes.jar。
		File classesFile = new File(libraryDirectory, "classes.jar");
		InputStream input = getClass().getClassLoader().getResourceAsStream(TEST_RUNTIME_CLASS);
		assertNotNull(input);
		JarOutputStream output = new JarOutputStream(new FileOutputStream(classesFile));
		try {
			output.putNextEntry(new JarEntry(TEST_RUNTIME_CLASS));
			byte[] buffer = new byte[4096];
			for (int read; (read = input.read(buffer)) != -1;) {
				output.write(buffer, 0, read);
			}
			output.closeEntry();
		} finally {
			input.close();
			output.close();
		}

		Compiler compiler = new Compiler(Platform.None, System.out, System.err);
		RuntimeLoader loader = new RuntimeLoader(compiler,
				new File("build/runtime/android/classes").getAbsolutePath(),
				runtimeDirectory.getAbsolutePath(),
				librariesDirectory.getAbsolutePath());
		loader.loadSimpleObjects();

		ObjectSymbol object = ObjectSymbol.getObjectSymbol(compiler,
				"simple/runtime/OverloadedRuntimeObject");
		assertNotNull(object);
		assertEquals(libraryDirectory.getAbsoluteFile(),
				loader.getExtensionLibraryDirectory("simple/runtime/OverloadedRuntimeObject"));
		assertEquals(0, compiler.getErrorCount());
	}

	public void testMissingOptionalExtensionLibraryDirectoryIsIgnored() {
		File librariesDirectory = new File("build/tests/missing-extension-libraries").getAbsoluteFile();
		deleteRecursively(librariesDirectory);
		File runtimeDirectory = new File("build/tests/empty-runtime").getAbsoluteFile();
		if (!runtimeDirectory.isDirectory()) {
			assertTrue(runtimeDirectory.mkdirs());
		}

		Compiler compiler = new Compiler(Platform.None, System.out, System.err);
		RuntimeLoader loader = new RuntimeLoader(compiler,
				new File("build/runtime/android/classes").getAbsolutePath(),
				runtimeDirectory.getAbsolutePath(),
				librariesDirectory.getAbsolutePath());
		loader.loadSimpleObjects();

		assertEquals(0, compiler.getErrorCount());
	}

	private static void deleteRecursively(File file) {
		if (!file.exists()) {
			return;
		}
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children != null) {
				for (File child : children) {
					deleteRecursively(child);
				}
			}
		}
		assertTrue(file.delete());
	}
}

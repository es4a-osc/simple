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

package simple.compiler.parser;

import junit.framework.TestCase;
import simple.compiler.Compiler;
import simple.compiler.Compiler.Platform;
import simple.compiler.parser.Parser;
import simple.compiler.scanner.Scanner;

/**
 * Tests for {@link Parser}.
 *
 * @author Igor Karp
 */
public class ParserTest extends TestCase {
	private Compiler compiler;

	public ParserTest(String testName) {
		super(testName);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		compiler = new Compiler(Platform.None, System.out, System.err);
	}

	/**
	 * Tests whether the Simple parser correctly (without exception) handles the
	 * absence of () in event handler declaration.
	 */
	public void testErrorInEventHandlerDeclaration() {
		// TODO: parser should complain about Int, should be Integer
		try {
			Scanner scanner = new Scanner(compiler, "变量 A 为 Int\n"
					+ "\n"
					+ "事件 Form1.初始化\n" // 应该是 事件 Form1.初始化()
					+ "  A = 1\n"
					+ "结束 事件\n"
					+ "\n"
					+ "$属性\n"
					+ "  $资源 $对象\n"
					+ "$结束 $属性\n");

			Parser parser = new Parser(compiler, scanner, "does.not.matter");
			parser.parse();

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		assertEquals(1, compiler.getErrorCount());
	}

	/**
	 * Tests whether the Simple parser correctly (without exception) handles the
	 * presence of () after the New operator for objects (instead of arrays).
	 */
	public void testErrorInNewOperator() {
		try {
			Scanner scanner = new Scanner(compiler,
					"过程 Bar()\n"
					+ "  变量 n 为 对象\n"
					+ "  n = 创建 对象()\n"
					+ "结束 过程\n"
					+ "\n"
					+ "$属性\n"
					+ "  $资源 $对象\n"
					+ "$结束 $属性\n");

			Parser parser = new Parser(compiler, scanner, "does.not.matter");
			parser.parse();
			compiler.resolve();

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		assertEquals(1, compiler.getErrorCount());
	}

	/**
	 * Tests whether the Simple parser prohibits sequences of identity operators.
	 */
	public void testErrorInIdentityOperator() {
		try {
			Scanner scanner = new Scanner(compiler,
					"过程 Bar()\n"
					+ "  变量 n 为 整数型\n"
					+ "  n = -+n\n"
					+ "结束 过程\n"
					+ "\n"
					+ "$属性\n"
					+ "  $资源 $对象\n"
					+ "$结束 $属性\n");

			Parser parser = new Parser(compiler, scanner, "does.not.matter");
			parser.parse();

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		assertEquals(1, compiler.getErrorCount());
	}


	/**
	 * Tests whether the Simple parser detects function wrong Exit statements.
	 */
	public void testErrorInExitStatement() {
		try {
			Scanner scanner = new Scanner(compiler,
					"过程 Bar()\n"
					+ "  退出 函数\n"
					+ "结束 过程\n"
					+ "\n"
					+ "$属性\n"
					+ "  $资源 $对象\n"
					+ "$结束 $属性\n");

			Parser parser = new Parser(compiler, scanner, "does.not.matter");
			parser.parse();
			compiler.resolve();

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		assertEquals(1, compiler.getErrorCount());
	}

	/**
	 * 同名函数或过程只能按参数个数重载，参数类型不能单独区分重载。
	 */
	public void testRejectsOverloadsWithSameParameterCount() {
		try {
			Scanner scanner = new Scanner(compiler,
					"过程 Calculate(value 为 整数型)\n"
					+ "结束 过程\n"
					+ "\n"
					+ "过程 Calculate(value 为 文本型)\n"
					+ "结束 过程\n"
					+ "\n"
					+ "$属性\n"
					+ "  $资源 $对象\n"
					+ "$结束 $属性\n");

			Parser parser = new Parser(compiler, scanner, "does.not.matter");
			parser.parse();

		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}

		assertEquals(1, compiler.getErrorCount());
	}
}

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

package simple;

import simple.compiler.CompilerSmokeTest;
import simple.compiler.parser.ParserTest;
import simple.compiler.scanner.ScannerTest;
import simple.compiler.util.ResourceTest;

import simple.runtime.ArraysTest;
import simple.runtime.ConversionsTest;
import simple.runtime.DatesTest;
import simple.runtime.FilesPathTest;
import simple.runtime.FilesTest;
import simple.runtime.MathTest;
import simple.runtime.PixelsTest;
import simple.runtime.OtherOperationsTest;
import simple.runtime.ObjectOperationsTest;
import simple.runtime.RadixConversionsTest;
import simple.runtime.SequencesTest;
import simple.runtime.StringsTest;
import simple.runtime.TextFormattingTest;

import junit.framework.TestSuite;
import junit.framework.Test;

/**
 * Unit tests for simple.*
 *
 * <b>THIS TEST SUITE MUST BE RUN BY EVERY DEVELOPER BEFORE EVERY CHECKIN!</b>
 *
 * @author Herbert Czymontek
 */
public class AllTests {

	/**
	 * 组织编译器与运行库回归用例，供统一测试入口执行。
	 *
	 * @return 包含全部已登记用例的测试套件。
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite("Simple Tests");

		// simple.compiler
		suite.addTestSuite(CompilerSmokeTest.class);

		// simple.compiler.parser
		suite.addTestSuite(ParserTest.class);

		// simple.compiler.scanner
		suite.addTestSuite(ScannerTest.class);

		// simple.util
		suite.addTestSuite(ResourceTest.class);

		// simple.runtime
		suite.addTestSuite(ArraysTest.class);
		suite.addTestSuite(ConversionsTest.class);
		suite.addTestSuite(RadixConversionsTest.class);
		suite.addTestSuite(DatesTest.class);
		suite.addTestSuite(FilesTest.class);
		suite.addTestSuite(FilesPathTest.class);
		suite.addTestSuite(MathTest.class);
		suite.addTestSuite(PixelsTest.class);
		suite.addTestSuite(OtherOperationsTest.class);
		suite.addTestSuite(ObjectOperationsTest.class);
		suite.addTestSuite(StringsTest.class);
		suite.addTestSuite(TextFormattingTest.class);
		suite.addTestSuite(SequencesTest.class);

		return suite;
	}
}

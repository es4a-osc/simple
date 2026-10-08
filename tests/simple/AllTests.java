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
import simple.runtime.ArraysTest;
import simple.runtime.ConversionsTest;
import simple.runtime.DatesTest;
import simple.runtime.FilesTest;
import simple.runtime.MathTest;
import simple.runtime.PixelsTest;
import simple.runtime.StringsTest;
import simple.compiler.util.ResourceTest;

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

	public static Test suite() {
		TestSuite suite = new TestSuite("Simple Tests");
		// compiler
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
		suite.addTestSuite(DatesTest.class);
		suite.addTestSuite(FilesTest.class);
		suite.addTestSuite(MathTest.class);
		suite.addTestSuite(PixelsTest.class);
		suite.addTestSuite(StringsTest.class);
		return suite;
	}
}

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
import simple.compiler.PopupMenuComponentTest;
import simple.runtime.components.impl.android.util.PopupMenuModelTest;
import simple.runtime.components.impl.android.util.HttpTransportTest;
import simple.compiler.CanvasComponentTest;
import simple.compiler.BatteryMonitorComponentTest;
import simple.compiler.FingerprintComponentTest;
import simple.runtime.components.impl.android.util.BatterySnapshotTest;
import simple.compiler.ClipboardOperationsTest;
import simple.compiler.StorageSpaceTest;
import simple.compiler.DateTimePickerTest;
import simple.compiler.DataTableComponentTest;
import simple.compiler.RichTextComponentTest;
import simple.runtime.components.impl.android.util.RichTextRangesTest;
import simple.runtime.components.impl.android.util.DataTableModelTest;
import simple.compiler.ContactsPermissionsTest;
import simple.compiler.LibraryDependenciesTest;
import simple.compiler.KeywordMemberTest;
import simple.compiler.parser.ParserTest;
import simple.compiler.scanner.ScannerTest;
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
import simple.compiler.util.ResourceTest;
import simple.compiler.util.DexToolchainTest;
import simple.compiler.util.SigningToolchainTest;

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
		suite.addTestSuite(PopupMenuComponentTest.class);
		suite.addTestSuite(PopupMenuModelTest.class);
		suite.addTestSuite(CanvasComponentTest.class);
		suite.addTestSuite(BatteryMonitorComponentTest.class);
		suite.addTestSuite(FingerprintComponentTest.class);
		suite.addTestSuite(BatterySnapshotTest.class);
		suite.addTestSuite(ClipboardOperationsTest.class);
		suite.addTestSuite(StorageSpaceTest.class);
		suite.addTestSuite(DateTimePickerTest.class);
		suite.addTestSuite(DataTableComponentTest.class);
		suite.addTestSuite(RichTextComponentTest.class);
		suite.addTestSuite(RichTextRangesTest.class);
		suite.addTestSuite(DataTableModelTest.class);
		suite.addTestSuite(ContactsPermissionsTest.class);
		suite.addTestSuite(LibraryDependenciesTest.class);
		suite.addTestSuite(KeywordMemberTest.class);
		// simple.compiler.parser
		suite.addTestSuite(ParserTest.class);
		// simple.compiler.scanner
		suite.addTestSuite(ScannerTest.class);
		// simple.util
		suite.addTestSuite(ResourceTest.class);
		suite.addTestSuite(DexToolchainTest.class);
		suite.addTestSuite(SigningToolchainTest.class);

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
		suite.addTestSuite(HttpTransportTest.class);
		return suite;
	}
}

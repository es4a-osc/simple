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

package simple.compiler.scanner;

import junit.framework.TestCase;
import simple.compiler.Compiler;
import simple.compiler.Compiler.Platform;
import simple.compiler.scanner.Keywords;
import simple.compiler.scanner.Scanner;
import simple.compiler.scanner.TokenKind;

/**
 * Tests for {@link Scanner}.
 *
 * @author Herbert Czymontek
 */
public class ScannerTest extends TestCase {

	private Compiler compiler;

	public ScannerTest(String testName) {
		super(testName);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		compiler = new Compiler(Platform.None, System.out, System.err);
	}

	/**
	 * Tests {@link Scanner#scanSourceSection()} and
	 * {@link Scanner#scanPropertiesSection()}.
	 */
	public void testScanSection() {
		// Scanner relies on a properly formed properties section
		Scanner scanner = new Scanner(compiler, "循环 $定义 \n$属性\n 循环\n $结束 $属性\n");

		int errorCount = 0;
		assertEquals(errorCount, compiler.getErrorCount());

		// $Properties
		scanner.scanPropertiesSection();
		assertEquals(TokenKind.TOK_$PROPERTIES, scanner.nextToken());
		assertEquals(TokenKind.TOK_EOS, scanner.nextToken());

		// For
		assertEquals(TokenKind.TOK_FOR, scanner.nextToken());
		assertEquals(TokenKind.TOK_EOS, scanner.nextToken());

		// $End $Properties
		assertEquals(TokenKind.TOK_$END, scanner.nextToken());
		assertEquals(TokenKind.TOK_$PROPERTIES, scanner.nextToken());
		assertEquals(TokenKind.TOK_EOS, scanner.nextToken());
		assertEquals(TokenKind.TOK_EOF, scanner.nextToken());

		// For
		scanner.scanSourceSection();
		assertEquals(TokenKind.TOK_FOR, scanner.nextToken());

		// $Define
		assertEquals(TokenKind.TOK_IDENTIFIER, scanner.nextToken());
		assertEquals(++errorCount, compiler.getErrorCount());
		assertEquals(TokenKind.TOK_EOS, scanner.nextToken());

		assertEquals(TokenKind.TOK_EOF, scanner.nextToken());
	}

	/**
	 * Tests {@link Scanner#nextToken()},
	 * {@link Scanner#getTokenValueIdentifier()} and indirectly
	 * {@link Keywords#checkForKeyword(String)}.
	 */
	public void testKeywordAndIdentifier() {
		Scanner scanner = new Scanner(compiler, "循环 Fork _Fork $Fork Fork_Fork Fork$Fork");

		int errorCount = 0;
		assertEquals(errorCount, compiler.getErrorCount());

		// For
		assertEquals(TokenKind.TOK_FOR, scanner.nextToken());

		// Fork
		assertEquals(TokenKind.TOK_IDENTIFIER, scanner.nextToken());
		assertEquals("Fork", scanner.getTokenValueIdentifier());

		// _Fork
		assertEquals(TokenKind.TOK_IDENTIFIER, scanner.nextToken());
		assertEquals(++errorCount, compiler.getErrorCount());
		assertEquals("Fork", scanner.getTokenValueIdentifier());

		// $Fork
		assertEquals(TokenKind.TOK_IDENTIFIER, scanner.nextToken());
		assertEquals(++errorCount, compiler.getErrorCount());
		assertEquals("Fork", scanner.getTokenValueIdentifier());

		// Fork_Fork
		assertEquals(TokenKind.TOK_IDENTIFIER, scanner.nextToken());
		assertEquals("Fork_Fork", scanner.getTokenValueIdentifier());

		// Fork$Fork
		assertEquals(TokenKind.TOK_IDENTIFIER, scanner.nextToken());
		assertEquals("Fork", scanner.getTokenValueIdentifier());
		assertEquals(TokenKind.TOK_IDENTIFIER, scanner.nextToken());
		assertEquals(++errorCount, compiler.getErrorCount());
		assertEquals("Fork", scanner.getTokenValueIdentifier());

		assertEquals(TokenKind.TOK_EOF, scanner.nextToken());
		assertEquals(errorCount, compiler.getErrorCount());
	}

	/**
	 * Tests {@link Scanner#nextToken()} and
	 * {@link Scanner#getTokenValueString()}.
	 */
	public void testStringLiteral() {
		Scanner scanner = new Scanner(compiler, "\"stringconstant\"  \"\n  \"");

		int errorCount = 0;
		assertEquals(compiler.getErrorCount(), errorCount);

		// "stringconstant"
		assertEquals(TokenKind.TOK_STRINGCONSTANT, scanner.nextToken());
		assertEquals("stringconstant", scanner.getTokenValueString());

		// "\n
		assertEquals(TokenKind.TOK_STRINGCONSTANT, scanner.nextToken());
		assertEquals(++errorCount, compiler.getErrorCount());

		// "<eof>
		assertEquals(TokenKind.TOK_EOF, scanner.nextToken());
		assertEquals(++errorCount, compiler.getErrorCount());
	}

	public void testStringLiteralWithUnicodeEscapeSequences() {
		Scanner scanner = new Scanner(compiler,
				"\"\\u0000\" \"\\u1234\" \"\\uFFFF\" \"\\uFFFG\" \"\\uFFF\uFFFF\" \"\\u000");

		int errorCount = 0;
		assertEquals(compiler.getErrorCount(), errorCount);

		// "\\u0000"
		assertEquals(TokenKind.TOK_STRINGCONSTANT, scanner.nextToken());
		assertEquals("\u0000", scanner.getTokenValueString());

		// "\\u1234"
		assertEquals(TokenKind.TOK_STRINGCONSTANT, scanner.nextToken());
		assertEquals("\u1234", scanner.getTokenValueString());

		// "\\uFFFF"
		assertEquals(TokenKind.TOK_STRINGCONSTANT, scanner.nextToken());
		assertEquals("\uFFFF", scanner.getTokenValueString());

		// "\\uFFFG"
		assertEquals(TokenKind.TOK_STRINGCONSTANT, scanner.nextToken());
		assertEquals(++errorCount, compiler.getErrorCount());

		// "\\uFFF\uFFFF"
		assertEquals(TokenKind.TOK_STRINGCONSTANT, scanner.nextToken());
		assertEquals(++errorCount, compiler.getErrorCount());

		// "\\u000"
		assertEquals(TokenKind.TOK_STRINGCONSTANT, scanner.nextToken());
		assertEquals(++errorCount, compiler.getErrorCount());

		assertEquals(scanner.nextToken(), TokenKind.TOK_EOF);
		assertEquals(errorCount, compiler.getErrorCount());
	}

	/**
	 * Tests {@link Scanner#nextToken()} and
	 * {@link Scanner#getTokenValueBoolean()}.
	 */
	public void testBooleanLiteral() {
		Scanner scanner = new Scanner(compiler, "真 假");

		// True
		assertEquals(TokenKind.TOK_BOOLEANCONSTANT, scanner.nextToken());
		assertEquals(true, scanner.getTokenValueBoolean());

		// False
		assertEquals(TokenKind.TOK_BOOLEANCONSTANT, scanner.nextToken());
		assertEquals(false, scanner.getTokenValueBoolean());

		assertEquals(TokenKind.TOK_EOF, scanner.nextToken());
		assertEquals(0, compiler.getErrorCount());
	}

	/**
	 * 验证百分号由扫描器直接识别为求模运算符，原“求模”文字不再作为关键字。
	 * xhwsd@qq.com 2026-9-4
	 */
	public void testModuloToken() {
		Scanner scanner = new Scanner(compiler, "% 求模");

		assertEquals(TokenKind.TOK_MOD, scanner.nextToken());
		assertEquals(TokenKind.TOK_IDENTIFIER, scanner.nextToken());
		assertEquals("求模", scanner.getTokenValueIdentifier());
		assertEquals(TokenKind.TOK_EOF, scanner.nextToken());
		assertEquals(0, compiler.getErrorCount());
	}

	/**
	 * Tests {@link Scanner#nextToken()} and
	 * {@link Scanner#getTokenValueNumber()}.
	 */
	public void testDecimalNumberLiteral() {
		Scanner scanner = new Scanner(compiler, "00 0 123456789012345678901234567890 +1 -1");

		int errorCount = 0;
		assertEquals(errorCount, compiler.getErrorCount());

		// 00
		assertEquals(TokenKind.TOK_NUMERICCONSTANT, scanner.nextToken());
		assertEquals(++errorCount, compiler.getErrorCount());

		// 0
		assertEquals(TokenKind.TOK_NUMERICCONSTANT, scanner.nextToken());
		assertEquals("0", scanner.getTokenValueNumber().toPlainString());

		// 123456789012345678901234567890
		assertEquals(TokenKind.TOK_NUMERICCONSTANT, scanner.nextToken());
		assertEquals("123456789012345678901234567890", scanner.getTokenValueNumber().toPlainString());

		// +1
		assertEquals(TokenKind.TOK_PLUS, scanner.nextToken());
		assertEquals(TokenKind.TOK_NUMERICCONSTANT, scanner.nextToken());
		assertEquals("1", scanner.getTokenValueNumber().toPlainString());

		// -1
		assertEquals(TokenKind.TOK_MINUS, scanner.nextToken());
		assertEquals(TokenKind.TOK_NUMERICCONSTANT, scanner.nextToken());
		assertEquals("1", scanner.getTokenValueNumber().toPlainString());

		assertEquals(TokenKind.TOK_EOF, scanner.nextToken());
		assertEquals(errorCount, compiler.getErrorCount());
	}

	/**
	 * Tests {@link Scanner#nextToken()} and
	 * {@link Scanner#getTokenValueNumber()}.
	 */
	public void testHexNumberToken() {
		Scanner scanner = new Scanner(compiler, "&H0 &H01234567890ABCDEF &HG &H");

		int errorCount = 0;
		assertEquals(errorCount, compiler.getErrorCount());

		// &H0
		assertEquals(TokenKind.TOK_NUMERICCONSTANT, scanner.nextToken());
		assertEquals("0", scanner.getTokenValueNumber().toPlainString());

		// &H01234567890ABCDEF
		assertEquals(TokenKind.TOK_NUMERICCONSTANT, scanner.nextToken());
		assertEquals("1311768467294899695", scanner.getTokenValueNumber().toPlainString());

		// &HG
		assertEquals(TokenKind.TOK_NUMERICCONSTANT, scanner.nextToken());
		assertEquals(++errorCount, compiler.getErrorCount());
		assertEquals(TokenKind.TOK_IDENTIFIER, scanner.nextToken());
		assertEquals("G", scanner.getTokenValueIdentifier());

		// &H<eof>
		assertEquals(TokenKind.TOK_NUMERICCONSTANT, scanner.nextToken());
		assertEquals(++errorCount, compiler.getErrorCount());

		assertEquals(TokenKind.TOK_EOF, scanner.nextToken());
		assertEquals(errorCount, compiler.getErrorCount());
	}

	/**
	 * Tests {@link Scanner#nextToken()} and
	 * {@link Scanner#getTokenValueNumber()}.
	 */
	public void testFloatingPointNumberToken() {
		// TODO: Floating point scanning needs still to be finalized.
	}

	/**
	 * Tests {@link Scanner#nextToken()}, {@link Scanner#getOffset(long)},
	 * {@link Scanner#getLine(long)} and {@link Scanner#getColumn(long)}.
	 */
	public void testTokenPosition() {
		// Offset                                0 1 234567
		// Line                                  1 2 3
		// Column                                    012345
		Scanner scanner = new Scanner(compiler, "\n\n     循环");

		assertEquals(TokenKind.TOK_EOS, scanner.nextToken());
		assertEquals(TokenKind.TOK_EOS, scanner.nextToken());
		assertEquals(TokenKind.TOK_FOR, scanner.nextToken());

		long startPosition = scanner.getTokenStartPosition();
		assertEquals(7, Scanner.getOffset(startPosition));
		assertEquals(3, Scanner.getLine(startPosition));
		assertEquals(5, Scanner.getColumn(startPosition));
	}
}

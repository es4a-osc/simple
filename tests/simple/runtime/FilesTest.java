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

package simple.runtime;

import simple.runtime.errors.文件已存在错误;
import simple.runtime.errors.文件未存在错误;
import simple.runtime.errors.文件读写错误;
import simple.runtime.errors.未知文件句柄错误;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

/**
 * Tests for {@link 文件操作}.
 *
 * @author Herbert Czymontek
 */
public class FilesTest extends TestCase {

	private File tmpDir;

	public FilesTest(String testName) {
		super(testName);

		// xhwsd@qq.com 2021-5-22 解决在Win下自动化测试失败
		if (!System.getProperty("os.name").startsWith("Windows")) {
			文件操作.initialize(new File("/"));
		}
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		tmpDir = createTempDir();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		deleteDirectoryContents(tmpDir);
		tmpDir.delete();
	}

	/**
	 * Tests {@link 文件操作#重命名文件(String, String)}.
	 */
	public void testRename() throws IOException {

		File source = new File(tmpDir, "testRenameSource");
		source.createNewFile();

		File target = new File(tmpDir, "testRenameTarget");

		// Source is null
		try {
			文件操作.重命名文件(null, target.getAbsolutePath());
			fail();
		} catch (NullPointerException expected) {
		}

		// Target is null
		try {
			文件操作.重命名文件(source.getAbsolutePath(), null);
			fail();
		} catch (NullPointerException expected) {
		}

		// Source does not exist
		try {
			文件操作.重命名文件("whatever", target.getAbsolutePath());
			fail();
		} catch (文件未存在错误 expected) {
		}

		// Target exists
		File existingTarget = new File(tmpDir, "testRenameExistingTarget");
		existingTarget.createNewFile();

		try {
			文件操作.重命名文件(source.getAbsolutePath(), existingTarget.getAbsolutePath());
			fail();
		} catch (文件已存在错误 expected) {
		}

		// Rename with both names the same
		文件操作.重命名文件(source.getAbsolutePath(), source.getAbsolutePath());

		// Rename
		文件操作.重命名文件(source.getAbsolutePath(), target.getAbsolutePath());
	}

	/**
	 * Tests {@link 文件操作#删除普通文件(String)}.
	 */
	public void testDelete() throws IOException {
		// Name is null
		try {
			文件操作.删除普通文件(null);
			fail();
		} catch (NullPointerException expected) {
		}

		// File does not exist
		try {
			文件操作.删除普通文件("whatever");
			fail();
		} catch (文件未存在错误 expected) {
		}

		// File is directory
		File directory = new File(tmpDir, "testDeleteDirectory");
		directory.mkdir();

		try {
			文件操作.删除普通文件(directory.getAbsolutePath());
			fail();
		} catch (文件读写错误 expected) {
		}

		// Delete
		File file = new File(tmpDir, "testDeleteFile");
		file.createNewFile();

		文件操作.删除普通文件(file.getAbsolutePath());
	}

	/**
	 * Tests {@link 文件操作#创建单级目录(String)}.
	 */
	public void testMkdir() throws IOException {
		// Name is null
		try {
			文件操作.创建单级目录(null);
			fail();
		} catch (NullPointerException expected) {
		}

		// Directory already exists
		File existingDirectory = new File(tmpDir, "testMkdirExistingDirectory");
		existingDirectory.mkdir();

		try {
			文件操作.创建单级目录(existingDirectory.getAbsolutePath());
			fail();
		} catch (文件已存在错误 expected) {
		}

		// File with same name exists
		File existingFile = new File(tmpDir, "testMkdirExistingFile");
		existingFile.createNewFile();

		try {
			文件操作.创建单级目录(existingFile.getAbsolutePath());
			fail();
		} catch (文件已存在错误 expected) {
		}

		// Mkdir
		File directory = new File(tmpDir, "testMkdirDirectory");
		文件操作.创建单级目录(directory.getAbsolutePath());
	}

	/**
	 * Tests {@link 文件操作#删除空目录(String)}.
	 */
	public void testRmdir() throws IOException {
		// Name is null
		try {
			文件操作.删除空目录(null);
			fail();
		} catch (NullPointerException expected) {
		}

		// Directory does not exist
		try {
			文件操作.删除空目录("whatever");
			fail();
		} catch (文件未存在错误 expected) {
		}

		// File with same name exists
		File existingFile = new File(tmpDir, "testRmdirExistingFile");
		existingFile.createNewFile();
		try {
			文件操作.删除空目录(existingFile.getAbsolutePath());
			fail();
		} catch (文件读写错误 expected) {
		}

		// Rmdir
		File existingDirectory = new File(tmpDir, "testRmdirExistingDirectory");
		existingDirectory.mkdir();

		文件操作.删除空目录(existingDirectory.getAbsolutePath());
	}

	/**
	 * Tests {@link 文件操作#路径是否存在(String)}.
	 */
	public void testExists() throws IOException {
		// Name is null
		try {
			文件操作.路径是否存在(null);
			fail();
		} catch (NullPointerException expected) {
		}

		// File/directory does not exist
		assertFalse(文件操作.路径是否存在("whatever"));

		// File exists
		File existingFile = new File(tmpDir, "testExistsExistingFile");
		existingFile.createNewFile();

		assertTrue(文件操作.路径是否存在(existingFile.getAbsolutePath()));

		// Directory exists
		File existingDirectory = new File(tmpDir, "testExistsExistingDirectory");
		existingDirectory.mkdir();

		assertTrue(文件操作.路径是否存在(existingDirectory.getAbsolutePath()));
	}

	/**
	 * Tests {@link 文件操作#路径是否为目录(String)}.
	 */
	public void testIsDirectory() throws IOException {
		// Name is null
		try {
			文件操作.路径是否为目录(null);
			fail();
		} catch (NullPointerException expected) {
		}

		// File/directory does not exist
		try {
			文件操作.路径是否为目录("whatever");
			fail();
		} catch (文件未存在错误 expected) {
		}

		// Is directory
		File existingDirectory = new File(tmpDir, "testIsDirectoryExistingDirectory");
		existingDirectory.mkdir();

		assertTrue(文件操作.路径是否为目录(existingDirectory.getAbsolutePath()));

		// Is file
		File existingFile = new File(tmpDir, "testIsDirectoryExistingFile");
		existingFile.createNewFile();

		assertFalse(文件操作.路径是否为目录(existingFile.getAbsolutePath()));
	}

	/**
	 * Tests {@link 文件操作#打开文件(String)}.
	 */
	public void testOpen() throws IOException {
		// Name is null
		try {
			文件操作.打开文件(null);
			fail();
		} catch (NullPointerException expected) {
		}

		// Is directory
		File existingDirectory = new File(tmpDir, "testIsDirectoryExistingDirectory");
		existingDirectory.mkdir();

		try {
			文件操作.打开文件(existingDirectory.getAbsolutePath());
			fail();
		} catch (文件读写错误 expected) {
		}

		// Does not exist
		File file = new File(tmpDir, "testOpenFile");

		int handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.关闭文件(handle);

		assertTrue(file.exists());

		// Exists
		File existingFile = new File(tmpDir, "testOpenExistingFile");
		existingFile.createNewFile();

		handle = 文件操作.打开文件(existingFile.getAbsolutePath());
		文件操作.关闭文件(handle);
	}

	/**
	 * Tests {@link 文件操作#关闭文件(int)}.
	 */
	public void testClose() {
		// Unknown handle and Close
		File file = new File(tmpDir, "testCloseFile");

		int handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.关闭文件(handle);

		try {
			文件操作.关闭文件(handle);
			fail();
		} catch (未知文件句柄错误 expected) {
		}
	}

	/**
	 * Tests {@link 文件操作#是否到文件尾(int)}.
	 */
	public void testEof() {
		// Unknown handle
		File file = new File(tmpDir, "testEofFile");

		int handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.写入长整数(handle, 0x123456789ABCDEFL);
		文件操作.关闭文件(handle);

		try {
			文件操作.是否到文件尾(handle);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Is not eof
		handle = 文件操作.打开文件(file.getAbsolutePath());
		assertFalse(文件操作.是否到文件尾(handle));

		// Is eof
		文件操作.读入长整数(handle);
		assertTrue(文件操作.是否到文件尾(handle));

		// Write some more
		文件操作.写入长整数(handle, 0x123456789ABCDEFL);
		assertTrue(文件操作.是否到文件尾(handle));

		// Seek back
		文件操作.移动文件指针(handle, 4);
		assertFalse(文件操作.是否到文件尾(handle));

		文件操作.关闭文件(handle);
	}

	/**
	 * Tests {@link 文件操作#移动文件指针(int, long)}.
	 */
	public void testSeek() {
		// Unknown handle
		File file = new File(tmpDir, "testSeekFile");

		int handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.写入长整数(handle, 0x123456789ABCDEFL);
		文件操作.写入长整数(handle, 0xFEDCBA987654321L);
		文件操作.关闭文件(handle);

		try {
			文件操作.移动文件指针(handle, 0);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Seek position before start of file (negative offset)
		handle = 文件操作.打开文件(file.getAbsolutePath());

		try {
			文件操作.移动文件指针(handle, -1);
			fail();
		} catch (文件读写错误 expected) {
		}

		// Seek position in the middle of file
		assertEquals(8, 文件操作.移动文件指针(handle, 8));
		assertEquals(0xFEDCBA987654321L, 文件操作.读入长整数(handle));

		// Seek position start of file
		assertEquals(0, 文件操作.移动文件指针(handle, 0));
		assertEquals(0x123456789ABCDEFL, 文件操作.读入长整数(handle));

		// Seek position end of file
		assertEquals(16, 文件操作.移动文件指针(handle, 16));
		assertTrue(文件操作.是否到文件尾(handle));

		// Seek position beyond end of file
		try {
			文件操作.移动文件指针(handle, Long.MAX_VALUE);
			fail();
		} catch (文件读写错误 expected) {
		}

		文件操作.关闭文件(handle);
	}

	/**
	 * Tests {@link 文件操作#取文件长度(int)}.
	 */
	public void testSize() {
		// Unknown handle
		File file = new File(tmpDir, "testSizeFile");

		int handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.关闭文件(handle);

		try {
			文件操作.取文件长度(handle);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Empty file size
		handle = 文件操作.打开文件(file.getAbsolutePath());

		assertEquals(0, 文件操作.取文件长度(handle));

		// File size
		文件操作.写入长整数(handle, 0x123456789ABCDEFL);

		assertEquals(8, 文件操作.取文件长度(handle));

		文件操作.关闭文件(handle);
	}

	/**
	 * Tests {@link 文件操作#写入文本(int, String)}.
	 */
	public void testWriteString() {
		// Unknown handle
		File file = new File(tmpDir, "testWriteStringFile");

		int handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.关闭文件(handle);

		try {
			文件操作.写入文本(handle, "foo");
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Write null
		handle = 文件操作.打开文件(file.getAbsolutePath());

		try {
			文件操作.写入文本(handle, null);
			fail();
		} catch (NullPointerException expected) {
		}

		// Write empty string
		文件操作.写入文本(handle, "");

		// Write
		文件操作.写入文本(handle, "foo");

		文件操作.移动文件指针(handle, 0);
		assertEquals("", 文件操作.读入文本(handle));
		assertEquals("foo", 文件操作.读入文本(handle));
		assertTrue(文件操作.是否到文件尾(handle));

		文件操作.关闭文件(handle);
	}

	/**
	 * Tests {@link 文件操作#读入文本(int)}.
	 */
	public void testReadString() {
		// Unknown handle
		File file = new File(tmpDir, "testReadStringFile");

		int handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.写入文本(handle, "");
		文件操作.写入文本(handle, "foo");
		文件操作.关闭文件(handle);

		try {
			文件操作.读入文本(handle);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Read
		handle = 文件操作.打开文件(file.getAbsolutePath());

		assertEquals("", 文件操作.读入文本(handle));
		assertEquals("foo", 文件操作.读入文本(handle));
		assertTrue(文件操作.是否到文件尾(handle));

		文件操作.关闭文件(handle);
	}

	/**
	 * Tests {@link 文件操作#写入逻辑值(int, boolean)}.
	 */
	public void testWriteBoolean() {
		// Unknown handle
		File file = new File(tmpDir, "testWriteBooleanFile");

		int handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.关闭文件(handle);

		try {
			文件操作.写入逻辑值(handle, true);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Write
		handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.写入逻辑值(handle, true);
		文件操作.写入逻辑值(handle, false);

		文件操作.移动文件指针(handle, 0);
		assertTrue(文件操作.读入逻辑值(handle));
		assertFalse(文件操作.读入逻辑值(handle));
		assertTrue(文件操作.是否到文件尾(handle));

		文件操作.关闭文件(handle);
	}

	/**
	 * Tests {@link 文件操作#读入逻辑值(int)}.
	 */
	public void testReadBoolean() {
		// Unknown handle
		File file = new File(tmpDir, "testReadBooleanFile");

		int handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.写入逻辑值(handle, true);
		文件操作.写入逻辑值(handle, false);
		文件操作.关闭文件(handle);

		try {
			文件操作.读入文本(handle);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Read
		handle = 文件操作.打开文件(file.getAbsolutePath());

		assertTrue(文件操作.读入逻辑值(handle));
		assertFalse(文件操作.读入逻辑值(handle));
		assertTrue(文件操作.是否到文件尾(handle));

		文件操作.关闭文件(handle);
	}

	/**
	 * Tests {@link 文件操作#写入字节(int, byte)}.
	 */
	public void testWriteByte() {
		// Unknown handle
		File file = new File(tmpDir, "testWriteByteFile");

		int handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.关闭文件(handle);

		try {
			文件操作.写入字节(handle, Byte.MAX_VALUE);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Write
		handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.写入字节(handle, Byte.MAX_VALUE);
		文件操作.写入字节(handle, Byte.MIN_VALUE);

		文件操作.移动文件指针(handle, 0);
		assertEquals(Byte.MAX_VALUE, 文件操作.读入字节(handle));
		assertEquals(Byte.MIN_VALUE, 文件操作.读入字节(handle));
		assertTrue(文件操作.是否到文件尾(handle));

		文件操作.关闭文件(handle);
	}

	/**
	 * Tests {@link 文件操作#读入字节(int)}.
	 */
	public void testReadByte() {
		// Unknown handle
		File file = new File(tmpDir, "testReadByteFile");

		int handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.写入字节(handle, Byte.MAX_VALUE);
		文件操作.写入字节(handle, Byte.MIN_VALUE);
		文件操作.关闭文件(handle);

		try {
			文件操作.读入字节(handle);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Read
		handle = 文件操作.打开文件(file.getAbsolutePath());

		assertEquals(Byte.MAX_VALUE, 文件操作.读入字节(handle));
		assertEquals(Byte.MIN_VALUE, 文件操作.读入字节(handle));
		assertTrue(文件操作.是否到文件尾(handle));

		文件操作.关闭文件(handle);
	}

	/**
	 * Tests {@link 文件操作#写入短整数(int, short)}.
	 */
	public void testWriteShort() {
		// Unknown handle
		File file = new File(tmpDir, "testWriteShortFile");

		int handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.关闭文件(handle);

		try {
			文件操作.写入短整数(handle, Short.MAX_VALUE);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Write
		handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.写入短整数(handle, Short.MAX_VALUE);
		文件操作.写入短整数(handle, Short.MIN_VALUE);

		文件操作.移动文件指针(handle, 0);
		assertEquals(Short.MAX_VALUE, 文件操作.读入短整数(handle));
		assertEquals(Short.MIN_VALUE, 文件操作.读入短整数(handle));
		assertTrue(文件操作.是否到文件尾(handle));

		文件操作.关闭文件(handle);
	}

	/**
	 * Tests {@link 文件操作#读入短整数(int)}.
	 */
	public void testReadShort() {
		// Unknown handle
		File file = new File(tmpDir, "testReadShortFile");

		int handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.写入短整数(handle, Short.MAX_VALUE);
		文件操作.写入短整数(handle, Short.MIN_VALUE);
		文件操作.关闭文件(handle);

		try {
			文件操作.读入短整数(handle);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Read
		handle = 文件操作.打开文件(file.getAbsolutePath());

		assertEquals(Short.MAX_VALUE, 文件操作.读入短整数(handle));
		assertEquals(Short.MIN_VALUE, 文件操作.读入短整数(handle));
		assertTrue(文件操作.是否到文件尾(handle));

		文件操作.关闭文件(handle);
	}

	/**
	 * Tests {@link 文件操作#写入整数(int, int)}.
	 */
	public void testWriteInteger() {
		// Unknown handle
		File file = new File(tmpDir, "testWriteIntegerFile");

		int handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.关闭文件(handle);

		try {
			文件操作.写入整数(handle, Integer.MAX_VALUE);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Write
		handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.写入整数(handle, Integer.MAX_VALUE);
		文件操作.写入整数(handle, Integer.MIN_VALUE);

		文件操作.移动文件指针(handle, 0);
		assertEquals(Integer.MAX_VALUE, 文件操作.读入整数(handle));
		assertEquals(Integer.MIN_VALUE, 文件操作.读入整数(handle));
		assertTrue(文件操作.是否到文件尾(handle));

		文件操作.关闭文件(handle);
	}

	/**
	 * Tests {@link 文件操作#读入整数(int)}.
	 */
	public void testReadInteger() {
		// Unknown handle
		File file = new File(tmpDir, "testReadIntegerFile");

		int handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.写入整数(handle, Integer.MAX_VALUE);
		文件操作.写入整数(handle, Integer.MIN_VALUE);
		文件操作.关闭文件(handle);

		try {
			文件操作.读入整数(handle);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Read
		handle = 文件操作.打开文件(file.getAbsolutePath());

		assertEquals(Integer.MAX_VALUE, 文件操作.读入整数(handle));
		assertEquals(Integer.MIN_VALUE, 文件操作.读入整数(handle));
		assertTrue(文件操作.是否到文件尾(handle));

		文件操作.关闭文件(handle);
	}

	/**
	 * Tests {@link 文件操作#写入长整数(int, long)}.
	 */
	public void testWriteLong() {
		// Unknown handle
		File file = new File(tmpDir, "testWriteLongFile");

		int handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.关闭文件(handle);

		try {
			文件操作.写入长整数(handle, Long.MAX_VALUE);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Write
		handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.写入长整数(handle, Long.MAX_VALUE);
		文件操作.写入长整数(handle, Long.MIN_VALUE);

		文件操作.移动文件指针(handle, 0);
		assertEquals(Long.MAX_VALUE, 文件操作.读入长整数(handle));
		assertEquals(Long.MIN_VALUE, 文件操作.读入长整数(handle));
		assertTrue(文件操作.是否到文件尾(handle));

		文件操作.关闭文件(handle);
	}

	/**
	 * Tests {@link 文件操作#读入长整数(int)}.
	 */
	public void testReadLong() {
		// Unknown handle
		File file = new File(tmpDir, "testReadLongFile");

		int handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.写入长整数(handle, Long.MAX_VALUE);
		文件操作.写入长整数(handle, Long.MIN_VALUE);
		文件操作.关闭文件(handle);

		try {
			文件操作.读入长整数(handle);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Read
		handle = 文件操作.打开文件(file.getAbsolutePath());

		assertEquals(Long.MAX_VALUE, 文件操作.读入长整数(handle));
		assertEquals(Long.MIN_VALUE, 文件操作.读入长整数(handle));
		assertTrue(文件操作.是否到文件尾(handle));

		文件操作.关闭文件(handle);
	}

	/**
	 * Tests {@link 文件操作#写入单精度小数(int, float)}.
	 */
	public void testWriteSingle() {
		// Unknown handle
		File file = new File(tmpDir, "testWriteSingleFile");

		int handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.关闭文件(handle);

		try {
			文件操作.写入单精度小数(handle, Float.MAX_VALUE);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Write
		handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.写入单精度小数(handle, Float.MAX_VALUE);
		文件操作.写入单精度小数(handle, Float.MIN_VALUE);

		文件操作.移动文件指针(handle, 0);
		assertEquals(Float.MAX_VALUE, 文件操作.读入单精度小数(handle));
		assertEquals(Float.MIN_VALUE, 文件操作.读入单精度小数(handle));
		assertTrue(文件操作.是否到文件尾(handle));

		文件操作.关闭文件(handle);
	}

	/**
	 * Tests {@link 文件操作#读入单精度小数(int)}.
	 */
	public void testReadSingle() {
		// Unknown handle
		File file = new File(tmpDir, "testReadSingleFile");

		int handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.写入单精度小数(handle, Float.MAX_VALUE);
		文件操作.写入单精度小数(handle, Float.MIN_VALUE);
		文件操作.关闭文件(handle);

		try {
			文件操作.读入单精度小数(handle);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Read
		handle = 文件操作.打开文件(file.getAbsolutePath());

		assertEquals(Float.MAX_VALUE, 文件操作.读入单精度小数(handle));
		assertEquals(Float.MIN_VALUE, 文件操作.读入单精度小数(handle));
		assertTrue(文件操作.是否到文件尾(handle));

		文件操作.关闭文件(handle);
	}

	/**
	 * Tests {@link 文件操作#写入双精度小数(int, double)}.
	 */
	public void testWriteDouble() {
		// Unknown handle
		File file = new File(tmpDir, "testWriteDoubleFile");

		int handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.关闭文件(handle);

		try {
			文件操作.写入双精度小数(handle, Double.MAX_VALUE);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Write
		handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.写入双精度小数(handle, Double.MAX_VALUE);
		文件操作.写入双精度小数(handle, Double.MIN_VALUE);

		文件操作.移动文件指针(handle, 0);
		assertEquals(Double.MAX_VALUE, 文件操作.读入双精度小数(handle));
		assertEquals(Double.MIN_VALUE, 文件操作.读入双精度小数(handle));
		assertTrue(文件操作.是否到文件尾(handle));

		文件操作.关闭文件(handle);
	}

	/**
	 * Tests {@link 文件操作#读入双精度小数(int)}.
	 */
	public void testReadDouble() {
		// Unknown handle
		File file = new File(tmpDir, "testReadDoubleFile");

		int handle = 文件操作.打开文件(file.getAbsolutePath());
		文件操作.写入双精度小数(handle, Double.MAX_VALUE);
		文件操作.写入双精度小数(handle, Double.MIN_VALUE);
		文件操作.关闭文件(handle);

		try {
			文件操作.读入双精度小数(handle);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Read
		handle = 文件操作.打开文件(file.getAbsolutePath());

		assertEquals(Double.MAX_VALUE, 文件操作.读入双精度小数(handle));
		assertEquals(Double.MIN_VALUE, 文件操作.读入双精度小数(handle));
		assertTrue(文件操作.是否到文件尾(handle));

		文件操作.关闭文件(handle);
	}

	private static File createTempDir() {
		String baseName = System.getProperty("java.io.tmpdir") + File.separator + System.currentTimeMillis() + '-';
		for (int attempts = 0; attempts < 1000; attempts++) {
			File tempDir = new File(baseName + attempts);
			if (tempDir.mkdir()) {
				return tempDir;
			}
		}

		throw new IllegalStateException("Cannot create temp directory with base name " + baseName);
	}

	private static void deleteDirectoryContents(File dir) throws IOException {
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException("directory expected");
		}

		File[] files = dir.listFiles();
		if (files == null) {
			throw new IOException("cannot get directory listings");
		}
		for (File file : files) {
			if (file.isDirectory()) {
				deleteDirectoryContents(file);
			}
			file.delete();
		}
	}
}

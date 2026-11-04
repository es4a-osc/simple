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

import java.util.Arrays;

import java.nio.file.Files;

import java.lang.reflect.Field;

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
 * @author 树先生 xhwsd@qq.com
 */
public class FilesTest extends TestCase {

	private File tmpDir;
	private File root;
	private File previousRoot;

	/**
	 * 创建指定名称的运行库用例。
	 *
	 * @param testName 用例名称
	 */
	public FilesTest(String testName) {
		super(testName);

	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Field field = 文件操作.class.getDeclaredField("rootDirectory");
		field.setAccessible(true);
		previousRoot = (File) field.get(null);

		// 每个用例使用独立临时根目录，不继承上一用例的全局配置。
		root = Files.createTempDirectory("es4a-files-test-").toFile();
		tmpDir = root;
		文件操作.initialize(root);
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			deleteDirectoryContents(root);
			assertTrue("测试根目录未能清理", root.delete());
		} finally {
			文件操作.initialize(previousRoot);
			super.tearDown();
		}
	}

	/**
	 * 设置应用私有目录后，旧接口仍须按绝对路径操作真实文件。
	 */
	public void testAbsolutePathsWithInitializedApplicationRoot() {
		// 先用目录接口建立目标，不能把绝对路径拼接成根目录下的子路径。
		File directory = new File(root, "absolute-path");
		文件操作.创建单级目录(directory.getAbsolutePath());
		assertTrue(directory.isDirectory());
		File original = new File(directory, "source.bin");
		File renamed = new File(directory, "renamed.bin");

		// 句柄、存在判断和改名均通过公开接口执行。
		int handle = 文件操作.打开文件(original.getAbsolutePath());
		try {
			文件操作.写入字节(handle, (byte) 42);
		} finally {
			文件操作.关闭文件(handle);
		}
		assertTrue(文件操作.路径是否存在(original.getAbsolutePath()));
		assertEquals(1L, original.length());
		文件操作.重命名文件(original.getAbsolutePath(), renamed.getAbsolutePath());
		assertFalse(original.exists());
		assertTrue(renamed.isFile());

		// 删除接口也必须命中同一物理路径。
		文件操作.删除普通文件(renamed.getAbsolutePath());
		assertFalse(renamed.exists());
		文件操作.删除目录(directory.getAbsolutePath());
		assertFalse(directory.exists());
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



	/**
	 * 清理当前用例创建的临时目录，并检查实际删除结果。
	 *
	 * @param dir 当前测试目录
	 * @throws IOException 目录无法读取
	 */
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
			assertTrue("测试文件未能清理：" + file, file.delete());
		}
	}

	/**

	 * 验证默认 UTF-8、指定 GBK、空文本及覆盖写入。

	 */
	public void testTextRoundTripAndOverwrite() {
		String text = "中文 Simple\r\n第二行";
		assertTrue(文件操作.写出文本文件("中文.txt", text));
		assertEquals(text, 文件操作.读入文本文件("中文.txt"));
		assertTrue(文件操作.写出文本文件("gbk.txt", text, "GBK"));
		assertEquals(text, 文件操作.读入文本文件("gbk.txt", "GBK"));

		文件操作.写出文本文件("中文.txt", "短");
		assertEquals("短", 文件操作.读入文本文件("中文.txt"));
		assertEquals(3L, 文件操作.取文件尺寸("中文.txt"));
		文件操作.写出文本文件("中文.txt", "");
		assertEquals("", 文件操作.读入文本文件("中文.txt"));
		assertEquals(0L, 文件操作.取文件尺寸("中文.txt"));
	}

	/**

	 * 数据超过一次缓冲区，复制覆盖后逐字节检查尾部没有残留。

	 */
	public void testByteRoundTripAndChunkedCopy() {
		byte[] data = new byte[20003];
		for (int index = 0; index < data.length; index++) {
			data[index] = (byte) index;
		}
		文件操作.写出字节文件("source.bin", data);
		assertTrue(Arrays.equals(data, 文件操作.读入字节文件("source.bin")));
		文件操作.写出字节文件("target.bin", new byte[25000]);
		assertTrue(文件操作.复制文件("source.bin", "target.bin"));
		assertTrue(Arrays.equals(data, 文件操作.读入字节文件("target.bin")));
		assertEquals(data.length, 文件操作.取文件尺寸("target.bin"));

		文件操作.写出字节文件("source.bin", new byte[0]);
		assertEquals(0, 文件操作.读入字节文件("source.bin").length);
		文件操作.复制文件("source.bin", "target.bin");
		assertEquals(0L, 文件操作.取文件尺寸("target.bin"));
	}

	/**

	 * 创建不覆盖，改名冲突和复制自身均保留源文件。

	 */
	public void testFileLifecycleAndConflicts() {
		assertTrue(文件操作.创建文件("first.bin"));
		assertFalse(文件操作.创建文件("first.bin"));
		文件操作.写出文本文件("first.bin", "源内容");
		文件操作.写出文本文件("second.bin", "目标内容");
		expectFailure(文件已存在错误.class, new Runnable() {
			@Override
			public void run() {
				文件操作.修改文件名("first.bin", "second.bin");
			}
		});
		expectFailure(IllegalArgumentException.class, new Runnable() {
			@Override
			public void run() {
				文件操作.复制文件("first.bin", "./first.bin");
			}
		});
		assertEquals("源内容", 文件操作.读入文本文件("first.bin"));
		assertEquals("目标内容", 文件操作.读入文本文件("second.bin"));

		assertTrue(文件操作.修改文件名("first.bin", "./first.bin"));
		assertTrue(文件操作.修改文件名("first.bin", "renamed.bin"));
		assertFalse(文件操作.文件是否存在("first.bin"));
		assertTrue(文件操作.文件是否存在("renamed.bin"));
		assertTrue(文件操作.删除文件("renamed.bin"));
		assertFalse(文件操作.删除文件("renamed.bin"));
	}

	/**

	 * 目录枚举只返回下一级对应类型，结果为有序绝对路径。

	 */
	public void testDirectoryListsAndInformation() {
		文件操作.创建目录("work/z/sub");
		文件操作.创建目录("work/a");
		文件操作.写出字节文件("work/z.bin", new byte[]{1, 2, 3});
		文件操作.写出字节文件("work/a.bin", new byte[]{4, 5});
		文件操作.写出字节文件("work/z/sub/inner.bin", new byte[]{6});
		String base = new File(root, "work").getAbsolutePath() + File.separator;
		assertTrue(Arrays.equals(new String[]{base + "a.bin", base + "z.bin"}, 文件操作.取文件列表("work")));
		assertTrue(Arrays.equals(new String[]{base + "a", base + "z"}, 文件操作.取子目录("work")));
		assertEquals(6L, 文件操作.取文件尺寸("work"));
		assertTrue(文件操作.取文件修改时间("work/a.bin").matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
		assertFalse(文件操作.是否为隐藏文件("work/a.bin"));
		assertEquals(0, 文件操作.取文件列表("work/a").length);
		assertEquals(0, 文件操作.取子目录("work/a").length);
	}

	/**

	 * 空目录接口不递归，递归接口删除目录后再次调用返回假。

	 */
	public void testEmptyAndRecursiveDirectoryRemoval() {
		文件操作.创建单级目录("empty");
		文件操作.删除空目录("empty");
		assertFalse(文件操作.路径是否存在("empty"));
		文件操作.创建目录("work/a/b");
		文件操作.写出文本文件("work/a/b/file.txt", "内容");
		expectFailure(文件读写错误.class, new Runnable() {
			@Override
			public void run() {
				文件操作.删除空目录("work");
			}
		});
		assertTrue(文件操作.删除目录("work"));
		assertFalse(文件操作.删除目录("work"));
		assertFalse(文件操作.文件是否存在("work"));
	}

	/**

	 * 覆盖五种 BOM 及短文件，不把无标记内容误当成已识别编码。

	 */
	public void testBomRecognition() {
		byte[][] prefixes = {
			{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF},
			{(byte) 0xFF, (byte) 0xFE},
			{(byte) 0xFE, (byte) 0xFF},
			{0, 0, (byte) 0xFE, (byte) 0xFF},
			{(byte) 0xFF, (byte) 0xFE, 0, 0}
		};
		String[] names = {"UTF-8", "UTF-16LE", "UTF-16BE", "UTF-32BE", "UTF-32LE"};
		for (int index = 0; index < prefixes.length; index++) {
			文件操作.写出字节文件("bom.bin", prefixes[index]);
			assertEquals(names[index], 文件操作.取文件编码("bom.bin"));
		}
		for (byte[] bytes : new byte[][]{new byte[0], {1}, {(byte) 0xEF, (byte) 0xBB}, {65, 66, 67}}) {
			文件操作.写出字节文件("bom.bin", bytes);
			assertEquals("", 文件操作.取文件编码("bom.bin"));
		}
	}

	/**

	 * 参数错误应先失败，不能截断已有目标或把读取失败当成空文件。

	 */
	public void testInvalidArgumentsPreserveData() {
		文件操作.写出文本文件("guard.txt", "保留原文");
		expectFailure(IllegalArgumentException.class, new Runnable() {
			@Override
			public void run() {
				文件操作.写出文本文件("guard.txt", "覆盖", "NO-SUCH-CHARSET");
			}
		});
		expectFailure(IllegalArgumentException.class, new Runnable() {
			@Override
			public void run() {
				文件操作.写出文本文件("guard.txt", null);
			}
		});
		expectFailure(IllegalArgumentException.class, new Runnable() {
			@Override
			public void run() {
				文件操作.写出字节文件("guard.txt", null);
			}
		});
		expectFailure(IllegalArgumentException.class, new Runnable() {
			@Override
			public void run() {
				文件操作.文件是否存在("content://example/document/1");
			}
		});
		expectFailure(IllegalArgumentException.class, new Runnable() {
			@Override
			public void run() {
				文件操作.创建文件("");
			}
		});
		expectFailure(IllegalArgumentException.class, new Runnable() {
			@Override
			public void run() {
				文件操作.创建目录(null);
			}
		});
		expectFailure(文件未存在错误.class, new Runnable() {
			@Override
			public void run() {
				文件操作.读入文本文件("missing.txt");
			}
		});
		assertEquals("保留原文", 文件操作.读入文本文件("guard.txt"));
	}

	/**

	 * 文件、目录类型错误及缺失父目录应报告失败。

	 */
	public void testPathTypeErrors() {
		文件操作.创建目录("work");
		文件操作.写出文本文件("plain.txt", "文件");
		expectFailure(文件读写错误.class, new Runnable() {
			@Override
			public void run() {
				文件操作.读入字节文件("work");
			}
		});
		expectFailure(文件读写错误.class, new Runnable() {
			@Override
			public void run() {
				文件操作.删除文件("work");
			}
		});
		expectFailure(文件读写错误.class, new Runnable() {
			@Override
			public void run() {
				文件操作.删除目录("plain.txt");
			}
		});
		expectFailure(文件读写错误.class, new Runnable() {
			@Override
			public void run() {
				文件操作.创建目录("plain.txt");
			}
		});
		expectFailure(文件读写错误.class, new Runnable() {
			@Override
			public void run() {
				文件操作.创建文件("work");
			}
		});
		expectFailure(文件读写错误.class, new Runnable() {
			@Override
			public void run() {
				文件操作.取文件列表("plain.txt");
			}
		});
		expectFailure(文件读写错误.class, new Runnable() {
			@Override
			public void run() {
				文件操作.写出文本文件("missing/child.txt", "内容");
			}
		});
		assertTrue(文件操作.是否为目录("work"));
		assertFalse(文件操作.是否为目录("missing"));
		assertTrue(文件操作.路径是否为目录("work"));
		expectFailure(文件未存在错误.class, new Runnable() {
			@Override
			public void run() {
				文件操作.路径是否为目录("missing");
			}
		});
	}

	/**

	 * 相对路径、绝对路径指向同一文件，根目录及祖先不能递归清理。

	 */
	public void testRelativeAndAbsolutePaths() {
		文件操作.写出文本文件("relative.txt", "相对路径");
		File absolute = new File(root, "relative.txt");
		assertEquals("相对路径", 文件操作.读入文本文件(absolute.getAbsolutePath()));
		文件操作.写出文本文件(absolute.getAbsolutePath(), "绝对路径");
		assertEquals("绝对路径", 文件操作.读入文本文件("relative.txt"));
		assertTrue(文件操作.文件是否存在(absolute.getAbsolutePath()));
	}

	/**

	 * 删除保护触发后，已有文件和测试根目录必须保持原样。

	 */
	public void testRootDirectoryProtection() {
		文件操作.写出文本文件("guard.txt", "保留");
		expectFailure(IllegalArgumentException.class, new Runnable() {
			@Override
			public void run() {
				文件操作.删除目录(".");
			}
		});
		expectFailure(IllegalArgumentException.class, new Runnable() {
			@Override
			public void run() {
				文件操作.删除目录(root.getParentFile().getAbsolutePath());
			}
		});
		File top = root;
		while (top.getParentFile() != null) {
			top = top.getParentFile();
		}
		final String topPath = top.getAbsolutePath();
		expectFailure(IllegalArgumentException.class, new Runnable() {
			@Override
			public void run() {
				文件操作.删除目录(topPath);
			}
		});
		assertTrue(root.isDirectory());
		assertEquals("保留", 文件操作.读入文本文件("guard.txt"));
	}

	/**
	 * 默认、相对、绝对、普通文件和中文目录均查询所在卷，而不是文件尺寸。
	 *
	 * @throws Exception 创建测试文件失败。
	 */
	public void testStorageTotalsForRelativeAbsoluteAndFilePaths() throws Exception {
		文件操作.创建目录("空间查询/子目录");
		文件操作.写出文本文件("空间查询/data.txt", "保留内容");
		long expected = root.getTotalSpace();

		assertTrue(expected > 0);
		assertEquals(expected, 文件操作.取存储总空间());
		assertEquals(expected, 文件操作.取存储总空间("."));
		assertEquals(expected, 文件操作.取存储总空间(root.getAbsolutePath()));
		assertEquals(expected, 文件操作.取存储总空间("空间查询/子目录"));
		assertEquals(expected, 文件操作.取存储总空间("空间查询/data.txt"));
		assertEquals(expected, 文件操作.取存储总空间(new File(root, "空间查询/data.txt").getAbsolutePath()));
	}

	/**

	 * 可用空间随系统活动变化，只检查有效范围，不断言每次查询结果完全相同。

	 */
	public void testStorageAvailableSpaceBounds() {
		文件操作.写出文本文件("space.txt", "内容");
		long total = 文件操作.取存储总空间();

		for (String path : new String[] { ".", root.getAbsolutePath(), "space.txt" }) {
			long available = 文件操作.取存储可用空间(path);
			assertTrue(available >= 0);
			assertTrue(available <= total);
		}
		long available = 文件操作.取存储可用空间();
		assertTrue(available >= 0);
		assertTrue(available <= total);
	}

	/**

	 * 无效或缺失路径沿用路径接口的错误约定，不以容量 0 掩盖缺失路径。

	 */
	public void testStorageSpaceRejectsInvalidAndMissingPaths() {
		for (String path : new String[] { null, "", "  ", "content://example/document/1" }) {
			expectFailure(IllegalArgumentException.class, new Runnable() {
				@Override
				public void run() {
					文件操作.取存储总空间(path);
				}
			});
			expectFailure(IllegalArgumentException.class, new Runnable() {
				@Override
				public void run() {
					文件操作.取存储可用空间(path);
				}
			});
		}
		expectFailure(文件未存在错误.class, new Runnable() {
			@Override
			public void run() {
				文件操作.取存储总空间("missing-space");
			}
		});
		expectFailure(文件未存在错误.class, new Runnable() {
			@Override
			public void run() {
				文件操作.取存储可用空间("missing-space");
			}
		});
		assertFalse(文件操作.文件是否存在("missing-space"));
	}

	/**

	 * 查询不创建探测文件、不改写内容，也不改变当前目录项目。

	 */
	public void testStorageQueriesDoNotChangeFiles() {
		文件操作.写出文本文件("guard-space.txt", "查询不能改写");
		String[] before = root.list();

		文件操作.取存储总空间("guard-space.txt");
		文件操作.取存储可用空间("guard-space.txt");
		文件操作.取存储总空间();
		文件操作.取存储可用空间();

		assertEquals("查询不能改写", 文件操作.读入文本文件("guard-space.txt"));
		assertTrue(Arrays.equals(before, root.list()));
	}

	/**

	 * 未初始化的默认查询不能意外改为查询宿主进程工作目录。

	 */
	public void testStorageQueriesRequireInitializedDefaultRoot() {
		文件操作.initialize(null);
		try {
			expectFailure(IllegalStateException.class, new Runnable() {
				@Override
				public void run() {
					文件操作.取存储总空间();
				}
			});
			expectFailure(IllegalStateException.class, new Runnable() {
				@Override
				public void run() {
					文件操作.取存储可用空间();
				}
			});

			// 显式绝对路径不依赖应用相对路径基准。
			assertEquals(root.getTotalSpace(), 文件操作.取存储总空间(root.getAbsolutePath()));
		} finally {
			文件操作.initialize(root);
		}
	}

	/**
	 * 检查预期错误类型，意外错误继续向测试运行器报告。
	 *
	 * @param expected 预期的错误类型。
	 * @param action 应失败的操作。
	 */
	private void expectFailure(Class<? extends RuntimeException> expected, Runnable action) {
		try {
			action.run();
		} catch (RuntimeException exception) {
			if (!expected.isInstance(exception)) {
				throw exception;
			}
			return;
		}
		fail("应抛出错误：" + expected.getSimpleName());
	}
}

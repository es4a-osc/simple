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


import simple.runtime.文件操作;
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
	 * Tests {@link 文件操作#Rename(String, String)}.
	 */
	public void testRename() throws IOException {

		File source = new File(tmpDir, "testRenameSource");
		source.createNewFile();

		File target = new File(tmpDir, "testRenameTarget");

		// Source is null
		try {
			文件操作.Rename(null, target.getAbsolutePath());
			fail();
		} catch (NullPointerException expected) {
		}

		// Target is null
		try {
			文件操作.Rename(source.getAbsolutePath(), null);
			fail();
		} catch (NullPointerException expected) {
		}

		// Source does not exist
		try {
			文件操作.Rename("whatever", target.getAbsolutePath());
			fail();
		} catch (文件未存在错误 expected) {
		}

		// Target exists
		File existingTarget = new File(tmpDir, "testRenameExistingTarget");
		existingTarget.createNewFile();

		try {
			文件操作.Rename(source.getAbsolutePath(), existingTarget.getAbsolutePath());
			fail();
		} catch (文件已存在错误 expected) {
		}

		// Rename with both names the same
		文件操作.Rename(source.getAbsolutePath(), source.getAbsolutePath());

		// Rename
		文件操作.Rename(source.getAbsolutePath(), target.getAbsolutePath());
	}

	/**
	 * Tests {@link 文件操作#Delete(String)}.
	 */
	public void testDelete() throws IOException {
		// Name is null
		try {
			文件操作.Delete(null);
			fail();
		} catch (NullPointerException expected) {
		}

		// File does not exist
		try {
			文件操作.Delete("whatever");
			fail();
		} catch (文件未存在错误 expected) {
		}

		// File is directory
		File directory = new File(tmpDir, "testDeleteDirectory");
		directory.mkdir();

		try {
			文件操作.Delete(directory.getAbsolutePath());
			fail();
		} catch (文件读写错误 expected) {
		}

		// Delete
		File file = new File(tmpDir, "testDeleteFile");
		file.createNewFile();

		文件操作.Delete(file.getAbsolutePath());
	}

	/**
	 * Tests {@link 文件操作#Mkdir(String)}.
	 */
	public void testMkdir() throws IOException {
		// Name is null
		try {
			文件操作.Mkdir(null);
			fail();
		} catch (NullPointerException expected) {
		}

		// Directory already exists
		File existingDirectory = new File(tmpDir, "testMkdirExistingDirectory");
		existingDirectory.mkdir();

		try {
			文件操作.Mkdir(existingDirectory.getAbsolutePath());
			fail();
		} catch (文件已存在错误 expected) {
		}

		// File with same name exists
		File existingFile = new File(tmpDir, "testMkdirExistingFile");
		existingFile.createNewFile();

		try {
			文件操作.Mkdir(existingFile.getAbsolutePath());
			fail();
		} catch (文件已存在错误 expected) {
		}

		// Mkdir
		File directory = new File(tmpDir, "testMkdirDirectory");
		文件操作.Mkdir(directory.getAbsolutePath());
	}

	/**
	 * Tests {@link 文件操作#Rmdir(String)}.
	 */
	public void testRmdir() throws IOException {
		// Name is null
		try {
			文件操作.Rmdir(null);
			fail();
		} catch (NullPointerException expected) {
		}

		// Directory does not exist
		try {
			文件操作.Rmdir("whatever");
			fail();
		} catch (文件未存在错误 expected) {
		}

		// File with same name exists
		File existingFile = new File(tmpDir, "testRmdirExistingFile");
		existingFile.createNewFile();
		try {
			文件操作.Rmdir(existingFile.getAbsolutePath());
			fail();
		} catch (文件读写错误 expected) {
		}

		// Rmdir
		File existingDirectory = new File(tmpDir, "testRmdirExistingDirectory");
		existingDirectory.mkdir();

		文件操作.Rmdir(existingDirectory.getAbsolutePath());
	}

	/**
	 * Tests {@link 文件操作#Exists(String)}.
	 */
	public void testExists() throws IOException {
		// Name is null
		try {
			文件操作.Exists(null);
			fail();
		} catch (NullPointerException expected) {
		}

		// File/directory does not exist
		assertFalse(文件操作.Exists("whatever"));

		// File exists
		File existingFile = new File(tmpDir, "testExistsExistingFile");
		existingFile.createNewFile();

		assertTrue(文件操作.Exists(existingFile.getAbsolutePath()));

		// Directory exists
		File existingDirectory = new File(tmpDir, "testExistsExistingDirectory");
		existingDirectory.mkdir();

		assertTrue(文件操作.Exists(existingDirectory.getAbsolutePath()));
	}

	/**
	 * Tests {@link 文件操作#IsDirectory(String)}.
	 */
	public void testIsDirectory() throws IOException {
		// Name is null
		try {
			文件操作.IsDirectory(null);
			fail();
		} catch (NullPointerException expected) {
		}

		// File/directory does not exist
		try {
			文件操作.IsDirectory("whatever");
			fail();
		} catch (文件未存在错误 expected) {
		}

		// Is directory
		File existingDirectory = new File(tmpDir, "testIsDirectoryExistingDirectory");
		existingDirectory.mkdir();

		assertTrue(文件操作.IsDirectory(existingDirectory.getAbsolutePath()));

		// Is file
		File existingFile = new File(tmpDir, "testIsDirectoryExistingFile");
		existingFile.createNewFile();

		assertFalse(文件操作.IsDirectory(existingFile.getAbsolutePath()));
	}

	/**
	 * Tests {@link 文件操作#Open(String)}.
	 */
	public void testOpen() throws IOException {
		// Name is null
		try {
			文件操作.Open(null);
			fail();
		} catch (NullPointerException expected) {
		}

		// Is directory
		File existingDirectory = new File(tmpDir, "testIsDirectoryExistingDirectory");
		existingDirectory.mkdir();

		try {
			文件操作.Open(existingDirectory.getAbsolutePath());
			fail();
		} catch (文件读写错误 expected) {
		}

		// Does not exist
		File file = new File(tmpDir, "testOpenFile");

		int handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.Close(handle);

		assertTrue(file.exists());

		// Exists
		File existingFile = new File(tmpDir, "testOpenExistingFile");
		existingFile.createNewFile();

		handle = 文件操作.Open(existingFile.getAbsolutePath());
		文件操作.Close(handle);
	}

	/**
	 * Tests {@link 文件操作#Close(int)}.
	 */
	public void testClose() {
		// Unknown handle and Close
		File file = new File(tmpDir, "testCloseFile");

		int handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.Close(handle);

		try {
			文件操作.Close(handle);
			fail();
		} catch (未知文件句柄错误 expected) {
		}
	}

	/**
	 * Tests {@link 文件操作#Eof(int)}.
	 */
	public void testEof() {
		// Unknown handle
		File file = new File(tmpDir, "testEofFile");

		int handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.WriteLong(handle, 0x123456789ABCDEFL);
		文件操作.Close(handle);

		try {
			文件操作.Eof(handle);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Is not eof
		handle = 文件操作.Open(file.getAbsolutePath());
		assertFalse(文件操作.Eof(handle));

		// Is eof
		文件操作.ReadLong(handle);
		assertTrue(文件操作.Eof(handle));

		// Write some more
		文件操作.WriteLong(handle, 0x123456789ABCDEFL);
		assertTrue(文件操作.Eof(handle));

		// Seek back
		文件操作.Seek(handle, 4);
		assertFalse(文件操作.Eof(handle));

		文件操作.Close(handle);
	}

	/**
	 * Tests {@link 文件操作#Seek(int, long)}.
	 */
	public void testSeek() {
		// Unknown handle
		File file = new File(tmpDir, "testSeekFile");

		int handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.WriteLong(handle, 0x123456789ABCDEFL);
		文件操作.WriteLong(handle, 0xFEDCBA987654321L);
		文件操作.Close(handle);

		try {
			文件操作.Seek(handle, 0);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Seek position before start of file (negative offset)
		handle = 文件操作.Open(file.getAbsolutePath());

		try {
			文件操作.Seek(handle, -1);
			fail();
		} catch (文件读写错误 expected) {
		}

		// Seek position in the middle of file
		assertEquals(8, 文件操作.Seek(handle, 8));
		assertEquals(0xFEDCBA987654321L, 文件操作.ReadLong(handle));

		// Seek position start of file
		assertEquals(0, 文件操作.Seek(handle, 0));
		assertEquals(0x123456789ABCDEFL, 文件操作.ReadLong(handle));

		// Seek position end of file
		assertEquals(16, 文件操作.Seek(handle, 16));
		assertTrue(文件操作.Eof(handle));

		// Seek position beyond end of file
		try {
			文件操作.Seek(handle, Long.MAX_VALUE);
			fail();
		} catch (文件读写错误 expected) {
		}

		文件操作.Close(handle);
	}

	/**
	 * Tests {@link 文件操作#Size(int)}.
	 */
	public void testSize() {
		// Unknown handle
		File file = new File(tmpDir, "testSizeFile");

		int handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.Close(handle);

		try {
			文件操作.Size(handle);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Empty file size
		handle = 文件操作.Open(file.getAbsolutePath());

		assertEquals(0, 文件操作.Size(handle));

		// File size
		文件操作.WriteLong(handle, 0x123456789ABCDEFL);

		assertEquals(8, 文件操作.Size(handle));

		文件操作.Close(handle);
	}

	/**
	 * Tests {@link 文件操作#WriteString(int, String)}.
	 */
	public void testWriteString() {
		// Unknown handle
		File file = new File(tmpDir, "testWriteStringFile");

		int handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.Close(handle);

		try {
			文件操作.WriteString(handle, "foo");
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Write null
		handle = 文件操作.Open(file.getAbsolutePath());

		try {
			文件操作.WriteString(handle, null);
			fail();
		} catch (NullPointerException expected) {
		}

		// Write empty string
		文件操作.WriteString(handle, "");

		// Write
		文件操作.WriteString(handle, "foo");

		文件操作.Seek(handle, 0);
		assertEquals("", 文件操作.ReadString(handle));
		assertEquals("foo", 文件操作.ReadString(handle));
		assertTrue(文件操作.Eof(handle));

		文件操作.Close(handle);
	}

	/**
	 * Tests {@link 文件操作#ReadString(int)}.
	 */
	public void testReadString() {
		// Unknown handle
		File file = new File(tmpDir, "testReadStringFile");

		int handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.WriteString(handle, "");
		文件操作.WriteString(handle, "foo");
		文件操作.Close(handle);

		try {
			文件操作.ReadString(handle);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Read
		handle = 文件操作.Open(file.getAbsolutePath());

		assertEquals("", 文件操作.ReadString(handle));
		assertEquals("foo", 文件操作.ReadString(handle));
		assertTrue(文件操作.Eof(handle));

		文件操作.Close(handle);
	}

	/**
	 * Tests {@link 文件操作#WriteBoolean(int, boolean)}.
	 */
	public void testWriteBoolean() {
		// Unknown handle
		File file = new File(tmpDir, "testWriteBooleanFile");

		int handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.Close(handle);

		try {
			文件操作.WriteBoolean(handle, true);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Write
		handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.WriteBoolean(handle, true);
		文件操作.WriteBoolean(handle, false);

		文件操作.Seek(handle, 0);
		assertTrue(文件操作.ReadBoolean(handle));
		assertFalse(文件操作.ReadBoolean(handle));
		assertTrue(文件操作.Eof(handle));

		文件操作.Close(handle);
	}

	/**
	 * Tests {@link 文件操作#ReadBoolean(int)}.
	 */
	public void testReadBoolean() {
		// Unknown handle
		File file = new File(tmpDir, "testReadBooleanFile");

		int handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.WriteBoolean(handle, true);
		文件操作.WriteBoolean(handle, false);
		文件操作.Close(handle);

		try {
			文件操作.ReadString(handle);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Read
		handle = 文件操作.Open(file.getAbsolutePath());

		assertTrue(文件操作.ReadBoolean(handle));
		assertFalse(文件操作.ReadBoolean(handle));
		assertTrue(文件操作.Eof(handle));

		文件操作.Close(handle);
	}

	/**
	 * Tests {@link 文件操作#WriteByte(int, byte)}.
	 */
	public void testWriteByte() {
		// Unknown handle
		File file = new File(tmpDir, "testWriteByteFile");

		int handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.Close(handle);

		try {
			文件操作.WriteByte(handle, Byte.MAX_VALUE);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Write
		handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.WriteByte(handle, Byte.MAX_VALUE);
		文件操作.WriteByte(handle, Byte.MIN_VALUE);

		文件操作.Seek(handle, 0);
		assertEquals(Byte.MAX_VALUE, 文件操作.ReadByte(handle));
		assertEquals(Byte.MIN_VALUE, 文件操作.ReadByte(handle));
		assertTrue(文件操作.Eof(handle));

		文件操作.Close(handle);
	}

	/**
	 * Tests {@link 文件操作#ReadByte(int)}.
	 */
	public void testReadByte() {
		// Unknown handle
		File file = new File(tmpDir, "testReadByteFile");

		int handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.WriteByte(handle, Byte.MAX_VALUE);
		文件操作.WriteByte(handle, Byte.MIN_VALUE);
		文件操作.Close(handle);

		try {
			文件操作.ReadByte(handle);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Read
		handle = 文件操作.Open(file.getAbsolutePath());

		assertEquals(Byte.MAX_VALUE, 文件操作.ReadByte(handle));
		assertEquals(Byte.MIN_VALUE, 文件操作.ReadByte(handle));
		assertTrue(文件操作.Eof(handle));

		文件操作.Close(handle);
	}

	/**
	 * Tests {@link 文件操作#WriteShort(int, short)}.
	 */
	public void testWriteShort() {
		// Unknown handle
		File file = new File(tmpDir, "testWriteShortFile");

		int handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.Close(handle);

		try {
			文件操作.WriteShort(handle, Short.MAX_VALUE);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Write
		handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.WriteShort(handle, Short.MAX_VALUE);
		文件操作.WriteShort(handle, Short.MIN_VALUE);

		文件操作.Seek(handle, 0);
		assertEquals(Short.MAX_VALUE, 文件操作.ReadShort(handle));
		assertEquals(Short.MIN_VALUE, 文件操作.ReadShort(handle));
		assertTrue(文件操作.Eof(handle));

		文件操作.Close(handle);
	}

	/**
	 * Tests {@link 文件操作#ReadShort(int)}.
	 */
	public void testReadShort() {
		// Unknown handle
		File file = new File(tmpDir, "testReadShortFile");

		int handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.WriteShort(handle, Short.MAX_VALUE);
		文件操作.WriteShort(handle, Short.MIN_VALUE);
		文件操作.Close(handle);

		try {
			文件操作.ReadShort(handle);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Read
		handle = 文件操作.Open(file.getAbsolutePath());

		assertEquals(Short.MAX_VALUE, 文件操作.ReadShort(handle));
		assertEquals(Short.MIN_VALUE, 文件操作.ReadShort(handle));
		assertTrue(文件操作.Eof(handle));

		文件操作.Close(handle);
	}

	/**
	 * Tests {@link 文件操作#WriteInteger(int, int)}.
	 */
	public void testWriteInteger() {
		// Unknown handle
		File file = new File(tmpDir, "testWriteIntegerFile");

		int handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.Close(handle);

		try {
			文件操作.WriteInteger(handle, Integer.MAX_VALUE);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Write
		handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.WriteInteger(handle, Integer.MAX_VALUE);
		文件操作.WriteInteger(handle, Integer.MIN_VALUE);

		文件操作.Seek(handle, 0);
		assertEquals(Integer.MAX_VALUE, 文件操作.ReadInteger(handle));
		assertEquals(Integer.MIN_VALUE, 文件操作.ReadInteger(handle));
		assertTrue(文件操作.Eof(handle));

		文件操作.Close(handle);
	}

	/**
	 * Tests {@link 文件操作#ReadInteger(int)}.
	 */
	public void testReadInteger() {
		// Unknown handle
		File file = new File(tmpDir, "testReadIntegerFile");

		int handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.WriteInteger(handle, Integer.MAX_VALUE);
		文件操作.WriteInteger(handle, Integer.MIN_VALUE);
		文件操作.Close(handle);

		try {
			文件操作.ReadInteger(handle);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Read
		handle = 文件操作.Open(file.getAbsolutePath());

		assertEquals(Integer.MAX_VALUE, 文件操作.ReadInteger(handle));
		assertEquals(Integer.MIN_VALUE, 文件操作.ReadInteger(handle));
		assertTrue(文件操作.Eof(handle));

		文件操作.Close(handle);
	}

	/**
	 * Tests {@link 文件操作#WriteLong(int, long)}.
	 */
	public void testWriteLong() {
		// Unknown handle
		File file = new File(tmpDir, "testWriteLongFile");

		int handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.Close(handle);

		try {
			文件操作.WriteLong(handle, Long.MAX_VALUE);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Write
		handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.WriteLong(handle, Long.MAX_VALUE);
		文件操作.WriteLong(handle, Long.MIN_VALUE);

		文件操作.Seek(handle, 0);
		assertEquals(Long.MAX_VALUE, 文件操作.ReadLong(handle));
		assertEquals(Long.MIN_VALUE, 文件操作.ReadLong(handle));
		assertTrue(文件操作.Eof(handle));

		文件操作.Close(handle);
	}

	/**
	 * Tests {@link 文件操作#ReadLong(int)}.
	 */
	public void testReadLong() {
		// Unknown handle
		File file = new File(tmpDir, "testReadLongFile");

		int handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.WriteLong(handle, Long.MAX_VALUE);
		文件操作.WriteLong(handle, Long.MIN_VALUE);
		文件操作.Close(handle);

		try {
			文件操作.ReadLong(handle);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Read
		handle = 文件操作.Open(file.getAbsolutePath());

		assertEquals(Long.MAX_VALUE, 文件操作.ReadLong(handle));
		assertEquals(Long.MIN_VALUE, 文件操作.ReadLong(handle));
		assertTrue(文件操作.Eof(handle));

		文件操作.Close(handle);
	}

	/**
	 * Tests {@link 文件操作#WriteSingle(int, float)}.
	 */
	public void testWriteSingle() {
		// Unknown handle
		File file = new File(tmpDir, "testWriteSingleFile");

		int handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.Close(handle);

		try {
			文件操作.WriteSingle(handle, Float.MAX_VALUE);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Write
		handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.WriteSingle(handle, Float.MAX_VALUE);
		文件操作.WriteSingle(handle, Float.MIN_VALUE);

		文件操作.Seek(handle, 0);
		assertEquals(Float.MAX_VALUE, 文件操作.ReadSingle(handle));
		assertEquals(Float.MIN_VALUE, 文件操作.ReadSingle(handle));
		assertTrue(文件操作.Eof(handle));

		文件操作.Close(handle);
	}

	/**
	 * Tests {@link 文件操作#ReadSingle(int)}.
	 */
	public void testReadSingle() {
		// Unknown handle
		File file = new File(tmpDir, "testReadSingleFile");

		int handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.WriteSingle(handle, Float.MAX_VALUE);
		文件操作.WriteSingle(handle, Float.MIN_VALUE);
		文件操作.Close(handle);

		try {
			文件操作.ReadSingle(handle);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Read
		handle = 文件操作.Open(file.getAbsolutePath());

		assertEquals(Float.MAX_VALUE, 文件操作.ReadSingle(handle));
		assertEquals(Float.MIN_VALUE, 文件操作.ReadSingle(handle));
		assertTrue(文件操作.Eof(handle));

		文件操作.Close(handle);
	}

	/**
	 * Tests {@link 文件操作#WriteDouble(int, double)}.
	 */
	public void testWriteDouble() {
		// Unknown handle
		File file = new File(tmpDir, "testWriteDoubleFile");

		int handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.Close(handle);

		try {
			文件操作.WriteDouble(handle, Double.MAX_VALUE);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Write
		handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.WriteDouble(handle, Double.MAX_VALUE);
		文件操作.WriteDouble(handle, Double.MIN_VALUE);

		文件操作.Seek(handle, 0);
		assertEquals(Double.MAX_VALUE, 文件操作.ReadDouble(handle));
		assertEquals(Double.MIN_VALUE, 文件操作.ReadDouble(handle));
		assertTrue(文件操作.Eof(handle));

		文件操作.Close(handle);
	}

	/**
	 * Tests {@link 文件操作#ReadDouble(int)}.
	 */
	public void testReadDouble() {
		// Unknown handle
		File file = new File(tmpDir, "testReadDoubleFile");

		int handle = 文件操作.Open(file.getAbsolutePath());
		文件操作.WriteDouble(handle, Double.MAX_VALUE);
		文件操作.WriteDouble(handle, Double.MIN_VALUE);
		文件操作.Close(handle);

		try {
			文件操作.ReadDouble(handle);
			fail();
		} catch (未知文件句柄错误 expected) {
		}

		// Read
		handle = 文件操作.Open(file.getAbsolutePath());

		assertEquals(Double.MAX_VALUE, 文件操作.ReadDouble(handle));
		assertEquals(Double.MIN_VALUE, 文件操作.ReadDouble(handle));
		assertTrue(文件操作.Eof(handle));

		文件操作.Close(handle);
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

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

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.errors.文件已存在错误;
import simple.runtime.errors.文件未存在错误;
import simple.runtime.errors.文件读写错误;
import simple.runtime.errors.未知文件句柄错误;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 提供文件句柄读写及按路径操作文件、目录的便捷函数。
 *
 * <p>相对路径以应用私有 files 目录为基准，该目录无需存储权限。新增路径接口也接受
 * 绝对路径，访问范围由 Android 授权决定，不支持 content URI。句柄字符串使用
 * RandomAccessFile 的二进制 UTF 格式，普通文本文件请使用读入文本文件和写出文本文件。</p>
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 文件操作 {

	/**
	 * 提供应用文件根目录的接口。
	 */
	public static interface RootProvider {
		File getApplicationRootDirectory();
	}

	/*
	 * Helper class for handling file I/O.
	 */
	private static class FileDescriptor {
		private final File file;
		private RandomAccessFile raf;

		private void raiseError(String message) {
			throw new 文件读写错误(message + ": " + file.getName());
		}

		FileDescriptor(File file) {
			this.file = file;

			try {
				raf = new RandomAccessFile(file, "rws");
			} catch (FileNotFoundException e) {
				raiseError("Opening file");
			}
		}

		void close() {
			try {
				raf.close();
			} catch (IOException e) {
				raiseError("Closing file");
			}
		}
	}

	/*
	 * 旧句柄表维持既有实现；调用方应避免并发创建、关闭句柄，
	 * 也不要同时读写同一句柄的共享文件指针。
	 */
	private static int fileHandleCounter;
	private static Map<Integer, FileDescriptor> fileHandleMap =
			new HashMap<Integer, FileDescriptor>();

	private static File rootDirectory;

	private 文件操作() {  // COV_NF_LINE
	}                  // COV_NF_LINE

	/**
	 * 设置旧句柄接口和新路径接口的相对路径基准目录。
	 *
	 * @param rootDir 应用私有 files 目录。
	 */
	public static void initialize(File rootDir) {
		rootDirectory = rootDir;
	}

	/* 路径与目录操作 */

	/**
	 * 判断指定路径是否存在。
	 *
	 * @param path 文件或目录路径。
	 * @return 存在时返回真。
	 */
	@SimpleFunction
	public static boolean 文件是否存在(String path) {
		File file = resolvePath(path);

		try {
			return file.exists();
		} catch (SecurityException exception) {
			throw pathError("检查文件失败", file, exception);
		}
	}

	/**
	 * 判断文件或目录是否存在。
	 *
	 * @param name 文件或目录路径。
	 * @return 存在时返回真。
	 */
	@SimpleFunction
	public static boolean 路径是否存在(String name) {
		return new File(rootDirectory, name).exists();
	}

	/**
	 * 判断指定路径是否为目录。
	 *
	 * @param path 文件或目录路径。
	 * @return 是目录时返回真，不存在时返回假。
	 */
	@SimpleFunction
	public static boolean 是否为目录(String path) {
		File file = resolvePath(path);

		try {
			return file.isDirectory();
		} catch (SecurityException exception) {
			throw pathError("检查目录失败", file, exception);
		}
	}

	/**
	 * 判断已存在的路径是否为目录；路径不存在时抛出错误。
	 *
	 * @param name 文件或目录路径。
	 * @return 是目录时返回真。
	 */
	@SimpleFunction
	public static boolean 路径是否为目录(String name) {
		return getExistingFile(name).isDirectory();
	}

	/**
	 * 判断现有文件或目录是否被文件系统标记为隐藏。
	 *
	 * @param path 文件或目录路径。
	 * @return 隐藏时返回真。
	 */
	@SimpleFunction
	public static boolean 是否为隐藏文件(String path) {
		File file = requireExistingPath(path);

		try {
			return file.isHidden();
		} catch (SecurityException exception) {
			throw pathError("检查隐藏状态失败", file, exception);
		}
	}

	/**
	 * 创建空文件，不覆盖已有内容，也不自动创建父目录。
	 *
	 * @param path 文件路径。
	 * @return 新建成功时返回真，普通文件已存在时返回假。
	 */
	@SimpleFunction
	public static boolean 创建文件(String path) {
		File file = resolvePath(path);

		try {
			if (file.isDirectory()) {
				throw new IOException("目标是目录");
			}

			return file.createNewFile();
		} catch (IOException | SecurityException exception) {
			throw pathError("创建文件失败", file, exception);
		}
	}

	/**
	 * 创建目录及缺少的父目录。
	 *
	 * @param path 目录路径。
	 * @return 目录已存在或创建成功时返回真。
	 */
	@SimpleFunction
	public static boolean 创建目录(String path) {
		File directory = resolvePath(path);

		try {
			if (!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory()) {
				throw new IOException("无法创建目录");
			}

			return true;
		} catch (IOException | SecurityException exception) {
			throw pathError("创建目录失败", directory, exception);
		}
	}

	/**
	 * 创建一级目录；目标已存在或父目录不存在时抛出错误。
	 *
	 * @param name 新目录路径。
	 */
	@SimpleFunction
	public static void 创建单级目录(String name) {
		if (!getNonExistingFile(name).mkdir()) {
			accessError(name);
		}
	}

	/**
	 * 删除指定普通文件，不递归删除目录。
	 *
	 * @param path 文件路径。
	 * @return 删除成功时返回真，不存在时返回假。
	 */
	@SimpleFunction
	public static boolean 删除文件(String path) {
		File file = resolvePath(path);

		try {
			if (!file.exists()) {
				return false;
			}

			if (!file.isFile() || !file.delete()) {
				throw new IOException("目标不是普通文件或无法删除");
			}

			return true;
		} catch (IOException | SecurityException exception) {
			throw pathError("删除文件失败", file, exception);
		}
	}

	/**
	 * 删除普通文件，目标不存在、是目录或删除失败时抛出错误。
	 *
	 * @param name 文件路径。
	 */
	@SimpleFunction
	public static void 删除普通文件(String name) {
		File file = getExistingFile(name);
		if (file.isDirectory() || !file.delete()) {
			accessError(name);
		}
	}

	/**
	 * 递归删除目录内容及目录本身，不跟随目录符号链接。
	 *
	 * <p>不允许删除文件系统根目录、应用 files 根目录或其祖先目录。
	 * 删除中途失败时抛出错误，之前已删除的内容不能恢复。</p>
	 *
	 * @param path 待删除目录路径，不得为空。
	 * @return 删除成功时返回真，目录不存在时返回假。
	 */
	@SimpleFunction
	public static boolean 删除目录(String path) {
		File directory = resolvePath(path);

		try {
			File target = directory.getCanonicalFile();
			if (target.getParentFile() == null) {
				throw new IllegalArgumentException("不能删除文件系统根目录");
			}

			// 根目录和祖先目录不能成为递归清理的目标。
			for (File current = rootDirectory.getCanonicalFile(); current != null; current = current.getParentFile()) {
				if (target.equals(current)) {
					throw new IllegalArgumentException("不能删除应用文件根目录或其祖先目录");
				}
			}

			if (!directory.exists()) {
				return false;
			}

			if (!directory.isDirectory()) {
				throw new IOException("目标不是目录");
			}

			deleteDirectory(directory);
			return true;
		} catch (IOException | SecurityException exception) {
			throw pathError("删除目录失败", directory, exception);
		}
	}

	/**
	 * 删除空目录，不递归删除内容。
	 *
	 * @param name 待删除的空目录路径。
	 */
	@SimpleFunction
	public static void 删除空目录(String name) {
		File directory = getExistingFile(name);
		if (!directory.isDirectory() || !directory.delete()) {
			accessError(name);
		}
	}

	/**
	 * 修改文件或目录名称，不覆盖已存在的目标。
	 *
	 * @param sourcePath 原路径。
	 * @param targetPath 新路径。
	 * @return 改名成功或源、目标为同一路径时返回真。
	 */
	@SimpleFunction
	public static boolean 修改文件名(String sourcePath, String targetPath) {
		File source = requireExistingPath(sourcePath);
		File target = resolvePath(targetPath);

		try {
			if (source.getCanonicalFile().equals(target.getCanonicalFile())) {
				return true;
			}

			if (target.exists()) {
				throw new 文件已存在错误(targetPath);
			}

			if (!source.renameTo(target)) {
				throw new IOException("系统未完成改名");
			}

			return true;
		} catch (IOException | SecurityException exception) {
			throw pathError("修改文件名失败", source, exception);
		}
	}

	/**
	 * 修改文件或目录名称，不覆盖已存在的目标。
	 *
	 * @param oldname 原路径。
	 * @param newname 新路径。
	 */
	@SimpleFunction
	public static void 重命名文件(String oldname, String newname) {

		File oldfile = getExistingFile(oldname);
		File newfile = new File(rootDirectory, newname);

		if (!oldfile.equals(newfile)) {
			if (newfile.exists()) {
				throw new 文件已存在错误(newname);
			}

			if (!oldfile.renameTo(newfile)) {
				accessError(oldname);
			}
		}
	}

	/**
	 * 分块复制普通文件，覆盖已有目标，不自动创建目标父目录。
	 *
	 * @param sourcePath 源文件路径。
	 * @param targetPath 目标文件路径，不得与源文件指向同一规范路径。
	 * @return 复制并关闭成功时返回真；失败时目标可能包含已复制的部分内容。
	 */
	@SimpleFunction
	public static boolean 复制文件(String sourcePath, String targetPath) {
		File source = requireRegularFile(sourcePath);
		File target = resolvePath(targetPath);
		FileInputStream input = null;
		FileOutputStream output = null;

		try {
			if (source.getCanonicalFile().equals(target.getCanonicalFile())) {
				throw new IllegalArgumentException("源文件与目标文件不能相同");
			}

			input = new FileInputStream(source);
			output = new FileOutputStream(target);
			byte[] buffer = new byte[8192];
			int count;

			while ((count = input.read(buffer)) != -1) {
				output.write(buffer, 0, count);
			}

			output.close();
			output = null;
			return true;
		} catch (IOException | SecurityException exception) {
			throw pathError("复制文件失败", source, exception);
		} finally {
			closeStream(output);
			closeStream(input);
		}
	}

	/**
	 * 返回普通文件字节数；目录则递归累计普通文件大小，不遍历目录符号链接。
	 *
	 * @param path 文件或目录路径。
	 * @return 字节数，空文件或空目录返回 0。
	 */
	@SimpleFunction
	public static long 取文件尺寸(String path) {
		File file = requireExistingPath(path);

		try {
			return fileSize(file);
		} catch (IOException | SecurityException exception) {
			throw pathError("读取文件尺寸失败", file, exception);
		}
	}

	/**
	 * 返回文件或目录的最后修改时间，按设备本地时区格式化。
	 *
	 * @param path 文件或目录路径。
	 * @return 格式为 yyyy-MM-dd HH:mm:ss 的文本，系统无法提供时间时返回空文本。
	 */
	@SimpleFunction
	public static String 取文件修改时间(String path) {
		File file = requireExistingPath(path);

		try {
			long modified = file.lastModified();
			return modified == 0 ? "" : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
				.format(new java.util.Date(modified));
		} catch (SecurityException exception) {
			throw pathError("读取修改时间失败", file, exception);
		}
	}

	/**
	 * 枚举目录下一级普通文件，不递归。
	 *
	 * @param path 目录路径。
	 * @return 按路径文本排序的绝对路径数组，没有文件时返回空数组。
	 */
	@SimpleFunction
	public static String[] 取文件列表(String path) {
		return listPaths(path, false);
	}

	/**
	 * 枚举目录下一级子目录，不递归。
	 *
	 * @param path 目录路径。
	 * @return 按路径文本排序的绝对路径数组，没有子目录时返回空数组。
	 */
	@SimpleFunction
	public static String[] 取子目录(String path) {
		return listPaths(path, true);
	}

	/* 整文件读写与编码 */

	/**
	 * 根据文件开头的 BOM 判断 UTF-8、UTF-16 或 UTF-32 编码。
	 *
	 * @param path 普通文件路径。
	 * @return 标准编码名；无 BOM 或无法识别时返回空文本，不据此猜测 GBK。
	 */
	@SimpleFunction
	public static String 取文件编码(String path) {
		File file = requireRegularFile(path);
		FileInputStream input = null;

		try {
			input = new FileInputStream(file);
			byte[] prefix = new byte[4];
			int count = 0;

			while (count < prefix.length) {
				int value = input.read();
				if (value < 0) {
					break;
				}

				prefix[count++] = (byte) value;
			}

			// 先识别四字节 BOM，再识别 UTF-16，避免相同前缀被提前匹配。
			if (count >= 4 && prefix[0] == 0 && prefix[1] == 0 && prefix[2] == -2 && prefix[3] == -1) {
				return "UTF-32BE";
			}

			if (count >= 4 && prefix[0] == -1 && prefix[1] == -2 && prefix[2] == 0 && prefix[3] == 0) {
				return "UTF-32LE";
			}

			if (count >= 3 && prefix[0] == -17 && prefix[1] == -69 && prefix[2] == -65) {
				return "UTF-8";
			}

			if (count >= 2 && prefix[0] == -1 && prefix[1] == -2) {
				return "UTF-16LE";
			}

			if (count >= 2 && prefix[0] == -2 && prefix[1] == -1) {
				return "UTF-16BE";
			}

			return "";
		} catch (IOException | SecurityException exception) {
			throw pathError("读取文件编码失败", file, exception);
		} finally {
			closeStream(input);
		}
	}

	/**
	 * 按 UTF-8 读取整个普通文本文件，不自动移除 BOM。
	 *
	 * @param path 普通文件路径。
	 * @return 文件文本，空文件返回空文本。
	 */
	@SimpleFunction
	public static String 读入文本文件(String path) {
		return 读入文本文件(path, "UTF-8");
	}

	/**
	 * 按指定编码读取整个普通文本文件，不自动探测编码或移除 BOM。
	 *
	 * @param path 普通文件路径。
	 * @param encoding Java 字符集名称，例如 UTF-8、GBK。
	 * @return 文件文本，空文件返回空文本；读取失败抛出文件错误。
	 */
	@SimpleFunction
	public static String 读入文本文件(String path, String encoding) {
		Charset charset = requireCharset(encoding);
		return new String(读入字节文件(path), charset);
	}

	/**
	 * 以 UTF-8 覆盖写入普通文本文件，不自动添加 BOM 或创建父目录。
	 *
	 * @param path 目标文件路径。
	 * @param text 待写入文本，不得为空引用，允许空文本。
	 * @return 写入成功时返回真。
	 */
	@SimpleFunction
	public static boolean 写出文本文件(String path, String text) {
		return 写出文本文件(path, text, "UTF-8");
	}

	/**
	 * 按指定编码覆盖写入普通文本文件，不自动创建父目录。
	 *
	 * @param path 目标文件路径。
	 * @param text 待写入文本，不得为空引用。
	 * @param encoding Java 字符集名称，例如 UTF-8、GBK。
	 * @return 写入成功时返回真。
	 */
	@SimpleFunction
	public static boolean 写出文本文件(String path, String text, String encoding) {
		if (text == null) {
			throw new IllegalArgumentException("待写入文本不得为空引用");
		}

		// 先验证编码并完成转换，避免参数错误时截断已有文件。
		Charset charset = requireCharset(encoding);
		return 写出字节文件(path, text.getBytes(charset));
	}

	/**
	 * 循环读取整个普通文件，适合能够放入内存的小文件。
	 *
	 * @param path 普通文件路径。
	 * @return 文件字节数组，空文件返回空数组；读取失败抛出文件错误。
	 */
	@SimpleFunction
	public static byte[] 读入字节文件(String path) {
		File file = requireRegularFile(path);
		FileInputStream input = null;

		try {
			input = new FileInputStream(file);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int count;

			while ((count = input.read(buffer)) != -1) {
				output.write(buffer, 0, count);
			}

			return output.toByteArray();
		} catch (IOException | SecurityException exception) {
			throw pathError("读取字节文件失败", file, exception);
		} finally {
			closeStream(input);
		}
	}

	/**
	 * 覆盖写入字节文件，不自动创建父目录。
	 *
	 * @param path 目标文件路径。
	 * @param data 待写入数据，不得为空引用，允许空数组。
	 * @return 写入并关闭成功时返回真。
	 */
	@SimpleFunction
	public static boolean 写出字节文件(String path, byte[] data) {
		if (data == null) {
			throw new IllegalArgumentException("待写入字节不得为空引用");
		}

		File file = resolvePath(path);
		FileOutputStream output = null;

		try {
			output = new FileOutputStream(file);
			output.write(data);
			output.close();
			output = null;
			return true;
		} catch (IOException | SecurityException exception) {
			throw pathError("写入字节文件失败", file, exception);
		} finally {
			closeStream(output);
		}
	}

	/* 句柄管理与定位 */

	/**
	 * 以同步读写模式打开文件，文件不存在时创建，不清空已有内容。
	 *
	 * @param name 文件路径，父目录必须存在。
	 * @return 文件句柄，使用后应调用 关闭文件 关闭。
	 */
	@SimpleFunction
	public static int 打开文件(String name) {
		File file = new File(rootDirectory, name);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				日志输出.输出错误(日志输出.MODULE_NAME_RTL, e.getMessage());
				e.printStackTrace();

				throw new 文件读写错误("Creating file: " + name);
			}
		} else if (路径是否为目录(name)) {
			throw new 文件读写错误("Cannot open directory: " + name);
		}

		FileDescriptor descriptor = new FileDescriptor(file);
		int handle = ++fileHandleCounter;

		fileHandleMap.put(handle, descriptor);

		return handle;
	}

	/**
	 * 关闭文件句柄并移除登记，未知或已关闭的句柄会抛出错误。
	 *
	 * @param handle 待关闭的文件句柄。
	 */
	@SimpleFunction
	public static void 关闭文件(int handle) {
		getFileDescriptor(handle).close();
		fileHandleMap.remove(handle);
	}

	/**
	 * 将文件指针移动到从文件开头计算的字节位置。
	 *
	 * @param handle 文件句柄。
	 * @param offset 字节位置，不得小于 0 或大于文件长度。
	 * @return 移动后的字节位置。
	 */
	@SimpleFunction
	public static long 移动文件指针(int handle, long offset) {
		FileDescriptor descriptor = getFileDescriptor(handle);

		try {
			if (offset < 0 || offset > descriptor.file.length()) {
				seekError(descriptor.file.getName());
			}

			descriptor.raf.seek(offset);
			long newOffset = descriptor.raf.getFilePointer();
			if (newOffset != offset) {
				seekError(descriptor.file.getName());
			}

			return newOffset;
		} catch (IOException e) {
			seekError(descriptor.file.getName());
			return 0;  // COV_NF_LINE
		}
	}

	/**
	 * 取得句柄对应文件的字节长度。
	 *
	 * @param handle 文件句柄。
	 * @return 文件字节数。
	 */
	@SimpleFunction
	public static long 取文件长度(int handle) {
		FileDescriptor descriptor = getFileDescriptor(handle);
		return descriptor.file.length();
	}

	/**
	 * 判断文件指针是否位于文件末尾。
	 *
	 * @param handle 文件句柄。
	 * @return 位于末尾时返回真。
	 */
	@SimpleFunction
	public static boolean 是否到文件尾(int handle) {
		FileDescriptor descriptor = getFileDescriptor(handle);

		try {
			return descriptor.raf.getFilePointer() == descriptor.file.length();
		} catch (IOException e) {
			readError(descriptor.file.getName());
			return false;  // COV_NF_LINE
		}
	}

	/* 按类型成对读写 */

	/**
	 * 写入带长度信息的二进制 UTF 字符串，不是普通 UTF-8 文本。
	 *
	 * @param handle 文件句柄。
	 * @param value 字符串，应使用 读入文本 按相同顺序读取。
	 */
	@SimpleFunction
	public static void 写入文本(int handle, String value) {
		FileDescriptor descriptor = getFileDescriptor(handle);

		try {
			descriptor.raf.writeUTF(value);
		} catch (IOException e) {
			writeError(descriptor.file.getName());
		}
	}

	/**
	 * 读取由 写入文本 写入的二进制 UTF 字符串。
	 *
	 * @param handle 文件句柄。
	 * @return 读取的字符串。
	 */
	@SimpleFunction
	public static String 读入文本(int handle) {
		FileDescriptor descriptor = getFileDescriptor(handle);

		try {
			return descriptor.raf.readUTF();
		} catch (IOException e) {
			readError(descriptor.file.getName());
			return "";  // COV_NF_LINE
		}
	}

	/**
	 * 按二进制格式写入逻辑值。
	 *
	 * @param handle 文件句柄。
	 * @param value 待写入的逻辑值，应以对应读取函数按相同顺序读取。
	 */
	@SimpleFunction
	public static void 写入逻辑值(int handle, boolean value) {
		FileDescriptor descriptor = getFileDescriptor(handle);

		try {
			descriptor.raf.writeBoolean(value);
		} catch (IOException e) {
			writeError(descriptor.file.getName());
		}
	}

	/**
	 * 读取对应二进制格式的逻辑值。
	 *
	 * @param handle 文件句柄。
	 * @return 读取的逻辑值。
	 */
	@SimpleFunction
	public static boolean 读入逻辑值(int handle) {
		FileDescriptor descriptor = getFileDescriptor(handle);

		try {
			return descriptor.raf.readBoolean();
		} catch (IOException e) {
			readError(descriptor.file.getName());
			return false;  // COV_NF_LINE
		}
	}

	/**
	 * 按二进制格式写入字节。
	 *
	 * @param handle 文件句柄。
	 * @param value 待写入的字节，应以对应读取函数按相同顺序读取。
	 */
	@SimpleFunction
	public static void 写入字节(int handle, byte value) {
		FileDescriptor descriptor = getFileDescriptor(handle);

		try {
			descriptor.raf.writeByte(value);
		} catch (IOException e) {
			writeError(descriptor.file.getName());
		}
	}

	/**
	 * 读取对应二进制格式的字节。
	 *
	 * @param handle 文件句柄。
	 * @return 读取的字节。
	 */
	@SimpleFunction
	public static byte 读入字节(int handle) {
		FileDescriptor descriptor = getFileDescriptor(handle);

		try {
			return descriptor.raf.readByte();
		} catch (IOException e) {
			readError(descriptor.file.getName());
			return 0;  // COV_NF_LINE
		}
	}

	/**
	 * 按二进制格式写入短整数。
	 *
	 * @param handle 文件句柄。
	 * @param value 待写入的短整数，应以对应读取函数按相同顺序读取。
	 */
	@SimpleFunction
	public static void 写入短整数(int handle, short value) {
		FileDescriptor descriptor = getFileDescriptor(handle);

		try {
			descriptor.raf.writeShort(value);
		} catch (IOException e) {
			writeError(descriptor.file.getName());
		}
	}

	/**
	 * 读取对应二进制格式的短整数。
	 *
	 * @param handle 文件句柄。
	 * @return 读取的短整数。
	 */
	@SimpleFunction
	public static short 读入短整数(int handle) {
		FileDescriptor descriptor = getFileDescriptor(handle);

		try {
			return descriptor.raf.readShort();
		} catch (IOException e) {
			readError(descriptor.file.getName());
			return 0;  // COV_NF_LINE
		}
	}

	/**
	 * 按二进制格式写入整数。
	 *
	 * @param handle 文件句柄。
	 * @param value 待写入的整数，应以对应读取函数按相同顺序读取。
	 */
	@SimpleFunction
	public static void 写入整数(int handle, int value) {
		FileDescriptor descriptor = getFileDescriptor(handle);

		try {
			descriptor.raf.writeInt(value);
		} catch (IOException e) {
			writeError(descriptor.file.getName());
		}
	}

	/**
	 * 读取对应二进制格式的整数。
	 *
	 * @param handle 文件句柄。
	 * @return 读取的整数。
	 */
	@SimpleFunction
	public static int 读入整数(int handle) {
		FileDescriptor descriptor = getFileDescriptor(handle);

		try {
			return descriptor.raf.readInt();
		} catch (IOException e) {
			readError(descriptor.file.getName());
			return 0;  // COV_NF_LINE
		}
	}

	/**
	 * 按二进制格式写入长整数。
	 *
	 * @param handle 文件句柄。
	 * @param value 待写入的长整数，应以对应读取函数按相同顺序读取。
	 */
	@SimpleFunction
	public static void 写入长整数(int handle, long value) {
		FileDescriptor descriptor = getFileDescriptor(handle);

		try {
			descriptor.raf.writeLong(value);
		} catch (IOException e) {
			writeError(descriptor.file.getName());
		}
	}

	/**
	 * 读取对应二进制格式的长整数。
	 *
	 * @param handle 文件句柄。
	 * @return 读取的长整数。
	 */
	@SimpleFunction
	public static long 读入长整数(int handle) {
		FileDescriptor descriptor = getFileDescriptor(handle);

		try {
			return descriptor.raf.readLong();
		} catch (IOException e) {
			readError(descriptor.file.getName());
			return 0;  // COV_NF_LINE
		}
	}

	/**
	 * 按二进制格式写入单精度小数。
	 *
	 * @param handle 文件句柄。
	 * @param value 待写入的单精度小数，应以对应读取函数按相同顺序读取。
	 */
	@SimpleFunction
	public static void 写入单精度小数(int handle, float value) {
		FileDescriptor descriptor = getFileDescriptor(handle);

		try {
			descriptor.raf.writeFloat(value);
		} catch (IOException e) {
			writeError(descriptor.file.getName());
		}
	}

	/**
	 * 读取对应二进制格式的单精度小数。
	 *
	 * @param handle 文件句柄。
	 * @return 读取的单精度小数。
	 */
	@SimpleFunction
	public static float 读入单精度小数(int handle) {
		FileDescriptor descriptor = getFileDescriptor(handle);

		try {
			return descriptor.raf.readFloat();
		} catch (IOException e) {
			readError(descriptor.file.getName());
			return 0;  // COV_NF_LINE
		}
	}

	/**
	 * 按二进制格式写入双精度小数。
	 *
	 * @param handle 文件句柄。
	 * @param value 待写入的双精度小数，应以对应读取函数按相同顺序读取。
	 */
	@SimpleFunction
	public static void 写入双精度小数(int handle, double value) {
		FileDescriptor descriptor = getFileDescriptor(handle);

		try {
			descriptor.raf.writeDouble(value);
		} catch (IOException e) {
			writeError(descriptor.file.getName());
		}
	}

	/**
	 * 读取对应二进制格式的双精度小数。
	 *
	 * @param handle 文件句柄。
	 * @return 读取的双精度小数。
	 */
	@SimpleFunction
	public static double 读入双精度小数(int handle) {
		FileDescriptor descriptor = getFileDescriptor(handle);

		try {
			return descriptor.raf.readDouble();
		} catch (IOException e) {
			readError(descriptor.file.getName());
			return 0;  // COV_NF_LINE
		}
	}

	/* 句柄内部辅助 */

	/*
	 * Gets a File object for a given file name and raises a runtime error if the
	 * file doesn't exist.
	 */
	private static File getExistingFile(String name) {
		File file = new File(rootDirectory, name);
		if (!file.exists()) {
			throw new 文件未存在错误(name);
		}

		return file;
	}

	/*
	 * Gets a File object for a given file name and raises a runtime error if the
	 * file already exists.
	 */
	private static File getNonExistingFile(String name) {
		File file = new File(rootDirectory, name);
		if (file.exists()) {
			throw new 文件已存在错误(name);
		}

		return file;
	}

	/*
	 * Gets the file descriptor associated with the file handle. Raises a runtime
	 * error if no file descriptor can be found.
	 */
	private static FileDescriptor getFileDescriptor(int handle) {
		FileDescriptor descriptor = fileHandleMap.get(handle);
		if (descriptor == null) {
			throw new 未知文件句柄错误();
		}

		return descriptor;
	}

	/*
	 * Raises a file write runtime error.
	 */
	private static void writeError(String name) {
		throw new 文件读写错误("Write error: " + name);
	}

	/*
	 * Raises a file read runtime error.
	 */
	private static void readError(String name) {
		throw new 文件读写错误("Read error: " + name);
	}

	/*
	 * Raises a file read runtime error.
	 */
	private static void seekError(String name) {
		throw new 文件读写错误("移动文件指针 error: " + name);
	}

	/*
	 * Raises a file access runtime error.
	 */
	private static void accessError(String name) {
		throw new 文件读写错误("Access error: " + name);
	}

	/* 路径接口内部辅助 */

	/**
	 * 解析新路径接口的相对或绝对路径，沿用应用私有目录作为默认基准。
	 *
	 * @param path 文件系统路径，不接受空文本或 content URI。
	 * @return 对应文件，不检查是否存在。
	 */
	private static File resolvePath(String path) {
		if (path == null || path.trim().length() == 0 || path.startsWith("content://")) {
			throw new IllegalArgumentException("请提供非空文件系统路径，暂不支持 content URI");
		}

		File file = new File(path);
		return file.isAbsolute() ? file : new File(rootDirectory, path);
	}

	/**
	 * 取得已存在的文件或目录，不把缺失路径当成空文件。
	 *
	 * @param path 文件系统路径。
	 * @return 已存在的文件或目录。
	 */
	private static File requireExistingPath(String path) {
		File file = resolvePath(path);

		try {
			if (!file.exists()) {
				throw new 文件未存在错误(path);
			}

			return file;
		} catch (SecurityException exception) {
			throw pathError("访问文件失败", file, exception);
		}
	}

	/**
	 * 取得普通文件，拒绝将目录作为字节输入。
	 *
	 * @param path 文件系统路径。
	 * @return 已存在的普通文件。
	 */
	private static File requireRegularFile(String path) {
		File file = requireExistingPath(path);

		try {
			if (!file.isFile()) {
				throw new 文件读写错误("目标不是普通文件：" + file.getPath());
			}

			return file;
		} catch (SecurityException exception) {
			throw pathError("访问文件失败", file, exception);
		}
	}

	/**
	 * 枚举一层文件或目录，空结果与枚举失败分别处理。
	 *
	 * @param path 目录路径。
	 * @param directories 是否只保留子目录。
	 * @return 排序后的绝对路径数组。
	 */
	private static String[] listPaths(String path, boolean directories) {
		File directory = requireExistingPath(path);

		try {
			List<String> paths = new ArrayList<String>();

			for (File child : listChildren(directory)) {
				if (directories ? child.isDirectory() : child.isFile()) {
					paths.add(child.getAbsolutePath());
				}
			}

			String[] result = paths.toArray(new String[paths.size()]);
			Arrays.sort(result);
			return result;
		} catch (IOException | SecurityException exception) {
			throw pathError("枚举目录失败", directory, exception);
		}
	}

	/**
	 * 读取目录项目；系统返回空引用时报告错误，不当成空目录。
	 *
	 * @param directory 待读取目录。
	 * @return 目录项目数组。
	 * @throws IOException 路径不是目录或无法枚举。
	 */
	private static File[] listChildren(File directory) throws IOException {
		if (!directory.isDirectory()) {
			throw new IOException("目标不是目录");
		}

		File[] children = directory.listFiles();
		if (children == null) {
			throw new IOException("无法枚举目录");
		}

		return children;
	}

	/**
	 * 识别当前目录项自身的符号链接，不受父目录路径别名影响。
	 *
	 * @param file 目录项。
	 * @return 当前项为符号链接时返回真。
	 * @throws IOException 无法解析规范路径。
	 */
	private static boolean isSymbolicLink(File file) throws IOException {
		File absolute = file.getAbsoluteFile();
		File parent = absolute.getParentFile();
		File resolved = parent == null ? absolute : new File(parent.getCanonicalFile(), absolute.getName());

		return !resolved.getCanonicalFile().equals(resolved.getAbsoluteFile());
	}

	/**
	 * 递归删除普通目录内容；目录链接只删除链接本身。
	 *
	 * @param directory 待删除目录。
	 * @throws IOException 枚举或删除失败。
	 */
	private static void deleteDirectory(File directory) throws IOException {
		if (!isSymbolicLink(directory)) {
			for (File child : listChildren(directory)) {
				if (child.isDirectory()) {
					deleteDirectory(child);
				} else if (!child.delete()) {
					throw new IOException("无法删除文件：" + child.getPath());
				}
			}
		}

		if (!directory.delete()) {
			throw new IOException("无法删除目录：" + directory.getPath());
		}
	}

	/**
	 * 累计目录中的普通文件大小，不遍历目录符号链接。
	 *
	 * @param file 文件或目录。
	 * @return 累计字节数。
	 * @throws IOException 无法枚举目录。
	 */
	private static long fileSize(File file) throws IOException {
		if (!file.isDirectory()) {
			return file.length();
		}

		long total = 0;
		if (!isSymbolicLink(file)) {
			for (File child : listChildren(file)) {
				total += fileSize(child);
			}
		}

		return total;
	}

	/**
	 * 验证字符集名称，参数无效时不开始文件写入。
	 *
	 * @param encoding Java 字符集名称。
	 * @return 已解析的字符集。
	 */
	private static Charset requireCharset(String encoding) {
		if (encoding == null || encoding.trim().length() == 0) {
			throw new IllegalArgumentException("编码名称不得为空");
		}

		return Charset.forName(encoding);
	}

	/**
	 * 清理路径上的关闭失败只记录日志，避免掩盖原始操作错误。
	 *
	 * @param stream 需要关闭的流，允许为空。
	 */
	private static void closeStream(java.io.Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException exception) {
				日志输出.warning("关闭文件流失败：%s", exception.getMessage());
			}
		}
	}

	/**
	 * 将新路径接口中的系统异常转换为 Simple 文件错误。
	 *
	 * @param message 操作说明。
	 * @param file 操作目标。
	 * @param cause 原始异常。
	 * @return 保留原始原因的文件读写错误。
	 */
	private static 文件读写错误 pathError(String message, File file, Exception cause) {
		文件读写错误 error = new 文件读写错误(message + "：" + file.getPath() + "；" + cause.getMessage());
		error.initCause(cause);
		return error;
	}
}

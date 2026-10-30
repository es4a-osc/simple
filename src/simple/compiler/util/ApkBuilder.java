package simple.compiler.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.util.zip.CRC32;

/**
 * 使用 JDK 归档接口组装未签名 APK，不依赖旧版 Android APK 组装库。
 *
 * <p>一次写入已经链接的 Android 资源、全部 DEX、所选 JAR 的 Java 资源和原生库；
 * Java 资源沿用统一合并规则，对齐与签名由后续工具完成。
 *
 * @author 树先生 xhwsd@qq.com
 */
final class ApkBuilder {

	/**
	 * 禁止实例化无状态的 APK 组装入口。
	 */
	private ApkBuilder() {
	}

	/**
	 * 在独立临时归档中完成组装，全部成功后替换目标文件。
	 *
	 * @param outputFile 未签名 APK 输出文件，不得与资源包、DEX 或依赖 JAR 相同。
	 * @param resourcesFile AAPT2 已链接的资源包。
	 * @param dexFiles 本次生成的全部 DEX，文件名须按 classes.dex、classes2.dex 等连续编号。
	 * @param jars 项目实际使用的扩展库及其依赖 JAR。
	 * @param nativeDirectories 按 ABI 子目录组织的原生库根目录。
	 * @throws IOException 输入无法读取、归档条目冲突或输出失败。
	 */
	static void build(File outputFile, File resourcesFile, List<File> dexFiles,
			List<File> jars, List<File> nativeDirectories) throws IOException {
		// 输出不能覆盖输入；Java 资源也在开始写入 APK 前完成检查。
		checkDistinct(outputFile, resourcesFile);
		if (dexFiles == null || dexFiles.isEmpty()) {
			throw new IOException("APK 缺少 DEX 输入。");
		}
		for (int index = 0; index < dexFiles.size(); index++) {
			File dex = dexFiles.get(index);
			String name = index == 0 ? "classes.dex" : "classes" + (index + 1) + ".dex";
			if (dex == null || !dex.isFile() || !name.equals(dex.getName())) {
				throw new IOException("DEX 输入不存在或编号不连续：" + dex);
			}
			checkDistinct(outputFile, dex);
		}
		for (File jar : jars) {
			checkDistinct(outputFile, jar);
		}
		JarResources resources = new JarResources(jars);
		Map<String, File> owners = new LinkedHashMap<String, File>();
		Path target = outputFile.toPath().toAbsolutePath();
		Path temporary = Files.createTempFile(target.getParent(), "apk-build-", ".apk");
		try {
			// 保留资源包的压缩方式，原生库以未压缩条目参与后续页对齐。
			try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(temporary))) {
				copyResources(resourcesFile, output, owners);
				for (File dex : dexFiles) {
					addFile(dex, dex.getName(), output, owners);
				}
				for (File directory : nativeDirectories) {
					addNativeDirectory(directory, output, owners);
				}

				// Java 资源直接进入同一归档，复用已有输入检查冲突，不再重写整份 APK。
				resources.writeTo(output, resourcesFile, owners);
			}

			// 全部内容和冲突检查成功后才替换目标，失败保留上次完整产物。
			Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	/**
	 * 检查输出与输入的规范路径，防止组装成功后覆盖输入文件。
	 *
	 * @param output 目标 APK 文件。
	 * @param input 必须保留的输入文件。
	 * @throws IOException 路径无法解析或输出与输入相同。
	 */
	private static void checkDistinct(File output, File input) throws IOException {
		if (output.getCanonicalFile().equals(input.getCanonicalFile())) {
			throw new IOException("APK 输出不能覆盖输入文件：" + input);
		}
	}

	/**
	 * 复制已链接的资源包，保留未压缩条目的方式并重新计算压缩条目大小。
	 *
	 * @param file 资源包输入文件。
	 * @param output 正在组装的归档。
	 * @param owners 已写入条目及其来源。
	 * @throws IOException 资源包损坏、路径无效或发现重复条目。
	 */
	private static void copyResources(File file, ZipOutputStream output, Map<String, File> owners) throws IOException {
		try (ZipFile input = new ZipFile(file)) {
			Enumeration<? extends ZipEntry> entries = input.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.isDirectory()) {
					continue;
				}
				remember(entry.getName(), file, owners);
				ZipEntry copy = new ZipEntry(entry);
				if (copy.getMethod() == ZipEntry.DEFLATED) {
					copy.setCompressedSize(-1);
				}
				output.putNextEntry(copy);
				try (InputStream stream = input.getInputStream(entry)) {
					copy(stream, output);
				}
				output.closeEntry();
			}
		} catch (IOException failure) {
			throw new IOException("读取 Android 资源包失败：" + file + "\n" + failure.getMessage(), failure);
		}
	}

	/**
	 * 只收集 ABI 目录第一层的 SO，保留既有大小写规则与目录层级。
	 *
	 * <p>输入沿用 AAR 的类库目录结构 {@code jni/<ABI>/*.so}，
	 * 打包后写入 APK 内的 {@code lib/<ABI>/*.so}。
	 * 根目录文件、嵌套目录和调试服务器不参与当前组装流程。
	 *
	 * @param root 扩展库的 {@code jni/} 原生库根目录。
	 * @param output 正在组装的归档。
	 * @param owners 已写入条目及其来源。
	 * @throws IOException 目录无法枚举、文件无法读取或原生库冲突。
	 */
	private static void addNativeDirectory(File root, ZipOutputStream output, Map<String, File> owners)
			throws IOException {
		for (File abi : list(root)) {
			if (!abi.isDirectory()) {
				continue;
			}
			for (File file : list(abi)) {
				String name = file.getName();
				if (file.isFile() && name.length() > 3 && name.toLowerCase(Locale.ROOT).endsWith(".so")) {
					addFile(file, "lib/" + abi.getName() + "/" + file.getName(), output, owners);
				}
			}
		}
	}

	/**
	 * 枚举并排序一个目录，避免文件系统返回顺序影响归档条目顺序。
	 *
	 * @param directory 需要枚举的目录。
	 * @return 排序后的子项。
	 * @throws IOException 路径不是可读取目录。
	 */
	private static File[] list(File directory) throws IOException {
		File[] files = directory.listFiles();
		if (files == null) {
			throw new IOException("无法读取原生库目录：" + directory);
		}
		Arrays.sort(files);
		return files;
	}

	/**
	 * 将 DEX 或原生库流式写入归档，不把整个文件载入内存。
	 *
	 * @param file 文件输入。
	 * @param name 目标归档路径。
	 * @param output 正在组装的归档。
	 * @param owners 已写入条目及其来源。
	 * @throws IOException 文件无法读取、路径无效或条目内容冲突。
	 */
	private static void addFile(File file, String name, ZipOutputStream output, Map<String, File> owners)
			throws IOException {
		File previous = owners.get(name);
		if (previous != null && name.startsWith("lib/")
				&& Arrays.equals(Files.readAllBytes(previous.toPath()), Files.readAllBytes(file.toPath()))) {
			return;
		}
		remember(name, file, owners);
		ZipEntry entry = new ZipEntry(name);
		entry.setTime(file.lastModified());
		if (name.startsWith("lib/") && name.endsWith(".so")) {
			// extractNativeLibs=false 时由系统直接映射 SO，必须使用可页对齐的 STORED 条目。
			CRC32 crc = new CRC32();
			try (InputStream input = Files.newInputStream(file.toPath())) {
				byte[] buffer = new byte[8192];
				for (int count; (count = input.read(buffer)) != -1;) {
					crc.update(buffer, 0, count);
				}
			}
			entry.setMethod(ZipEntry.STORED);
			entry.setSize(file.length());
			entry.setCrc(crc.getValue());
		}
		output.putNextEntry(entry);
		try (InputStream input = Files.newInputStream(file.toPath())) {
			copy(input, output);
		} catch (IOException failure) {
			throw new IOException("读取 APK 输入文件失败：" + file + "\n" + failure.getMessage(), failure);
		}
		output.closeEntry();
	}

	/**
	 * 统一验证条目路径并记录来源，重复条目必须报告双方输入。
	 *
	 * @param name 归档内的相对路径。
	 * @param file 当前输入来源。
	 * @param owners 已写入条目及其来源。
	 * @throws IOException 路径无效或条目已存在。
	 */
	private static void remember(String name, File file, Map<String, File> owners) throws IOException {
		JarResources.validatePath(name);
		File previous = owners.put(name, file);
		if (previous != null) {
			throw new IOException("APK 条目冲突：" + name + "\n来源：" + previous + "\n来源：" + file);
		}
	}

	/**
	 * 在调用方管理的流之间复制内容，不改变两端的关闭时机。
	 *
	 * @param input 当前输入流。
	 * @param output 当前输出归档。
	 * @throws IOException 输入读取或输出写入失败。
	 */
	private static void copy(InputStream input, ZipOutputStream output) throws IOException {
		byte[] buffer = new byte[8192];
		for (int count; (count = input.read(buffer)) != -1;) {
			output.write(buffer, 0, count);
		}
	}
}

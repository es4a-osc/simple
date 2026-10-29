package simple.compiler.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import junit.framework.TestCase;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * 用真实 D8 超过单 DEX 方法数的输入验证完整输出、旧文件清理和失败边界。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class ToolchainTest extends TestCase {

	/**
	 * 多 DEX 应保存所有类；缩小输入后移除旧次级文件；转换失败不覆盖成功结果。
	 *
	 * @throws Exception 测试输入创建、D8 执行或输出读取失败。
	 */
	public void testMultipleDexThenSingleDexAndFailedConversion() throws Exception {
		File parent = new File("build/tests/dex");
		Files.createDirectories(parent.toPath());
		File root = Files.createTempDirectory(parent.toPath(), "conversion-").toFile();
		File platform = new File(System.getenv("ANDROID_HOME"), "platforms/android-30/android.jar");
		File large = createClasses(root, "large.jar", 70, 1000);
		File directory = new File(root, "dex");
		ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
		PrintStream messages = new PrintStream(diagnostics, true, "UTF-8");

		// 真实输入包含 70000 个方法，必须由 D8 自行划分为多个有效 DEX。
		List<File> multiple = Toolchain.generateDex(directory, Arrays.asList(large), platform, 24, messages, messages);
		assertNotNull(diagnostics.toString("UTF-8"), multiple);
		assertTrue(multiple.size() > 1);
		int classes = 0;
		int methods = 0;
		for (int index = 0; index < multiple.size(); index++) {
			assertEquals(index == 0 ? "classes.dex" : "classes" + (index + 1) + ".dex", multiple.get(index).getName());
			byte[] bytes = Files.readAllBytes(multiple.get(index).toPath());
			assertEquals('d', bytes[0]);
			assertEquals('e', bytes[1]);
			assertEquals('x', bytes[2]);
			ByteBuffer header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
			int methodCount = header.getInt(88);
			assertTrue(methodCount <= 65536);
			methods += methodCount;
			classes += header.getInt(96);
		}
		assertEquals(70, classes);
		assertTrue(methods >= 70000);
		for (File file : multiple) {
			assertTrue(diagnostics.toString("UTF-8").contains("正在生成 DEX：" + file.getName()));
		}
		assertFalse(diagnostics.toString("UTF-8").contains("D8 已生成"));

		// 复用同一输出目录时，本次清单与目录都不能残留 classes2.dex。
		File marker = new File(directory, "keep.txt");
		Files.write(marker.toPath(), new byte[] {1});
		File small = createClasses(root, "small.jar", 1, 1);
		List<File> single = Toolchain.generateDex(directory, Arrays.asList(small), platform, 24, messages, messages);
		assertNotNull(diagnostics.toString("UTF-8"), single);
		assertEquals(1, single.size());
		assertFalse(new File(directory, "classes2.dex").exists());
		assertTrue(marker.isFile());
		byte[] previous = Files.readAllBytes(single.get(0).toPath());

		// 损坏输入必须返回失败，不能将旧文件作为“本次输出”交给 APK 组装。
		File invalid = new File(root, "invalid.class");
		Files.write(invalid.toPath(), new byte[] {0, 1, 2});
		assertNull(Toolchain.generateDex(directory, Arrays.asList(invalid), platform, 24, messages, messages));
		assertTrue(Arrays.equals(previous, Files.readAllBytes(single.get(0).toPath())));
	}

	/**
	 * 创建仅含静态方法的类归档，控制方法数量而不依赖外部大类库。
	 *
	 * @param root 测试目录。
	 * @param name 输出归档名。
	 * @param count 类数量。
	 * @param methods 每个类的静态方法数量。
	 * @return 已写入的 JAR。
	 * @throws Exception 生成字节码或写入归档失败。
	 */
	private File createClasses(File root, String name, int count, int methods) throws Exception {
		File jar = new File(root, name);
		try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar.toPath()))) {
			for (int index = 0; index < count; index++) {
				String type = "fixture/dex/Generated" + index;
				ClassWriter writer = new ClassWriter(0);
				writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, type, null, "java/lang/Object", null);
				for (int number = 0; number < methods; number++) {
					MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
							"method" + number, "()V", null, null);
					method.visitCode();
					method.visitInsn(Opcodes.RETURN);
					method.visitMaxs(0, 0);
					method.visitEnd();
				}
				writer.visitEnd();
				output.putNextEntry(new JarEntry(type + ".class"));
				output.write(writer.toByteArray());
				output.closeEntry();
			}
		}
		return jar;
	}
}

package simple.compiler.util;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 识别基础类条目和模块描述，供类库分析与重复类检查共用。
 *
 * <p>只读取 class 访问标志之前的结构，不加载类，不改写原文件，
 * 也不预判普通类的 Java 或 Android API 兼容性。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class ClassFiles {

	private static final int ACC_MODULE = 0x8000;

	/**
	 * 禁止实例化无状态的类文件识别工具。
	 */
	private ClassFiles() {
	}

	/**
	 * 判断相对路径是否属于当前使用的基础类条目。
	 *
	 * @param name 使用斜杠分隔的 JAR 条目或类目录相对路径。
	 * @return 基础 class 条目返回 {@code true}，元数据和版本目录返回 {@code false}。
	 */
	public static boolean isBaseClass(String name) {
		return name.endsWith(".class") && !name.startsWith("META-INF/");
	}

	/**
	 * 按文件内容判断模块描述；输入流由调用方关闭。
	 *
	 * @param stream 位于 class 文件起始位置的输入流。
	 * @return 设置了 {@code ACC_MODULE} 标志时返回 {@code true}。
	 * @throws IOException 文件截断、标识错误或常量池格式无法识别。
	 */
	public static boolean isModule(InputStream stream) throws IOException {
		DataInputStream input = new DataInputStream(new BufferedInputStream(stream));
		// 访问标志位于可变长度的常量池之后，不能通过固定偏移读取。
		if (input.readInt() != 0xCAFEBABE) {
			throw new IOException("不是有效的 class 文件。");
		}
		input.readUnsignedShort();
		input.readUnsignedShort();
		int count = input.readUnsignedShort();
		if (count == 0) {
			throw new IOException("class 常量池长度无效。");
		}

		// 跳过常量内容；包括 Java 9 模块常量和动态常量，不解析业务字节码。
		for (int index = 1; index < count; index++) {
			int tag = input.readUnsignedByte();
			switch (tag) {
				case 1:
					skip(input, input.readUnsignedShort());
					break;
				case 3:
				case 4:
				case 9:
				case 10:
				case 11:
				case 12:
				case 17:
				case 18:
					skip(input, 4);
					break;
				case 5:
				case 6:
					skip(input, 8);
					if (++index >= count) {
						throw new IOException("class 双槽常量超出常量池。");
					}
					break;
				case 7:
				case 8:
				case 16:
				case 19:
				case 20:
					skip(input, 2);
					break;
				case 15:
					skip(input, 3);
					break;
				default:
					throw new IOException("无法识别 class 常量池标记：" + tag);
			}
		}

		return (input.readUnsignedShort() & ACC_MODULE) != 0;
	}

	/**
	 * 完整跳过指定字节数，避免短跳过或截断文件被误认为普通类。
	 *
	 * @param input 当前 class 输入流。
	 * @param length 要跳过的字节数。
	 * @throws IOException 输入截断或底层读取失败。
	 */
	private static void skip(DataInputStream input, int length) throws IOException {
		while (length > 0) {
			int skipped = input.skipBytes(length);
			if (skipped == 0) {
				input.readByte();
				skipped = 1;
			}
			length -= skipped;
		}
	}
}

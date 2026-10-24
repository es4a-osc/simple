package simple.runtime;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Arrays;

import junit.framework.TestCase;

import simple.runtime.errors.文件已存在错误;
import simple.runtime.errors.文件未存在错误;
import simple.runtime.errors.文件读写错误;

/**
 * 验证整文件读写、路径操作及错误情况下的数据保护。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class FilesPathTest extends TestCase {

	private File root;
	private File previousRoot;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Field field = 文件操作.class.getDeclaredField("rootDirectory");
		field.setAccessible(true);
		previousRoot = (File) field.get(null);
		root = Files.createTempDirectory("es4a-file-path-test-").toFile();
		文件操作.initialize(root);
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			文件操作.删除目录(new File(root, "work").getAbsolutePath());
			File[] children = root.listFiles();
			if (children != null) {
				for (File child : children) {
					if (child.isDirectory()) {
						文件操作.删除目录(child.getAbsolutePath());
					} else {
						assertTrue(child.delete());
					}
				}
			}
			assertTrue(root.delete());
		} finally {
			文件操作.initialize(previousRoot);
			super.tearDown();
		}
	}

	/** 验证默认 UTF-8、指定 GBK、空文本及覆盖写入。 */
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

	/** 数据超过一次缓冲区，复制覆盖后逐字节检查尾部没有残留。 */
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

	/** 创建不覆盖，改名冲突和复制自身均保留源文件。 */
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

	/** 目录枚举只返回下一级对应类型，结果为有序绝对路径。 */
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

	/** 空目录接口不递归，递归接口删除目录后再次调用返回假。 */
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

	/** 覆盖五种 BOM 及短文件，不把无标记内容误当成已识别编码。 */
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

	/** 参数错误应先失败，不能截断已有目标或把读取失败当成空文件。 */
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

	/** 文件、目录类型错误及缺失父目录应报告失败。 */
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

	/** 相对路径、绝对路径指向同一文件，根目录及祖先不能递归清理。 */
	public void testRelativeAndAbsolutePaths() {
		文件操作.写出文本文件("relative.txt", "相对路径");
		File absolute = new File(root, "relative.txt");
		assertEquals("相对路径", 文件操作.读入文本文件(absolute.getAbsolutePath()));
		文件操作.写出文本文件(absolute.getAbsolutePath(), "绝对路径");
		assertEquals("绝对路径", 文件操作.读入文本文件("relative.txt"));
		assertTrue(文件操作.文件是否存在(absolute.getAbsolutePath()));
	}

	/** 删除保护触发后，已有文件和测试根目录必须保持原样。 */
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

	/** 可用空间随系统活动变化，只检查有效范围，不断言每次查询结果完全相同。 */
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

	/** 无效或缺失路径沿用路径接口的错误约定，不以容量 0 掩盖缺失路径。 */
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

	/** 查询不创建探测文件、不改写内容，也不改变当前目录项目。 */
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

	/** 未初始化的默认查询不能意外改为查询宿主进程工作目录。 */
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

package simple.compiler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

/**
 * 验证项目工具读取编译器构建目录的命令行契约，不复制 Java Properties 的解析规则。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class MainTest extends TestCase {

	/**
	 * 默认、空值、重复键、中文空格和转义续行均通过正式项目模型解析，不生成构建目录。
	 *
	 * @throws Exception 夹具无法创建、查询失败或输出与项目配置不符。
	 */
	public void testBuildDirectoryQueryUsesProjectProperties() throws Exception {
		File parent = new File("build/tests/project-tool");
		Files.createDirectories(parent.toPath());
		File root = Files.createTempDirectory(parent.toPath(), "项目 中文 空格-").toFile().getCanonicalFile();
		File project = new File(root, "project.properties");
		String[][] cases = {
			{"name=查询测试\r\n", "build"},
			{"build=\r\n", "build"},
			{"build=./unused\r\nbuild : ./输出\\ 目录\r\n", "输出 目录"},
			{"build=./\\u76ee\\u5f55/\\\r\n\t子\\ 目录\r\n", "目录/子 目录"}
		};
		for (String[] value : cases) {
			Files.writeString(project.toPath(), value[0], StandardCharsets.UTF_8);
			String result = runMain(root, 0, "--build-directory", project.toString());
			File output = new File(root, value[1]).getCanonicalFile();
			assertEquals(output.toString(), result.trim());
			assertFalse("目录查询不得创建构建产物", output.exists());
		}
	}

	/**
	 * 错误参数或缺失的项目文件必须返回非零退出码，不能提供可供安装的伪造路径。
	 *
	 * @throws Exception 子进程无法执行或退出状态与诊断不符。
	 */
	public void testInvalidBuildDirectoryQueryFails() throws Exception {
		File parent = new File("build/tests/project-tool");
		Files.createDirectories(parent.toPath());
		File root = Files.createTempDirectory(parent.toPath(), "invalid-").toFile().getCanonicalFile();
		assertTrue(runMain(root, 1, "--build-directory", new File(root, "missing.properties").toString()).contains("无法读取项目文件"));
		assertTrue(runMain(root, 1, "--unknown", "project.properties").contains("用法"));
	}

	/**
	 * 在独立 JVM 执行正式入口，核对退出码并读取 UTF-8 输出。
	 *
	 * @param root 本用例生成目录。
	 * @param expectedExit 预期进程退出码。
	 * @param arguments 正式入口参数。
	 * @return 标准输出与错误输出的合并文本。
	 * @throws Exception 进程无法启动、超时、输出无法读取或退出码不符。
	 */
	private String runMain(File root, int expectedExit, String... arguments) throws Exception {
		List<String> command = new ArrayList<String>(Arrays.asList(
				new File(System.getProperty("java.home"), "bin/java").getPath(), "-Dfile.encoding=UTF-8",
				"-cp", System.getProperty("java.class.path"), Main.class.getName()));
		command.addAll(Arrays.asList(arguments));
		File log = new File(root, "query.log");
		Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log).start();
		if (!process.waitFor(15, TimeUnit.SECONDS)) {
			process.destroyForcibly();
			throw new IOException("等待构建目录查询超时：" + log);
		}
		String output = Files.readString(log.toPath(), StandardCharsets.UTF_8);
		assertEquals(output, expectedExit, process.exitValue());
		return output;
	}
}

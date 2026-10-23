package simple.compiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import simple.compiler.parser.Parser;
import simple.compiler.scanner.Scanner;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.components.语音处理器;

/**
 * 验证语音组件的导出契约、真实示例编译及按需生成的 Android 11 包可见性声明。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class SpeechManifestTest extends TestCase {

	/**
	 * 真实示例必须能使用受理返回值、只读状态和全部语音事件。
	 *
	 * @throws Exception 导出成员缺失或真实项目读取失败
	 */
	public void testSpeechContractAndRealExampleCompile() throws Exception {
		for (String name : new String[]{"是否支持语音识别", "语音识别", "初始化语音朗读"}) {
			Method method = 语音处理器.class.getMethod(name);
			assertEquals(boolean.class, method.getReturnType());
			assertTrue(method.isAnnotationPresent(SimpleFunction.class));
		}
		Method speak = 语音处理器.class.getMethod("语音朗读", String.class);
		assertEquals(boolean.class, speak.getReturnType());
		assertTrue(speak.isAnnotationPresent(SimpleFunction.class));
		for (String name : new String[]{"正在识别", "朗读就绪", "错误信息"}) {
			assertTrue(语音处理器.class.getMethod(name).isAnnotationPresent(SimpleProperty.class));
		}
		for (String name : new String[]{"语音识别取消", "语音朗读就绪"}) {
			assertTrue(语音处理器.class.getMethod(name).isAnnotationPresent(SimpleEvent.class));
		}
		assertTrue(语音处理器.class.getMethod("操作失败", String.class, String.class)
			.isAnnotationPresent(SimpleEvent.class));
		assertNotNull(Compiler.compileForUnitTesting(new Project(new File("examples/语音处理器/project.properties"))));
	}

	/**
	 * 语音示例仅查询识别界面与朗读服务，并保持本次升级的最低和目标 API。
	 *
	 * @throws Exception 项目解析、清单生成或 XML 读取失败。
	 */
	public void testSpeechExampleDeclaresRecognitionAndTtsQueries() throws Exception {
		Document manifest = manifest("examples/语音处理器/project.properties");
		NodeList queries = manifest.getElementsByTagName("queries");
		assertEquals(1, queries.getLength());
		assertSame(manifest.getDocumentElement(), queries.item(0).getParentNode());

		// 只开放组件实际使用的两类引擎，避免扩大到任意应用或其它识别接口。
		Element query = (Element) queries.item(0);
		NodeList intents = query.getElementsByTagName("intent");
		assertEquals(2, intents.getLength());
		Set<String> actions = new HashSet<String>();

		for (int i = 0; i < intents.getLength(); i++) {
			NodeList entries = ((Element) intents.item(i)).getElementsByTagName("action");
			assertEquals(1, entries.getLength());
			actions.add(((Element) entries.item(0)).getAttributeNS(
				"http://schemas.android.com/apk/res/android", "name"));
		}

		assertEquals(new HashSet<String>(Arrays.asList(
			"android.speech.action.RECOGNIZE_SPEECH", "android.intent.action.TTS_SERVICE")), actions);

		// 查询声明不提高安装门槛，也不申请查看所有应用的权限。
		Element sdk = (Element) manifest.getElementsByTagName("uses-sdk").item(0);
		assertEquals("24", sdk.getAttributeNS("http://schemas.android.com/apk/res/android", "minSdkVersion"));
		assertEquals("30", sdk.getAttributeNS("http://schemas.android.com/apk/res/android", "targetSdkVersion"));
		NodeList permissions = manifest.getElementsByTagName("uses-permission");

		for (int i = 0; i < permissions.getLength(); i++) {
			assertFalse("android.permission.QUERY_ALL_PACKAGES".equals(
				((Element) permissions.item(i)).getAttributeNS(
					"http://schemas.android.com/apk/res/android", "name")));
		}
	}

	/**
	 * 未使用语音组件的真实示例不携带语音查询声明。
	 *
	 * @throws Exception 项目解析、清单生成或 XML 读取失败。
	 */
	public void testUnrelatedExampleDoesNotDeclareSpeechQueries() throws Exception {
		Document manifest = manifest("examples/标签/project.properties");
		assertEquals(0, manifest.getElementsByTagName("queries").getLength());
	}

	/**
	 * 解析真实项目并调用现有清单生成入口，验证结果 XML 后清理临时文件。
	 *
	 * @param path 相对于 Simple 仓库的项目清单路径。
	 * @return 由当前编译器生成的 Android 清单文档。
	 * @throws Exception 项目解析、反射调用、临时文件操作或 XML 读取失败。
	 */
	private Document manifest(String path) throws Exception {
		ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();

		try (PrintStream messages = new PrintStream(diagnostics, true, "UTF-8")) {
			Compiler compiler = new Compiler(Compiler.Platform.Android, messages, messages);
			Project project = new Project(path, null);

			// 使用实际单元解析及符号解析链路，保留运行库按引用收集声明的行为。
			for (Project.SourceDescriptor descriptor : project.getSources()) {
				String source = new String(Files.readAllBytes(descriptor.getFile().toPath()), StandardCharsets.UTF_8);
				new Parser(compiler, new Scanner(compiler, source), descriptor.getQualifiedName()).parse();
			}

			if (compiler.getErrorCount() == 0) {
				compiler.resolve();
			}

			assertEquals(diagnostics.toString("UTF-8"), 0, compiler.getErrorCount());
			Method permissions = Compiler.class.getDeclaredMethod("generatePermissions");
			permissions.setAccessible(true);
			Method write = Compiler.class.getDeclaredMethod("writeAndroidManifest",
				Compiler.class, Project.class, File.class, Set.class);
			write.setAccessible(true);

			// 每次写入独立系统临时目录，不接触示例构建目录和批量测试产物。
			Path directory = Files.createTempDirectory("simple-speech-manifest-");
			Path output = directory.resolve("AndroidManifest.xml");

			try {
				assertEquals(Boolean.TRUE, write.invoke(null,
					compiler, project, directory.toFile(), permissions.invoke(compiler)));
				assertEquals(diagnostics.toString("UTF-8"), 0, compiler.getErrorCount());
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setNamespaceAware(true);
				return factory.newDocumentBuilder().parse(output.toFile());
			} finally {
				Files.deleteIfExists(output);
				Files.deleteIfExists(directory);
			}
		}
	}
}

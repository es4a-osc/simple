package simple.compiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * 验证编译器生成的应用清单保留网络功能所需的明文访问声明。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class AndroidManifestTest extends TestCase {

	/**
	 * 实际生成清单，确认目标 API 30 的应用显式允许 HTTP、ws:// 等明文连接。
	 *
	 * @throws Exception 项目读取、清单生成或 XML 解析失败
	 */
	public void testAllowsCleartextTrafficForNetworkComponents() throws Exception {
		Path directory = Files.createTempDirectory("simple-network-manifest-");
		Path output = directory.resolve("AndroidManifest.xml");
		ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();

		try (PrintStream messages = new PrintStream(diagnostics, true, "UTF-8")) {
			Compiler compiler = new Compiler(Compiler.Platform.Android, messages, messages);
			Project project = new Project(new File("examples/HTTP请求/project.properties"));
			Method write = Compiler.class.getDeclaredMethod("writeAndroidManifest",
				Compiler.class, Project.class, File.class, Set.class);
			write.setAccessible(true);
			assertEquals(Boolean.TRUE, write.invoke(null, compiler, project,
				directory.toFile(), Collections.<String>emptySet()));
			assertEquals(diagnostics.toString("UTF-8"), 0, compiler.getErrorCount());

			// 检查正式生成的 XML 属性，避免只匹配源码字符串而遗漏实际输出。
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			Document manifest = factory.newDocumentBuilder().parse(output.toFile());
			String android = "http://schemas.android.com/apk/res/android";
			Element application = (Element) manifest.getElementsByTagName("application").item(0);
			assertEquals("true", application.getAttributeNS(android, "usesCleartextTraffic"));
			Element sdk = (Element) manifest.getElementsByTagName("uses-sdk").item(0);
			assertEquals("30", sdk.getAttributeNS(android, "targetSdkVersion"));
			assertEquals("24", sdk.getAttributeNS(android, "minSdkVersion"));
		} finally {
			Files.deleteIfExists(output);
			Files.deleteIfExists(directory);
		}
	}
}

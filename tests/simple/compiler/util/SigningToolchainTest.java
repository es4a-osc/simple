package simple.compiler.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

/**
 * 使用真实 Android 签名工具验证双签名、密钥兼容、对齐及失败边界。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class SigningToolchainTest extends TestCase {

	private Path root;
	private File unsignedApk;
	private ByteArrayOutputStream diagnostics;
	private PrintStream log;

	@Override
	protected void setUp() throws Exception {
		Path base = new File("build/tests/signing").toPath().toAbsolutePath();
		Files.createDirectories(base);
		root = Files.createTempDirectory(base, "中文 空格-");
		diagnostics = new ByteArrayOutputStream();
		log = new PrintStream(diagnostics, true, "UTF-8");

		// 用 AAPT2 生成真实的二进制清单，避免签名测试依赖已有示例产物。
		Path manifest = root.resolve("AndroidManifest.xml");
		String xml = "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\""
			+ " package=\"simple.tests.signing\" android:versionCode=\"1\">"
			+ "<uses-sdk android:minSdkVersion=\"14\" android:targetSdkVersion=\"26\"/>"
			+ "<application android:hasCode=\"false\" android:label=\"签名测试\"/></manifest>";
		Files.write(manifest, xml.getBytes(StandardCharsets.UTF_8));
		unsignedApk = root.resolve("未签名 应用.apk").toFile();

		int exitCode = run(androidTool("aapt2"), "link", "-o", unsignedApk.getAbsolutePath(),
			"--manifest", manifest.toString(), "-I",
			new File(System.getenv("ANDROID_HOME"), "platforms/android-26/android.jar").getAbsolutePath());
		assertEquals(messages(), 0, exitCode);
	}

	@Override
	protected void tearDown() {
		log.close();
	}

	/**
	 * 调试 PKCS12 保留原证书，并生成可通过最低 API 14 校验的 V1 和 V2 签名。
	 *
	 * @throws Exception 密钥库或校验结果读取失败
	 */
	public void testDebugKeyAndDualSignatures() throws Exception {
		File key = root.resolve("中文 调试.p12").toFile();
		boolean created = Toolchain.createDebugKeystore(key, log);
		assertTrue(messages(), created);

		File aligned = align();
		File signed = root.resolve("已签名 应用.apk").toFile();
		boolean success = Toolchain.signApplication(key.getAbsolutePath(), "pass:android",
			aligned, signed, "AndroidDebugKey", "PKCS12", log);
		assertTrue(messages(), success);

		boolean verified = Toolchain.verifyApplication(signed, log, log);
		assertTrue(messages(), verified);
		checkSignaturesAndCertificate(signed, key, "PKCS12", "android", "AndroidDebugKey");

		// 签名完成后仍须保持条目及原生库对齐。
		int exitCode = run(androidTool("zipalign"), "-c", "-p", "4", signed.getAbsolutePath());
		assertEquals(messages(), 0, exitCode);
	}

	/**
	 * 发布 JKS 和 PKCS12 沿用默认格式识别，不要求项目转换现有密钥库。
	 *
	 * @throws Exception 密钥生成、读取或工具调用失败
	 */
	public void testReleaseKeystoreFormats() throws Exception {
		File aligned = align();
		for (String type : new String[]{"JKS", "PKCS12"}) {
			File key = root.resolve("发布 " + type + ".keystore").toFile();
			int exitCode = run(javaTool("keytool"), "-genkeypair", "-storetype", type,
				"-keystore", key.getAbsolutePath(), "-alias", "release", "-keyalg", "RSA",
				"-storepass", "test-only-password", "-keypass", "test-only-password",
				"-dname", "CN=Signing Test", "-validity", "365", "-noprompt");
			assertEquals(messages(), 0, exitCode);

			File signed = root.resolve(type + ".apk").toFile();
			boolean success = Toolchain.signApplication(key.getAbsolutePath(), "pass:test-only-password",
				aligned, signed, "release", null, log);
			assertTrue(messages(), success);
			checkSignaturesAndCertificate(signed, key, type, "test-only-password", "release");
		}
	}

	/**
	 * 错误口令必须直接失败，不等待交互输入或生成可交付文件。
	 *
	 * @throws Exception 测试输入准备失败
	 */
	public void testWrongPasswordFails() throws Exception {
		File key = root.resolve("debug.p12").toFile();
		boolean created = Toolchain.createDebugKeystore(key, log);
		assertTrue(messages(), created);
		File aligned = align();
		File signed = root.resolve("failed.apk").toFile();

		assertFalse(Toolchain.signApplication(key.getAbsolutePath(), "pass:wrong-password",
			aligned, signed, "AndroidDebugKey", "PKCS12", log));
		assertFalse(signed.exists());
	}

	/**
	 * 尚未签名的 APK 不能通过最终校验。
	 */
	public void testUnsignedApkFailsVerification() {
		assertFalse(Toolchain.verifyApplication(unsignedApk, log, log));
	}

	/**
	 * 签名完成后修改 APK，最终校验必须拒绝该文件。
	 *
	 * @throws Exception 测试文件读写失败
	 */
	public void testChangedApkFailsVerification() throws Exception {
		File key = root.resolve("debug.p12").toFile();
		boolean created = Toolchain.createDebugKeystore(key, log);
		assertTrue(messages(), created);
		File aligned = align();
		File signed = root.resolve("changed.apk").toFile();
		boolean success = Toolchain.signApplication(key.getAbsolutePath(), "pass:android",
			aligned, signed, "AndroidDebugKey", "PKCS12", log);
		assertTrue(messages(), success);

		Files.write(signed.toPath(), new byte[]{1}, StandardOpenOption.APPEND);
		assertFalse(Toolchain.verifyApplication(signed, log, log));
	}

	/**
	 * 损坏的 ZIP 输入必须在对齐阶段失败。
	 *
	 * @throws Exception 测试文件写入失败
	 */
	public void testInvalidZipFailsAlignment() throws Exception {
		File invalid = root.resolve("invalid.apk").toFile();
		Files.write(invalid.toPath(), new byte[]{0, 1, 2});

		assertFalse(Toolchain.alignApplication(invalid, root.resolve("invalid-aligned.apk").toFile(), log));
	}

	/**
	 * 对齐当前用例的 APK。
	 *
	 * @return 已对齐的 APK
	 */
	private File align() {
		File aligned = root.resolve("已对齐 应用.apk").toFile();
		boolean success = Toolchain.alignApplication(unsignedApk, aligned, log);
		assertTrue(messages(), success);
		return aligned;
	}

	/**
	 * 校验最低版本、双签名及证书摘要，避免工具切换引入新身份。
	 *
	 * @param apk 已签名 APK
	 * @param key 密钥库
	 * @param type 密钥库格式
	 * @param password 测试密钥库口令
	 * @param alias 密钥别名
	 * @throws Exception 工具调用或证书读取失败
	 */
	private void checkSignaturesAndCertificate(File apk, File key, String type,
			String password, String alias) throws Exception {
		KeyStore store = KeyStore.getInstance(type);
		try (InputStream input = Files.newInputStream(key.toPath())) {
			store.load(input, password.toCharArray());
		}
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(store.getCertificate(alias).getEncoded());
		StringBuilder expected = new StringBuilder();
		for (byte value : digest) {
			expected.append(String.format("%02x", value & 0xff));
		}

		diagnostics.reset();
		int exitCode = run(javaTool("java"), "-jar", apksigner(), "verify", "--verbose",
			"--print-certs", "--min-sdk-version", "14", apk.getAbsolutePath());
		assertEquals(messages(), 0, exitCode);
		assertTrue(messages().contains("Verified using v1 scheme (JAR signing): true"));
		assertTrue(messages().contains("Verified using v2 scheme (APK Signature Scheme v2): true"));
		assertTrue(messages().contains("certificate SHA-256 digest: " + expected));
	}

	/**
	 * 运行真实工具并完整收集输出，供测试断言使用。
	 *
	 * @param command 工具路径与参数
	 * @return 进程退出码
	 * @throws Exception 进程启动或输出读取失败
	 */
	private int run(String... command) throws Exception {
		List<String> arguments = new ArrayList<String>(Arrays.asList(command));
		ProcessBuilder builder = new ProcessBuilder(arguments);
		builder.redirectErrorStream(true);
		Process process = builder.start();
		try (InputStream input = process.getInputStream()) {
			byte[] buffer = new byte[4096];
			int count;
			while ((count = input.read(buffer)) != -1) {
				diagnostics.write(buffer, 0, count);
			}
		}
		return process.waitFor();
	}

	/**
	 * 定位当前构建使用的 JDK 工具。
	 *
	 * @param name 工具名称
	 * @return 工具绝对路径
	 */
	private String javaTool(String name) {
		return new File(System.getenv("JAVA_HOME"), "bin/" + name + executableExtension()).getAbsolutePath();
	}

	/**
	 * 定位当前 SDK 中的 Android 原生工具。
	 *
	 * @param name 工具名称
	 * @return 工具绝对路径
	 */
	private String androidTool(String name) {
		return new File(System.getenv("ANDROID_HOME"),
			"platforms/android-26/tools/" + name + executableExtension()).getAbsolutePath();
	}

	/**
	 * 定位当前 SDK 中的 APK 签名工具。
	 *
	 * @return JAR 绝对路径
	 */
	private String apksigner() {
		return new File(System.getenv("ANDROID_HOME"),
			"platforms/android-26/tools/lib/apksigner.jar").getAbsolutePath();
	}

	/**
	 * 返回当前平台的工具后缀。
	 *
	 * @return Windows 为 {@code .exe}，其他系统为空文本
	 */
	private String executableExtension() {
		return System.getProperty("os.name").startsWith("Windows") ? ".exe" : "";
	}

	/**
	 * 返回当前用例收集的工具输出。
	 *
	 * @return UTF-8 诊断文本
	 */
	private String messages() {
		return new String(diagnostics.toByteArray(), StandardCharsets.UTF_8);
	}
}

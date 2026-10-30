package simple.compiler;

import junit.framework.TestCase;
import simple.compiler.util.MavenRepository;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 验证离线仓库内容、元数据递归、目录互补和冲突处理，不访问网络或个人 Gradle 缓存。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class MavenRepositoryTest extends TestCase {

	private File root;

	/**
	 * 每个用例使用独立临时仓库。
	 *
	 * @throws Exception 目录无法创建。
	 */
	@Override
	protected void setUp() throws Exception {
		root = Files.createTempDirectory("simple-maven-repository-").toFile();
	}

	/**
	 * 从两个目录补齐同一模块的 POM 和 JAR，并同时保留不同版本。
	 *
	 * @throws Exception 仓库合并或文件校验失败。
	 */
	public void testFilesAndVersionsComplementEachOther() throws Exception {
		File first = new File(root, "first");
		File second = new File(root, "second");
		write(new File(first, "test/sample/1.0/sample-1.0.pom"), pom("sample", "1.0", ""));
		write(new File(second, "test/sample/1.0/sample-1.0.jar"), "one");
		write(new File(second, "test/sample/2.0/sample-2.0.pom"), pom("sample", "2.0", ""));
		write(new File(second, "test/sample/2.0/sample-2.0.jar"), "two");
		File target = new File(root, "merged");
		MavenRepository.merge(Arrays.asList(first, second), target);
		assertTrue(new File(target, "test/sample/1.0/sample-1.0.pom").isFile());
		assertTrue(new File(target, "test/sample/1.0/sample-1.0.jar").isFile());
		String versions = Files.readString(new File(target, "test/sample/maven-metadata.xml").toPath());
		assertTrue(versions.contains("<version>1.0</version>"));
		assertTrue(versions.contains("<version>2.0</version>"));
	}

	/**
	 * 同一路径不同内容不能按仓库顺序静默覆盖，失败保留原目标。
	 *
	 * @throws Exception 夹具准备或目标校验失败。
	 */
	public void testConflictLeavesPreviousRepositoryUntouched() throws Exception {
		File first = new File(root, "first");
		File second = new File(root, "second");
		write(new File(first, "test/sample/1.0/sample-1.0.jar"), "one");
		write(new File(second, "test/sample/1.0/sample-1.0.jar"), "different");
		File target = new File(root, "merged");
		write(new File(target, "previous.txt"), "keep");
		try {
			MavenRepository.merge(Arrays.asList(first, second), target);
			fail("不同内容必须报错");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("离线依赖内容冲突"));
		}
		assertEquals("keep", Files.readString(new File(target, "previous.txt").toPath()));
	}

	/**
	 * 仓库的新增、删除及内容变化均影响指纹。
	 *
	 * @throws Exception 文件或指纹无法读取。
	 */
	public void testFingerprintTracksActualRepositoryContents() throws Exception {
		File repository = new File(root, "repository");
		String empty = MavenRepository.fingerprint(Collections.singletonList(repository));
		File file = new File(repository, "test/sample/1.0/sample-1.0.jar");
		write(file, "one");
		String first = MavenRepository.fingerprint(Collections.singletonList(repository));
		assertFalse(empty.equals(first));
		write(file, "two");
		assertFalse(first.equals(MavenRepository.fingerprint(Collections.singletonList(repository))));
		Files.delete(file.toPath());
		assertEquals(empty, MavenRepository.fingerprint(Collections.singletonList(repository)));
	}

	/**
	 * 父 POM 属性和导入 BOM 的原始文件必须随模块保留，不改写选版声明。
	 *
	 * @throws Exception 元数据导出或校验失败。
	 */
	public void testExportRetainsParentPomAndImportedBom() throws Exception {
		final Map<String, File> metadata = new HashMap<String, File>();
		metadata.put("test:parent:1.0@pom", write(new File(root, "parent.pom"), pom("parent", "1.0",
				"<packaging>pom</packaging><properties><bom.version>2.0</bom.version></properties>")));
		metadata.put("test:bom:2.0@pom", write(new File(root, "bom.pom"), pom("bom", "2.0", "<packaging>pom</packaging>")));
		String body = "<parent><groupId>test</groupId><artifactId>parent</artifactId><version>1.0</version></parent>"
				+ "<dependencyManagement><dependencies><dependency><groupId>test</groupId><artifactId>bom</artifactId>"
				+ "<version>${bom.version}</version><type>pom</type><scope>import</scope></dependency></dependencies></dependencyManagement>";
		File pom = write(new File(root, "sample.pom"), pom("sample", "1.0", body));
		metadata.put("test:sample:1.0@pom", pom);
		File archive = write(new File(root, "sample-1.0.jar"), "sample");
		File target = new File(root, "repository");
		MavenRepository.export(target, Arrays.asList(new MavenRepository.Artifact("test:sample:1.0", archive)),
				Arrays.asList("test:sample:1.0"), source(metadata));
		assertEquals(Files.readString(pom.toPath()), Files.readString(new File(target, "test/sample/1.0/sample-1.0.pom").toPath()));
		assertTrue(new File(target, "test/parent/1.0/parent-1.0.pom").isFile());
		assertTrue(new File(target, "test/bom/2.0/bom-2.0.pom").isFile());
		assertFalse(new File(target, "test/bom/2.0/bom-2.0.jar").exists());
	}

	/**
	 * 缺少重定向的原生描述时，不允许以 POM 冒充完整元数据。
	 *
	 * @throws Exception 夹具准备或目标校验失败。
	 */
	public void testMissingGradleMetadataIsNotFlattenedToPom() throws Exception {
		Map<String, File> metadata = new HashMap<String, File>();
		metadata.put("test:sample:1.0@pom", write(new File(root, "sample.pom"),
				pom("sample", "1.0", "<!-- do_not_remove: published-with-gradle-metadata -->")));
		try {
			MavenRepository.export(new File(root, "repository"), Collections.<MavenRepository.Artifact>emptyList(),
					Arrays.asList("test:sample:1.0"), source(metadata));
			fail("不能丢弃原生元数据");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("Gradle 元数据"));
		}
	}

	/**
	 * 原始 .module 完整保留，摘要错误的制品不能形成成功的交付。
	 *
	 * @throws Exception 夹具准备或元数据校验失败。
	 */
	public void testModuleDigestRejectsCorruptArtifact() throws Exception {
		Map<String, File> metadata = new HashMap<String, File>();
		metadata.put("test:sample:1.0@module", write(new File(root, "sample.module"),
				"{\"formatVersion\":\"1.1\",\"variants\":[{\"name\":\"runtime\",\"files\":["
				+ "{\"name\":\"sample-1.0.jar\",\"url\":\"sample-1.0.jar\",\"sha256\":\"wrong\"}]}]}"));
		File archive = write(new File(root, "sample-1.0.jar"), "sample");
		try {
			MavenRepository.export(new File(root, "repository"), Arrays.asList(new MavenRepository.Artifact("test:sample:1.0", archive)),
					Arrays.asList("test:sample:1.0"), source(metadata));
			fail("摘要错误必须报错");
		} catch (IOException expected) {
			assertTrue(expected.getMessage().contains("摘要不符"));
		}
	}

	/**
	 * Maven 坐标不能逃逸仓库根目录。
	 *
	 * @throws Exception 合法坐标或失败诊断无法校验。
	 */
	public void testCoordinatesCannotEscapeRepository() throws Exception {
		for (String id : Arrays.asList("../test:sample:1.0", "test:../sample:1.0", "test:sample:../1.0")) {
			try {
				MavenRepository.directory(root, id);
				fail("非法坐标必须拒绝");
			} catch (IOException expected) {
				assertTrue(expected.getMessage().contains("坐标无效"));
			}
		}
	}

	/**
	 * 多平台的原生变体转发仍携带目标描述，不将其压平为普通 POM。
	 *
	 * @throws Exception 元数据递归导出失败。
	 */
	public void testVariantForwardingRetainsTargetMetadata() throws Exception {
		Map<String, File> metadata = new HashMap<String, File>();
		String value = "{\"formatVersion\":\"1.1\",\"variants\":[{\"name\":\"jvm\",\"available-at\":{"
				+ "\"group\":\"test\",\"module\":\"sample-jvm\",\"version\":\"1.0\","
				+ "\"url\":\"../../sample-jvm/1.0/sample-jvm-1.0.module\"}}]}";
		metadata.put("test:sample:1.0@module", write(new File(root, "sample.module"), value));
		metadata.put("test:sample-jvm:1.0@module", write(new File(root, "sample-jvm.module"),
				"{\"formatVersion\":\"1.1\",\"variants\":[{\"name\":\"runtime\"}]}"));
		File target = new File(root, "repository");
		MavenRepository.export(target, Collections.<MavenRepository.Artifact>emptyList(),
				Arrays.asList("test:sample:1.0"), source(metadata));
		assertEquals(value, Files.readString(new File(target, "test/sample/1.0/sample-1.0.module").toPath()));
		assertTrue(new File(target, "test/sample-jvm/1.0/sample-jvm-1.0.module").isFile());
	}

	/**
	 * 为用例提供可控的原始元数据，不依赖 Gradle 或网络。
	 *
	 * @param files 描述文件映射。
	 * @return 元数据读取器。
	 */
	private MavenRepository.MetadataSource source(final Map<String, File> files) {
		return new MavenRepository.MetadataSource() {
			@Override
			public File resolve(String coordinate, String extension) {
				return files.get(coordinate + "@" + extension);
			}
		};
	}

	/**
	 * 创建最小 POM 描述。
	 *
	 * @param name 模块名。
	 * @param version 模块版本。
	 * @param body 附加声明。
	 * @return 原始 POM 内容。
	 */
	private String pom(String name, String version, String body) {
		return "<project><modelVersion>4.0.0</modelVersion><groupId>test</groupId><artifactId>" + name
				+ "</artifactId><version>" + version + "</version>" + body + "</project>";
	}

	/**
	 * 写入一个用例文件。
	 *
	 * @param file 目标文件。
	 * @param content UTF-8 内容。
	 * @return 已写入文件。
	 * @throws IOException 文件无法创建。
	 */
	private File write(File file, String content) throws IOException {
		Files.createDirectories(file.toPath().getParent());
		Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
		return file;
	}
}

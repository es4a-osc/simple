package simple.compiler;

import com.android.ide.common.symbols.SymbolIo;
import com.android.ide.common.symbols.Symbol;
import com.android.ide.common.symbols.SymbolTable;
import com.android.ide.common.symbols.SymbolUtils;
import com.android.ide.common.resources.ResourceSet;
import com.android.ide.common.resources.ResourceMergerItem;
import com.android.ide.common.rendering.api.ResourceNamespace;
import com.android.manifmerger.ManifestMerger2;
import com.android.manifmerger.MergingReport;
import com.android.utils.ILogger;
import com.android.utils.NullLogger;
import simple.compiler.util.Toolchain;
import simple.compiler.util.ClassFiles;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.Properties;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;

/**
 * 使用 Android 官方实现处理类库清单及资源符号，保持 AAR 与应用的资源身份一致。
 *
 * <p>实例只管理一次应用构建中的清单副本和资产合并目录；关闭时清理自有临时输入。
 * 原始类库和应用文件不归实例所有，资源符号等无状态操作继续使用静态入口。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class AndroidLibraries implements AutoCloseable {

	/** 自有临时目录的父目录，固定为本轮应用构建目录。 */
	private final File buildDirectory;

	/** 按输入对象身份复用本轮清单，不能以命名空间代替类库身份。 */
	private final Map<LibraryArtifacts.AndroidLibrary, File> preparedManifests =
			new IdentityHashMap<LibraryArtifacts.AndroidLibrary, File>();

	/** 按需创建的自有临时根目录，关闭时只清理此目录。 */
	private File temporaryDirectory;

	/** 已关闭的实例不能继续准备或消费临时输入。 */
	private boolean closed;

	/**
	 * 为一次应用构建建立 Android 输入的生命周期，不提前创建临时文件。
	 *
	 * @param buildDirectory 当前应用的构建目录。
	 * @throws IOException 构建目录无法规范化。
	 */
	public AndroidLibraries(File buildDirectory) throws IOException {
		this.buildDirectory = buildDirectory.getCanonicalFile();
	}

	/**
	 * 清理本实例创建的临时输入，不遍历目录链接或删除类库原文件。
	 * 清理失败后允许再次关闭以重试释放，但不能继续准备或消费输入。
	 *
	 * @throws IOException 自有目录越界、不可访问或删除失败。
	 */
	@Override
	public void close() throws IOException {
		if (closed && temporaryDirectory == null) {
			return;
		}
		closed = true;
		preparedManifests.clear();
		if (temporaryDirectory == null) {
			return;
		}

		// 只清理本实例实际创建的直接子目录，拒绝通过外部参数扩大删除范围。
		final Path root = temporaryDirectory.toPath();
		if (!Files.isSameFile(buildDirectory.toPath(), root.getParent())) {
			throw new IOException("Android 临时输入目录超出本轮构建目录：" + root);
		}
		if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
			temporaryDirectory = null;
			return;
		}
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			/**
			 * 拒绝真实位置不同的目录，避免 Windows 目录联接绕过不跟随链接的遍历设置。
			 *
			 * @param directory 当前待遍历的自有目录。
			 * @param attributes 当前目录属性。
			 * @return 目录真实位置未变化时继续清理。
			 * @throws IOException 目录链接、真实路径异常或目录不可访问。
			 */
			@Override
			public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException {
				if (!directory.toRealPath().equals(directory.toAbsolutePath().normalize())) {
					throw new IOException("Android 临时输入中存在目录链接，已停止清理：" + directory);
				}
				return FileVisitResult.CONTINUE;
			}

			/**
			 * 删除普通文件或链接本身，不遍历链接指向的内容。
			 *
			 * @param file 当前自有目录中的文件。
			 * @param attributes 当前文件属性。
			 * @return 继续清理其它文件。
			 * @throws IOException 文件无法删除。
			 */
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			/**
			 * 子项清理完成后删除自有目录，遍历失败时保留原始错误。
			 *
			 * @param directory 当前已遍历目录。
			 * @param failure 遍历异常，成功时为 null。
			 * @return 继续清理父目录。
			 * @throws IOException 遍历或目录删除失败。
			 */
			@Override
			public FileVisitResult postVisitDirectory(Path directory, IOException failure) throws IOException {
				if (failure != null) {
					throw failure;
				}
				Files.delete(directory);
				return FileVisitResult.CONTINUE;
			}
		});
		temporaryDirectory = null;
	}

	/**
	 * 按需创建本实例的临时根目录，生成物不会写入安装后的类库目录。
	 *
	 * @return 本轮构建独占的临时根目录。
	 * @throws IOException 实例已关闭或目录无法创建。
	 */
	private File temporaryDirectory() throws IOException {
		ensureOpen();
		if (temporaryDirectory == null) {
			Files.createDirectories(buildDirectory.toPath());
			// 父目录允许使用工作区路径别名，记录真实位置后再创建和清理自有子目录。
			temporaryDirectory = Files.createTempDirectory(buildDirectory.toPath().toRealPath(), "android-inputs-").toFile();
		}
		return temporaryDirectory;
	}

	/**
	 * 拒绝在关闭后继续使用本轮临时输入。
	 *
	 * @throws IOException 本实例已经关闭。
	 */
	private void ensureOpen() throws IOException {
		if (closed) {
			throw new IOException("本轮 Android 构建输入已经关闭。");
		}
	}

	/**
	 * 读取 AAR 或扩展库清单中的包名，禁止清单通过外部实体访问其它输入。
	 *
	 * @param directory 包含 AndroidManifest.xml 的类库目录。
	 * @return 清单中的包名。
	 * @throws IOException 清单无效或包名缺失。
	 */
	public static String readNamespace(File directory) throws IOException {
		File manifest = new File(directory, "AndroidManifest.xml");
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setXIncludeAware(false);
			String namespace = factory.newDocumentBuilder().parse(manifest).getDocumentElement().getAttribute("package");
			validateNamespace(namespace);
			return namespace;
		} catch (Exception failure) {
			throw new IOException("读取类库清单失败：" + manifest + "\n" + failure.getMessage(), failure);
		}
	}

	/**
	 * 校验资源包名能用于生成 Java 包路径，防止错误描述改变生成目录。
	 *
	 * @param namespace 交付描述或清单中的包名。
	 * @throws IOException 包名为空或不是限定 Java 标识符。
	 */
	private static void validateNamespace(String namespace) throws IOException {
		if (namespace == null || !namespace.matches("[\\p{javaJavaIdentifierStart}][\\p{javaJavaIdentifierPart}]*(?:\\.[\\p{javaJavaIdentifierStart}][\\p{javaJavaIdentifierPart}]*)*")) {
			throw new IOException("类库资源包名无效：" + namespace);
		}
	}

	/**
	 * 合并所选类库清单，保留官方冲突、SDK 要求及占位符处理规则。
	 *
	 * @param manifest 应用清单；成功后替换为合并结果。
	 * @param libraries 低优先级依赖在前的 Android 类库输入。
	 * @param placeholders 项目明确声明的清单占位符。
	 * @param applicationId 应用包名，作为 applicationId 占位符的值。
	 * @param out 普通信息输出。
	 * @param err 错误和警告输出。
	 * @throws IOException 合并失败或类库要求更高的编译 SDK。
	 */
	public void mergeManifest(File manifest, List<LibraryArtifacts.AndroidLibrary> libraries,
			Map<String, Object> placeholders, String applicationId, final PrintStream out,
			final PrintStream err) throws IOException {
		ensureOpen();
		ILogger logger = new ILogger() {
			/**
			 * 输出官方合并器的错误消息及关联异常。
			 *
			 * @param failure 可选的异常原因。
			 * @param message 可选的格式化消息。
			 * @param arguments 消息参数。
			 */
			@Override
			public void error(Throwable failure, String message, Object... arguments) {
				if (message != null) {
					err.println(String.format(message, arguments));
				}
				if (failure != null) {
					failure.printStackTrace(err);
				}
			}

			/**
			 * 保留清单合并警告，避免把潜在运行条件隐藏在成功结果后。
			 *
			 * @param message 格式化消息。
			 * @param arguments 消息参数。
			 */
			@Override
			public void warning(String message, Object... arguments) {
				err.println(String.format(message, arguments));
			}

			/**
			 * 将合并器的普通消息写入本次编译输出。
			 *
			 * @param message 格式化消息。
			 * @param arguments 消息参数。
			 */
			@Override
			public void info(String message, Object... arguments) {
				out.println(String.format(message, arguments));
			}

			/**
			 * 保持常规编译输出精简，详细行为另由合并报告检查。
			 *
			 * @param message 详细格式化消息。
			 * @param arguments 消息参数。
			 */
			@Override
			public void verbose(String message, Object... arguments) {
				// 详细合并轨迹写入报告，不逐条输出到正常编译日志。
			}
		};
		try {
			ManifestMerger2.Invoker invoker = ManifestMerger2.newMerger(manifest, logger, ManifestMerger2.MergeType.APPLICATION);
			Map<String, Object> values = new LinkedHashMap<String, Object>(placeholders);
			values.put("applicationId", applicationId);
			invoker.setPlaceHolderValues(values);
			invoker.withFeatures(ManifestMerger2.Invoker.Feature.REMOVE_TOOLS_DECLARATIONS);
			invoker.setMergeReportFile(new File(manifest.getParentFile(), "manifest-merger-report.txt"));
			// 官方合并器先加入的库优先级更高，因此与资源输入顺序相反。
			for (int i = libraries.size() - 1; i >= 0; i--) {
				File directory = libraries.get(i).directory;
				validateMetadata(directory);
				File input = libraries.get(i).manifest;
				if (input.isFile()) {
					invoker.addLibraryManifest(libraries.get(i).source.toString(), prepareManifest(libraries.get(i)));
				}
			}
			MergingReport report = invoker.merge();
			if (report.getResult() == MergingReport.Result.ERROR) {
				report.log(logger);
				throw new IOException("Android 清单合并失败：\n" + report.getReportString());
			}
			if (report.getResult() == MergingReport.Result.WARNING) {
				report.log(logger);
			}
			String merged = report.getMergedDocument(MergingReport.MergedManifestKind.MERGED);
			if (merged == null) {
				throw new IOException("Android 清单合并器未生成输出。");
			}
			Files.write(manifest.toPath(), merged.getBytes(StandardCharsets.UTF_8));
		} catch (ManifestMerger2.MergeFailureException failure) {
			throw new IOException("Android 清单合并失败：" + failure.getMessage(), failure);
		}
	}

	/**
	 * 为省略 package 的扩展库清单制作构建副本，同一实例内复用已准备的清单。
	 *
	 * @param library 当前 Android 类库输入。
	 * @return 可直接交给官方合并器的清单。
	 * @throws IOException 清单无效或缺少可用命名空间。
	 */
	private File prepareManifest(LibraryArtifacts.AndroidLibrary library) throws IOException {
		// 两次清单合并共用同一份输入，但相同命名空间的不同类库仍保持独立身份。
		File cached = preparedManifests.get(library);
		if (cached != null && cached.isFile()) {
			return cached;
		}

		File original = library.manifest;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			Document document = factory.newDocumentBuilder().parse(original);
			if (!document.getDocumentElement().getAttribute("package").isEmpty()) {
				preparedManifests.put(library, original);
				return original;
			}
			validateNamespace(library.namespace);
			document.getDocumentElement().setAttribute("package", library.namespace);
			File manifests = new File(temporaryDirectory(), "manifests");
			Files.createDirectories(manifests.toPath());
			File prepared = new File(Files.createTempDirectory(manifests.toPath(), "library-").toFile(), "AndroidManifest.xml");
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.transform(new DOMSource(document), new StreamResult(prepared));
			preparedManifests.put(library, prepared);
			return prepared;
		} catch (Exception failure) {
			throw new IOException("准备类库清单失败：" + original + "\n" + failure.getMessage(), failure);
		}
	}

	/**
	 * 按最终链接值生成每个类库包的 R.java，交由同一 JDK 编译进入项目类目录。
	 *
	 * <p>使用官方符号模型筛选和重映射普通资源与 styleable，避免把库内占位编号带入 APK。
	 * 同一资源包的多个输入合并其符号，只生成一份 R 类。
	 *
	 * @param libraries 实际参与构建的 Android 类库。
	 * @param applicationSymbols 最终链接生成的应用 R.txt。
	 * @param applicationPackage 应用资源包名。
	 * @param sourceDirectory 生成的 Java 源文件根目录。
	 * @param jars 实际参与应用构建的依赖及扩展库 JAR，用于核对资源字段引用。
	 * @return 需要参与 Java 编译的类库 R.java 文件。
	 * @throws IOException 符号缺失、格式无效或文件无法写入。
	 */
	public static List<File> generateResourceSources(List<LibraryArtifacts.AndroidLibrary> libraries,
			File applicationSymbols, String applicationPackage, File sourceDirectory, List<File> jars) throws IOException {
		try {
			SymbolTable application = SymbolIo.readFromAapt(applicationSymbols, applicationPackage);
			Map<String, SymbolTable> tables = new LinkedHashMap<String, SymbolTable>();
			for (LibraryArtifacts.AndroidLibrary library : libraries) {
				File symbols = new File(library.directory, "R.txt");
				if (!symbols.isFile()) {
					continue;
				}
				validateNamespace(library.namespace);
				if (library.namespace.equals(applicationPackage)) {
					continue;
				}
				SymbolTable declared = SymbolIo.readFromAapt(symbols, library.namespace);
				Set<String> owned = readOwnedResources(library);
				SymbolTable.Builder retained = SymbolTable.builder().tablePackage(library.namespace);
				// 旧式 R.txt 包含传递资源。依赖升级后消失的无用条目不能视为本库资源缺失。
				for (Symbol symbol : declared.getSymbols().values()) {
					Symbol linked = application.getSymbols().get(symbol.getResourceType(), symbol.getCanonicalName());
					if (linked == null || (symbol instanceof Symbol.StyleableSymbol
							&& !linked.getChildren().containsAll(symbol.getChildren()))) {
						if (owned.contains(symbol.getResourceType() + "/" + symbol.getCanonicalName())) {
							throw new IOException("最终资源表缺少类库符号：" + library.namespace + ":"
									+ symbol.getResourceType() + "/" + symbol.getName());
						}
						continue;
					}
					retained.add(symbol);
				}
				SymbolTable table = retained.build().withValuesFrom(application);
				SymbolTable previous = tables.get(library.namespace);
				tables.put(library.namespace, previous == null ? table : previous.merge(table));
			}
			// 即使缺失符号不是本库声明，已有字节码仍引用它时也必须失败。
			validateResourceReferences(jars, tables);
			List<File> sources = new ArrayList<File>();
			Files.createDirectories(sourceDirectory.toPath());
			for (SymbolTable table : tables.values()) {
				sources.add(SymbolIo.exportToJava(table, sourceDirectory, false));
			}
			return sources;
		} catch (Exception failure) {
			throw new IOException("生成类库资源索引失败：" + failure.getMessage(), failure);
		}
	}

	/**
	 * 使用官方资源解析器识别本库真正声明的资源，排除旧 R.txt 中的传递符号。
	 *
	 * @param library 已展开的 Android 类库。
	 * @return 类型和规范名称组成的资源身份集合。
	 * @throws Exception 资源目录无法解析或包含无效 XML。
	 */
	private static Set<String> readOwnedResources(LibraryArtifacts.AndroidLibrary library) throws Exception {
		Set<String> owned = new LinkedHashSet<String>();
		File resources = new File(library.directory, "res");
		if (resources.isDirectory()) {
			// 这里只收集资源身份；不同布局重复声明同一 ID 合法，正式冲突由 AAPT2 链接检查。
			ResourceSet set = new ResourceSet(library.namespace, ResourceNamespace.RES_AUTO,
					library.namespace, false, null);
			set.setShouldParseResourceIds(true);
			set.setCheckDuplicates(false);
			set.addSource(resources);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			set.loadFromFiles(NullLogger.getLogger(), factory);
			for (ResourceMergerItem item : set.getDataMap().values()) {
				owned.add(item.getType() + "/" + SymbolUtils.canonicalizeValueResourceName(item.getName()));
			}
		}
		return owned;
	}

	/**
	 * 核对已交付字节码对生成 R 类的直接字段引用，避免在设备上才发现缺少字段。
	 *
	 * @param jars 实际参与构建的 JAR，不扫描未使用扩展库的代码。
	 * @param tables 使用最终资源值的各包符号表。
	 * @throws IOException 归档损坏、类文件无效或代码引用了缺失资源。
	 */
	private static void validateResourceReferences(List<File> jars, Map<String, SymbolTable> tables) throws IOException {
		final Map<String, Set<String>> fields = new LinkedHashMap<String, Set<String>>();
		final Set<String> packages = new LinkedHashSet<String>();
		for (SymbolTable table : tables.values()) {
			packages.add(table.getTablePackage().replace('.', '/') + "/R$");
			for (Symbol symbol : table.getSymbols().values()) {
				String owner = table.getTablePackage().replace('.', '/') + "/R$" + symbol.getResourceType();
				Set<String> names = fields.get(owner);
				if (names == null) {
					names = new LinkedHashSet<String>();
					fields.put(owner, names);
				}
				names.add(symbol.getCanonicalName());
				if (symbol instanceof Symbol.StyleableSymbol) {
					for (String child : symbol.getChildren()) {
						names.add(symbol.getCanonicalName() + "_" + SymbolUtils.canonicalizeValueResourceName(child));
					}
				}
			}
		}

		for (File jar : new LinkedHashSet<File>(jars)) {
			try (ZipFile archive = new ZipFile(jar)) {
				Enumeration<? extends ZipEntry> entries = archive.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					if (!ClassFiles.isBaseClass(entry.getName())) {
						continue;
					}
					final List<String> missing = new ArrayList<String>();
					try (InputStream input = archive.getInputStream(entry)) {
						new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM9) {
							/**
							 * 读取方法的资源字段访问，不加载或执行第三方类。
							 *
							 * @param access 方法访问标志。
							 * @param name 方法名称。
							 * @param descriptor 方法描述符。
							 * @param signature 可选泛型签名。
							 * @param exceptions 声明的异常类型。
							 * @return 检查字段访问的方法读取器。
							 */
							@Override
							public MethodVisitor visitMethod(int access, String name, String descriptor,
									String signature, String[] exceptions) {
								return new MethodVisitor(Opcodes.ASM9) {
									/**
									 * 记录首个缺失的直接 R 字段引用，保留所属类与字段诊断。
									 *
									 * @param opcode 字段访问指令。
									 * @param owner 字段所属类的内部名。
									 * @param name 字段名称。
									 * @param descriptor 字段描述符。
									 */
									@Override
									public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
										int separator = owner.lastIndexOf("/R$");
										if (missing.isEmpty() && separator >= 0
												&& packages.contains(owner.substring(0, separator + 3))
												&& (!fields.containsKey(owner) || !fields.get(owner).contains(name))) {
											missing.add(owner + "." + name);
										}
									}
									};
								}
							}, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
					}
					if (!missing.isEmpty()) {
						throw new IOException("代码引用了缺失的类库资源字段：" + missing.get(0)
								+ "\n" + jar + "!/" + entry.getName());
					}
				}
			}
		}
	}

	/**
	 * 检查 AAR 的最低编译 SDK 要求，避免资源链接偶然通过后交付不兼容应用。
	 *
	 * @param directory AAR 展开目录或扩展库交付目录。
	 * @throws IOException 元数据无效或要求高于编译 API 30。
	 */
	private static void validateMetadata(File directory) throws IOException {
		File metadata = new File(directory, "META-INF/com/android/build/gradle/aar-metadata.properties");
		if (!metadata.isFile()) {
			return;
		}
		Properties values = new Properties();
		try (InputStream input = Files.newInputStream(metadata.toPath())) {
			values.load(input);
		}
		try {
			if (Integer.parseInt(values.getProperty("minCompileSdk", "1")) > Toolchain.API_LEVEL) {
				throw new IOException("AAR 要求高于当前编译 API 30：" + metadata);
			}
		} catch (NumberFormatException failure) {
			throw new IOException("AAR 编译 SDK 元数据无效：" + metadata, failure);
		}
	}

	/**
	 * 合并资产并检查依赖之间的内容冲突，项目自身的同路径资产具有最终覆盖优先级。
	 *
	 * @param inputs 依赖资产在前、项目资产在后的目录列表。
	 * @return 可直接交给 AAPT2 的唯一资产目录。
	 * @throws IOException 依赖资产冲突或目录无法读取。
	 */
	public List<File> mergeAssets(List<File> inputs) throws IOException {
		ensureOpen();
		if (inputs.size() <= 1) {
			return inputs;
		}
		Map<String, File> files = new LinkedHashMap<String, File>();
		File project = inputs.get(inputs.size() - 1);
		for (File input : inputs) {
			collectAssets(input, input, project, files);
		}
		// 每次合并使用新的自有目录，移除的资产不会残留；所有副本在实例关闭时统一清理。
		File merged = Files.createTempDirectory(temporaryDirectory().toPath(), "assets-").toFile();
		for (Map.Entry<String, File> entry : files.entrySet()) {
			File output = new File(merged, entry.getKey());
			Files.createDirectories(output.getParentFile().toPath());
			Files.copy(entry.getValue().toPath(), output.toPath());
		}
		return Collections.singletonList(merged);
	}

	/**
	 * 收集单个资产目录，保留来源用于冲突诊断。
	 *
	 * @param root 当前资产根目录。
	 * @param directory 正在访问的子目录。
	 * @param project 项目资产根目录。
	 * @param files 已选资产路径及来源。
	 * @throws IOException 枚举、读取失败或依赖资产内容冲突。
	 */
	private static void collectAssets(File root, File directory, File project, Map<String, File> files) throws IOException {
		File[] children = directory.listFiles();
		if (children == null) {
			throw new IOException("无法读取资产目录：" + directory);
		}
		Arrays.sort(children);
		for (File child : children) {
			if (!child.getCanonicalFile().toPath().startsWith(root.getCanonicalFile().toPath())) {
				throw new IOException("资产路径超出声明目录：" + child);
			}
			if (child.isDirectory()) {
				collectAssets(root, child, project, files);
				continue;
			}
			String name = root.toPath().relativize(child.toPath()).toString();
			File previous = files.get(name);
			if (previous != null && !root.equals(project) && !new File(project, name).isFile()
					&& !Arrays.equals(Files.readAllBytes(previous.toPath()), Files.readAllBytes(child.toPath()))) {
				throw new IOException("类库资产冲突：" + name + "\n来源：" + previous + "\n来源：" + child);
			}
			files.put(name, child);
		}
	}
}

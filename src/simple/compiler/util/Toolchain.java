package simple.compiler.util;

import com.android.apkbuilder.ApkBuilder;
import com.android.tools.r8.CompilationMode;
import com.android.tools.r8.D8;
import com.android.tools.r8.D8Command;
import com.android.tools.r8.DexIndexedConsumer;
import com.android.tools.r8.Diagnostic;
import com.android.tools.r8.DiagnosticsHandler;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import simple.util.Execution;

/**
 * Android应用编译过程中使用的外部工具链。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class Toolchain {
	// 外部工具路径统一由工具链根据构建环境解析，调用方不关心可执行文件位置。
	private static final String EXECUTABLE_EXTENSION =
			System.getProperty("os.name").startsWith("Windows") ? ".exe" : "";
	private static final String JAVA_HOME = System.getenv("JAVA_HOME");
	private static final String ANDROID_HOME = System.getenv("ANDROID_HOME");
	private static final String AAPT2_BINARY =
			ANDROID_HOME + "/platforms/android-26/tools/aapt2" + EXECUTABLE_EXTENSION;
	private static final String JAVAC_BINARY =
			JAVA_HOME + "/bin/javac" + EXECUTABLE_EXTENSION;
	private static final String KEYTOOL_BINARY =
			JAVA_HOME + "/bin/keytool" + EXECUTABLE_EXTENSION;
	private static final String JAVA_BINARY =
			JAVA_HOME + "/bin/java" + EXECUTABLE_EXTENSION;
	private static final String ZIPALIGN_BINARY =
			ANDROID_HOME + "/platforms/android-26/tools/zipalign" + EXECUTABLE_EXTENSION;
	private static final String APKSIGNER_JAR =
			ANDROID_HOME + "/platforms/android-26/tools/lib/apksigner.jar";

	private Toolchain() {
	}

	/**
	 * 调用 AAPT2 编译项目资源目录，生成指定的资源中间文件，例如 {@code resources.zip}。
	 *
	 * @param workingDirectory 命令运行目录，Windows 下必须与项目构建目录位于同一盘
	 * @param resDirectory 项目资源目录
	 * @param outputFile 编译后的资源文件
	 * @param out 标准输出
	 * @param err 错误输出
	 * @return 执行成功返回 {@code true}，否则返回 {@code false}
	 */
	public static boolean compileResources(File workingDirectory,
			File resDirectory, File outputFile, PrintStream out, PrintStream err) {
		List<String> commandline = new ArrayList<String>();
		// AAPT2可执行文件。
		commandline.add(AAPT2_BINARY);
		// compile：把res目录中的原始资源编译为链接阶段使用的中间资源。
		commandline.add("compile");
		// --legacy：保持旧版AAPT对历史资源格式的宽容处理。
		commandline.add("--legacy");
		// --dir <目录>：递归编译指定的项目资源目录。
		commandline.add("--dir");
		commandline.add(resDirectory.getAbsolutePath());
		// -o <文件>：指定生成的资源中间文件，例如build/resources.zip。
		commandline.add("-o");
		commandline.add(outputFile.getAbsolutePath());
		// Windows下指定构建目录为运行目录，避免跨盘执行时错误解析输出路径。
		return Execution.execute(workingDirectory, commandline.toArray(new String[0]), out, err);
	}

	/**
	 * 首次链接已编译资源，生成Simple和Android共用的资源索引。
	 *
	 * <p>该步骤生成临时{@code .ap_}资源包、{@code R.txt}、标准{@code R.java}和
	 * {@code resource-ids.txt}。稳定资源编号供最终资源打包继续使用。
	 *
	 * @param workingDirectory 命令运行目录
	 * @param outputFile 临时{@code .ap_}资源包
	 * @param manifestFile 基础Android清单文件
	 * @param platformFile Android平台类库
	 * @param assetsDirectory 项目资产目录
	 * @param resourceSymbolsFile 要生成的{@code R.txt}文件
	 * @param javaSourceDirectory 标准{@code R.java}源码输出根目录
	 * @param emittedIdsFile 要生成的稳定资源编号文件
	 * @param compiledResourcesFile AAPT2已编译的资源文件
	 * @param out 标准输出
	 * @param err 错误输出
	 * @return 执行成功返回{@code true}，否则返回{@code false}
	 */
	public static boolean generateResourceIndex(File workingDirectory,
			File outputFile, File manifestFile, String platformFile, File assetsDirectory,
			File resourceSymbolsFile, File javaSourceDirectory, File emittedIdsFile,
			File compiledResourcesFile, PrintStream out, PrintStream err) {
		return linkResources(workingDirectory, outputFile, manifestFile, platformFile,
				Collections.singletonList(assetsDirectory), resourceSymbolsFile,
				javaSourceDirectory, emittedIdsFile, null,
				Collections.singletonList(compiledResourcesFile), Collections.<File>emptyList(),
				out, err);
	}

	/**
	 * 使用完整Android清单和首次链接保存的稳定资源编号生成最终资源包。
	 *
	 * <p>该步骤只更新{@code .ap_}资源包，不重复生成{@code R.txt}或{@code R.java}。
	 *
	 * @param workingDirectory 命令运行目录
	 * @param outputFile 最终{@code .ap_}资源包
	 * @param manifestFile 完整Android清单文件
	 * @param platformFile Android平台类库
	 * @param assetsDirectories 项目及扩展库资产目录
	 * @param stableIdsFile 首次链接生成的稳定资源编号文件
	 * @param compiledLibraryResources AAPT2已编译的扩展库资源文件，按稳定顺序加入；
	 *        扩展库之间的同名资源仍由AAPT2报告冲突
	 * @param compiledProjectResources AAPT2已编译的项目资源文件，按AAPT2覆盖语义处理同名资源；
	 *        同名样式默认合并条目
	 * @param out 标准输出
	 * @param err 错误输出
	 * @return 执行成功返回{@code true}，否则返回{@code false}
	 */
	public static boolean packageResources(File workingDirectory,
			File outputFile, File manifestFile, String platformFile,
			List<File> assetsDirectories, File stableIdsFile,
			List<File> compiledLibraryResources, File compiledProjectResources,
			PrintStream out, PrintStream err) {
		// 没有扩展库资源时，项目资源必须作为普通输入；否则作为最后的覆盖输入。
		List<File> regularResources = compiledLibraryResources.isEmpty()
				? Collections.singletonList(compiledProjectResources) : compiledLibraryResources;
		List<File> overlayResources = compiledLibraryResources.isEmpty()
				? Collections.<File>emptyList() : Collections.singletonList(compiledProjectResources);
		return linkResources(workingDirectory, outputFile, manifestFile, platformFile,
				assetsDirectories, null, null, null, stableIdsFile,
				regularResources, overlayResources, out, err);
	}

	/**
	 * 组装并执行AAPT2资源链接参数，供首次索引链接和最终资源打包共同使用。
	 *
	 * @param workingDirectory 命令运行目录，Windows 下必须与项目构建目录位于同一盘
	 * @param outputFile 输出的 {@code .ap_} 资源包
	 * @param manifestFile Android 清单文件
	 * @param platformFile Android 平台类库
	 * @param assetsDirectories 要加入资源包的资产目录
	 * @param resourceSymbolsFile {@code R.txt}资源符号输出文件；为{@code null}时不生成
	 * @param javaSourceDirectory 标准{@code R.java}源码输出根目录；为{@code null}时不生成，
	 *        AAPT2会在该目录下按应用包名建立子目录
	 * @param emittedIdsFile AAPT2分配的资源编号输出文件；为{@code null}时不生成
	 * @param stableIdsFile 固定资源编号所用的输入文件；为{@code null}时由AAPT2分配编号
	 * @param regularResources AAPT2普通资源输入，彼此重名时报错
	 * @param overlayResources AAPT2覆盖资源输入，后面的资源优先
	 * @param out 标准输出
	 * @param err 错误输出
	 * @return 执行成功返回 {@code true}，否则返回 {@code false}
	 */
	private static boolean linkResources(File workingDirectory,
			File outputFile, File manifestFile, String platformFile,
			List<File> assetsDirectories,
			File resourceSymbolsFile, File javaSourceDirectory, File emittedIdsFile,
			File stableIdsFile, List<File> regularResources, List<File> overlayResources,
			PrintStream out, PrintStream err) {
		List<String> commandline = new ArrayList<String>();
		// AAPT2可执行文件。
		commandline.add(AAPT2_BINARY);
		// link：链接已编译资源、清单和资产，生成APK资源包。
		commandline.add("link");
		// -o <文件>：指定生成的.ap_资源包。
		commandline.add("-o");
		commandline.add(outputFile.getAbsolutePath());
		// --manifest <文件>：指定要写入资源包的AndroidManifest.xml。
		commandline.add("--manifest");
		commandline.add(manifestFile.getAbsolutePath());
		// -I <文件>：引用Android平台android.jar中的系统资源。
		commandline.add("-I");
		commandline.add(platformFile);
		// -A <目录>：依次加入项目和实际使用的扩展库assets目录。
		for (File assetsDirectory : assetsDirectories) {
			commandline.add("-A");
			commandline.add(assetsDirectory.getAbsolutePath());
		}
		if (resourceSymbolsFile != null) {
			// --output-text-symbols <文件>：生成供Simple资源索引转换使用的R.txt。
			commandline.add("--output-text-symbols");
			commandline.add(resourceSymbolsFile.getAbsolutePath());
		}
		if (javaSourceDirectory != null) {
			// --java <目录>：按Android应用包名生成标准R.java及其包目录。
			commandline.add("--java");
			commandline.add(javaSourceDirectory.getAbsolutePath());
		}
		if (emittedIdsFile != null) {
			// --emit-ids <文件>：记录本次链接分配的资源编号，供后续链接复用。
			commandline.add("--emit-ids");
			commandline.add(emittedIdsFile.getAbsolutePath());
		}
		if (stableIdsFile != null) {
			// --stable-ids <文件>：复用第一次链接的资源编号，保证已解析索引不变。
			commandline.add("--stable-ids");
			commandline.add(stableIdsFile.getAbsolutePath());
		}
		// --auto-add-overlay：允许覆盖资源自动加入资源表，兼容原有项目行为。
		commandline.add("--auto-add-overlay");
		// 普通输入之间不允许重名，不能仅靠文件位置表达覆盖关系。
		for (File compiledResourcesFile : regularResources) {
			commandline.add(compiledResourcesFile.getAbsolutePath());
		}
		// -R 明确指定覆盖输入；AAPT2先处理普通输入，再按这里的顺序处理覆盖输入。
		for (File compiledResourcesFile : overlayResources) {
			commandline.add("-R");
			commandline.add(compiledResourcesFile.getAbsolutePath());
		}
		// Windows下指定构建目录为运行目录，避免跨盘执行时错误解析输出路径。
		return Execution.execute(workingDirectory, commandline.toArray(new String[0]), out, err);
	}

	/**
	 * 调用{@code javac}编译AAPT2生成的标准{@code R.java}。
	 *
	 * <p>编译结果写入指定类目录，并按应用包名生成{@code R.class}以及
	 * {@code R$drawable.class}、{@code R$id.class}等资源分类类文件。
	 *
	 * @param workingDirectory 命令运行目录
	 * @param outputDirectory Java类文件输出根目录
	 * @param sourceFile AAPT2生成的标准{@code R.java}源码文件
	 * @param out 标准输出
	 * @param err 错误输出
	 * @return 执行成功返回{@code true}，否则返回{@code false}
	 */
	public static boolean compileJava(File workingDirectory,
			File outputDirectory, File sourceFile, PrintStream out, PrintStream err) {
		List<String> commandline = new ArrayList<String>();
		// javac可执行文件。
		commandline.add(JAVAC_BINARY);
		// -encoding UTF-8：按UTF-8读取AAPT2生成的Java源码。
		commandline.add("-encoding");
		commandline.add("UTF-8");
		// -d <目录>：把R.class和R$*.class写入项目类输出目录。
		commandline.add("-d");
		commandline.add(outputDirectory.getAbsolutePath());
		// 最后的无选项参数是要编译的R.java源码文件。
		commandline.add(sourceFile.getAbsolutePath());
		return Execution.execute(workingDirectory, commandline.toArray(new String[0]), out, err);
	}

	/**
	 * 使用 D8 转换项目类、运行库及依赖，生成单个 {@code classes.dex}。
	 *
	 * <p>开启 Java 8 特性转换并保留调试信息。只在转换成功且仅生成一个 DEX 时写入文件，
	 * 不通过拆分 DEX 绕过方法数限制；最低 API 必须与应用清单一致。
	 *
	 * @param outputFile 要生成的{@code classes.dex}文件
	 * @param inputFiles 项目类目录、Simple运行库、扩展库及其依赖
	 * @param platformFile 编译平台的 {@code android.jar}，只用于解析系统类型，不打入应用
	 * @param minApiLevel 应用最低支持的 Android API 等级
	 * @param out 标准输出
	 * @param err 错误输出
	 * @return 执行成功返回{@code true}，否则返回{@code false}
	 */
	public static boolean generateDex(File outputFile, List<File> inputFiles, File platformFile, int minApiLevel,
			PrintStream out, PrintStream err) {
		try {
			// 保留输入来源，便于从编译日志定位出错的类或依赖。
			DiagnosticsHandler diagnostics = new DiagnosticsHandler() {
				@Override
				public void error(Diagnostic diagnostic) {
					err.println("D8 错误：" + diagnostic.getOrigin() + ": " + diagnostic.getDiagnosticMessage());
				}

				@Override
				public void warning(Diagnostic diagnostic) {
					// 屏蔽警告，尽可能不污染输出
					// err.println("D8 警告：" + diagnostic.getOrigin() + ": " + diagnostic.getDiagnosticMessage());
				}

				@Override
				public void info(Diagnostic diagnostic) {
					// 屏蔽信息，尽可能不污染输出
					// out.println("D8：" + diagnostic.getDiagnosticMessage());
				}
			};

			// 先接收转换结果；D8 整体成功前不覆盖已有文件。
			final AtomicReference<byte[]> dex = new AtomicReference<byte[]>();

			// 最低 API 与应用清单保持一致，转换 Java 8 特性并保留调试信息。
			D8Command.Builder command = D8Command.builder(diagnostics);
			command.setMode(CompilationMode.DEBUG);
			command.setMinApiLevel(minApiLevel);
			command.setDisableDesugaring(false);
			command.addLibraryFiles(platformFile.toPath());
			command.setProgramConsumer(new DexIndexedConsumer() {
				@Override
				public void accept(int index, byte[] data, Set<String> descriptors, DiagnosticsHandler handler) {
					if (index != 0) {
						throw new IllegalStateException("当前编译器只支持单个 classes.dex，不能输出多个 DEX。");
					}

					dex.set(data);
				}

				@Override
				public void finished(DiagnosticsHandler handler) {
					// 等待 D8.run 成功返回后再统一写入。
				}
			});

			// 项目类与依赖共同参与转换；平台 android.jar 仅用于解析系统类型。
			for (File input : inputFiles) {
				Path path = input.toPath();

				if (input.isDirectory()) {
					// D8 不接受类目录；只枚举 class，避免把目录中的资源当作程序输入。
					final List<Path> classes = new ArrayList<Path>();
					Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
							if (Files.isRegularFile(file) && file.getFileName().toString().endsWith(".class")) {
								classes.add(file);
							}
							return FileVisitResult.CONTINUE;
						}
					});

					// 固定输入顺序，不依赖文件系统的目录枚举顺序。
					Collections.sort(classes);
					for (Path file : classes) {
						command.addProgramFiles(file);
					}
				} else {
					command.addProgramFiles(path);
				}
			}

			D8.run(command.build());

			// 无有效输出时报告失败，避免后续打包沿用旧的 classes.dex。
			if (dex.get() == null) {
				err.println("D8 未生成 classes.dex。");
				return false;
			}

			Files.write(outputFile.toPath(), dex.get());
			return true;
		} catch (Exception failure) {
			failure.printStackTrace(err);
			return false;
		}
	}

	/**
	 * 调用{@code apkbuilder.jar}把资源包和{@code classes.dex}组装为未签名APK。
	 *
	 * @param outputFile 要生成的APK文件
	 * @param resourcesFile AAPT2生成的{@code .ap_}资源包
	 * @param dexFile D8生成的{@code classes.dex}文件
	 * @param dependencyFiles 已选定的扩展库依赖JAR
	 * @param nativeDirectories 扩展库原生库目录
	 * @param err 错误输出
	 * @return 执行成功返回{@code true}，否则返回{@code false}
	 */
	public static boolean buildApplication(File outputFile, File resourcesFile,
			File dexFile, List<File> dependencyFiles,
			List<File> nativeDirectories, PrintStream err) {
		List<String> commandline = new ArrayList<String>();
		// 第一个参数指定要生成的APK文件。
		commandline.add(outputFile.getAbsolutePath());
		// -u：创建未签名APK，签名由后续工具完成。
		commandline.add("-u");
		// -z <文件>：加入AAPT2生成的资源包。
		commandline.add("-z");
		commandline.add(resourcesFile.getAbsolutePath());
		// -f <文件>：加入D8生成的classes.dex。
		commandline.add("-f");
		commandline.add(dexFile.getAbsolutePath());
		// -rj <JAR>：只加入选定依赖的非class资源，不再扫描原始libs目录。
		for (File dependency : dependencyFiles) {
			commandline.add("-rj");
			commandline.add(dependency.getAbsolutePath());
		}
		// -nf <目录>：加入按ABI组织的原生SO文件。
		for (File nativeDirectory : nativeDirectories) {
			commandline.add("-nf");
			commandline.add(nativeDirectory.getAbsolutePath());
		}
		try {
			ApkBuilder.main(commandline.toArray(new String[0]));
			return true;
		} catch (Throwable throwable) {
			throwable.printStackTrace(err);
			return false;
		}
	}

	/**
	 * 调用 {@code keytool} 生成指定的 Android 调试密钥库文件。
	 *
	 * @param keystoreFile 调试密钥库文件
	 * @param err 错误输出
	 * @return 执行成功返回 {@code true}，否则返回 {@code false}
	 */
	public static boolean createDebugKeystore(File keystoreFile, PrintStream err) {
		String[] commandline = {
				// keytool可执行文件。
				KEYTOOL_BINARY,
				// -genkey：生成包含密钥对和证书的密钥库。
				"-genkey",
				// -storetype：新调试密钥使用PKCS12，避免依赖JDK默认的JKS格式。
				"-storetype", "PKCS12",
				// -keystore <文件>：指定生成的调试密钥库文件。
				"-keystore", keystoreFile.getAbsolutePath(),
				// -alias <名称>：设置调试密钥别名。
				"-alias", "AndroidDebugKey",
				// -keyalg <算法>：使用RSA密钥算法。
				"-keyalg", "RSA",
				// -storepass <口令>：设置密钥库口令。
				"-storepass", "android",
				// -keypass <口令>：设置密钥条目口令。
				"-keypass", "android",
				// -dname <名称>：设置调试证书的主题名称。
				"-dname", "CN=Android Debug, O=Android, C=US",
				// -validity <天数>：设置证书有效期。
				"-validity", "365"
		};
		return Execution.execute(null, commandline, null, err);
	}

	/**
	 * 在签名前对齐未压缩条目和原生库，输出独立的 APK 文件。
	 *
	 * @param inputFile 未签名 APK 文件
	 * @param outputFile 对齐后的 APK 文件，不得与输入文件相同
	 * @param err 错误输出
	 * @return 执行成功返回 {@code true}，否则返回 {@code false}
	 */
	public static boolean alignApplication(File inputFile, File outputFile, PrintStream err) {
		String[] commandline = {
			ZIPALIGN_BINARY, "-f", "-p", "4",
			inputFile.getAbsolutePath(), outputFile.getAbsolutePath()
		};
		return Execution.execute(null, commandline, null, err);
	}

	/**
	 * 使用 {@code apksigner.jar} 为已对齐的 APK 生成 V1 和 V2 签名。
	 *
	 * <p>签名算法依据 APK 清单中的最低 API 选择，不提高应用的系统要求。
	 * 发布口令通过 {@code env:KEY_PASSWORD} 从继承环境中读取，避免出现在命令行中。
	 *
	 * @param keystoreLocation 密钥库位置
	 * @param passwordSource apksigner 口令来源，调试签名为 {@code pass:android}，
	 *        发布签名为 {@code env:KEY_PASSWORD}；密钥库和私钥使用相同口令
	 * @param inputFile 已对齐的未签名 APK 文件
	 * @param outputFile 签名后的 APK 文件
	 * @param alias 密钥别名
	 * @param storeType 密钥库格式；发布签名沿用JDK默认格式时为{@code null}
	 * @param err 错误输出
	 * @return 执行成功返回 {@code true}，否则返回 {@code false}
	 */
	public static boolean signApplication(String keystoreLocation,
			String passwordSource, File inputFile, File outputFile, String alias,
			String storeType, PrintStream err) {
		List<String> commandline = new ArrayList<String>();
		commandline.add(JAVA_BINARY);
		commandline.add("-jar");
		commandline.add(APKSIGNER_JAR);
		commandline.add("sign");

		// 显式保留旧系统所需的 V1，同时为新系统生成 V2。
		commandline.add("--v1-signing-enabled");
		commandline.add("true");
		commandline.add("--v2-signing-enabled");
		commandline.add("true");

		commandline.add("--ks");
		commandline.add(keystoreLocation);
		commandline.add("--ks-key-alias");
		commandline.add(alias);
		commandline.add("--ks-pass");
		commandline.add(passwordSource);
		// 不允许后台构建停在交互式私钥口令提示上。
		commandline.add("--key-pass");
		commandline.add(passwordSource);
		if (storeType != null) {
			commandline.add("--ks-type");
			commandline.add(storeType);
		}

		commandline.add("--out");
		commandline.add(outputFile.getAbsolutePath());
		commandline.add(inputFile.getAbsolutePath());
		return Execution.execute(null, commandline.toArray(new String[0]), null, err);
	}

	/**
	 * 按 APK 清单声明的最低 API 校验签名，校验过程不修改文件。
	 *
	 * @param applicationFile 已签名的 APK 文件
	 * @param out 标准输出
	 * @param err 错误输出
	 * @return 所声明版本范围内的签名校验通过时返回 {@code true}，否则返回 {@code false}
	 */
	public static boolean verifyApplication(File applicationFile, PrintStream out, PrintStream err) {
		String[] commandline = {
			JAVA_BINARY, "-jar", APKSIGNER_JAR, "verify",
			applicationFile.getAbsolutePath()
		};
		return Execution.execute(null, commandline, out, err);
	}
}

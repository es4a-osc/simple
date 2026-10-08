package simple.compiler.util;

import com.android.apkbuilder.ApkBuilder;
import com.android.dx.command.dexer.DxContext;
import com.android.dx.command.dexer.Main.Arguments;
import com.android.dx.dex.code.PositionList;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
	private static final String JARSIGNER_BINARY =
			JAVA_HOME + "/bin/jarsigner" + EXECUTABLE_EXTENSION;

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
	 * 调用{@code dx.jar}把项目类和Simple Android运行库转换为{@code classes.dex}。
	 *
	 * @param outputFile 要生成的{@code classes.dex}文件
	 * @param classesDirectory 项目编译后的Java类目录
	 * @param inputFiles 项目类目录、Simple运行库、扩展库及其依赖
	 * @param out 标准输出
	 * @param err 错误输出
	 * @return 执行成功返回{@code true}，否则返回{@code false}
	 */
	public static boolean generateDex(File outputFile, List<File> inputFiles,
			PrintStream out, PrintStream err) {
		try {
			DxContext context = new DxContext(out, err);
			Arguments arguments = new Arguments();
			// outName：指定生成的classes.dex文件。
			arguments.outName = outputFile.getAbsolutePath();
			// jarOutput：输出普通.dex文件，不生成.jar文件。
			arguments.jarOutput = false;
			// localInfo：保留局部变量调试信息。
			arguments.localInfo = true;
			// positionInfo：保留源码行号信息。
			arguments.positionInfo = PositionList.LINES;
			// fileNames：转换项目类目录、Simple运行库及当前项目实际使用的扩展库。
			arguments.fileNames = new String[inputFiles.size()];
			for (int i = 0; i < inputFiles.size(); i++) {
				arguments.fileNames[i] = inputFiles.get(i).getAbsolutePath();
			}
			return new com.android.dx.command.dexer.Main(context).runDx(arguments) == 0;
		} catch (Throwable throwable) {
			throwable.printStackTrace(err);
			return false;
		}
	}

	/**
	 * 调用{@code apkbuilder.jar}把资源包和{@code classes.dex}组装为未签名APK。
	 *
	 * @param outputFile 要生成的APK文件
	 * @param resourcesFile AAPT2生成的{@code .ap_}资源包
	 * @param dexFile dx生成的{@code classes.dex}文件
	 * @param libraryDirectories 扩展库依赖JAR目录
	 * @param nativeDirectories 扩展库原生库目录
	 * @param err 错误输出
	 * @return 执行成功返回{@code true}，否则返回{@code false}
	 */
	public static boolean buildApplication(File outputFile, File resourcesFile,
			File dexFile, List<File> libraryDirectories,
			List<File> nativeDirectories, PrintStream err) {
		List<String> commandline = new ArrayList<String>();
		// 第一个参数指定要生成的APK文件。
		commandline.add(outputFile.getAbsolutePath());
		// -u：创建未签名APK，签名由后续工具完成。
		commandline.add("-u");
		// -z <文件>：加入AAPT2生成的资源包。
		commandline.add("-z");
		commandline.add(resourcesFile.getAbsolutePath());
		// -f <文件>：加入dx生成的classes.dex。
		commandline.add("-f");
		commandline.add(dexFile.getAbsolutePath());
		// -rj <目录>：加入依赖JAR中的非class资源。
		for (File libraryDirectory : libraryDirectories) {
			commandline.add("-rj");
			commandline.add(libraryDirectory.getAbsolutePath());
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
	 * 调用 {@code jarsigner} 为应用签名，直接更新原应用文件，不另外生成文件。
	 *
	 * @param keystoreLocation 密钥库位置
	 * @param storePassword 密钥库口令
	 * @param applicationFile 待签名应用
	 * @param alias 密钥别名
	 * @param storeType 密钥库格式；发布签名沿用JDK默认格式时为{@code null}
	 * @param err 错误输出
	 * @return 执行成功返回 {@code true}，否则返回 {@code false}
	 */
	public static boolean signApplication(String keystoreLocation,
			String storePassword, File applicationFile, String alias,
			String storeType, PrintStream err) {
		List<String> commandline = new ArrayList<String>();
		commandline.add(JARSIGNER_BINARY);
		commandline.add("-keystore");
		commandline.add(keystoreLocation);
		commandline.add("-storepass");
		commandline.add(storePassword);
		if (storeType != null) {
			commandline.add("-storetype");
			commandline.add(storeType);
		}
		// jarsigner直接更新指定APK，最后一个参数是密钥别名。
		commandline.add(applicationFile.getAbsolutePath());
		commandline.add(alias);
		return Execution.execute(null, commandline.toArray(new String[0]), null, err);
	}
}

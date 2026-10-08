/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simple.compiler;

import simple.util.Preconditions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 此类提供对Simple项目文件的访问。
 *
 * <p>Simple的项目文件本质上是一个Java属性文件。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public final class Project {

	/**
	 * 表示包含其名称和文件位置的源文件。
	 */
	public static class SourceDescriptor {

		// 源文件定义的类的限定名称
		private final String qualifiedName;

		// 源的文件描述符
		private final File file;

		private SourceDescriptor(String qualifiedName, File file) {
			this.qualifiedName = qualifiedName;
			this.file = file;
		}

		/**
		 * 返回由源文件定义的类的限定名。
		 *
		 * @return  源文件的类名
		 */
		public String getQualifiedName() {
			return qualifiedName;
		}

		/**
		 * 返回源文件的文件描述符
		 *
		 * @return  文件描述符
		 */
		public File getFile() {
			return file;
		}
	}

	/*
	 * 项目文件中定义的属性标记：
	 *
	 *    main - 主窗体类的限定名
	 *    name - 应用程序名称
	 *    version.code - 版本号
	 *    version.name - 版本名称
	 *    icon - 应用的图标
	 *    orientation - 应用屏幕方向
	 *    theme - 应用主题风格
	 *    source - 逗号分隔的源根目录列表
	 *    assets - 资产目录（用于与应用程序捆绑的图像和数据文件）
	 *    res - 资源目录（可编译资源）
	 *    build - 编译器的输出目录
	 *    key - Android应用程序签名的密钥存储位置
	 */
	private static final String MAINTAG = "main";
	private static final String NAMETAG = "name";
	private static final String VERSIONCODETAG = "version.code";
	private static final String VERSIONNAMETAG = "version.name";
	private static final String ICONTAG = "icon";
	private static final String ORIENTATIONTAG = "orientation";
	private static final String THEMETAG = "theme";
	private static final String SOURCETAG = "source";
	private static final String ASSETSTAG = "assets";
	private static final String RESTAG = "res";
	private static final String BUILDTAG = "build";
	private static final String KEYLOCATIONTAG = "key.location";
	private static final String KEYALIASTAG = "key.alias";

	// Simple的源文件扩展
	public static final String SOURCEFILE_EXTENSION = ".simple";

	// 包含项目属性的表
	private Properties properties;

	// 项目根目录
	private String projectRoot;

	// 构建输出目录覆盖，或为null
	private String buildDirOverride;

	// 源文件列表
	private List<SourceDescriptor> sources;

	/**
	 * 创建一个新的Simple项目描述符。
	 *
	 * @param projectFile  项目文件的路径
	 * @throws IOException  如果无法读取项目文件
	 */
	public Project(String projectFile) throws IOException {
		this(new File(projectFile));
	}

	/**
	 * 创建一个新的Simple项目描述符。
	 *
	 * @param projectFile  项目文件的路径
	 * @param buildDirOverride  构建输出目录覆盖，或为null
	 * @throws IOException  如果无法读取项目文件
	 */
	public Project(String projectFile, String buildDirOverride) throws IOException {
		this(new File(projectFile));
		this.buildDirOverride = buildDirOverride;
	}

	/**
	 * 创建一个新的Simple项目描述符。
	 *
	 * @param file  项目文件
	 * @throws IOException  如果无法读取项目文件
	 */
	public Project(File file) throws IOException {
		File parentFile = Preconditions.checkNotNull(file.getParentFile());
		projectRoot = parentFile.getAbsolutePath();

		// 加载项目文件
		properties = new Properties();
		FileInputStream in = new FileInputStream(file);
		try {
			// xhwsd@qq.com 2021-5-31 以指定编码方式载入，解决Simple属性文件含中文字符无法编译问题
			properties.load(new InputStreamReader(in, "UTF-8"));
		} finally {
			in.close();
		}
	}

	/**
	 * 返回主窗口类的名称
	 *
	 * @return  主窗口类名
	 */
	public String getMainForm() {
		return properties.getProperty(MAINTAG);
	}

	/**
	 * 设置主窗口类的名称。
	 *
	 * @param main  主窗口类名
	 */
	public void setMainForm(String main) {
		properties.setProperty(MAINTAG, main);
	}

	/**
	 * 返回项目（应用程序）的名称。
	 *
	 * @return  项目名称，缺省为{@code MyApp}
	 */
	public String getProjectName() {
		String value = properties.getProperty(NAMETAG, "");
		return value.isEmpty() ? "MyApp" : value;
	}

	/**
	 * 设置项目（应用程序）的名称。
	 *
	 * @param name  项目名称
	 */
	public void setProjectName(String name) {
		properties.setProperty(NAMETAG, name);
	}

	/**
	 * 返回应用版本号
	 *
	 * @return  应用版本号，缺省为{@code 1}
	 */
	public String getVersionCode() {
		String value = properties.getProperty(VERSIONCODETAG, "");
		return value.isEmpty() ? "1" : value;
	}

	/**
	 * 设置应用版本号
	 *
	 * @param code  应用版本号
	 */
	public void setVersionCode(String code) {
		properties.setProperty(VERSIONCODETAG, code);
	}

	/**
	 * 返回应用版本名
	 *
	 * @return  应用版本名，缺省为{@code 1.0}
	 */
	public String getVersionName() {
		String value = properties.getProperty(VERSIONNAMETAG, "");
		return value.isEmpty() ? "1.0" : value;
	}

	/**
	 * 设置应用版本名
	 *
	 * @param code  应用版本名
	 */
	public void setVersionName(String name) {
		properties.setProperty(VERSIONNAMETAG, name);
	}

	/**
	 * 返回应用图标
	 *
	 * @return  应用图标，缺省为{@code @drawable/icon}
	 */
	public String getIcon() {
		String value = properties.getProperty(ICONTAG, "");
		return value.isEmpty() ? "@drawable/icon" : value;
	}

	/**
	 * 设置应用图标
	 *
	 * @param icon  应用图标
	 */
	public void setIcon(String icon) {
		properties.setProperty(ICONTAG, icon);
	}

	/**
	 * 返回应用屏幕方向
	 *
	 * @return  屏幕方向，缺省为{@code unspecified}
	 */
	public String getOrientation() {
		String value = properties.getProperty(ORIENTATIONTAG, "");
		return value.isEmpty() ? "unspecified" : value;
	}

	/**
	 * 设置应用屏幕方向
	 *
	 * @param orientation  屏幕方向
	 */
	public void setOrientation(String orientation) {
		properties.setProperty(ORIENTATIONTAG, orientation);
	}

	/**
	 * 返回应用主题风格
	 *
	 * @return  主题风格，缺省为空文本
	 */
	public String getTheme() {
		/*
		参数文档：https://www.cnblogs.com/zhouyou96/p/5323138.html
		API 1:
		android:Theme 根主题
		android:Theme.Black 背景黑色
		android:Theme.Light 背景白色
		android:Theme.Wallpaper 以桌面墙纸为背景
		android:Theme.Translucent 透明背景
		android:Theme.Panel 平板风格
		android:Theme.Dialog 对话框风格

		API 11:
		android:Theme.Holo Holo根主题
		android:Theme.Holo.Black Holo黑主题
		android:Theme.Holo.Light Holo白主题

		选值：主题=Holo（默认黑色） 白色=+.Light 无标题=+.NoActionBar 全屏=+.Fullscreen (详细百度搜索)
		例如：@android:style/Theme.Holo.Light
		*/
		return properties.getProperty(THEMETAG, "");
	}

	/**
	 * 设置应用主题风格
	 *
	 * @param theme  主题风格
	 */
	public void setTheme(String theme) {
		properties.setProperty(THEMETAG, theme);
	}

	/**
	 * 返回项目的根路径。
	 *
	 * @return  项目根路径
	 */
	public String getProjectRoot() {
		return projectRoot;
	}

	/**
	 * 返回资产目录的位置。
	 *
	 * @return  资产目录，缺省为{@code ./res}的文件描述符
	 */
	public File getAssetsDirectory() {
		String value = properties.getProperty(ASSETSTAG);
		return new File(projectRoot, value.isEmpty() ? "./assets" : value);
	}

	/**
	 * 返回资源（res）目录的位置
	 *
	 * @return 资源（res）目录，缺省为{@code ./res}的文件描述符
	 */
	public File getResDirectory() {
		String value = properties.getProperty(RESTAG, "");
		return new File(projectRoot, value.isEmpty() ? "./res" : value);
	}

	/**
	 * 返回构建输出目录的位置。
	 *
	 * @return  构建输出目录，缺省为{@code ./build}的文件描述符
	 */
	public File getBuildDirectory() {
		if (buildDirOverride != null) {
			return new File(buildDirOverride);
		}
		String value = properties.getProperty(BUILDTAG, "");
		return new File(projectRoot, value.isEmpty() ? "./build" : value);
	}

	/**
	 * 返回用于签名Android应用程序的密钥库的位置。
	 *
	 * @return  密钥库位置，缺省为空字符串
	 */
	public String getKeystoreLocation() {
		return properties.getProperty(KEYLOCATIONTAG, "");
	}

	/**
	 * 设置用于签名Android应用程序的密钥库的位置。
	 *
	 * @param location  密钥库位置
	 */
	public void setKeystoreLocation(String location) {
		properties.setProperty(KEYLOCATIONTAG, location);
	}

	/**
	 * 返回用于签名Android应用程序的密钥库的别名。
	 *
	 * @return  别名的密钥库名称，缺省为空字符串
	 */
	public String getKeystoreAlias() {
		return properties.getProperty(KEYALIASTAG, "");
	}

	/**
	 * 设置用于签名Android应用程序的密钥库的别名名称。
	 *
	 * @param alias  别名的密钥库名称
	 */
	public void setKeystoreAlias(String alias) {
		properties.setProperty(KEYALIASTAG, alias);
	}

	/**
	 * 返回类库清单节点使用的项目宏值。
	 *
	 * <p>项目属性键固定为“类简名.宏名”，避免不同类库之间的宏名冲突。
	 *
	 * @param className 声明清单节点的类简名
	 * @param macroName 宏名
	 * @param defaultValue 项目未配置该宏时使用的缺省值，可为{@code null}
	 * @return 项目配置值；未配置时返回{@code defaultValue}
	 */
	public String getMacroValue(String className, String macroName, String defaultValue) {
		return properties.getProperty(className + "." + macroName, defaultValue);
	}

	/*
	 * 递归访问源目录并将找到的Simple源文件添加到源文件列表中。
	 */
	private void visitSourceDirectories(String root, File file) {
		if (file.isDirectory()) {
			// 递归访问嵌套目录。
			for (String child : file.list()) {
				visitSourceDirectories(root, new File(file, child));
			}
		} else {
			// 将Simple的源文件添加到源文件列表中
			if (file.getName().endsWith(SOURCEFILE_EXTENSION)) {
				String absName = file.getAbsolutePath();
				String name = absName.substring(root.length() + 1, absName.length() -
						SOURCEFILE_EXTENSION.length());
				sources.add(new SourceDescriptor(name.replace(File.separatorChar, '.'), file));
			}
		}
	}

	/**
	 * 返回项目中的Simple源文件列表。
	 *
	 * @return  源文件列表
	 */
	public List<SourceDescriptor> getSources() {
		// 懒惰地发现源文件
		if (sources == null) {
			sources = new ArrayList<SourceDescriptor>();
			String sourceTag = properties.getProperty(SOURCETAG);
			for (String sourceDir : sourceTag.split(",")) {
				File dir = new File(projectRoot + File.separatorChar + sourceDir);
				visitSourceDirectories(dir.getAbsolutePath(), dir);
			}
		}
		return sources;
	}
}

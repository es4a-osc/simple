# Simple
> [Simple](https://code.google.com/archive/p/simple/)是谷歌N年前开源的一款编程语言，目标是使用类似BASIC语法简单易用的去开发Android应用。

## 编程变得简单（Simple）
在90年代，一家来自北方（美国）的大公司非常成功地使用了一种编程语言BASIC（初学者通用符号指令代码的缩写）。它如此成功的原因之一是语言易于学习和使用。

为移动世界和Android平台带来一种易于学习和使用的语言是这个Simple项目的目标。Simple是开发Android应用程序的编程语言。它特别适合于非专业程序员（但不限于）。Simple允许程序员使用运行时系统提供的组件快速编写Android应用程序。

与90年代的相关程序类似，Simple程序是窗口定义（包含组件）和代码（包含程序逻辑）。组件和程序逻辑之间的交互是通过组件触发的事件来实现的。程序逻辑由事件处理程序组成，其中包含对事件作出反应的代码。

下面是两个用Simple编写的神奇画板（EtchSketch）和俄罗斯方块（Tetris）示例应用程序的屏幕截图。这些应用程序的源代码可以在Simple发行版的源代码的samples目录中找到：

<img src="https://gitee.com/es4a/simple/raw/master/_bak/tetris.jpg" width="240" height="400">

有关编写简单应用程序的更多信息，请参阅[如何编写Simple应用](#)。

**警告**：尽管这个项目已经进入初始阶段，但它仍在进行中。您很可能遇到错误以及遇到需要的情况，功能根本无法实现。您可以反馈问题或者更好的是自己解决问题并为Simple贡献解决方案！

## 当前源码结构

- `src/simple/compiler/`：Simple编译器源码。
- `src/simple/runtime/`：Android运行库源码。
- `samples/`：Simple示例项目。
- `tests/`：编译器和运行库测试。
- `build/`、`dist/`和`reports/`：构建生成目录，不在其中手工维护源码。

## 构建与测试

当前构建环境为JDK 8和Android API 26。构建前需要设置：

- `JAVA_HOME`：JDK 8根目录。
- `ANDROID_HOME`：Android工具根目录，其中需要包含`platforms/android-26`。
- `SIMPLE_HOME`：Simple发行目录，其中包含`SimpleCompiler.jar`和`SimpleAndroidRuntime.jar`。
- `LIBRARIES_HOME`：可选的扩展类库根目录；未设置或目录不存在时只加载Simple运行库。
- `KEY_PASSWORD`：可选的发布签名密码；项目同时配置`key.location`时才使用。

在本工作区执行完整测试：

```powershell
cd simple
$env:JAVA_HOME = "$PWD\..\sdk\tools\jdk1.8.0_503"
$env:ANDROID_HOME = "$PWD\..\sdk\tools\android"
$env:SIMPLE_HOME = "$PWD\dist\windows"
& "$PWD\..\sdk\tools\apache-ant-1.9.15\bin\ant.bat" runtests
```

`runtests`会先重新生成编译器和运行库，再编译并运行全部测试。构建产物位于`dist/`，SDK交付产物位于`../sdk/simple/`，二者不是同一目录。

## 扩展类库

扩展类库是可选能力。编译器通过`LIBRARIES_HOME`读取其直接子目录，不读取`library.json`：

```text
LIBRARIES_HOME/
└─ <类库>/
   ├─ classes.jar    # 必需，类库自身代码
   ├─ libs/          # 可选，依赖JAR
   ├─ res/           # 可选，Android资源
   ├─ assets/        # 可选，资产文件
   └─ jni/           # 可选，按ABI组织的原生SO
```

只有项目实际引用的类库才会参与APK构建。`library.json`只向ES4A扩展提供类库名称、图标和定义等IDE元数据；编译器不依赖该清单。

## 扩展Android清单

类库对象可以使用`simple.runtime.annotations.ManifestNodes`声明附加Android清单节点：

```java
@SimpleObject
@ManifestNodes(
	rootXml = "<uses-feature android:name=\"android.hardware.camera\" />",
	applicationXml = "<meta-data android:name=\"demo.appId\" android:value=\"${应用标识}\" />",
	activityXml = "<meta-data android:name=\"demo.mode\" android:value=\"${模式=normal}\" />",
	intentFilterXml = "<action android:name=\"demo.action.OPEN\" />")
public final class 示例对象 {
}
```

四个字段的插入位置如下：

- `rootXml`：`/manifest`。
- `applicationXml`：`/manifest/application`。
- `activityXml`：主`activity`。
- `intentFilterXml`：主`activity/intent-filter`。

清单节点同样只在项目实际引用该对象时注入。XML中支持两种项目宏：

- `${宏名}`：必填；项目没有配置时编译失败。
- `${宏名=缺省值}`：可选；项目没有配置时使用缺省值，缺省值允许为空。

项目在`project.properties`中使用“声明注解的类简名.宏名”赋值：

```properties
示例对象.应用标识=my-app-id
示例对象.模式=debug
```

宏替换不会自动生成或转义XML，类库作者必须保证注解内容及替换后的值能够组成合法的Android清单。

## 相关链接
- 更新日志 - [CHANGELOG](CHANGELOG.md)
- 资源下载 - [百度网盘](https://pan.baidu.com/s/1szDDTkgPANIgkKDqxw5zwg)（提取码`5npg`）
- 联系邮箱 - [xhwsd@qq.com](https://dwz.wsd.cx/wsd-yx)

> 目前需要JDK版本为1.8（java8），Android API等级为28。

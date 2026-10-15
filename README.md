# Simple

本仓库基于谷歌 [Simple](https://code.google.com/archive/p/simple/)进行中文化、功能扩展和 Android 兼容性改进，提供编译器、运行库源码及样例和测试。


## 目录

- `src/simple/compiler/`：编译器。
- `src/simple/runtime/`：Android 运行库。
- `samples/`：示例项目。
- `tests/`：编译器和运行库测试。


## 构建

1. 获取 [ES4A SDK 仓库](https://gitee.com/es4a/sdk)，与 `simple` 同级放置，并按 SDK 的 README 准备 JDK 和 Android 工具。
2. [下载 Ant](https://dwz.wsd.cx/es4a-ant)，解压到 `simple/tools/apache-ant-1.9.15/`。
3. 在 Windows 下，于本目录运行 `build.bat`。

脚本执行清理、构建和测试，产物位于 `dist/`。

构建成功后运行 `release.bat`，将 `dist/windows/` 中的编译器和运行库 JAR 更新到同级 `sdk/simple/`；发布不会重新构建。


## 相关链接

- [开源仓库](https://gitee.com/es4a)
- [留言反馈](https://dwz.wsd.cx/wsd-ly)

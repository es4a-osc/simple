# 应用设置

文档类型：运行库使用示例。核对日期：2026-10-16；依据：当前 Simple 编译器、运行库源码与本项目。

查看应用信息、保存和重新读取设置，输出运行日志。

运行准备与完整目录见[示例入口](https://gitee.com/es4a/sdk/blob/main/docs/Simple项目开发.md)。在 ES4A 添加本目录的 [project.properties](project.properties)，保存后选择“编译应用”或“调试应用”。源码：[主窗口](src/simple/examples/preferences/主窗口.simple)。

1. 修改文本并点击保存。
2. 改动输入框但不保存，再点击读取，观察已保存的值。
3. 关闭并重新打开应用，查看值是否保留。

只使用本应用的 `examples.message` 键；保存后关闭再打开应用可检查持久化。日志不包含账号、密码或设备标识。当前 Android 实现将设置保存为文本；未写入过的键返回空文本。

验证状态：已通过 APK 编译。Android 真机操作和扩展宿主可见交互尚待验证。

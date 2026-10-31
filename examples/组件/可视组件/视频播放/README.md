# 视频播放

文档类型：运行库使用示例。核对日期：2026-10-16；依据：当前 Simple 编译器、运行库源码与本项目。

播放随项目打包的短片，暂停、继续、定位和查询状态。

运行准备与完整目录见[项目说明](https://gitee.com/es4a/sdk/blob/main/docs/Simple项目开发.md)。在 ES4A 添加本目录的 [project.properties](project.properties)，保存后选择“编译应用”或“调试应用”。源码：[主窗口](src/simple/examples/video/主窗口.simple)。

1. 点击打开并播放内置短片。
2. 暂停、继续，并查询位置和时长。
3. 播放中可定位到开头；停止后应重新打开播放。

演示视频播放、暂停和恢复。视频从 `res/raw/demo.mp4` 通过 android.resource URI 播放，无需网络和共享存储。素材沿用仓库原 SmokeTests 的本地测试短片。当前接口没有准备完成或错误事件，不把播放方法返回当成播放成功。

验证状态：已通过 APK 编译。Android 真机操作和扩展宿主可见交互尚待验证。

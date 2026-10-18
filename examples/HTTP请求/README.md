# HTTP请求

通过 TCPLab 接口学习 GET、POST JSON、超时与响应状态处理。

运行准备见[项目说明](../../README.md)。在 ES4A 中打开本项目，按 `F5` 编译运行。

1. 联网后点击“查询我的 IP”，查看调用设备的出口 IP 响应。
2. 在 IP 输入框填写 `8.8.8.8`，点击“查询指定 IP”。
3. 保留域名 `example.com`，点击“查询 DNS（POST JSON）”，查看 JSON 请求与记录数组。
4. 比较 HTTP 状态、JSON 中的业务状态 `code` 与错误标识 `msg`。

接口与字段说明见 [TCPLab API 文档](https://tcp.lc/zh/page/api)。GET 使用 IP 查询接口，POST 使用 DNS 查询接口；接口有调用额度，请按需点击。HTTP 请求实现由所选 SDK 的运行库提供，失败时查看界面结果及运行日志。


POST JSON 需要包含输出修复的运行库；旧版运行库可能在写入请求体时失败。源码修复后，需构建并将运行库更新到所选 SDK，普通 F5 编译才会使用修复后的实现。

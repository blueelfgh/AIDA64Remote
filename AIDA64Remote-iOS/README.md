# AIDA64 Remote (iOS)

基于 SwiftUI 的 iPhone / iPad 横屏监控客户端，通过局域网实时读取 PC 端 [AIDA64](https://www.aida64.com/) RemoteSensor（LCD）推送的传感器数据，在设备上展示硬件仪表盘。

本目录为同仓中的 iOS 工程，与 [AIDA64Remote-Android](../AIDA64Remote-Android) 并列；协议与传感器 ID 映射与 Android 的 AIDA64 路径保持一致。

## 功能特性

- **实时监控**：通过 SSE（Server-Sent Events）订阅 AIDA64 传感器数据流
- **仪表盘布局**：横竖屏自适应展示 CPU、GPU、内存、硬盘、网络、风扇、FPS 等指标
- **自动重连**：连接中断后指数退避自动重试
- **连接记忆**：主机 IP / 端口等设置持久化，下次启动自动连接
- **主题切换**：深色 / 亮色 / 跟随系统
- **屏幕常亮**：可选保持主界面亮屏
- **全屏模式**：双击进入全屏，隐藏状态栏

## 环境要求

| 项目 | 要求 |
|------|------|
| iOS | 17.0+ |
| 开发工具 | Xcode 16+（推荐最新稳定版） |
| 网络 | 手机与 PC 处于同一局域网 |
| PC 端 | 已安装并启用 AIDA64 Extreme / Engineer 的 RemoteSensor |

## PC 端配置

1. 打开 AIDA64，进入 **File → Preferences → LCD**（或 RemoteSensor 相关选项）。
2. 启用 **RemoteSensor / LCD** 服务，确认监听端口（默认常见为 `35080`）。
3. 确保防火墙允许该端口的入站连接。
4. 在浏览器访问 `http://<PC局域网IP>:<端口>/` 能打开页面即可。

> 客户端会请求 `http://host:port/sse` 获取实时数据，并从根页面解析传感器标签。

## 使用方法

1. 用 Xcode 打开 `AIDA64Remote.xcodeproj`，选择真机或模拟器后运行。
2. 在「连接设置」中填写 PC 的局域网 IP 与端口。
3. 点击 **连接**，进入监控仪表盘。
4. 需要时可在设置中调整主题与屏幕常亮；双击仪表盘可切换全屏。

## 技术栈

- SwiftUI + Observation（`@Observable`）
- Swift Concurrency（`async/await` / `AsyncStream`）
- URLSession 流式读取 SSE
- UserDefaults 配置持久化

## 项目结构

```
AIDA64Remote/
├── AIDA64RemoteApp.swift      # 入口、常亮
├── ContentRootView.swift      # 设置 / 监控导航
├── Data/
│   ├── Aida64SseClient.swift  # SSE 连接与重连
│   ├── SensorParser.swift     # 传感器数据解析
│   ├── HtmlLabelParser.swift  # HTML 标签解析
│   ├── ConnectionConfig.swift
│   └── SettingsStore.swift
├── Model/
│   ├── SensorModels.swift
│   └── DashboardMapping.swift # 传感器 ID → 仪表盘映射
├── ViewModels/
│   └── SensorViewModel.swift
└── Views/
    ├── MonitorView.swift
    ├── SettingsView.swift
    ├── Components/            # 仪表盘面板
    └── Theme/
```

## 构建与测试

```bash
# 在仓库根目录
xcodebuild -project AIDA64Remote-iOS/AIDA64Remote.xcodeproj \
  -scheme AIDA64Remote \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=18.5' \
  build

# 单元测试
xcodebuild -project AIDA64Remote-iOS/AIDA64Remote.xcodeproj \
  -scheme AIDA64Remote \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=18.5' \
  test
```

## 注意事项

- 应用允许明文 HTTP（ATS `NSAllowsArbitraryLoads`），仅适合可信局域网使用。
- 支持横屏与竖屏：横屏为固定三行仪表盘网格，竖屏为可滚动自适应布局。
- 仪表盘字段映射依赖 AIDA64 RemoteSensor 页面中的传感器 ID；若 PC 端 LCD 布局不同，部分数值可能显示为 `—`，需按实际 ID 调整映射。

## 许可证

个人项目，按需使用。

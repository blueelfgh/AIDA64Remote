# AIDA64 Remote

基于 Jetpack Compose 的 Android 横屏监控客户端，通过局域网实时读取 PC 传感器数据并展示硬件仪表盘。

支持两种数据源（可在设置中切换）：

1. **AIDA64 RemoteSensor（LCD）** — SSE 推送
2. **LibreHardwareMonitor 托盘服务** — HTTP 轮询 `/api/snapshot`（无需配置 AIDA64 LCD 项）

配套 PC 服务见：[`LibreHardwareMonitorService`](../LibreHardwareMonitorService)

## 功能特性

- **双数据源**：AIDA64 SSE / LibreHardwareMonitor HTTP
- **实时监控**：按所选服务类型订阅或轮询传感器数据
- **仪表盘布局**：横屏展示 CPU、GPU、内存、硬盘、网络、风扇、FPS 等指标
- **自动重连**：连接中断后指数退避自动重试
- **连接记忆**：主机 IP / 端口 / 服务类型等设置持久化，下次启动自动连接
- **主题切换**：深色 / 亮色 / 跟随系统
- **屏幕常亮**：可选保持主界面亮屏
- **全屏模式**：双击进入全屏，隐藏系统栏

## 环境要求

| 项目 | 要求 |
|------|------|
| Android | minSdk 34 / targetSdk 35 |
| 开发工具 | Android Studio（推荐最新稳定版） |
| 网络 | 手机与 PC 处于同一局域网 |
| PC（AIDA64） | 已启用 AIDA64 Extreme / Engineer 的 RemoteSensor |
| PC（LHM） | 运行 LibreHardwareMonitorService（建议管理员 + 放行 18080） |

## PC 端配置

### 方案 A：AIDA64

1. 打开 AIDA64，进入 **File → Preferences → LCD**（或 RemoteSensor 相关选项）。
2. 启用 **RemoteSensor / LCD** 服务，确认监听端口（默认常见为 `35080`）。
3. 确保防火墙允许该端口的入站连接。
4. 在浏览器访问 `http://<PC局域网IP>:<端口>/` 能打开页面即可。

> 手机端会请求 `http://host:port/sse` 获取实时数据，并从根页面解析传感器标签。

### 方案 B：LibreHardwareMonitorService

1. 构建并运行 `LibreHardwareMonitorService`（建议管理员）。
2. 默认监听 `http://0.0.0.0:18080`，防火墙放行 TCP **18080**。
3. 本机自测：`http://127.0.0.1:18080/api/snapshot`。

## 使用方法

1. 用 Android Studio 打开本项目，连接真机或模拟器后运行。
2. 在「连接设置」中选择服务类型，填写 PC 的局域网 IP 与端口。
3. 点击 **连接**，进入监控仪表盘。
4. 需要时可在设置中调整主题与屏幕常亮；双击仪表盘可切换全屏。

默认端口：AIDA64 `35080`，LibreHardwareMonitor `18080`。切换服务类型时，若当前端口仍是上述默认值之一，会自动切到对应默认端口。

## 技术栈

- Kotlin + Jetpack Compose + Material 3
- Navigation Compose
- DataStore Preferences（配置持久化）
- OkHttp（HTTP / SSE）
- ViewModel + Kotlin Flow

## 项目结构

```
app/src/main/java/com/example/aida64remote/
├── MainActivity.kt          # 入口、导航、常亮/全屏
├── data/
│   ├── Aida64SseClient.kt   # AIDA64 SSE 连接与重连
│   ├── LhmHttpClient.kt     # LHM HTTP 轮询
│   ├── SensorParser.kt      # AIDA64 传感器数据解析
│   ├── HtmlLabelParser.kt   # HTML 标签解析
│   ├── ConnectionConfig.kt  # 连接配置
│   ├── AppSettings.kt       # 应用设置（含 ServiceType）
│   └── SettingsRepository.kt
├── model/
│   └── Models.kt            # 数据模型与仪表盘映射
└── ui/
    ├── MonitorScreen.kt     # 监控主界面
    ├── SettingsScreen.kt    # 连接与偏好设置
    ├── SensorViewModel.kt
    ├── components/          # 仪表盘面板组件
    └── theme/
```

## 构建与运行

```bash
# Debug 构建
./gradlew :app:assembleDebug

# 安装到已连接设备
./gradlew :app:installDebug
```

Windows 可使用 `gradlew.bat`。

## 注意事项

- 应用允许明文 HTTP（`usesCleartextTraffic`），仅适合可信局域网使用。
- 默认横屏（`sensorLandscape`），建议使用平板或支持横屏的设备。
- AIDA64 路径的仪表盘字段映射依赖 RemoteSensor 页面中的传感器 ID；若 PC 端 LCD 布局不同，部分数值可能显示为 `—`。
- LHM 路径由 PC 服务做启发式映射；不同硬件命名差异可能导致个别字段为空。

## 许可证

个人项目，按需使用。

# AIDA64 Remote

局域网硬件监控客户端（Android + iOS）同仓管理。

| 目录 | 说明 | 打开方式 |
|------|------|----------|
| [AIDA64Remote-Android](AIDA64Remote-Android/) | Android（Jetpack Compose） | Android Studio 打开该目录 |
| [AIDA64Remote-iOS](AIDA64Remote-iOS/) | iOS（SwiftUI） | Xcode 打开 `AIDA64Remote-iOS/AIDA64Remote.xcodeproj` |

## 数据源

- **Android**：支持 AIDA64 RemoteSensor（SSE）与 LibreHardwareMonitor 托盘服务（HTTP `/api/snapshot`）
- **iOS**：当前主要为 AIDA64 RemoteSensor（SSE）；与 Android 的 LHM / Widget 等能力对齐另议

配套 PC 服务见同级目录 [`LibreHardwareMonitorService`](../LibreHardwareMonitorService)。

## 快速构建

```bash
# Android
cd AIDA64Remote-Android
./gradlew :app:assembleDebug

# iOS（示例 destination 按本机模拟器调整）
xcodebuild -project AIDA64Remote-iOS/AIDA64Remote.xcodeproj \
  -scheme AIDA64Remote \
  -destination 'platform=iOS Simulator,name=iPhone 16,OS=18.5' \
  build
```

更细的功能说明、PC 配置与注意事项见各端 README。

## 分支说明

同仓目录整理在分支 `chore/monorepo-android-ios` 上完成；原 `LibreHardwareMonitorClient` 布局保持不变，确认后再合并。

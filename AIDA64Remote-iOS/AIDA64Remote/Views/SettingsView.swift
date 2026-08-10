import SwiftUI

struct SettingsView: View {
    let initialHost: String
    let initialPort: Int
    let serviceType: ServiceType
    let keepScreenOn: Bool
    let themeMode: ThemeMode
    let showsDisconnect: Bool
    let onServiceTypeChange: (ServiceType) -> Void
    let onKeepScreenOnChange: (Bool) -> Void
    let onThemeModeChange: (ThemeMode) -> Void
    let onClearBarPeaks: () -> Void
    let onConnect: (String, Int, ServiceType) -> Void
    let onDisconnect: (() -> Void)?

    @State private var host: String = ""
    @State private var portText: String = ""
    @State private var selectedType: ServiceType = .aida64
    @State private var hostError: String?
    @State private var portError: String?
    @State private var peaksClearedHint = false

    var body: some View {
        Form {
            Section {
                Text(hintText)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }

            Section("服务类型") {
                Text("切换类型时，若端口仍是默认值会自动改为对应默认端口。")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Picker("服务", selection: $selectedType) {
                    ForEach(ServiceType.allCases) { type in
                        Text(type.title).tag(type)
                    }
                }
                .pickerStyle(.segmented)
                .onChange(of: selectedType) { _, newValue in
                    onServiceTypeChange(newValue)
                    // 若本地端口仍是默认端口之一，同步刷新输入框
                    let port = Int(portText) ?? 0
                    if port == ServiceType.aida64.defaultPort
                        || port == ServiceType.libreHardwareMonitor.defaultPort {
                        portText = String(newValue.defaultPort)
                    }
                }
            }

            Section("连接") {
                TextField("主机 IP", text: $host)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.numbersAndPunctuation)
                if let hostError {
                    Text(hostError).font(.caption).foregroundStyle(.red)
                }

                TextField("端口", text: $portText)
                    .keyboardType(.numberPad)
                    .onChange(of: portText) { _, newValue in
                        portText = String(newValue.filter(\.isNumber).prefix(5))
                    }
                if let portError {
                    Text(portError).font(.caption).foregroundStyle(.red)
                }
            }

            Section {
                Toggle(isOn: Binding(
                    get: { keepScreenOn },
                    set: onKeepScreenOnChange
                )) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("屏幕常亮")
                        Text("开启后主界面保持亮屏，并显示常亮提示")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            Section {
                Text("可选择深色、亮色，或跟随系统")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Picker("主题", selection: Binding(
                    get: { themeMode },
                    set: onThemeModeChange
                )) {
                    ForEach(ThemeMode.allCases) { mode in
                        Text(mode.title).tag(mode)
                    }
                }
                .pickerStyle(.segmented)
            } header: {
                Text("主题")
            }

            Section {
                Button {
                    onClearBarPeaks()
                    peaksClearedHint = true
                } label: {
                    Text("清除进度条峰值")
                        .frame(maxWidth: .infinity)
                }
                if peaksClearedHint {
                    Text("已清除历史峰值，进度条将重新按当前数据缩放。")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            } header: {
                Text("进度条")
            } footer: {
                Text("峰值用于把进度条满刻度设为历史最大值，便于观察相对负载。")
            }

            Section {
                Button {
                    connectTapped()
                } label: {
                    Text(showsDisconnect ? "重新连接" : "连接")
                        .frame(maxWidth: .infinity)
                }
            }

            if showsDisconnect, let onDisconnect {
                Section {
                    Button("断开连接", role: .destructive, action: onDisconnect)
                        .frame(maxWidth: .infinity)
                }
            }
        }
        .navigationTitle("连接设置")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.visible, for: .navigationBar)
        .onAppear {
            host = initialHost
            portText = String(initialPort)
            selectedType = serviceType
            peaksClearedHint = false
        }
        .onChange(of: serviceType) { _, newValue in
            selectedType = newValue
        }
        .onChange(of: initialPort) { _, newValue in
            portText = String(newValue)
        }
    }

    private var hintText: String {
        switch selectedType {
        case .aida64:
            return "请在 PC 端 AIDA64 启用 RemoteSensor（LCD），并确保与手机在同一局域网。"
        case .libreHardwareMonitor:
            return "请确保 PC 端 LibreHardwareMonitor 导出服务已启动（默认端口 18080），并与手机在同一局域网。"
        }
    }

    private func connectTapped() {
        let trimmed = host.trimmingCharacters(in: .whitespacesAndNewlines)
        let port = Int(portText)
        var ok = true
        if trimmed.isEmpty {
            hostError = "请输入有效的主机地址"
            ok = false
        } else {
            hostError = nil
        }
        if let port, (1...65535).contains(port) {
            portError = nil
        } else {
            portError = "端口需为 1–65535"
            ok = false
        }
        if ok, let port {
            onConnect(trimmed, port, selectedType)
        }
    }
}

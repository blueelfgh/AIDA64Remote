import SwiftUI

struct SettingsView: View {
    let initialHost: String
    let initialPort: Int
    let keepScreenOn: Bool
    let themeMode: ThemeMode
    let showsDisconnect: Bool
    let onKeepScreenOnChange: (Bool) -> Void
    let onThemeModeChange: (ThemeMode) -> Void
    let onConnect: (String, Int) -> Void
    let onDisconnect: (() -> Void)?

    @State private var host: String = ""
    @State private var portText: String = ""
    @State private var hostError: String?
    @State private var portError: String?

    var body: some View {
        Form {
            Section {
                Text("请在 PC 端 AIDA64 启用 RemoteSensor（LCD），并确保与手机在同一局域网。")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
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
            onConnect(trimmed, port)
        }
    }
}

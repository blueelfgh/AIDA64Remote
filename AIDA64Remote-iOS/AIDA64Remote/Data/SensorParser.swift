import Foundation

enum SensorParser {
    private static let pageMarker = try! NSRegularExpression(pattern: #"^Page\d+$"#, options: .caseInsensitive)

    static func parse(_ dataLine: String) -> [SensorItem] {
        var payload = dataLine
        if payload.hasPrefix("data:") {
            payload = String(payload.dropFirst(5))
        }
        payload = payload.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !payload.isEmpty else { return [] }

        return payload
            .components(separatedBy: "{|}")
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .compactMap { segment -> SensorItem? in
                guard let separator = segment.firstIndex(of: "|") else { return nil }
                let id = String(segment[..<separator]).trimmingCharacters(in: .whitespacesAndNewlines)
                guard !id.isEmpty, !isPageMarker(id) else { return nil }
                let rawValue = String(segment[segment.index(after: separator)...])
                let value = rawValue
                    .split(separator: "|", maxSplits: 1, omittingEmptySubsequences: false)
                    .first
                    .map(String.init)?
                    .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
                return SensorItem(id: id, value: value)
            }
    }

    static func toMap(_ items: [SensorItem]) -> [String: String] {
        Dictionary(uniqueKeysWithValues: items.map { ($0.id, $0.value) })
    }

    private static func isPageMarker(_ id: String) -> Bool {
        let range = NSRange(id.startIndex..<id.endIndex, in: id)
        return pageMarker.firstMatch(in: id, options: [], range: range) != nil
    }
}

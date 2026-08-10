import Foundation

enum HtmlLabelParser {
    private static let labelSpan = try! NSRegularExpression(
        pattern: #"(?is)<span\s+[^>]*id\s*=\s*[\"'](Label\d+)[\"'][^>]*>(.*?)</span>"#,
        options: []
    )
    private static let siLabelNear = try! NSRegularExpression(
        pattern: #"(?is)id\s*=\s*[\"']SI(\d+)[\"'][\s\S]{0,1200}?float:\s*left[^>]*>(.*?)</div>"#,
        options: []
    )
    private static let tagStrip = try! NSRegularExpression(pattern: #"(?is)<[^>]+>"#, options: [])
    private static let whitespace = try! NSRegularExpression(pattern: #"\s+"#, options: [])

    static func parseLabels(_ html: String) -> [String: String] {
        var labels: [String: String] = [:]
        let nsRange = NSRange(html.startIndex..<html.endIndex, in: html)

        labelSpan.enumerateMatches(in: html, options: [], range: nsRange) { match, _, _ in
            guard let match,
                  let idRange = Range(match.range(at: 1), in: html),
                  let textRange = Range(match.range(at: 2), in: html)
            else { return }
            let id = String(html[idRange])
            let text = decodeHTML(String(html[textRange]))
            if !text.isEmpty {
                labels[id] = text
            }
        }

        siLabelNear.enumerateMatches(in: html, options: [], range: nsRange) { match, _, _ in
            guard let match,
                  let indexRange = Range(match.range(at: 1), in: html),
                  let textRange = Range(match.range(at: 2), in: html)
            else { return }
            let index = String(html[indexRange])
            let text = decodeHTML(String(html[textRange]))
            if !text.isEmpty {
                labels["SIV\(index)"] = text
                labels["SI\(index)"] = text
            }
        }

        return labels
    }

    private static func decodeHTML(_ raw: String) -> String {
        var text = tagStrip.stringByReplacingMatches(
            in: raw,
            options: [],
            range: NSRange(raw.startIndex..<raw.endIndex, in: raw),
            withTemplate: " "
        )
        text = text
            .replacingOccurrences(of: "&nbsp;", with: " ")
            .replacingOccurrences(of: "&amp;", with: "&")
            .replacingOccurrences(of: "&lt;", with: "<")
            .replacingOccurrences(of: "&gt;", with: ">")
            .replacingOccurrences(of: "&quot;", with: "\"")
            .replacingOccurrences(of: "&#39;", with: "'")
        text = whitespace.stringByReplacingMatches(
            in: text,
            options: [],
            range: NSRange(text.startIndex..<text.endIndex, in: text),
            withTemplate: " "
        )
        return text.trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

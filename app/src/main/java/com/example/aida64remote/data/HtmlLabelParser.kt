package com.example.aida64remote.data

import android.text.Html
import java.util.regex.Pattern

object HtmlLabelParser {
    private val labelSpan = Pattern.compile(
        """(?is)<span\s+[^>]*id\s*=\s*["'](Label\d+)["'][^>]*>(.*?)</span>""",
    )
    private val siLabelNear = Pattern.compile(
        """(?is)id\s*=\s*["']SI(\d+)["'][\s\S]{0,1200}?float:\s*left[^>]*>(.*?)</div>""",
    )

    fun parseLabels(html: String): Map<String, String> {
        val labels = linkedMapOf<String, String>()

        val labelMatcher = labelSpan.matcher(html)
        while (labelMatcher.find()) {
            val id = labelMatcher.group(1) ?: continue
            val text = decodeHtml(labelMatcher.group(2).orEmpty())
            if (text.isNotBlank()) {
                labels[id] = text
            }
        }

        val siMatcher = siLabelNear.matcher(html)
        while (siMatcher.find()) {
            val index = siMatcher.group(1) ?: continue
            val text = decodeHtml(siMatcher.group(2).orEmpty())
            if (text.isNotBlank()) {
                labels["SIV$index"] = text
                labels["SI$index"] = text
            }
        }

        return labels
    }

    private fun decodeHtml(raw: String): String {
        val withoutTags = raw
            .replace(Regex("(?is)<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return Html.fromHtml(withoutTags, Html.FROM_HTML_MODE_LEGACY).toString().trim()
    }
}

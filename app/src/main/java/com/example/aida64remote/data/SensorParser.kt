package com.example.aida64remote.data

import com.example.aida64remote.model.SensorItem

object SensorParser {
    private val pageMarker = Regex("^Page\\d+$", RegexOption.IGNORE_CASE)

    fun parse(dataLine: String): List<SensorItem> {
        val payload = dataLine
            .removePrefix("data:")
            .trim()
            .ifEmpty { return emptyList() }

        return payload
            .split("{|}")
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { segment ->
                val separator = segment.indexOf('|')
                if (separator <= 0) return@mapNotNull null
                val id = segment.substring(0, separator).trim()
                if (id.isEmpty() || pageMarker.matches(id)) return@mapNotNull null
                val rawValue = segment.substring(separator + 1)
                val value = rawValue.substringBefore('|').trim()
                SensorItem(id = id, value = value)
            }
            .toList()
    }

    fun toMap(items: List<SensorItem>): Map<String, String> =
        items.associate { it.id to it.value }
}

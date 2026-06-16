package com.zestyysports.app.data.model

object M3UParser {

    fun parse(content: String, sourceId: String = ""): List<Channel> {
        val lines = content.lines()
        val items = mutableListOf<Channel>()
        var pendingName = ""
        var pendingLogo = ""
        var pendingGroup = ""
        var pendingId = ""
        var index = 0

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                val logoMatch = Regex("""tvg-logo=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(trimmed)
                val nameMatch = Regex("""tvg-name=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(trimmed)
                val groupMatch = Regex("""group-title=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(trimmed)
                val idMatch = Regex("""tvg-id=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(trimmed)

                val lastComma = trimmed.lastIndexOf(',')
                val displayName = if (lastComma != -1) trimmed.substring(lastComma + 1).trim() else "Unknown Channel"

                pendingName = nameMatch?.groupValues?.get(1)?.takeIf { it.isNotBlank() } ?: displayName
                pendingLogo = logoMatch?.groupValues?.get(1) ?: ""
                pendingGroup = groupMatch?.groupValues?.get(1) ?: "General"
                pendingId = idMatch?.groupValues?.get(1) ?: ""

            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                if (pendingName.isNotBlank()) {
                    val id = if (pendingId.isNotBlank()) "$sourceId-$pendingId-$index"
                             else "$sourceId-ch-$index"
                    items.add(
                        Channel(
                            id = id,
                            name = pendingName,
                            logo = pendingLogo,
                            url = trimmed,
                            group = pendingGroup,
                            sourceId = sourceId
                        )
                    )
                    pendingName = ""
                    pendingLogo = ""
                    pendingGroup = ""
                    pendingId = ""
                    index++
                }
            }
        }
        return items
    }
}

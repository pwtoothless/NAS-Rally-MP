package com.nasrally.nasrally

data class RallyCalendarEvent(
    val id: String,
    val title: String,
    val description: String?,
    val startDateIso: String?,
    val endDateIso: String?
)

fun generateIcsContent(event: RallyCalendarEvent): String {
    val startStr = formatIcsDate(event.startDateIso)
    val endStr = formatIcsDate(event.endDateIso)
    val cleanDesc = (event.description ?: "NAS Rally Event: ${event.title}")
        .replace("\n", "\\n")
        .replace(",", "\\,")

    return buildString {
        append("BEGIN:VCALENDAR\r\n")
        append("VERSION:2.0\r\n")
        append("PRODID:-//NAS Rally//NONSGML Calendar Event//EN\r\n")
        append("CALSCALE:GREGORIAN\r\n")
        append("BEGIN:VEVENT\r\n")
        append("UID:${event.id}@nasrally.com\r\n")
        append("SUMMARY:${event.title}\r\n")
        append("DESCRIPTION:$cleanDesc\r\n")
        if (startStr.isNotBlank()) {
            append("DTSTART:$startStr\r\n")
        }
        if (endStr.isNotBlank()) {
            append("DTEND:$endStr\r\n")
        }
        append("END:VEVENT\r\n")
        append("END:VCALENDAR\r\n")
    }
}

private fun formatIcsDate(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return ""
    val cleaned = dateStr.replace("-", "").replace(":", "").replace(" ", "T")
    return if (cleaned.length == 8) {
        "${cleaned}T090000Z"
    } else if (!cleaned.endsWith("Z") && cleaned.contains("T")) {
        "${cleaned}Z"
    } else {
        cleaned
    }
}

expect object CalendarManager {
    fun addEventToCalendar(event: RallyCalendarEvent)
}

package com.nasrally.nasrally

/**
 * Data model representing a rally calendar event for export.
 *
 * @property id Unique identifier of the rally event.
 * @property title Name/summary of the rally.
 * @property description Details/description of the rally event.
 * @property startDateIso ISO 8601 formatted start timestamp string.
 * @property endDateIso ISO 8601 formatted end timestamp string.
 */
data class RallyCalendarEvent(
    val id: String,
    val title: String,
    val description: String?,
    val startDateIso: String?,
    val endDateIso: String?
)

/**
 * Generates valid iCalendar (.ics) format file content from a [RallyCalendarEvent].
 *
 * @param event The rally calendar event data.
 * @return Formatted iCalendar string payload.
 */
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

/**
 * Converts ISO 8601 date strings to iCalendar compliant UTC timestamp strings (YYYYMMDDTHHMMSSZ).
 */
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

/**
 * Multiplatform expect object for adding rally events directly to native system calendars.
 */
expect object CalendarManager {
    /**
     * Adds the specified [RallyCalendarEvent] to the device's native calendar.
     */
    fun addEventToCalendar(event: RallyCalendarEvent)
}

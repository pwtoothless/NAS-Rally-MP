package com.nasrally.nasrally

import java.awt.Desktop
import java.io.File

/**
 * Desktop JVM actual calendar manager implementation.
 *
 * Writes iCalendar (.ics) file to a temporary location and opens it via [Desktop.getDesktop].
 */
actual object CalendarManager {
    actual fun addEventToCalendar(event: RallyCalendarEvent) {
        try {
            val icsContent = generateIcsContent(event)
            val tempFile = File.createTempFile("rally_event_", ".ics")
            tempFile.writeText(icsContent)
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(tempFile)
            }
        } catch (e: Exception) {
            println("Error opening calendar on Desktop/JVM: ${e.message}")
        }
    }
}

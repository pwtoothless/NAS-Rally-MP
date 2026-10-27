package com.nasrally.nasrally

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * iOS actual calendar manager implementation.
 *
 * Encodes iCalendar (.ics) content into a data URI scheme and opens it via [UIApplication.sharedApplication].
 */
actual object CalendarManager {
    actual fun addEventToCalendar(event: RallyCalendarEvent) {
        try {
            val ics = generateIcsContent(event)
            val encoded = ics.encodeUrl()
            val url = NSURL.URLWithString("data:text/calendar;charset=utf8,$encoded")
            if (url != null) {
                UIApplication.sharedApplication.openURL(url)
            }
        } catch (e: Exception) {
            println("Error launching iOS calendar: ${e.message}")
        }
    }
}

private fun String.encodeUrl(): String {
    return this.replace("\n", "%0A")
        .replace("\r", "%0D")
        .replace(" ", "%20")
        .replace(":", "%3A")
        .replace(";", "%3B")
        .replace(",", "%2C")
}

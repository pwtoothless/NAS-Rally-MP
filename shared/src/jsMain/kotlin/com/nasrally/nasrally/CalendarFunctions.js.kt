package com.nasrally.nasrally

import kotlinx.browser.document
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag

actual object CalendarManager {
    actual fun addEventToCalendar(event: RallyCalendarEvent) {
        try {
            val icsContent = generateIcsContent(event)
            val blob = Blob(arrayOf(icsContent), BlobPropertyBag(type = "text/calendar;charset=utf-8"))
            val blobUrl = URL.createObjectURL(blob)
            val anchor = document.createElement("a") as HTMLAnchorElement
            anchor.href = blobUrl
            anchor.download = "${event.title.replace(" ", "_")}.ics"
            document.body?.appendChild(anchor)
            anchor.click()
            document.body?.removeChild(anchor)
            URL.revokeObjectURL(blobUrl)
        } catch (e: Exception) {
            println("Error downloading calendar .ics file on Web JS: ${e.message}")
        }
    }
}

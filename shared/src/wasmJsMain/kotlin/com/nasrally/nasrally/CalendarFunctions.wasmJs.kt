package com.nasrally.nasrally

import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""
(filename, content) => {
    try {
        var blob = new Blob([content], { type: 'text/calendar;charset=utf-8' });
        var blobUrl = URL.createObjectURL(blob);
        var anchor = document.createElement('a');
        anchor.href = blobUrl;
        anchor.download = filename;
        document.body.appendChild(anchor);
        anchor.click();
        document.body.removeChild(anchor);
        URL.revokeObjectURL(blobUrl);
    } catch (e) {
        console.error("Calendar download error: " + e);
    }
}
""")
private external fun jsDownloadIcsFile(filename: String, content: String)

/**
 * Wasm/JS Web actual calendar manager implementation invoking external JS function to download .ics files.
 */
actual object CalendarManager {
    @OptIn(ExperimentalWasmJsInterop::class)
    actual fun addEventToCalendar(event: RallyCalendarEvent) {
        try {
            val ics = generateIcsContent(event)
            val filename = "${event.title.replace(" ", "_")}.ics"
            jsDownloadIcsFile(filename, ics)
        } catch (e: Exception) {
            println("Error downloading calendar .ics file on Wasm JS: ${e.message}")
        }
    }
}

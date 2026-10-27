package com.nasrally.nasrally

import android.app.Application
import android.content.Intent
import android.provider.CalendarContract

/**
 * Android actual calendar manager implementation.
 *
 * Launches an [Intent.ACTION_INSERT] intent to add events to the native Android System Calendar via [CalendarContract].
 */
actual object CalendarManager {
    actual fun addEventToCalendar(event: RallyCalendarEvent) {
        try {
            val app = try {
                val activityThreadClass = Class.forName("android.app.ActivityThread")
                val method = activityThreadClass.getMethod("currentApplication")
                method.invoke(null) as? Application
            } catch (_: Exception) {
                null
            }

            if (app != null) {
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                    putExtra(CalendarContract.Events.TITLE, event.title)
                    putExtra(CalendarContract.Events.DESCRIPTION, event.description ?: "NAS Rally Event: ${event.title}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                app.startActivity(intent)
            }
        } catch (e: Exception) {
            println("Error adding event to Android calendar: ${e.message}")
        }
    }
}

package com.nasrally.nasrally

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedCommonTest {

    @Test
    fun testGenerateIcsContent() {
        val event = RallyCalendarEvent(
            id = "test-123",
            title = "Test Rally Event",
            description = "Fun driving rally",
            startDateIso = "2025-07-20",
            endDateIso = "2025-07-22"
        )
        val ics = generateIcsContent(event)
        assertTrue(ics.contains("BEGIN:VCALENDAR"))
        assertTrue(ics.contains("SUMMARY:Test Rally Event"))
        assertTrue(ics.contains("DESCRIPTION:Fun driving rally"))
        assertTrue(ics.contains("END:VCALENDAR"))
    }

    @Test
    fun testNotificationRateLimiter() {
        val groupId = "group-abc"
        // First notification should be allowed
        val allowedFirst = NotificationManager.shouldAllowOutOfAppNotification(groupId, cooldownMillis = 5000L)
        assertTrue(allowedFirst)

        // Immediate second notification should be throttled
        val allowedSecond = NotificationManager.shouldAllowOutOfAppNotification(groupId, cooldownMillis = 5000L)
        assertFalse(allowedSecond)
    }
}

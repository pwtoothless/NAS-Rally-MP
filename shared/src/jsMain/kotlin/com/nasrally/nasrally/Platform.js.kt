package com.nasrally.nasrally

import web.navigator.navigator

/**
 * JS Web platform descriptor implementation.
 */
class JsPlatform: Platform {
    private val userAgent = navigator.userAgent
    private val browserList = listOf("Chrome", "Firefox", "Safari", "Edge")

    override val name: String = userAgent.findAnyOf(browserList, ignoreCase = true)
            ?.let { (startIndex) -> userAgent.substring(startIndex).substringBefore(" ") }
            ?: "Unknown"
}

/**
 * Returns [JsPlatform] instance.
 */
actual fun getPlatform(): Platform = JsPlatform()
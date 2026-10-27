package com.nasrally.nasrally

/**
 * Represents execution platform details.
 */
interface Platform {
    /**
     * Name and version string of the platform implementation.
     */
    val name: String
}

/**
 * Factory function returning the platform-specific [Platform] instance.
 */
expect fun getPlatform(): Platform
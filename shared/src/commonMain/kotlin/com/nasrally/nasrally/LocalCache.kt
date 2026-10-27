package com.nasrally.nasrally

/**
 * Multiplatform expect object for key-value preference storage across Android, iOS, JVM Desktop, and Wasm/JS Web.
 */
expect object LocalCache {
    /**
     * Retrieves a stored string value for [key], or `null` if missing.
     */
    fun getString(key: String): String?

    /**
     * Stores a string [value] under [key].
     */
    fun setString(key: String, value: String)

    /**
     * Retrieves a stored long integer value for [key], or `null` if missing.
     */
    fun getLong(key: String): Long?

    /**
     * Stores a long integer [value] under [key].
     */
    fun setLong(key: String, value: Long)
}

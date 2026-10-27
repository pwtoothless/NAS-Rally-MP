package com.nasrally.nasrally

/**
 * Multiplatform expect object for raw binary blob and file timestamp storage on disk/local storage.
 */
expect object LocalDiskCache {
    /**
     * Reads cached binary bytes stored under [key].
     */
    fun getBytes(key: String): ByteArray?

    /**
     * Saves binary data [bytes] under [key].
     */
    fun saveBytes(key: String, bytes: ByteArray)

    /**
     * Gets the modification timestamp (in milliseconds) of cached data under [key].
     */
    fun getTimestamp(key: String): Long?

    /**
     * Stores a modification timestamp for [key].
     */
    fun saveTimestamp(key: String, timestamp: Long)

    /**
     * Removes cached data for [key].
     */
    fun remove(key: String)
}

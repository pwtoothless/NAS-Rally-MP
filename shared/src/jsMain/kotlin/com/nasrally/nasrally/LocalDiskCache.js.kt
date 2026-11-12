package com.nasrally.nasrally

/**
 * JS Web actual disk cache implementation backed by in-memory storage map.
 * Includes cache versioning for automatic invalidation on deployment.
 */
actual object LocalDiskCache {
    private val memoryCache = mutableMapOf<String, ByteArray>()

    actual fun getBytes(key: String): ByteArray? {
        return memoryCache[CacheVersion.versionedKey(key)]
    }

    actual fun saveBytes(key: String, bytes: ByteArray) {
        memoryCache[CacheVersion.versionedKey(key)] = bytes
    }

    actual fun getTimestamp(key: String): Long? {
        return LocalCache.getLong("ts_${CacheVersion.versionedKey(key)}")
    }

    actual fun saveTimestamp(key: String, timestamp: Long) {
        LocalCache.setLong("ts_${CacheVersion.versionedKey(key)}", timestamp)
    }

    actual fun remove(key: String) {
        memoryCache.remove(CacheVersion.versionedKey(key))
    }
}

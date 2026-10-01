package com.nasrally.nasrally

actual object LocalDiskCache {
    private val memoryCache = mutableMapOf<String, ByteArray>()

    actual fun getBytes(key: String): ByteArray? {
        return memoryCache[key]
    }

    actual fun saveBytes(key: String, bytes: ByteArray) {
        memoryCache[key] = bytes
    }

    actual fun getTimestamp(key: String): Long? {
        return LocalCache.getLong("ts_$key")
    }

    actual fun saveTimestamp(key: String, timestamp: Long) {
        LocalCache.setLong("ts_$key", timestamp)
    }

    actual fun remove(key: String) {
        memoryCache.remove(key)
    }
}

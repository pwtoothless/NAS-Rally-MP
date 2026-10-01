package com.nasrally.nasrally

expect object LocalDiskCache {
    fun getBytes(key: String): ByteArray?
    fun saveBytes(key: String, bytes: ByteArray)
    fun getTimestamp(key: String): Long?
    fun saveTimestamp(key: String, timestamp: Long)
    fun remove(key: String)
}

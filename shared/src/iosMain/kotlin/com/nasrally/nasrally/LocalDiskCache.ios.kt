package com.nasrally.nasrally

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.getBytes
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class)
actual object LocalDiskCache {
    private fun getCachePath(): String {
        val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
        val basePath = paths.firstOrNull() as? String ?: ""
        val fullPath = "$basePath/nasrally_disk_cache"
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(fullPath)) {
            fileManager.createDirectoryAtPath(fullPath, withIntermediateDirectories = true, attributes = null, error = null)
        }
        return fullPath
    }

    private fun getFilePath(key: String): String = "${getCachePath()}/$key"

    actual fun getBytes(key: String): ByteArray? {
        val path = getFilePath(key)
        val data = NSData.dataWithContentsOfFile(path) ?: return null
        val length = data.length.toInt()
        if (length == 0) return null
        val bytes = ByteArray(length)
        bytes.usePinned { pinned ->
            data.getBytes(pinned.addressOf(0), data.length)
        }
        return bytes
    }

    actual fun saveBytes(key: String, bytes: ByteArray) {
        val path = getFilePath(key)
        if (bytes.isEmpty()) return
        val data = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
        data.writeToFile(path, atomically = true)
    }

    actual fun getTimestamp(key: String): Long? {
        return LocalCache.getLong("ts_$key")
    }

    actual fun saveTimestamp(key: String, timestamp: Long) {
        LocalCache.setLong("ts_$key", timestamp)
    }

    actual fun remove(key: String) {
        val fileManager = NSFileManager.defaultManager
        fileManager.removeItemAtPath(getFilePath(key), error = null)
    }
}

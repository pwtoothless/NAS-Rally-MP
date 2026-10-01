package com.nasrally.nasrally

import android.app.Application
import java.io.File

actual object LocalDiskCache {
    private fun getCacheDir(): File {
        val app = try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val method = activityThreadClass.getMethod("currentApplication")
            method.invoke(null) as? Application
        } catch (_: Exception) {
            null
        }
        val baseDir = app?.cacheDir ?: File(System.getProperty("java.io.tmpdir") ?: ".", "nasrally_cache")
        val dir = File(baseDir, "nasrally_disk_cache")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getFile(key: String): File = File(getCacheDir(), key)
    private fun getMetaFile(key: String): File = File(getCacheDir(), "$key.meta")

    actual fun getBytes(key: String): ByteArray? {
        val file = getFile(key)
        return if (file.exists() && file.isFile) {
            try { file.readBytes() } catch (e: Exception) { null }
        } else {
            null
        }
    }

    actual fun saveBytes(key: String, bytes: ByteArray) {
        try {
            getFile(key).writeBytes(bytes)
        } catch (e: Exception) {
            println("Error saving disk cache bytes: ${e.message}")
        }
    }

    actual fun getTimestamp(key: String): Long? {
        val metaFile = getMetaFile(key)
        if (metaFile.exists()) {
            val ts = metaFile.readText().trim().toLongOrNull()
            if (ts != null) return ts
        }
        val file = getFile(key)
        return if (file.exists()) file.lastModified() else null
    }

    actual fun saveTimestamp(key: String, timestamp: Long) {
        try {
            getMetaFile(key).writeText(timestamp.toString())
        } catch (e: Exception) {
            println("Error saving timestamp: ${e.message}")
        }
    }

    actual fun remove(key: String) {
        try {
            getFile(key).delete()
            getMetaFile(key).delete()
        } catch (_: Exception) {}
    }
}

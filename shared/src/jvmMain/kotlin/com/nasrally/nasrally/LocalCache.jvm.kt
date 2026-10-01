package com.nasrally.nasrally

import java.util.prefs.Preferences

actual object LocalCache {
    private val prefs = Preferences.userRoot().node("nasrally_cache")

    actual fun getString(key: String): String? {
        return prefs.get(key, null)
    }

    actual fun setString(key: String, value: String) {
        prefs.put(key, value)
    }

    actual fun getLong(key: String): Long? {
        val valStr = prefs.get(key, null) ?: return null
        return valStr.toLongOrNull()
    }

    actual fun setLong(key: String, value: Long) {
        prefs.put(key, value.toString())
    }
}

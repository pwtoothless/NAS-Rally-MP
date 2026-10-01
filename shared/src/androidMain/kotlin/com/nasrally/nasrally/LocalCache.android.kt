package com.nasrally.nasrally

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import java.util.prefs.Preferences

actual object LocalCache {
    private val sharedPrefs: SharedPreferences? by lazy {
        try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val method = activityThreadClass.getMethod("currentApplication")
            val app = method.invoke(null) as? Application
            app?.getSharedPreferences("nasrally_cache", Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
    }

    private val fallbackPrefs by lazy {
        try {
            Preferences.userRoot().node("nasrally_cache")
        } catch (_: Exception) {
            null
        }
    }

    actual fun getString(key: String): String? {
        val sp = sharedPrefs
        if (sp != null) {
            return sp.getString(key, null)
        }
        return fallbackPrefs?.get(key, null)
    }

    actual fun setString(key: String, value: String) {
        val sp = sharedPrefs
        if (sp != null) {
            sp.edit().putString(key, value).apply()
        } else {
            fallbackPrefs?.put(key, value)
        }
    }

    actual fun getLong(key: String): Long? {
        val sp = sharedPrefs
        if (sp != null) {
            if (sp.contains(key)) {
                return sp.getLong(key, 0L)
            }
            return null
        }
        val valStr = fallbackPrefs?.get(key, null) ?: return null
        return valStr.toLongOrNull()
    }

    actual fun setLong(key: String, value: Long) {
        val sp = sharedPrefs
        if (sp != null) {
            sp.edit().putLong(key, value).apply()
        } else {
            fallbackPrefs?.put(key, value.toString())
        }
    }
}


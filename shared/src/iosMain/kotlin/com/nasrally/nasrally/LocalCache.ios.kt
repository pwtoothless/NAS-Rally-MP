package com.nasrally.nasrally

import platform.Foundation.NSUserDefaults

actual object LocalCache {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getString(key: String): String? {
        return defaults.stringForKey(key)
    }

    actual fun setString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    actual fun getLong(key: String): Long? {
        if (defaults.objectForKey(key) == null) return null
        return defaults.integerForKey(key)
    }

    actual fun setLong(key: String, value: Long) {
        defaults.setInteger(value, forKey = key)
    }
}

package com.nasrally.nasrally

import kotlinx.browser.localStorage

actual object LocalCache {
    actual fun getString(key: String): String? {
        return localStorage.getItem(key)
    }

    actual fun setString(key: String, value: String) {
        localStorage.setItem(key, value)
    }

    actual fun getLong(key: String): Long? {
        val valStr = localStorage.getItem(key) ?: return null
        return valStr.toLongOrNull()
    }

    actual fun setLong(key: String, value: Long) {
        localStorage.setItem(key, value.toString())
    }
}

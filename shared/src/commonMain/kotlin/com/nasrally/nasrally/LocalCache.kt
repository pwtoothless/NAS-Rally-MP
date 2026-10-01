package com.nasrally.nasrally

expect object LocalCache {
    fun getString(key: String): String?
    fun setString(key: String, value: String)
    fun getLong(key: String): Long?
    fun setLong(key: String, value: Long)
}

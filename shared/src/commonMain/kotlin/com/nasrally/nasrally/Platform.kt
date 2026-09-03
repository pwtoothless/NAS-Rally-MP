package com.nasrally.nasrally

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
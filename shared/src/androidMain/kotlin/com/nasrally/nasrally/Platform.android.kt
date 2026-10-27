package com.nasrally.nasrally

import android.os.Build

/**
 * Android platform descriptor implementation.
 */
class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

/**
 * Returns [AndroidPlatform] instance.
 */
actual fun getPlatform(): Platform = AndroidPlatform()
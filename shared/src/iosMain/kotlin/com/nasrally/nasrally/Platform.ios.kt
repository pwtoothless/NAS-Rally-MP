package com.nasrally.nasrally

import platform.UIKit.UIDevice

/**
 * iOS platform descriptor implementation.
 */
class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

/**
 * Returns [IOSPlatform] instance.
 */
actual fun getPlatform(): Platform = IOSPlatform()
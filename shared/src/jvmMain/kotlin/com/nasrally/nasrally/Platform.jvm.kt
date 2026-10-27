package com.nasrally.nasrally

/**
 * JVM Desktop platform descriptor implementation.
 */
class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

/**
 * Returns [JVMPlatform] instance.
 */
actual fun getPlatform(): Platform = JVMPlatform()
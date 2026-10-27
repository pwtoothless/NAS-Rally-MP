package com.nasrally.nasrally

/**
 * Wasm/JS Web platform descriptor implementation.
 */
class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

/**
 * Returns [WasmPlatform] instance.
 */
actual fun getPlatform(): Platform = WasmPlatform()
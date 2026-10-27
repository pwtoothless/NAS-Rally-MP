package com.nasrally.nasrally

/**
 * Helper class providing platform greeting functionality.
 */
class Greeting {
    private val platform = getPlatform()

    /**
     * Generates a platform-specific greeting message.
     *
     * @return Formatted greeting string containing current target platform name.
     */
    fun greet(): String {
        return sayHello(platform.name)
    }
}
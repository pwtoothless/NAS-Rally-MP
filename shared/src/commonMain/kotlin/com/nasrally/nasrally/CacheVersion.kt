package com.nasrally.nasrally

/**
 * Cache version for invalidation strategy.
 * 
 * Increment this version when you want to invalidate all client-side caches on deployment.
 * This is useful when:
 * - Data schema changes
 * - API response format changes
 * - Image formats change
 * - Any breaking change that requires fresh data
 * 
 * The version is included in all cache keys, so changing it effectively busts all caches.
 * 
 * For automated deployments, consider setting this via build config or environment variable.
 */
object CacheVersion {
    // Update this value to invalidate all caches on new deployment
    // Format: "v{major}.{minor}.{patch}" or use build number/timestamp
    const val CURRENT = "v1.0.0"
    
    /**
     * Generates a versioned cache key by prefixing the original key with the cache version.
     * This ensures caches are automatically invalidated when the version changes.
     */
    fun versionedKey(key: String): String {
        return "${CURRENT}_$key"
    }
}
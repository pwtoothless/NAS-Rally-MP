package com.nasrally.nasrally

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class DeviceTokenRow(
    val user_id: String,
    val fcm_token: String,
    val device_type: String
)

object PushNotificationManager {
    private var pendingToken: String? = null
    private var pendingDeviceType: String? = null

    init {
        CoroutineScope(Dispatchers.Default).launch {
            supabase.auth.sessionStatus.collect { status ->
                if (status is SessionStatus.Authenticated) {
                    val token = pendingToken
                    val type = pendingDeviceType
                    if (token != null && type != null) {
                        registerToken(token, type)
                    }
                }
            }
        }
    }

    fun registerToken(token: String, deviceType: String) {
        pendingToken = token
        pendingDeviceType = deviceType

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val user = supabase.auth.currentUserOrNull()
                if (user != null) {
                    val row = DeviceTokenRow(
                        user_id = user.id,
                        fcm_token = token,
                        device_type = deviceType
                    )
                    supabase.from("device_tokens").upsert(row) {
                        onConflict = "user_id, fcm_token"
                    }
                    // Clear pending once successfully registered
                    pendingToken = null
                    pendingDeviceType = null
                }
            } catch (e: Exception) {
                println("Failed to register token: ${e.message}")
            }
        }
    }
}

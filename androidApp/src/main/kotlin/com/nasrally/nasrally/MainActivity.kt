package com.nasrally.nasrally

import android.os.Bundle
import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.messaging.FirebaseMessaging
import com.nasrally.nasrally.PushNotificationManager

/**
 * Main Android entry activity for NASRally.
 *
 * Configures edge-to-edge display layout, requests POST_NOTIFICATIONS permissions on Android 13+ (Tiramisu),
 * fetches the Firebase Cloud Messaging (FCM) token for push notifications, and sets up the root Compose Multiplatform [App] UI.
 */
class MainActivity : ComponentActivity() {

    /**
     * Activity result launcher for runtime notification permission requests.
     * Fetches FCM token upon permission grant.
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            fetchFCMToken()
        }
    }

    /**
     * Initializes edge-to-edge window framing, requests notification permissions, and sets up Compose content.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        askNotificationPermission()

        setContent {
            App()
        }
    }

    /**
     * Prompts for notification permission on Android 13 (API 33) or higher.
     * Immediately fetches FCM token on older Android releases.
     */
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            fetchFCMToken()
        }
    }

    /**
     * Obtains the device's FCM registration token and uploads it to backend via [PushNotificationManager].
     */
    private fun fetchFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                PushNotificationManager.registerToken(token, "android")
            }
        }
    }
}

/**
     * Android Studio Composable preview for the main application root.
     */
@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
package com.nasrally.nasrally

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Firebase Cloud Messaging service handling push notification token refreshes and incoming message payloads.
 *
 * Implements message filtering logic to suppress alerts when a user is actively viewing a chat thread,
 * queues subtle in-app message banners when the app is in the foreground, and applies rate-limiting to system notifications.
 */
class AppFirebaseMessagingService : FirebaseMessagingService() {

    /**
     * Triggered when a new FCM registration token is generated for the device.
     *
     * @param token The updated FCM token.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PushNotificationManager.registerToken(token, "android")
    }

    /**
     * Handles incoming remote push messages.
     *
     * Evaluates message context against [ActiveChatTracker] to suppress notifications for currently active chat threads,
     * routes messages to [NotificationManager] for in-app banner accumulation when in foreground, or enforces rate-limiting
     * for system notification banners when out-of-app.
     *
     * @param message The received [RemoteMessage] payload.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val groupId = message.data["group_id"] ?: ""
        val groupName = message.data["group_name"] ?: message.data["title"] ?: "New Message"
        val senderName = message.data["sender_name"] ?: "Someone"
        val bodyText = message.data["body"] ?: message.data["content"] ?: message.notification?.body ?: ""

        val currentActiveGroup = ActiveChatTracker.currentGroupId.value

        if (groupId.isNotBlank() && groupId == currentActiveGroup) {
            // User is currently viewing this exact chat thread, suppress all alerts
            return
        }

        if (NotificationManager.isAppInForeground) {
            // In-App Handling: Route to NotificationManager to accumulate messages
            // and trigger subtle in-app banner overlay every 10-20 messages
            NotificationManager.onInAppMessageReceived(
                groupId = groupId,
                groupName = groupName,
                senderName = senderName,
                text = bodyText
            )
            return
        }

        // Out-of-App Handling: Check rate limiter (e.g. max 1 notification every 20s)
        if (groupId.isNotBlank() && !NotificationManager.shouldAllowOutOfAppNotification(groupId)) {
            // Rate limit exceeded for out-of-app notifications, suppress system notification
            return
        }
    }
}

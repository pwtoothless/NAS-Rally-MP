package com.nasrally.nasrally

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PushNotificationManager.registerToken(token, "android")
    }

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

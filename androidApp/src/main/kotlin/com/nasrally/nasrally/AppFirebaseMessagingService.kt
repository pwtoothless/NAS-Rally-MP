package com.nasrally.nasrally

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // If the user logs in, we need to register the token. Since the token might refresh
        // before login, the registration logic inside PushNotificationManager will ensure
        // it checks if user is not null.
        PushNotificationManager.registerToken(token, "android")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        val notificationGroupId = message.data["group_id"]
        val currentActiveGroup = ActiveChatTracker.currentGroupId.value
        
        if (notificationGroupId != null && notificationGroupId == currentActiveGroup) {
            // User is currently viewing this chat, suppress the notification
            return
        }
        
        // Let Firebase handle the automatic notification display since it contains a notification payload
        // Alternatively, if the message only contained data, we would build the NotificationCompat here.
    }
}

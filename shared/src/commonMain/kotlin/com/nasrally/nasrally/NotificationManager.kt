package com.nasrally.nasrally

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock

data class InAppBannerData(
    val groupId: String,
    val groupName: String,
    val unreadCount: Int,
    val latestSenderName: String,
    val latestMessageText: String
)

object NotificationManager {
    private const val DEFAULT_COOLDOWN_MILLIS = 20_000L // 20 seconds rate limit
    private const val IN_APP_BANNER_THRESHOLD = 10 // Trigger banner every 10-20 messages

    private val lastNotificationTimestamp = mutableMapOf<String, Long>()
    private val inAppMessageCounts = mutableMapOf<String, Int>()

    var isAppInForeground: Boolean = true

    private val _inAppBannerState = MutableStateFlow<InAppBannerData?>(null)
    val inAppBannerState: StateFlow<InAppBannerData?> = _inAppBannerState.asStateFlow()

    val currentBannerData: InAppBannerData? get() = _inAppBannerState.value

    private var onBannerDataChangedListener: ((InAppBannerData?) -> Unit)? = null

    fun setOnBannerDataChangedListener(listener: ((InAppBannerData?) -> Unit)?) {
        onBannerDataChangedListener = listener
        listener?.invoke(_inAppBannerState.value)
    }

    /**
     * Determines whether an out-of-app notification should be displayed
     * based on the rate-limiting rules (e.g. max 1 notification per group every 20 seconds).
     */
    fun shouldAllowOutOfAppNotification(groupId: String, cooldownMillis: Long = DEFAULT_COOLDOWN_MILLIS): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        val lastTime = lastNotificationTimestamp[groupId] ?: 0L
        val globalLastTime = lastNotificationTimestamp["_global_"] ?: 0L

        if (now - lastTime >= cooldownMillis && now - globalLastTime >= (cooldownMillis / 2)) {
            lastNotificationTimestamp[groupId] = now
            lastNotificationTimestamp["_global_"] = now
            return true
        }
        return false
    }

    /**
     * Called when a message FCM/Realtime payload is received while the user is inside the app.
     * Accumulates messages and triggers a subtle banner overlay every 10-20 messages.
     */
    fun onInAppMessageReceived(
        groupId: String,
        groupName: String,
        senderName: String = "Someone",
        text: String = ""
    ) {
        val currentActiveGroup = ActiveChatTracker.currentGroupId.value
        if (groupId == currentActiveGroup) {
            // User is currently in this active chat thread, no banner needed
            return
        }

        val newCount = (inAppMessageCounts[groupId] ?: 0) + 1
        inAppMessageCounts[groupId] = newCount

        if (newCount >= IN_APP_BANNER_THRESHOLD) {
            val banner = InAppBannerData(
                groupId = groupId,
                groupName = groupName.ifBlank { "Group Chat" },
                unreadCount = newCount,
                latestSenderName = senderName.ifBlank { "Someone" },
                latestMessageText = text
            )
            _inAppBannerState.value = banner
            onBannerDataChangedListener?.invoke(banner)
            // Reset counter after triggering banner
            inAppMessageCounts[groupId] = 0
        }
    }

    fun dismissInAppBanner() {
        _inAppBannerState.value = null
        onBannerDataChangedListener?.invoke(null)
    }

    fun resetGroupMessageCount(groupId: String) {
        inAppMessageCounts[groupId] = 0
        if (_inAppBannerState.value?.groupId == groupId) {
            dismissInAppBanner()
        }
    }
}

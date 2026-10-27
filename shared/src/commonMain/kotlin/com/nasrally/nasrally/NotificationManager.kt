package com.nasrally.nasrally

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock

/**
 * Data payload for in-app floating banner notifications.
 *
 * @property groupId Target group chat ID.
 * @property groupName Display name of the group.
 * @property unreadCount Accumulated unread message count.
 * @property latestSenderName Display name of the last message sender.
 * @property latestMessageText Content preview of the last message.
 */
data class InAppBannerData(
    val groupId: String,
    val groupName: String,
    val unreadCount: Int,
    val latestSenderName: String,
    val latestMessageText: String
)

/**
 * Singleton managing in-app chat banner notifications, rate-limiting out-of-app system alerts, and foreground state tracking.
 */
object NotificationManager {
    private const val DEFAULT_COOLDOWN_MILLIS = 20_000L // 20 seconds rate limit
    private const val IN_APP_BANNER_THRESHOLD = 10 // Trigger banner every 10-20 messages

    private val lastNotificationTimestamp = mutableMapOf<String, Long>()
    private val inAppMessageCounts = mutableMapOf<String, Int>()

    /**
     * Whether the application is currently actively in the foreground.
     */
    var isAppInForeground: Boolean = true

    private val _inAppBannerState = MutableStateFlow<InAppBannerData?>(null)

    /**
     * [StateFlow] emitting current active in-app banner data, or `null` if hidden.
     */
    val inAppBannerState: StateFlow<InAppBannerData?> = _inAppBannerState.asStateFlow()

    /**
     * Retrieves the current banner data payload.
     */
    val currentBannerData: InAppBannerData? get() = _inAppBannerState.value

    private var onBannerDataChangedListener: ((InAppBannerData?) -> Unit)? = null

    /**
     * Registers a listener callback invoked when banner state changes.
     */
    fun setOnBannerDataChangedListener(listener: ((InAppBannerData?) -> Unit)?) {
        onBannerDataChangedListener = listener
        listener?.invoke(_inAppBannerState.value)
    }

    /**
     * Determines whether an out-of-app notification should be displayed
     * based on the rate-limiting rules (e.g. max 1 notification per group every 20 seconds).
     *
     * @param groupId Target group ID.
     * @param cooldownMillis Minimum required time gap between notifications.
     * @return `true` if allowed; `false` if rate-limited.
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
     * Called when a message payload is received while the user is inside the app.
     * Accumulates messages and triggers a subtle banner overlay every 10-20 messages.
     *
     * @param groupId Group chat ID.
     * @param groupName Group chat display name.
     * @param senderName Sender's name.
     * @param text Message body text.
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

    /**
     * Dismisses the active in-app banner overlay.
     */
    fun dismissInAppBanner() {
        _inAppBannerState.value = null
        onBannerDataChangedListener?.invoke(null)
    }

    /**
     * Resets accumulated unread message count for [groupId] and dismisses matching banners.
     */
    fun resetGroupMessageCount(groupId: String) {
        inAppMessageCounts[groupId] = 0
        if (_inAppBannerState.value?.groupId == groupId) {
            dismissInAppBanner()
        }
    }
}

package com.nasrally.nasrally

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thread-safe singleton tracker maintaining the currently focused group chat ID.
 *
 * Used across push notification services (e.g. Firebase Messaging / APNs) to suppress in-app and out-of-app notification banners when the user is actively viewing a chat.
 */
object ActiveChatTracker {
    private val _currentGroupId = MutableStateFlow<String?>(null)

    /**
     * [StateFlow] emitting the ID of the group chat thread currently being viewed by the user, or `null` if none.
     */
    val currentGroupId: StateFlow<String?> = _currentGroupId.asStateFlow()

    /**
     * Updates the currently active group chat ID.
     *
     * @param groupId The active group ID, or `null` when leaving a chat screen.
     */
    fun setActiveGroup(groupId: String?) {
        _currentGroupId.value = groupId
    }
}

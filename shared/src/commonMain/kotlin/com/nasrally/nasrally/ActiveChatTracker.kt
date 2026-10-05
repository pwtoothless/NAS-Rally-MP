package com.nasrally.nasrally

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ActiveChatTracker {
    private val _currentGroupId = MutableStateFlow<String?>(null)
    val currentGroupId: StateFlow<String?> = _currentGroupId.asStateFlow()

    fun setActiveGroup(groupId: String?) {
        _currentGroupId.value = groupId
    }
}

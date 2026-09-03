package com.nasrally.nasrally

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ChatViewModel(private val groupId: String, private val scope: CoroutineScope) {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val pageSize = 50
    private var currentPage = 0
    private var canLoadMore = true

    init {
        setupRealtimeSubscription()
    }

    private fun setupRealtimeSubscription() {
        val channel = supabase.channel("chat:$groupId")
        val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(
            schema = "public"
        ) {
            table = "messages"
            filter("group_id", FilterOperator.EQ, groupId)
        }

        scope.launch(Dispatchers.Default) {
            changeFlow.collect { action ->
                val newMessage = action.decodeRecord<Message>()
                val current = _messages.value.toMutableList()
                if (current.none { it.id == newMessage.id }) {
                    current.add(0, newMessage)
                    _messages.value = current
                }
            }
        }
        scope.launch { channel.subscribe() }
    }

    suspend fun loadMessages(isRefresh: Boolean = false) {
        if (_isLoading.value || (!canLoadMore && !isRefresh)) return
        _isLoading.value = true
        if (isRefresh) { currentPage = 0; canLoadMore = true }

        val from = currentPage * pageSize
        val to = from + pageSize - 1

        try {
            val fetched: List<Message> = supabase.from("messages")
                .select {
                    filter { eq("group_id", groupId) }
                    order("created_at", order = Order.DESCENDING)
                    range(from.toLong(), to.toLong())
                }.decodeList()

            if (fetched.size < pageSize) canLoadMore = false

            val current = if (isRefresh) mutableListOf() else _messages.value.toMutableList()
            current.addAll(fetched)
            _messages.value = current
            currentPage++
        } catch (e: Exception) {
            println("Fetch error: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun sendMessage(content: String, senderId: String) {
        val newMessage = Message(
            id = Uuid.random().toString(),
            groupId = groupId,
            senderId = senderId,
            content = content,
            createdAt = Clock.System.now().toString()
        )

        val current = _messages.value.toMutableList()
        current.add(0, newMessage)
        _messages.value = current

        try {
            supabase.from("messages").insert(newMessage)
        } catch (e: Exception) {
            _messages.value = _messages.value.filter { it.id != newMessage.id }
        }
    }
}
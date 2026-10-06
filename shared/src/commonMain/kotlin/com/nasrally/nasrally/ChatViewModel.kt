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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class ChatViewModel(val groupId: String, private val scope: CoroutineScope) {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _groupMembers = MutableStateFlow<List<PersonInfo>>(emptyList())
    val groupMembers: StateFlow<List<PersonInfo>> = _groupMembers.asStateFlow()

    private val _profilesMap = MutableStateFlow<Map<String, PersonInfo>>(emptyMap())
    val profilesMap: StateFlow<Map<String, PersonInfo>> = _profilesMap.asStateFlow()

    private val _readReceipts = MutableStateFlow<Map<String, List<ReadReceipt>>>(emptyMap())
    val readReceipts: StateFlow<Map<String, List<ReadReceipt>>> = _readReceipts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val pageSize = 50
    private var currentPage = 0
    private var canLoadMore = true
    private val cacheKey = "chat_messages_$groupId"

    init {
        loadCachedMessages()
        setupRealtimeSubscription()
        scope.launch {
            loadGroupMembers()
        }
    }

    private fun loadCachedMessages() {
        val cachedJson = LocalCache.getString(cacheKey) ?: return
        try {
            val cachedList = Json.decodeFromString<List<Message>>(cachedJson)
            if (cachedList.isNotEmpty()) {
                _messages.value = cachedList.take(50)
            }
        } catch (e: Exception) {
            println("Error decoding cached messages: ${e.message}")
        }
    }

    private fun saveCachedMessages(list: List<Message>) {
        try {
            val top50 = list.take(50)
            val jsonStr = Json.encodeToString(top50)
            LocalCache.setString(cacheKey, jsonStr)
        } catch (e: Exception) {
            println("Error saving cached messages: ${e.message}")
        }
    }

    suspend fun loadGroupMembers() {
        try {
            val memberRows: List<GroupMemberRow> = supabase.from("group_members")
                .select {
                    filter { eq("group_id", groupId) }
                }.decodeList()

            val userIds = memberRows.map { it.userId }.distinct()
            if (userIds.isNotEmpty()) {
                val profileRows: List<SupabasePersonRow> = supabase.from("profiles")
                    .select {
                        filter { isIn("id", userIds) }
                    }.decodeList()

                val membersList = profileRows.map { it.toPersonInfo() }
                _groupMembers.value = membersList
                _profilesMap.value = membersList.associateBy { it.id }
            }
        } catch (e: Exception) {
            println("Error loading group members: ${e.message}")
        }
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
                    saveCachedMessages(current)
                    ensureProfileLoaded(newMessage.senderId)
                }
            }
        }
        scope.launch { channel.subscribe() }

        val readChannel = supabase.channel("read_receipts:$groupId")
        val receiptsFlow = readChannel.postgresChangeFlow<PostgresAction.Insert>(
            schema = "public"
        ) {
            table = "read_receipts"
        }

        scope.launch(Dispatchers.Default) {
            receiptsFlow.collect { action ->
                try {
                    val receipt = action.decodeRecord<ReadReceipt>()
                    val currentMap = _readReceipts.value.toMutableMap()
                    val list = currentMap[receipt.messageId]?.toMutableList() ?: mutableListOf()
                    if (list.none { it.userId == receipt.userId }) {
                        list.add(receipt)
                        currentMap[receipt.messageId] = list
                        _readReceipts.value = currentMap
                    }
                } catch (e: Exception) {
                    println("Error parsing read receipt realtime event: ${e.message}")
                }
            }
        }
        scope.launch { readChannel.subscribe() }
    }

    private fun ensureProfileLoaded(senderId: String) {
        if (_profilesMap.value.containsKey(senderId)) return
        scope.launch {
            try {
                val rows: List<SupabasePersonRow> = supabase.from("profiles")
                    .select {
                        filter { eq("id", senderId) }
                    }.decodeList()
                val person = rows.firstOrNull()?.toPersonInfo()
                if (person != null) {
                    val updated = _profilesMap.value.toMutableMap()
                    updated[senderId] = person
                    _profilesMap.value = updated
                }
            } catch (e: Exception) {
                println("Error fetching profile for $senderId: ${e.message}")
            }
        }
    }

    suspend fun loadMessages(isRefresh: Boolean = false, currentUserId: String? = null) {
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
            val existingIds = current.map { it.id }.toSet()
            val newUnique = fetched.filter { it.id !in existingIds }
            current.addAll(newUnique)

            _messages.value = current
            saveCachedMessages(current)
            currentPage++

            val messageIds = current.map { it.id }
            if (messageIds.isNotEmpty()) {
                loadReadReceipts(messageIds)
            }

            // Ensure profiles are loaded for senders
            val senderIds = current.map { it.senderId }.distinct()
            val missingSenderIds = senderIds.filter { !profilesMap.value.containsKey(it) }
            if (missingSenderIds.isNotEmpty()) {
                val missingRows: List<SupabasePersonRow> = supabase.from("profiles")
                    .select {
                        filter { isIn("id", missingSenderIds) }
                    }.decodeList()
                val newProfiles = missingRows.map { it.toPersonInfo() }
                val updatedMap = _profilesMap.value.toMutableMap()
                newProfiles.forEach { updatedMap[it.id] = it }
                _profilesMap.value = updatedMap
            }

            if (currentUserId != null) {
                markMessagesAsRead(currentUserId)
            }
        } catch (e: Exception) {
            println("Fetch error: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun loadReadReceipts(messageIds: List<String>) {
        if (messageIds.isEmpty()) return
        try {
            val receipts: List<ReadReceipt> = supabase.from("read_receipts")
                .select {
                    filter { isIn("message_id", messageIds) }
                }.decodeList()

            val map = receipts.groupBy { it.messageId }
            val currentMap = _readReceipts.value.toMutableMap()
            map.forEach { (msgId, list) ->
                currentMap[msgId] = list
            }
            _readReceipts.value = currentMap
        } catch (e: Exception) {
            println("Error fetching read receipts: ${e.message}")
        }
    }

    suspend fun markMessagesAsRead(currentUserId: String) {
        val unreadMessageIds = _messages.value.filter { msg ->
            val existing = _readReceipts.value[msg.id] ?: emptyList()
            existing.none { it.userId == currentUserId }
        }.map { it.id }

        if (unreadMessageIds.isEmpty()) return

        val now = Clock.System.now().toString()
        val newReceipts = unreadMessageIds.map { ReadReceiptInsert(it, currentUserId, now) }

        val currentMap = _readReceipts.value.toMutableMap()
        for (msgId in unreadMessageIds) {
            val list = currentMap[msgId]?.toMutableList() ?: mutableListOf()
            if (list.none { it.userId == currentUserId }) {
                list.add(ReadReceipt(msgId, currentUserId, now))
                currentMap[msgId] = list
            }
        }
        _readReceipts.value = currentMap

        try {
            supabase.from("read_receipts").upsert(newReceipts)
        } catch (e: Exception) {
            println("Error marking messages as read: ${e.message}")
        }
    }

    suspend fun sendMessage(content: String, senderId: String) {
        val now = Clock.System.now().toString()
        val newMessage = Message(
            id = Uuid.random().toString(),
            groupId = groupId,
            senderId = senderId,
            content = content,
            createdAt = now
        )

        val current = _messages.value.toMutableList()
        current.add(0, newMessage)
        _messages.value = current
        saveCachedMessages(current)

        // Sender automatically reads their own message
        val currentReceipts = _readReceipts.value.toMutableMap()
        val receiptList = mutableListOf(ReadReceipt(newMessage.id, senderId, now))
        currentReceipts[newMessage.id] = receiptList
        _readReceipts.value = currentReceipts

        try {
            supabase.from("messages").insert(newMessage)
            supabase.from("read_receipts").upsert(ReadReceiptInsert(newMessage.id, senderId, now))
        } catch (e: Exception) {
            _messages.value = _messages.value.filter { it.id != newMessage.id }
            saveCachedMessages(_messages.value)
        }
    }
}

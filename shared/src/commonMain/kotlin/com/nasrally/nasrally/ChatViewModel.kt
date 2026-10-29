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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * ViewModel managing chat state, pagination, read receipts, member profiles, and Supabase Realtime message streaming.
 *
 * @property groupId The unique ID of the group chat thread managed by this view model.
 * @property scope Coroutine scope bound to the life of the view model or UI component.
 */
@OptIn(ExperimentalUuidApi::class)
class ChatViewModel(val groupId: String, private val scope: CoroutineScope) {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())

    /**
     * [StateFlow] emitting the list of chat messages, sorted newest first.
     */
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _groupMembers = MutableStateFlow<List<PersonInfo>>(emptyList())

    /**
     * [StateFlow] emitting members enrolled in this chat group.
     */
    val groupMembers: StateFlow<List<PersonInfo>> = _groupMembers.asStateFlow()

    private val _profilesMap = MutableStateFlow<Map<String, PersonInfo>>(emptyMap())

    /**
     * [StateFlow] mapping user IDs to their cached [PersonInfo] profile records.
     */
    val profilesMap: StateFlow<Map<String, PersonInfo>> = _profilesMap.asStateFlow()

    private val _readReceipts = MutableStateFlow<Map<String, List<ReadReceipt>>>(emptyMap())

    /**
     * [StateFlow] mapping message IDs to their list of read receipt records.
     */
    val readReceipts: StateFlow<Map<String, List<ReadReceipt>>> = _readReceipts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)

    /**
     * [StateFlow] indicating whether a message history fetch is in progress.
     */
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

    /**
     * Loads locally cached messages from [LocalCache].
     */
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

    /**
     * Persists top 50 messages to [LocalCache].
     */
    private fun saveCachedMessages(list: List<Message>) {
        try {
            val top50 = list.take(50)
            val jsonStr = Json.encodeToString(top50)
            LocalCache.setString(cacheKey, jsonStr)
        } catch (e: Exception) {
            println("Error saving cached messages: ${e.message}")
        }
    }

    /**
     * Fetches group members and populates user profile records.
     */
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
                _profilesMap.update { current ->
                    current + membersList.associateBy { it.id }
                }
            }
        } catch (e: Exception) {
            println("Error loading group members: ${e.message}")
        }
    }

    /**
     * Connects Supabase Realtime channels for live Postgres insertions into `messages` and `read_receipts` tables.
     */
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
                _messages.update { current ->
                    if (current.none { it.id == newMessage.id }) {
                        val updated = current.toMutableList()
                        updated.add(0, newMessage)
                        saveCachedMessages(updated)
                        updated
                    } else current
                }
                ensureProfileLoaded(newMessage.senderId)
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
                    _readReceipts.update { currentMap ->
                        val updatedMap = currentMap.toMutableMap()
                        val list = updatedMap[receipt.messageId]?.toMutableList() ?: mutableListOf()
                        if (list.none { it.userId == receipt.userId }) {
                            list.add(receipt)
                            updatedMap[receipt.messageId] = list
                        }
                        updatedMap
                    }
                } catch (e: Exception) {
                    println("Error parsing read receipt realtime event: ${e.message}")
                }
            }
        }
        scope.launch { readChannel.subscribe() }
    }

    /**
     * Ensures sender profile information is cached in [_profilesMap].
     */
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
                    _profilesMap.update { current -> current + (senderId to person) }
                }
            } catch (e: Exception) {
                println("Error fetching profile for $senderId: ${e.message}")
            }
        }
    }

    /**
     * Fetches message history from Supabase with pagination support.
     *
     * @param isRefresh If `true`, resets pagination state and reloads from first page.
     * @param currentUserId Optional ID of the currently logged-in user to mark messages as read.
     */
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

            var updatedList: List<Message> = emptyList()
            _messages.update { current ->
                val list = if (isRefresh) mutableListOf() else current.toMutableList()
                val existingIds = list.map { it.id }.toSet()
                val newUnique = fetched.filter { it.id !in existingIds }
                list.addAll(newUnique)
                updatedList = list
                list
            }

            saveCachedMessages(updatedList)
            currentPage++

            val messageIds = updatedList.map { it.id }
            if (messageIds.isNotEmpty()) {
                loadReadReceipts(messageIds)
            }

            // Ensure profiles are loaded for senders
            val senderIds = updatedList.map { it.senderId }.distinct()
            val missingSenderIds = senderIds.filter { !profilesMap.value.containsKey(it) }
            if (missingSenderIds.isNotEmpty()) {
                val missingRows: List<SupabasePersonRow> = supabase.from("profiles")
                    .select {
                        filter { isIn("id", missingSenderIds) }
                    }.decodeList()
                val newProfiles = missingRows.map { it.toPersonInfo() }
                _profilesMap.update { current ->
                    current + newProfiles.associateBy { it.id }
                }
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

    /**
     * Loads read receipt records for the specified list of message IDs.
     */
    suspend fun loadReadReceipts(messageIds: List<String>) {
        if (messageIds.isEmpty()) return
        try {
            val receipts: List<ReadReceipt> = supabase.from("read_receipts")
                .select {
                    filter { isIn("message_id", messageIds) }
                }.decodeList()

            val map = receipts.groupBy { it.messageId }
            _readReceipts.update { currentMap ->
                val updated = currentMap.toMutableMap()
                map.forEach { (msgId, list) ->
                    updated[msgId] = list
                }
                updated
            }
        } catch (e: Exception) {
            println("Error fetching read receipts: ${e.message}")
        }
    }

    /**
     * Inserts read receipts for unread messages on behalf of [currentUserId].
     */
    suspend fun markMessagesAsRead(currentUserId: String) {
        val unreadMessageIds = _messages.value.filter { msg ->
            val existing = _readReceipts.value[msg.id] ?: emptyList()
            existing.none { it.userId == currentUserId }
        }.map { it.id }

        if (unreadMessageIds.isEmpty()) return

        val now = Clock.System.now().toString()
        val newReceipts = unreadMessageIds.map { ReadReceiptInsert(it, currentUserId, now) }

        _readReceipts.update { currentMap ->
            val updated = currentMap.toMutableMap()
            for (msgId in unreadMessageIds) {
                val list = updated[msgId]?.toMutableList() ?: mutableListOf()
                if (list.none { it.userId == currentUserId }) {
                    list.add(ReadReceipt(msgId, currentUserId, now))
                    updated[msgId] = list
                }
            }
            updated
        }

        try {
            supabase.from("read_receipts").upsert(newReceipts)
        } catch (e: Exception) {
            println("Error marking messages as read: ${e.message}")
        }
    }

    /**
     * Sends a new chat message to Supabase.
     *
     * @param content Text body of the message.
     * @param senderId User ID of the message sender.
     */
    suspend fun sendMessage(content: String, senderId: String) {
        val now = Clock.System.now().toString()
        val newMessage = Message(
            id = Uuid.random().toString(),
            groupId = groupId,
            senderId = senderId,
            content = content,
            createdAt = now
        )

        _messages.update { current ->
            val updated = current.toMutableList()
            updated.add(0, newMessage)
            saveCachedMessages(updated)
            updated
        }

        // Sender automatically reads their own message
        _readReceipts.update { currentMap ->
            val updated = currentMap.toMutableMap()
            val receiptList = mutableListOf(ReadReceipt(newMessage.id, senderId, now))
            updated[newMessage.id] = receiptList
            updated
        }

        try {
            supabase.from("messages").insert(newMessage)
            supabase.from("read_receipts").upsert(ReadReceiptInsert(newMessage.id, senderId, now))
        } catch (_: Exception) {
            _messages.update { current ->
                val updated = current.filter { it.id != newMessage.id }
                saveCachedMessages(updated)
                updated
            }
        }
    }
}

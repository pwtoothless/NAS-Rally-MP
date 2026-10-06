package com.nasrally.nasrally.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nasrally.nasrally.ActiveChatTracker
import com.nasrally.nasrally.ChatViewModel
import com.nasrally.nasrally.GroupRow
import com.nasrally.nasrally.Message
import com.nasrally.nasrally.PersonInfo
import com.nasrally.nasrally.ReadReceipt
import com.nasrally.nasrally.supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch

@Composable
fun ChatView(person: PersonInfo) {
    var availableGroups by remember { mutableStateOf<List<GroupRow>>(emptyList()) }
    var selectedGroup by remember { mutableStateOf<GroupRow?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(person.id) {
        if (!person.isTestUser) {
            isLoading = true
            try {
                availableGroups = supabase.from("groups")
                    .select(Columns.raw("id, name, group_members!inner(user_id)")) {
                        filter {
                            eq("group_members.user_id", person.id)
                        }
                    }
                    .decodeList<GroupRow>()
            } catch (e: Exception) {
                println("Error loading groups: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedGroup != null) {
            val group = selectedGroup!!
            val scope = rememberCoroutineScope()
            val viewModel = remember(group.id) { ChatViewModel(group.id, scope) }

            MessageThreadView(
                groupName = group.name ?: "Chat",
                person = person,
                viewModel = viewModel,
                onBack = { selectedGroup = null }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Chat",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (availableGroups.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No active group chats found.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn {
                        items(availableGroups) { group ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedGroup = group },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CachedRallyLogoImage(
                                        rallyName = group.name ?: "",
                                        contentDescription = group.name,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape),
                                        error = {
                                            Icon(
                                                imageVector = Icons.Default.DirectionsCar,
                                                contentDescription = null,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    )

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Text(
                                        text = group.name ?: "Group Chat",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageThreadView(
    groupName: String,
    person: PersonInfo,
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val groupMembers by viewModel.groupMembers.collectAsState()
    val profilesMap by viewModel.profilesMap.collectAsState()
    val readReceipts by viewModel.readReceipts.collectAsState()

    var messageInput by remember { mutableStateOf("") }
    var selectedProfileForSheet by remember { mutableStateOf<PersonInfo?>(null) }
    var selectedMessageForReceipts by remember { mutableStateOf<Message?>(null) }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.loadMessages(isRefresh = true, currentUserId = person.id)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            viewModel.markMessagesAsRead(person.id)
        }
    }

    LaunchedEffect(messages.firstOrNull()?.id, messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    DisposableEffect(groupName) {
        ActiveChatTracker.setActiveGroup(viewModel.groupId)
        onDispose {
            ActiveChatTracker.setActiveGroup(null)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    CachedRallyLogoImage(
                        rallyName = groupName,
                        contentDescription = groupName,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        error = {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = groupName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    val senderProfile = profilesMap[message.senderId]
                    val msgReceipts = readReceipts[message.id] ?: emptyList()
                    val totalMembers = groupMembers.size.coerceAtLeast(1)

                    MessageBubble(
                        message = message,
                        isCurrentUser = message.senderId == person.id,
                        senderProfile = senderProfile,
                        msgReceipts = msgReceipts,
                        totalMembers = totalMembers,
                        onAvatarClick = { profile ->
                            selectedProfileForSheet = profile
                        },
                        onReceiptClick = {
                            selectedMessageForReceipts = message
                        }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    placeholder = { Text("Message") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        val contentToSend = messageInput.trim()
                        if (contentToSend.isNotBlank()) {
                            messageInput = ""
                            scope.launch {
                                viewModel.sendMessage(contentToSend, person.id)
                            }
                        }
                    },
                    enabled = messageInput.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (messageInput.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        }

        // Selected Profile Dialog Sheet
        selectedProfileForSheet?.let { profile ->
            UserProfileSheetDialog(
                user = profile,
                onDismiss = { selectedProfileForSheet = null }
            )
        }

        // Read Receipts Dialog Sheet
        selectedMessageForReceipts?.let { message ->
            val receipts = readReceipts[message.id] ?: emptyList()
            ReadReceiptsDialog(
                message = message,
                readReceipts = receipts,
                groupMembers = groupMembers,
                profilesMap = profilesMap,
                onDismiss = { selectedMessageForReceipts = null },
                onSelectUser = { user ->
                    selectedProfileForSheet = user
                }
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean,
    senderProfile: PersonInfo? = null,
    msgReceipts: List<ReadReceipt> = emptyList(),
    totalMembers: Int = 1,
    onAvatarClick: (PersonInfo) -> Unit = {},
    onReceiptClick: () -> Unit = {}
) {
    val firstName = remember(senderProfile?.name) {
        val raw = senderProfile?.name?.trim()
        if (raw.isNullOrBlank()) "User" else raw.split(" ").firstOrNull() ?: raw
    }

    val readCount = remember(msgReceipts) {
        msgReceipts.map { it.userId }.toSet().size
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Show avatar + first name ONLY for other users' messages
        if (!isCurrentUser) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .clickable {
                        onAvatarClick(senderProfile ?: PersonInfo(id = message.senderId, name = "User"))
                    }
            ) {
                CachedProfileImage(
                    userId = message.senderId,
                    contentDescription = firstName,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape),
                    error = {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = firstName,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Message Bubble
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message.createdAt.take(16).replace("T", " "),
                        fontSize = 10.sp,
                        color = (if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Read Receipt Indicator: e.g. "3/5"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = (if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary).copy(alpha = 0.2f),
                        modifier = Modifier.clickable { onReceiptClick() }
                    ) {
                        Text(
                            text = "$readCount/$totalMembers",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReadReceiptsDialog(
    message: Message,
    readReceipts: List<ReadReceipt>,
    groupMembers: List<PersonInfo>,
    profilesMap: Map<String, PersonInfo>,
    onDismiss: () -> Unit,
    onSelectUser: (PersonInfo) -> Unit
) {
    val readUserIds = remember(readReceipts) { readReceipts.map { it.userId }.toSet() }
    val readMap = remember(readReceipts) { readReceipts.associateBy { it.userId } }

    val readMembers = remember(groupMembers, readUserIds) {
        groupMembers.filter { it.id in readUserIds }
    }
    val unreadMembers = remember(groupMembers, readUserIds) {
        groupMembers.filter { it.id !in readUserIds }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Message Read Status",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "${readMembers.size} of ${groupMembers.size} members read this message",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    if (readMembers.isNotEmpty()) {
                        item {
                            Text(
                                text = "READ BY (${readMembers.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                        items(readMembers) { member ->
                            val receipt = readMap[member.id]
                            val timeStr = receipt?.readAt?.take(16)?.replace("T", " ") ?: "Read"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable {
                                        onDismiss()
                                        onSelectUser(member)
                                    }
                            ) {
                                CachedProfileImage(
                                    userId = member.id,
                                    contentDescription = member.name,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = member.name, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                    Text(text = timeStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Read",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (unreadMembers.isNotEmpty()) {
                        item {
                            Text(
                                text = "UNREAD (${unreadMembers.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                            )
                        }
                        items(unreadMembers) { member ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable {
                                        onDismiss()
                                        onSelectUser(member)
                                    }
                            ) {
                                CachedProfileImage(
                                    userId = member.id,
                                    contentDescription = member.name,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = member.name,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Unread",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun UserProfileSheetDialog(
    user: PersonInfo,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CachedProfileImage(
                    userId = user.id,
                    contentDescription = user.name,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape),
                    error = {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(50.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = user.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)

                if (user.instaHandle.isNotBlank()) {
                    Text(
                        text = "@${user.instaHandle}",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }

                if (user.carModel.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = user.carModel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                if (user.bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = user.bio,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}

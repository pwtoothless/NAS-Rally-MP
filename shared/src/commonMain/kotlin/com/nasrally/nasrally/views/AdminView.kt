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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CarRental
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nasrally.nasrally.AdminProfile
import com.nasrally.nasrally.DatabaseRally
import com.nasrally.nasrally.GroupInsertRow
import com.nasrally.nasrally.GroupMemberInsert
import com.nasrally.nasrally.GroupRow
import com.nasrally.nasrally.GroupUpdateRow
import com.nasrally.nasrally.PersonInfo
import com.nasrally.nasrally.RallyInsertRow
import com.nasrally.nasrally.RallyParticipantInsert
import com.nasrally.nasrally.RallyParticipantRow
import com.nasrally.nasrally.RallyRequestRow
import com.nasrally.nasrally.RallyUpdateRow
import com.nasrally.nasrally.SignedWaiverRow
import com.nasrally.nasrally.Waiver
import com.nasrally.nasrally.WaiverInsertRow
import com.nasrally.nasrally.fetchUserIDImageData
import com.nasrally.nasrally.getProfileImageURL
import com.nasrally.nasrally.getRallyImageURL
import com.nasrally.nasrally.supabase
import io.github.jan.supabase.postgrest.from
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.launch

@OptIn(ExperimentalUuidApi::class)
@Composable
fun AdminView(person: PersonInfo) {
    var selectedTab by remember { mutableStateOf(0) } // 0=Users, 1=Rallies, 2=Roles, 3=Approvals
    var users by remember { mutableStateOf<List<AdminProfile>>(emptyList()) }
    var rallies by remember { mutableStateOf<List<DatabaseRally>>(emptyList()) }
    var participants by remember { mutableStateOf<List<RallyParticipantRow>>(emptyList()) }
    var requests by remember { mutableStateOf<List<RallyRequestRow>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    var selectedRally by remember { mutableStateOf<DatabaseRally?>(null) }
    var editingRally by remember { mutableStateOf<DatabaseRally?>(null) }
    var showCreateRallyDialog by remember { mutableStateOf(false) }
    var selectedUserForRole by remember { mutableStateOf<AdminProfile?>(null) }
    var selectedUserForApproval by remember { mutableStateOf<AdminProfile?>(null) }
    var selectedUserForProfile by remember { mutableStateOf<AdminProfile?>(null) }

    val scope = rememberCoroutineScope()

    val loadData: () -> Unit = {
        scope.launch {
            isLoading = true
            try {
                users = supabase.from("profiles").select().decodeList<AdminProfile>()
                rallies = supabase.from("rallies").select().decodeList<DatabaseRally>()
                participants = supabase.from("rally_participants").select().decodeList<RallyParticipantRow>()
                requests = supabase.from("rally_requests").select().decodeList<RallyRequestRow>()
            } catch (e: Exception) {
                println("Error loading admin data: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    val deleteRally: (DatabaseRally) -> Unit = { rally ->
        scope.launch {
            isLoading = true
            try {
                // 1. Delete signed waivers for this rally's waivers
                val waiverRows: List<Waiver> = try {
                    supabase.from("waivers").select { filter { eq("rally_id", rally.id) } }.decodeList()
                } catch (e: Exception) { emptyList() }

                for (w in waiverRows) {
                    try { supabase.from("signed_waivers").delete { filter { eq("waiver_id", w.id) } } } catch (_: Exception) {}
                }

                // 2. Delete waivers, messages, group members, requests, participants, rallies, groups
                try { supabase.from("waivers").delete { filter { eq("rally_id", rally.id) } } } catch (_: Exception) {}
                try { supabase.from("messages").delete { filter { eq("group_id", rally.id) } } } catch (_: Exception) {}
                try { supabase.from("group_members").delete { filter { eq("group_id", rally.id) } } } catch (_: Exception) {}
                try { supabase.from("rally_requests").delete { filter { eq("rally_id", rally.id) } } } catch (_: Exception) {}
                try { supabase.from("rally_participants").delete { filter { eq("rally_id", rally.id) } } } catch (_: Exception) {}
                try { supabase.from("rallies").delete { filter { eq("id", rally.id) } } } catch (_: Exception) {}
                try { supabase.from("groups").delete { filter { eq("id", rally.id) } } } catch (_: Exception) {}

                loadData()
            } catch (e: Exception) {
                println("Error deleting rally: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Admin Dashboard",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Users") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Rallies") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Roles") })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Approvals") })
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> UsersTabView(
                        users = users,
                        onSelectUser = { selectedUserForProfile = it }
                    )
                    1 -> RalliesTabView(
                        rallies = rallies,
                        participants = participants,
                        onSelectRally = { selectedRally = it },
                        onEditRally = { editingRally = it },
                        onDeleteRally = deleteRally,
                        onCreateRally = { showCreateRallyDialog = true }
                    )
                    2 -> RolesTabView(
                        users = users,
                        onSelectUser = { selectedUserForRole = it }
                    )
                    3 -> ApprovalsTabView(
                        requests = requests,
                        users = users,
                        onSelectUser = { selectedUserForApproval = it }
                    )
                }
            }
        }
    }

    if (showCreateRallyDialog) {
        CreateRallyDialog(
            adminPerson = person,
            onCreated = {
                showCreateRallyDialog = false
                loadData()
            },
            onDismiss = { showCreateRallyDialog = false }
        )
    }

    editingRally?.let { rally ->
        EditRallyDialog(
            rally = rally,
            onUpdated = {
                editingRally = null
                loadData()
            },
            onDismiss = { editingRally = null }
        )
    }

    selectedRally?.let { rally ->
        RallyAdminDialog(
            rally = rally,
            allUsers = users,
            allParticipants = participants,
            onUpdate = loadData,
            onEdit = {
                selectedRally = null
                editingRally = rally
            },
            onDismiss = { selectedRally = null }
        )
    }

    selectedUserForRole?.let { user ->
        RoleEditorDialog(
            user = user,
            onUpdate = loadData,
            onDismiss = { selectedUserForRole = null }
        )
    }

    selectedUserForApproval?.let { user ->
        ApprovalAdminDialog(
            user = user,
            userRequests = requests.filter { it.userId == user.id },
            rallies = rallies,
            onUpdate = loadData,
            onViewProfile = { selectedUserForProfile = user },
            onDismiss = { selectedUserForApproval = null }
        )
    }

    selectedUserForProfile?.let { user ->
        UserProfileDetailDialog(
            user = user,
            onDismiss = { selectedUserForProfile = null }
        )
    }
}

@Composable
private fun UsersTabView(
    users: List<AdminProfile>,
    onSelectUser: (AdminProfile) -> Unit
) {
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(users) { user ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSelectUser(user) },
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
                    UserAvatar(
                        userId = user.id,
                        userName = user.name,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (user.bio.isNotBlank()) {
                            Text(
                                text = user.bio,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (user.privligeLevel == "Admin") Color(0xFFFFEBEE) else Color(0xFFE3F2FD)
                    ) {
                        Text(
                            text = user.privligeLevel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (user.privligeLevel == "Admin") Color(0xFFC62828) else Color(0xFF1565C0)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RalliesTabView(
    rallies: List<DatabaseRally>,
    participants: List<RallyParticipantRow>,
    onSelectRally: (DatabaseRally) -> Unit,
    onEditRally: (DatabaseRally) -> Unit,
    onDeleteRally: (DatabaseRally) -> Unit,
    onCreateRally: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Events (${rallies.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Button(onClick = onCreateRally) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Rally", fontWeight = FontWeight.Bold)
            }
        }

        if (rallies.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CarRental,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No rallies created yet.", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onCreateRally) {
                        Text("Create First Rally")
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                items(rallies) { rally ->
                    val attendeeCount = participants.count { it.rallyId == rally.id }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelectRally(rally) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CarRental,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = rally.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    text = "$attendeeCount participant${if (attendeeCount == 1) "" else "s"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(onClick = { onEditRally(rally) }) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                            }

                            IconButton(onClick = { onDeleteRally(rally) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RolesTabView(
    users: List<AdminProfile>,
    onSelectUser: (AdminProfile) -> Unit
) {
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(users) { user ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSelectUser(user) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = "Role: ${user.privligeLevel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ApprovalsTabView(
    requests: List<RallyRequestRow>,
    users: List<AdminProfile>,
    onSelectUser: (AdminProfile) -> Unit
) {
    val userIdsWithRequests = remember(requests) { requests.map { it.userId }.toSet() }
    val pendingUsers = remember(users, userIdsWithRequests) { users.filter { userIdsWithRequests.contains(it.id) } }

    if (pendingUsers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No pending requests.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            items(pendingUsers) { user ->
                val count = requests.count { it.userId == user.id }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelectUser(user) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                text = "$count Request${if (count == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

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

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun CreateRallyDialog(
    adminPerson: PersonInfo,
    onCreated: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var eventCost by remember { mutableStateOf("") }
    var eventStart by remember { mutableStateOf("2026-06-01") }
    var eventEnd by remember { mutableStateOf("2026-06-03") }
    var logoUrl by remember { mutableStateOf("") }

    var includeWaiver by remember { mutableStateOf(true) }
    var waiverName by remember { mutableStateOf("") }
    var waiverContent by remember { mutableStateOf("") }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

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
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = "Create New Rally", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rally Name (Required)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = eventCost,
                    onValueChange = { eventCost = it },
                    label = { Text("Event Cost ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = eventStart,
                        onValueChange = { eventStart = it },
                        label = { Text("Start (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = eventEnd,
                        onValueChange = { eventEnd = it },
                        label = { Text("End (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = logoUrl,
                    onValueChange = { logoUrl = it },
                    label = { Text("Rally Logo / Image URL") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Include Waiver", fontWeight = FontWeight.Bold)
                    Switch(checked = includeWaiver, onCheckedChange = { includeWaiver = it })
                }

                if (includeWaiver) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = waiverName,
                        onValueChange = { waiverName = it },
                        label = { Text("Waiver Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = waiverContent,
                        onValueChange = { waiverContent = it },
                        label = { Text("Waiver Agreement Content") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (errorMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val trimmedName = name.trim()
                            if (trimmedName.isBlank()) return@Button
                            isSaving = true
                            errorMessage = ""
                            scope.launch {
                                try {
                                    val rallyId = Uuid.random().toString()
                                    val costVal = eventCost.replace("$", "").trim().toDoubleOrNull()

                                    // 1. Group insert
                                    supabase.from("groups").insert(GroupInsertRow(id = rallyId, name = trimmedName))

                                    // 2. Rally insert
                                    supabase.from("rallies").insert(
                                        RallyInsertRow(
                                            id = rallyId,
                                            name = trimmedName,
                                            description = description.trim().ifEmpty { null },
                                            eventStart = eventStart.trim().ifEmpty { null },
                                            eventEnd = eventEnd.trim().ifEmpty { null },
                                            eventImage = logoUrl.trim().ifEmpty { "$trimmedName.png" },
                                            eventCost = costVal
                                        )
                                    )

                                    // 3. Waiver insert if enabled
                                    if (includeWaiver || waiverName.isNotBlank() || waiverContent.isNotBlank()) {
                                        val finalTitle = waiverName.trim().ifEmpty { "$trimmedName Waiver" }
                                        val finalContent = waiverContent.trim().ifEmpty { "Standard Rally Liability Waiver and Release." }
                                        supabase.from("waivers").insert(
                                            WaiverInsertRow(
                                                id = Uuid.random().toString(),
                                                rallyId = rallyId,
                                                waiverName = finalTitle,
                                                waiverContent = finalContent
                                            )
                                        )
                                    }

                                    // 4. Add admin as participant and group member
                                    if (!adminPerson.isTestUser) {
                                        try { supabase.from("rally_participants").insert(RallyParticipantInsert(adminPerson.id, rallyId)) } catch (_: Exception) {}
                                        try { supabase.from("group_members").insert(GroupMemberInsert(groupId = rallyId, userId = adminPerson.id)) } catch (_: Exception) {}
                                    }

                                    onCreated()
                                } catch (e: Exception) {
                                    println("Error creating rally: ${e.message}")
                                    errorMessage = e.message ?: "Failed to create rally."
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        enabled = name.isNotBlank() && !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditRallyDialog(
    rally: DatabaseRally,
    onUpdated: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(rally.name) }
    var description by remember { mutableStateOf(rally.description ?: "") }
    var eventCost by remember { mutableStateOf(rally.eventCost?.toString() ?: "") }
    var eventStart by remember { mutableStateOf(rally.eventStart ?: "") }
    var eventEnd by remember { mutableStateOf(rally.eventEnd ?: "") }
    var logoUrl by remember { mutableStateOf(rally.eventImage ?: "") }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

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
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = "Edit Rally", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rally Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = eventCost,
                    onValueChange = { eventCost = it },
                    label = { Text("Event Cost ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = eventStart,
                        onValueChange = { eventStart = it },
                        label = { Text("Start Date") },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = eventEnd,
                        onValueChange = { eventEnd = it },
                        label = { Text("End Date") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = logoUrl,
                    onValueChange = { logoUrl = it },
                    label = { Text("Logo / Image URL") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val trimmedName = name.trim()
                            if (trimmedName.isBlank()) return@Button
                            isSaving = true
                            errorMessage = ""
                            scope.launch {
                                try {
                                    val costVal = eventCost.replace("$", "").trim().toDoubleOrNull()
                                    val updateRow = RallyUpdateRow(
                                        name = trimmedName,
                                        description = description.trim().ifEmpty { null },
                                        eventStart = eventStart.trim().ifEmpty { null },
                                        eventEnd = eventEnd.trim().ifEmpty { null },
                                        eventImage = logoUrl.trim().ifEmpty { null },
                                        eventCost = costVal
                                    )

                                    supabase.from("rallies").update(updateRow) {
                                        filter { eq("id", rally.id) }
                                    }

                                    try {
                                        supabase.from("groups").update(GroupUpdateRow(name = trimmedName)) {
                                            filter { eq("id", rally.id) }
                                        }
                                    } catch (_: Exception) {}

                                    onUpdated()
                                } catch (e: Exception) {
                                    println("Error updating rally: ${e.message}")
                                    errorMessage = e.message ?: "Failed to update rally."
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        enabled = name.isNotBlank() && !isSaving
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun RallyAdminDialog(
    rally: DatabaseRally,
    allUsers: List<AdminProfile>,
    allParticipants: List<RallyParticipantRow>,
    onUpdate: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(0) } // 0: Name, 1: Signed Waivers
    var sortMenuExpanded by remember { mutableStateOf(false) }

    var signedUserIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showAddPeopleDialog by remember { mutableStateOf(false) }
    var selectedUserProfile by remember { mutableStateOf<AdminProfile?>(null) }

    val attendeeIds = remember(allParticipants, rally.id) {
        allParticipants.filter { it.rallyId == rally.id }.map { it.userId }.toSet()
    }
    val attendees = remember(allUsers, attendeeIds) {
        allUsers.filter { attendeeIds.contains(it.id) }
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(rally.id) {
        scope.launch {
            try {
                val waiverRows: List<Waiver> = supabase.from("waivers")
                    .select { filter { eq("rally_id", rally.id) } }
                    .decodeList()

                if (waiverRows.isEmpty()) {
                    signedUserIds = emptySet()
                    return@launch
                }

                val reqWaiverIds = waiverRows.map { it.id }.toSet()
                val signedRows: List<SignedWaiverRow> = supabase.from("signed_waivers")
                    .select()
                    .decodeList()

                val userSignedMap = mutableMapOf<String, MutableSet<String>>()
                for (row in signedRows) {
                    if (reqWaiverIds.contains(row.waiverId)) {
                        userSignedMap.getOrPut(row.userId) { mutableSetOf() }.add(row.waiverId)
                    }
                }

                val signedSet = mutableSetOf<String>()
                for ((uId, set) in userSignedMap) {
                    if (reqWaiverIds.all { set.contains(it) }) {
                        signedSet.add(uId)
                    }
                }
                signedUserIds = signedSet
            } catch (e: Exception) {
                println("Error loading waiver status: ${e.message}")
            }
        }
    }

    val filteredAttendees = remember(attendees, searchText, sortOption, signedUserIds) {
        val list = if (searchText.isBlank()) attendees else attendees.filter { it.name.contains(searchText, ignoreCase = true) }
        if (sortOption == 0) {
            list.sortedBy { it.name.lowercase() }
        } else {
            list.sortedWith(compareBy({ !signedUserIds.contains(it.id) }, { it.name.lowercase() }))
        }
    }

    val eligibleUsers = remember(allUsers, attendeeIds) {
        allUsers.filter { !attendeeIds.contains(it.id) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = rally.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        rally.eventCost?.let { cost ->
                            Text(text = "Cost: $$cost", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        }
                    }

                    Row {
                        IconButton(onClick = onEdit) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Rally", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Attendees (${filteredAttendees.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Box {
                        OutlinedButton(onClick = { sortMenuExpanded = true }) {
                            Text(if (sortOption == 0) "Sort: Name" else "Sort: Waivers", fontSize = 12.sp)
                        }
                        DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                            DropdownMenuItem(text = { Text("Name") }, onClick = { sortOption = 0; sortMenuExpanded = false })
                            DropdownMenuItem(text = { Text("Signed Waivers") }, onClick = { sortOption = 1; sortMenuExpanded = false })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Search attendees") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.height(220.dp)) {
                    items(filteredAttendees) { attendee ->
                        val isSigned = signedUserIds.contains(attendee.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedUserProfile = attendee }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = attendee.name, fontWeight = FontWeight.Medium)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isSigned) Color(0xFFE3F2FD) else Color(0xFFFFEBEE)
                                ) {
                                    Text(
                                        text = if (isSigned) "Signed" else "Not Signed",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSigned) Color(0xFF1565C0) else Color(0xFFC62828),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            supabase.from("rally_participants").delete { filter { eq("rally_id", rally.id); eq("user_id", attendee.id) } }
                                            try { supabase.from("group_members").delete { filter { eq("group_id", rally.id); eq("user_id", attendee.id) } } } catch (_: Exception) {}
                                            onUpdate()
                                        } catch (e: Exception) {
                                            println("Failed to remove attendee: ${e.message}")
                                        }
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(onClick = { showAddPeopleDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add People")
                    }

                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }

    if (showAddPeopleDialog) {
        AddPeopleDialog(
            rally = rally,
            eligibleUsers = eligibleUsers,
            onDone = { selectedIds ->
                scope.launch {
                    try {
                        for (uId in selectedIds) {
                            try { supabase.from("rally_participants").insert(RallyParticipantInsert(uId, rally.id)) } catch (_: Exception) {}
                            try { supabase.from("group_members").insert(GroupMemberInsert(groupId = rally.id, userId = uId)) } catch (_: Exception) {}
                        }
                        onUpdate()
                    } catch (e: Exception) {
                        println("Failed adding people: ${e.message}")
                    } finally {
                        showAddPeopleDialog = false
                    }
                }
            },
            onDismiss = { showAddPeopleDialog = false }
        )
    }

    selectedUserProfile?.let { user ->
        UserProfileDetailDialog(
            user = user,
            onDismiss = { selectedUserProfile = null }
        )
    }
}

@Composable
private fun AddPeopleDialog(
    rally: DatabaseRally,
    eligibleUsers: List<AdminProfile>,
    onDone: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedUserIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Select People for ${rally.name}", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(12.dp))

                if (eligibleUsers.isEmpty()) {
                    Text("All users are already participants.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(modifier = Modifier.height(250.dp)) {
                        items(eligibleUsers) { user ->
                            val isSelected = selectedUserIds.contains(user.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedUserIds = if (isSelected) selectedUserIds - user.id else selectedUserIds + user.id
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = isSelected, onCheckedChange = {
                                    selectedUserIds = if (it) selectedUserIds + user.id else selectedUserIds - user.id
                                })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = user.name, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onDone(selectedUserIds.toList()) }, enabled = selectedUserIds.isNotEmpty()) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleEditorDialog(
    user: AdminProfile,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    var role by remember { mutableStateOf(user.privligeLevel) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Edit Permissions", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "User: ${user.name}", fontWeight = FontWeight.SemiBold)

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = role == "User",
                        onClick = { role = "User" }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("User")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = role == "Admin",
                        onClick = { role = "Admin" }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Admin")
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            isSaving = true
                            scope.launch {
                                try {
                                    supabase.from("profiles")
                                        .update(mapOf("privilege_level" to role)) {
                                            filter { eq("id", user.id) }
                                        }
                                    onUpdate()
                                    onDismiss()
                                } catch (e: Exception) {
                                    println("Error updating role: ${e.message}")
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isSaving
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun ApprovalAdminDialog(
    user: AdminProfile,
    userRequests: List<RallyRequestRow>,
    rallies: List<DatabaseRally>,
    onUpdate: () -> Unit,
    onViewProfile: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "${user.name}'s Requests",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(userRequests) { req ->
                        val rally = rallies.firstOrNull { it.id == req.rallyId }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = rally?.name ?: "Rally",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            supabase.from("rally_requests")
                                                .delete { filter { eq("id", req.id) } }
                                            onUpdate()
                                        } catch (e: Exception) {
                                            println("Failed to decline: ${e.message}")
                                        }
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = null)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            supabase.from("rally_participants")
                                                .upsert(RallyParticipantInsert(user.id, req.rallyId))

                                            if (rally != null) {
                                                val groups = supabase.from("groups")
                                                    .select { filter { eq("name", rally.name) } }
                                                    .decodeList<GroupRow>()
                                                groups.firstOrNull()?.let { group ->
                                                    supabase.from("group_members")
                                                        .upsert(GroupMemberInsert(groupId = group.id, userId = user.id))
                                                }
                                            }

                                            supabase.from("rally_requests")
                                                .delete { filter { eq("id", req.id) } }

                                            onUpdate()
                                        } catch (e: Exception) {
                                            println("Failed to approve: ${e.message}")
                                        }
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onViewProfile, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Profile & ID", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            isProcessing = true
                            scope.launch {
                                for (req in userRequests) {
                                    val rally = rallies.firstOrNull { it.id == req.rallyId }
                                    try {
                                        supabase.from("rally_participants").upsert(RallyParticipantInsert(user.id, req.rallyId))
                                        if (rally != null) {
                                            val groups = supabase.from("groups").select { filter { eq("name", rally.name) } }.decodeList<GroupRow>()
                                            groups.firstOrNull()?.let { group ->
                                                supabase.from("group_members").upsert(GroupMemberInsert(groupId = group.id, userId = user.id))
                                            }
                                        }
                                        supabase.from("rally_requests").delete { filter { eq("id", req.id) } }
                                    } catch (_: Exception) {}
                                }
                                onUpdate()
                                isProcessing = false
                                onDismiss()
                            }
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Approve All", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun UserAvatar(userId: String, userName: String, modifier: Modifier = Modifier) {
    var avatarUrl by remember(userId) { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        avatarUrl = getProfileImageURL(userId)
    }

    AsyncImage(
        url = avatarUrl,
        contentDescription = userName,
        modifier = modifier,
        error = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        }
    )
}

@Composable
fun UserProfileDetailDialog(
    user: AdminProfile,
    onDismiss: () -> Unit
) {
    var idImageData by remember { mutableStateOf<ByteArray?>(null) }
    var isLoadingID by remember { mutableStateOf(true) }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(user.id) {
        isLoadingID = true
        profileImageUrl = getProfileImageURL(user.id)
        idImageData = fetchUserIDImageData(user.id)
        isLoadingID = false
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
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    url = profileImageUrl,
                    contentDescription = user.name,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape),
                    error = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = user.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)

                if (!user.instaHandle.isNullOrEmpty()) {
                    Text(text = "@${user.instaHandle}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }

                if (!user.carModel.isNullOrEmpty()) {
                    Text(text = user.carModel, style = MaterialTheme.typography.bodyMedium)
                }

                if (!user.phoneNumber.isNullOrEmpty()) {
                    Text(text = user.phoneNumber, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (user.bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = user.bio, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "User ID Document", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoadingID) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                } else if (idImageData != null) {
                    AsyncImage(
                        byteArray = idImageData!!,
                        contentDescription = "User ID Document",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No ID document uploaded", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close")
                }
            }
        }
    }
}

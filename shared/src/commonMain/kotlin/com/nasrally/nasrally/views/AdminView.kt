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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CarRental
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nasrally.nasrally.AdminProfile
import com.nasrally.nasrally.DatabaseRally
import com.nasrally.nasrally.GroupMemberInsert
import com.nasrally.nasrally.GroupRow
import com.nasrally.nasrally.PersonInfo
import com.nasrally.nasrally.RallyParticipantInsert
import com.nasrally.nasrally.RallyParticipantRow
import com.nasrally.nasrally.RallyRequestRow
import com.nasrally.nasrally.getProfileImageURL
import com.nasrally.nasrally.getRallyImageURL
import com.nasrally.nasrally.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

@Composable
fun AdminView(person: PersonInfo) {
    var selectedTab by remember { mutableStateOf(0) } // 0=Users, 1=Rallies, 2=Roles, 3=Approvals
    var users by remember { mutableStateOf<List<AdminProfile>>(emptyList()) }
    var rallies by remember { mutableStateOf<List<DatabaseRally>>(emptyList()) }
    var participants by remember { mutableStateOf<List<RallyParticipantRow>>(emptyList()) }
    var requests by remember { mutableStateOf<List<RallyRequestRow>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    var selectedRally by remember { mutableStateOf<DatabaseRally?>(null) }
    var selectedUserForRole by remember { mutableStateOf<AdminProfile?>(null) }
    var selectedUserForApproval by remember { mutableStateOf<AdminProfile?>(null) }

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

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Admin Dashboard",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
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
                    0 -> UsersTabView(users = users)
                    1 -> RalliesTabView(
                        rallies = rallies,
                        participants = participants,
                        onSelectRally = { selectedRally = it }
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

    selectedRally?.let { rally ->
        RallyAdminDialog(
            rally = rally,
            allUsers = users,
            allParticipants = participants,
            onUpdate = loadData,
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
            onDismiss = { selectedUserForApproval = null }
        )
    }
}

@Composable
private fun UsersTabView(users: List<AdminProfile>) {
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        items(users) { user ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
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
                    AsyncImage(
                        url = getProfileImageURL(user.id),
                        contentDescription = user.name,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape),
                        error = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                        }
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
    onSelectRally: (DatabaseRally) -> Unit
) {
    LazyColumn(modifier = Modifier.padding(16.dp)) {
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

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null
                    )
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

@Composable
private fun RallyAdminDialog(
    rally: DatabaseRally,
    allUsers: List<AdminProfile>,
    allParticipants: List<RallyParticipantRow>,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    val attendeeIds = remember(allParticipants, rally.id) {
        allParticipants.filter { it.rallyId == rally.id }.map { it.userId }.toSet()
    }
    val attendees = remember(allUsers, attendeeIds) {
        allUsers.filter { attendeeIds.contains(it.id) }
    }
    val scope = rememberCoroutineScope()

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
                    Text(text = rally.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Attendees (${attendees.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.height(250.dp)) {
                    items(attendees) { attendee ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = attendee.name,
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Medium
                            )

                            IconButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            supabase.from("rally_participants")
                                                .delete {
                                                    filter {
                                                        eq("rally_id", rally.id)
                                                        eq("user_id", attendee.id)
                                                    }
                                                }
                                            onUpdate()
                                        } catch (e: Exception) {
                                            println("Failed to remove attendee: ${e.message}")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
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
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}

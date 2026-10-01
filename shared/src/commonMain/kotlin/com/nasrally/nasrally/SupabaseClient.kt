package com.nasrally.nasrally

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.util.toUpperCasePreservingASCIIRules
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.compose.resources.decodeToImageBitmap

val supabase = createSupabaseClient(
    supabaseUrl = "https://api-nas-rally.mayflower-paradise.us",
    supabaseKey = "sb_publishable_xflKOJnjZKIKm7f_Ri4Bn4_7-zhLiVC"
) {
    install(Auth)
    install(Postgrest)
    install(Realtime)
    install(Storage)
}

suspend fun fetchCurrentProfile(): PersonInfo? {
    try {
        supabase.auth.awaitInitialization()
    } catch (e: Exception) {
        println("Error awaiting auth initialization: ${e.message}")
    }
    val session = supabase.auth.currentSessionOrNull() ?: return null
    val user = session.user ?: return null
    val userName = user.userMetadata?.get("name")?.toString() ?: user.email ?: "User"
    return loadPersonInfo(user.id) ?: loadPersonInfoOrCreateDefault(user.id, userName)
}

suspend fun loadPersonInfo(userId: String): PersonInfo? {
    return try {
        val row = supabase.from("profiles")
            .select {
                filter {
                    eq("id", userId)
                }
            }
            .decodeSingleOrNull<SupabasePersonRow>() ?: return null

        @Serializable
        data class ParticipantRow(val rally_id: String)

        val participantRows = try {
            supabase.from("rally_participants")
                .select(Columns.raw("rally_id")) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<ParticipantRow>()
        } catch (e: Exception) {
            emptyList()
        }

        val rallyIds = participantRows.map { it.rally_id }
        val rallieNames = if (rallyIds.isNotEmpty()) {
            @Serializable
            data class RallyNameRow(val id: String, val name: String)

            try {
                supabase.from("rallies")
                    .select(Columns.raw("id, name")) {
                        filter {
                            isIn("id", rallyIds)
                        }
                    }
                    .decodeList<RallyNameRow>()
                    .map { it.name }
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        row.toPersonInfo(ralliesJoined = rallieNames.size, rallieNames = rallieNames)
    } catch (e: Exception) {
        println("Error loading person info: ${e.message}")
        null
    }
}

suspend fun loadPersonInfoOrCreateDefault(userId: String, name: String): PersonInfo {
    val existing = loadPersonInfo(userId)
    if (existing != null) return existing

    val newPerson = NewSupabasePersonRow(
        id = userId,
        name = name,
        theme = "Auto",
        bio = "",
        privligeLevel = "User",
        tos = false
    )

    return try {
        supabase.from("profiles").upsert(newPerson)
        loadPersonInfo(userId) ?: PersonInfo(id = userId, name = name)
    } catch (e: Exception) {
        PersonInfo(id = userId, name = name)
    }
}

suspend fun login(emailInput: String, passwordInput: String): AuthResult {
    return try {
        supabase.auth.signInWith(Email) {
            email = emailInput
            password = passwordInput
        }
        val user = supabase.auth.currentUserOrNull()
            ?: return AuthResult.Failure("Failed to retrieve user after login.")
        val userName = user.userMetadata?.get("name")?.toString() ?: emailInput
        val personInfo = loadPersonInfoOrCreateDefault(user.id, userName)
        AuthResult.Success(personInfo)
    } catch (e: Exception) {
        println("Login failed: ${e.message}")
        AuthResult.Failure(e.message ?: "Login failed. Please check your credentials.")
    }
}

suspend fun signup(nameInput: String, emailInput: String, passwordInput: String): AuthResult {
    return try {
        supabase.auth.signUpWith(Email) {
            email = emailInput
            password = passwordInput
            data = buildJsonObject {
                put("name", nameInput)
            }
        }
        val user = supabase.auth.currentUserOrNull()
            ?: return AuthResult.Failure("Signup successful, but user session is null.")

        val newPerson = NewSupabasePersonRow(
            id = user.id,
            name = nameInput,
            theme = "Auto",
            bio = "",
            privligeLevel = "User",
            tos = false
        )

        supabase.from("profiles").upsert(newPerson)
        val personInfo = loadPersonInfo(user.id) ?: loadPersonInfoOrCreateDefault(user.id, nameInput)
        AuthResult.Success(personInfo)
    } catch (e: Exception) {
        println("Signup failed: ${e.message}")
        AuthResult.Failure(e.message ?: "Signup failed. Please try again.")
    }
}

suspend fun logout() {
    try {
        supabase.auth.signOut()
    } catch (e: Exception) {
        println("Logout failed: ${e.message}")
    }
}

@Serializable
data class SupabaseThemeUpdateRow(
    val theme: String
)

suspend fun updateTheme(userId: String, theme: String) {
    if (userId == PersonInfo.TEST_USER_ID) return
    try {
        supabase.from("profiles")
            .update(SupabaseThemeUpdateRow(theme = theme)) {
                filter {
                    eq("id", userId)
                }
            }
    } catch (e: Exception) {
        println("Error updating theme: ${e.message}")
    }
}

suspend fun updateProfile(person: PersonInfo) {
    if (person.isTestUser) return
    val updateData = SupabaseProfileUpdateRow(
        name = person.name,
        bio = person.bio,
        instaHandle = person.instaHandle,
        carModel = person.carModel,
        phoneNumber = person.phoneNumber
    )
    supabase.from("profiles")
        .update(updateData) {
            filter {
                eq("id", person.id)
            }
        }
}

suspend fun getProfileImageURL(userId: String): String? {
    if (userId.isBlank()) return null
    return try {
        supabase.storage.from("Profile Pictures").publicUrl("$userId".toUpperCasePreservingASCIIRules()+"/images/profile.jpg")
    } catch (e: Exception) {
        println("Error getting public URL for profile picture: ${e.message}")
        null
    }
}

fun getRallyImageURL(name: String): String {
    return supabase.storage.from("RallyLogos").publicUrl("$name.png")
}

suspend fun fetchUserWaivers(userId: String): UserWaiversResult {
    return try {
        val allWaivers: List<Waiver> = supabase.from("waivers")
            .select(Columns.raw("id, waiver_name, waiver_content, rallies!inner(name, rally_participants!inner(user_id))")) {
                filter {
                    eq("rallies.rally_participants.user_id", userId)
                }
            }
            .decodeList<Waiver>()

        val signedRows: List<SignedWaiverRow> = try {
            supabase.from("signed_waivers")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<SignedWaiverRow>()
        } catch (e: Exception) {
            emptyList()
        }

        val signedSet = signedRows.map { it.waiverId }.toSet()
        val pending = allWaivers.filter { !signedSet.contains(it.id) }
        val signed = allWaivers.filter { signedSet.contains(it.id) }

        UserWaiversResult(pending = pending, signed = signed)
    } catch (e: Exception) {
        println("Fetch waivers failed: ${e.message}")
        UserWaiversResult(pending = emptyList(), signed = emptyList())
    }
}

suspend fun fetchUserIDImageData(userId: String): ByteArray? {
    return try {
        supabase.storage.from("User-IDs").downloadAuthenticated("${userId.toUpperCasePreservingASCIIRules()}/userid.png")
    } catch (e: Exception) {
        println("Download ID image failed: ${e.message}")
        null
    }
}

private val imageHttpClient = HttpClient()

suspend fun getCachedRallyLogoData(name: String): ByteArray? {
    if (name.isBlank()) return null
    val sanitized = name.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
    val key = "rally_logo_$sanitized.png"
    val tsKey = "ts_rally_logo_$sanitized"
    val lastTs = LocalDiskCache.getTimestamp(tsKey)
    val now = Clock.System.now().toEpochMilliseconds()
    val oneWeekMs = 7 * 24 * 60 * 60 * 1000L

    if (lastTs != null && (now - lastTs < oneWeekMs)) {
        val cached = LocalDiskCache.getBytes(key)
        if (cached != null && cached.isNotEmpty()) {
            try {
                cached.decodeToImageBitmap()
                return cached
            } catch (_: Exception) {
                LocalDiskCache.remove(key)
            }
        }
    }

    return try {
        val url = getRallyImageURL(name)
        val response = imageHttpClient.get(url)
        if (response.status.value in 200..299) {
            val bytes = response.bodyAsBytes()
            if (bytes.isNotEmpty()) {
                try {
                    bytes.decodeToImageBitmap()
                    LocalDiskCache.saveBytes(key, bytes)
                    LocalDiskCache.saveTimestamp(tsKey, now)
                    return bytes
                } catch (_: Exception) {}
            }
        }
        null
    } catch (e: Exception) {
        println("Error downloading rally logo ($name): ${e.message}")
        val cached = LocalDiskCache.getBytes(key)
        if (cached != null && cached.isNotEmpty()) {
            try {
                cached.decodeToImageBitmap()
                return cached
            } catch (_: Exception) {
                LocalDiskCache.remove(key)
                null
            }
        } else null
    }
}

suspend fun getCachedProfileImageData(userId: String): ByteArray? {
    if (userId.isBlank()) return null
    val key = "profile_$userId.jpg"
    val cached = LocalDiskCache.getBytes(key)
    if (cached != null && cached.isNotEmpty()) {
        try {
            cached.decodeToImageBitmap()
            return cached
        } catch (_: Exception) {
            LocalDiskCache.remove(key)
        }
    }

    val url = getProfileImageURL(userId) ?: return null
    return try {
        val response = imageHttpClient.get(url)
        if (response.status.value in 200..299) {
            val bytes = response.bodyAsBytes()
            if (bytes.isNotEmpty()) {
                try {
                    bytes.decodeToImageBitmap()
                    LocalDiskCache.saveBytes(key, bytes)
                    return bytes
                } catch (_: Exception) {}
            }
        } else {
            println("Profile image download returned HTTP ${response.status.value} for user $userId")
        }
        null
    } catch (e: Exception) {
        println("Error downloading profile image ($userId): ${e.message}")
        null
    }
}

fun invalidateCachedProfileImage(userId: String) {
    if (userId.isNotBlank()) {
        LocalDiskCache.remove("profile_$userId.jpg")
    }
}

suspend fun getCachedUserIDImageData(userId: String, isAdmin: Boolean): ByteArray? {
    if (!isAdmin || userId.isBlank()) return null
    val key = "id_${userId.toUpperCasePreservingASCIIRules()}.png"
    val cached = LocalDiskCache.getBytes(key)
    if (cached != null && cached.isNotEmpty()) {
        return cached
    }

    val downloaded = fetchUserIDImageData(userId)
    if (downloaded != null && downloaded.isNotEmpty()) {
        LocalDiskCache.saveBytes(key, downloaded)
    }
    return downloaded
}

suspend fun loadSensitiveInfo(): SensitiveInfoRow? {
    return try {
        val rows = supabase.from("sensitiveInfo")
            .select()
            .decodeList<SensitiveInfoRow>()
        rows.firstOrNull()
    } catch (e: Exception) {
        println("Error loading sensitive info: ${e.message}")
        null
    }
}

suspend fun saveSensitiveInfo(row: SensitiveInfoRow) {
    supabase.from("sensitiveInfo").upsert(row)
}

@Serializable
data class AISummaryResponse(
    val summary: String? = null,
    val error: String? = null
)

suspend fun fetchAISummary(): String {
    val session = supabase.auth.currentSessionOrNull() ?: throw IllegalStateException("Not logged in")
    val accessToken = session.accessToken

    val client = HttpClient()
    return try {
        val response: HttpResponse = client.post("https://api-nas-rally.mayflower-paradise.us/functions/v1/ai-summarization") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header(HttpHeaders.ContentType, "application/json")
        }
        val text = response.bodyAsText()
        if (response.status.value == 200) {
            val parsed = Json.decodeFromString<AISummaryResponse>(text)
            parsed.summary ?: throw IllegalStateException("No summary returned")
        } else {
            val parsed = runCatching { Json.decodeFromString<AISummaryResponse>(text) }.getOrNull()
            throw IllegalStateException(parsed?.error ?: "Server error: ${response.status.value}")
        }
    } finally {
        client.close()
    }
}


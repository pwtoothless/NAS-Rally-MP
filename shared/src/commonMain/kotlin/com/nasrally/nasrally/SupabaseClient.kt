package com.nasrally.nasrally

import androidx.compose.ui.graphics.ImageBitmap
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
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

/**
 * Main Supabase client instance configured with Auth, Postgrest, Realtime, and Storage modules.
 */
val supabase = createSupabaseClient(
    supabaseUrl = "https://api-nas-rally.mayflower-paradise.us",
    supabaseKey = "sb_publishable_xflKOJnjZKIKm7f_Ri4Bn4_7-zhLiVC"
) {
    install(Auth)
    install(Postgrest)
    install(Realtime)
    install(Storage)
}

/**
 * Fetches the currently authenticated user's profile information.
 *
 * @return [PersonInfo] profile if authenticated; `null` otherwise.
 */
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

/**
 * Loads [PersonInfo] profile from Supabase `profiles` table for [userId].
 *
 * @param userId Unique user ID.
 * @return [PersonInfo] if found; `null` if profile query fails or doesn't exist.
 */
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

/**
 * Loads existing profile or creates a default profile row if missing.
 *
 * @param userId Unique user ID.
 * @param name Default name string.
 * @return [PersonInfo] instance.
 */
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

/**
 * Authenticates user via email and password against Supabase Auth.
 *
 * @param emailInput User email address.
 * @param passwordInput User password.
 * @return [AuthResult] containing profile on success or error message on failure.
 */
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

/**
 * Registers a new account via email/password and initializes user profile in database.
 *
 * @param nameInput Full name.
 * @param emailInput Email address.
 * @param passwordInput Password.
 * @return [AuthResult] outcome.
 */
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

/**
 * Logs out current user session.
 */
suspend fun logout() {
    try {
        supabase.auth.signOut()
    } catch (e: Exception) {
        println("Logout failed: ${e.message}")
    }
}

/**
 * Deletes user account via RPC function or marks user privilege level as Removed.
 *
 * @param userId Target user ID to delete.
 */
suspend fun deleteUserAccount(userId: String) {
    if (userId.isBlank() || userId == PersonInfo.TEST_USER_ID) return
    try {
        supabase.postgrest.rpc("delete_user_account", buildJsonObject { put("target_user_id", userId) })
    } catch (e: Exception) {
        println("RPC delete_user_account failed: ${e.message}, falling back to profile status update.")
        try {
            supabase.from("profiles").update(mapOf("privligeLevel" to "Removed")) {
                filter { eq("id", userId) }
            }
        } catch (ex: Exception) {
            println("Fallback profile update failed: ${ex.message}")
        }
    }
}

/** Theme update row container. */
@Serializable
data class SupabaseThemeUpdateRow(
    val theme: String
)

/**
 * Updates user theme preference in database.
 */
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

/** Terms of Service update row container. */
@Serializable
data class SupabaseTOSUpdateRow(
    val tos: Boolean
)

/**
 * Updates Terms of Service acceptance state in database.
 */
suspend fun updateTOS(userId: String, tos: Boolean) {
    if (userId == PersonInfo.TEST_USER_ID) return
    try {
        supabase.from("profiles")
            .update(SupabaseTOSUpdateRow(tos = tos)) {
                filter {
                    eq("id", userId)
                }
            }
    } catch (e: Exception) {
        println("Error updating TOS: ${e.message}")
    }
}

/**
 * Updates profile detail fields in database.
 */
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

/**
 * Obtains public HTTP URL for user profile picture in Supabase Storage.
 */
suspend fun getProfileImageURL(userId: String): String? {
    if (userId.isBlank()) return null
    return try {
        supabase.storage.from("Profile Pictures").publicUrl("$userId".toUpperCasePreservingASCIIRules()+"/images/profile.jpg")
    } catch (e: Exception) {
        println("Error getting public URL for profile picture: ${e.message}")
        null
    }
}

/**
 * Obtains public HTTP URL for rally event logo in Supabase Storage.
 */
fun getRallyImageURL(name: String): String {
    return supabase.storage.from("RallyLogos").publicUrl("$name.png")
}

/**
 * Fetches pending and signed waivers for [userId].
 */
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

/**
 * Downloads user identity card document image bytes from Supabase Storage.
 */
suspend fun fetchUserIDImageData(userId: String): ByteArray? {
    return try {
        supabase.storage.from("User-IDs").downloadAuthenticated("${userId.toUpperCasePreservingASCIIRules()}/userid.png")
    } catch (e: Exception) {
        println("Download ID image failed: ${e.message}")
        null
    }
}

private val imageHttpClient = HttpClient()

/**
 * Fetches and caches rally logo image bytes on local disk with 7-day expiration.
 */
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

/** Memory cache for decoded user profile bitmaps. */
object ProfileImageMemoryCache {
    private val cache = mutableMapOf<String, ImageBitmap>()

    fun get(userId: String): ImageBitmap? {
        if (userId.isBlank()) return null
        return cache[userId]
    }

    fun put(userId: String, bitmap: ImageBitmap) {
        if (userId.isNotBlank()) {
            cache[userId] = bitmap
        }
    }

    fun remove(userId: String) {
        cache.remove(userId)
    }
}

/** Memory cache for decoded rally logo bitmaps. */
object RallyLogoMemoryCache {
    private val cache = mutableMapOf<String, ImageBitmap>()

    fun get(name: String): ImageBitmap? {
        if (name.isBlank()) return null
        return cache[name]
    }

    fun put(name: String, bitmap: ImageBitmap) {
        if (name.isNotBlank()) {
            cache[name] = bitmap
        }
    }
}

/**
 * Downloads and caches profile image bitmap.
 */
suspend fun getCachedProfileImageBitmap(userId: String): ImageBitmap? {
    if (userId.isBlank()) return null
    ProfileImageMemoryCache.get(userId)?.let { return it }

    val key = "profile_$userId.jpg"
    val cached = LocalDiskCache.getBytes(key)
    if (cached != null && cached.isNotEmpty()) {
        try {
            val bitmap = cached.decodeToImageBitmap()
            ProfileImageMemoryCache.put(userId, bitmap)
            return bitmap
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
                    val bitmap = bytes.decodeToImageBitmap()
                    LocalDiskCache.saveBytes(key, bytes)
                    ProfileImageMemoryCache.put(userId, bitmap)
                    return bitmap
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

/**
 * Downloads profile image raw byte array with local disk caching.
 */
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

/**
 * Clears memory and disk cache entries for [userId]'s profile picture.
 */
fun invalidateCachedProfileImage(userId: String) {
    if (userId.isNotBlank()) {
        ProfileImageMemoryCache.remove(userId)
        LocalDiskCache.remove("profile_$userId.jpg")
    }
}

/**
 * Downloads and caches ID document image bytes for admin review.
 */
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

/**
 * Loads sensitive administrative records from `sensitiveInfo` table.
 */
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

/**
 * Upserts sensitive administrative records to `sensitiveInfo` table.
 */
suspend fun saveSensitiveInfo(row: SensitiveInfoRow) {
    supabase.from("sensitiveInfo").upsert(row)
}

/**
 * AI summary function response structure.
 */
@Serializable
data class AISummaryResponse(
    val summary: String? = null,
    val error: String? = null
)

/**
 * Invokes Supabase Edge Function `ai-summarization` to retrieve AI-generated rally summaries.
 */
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


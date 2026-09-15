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
import kotlin.time.Duration.Companion.minutes
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
    val session = supabase.auth.currentSessionOrNull() ?: return null
    val userId = session.user?.id ?: return null
    return loadPersonInfo(userId)
}

suspend fun loadPersonInfo(userId: String): PersonInfo? {
    return try {
        val row = supabase.from("profiles")
            .select {
                filter {
                    eq("id", userId)
                }
            }
            .decodeSingleOrNull<SupabasePersonRow>()
        row?.toPersonInfo()
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
        theme = "Default",
        bio = "",
        ralliesJoined = 0,
        rallieNames = emptyList(),
        privligeLevel = "User",
        tos = false
    )

    return try {
        val inserted = supabase.from("profiles")
            .upsert(newPerson) {
                select()
            }
            .decodeSingle<SupabasePersonRow>()
        inserted.toPersonInfo()
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
            theme = "Dark",
            bio = "",
            ralliesJoined = 0,
            rallieNames = emptyList(),
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
    return try {
        supabase.storage.from("Profile Pictures").createSignedUrl(
            path = "$userId/images/profile.jpg",
            expiresIn = 60.minutes
        )
    } catch (e: Exception) {
        println("Error creating signed URL for profile picture: ${e.message}")
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
        supabase.storage.from("User-IDs").downloadAuthenticated("$userId/userid.png")
    } catch (e: Exception) {
        println("Download ID image failed: ${e.message}")
        null
    }
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

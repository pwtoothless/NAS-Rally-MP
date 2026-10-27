package com.nasrally.nasrally

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Complete profile representation for a user in NASRally.
 *
 * @property id Unique user ID uuid string.
 * @property name User's full display name.
 * @property theme User selected UI theme preference ("Auto", "Dark", "Light", "Blue", "Red").
 * @property bio User biography statement.
 * @property ralliesJoined Total number of rallies joined by the user.
 * @property rallieNames List of names of rallies the user has joined.
 * @property privligeLevel User role ("User", "Admin", etc.).
 * @property tos Flag indicating whether terms of service are accepted.
 * @property instaHandle Instagram handle.
 * @property carModel Vehicle description / car model.
 * @property phoneNumber Contact phone number.
 */
@Serializable
data class PersonInfo(
    val id: String,
    val name: String,
    val theme: String = "Auto",
    val bio: String = "",
    val ralliesJoined: Int = 0,
    val rallieNames: List<String> = emptyList(),
    @SerialName("privilege_level") val privligeLevel: String = "User",
    val tos: Boolean = false,
    @SerialName("insta_handle") val instaHandle: String = "",
    @SerialName("car_model") val carModel: String = "",
    @SerialName("phone_number") val phoneNumber: String = ""
) {
    companion object {
        const val TEST_USER_ID = "54455354-5553-4552-0000-000000000001"
        val testUser = PersonInfo(
            id = TEST_USER_ID,
            name = "Test User",
            theme = "Auto",
            bio = "Local development profile",
            ralliesJoined = 0,
            rallieNames = emptyList(),
            privligeLevel = "User",
            tos = true,
            instaHandle = "",
            carModel = "",
            phoneNumber = ""
        )
    }
    val isTestUser: Boolean get() = id == TEST_USER_ID
}

/**
 * Supabase `profiles` table row mapping data structure.
 */
@Serializable
data class SupabasePersonRow(
    val id: String,
    val name: String? = null,
    val theme: String? = null,
    val bio: String? = null,
    @SerialName("privilege_level") val privligeLevel: String? = null,
    val tos: Boolean? = null,
    @SerialName("insta_handle") val instaHandle: String? = null,
    @SerialName("car_model") val carModel: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null
) {
    /**
     * Converts a database row [SupabasePersonRow] into application domain model [PersonInfo].
     */
    fun toPersonInfo(ralliesJoined: Int = 0, rallieNames: List<String> = emptyList()): PersonInfo = PersonInfo(
        id = id,
        name = name ?: "",
        theme = theme ?: "Auto",
        bio = bio ?: "",
        ralliesJoined = ralliesJoined,
        rallieNames = rallieNames,
        privligeLevel = privligeLevel ?: "User",
        tos = tos ?: false,
        instaHandle = instaHandle ?: "",
        carModel = carModel ?: "",
        phoneNumber = phoneNumber ?: ""
    )
}

/**
 * Model for inserting new user profiles into Supabase `profiles` table.
 */
@Serializable
data class NewSupabasePersonRow(
    val id: String,
    val name: String,
    val theme: String = "Auto",
    val bio: String = "",
    @SerialName("privilege_level") val privligeLevel: String = "User",
    val tos: Boolean = false,
    @SerialName("insta_handle") val instaHandle: String? = null,
    @SerialName("car_model") val carModel: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null
)

/**
 * Model for updating profile information in Supabase `profiles` table.
 */
@Serializable
data class SupabaseProfileUpdateRow(
    val name: String,
    val bio: String,
    @SerialName("insta_handle") val instaHandle: String,
    @SerialName("car_model") val carModel: String,
    @SerialName("phone_number") val phoneNumber: String
)

/**
 * Chat message model for Supabase `messages` table.
 */
@Serializable
data class Message(
    val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String,
    @SerialName("created_at") val createdAt: String
)

/**
 * Database representation of a rally event.
 */
@Serializable
data class DatabaseRally(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("event_start") val eventStart: String? = null,
    @SerialName("event_end") val eventEnd: String? = null,
    @SerialName("event_image") val eventImage: String? = null,
    @SerialName("event_cost") val eventCost: Double? = null
)

/**
 * Nested rally info for waiver queries.
 */
@Serializable
data class WaiverRallyInfo(
    val name: String
)

/**
 * Waiver record model from Supabase `waivers` table.
 */
@Serializable
data class Waiver(
    val id: String,
    @SerialName("waiver_name") val waiverName: String,
    @SerialName("waiver_content") val waiverContent: String? = null,
    val rallies: WaiverRallyInfo? = null
)

/**
 * Database record representing a signed waiver relationship.
 */
@Serializable
data class SignedWaiverRow(
    @SerialName("waiver_id") val waiverId: String,
    @SerialName("user_id") val userId: String
)

/**
 * Insertion payload for signing a waiver.
 */
@Serializable
data class SignedWaiverInsertRow(
    @SerialName("user_id") val userId: String,
    @SerialName("waiver_id") val waiverId: String
)

/**
 * Container holding lists of pending and signed waivers for a user.
 */
data class UserWaiversResult(
    val pending: List<Waiver>,
    val signed: List<Waiver>
)

/**
 * Insertion model for administrative rally creation.
 */
@Serializable
data class RallyInsertRow(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("event_start") val eventStart: String? = null,
    @SerialName("event_end") val eventEnd: String? = null,
    @SerialName("event_image") val eventImage: String? = null,
    @SerialName("event_cost") val eventCost: Double? = null
)

/**
 * Update model for administrative rally modification.
 */
@Serializable
data class RallyUpdateRow(
    val name: String,
    val description: String? = null,
    @SerialName("event_start") val eventStart: String? = null,
    @SerialName("event_end") val eventEnd: String? = null,
    @SerialName("event_image") val eventImage: String? = null,
    @SerialName("event_cost") val eventCost: Double? = null
)

/**
 * Group chat insertion row.
 */
@Serializable
data class GroupInsertRow(
    val id: String,
    val name: String
)

/**
 * Group chat update row.
 */
@Serializable
data class GroupUpdateRow(
    val name: String
)

/**
 * Waiver insertion model.
 */
@Serializable
data class WaiverInsertRow(
    val id: String,
    @SerialName("rally_id") val rallyId: String,
    @SerialName("waiver_name") val waiverName: String,
    @SerialName("waiver_content") val waiverContent: String
)

/**
 * Administrative sensitive user information (e.g., payment details).
 */
@Serializable
data class SensitiveInfoRow(
    val id: String,
    val ccn: Long = 0,
    val cvv: Int = 0,
    val exp: String = "",
    val name: String = ""
)

/**
 * Profile model used in administrative user management.
 */
@Serializable
data class AdminProfile(
    val id: String,
    val name: String = "",
    val bio: String = "",
    @SerialName("privilege_level") val privligeLevel: String = "User",
    val theme: String? = null,
    val tos: Boolean? = null,
    @SerialName("insta_handle") val instaHandle: String? = null,
    @SerialName("car_model") val carModel: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null
)

/**
 * User join request record for a rally event.
 */
@Serializable
data class RallyRequestRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("rally_id") val rallyId: String
)

/**
 * Participant record for an enrolled rally member.
 */
@Serializable
data class RallyParticipantRow(
    @SerialName("rally_id") val rallyId: String,
    @SerialName("user_id") val userId: String
)

/**
 * Chat group table row representation.
 */
@Serializable
data class GroupRow(
    val id: String,
    val name: String? = null
)

/**
 * Insertion payload for adding a user to a chat group.
 */
@Serializable
data class GroupMemberInsert(
    @SerialName("group_id") val groupId: String,
    @SerialName("user_id") val userId: String
)

/**
 * Insertion payload for adding a rally participant.
 */
@Serializable
data class RallyParticipantInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("rally_id") val rallyId: String
)

/**
 * Insertion payload for requesting to join a rally.
 */
@Serializable
data class RallyRequestInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("rally_id") val rallyId: String
)

/**
 * Read receipt record indicating a user has viewed a chat message.
 */
@Serializable
data class ReadReceipt(
    @SerialName("message_id") val messageId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("read_at") val readAt: String
)

/**
 * Insertion payload for creating a message read receipt.
 */
@Serializable
data class ReadReceiptInsert(
    @SerialName("message_id") val messageId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("read_at") val readAt: String
)

/**
 * Table row representing group membership.
 */
@Serializable
data class GroupMemberRow(
    @SerialName("group_id") val groupId: String,
    @SerialName("user_id") val userId: String
)

/**
 * Sealed class representing authentication request results.
 */
sealed class AuthResult {
    /** Successful authentication holding [person] profile. */
    data class Success(val person: PersonInfo) : AuthResult()
    /** Failed authentication with error [message]. */
    data class Failure(val message: String) : AuthResult()
}

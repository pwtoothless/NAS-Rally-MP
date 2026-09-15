package com.nasrally.nasrally

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonInfo(
    val id: String,
    val name: String,
    val theme: String = "Default",
    val bio: String = "",
    @SerialName("rallies_joined") val ralliesJoined: Int = 0,
    @SerialName("rallie_names") val rallieNames: List<String> = emptyList(),
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
            theme = "Default",
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

@Serializable
data class SupabasePersonRow(
    val id: String,
    val name: String? = null,
    val theme: String? = null,
    val bio: String? = null,
    @SerialName("rallies_joined") val ralliesJoined: Int? = null,
    @SerialName("rallie_names") val rallieNames: List<String>? = null,
    @SerialName("privilege_level") val privligeLevel: String? = null,
    val tos: Boolean? = null,
    @SerialName("insta_handle") val instaHandle: String? = null,
    @SerialName("car_model") val carModel: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null
) {
    fun toPersonInfo(): PersonInfo = PersonInfo(
        id = id,
        name = name ?: "",
        theme = theme ?: "Default",
        bio = bio ?: "",
        ralliesJoined = ralliesJoined ?: 0,
        rallieNames = rallieNames ?: emptyList(),
        privligeLevel = privligeLevel ?: "User",
        tos = tos ?: false,
        instaHandle = instaHandle ?: "",
        carModel = carModel ?: "",
        phoneNumber = phoneNumber ?: ""
    )
}

@Serializable
data class NewSupabasePersonRow(
    val id: String,
    val name: String,
    val theme: String = "Dark",
    val bio: String = "",
    @SerialName("rallies_joined") val ralliesJoined: Int = 0,
    @SerialName("rallie_names") val rallieNames: List<String> = emptyList(),
    @SerialName("privilege_level") val privligeLevel: String = "User",
    val tos: Boolean = false,
    @SerialName("insta_handle") val instaHandle: String? = null,
    @SerialName("car_model") val carModel: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null
)

@Serializable
data class SupabaseProfileUpdateRow(
    val name: String,
    val bio: String,
    @SerialName("insta_handle") val instaHandle: String,
    @SerialName("car_model") val carModel: String,
    @SerialName("phone_number") val phoneNumber: String
)

@Serializable
data class Message(
    val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String,
    @SerialName("created_at") val createdAt: String
)

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

@Serializable
data class WaiverRallyInfo(
    val name: String
)

@Serializable
data class Waiver(
    val id: String,
    @SerialName("waiver_name") val waiverName: String,
    @SerialName("waiver_content") val waiverContent: String? = null,
    val rallies: WaiverRallyInfo? = null
)

@Serializable
data class SignedWaiverRow(
    @SerialName("waiver_id") val waiverId: String,
    @SerialName("user_id") val userId: String
)

@Serializable
data class SignedWaiverInsertRow(
    @SerialName("user_id") val userId: String,
    @SerialName("waiver_id") val waiverId: String
)

data class UserWaiversResult(
    val pending: List<Waiver>,
    val signed: List<Waiver>
)

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

@Serializable
data class RallyUpdateRow(
    val name: String,
    val description: String? = null,
    @SerialName("event_start") val eventStart: String? = null,
    @SerialName("event_end") val eventEnd: String? = null,
    @SerialName("event_image") val eventImage: String? = null,
    @SerialName("event_cost") val eventCost: Double? = null
)

@Serializable
data class GroupInsertRow(
    val id: String,
    val name: String
)

@Serializable
data class GroupUpdateRow(
    val name: String
)

@Serializable
data class WaiverInsertRow(
    val id: String,
    @SerialName("rally_id") val rallyId: String,
    @SerialName("waiver_name") val waiverName: String,
    @SerialName("waiver_content") val waiverContent: String
)

@Serializable
data class SensitiveInfoRow(
    val id: String,
    val ccn: Long = 0,
    val cvv: Int = 0,
    val exp: String = "",
    val name: String = ""
)

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

@Serializable
data class RallyRequestRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("rally_id") val rallyId: String
)

@Serializable
data class RallyParticipantRow(
    @SerialName("rally_id") val rallyId: String,
    @SerialName("user_id") val userId: String
)

@Serializable
data class GroupRow(
    val id: String,
    val name: String? = null
)

@Serializable
data class GroupMemberInsert(
    @SerialName("group_id") val groupId: String,
    @SerialName("user_id") val userId: String
)

@Serializable
data class RallyParticipantInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("rally_id") val rallyId: String
)

@Serializable
data class RallyRequestInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("rally_id") val rallyId: String
)

sealed class AuthResult {
    data class Success(val person: PersonInfo) : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

//
//  Supabase.swift
//  NAS Rally
//

import Foundation
import Supabase

let supabase = SupabaseClient(
    supabaseURL: URL(string: "http://24.16.3.100:8000")!,
    supabaseKey: "sb_publishable_xflKOJnjZKIKm7f_Ri4Bn4_7-zhLiVC"
)

// Helper struct for encoding dynamic values
struct AnyEncodable: Encodable {
    private let encode: (Encoder) throws -> Void

    init<T: Encodable>(_ value: T) {
        self.encode = { encoder in try value.encode(to: encoder) }
    }

    func encode(to encoder: Encoder) throws {
        try encode(encoder)
    }
}

// MARK: - Helper Function for Logout
func logout() async {
    try? await supabase.auth.signOut()
}

nonisolated private struct SupabasePersonRow: Codable {
    var id: UUID
    var name: String?
    var theme: String?
    var bio: String?
    var ralliesJoined: Int?
    var rallieNames: [String]?
    var privligeLevel: String?
    var tos: Bool?
    var instaHandle: String?
    var carModel: String?
    var phoneNumber: String?
    
    enum CodingKeys: String, CodingKey {
        case id
        case name
        case theme
        case bio
        case ralliesJoined = "rallies_joined"
        case rallieNames = "rallie_names"
        case privligeLevel = "privilege_level"
        case tos
        case instaHandle = "insta_handle"
        case carModel = "car_model"
        case phoneNumber = "phone_number"
    }
    
    var personInfo: PersonInfo {
        PersonInfo(
            id: id,
            name: name ?? "",
            theme: theme ?? "Default",
            bio: bio ?? "",
            ralliesJoined: ralliesJoined ?? 0,
            rallieNames: rallieNames ?? [],
            privligeLevel: privligeLevel ?? "User",
            tos: tos ?? false,
            instaHandle: instaHandle ?? "",
            carModel: carModel ?? "",
            phoneNumber: phoneNumber ?? ""
        )
    }
}

// MARK: - App Helper Functions
func fetchCurrentProfile() async throws -> PersonInfo? {
    let session = try await supabase.auth.session
    let profile: SupabasePersonRow = try await supabase.from("profiles")
        .select()
        .eq("id", value: session.user.id.uuidString)
        .single()
        .execute()
        .value
    return profile.personInfo
}

nonisolated private struct NewSupabasePersonRow: Encodable {
    var id: UUID
    var name: String
    var theme: String
    var bio: String
    var ralliesJoined: Int
    var rallieNames: [String]
    var privligeLevel: String
    var tos: Bool
    var instaHandle: String? = nil
    var carModel: String? = nil
    var phoneNumber: String? = nil
    
    enum CodingKeys: String, CodingKey {
        case id
        case name
        case theme
        case bio
        case ralliesJoined = "rallies_joined"
        case rallieNames = "rallie_names"
        case privligeLevel = "privilege_level"
        case tos
        case instaHandle = "insta_handle"
        case carModel = "car_model"
        case phoneNumber = "phone_number"
    }
}

nonisolated private struct SupabaseProfileUpdateRow: Encodable {
    var name: String
    var bio: String
    var instaHandle: String
    var carModel: String
    var phoneNumber: String

    enum CodingKeys: String, CodingKey {
        case name
        case bio
        case instaHandle = "insta_handle"
        case carModel = "car_model"
        case phoneNumber = "phone_number"
    }
}

enum AuthResult {
    case success(PersonInfo)
    case failure(String)
}

func login(Email: String, Password: String) async -> AuthResult {
    do {
        let session = try await supabase.auth.signIn(email: Email, password: Password)
        let nameMeta = session.user.userMetadata["name"]?.value as? String ?? Email
        let personInfo = try await loadPersonInfoOrCreateDefault(
            userID: session.user.id,
            name: nameMeta
        )
        return .success(personInfo)
    } catch {
        print("Login failed: \(error)")
        return .failure(authErrorMessage(for: error, fallback: "Login failed. Please try again."))
    }
}

func signup(Name: String, Email: String, Password: String) async -> AuthResult {
    do {
        let authResponse = try await supabase.auth.signUp(
            email: Email,
            password: Password,
            data: ["name": .string(Name)]
        )
        
        let newPerson = NewSupabasePersonRow(
            id: authResponse.user.id,
            name: Name,
            theme: "Dark",
            bio: "",
            ralliesJoined: 0,
            rallieNames: [],
            privligeLevel: "User",
            tos: false
        )
        
        try await supabase.from("profiles")
            .upsert(newPerson, onConflict: "id")
            .execute()
        
        let personInfo = try await loadPersonInfo(for: authResponse.user.id)
        return .success(personInfo)
    } catch {
        print("Signup failed: \(error)")
        return .failure(authErrorMessage(for: error, fallback: "Signup failed. Please try again."))
    }
}

private func loadPersonInfo(for userID: UUID) async throws -> PersonInfo {
    let personRow: SupabasePersonRow = try await supabase.from("profiles")
        .select()
        .eq("id", value: userID.uuidString)
        .single()
        .execute()
        .value
    
    return personRow.personInfo
}

private func loadPersonInfoOrCreateDefault(userID: UUID, name: String) async throws -> PersonInfo {
    do {
        return try await loadPersonInfo(for: userID)
    } catch let error as PostgrestError where error.code == "PGRST116" {
        let newPerson = NewSupabasePersonRow(
            id: userID,
            name: name,
            theme: "Default",
            bio: "",
            ralliesJoined: 0,
            rallieNames: [],
            privligeLevel: "User",
            tos: false
        )
        
        let insertedPerson: SupabasePersonRow = try await supabase.from("profiles")
            .upsert(newPerson, onConflict: "id")
            .select()
            .single()
            .execute()
            .value
        
        return insertedPerson.personInfo
    }
}

private func authErrorMessage(for error: any Error, fallback: String) -> String {
    if let urlError = error as? URLError {
        switch urlError.code {
        case .cannotConnectToHost, .cannotFindHost, .networkConnectionLost, .notConnectedToInternet, .timedOut:
            return "Could not connect to the server. Check the network or Supabase server."
        case .secureConnectionFailed, .serverCertificateHasBadDate, .serverCertificateUntrusted, .serverCertificateHasUnknownRoot, .serverCertificateNotYetValid:
            return "Could not establish a secure connection to the server. Check the HTTPS certificate and proxy configuration."
        default:
            return urlError.localizedDescription
        }
    }
    
    if let authError = error as? AuthError {
        return authError.localizedDescription
    }
    
    return error.localizedDescription.isEmpty ? fallback : error.localizedDescription
}

func updateProfile(person: PersonInfo) async throws {
    guard !person.isTestUser else { return }

    let updateData = SupabaseProfileUpdateRow(
        name: person.name,
        bio: person.bio,
        instaHandle: person.instaHandle,
        carModel: person.carModel,
        phoneNumber: person.phoneNumber
    )
    
    try await supabase.from("profiles")
        .update(updateData)
        .eq("id", value: person.id.uuidString)
        .execute()
}

func getProfileImageURL(for userID: UUID) throws -> URL {
    return try supabase.storage
        .from("Profile Pictures")
        .getPublicURL(path: userID.uuidString + "/images/profile.jpg")
}

func getRallyImageURL(for name: String) throws -> URL {
    return try supabase.storage
        .from("RallyLogos")
        .getPublicURL(path: name + ".png")
}

// MARK: - Waiver Models

struct Waiver: Codable, Identifiable {
    let id: UUID
    let waiver_name: String
    
    enum CodingKeys: String, CodingKey {
        case id
        case waiver_name
    }
}

// MARK: - Waiver Fetching

func fetchUserWaivers(for userID: UUID) async throws -> [Waiver] {
    let waivers: [Waiver] = try await supabase.from("waivers")
        .select("id, waiver_name, rallies!inner(rally_participants!inner(user_id))")
        .eq("rallies.rally_participants.user_id", value: userID.uuidString)
        .execute()
        .value
        
    return waivers
}

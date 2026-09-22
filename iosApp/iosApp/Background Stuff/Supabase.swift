//
//  Supabase.swift
//  NAS Rally
//

import Foundation
import Supabase

let supabase = SupabaseClient(
    supabaseURL: URL(string: "https://api-nas-rally.mayflower-paradise.us")!,
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
        case privligeLevel = "privilege_level"
        case tos
        case instaHandle = "insta_handle"
        case carModel = "car_model"
        case phoneNumber = "phone_number"
    }
    
    func toPersonInfo(ralliesJoined: Int = 0, rallieNames: [String] = []) -> PersonInfo {
        PersonInfo(
            id: id,
            name: name ?? "",
            theme: theme ?? "Auto",
            bio: bio ?? "",
            ralliesJoined: ralliesJoined,
            rallieNames: rallieNames,
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
    return try await loadPersonInfo(for: session.user.id)
}

nonisolated private struct NewSupabasePersonRow: Encodable {
    var id: UUID
    var name: String
    var theme: String
    var bio: String
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

struct SupabaseThemeUpdateRow: Encodable {
    let theme: String
}

func updateTheme(personID: UUID, theme: String) async {
    guard personID != PersonInfo.testUserID else { return }
    do {
        try await supabase.from("profiles")
            .update(SupabaseThemeUpdateRow(theme: theme))
            .eq("id", value: personID.uuidString)
            .execute()
    } catch {
        print("Error updating theme: \(error)")
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
            theme: "Auto",
            bio: "",
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
    
    struct ParticipantRow: Codable {
        let rally_id: UUID
    }
    
    let participantRows: [ParticipantRow] = (try? await supabase.from("rally_participants")
        .select("rally_id")
        .eq("user_id", value: userID.uuidString)
        .execute()
        .value) ?? []
    
    let rallyIDs = participantRows.map { $0.rally_id }
    
    var rallieNames: [String] = []
    if !rallyIDs.isEmpty {
        struct RallyNameRow: Codable {
            let id: UUID
            let name: String
        }
        let rallyIDStrings = rallyIDs.map { $0.uuidString }
        let rallyRows: [RallyNameRow] = (try? await supabase.from("rallies")
            .select("id, name")
            .in("id", values: rallyIDStrings)
            .execute()
            .value) ?? []
        rallieNames = rallyRows.map { $0.name }
    }
    
    return personRow.toPersonInfo(ralliesJoined: rallieNames.count, rallieNames: rallieNames)
}

private func loadPersonInfoOrCreateDefault(userID: UUID, name: String) async throws -> PersonInfo {
    do {
        return try await loadPersonInfo(for: userID)
    } catch let error as PostgrestError where error.code == "PGRST116" {
        let newPerson = NewSupabasePersonRow(
            id: userID,
            name: name,
            theme: "Auto",
            bio: "",
            privligeLevel: "User",
            tos: false
        )
        
        try await supabase.from("profiles")
            .upsert(newPerson, onConflict: "id")
            .execute()
        
        return try await loadPersonInfo(for: userID)
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

func fetchUserIDImageData(for userID: UUID) async throws -> Data {
    return try await supabase.storage
        .from("User-IDs")
        .download(path: userID.uuidString + "/userid.png")
}

func getRallyImageURL(for name: String) throws -> URL {
    return try supabase.storage
        .from("RallyLogos")
        .getPublicURL(path: name + ".png")
}

// MARK: - Waiver Models

struct WaiverRallyInfo: Codable {
    let name: String
}

struct Waiver: Codable, Identifiable {
    let id: UUID
    let waiver_name: String
    let waiver_content: String?
    let rallies: WaiverRallyInfo?
    
    enum CodingKeys: String, CodingKey {
        case id
        case waiver_name
        case waiver_content
        case rallies
    }
}

struct SignedWaiverRow: Codable {
    let waiver_id: UUID
    let user_id: UUID
}

struct UserWaiversResult {
    let pending: [Waiver]
    let signed: [Waiver]
}

// MARK: - Waiver Fetching

func fetchUserWaivers(for userID: UUID) async throws -> UserWaiversResult {
    let allWaivers: [Waiver] = try await supabase.from("waivers")
        .select("id, waiver_name, waiver_content, rallies!inner(name, rally_participants!inner(user_id))")
        .eq("rallies.rally_participants.user_id", value: userID.uuidString)
        .execute()
        .value
        
    let signedRows: [SignedWaiverRow] = (try? await supabase.from("signed_waivers")
        .select("waiver_id, user_id")
        .eq("user_id", value: userID.uuidString)
        .execute()
        .value) ?? []
        
    let signedSet = Set(signedRows.map { $0.waiver_id })
    
    let pending = allWaivers.filter { !signedSet.contains($0.id) }
    let signed = allWaivers.filter { signedSet.contains($0.id) }
    
    return UserWaiversResult(pending: pending, signed: signed)
}

// MARK: - AI Summarization

struct AISummaryResponse: Codable {
    let summary: String?
    let error: String?
}

func fetchAISummary() async throws -> String {
    // 1. Get current session to pass token
    let session = try await supabase.auth.session
    let accessToken = session.accessToken
    
    // 2. Build Request
    guard let url = URL(string: "https://api-nas-rally.mayflower-paradise.us/functions/v1/ai-summarization") else {
        throw URLError(.badURL)
    }
    
    var request = URLRequest(url: url)
    request.httpMethod = "POST"
    request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
    request.setValue("application/json", forHTTPHeaderField: "Content-Type")
    
    // 3. Send Request
    let (data, response) = try await URLSession.shared.data(for: request)
    
    guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
        let statusCode = (response as? HTTPURLResponse)?.statusCode ?? -1
        let responseString = String(data: data, encoding: .utf8) ?? "Unable to decode response string"
        print("Edge Function Error - Status: \(statusCode), Body: \(responseString)")
        
        if let errorResponse = try? JSONDecoder().decode(AISummaryResponse.self, from: data), let errorMessage = errorResponse.error {
             throw NSError(domain: "SupabaseEdgeFunction", code: 1, userInfo: [NSLocalizedDescriptionKey: errorMessage])
        }
        throw URLError(.badServerResponse)
    }
    
    let result = try JSONDecoder().decode(AISummaryResponse.self, from: data)
    
    guard let summary = result.summary else {
        throw NSError(domain: "SupabaseEdgeFunction", code: 2, userInfo: [NSLocalizedDescriptionKey: "No summary returned"])
    }
    
    return summary
}


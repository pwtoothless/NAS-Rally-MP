//
//  Objects.swift
//  NAS Rally
//

import Foundation

struct PersonInfo {
    var id: UUID
    var name: String
    var theme: String
    var bio: String
    var ralliesJoined: Int
    var rallieNames: [String]
    var privligeLevel: String
    var tos: Bool
    var instaHandle: String
    var carModel: String
    var phoneNumber: String
}

extension PersonInfo {
    static let testUserID = UUID(uuid: (0x54, 0x45, 0x53, 0x54, 0x55, 0x53, 0x45, 0x52, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01))

    static var testUser: PersonInfo {
        PersonInfo(
            id: testUserID,
            name: "Test User",
            theme: "Default",
            bio: "Local development profile",
            ralliesJoined: 0,
            rallieNames: [],
            privligeLevel: "User",
            tos: true,
            instaHandle: "",
            carModel: "",
            phoneNumber: ""
        )
    }

    var isTestUser: Bool {
        id == Self.testUserID
    }
}

struct SensitiveInfoRow: Codable {
    var id: UUID
    var ccn: Int
    var cvv: Int
    var exp: String
    var name: String
}

struct CalendarEvent {
    var eventName: String
    var eventDate: String
    var eventTime: String
    var eventLocation: String
    var eventDescription: String
    var eventImage: String
    var peopleGoing: Int
}

func saveCalendarEvent(_ event: CalendarEvent) {
    // Will take in event with info, then format to an event making call
}

struct Message: Codable, Identifiable, Equatable {
    let id: UUID
    let groupId: UUID
    let senderId: UUID
    let content: String
    let createdAt: Date
    
    enum CodingKeys: String, CodingKey {
        case id, content
        case groupId = "group_id"
        case senderId = "sender_id"
        case createdAt = "created_at"
    }
}

struct ReadReceipt: Codable {
    let messageId: UUID
    let userId: UUID
    let readAt: Date
    
    enum CodingKeys: String, CodingKey {
        case messageId = "message_id"
        case userId = "user_id"
        case readAt = "read_at"
    }
}

struct RallyRow: Codable, Identifiable {
    let id: UUID
    let name: String
}

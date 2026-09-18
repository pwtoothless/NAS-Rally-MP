import Foundation
import SwiftUI

extension Color {
    init(hexString: String) {
        let hex = hexString.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }

        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}

extension PersonInfo {
    var themeColor: Color {
        switch theme {
        case "Blue":
            return Color(hexString: "#1E54B3")
        case "Red":
            return Color(hexString: "#C11326")
        default:
            return Color.blue
        }
    }
}

struct PersonInfo {
    var id: UUID
    var name: String
    var theme: String
    var bio: String
    var ralliesJoined: Int = 0
    var rallieNames: [String] = []
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
            theme: "Auto",
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
    let description: String?
    let eventStart: String?
    let eventEnd: String?
    let eventImage: String?
    let eventCost: Double?
    
    enum CodingKeys: String, CodingKey {
        case id
        case name
        case description
        case eventStart = "event_start"
        case eventEnd = "event_end"
        case eventImage = "event_image"
        case eventCost = "event_cost"
    }
}

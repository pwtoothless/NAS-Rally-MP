import SwiftUI
import Supabase
import Combine

@MainActor
class ChatViewModel: ObservableObject {
    @Published var messages: [Message] = []
    @Published var groupMembers: [PersonInfo] = []
    @Published var profilesMap: [UUID: PersonInfo] = [:]
    @Published var readReceipts: [UUID: [ReadReceipt]] = [:]
    @Published var isLoading = false
    
    private let client: SupabaseClient
    let groupId: UUID
    private let pageSize = 50
    private var currentPage = 0
    private var canLoadMore = true
    private var channel: RealtimeChannelV2?
    private var readChannel: RealtimeChannelV2?
    private var cacheKey: String { "chat_messages_\(groupId.uuidString)" }
    
    private let decoder: JSONDecoder = {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return decoder
    }()
    
    private let encoder: JSONEncoder = {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        return encoder
    }()
    
    init(client: SupabaseClient, groupId: UUID) {
        self.client = client
        self.groupId = groupId
        loadCachedMessages()
        setupRealtimeSubscription()
        Task {
            await loadGroupMembers()
        }
    }
    
    private func loadCachedMessages() {
        if let data = UserDefaults.standard.data(forKey: cacheKey),
           let cached = try? decoder.decode([Message].self, from: data), !cached.isEmpty {
            self.messages = Array(cached.prefix(50))
        }
    }
    
    private func saveCachedMessages() {
        let top50 = Array(self.messages.prefix(50))
        if let data = try? encoder.encode(top50) {
            UserDefaults.standard.set(data, forKey: cacheKey)
        }
    }
    
    func loadGroupMembers() async {
        do {
            let memberRows: [GroupMemberRow] = try await client.from("group_members")
                .select()
                .eq("group_id", value: groupId.uuidString)
                .execute()
                .value
            
            let userIDs = memberRows.map { $0.userId }
            guard !userIDs.isEmpty else { return }
            
            let userIDStrings = userIDs.map { $0.uuidString }
            
            struct ProfileRow: Codable {
                let id: UUID
                let name: String?
                let theme: String?
                let bio: String?
                let privilege_level: String?
                let tos: Bool?
                let insta_handle: String?
                let car_model: String?
                let phone_number: String?
                
                func toPersonInfo() -> PersonInfo {
                    PersonInfo(
                        id: id,
                        name: name ?? "",
                        theme: theme ?? "Auto",
                        bio: bio ?? "",
                        ralliesJoined: 0,
                        rallieNames: [],
                        privligeLevel: privilege_level ?? "User",
                        tos: tos ?? false,
                        instaHandle: insta_handle ?? "",
                        carModel: car_model ?? "",
                        phoneNumber: phone_number ?? ""
                    )
                }
            }
            
            let profileRows: [ProfileRow] = try await client.from("profiles")
                .select()
                .in("id", values: userIDStrings)
                .execute()
                .value
            
            let membersList = profileRows.map { $0.toPersonInfo() }
            self.groupMembers = membersList
            var map: [UUID: PersonInfo] = [:]
            for member in membersList {
                map[member.id] = member
            }
            self.profilesMap = map
        } catch {
            print("Error loading group members: \(error)")
        }
    }
    
    private func setupRealtimeSubscription() {
        let channel = client.channel("chat:\(groupId.uuidString)")
        
        let subscription = channel.postgresChange(
            InsertAction.self,
            schema: "public",
            table: "messages",
            filter: .eq("group_id", value: groupId.uuidString)
        )
        
        Task {
            for await action in subscription {
                do {
                    let newMessage = try action.decodeRecord(as: Message.self, decoder: self.decoder)
                    await MainActor.run {
                        if !self.messages.contains(where: { $0.id == newMessage.id }) {
                            self.messages.insert(newMessage, at: 0)
                            self.saveCachedMessages()
                        }
                    }
                } catch { print("Realtime message decode error: \(error)") }
            }
        }
        
        Task { try? await channel.subscribeWithError() }
        self.channel = channel
        
        let readChannel = client.channel("read_receipts:\(groupId.uuidString)")
        let readSubscription = readChannel.postgresChange(
            InsertAction.self,
            schema: "public",
            table: "read_receipts"
        )
        
        Task {
            for await action in readSubscription {
                do {
                    let receipt = try action.decodeRecord(as: ReadReceipt.self, decoder: self.decoder)
                    await MainActor.run {
                        var list = self.readReceipts[receipt.messageId] ?? []
                        if !list.contains(where: { $0.userId == receipt.userId }) {
                            list.append(receipt)
                            self.readReceipts[receipt.messageId] = list
                        }
                    }
                } catch { print("Realtime read receipt decode error: \(error)") }
            }
        }
        
        Task { try? await readChannel.subscribeWithError() }
        self.readChannel = readChannel
    }

    func loadMessages(isRefresh: Bool = false, currentUserId: UUID? = nil) async {
        guard !isLoading && (canLoadMore || isRefresh) else { return }
        isLoading = true
        if isRefresh { currentPage = 0; canLoadMore = true }
        
        let from = currentPage * pageSize
        let to = from + pageSize - 1
        
        do {
            let fetchedMessages: [Message] = try await client.from("messages")
                .select()
                .eq("group_id", value: groupId)
                .order("created_at", ascending: false)
                .range(from: from, to: to)
                .execute()
                .value
            
            if fetchedMessages.count < pageSize { canLoadMore = false }
            
            await MainActor.run {
                if isRefresh {
                    self.messages = fetchedMessages
                } else {
                    let existingIds = Set(self.messages.map { $0.id })
                    let newUnique = fetchedMessages.filter { !existingIds.contains($0.id) }
                    self.messages.append(contentsOf: newUnique)
                }
                self.saveCachedMessages()
                currentPage += 1
            }
            
            let messageIDs = self.messages.map { $0.id }
            if !messageIDs.isEmpty {
                await loadReadReceipts(messageIds: messageIDs)
            }
            
            if let uid = currentUserId {
                await markMessagesAsRead(currentUserId: uid)
            }
        } catch { print("Fetch error: \(error)") }
        isLoading = false
    }
    
    func loadReadReceipts(messageIds: [UUID]) async {
        guard !messageIds.isEmpty else { return }
        let idStrings = messageIds.map { $0.uuidString }
        do {
            let receipts: [ReadReceipt] = try await client.from("read_receipts")
                .select()
                .in("message_id", values: idStrings)
                .execute()
                .value
            
            var newMap = self.readReceipts
            for receipt in receipts {
                var list = newMap[receipt.messageId] ?? []
                if !list.contains(where: { $0.userId == receipt.userId }) {
                    list.append(receipt)
                }
                newMap[receipt.messageId] = list
            }
            self.readReceipts = newMap
        } catch {
            print("Error loading read receipts: \(error)")
        }
    }
    
    func markMessagesAsRead(currentUserId: UUID) async {
        let unreadIDs = messages.filter { msg in
            let existing = readReceipts[msg.id] ?? []
            return !existing.contains(where: { $0.userId == currentUserId })
        }.map { $0.id }
        
        guard !unreadIDs.isEmpty else { return }
        
        let now = Date()
        var newMap = self.readReceipts
        for msgId in unreadIDs {
            var list = newMap[msgId] ?? []
            if !list.contains(where: { $0.userId == currentUserId }) {
                list.append(ReadReceipt(messageId: msgId, userId: currentUserId, readAt: now))
            }
            newMap[msgId] = list
        }
        self.readReceipts = newMap
        
        struct ReadReceiptInsertRow: Encodable {
            let message_id: UUID
            let user_id: UUID
            let read_at: Date
            
            enum CodingKeys: String, CodingKey {
                case message_id
                case user_id
                case read_at
            }
        }
        
        let rowsToInsert = unreadIDs.map { ReadReceiptInsertRow(message_id: $0, user_id: currentUserId, read_at: now) }
        
        do {
            try await client.from("read_receipts")
                .upsert(rowsToInsert)
                .execute()
        } catch {
            print("Error marking messages as read in Supabase: \(error)")
        }
    }
    
    func sendMessage(_ content: String, senderId: UUID) async {
        let now = Date()
        let newMessage = Message(
            id: UUID(),
            groupId: groupId,
            senderId: senderId,
            content: content,
            createdAt: now
        )
        
        await MainActor.run {
            self.messages.insert(newMessage, at: 0)
            self.saveCachedMessages()
            var list = self.readReceipts[newMessage.id] ?? []
            list.append(ReadReceipt(messageId: newMessage.id, userId: senderId, readAt: now))
            self.readReceipts[newMessage.id] = list
        }
        
        struct ReadReceiptInsertRow: Encodable {
            let message_id: UUID
            let user_id: UUID
            let read_at: Date
            
            enum CodingKeys: String, CodingKey {
                case message_id
                case user_id
                case read_at
            }
        }
        
        do {
            try await client.from("messages")
                .insert(newMessage)
                .execute()
            
            try await client.from("read_receipts")
                .upsert(ReadReceiptInsertRow(message_id: newMessage.id, user_id: senderId, read_at: now))
                .execute()
        } catch {
            print("Insert Error: \(error)")
            await MainActor.run {
                self.messages.removeAll(where: { $0.id == newMessage.id })
                self.saveCachedMessages()
            }
        }
    }
}

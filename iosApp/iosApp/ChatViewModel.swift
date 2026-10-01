import SwiftUI
import Supabase
import Combine

@MainActor
class ChatViewModel: ObservableObject {
    @Published var messages: [Message] = []
    @Published var isLoading = false
    
    private let client: SupabaseClient
    let groupId: UUID
    private let pageSize = 50
    private var currentPage = 0
    private var canLoadMore = true
    private var channel: RealtimeChannelV2?
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
            for await status in client.realtimeV2.statusChange {
                print("Realtime Status: \(status)")
            }
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
    
    private func setupRealtimeSubscription() {
        let channel = client.channel("chat:\(groupId.uuidString)")
        
        // Listen for new messages
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
                } catch { print("Realtime decode error: \(error)") }
            }
        }
        
        Task { try? await channel.subscribeWithError() }
        self.channel = channel
    }

    func loadMessages(isRefresh: Bool = false) async {
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
        } catch { print("Fetch error: \(error)") }
        isLoading = false
    }
    
    func sendMessage(_ content: String, senderId: UUID) async {
        let newMessage = Message(
            id: UUID(),
            groupId: groupId,
            senderId: senderId,
            content: content,
            createdAt: Date()
        )
        
        // Add locally immediately so UI doesn't look broken
        await MainActor.run {
            self.messages.insert(newMessage, at: 0)
            self.saveCachedMessages()
        }
        
        do {
            try await client.from("messages")
                .insert(newMessage)
                .execute()
        } catch {
            print("Insert Error: \(error)")
            // Remove locally if it fails
            await MainActor.run {
                self.messages.removeAll(where: { $0.id == newMessage.id })
                self.saveCachedMessages()
            }
        }
    }
}

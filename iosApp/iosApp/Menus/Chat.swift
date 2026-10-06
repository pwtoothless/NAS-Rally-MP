//
//  Chat.swift
//  NAS Rally
//

import SwiftUI
import Supabase

struct ChatView: View {
    @Binding var person: PersonInfo
    @State private var availableRallies: [RallyRow] = []
    
    struct RallyRow: Codable, Identifiable {
        let id: UUID
        let name: String
    }
    
    var body: some View {
        NavigationStack {
            List(availableRallies) { rally in
                NavigationLink(destination: MessageThreadView(
                    person: $person,
                    viewModel: ChatViewModel(client: supabase, groupId: rally.id),
                    groupName: rally.name
                )) {
                    HStack(spacing: 12) {
                        CachedRallyLogoView(name: rally.name)
                            .scaledToFill()
                            .frame(width: 48, height: 48)
                            .clipShape(Circle())
                        
                        Text(rally.name)
                            .font(.body)
                            .fontWeight(.medium)
                    }
                    .padding(.vertical, 4)
                }
            }
            .navigationTitle("Messages")
            .task {
                await fetchJoinedRallies()
            }
        }
    }
    
    private func fetchJoinedRallies() async {
        do {
            let rows: [RallyRow] = try await supabase.from("groups")
                .select("id, name, group_members!inner(user_id)")
                .eq("group_members.user_id", value: person.id.uuidString)
                .execute()
                .value
            
            self.availableRallies = rows
        } catch {
            print("Error loading joined groups: \(error)")
        }
    }
}

struct MessageThreadView: View {
    @Environment(\.dismiss) private var dismiss
    @Binding var person: PersonInfo
    @StateObject var viewModel: ChatViewModel
    @State private var messageInput: String = ""
    @FocusState private var isInputFocused: Bool
    @State private var selectedProfile: PersonInfo? = nil
    @State private var selectedMessageForReceipts: Message? = nil
    let groupName: String
    
    init(person: Binding<PersonInfo>, viewModel: ChatViewModel, groupName: String) {
        self._person = person
        self._viewModel = StateObject(wrappedValue: viewModel)
        self.groupName = groupName
    }
    
    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(viewModel.messages.reversed()) { message in
                        let senderProfile = viewModel.profilesMap[message.senderId]
                        let receipts = viewModel.readReceipts[message.id] ?? []
                        let totalMembers = viewModel.groupMembers.count
                        
                        MessageBubble(
                            message: message,
                            isCurrentUser: message.senderId == person.id,
                            senderProfile: senderProfile,
                            receipts: receipts,
                            totalMembers: totalMembers,
                            themeColor: person.themeColor,
                            onAvatarTap: {
                                selectedProfile = senderProfile ?? PersonInfo(
                                    id: message.senderId,
                                    name: "User",
                                    theme: "Auto",
                                    bio: "",
                                    privligeLevel: "User",
                                    tos: true,
                                    instaHandle: "",
                                    carModel: "",
                                    phoneNumber: ""
                                )
                            },
                            onReceiptTap: {
                                selectedMessageForReceipts = message
                            }
                        )
                        .id(message.id)
                    }
                    
                    // Bottom anchor for auto-scrolling
                    Color.clear
                        .frame(height: 1)
                        .id("bottom")
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
            }
            .scrollDismissesKeyboard(.interactively)
            .defaultScrollAnchor(.bottom)
            .refreshable {
                await viewModel.loadMessages(isRefresh: false, currentUserId: person.id)
            }
            .safeAreaInset(edge: .top) {
                messagesHeaderView
            }
            .safeAreaInset(edge: .bottom) {
                inputBarView
            }
            .onChange(of: viewModel.messages.count) {
                Task {
                    await viewModel.markMessagesAsRead(currentUserId: person.id)
                }
                withAnimation(.easeOut(duration: 0.25)) {
                    proxy.scrollTo("bottom", anchor: .bottom)
                }
            }
            .onChange(of: isInputFocused) {
                if isInputFocused {
                    Task {
                        try? await Task.sleep(nanoseconds: 250_000_000)
                        withAnimation(.easeOut(duration: 0.25)) {
                            proxy.scrollTo("bottom", anchor: .bottom)
                        }
                    }
                }
            }
            .onAppear {
                proxy.scrollTo("bottom", anchor: .bottom)
            }
        }
        .task {
            await viewModel.loadMessages(isRefresh: true, currentUserId: person.id)
        }
        .toolbar(.hidden, for: .navigationBar)
        .sheet(item: $selectedProfile) { user in
            UserProfileDetailSheet(user: user.toAdminProfile)
        }
        .sheet(item: $selectedMessageForReceipts) { msg in
            ReadReceiptsSheet(
                message: msg,
                viewModel: viewModel,
                onSelectUser: { p in
                    selectedProfile = p
                }
            )
        }
    }
    
    // MARK: - Glass Top Header View
    private var messagesHeaderView: some View {
        ZStack {
            // Center: Avatar + Group Name Pill
            VStack(spacing: 4) {
                CachedRallyLogoView(name: groupName)
                    .scaledToFit()
                    .frame(width: 50, height: 50)
                    .clipShape(Circle())
                    .overlay(Circle().stroke(Color.white.opacity(0.3), lineWidth: 1))
                    .shadow(color: .black.opacity(0.15), radius: 4, x: 0, y: 2)

                Text(groupName)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.primary)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 4)
                    .background {
                        Color.clear.glassEffectCompat(in: Capsule())
                    }
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 10)
            .background {
                Color.clear.glassEffectCompat(in:  RoundedRectangle(cornerRadius: 18, style: .continuous))
            }
            
            // Left: Glass Back Button
            HStack {
                Button(action: { dismiss() }) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(.primary)
                        .frame(width: 38, height: 38)
                        .background {
                            Color.clear.glassEffectCompat(in: Circle())
                        }
                        .overlay(Circle().stroke(Color.white.opacity(0.2), lineWidth: 0.5))
                }
                .padding(.leading, 16)
                
                Spacer()
            }
        }
        .padding(.vertical, 8)
    }
    
    // MARK: - Glass Input Bar View
    private var inputBarView: some View {
        HStack(alignment: .bottom, spacing: 8) {
            TextField("Message", text: $messageInput, axis: .vertical)
                .font(.body)
                .lineLimit(1...5)
                .focused($isInputFocused)
                .padding(.vertical, 8)
                .padding(.leading, 14)
            
            Button(action: sendMessage) {
                Image(systemName: "arrow.up.circle.fill")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 30, height: 30)
                    .foregroundStyle(
                        messageInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                        ? Color.gray.opacity(0.4)
                        : person.themeColor
                    )
            }
            .disabled(messageInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            .padding(.trailing, 6)
            .padding(.bottom, 4)
        }
        .background {
            Color.clear.glassEffectCompat(in: Capsule())
        }
        .overlay(Capsule().stroke(Color.white.opacity(0.2), lineWidth: 0.5))
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }
    
    private func sendMessage() {
        let trimmed = messageInput.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        
        let contentToSend = trimmed
        messageInput = ""
        
        Task {
            await viewModel.sendMessage(contentToSend, senderId: person.id)
        }
    }
}

struct MessageBubble: View {
    let message: Message
    let isCurrentUser: Bool
    let senderProfile: PersonInfo?
    let receipts: [ReadReceipt]
    let totalMembers: Int
    var themeColor: Color = .blue
    let onAvatarTap: () -> Void
    let onReceiptTap: () -> Void
    
    var firstName: String {
        let name = senderProfile?.name.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if name.isEmpty { return "User" }
        return String(name.split(separator: " ").first ?? "User")
    }
    
    var readCount: Int {
        Set(receipts.map { $0.userId }).count
    }
    
    var body: some View {
        HStack(alignment: .bottom, spacing: 6) {
            if isCurrentUser {
                Spacer(minLength: 30)
            } else {
                // Profile Picture & First Name underneath ONLY for other users
                Button(action: onAvatarTap) {
                    VStack(spacing: 2) {
                        CachedProfileImageView(userID: message.senderId)
                            .frame(width: 28, height: 28)
                            .clipShape(Circle())
                        
                        Text(firstName)
                            .font(.system(size: 9, weight: .medium))
                            .foregroundColor(.secondary)
                            .lineLimit(1)
                    }
                }
                .buttonStyle(.plain)
            }
            
            // Content bubble & Read Receipt indicator
            VStack(alignment: isCurrentUser ? .trailing : .leading, spacing: 4) {
                Text(message.content)
                    .font(.body)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(isCurrentUser ? themeColor : Color(uiColor: .secondarySystemBackground))
                    .foregroundColor(isCurrentUser ? .white : .primary)
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                
                HStack(spacing: 6) {
                    Text(message.createdAt.formatted(date: .omitted, time: .shortened))
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    
                    Button(action: onReceiptTap) {
                        Text("\(readCount)/\(max(totalMembers, 1))")
                            .font(.caption2)
                            .fontWeight(.bold)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(themeColor.opacity(0.15))
                            .foregroundColor(themeColor)
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 4)
            }
            
            if !isCurrentUser {
                Spacer(minLength: 30)
            }
        }
    }
}

struct ReadReceiptsSheet: View {
    @Environment(\.dismiss) private var dismiss
    let message: Message
    @ObservedObject var viewModel: ChatViewModel
    let onSelectUser: (PersonInfo) -> Void
    
    var readReceipts: [ReadReceipt] {
        viewModel.readReceipts[message.id] ?? []
    }
    
    var readUserIds: Set<UUID> {
        Set(readReceipts.map { $0.userId })
    }
    
    var readMap: [UUID: ReadReceipt] {
        Dictionary(uniqueKeysWithValues: readReceipts.map { ($0.userId, $0) })
    }
    
    var readMembers: [PersonInfo] {
        viewModel.groupMembers.filter { readUserIds.contains($0.id) }
    }
    
    var unreadMembers: [PersonInfo] {
        viewModel.groupMembers.filter { !readUserIds.contains($0.id) }
    }
    
    var body: some View {
        NavigationStack {
            List {
                Section(header: Text("Read Progress")) {
                    HStack {
                        Text("Status")
                        Spacer()
                        Text("\(readMembers.count) of \(viewModel.groupMembers.count) read")
                            .foregroundColor(.secondary)
                    }
                }
                
                if !readMembers.isEmpty {
                    Section(header: Text("Read By (\(readMembers.count))")) {
                        ForEach(readMembers, id: \.id) { member in
                            Button(action: {
                                dismiss()
                                onSelectUser(member)
                            }) {
                                HStack(spacing: 12) {
                                    CachedProfileImageView(userID: member.id)
                                        .frame(width: 40, height: 40)
                                        .clipShape(Circle())
                                    
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(member.name)
                                            .font(.body)
                                            .fontWeight(.medium)
                                            .foregroundColor(.primary)
                                        
                                        if let receipt = readMap[member.id] {
                                            Text(receipt.readAt.formatted(date: .numeric, time: .shortened))
                                                .font(.caption2)
                                                .foregroundColor(.secondary)
                                        }
                                    }
                                    
                                    Spacer()
                                    
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundColor(.blue)
                                }
                            }
                        }
                    }
                }
                
                if !unreadMembers.isEmpty {
                    Section(header: Text("Unread (\(unreadMembers.count))")) {
                        ForEach(unreadMembers, id: \.id) { member in
                            Button(action: {
                                dismiss()
                                onSelectUser(member)
                            }) {
                                HStack(spacing: 12) {
                                    CachedProfileImageView(userID: member.id)
                                        .frame(width: 40, height: 40)
                                        .clipShape(Circle())
                                    
                                    Text(member.name)
                                        .font(.body)
                                        .fontWeight(.medium)
                                        .foregroundColor(.primary)
                                    
                                    Spacer()
                                    
                                    Text("Unread")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("Read Status")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}

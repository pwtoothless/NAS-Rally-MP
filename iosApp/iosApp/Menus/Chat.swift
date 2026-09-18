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
                        AsyncImage(url: try? getRallyImageURL(for: rally.name)) { phase in
                            switch phase {
                            case .success(let image):
                                image
                                    .resizable()
                                    .scaledToFill()
                                    .frame(width: 48, height: 48)
                                    .clipShape(Circle())
                            case .failure(_):
                                Image(systemName: "car.circle.fill")
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 48, height: 48)
                                    .foregroundColor(.gray)
                                    .opacity(0.5)
                            case .empty:
                                ProgressView()
                                    .frame(width: 48, height: 48)
                            @unknown default:
                                ProgressView()
                                    .frame(width: 48, height: 48)
                            }
                        }
                        
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
                        MessageBubble(
                            message: message,
                            isCurrentUser: message.senderId == person.id,
                            themeColor: person.themeColor
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
                await viewModel.loadMessages(isRefresh: false)
            }
            .safeAreaInset(edge: .top) {
                messagesHeaderView
            }
            .safeAreaInset(edge: .bottom) {
                inputBarView
            }
            .onChange(of: viewModel.messages.count) {
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
        .task { await viewModel.loadMessages(isRefresh: true) }
        .toolbar(.hidden, for: .navigationBar)
    }
    
    // MARK: - Glass Top Header View
    private var messagesHeaderView: some View {
        ZStack {
            // Center: Avatar + Group Name Pill
            VStack(spacing: 4) {
                AsyncImage(url: try? getRallyImageURL(for: groupName)) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFit()
                            .frame(width: 50, height: 50)
                            .clipShape(Circle())
                            .overlay(Circle().stroke(Color.white.opacity(0.3), lineWidth: 1))
                            .shadow(color: .black.opacity(0.15), radius: 4, x: 0, y: 2)
                    case .failure(_):
                        Image(systemName: "car.circle.fill")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 44, height: 44)
                            .foregroundColor(.gray)
                    case .empty:
                        ProgressView()
                            .frame(width: 44, height: 44)
                    @unknown default:
                        ProgressView()
                            .frame(width: 44, height: 44)
                    }
                }

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
    var themeColor: Color = .blue
    
    var body: some View {
        HStack {
            if isCurrentUser { Spacer(minLength: 40) }
            
            VStack(alignment: isCurrentUser ? .trailing : .leading, spacing: 3) {
                Text(message.content)
                    .font(.body)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(isCurrentUser ? themeColor : Color(uiColor: .secondarySystemBackground))
                    .foregroundColor(isCurrentUser ? .white : .primary)
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                    .foregroundColor(isCurrentUser ? .white : .primary)
                    .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                
                Text(message.createdAt.formatted(date: .omitted, time: .shortened))
                    .font(.caption2)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 6)
            }
            
            if !isCurrentUser { Spacer(minLength: 40) }
        }
    }
}

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
                    HStack {
                        AsyncImage(url: try? getRallyImageURL(for: rally.name)) { phase in
                            switch phase {
                            case .success(let image):
                                image
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 50, height: 50)
                                    .clipShape(Circle())
                            case .failure(_):
                                Image(systemName: "car.circle.fill")
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 50, height: 50)
                                    .foregroundColor(.gray)
                                    .opacity(0.5)
                                    .clipShape(Circle())
                            case .empty:
                                ProgressView()
                                    .frame(width: 50, height: 50)
                            @unknown default:
                                ProgressView()
                                    .frame(width: 50, height: 50)
                            }
                        }
                        Text(rally.name)
                    }
                }
            }
            .navigationTitle("Chat")
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
    let groupName: String
    
    init(person: Binding<PersonInfo>, viewModel: ChatViewModel, groupName: String) {
        self._person = person
        self._viewModel = StateObject(wrappedValue: viewModel)
        self.groupName = groupName
    }
    
    var body: some View {
        ZStack(alignment: .top) {
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(viewModel.messages.reversed()) { message in
                        MessageBubble(message: message, isCurrentUser: message.senderId == person.id)
                    }
                }
                .padding(.top, 130)
                .padding(.bottom, 150)
                .refreshable {
                    await viewModel.loadMessages(isRefresh: false)
                }
            }
            .scrollDismissesKeyboard(.interactively)
            .defaultScrollAnchor(.bottom)
            .ignoresSafeArea()
            
            // --- CUSTOM FULL-BLEED GLASS HEADER ---
            VStack(spacing: 4) {
                Spacer()
                    .frame(height: 50)
                
                ZStack {
                    HStack {
                        Button(action: { dismiss() }) {
                            HStack(spacing: 6) {
                                Image(systemName: "chevron.left")
                                    .font(.system(size: 16, weight: .bold))
                            }
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
                    
                    VStack(spacing: 2) {
                        AsyncImage(url: try? getRallyImageURL(for: groupName)) { phase in
                            switch phase {
                            case .success(let image):
                                image
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 50, height: 50)
                                    .clipShape(Circle())
                                    .overlay(Circle().stroke(Color.white, lineWidth: 2))
                                    .shadow(radius: 5)
                            case .failure(_):
                                Image(systemName: "car.circle.fill")
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 50, height: 50)
                                    .foregroundColor(.gray)
                                    .opacity(0.5)
                                    .clipShape(Circle())
                            case .empty:
                                ProgressView()
                                    .frame(width: 50, height: 50)
                                    .background(Circle().fill(Color.gray.opacity(0.1)))
                                    .overlay(Circle().stroke(Color.white, lineWidth: 2))
                                    .shadow(radius: 5)
                            @unknown default:
                                ProgressView()
                                    .frame(width: 50, height: 50)
                                    .background(Circle().fill(Color.gray.opacity(0.1)))
                                    .overlay(Circle().stroke(Color.white, lineWidth: 2))
                                    .shadow(radius: 5)
                            }
                        }
                        Text(groupName)
                            .font(.headline)
                            .foregroundStyle(.primary)
                    }
                }
                .padding(.bottom, 12)
            }
            .background {
                Color.clear.glassEffectCompat(.regular, in: Rectangle())
            }
            .overlay(
                VStack {
                    Spacer()
                    Color.white.opacity(0.15).frame(height: 0.5)
                }
            )
            .ignoresSafeArea(edges: .top)
            
            // --- INPUT PILL ---
            VStack {
                Spacer()
                HStack {
                    TextField("Message", text: $messageInput)
                        .font(.subheadline).padding(12)
                    
                    Button(action: {
                        guard !messageInput.trimmingCharacters(in: .whitespaces).isEmpty else { return }
                        
                        let contentToSend = messageInput
                        messageInput = ""
                        
                        Task {
                            await viewModel.sendMessage(contentToSend, senderId: person.id)
                        }
                    }) {
                        Image(systemName: "arrow.up.circle.fill")
                            .font(.title2)
                            .foregroundStyle(messageInput.isEmpty ? .gray : .blue)
                    }
                    .disabled(messageInput.isEmpty)
                    .padding(.trailing, 8)
                }
                .background {
                    Color.clear.glassEffectCompat(in: Capsule())
                }
                .overlay(Capsule().stroke(Color.white.opacity(0.2), lineWidth: 0.5))
                .padding(.horizontal, 16)
                .padding(.bottom, 20)
            }
        }
        .task { await viewModel.loadMessages(isRefresh: true) }
        .toolbar(.hidden, for: .navigationBar)
    }
}

struct MessageBubble: View {
    let message: Message
    let isCurrentUser: Bool
    
    var body: some View {
        HStack {
            if isCurrentUser { Spacer() }
            
            VStack(alignment: isCurrentUser ? .trailing : .leading) {
                Text(message.content)
                    .padding(12)
                    .background(isCurrentUser ? Color.blue : Color.gray.opacity(0.2))
                    .foregroundColor(isCurrentUser ? .white : .primary)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                
                Text(message.createdAt.formatted(date: .omitted, time: .shortened))
                    .font(.caption2)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, 4)
            }
            
            if !isCurrentUser { Spacer() }
        }
        .padding(.horizontal)
    }
}

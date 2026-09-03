//
//  Rallies.swift
//  NAS Rally
//

import SwiftUI
import Supabase

struct DatabaseRally: Codable, Identifiable {
    let id: UUID
    let name: String
    let description: String?
}

struct RalliesView: View {
    @Binding var person: PersonInfo
    @State private var allRallies: [DatabaseRally] = []
    @State private var isLoading = false
    @State private var selectedRally: DatabaseRally? = nil
    
    var body: some View {
        NavigationStack {
            ZStack {
                if person.isTestUser {
                    ContentUnavailableView(
                        "Test User",
                        systemImage: "person.crop.circle.badge.checkmark",
                        description: Text("Rallies are unavailable while using the local test profile.")
                    )
                } else if isLoading && allRallies.isEmpty {
                    VStack(spacing: 12) {
                        ProgressView()
                        Text("Loading rallies...")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxHeight: .infinity)
                } else {
                    List(allRallies) { rally in
                        Button(action: {
                            selectedRally = rally
                        }) {
                            HStack(spacing: 14) {
                                AsyncImage(url: try? getRallyImageURL(for: rally.name)) { phase in
                                    switch phase {
                                    case .success(let image):
                                        image
                                            .resizable()
                                            .scaledToFit()
                                            .frame(width: 50, height: 50)
                                            .clipShape(Circle())
                                            .overlay(Circle().stroke(Color.white.opacity(0.2), lineWidth: 1))
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
                                
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(rally.name)
                                        .font(.headline)
                                        .foregroundColor(.primary)
                                    
                                    let isJoined = person.rallieNames.contains(rally.name)
                                    HStack(spacing: 4) {
                                        Text("Join Status:")
                                            .font(.subheadline)
                                            .foregroundColor(.secondary)
                                        Text(isJoined ? "Joined" : "Not Joined")
                                            .font(.subheadline)
                                            .bold()
                                            .foregroundColor(isJoined ? .green : .red)
                                    }
                                }
                                
                                Spacer()
                                
                                Image(systemName: "chevron.right")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            .padding(.vertical, 4)
                        }
                    }
                    .listStyle(.plain)
                    .refreshable {
                        await fetchAllRallies()
                    }
                }
            }
            .navigationTitle("Rallies")
            .task {
                await fetchAllRallies()
            }
            .sheet(item: $selectedRally) { rally in
                RallyUserDetailSheet(rally: rally, person: $person)
            }
        }
    }
    
    private func fetchAllRallies() async {
        guard !person.isTestUser else { return }

        isLoading = true
        do {
            let rows: [DatabaseRally] = try await supabase.from("rallies")
                .select()
                .execute()
                .value
            self.allRallies = rows
        } catch {
            print("Error loading rallies: \(error)")
        }
        isLoading = false
    }
}

struct RallyUserDetailSheet: View {
    @Environment(\.dismiss) private var dismiss
    let rally: DatabaseRally
    @Binding var person: PersonInfo
    
    @State private var attendeeCount = 0
    @State private var isLoadingCount = false
    @State private var isSendingRequest = false
    @State private var showToast = false
    @State private var toastMessage = ""
    
    var isJoined: Bool {
        person.rallieNames.contains(rally.name)
    }
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                HStack(alignment: .top) {
                    AsyncImage(url: try? getRallyImageURL(for: rally.name)) { phase in
                        switch phase {
                        case .success(let image):
                            image
                                .resizable()
                                .scaledToFit()
                                .frame(width: 90, height: 90)
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                                .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.white.opacity(0.2), lineWidth: 1))
                                .shadow(radius: 4)
                        case .failure(_):
                            Image(systemName: "car.fill")
                                .font(.system(size: 40))
                                .foregroundColor(.gray)
                                .opacity(0.3)
                                .frame(width: 90, height: 90)
                                .background(RoundedRectangle(cornerRadius: 16).fill(Color.gray.opacity(0.1)))
                        case .empty:
                            Image(systemName: "car.fill")
                                .font(.system(size: 40))
                                .foregroundColor(.gray)
                                .opacity(0.3)
                                .frame(width: 90, height: 90)
                                .background(RoundedRectangle(cornerRadius: 16).fill(Color.gray.opacity(0.1)))
                        @unknown default:
                            Image(systemName: "car.fill")
                                .font(.system(size: 40))
                                .foregroundColor(.gray)
                                .opacity(0.3)
                                .frame(width: 90, height: 90)
                                .background(RoundedRectangle(cornerRadius: 16).fill(Color.gray.opacity(0.1)))
                        }
                    }
                    
                    Spacer()
                    
                    Text(rally.name)
                        .font(.title2)
                        .bold()
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: .infinity)
                        .padding(.top, 10)
                    
                    Spacer()
                    
                    VStack(alignment: .center, spacing: 4) {
                        if isLoadingCount {
                            ProgressView()
                                .scaleEffect(0.8)
                        } else {
                            Text("\(attendeeCount)")
                                .font(.title3)
                                .bold()
                                .foregroundColor(.blue)
                            
                            Text("Attending")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(.top, 8)
                    .frame(width: 70)
                }
                .padding(.horizontal)
                .padding(.vertical, 20)
                .background(Color.primary.opacity(0.02))
                
                Divider()
                
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        Text("About the Rally")
                            .font(.headline)
                            .padding(.top, 16)
                        
                        Text(rally.description ?? "Welcome to the \(rally.name) rally! Join us for a thrilling experience filled with automotive adventure, scenic routes, and camaraderie with fellow car enthusiasts. Detailed maps and schedules will be provided upon approval.")
                            .font(.body)
                            .foregroundColor(.secondary)
                            .lineSpacing(4)
                    }
                    .padding(.horizontal)
                }
                
                Spacer()
                
                Button(action: {
                    sendJoinRequest()
                }) {
                    HStack {
                        if isSendingRequest {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                .padding(.trailing, 8)
                        }
                        Text(isJoined ? "Already Joined" : (isSendingRequest ? "Sending Request..." : "Join Event"))
                            .bold()
                            .foregroundColor(.white)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(isJoined ? Color.gray : Color.blue)
                    .cornerRadius(12)
                    .shadow(color: isJoined ? Color.clear : Color.blue.opacity(0.3), radius: 8, x: 0, y: 4)
                }
                .disabled(isJoined || isSendingRequest)
                .padding(.horizontal)
                .padding(.bottom, 24)
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Close") {
                        dismiss()
                    }
                }
            }
            .overlay {
                if showToast {
                    VStack {
                        HStack {
                            Image(systemName: "paperplane.fill")
                                .foregroundColor(.blue)
                            Text(toastMessage)
                                .font(.subheadline)
                                .bold()
                            Spacer()
                        }
                        .padding()
                        .background(RoundedRectangle(cornerRadius: 12).fill(.ultraThinMaterial))
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.blue.opacity(0.3), lineWidth: 1))
                        .padding()
                        .transition(.move(edge: .top).combined(with: .opacity))
                        
                        Spacer()
                    }
                    .animation(.spring(), value: showToast)
                }
            }
            .task {
                await fetchAttendeeCount()
            }
        }
    }
    
    private func fetchAttendeeCount() async {
        isLoadingCount = true
        do {
            let countResponse = try await supabase.from("rally_participants")
                .select("user_id", head: true, count: .exact)
                .eq("rally_id", value: rally.id.uuidString)
                .execute()
            
            self.attendeeCount = countResponse.count ?? 0
        } catch {
            print("Error loading attendee count: \(error)")
        }
        isLoadingCount = false
    }
    
    private func sendJoinRequest() {
        isSendingRequest = true
        
        Task {
            do {
                struct RallyRequestInsert: Encodable {
                    let user_id: UUID
                    let rally_id: UUID
                }
                
                let newRequest = RallyRequestInsert(user_id: person.id, rally_id: rally.id)
                
                try await supabase.from("rally_requests")
                    .insert(newRequest)
                    .execute()
                
                toastMessage = "Join request sent to Admin!"
                showToast = true
                
                try? await Task.sleep(nanoseconds: 2_000_000_000)
                showToast = false
                dismiss()
            } catch {
                print("Failed to send request: \(error)")
                toastMessage = "Failed to send request."
                showToast = true
                try? await Task.sleep(nanoseconds: 2_000_000_000)
                showToast = false
            }
            isSendingRequest = false
        }
    }
}

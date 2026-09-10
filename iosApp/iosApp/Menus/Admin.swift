//
//  Admin.swift
//  NAS Rally
//

import SwiftUI
import Foundation
import Supabase

var penisChance = Int.random(in: 1..<11)
let eggplantEmoji = "🍆"

struct AdminProfile: Codable, Identifiable {
    var id: UUID
    var name: String
    var bio: String
    var privligeLevel: String
    var theme: String?
    var tos: Bool?
    var instaHandle: String?
    var carModel: String?
    var phoneNumber: String?
    
    enum CodingKeys: String, CodingKey {
        case id
        case name
        case bio
        case privligeLevel = "privilege_level"
        case theme
        case tos
        case instaHandle = "insta_handle"
        case carModel = "car_model"
        case phoneNumber = "phone_number"
    }
}

struct RallyRequestRow: Codable, Identifiable {
    let id: UUID
    let user_id: UUID
    let rally_id: UUID
}

struct RallyParticipantRow: Codable {
    let rally_id: UUID
    let user_id: UUID
}

struct AdminView: View {
    @Binding var person: PersonInfo
    @State private var selectedTab = 0
    
    @State private var users: [AdminProfile] = []
    @State private var rallies: [RallyRow] = []
    @State private var participants: [RallyParticipantRow] = []
    @State private var requests: [RallyRequestRow] = []
    @State private var isLoading = false
    
    @State private var selectedRally: RallyRow? = nil
    @State private var selectedUserForRole: AdminProfile? = nil
    @State private var selectedUserForApproval: AdminProfile? = nil
    
    var body: some View {
        VStack(spacing: 0) {
            HStack() {
                Text("Admin Dashboard")
                    .font(.title2)
                    .bold()
                    .padding(.top, 16)
                    .padding(.bottom, 12)
                    .onAppear() {
                        penisChance = Int.random(in: 1..<11)
                    }
                if (penisChance == 1) {
                    Text("\(eggplantEmoji)")
                        .font(.title2)
                        .bold()
                        .padding(.top, 16)
                        .padding(.bottom, 12)
                }
            }
                .onAppear() {
                    print(" penis chance: \(penisChance)")
                }
            
            HStack(spacing: 0) {
                tabButton(title: "Users", index: 0)
                tabButton(title: "Rallies", index: 1)
                tabButton(title: "Roles", index: 2)
                tabButton(title: "Approvals", index: 3)
            }
            .background(Color.primary.opacity(0.05))
            .cornerRadius(12)
            .padding(.horizontal)
            .padding(.bottom, 16)
            
            ZStack {
                if isLoading {
                    VStack(spacing: 12) {
                        ProgressView()
                        Text("Loading dashboard...")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxHeight: .infinity)
                } else {
                    switch selectedTab {
                    case 0:
                        UsersTabView(users: users)
                    case 1:
                        RalliesTabView(rallies: rallies, users: users, participants: participants, selectedRally: $selectedRally)
                    case 2:
                        PermissionsTabView(users: users, selectedUser: $selectedUserForRole)
                    case 3:
                        ApprovalsTabView(requests: requests, users: users, rallies: rallies, selectedUser: $selectedUserForApproval)
                    default:
                        EmptyView()
                    }
                }
            }
        }
        .task {
            await loadAdminData()
        }
        .sheet(item: $selectedRally) { rally in
            RallyDetailSheet(rally: rally, allUsers: users, allParticipants: participants, onUpdate: {
                Task {
                    await loadAdminData()
                }
            })
        }
        .sheet(item: $selectedUserForRole) { user in
            PermissionDetailSheet(user: user, onSave: { updatedLevel in
                Task {
                    await loadAdminData()
                }
            })
        }
        .sheet(item: $selectedUserForApproval) { user in
            ApprovalDetailSheet(
                user: user,
                userRequests: requests.filter { $0.user_id == user.id },
                rallies: rallies,
                onUpdate: {
                    Task { await loadAdminData() }
                }
            )
        }
    }
    
    private func tabButton(title: String, index: Int) -> some View {
        Button(action: {
            withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                selectedTab = index
            }
        }) {
            Text(title)
                .font(.subheadline)
                .bold()
                .padding(.vertical, 10)
                .frame(maxWidth: .infinity)
                .foregroundColor(selectedTab == index ? .white : .primary)
                .background(
                    RoundedRectangle(cornerRadius: 10)
                        .fill(selectedTab == index ? Color.blue : Color.clear)
                )
                .padding(2)
        }
    }
    
    private func loadAdminData() async {
        isLoading = true
        do {
            let fetchedProfiles: [AdminProfile] = try await supabase.from("profiles")
                .select()
                .execute()
                .value
            self.users = fetchedProfiles
            
            let fetchedRallies: [RallyRow] = try await supabase.from("rallies")
                .select()
                .execute()
                .value
            self.rallies = fetchedRallies
            
            let fetchedParticipants: [RallyParticipantRow] = try await supabase.from("rally_participants")
                .select()
                .execute()
                .value
            self.participants = fetchedParticipants
            
            let fetchedRequests: [RallyRequestRow] = try await supabase.from("rally_requests")
                .select()
                .execute()
                .value
            self.requests = fetchedRequests
        } catch {
            print("Error loading admin data: \(error)")
        }
        isLoading = false
    }
}

struct UsersTabView: View {
    let users: [AdminProfile]
    
    var body: some View {
        List(users) { user in
            HStack(spacing: 12) {
                Image(systemName: "person.crop.circle.fill")
                    .font(.title2)
                    .foregroundColor(.secondary)
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(user.name)
                        .font(.headline)
                    if !user.bio.isEmpty {
                        Text(user.bio)
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .lineLimit(1)
                    }
                }
                
                Spacer()
                
                Text(user.privligeLevel)
                    .font(.caption2)
                    .bold()
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(user.privligeLevel == "Admin" ? Color.red.opacity(0.1) : Color.blue.opacity(0.1))
                    .foregroundColor(user.privligeLevel == "Admin" ? .red : .blue)
                    .cornerRadius(8)
            }
            .padding(.vertical, 4)
        }
        .listStyle(.insetGrouped)
    }
}

struct RalliesTabView: View {
    let rallies: [RallyRow]
    let users: [AdminProfile]
    let participants: [RallyParticipantRow]
    @Binding var selectedRally: RallyRow?
    
    var body: some View {
        List(rallies) { rally in
            Button(action: {
                selectedRally = rally
            }) {
                HStack {
                    Image(systemName: "car.2.fill")
                        .foregroundColor(.blue)
                        .font(.title3)
                        .padding(.trailing, 4)
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text(rally.name)
                            .font(.headline)
                            .foregroundColor(.primary)
                        
                        let attendeeCount = participants.filter { $0.rally_id == rally.id }.count
                        Text("\(attendeeCount) participant\(attendeeCount == 1 ? "" : "s")")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    
                    Spacer()
                    
                    Image(systemName: "chevron.right")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding(.vertical, 4)
            }
        }
        .listStyle(.insetGrouped)
    }
}

struct RallyDetailSheet: View {
    @Environment(\.dismiss) private var dismiss
    let rally: RallyRow
    let allUsers: [AdminProfile]
    let allParticipants: [RallyParticipantRow]
    let onUpdate: () -> Void
    
    @State private var showAddPeopleSheet = false
    @State private var selectedUserProfile: AdminProfile? = nil
    @State private var isSaving = false
    @State private var errorMessage = ""
    @State private var successMessage = ""
    @State private var showToast = false
    @State private var isError = false
    
    var attendees: [AdminProfile] {
        let userIdsInRally = allParticipants.filter { $0.rally_id == rally.id }.map { $0.user_id }
        return allUsers.filter { userIdsInRally.contains($0.id) }
    }
    
    var eligibleUsers: [AdminProfile] {
        let userIdsInRally = allParticipants.filter { $0.rally_id == rally.id }.map { $0.user_id }
        return allUsers.filter { !userIdsInRally.contains($0.id) }
    }
    
    var body: some View {
        NavigationStack {
            ZStack {
                VStack(spacing: 0) {
                    VStack(spacing: 12) {
                        AsyncImage(url: try? getRallyImageURL(for: rally.name)) { phase in
                            switch phase {
                            case .success(let image):
                                image
                                    .resizable()
                                    .scaledToFill()
                                    .frame(width: 80, height: 80)
                                    .clipShape(Circle())
                                    .overlay(Circle().stroke(Color.white, lineWidth: 2))
                                    .shadow(radius: 4)
                            case .failure:
                                Image(systemName: "car.fill")
                                    .font(.title)
                                    .foregroundColor(.gray)
                                    .opacity(0.3)
                                    .frame(width: 80, height: 80)
                                    .background(Circle().fill(Color.gray.opacity(0.1)))
                                    .clipShape(Circle())
                                    .overlay(Circle().stroke(Color.white, lineWidth: 2))
                                    .shadow(radius: 4)
                            case .empty:
                                ProgressView()
                                    .frame(width: 80, height: 80)
                                    .background(Circle().fill(Color.gray.opacity(0.1)))
                                    .clipShape(Circle())
                                    .overlay(Circle().stroke(Color.white, lineWidth: 2))
                                    .shadow(radius: 4)
                            @unknown default:
                                ProgressView()
                                    .frame(width: 80, height: 80)
                                    .background(Circle().fill(Color.gray.opacity(0.1)))
                                    .clipShape(Circle())
                                    .overlay(Circle().stroke(Color.white, lineWidth: 2))
                                    .shadow(radius: 4)
                            }
                        }
                        
                        Text(rally.name)
                            .font(.title2)
                            .bold()
                    }
                    .padding(.vertical, 24)
                    .frame(maxWidth: .infinity)
                    .background(Color.primary.opacity(0.02))
                    
                    HStack {
                        Text("Attendees (\(attendees.count))")
                            .font(.headline)
                        Spacer()
                    }
                    .padding(.horizontal)
                    .padding(.top, 16)
                    .padding(.bottom, 8)
                    
                    if attendees.isEmpty {
                        VStack(spacing: 8) {
                            Image(systemName: "person.3.fill")
                                .font(.largeTitle)
                                .foregroundColor(.secondary.opacity(0.5))
                            Text("No participants registered yet.")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        .frame(maxHeight: .infinity)
                    } else {
                        List(attendees) { attendee in
                            HStack {
                                Image(systemName: "person.circle.fill")
                                    .foregroundColor(.secondary)
                                Text(attendee.name)
                                    .font(.body)
                                Spacer()
                                if attendee.privligeLevel == "Admin" {
                                    Text("Admin")
                                        .font(.caption2)
                                        .bold()
                                        .padding(.horizontal, 6)
                                        .padding(.vertical, 2)
                                        .background(Color.red.opacity(0.1))
                                        .foregroundColor(.red)
                                        .cornerRadius(4)
                                }
                            }
                            .contextMenu {
                                Button(action: {
                                    selectedUserProfile = attendee
                                }) {
                                    Label("Profile", systemImage: "person.crop.circle")
                                }
                                
                                Button(role: .destructive, action: {
                                    removeMemberFromRally(userId: attendee.id)
                                }) {
                                    Label("Remove from Rally", systemImage: "trash")
                                }
                            }
                            .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                                Button(role: .destructive) {
                                    removeMemberFromRally(userId: attendee.id)
                                } label: {
                                    Label("Delete", systemImage: "trash")
                                }
                            }
                        }
                        .listStyle(.plain)
                    }
                }
                
                if isSaving {
                    ZStack {
                        Color.black.opacity(0.15)
                            .ignoresSafeArea()
                        ProgressView("Updating participants...")
                            .padding()
                            .background(RoundedRectangle(cornerRadius: 12).fill(.ultraThinMaterial))
                    }
                }
                
                if showToast {
                    VStack {
                        HStack {
                            Image(systemName: isError ? "exclamationmark.triangle.fill" : "checkmark.circle.fill")
                                .foregroundColor(isError ? .red : .green)
                            Text(errorMessage.isEmpty ? successMessage : errorMessage)
                                .font(.subheadline)
                                .bold()
                                .foregroundColor(.primary)
                            Spacer()
                        }
                        .padding()
                        .background(RoundedRectangle(cornerRadius: 12).fill(.ultraThinMaterial))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(isError ? Color.red.opacity(0.3) : Color.green.opacity(0.3), lineWidth: 1)
                        )
                        .padding()
                        .transition(.move(edge: .top).combined(with: .opacity))
                        
                        Spacer()
                    }
                    .animation(.spring(), value: showToast)
                }
            }
            .navigationTitle("Rally Details")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .bottomBar) {
                    HStack {
                        Button(action: {
                            showAddPeopleSheet = true
                        }) {
                            HStack {
                                Image(systemName: "plus.circle.fill")
                                Text("Add People")
                            }
                            .bold()
                        }
                        Spacer()
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Close") {
                        dismiss()
                    }
                }
            }
            .sheet(isPresented: $showAddPeopleSheet) {
                AddPeopleSheet(rally: rally, eligibleUsers: eligibleUsers, onDone: { selectedIds in
                    addMembersToRally(userIds: selectedIds)
                })
            }
            .sheet(item: $selectedUserProfile) { user in
                UserProfileDetailSheet(user: user)
            }
        }
    }
    
    private func removeMemberFromRally(userId: UUID) {
        isSaving = true
        isError = false
        errorMessage = ""
        successMessage = ""
        
        Task {
            do {
                try await supabase.from("rally_participants")
                    .delete()
                    .eq("rally_id", value: rally.id.uuidString)
                    .eq("user_id", value: userId.uuidString)
                    .execute()
                
                struct GroupRow: Decodable {
                    let id: UUID
                }
                
                let groups: [GroupRow] = try await supabase.from("groups")
                    .select("id")
                    .eq("name", value: rally.name)
                    .execute()
                    .value
                
                if let groupId = groups.first?.id {
                    try await supabase.from("group_members")
                        .delete()
                        .eq("group_id", value: groupId.uuidString)
                        .eq("user_id", value: userId.uuidString)
                        .execute()
                }
                
                onUpdate()
                
                successMessage = "Participant removed."
                isError = false
                showToast = true
                
                try? await Task.sleep(nanoseconds: 2_000_000_000)
                showToast = false
            } catch {
                print("Failed to remove member: \(error)")
                errorMessage = "Failed to remove member: \(error.localizedDescription)"
                isError = true
                showToast = true
            }
            isSaving = false
        }
    }
    
    private func addMembersToRally(userIds: [UUID]) {
        isSaving = true
        isError = false
        errorMessage = ""
        successMessage = ""
        
        Task {
            do {
                struct RallyParticipantInsert: Encodable {
                    let user_id: UUID
                    let rally_id: UUID
                }
                
                let rallyInserts = userIds.map { RallyParticipantInsert(user_id: $0, rally_id: rally.id) }
                try await supabase.from("rally_participants")
                    .upsert(rallyInserts, onConflict: "user_id, rally_id")
                    .execute()
                
                struct GroupRow: Decodable {
                    let id: UUID
                }
                
                let groups: [GroupRow] = try await supabase.from("groups")
                    .select("id")
                    .eq("name", value: rally.name)
                    .execute()
                    .value
                
                if let groupId = groups.first?.id {
                    struct GroupMemberInsert: Encodable {
                        let group_id: UUID
                        let user_id: UUID
                    }
                    
                    let groupInserts = userIds.map { GroupMemberInsert(group_id: groupId, user_id: $0) }
                    try await supabase.from("group_members")
                        .upsert(groupInserts, onConflict: "group_id, user_id")
                        .execute()
                }
                
                onUpdate()
                
                successMessage = "Added \(userIds.count) participant\(userIds.count == 1 ? "" : "s") to the rally!"
                isError = false
                showToast = true
                
                try? await Task.sleep(nanoseconds: 2_000_000_000)
                showToast = false
            } catch {
                print("Failed to add members: \(error)")
                errorMessage = "Failed to add members: \(error.localizedDescription)"
                isError = true
                showToast = true
            }
            isSaving = false
        }
    }
}

struct AddPeopleSheet: View {
    @Environment(\.dismiss) private var dismiss
    let rally: RallyRow
    let eligibleUsers: [AdminProfile]
    let onDone: ([UUID]) -> Void
    
    @State private var selectedUserIds: Set<UUID> = []
    
    var body: some View {
        NavigationStack {
            List(eligibleUsers) { user in
                HStack {
                    AsyncImage(url: try? getProfileImageURL(for: user.id)) { phase in
                        switch phase {
                        case .success(let image):
                            image
                                .resizable()
                                .scaledToFill()
                                .frame(width: 40, height: 40)
                                .clipShape(Circle())
                        case .failure(_):
                            Image(systemName: "person.crop.circle.fill")
                                .resizable()
                                .frame(width: 40, height: 40)
                                .foregroundColor(.gray)
                                .opacity(0.5)
                        case .empty:
                            Image(systemName: "person.crop.circle.fill")
                                .resizable()
                                .frame(width: 40, height: 40)
                                .foregroundColor(.gray)
                                .opacity(0.5)
                        @unknown default:
                            Image(systemName: "person.crop.circle.fill")
                                .resizable()
                                .frame(width: 40, height: 40)
                                .foregroundColor(.gray)
                                .opacity(0.5)
                        }
                    }
                    
                    Text(user.name)
                        .font(.body)
                        .padding(.leading, 8)
                    
                    Spacer()
                    
                    Image(systemName: selectedUserIds.contains(user.id) ? "checkmark.circle.fill" : "circle")
                        .font(.title2)
                        .foregroundColor(selectedUserIds.contains(user.id) ? .blue : .secondary)
                }
                .contentShape(Rectangle())
                .onTapGesture {
                    if selectedUserIds.contains(user.id) {
                        selectedUserIds.remove(user.id)
                    } else {
                        selectedUserIds.insert(user.id)
                    }
                }
            }
            .listStyle(.plain)
            .navigationTitle("Select People")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") {
                        onDone(Array(selectedUserIds))
                        dismiss()
                    }
                    .disabled(selectedUserIds.isEmpty)
                }
            }
        }
    }
}

struct PermissionsTabView: View {
    let users: [AdminProfile]
    @Binding var selectedUser: AdminProfile?
    
    var body: some View {
        List(users) { user in
            Button(action: {
                selectedUser = user
            }) {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(user.name)
                            .font(.headline)
                            .foregroundColor(.primary)
                        
                        Text("Role: \(user.privligeLevel)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    
                    Spacer()
                    
                    Image(systemName: "slider.horizontal.3")
                        .foregroundColor(.blue)
                }
                .padding(.vertical, 4)
            }
        }
        .listStyle(.insetGrouped)
    }
}

struct PermissionDetailSheet: View {
    @Environment(\.dismiss) private var dismiss
    let user: AdminProfile
    let onSave: (String) -> Void
    
    @State private var privilegeLevel: String
    
    @State private var isSaving = false
    @State private var showSuccess = false
    @State private var errorMessage = ""
    
    init(user: AdminProfile, onSave: @escaping (String) -> Void) {
        self.user = user
        self.onSave = onSave
        self._privilegeLevel = State(initialValue: user.privligeLevel)
    }
    
    var body: some View {
        NavigationStack {
            Form {
                Section(header: Text("User Info")) {
                    HStack {
                        Text("Name")
                            .bold()
                        Spacer()
                        Text(user.name)
                            .foregroundColor(.secondary)
                    }
                }
                
                Section(header: Text("Database Permissions")) {
                    Picker("Role Level", selection: $privilegeLevel) {
                        Text("User").tag("User")
                        Text("Admin").tag("Admin")
                    }
                    .pickerStyle(.segmented)
                }
                
                if !errorMessage.isEmpty {
                    Section {
                        Text(errorMessage)
                            .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle("Edit Permissions")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button(action: {
                        savePermissions()
                    }) {
                        if isSaving {
                            ProgressView()
                        } else {
                            Text("Save")
                                .bold()
                        }
                    }
                    .disabled(isSaving)
                }
            }
            .alert("Permissions Saved", isPresented: $showSuccess) {
                Button("OK") {
                    dismiss()
                }
            } message: {
                Text("Role updated in database to '\(privilegeLevel)'. Simulated options updated successfully.")
            }
        }
    }
    
    private func savePermissions() {
        isSaving = true
        errorMessage = ""
        
        Task {
            do {
                let updateData: [String: AnyEncodable] = [
                    "privilege_level": AnyEncodable(privilegeLevel)
                ]
                
                try await supabase.from("profiles")
                    .update(updateData)
                    .eq("id", value: user.id.uuidString)
                    .execute()
                
                onSave(privilegeLevel)
                showSuccess = true
            } catch {
                errorMessage = "Failed to update role: \(error.localizedDescription)"
            }
            isSaving = false
        }
    }
}

struct UserProfileDetailSheet: View {
    @Environment(\.dismiss) private var dismiss
    let user: AdminProfile
    @State private var profileImageURL: URL? = nil
    
    var body: some View {
        NavigationStack {
            VStack {
                VStack(spacing: 20) {
                    AsyncImage(url: profileImageURL) { phase in
                        switch phase {
                        case .empty:
                            ProgressView()
                                .frame(width: 100, height: 100)
                        case .success(let image):
                            image
                                .resizable()
                                .scaledToFill()
                                .frame(width: 100, height: 100)
                                .clipShape(Circle())
                        case .failure:
                            Image(systemName: "person.crop.circle.fill")
                                .resizable()
                                .foregroundStyle(.gray)
                                .frame(width: 100, height: 100)
                        @unknown default:
                            EmptyView()
                        }
                    }
                    
                    VStack(spacing: 8) {
                        Text(user.name)
                            .font(.title)
                            .bold()
                        
                        if let handle = user.instaHandle, !handle.isEmpty {
                            Text("@\(handle)")
                                .font(.subheadline)
                                .foregroundColor(.blue)
                        }
                        
                        if let car = user.carModel, !car.isEmpty {
                            Text(car)
                                .font(.subheadline)
                        }
                        
                        if let phone = user.phoneNumber, !phone.isEmpty {
                            Text(phone)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        
                        if !user.bio.isEmpty {
                            Text(user.bio)
                                .font(.body)
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                        }
                        
                        Text(user.privligeLevel)
                            .font(.caption)
                            .bold()
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(user.privligeLevel == "Admin" ? Color.red.opacity(0.1) : Color.blue.opacity(0.1))
                            .foregroundColor(user.privligeLevel == "Admin" ? .red : .blue)
                            .cornerRadius(8)
                            .padding(.top, 4)
                    }
                }
                .padding()
                .frame(maxWidth: .infinity)
                .background(RoundedRectangle(cornerRadius: 24).fill(.ultraThinMaterial))
                .padding(.horizontal)
                .padding(.top, 20)
                
                Spacer()
            }
            .navigationTitle("Profile")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
            .task {
                do {
                    self.profileImageURL = try getProfileImageURL(for: user.id)
                } catch {
                    print("Failed to load profile image URL: \(error)")
                }
            }
        }
    }
}

struct ApprovalsTabView: View {
    let requests: [RallyRequestRow]
    let users: [AdminProfile]
    let rallies: [RallyRow]
    @Binding var selectedUser: AdminProfile?
    
    var body: some View {
        let userIdsWithRequests = Set(requests.map { $0.user_id })
        let pendingUsers = users.filter { userIdsWithRequests.contains($0.id) }
        
        if pendingUsers.isEmpty {
            Text("No pending requests.")
                .foregroundColor(.secondary)
                .frame(maxHeight: .infinity)
        } else {
            List(pendingUsers) { user in
                Button(action: {
                    selectedUser = user
                }) {
                    HStack {
                        Image(systemName: "person.crop.circle.badge.questionmark")
                            .foregroundColor(.orange)
                        Text(user.name)
                            .font(.headline)
                            .foregroundColor(.primary)
                        Spacer()
                        let requestCount = requests.filter { $0.user_id == user.id }.count
                        Text("\(requestCount) Request\(requestCount == 1 ? "" : "s")")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Image(systemName: "chevron.right")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding(.vertical, 4)
                }
            }
            .listStyle(.insetGrouped)
        }
    }
}

struct ApprovalDetailSheet: View {
    @Environment(\.dismiss) private var dismiss
    let user: AdminProfile
    let userRequests: [RallyRequestRow]
    let rallies: [RallyRow]
    let onUpdate: () -> Void
    
    @State private var showProfile = false
    @State private var showID = false
    @State private var isProcessing = false
    @State private var processingRequestIds: Set<UUID> = []
    
    var body: some View {
        NavigationStack {
            VStack {
                List(userRequests) { request in
                    if let rally = rallies.first(where: { $0.id == request.rally_id }) {
                        HStack {
                            Image(systemName: "car.circle.fill")
                                .foregroundColor(.blue)
                            Text(rally.name)
                                .font(.headline)
                            
                            Spacer()
                            
                            if processingRequestIds.contains(request.id) || isProcessing {
                                ProgressView()
                                    .padding(.trailing, 8)
                            } else {
                                Button("Decline") {
                                    declineRequest(request: request)
                                }
                                .buttonStyle(.bordered)
                                .tint(.red)
                                
                                Button("Approve") {
                                    approveIndividualRequest(request: request, rally: rally)
                                }
                                .buttonStyle(.borderedProminent)
                                .tint(.blue)
                            }
                        }
                    }
                }
                
                Spacer()
                
                HStack(spacing: 12) {
                    Button(action: { showProfile = true }) {
                        VStack {
                            Image(systemName: "person.crop.circle")
                            Text("View Profile")
                                .font(.caption)
                        }
                        .padding(.vertical, 8)
                        .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.blue)
                    
                    Button(action: { showID = true }) {
                        VStack {
                            Image(systemName: "person.text.rectangle")
                            Text("Show ID")
                                .font(.caption)
                        }
                        .padding(.vertical, 8)
                        .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.blue)
                    
                    Button(action: { approveAllRequests() }) {
                        VStack {
                            Image(systemName: "checkmark.circle.fill")
                            Text("Approve All")
                                .font(.caption)
                        }
                        .padding(.vertical, 8)
                        .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.blue)
                    .disabled(isProcessing || userRequests.isEmpty)
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 20)
            }
            .navigationTitle("\(user.name)'s Requests")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Close") { dismiss() }
                }
            }
            .sheet(isPresented: $showProfile) {
                UserProfileDetailSheet(user: user)
            }
            .sheet(isPresented: $showID) {
                MockIDSheet()
            }
        }
    }
    
    private func declineRequest(request: RallyRequestRow) {
        processingRequestIds.insert(request.id)
        Task {
            do {
                try await supabase.from("rally_requests")
                    .delete()
                    .eq("id", value: request.id.uuidString)
                    .execute()
                
                onUpdate()
                if userRequests.count <= 1 {
                    dismiss()
                }
            } catch {
                print("Failed to decline request: \(error)")
            }
            processingRequestIds.remove(request.id)
        }
    }
    
    private func approveIndividualRequest(request: RallyRequestRow, rally: RallyRow) {
        processingRequestIds.insert(request.id)
        Task {
            do {
                struct RallyParticipantInsert: Encodable {
                    let user_id: UUID
                    let rally_id: UUID
                }
                let participant = RallyParticipantInsert(user_id: user.id, rally_id: rally.id)
                try await supabase.from("rally_participants")
                    .upsert(participant, onConflict: "user_id, rally_id")
                    .execute()
                
                struct GroupRow: Decodable { let id: UUID }
                let groups: [GroupRow] = try await supabase.from("groups")
                    .select("id")
                    .eq("name", value: rally.name)
                    .execute()
                    .value
                
                if let groupId = groups.first?.id {
                    struct GroupMemberInsert: Encodable {
                        let group_id: UUID
                        let user_id: UUID
                    }
                    let groupMember = GroupMemberInsert(group_id: groupId, user_id: user.id)
                    try await supabase.from("group_members")
                        .upsert(groupMember, onConflict: "group_id, user_id")
                        .execute()
                }
                
                try await supabase.from("rally_requests")
                    .delete()
                    .eq("id", value: request.id.uuidString)
                    .execute()
                
                onUpdate()
                if userRequests.count <= 1 {
                    dismiss()
                }
            } catch {
                print("Failed to approve individual request: \(error)")
            }
            processingRequestIds.remove(request.id)
        }
    }
    
    private func approveAllRequests() {
        isProcessing = true
        Task {
            for request in userRequests {
                if let rally = rallies.first(where: { $0.id == request.rally_id }) {
                    do {
                        struct RallyParticipantInsert: Encodable {
                            let user_id: UUID
                            let rally_id: UUID
                        }
                        let participant = RallyParticipantInsert(user_id: user.id, rally_id: rally.id)
                        try await supabase.from("rally_participants")
                            .upsert(participant, onConflict: "user_id, rally_id")
                            .execute()
                        
                        struct GroupRow: Decodable { let id: UUID }
                        let groups: [GroupRow] = try await supabase.from("groups")
                            .select("id")
                            .eq("name", value: rally.name)
                            .execute()
                            .value
                        
                        if let groupId = groups.first?.id {
                            struct GroupMemberInsert: Encodable {
                                let group_id: UUID
                                let user_id: UUID
                            }
                            let groupMember = GroupMemberInsert(group_id: groupId, user_id: user.id)
                            try await supabase.from("group_members")
                                .upsert(groupMember, onConflict: "group_id, user_id")
                                .execute()
                        }
                        
                        try await supabase.from("rally_requests")
                            .delete()
                            .eq("id", value: request.id.uuidString)
                            .execute()
                    } catch {
                        print("Failed to approve request \(request.id): \(error)")
                    }
                }
            }
            
            onUpdate()
            dismiss()
            isProcessing = false
        }
    }
}

struct MockIDSheet: View {
    @Environment(\.dismiss) private var dismiss
    
    var body: some View {
        NavigationStack {
            VStack {
                Image(systemName: "person.text.rectangle")
                    .font(.system(size: 100))
                    .foregroundColor(.blue)
                    .padding()
                
                Text("Submitted ID")
                    .font(.title)
                    .bold()
                
                Text("Verification image would load here.")
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.primary.opacity(0.05))
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}

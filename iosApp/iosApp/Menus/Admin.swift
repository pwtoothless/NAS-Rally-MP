//
//  Admin.swift
//  NAS Rally
//

import SwiftUI
import Foundation
import Supabase
import PhotosUI

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
    @State private var editingRally: RallyRow? = nil
    @State private var selectedUserForRole: AdminProfile? = nil
    @State private var selectedUserForApproval: AdminProfile? = nil
    @State private var showCreateRallySheet = false
    
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
                Spacer()
            }
            .padding(.horizontal, 16)
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
                        RalliesTabView(
                            rallies: rallies,
                            users: users,
                            participants: participants,
                            selectedRally: $selectedRally,
                            editingRally: $editingRally,
                            onDeleteRally: { rally in
                                Task {
                                    await deleteRally(rally)
                                }
                            },
                            onCreateRally: { showCreateRallySheet = true }
                        )
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
        .sheet(isPresented: $showCreateRallySheet) {
            CreateRallySheet(adminPerson: person, onCreated: {
                Task {
                    await loadAdminData()
                }
            })
        }
        .sheet(item: $selectedRally) { rally in
            RallyDetailSheet(rally: rally, allUsers: users, allParticipants: participants, onUpdate: {
                Task {
                    await loadAdminData()
                }
            })
        }
        .sheet(item: $editingRally) { rally in
            EditRallySheet(rally: rally, onUpdated: {
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
    
    private func deleteRally(_ rally: RallyRow) async {
        isLoading = true
        let idStr = rally.id.uuidString
        
        // 1. Fetch and delete any signed waivers referencing waivers for this rally
        struct WaiverIdRow: Decodable {
            let id: UUID
        }
        let waiverRows: [WaiverIdRow] = (try? await supabase.from("waivers")
            .select("id")
            .eq("rally_id", value: idStr)
            .execute()
            .value) ?? []
        
        for w in waiverRows {
            try? await supabase.from("signed_waivers")
                .delete()
                .eq("waiver_id", value: w.id.uuidString)
                .execute()
        }
        
        // 2. Delete waivers for this rally
        try? await supabase.from("waivers")
            .delete()
            .eq("rally_id", value: idStr)
            .execute()
        
        // 3. Delete chat messages for this rally's group
        try? await supabase.from("messages")
            .delete()
            .eq("group_id", value: idStr)
            .execute()
        
        // 4. Delete group members
        try? await supabase.from("group_members")
            .delete()
            .eq("group_id", value: idStr)
            .execute()
        
        // 5. Delete rally requests
        try? await supabase.from("rally_requests")
            .delete()
            .eq("rally_id", value: idStr)
            .execute()
        
        // 6. Delete rally participants
        try? await supabase.from("rally_participants")
            .delete()
            .eq("rally_id", value: idStr)
            .execute()
        
        // 7. Delete rally from rallies table
        do {
            try await supabase.from("rallies")
                .delete()
                .eq("id", value: idStr)
                .execute()
        } catch {
            print("Failed to delete rally by ID (\(error.localizedDescription)), attempting delete by name...")
            try? await supabase.from("rallies")
                .delete()
                .eq("name", value: rally.name)
                .execute()
        }
        
        // 8. Delete group from groups table by ID and name
        try? await supabase.from("groups")
            .delete()
            .eq("id", value: idStr)
            .execute()
        
        try? await supabase.from("groups")
            .delete()
            .eq("name", value: rally.name)
            .execute()
        
        await loadAdminData()
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
    @Binding var editingRally: RallyRow?
    let onDeleteRally: (RallyRow) -> Void
    let onCreateRally: () -> Void
    
    @State private var rallyToDelete: RallyRow? = nil
    @State private var showDeleteConfirmation = false
    
    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text("Events (\(rallies.count))")
                    .font(.headline)
                    .foregroundColor(.secondary)
                
                Spacer()
                
                Button(action: onCreateRally) {
                    HStack(spacing: 6) {
                        Image(systemName: "plus.circle.fill")
                        Text("Create Rally")
                            .bold()
                    }
                    .font(.subheadline)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(10)
                }
            }
            .padding(.horizontal)
            .padding(.bottom, 8)
            
            if rallies.isEmpty {
                VStack(spacing: 12) {
                    Image(systemName: "car.2.fill")
                        .font(.system(size: 48))
                        .foregroundColor(.secondary.opacity(0.6))
                    Text("No rallies created yet.")
                        .font(.headline)
                        .foregroundColor(.secondary)
                    Button(action: onCreateRally) {
                        HStack {
                            Image(systemName: "plus.circle.fill")
                            Text("Create First Rally")
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.blue)
                }
                .frame(maxHeight: .infinity)
            } else {
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
                    .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                        Button(role: .destructive) {
                            rallyToDelete = rally
                            showDeleteConfirmation = true
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                    }
                    .contextMenu {
                        Button {
                            editingRally = rally
                        } label: {
                            Label("Edit", systemImage: "pencil")
                        }
                        
                        Button(role: .destructive) {
                            rallyToDelete = rally
                            showDeleteConfirmation = true
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                    }
                }
                .listStyle(.insetGrouped)
                .alert("Delete Rally", isPresented: $showDeleteConfirmation, presenting: rallyToDelete) { rally in
                    Button("Delete", role: .destructive) {
                        onDeleteRally(rally)
                    }
                    Button("Cancel", role: .cancel) {
                        rallyToDelete = nil
                    }
                } message: { rally in
                    Text("Are you sure you want to delete '\(rally.name)'? This action cannot be undone.")
                }
            }
        }
    }
}

enum AttendeeSortOption: String, CaseIterable, Identifiable {
    case name = "Name"
    case signedWaivers = "Signed Waivers"
    
    var id: String { rawValue }
}

struct RallyDetailSheet: View {
    @Environment(\.dismiss) private var dismiss
    let rally: RallyRow
    let allUsers: [AdminProfile]
    let allParticipants: [RallyParticipantRow]
    let onUpdate: () -> Void
    
    @State private var showAddPeopleSheet = false
    @State private var showEditSheet = false
    @State private var selectedUserProfile: AdminProfile? = nil
    @State private var isSaving = false
    @State private var errorMessage = ""
    @State private var successMessage = ""
    @State private var showToast = false
    @State private var isError = false
    
    @State private var searchText = ""
    @State private var sortOption: AttendeeSortOption = .name
    @State private var signedUserIds: Set<UUID> = []
    
    var attendees: [AdminProfile] {
        let userIdsInRally = allParticipants.filter { $0.rally_id == rally.id }.map { $0.user_id }
        return allUsers.filter { userIdsInRally.contains($0.id) }
    }
    
    var filteredAttendees: [AdminProfile] {
        let list = attendees
        let searchTrimmed = searchText.trimmingCharacters(in: .whitespaces)
        let filtered: [AdminProfile]
        if searchTrimmed.isEmpty {
            filtered = list
        } else {
            filtered = list.filter { $0.name.localizedCaseInsensitiveContains(searchTrimmed) }
        }
        
        switch sortOption {
        case .name:
            return filtered.sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
        case .signedWaivers:
            return filtered.sorted { a, b in
                let aSigned = signedUserIds.contains(a.id)
                let bSigned = signedUserIds.contains(b.id)
                if aSigned != bSigned {
                    return !aSigned && bSigned
                }
                return a.name.localizedCaseInsensitiveCompare(b.name) == .orderedAscending
            }
        }
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
                        
                        if let desc = rally.description, !desc.isEmpty {
                            Text(desc)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 24)
                        }
                        
                        if let cost = rally.eventCost {
                            Text("Cost: $\(String(format: "%.2f", cost))")
                                .font(.caption)
                                .bold()
                                .foregroundColor(.green)
                        }
                        
                        if let start = rally.eventStart, let end = rally.eventEnd, !start.isEmpty {
                            Text("\(start) — \(end)")
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(.vertical, 24)
                    .frame(maxWidth: .infinity)
                    .background(Color.primary.opacity(0.02))
                    
                    HStack(spacing: 8) {
                        Text("Attendees (\(filteredAttendees.count))")
                            .font(.headline)
                        
                        Spacer()
                        
                        HStack(spacing: 6) {
                            Image(systemName: "magnifyingglass")
                                .foregroundColor(.secondary)
                                .font(.footnote)
                            TextField("Search", text: $searchText)
                                .font(.subheadline)
                                .textFieldStyle(.plain)
                            if !searchText.isEmpty {
                                Button(action: { searchText = "" }) {
                                    Image(systemName: "xmark.circle.fill")
                                        .foregroundColor(.secondary)
                                        .font(.footnote)
                                }
                            }
                        }
                        .padding(.horizontal, 8)
                        .padding(.vertical, 6)
                        .background(Color.primary.opacity(0.06))
                        .cornerRadius(8)
                        .frame(maxWidth: 140)
                        
                        Menu {
                            Picker("Sort By", selection: $sortOption) {
                                ForEach(AttendeeSortOption.allCases) { option in
                                    Text(option.rawValue).tag(option)
                                }
                            }
                        } label: {
                            HStack(spacing: 4) {
                                Image(systemName: "arrow.up.arrow.down")
                                Text(sortOption.rawValue)
                            }
                            .font(.caption)
                            .bold()
                            .padding(.horizontal, 8)
                            .padding(.vertical, 6)
                            .background(Color.primary.opacity(0.06))
                            .cornerRadius(8)
                            .foregroundColor(.primary)
                        }
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
                    } else if filteredAttendees.isEmpty {
                        VStack(spacing: 8) {
                            Image(systemName: "magnifyingglass")
                                .font(.largeTitle)
                                .foregroundColor(.secondary.opacity(0.5))
                            Text("No attendees match '\(searchText)'.")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        .frame(maxHeight: .infinity)
                    } else {
                        List(filteredAttendees) { attendee in
                            HStack {
                                Image(systemName: "person.circle.fill")
                                    .foregroundColor(.secondary)
                                Text(attendee.name)
                                    .font(.body)
                                Spacer()
                                
                                let isSigned = signedUserIds.contains(attendee.id)
                                Text(isSigned ? "Signed" : "Not Signed")
                                    .font(.caption2)
                                    .bold()
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(isSigned ? Color.blue.opacity(0.1) : Color.red.opacity(0.1))
                                    .foregroundColor(isSigned ? .blue : .red)
                                    .cornerRadius(4)
                                
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
                ToolbarItem(placement: .topBarLeading) {
                    Button("Edit") {
                        showEditSheet = true
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
            .sheet(isPresented: $showEditSheet) {
                EditRallySheet(rally: rally, onUpdated: {
                    onUpdate()
                })
            }
            .sheet(item: $selectedUserProfile) { user in
                UserProfileDetailSheet(user: user)
            }
            .task {
                await loadWaiverStatus()
            }
        }
    }
    
    private func loadWaiverStatus() async {
        do {
            let idStr = rally.id.uuidString
            struct RallyWaiverRow: Decodable {
                let id: UUID
            }
            let waiverRows: [RallyWaiverRow] = (try? await supabase.from("waivers")
                .select("id")
                .eq("rally_id", value: idStr)
                .execute()
                .value) ?? []
            
            if waiverRows.isEmpty {
                signedUserIds = []
                return
            }
            
            let requiredWaiverIds = Set(waiverRows.map { $0.id })
            
            struct SignedRow: Decodable {
                let waiver_id: UUID
                let user_id: UUID
            }
            let signedRows: [SignedRow] = (try? await supabase.from("signed_waivers")
                .select("waiver_id, user_id")
                .execute()
                .value) ?? []
            
            var userSignedWaivers = [UUID: Set<UUID>]()
            for row in signedRows {
                if requiredWaiverIds.contains(row.waiver_id) {
                    userSignedWaivers[row.user_id, default: []].insert(row.waiver_id)
                }
            }
            
            var signed = Set<UUID>()
            for (userId, signedWaiverIdsSet) in userSignedWaivers {
                if requiredWaiverIds.isSubset(of: signedWaiverIdsSet) {
                    signed.insert(userId)
                }
            }
            self.signedUserIds = signed
        } catch {
            print("Error loading waiver status: \(error)")
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
                await loadWaiverStatus()
                
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
                    .insert(rallyInserts)
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
                        .insert(groupInserts)
                        .execute()
                }
                
                onUpdate()
                await loadWaiverStatus()
                
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
    @State private var idImage: UIImage? = nil
    @State private var isLoadingID = false
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
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
                    
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Image(systemName: "person.text.rectangle.fill")
                                .foregroundColor(.blue)
                            Text("User ID Document")
                                .font(.headline)
                        }
                        
                        if isLoadingID {
                            HStack {
                                Spacer()
                                ProgressView("Loading ID...")
                                Spacer()
                            }
                            .frame(height: 180)
                            .background(RoundedRectangle(cornerRadius: 16).fill(Color.primary.opacity(0.05)))
                        } else if let idImage = idImage {
                            Image(uiImage: idImage)
                                .resizable()
                                .scaledToFit()
                                .cornerRadius(16)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 16)
                                        .stroke(Color.primary.opacity(0.1), lineWidth: 1)
                                )
                                .shadow(color: Color.black.opacity(0.1), radius: 6, x: 0, y: 3)
                        } else {
                            VStack(spacing: 8) {
                                Image(systemName: "doc.text.viewfinder")
                                    .font(.system(size: 40))
                                    .foregroundColor(.secondary)
                                Text("No ID document uploaded")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            .frame(maxWidth: .infinity)
                            .frame(height: 140)
                            .background(RoundedRectangle(cornerRadius: 16).fill(Color.primary.opacity(0.05)))
                        }
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(RoundedRectangle(cornerRadius: 24).fill(.ultraThinMaterial))
                    .padding(.horizontal)
                }
                .padding(.bottom, 20)
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
                
                isLoadingID = true
                do {
                    let data = try await fetchUserIDImageData(for: user.id)
                    if let image = UIImage(data: data) {
                        self.idImage = image
                    }
                } catch {
                    print("Failed to download ID image: \(error)")
                }
                isLoadingID = false
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
                MockIDSheet(userId: user.id)
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
                    .insert(participant)
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
                        .insert(groupMember)
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
                            .insert(participant)
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
                                .insert(groupMember)
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
    let userId: UUID
    @State private var idImage: UIImage? = nil
    @State private var isLoading = false
    
    var body: some View {
        NavigationStack {
            VStack {
                if isLoading {
                    ProgressView("Loading ID...")
                } else if let idImage = idImage {
                    Image(uiImage: idImage)
                        .resizable()
                        .scaledToFit()
                        .cornerRadius(16)
                        .padding()
                } else {
                    VStack(spacing: 12) {
                        Image(systemName: "person.text.rectangle")
                            .font(.system(size: 80))
                            .foregroundColor(.gray)
                        Text("Submitted ID")
                            .font(.title2)
                            .bold()
                        Text("No ID document found.")
                            .foregroundColor(.secondary)
                    }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.primary.opacity(0.05))
            .navigationTitle("Submitted ID")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
            .task {
                isLoading = true
                do {
                    let data = try await fetchUserIDImageData(for: userId)
                    if let image = UIImage(data: data) {
                        self.idImage = image
                    }
                } catch {
                    print("Failed to download ID image: \(error)")
                }
                isLoading = false
            }
        }
    }
}

// MARK: - Image Source Enum

enum RallyImageSourceType: String, CaseIterable, Identifiable {
    case photoPicker = "Photo Library"
    case webLink = "Web Link"
    
    var id: String { rawValue }
}

// MARK: - Encodable Helper Structs for Rally Creation

struct RallyInsertRow: Encodable {
    let id: UUID
    let name: String
    let description: String?
    let event_start: String?
    let event_end: String?
    let event_image: String?
    let event_cost: Double?
}

struct RallyUpdateRow: Encodable {
    let name: String
    let description: String?
    let event_start: String?
    let event_end: String?
    let event_image: String?
    let event_cost: Double?
}

struct GroupInsertRow: Encodable {
    let id: UUID
    let name: String
}

struct WaiverInsertRow: Encodable {
    let id: UUID
    let rally_id: UUID
    let waiver_name: String
    let waiver_content: String
}

struct RallyParticipantInsertRow: Encodable {
    let user_id: UUID
    let rally_id: UUID
}

struct GroupMemberInsertRow: Encodable {
    let group_id: UUID
    let user_id: UUID
}

// MARK: - Create Rally Sheet

struct CreateRallySheet: View {
    @Environment(\.dismiss) private var dismiss
    let adminPerson: PersonInfo
    let onCreated: () -> Void
    
    @State private var name: String = ""
    @State private var description: String = ""
    @State private var eventStart: Date = Date()
    @State private var eventEnd: Date = Calendar.current.date(byAdding: .day, value: 2, to: Date()) ?? Date()
    @State private var eventCost: String = ""
    
    @State private var imageSource: RallyImageSourceType = .photoPicker
    @State private var selectedPhotoItem: PhotosPickerItem? = nil
    @State private var selectedImageData: Data? = nil
    @State private var selectedUIImage: UIImage? = nil
    @State private var webURLString: String = ""
    
    @State private var includeWaiver: Bool = true
    @State private var waiverName: String = ""
    @State private var waiverContent: String = ""
    
    @State private var isSaving: Bool = false
    @State private var showToast: Bool = false
    @State private var toastMessage: String = ""
    @State private var isError: Bool = false
    
    var body: some View {
        NavigationStack {
            ZStack {
                Form {
                    Section(header: Text("Rally Information")) {
                        TextField("Rally Name (Required)", text: $name)
                        
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Description")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            TextEditor(text: $description)
                                .frame(minHeight: 80)
                        }
                        
                        HStack {
                            Text("Event Cost ($)")
                            Spacer()
                            TextField("0.00", text: $eventCost)
                                .keyboardType(.decimalPad)
                                .multilineTextAlignment(.trailing)
                                .frame(maxWidth: 120)
                        }
                    }
                    
                    Section(header: Text("Rally Logo / Image")) {
                        Picker("Source", selection: $imageSource) {
                            ForEach(RallyImageSourceType.allCases) { source in
                                Text(source.rawValue).tag(source)
                            }
                        }
                        .pickerStyle(.segmented)
                        
                        if imageSource == .photoPicker {
                            PhotosPicker(selection: $selectedPhotoItem, matching: .images) {
                                HStack {
                                    Image(systemName: "photo.badge.plus")
                                        .foregroundColor(.blue)
                                    Text(selectedUIImage == nil ? "Select Image File" : "Change Selected Image")
                                        .fontWeight(.medium)
                                }
                            }
                            
                            if let selectedUIImage {
                                HStack {
                                    Spacer()
                                    Image(uiImage: selectedUIImage)
                                        .resizable()
                                        .scaledToFit()
                                        .frame(maxHeight: 120)
                                        .clipShape(RoundedRectangle(cornerRadius: 12))
                                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.gray.opacity(0.3), lineWidth: 1))
                                    Spacer()
                                }
                                .padding(.vertical, 4)
                            }
                        } else {
                            TextField("Paste Image URL (https://...)", text: $webURLString)
                                .textInputAutocapitalization(.never)
                                .autocorrectionDisabled(true)
                                .keyboardType(.URL)
                            
                            if let url = URL(string: webURLString.trimmingCharacters(in: .whitespaces)), !webURLString.isEmpty {
                                AsyncImage(url: url) { phase in
                                    switch phase {
                                    case .success(let image):
                                        HStack {
                                            Spacer()
                                            image
                                                .resizable()
                                                .scaledToFit()
                                                .frame(maxHeight: 120)
                                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                            Spacer()
                                        }
                                    case .failure:
                                        Text("Invalid or unreachable image URL")
                                            .font(.caption)
                                            .foregroundColor(.red)
                                    case .empty:
                                        ProgressView()
                                    @unknown default:
                                        EmptyView()
                                    }
                                }
                            }
                        }
                    }
                    
                    Section(header: Text("Event Schedule")) {
                        DatePicker("Start Date", selection: $eventStart, displayedComponents: .date)
                        DatePicker("End Date", selection: $eventEnd, displayedComponents: .date)
                    }
                    
                    Section(header: Text("Waiver Setup")) {
                        Toggle("Include Waiver with this Rally", isOn: $includeWaiver)
                        
                        if includeWaiver {
                            TextField("Waiver Title (e.g. Liability Release)", text: $waiverName)
                            
                            VStack(alignment: .leading, spacing: 6) {
                                Text("Waiver Agreement Content")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                TextEditor(text: $waiverContent)
                                    .frame(minHeight: 100)
                            }
                        }
                    }
                }
                
                if isSaving {
                    ZStack {
                        Color.black.opacity(0.15)
                            .ignoresSafeArea()
                        ProgressView("Creating Rally & Waiver...")
                            .padding()
                            .background(RoundedRectangle(cornerRadius: 12).fill(.ultraThinMaterial))
                    }
                }
                
                if showToast {
                    VStack {
                        HStack {
                            Image(systemName: isError ? "exclamationmark.triangle.fill" : "checkmark.circle.fill")
                                .foregroundColor(isError ? .red : .green)
                            Text(toastMessage)
                                .font(.subheadline)
                                .bold()
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
            .navigationTitle("Create New Rally")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .topBarTrailing) {
                    Button(action: createRally) {
                        Text("Create")
                            .bold()
                    }
                    .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty || isSaving)
                }
            }
            .task(id: selectedPhotoItem) {
                if let data = try? await selectedPhotoItem?.loadTransferable(type: Data.self),
                   let uiImage = UIImage(data: data) {
                    await MainActor.run {
                        self.selectedImageData = data
                        self.selectedUIImage = uiImage
                    }
                }
            }
        }
    }
    
    private func createRally() {
        let trimmedName = name.trimmingCharacters(in: .whitespaces)
        guard !trimmedName.isEmpty else { return }
        
        isSaving = true
        isError = false
        toastMessage = ""
        
        Task {
            do {
                let rallyId = UUID()
                let dateFormatter = DateFormatter()
                dateFormatter.dateFormat = "yyyy-MM-dd"
                let startStr = dateFormatter.string(from: eventStart)
                let endStr = dateFormatter.string(from: eventEnd)
                
                let costVal = Double(eventCost.replacingOccurrences(of: "$", with: "").trimmingCharacters(in: .whitespaces))
                let descVal = description.trimmingCharacters(in: .whitespaces).isEmpty ? nil : description.trimmingCharacters(in: .whitespaces)
                
                // Process & Upload Image if provided
                var imageFileName: String? = "\(trimmedName).png"
                var finalImageData: Data? = nil
                
                if imageSource == .photoPicker {
                    if let selectedUIImage {
                        finalImageData = selectedUIImage.pngData()
                    } else if let selectedImageData {
                        finalImageData = selectedImageData
                    }
                } else if imageSource == .webLink {
                    let urlStr = webURLString.trimmingCharacters(in: .whitespaces)
                    if !urlStr.isEmpty, let url = URL(string: urlStr) {
                        do {
                            let (data, _) = try await URLSession.shared.data(from: url)
                            if let uiImg = UIImage(data: data) {
                                finalImageData = uiImg.pngData()
                            } else {
                                finalImageData = data
                            }
                        } catch {
                            print("Failed to download image from web link: \(error)")
                        }
                    }
                }
                
                if let pngData = finalImageData {
                    let path = "\(trimmedName).png"
                    try await supabase.storage
                        .from("RallyLogos")
                        .upload(
                            path,
                            data: pngData,
                            options: FileOptions(contentType: "image/png", upsert: true)
                        )
                    imageFileName = path
                }
                
                // STEP 1: Insert corresponding group into "groups" table FIRST (Foreign Key Requirement!)
                let groupInsert = GroupInsertRow(
                    id: rallyId,
                    name: trimmedName
                )
                
                try await supabase.from("groups")
                    .insert(groupInsert)
                    .execute()
                
                // STEP 2: Insert rally into "rallies" table SECOND
                let rallyInsert = RallyInsertRow(
                    id: rallyId,
                    name: trimmedName,
                    description: descVal,
                    event_start: startStr,
                    event_end: endStr,
                    event_image: imageFileName,
                    event_cost: costVal
                )
                
                try await supabase.from("rallies")
                    .insert(rallyInsert)
                    .execute()
                
                // STEP 3: Insert waiver into "waivers" table if enabled or content/title filled
                let wName = waiverName.trimmingCharacters(in: .whitespaces)
                let wContent = waiverContent.trimmingCharacters(in: .whitespaces)
                
                if includeWaiver || !wName.isEmpty || !wContent.isEmpty {
                    let finalWaiverName = wName.isEmpty ? "\(trimmedName) Waiver" : wName
                    let finalWaiverContent = wContent.isEmpty ? "Standard Rally Liability Waiver and Release." : wContent
                    
                    let waiverInsert = WaiverInsertRow(
                        id: UUID(),
                        rally_id: rallyId,
                        waiver_name: finalWaiverName,
                        waiver_content: finalWaiverContent
                    )
                    
                    try await supabase.from("waivers")
                        .insert(waiverInsert)
                        .execute()
                }
                
                // STEP 4: Add admin as participant and group member
                if !adminPerson.isTestUser {
                    let participant = RallyParticipantInsertRow(user_id: adminPerson.id, rally_id: rallyId)
                    try await supabase.from("rally_participants")
                        .insert(participant)
                        .execute()
                    
                    let groupMember = GroupMemberInsertRow(group_id: rallyId, user_id: adminPerson.id)
                    try await supabase.from("group_members")
                        .insert(groupMember)
                        .execute()
                }
                
                isSaving = false
                onCreated()
                dismiss()
            } catch {
                print("Failed to create rally: \(error)")
                toastMessage = "Failed to create rally: \(error.localizedDescription)"
                isError = true
                showToast = true
                isSaving = false
            }
        }
    }
}

// MARK: - Edit Rally Sheet

struct EditRallySheet: View {
    @Environment(\.dismiss) private var dismiss
    let rally: RallyRow
    let onUpdated: () -> Void
    
    @State private var name: String
    @State private var description: String
    @State private var eventStart: Date
    @State private var eventEnd: Date
    @State private var eventCost: String
    
    @State private var imageSource: RallyImageSourceType = .photoPicker
    @State private var selectedPhotoItem: PhotosPickerItem? = nil
    @State private var selectedImageData: Data? = nil
    @State private var selectedUIImage: UIImage? = nil
    @State private var webURLString: String = ""
    
    @State private var isSaving: Bool = false
    @State private var showToast: Bool = false
    @State private var toastMessage: String = ""
    @State private var isError: Bool = false
    
    init(rally: RallyRow, onUpdated: @escaping () -> Void) {
        self.rally = rally
        self.onUpdated = onUpdated
        
        _name = State(initialValue: rally.name)
        _description = State(initialValue: rally.description ?? "")
        
        if let cost = rally.eventCost {
            _eventCost = State(initialValue: String(format: "%.2f", cost))
        } else {
            _eventCost = State(initialValue: "")
        }
        
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd"
        
        if let startStr = rally.eventStart, let startDate = dateFormatter.date(from: startStr) {
            _eventStart = State(initialValue: startDate)
        } else {
            _eventStart = State(initialValue: Date())
        }
        
        if let endStr = rally.eventEnd, let endDate = dateFormatter.date(from: endStr) {
            _eventEnd = State(initialValue: endDate)
        } else {
            _eventEnd = State(initialValue: Calendar.current.date(byAdding: .day, value: 2, to: Date()) ?? Date())
        }
    }
    
    var body: some View {
        NavigationStack {
            ZStack {
                Form {
                    Section(header: Text("Rally Information")) {
                        TextField("Rally Name (Required)", text: $name)
                        
                        VStack(alignment: .leading, spacing: 6) {
                            Text("Description")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            TextEditor(text: $description)
                                .frame(minHeight: 80)
                        }
                        
                        HStack {
                            Text("Event Cost ($)")
                            Spacer()
                            TextField("0.00", text: $eventCost)
                                .keyboardType(.decimalPad)
                                .multilineTextAlignment(.trailing)
                                .frame(maxWidth: 120)
                        }
                    }
                    
                    Section(header: Text("Rally Logo / Image")) {
                        Picker("Source", selection: $imageSource) {
                            ForEach(RallyImageSourceType.allCases) { source in
                                Text(source.rawValue).tag(source)
                            }
                        }
                        .pickerStyle(.segmented)
                        
                        if imageSource == .photoPicker {
                            PhotosPicker(selection: $selectedPhotoItem, matching: .images) {
                                HStack {
                                    Image(systemName: "photo.badge.plus")
                                        .foregroundColor(.blue)
                                    Text(selectedUIImage == nil ? "Select Image File" : "Change Selected Image")
                                        .fontWeight(.medium)
                                }
                            }
                            
                            if let selectedUIImage {
                                HStack {
                                    Spacer()
                                    Image(uiImage: selectedUIImage)
                                        .resizable()
                                        .scaledToFit()
                                        .frame(maxHeight: 120)
                                        .clipShape(RoundedRectangle(cornerRadius: 12))
                                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.gray.opacity(0.3), lineWidth: 1))
                                    Spacer()
                                }
                                .padding(.vertical, 4)
                            } else {
                                AsyncImage(url: try? getRallyImageURL(for: rally.name)) { phase in
                                    if let image = phase.image {
                                        HStack {
                                            Spacer()
                                            image
                                                .resizable()
                                                .scaledToFit()
                                                .frame(maxHeight: 120)
                                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                            Spacer()
                                        }
                                    }
                                }
                            }
                        } else {
                            TextField("Paste Image URL (https://...)", text: $webURLString)
                                .textInputAutocapitalization(.never)
                                .autocorrectionDisabled(true)
                                .keyboardType(.URL)
                            
                            if let url = URL(string: webURLString.trimmingCharacters(in: .whitespaces)), !webURLString.isEmpty {
                                AsyncImage(url: url) { phase in
                                    switch phase {
                                    case .success(let image):
                                        HStack {
                                            Spacer()
                                            image
                                                .resizable()
                                                .scaledToFit()
                                                .frame(maxHeight: 120)
                                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                            Spacer()
                                        }
                                    case .failure:
                                        Text("Invalid or unreachable image URL")
                                            .font(.caption)
                                            .foregroundColor(.red)
                                    case .empty:
                                        ProgressView()
                                    @unknown default:
                                        EmptyView()
                                    }
                                }
                            }
                        }
                    }
                    
                    Section(header: Text("Event Schedule")) {
                        DatePicker("Start Date", selection: $eventStart, displayedComponents: .date)
                        DatePicker("End Date", selection: $eventEnd, displayedComponents: .date)
                    }
                }
                
                if isSaving {
                    ZStack {
                        Color.black.opacity(0.15)
                            .ignoresSafeArea()
                        ProgressView("Saving Changes...")
                            .padding()
                            .background(RoundedRectangle(cornerRadius: 12).fill(.ultraThinMaterial))
                    }
                }
                
                if showToast {
                    VStack {
                        HStack {
                            Image(systemName: isError ? "exclamationmark.triangle.fill" : "checkmark.circle.fill")
                                .foregroundColor(isError ? .red : .green)
                            Text(toastMessage)
                                .font(.subheadline)
                                .bold()
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
            .navigationTitle("Edit Rally")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .topBarTrailing) {
                    Button(action: saveChanges) {
                        Text("Save")
                            .bold()
                    }
                    .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty || isSaving)
                }
            }
            .task(id: selectedPhotoItem) {
                if let data = try? await selectedPhotoItem?.loadTransferable(type: Data.self),
                   let uiImage = UIImage(data: data) {
                    await MainActor.run {
                        self.selectedImageData = data
                        self.selectedUIImage = uiImage
                    }
                }
            }
        }
    }
    
    private func saveChanges() {
        let trimmedName = name.trimmingCharacters(in: .whitespaces)
        guard !trimmedName.isEmpty else { return }
        
        isSaving = true
        isError = false
        toastMessage = ""
        
        Task {
            do {
                let dateFormatter = DateFormatter()
                dateFormatter.dateFormat = "yyyy-MM-dd"
                let startStr = dateFormatter.string(from: eventStart)
                let endStr = dateFormatter.string(from: eventEnd)
                
                let costVal = Double(eventCost.replacingOccurrences(of: "$", with: "").trimmingCharacters(in: .whitespaces))
                let descVal = description.trimmingCharacters(in: .whitespaces).isEmpty ? nil : description.trimmingCharacters(in: .whitespaces)
                
                var imageFileName: String? = rally.eventImage
                var finalImageData: Data? = nil
                
                if imageSource == .photoPicker {
                    if let selectedUIImage {
                        finalImageData = selectedUIImage.pngData()
                    } else if let selectedImageData {
                        finalImageData = selectedImageData
                    }
                } else if imageSource == .webLink {
                    let urlStr = webURLString.trimmingCharacters(in: .whitespaces)
                    if !urlStr.isEmpty, let url = URL(string: urlStr) {
                        do {
                            let (data, _) = try await URLSession.shared.data(from: url)
                            if let uiImg = UIImage(data: data) {
                                finalImageData = uiImg.pngData()
                            } else {
                                finalImageData = data
                            }
                        } catch {
                            print("Failed to download image from web link: \(error)")
                        }
                    }
                }
                
                if let pngData = finalImageData {
                    let path = "\(trimmedName).png"
                    try await supabase.storage
                        .from("RallyLogos")
                        .upload(
                            path,
                            data: pngData,
                            options: FileOptions(contentType: "image/png", upsert: true)
                        )
                    imageFileName = path
                }
                
                let rallyUpdate = RallyUpdateRow(
                    name: trimmedName,
                    description: descVal,
                    event_start: startStr,
                    event_end: endStr,
                    event_image: imageFileName,
                    event_cost: costVal
                )
                
                try await supabase.from("rallies")
                    .update(rallyUpdate)
                    .eq("id", value: rally.id.uuidString)
                    .execute()
                
                struct GroupUpdateRow: Encodable {
                    let name: String
                }
                
                try? await supabase.from("groups")
                    .update(GroupUpdateRow(name: trimmedName))
                    .eq("id", value: rally.id.uuidString)
                    .execute()
                
                isSaving = false
                onUpdated()
                dismiss()
            } catch {
                print("Failed to update rally: \(error)")
                toastMessage = "Failed to update rally: \(error.localizedDescription)"
                isError = true
                showToast = true
                isSaving = false
            }
        }
    }
}

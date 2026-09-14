//
//  Wavers.swift
//  NAS Rally
//

import SwiftUI
import Supabase

struct SignedWaiverInsertRow: Encodable {
    let user_id: UUID
    let waiver_id: UUID
}

struct WaversView: View {
    @Binding var person: PersonInfo
    @State private var selectedTab = 0 // 0: Pending, 1: Signed
    @State private var pendingWaivers: [Waiver] = []
    @State private var signedWaivers: [Waiver] = []
    @State private var isLoading = true
    @State private var selectedWaiver: Waiver? = nil
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Text("Waivers")
                    .font(.title2)
                    .bold()
                    .padding(.top, 16)
                    .padding(.bottom, 12)
                
                // --- TOP VIEW PICKER (LIKE ADMIN VIEW) ---
                HStack(spacing: 0) {
                    tabButton(title: "Pending (\(pendingWaivers.count))", index: 0)
                    tabButton(title: "Signed (\(signedWaivers.count))", index: 1)
                }
                .background(Color.primary.opacity(0.05))
                .cornerRadius(12)
                .padding(.horizontal, 24)
                .padding(.bottom, 16)
                
                if isLoading {
                    VStack(spacing: 12) {
                        ProgressView()
                        Text("Loading waivers...")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxHeight: .infinity)
                } else if selectedTab == 0 {
                    // --- PENDING WAIVERS TAB ---
                    if pendingWaivers.isEmpty {
                        VStack(spacing: 12) {
                            Image(systemName: "checkmark.seal.fill")
                                .font(.system(size: 48))
                                .foregroundColor(.green.opacity(0.8))
                            Text("All waivers completed!")
                                .font(.headline)
                                .foregroundColor(.primary)
                            Text("You have signed all required event waivers.")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .frame(maxHeight: .infinity)
                    } else {
                        ScrollView {
                            VStack(spacing: 12) {
                                ForEach(pendingWaivers) { waiver in
                                    Button(action: {
                                        selectedWaiver = waiver
                                    }) {
                                        HStack {
                                            VStack(alignment: .leading, spacing: 4) {
                                                if let rallyName = waiver.rallies?.name, !rallyName.isEmpty {
                                                    Text(rallyName)
                                                        .font(.caption)
                                                        .bold()
                                                        .foregroundColor(.blue)
                                                }
                                                Text(waiver.waiver_name)
                                                    .font(.headline)
                                                    .foregroundColor(.primary)
                                                Text("Tap to view & sign")
                                                    .font(.caption)
                                                    .foregroundColor(.secondary)
                                            }
                                            Spacer()
                                            Image(systemName: "doc.text.fill")
                                                .font(.title2)
                                                .foregroundColor(.blue)
                                        }
                                        .padding()
                                        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                                    }
                                }
                            }
                            .padding(.horizontal, 24)
                        }
                    }
                } else {
                    // --- SIGNED WAIVERS TAB ---
                    if signedWaivers.isEmpty {
                        VStack(spacing: 12) {
                            Image(systemName: "doc.text")
                                .font(.system(size: 48))
                                .foregroundColor(.secondary.opacity(0.6))
                            Text("No signed waivers yet.")
                                .font(.headline)
                                .foregroundColor(.secondary)
                        }
                        .frame(maxHeight: .infinity)
                    } else {
                        ScrollView {
                            VStack(spacing: 12) {
                                ForEach(signedWaivers) { waiver in
                                    Button(action: {
                                        selectedWaiver = waiver
                                    }) {
                                        HStack {
                                            VStack(alignment: .leading, spacing: 4) {
                                                if let rallyName = waiver.rallies?.name, !rallyName.isEmpty {
                                                    Text(rallyName)
                                                        .font(.caption)
                                                        .bold()
                                                        .foregroundColor(.blue)
                                                }
                                                Text(waiver.waiver_name)
                                                    .font(.headline)
                                                    .foregroundColor(.primary)
                                                
                                                HStack(spacing: 4) {
                                                    Image(systemName: "checkmark.circle.fill")
                                                        .foregroundColor(.green)
                                                        .font(.caption)
                                                    Text("Signed")
                                                        .font(.caption)
                                                        .bold()
                                                        .foregroundColor(.green)
                                                }
                                            }
                                            Spacer()
                                            Image(systemName: "doc.badge.checkmark")
                                                .font(.title2)
                                                .foregroundColor(.green)
                                        }
                                        .padding()
                                        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                                    }
                                }
                            }
                            .padding(.horizontal, 24)
                        }
                    }
                }
            }
            .frame(maxWidth: .infinity)
            .task {
                await loadWaivers()
            }
            .refreshable {
                await loadWaivers()
            }
            .sheet(item: $selectedWaiver) { waiver in
                WaiverDetailSheet(
                    waiver: waiver,
                    userId: person.id,
                    isAlreadySigned: signedWaivers.contains(where: { $0.id == waiver.id }),
                    onSigned: {
                        Task {
                            await loadWaivers()
                        }
                    }
                )
            }
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
    
    private func loadWaivers() async {
        isLoading = true
        do {
            let result = try await fetchUserWaivers(for: person.id)
            self.pendingWaivers = result.pending
            self.signedWaivers = result.signed
        } catch {
            print("Failed to fetch waivers: \(error)")
        }
        isLoading = false
    }
}

struct WaiverDetailSheet: View {
    @Environment(\.dismiss) private var dismiss
    let waiver: Waiver
    let userId: UUID
    let isAlreadySigned: Bool
    let onSigned: () -> Void
    
    @State private var isSigning = false
    @State private var signedSuccessfully = false
    @State private var errorMessage = ""
    
    var isCompleted: Bool {
        isAlreadySigned || signedSuccessfully
    }
    
    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 16) {
                if let rallyName = waiver.rallies?.name, !rallyName.isEmpty {
                    HStack(spacing: 6) {
                        Image(systemName: "flag.checkered")
                            .foregroundColor(.blue)
                        Text(rallyName)
                            .font(.subheadline)
                            .bold()
                            .foregroundColor(.blue)
                        Spacer()
                    }
                    .padding(.vertical, 6)
                    .padding(.horizontal, 10)
                    .background(Color.blue.opacity(0.1))
                    .cornerRadius(8)
                }
                
                Text(waiver.waiver_name)
                    .font(.title2)
                    .bold()
                
                Divider()
                
                ScrollView {
                    Text(waiver.waiver_content ?? "Standard Rally Waiver Agreement and Liability Release.")
                        .font(.body)
                        .lineSpacing(4)
                        .foregroundColor(.primary)
                }
                
                Spacer()
                
                Button(action: signWaiver) {
                    HStack {
                        if isSigning {
                            ProgressView()
                                .padding(.trailing, 8)
                        }
                        Text(isCompleted ? "Signed ✓" : "Sign & Agree to Waiver")
                            .bold()
                            .foregroundColor(.white)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(isCompleted ? Color.green : Color.blue)
                    .cornerRadius(12)
                }
                .disabled(isSigning || isCompleted)
                
                if !errorMessage.isEmpty {
                    Text(errorMessage)
                        .font(.caption)
                        .foregroundColor(.red)
                }
            }
            .padding()
            .navigationTitle("Waiver Agreement")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }
    
    private func signWaiver() {
        guard !isCompleted else { return }
        isSigning = true
        errorMessage = ""
        
        Task {
            do {
                let row = SignedWaiverInsertRow(user_id: userId, waiver_id: waiver.id)
                try await supabase.from("signed_waivers")
                    .insert(row)
                    .execute()
                
                signedSuccessfully = true
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                onSigned()
                dismiss()
            } catch {
                print("Failed to sign waiver: \(error)")
                errorMessage = "Failed to sign waiver: \(error.localizedDescription)"
            }
            isSigning = false
        }
    }
}

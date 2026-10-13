//
//  Settings.swift
//  NAS Rally
//

import SwiftUI
import Supabase

struct SettingsView: View {
    @Binding var person: PersonInfo
    let themes = ["Auto", "Blue", "Red"]
    @State private var showDeleteConfirmStep1 = false
    @State private var showDeleteConfirmStep2 = false
    
    var body: some View {
        NavigationStack {
            VStack {
                HStack {
                    Text("Settings")
                        .font(.title2)
                        .bold()
                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.top, 10)
                .padding(.bottom, 6)
                
                List {
                    Section {
                        NavigationLink(destination: ProfileView(person: $person)) {
                            HStack {
                                Image(systemName: "person.crop.circle.fill")
                                    .padding(.leading, 10)
                                Text("Profile")
                                    .padding(.leading, 8)
                            }
                        }
                        
                        HStack {
                            Image(systemName: "photo.artframe")
                                .padding(.leading, 10)
                            
                            Picker("Select a Theme", selection: $person.theme) {
                                ForEach(themes, id: \.self) { themeName in
                                    Text(themeName)
                                }
                            }
                            .padding(.leading, 8)
                            .pickerStyle(.menu)
                            .onChange(of: person.theme) { newTheme in
                                Task {
                                    await updateTheme(personID: person.id, theme: newTheme)
                                }
                            }
                        }
                        
                        NavigationLink(destination: IDView(person: $person)) {
                            HStack {
                                Image(systemName: "person.text.rectangle")
                                    .padding(.leading, 10)
                                Text("ID")
                                    .padding(.leading, 8)
                            }
                        }
                    }
                    
                    Section {
                        HStack {
                            Spacer()
                            Button("Logout") {
                                logout(person: person)
                            }
                            .buttonStyle(.bordered)
                            .foregroundColor(.primary)
                            Spacer()
                        }
                    }
                    
                    Section(header: Text("Danger Zone").foregroundColor(.red)) {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Account Deletion")
                                .font(.headline)
                                .foregroundColor(.red)
                            Text("Permanently delete your account and remove login credentials.")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            
                            Button(role: .destructive, action: {
                                showDeleteConfirmStep1 = true
                            }) {
                                HStack {
                                    Spacer()
                                    Text("Delete Account")
                                        .bold()
                                    Spacer()
                                }
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(.red)
                            .padding(.top, 4)
                        }
                        .padding(.vertical, 4)
                    }
                }
                .glassEffectCompat(.regular, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
                .alert("Delete Your Account?", isPresented: $showDeleteConfirmStep1) {
                    Button("Continue", role: .destructive) {
                        showDeleteConfirmStep2 = true
                    }
                    Button("Cancel", role: .cancel) {}
                } message: {
                    Text("Are you sure you want to delete your account? This will log you out and remove your login credentials.")
                }
                .alert("FINAL WARNING: Delete Account", isPresented: $showDeleteConfirmStep2) {
                    Button("Confirm Delete Account", role: .destructive) {
                        let userId = person.id
                        Task {
                            await deleteUserAccount(userId: userId)
                            logout(person: person)
                        }
                    }
                    Button("Cancel", role: .cancel) {}
                } message: {
                    Text("This action cannot be undone. Your login credentials will be permanently removed. Your profile record and signed waivers will be retained for legal compliance.")
                }
            }
        }
    }
}

private func logout(person: PersonInfo) {
    guard !person.isTestUser else { return }

    Task {
        do {
            try await supabase.auth.signOut()
        } catch {
            print("Logout failed: \(error)")
        }
    }
}

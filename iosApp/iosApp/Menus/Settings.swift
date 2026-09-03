//
//  Settings.swift
//  NAS Rally
//

import SwiftUI
import Supabase

struct SettingsView: View {
    @Binding var person: PersonInfo
    let themes = ["Auto", "Blue", "Red"]
    
    var body: some View {
        VStack {
            Text("Settings")
                .padding(.horizontal, 15)
                .padding(.top, 10)
                .padding(.bottom, 10)
                .bold()
            
            List {
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
                }
                
                NavigationLink(destination: CardView(person: $person)) {
                    HStack {
                        Image(systemName: "creditcard")
                            .padding(.leading, 10)
                        Text("Payment Info")
                            .padding(.leading, 8)
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
            .glassEffectCompat(.regular, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
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

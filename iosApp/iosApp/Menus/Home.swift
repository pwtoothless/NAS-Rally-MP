//
//  Home.swift
//  NAS Rally
//

import SwiftUI

struct HomeView: View {
    @Binding var person: PersonInfo
    @State private var profileImageURL: URL? = nil

    var body: some View {
        VStack {
            Text("Home")
                .padding(.top, 10)
                .padding(.bottom, 10)
                .bold()
            
            // Main Page
            HStack {
                AsyncImage(url: profileImageURL) { phase in
                    switch phase {
                    case .empty:
                        ProgressView().frame(width: 100, height: 100)
                    case .success(let image):
                        image.resizable().scaledToFill().frame(width: 100, height: 100).clipShape(Circle())
                    case .failure:
                        Image(systemName: "person.crop.circle.fill")
                            .resizable()
                            .foregroundStyle(.gray)
                            .frame(width: 100, height: 100)
                    @unknown default:
                        EmptyView()
                    }
                }
                
                Text("Hi, " + person.name)
                    .padding()
                    .font(.headline)
            }
            .foregroundColor(.primary)
            .frame(height: 150)
            .padding(.leading, 15)
            .glassEffectCompat(.regular, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
            
            Text("Updates for user go here")
            
            Spacer() // Top Aligns the Page
        }
        .task {
            guard !person.isTestUser else { return }

            do {
                self.profileImageURL = try await getProfileImageURL(for: person.id)
            } catch {
                print("Failed to load image URL: \(error)")
            }
        }
    }
}

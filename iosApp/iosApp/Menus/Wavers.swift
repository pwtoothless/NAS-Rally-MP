//
//  Wavers.swift
//  NAS Rally
//

import SwiftUI

struct WaversView: View {
    @Binding var person: PersonInfo
    @State private var pendingWaivers: [Waiver] = []
    @State private var isLoading = true
    
    var body: some View {
        NavigationStack {
            VStack {
                Text("Waivers")
                    .font(.title2)
                    .bold()
                    .padding(.vertical, 10)
                
                if isLoading {
                    ProgressView()
                        .frame(maxHeight: .infinity)
                } else if pendingWaivers.isEmpty {
                    Text("No pending waivers.")
                        .foregroundColor(.secondary)
                        .frame(maxHeight: .infinity)
                } else {
                    ScrollView {
                        VStack(spacing: 12) {
                            ForEach(pendingWaivers) { waiver in
                                HStack {
                                    Text(waiver.waiver_name)
                                        .font(.headline)
                                    Spacer()
                                    Image(systemName: "doc.text")
                                        .foregroundColor(.blue)
                                }
                                .padding()
                                .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                            }
                        }
                        .padding(.horizontal, 35)
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
        }
    }
    
    private func loadWaivers() async {
        isLoading = true
        do {
            self.pendingWaivers = try await fetchUserWaivers(for: person.id)
        } catch {
            print("Failed to fetch waivers: \(error)")
        }
        isLoading = false
    }
}

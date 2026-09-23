//
//  Home.swift
//  NAS Rally
//

import SwiftUI

struct HomeView: View {
    @Binding var person: PersonInfo
    @State private var profileImageURL: URL? = nil
    
    // AI Summary State
    @State private var aiSummary: String? = nil
    @State private var isLoadingSummary: Bool = false
    @State private var gradientOffset: CGFloat = -1.0
    
    // Gradient colors matching themes
    let aiGradientColors = [
        Color(hexString: "#1E54B3") ?? .blue,
        Color(hexString: "#C11326") ?? .red
    ]

    var body: some View {
        ScrollView {
            VStack {
                HStack {
                    Text("Home")
                        .font(.title2)
                        .bold()
                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.top, 10)
                .padding(.bottom, 6)
                
                // Main Page Profile Card
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
                .frame(maxWidth: .infinity, alignment: .leading)
                .frame(height: 150)
                .padding(.leading, 15)
                .glassEffectCompat(.regular, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
                .padding(.horizontal, 16)
                
                // AI Summary Section
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Image(systemName: "sparkles")
                            .foregroundStyle(
                                LinearGradient(
                                    colors: aiGradientColors,
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                        Text("Notification Summary")
                            .font(.headline)
                            .bold()
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 24)
                    
                    if isLoadingSummary {
                        // Shimmering Gradient Loading State
                        RoundedRectangle(cornerRadius: 16)
                            .fill(
                                LinearGradient(
                                    gradient: Gradient(colors: [
                                        aiGradientColors[0].opacity(0.3),
                                        aiGradientColors[1].opacity(0.6),
                                        aiGradientColors[0].opacity(0.3)
                                    ]),
                                    startPoint: UnitPoint(x: gradientOffset, y: 0),
                                    endPoint: UnitPoint(x: gradientOffset + 1, y: 0)
                                )
                            )
                            .frame(height: 100)
                            .padding(.horizontal, 16)
                            .onAppear {
                                withAnimation(Animation.linear(duration: 1.5).repeatForever(autoreverses: false)) {
                                    gradientOffset = 1.0
                                }
                            }
                    } else if let summary = aiSummary {
                        Text(summary)
                            .padding()
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color(UIColor.secondarySystemBackground))
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                            .padding(.horizontal, 16)
                    } else {
                        Text("Pull down to refresh your summary.")
                            .foregroundColor(.secondary)
                            .padding(.horizontal, 16)
                    }
                }
                
                Spacer() // Top Aligns the Page
            }
        }
        .refreshable {
            await loadAISummary(forceRefresh: true)
        }
        .task {
            guard !person.isTestUser else { return }

            do {
                self.profileImageURL = try getProfileImageURL(for: person.id)
            } catch {
                print("Failed to load image URL: \(error)")
            }
            
            await loadAISummary(forceRefresh: false)
        }
    }
    
    // MARK: - AI Summary Logic
    
    private func loadAISummary(forceRefresh: Bool) async {
        guard !person.isTestUser else { return }
        
        let defaults = UserDefaults.standard
        let lastFetchKey = "aiSummaryLastFetch_\(person.id.uuidString)"
        let summaryKey = "aiSummaryText_\(person.id.uuidString)"
        
        let lastFetchTime = defaults.object(forKey: lastFetchKey) as? Date
        let cachedSummary = defaults.string(forKey: summaryKey)
        
        let now = Date()
        let timeSinceLastFetch = lastFetchTime.map { now.timeIntervalSince($0) } ?? .infinity
        
        // 6 hours = 21600 seconds, 30 mins = 1800 seconds
        let shouldFetch: Bool
        
        if forceRefresh {
            // Only allow manual refresh if it's been more than 30 mins
            if timeSinceLastFetch > 1800 {
                shouldFetch = true
            } else {
                shouldFetch = false
                print("Manual refresh restricted: Wait 30 mins between fetches. (Current wait: \(Int(1800 - timeSinceLastFetch)) seconds left)")
            }
        } else {
            // Auto fetch if no cache or older than 6 hours
            if cachedSummary == nil || timeSinceLastFetch > 21600 {
                shouldFetch = true
            } else {
                shouldFetch = false
            }
        }
        
        if !shouldFetch {
            if let cached = cachedSummary {
                self.aiSummary = cached
            }
            return
        }
        
        // Perform Fetch
        isLoadingSummary = true
        defer { isLoadingSummary = false }
        
        do {
            let fetchedSummary = try await fetchAISummary()
            
            // Save to cache
            defaults.set(now, forKey: lastFetchKey)
            defaults.set(fetchedSummary, forKey: summaryKey)
            
            self.aiSummary = fetchedSummary
        } catch {
            print("Failed to fetch AI Summary: \(error)")
            // Fallback to cache if error
            if let cached = cachedSummary {
                self.aiSummary = cached
            } else {
                self.aiSummary = "Failed to load summary."
            }
        }
    }
}

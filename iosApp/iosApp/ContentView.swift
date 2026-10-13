//
//  ContentView.swift
//  NAS Rally
//

import SwiftUI
import Shared

struct ContentView: View {
    @Binding var person: PersonInfo
    @State private var bannerData: InAppBannerData? = nil
    @State private var selectedTabTag = 0

    var body: some View {
        ZStack(alignment: .top) {
            Group {
                if #available(iOS 26.0, macOS 26.0, *) {
                    TabView(selection: $selectedTabTag) {
                        Tab("Home", systemImage: "house", value: 0) {
                            HomeView(person: $person)
                        }
                        Tab("Rallies", systemImage: "car.2.fill", value: 1) {
                            RalliesView(person: $person)
                        }
                        Tab("Chat", systemImage: "bubble.left.and.bubble.right", value: 2) {
                            ChatView(person: $person)
                        }
                        Tab("Wavers", systemImage: "long.text.page.and.pencil", value: 3) {
                            WaversView(person: $person)
                        }
                        Tab("Settings", systemImage: "gearshape", value: 4) {
                            SettingsView(person: $person)
                        }
                        if (person.privligeLevel == "Admin") {
                            Tab("Admin", systemImage: "person.badge.checkmark.seal.fill", value: 5) {
                                AdminView(person: $person)
                            }
                            Tab("Onboarding-Test", systemImage: "long.text.page.and.pencil", value: 6) {
                                OnboardingView(person: $person)
                            }
                            Tab("TOS-Test", systemImage: "long.text.page.and.pencil", value: 7) {
                                TOSView(person: $person)
                            }
                        }
                    }
                    .tabViewStyle(.sidebarAdaptable)
                } else {
                    TabView(selection: $selectedTabTag) {
                        HomeView(person: $person)
                            .tabItem { Label("Home", systemImage: "house") }
                            .tag(0)
                        RalliesView(person: $person)
                            .tabItem { Label("Rallies", systemImage: "car.2.fill") }
                            .tag(1)
                        ChatView(person: $person)
                            .tabItem { Label("Chat", systemImage: "bubble.left.and.bubble.right") }
                            .tag(2)
                        WaversView(person: $person)
                            .tabItem { Label("Wavers", systemImage: "doc.text") }
                            .tag(3)
                        SettingsView(person: $person)
                            .tabItem { Label("Settings", systemImage: "gearshape") }
                            .tag(4)
                        if person.privligeLevel == "Admin" {
                            AdminView(person: $person)
                                .tabItem { Label("Admin", systemImage: "person.circle") }
                                .tag(5)
                        }
                    }
                }
            }

            if let banner = bannerData {
                SubtleInAppBannerView(
                    data: banner,
                    onTap: {
                        selectedTabTag = 2
                        NotificationManager.shared.dismissInAppBanner()
                        bannerData = nil
                    },
                    onDismiss: {
                        NotificationManager.shared.dismissInAppBanner()
                        bannerData = nil
                    }
                )
                .transition(.move(edge: .top).combined(with: .opacity))
                .animation(.spring(), value: bannerData != nil)
            }
        }
        .tint(person.themeColor)
        .accentColor(person.themeColor)
        .onAppear {
            UITabBar.appearance().tintColor = UIColor(person.themeColor)
            observeInAppBanner()
        }
        .onChange(of: person.theme) { _ in
            UITabBar.appearance().tintColor = UIColor(person.themeColor)
        }
    }

    private func observeInAppBanner() {
        NotificationManager.shared.setOnBannerDataChangedListener { banner in
            DispatchQueue.main.async {
                self.bannerData = banner
            }
        }
    }
}

struct SubtleInAppBannerView: View {
    let data: InAppBannerData
    let onTap: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "bubble.left.and.bubble.right.fill")
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(.blue)

            VStack(alignment: .leading, spacing: 2) {
                Text("\(data.groupName) (\(data.unreadCount) new messages)")
                    .font(.subheadline)
                    .bold()
                    .foregroundColor(.primary)

                if !data.latestMessageText.isEmpty {
                    Text("\(data.latestSenderName): \(data.latestMessageText)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }
            }

            Spacer()

            Button(action: onDismiss) {
                Image(systemName: "xmark.circle.fill")
                    .foregroundColor(.secondary)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(.ultraThinMaterial)
        .cornerRadius(16)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.primary.opacity(0.1), lineWidth: 1)
        )
        .shadow(color: .black.opacity(0.12), radius: 8, x: 0, y: 4)
        .padding(.horizontal, 16)
        .padding(.top, 8)
        .onTapGesture {
            onTap()
        }
    }
}

// MARK: - Compatibility Glass Modifiers

struct GlassEffectModifier<S: Shape>: ViewModifier {
    var shape: S?
    var material: Material

    func body(content: Content) -> some View {
        if let shape = shape {
            content.background(shape.fill(material))
        } else {
            content.background(material)
        }
    }
}

extension View {
    /// Applies the glass effect with a custom material and shape.
    @ViewBuilder
    func glassEffectCompat<S: Shape>(_ material: Material = .regular, in shape: S, interactive: Bool = false) -> some View {
        if #available(iOS 26.0, macOS 26.0, tvOS 26.0, watchOS 26.0, *) {
            self.glassEffect(interactive ? .regular.interactive() : .regular, in: shape)
        } else {
            self.background(shape.fill(material))
        }
    }

    /// Applies the effect using the system's default Capsule shape.
    @ViewBuilder
    func glassEffectCompat(_ material: Material = .regular, interactive: Bool = false) -> some View {
        glassEffectCompat(material, in: Capsule(), interactive: interactive)
    }
}

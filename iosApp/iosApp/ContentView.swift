//
//  ContentView.swift
//  NAS Rally
//

import SwiftUI

struct ContentView: View {
    @Binding var person: PersonInfo
    
    var body: some View {
        if #available(iOS 26.0, macOS 26.0, *) {
            TabView() {
                Tab("Home", systemImage: "house") {
                    HomeView(person: $person)
                }
                Tab("Rallies", systemImage: "car.2.fill") {
                    RalliesView(person: $person)
                }
                Tab("Chat", systemImage: "bubble.left.and.bubble.right") {
                    ChatView(person: $person)
                }
                Tab("Wavers", systemImage: "long.text.page.and.pencil") {
                    WaversView(person: $person)
                }
                Tab("Settings", systemImage: "gearshape") {
                    SettingsView(person: $person)
                }
                if (person.privligeLevel == "Admin") {
                    Tab("Admin", systemImage: "person.badge.checkmark.seal.fill") {
                        AdminView(person: $person)
                    }
                    Tab("Onboarding-Test", systemImage: "person.badge.checkmark.seal.fill") {
                        OnboardingView(person: $person)
                    }
                }
            }
            .tabViewStyle(.sidebarAdaptable)
        } else {
            TabView {
                HomeView(person: $person)
                    .tabItem { Label("Home", systemImage: "house") }
                RalliesView(person: $person)
                    .tabItem { Label("Rallies", systemImage: "car.2.fill") }
                ChatView(person: $person)
                    .tabItem { Label("Chat", systemImage: "bubble.left.and.bubble.right") }
                WaversView(person: $person)
                    .tabItem { Label("Wavers", systemImage: "doc.text") }
                SettingsView(person: $person)
                    .tabItem { Label("Settings", systemImage: "gearshape") }
                if person.privligeLevel == "Admin" {
                    AdminView(person: $person)
                        .tabItem { Label("Admin", systemImage: "person.circle") }
                }
            }
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

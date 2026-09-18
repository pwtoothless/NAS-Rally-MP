//
//  ContentView.swift
//  NAS Rally
//

import SwiftUI

struct ContentView: View {
    @Binding var person: PersonInfo
    
    var body: some View {
        Group {
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
                        Tab("Onboarding-Test", systemImage: "long.text.page.and.pencil") {
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
        .tint(themeTint)
    }

    private var themeTint: Color? {
        switch person.theme {
        case "Blue":
            return Color(hexString: "#1E54B3")
        case "Red":
            return Color(hexString: "#C11326")
        default:
            return nil
        }
    }
}

extension Color {
    init(hexString: String) {
        let hex = hexString.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }

        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
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

//
//  iOSApp.swift
//  NAS Rally
//

import SwiftUI
import FirebaseCore
import FirebaseMessaging
import Shared

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        FirebaseApp.configure()
        
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self

        let authOptions: UNAuthorizationOptions = [.alert, .badge, .sound]
        UNUserNotificationCenter.current().requestAuthorization(options: authOptions) { granted, _ in
            if granted {
                DispatchQueue.main.async {
                    application.registerForRemoteNotifications()
                    
                    // Explicitly fetch the token as well, in case the delegate doesn't fire for cached tokens
                    Messaging.messaging().token { token, error in
                        if let error = error {
                            print("Error fetching FCM registration token: \(error)")
                        } else if let token = token {
                            PushNotificationManager.shared.registerToken(token: token, deviceType: "ios")
                        }
                    }
                }
            }
        }
        
        return true
    }
    
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        if let token = fcmToken {
            PushNotificationManager.shared.registerToken(token: token, deviceType: "ios")
        }
    }
    
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        let userInfo = notification.request.content.userInfo
        let groupId = userInfo["group_id"] as? String ?? ""
        let groupName = userInfo["group_name"] as? String ?? userInfo["title"] as? String ?? "New Message"
        let senderName = userInfo["sender_name"] as? String ?? "Someone"
        let bodyText = notification.request.content.body
        
        let currentGroup = ActiveChatTracker.shared.currentGroupId.value as? String
        if !groupId.isEmpty && groupId == currentGroup {
            completionHandler([])
            return
        }
        
        if UIApplication.shared.applicationState == .active {
            // App is in foreground (inside the app) -> Route to in-app message accumulator
            NotificationManager.shared.onInAppMessageReceived(
                groupId: groupId,
                groupName: groupName,
                senderName: senderName,
                text: bodyText
            )
            completionHandler([])
            return
        }
        
        // App is outside/background -> Check 20-second rate limiter
        if !groupId.isEmpty && !NotificationManager.shared.shouldAllowOutOfAppNotification(groupId: groupId, cooldownMillis: 20000) {
            completionHandler([])
            return
        }
        
        if #available(iOS 14.0, *) {
            completionHandler([.banner, .sound, .badge])
        } else {
            completionHandler([.alert, .sound, .badge])
        }
    }
}

struct OnboardingFlowContainerView: View {
    @Binding var person: PersonInfo
    @State private var step: OnboardingStep = .tos

    var body: some View {
        Group {
            switch step {
            case .signup, .tos:
                TOSView(person: $person) {
                    step = .onboarding
                }
            case .onboarding:
                OnboardingView(person: $person) {
                    step = .idPrompt
                }
            case .idPrompt:
                IDPromptView(
                    onProvideIDNow: {
                        step = .idUpload
                    },
                    onSkip: {
                        person.tos = true
                    }
                )
            case .idUpload:
                IDView(person: $person, isOnboarding: true) {
                    person.tos = true
                }
            }
        }
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    @State private var personInfo: PersonInfo? = nil
    @State private var isLoading = true
    
    var body: some Scene {
        WindowGroup {
            Group {
                if isLoading {
                    ProgressView("Loading Profile...")
                } else if let person = personInfo {
                    if !person.tos {
                        OnboardingFlowContainerView(
                            person: Binding(
                                get: { person },
                                set: { personInfo = $0 }
                            )
                        )
                    } else {
                        ContentView(
                            person: Binding(
                                get: { person },
                                set: { personInfo = $0 }
                            )
                        )
                    }
                } else {
                    LoginView(person: Binding(
                        get: { personInfo ?? PersonInfo(id: UUID(), name: "", theme: "Auto", bio: "", ralliesJoined: 0, rallieNames: [], privligeLevel: "", tos: false, instaHandle: "", carModel: "", phoneNumber: "") },
                        set: { personInfo = $0 }
                    ))
                }
            }
            .tint(themeTint)
            .task {
                await loadSession()
            }
        }
    }

    private var themeTint: Color? {
        switch personInfo?.theme {
        case "Blue":
            return Color(hexString: "#1E54B3")
        case "Red":
            return Color(hexString: "#C11326")
        default:
            return nil
        }
    }
    
    private func loadSession() async {
        do {
            self.personInfo = try await fetchCurrentProfile()
        } catch {
            print("No active session or error fetching profile: \(error)")
        }
        self.isLoading = false
    }
}

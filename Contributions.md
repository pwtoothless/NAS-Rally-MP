# Contributions & Architecture Guide

Welcome to the NASRally repository documentation! This document provides an exhaustive breakdown of the project structure, architectural design, and a complete catalog of every source file, class, data model, object, function, and composable across the entire multiplatform codebase.

---

## Architecture & Project Structure

NASRally is built using **Kotlin Multiplatform (KMP)** and **Compose Multiplatform** targeting Android, iOS, JVM Desktop, and Wasm/JS Web browsers, alongside a native **SwiftUI iOS application**.

### Sub-projects and Modules

- **`:shared`**: Core business logic, data models, Supabase API client integration, state management (ViewModels), Material 3 UI composables, themes, and platform expect/actual implementations (`commonMain`, `androidMain`, `iosMain`, `jvmMain`, `jsMain`, `wasmJsMain`).
- **`:androidApp`**: Android platform entry module containing `MainActivity` and `AppFirebaseMessagingService`.
- **`:desktopApp`**: Compose Desktop JVM application launcher.
- **`:webApp`**: Wasm/JS Web application launcher rendering via `ComposeViewport`.
- **`iosApp/`**: Native SwiftUI iOS application consuming shared Kotlin logic via framework bindings.

---

## File, Class, and Function Catalog

---

### 1. `androidApp` Module

#### [androidApp/src/main/kotlin/com/nasrally/nasrally/MainActivity.kt](file:///Users/michael/AndroidStudioProjects/NASRally-Universal/androidApp/src/main/kotlin/com/nasrally/nasrally/MainActivity.kt)
- **File Purpose**: Main Android Activity entry point for NASRally. Configures edge-to-edge window framing, notification permissions, FCM push token registration, and sets up Compose UI content.
- **Classes**:
  - `MainActivity`: ComponentActivity subclass acting as the primary Android Activity.
- **Functions**:
  - `onCreate(savedInstanceState: Bundle?)`: Lifecycle callback initializing layout framing, asking notification permissions, and setting root Compose content `App()`.
  - `askNotificationPermission()`: Prompts for `POST_NOTIFICATIONS` runtime permission on Android 13+ (API 33) or fetches FCM token on earlier versions.
  - `fetchFCMToken()`: Retrieves FCM registration token from `FirebaseMessaging` instance and sends token to `PushNotificationManager`.
  - `AppAndroidPreview()`: Composable preview for Android Studio.

#### [androidApp/src/main/kotlin/com/nasrally/nasrally/AppFirebaseMessagingService.kt](file:///Users/michael/AndroidStudioProjects/NASRally-Universal/androidApp/src/main/kotlin/com/nasrally/nasrally/AppFirebaseMessagingService.kt)
- **File Purpose**: Handles Firebase Cloud Messaging (FCM) push token refreshes and incoming background/foreground notification payloads.
- **Classes**:
  - `AppFirebaseMessagingService`: `FirebaseMessagingService` implementation.
- **Functions**:
  - `onNewToken(token: String)`: Called when FCM issues a new token; registers token with `PushNotificationManager`.
  - `onMessageReceived(message: RemoteMessage)`: Parses push message payload, checks `ActiveChatTracker` to suppress alerts if viewing the thread, queues in-app banners via `NotificationManager` when in foreground, or enforces rate-limiting for out-of-app system notifications.

---

### 2. `desktopApp` Module

#### [desktopApp/src/main/kotlin/com/nasrally/nasrally/main.kt](file:///Users/michael/AndroidStudioProjects/NASRally-Universal/desktopApp/src/main/kotlin/com/nasrally/nasrally/main.kt)
- **File Purpose**: Desktop JVM application entry point.
- **Functions**:
  - `main()`: Launches Compose Desktop window (1280x800) hosting the root `App()` composable.

---

### 3. `webApp` Module

#### [webApp/src/webMain/kotlin/com/nasrally/nasrally/main.kt](file:///Users/michael/AndroidStudioProjects/NASRally-Universal/webApp/src/webMain/kotlin/com/nasrally/nasrally/main.kt)
- **File Purpose**: Web Wasm/JS application entry point.
- **Functions**:
  - `main()`: Initializes `ComposeViewport` canvas element hosting `WebApp()`.

---

### 4. `shared` Module — `commonMain` Source Set

#### [shared/src/commonMain/kotlin/com/nasrally/nasrally/App.kt](file:///Users/michael/AndroidStudioProjects/NASRally-Universal/shared/src/commonMain/kotlin/com/nasrally/nasrally/App.kt)
- **File Purpose**: Root multiplatform Compose entry component. Directs desktop/web platforms to `WebApp`, verifies user authentication session, handles onboarding terms-of-service flows, and renders the mobile application scaffold.
- **Composables**:
  - `App()`: Root application composable managing session initialization, theme application, and platform routing.
  - `ContentView(personInfo: PersonInfo, onUpdatePerson: (PersonInfo) -> Unit, onLogout: () -> Unit)`: Scaffold layout with bottom navigation bar for Home, Rallies, Chat, Wavers, Settings, and Admin views.
  - `SubtleInAppBannerOverlay(data: InAppBannerData, onBannerClick: () -> Unit, onDismiss: () -> Unit)`: Floating overlay banner for in-app chat message notifications.

#### [shared/src/commonMain/kotlin/com/nasrally/nasrally/ActiveChatTracker.kt](file:///Users/michael/AndroidStudioProjects/NASRally-Universal/shared/src/commonMain/kotlin/com/nasrally/nasrally/ActiveChatTracker.kt)
- **File Purpose**: Thread-safe active chat thread state tracker.
- **Objects**:
  - `ActiveChatTracker`: Singleton holding `currentGroupId` state flow.
- **Functions & Properties**:
  - `currentGroupId`: `StateFlow<String?>` holding currently viewed group ID.
  - `setActiveGroup(groupId: String?)`: Sets or clears active group ID.

#### [shared/src/commonMain/kotlin/com/nasrally/nasrally/CalendarFunctions.kt](file:///Users/michael/AndroidStudioProjects/NASRally-Universal/shared/src/commonMain/kotlin/com/nasrally/nasrally/CalendarFunctions.kt)
- **File Purpose**: Calendar export data models, iCalendar (.ics) string formatting utilities, and expect object declaration for native calendar integration.
- **Data Classes**:
  - `RallyCalendarEvent`: Model representing a calendar event (`id`, `title`, `description`, `startDateIso`, `endDateIso`).
- **Objects**:
  - `CalendarManager`: `expect` object declaring `addEventToCalendar(event: RallyCalendarEvent)`.
- **Functions**:
  - `generateIcsContent(event: RallyCalendarEvent)`: Generates iCalendar (.ics) string payload.
  - `formatIcsDate(dateStr: String?)`: Converts ISO date string to UTC iCalendar date format.

#### [shared/src/commonMain/kotlin/com/nasrally/nasrally/ChatViewModel.kt](file:///Users/michael/AndroidStudioProjects/NASRally-Universal/shared/src/commonMain/kotlin/com/nasrally/nasrally/ChatViewModel.kt)
- **File Purpose**: StateViewModel managing message loading, pagination, local message disk caching, read receipts, sender profile resolution, and Supabase Realtime live subscriptions.
- **Classes**:
  - `ChatViewModel`: ViewModel class initialized with `groupId` and `CoroutineScope`.
- **Functions & Properties**:
  - `messages`: `StateFlow<List<Message>>` emitting chat messages.
  - `groupMembers`: `StateFlow<List<PersonInfo>>` emitting group member profiles.
  - `profilesMap`: `StateFlow<Map<String, PersonInfo>>` mapping user IDs to profiles.
  - `readReceipts`: `StateFlow<Map<String, List<ReadReceipt>>>` mapping message IDs to read receipts.
  - `isLoading`: `StateFlow<Boolean>` emitting message fetch loading state.
  - `loadCachedMessages()`: Loads top 50 cached messages from local cache.
  - `saveCachedMessages(list: List<Message>)`: Saves top 50 messages to local cache.
  - `loadGroupMembers()`: Queries `group_members` and `profiles` from Supabase.
  - `setupRealtimeSubscription()`: Connects Supabase Realtime channels for live postgres inserts into `messages` and `read_receipts` tables.
  - `loadMessages(isRefresh: Boolean, currentUserId: String?)`: Paged fetch of message history.
  - `loadReadReceipts(messageIds: List<String>)`: Queries read receipt rows from database.
  - `markMessagesAsRead(currentUserId: String)`: Inserts read receipt entries for unread messages.
  - `sendMessage(content: String, senderId: String)`: Inserts new message into Supabase database.

#### [shared/src/commonMain/kotlin/com/nasrally/nasrally/Greeting.kt](file:///Users/michael/AndroidStudioProjects/NASRally-Universal/shared/src/commonMain/kotlin/com/nasrally/nasrally/Greeting.kt)
- **File Purpose**: Basic platform greeting utility class.
- **Classes**:
  - `Greeting`: Helper class obtaining platform name.
- **Functions**:
  - `greet()`: Returns formatted hello greeting string.

#### [shared/src/commonMain/kotlin/com/nasrally/nasrally/GreetingUtil.kt](file:///Users/michael/AndroidStudioProjects/NASRally-Universal/shared/src/commonMain/kotlin/com/nasrally/nasrally/GreetingUtil.kt)
- **File Purpose**: Top-level string formatting function.
- **Functions**:
  - `sayHello(to: String)`: Returns greeting string formatted with parameter `to`.

#### [shared/src/commonMain/kotlin/com/nasrally/nasrally/LocalCache.kt](file:///Users/michael/AndroidStudioProjects/NASRally-Universal/shared/src/commonMain/kotlin/com/nasrally/nasrally/LocalCache.kt)
- **File Purpose**: Multiplatform `expect` object for key-value string and long preference storage.
- **Objects**:
  - `LocalCache`: `expect` object declaring `getString`, `setString`, `getLong`, `setLong`.

#### [shared/src/commonMain/kotlin/com/nasrally/nasrally/LocalDiskCache.kt](file:///Users/michael/AndroidStudioProjects/NASRally-Universal/shared/src/commonMain/kotlin/com/nasrally/nasrally/LocalDiskCache.kt)
- **File Purpose**: Multiplatform `expect` object for binary disk blob and timestamp file caching.
- **Objects**:
  - `LocalDiskCache`: `expect` object declaring `getBytes`, `saveBytes`, `getTimestamp`, `saveTimestamp`, `remove`.

#### [shared/src/commonMain/kotlin/com/nasrally/nasrally/Models.kt](file:///Users/michael/AndroidStudioProjects/NASRally-Universal/shared/src/commonMain/kotlin/com/nasrally/nasrally/Models.kt)
- **File Purpose**: Domain models and serializable database DTO structures for Supabase communication.
- **Classes & Sealed Hierarchies**:
  - `PersonInfo`: Core user profile model (`id`, `name`, `theme`, `bio`, `ralliesJoined`, `rallieNames`, `privligeLevel`, `tos`, `instaHandle`, `carModel`, `phoneNumber`).
  - `SupabasePersonRow`: Database row structure for `profiles` table.
  - `NewSupabasePersonRow`: Database insert structure for new profiles.
  - `SupabaseProfileUpdateRow`: Database update structure for profiles.
  - `Message`: Chat message record (`id`, `groupId`, `senderId`, `content`, `createdAt`).
  - `DatabaseRally`: Rally event row (`id`, `name`, `description`, `eventStart`, `eventEnd`, `eventImage`, `eventCost`).
  - `WaiverRallyInfo`: Nested rally name structure for waiver queries.
  - `Waiver`: Event waiver document structure.
  - `SignedWaiverRow`: Database record for signed waivers.
  - `SignedWaiverInsertRow`: Insertion model for signing waivers.
  - `UserWaiversResult`: Container for pending and signed waiver lists.
  - `RallyInsertRow`, `RallyUpdateRow`: Rally administration models.
  - `GroupInsertRow`, `GroupUpdateRow`, `GroupRow`, `GroupMemberRow`, `GroupMemberInsert`: Chat group administration models.
  - `WaiverInsertRow`: Waiver creation model.
  - `SensitiveInfoRow`: Payment/sensitive detail model.
  - `AdminProfile`: Administrative profile management model.
  - `RallyRequestRow`, `RallyRequestInsert`: Rally join request models.
  - `RallyParticipantRow`, `RallyParticipantInsert`: Rally participant models.
  - `ReadReceipt`, `ReadReceiptInsert`: Message read receipt models.
  - `AuthResult`: Sealed class (`Success(person)`, `Failure(message)`).

#### [shared/src/commonMain/kotlin/com/nasrally/nasrally/NotificationManager.kt](file:///Users/michael/AndroidStudioProjects/NASRally-Universal/shared/src/commonMain/kotlin/com/nasrally/nasrally/NotificationManager.kt)
- **File Purpose**: In-app banner notification accumulation, rate limiting, and foreground state tracker.
- **Data Classes**:
  - `InAppBannerData`: Banner data payload (`groupId`, `groupName`, `unreadCount`, `latestSenderName`, `latestMessageText`).
- **Objects**:
  - `NotificationManager`: Singleton tracking active notifications.
- **Functions & Properties**:
  - `isAppInForeground`: Boolean flag indicating app visibility.
  - `inAppBannerState`: `StateFlow<InAppBannerData?>` emitting active floating banner.
  - `shouldAllowOutOfAppNotification(groupId: String, cooldownMillis: Long)`: Rate-limits system notifications (e.g. 20s gap).
  - `onInAppMessageReceived(groupId: String, groupName: String, senderName: String, text: String)`: Accumulates unread count and triggers banner overlay every 10-20 messages.
  - `dismissInAppBanner()`: Clears active in-app banner.
  - `resetGroupMessageCount(groupId: String)`: Resets unread counter for specified group.

#### [shared/src/commonMain/kotlin/com/nasrally/nasrally/Platform.kt](file:///Users/michael/AndroidStudioProjects/NASRally-Universal/shared/src/commonMain/kotlin/com/nasrally/nasrally/Platform.kt)
- **File Purpose**: Expect platform declaration interface.
- **Interfaces**:
  - `Platform`: Interface declaring `val name: String`.
- **Functions**:
  - `getPlatform()`: `expect` factory function returning platform instance.

#### [shared/src/commonMain/kotlin/com/nasrally/nasrally/PushNotificationManager.kt](file:///Users/michael/AndroidStudioProjects/NASRally-Universal/shared/src/commonMain/kotlin/com/nasrally/nasrally/PushNotificationManager.kt)
- **File Purpose**: FCM token registration singleton writing tokens to Supabase `device_tokens` table.
- **Data Classes**:
  - `DeviceTokenRow`: Serialized structure (`user_id`, `fcm_token`, `device_type`).
- **Objects**:
  - `PushNotificationManager`: Registration manager.
- **Functions**:
  - `registerToken(token: String, deviceType: String)`: Upserts FCM token to Supabase `device_tokens` table.

#### [shared/src/commonMain/kotlin/com/nasrally/nasrally/SupabaseClient.kt](file:///Users/michael/AndroidStudioProjects/NASRally-Universal/shared/src/commonMain/kotlin/com/nasrally/nasrally/SupabaseClient.kt)
- **File Purpose**: Main Supabase client setup and global network API helper functions.
- **Objects & Properties**:
  - `supabase`: Shared `SupabaseClient` instance configured with Auth, Postgrest, Realtime, and Storage.
  - `ProfileImageMemoryCache`: Memory cache for decoded user profile bitmaps.
  - `RallyLogoMemoryCache`: Memory cache for decoded rally logo bitmaps.
- **Functions**:
  - `fetchCurrentProfile()`: Fetches current logged-in user profile.
  - `loadPersonInfo(userId: String)`: Loads user profile and joined rally names.
  - `loadPersonInfoOrCreateDefault(userId: String, name: String)`: Loads profile or creates default row if missing.
  - `login(emailInput: String, passwordInput: String)`: Performs email/password sign-in.
  - `signup(nameInput: String, emailInput: String, passwordInput: String)`: Performs user sign-up.
  - `logout()`: Signs out current session.
  - `deleteUserAccount(userId: String)`: Deletes user via RPC or updates status.
  - `updateTheme(userId: String, theme: String)`: Updates theme preference in database.
  - `updateTOS(userId: String, tos: Boolean)`: Updates TOS acceptance status.
  - `updateProfile(person: PersonInfo)`: Updates user profile detail fields.
  - `getProfileImageURL(userId: String)`: Generates public image URL for profile photo.
  - `getRallyImageURL(name: String)`: Generates public image URL for rally logo.
  - `fetchUserWaivers(userId: String)`: Fetches user pending and signed waivers.
  - `fetchUserIDImageData(userId: String)`: Downloads user ID document bytes.
  - `getCachedRallyLogoData(name: String)`: Retrieves or downloads rally logo bytes with 7-day disk cache.
  - `getCachedProfileImageBitmap(userId: String)`: Downloads and caches profile image bitmap.
  - `getCachedProfileImageData(userId: String)`: Downloads and caches raw profile image bytes.
  - `invalidateCachedProfileImage(userId: String)`: Flushes cached profile picture.
  - `getCachedUserIDImageData(userId: String, isAdmin: Boolean)`: Downloads user ID image for admin view.
  - `loadSensitiveInfo()`, `saveSensitiveInfo(row: SensitiveInfoRow)`: Administrative payment details helpers.
  - `fetchAISummary()`: Invokes Supabase Edge Function `ai-summarization`.

#### [shared/src/commonMain/kotlin/com/nasrally/nasrally/theme/AppTheme.kt](file:///Users/michael/AndroidStudioProjects/NASRally-Universal/shared/src/commonMain/kotlin/com/nasrally/nasrally/theme/AppTheme.kt)
- **File Purpose**: Custom dynamic Material 3 color schemes ("Auto", "Blue", "Red", "Dark", "Light") and theme wrapper composable.
- **Composables**:
  - `AppTheme(themeName: String, darkTheme: Boolean, content: @Composable () -> Unit)`: Theme wrapper component applying appropriate `ColorScheme`.

#### View Composables in `shared/src/commonMain/.../views/` and `web/`

- **`AdminView.kt`**:
  - Composable: `AdminView(person: PersonInfo)` — Administrative view providing user management, rally creation/editing, role assignment, join request approval, and ID document verification.
- **`AsyncImage.kt`**:
  - Composables: `AsyncImage` (URL and ByteArray variants), `CachedRallyLogoImage`, `CachedProfileImage`.
- **`AuthView.kt`**:
  - Composables: `AuthView(onLoginSuccess: (PersonInfo) -> Unit)`, `LoginContent`, `SignupContent`.
- **`ChatView.kt`**:
  - Composables: `ChatView(person: PersonInfo)`, `MessageThreadView`, `MessageBubble`, `ReadReceiptsDialog`, `UserProfileSheetDialog`.
- **`HomeView.kt`**:
  - Composable: `HomeView(person: PersonInfo)` — Dashboard showing user profile banner, AI-generated event summary, and pull-to-refresh controls.
- **`IDView.kt`**:
  - Composables: `IDView(person: PersonInfo, isOnboarding: Boolean, onComplete: (() -> Unit)?)`, `IDCardPreview`.
- **`ImagePicker.kt`**:
  - Enum: `ImageSource` (`CAMERA`, `GALLERY`).
  - Composable Function: `rememberImagePicker(source: ImageSource, onImagePicked: (ByteArray) -> Unit): () -> Unit` (`expect`).
- **`OnboardingViews.kt`**:
  - Enum: `OnboardingStep` (`TOS`, `ONBOARDING`, `ID_PROMPT`, `ID_UPLOAD`).
  - Composables: `OnboardingFlowContainer`, `TOSView`, `OnboardingView`, `IDPromptView`.
- **`ProfileView.kt`**:
  - Composable: `ProfileView(person: PersonInfo, onUpdatePerson: (PersonInfo) -> Unit)`.
- **`RalliesView.kt`**:
  - Composables: `RalliesView(person: PersonInfo)`, `RallyUserDetailDialog`.
- **`SettingsView.kt`**:
  - Sealed Class: `SettingsSubScreen` (`Main`, `Profile`, `ID`).
  - Composables: `SettingsView`, `SettingsRow`.
- **`WaversView.kt`**:
  - Composables: `WaversView(person: PersonInfo)`, `WaiverDetailDialog`.
- **`web/WebAppView.kt`**:
  - Composable: `WebApp()` — Multi-pane desktop/web workspace layout.

---

### 5. `shared` Platform Implementations (`actual`)

#### `androidMain`
- `CalendarFunctions.android.kt`: `CalendarManager` actual implementation using `Intent.ACTION_INSERT`.
- `LocalCache.android.kt`: `LocalCache` actual implementation backed by `SharedPreferences`.
- `LocalDiskCache.android.kt`: `LocalDiskCache` actual implementation writing files to `context.cacheDir`.
- `Platform.android.kt`: `AndroidPlatform` class and `getPlatform()` actual function.
- `views/ImagePicker.android.kt`: `rememberImagePicker` actual implementation using Activity Result Contracts.

#### `iosMain`
- `CalendarFunctions.ios.kt`: `CalendarManager` actual implementation launching data calendar URLs.
- `LocalCache.ios.kt`: `LocalCache` actual implementation backed by `NSUserDefaults`.
- `LocalDiskCache.ios.kt`: `LocalDiskCache` actual implementation writing files to `NSCachesDirectory`.
- `MainViewController.kt`: `MainViewController()` factory function returning `ComposeUIViewController`.
- `Platform.ios.kt`: `IOSPlatform` class and `getPlatform()` actual function.
- `views/ImagePicker.ios.kt`: `rememberImagePicker` actual stub implementation for iOS.

#### `jvmMain`
- `CalendarFunctions.jvm.kt`: `CalendarManager` actual implementation writing temp .ics files and invoking `Desktop.getDesktop().open()`.
- `LocalCache.jvm.kt`: `LocalCache` actual implementation backed by `java.util.prefs.Preferences`.
- `LocalDiskCache.jvm.kt`: `LocalDiskCache` actual implementation writing files to `java.io.tmpdir`.
- `Platform.jvm.kt`: `JVMPlatform` class and `getPlatform()` actual function.
- `views/ImagePicker.jvm.kt`: `rememberImagePicker` actual implementation using AWT `FileDialog`.

#### `jsMain`
- `CalendarFunctions.js.kt`: `CalendarManager` actual implementation triggering DOM blob downloads.
- `LocalCache.js.kt`: `LocalCache` actual implementation backed by `localStorage`.
- `LocalDiskCache.js.kt`: `LocalDiskCache` actual implementation backed by in-memory map.
- `Platform.js.kt`: `JsPlatform` class and `getPlatform()` actual function parsing userAgent.
- `views/ImagePicker.js.kt`: `rememberImagePicker` actual implementation using DOM `<input type="file">`.

#### `wasmJsMain`
- `CalendarFunctions.wasmJs.kt`: `CalendarManager` actual implementation invoking JS interop `jsDownloadIcsFile`.
- `LocalCache.wasmJs.kt`: `LocalCache` actual implementation backed by `localStorage`.
- `LocalDiskCache.wasmJs.kt`: `LocalDiskCache` actual implementation backed by in-memory map.
- `Platform.wasmJs.kt`: `WasmPlatform` class and `getPlatform()` actual function.
- `views/ImagePicker.wasmJs.kt`: `rememberImagePicker` actual implementation invoking `jsLaunchImagePicker`.

---

### 6. `iosApp` Native Swift Application

- **`iOSApp.swift`**:
  - `AppDelegate`: `UIApplicationDelegate` managing Firebase setup, APNs registration, FCM tokens, and foreground notification handling.
  - `OnboardingFlowContainerView`: SwiftUI container managing onboarding steps.
  - `iOSApp`: `@main` App structure hosting session initialization and root view hierarchy.
- **`ContentView.swift`**:
  - `ContentView`: Primary SwiftUI tab navigation bar component.
  - `SubtleInAppBannerView`: Floating banner overlay component for chat notifications.
  - `GlassEffectModifier`, `View.glassEffectCompat`: Backward-compatible glass visual effect modifiers.
- **`ChatViewModel.swift`**:
  - `ChatViewModel`: `@MainActor` `ObservableObject` managing real-time chat subscriptions, message pagination, read receipts, and profile resolution.
- **`Background Stuff/CacheManager.swift`**:
  - `MemoryImageCache`: `NSCache` wrapper singleton for images.
  - `CacheManager`: Disk cache manager handling binary file storage in `CachesDirectory`.
  - `CachedRallyLogoView`: SwiftUI image component for rally logos.
  - `CachedProfileImageView`: SwiftUI image component for profile photos.
- **`Background Stuff/CalendarManager.swift`**:
  - `CalendarManager`: `EKEventStore` wrapper adding events to native iOS System Calendar.
- **`Background Stuff/Objects.swift`**:
  - `Color(hexString:)`: Hex string color initializer extension.
  - `PersonInfo`: Swift struct representing user profile.
  - `SensitiveInfoRow`, `Message`, `ReadReceipt`, `GroupMemberRow`, `RallyRow`: Codable Swift data models.
- **`Background Stuff/Supabase.swift`**:
  - `supabase`: Shared `SupabaseClient` instance.
  - Helper functions: `logout()`, `deleteUserAccount()`, `fetchCurrentProfile()`, `login()`, `signup()`, `updateProfile()`, `fetchUserWaivers()`, `fetchAISummary()`.
- **`Menus/` Views**:
  - `Admin.swift`: SwiftUI administrative management interface.
  - `Chat.swift`: SwiftUI chat list and thread view.
  - `Home.swift`: SwiftUI dashboard and AI summary view.
  - `Login-Signup.swift`: SwiftUI authentication views.
  - `Onboarding.swift`: SwiftUI user detail onboarding form.
  - `Profile.swift`: SwiftUI profile editor view.
  - `Rallies.swift`: SwiftUI rally listing and join request view.
  - `Sensitive Info.swift`: SwiftUI payment detail management view.
  - `Settings.swift`: SwiftUI app settings view.
  - `Wavers.swift`: SwiftUI waiver list and signing view.

import SwiftUI
import Shared
import FirebaseCore

@main
struct iOSApp: App {
    init() {
        // Initialize Firebase FIRST (before Koin, which depends on Firebase services)
        FirebaseApp.configure()

        // Create Firebase providers that implement Kotlin interfaces
        let identityProvider = FirebaseInstallationIdentityProvider()
        let analyticsProvider = FirebaseAnalyticsProvider()
        let crashReporterProvider = FirebaseCrashReporterProvider()

        // Initialize Koin DI with Firebase providers
        MainViewControllerKt.doInitKoin(
            identityProvider: identityProvider,
            analyticsProvider: analyticsProvider,
            crashReporterProvider: crashReporterProvider
        )
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
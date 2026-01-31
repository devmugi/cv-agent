import Foundation
import FirebaseCore
import FirebaseAnalytics
import FirebaseCrashlytics
import FirebaseInstallations
import Shared

// MARK: - Installation Identity Provider

/// Swift implementation of Kotlin's InstallationIdentityProvider interface.
/// Uses Firebase Installations SDK to get stable installation ID.
class FirebaseInstallationIdentityProvider: InstallationIdentityProvider {
    func getInstallationId(completion: @escaping (String?, String?) -> Void) {
        Installations.installations().installationID { id, error in
            if let error = error {
                completion(nil, error.localizedDescription)
            } else {
                completion(id, nil)
            }
        }
    }
}

// MARK: - Analytics Provider

/// Swift implementation of Kotlin's AnalyticsProvider interface.
/// Uses Firebase Analytics SDK.
class FirebaseAnalyticsProvider: AnalyticsProvider {
    func logEvent(name: String, parameters: [String: Any]?) {
        Analytics.logEvent(name, parameters: parameters)
    }

    func setUserId(userId: String?) {
        Analytics.setUserID(userId)
    }

    func setUserProperty(name: String, value: String?) {
        Analytics.setUserProperty(value, forName: name)
    }

    func setCurrentScreen(screenName: String, screenClass: String?) {
        Analytics.logEvent(AnalyticsEventScreenView, parameters: [
            AnalyticsParameterScreenName: screenName,
            AnalyticsParameterScreenClass: screenClass ?? ""
        ])
    }
}

// MARK: - Crash Reporter Provider

/// Swift implementation of Kotlin's CrashReporterProvider interface.
/// Uses Firebase Crashlytics SDK.
class FirebaseCrashReporterProvider: CrashReporterProvider {
    func recordException(domain: String, code: Int32, message: String) {
        let error = NSError(
            domain: domain,
            code: Int(code),
            userInfo: [NSLocalizedDescriptionKey: message]
        )
        Crashlytics.crashlytics().record(error: error)
    }

    func log(message: String) {
        Crashlytics.crashlytics().log(message)
    }

    func setCustomKey(key: String, value: String) {
        Crashlytics.crashlytics().setCustomValue(value, forKey: key)
    }

    // Note: Kotlin exports parameter as 'userId_' to avoid Swift keyword conflict
    func setUserId(userId_: String) {
        Crashlytics.crashlytics().setUserID(userId_)
    }
}

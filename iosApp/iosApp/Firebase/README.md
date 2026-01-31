# Firebase Configuration

Download `GoogleService-Info.plist` from Firebase Console for each environment:

## Dev Environment
1. Go to Firebase Console → cv-agent-dev project
2. Add iOS app with bundle ID: `io.github.devmugi.cv.agent.dev`
3. Download `GoogleService-Info.plist`
4. Place it in `Dev/GoogleService-Info.plist`

## Prod Environment
1. Go to Firebase Console → cv-agent-26a21 project
2. Add iOS app with bundle ID: `io.github.devmugi.cv.agent`
3. Download `GoogleService-Info.plist`
4. Place it in `Prod/GoogleService-Info.plist`

## Build Phase
The correct plist is copied to the app bundle based on build configuration:
- Debug builds → Dev/GoogleService-Info.plist
- Release builds → Prod/GoogleService-Info.plist

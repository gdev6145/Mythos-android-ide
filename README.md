# MYTHOS Android IDE

A self-contained Android app that integrates MYTHOS-26B with Termux for offline AI code assistance.

## Download APK

[Click here to download the APK](https://github.com/gdev6145/mythos-android-ide/releases/download/v1.0/mythos-ide.apk)

## Installation Steps

1. **Download the APK** to your phone
2. **Open your file manager**
3. **Tap the APK file** (mythos-ide.apk)
4. **Allow installation** from unknown sources
5. **Open the MYTHOS IDE app**
6. **Tap "Install MYTHOS-26B"**
7. **Wait for setup** to complete
8. **Start coding!**

## Features

- Single APK install
- Termux embedded (no separate app needed)
- MYTHOS-26B auto-setup on first run
- Background model service with foreground notification
- Code editor with dark theme, line numbers, and file save/load
- File explorer for browsing and opening project files
- Settings screen for editor and model preferences
- Offline mode support

## Project Structure

```
mythos-android-ide/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/mythos/ide/
│       │   ├── MainActivity.kt
│       │   ├── CodeEditorActivity.kt
│       │   ├── FileExplorerActivity.kt
│       │   ├── SettingsActivity.kt
│       │   ├── services/
│       │   │   └── ModelService.kt
│       │   └── util/
│       │       └── TermuxBridge.kt
│       └── res/
│           ├── drawable/
│           │   ├── ic_launcher_background.xml
│           │   └── ic_launcher_foreground.xml
│           ├── layout/
│           │   ├── activity_main.xml
│           │   ├── activity_editor.xml
│           │   ├── activity_file_explorer.xml
│           │   ├── activity_settings.xml
│           │   └── item_file.xml
│           ├── mipmap-anydpi-v26/
│           │   ├── ic_launcher.xml
│           │   └── ic_launcher_round.xml
│           └── values/
│               ├── colors.xml
│               ├── strings.xml
│               └── styles.xml
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
└── gradle/wrapper/
    └── gradle-wrapper.properties
```

## Building from Source

Prerequisites: JDK 17, Android SDK 34

```bash
git clone https://github.com/gdev6145/mythos-android-ide.git
cd mythos-android-ide
./gradlew assembleDebug
```

The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Tech Stack

- **Language:** Kotlin
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)
- **Dependencies:** AndroidX AppCompat, Material Components, RecyclerView, Kotlin Coroutines

## License

See [LICENSE](LICENSE) for details.

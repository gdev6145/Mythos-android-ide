# Mythos-android-ide
# MYTHOS Android IDE  A self-contained Android app that integrates MYTHOS-26B with Termux for offline AI code assistance.
# MYTHOS Android IDE

A self-contained Android app that integrates MYTHOS-26B with Termux for offline AI code assistance.

## 📥 Download APK
[Click here to download the APK](https://github.com/gdev6145/mythos-android-ide/releases/download/v1.0/mythos-ide.apk)

## 📋 Installation Steps
1. **Download the APK** to your phone
2. **Open your file manager**
3. **Tap the APK file** (mythos-ide.apk)
4. **Allow installation** from unknown sources
5. **Open the MYTHOS IDE app**
6. **Tap "Install MYTHOS-26B"**
7. **Wait 5 seconds** for setup to complete
8. **Start coding!**

## ✨ Features
- ✅ Single APK - install once, works forever
- ✅ Termux embedded (no separate app needed)
- ✅ MYTHOS-26B auto-setup on first run
- ✅ Background model service
- ✅ Simple code editor
- ✅ Offline mode support

## 🔧 For Developers
To build from source:
```bash
pkg install git openjdk-17
git clone https://github.com/gdev6145/mythos-android-ide.git
cd mythos-android-ide
./gradlew assembleDebug

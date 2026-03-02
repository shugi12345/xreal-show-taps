# XREAL Tap Toggle

Android app that:

- watches for an external display, which is how XREAL glasses usually appear on Android
- uses Shizuku to turn `Show taps` on when connected
- restores the previous `Show taps` value when disconnected

## Why Shizuku

Samsung blocks normal apps from writing `show_touches`, even with `Modify system settings`. Shizuku solves that by running the change through the Android `shell` user.

## Setup

1. Install Shizuku on the phone from https://shizuku.rikka.app/download/
2. Start Shizuku on the phone.
3. Open this project in Android Studio.
4. Build and install the app on the phone.
5. Open the app.
6. Tap `Open or Install Shizuku` if Shizuku is not already running.
7. Tap `Start monitoring`.
8. When Shizuku asks for permission, allow it.
9. Plug in the XREAL glasses.

## Notes

- On non-rooted phones, Shizuku needs to be started again after reboot.
- Detection is based on "any non-default display connected", so other external displays can also trigger it.
- The app stores the previous `Show taps` value and restores it when the display disconnects.

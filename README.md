# mOS 1.0 — Android launcher

An independent Home app (launcher) for Android, tuned for the Samsung Galaxy A06 (720 × 1600).
It is a normal installable app. It needs no root, no unlocked bootloader and no firmware changes,
and uninstalling it brings back Samsung's own Home app.

## How v1.0 is built

The mOS interface is the **exact mOS artifact page** (`app/src/main/assets/web/index.html`; same file as
`prototype/index.html`). It runs inside a WebView in `MainActivity`, which is the Home activity.
When the page detects the APK it switches to *device mode*:
- full screen with Android's real status/navigation bars
- the phone's real apps and icons instead of the sample apps
- real actions (launch, app info, Control Center)

Opened in a normal browser, the same file still runs as the clickable prototype.

| Piece | File |
|---|---|
| Home activity, WebView host, Back/Home, lifecycle | `MainActivity.kt` |
| WebView setup, local-only loading, renderer-crash recovery | `webapp/MosWebView.kt` |
| Restricted JS bridge (origin-checked WebMessageListener) | `webapp/NativeBridge.kt` |
| Real app icons served to the page as PNG (`/icons/…`) | `webapp/IconPathHandler.kt` |
| App discovery, launch, install/uninstall updates | `data/apps/LauncherAppsRepository.kt` |
| First-run Home layout from your default apps | `data/apps/RoleAppsResolver.kt` |
| Flashlight, Do not disturb, rotation, brightness, volume | `launcher/SystemControls.kt` |
| Default-Home request (RoleManager) | `launcher/DefaultHomeHelper.kt` |
| Compose home screen, used only if the phone's WebView is too old | `NativeHomeActivity.kt`, `ui/` |
| Bundled Inter font (SIL OFL 1.1) | `assets/web/fonts/` |

### Bridge messages (page → Android)
`ready`, `launch{key,rect}`, `appInfo{key}`, `system{what}` (settings, wallpaper, alarms, calendar,
homeSettings, wifi, bluetooth), `requestDefault`, `bars{dark}`, `wallpaper{system}`, `haptic`, `clearIcons`,
`control{name,on|v,final}` (torch, dnd, rot, bright, vol).
Android → page: `window.mOS.receive({t: apps|state|insets|toast|cmd})`.
Only the local origin `https://appassets.androidplatform.net` can post messages, and all other network requests are blocked.

## Permissions and why
- **INTERNET**: WebView needs it to load its local https asset address. Nothing is downloaded.
- **ACCESS_WIFI_STATE, BLUETOOTH (Android ≤11)**: show whether Wi-Fi/Bluetooth are on.
- **ACCESS_NOTIFICATION_POLICY**: Do not disturb tile (you approve it once in Android settings).
- **WRITE_SETTINGS**: brightness and auto-rotate (you turn on "Modify system settings" once).

Wi-Fi and Bluetooth tiles open Android's own panel, because Android doesn't let apps switch them.

## Build the APK (Windows, Android Studio)

1. Unzip, then **File → Open** the `mOS` folder (the one containing `settings.gradle.kts`).
2. Let Gradle sync; Android Studio downloads Gradle 8.11.1, AGP 8.7.3 and the libraries.
   If it offers to upgrade AGP, choose **Don't remind me for this project**.
3. If asked, install **Android SDK Platform 35** in SDK Manager.
4. **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
   Result: `app\build\outputs\apk\debug\app-debug.apk`.
   Command line: `gradlew.bat assembleDebug`

## Install on the Galaxy A06

USB: Settings → About phone → Software information → tap **Build number** 7 times →
Developer options → **USB debugging** on. Connect, accept the prompt, press **Run ▶** in Android Studio
(or `adb install -r app\build\outputs\apk\debug\app-debug.apk`).

Without USB: copy the APK to the phone, open it in My Files, allow "Install unknown apps" for My Files.

Also update **Android System WebView** and **Chrome** in Play Store; mOS needs a WebView from 2020 or later.

## Set mOS as the default Home app

- Tap **Set** under the clock (or mOS Settings → **Set default**) → choose **mOS** in Android's dialog, or
- press Home → choose **mOS** → **Always**, or
- Settings → Apps → **Choose default apps** → **Home app** → **mOS**.

To go back: same screen → **One UI Home**, or uninstall mOS. Samsung firmware is never modified.

## Test checklist on the A06
1. App opens to the mOS Home screen with clock, dock and your real apps.
2. Swipe up: drawer lists all your apps with real icons; search works (also Cyrillic).
3. Tapping any app opens it; pressing Home returns to mOS with the drawer closed.
4. Back closes the drawer/settings/sheets and never leaves mOS.
5. Long-press an app → Add to Home / favourites / App info (opens Android's App info).
6. Swipe down: Control Center. Flashlight works straight away; Do not disturb, brightness and
   auto-rotate ask for permission the first time. Wi-Fi/Bluetooth open Android's panel.
7. mOS Settings → Appearance → wallpaper **Phone** shows your phone's own wallpaper.
8. Install or uninstall any app: it appears or disappears without restarting mOS.
9. Rotate, switch dark mode, reboot: layout, folders and settings are kept.

## Debugging
- Logcat tags: `mOS/Main`, `mOS/Bridge`, `mOS/Web`, `mOS/Apps`, `mOS/Icons`, `mOS/Controls`.
- Inspect the page: with a debug build and the phone connected by USB, open `chrome://inspect` in Chrome on your PC.

## Known limits
- Android's own Recents button replaces the prototype's recents sheet.
- Wi-Fi/Bluetooth can't be switched from inside mOS (Android rule); network names aren't shown (would need location permission).
- Monochrome icon style greys real icons instead of redrawing them.

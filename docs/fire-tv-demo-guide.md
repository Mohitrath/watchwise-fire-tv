# Fire TV demo preparation

## What is in the repository

- Native Android/Kotlin app in `app/` with a Leanback launcher entry, D-pad-focusable controls, search, genre filters, local favorites, details, Media3 playback, and backend settings.
- Responsive browser preview in `backend/public/index.html`.
- Express API in `backend/src/server.js`; the browser preview and native app consume the same catalogue and recommendation endpoints.
- Optional server-side AI recommendations. The default path is deterministic and does not require an API key.

## Build the Android APK

1. Install Android Studio and a compatible JDK 17.
2. Install Android SDK Platform 35 and Build Tools from SDK Manager.
3. Open the repository root in Android Studio and allow Gradle sync.
4. Build **Build > Build Bundle(s) / APK(s) > Build APK(s)**, or install Gradle 8.9 and run `gradle assembleDebug` from the repository root.
5. The APK should be at `app/build/outputs/apk/debug/app-debug.apk`.
6. The GitHub Actions workflow named **Android build** also attempts this build on each push and pull request. Download the `watchwise-tv-debug-apk` artifact if the workflow succeeds.

## Install and test on Fire TV

1. Use a Fire TV device or an accepted Fire TV simulator. A generic Android emulator is useful for debugging but is not proof of Fire TV track compliance.
2. Enable the target's supported developer/ADB debugging option and connect Android Studio/ADB using the target's documented pairing flow.
3. Install the APK using Android Studio or `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
4. Launch **WatchWise TV** from the TV app launcher.
5. Test using only the remote: focus movement, Select, Back, genre chips, search, Details, Save, Play/Pause, and player exit.
6. Confirm the status reads that the catalogue connected. The default API base URL is `https://watchwise-fire-tv.vercel.app`.
7. Try **Recommend for me** and confirm a recommendation dialog appears.
8. Play a sample title. Sample streams are third-party demo assets and may change or become unavailable; verify every stream and its license before submission.

## Browser preview checks

Open https://watchwise-fire-tv.vercel.app/ and verify the catalogue, search, genre chips, favorites, recommendation action, and video modal. Test API routes separately at `/health`, `/api/titles`, `/api/genres`, and `/api-info`.

## Suggested under-3-minute demo recording

- 0:00–0:15 — Show WatchWise TV running on the actual Fire TV device or accepted simulator.
- 0:15–0:40 — Navigate the catalogue with the remote and filter by genre.
- 0:40–1:00 — Search for a title and save a favorite.
- 1:00–1:25 — Ask for recommendations and show why the picks match.
- 1:25–1:55 — Start playback and demonstrate pause/seek/Back navigation.
- 1:55–2:15 — Briefly show the responsive browser preview and API health if useful.
- 2:15–2:45 — Explain the user problem, the shared backend, and what is AI-assisted versus deterministic fallback.

Record the actual target platform, upload the video to a public YouTube/Vimeo URL, and include that URL and any required product feedback in the hackathon submission. Do not imply Fire TV device validation until it has actually been performed.

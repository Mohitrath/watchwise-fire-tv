# WatchWise TV

WatchWise TV is a Fire OS-oriented TV discovery demo built with Kotlin and Android's native UI APIs. It includes remote-friendly controls, a sample catalogue, search, genre filters, locally persisted favourites, Media3 playback, and an optional Node.js recommendation API with deterministic fallback recommendations.

> **Status:** This is an implementation starter. It has not yet been verified on a physical Fire TV device or a Fire TV simulator. Build, install, remote navigation, and playback must be tested on the target platform before claiming hackathon compliance.

## Features

- TV-oriented dark interface with visible focus states and large controls.
- D-pad/Select navigation through genre filters and catalogue entries.
- Search by title, genre, or description.
- Favourites saved on-device.
- Details and Media3 video playback.
- Optional HTTP API for recommendations with local fallback.
- Optional AI provider integration configured only on the backend.

## Project structure

- `app/` — native Android/Kotlin Fire OS app.
- `backend/` — optional Express API and recommendation service.
- `docs/` — architecture, testing, and product-feedback notes.

## Requirements

- Android Studio with a compatible JDK (Android Studio's bundled JDK is recommended).
- Android SDK Platform 35 and Build Tools installed through SDK Manager.
- Internet access for Gradle dependencies and sample streams.
- Node.js 18+ and npm for the optional backend.
- A supported Fire TV device or accepted Fire TV simulator for final platform verification.

## Build the Android app

1. Open the repository root in Android Studio.
2. Allow Gradle sync to finish and install any requested SDK components.
3. Select the `app` run configuration and build/run it on a compatible emulator or target Fire TV device.
4. For a command-line build, run `gradle assembleDebug` from the repository root if Gradle is installed. Add a Gradle Wrapper through Android Studio if you want `gradlew.bat assembleDebug` on Windows.
5. The debug APK is normally generated at `app/build/outputs/apk/debug/app-debug.apk`.

A standard Android emulator is useful for development, but does **not** prove Fire TV compliance. Final testing must be on the intended Fire TV platform or accepted Fire TV/Vega simulator.

## Run the optional backend

```bash
cd backend
npm install
cp .env.example .env
npm start
```

On Windows PowerShell, use `Copy-Item .env.example .env` instead of `cp`.

Check the server:

```bash
curl http://localhost:3000/health
curl http://localhost:3000/api/titles
curl -X POST http://localhost:3000/api/recommendations -H "Content-Type: application/json" -d "{\"genres\":[\"Family\",\"Adventure\"],\"maxDurationMinutes\":15,\"language\":\"English\"}"
```

### Connect the TV app to the backend

The TV device must be able to reach the backend over the network. Do not use `localhost` in the TV app unless the backend runs on the same device. For local development, use the computer's LAN IP only on a trusted test network. For a public demo, deploy the backend behind HTTPS and enter its HTTPS base URL in **Backend settings** inside the app.

Android uses HTTPS-safe networking defaults; HTTP may be blocked. For local testing, use HTTPS or configure a temporary debug-only network security exception. Do not enable cleartext traffic in a production build.

### Optional AI provider

The backend uses an OpenAI-compatible Chat Completions endpoint when `AI_API_KEY` is configured. Set `AI_API_KEY`, `AI_BASE_URL`, and `AI_MODEL` in `backend/.env` to a provider you are authorized to use. Without a key, the backend returns deterministic local recommendations. Keep `.env` out of Git and never put provider credentials in the Android app.

## Sample video notice

The sample catalogue uses publicly accessible sample video URLs for development convenience. Verify availability, terms, and licensing/attribution for every media asset before public distribution or a judged demo. Replace these URLs with media you own or are explicitly licensed to stream. Playback may fail if a URL is removed or network access is unavailable.

## Testing checklist

- [ ] Gradle sync and debug build succeed.
- [ ] App launches on the intended Fire OS device/simulator.
- [ ] Primary controls are reachable using D-pad only.
- [ ] Focus state remains visible and Back navigation is predictable.
- [ ] Search, genre filtering, and favourites work.
- [ ] Sample video starts, pauses, seeks, and exits cleanly.
- [ ] Playback failure displays a useful message.
- [ ] Backend `/health` and `/api/recommendations` work.
- [ ] TV app connects to the backend over the intended network.
- [ ] AI mode and fallback mode have both been tested if AI is configured.
- [ ] Demo video under three minutes shows the app running on the intended platform.

## Hackathon status

The repository is not claimed to be hackathon-compliant until the build, device demonstration, repository materials, and submission requirements have been verified.

## License

MIT. See [LICENSE](LICENSE).

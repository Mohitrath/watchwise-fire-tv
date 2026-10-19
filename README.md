# WatchWise TV

WatchWise TV is a Fire OS-oriented TV discovery demo built with Kotlin and Android's native UI APIs. It includes remote-friendly focusable controls, a sample catalogue, search, genre filters, locally persisted favourites, Media3 playback, and an optional Node.js recommendation API with deterministic fallback recommendations.

> **Status:** Not yet verified on a physical Fire TV device or a Fire TV simulator. Build, install, remote navigation, and playback must be tested on the target platform before claiming hackathon compliance.

## Source archive

The full project archive is available as a release asset or can be downloaded from the project files supplied with this submission. This repository currently contains this README only; the extracted source files will be committed separately.

## Features

- TV-oriented dark interface with visible focus states and large cards.
- D-pad/Select navigation through genre filters and catalogue cards.
- Search by title, genre, or description.
- Favourites saved on-device.
- Detail dialog and Media3 video playback.
- Optional HTTP API for recommendations, with offline fallback.
- Optional AI provider integration configured on the backend.

## Stack

- Kotlin and Android SDK for the Fire OS application
- AndroidX Media3 for playback
- Node.js and Express for the optional backend
- Optional OpenAI-compatible chat-completions API for AI recommendations

## Requirements

- Android Studio and a compatible JDK
- Android SDK Platform 35 and Build Tools
- Node.js 18+ and npm for the optional backend

## Build and test

Open the Android project root in Android Studio, allow Gradle sync to finish, and build the `app` module. For final hackathon validation, install and demonstrate it on the intended Fire TV device or accepted Fire TV simulator; a standard Android emulator alone does not prove compliance.

For the backend:

```bash
cd backend
npm install
cp .env.example .env
npm start
```

On Windows PowerShell, use `Copy-Item .env.example .env`. Keep API keys in the backend `.env` file; never commit secrets or embed them in the Android app.

## Media licensing

Verify availability, terms, and licensing/attribution requirements for every sample media URL before public distribution or a judged demo. Use media you own or are explicitly licensed to stream.

## License

MIT. See `LICENSE` once committed with the source tree.

# Testing plan

## Build
- Sync Gradle and build a debug APK with Android Studio.
- Resolve SDK, dependency, and target-platform issues on the selected Fire OS release.

## Device / simulator
- Install the APK on the intended Fire TV platform.
- Test launcher visibility, landscape layout, remote focus, D-pad movement, Select, Back, and focus restoration.
- Test search, every genre, favourites persistence after restart, video playback, pause, seek, and player exit.
- Test offline mode, missing backend, backend timeout, and invalid backend response.

## API
- `GET /health` returns status `ok`.
- `GET /api/titles` returns the catalogue.
- `GET /api/titles/demo-001` returns a title.
- `GET /api/titles/not-a-title` returns HTTP 404.
- `POST /api/recommendations` returns only known `titleId` values.
- With AI configured, invalid model IDs are discarded and local recommendations are used if necessary.

## Evidence to capture
- Build output and app version.
- Short screen recording showing remote navigation and playback on the required platform.
- API test results and a note identifying which AI mode was used.

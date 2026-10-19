# Architecture

## Components

1. **Android app (`app/`)**: UI, D-pad focus navigation, local title filtering, favourites, and playback.
2. **Recommendation API (`backend/`)**: Express routes for health, catalogue lookup, genres, and recommendations.
3. **AI provider (optional)**: Configured only through backend environment variables. The backend validates all returned title IDs against the known catalogue.
4. **Local fallback**: The API can return deterministic recommendations when no provider key is configured or the provider call fails.

## Security and reliability

- Never ship AI API keys in the APK.
- Use HTTPS for deployed backend endpoints.
- Add authentication, rate limiting, abuse controls, and privacy disclosures before public production use.
- Only play media that the project has permission to stream.
- Do not treat model output as trusted URLs or executable instructions.

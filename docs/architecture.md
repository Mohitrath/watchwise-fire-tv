# WatchWise TV architecture

The Kotlin Fire OS app renders the catalogue and stores favorites/preferences locally. Media3 handles video playback. The optional Node.js/Express backend exposes health, catalogue and recommendation endpoints. When `AI_API_KEY` is configured, the backend calls an OpenAI-compatible chat-completions endpoint; otherwise it provides deterministic catalogue-based recommendations.

Provider secrets stay on the backend. Recommendation responses are restricted to title IDs in the bundled catalogue. For demonstrations, use HTTPS for the deployed backend and licensed media URLs.

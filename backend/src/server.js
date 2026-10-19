import 'dotenv/config';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import { titles } from './catalogue.js';

const app = express();
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const publicDir = path.join(__dirname, '../public');
const port = Number(process.env.PORT || 3000);
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      "default-src": ["'self'"],
      "script-src": ["'self'", "'unsafe-inline'"],
      "style-src": ["'self'", "'unsafe-inline'"],
      "img-src": ["'self'", "data:", "https:"],
      "media-src": ["'self'", "https:", "blob:"],
      "frame-src": ["https://www.youtube-nocookie.com", "https://www.youtube.com"],
      "connect-src": ["'self'", "https:"],
      "font-src": ["'self'", "data:", "https:"],
      "object-src": ["'none'"],
      "base-uri": ["'self'"],
      "form-action": ["'self'"],
      "frame-ancestors": ["'none'"]
    }
  }
}));
app.use(cors());
app.use(express.json({ limit: '16kb' }));
app.use(express.static(publicDir, { index: false, maxAge: '1h' }));

app.get('/', (_req, res) => res.sendFile(path.join(publicDir, 'index.html')));
app.get('/api-info', (_req, res) => res.json({
  service: 'WatchWise TV API', status: 'ok',
  endpoints: { health: '/health', titles: '/api/titles', titleDetails: '/api/titles/:id', genres: '/api/genres', recommendations: 'POST /api/recommendations' }
}));
app.get('/health', (_req, res) => res.json({ status: 'ok', service: 'watchwise-tv-backend', aiConfigured: Boolean(process.env.AI_API_KEY) }));
app.get('/api/titles', (req, res) => {
  const genre = typeof req.query.genre === 'string' ? req.query.genre : '';
  const q = typeof req.query.q === 'string' ? req.query.q.toLowerCase() : '';
  res.json({ titles: titles.filter(t => (!genre || t.genre.toLowerCase() === genre.toLowerCase()) && (!q || `${t.name} ${t.description} ${t.genre}`.toLowerCase().includes(q))) });
});
app.get('/api/titles/:id', (req, res) => {
  const title = titles.find(t => t.id === req.params.id);
  if (!title) return res.status(404).json({ error: 'Title not found' });
  return res.json({ title });
});
app.get('/api/genres', (_req, res) => res.json({ genres: [...new Set(titles.map(t => t.genre))] }));

app.get('/api/trending-series', async (_req, res) => {
  const apiKey = process.env.TMDB_API_KEY;
  if (!apiKey) return res.status(503).json({
    error: 'TMDB_API_KEY is not configured',
    message: 'Add a TMDB API key in your deployment environment to enable live trending series.'
  });
  try {
    const response = await fetch(`https://api.themoviedb.org/3/trending/tv/day?api_key=${encodeURIComponent(apiKey)}&language=en-US`, {
      signal: AbortSignal.timeout(8000)
    });
    if (!response.ok) throw new Error(`TMDB returned HTTP ${response.status}`);
    const payload = await response.json();
    const results = Array.isArray(payload.results) ? payload.results : [];
    res.set('Cache-Control', 'public, s-maxage=900, stale-while-revalidate=3600');
    return res.json({
      source: 'TMDB',
      updatedAt: new Date().toISOString(),
      results: results.slice(0, 20).map((show, index) => ({
        id: show.id,
        rank: index + 1,
        name: show.name || show.original_name || 'Untitled series',
        overview: show.overview || 'No synopsis is available yet.',
        firstAirDate: show.first_air_date || '',
        rating: Number(show.vote_average || 0),
        voteCount: Number(show.vote_count || 0),
        popularity: Number(show.popularity || 0),
        poster: show.poster_path ? `https://image.tmdb.org/t/p/w500${show.poster_path}` : null,
        backdrop: show.backdrop_path ? `https://image.tmdb.org/t/p/w780${show.backdrop_path}` : null,
        analysis: Number(show.vote_average || 0) >= 7.5 ? 'Strong audience score' : Number(show.vote_average || 0) >= 6 ? 'Mixed-to-positive audience score' : 'Audience score is still developing',
        trailerUrl: null      }))
    });
  } catch (error) {
    console.error('Trending series fetch failed:', error.message);
    return res.status(502).json({ error: 'Trending series are temporarily unavailable.' });
  }
});

app.get('/api/trending-series/:id/trailer', async (req, res) => {
  const apiKey = process.env.TMDB_API_KEY;
  const id = String(req.params.id || '');
  if (!apiKey) return res.status(503).json({ error: 'TMDB_API_KEY is not configured' });
  if (!/^\\d{1,12}$/.test(id)) return res.status(400).json({ error: 'Invalid series ID' });
  try {
    const response = await fetch(`https://api.themoviedb.org/3/tv/${encodeURIComponent(id)}/videos?api_key=${encodeURIComponent(apiKey)}&language=en-US`, { signal: AbortSignal.timeout(8000) });
    if (!response.ok) throw new Error(`TMDB returned HTTP ${response.status}`);
    const payload = await response.json();
    const videos = Array.isArray(payload.results) ? payload.results : [];
    const video = videos.find(v => v.site === 'YouTube' && v.type === 'Trailer' && v.official && v.key)
      || videos.find(v => v.site === 'YouTube' && v.type === 'Trailer' && v.key)
      || videos.find(v => v.site === 'YouTube' && v.key);
    if (!video) return res.status(404).json({ error: 'No YouTube trailer is listed for this series.' });
    res.set('Cache-Control', 'public, s-maxage=3600, stale-while-revalidate=86400');
    return res.json({ id: video.key, name: video.name || 'Trailer', site: 'YouTube', url: `https://www.youtube.com/watch?v=${video.key}` });
  } catch (error) {
    console.error('Trailer lookup failed:', error.message);
    return res.status(502).json({ error: 'Trailer lookup is temporarily unavailable.' });
  }
});



function localRecommendations({ genres = [], maxDurationMinutes = 15 }) {
  const requested = Array.isArray(genres) ? genres.filter(x => typeof x === 'string').slice(0, 8) : [];
  const max = Number.isFinite(Number(maxDurationMinutes)) ? Math.min(240, Math.max(1, Number(maxDurationMinutes))) : 15;
  const matched = titles.filter(t => t.duration <= max && (!requested.length || requested.some(g => g.toLowerCase() === t.genre.toLowerCase()) || (requested.some(g => g.toLowerCase() === 'comedy') && t.id === 'demo-001')));
  const fallback = titles.filter(t => t.duration <= max);
  return (matched.length ? matched : fallback).slice(0, 3).map(t => ({ titleId: t.id, reason: `Matches your ${t.genre.toLowerCase()} interest and fits within ${max} minutes.` }));
}

app.post('/api/recommendations', async (req, res) => {
  const body = req.body && typeof req.body === 'object' ? req.body : {};
  const genres = Array.isArray(body.genres) ? body.genres.filter(x => typeof x === 'string').slice(0, 8) : [];
  const maxDurationMinutes = Number(body.maxDurationMinutes ?? 15);
  const language = typeof body.language === 'string' ? body.language.slice(0, 40) : 'English';
  const local = localRecommendations({ genres, maxDurationMinutes });
  if (!process.env.AI_API_KEY) return res.json({ source: 'local-fallback', recommendations: local });
  try {
    const response = await fetch(`${(process.env.AI_BASE_URL || 'https://api.openai.com/v1').replace(/\/$/, '')}/chat/completions`, {
      method: 'POST', headers: { Authorization: `Bearer ${process.env.AI_API_KEY}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({ model: process.env.AI_MODEL || 'gpt-4o-mini', temperature: 0.2, response_format: { type: 'json_object' }, messages: [
        { role: 'system', content: 'Recommend only titles in the supplied catalogue. Return JSON: {"recommendations":[{"titleId":"existing id","reason":"short reason"}]}. Never invent IDs. Maximum 3 recommendations.' },
        { role: 'user', content: JSON.stringify({ preferences: { genres, maxDurationMinutes, language }, catalogue: titles }) }
      ] }), signal: AbortSignal.timeout(8000)
    });
    if (!response.ok) throw new Error(`AI provider HTTP ${response.status}`);
    const data = await response.json();
    const content = data.choices?.[0]?.message?.content;
    const parsed = JSON.parse(content || '{}');
    const allowed = new Set(titles.map(t => t.id));
    const recommendations = (Array.isArray(parsed.recommendations) ? parsed.recommendations : []).filter(r => r && allowed.has(r.titleId)).slice(0, 3).map(r => ({ titleId: r.titleId, reason: String(r.reason || 'Recommended for your preferences.').slice(0, 220) }));
    return res.json({ source: 'ai', recommendations: recommendations.length ? recommendations : local });
  } catch (error) {
    console.error('AI recommendation fallback:', error.message);
    return res.json({ source: 'local-fallback', warning: 'AI service unavailable; local recommendations returned.', recommendations: local });
  }
});

app.use((err, _req, res, _next) => { console.error(err); res.status(500).json({ error: 'Internal server error' }); });
if (!process.env.VERCEL) app.listen(port, '0.0.0.0', () => console.log(`WatchWise API listening on ${port}`));

export { app };
export default app;

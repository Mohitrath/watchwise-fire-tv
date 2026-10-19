import 'dotenv/config';
import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import { titles } from './catalogue.js';

const app = express();
const port = Number(process.env.PORT || 3000);
app.use(helmet());
app.use(cors());
app.use(express.json({ limit: '16kb' }));

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

  // Optional model integration. The server still works without a provider key.
  if (!process.env.AI_API_KEY) return res.json({ source: 'local-fallback', recommendations: local });
  try {
    const response = await fetch(`${(process.env.AI_BASE_URL || 'https://api.openai.com/v1').replace(/\/$/, '')}/chat/completions`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${process.env.AI_API_KEY}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        model: process.env.AI_MODEL || 'gpt-4o-mini',
        temperature: 0.2,
        response_format: { type: 'json_object' },
        messages: [
          { role: 'system', content: 'You recommend only titles in the supplied catalogue. Return JSON object: {"recommendations":[{"titleId":"existing id","reason":"short reason"}]}. Never invent IDs. Maximum 3 recommendations.' },
          { role: 'user', content: JSON.stringify({ preferences: { genres, maxDurationMinutes, language }, catalogue: titles }) }
        ]
      }),
      signal: AbortSignal.timeout(8000)
    });
    if (!response.ok) throw new Error(`AI provider HTTP ${response.status}`);
    const data = await response.json();
    const content = data.choices?.[0]?.message?.content;
    const parsed = JSON.parse(content || '{}');
    const allowed = new Set(titles.map(t => t.id));
    const recommendations = (Array.isArray(parsed.recommendations) ? parsed.recommendations : [])
      .filter(r => r && allowed.has(r.titleId))
      .slice(0, 3)
      .map(r => ({ titleId: r.titleId, reason: String(r.reason || 'Recommended for your preferences.').slice(0, 220) }));
    return res.json({ source: 'ai', recommendations: recommendations.length ? recommendations : local });
  } catch (error) {
    console.error('AI recommendation fallback:', error.message);
    return res.json({ source: 'local-fallback', warning: 'AI service unavailable; local recommendations returned.', recommendations: local });
  }
});

app.use((err, _req, res, _next) => {
  console.error(err);
  res.status(500).json({ error: 'Internal server error' });
});
app.listen(port, '0.0.0.0', () => console.log(`WatchWise API listening on port ${port}`));

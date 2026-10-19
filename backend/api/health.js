import app from '../src/server.js';

export default function health(req, res) {
  req.url = '/health';
  return app(req, res);
}

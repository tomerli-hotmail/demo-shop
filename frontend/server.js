import express from 'express';
import cookieParser from 'cookie-parser';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __dirname = dirname(fileURLToPath(import.meta.url));

function withProtocol(url) {
  return url.startsWith('http') ? url : `https://${url}`;
}

const PRODUCT_URL  = withProtocol(process.env.PRODUCT_CATALOG_URL ?? 'http://localhost:3001');
const CART_URL     = withProtocol(process.env.CART_URL             ?? 'http://localhost:3002');
const CHECKOUT_URL = withProtocol(process.env.CHECKOUT_URL         ?? 'http://localhost:8080');
const PORT         = process.env.PORT                 ?? 3000;
const isProd       = process.env.NODE_ENV === 'production';

const app = express();
app.use(express.json());
app.use(cookieParser());

async function proxyJson(r) {
  const ct = r.headers.get('content-type') ?? '';
  if (ct.includes('application/json')) return r.json();
  const text = await r.text();
  return { error: text || `HTTP ${r.status}` };
}

// ── Product catalog ──────────────────────────────────────────────────────────

app.get('/api/products', async (req, res) => {
  const q = req.query.q;
  const url = q
    ? `${PRODUCT_URL}/products/search?q=${encodeURIComponent(q)}`
    : `${PRODUCT_URL}/products`;
  try {
    const r = await fetch(url);
    res.status(r.status).json(await proxyJson(r));
  } catch (err) {
    res.status(502).json({ error: String(err) });
  }
});

app.get('/api/products/:id', async (req, res) => {
  try {
    const r = await fetch(`${PRODUCT_URL}/products/${req.params.id}`);
    res.status(r.status).json(await proxyJson(r));
  } catch (err) {
    res.status(502).json({ error: String(err) });
  }
});

// ── Cart ──────────────────────────────────────────────────────────────────────

app.get('/api/cart', async (req, res) => {
  const sessionId = req.cookies.session_id;
  if (!sessionId) return res.json({ items: [] });
  try {
    const r = await fetch(`${CART_URL}/cart/${sessionId}`);
    res.status(r.status).json(await proxyJson(r));
  } catch (err) {
    res.status(502).json({ error: String(err) });
  }
});

app.post('/api/cart/items', async (req, res) => {
  const sessionId = req.cookies.session_id;
  if (!sessionId) return res.status(400).json({ error: 'No session' });
  try {
    const r = await fetch(`${CART_URL}/cart/${sessionId}/items`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req.body),
    });
    res.status(r.status).json(await proxyJson(r));
  } catch (err) {
    res.status(502).json({ error: String(err) });
  }
});

app.patch('/api/cart/items/:productId', async (req, res) => {
  const sessionId = req.cookies.session_id;
  if (!sessionId) return res.status(400).json({ error: 'No session' });
  try {
    const r = await fetch(`${CART_URL}/cart/${sessionId}/items/${req.params.productId}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req.body),
    });
    res.status(r.status).json(await proxyJson(r));
  } catch (err) {
    res.status(502).json({ error: String(err) });
  }
});

app.delete('/api/cart/items/:productId', async (req, res) => {
  const sessionId = req.cookies.session_id;
  if (!sessionId) return res.status(400).json({ error: 'No session' });
  try {
    await fetch(`${CART_URL}/cart/${sessionId}/items/${req.params.productId}`, {
      method: 'DELETE',
    });
    res.status(204).end();
  } catch (err) {
    res.status(502).json({ error: String(err) });
  }
});

// ── Auth ──────────────────────────────────────────────────────────────────────

app.post('/api/auth/login', async (req, res) => {
  try {
    const r = await fetch(`${CHECKOUT_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req.body),
    });
    const data = await proxyJson(r);
    if (!r.ok) return res.status(r.status).json(data);

    const maxAge = 60 * 60 * 24 * 7; // 7 days in seconds
    res.cookie('auth_token', data.token,    { path: '/', maxAge, sameSite: 'lax' });
    res.cookie('auth_user',  data.username, { path: '/', maxAge, sameSite: 'lax' });
    res.cookie('auth_email', data.email,    { path: '/', maxAge, sameSite: 'lax' });
    res.json(data);
  } catch (err) {
    res.status(502).json({ error: `Auth service unavailable: ${err}` });
  }
});

app.post('/api/auth/logout', async (req, res) => {
  const token = req.cookies.auth_token;
  if (token) {
    try {
      await fetch(`${CHECKOUT_URL}/auth/logout?token=${token}`, { method: 'POST' });
    } catch {
      // best-effort — still clear cookies
    }
  }
  res.cookie('auth_token', '', { path: '/', maxAge: 0 });
  res.cookie('auth_user',  '', { path: '/', maxAge: 0 });
  res.cookie('auth_email', '', { path: '/', maxAge: 0 });
  res.json({ status: 'logged out' });
});

app.get('/api/auth/me', async (req, res) => {
  const token = req.cookies.auth_token;
  if (!token) return res.status(401).json({ error: 'Not logged in' });
  try {
    const r = await fetch(`${CHECKOUT_URL}/auth/me?token=${token}`);
    res.status(r.status).json(await proxyJson(r));
  } catch (err) {
    res.status(502).json({ error: String(err) });
  }
});

// ── Orders ────────────────────────────────────────────────────────────────────

app.get('/api/orders', async (req, res) => {
  const email = req.cookies.auth_email;
  if (!email) return res.status(401).json({ error: 'Not logged in' });
  try {
    const r = await fetch(`${CHECKOUT_URL}/orders?email=${encodeURIComponent(email)}`);
    res.status(r.status).json(await proxyJson(r));
  } catch (err) {
    res.status(502).json({ error: String(err) });
  }
});

// ── Checkout & coupons ────────────────────────────────────────────────────────

app.post('/api/checkout', async (req, res) => {
  const sessionId = req.cookies.session_id;
  if (!sessionId) return res.status(400).json({ error: 'No session' });
  try {
    const r = await fetch(`${CHECKOUT_URL}/checkout`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...req.body, sessionId }),
    });
    res.status(r.status).json(await proxyJson(r));
  } catch (err) {
    res.status(502).json({ error: `Checkout service unavailable: ${err}` });
  }
});

app.post('/api/coupons/validate', async (req, res) => {
  try {
    const r = await fetch(`${CHECKOUT_URL}/coupons/validate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req.body),
    });
    res.status(r.status).json(await proxyJson(r));
  } catch (err) {
    res.status(502).json({ error: String(err) });
  }
});

// ── Static files (production only) ───────────────────────────────────────────

if (isProd) {
  app.use(express.static(join(__dirname, 'dist')));
  // SPA catch-all: let React Router handle client-side routes
  app.get('*', (_req, res) => {
    res.sendFile(join(__dirname, 'dist', 'index.html'));
  });
}

app.listen(PORT, () => {
  console.log(`API server listening on :${PORT}${isProd ? ' (serving static dist/)' : ''}`);
});

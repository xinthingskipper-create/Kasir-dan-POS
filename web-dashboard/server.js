const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = Number(process.env.PORT) || 3000;
const PUBLIC_DIR = path.join(__dirname);
const STORE_FILE = path.join(__dirname, '.sync-store.json');

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
};

const DEFAULT_COLLECTIONS = {
  expenses: {},
  transactions: {},
  shift_reports: {},
  daily_reports: {},
  cash_flows: {},
  realtime_stats: {},
  store_profile: {},
  customer_tracking: {},
  purchase_tracking: {},
  reports: {
    daily_transactions: {},
    daily_expenses: {},
  },
};

function loadStore(storePath = STORE_FILE) {
  try {
    if (fs.existsSync(storePath)) {
      const raw = fs.readFileSync(storePath, 'utf8');
      const parsed = JSON.parse(raw);
      if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
        return { ...structuredCloneSafe(DEFAULT_COLLECTIONS), ...parsed };
      }
    }
  } catch (err) {
    console.warn('Warning: failed to load sync store, starting fresh:', err.message);
  }
  return structuredCloneSafe(DEFAULT_COLLECTIONS);
}

function structuredCloneSafe(obj) {
  return JSON.parse(JSON.stringify(obj));
}

function saveStore(store, storePath = STORE_FILE) {
  try {
    const tmpFile = `${storePath}.tmp`;
    fs.writeFileSync(tmpFile, JSON.stringify(store, null, 2), 'utf8');
    fs.renameSync(tmpFile, storePath);
  } catch (err) {
    console.warn('Warning: failed to persist sync store:', err.message);
  }
}

function parseSegments(urlPath) {
  const clean = decodeURIComponent(urlPath.replace(/^\/api\/sync\/?/, '').replace(/\.json$/i, ''));
  return clean
    .split('/')
    .map(s => s.trim())
    .filter(s => s && s !== '.' && s !== '..');
}

function getAtPath(root, segments) {
  let curr = root;
  for (const seg of segments) {
    if (curr == null || typeof curr !== 'object' || !(seg in curr)) {
      return undefined;
    }
    curr = curr[seg];
  }
  return curr;
}

function setAtPath(root, segments, value) {
  if (segments.length === 0) return false;
  let curr = root;
  for (let i = 0; i < segments.length - 1; i++) {
    const seg = segments[i];
    if (curr[seg] == null || typeof curr[seg] !== 'object' || Array.isArray(curr[seg])) {
      curr[seg] = {};
    }
    curr = curr[seg];
  }
  const last = segments[segments.length - 1];
  if (value === null) {
    delete curr[last];
  } else {
    curr[last] = value;
  }
  return true;
}

function patchAtPath(root, segments, value) {
  if (segments.length === 0 || !value || typeof value !== 'object' || Array.isArray(value)) {
    return setAtPath(root, segments, value);
  }
  let curr = root;
  for (let i = 0; i < segments.length; i++) {
    const seg = segments[i];
    if (curr[seg] == null || typeof curr[seg] !== 'object' || Array.isArray(curr[seg])) {
      curr[seg] = {};
    }
    curr = curr[seg];
  }
  Object.assign(curr, value);
  return true;
}

function deleteAtPath(root, segments) {
  if (segments.length === 0) return false;
  let curr = root;
  for (let i = 0; i < segments.length - 1; i++) {
    const seg = segments[i];
    if (curr[seg] == null || typeof curr[seg] !== 'object') {
      return false;
    }
    curr = curr[seg];
  }
  const last = segments[segments.length - 1];
  const existed = last in curr;
  delete curr[last];
  return existed;
}

function readJsonBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    let totalBytes = 0;
    req.on('data', chunk => {
      totalBytes += chunk.length;
      if (totalBytes > 5 * 1024 * 1024) {
        reject(new Error('Payload too large'));
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on('end', () => {
      const raw = Buffer.concat(chunks).toString('utf8').trim();
      if (!raw) {
        resolve({});
        return;
      }
      try {
        resolve(JSON.parse(raw));
      } catch (e) {
        reject(new Error('Invalid JSON payload'));
      }
    });
    req.on('error', reject);
  });
}

function sendJson(res, statusCode, data) {
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    'Cache-Control': 'no-store',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, PUT, PATCH, DELETE, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
  });
  res.end(JSON.stringify(data));
}

function createServer(options = {}) {
  const publicDir = options.publicDir || PUBLIC_DIR;
  const storePath = options.storePath || STORE_FILE;
  const syncStore = loadStore(storePath);

  return http.createServer(async (req, res) => {
    const rawUrl = req.url || '/';
    const qIndex = rawUrl.indexOf('?');
    const urlPath = qIndex >= 0 ? rawUrl.slice(0, qIndex) : rawUrl;

    if (urlPath.startsWith('/api/')) {
      if (req.method === 'OPTIONS') {
        res.writeHead(204, {
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Methods': 'GET, POST, PUT, PATCH, DELETE, OPTIONS',
          'Access-Control-Allow-Headers': 'Content-Type, Authorization',
        });
        res.end();
        return;
      }

      if (urlPath === '/api/health') {
        sendJson(res, 200, { ok: true, status: 'ready' });
        return;
      }

      if (urlPath === '/api/sync' || urlPath.startsWith('/api/sync/')) {
        const segments = parseSegments(urlPath);
        try {
          if (req.method === 'GET') {
            if (segments.length === 0) {
              sendJson(res, 200, syncStore);
              return;
            }
            const val = getAtPath(syncStore, segments);
            sendJson(res, 200, val === undefined ? null : val);
            return;
          }

          if (req.method === 'DELETE') {
            if (segments.length === 0) {
              sendJson(res, 400, { ok: false, error: 'Missing path' });
              return;
            }
            deleteAtPath(syncStore, segments);
            saveStore(syncStore, storePath);
            sendJson(res, 200, { ok: true });
            return;
          }

          if (req.method === 'PUT' || req.method === 'PATCH' || req.method === 'POST') {
            const payload = await readJsonBody(req);
            if (segments.length === 0) {
              sendJson(res, 400, { ok: false, error: 'Missing collection path' });
              return;
            }
            if (req.method === 'PUT') {
              setAtPath(syncStore, segments, payload);
            } else if (req.method === 'PATCH') {
              patchAtPath(syncStore, segments, payload);
            } else {
              const itemId = (payload && typeof payload === 'object' && (payload.id || payload.invoiceNumber))
                ? String(payload.id || payload.invoiceNumber)
                : `ITEM-${Date.now()}`;
              setAtPath(syncStore, [...segments, itemId], payload);
            }
            saveStore(syncStore, storePath);
            sendJson(res, 200, { ok: true, path: segments.join('/') });
            return;
          }

          sendJson(res, 405, { ok: false, error: 'Method not allowed' });
        } catch (err) {
          sendJson(res, 400, { ok: false, error: err.message || 'Bad request' });
        }
        return;
      }

      sendJson(res, 404, { ok: false, error: 'API endpoint not found' });
      return;
    }

    // Serve static files safely (ignoring URL query parameters)
    const relativePath = urlPath === '/' ? 'index.html' : urlPath.replace(/^\/+/, '');
    const safePath = path.normalize(relativePath).replace(/^(\.\.[/\\])+/, '');
    const filePath = path.join(publicDir, safePath);

    if (!filePath.startsWith(publicDir)) {
      res.writeHead(403, { 'Content-Type': 'text/plain' });
      res.end('403 Forbidden');
      return;
    }

    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'text/html; charset=utf-8';

    fs.readFile(filePath, (err, content) => {
      if (err) {
        if (err.code === 'ENOENT') {
          fs.readFile(path.join(publicDir, 'index.html'), (e, indexContent) => {
            if (e) {
              res.writeHead(404, { 'Content-Type': 'text/plain' });
              res.end('404 Not Found');
            } else {
              res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
              res.end(indexContent, 'utf-8');
            }
          });
        } else {
          res.writeHead(500);
          res.end(`Server Error: ${err.code}`);
        }
      } else {
        res.writeHead(200, { 'Content-Type': contentType });
        res.end(content, 'utf-8');
      }
    });
  });
}

if (require.main === module) {
  const server = createServer();
  server.listen(PORT, '0.0.0.0', () => {
    console.log(`Owner Web Report Dashboard running at http://0.0.0.0:${PORT}`);
  });
}

module.exports = { createServer };

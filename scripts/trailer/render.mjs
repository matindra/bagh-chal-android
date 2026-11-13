// Renders trailer.html frame by frame through headless Chrome (DevTools protocol, no npm packages).
// Usage: node render.mjs <out-dir> [fps] [only-seconds,comma,separated]
// PAGE (default trailer.html), WIDTH and HEIGHT (default 1920x1080) pick the page and its size.
import { spawn } from 'node:child_process';
import { mkdirSync, writeFileSync, mkdtempSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const CHROME = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const here = dirname(fileURLToPath(import.meta.url));
const outDir = resolve(process.argv[2] ?? 'frames');
const fps = Number(process.argv[3] ?? 30);
const only = process.argv[4]?.split(',').map(Number);
mkdirSync(outDir, { recursive: true });
const PAGE = process.env.PAGE ?? 'trailer.html';
const W = Number(process.env.WIDTH ?? 1920), H = Number(process.env.HEIGHT ?? 1080);

const port = 9300 + Math.floor(Math.random() * 500);
const chrome = spawn(CHROME, [
  '--headless=new', '--disable-gpu', '--hide-scrollbars', '--allow-file-access-from-files', '--force-device-scale-factor=1',
  `--window-size=${W},${H}`, `--remote-debugging-port=${port}`, `--user-data-dir=${mkdtempSync(join(tmpdir(), 'trailer-chrome-'))}`,
  'about:blank',
], { stdio: 'ignore' });

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
let target;
for (let i = 0; i < 100 && !target; i++) {
  try { target = (await (await fetch(`http://127.0.0.1:${port}/json/list`)).json()).find((t) => t.type === 'page'); } catch { await sleep(100); }
}
const ws = new WebSocket(target.webSocketDebuggerUrl);
await new Promise((r) => ws.addEventListener('open', r));
let nextId = 1;
const pending = new Map();
ws.addEventListener('message', (e) => { const m = JSON.parse(e.data); if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); } });
const send = (method, params = {}) => new Promise((r) => { const id = nextId++; pending.set(id, r); ws.send(JSON.stringify({ id, method, params })); });
const evaluate = async (expression) => {
  const m = await send('Runtime.evaluate', { expression, awaitPromise: true, returnByValue: true });
  if (m.result?.exceptionDetails) throw new Error(JSON.stringify(m.result.exceptionDetails));
  return m.result?.result?.value;
};

await send('Emulation.setDeviceMetricsOverride', { width: W, height: H, deviceScaleFactor: 1, mobile: false });
await send('Page.enable');
await send('Page.navigate', { url: 'file://' + join(here, PAGE) });
for (let i = 0; i < 200; i++) { if (await evaluate('!!window.ready')) break; await sleep(100); }
await evaluate('window.ready');

const duration = await evaluate('DURATION');
const times = only ?? Array.from({ length: Math.round(duration * fps) }, (_, i) => i / fps);
for (let i = 0; i < times.length; i++) {
  await evaluate(`render(${times[i]}); new Promise(r => requestAnimationFrame(() => requestAnimationFrame(r)))`);
  const shot = await send('Page.captureScreenshot', { format: 'png', clip: { x: 0, y: 0, width: W, height: H, scale: 1 } });
  const name = only ? `t${times[i].toFixed(2)}.png` : `f${String(i).padStart(5, '0')}.png`;
  writeFileSync(join(outDir, name), Buffer.from(shot.result.data, 'base64'));
  if (i % 60 === 0) process.stdout.write(`frame ${i}/${times.length}\n`);
}
ws.close();
chrome.kill();

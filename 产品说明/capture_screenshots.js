const { spawn } = require('node:child_process');
const fs = require('node:fs/promises');
const path = require('node:path');

const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const root = 'F:\\tool share\\产品说明';
const outputDir = path.join(root, 'screenshots');
const userDataDir = path.join(root, '.chrome-profile');
const port = 9333;
const baseUrl = 'http://localhost:3000';
const backendUrl = 'http://localhost:8080';

const pages = [
  { name: '01-login', title: '登录页面', url: '/login', auth: false },
  { name: '02-tools', title: '工具目录', url: '/tools', auth: true },
  { name: '03-submit', title: '提交工具', url: '/tools/submit', auth: true },
  { name: '04-workflows', title: '工作流', url: '/workflows', auth: true },
  { name: '05-reviews', title: '审核中心', url: '/admin/reviews', auth: true },
  { name: '06-users', title: '账号权限', url: '/admin/users', auth: true },
  { name: '07-import-export', title: '导入导出', url: '/admin/import-export', auth: true },
  { name: '08-logs', title: '日志审计', url: '/admin/logs', auth: true }
];

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function waitForChrome() {
  const deadline = Date.now() + 15000;
  while (Date.now() < deadline) {
    try {
      const res = await fetch(`http://127.0.0.1:${port}/json/version`);
      if (res.ok) return await res.json();
    } catch {}
    await sleep(250);
  }
  throw new Error('Chrome DevTools endpoint did not become available.');
}

class CdpClient {
  constructor(url) {
    this.url = url;
    this.nextId = 1;
    this.pending = new Map();
  }

  async connect() {
    this.ws = new WebSocket(this.url);
    await new Promise((resolve, reject) => {
      this.ws.addEventListener('open', resolve, { once: true });
      this.ws.addEventListener('error', reject, { once: true });
    });
    this.ws.addEventListener('message', (event) => {
      const message = JSON.parse(event.data);
      if (!message.id) return;
      const pending = this.pending.get(message.id);
      if (!pending) return;
      this.pending.delete(message.id);
      if (message.error) {
        pending.reject(new Error(message.error.message));
      } else {
        pending.resolve(message.result);
      }
    });
  }

  send(method, params = {}, sessionId) {
    const id = this.nextId++;
    const payload = { id, method, params };
    if (sessionId) payload.sessionId = sessionId;
    this.ws.send(JSON.stringify(payload));
    return new Promise((resolve, reject) => {
      this.pending.set(id, { resolve, reject });
    });
  }

  close() {
    this.ws.close();
  }
}

async function loginSession() {
  const response = await fetch(`${backendUrl}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'Admin@123456' })
  });
  const payload = await response.json();
  if (!response.ok || payload.status !== 'ok') {
    throw new Error(`Login failed: ${JSON.stringify(payload)}`);
  }
  return payload.data;
}

async function capturePage(client, sessionId, page) {
  await client.send('Page.navigate', { url: `${baseUrl}${page.url}` }, sessionId);
  await sleep(2500);
  if (page.auth) {
    const title = await client.send('Runtime.evaluate', {
      expression: 'document.title',
      returnByValue: true
    }, sessionId);
    if (title.result?.value?.includes('登录')) {
      throw new Error(`Auth state was not applied for ${page.url}`);
    }
  }
  await client.send('Runtime.evaluate', {
    expression: 'window.scrollTo(0, 0)',
    returnByValue: true
  }, sessionId);
  await sleep(500);
  const screenshot = await client.send('Page.captureScreenshot', {
    format: 'png',
    fromSurface: true,
    captureBeyondViewport: true
  }, sessionId);
  const file = path.join(outputDir, `${page.name}.png`);
  await fs.writeFile(file, Buffer.from(screenshot.data, 'base64'));
  return { ...page, file };
}

async function main() {
  await fs.mkdir(outputDir, { recursive: true });
  await fs.rm(userDataDir, { recursive: true, force: true });
  await fs.mkdir(userDataDir, { recursive: true });

  const chrome = spawn(chromePath, [
    `--remote-debugging-port=${port}`,
    `--user-data-dir=${userDataDir}`,
    '--headless=new',
    '--disable-gpu',
    '--no-first-run',
    '--no-default-browser-check',
    '--window-size=1440,1100',
    `${baseUrl}/login`
  ], { stdio: 'ignore' });

  try {
    const version = await waitForChrome();
    const client = new CdpClient(version.webSocketDebuggerUrl);
    await client.connect();

    const { targetId } = await client.send('Target.createTarget', { url: `${baseUrl}/login` });
    const { sessionId } = await client.send('Target.attachToTarget', { targetId, flatten: true });
    await client.send('Page.enable', {}, sessionId);
    await client.send('Runtime.enable', {}, sessionId);
    await client.send('Emulation.setDeviceMetricsOverride', {
      width: 1440,
      height: 1100,
      deviceScaleFactor: 1,
      mobile: false
    }, sessionId);

    const session = await loginSession();
    await client.send('Page.navigate', { url: `${baseUrl}/login` }, sessionId);
    await sleep(1500);
    await client.send('Runtime.evaluate', {
      expression: `sessionStorage.setItem('space-auth-session', ${JSON.stringify(JSON.stringify(session))})`,
      returnByValue: true
    }, sessionId);

    const manifest = [];
    for (const page of pages) {
      manifest.push(await capturePage(client, sessionId, page));
    }
    await fs.writeFile(path.join(outputDir, 'manifest.json'), JSON.stringify(manifest, null, 2), 'utf8');
    client.close();
  } finally {
    chrome.kill();
  }
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});

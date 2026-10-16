// node tools/test-control-panel.cjs; uses fresh headless browser + local fixtures only.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const http = require('node:http');
const path = require('node:path');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');

(async () => {
  let fixture = 'active', reply = { status: 200, body: 'ok' }, pending, hold = false;
  const requests = [], scriptErrors = [];
  const root = '/c/fixture-session/';
  const server = http.createServer(async (req, res) => {
    if (req.method === 'POST') {
      let body = '';
      for await (const chunk of req) body += chunk;
      requests.push({ url: req.url, form: new URLSearchParams(body), headers: req.headers });
      if (hold) await new Promise(resolve => { pending = resolve; });
      res.writeHead(reply.status, { 'Content-Type': 'text/plain; charset=utf-8' });
      return res.end(reply.body);
    }
    if (req.url === root + 'app.js') {
      res.writeHead(200, { 'Content-Type': 'application/javascript' });
      return res.end(fs.readFileSync(path.join('build/control-probe', 'app.js')));
    }
    if (req.url === root) {
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
      return res.end(fs.readFileSync(path.join('build/control-probe', fixture + '.html')));
    }
    res.writeHead(404); res.end('not found');
  });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  let browser;
  try {
    browser = await chromium.launch({ headless: true, ...(process.env.BROWSER_EXE ? { executablePath: process.env.BROWSER_EXE } : {}) });
    const page = await browser.newPage({ viewport: { width: 1180, height: 920 }, locale: 'pt-BR' });
    page.on('pageerror', error => scriptErrors.push(error.message));
    page.on('dialog', dialog => dialog.accept());
    const url = `http://127.0.0.1:${server.address().port}${root}`;
    async function open(tab, kind = 'active') {
      fixture = kind;
      await page.goto(url);
      await page.locator(`[data-category=${tab}]`).click();
      await page.locator(`[data-panel=${tab}].active`).waitFor({ state: 'visible' });
    }
    async function act(selector, action, endpoint, reload = true) {
      const before = requests.length;
      const response = page.waitForResponse(r => r.request().method() === 'POST');
      const loaded = reload ? page.waitForEvent('framenavigated', { predicate: frame => frame === page.mainFrame() }) : null;
      await page.locator('.category-panel.active').locator(selector).click();
      await response;
      if (loaded) { await loaded; await page.waitForLoadState(); }
      else await page.waitForFunction(() => !document.querySelector('[data-saving="1"]'));
      assert.equal(requests.length, before + 1, 'one request per action');
      const request = requests.at(-1);
      assert.equal(request.url, root + endpoint, 'URL must not be a named form control');
      assert.equal(request.form.get('action'), action);
      assert.equal(request.form.getAll('action').length, 1);
      assert.ok(request.form.get('csrf'));
      assert.equal(request.headers['x-sonyvegas-request'], '1');
      return request;
    }
    await open('category-0');
    const hit = page.locator('.card').filter({ hasText: 'HitSelect' });
    await hit.locator('.toggle').click({ button: 'right' });
    const burst = hit.locator('select[name=value]').filter({ has: page.locator('option', { hasText: /^Burst$/ }) });
    assert.equal(await burst.count(), 1);
    const modeResponse = page.waitForResponse(r => r.request().method() === 'POST');
    await burst.selectOption({ label: 'Normal' });
    await modeResponse;
    await page.waitForFunction(() => !document.querySelector('[data-saving="1"]'));
    assert.equal(requests.at(-1).form.get('value'), 'Normal');
    await open('category-0');
    if (!(await hit.getAttribute('class')).includes('configuring')) await hit.locator('.toggle').click({ button: 'right' });
    const items = page.locator('.item-grid');
    await items.locator('..').locator('summary').click();
    assert.equal(await items.locator('[data-item=swords]').isChecked(), true);
    const itemsResponse = page.waitForResponse(r => r.request().method() === 'POST');
    await items.locator('[data-item=axes]').check();
    await itemsResponse;
    await page.waitForFunction(() => !document.querySelector('[data-saving="1"]'));
    assert.equal(requests.at(-1).form.get('value'), 'swords\naxes');
    assert.equal(await page.locator('.value textarea[name=value]').count(), 0);
    await open('configs');
    assert.equal(await page.locator('.cloud-slot').count(), 4);
    assert.equal(await page.locator('.slot-status.stored').count(), 1);
    assert.equal(await page.locator('.cloud-slot input[name=name]').first().inputValue(), 'Minha config & <teste>');
    await page.screenshot({ path: 'build/control-probe/configs-desktop.png', fullPage: true, animations: 'disabled' });
    let request = await act('.cloud-slot:nth-child(2) button[value=save]', 'save', 'remote-config');
    assert.equal(request.form.get('slot'), '1');
    assert.equal(request.form.get('name'), 'Config 2');
    assert.equal(await page.locator('.tab.active').getAttribute('data-category'), 'configs');
    assert.match(await page.locator('#toast').innerText(), /Config salva/);
    await act('.cloud-slot:first-child button[value=save]', 'save', 'remote-config');
    // Loading/deleting a saved slot must not require the user to fill its name again.
    await page.locator('.cloud-slot input[name=name]').first().fill('');
    await act('button[value=load]', 'load', 'remote-config');
    await act('button[value=delete]', 'delete', 'remote-config');

    // Canceling destructive confirmation must send no request.
    page.removeAllListeners('dialog');
    page.once('dialog', dialog => dialog.dismiss());
    const beforeCancel = requests.length;
    await page.locator('.category-panel.active button[value=delete]').click();
    assert.equal(requests.length, beforeCancel);
    page.on('dialog', dialog => dialog.accept());

    // Pending/error: lock controls, prevent duplicate Enter, retain entered name, show safe server error.
    reply = { status: 401, body: 'Sessao remota indisponivel ou expirada.' }; hold = true;
    const target = page.locator('.cloud-slot').nth(1);
    await target.locator('input[name=name]').fill('Minha nova config');
    await target.locator('button[value=save]').click();
    await page.waitForFunction(() => !!document.querySelector('[aria-busy=true]'));
    assert.equal(await target.locator('button').isDisabled(), true);
    const beforeDuplicate = requests.length;
    await target.evaluate(form => form.dispatchEvent(new SubmitEvent('submit', { bubbles: true, cancelable: true })));
    assert.equal(requests.length, beforeDuplicate);
    while (!pending) await new Promise(resolve => setTimeout(resolve, 5));
    pending(); hold = false;
    await page.waitForFunction(() => !document.querySelector('[data-saving="1"]'));
    assert.equal(await target.locator('input[name=name]').inputValue(), 'Minha nova config');
    assert.match(await target.locator('.form-status').innerText(), /Sessao remota/);
    assert.equal(await target.locator('button').isEnabled(), true);
    reply = { status: 200, body: 'ok' };

    // Enter/older-browser submit without event.submitter still supplies the save action.
    const entered = page.waitForRequest(r => r.method() === 'POST');
    await target.evaluate(form => form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true })));
    assert.equal(new URLSearchParams((await entered).postData()).get('action'), 'save');
    await page.waitForEvent('framenavigated'); await page.waitForLoadState();

    await open('party', 'empty');
    await act('button[value=create]', 'create', 'party');
    await page.locator('input[name=code]').fill('abc123');
    await act('button[value=join]', 'join', 'party');
    await open('party');
    await page.locator('input[name="share:Reach"]').check();
    request = await act('button[value=sync]', 'sync', 'party', false);
    assert.equal(request.form.get('share:Reach'), 'on');
    assert.equal(request.form.get('mode'), 'config');
    await page.locator('select[name=mode]').selectOption('activate');
    request = await act('button[value=sync]', 'sync', 'party', false);
    assert.equal(request.form.get('mode'), 'activate');
    await page.screenshot({ path: 'build/control-probe/party-desktop.png', fullPage: true, animations: 'disabled' });
    await act('button[value=leave]', 'leave', 'party');

    await open('friends');
    request = await act('form:has(input[value=middle]) button', 'middle', 'friends');
    assert.equal(request.form.get('enabled'), 'true');
    await page.locator('[data-panel=friends] input[name=name]:not([type=hidden])').fill('Alex');
    request = await act('form:has(input[value=add]) button', 'add', 'friends');
    assert.equal(request.form.get('name'), 'Alex');
    request = await act('form:has(input[value=remove]) button', 'remove', 'friends');
    assert.equal(request.form.get('name'), 'Steve');
    await page.screenshot({ path: 'build/control-probe/friends-desktop.png', fullPage: true, animations: 'disabled' });
    await open('category-0');
    await page.locator('.card[data-module-search=reach]').click({ button: 'right' });
    const bindResponse = page.waitForResponse(r => r.request().method() === 'POST');
    await page.locator('.card[data-module-search=reach] select[name=key]').selectOption('65');
    await bindResponse;
    await page.waitForFunction(() => !document.querySelector('[data-saving="1"]'));
    assert.equal(requests.at(-1).url, root + 'bind');
    assert.equal(requests.at(-1).form.get('key'), '65');
    await page.screenshot({ path: 'build/control-probe/bind-desktop.png', fullPage: true, animations: 'disabled' });
    await open('community');
    assert.equal(await page.locator('.community-item').count(), 1);
    assert.match(await page.locator('.community-item').innerText(), /Jogador <1>/);
    await page.screenshot({ path: 'build/control-probe/community-desktop.png', fullPage: true, animations: 'disabled' });
    await page.locator('.community-publish input[name=name]').fill('Minha publica');
    await page.locator('.community-publish input[name=description]').fill('Perfil compartilhado');
    request = await act('.community-publish button[value=publish]', 'publish', 'community');
    assert.equal(request.form.get('name'), 'Minha publica');
    assert.equal(request.form.get('description'), 'Perfil compartilhado');
    await open('community');
    await act('.community-item button[value=load]', 'load', 'community');
    await open('community');
    await act('.community-item button[value=delete]', 'delete', 'community');
    await open('community', 'empty');
    assert.match(await page.locator('.community-empty').innerText(), /Ainda nao existem/);

    await open('configs', 'offline');
    assert.equal(await page.locator('.cloud-slot button:enabled').count(), 0);
    assert.equal(await page.locator('.slot-status').first().innerText(), 'INDISPONIVEL');
    assert.match(await page.locator('.notice').innerText(), /Falha ao consultar/);
    await open('configs');
    await page.setViewportSize({ width: 390, height: 844 });
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true, 'mobile must not overflow');
    await page.screenshot({ path: 'build/control-probe/configs-mobile.png', fullPage: true, animations: 'disabled' });
    await open('community');
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true, 'community mobile must not overflow');
    await page.screenshot({ path: 'build/control-probe/community-mobile.png', fullPage: true, animations: 'disabled' });
    for (const tab of ['friends', 'party']) {
      await open(tab);
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true, tab + ' mobile overflow');
      await page.screenshot({ path: 'build/control-probe/' + tab + '-mobile.png', fullPage: true, animations: 'disabled' });
    }
    assert.deepEqual(scriptErrors, []);
    console.log('PASS: config and Community publish/load/delete, party create/join/sync/leave, CSRF, Enter, pending/duplicate, errors, escaping, offline and mobile layout.');
  } finally {
    if (pending) pending();
    if (browser) await browser.close();
    server.closeAllConnections();
    await new Promise(resolve => server.close(resolve));
  }
})().catch(error => { console.error(error); process.exitCode = 1; });

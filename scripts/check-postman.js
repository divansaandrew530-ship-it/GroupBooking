// Smoke-tests the Postman collection against a running backend (file uploads are skipped).
// Run: node scripts/check-postman.js http://localhost:8080
const collection = require('../postman/GroupBookingLA.postman_collection.json');
const vars = Object.fromEntries(collection.variable.map((v) => [v.key, v.value]));
vars.baseUrl = process.argv[2];
const jar = {};
const needsUpload = ['Download item document', 'Download booking document', 'Download registration document', 'Company logo image', 'Item image', 'Reject company (instead of approving)'];
const fill = (text) => text.replace(/\{\{(\w+)\}\}/g, (_, key) => vars[key] ?? '');

(async () => {
  let failed = 0;
  for (const folder of collection.item) {
    for (const item of folder.item) {
      const r = item.request;
      if (r.body && r.body.mode === 'formdata') { console.log(`SKIP  ${folder.name} / ${item.name} (file upload)`); continue; }
      if (item.name === 'Logout' || item.name.startsWith('Register company with')) {
        console.log(`SKIP  ${folder.name} / ${item.name} (changes session)`); continue;
      }
      const url = fill(r.url.raw);
      const headers = { cookie: Object.entries(jar).map(([k, v]) => k + '=' + v).join('; ') };
      let body;
      if (r.body) { headers['content-type'] = 'application/json'; body = fill(r.body.raw); }
      const res = await fetch(url, { method: r.method, headers, body, redirect: 'manual' });
      for (const c of res.headers.getSetCookie()) { const [pair] = c.split(';'); const i = pair.indexOf('='); jar[pair.slice(0, i)] = pair.slice(i + 1); }
      const text = await res.text();
      const ok = res.status >= 200 && res.status < 300;
      const expected = !ok && needsUpload.includes(item.name);
      if (!ok && !expected) failed++;
      console.log(`${ok ? 'OK  ' : expected ? 'EXPT' : 'FAIL'}  ${res.status}  ${folder.name} / ${item.name}${ok ? '' : '  ' + text.slice(0, 160)}`);
      const pm = {
        response: { json: () => JSON.parse(text), to: { be: { success: true } } },
        collectionVariables: { set: (k, v) => { vars[k] = v; } },
        test: () => {},
      };
      if (ok) { try { new Function('pm', item.event[0].script.exec.join('\n'))(pm); } catch (e) { console.log('      script error: ' + e.message); } }
    }
  }
  console.log(failed ? `${failed} request(s) failed` : 'all requests succeeded');
})();

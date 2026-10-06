// Adds sample companies, users, quotes and bookings through the REST API (so passwords, ids and
// history are created exactly like real data). Run against a backend using the database to fill:
//   node scripts/seed-test-data.js http://localhost:8080
// All seeded users have the password Test@1234. Safe to re-run: existing data is skipped.
const base = process.argv[2] || 'http://localhost:8080';
const PASSWORD = 'Test@1234';

// UAE park ids from src/main/resources/data/experiences.json (a quote may only mix one country).
const DUBAI_FRAME = 'dubai-frame';
const AYA_UNIVERSE = 'aya-universe-dubai';
const IMG_WORLDS = 'img-worlds-tickets-only';
const GREEN_PLANET = 'green-planet';
const FERRARI_WORLD_3 = 'ferrari-world-3-park';

/** A signed-in client: keeps its own session cookie. */
function client(label) {
  const jar = {};
  return async function call(method, path, body) {
    const res = await fetch(base + path, {
      method,
      headers: {
        'content-type': 'application/json',
        cookie: Object.entries(jar).map(([k, v]) => `${k}=${v}`).join('; '),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    for (const c of res.headers.getSetCookie()) {
      const [pair] = c.split(';');
      const i = pair.indexOf('=');
      jar[pair.slice(0, i)] = pair.slice(i + 1);
    }
    const text = await res.text();
    if (!res.ok) throw new Error(`${label}: ${method} ${path} -> ${res.status} ${text}`);
    return text ? JSON.parse(text) : null;
  };
}

async function login(label, email, password = PASSWORD) {
  const call = client(label);
  await call('POST', '/api/auth/login', { email, password });
  return call;
}

/** Registers a company, verifies the OTP and returns { companyId, call } signed in as its admin. */
async function registerCompany(companyName, userName, email, phone) {
  const call = client(companyName);
  const registered = await call('POST', '/api/auth/register', { companyName, userName, email, phone, password: PASSWORD });
  await call('POST', '/api/auth/verify-otp', { email, otp: registered.otp });
  return { companyId: String(registered.companyId), call };
}

const line = (itemId, visitDate, adults, children = 0) => ({ itemId, visitDate, adults, children });
const trip = (lead, email, phone, items, extra = {}) => ({
  adults: Math.max(...items.map((i) => i.adults)),
  children: Math.max(...items.map((i) => i.children)),
  markupPercent: 0, country: 'UAE', currency: 'AED',
  leadName: lead, email, phone, items, ...extra,
});
const card = { paymentMethod: 'CARD', cardName: 'Test Card', cardNumber: '4242424242424242', cardExpiry: '12/29', cardCvc: '123' };

(async () => {
  const admin = await login('super admin', 'Admin@rathin.com', 'R@t@2023');
  const existing = await admin('GET', '/api/admin/companies');
  const alreadyRegistered = existing.some((c) => c.companyName === 'Desert Travels LLC');

  // ---- companies and users (skipped when they already exist) --------------------------------
  if (!alreadyRegistered) {
    const desert = await registerCompany('Desert Travels LLC', 'Aisha Khan', 'aisha@desert.test', '+971500001001');
    await admin('POST', `/api/admin/companies/${desert.companyId}/approve`, { note: 'Test data', creditLimit: '10000' });
    const oasis = await registerCompany('Oasis Tours', 'Rahul Mehta', 'rahul@oasis.test', '+971500002001');
    await admin('POST', `/api/admin/companies/${oasis.companyId}/approve`, { note: 'Test data', creditLimit: '5000' });
    await registerCompany('Harbor Holidays', 'Nora Ali', 'nora@harbor.test', '+971500003001');
    const bluesky = await registerCompany('Blue Sky Travel', 'Tom Baker', 'tom@bluesky.test', '+971500004001');
    await admin('POST', `/api/admin/companies/${bluesky.companyId}/reject`, { note: 'Test data: missing trade licence' });
    console.log('companies: Desert Travels LLC (active), Oasis Tours (active), Harbor Holidays (pending), Blue Sky Travel (rejected)');

    // ---- users --------------------------------------------------------------------------------
    const aishaAdmin = await login('aisha', 'aisha@desert.test');
    await aishaAdmin('POST', '/api/company/users', { userName: 'Omar Farooq', email: 'omar@desert.test', password: PASSWORD, role: 'COMPANY_USER' });
    const rahulAdmin = await login('rahul', 'rahul@oasis.test');
    await rahulAdmin('POST', '/api/company/users', { userName: 'Priya Nair', email: 'priya@oasis.test', password: PASSWORD, role: 'COMPANY_USER' });
    await admin('POST', '/api/company/users', { userName: 'Sara Parent Agent', email: 'sara@weegootour.test', password: PASSWORD, role: 'COMPANY_USER' });
    console.log('users: omar@desert.test, priya@oasis.test, sara@weegootour.test (+ the 4 company admins)');
  }
  const aisha = await login('aisha', 'aisha@desert.test');
  const rahul = await login('rahul', 'rahul@oasis.test');
  const omar = await login('omar', 'omar@desert.test');
  if ((await aisha('GET', '/api/company/quotes')).length > 0) {
    console.log('Quotes and bookings are already there. Nothing more to do.');
    return;
  }

  // ---- quotes -------------------------------------------------------------------------------
  const q1 = await aisha('POST', '/api/parks/quote',
    trip('Ahmed Family', 'ahmed.family@example.com', '+971501110001', [line(DUBAI_FRAME, '2026-11-10', 4, 2), line(AYA_UNIVERSE, '2026-11-11', 4, 2)]));
  await aisha('POST', `/api/company/quotes/${q1.quoteNumber}/follow-up`, { followUp: 'Customer will confirm by Friday' });

  const q2 = await omar('POST', '/api/parks/quote',
    trip('School Trip Group', 'teacher@school.example', '+971501110002', [line(IMG_WORLDS, '2026-12-05', 20, 15)]));
  const accepted = await omar('POST', `/api/company/quotes/${q2.quoteNumber}/status`, { status: 'ACCEPTED' });

  const q3 = await rahul('POST', '/api/parks/quote',
    trip('Kumar Group', 'kumar@example.com', '+971501110003', [line(FERRARI_WORLD_3, '2026-10-20', 6)], { currency: 'USD' }));

  const q4 = await admin('POST', '/api/parks/quote',
    trip('Walk-in Customer', 'walkin@example.com', '+971501110004', [line(GREEN_PLANET, '2026-10-15', 2, 1)], { directCustomer: true }));
  console.log(`quotes: ${q1.quoteNumber} (pending), ${q2.quoteNumber} (accepted -> booking ${accepted.bookingRefNum}), ${q3.quoteNumber} (USD), ${q4.quoteNumber} (direct customer)`);

  // ---- bookings -----------------------------------------------------------------------------
  const b1 = await aisha('POST', '/api/parks/confirm',
    { ...trip('Fatima Hassan', 'fatima@example.com', '+971501110005', [line(DUBAI_FRAME, '2026-10-12', 2, 1)]), ...card });
  const b2 = await rahul('POST', '/api/parks/confirm',
    { ...trip('Singh Family', 'singh@example.com', '+971501110006', [line(GREEN_PLANET, '2026-10-18', 3, 2)]), paymentMethod: 'CREDIT' });
  const b3 = await omar('POST', '/api/parks/confirm',
    { ...trip('Corporate Offsite', 'events@corp.example', '+971501110007', [line(AYA_UNIVERSE, '2026-11-02', 12), line(DUBAI_FRAME, '2026-11-03', 12)]), ...card });

  // Parent company works on the bookings: confirm items and add follow-ups.
  const first = await admin('GET', `/api/company/bookings/${b1.bookingRef}`);
  await admin('POST', `/api/company/bookings/${b1.bookingRef}/items/${first.details[0].id}/status`, { status: 'CONFIRMED' });
  const second = await admin('GET', `/api/company/bookings/${b2.bookingRef}`);
  await admin('POST', `/api/company/bookings/${b2.bookingRef}/items/${second.details[0].id}/follow-up`, { followUp: 'Waiting on supplier voucher' });
  console.log(`bookings: ${b1.bookingRef} (card, confirmed), ${b2.bookingRef} (company credit, follow-up), ${b3.bookingRef} (card, 2 items), ${accepted.bookingRefNum} (from accepted quote)`);

  console.log(`\nDone. Log in with any seeded email and password ${PASSWORD}.`);
})().catch((error) => {
  console.error(error.message);
  process.exit(1);
});

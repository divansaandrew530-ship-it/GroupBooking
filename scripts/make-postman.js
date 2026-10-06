// Regenerates postman/*.json from the endpoint list below. Run: node scripts/make-postman.js
const fs = require('fs');
const path = require('path');

const json = (value) => ({ mode: 'raw', raw: JSON.stringify(value, null, 2), options: { raw: { language: 'json' } } });
const form = (fields) => ({
  mode: 'formdata',
  formdata: fields.map(([key, value, type]) =>
    type === 'file' ? { key, type: 'file', src: [], description: value } : { key, value, type: 'text' }),
});
const save = (lines) => [{ listen: 'test', script: { type: 'text/javascript', exec: lines } }];
const expectOk = 'pm.test("status is 2xx", () => pm.response.to.be.success);';

function req(name, method, url, { body, desc, tests = [] } = {}) {
  const [pathPart, query = ''] = url.split('?');
  const item = {
    name,
    request: {
      method,
      header: [],
      url: {
        raw: '{{baseUrl}}' + url,
        host: ['{{baseUrl}}'],
        path: pathPart.split('/').filter(Boolean),
        ...(query ? { query: query.split('&').map((pair) => { const [key, value] = pair.split('='); return { key, value }; }) } : {}),
      },
      ...(desc ? { description: desc } : {}),
    },
    event: save([expectOk, ...tests]),
  };
  if (body) item.request.body = body;
  return item;
}
const folder = (name, description, item) => ({ name, description, item });

const itinerary = {
  adults: 1, children: 0, markupPercent: 0, country: 'UAE', currency: 'AED',
  leadName: 'Test Guest', email: 'guest@example.com', phone: '+971500000000',
  items: [{ itemId: 'dubai-frame', visitDate: '2026-12-10', adults: 1, children: 0 }],
};

const collection = {
  info: {
    name: 'GroupBookingLA API',
    description: [
      'REST API of the GroupBookingLA backend.',
      '',
      'How to use:',
      '1. Start the backend (profile `dev` or `h2`) and pick the "GroupBookingLA - Local" environment.',
      '2. Run "Auth / Login as Super Admin" first. Postman keeps the session cookie, so later requests are signed in.',
      '3. Requests save ids for the next ones (quoteNumber, bookingRef, itemId, companyId, ...), so run a folder top to bottom.',
      '',
      'Errors come back as JSON: {"error": "message"} with 400 (bad input), 401 (not signed in / not allowed), 404 (not found) or 409 (conflict).',
    ].join('\n'),
    schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
  },
  variable: [
    ['baseUrl', 'http://localhost:8080'], ['adminEmail', 'Admin@rathin.com'], ['adminPassword', 'R@t@2023'],
    ['quoteNumber', ''], ['bookingRef', ''], ['bookingItemId', ''], ['itemId', ''], ['companyId', ''],
    ['userId', ''], ['slideId', ''], ['otp', ''], ['registerEmail', 'new.agency@example.com'],
  ].map(([key, value]) => ({ key, value })),
  item: [
    folder('Auth', 'Sign in and sign out. Run "Login as Super Admin" first.', [
      req('Login as Super Admin', 'POST', '/api/auth/login', {
        body: json({ email: '{{adminEmail}}', password: '{{adminPassword}}', remember: 'true' }),
        desc: 'Signs in and stores the session cookie. `remember: "true"` keeps the session for 30 days.',
      }),
      req('Who am I', 'GET', '/api/auth/me'),
      req('Logout', 'POST', '/api/auth/logout'),
    ]),

    folder('Registration (new company)', 'Run top to bottom: register, verify the OTP, then approve as Super Admin.', [
      req('Register company (JSON)', 'POST', '/api/auth/register', {
        body: json({ companyName: 'New Agency', userName: 'New Admin', email: '{{registerEmail}}', phone: '+971500000001', password: 'NewAgency1' }),
        desc: 'Quick registration. Returns the OTP in test/dev (RUZINN_OTP_ECHO=true); in production it is only emailed.',
        tests: ['const data = pm.response.json();', 'pm.collectionVariables.set("otp", data.otp);', 'pm.collectionVariables.set("companyId", data.companyId);'],
      }),
      req('Verify OTP', 'POST', '/api/auth/verify-otp', {
        body: json({ email: '{{registerEmail}}', otp: '{{otp}}' }),
        desc: 'Signs the new user in. The company stays pending until a Super Admin approves it. Note: this replaces the current session - log in as Super Admin again afterwards.',
      }),
      req('Login as Super Admin again', 'POST', '/api/auth/login', {
        body: json({ email: '{{adminEmail}}', password: '{{adminPassword}}', remember: 'true' }),
        desc: 'Verify OTP signed in the new user; switch back to the Super Admin before approving.',
      }),
      req('Approve the new company', 'POST', '/api/admin/companies/{{companyId}}/approve', { body: json({ note: 'Documents checked', creditLimit: '5000' }), desc: 'The company becomes ACTIVE and its users can book.' }),
      req('Register company with documents (multipart)', 'POST', '/api/auth/register', {
        body: form([
          ['companyName', 'Coastal Travels'], ['userName', 'Lina Owner'], ['email', 'lina@coastal.example'],
          ['phone', '+971500000333'], ['password', 'CoastalPass1'], ['registrationNo', 'CN-77881'],
          ['address', 'Sheikh Zayed Road'], ['website', 'https://coastal.example'], ['country', 'United Arab Emirates'],
          ['city', 'Dubai'], ['zipCode', '00000'], ['ownerTelephone', '+97143000001'],
          ['operationsStaffName', 'Sam Ops'], ['operationsStaffNumber', '+971500000334'],
          ['financeName', 'Finance Desk'], ['financeMobile', '+971500000335'], ['financeTelephone', '+97143000002'],
          ['financeEmail', 'accounts@coastal.example'],
          ['vatCertificate', 'VAT certificate (PDF)', 'file'], ['licence', 'Trade licence (PDF)', 'file'], ['logo', 'Company logo (JPG/PNG)', 'file'],
        ]),
        desc: 'Full registration: all profile fields are required. Choose the three files in Postman before sending.',
        tests: ['const data = pm.response.json();', 'pm.collectionVariables.set("otp", data.otp);', 'pm.collectionVariables.set("companyId", data.companyId);'],
      }),
    ]),

    folder('Parks - Catalog and Quotes', 'Everything needed to build and price an itinerary.', [
      req('Catalog', 'GET', '/api/parks/catalog', { desc: 'Countries, parks, hotels and prices. Use the item ids in quotes.' }),
      req('Exchange rates (ROE)', 'GET', '/api/parks/roe', { desc: 'AED is the base currency.' }),
      req('Booking party', 'GET', '/api/parks/booking-party', { desc: 'Companies and users a parent company can book for.' }),
      req('Time slots', 'GET', '/api/parks/timeslots?itemId=burj-khalifa-at-the-top-level-124-non-prime&date=2026-12-10'),
      req('Create / update quote', 'POST', '/api/parks/quote', {
        body: json(itinerary),
        desc: 'Prices the itinerary and saves it. Add "quoteNumber" to update an existing quote. "currency" can be AED, USD or INR.',
        tests: ['pm.collectionVariables.set("quoteNumber", pm.response.json().quoteNumber);'],
      }),
      req('Quote PDF', 'POST', '/api/parks/quote/pdf', { body: json({ ...itinerary, quoteNumber: '{{quoteNumber}}' }), desc: 'Returns the PDF. In Postman use "Send and Download".' }),
      req('Email quote', 'POST', '/api/parks/quote/send', { body: json({ ...itinerary, quoteNumber: '{{quoteNumber}}' }) }),
    ]),

    folder('Parks - Checkout', 'Creating bookings.', [
      req('Confirm booking (card)', 'POST', '/api/parks/confirm', {
        body: json({ ...itinerary, paymentMethod: 'CARD', cardName: 'Test Guest', cardNumber: '4242424242424242', cardExpiry: '09/28', cardCvc: '123' }),
        desc: 'Validates passengers and card, takes the (mock) payment and records the booking. Parks that need passenger details require items[].passengers.',
        tests: ['pm.collectionVariables.set("bookingRef", pm.response.json().bookingRef);'],
      }),
      req('Confirm booking (company credit)', 'POST', '/api/parks/confirm', {
        body: json({ ...itinerary, paymentMethod: 'CREDIT' }),
        desc: 'Charges the company credit (see "Company Bookings / Company credit").',
        tests: ['pm.collectionVariables.set("bookingRef", pm.response.json().bookingRef);'],
      }),
      req('Confirm booking with passenger details', 'POST', '/api/parks/confirm', {
        body: json({
          ...itinerary,
          items: [{ itemId: 'xline-dubai-marina', visitDate: '2026-12-10', adults: 1, children: 0,
            passengers: [{ type: 'ADULT', firstName: 'Sam', lastName: 'Rider', extra: { passport: 'P1234567', dateOfBirth: '1994-04-12', age: '32' } }] }],
          paymentMethod: 'CARD', cardName: 'Test Guest', cardNumber: '4242424242424242', cardExpiry: '09/28', cardCvc: '123',
        }),
        desc: 'Add-on parks list their required fields in quote.items[].passengerFields; send them in passengers[].extra.',
      }),
      req('Record booking without payment', 'POST', '/api/parks/book', { body: json(itinerary), tests: ['pm.collectionVariables.set("bookingRef", pm.response.json().bookingRefNum);'] }),
      req('Get booking by reference', 'GET', '/api/parks/bookings/{{bookingRef}}'),
    ]),

    folder('Company Quotes', 'Saved quotes of the signed-in company.', [
      req('List quotes', 'GET', '/api/company/quotes'),
      req('Get quote', 'GET', '/api/company/quotes/{{quoteNumber}}'),
      req('Update quote status', 'POST', '/api/company/quotes/{{quoteNumber}}/status', {
        body: json({ status: 'PENDING' }),
        desc: 'Set "ACCEPTED" to turn the quote into a booking (response contains bookingRefNum).',
      }),
      req('Update quote follow-up', 'POST', '/api/company/quotes/{{quoteNumber}}/follow-up', { body: json({ followUp: 'Call customer tomorrow' }) }),
    ]),

    folder('Company Bookings', 'Child companies can view; parent-company users can also change bookings.', [
      req('List bookings', 'GET', '/api/company/bookings', { desc: 'Parent companies may add ?companyId=... to filter.' }),
      req('Get booking', 'GET', '/api/company/bookings/{{bookingRef}}', {
        tests: ['const details = pm.response.json().details || [];', 'if (details.length) pm.collectionVariables.set("bookingItemId", details[0].id);'],
      }),
      req('Company credit', 'GET', '/api/company/credit'),
      req('Update booking status', 'POST', '/api/company/bookings/{{bookingRef}}/status', { body: json({ status: 'CONFIRMED' }) }),
      req('Update item status', 'POST', '/api/company/bookings/{{bookingRef}}/items/{{bookingItemId}}/status', { body: json({ status: 'CONFIRMED' }) }),
      req('Update item follow-up', 'POST', '/api/company/bookings/{{bookingRef}}/items/{{bookingItemId}}/follow-up', { body: json({ followUp: 'Waiting on supplier voucher' }) }),
      req('Upload item document', 'POST', '/api/company/bookings/{{bookingRef}}/items/{{bookingItemId}}/documents', {
        body: form([['kind', 'ticket'], ['file', 'PDF or image', 'file']]),
        desc: 'Upload the ticket first; voucher/visa after that.',
      }),
      req('Upload booking document', 'POST', '/api/company/bookings/{{bookingRef}}/documents', { body: form([['kind', 'voucher'], ['file', 'PDF or image', 'file']]) }),
      req('Download item document', 'GET', '/company/bookings/{{bookingRef}}/items/{{bookingItemId}}/documents/ticket', { desc: 'Use "preferred" as kind for the best available document.' }),
      req('Download booking document', 'GET', '/company/bookings/{{bookingRef}}/documents/preferred'),
    ]),

    folder('Users', 'Company admins manage their users; the Super Admin manages all users.', [
      req('List company users', 'GET', '/api/company/users'),
      req('Create company user', 'POST', '/api/company/users', {
        body: json({ userName: 'New Agent', email: 'new.agent@example.com', password: 'AgentPass1', role: 'COMPANY_USER' }),
        desc: 'role: COMPANY_USER or COMPANY_ADMIN. The Super Admin may add "companyId".',
        tests: ['pm.collectionVariables.set("userId", pm.response.json().userId);'],
      }),
      req('List all users (Super Admin)', 'GET', '/api/admin/users'),
      req('Block user', 'POST', '/api/admin/users/{{userId}}/block'),
      req('Unblock user', 'POST', '/api/admin/users/{{userId}}/unblock'),
      req('Reset user password', 'POST', '/api/admin/users/{{userId}}/password', { body: json({ password: 'NewPass123' }) }),
    ]),

    folder('Admin - Companies', 'Super Admin: review registrations.', [
      req('List companies', 'GET', '/api/admin/companies'),
      req('Company directory (with users)', 'GET', '/api/admin/companies/directory'),
      req('Reject company (instead of approving)', 'POST', '/api/admin/companies/{{companyId}}/reject', { body: json({ note: 'Missing licence' }) }),
      req('Upload company logo', 'POST', '/api/admin/companies/{{companyId}}/logo', { body: form([['logo', 'JPG or PNG', 'file']]) }),
      req('Download registration document', 'GET', '/admin/companies/{{companyId}}/documents/vat', { desc: 'kind: vat or licence.' }),
      req('Company logo image', 'GET', '/companies/{{companyId}}/logo'),
    ]),

    folder('Admin - Items', 'Super Admin: parks, hotels and other bookable items.', [
      req('List items', 'GET', '/api/admin/items?q=Dubai'),
      req('Create item', 'POST', '/api/admin/items', {
        body: json({
          itemName: 'Postman Test Park', country: 'UAE', datedType: 'DA', category: 'Park', adultRate: '120', childRate: '60',
          bookingInfo: 'DFT', apiTimeslot: false, apiBooking: false, extraFields: [],
          description: 'Created from Postman', termsAndCondition: '', included: '', excluded: '',
        }),
        desc: 'datedType: DA (any date) or DT (time slots). bookingInfo: DFT (lead contact) or ADD_ON (passenger fields from extraFields).',
        tests: ['pm.collectionVariables.set("itemId", pm.response.json().itemId);'],
      }),
      req('Get item', 'GET', '/api/admin/items/{{itemId}}'),
      req('Update item', 'PUT', '/api/admin/items/{{itemId}}', {
        body: json({ itemName: 'Postman Test Park', country: 'UAE', datedType: 'DA', category: 'Park', adultRate: '130', childRate: '65', bookingInfo: 'DFT', description: 'Updated from Postman' }),
      }),
      req('Upload item images', 'POST', '/api/admin/items/{{itemId}}/images', { body: form([['imagePdfFile', 'Image shown in the quote PDF', 'file'], ['imageTicketFile', 'Image shown on the ticket', 'file']]) }),
      req('Item image', 'GET', '/admin/items/{{itemId}}/image/pdf', { desc: 'kind: pdf or ticket.' }),
      req('Get item translation', 'GET', '/api/admin/items/{{itemId}}/translations/hi', { desc: 'lang: en, hi, ar, ru.' }),
      req('Save item translation', 'PUT', '/api/admin/items/{{itemId}}/translations/hi', { body: json({ itemName: 'पोस्टमैन टेस्ट पार्क', description: 'हिंदी विवरण' }) }),
      req('Download Excel template', 'GET', '/api/admin/items/template'),
      req('Bulk upload Excel', 'POST', '/api/admin/items/upload', { body: form([['file', 'Filled-in Excel template (.xlsx)', 'file']]) }),
      req('Delete item', 'DELETE', '/api/admin/items/{{itemId}}'),
    ]),

    folder('Admin - Exchange rates', '', [
      req('Save rates', 'POST', '/api/admin/roe', { body: json({ usdRate: 0.2723, inrRate: 22.7 }), desc: 'Rates from 1 AED.' }),
    ]),

    folder('Translations and Sign-in page', 'Public text for the Angular app, and the Super Admin editors.', [
      req('UI translations (public)', 'GET', '/api/i18n?lang=en', { desc: 'lang: en, hi, ar, ru.' }),
      req('Sign-in page content (public)', 'GET', '/api/login-home?lang=en'),
      req('Translation editor', 'GET', '/api/admin/cms?lang=hi'),
      req('Save translations', 'POST', '/api/admin/cms', { body: json({ lang: 'hi', values: { 'nav.quotes': 'कोट' } }), desc: 'Empty values fall back to English.' }),
      req('Sign-in page editor', 'GET', '/api/admin/login-home?lang=en', {
        tests: ['const slides = pm.response.json().slides || [];', 'if (slides.length) pm.collectionVariables.set("slideId", slides[0].id);'],
      }),
      req('Slide image (public)', 'GET', '/login-home/media/slides/{{slideId}}'),
      req('Save sign-in page', 'POST', '/api/admin/login-home', {
        body: json({ lang: 'en', productName: 'GROUPBOOKINGLA', productPill: 'B2B company group booking', panelTagline: 'Travel Together', signInTitle: 'Sign In', signInSubtitle: 'Please enter your credentials to sign in!', registerLabel: 'Register', footerCopy: '© 2026 Weegootour. All rights reserved.', termsHref: '#terms', privacyHref: '#privacy', supportHref: '#support', slides: [], features: [] }),
        desc: 'For lang "en" this saves the page itself (send all slides you want to keep - load them with the editor first). For other languages it saves translations only.',
      }),
      req('Add slide', 'POST', '/api/admin/login-home/slides'),
      req('Upload slide image', 'POST', '/api/admin/login-home/slides/{{slideId}}/image', { body: form([['file', 'JPG or PNG', 'file']]) }),
      req('Delete slide', 'POST', '/api/admin/login-home/slides/{{slideId}}/delete'),
    ]),

    folder('Audit history', 'Who changed what, and when.', [
      req('Booking history', 'GET', '/api/company/audit/bookings/{{bookingRef}}'),
      req('Booking item history', 'GET', '/api/company/audit/bookings/{{bookingRef}}/items/{{bookingItemId}}'),
      req('Quote history', 'GET', '/api/company/audit/quotes/{{quoteNumber}}'),
      req('User history', 'GET', '/api/company/audit/users/{{userId}}'),
      req('Company history', 'GET', '/api/company/audit/companies/{{companyId}}'),
      req('Company history (Super Admin)', 'GET', '/api/admin/audit/companies/{{companyId}}'),
      req('User history (Super Admin)', 'GET', '/api/admin/audit/users/{{userId}}'),
      req('Sign-in page history', 'GET', '/api/admin/audit/login-home'),
      req('Item history', 'GET', '/api/admin/audit/items/{{itemId}}'),
    ]),

  ],
};

const environment = {
  name: 'GroupBookingLA - Local',
  values: [
    { key: 'baseUrl', value: 'http://localhost:8080', enabled: true },
    { key: 'adminEmail', value: 'Admin@rathin.com', enabled: true },
    { key: 'adminPassword', value: 'R@t@2023', enabled: true, type: 'secret' },
  ],
  _postman_variable_scope: 'environment',
};

const out = path.join(__dirname, '..', 'postman');
fs.mkdirSync(out, { recursive: true });
fs.writeFileSync(path.join(out, 'GroupBookingLA.postman_collection.json'), JSON.stringify(collection, null, 2) + '\n');
fs.writeFileSync(path.join(out, 'GroupBookingLA-local.postman_environment.json'), JSON.stringify(environment, null, 2) + '\n');
const count = collection.item.reduce((sum, f) => sum + f.item.length, 0);
console.log(`wrote ${collection.item.length} folders, ${count} requests`);

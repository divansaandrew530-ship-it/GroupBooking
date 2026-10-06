# GroupBookingLA backend

Spring Boot 4 / Java 21 REST API for group bookings of parks, attractions and hotels.
The UI is the Angular app in `ruzinn-group-booking-ng`; this project serves JSON only.

## Run locally

| Profile | Database | Command |
|---|---|---|
| `h2`  | In-memory, reset on every start | `mvnw spring-boot:run -Dspring-boot.run.profiles=h2` (or `run-local.cmd`) |
| `dev` | Local MySQL `ruzinn` on localhost:3306 | `mvnw spring-boot:run -Dspring-boot.run.profiles=dev` |

- `dev` reads `src/main/resources/application-dev.yml`, which holds the local MySQL password
  and is gitignored. Create the tables with the scripts in `groupbookingla-db-scripts`.
- In IntelliJ, set **Active profiles** to `h2` or `dev`. Without a profile the app tries the
  shared MySQL server and needs `RUZINN_DB_PASSWORD`.
- API: http://localhost:8080 · Super Admin: `Admin@rathin.com` / `R@t@2023`
- Tests: `mvnw test`

## Project layout

```
com.ruzinn
├── controller/    REST endpoints only: read the request, call a service, return the result
├── service/       Service interfaces (what the business logic offers)
├── serviceimpl/   Service implementations (@Service, @Transactional)
├── repository/    Spring Data JPA repositories
├── entity/        JPA entities (database tables)
├── dto/           Request and response objects
├── config/        Spring configuration, auth interceptor, startup seed data
├── exception/     Exceptions and the JSON error handler
├── enums/         Enums
└── util/          Small helpers (SessionUser, FileResponses, ...)
```

Request flow: `controller → service (interface) → serviceimpl → repository → entity`.

When adding a feature: entity → repository → dto → service interface → serviceimpl →
controller (inject the interface, not the Impl).

## API

| Controller | Base path | What it does |
|---|---|---|
| AuthController | `/api/auth` | Login, logout, current user, company registration + OTP |
| ParkController | `/api/parks` | Catalog, rates, quotes (save/PDF/email), checkout |
| QuoteController | `/api/company/quotes` | Saved quotes, status, follow-up |
| BookingController | `/api/company/bookings` | Bookings, status, follow-up, documents, credit |
| UserController | `/api/company/users`, `/api/admin/users` | Users, block/unblock, password reset |
| CompanyController | `/api/admin/companies` | Approve/reject companies, documents, logos |
| ItemMasterController | `/api/admin/items` | Items, images, translations, Excel import |
| LoginHomeController | `/api/login-home`, `/api/admin/login-home` | Sign-in page content |
| CmsController, I18nController | `/api/admin/cms`, `/api/i18n` | UI translations |
| RoeController | `/api/admin/roe` | Exchange rates |
| AuditController | `/api/company/audit`, `/api/admin/audit` | Change history |

Sign-in uses a session cookie. Errors are JSON `{"error": "..."}` with status 400 (bad input),
401 (not signed in or not allowed), 404 (not found) or 409 (conflict).

### Postman

Import `postman/GroupBookingLA.postman_collection.json` and
`postman/GroupBookingLA-local.postman_environment.json`, select the environment, then run
**Auth / Login as Super Admin** first. Requests store ids (quote number, booking ref, ...) for the
next ones, so run a folder from top to bottom.

After changing endpoints: `node scripts/make-postman.js` regenerates the collection, and
`node scripts/check-postman.js http://localhost:8080` runs it against a running backend.

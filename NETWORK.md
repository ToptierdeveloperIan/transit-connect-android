# Network layer (backend Retrofit hub)

**Last updated:** 2026-07-19  
**Related session log:** [SESSION_CHANGELOG_2026-07-19.md](./SESSION_CHANGELOG_2026-07-19.md)

---

## Single hub for `/api/`

All backend HTTP must go through:

| API | Role |
|-----|------|
| `authRetrofitClient.getPlainClient()` | Login, OTP, register, **token refresh** — no Bearer, no authenticator |
| `authRetrofitClient.getClient(Context)` | Authenticated API traffic — Bearer + `TokenAuthenticator` |
| `RetrofitClient.getClient(Context)` | Facade → authenticated hub (existing call sites) |

**Base URL (one place):** `http://10.0.2.2:8000/api/` in `authRetrofitClient`.

---

## Process init

`ImaniApp` (`android:name=".ImaniApp"` in the manifest) on process start:

1. `authRetrofitClient.init(this)` — binds `TokenManager` to application context  
2. `authRetrofitClient.getClient(this)` — builds authenticated Retrofit once  

This removes “cold path” risk if something calls `getClient()` without a prior UI-driven init.

---

## Intentionally separate

| Client | Host | Why separate |
|--------|------|----------------|
| `GoogleMapsRetrofitClient` | `maps.googleapis.com` | Different origin; must not use JWT hub |

---

## Call-site rules

| Scenario | Use |
|----------|-----|
| OTP / token / register | `authRetrofitClient.getPlainClient()` |
| Booking, pay, profile, driver, etc. | `RetrofitClient.getClient(ctx)` or `authRetrofitClient.getClient(ctx)` |
| Never | `new Retrofit.Builder()` against the Django API |
| Never | Authenticated OkHttp for the refresh call (loop risk) |

---

## Terms of Service (related)

Legal traffic also uses this hub:

| Call | Client |
|------|--------|
| `GET support/terms/?locale=` | Plain |
| `GET support/terms/status/` | Authenticated |
| `POST support/terms/accept/` | Authenticated |

See `docs/TERMS_OF_SERVICE.md`.

## Profile settings

| Call | Client |
|------|--------|
| `GET/PATCH settings/profile*` | Authenticated hub |
| `POST settings/profile/phone/*` | Authenticated hub |

Offline names use `ProfileNameQueue` (not a second Retrofit). See `docs/PROFILE_SETTINGS_SYNC.md`.

---

## Files touched (network refactor)

| File | Change |
|------|--------|
| `auth/Repository/authRetrofitClient.java` | Production dual-client hub (thread-safe) |
| `auth/Repository/TokenAuthenticator.java` | Uses plain hub only; safer null handling |
| `auth/Repository/authInterceptor.java` | Attach Bearer only; removed dead LAN refresh |
| `auth/Repository/AuthRepository.java` | Plain hub for login/OTP |
| `auth/DataLayer/OkHttpclient.java` | Facade; no rogue OkHttp factory |
| `RetrofitClient.java` | Delegates to authenticated hub |
| `SignupFragment.java` | Plain hub; no local builder |
| `APIService.java` | `register/` relative to `/api/` |
| `ImaniApp.java` | Application-level init |
| `AndroidManifest.xml` | `android:name=".ImaniApp"` |

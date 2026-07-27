# Session changelog — 2026-07-19

Work spanning **Imani Android client** and **ridehaiing backend**.  
Treat both codebases as production targets, not demos.

---

## 1. Backend (`C:\Users\Ian\ridehaiingbackend`)

### New Django apps (scaffold + root wiring only)

| App | Package | Mount | Health |
|-----|---------|-------|--------|
| **Support** | `Support/` | `api/support/` | `GET /api/support/health/` |
| **UserSettings** | `UserSettings/` | `api/settings/` | `GET /api/settings/health/` |

### Root project wiring

| File | Change |
|------|--------|
| `ridehaiingbackend/settings.py` | `INSTALLED_APPS`: `'Support'`, `'UserSettings'` |
| `ridehaiingbackend/urls.py` | `path('api/support/', include('Support.urls'))`, `path('api/settings/', include('UserSettings.urls'))` |
| `Support/urls.py`, `Support/views.py` | Health endpoint (`AllowAny`) |
| `UserSettings/urls.py`, `UserSettings/views.py` | Health endpoint (`AllowAny`) |

### Not done (by design this session)

- No profile mutation APIs yet (name/email/phone)  
- No FAQ/tickets/legal content models yet  
- Old stubs in `Loginandauthentication.editprofileView` left as-is  

### Docs

- `Support/README.md` — app purpose + future endpoints map  
- `UserSettings/README.md` — app purpose + future endpoints map  
- `SESSION_CHANGELOG_2026-07-19.md` (this file’s twin under backend root)

---

## 2. Android client (`Imanicommunityapp`)

### Network: single Retrofit hub (production refactor)

**Problem:** Multiple ad-hoc `Retrofit.Builder()` instances (AuthRepository, TokenAuthenticator, SignupFragment) plus a partially correct `RetrofitClient` singleton; `authRetrofitClient` existed but was unused by login.

**Solution:** One hub in auth with two logical clients.

```text
authRetrofitClient
  ├─ getPlainClient()     → login / OTP / register / refresh
  └─ getClient(Context)   → all authenticated /api/ traffic
        ↑
  RetrofitClient          → facade for existing call sites
```

### Application cold-start init

| File | Change |
|------|--------|
| `ImaniApp.java` | **New** — `Application` that inits hub on process start |
| `AndroidManifest.xml` | `android:name=".ImaniApp"` |

### Files changed (IDE)

| File | Summary |
|------|---------|
| `auth/Repository/authRetrofitClient.java` | Rewritten: dual client, DCL, `init`, `getBaseUrl` |
| `auth/Repository/TokenAuthenticator.java` | Plain hub only; no NPE on failed refresh |
| `auth/Repository/authInterceptor.java` | Bearer only; removed unused 192.168 refresh path |
| `auth/Repository/AuthRepository.java` | Uses `getPlainClient()` |
| `auth/DataLayer/OkHttpclient.java` | Compatibility facade (no independent OkHttp build) |
| `RetrofitClient.java` | Thin delegate to authenticated hub |
| `SignupFragment.java` | Uses plain hub |
| `APIService.java` | `@POST("register/")` (base already ends with `/api/`) |
| `ImaniApp.java` | New Application class |
| `AndroidManifest.xml` | Registers `ImaniApp` |

### Intentionally unchanged

| Item | Reason |
|------|--------|
| `GoogleMapsRetrofitClient` | External host; must not share JWT OkHttp |
| Call sites already on `RetrofitClient.getClient` | Automatically on hub via facade |

### Docs

- [NETWORK.md](./NETWORK.md) — hub contract, rules, file map  
- This file — full session log  

---

## 3. Analysis / planning (no code, session context)

- Full-stack context load: client + backend domains (auth, booking, pay, promo, sync)  
- Client **Support / Settings** feature report mapped onto new backend apps  
- Product note: close payment → booking → promo loop remains the highest product priority (not implemented this session)  
- Codebase weight (~16k LOC logic) measured excluding `.venv`  

---

## 4. How to verify

### Backend

```bash
cd C:\Users\Ian\ridehaiingbackend
.\.venv\Scripts\python.exe manage.py check
# With server running:
# GET http://127.0.0.1:8000/api/support/health/
# GET http://127.0.0.1:8000/api/settings/health/
```

### Android

1. Clean/rebuild so `ImaniApp` is on the classpath  
2. Logcat filter `ImaniApp` → expect hub initialized on launch  
3. Login (OTP) still uses plain client; booking/pay still use `RetrofitClient`  

---

## 5. Terms of Service E2E (completed later same session)

### Backend
- `LegalDocument` / `LegalAcceptance` models + migration `0001_legal_documents`
- `LegalService`, serializers, views, admin
- Content EN + SW v1.0.0; `seed_terms` command
- Docs: `Support/TERMS_OF_SERVICE.md`

### Android
- Jetpack Compose Terms UI (`ImaniTheme`, EN/Kiswahili chips)
- `TermsApi` / `TermsRepository` / `TermsViewModel` / `TermsComposeFragment`
- Gate: `TermsGate` from Splash + post-login Verification
- Settings → Terms wired
- Docs: `docs/TERMS_OF_SERVICE.md`, `NETWORK.md` (hub unchanged)

## 6. UserSettings profile (name + phone + offline names) — implemented

### Backend (`UserSettings/`)
- `ProfileService` — name PATCH, version + mutation_id idempotency, server-wins 409  
- `PhoneChangeService` — OTP request/confirm, Redis challenge, uniqueness  
- Routes under `/api/settings/profile/*`  
- `datasync` PATCH rejects name/phone/email (`use_settings_api`)  
- Docs: `POLICY.md`, `PROFILE_SETTINGS.md`, `README.md`

### Android
- `settings/*` API + `SettingsRepository`  
- Room v6 pending/version fields  
- `ProfileNameQueue` + connectivity drain in `ImaniApp`  
- Wired `editingNameFragment`, `ChangePhoneFragment`, Settings nav  
- Docs: `docs/PROFILE_SETTINGS_SYNC.md`

## 7. Recommended next sessions

1. **Support:** Privacy Policy same pattern; Help FAQ  
2. Email change under UserSettings  
3. Close checkout → STK → booking → promo consume loop  

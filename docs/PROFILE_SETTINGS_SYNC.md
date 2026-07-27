# Profile settings + offline sync (Android)

**Backend:** `UserSettings/` — [POLICY.md](../../ridehaiingbackend/UserSettings/POLICY.md)  
**API:** `/api/settings/*`  
**Conflict policy:** Server wins on `profile_version` mismatch  
**Phone:** Online OTP only — never queued offline

---

## Architecture

```text
UI (Settings / editingName / ChangePhone)
        │
        ▼
SettingsRepository  ── online ──► authRetrofitClient (authenticated)
        │ offline names
        ▼
Room projection          ProfileNameQueue (SQLite events)
  pending flags                 │
        ▲                       │ connectivity / Settings open
        │                       ▼
        └──── applyServerProfile ◄── PATCH name / GET profile
```

**Single server writer for names + phone:** UserSettings only.  
`PATCH /api/sync/profile/` rejects identity fields (`use_settings_api`).

---

## Flows

### Name online
1. User saves names  
2. `PATCH settings/profile/name/` with `mutation_id` + `base_version`  
3. Room updated from response; pending cleared  

### Name offline
1. Room: new names + `pending_name_mutation=true`  
2. Queue event `PROFILE_NAME_UPDATE`  
3. On network: drain coalesces to **latest** event, PATCH same API  
4. Success → delete events, apply server snapshot  

### Name conflict (409)
- Re-GET profile / apply server names  
- UI message: server had a newer version  

### Phone (online only)
1. Block if offline  
2. `POST phone/request/` → OTP to **new** number; Room draft only  
3. `POST phone/confirm/` → Room `phone_no` = server only  
4. User stays logged in; next login uses new number  

---

## Room fields (v6)

| Column | Meaning |
|--------|---------|
| `profile_version` | Last server ResourceVersion |
| `pending_name_mutation` | Offline name not yet ACKed |
| `pending_mutation_id` | Idempotency key |
| `pending_base_version` | Version at offline edit time |
| `phone_pending_verification` | Draft during OTP (not account phone) |

### Rehydrate merge
| Condition | Action |
|-----------|--------|
| pending && server_version > pending_base | Drop pending; apply server (server wins) |
| pending && server_version == pending_base | Keep local pending names |
| no pending | Apply server names + phone |

---

## Key classes

| Class | Role |
|-------|------|
| `settings.SettingsApi` | Retrofit |
| `settings.SettingsRepository` | Online/offline orchestration |
| `settings.sync.ProfileNameQueue` | Offline name queue + drain |
| `UserProfileRepository` | Room projection + merge |
| `editingNameFragment` | Name UI |
| `ChangePhoneFragment` | OTP phone UI |
| `ImaniApp` | Drain queue on connectivity |

---

## Error codes (server)

`version_conflict`, `phone_taken`, `otp_invalid`, `challenge_expired`, `phone_unchanged`, `use_settings_api`, …

---

## Manual test checklist

1. Online rename → Settings header updates  
2. Airplane mode rename → “syncing” badge → go online → drains  
3. Phone offline → blocked message  
4. Phone request + wrong OTP → error  
5. Phone request + correct OTP → Room phone updates  
6. Two devices name conflict → 409 path / server wins  

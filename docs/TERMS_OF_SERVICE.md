# Terms of Service — Android client

**Status:** Production vertical slice (Compose UI + gate + EN/Kiswahili)  
**Backend contract:** `ridehaiingbackend/Support/TERMS_OF_SERVICE.md`  
**Session:** 2026-07-19 / completed wiring

---

## User flows

### A. Gate (must accept)

```text
Splash (token present)  ──┐
Login success             ──┼─► GET support/terms/status/
                            │     must_accept == true
                            ▼
                   TermsComposeFragment (requireAccept=true)
                     EN | Kiswahili toggle
                     scroll → Accept
                            ▼
                   POST support/terms/accept/
                            ▼
                   Home or Driver home
```

### B. Settings (read / optional re-accept)

```text
Settings → Terms & Conditions
  → TermsComposeFragment (requireAccept=false)
  → load GET support/terms/?locale=
```

---

## Architecture

| Layer | Location |
|-------|----------|
| Theme | `ui/theme/ImaniTheme.kt`, `ImaniColor.kt` (matches `blue_primary`) |
| UI | `ui/terms/TermsScreen.kt` (Compose Material3) |
| VM | `ui/terms/TermsViewModel.kt` |
| Host | `ui/terms/TermsComposeFragment.kt` (Nav Component bridge) |
| API | `support/terms/TermsApi.kt` |
| Models | `support/terms/TermsModels.kt` |
| Repo | `support/terms/TermsRepository.kt` |
| Java gate | `support/terms/TermsGate.java` |

Network:

- Document: `authRetrofitClient.getPlainClient()` (public)
- Status / accept: authenticated hub

---

## Locales

| Code | UI chip |
|------|---------|
| `en` | English |
| `sw` | Kiswahili |

Server falls back to English if a locale is missing.

---

## Nav

- Destination: `@+id/Terms_conditions` → `TermsComposeFragment`
- Arg: `requireAccept` (boolean, default false)
- Actions: splash → terms, verification → terms, settings → terms

---

## Gate policy

| Status result | Behaviour |
|---------------|-----------|
| `must_accept: true` | Open Terms gate |
| `must_accept: false` | Role home |
| Network error | Proceed to home (availability); document failure in logs |

---

## Build requirements

- Compose enabled in `app/build.gradle.kts`
- `fragment-ktx` for `viewModels()`
- Kotlin 1.9.24 + compose compiler 1.5.14

---

## Verify manually

1. Backend: `migrate Support` + `seed_terms`  
2. Cold login → Terms gate (if not accepted)  
3. Toggle Kiswahili → body switches  
4. Scroll + Accept → home  
5. Relaunch with token → skip Terms  
6. Settings → Terms → read-only accepted state  

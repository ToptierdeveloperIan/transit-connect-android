# Context-aware offline UI (Network module)

**Package:** `com.example.imanicommunityapp.Network`  
**Constraint:** Implementation lives **only** in this module — no edits to other packages.

---

## Problem

Users expect the app to **notice offline immediately** and change the UI
**depending on where they are** (checkout vs edit name vs wallet deposit),
not only a global “No internet” toast.

---

## Architecture

```text
ConnectivityChecker  (OS NetworkCallback — immediate)
        │
NetworkMonitor       (process singleton, full InternetStatus fan-out)
        │
        ├──────────────────────────────┐
        │                              │
OfflineScopeTracker              OfflinePolicy
 (stack: where is user?)     (scope × status → OfflineUiState)
        │                              │
        └──────────┬───────────────────┘
                   ▼
        ContextualOfflineController
                   │
                   ▼
        Feature UI (opt-in listeners — outside this package)
```

---

## Types

| Type | Role |
|------|------|
| `InternetStatus` | ONLINE / LIMITED / OFFLINE + transport + cost |
| `OfflineScope` | Product place (SETTINGS_PHONE, PAYMENT, …) |
| `OfflineMode` | ONLINE, DEGRADED, OFFLINE_SOFT, OFFLINE_HARD |
| `OfflineUiState` | title, message, primaryAllowed, showBanner |
| `OfflinePolicy` | Central matrix of scope behaviour |
| `OfflineScopeTracker` | Enter/leave scope stack |
| `NetworkMonitor` | App-wide connectivity hub |
| `ContextualOfflineController` | Combines network + scope for UI |

---

## Policy examples

| Scope | Offline |
|-------|---------|
| `SETTINGS_NAME` | SOFT — edit allowed, sync later |
| `SETTINGS_PHONE` | HARD — OTP needs network |
| `PAYMENT` / `WALLET_DEPOSIT` | HARD |
| `HOME_RIDER` | SOFT — banner only |
| `CHECKOUT` | HARD — live quote |

Edit rules only in `OfflinePolicy.java`.

---

## Opt-in wiring (other modules — not done here)

```java
// onResume
OfflineScopeTracker.getInstance().enter(OfflineScope.SETTINGS_PHONE);
ContextualOfflineController.getInstance(context).addListener(state -> {
    // update banner / buttons
});

// onPause
OfflineScopeTracker.getInstance().leave(OfflineScope.SETTINGS_PHONE);
```

This module compiles and runs without those calls; default scope is `GENERIC`.

---

## Files

- `ConnectivityChecker.java` — sensor + full status listeners  
- `NetworkMonitor.java`  
- `OfflineScope.java`, `OfflineMode.java`, `OfflineUiState.java`  
- `OfflinePolicy.java`  
- `OfflineScopeTracker.java`  
- `ContextualOfflineController.java`  
- `InternetStatusListener.java`  
- `package-info.java`  

---

## Testing tips

1. Enable airplane mode → expect HARD on payment scopes, SOFT on name.  
2. Enter/leave scopes in logcat: tags `OfflineScopeTracker`, `ContextualOffline`.  
3. LIMITED (captive portal) treated as hard for money/auth scopes.

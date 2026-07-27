# Android Wallet (Settings → Compose UI)

**Added:** 2026-07-20  
**Scope:** Wire wallet **UI only** under Settings. Does not change drawer payment dialog, booking STK flow, or other Settings rows.

---

## What was built

| Layer | Location | Role |
|-------|----------|------|
| Settings row | `fragment_settings.xml` `@+id/Wallet` | Clickable row under Profile |
| Settings click | `SettingsFragment.openWallet()` | Navigates to Compose wallet |
| Nav | `nav_graph.xml` `@id/walletFragment` + `action_settingsFragment_to_wallet` | Graph destination |
| Compose host | `ui/wallet/WalletComposeFragment.kt` | `ComposeView` + back |
| UI | `ui/wallet/WalletScreen.kt` | Balance hero, top-up sheet, ledger, intents |
| State | `ui/wallet/WalletViewModel.kt` | Load + deposit intent |
| API | `wallet/WalletApi.kt` | Retrofit `/api/wallet/*` |
| Models | `wallet/WalletModels.kt` | DTOs / envelope |
| Repo | `wallet/WalletRepository.kt` | Authenticated calls via `RetrofitClient` |

Theme: `ui/theme/ImaniTheme` + `ImaniColor` (blue primary `#0D47A1`, surface, green/red for ledger).

---

## Backend contract

Mount (Django): **`/api/wallet/`** — see `ridehaiingbackend/Wallet/WALLET.md`.

| Method | Path | Used by UI |
|--------|------|------------|
| GET | `wallet/balance/` | Hero card |
| GET | `wallet/ledger/?limit=` | Activity list |
| GET | `wallet/intents/?limit=` | Recent intents |
| POST | `wallet/deposits/` | Top-up bottom sheet |
| POST | `wallet/spend/` | **Not** from this screen (needs checkout `quote_id`) |

Auth: JWT via `RetrofitClient` / `authRetrofitClient` hub.

### Deposit semantics (important)

1. `POST deposits/` creates a **PENDING_PROVIDER** intent only — **does not** credit balance.  
2. Balance credits when backend runs `DepositService.apply_provider_success` after M-Pesa/Airtel success.  
3. STK callback auto-wire on backend may still be incomplete; UI shows a clear “after provider confirms” message.

---

## User flow

```text
Settings
  └─ Wallet row
       └─ WalletComposeFragment (Compose)
            ├─ Load balance + ledger + intents
            ├─ FAB / Top up → bottom sheet (amount + MPESA|AIRTEL)
            └─ POST deposits/ → success banner → refresh intents
```

---

## Intentionally unchanged

- Drawer menu item `@id/wallet` still opens **STK ride payment** dialog (`paymentsystem.PaymentFragment`) — separate product path.  
- No other Settings rows (name/phone/email/support) modified beyond adding Wallet.  
- No changes to booking checkout spend integration (future: call `POST wallet/spend/` with `quote_id`).

---

## Files touched / added

### Added

- `app/src/main/java/.../wallet/WalletApi.kt`
- `app/src/main/java/.../wallet/WalletModels.kt`
- `app/src/main/java/.../wallet/WalletRepository.kt`
- `app/src/main/java/.../ui/wallet/WalletComposeFragment.kt`
- `app/src/main/java/.../ui/wallet/WalletViewModel.kt`
- `app/src/main/java/.../ui/wallet/WalletScreen.kt`
- `docs/WALLET_ANDROID.md` (this file)

### Modified (minimal)

- `app/src/main/res/layout/fragment_settings.xml` — Wallet include only  
- `app/src/main/java/.../supportProfile/SettingsFragment.java` — setup + `openWallet()`  
- `app/src/main/res/navigation/nav_graph.xml` — destination + action  

### Unrelated build unblocks (pre-existing missing resources)

- `res/drawable/textbox_background.xml` — referenced by `layout_top_panel.xml`  
- `res/values/strings.xml` — `field_one_hint` / `field_two_hint` / `field_three_hint`  

Compose dependencies already present (Terms of Service).

**Compile note:** Wallet Kotlin sources compile. Full APK may still fail on pre-existing `Sync/DataSyncStatus.java` (Kotlin syntax in a `.java` file) until that file is fixed separately.

---

## Manual test checklist

1. Log in → open **Settings**.  
2. Tap **Wallet** → Compose screen with blue top bar + balance card.  
3. With backend up: balance loads (zeros for new accounts).  
4. Top up KES 100 M-Pesa → intent appears; message about provider confirmation.  
5. Back returns to Settings.  
6. Offline / bad token → error panel + Retry.

---

## Follow-ups (not in this change)

- Wire backend STK success → `DepositService.apply_provider_success` for deposits.  
- Optional: initiate STK from Android after deposit intent + `attach_provider_reference`.  
- Checkout “Pay with wallet” using `POST wallet/spend/`.  
- Offline soft browse via `OfflineScope.WALLET_BROWSE` / `WALLET_DEPOSIT`.  
- Distinct wallet icon drawable (currently reuses profile icon like other settings rows).

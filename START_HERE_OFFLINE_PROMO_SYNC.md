# START HERE — Offline sync & promo attempts (distributed systems)

> **Read this before touching promo UI, checkout, or offline sync again.**  
> File location (project root of the Android app):  
> `Imanicommunityapp/START_HERE_OFFLINE_PROMO_SYNC.md`  
> Rename or move only if you update every pointer to it.

---

## The problem in one sentence

**The phone and the server can disagree about whether the user still has a usable promo (attempts left).**  
If the UI only trusts local state, it can show “you have a discount” when the server already spent or expired it—or the opposite after offline use.

That is a **distributed systems** problem (two sources of truth + delay + partial offline), not a simple UI bug.

---

## Why this bites *this* app

| System | What it knows |
|--------|----------------|
| **Server `DiscountCode`** | Real `allowed_attempts`, status (REDEEMED / USED / EXPIRED), clocks |
| **Android UI / RAM / Room** | Whatever was last synced, last validate response, or cached |
| **FareQuote / checkout** | Snapshot at quote time; does not always re-check attempts at pay |
| **Offline / sync queue** | Partial — not a full promo ledger yet |

**Attempts are deducted on the server** (checkout reserve + payment consume path), not when the user only *sees* a promo on screen.

So:

```text
UI: "You have a promo"     ← local / last successful redeem-validate
Server: attempts == 0      ← already USED after a paid trip (or expired)
```

Until the app **tracks or revalidates attempts**, the UI can lie.

---

## What “attempts haven’t been deducted” actually means

| Question | Who can answer truthfully? |
|----------|----------------------------|
| Did the user redeem the code onto their account? | Server: status **REDEEMED** |
| Are attempts still &gt; 0? | Server: `allowed_attempts` |
| Was a spend completed? | Server: status **USED** (or attempts decremented) after **payment success** |
| Can the UI assume attempts still remain? | **Only after a successful sync/read of server state** (or a conservative local ledger you design) |

**Redeem (validate API) ≠ deduct attempt.**  
Redeem = couple code to user.  
Deduct = pay path (RESERVED → consume → USED).

If the UI treats “I redeemed earlier” as “I always have a discount,” it is wrong.

---

## Where offline sync fits

Your offline/sync engine is **partial** because hard problems like this need an explicit policy:

1. **What is cached offline?** (e.g. last known `allowed_attempts`, status, `redeemed_at`)  
2. **What is allowed offline?** (e.g. show promo only if last sync said attempts &gt; 0 **and** not older than X)  
3. **What is reconciled online?** (pull promo state before checkout/pay; on conflict, **server wins**)  
4. **What events go in the queue?** (e.g. “checkout quote”, not “I decided I have a promo”)

Until those rules exist, “offline sync” cannot correctly answer:

> How does the UI know attempts haven’t been deducted?

**Answer today (honest):**  
It largely **doesn’t**, unless it just called the server. There is no first-class local attempt ledger wired to payment success/failure and rehydration.

---

## What the UI should do (product rules to implement next)

### Conservative display rule (recommended start)

```text
Show “you have a promo” only if:
  - last successful server snapshot says status = REDEEMED
  - AND allowed_attempts > 0
  - AND not past claim/shelf time (if you store those fields)
  - AND snapshot age < freshness threshold (or force refresh online)

If offline and snapshot missing/stale:
  - do NOT promise a discount
  - show base fare only / “connect to apply promo”
```

### At checkout / pay (online)

```text
Never trust UI alone.
Re-check server (or rely on FareQuote + payment using discounted_fare
only from an OPEN quote that already applied promo server-side).
If attempts were 0, server returns base fare / rejects reserve.
```

### After payment success (when wired)

```text
Local promo cache: set attempts-- or mark USED from server response.
Do not wait for a vague “sync later” without writing the event.
```

### After payment fail / abandon

```text
Attempts should still be remaining on server if consume never ran.
Local cache must not mark USED optimistically unless you have a
compensating transaction (usually: only mark USED after server confirms).
```

---

## Distributed systems patterns to use here

| Pattern | Application |
|---------|-------------|
| **Server is source of truth** for attempts | Always |
| **Local cache is a projection** | Display only; versioned / timestamped |
| **Optimistic UI is dangerous** for money/promo | Prefer pessimistic or “soft” UI offline |
| **Idempotent revalidation** | Re-hit validate/status before checkout |
| **Event log offline** | Queue “intent to checkout”; not “I spent a promo” until ACK |
| **Conflict resolution** | Server wins on attempts/status |

---

## Where to start next time (checklist)

When you open the project again, start here:

1. [ ] **Define a local PromoSnapshot** (code, status, attempts_remaining, redeemed_at, synced_at).  
2. [ ] **Write it only from server responses** (validate, checkout, pay success/fail).  
3. [ ] **UI reads PromoSnapshot**, never invents attempts.  
4. [ ] **If `synced_at` stale or offline → hide or soft-warn promo.**  
5. [ ] **Checkout/pay path revalidates online** when possible.  
6. [ ] **Sync engine:** rehydrate promo snapshot on app start / connectivity restore (priority high).  
7. [ ] **Never deduct local attempts until server confirms spend** (or design explicit pending + rollback).  
8. [ ] Document the same rules in the offline queue package (link to this file).

---

## Related systems (so you don’t search forever)

| Area | Location (approx.) |
|------|---------------------|
| Promo redeem API client | `RandRsystem/` |
| Checkout client | `BookingSystem/` — hits `bookings/checkout/` |
| Coords + fares | `userCoordinates` — `fare`, nullable `discounted_fare` |
| Offline / queue | `Sync/Queue/` |
| Server promo truth | backend `RedeemAndRefferalSys` |
| Server quote | backend `FareQuote` / `FareQuoteService` |
| Server checkout | `POST /api/bookings/checkout/` |

Backend docs (if present on disk):  
`ridehaiingbackend/RedeemAndRefferalSys/PROMO_LIFECYCLE.md`  
`ridehaiingbackend/ride_matching/FARE_QUOTE.md`  
`ridehaiingbackend/ride_matching/CHECKOUT_API.md`

---

## Mental model (sticky)

```text
REDEEMED on account  ≠  attempt already spent
UI memory            ≠  server attempts
Offline cache        =  last projection, not truth
Pay success          =  only safe moment to mark attempt spent
```

**If the UI asks “do they still have a promo?” the app must answer from a synced attempts projection—or refuse to claim one until online.**

---

## Changelog

| Date | Note |
|------|------|
| 2026-03 | Created as the can’t-miss note for offline sync + promo attempts |
| 2026-07-19 | Unrelated to promo: network hub + Application init — see `NETWORK.md` and `SESSION_CHANGELOG_2026-07-19.md`. Backend `Support` / `UserSettings` apps scaffolded. |

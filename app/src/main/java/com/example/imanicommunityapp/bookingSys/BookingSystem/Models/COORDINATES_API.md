# Coordinates API contract (Android)

**Model:** `userCoordinates.java`  
**Stored via:** `UICoordinateRepo`, `BookingDetailsEntity`  
**Backend:** `get_route_coordinates` / create-booking nested `coordinates`

---

## JSON shape (success)

```json
{
  "start_lat": -1.28,
  "start_lng": 36.82,
  "end_lat": -1.30,
  "end_lng": 36.90,
  "destinations": ["CABANAS", "..."],
  "fare": 200,
  "base_fare": 200,
  "discounted_fare": 160
}
```

Or without pricing applied / older server:

```json
{
  "start_lat": ...,
  "fare": 200,
  "discounted_fare": null
}
```

Or field omitted entirely → Gson leaves `discounted_fare` as **null**.

---

## Field rules (client must enforce)

| Field | Type on client | Null? | Meaning |
|-------|----------------|-------|---------|
| `fare` | `int` (primitive) | No (0 if missing) | Base list price |
| `base_fare` | `Double` optional | Yes | Prefer for base when present |
| **`discounted_fare`** | **`Double`** | **Yes** | Pay amount after promo |

### Null rules for `discounted_fare`

1. **Never assume** it is present.  
2. **Never** write `double x = coords.getDiscountedFare()` without a null check.  
3. Use:
   - `coords.hasDiscountedFare()`
   - `coords.getDiscountedFare()` → `Double`
   - or `coords.getDisplayOrPayAmount()` (discounted if non-null, else base)  
4. Prefer server `quote_id` + payment bridge when charging money; local fallback is for UI only.

### Payment amount

```
pay = discounted_fare if not null else base_fare/fare
```

There is **no** separate `amount_due` field on the client model.

---

## Where it is wired

| Layer | Behaviour |
|-------|-----------|
| Gson → `userCoordinates` | Maps `discounted_fare` to `Double` (null-safe) |
| `UICoordinateRepo` | RAM `discountedFare` nullable; `getDisplayOrPayAmount()` |
| Room `booking_details` | Column `discounted_fare` nullable (DB v5) |
| `BookingRepository.storeBookingToDb` | Persists null when API omits discount |

---

## Changelog

| Date | Change |
|------|--------|
| 2026-03 | Introduced `fare` on coordinates |
| 2026-03 | Introduced nullable `discounted_fare` + docs + Room/RAM |

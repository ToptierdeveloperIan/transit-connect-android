# Android checkout client

**Endpoint:** `POST bookings/checkout/` (base URL already includes `/api/`)  
**Backend:** `CheckoutView` — see `ride_matching/CHECKOUT_API.md`

---

## Flow change

| Before | After |
|--------|--------|
| SelectStop submit → `createBooking` → `bookings/create/` | SelectStop submit → **`checkout`** → **`bookings/checkout/`** |
| Server creates Booking | **No** Booking row; quote + coords only |
| `booking_id` required | `booking_id` null; `quote_id` for pay |

---

## Classes

| Class | Role |
|-------|------|
| `CheckoutRequest` | Body: route_name, destination, optional promo_code |
| `CheckoutResponse` | success, coordinates, quote_id, booking_id=null |
| `BookingInterface.checkout` | Retrofit method |
| `BookingRepository.checkout` | Network + RAM store + callback |
| `BookingFlowViewModel.onEnterSubmittingBooking` | Calls checkout, dispatches success/failure |

Legacy `createBooking` remains for future canonical booking after payment.

---

## Success handling

1. `UICoordinateRepo.storeCheckoutInRam` — coords, fares, quoteId; bookingId=-1  
2. `BookingEvent.bookingSuccess(-1, coordinates)` — UI can show route / leave SelectStop  
3. Payment (later) uses `quoteId` + `discounted_fare` (null-safe)

---

## Null rules

- `discounted_fare` may be null → use `hasDiscountedFare()` / `getDisplayOrPayAmount()`  
- `booking_id` is null on checkout  
- `quote_id` may be null if pricing did not persist (e.g. anonymous — not the case when logged in)

---

## Changelog

| Date | Change |
|------|--------|
| 2026-03 | Rider submit flow switched from create → checkout API |

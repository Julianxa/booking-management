### Booking / Booked Event / Payment Status Model

This project uses **three layers**:

1. **Booking (parent)**: `Enums.BookingStatus`
2. **Payment (money)**: `Enums.PaymentStatus`
3. **BookingEvents (child tickets)**: `Enums.BookingEventStatus` (`PENDING | AVAILABLE | CHECKED_IN | NO_SHOW | CANCELLED`)

Key ideas:

- **BookingEvents** represent seat holding and ticket availability.
- **Booking** represents the payment/authorization state for the whole booking.
- **Payment** represents the money state (captured/refunded).

---

## 1) Booking & Payment lifecycle (whole booking)

| BookingStatus         | PaymentStatus                 | When it happens                                 | BookingEvents impact                              |
|-----------------------|-------------------------------|-------------------------------------------------|---------------------------------------------------|
| `ON_HOLD`             | (none)                        | booking row created                             | booking events start in `PENDING`                 |
| `AWAITING_PAYMENT`    | `PENDING`                     | Stripe Checkout Session created                 | still `PENDING`                                   |
| `PAYMENT_IN_PROGRESS` | `INITIATED`                   | `payment_intent.created` received               | still `PENDING`                                   |
| `PAYMENT_IN_PROGRESS` | `REQUIRES_ACTION`             | `payment_intent.requires_action` (e.g. 3DS)     | still `PENDING`                                   |
| `PAID`                | `SUCCEEDED`                   | payment succeeded (checkpoint)                  | events not activated until confirmation completes |
| `CONFIRMED`           | `SUCCEEDED`                   | confirmation done; events activated             | each ticket becomes `AVAILABLE`                   |
| `FAILED`              | `FAILED`                      | unpaid checkout ended after a decline / failure | tickets become `CANCELLED`                        |
| `EXPIRED`             | `EXPIRED`                     | unpaid checkout timed out / hold released       | tickets become `CANCELLED`                        |
| `CANCELLED`           | `SUCCEEDED`                   | paid booking cancelled (admin/OCTO/customer)    | tickets become `CANCELLED`                        |
| `REFUNDED`            | `REFUNDED`                    | refund completed (money returned)               | tickets are expected to already be `CANCELLED`    |

### Late payment behavior (no revive)
If Stripe reports a late `payment_intent.succeeded` after the booking is already in:

- unpaid terminal state (`EXPIRED` / `FAILED`) or
- paid-cancel terminal state (`CANCELLED`)

then:

- booking is **not** revived back to `CONFIRMED`
- payment is recorded as `SUCCEEDED`
- refund can be processed later

---

## 2) 3DS retry / retry after decline (same checkout session)

While the Stripe checkout session is still open, the system allows transitions so that after a decline:

- `FAILED` payment can transition back to `REQUIRES_ACTION` and then to `SUCCEEDED`
- booking remains in retryable states until the session ends successfully

---

## 3) Cancel-before-pay / Stripe back button

Stripe cancel button/back is not used as a “final cancel” signal.

Unpaid booking finalization happens via:

- Stripe `checkout.session.expired`, and
- `BookingReservationCleanupScheduler` (pending/awaiting/payment-in-progress timeouts)

---

## 4) Admin/OCTO close & OPEN_WITH_BOOKINGS

### CLOSE_WITH_BOOKINGS
- Cancels booked events (`BookingEventStatus.CANCELLED`) and releases capacity.
- If the parent booking was **unpaid and still in-progress**, CLOSE must end as:
  - `EXPIRED` / `FAILED` (not `CANCELLED`)

### OPEN_WITH_BOOKINGS
- Restores only tickets whose **parent booking is restorable** (paid).
- Parent `EXPIRED` / `FAILED` / `REFUNDED` bookings are **not** restored.

---

## 5) Booked Event status rules (alignment with booking/payment)

### Activation after confirmation
When booking reaches `CONFIRMED`:
- ticket status becomes `AVAILABLE`
- `cancelledAt` is cleared

### Ticket cancellation
When booking is finalized as unpaid (`EXPIRED` / `FAILED`) or paid-cancelled (`CANCELLED`):
- ticket status becomes `CANCELLED`
- capacity is released

### Check-in gating
Check-in is only allowed when:
- booked event is `AVAILABLE`
- parent booking is `CONFIRMED`
- booked event is not cancelled (`cancelledAt == null`)

---

## 6) Gift certificate alignment

- Gift certificate redemption is confirmed only when booking is confirmed (`CONFIRMED`).
- Cancellation of a booking only cancels **`PENDING`** redemptions.
- If the booking was already confirmed and redemption is `SUCCESS`, cancellation keeps the gift certificate used.

---

## 7) Refunds: cancel/expire/fail is step 1, refund is step 2

Refund is **money-only** and is a separate step from seat/ticket cancellation.

### Refund eligibility (whole booking)
Refund API allows only bookings in:
- `CANCELLED` (paid cancel)
- `EXPIRED` (unpaid timeout/hold release)
- `FAILED` (declined unpaid/failed hold)

For `ONLINE_PAYMENT`, refund requires a captured Stripe payment (`PaymentStatus.SUCCEEDED`).
If Stripe never captured money, refund fails with “no captured payment”.

### Failed to partial refund (documented limitation)
Partial refunds are not fully modeled as a real “partially refunded money state”.

Current behavior:
- `OFFLINE_PAYMENT`: refunds call `finalizeOfflineRefund(...)` and mark Booking/Payment as `REFUNDED` (even if `is_full_refund=false`).
- `ONLINE_PAYMENT`:
  - full refunds (`is_full_refund=true`) create Stripe refunds
  - non-full requests (`is_full_refund=false`) do **not** reliably trigger Stripe partial refunds; the system finalizes like an offline finalization path and still marks `REFUNDED`.


### Reorder the event images during adding new image(s)
curl -X 'PATCH' \
'http://127.0.0.1:8080/api/v1/events/EVT-TME8PVU4GA' \
-H 'accept: application/json' \
-H 'Content-Type: multipart/form-data' \
-F 'eventPics=@cherry.jpeg;type=image/jpeg' \
-F 'contactInfo={"event_pics":[{"upload_index":0},{"key":"eventImages/EVT-TME8PVU4GA_ec9e919b-184e-4c47-97d4-e5ba3062ff9e_apple.jpeg"},{"key":"eventImages/EVT-TME8PVU4GA_15440324-5235-4d9b-a55b-bd6b95d286e3_banana.jpeg"}]}'

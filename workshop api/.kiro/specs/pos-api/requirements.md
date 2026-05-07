# Sales API Requirements

## 1. Product & Customer Integration
- The system must communicate with external Product and Customer APIs.
- Must handle 503 errors gracefully if external services are down.

## 2. Sale Lifecycle
- **Active**: New sales start here. Items can be added/removed.
- **Completed**: Checkout successful. Stock decremented.
- **Cancelled**: Abandoned sales. Free text reason required.
- **Frozen**: Put on hold. Retains all items. Expires after 2 hours.
- **Returned**: Fully refunded. Stock restored.
- **Partially Returned**: Some items refunded. Stock partially restored.

## 3. Payments
- **Cash**: Requires amount received >= total.
- **Credit**: Requires approved customer.

## 4. Business Rules
- All monetary math uses `BigDecimal`.
- Stock checks happen at item addition AND checkout.

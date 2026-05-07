# Design Specification

## Architecture
- Layered Architecture: Controllers, Services, Repositories, Clients.
- Global Exception Handler to translate domain exceptions to HTTP statuses (404, 409, 422, 503).

## Data Model
- `Sale`: id, terminalId, cashierId, customerId, status, paymentType, subtotal, tax, discount, total, amountReceived, changeAmount, creditReference, transactionId, cancellationReason, frozenAt, completedAt.
- `SaleItem`: id, saleId, productId, barcode, productName, unitPrice, quantity, lineTotal, returnedQuantity.

## Integrations
- `RestTemplate` used for HTTP calls to external services.
- Base URLs injected via `@Value`.

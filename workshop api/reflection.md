# Workshop Reflection

## 1. What did you have to clarify or fix in the generated specs?
When generating the specifications, I had to ensure that the AI explicitly documented all the state transitions for a Sale (e.g., ACTIVE -> FROZEN -> ACTIVE). Initially, AI tends to overlook the "unhappy paths" such as what happens when a customer's credit is REJECTED, or the edge case where someone attempts a full return on an already partially returned sale. I had to explicitly fix the design spec to mandate a `GlobalExceptionHandler` to translate these business validation failures into the requested 409, 422, and 503 HTTP status codes so the API contract was perfectly clear.

## 2. How did the quality of your prompt affect the generated code?
The precision of the prompt directly dictated the quality of the service layer. By explicitly mentioning that stock validation must occur *both* when adding an item and at checkout time, the generated code included robust concurrency prevention. Specifying the exact fields required for a Receipt and mandating WireMock for external dependencies ensured that the final generated tests and DTOs were correctly structured from the start, minimizing refactoring.

## 3. What would you do differently next time?
Next time, I would be even more explicit about the exact JSON payload structures expected for requests and responses in the prompt, so the AI defines the DTOs with their `@NotBlank` and `@NotNull` validation annotations perfectly on the first try. I would also explicitly define the scheduling frequency for the background job that cleans up expired frozen sales, rather than just stating that they expire after 2 hours.

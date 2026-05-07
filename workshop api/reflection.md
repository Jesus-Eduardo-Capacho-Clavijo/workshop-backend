# Reflexión del Taller

## 1. ¿Qué tuviste que aclarar o corregir en las especificaciones generadas?
Al generar las especificaciones, tuve que asegurarme de que la IA documentara explícitamente todas las transiciones de estado de una Venta (p.ej., ACTIVA → CONGELADA → ACTIVA). Inicialmente, la IA tiende a pasar por alto los "caminos no deseados", como lo que ocurre cuando el crédito de un cliente es RECHAZADO, o el caso límite en el que alguien intenta una devolución total de una venta que ya había sido devuelta parcialmente. Tuve que corregir explícitamente el diseño para exigir un `GlobalExceptionHandler` que tradujera estas fallas de validación de negocio a los códigos de estado HTTP 409, 422 y 503 solicitados, de modo que el contrato de la API quedara perfectamente claro.

## 2. ¿Cómo afectó la calidad de tu prompt al código generado?
La precisión del prompt determinó directamente la calidad de la capa de servicio. Al mencionar explícitamente que la validación de stock debe realizarse *tanto* al agregar un artículo como al momento del checkout, el código generado incluyó una robusta prevención de concurrencia. Especificar los campos exactos requeridos para un Recibo y exigir WireMock para dependencias externas aseguró que las pruebas y DTOs finales estuvieran estructurados correctamente desde el principio, minimizando la refactorización.

## 3. ¿Qué harías diferente la próxima vez?
La próxima vez, sería aún más explícito sobre las estructuras exactas de payload JSON esperadas para solicitudes y respuestas en el prompt, de modo que la IA defina los DTOs con sus anotaciones de validación `@NotBlank` y `@NotNull` perfectamente en el primer intento. También definiría explícitamente la frecuencia de ejecución del trabajo en segundo plano que limpia ventas congeladas expiradas, en lugar de simplemente indicar que expiran después de 2 horas.

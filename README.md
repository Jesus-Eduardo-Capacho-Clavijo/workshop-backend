## Descripción
Este proyecto es una **API REST** desarrollada con **Spring Boot 3.x** que implementa la lógica de negocio de un sistema de Punto de Venta (POS) para un supermercado. Está pensado para ser ejecutado localmente sin depender de bases de datos externas, usando almacenamiento **en memoria**.

---

## Requisitos previos
- **Java 17** o superior (JDK)
- **Maven 3.8+** (gestor de dependencias)
- **Git** (opcional, para clonar el repositorio)
- **IDE** opcional (IntelliJ IDEA, VS Code, Eclipse)
- **Docker** (solo si deseas usar contenedores, no es obligatorio)

---

## Cómo obtener el código
```bash
# Clonar el repositorio (ejemplo)
git clone https://github.com/tu-usuario/workshop-backend.git
cd workshop-backend/workshop api
```

---

## Compilación y ejecución en local
1. **Instalar dependencias**
   ```bash
   mvn clean install
   ```
   Esto compila el proyecto y descarga todas las dependencias necesarias.

2. **Ejecutar la aplicación**
   ```bash
   mvn spring-boot:run
   ```
   La API quedará disponible en `http://localhost:8080`.

3. **Verificar que funciona**
   - Abrir tu navegador y visitar `http://localhost:8080/actuator/health` para comprobar el estado de salud (debe responder `{"status":"UP"}`).
   - La documentación OpenAPI/Swagger está disponible en `http://localhost:8080/swagger-ui.html`.

---

## Pruebas
Ejecuta el conjunto completo de pruebas unitarias e integrales con:
```bash
mvn test
```
Los tests usan **JUnit 5**, **Mockito** y **WireMock** para simular servicios externos.

---

## Tecnologías y lenguajes usados
| Categoría | Herramienta / Lenguaje |
|-----------|------------------------|
| **Lenguaje** | Java 17 |
| **Framework** | Spring Boot 3.x (Web, Data, Validation, Actuator) |
| **Gestor de dependencias** | Maven |
| **Base de datos en memoria** | H2 (modo *in‑memory*) |
| **Pruebas** | JUnit 5, Mockito, WireMock |
| **Documentación API** | Springdoc OpenAPI + Swagger UI |
| **Control de versiones** | Git |
| **Contenedorización (opcional)** | Docker |

---

## Variables de entorno (opcional)
| Variable | Descripción | Valor por defecto |
|----------|-------------|-------------------|
| `SERVER_PORT` | Puerto en el que la aplicación escucha | `8080` |
| `SPRING_PROFILES_ACTIVE` | Perfil de Spring activo (por ejemplo `dev`, `test`) | `dev` |

---

## Contribuir
1. Forkea el repositorio.
2. Crea una rama para tu funcionalidad (`git checkout -b feature/nueva-funcionalidad`).
3. Implementa los cambios y escribe pruebas.
4. Ejecuta `mvn test` y verifica que todo pase.
5. Abre un Pull Request describiendo los cambios.

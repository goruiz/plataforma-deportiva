# Arquitectura del backend

Monolito modular con **Clean Architecture** sobre Java 25, Spring Boot 4 y PostgreSQL. Es la API REST única de los dos frontends (`administration-angular` y `usuario-flutter`) bajo el context-path `/api`.

> Verificado contra el código el 2026-10-12. Las reglas de trabajo y el estado detallado (reglas de negocio, seguridad, deuda técnica) están en [CLAUDE.md](CLAUDE.md); aquí solo está la **estructura** y cómo se conectan las piezas. Los textos de `documentation` y `modules/documentation` describen módulos que no existen (venue, ranking, report, audit): no los uses como referencia.

## Vista general

```
com.platform.backend
├── BackendApplication
├── modules/        # un paquete por dominio de negocio, cada uno con las 4 capas
│   ├── users  players  teams  events  matches
│   └── courts  sportcomplexes  holidays  menu  permissions
└── shared/         # código transversal sin lógica de ningún módulo
```

| Módulo | Responsabilidad |
|---|---|
| `users` | Usuarios (admin y app), autenticación, roles. Aloja también la configuración de seguridad |
| `players` | Jugadores con un único equipo e invitaciones por email |
| `teams` | Equipos e inscripción a eventos |
| `events` | Eventos, tipos de evento (liga, torneo…) y categorías |
| `matches` | Partidos, calendario automático y tabla de posiciones |
| `courts`, `sportcomplexes` | Canchas dentro de complejos deportivos |
| `holidays` | Feriados y días sin juego que el calendario automático omite |
| `menu` | Menú jerárquico por rol (alimenta la navegación de ambos frontends) |
| `permissions` | CRUD de permisos por rol, sin uso real |

Los diez módulos tienen las cuatro capas con código. `modules/documentation` es un archivo de texto, no un módulo.

## Capas de un módulo

```
modules/<modulo>/
├── domain/
│   ├── entities/            <Noun>Entity            entidad JPA
│   └── irepositories/       I<Noun>Repository       puerto de salida, en lenguaje de dominio
├── application/
│   ├── iservices/           I<Noun>Service          puerto de entrada (caso de uso)
│   ├── services/            <Noun>Service           implementación, aquí van las reglas de negocio
│   └── mappers/             <Noun>Mapper            MapStruct: Request/Entity ↔ Entity/Response
├── infrastructure/
│   └── persistence/
│       ├── jpa/             <Noun>JpaRepository     Spring Data crudo
│       └── repositories/    <Noun>Repository        adaptador: implementa I<Noun>Repository sobre el JpaRepository
└── presentation/
    ├── controllers/         <Noun>Controller        REST, devuelve ResponseEntity<ApiResponse<T>>
    ├── requests/<Verbo><Noun>Request/…              un archivo y una carpeta por DTO
    └── responses/<Noun>Response/…
```

**Dependencias:** `domain` no depende de nada; `application` depende de `domain`; `infrastructure` y `presentation` dependen de `application` y `domain`. Una entidad JPA nunca sale por un controller: siempre pasa por Request/Response y Mapper.

**Puerto y adaptador:** el servicio solo conoce `I<Noun>Repository`. Su implementación `<Noun>Repository` (infraestructura) traduce cada método de dominio a una consulta del `<Noun>JpaRepository`. Por ejemplo, `IEventRepository.findAllActive()` se resuelve con `EventsJpaRepository.findAllByDeletedAtIsNull()`.

## Recorrido de una petición

Ejemplo, `POST /events`:

1. **Filtros de seguridad** (ver más abajo): `LoginRateLimiterFilter` y `JwtAuthenticationFilter` antes de llegar al controller.
2. **`EventsController`** valida el body con `@Valid` (`CreateEventRequest`) y delega en `IEventsService`.
3. **`EventsService`** (`@Transactional`) aplica las reglas (nombre único, el tipo de evento debe existir), convierte con `EventMapper` y guarda a través de `IEventRepository`.
4. **El controller** envuelve el resultado con `ApiResponse.created(...)` (HTTP 201).
5. Si algo falla, **`GlobalException`** (`@RestControllerAdvice`) lo convierte en un `ApiResponse` de error.

## Paquete `shared`

| Subpaquete | Contenido |
|---|---|
| `domain/entities` | `BaseEntity` (id UUID, auditoría y soft delete) y `BaseTranslatableEntity` (suma claves de traducción) |
| `domain/interfaces`, `domain/utils` | `HasUniqueField` y `SoftDeleteHelper` |
| `presentation/responses` | `ApiResponse<T>` con `ok`, `created`, `deleted` y `error` |
| `infraestructure/exceptions` | `GlobalException`, `FieldValidationException` |
| `infraestructure/security` | `jwt/` (`JwtService`, `JwtAuthenticationFilter`, `JwtAuthenticationEntryPoint`) y `password/` (`PasswordPolicy`, `TemporaryPasswordGenerator`) |
| `infraestructure/ratelimit` | `LoginRateLimiterFilter` |
| `infraestructure/email` | `EmailService` |
| `infraestructure/config`, `infraestructure/audit` | `JacksonConfig`, `JpaAuditConfig`, `SwaggerConfig` y el acceso al auditor actual |

Ojo con la ortografía: dentro de un módulo la capa se llama `infrastructure`, pero en `shared` se llama `infraestructure`. Es así en el repo real y se respeta en cada lugar.

## Seguridad

- **Stateless con JWT**, CSRF desactivado, sesión `STATELESS`, contraseñas con BCrypt. CORS abierto (`*` con credenciales).
- `SecurityFilterChain` (en `modules/users/infrastructure/configuration/security/SecurityConfig`): `LoginRateLimiterFilter` y `JwtAuthenticationFilter` se registran antes de `UsernamePasswordAuthenticationFilter`.
- **Públicos:** `POST /users/auth/login`, `POST /users/auth/register`, `POST /players/auth/register` y Swagger (`/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`). Todo lo demás exige un JWT válido.
- Sin token, o con uno vencido o inválido, `JwtAuthenticationEntryPoint` responde **401** con `ApiResponse`; los dos frontends usan ese 401 para cerrar la sesión.
- **No hay autorización por rol**: basta un JWT válido (ver CLAUDE.md, "Seguridad: estado real"). El flag `admin` solo lo usa el login para decidir quién entra a qué app, según `app.security.admin-role-id`.

## Persistencia

- **Base de datos:** PostgreSQL. **Flyway es el único dueño del esquema y de los datos iniciales**: hay una sola migración, `db/migration/V1__migracion_inicial.sql`, y Hibernate solo valida (`ddl-auto=validate`). Nada de scripts ni de seeders en Java: todo cambio es una nueva `V{n}`. Ver "Base de datos y migraciones: estado real" en CLAUDE.md.
- **`BaseEntity`:** id UUID generado, `createdBy/At`, `updatedBy/At` y `deletedBy/At`. El auditor sale de `AuditorAwareImpl`, que vive en `users`.
- **Soft delete:** borrar es marcar `deletedAt` y guardar; el método `delete` del adaptador hace un `save`. Todas las consultas de lectura filtran `...DeletedAtIsNull`. Cada recurso expone además `DELETE /x/{id}/hard` (borrado físico).
- **Campos únicos y borrado:** `HasUniqueField.releaseUniqueFields` renombra los campos únicos al borrar para poder reutilizarlos, pero hoy **solo `EventsService.delete` llama a `SoftDeleteHelper`**. En el resto, borrar y recrear el mismo nombre o email choca con el unique de la base (409).
- **Relaciones:** las de `matches` (equipos, evento, cancha), `team_invitations.teamId`, `teams.categoryId` y `menu.idParentMenu` son UUID planos, sin FK. Solo `teams.categoryId` se valida (en `TeamsService`).

## Errores y contrato de respuesta

Toda respuesta, éxito o error, es `ApiResponse<T> = {success, status, message, data, timestamp}`. `GlobalException` traduce así:

| Excepción | HTTP | Nota |
|---|---|---|
| `MethodArgumentNotValidException`, `FieldValidationException` | 400 | `data` trae el mapa `campo → motivo` |
| `IllegalArgumentException`, `IllegalStateException` | 400 | mensaje de la excepción |
| `BadCredentialsException` | 401 | |
| `AccessDeniedException`, `AuthorizationDeniedException` | 403 | |
| `EntityNotFoundException` | 404 | |
| `DataIntegrityViolationException` | 409 | mensaje genérico: se pierde el motivo concreto |
| cualquier otra | 500 | con `ex.getMessage()` |

Los frontends desenvuelven `data` a mano y no hay cliente generado: **un cambio de endpoint o DTO hay que reflejarlo en los servicios de Angular y de Flutter**.

## Cómo agregar un recurso o módulo

1. Copia el patrón de un módulo maduro (`teams`, `events`): mismos nombres y misma ubicación por capa, descritos arriba.
2. Entidad que extiende `BaseEntity` (o `BaseTranslatableEntity`), con `@Table(name="snake_case_plural")`.
3. Puerto `I<Noun>Repository` con métodos de dominio que filtren borrados, adaptador `<Noun>Repository` y `<Noun>JpaRepository`.
4. `I<Noun>Service` y `<Noun>Service` con `@Transactional`; el borrado lógico debe usar `SoftDeleteHelper` si la entidad tiene campos únicos.
5. `Create/Update<Noun>Request` y `<Noun>Response`, cada uno en su carpeta, más el `<Noun>Mapper`.
6. Controller con `ResponseEntity<ApiResponse<T>>` y los factory methods de `ApiResponse`.
7. Estados y tipos fijos: `enum`, no `String` suelto; ids y límites: configuración (`app.*`), no constantes.
8. Si el recurso es una pantalla del panel, sembrar un ítem de `menu` con la `url` del frontend (en una migración nueva) y reflejar el contrato en Angular y Flutter.
9. Tests siguiendo el estilo de `UsersServiceTest` (Mockito) o de los tests de controller con `MockMvc` standalone y `GlobalException`.

## Desviaciones conocidas de la arquitectura

El detalle y el resto de la deuda está en CLAUDE.md; las que afectan a la estructura:

- La **configuración de seguridad** y `AuditorAwareImpl` viven en `modules/users`, y `shared` importa `AuthenticatedUser` desde allí, lo que invierte la dependencia (`shared` no debería conocer un módulo).
- Estados y tipos son `String` libres; los enums del backend son `ClientType`, `TeamStatus` (valida el `status` de un equipo, aunque la columna sigue siendo texto) y los códigos de error que los clientes reciben como motivo (`EventFormatError`, `CategoryError`, `PasswordChangeError`, `TeamError`).
- `JacksonConfig` define un `ObjectMapper` que nadie inyecta, así que `app.json.naming-strategy` probablemente no tiene efecto.
- Hay 13 clases de test: cubren `users`, `events`, `teams` (solo `TeamsService`), `matches` (solo posiciones), JWT y el generador de contraseñas. `players`, `courts`, `sportcomplexes`, `menu` y el calendario automático no tienen.

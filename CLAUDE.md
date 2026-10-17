# Instrucciones para Claude en este proyecto

## Cómo trabajar (siempre)

### Mentalidad

- Pensar siempre como un **desarrollador senior** y un **diseñador UX/UI senior** al mismo tiempo. Aquí se aplica sobre todo al diseño de la API: respuestas claras, errores útiles y contratos consistentes para quien consume el backend.

### Coherencia con lo existente

- **Siempre seguir los patrones, la arquitectura y la estructura de carpetas ya implementados.** No introducir un patrón distinto (otra forma de organizar capas, otra convención de nombres, otro estilo de respuesta HTTP) sin que se pida explícitamente.
- Antes de crear algo nuevo, mirar cómo está hecho en un módulo ya maduro (ej. `modules/teams`) y replicar exactamente ese patrón.

### Legibilidad

- El código debe ser **muy fácil de entender para cualquier persona**, no solo para quien lo escribió.
- Los nombres de clases, variables, métodos, archivos, etc. deben dejar **muy claro qué hacen**, sin miedo a que sean largos.
  - **Excepción:** el nombre del **archivo** debe ser claro pero **no demasiado largo** — nombres de archivo muy largos rompen la subida de cambios (límite de longitud de ruta en Windows). Si un nombre de archivo se vuelve muy largo, acortarlo sin perder claridad.
- Los comentarios deben ser **precisos, claros y concretos**, nunca exhaustivos: explican el *porqué* de lo que no es evidente, no repiten lo que el código ya dice.

### Estructura y calidad

- El código debe ser siempre **ordenado, eficiente y escalable**.
- Los métodos **no deben ser demasiado largos**: cuando crezcan, dividirlos en submétodos con nombres descriptivos, cada uno con una única responsabilidad.
- Los métodos deben ser **reutilizables**: evitar duplicar lógica entre servicios/mappers y extraerla a un método común; si aplica a 2+ módulos, subirla a `shared`.

### Nada fijo en el código

- **Nada debe estar fijo ("hardcodeado"): todo debe ser dinámico** (ids, roles, límites, URLs, mensajes, etc. vienen de configuración, base de datos o parámetros; ver el ejemplo de `app.security.admin-role-id`).
- Si algo **necesariamente** debe ser fijo (estados, tipos, roles, etc.), modelarlo con un **`enum`** en lugar de strings o números mágicos. Ubicarlo en el módulo al que pertenece, o en `shared` si lo usan 2+ módulos.

## Proyectos relacionados (monorepo lógico "Plataforma deportiva")

- Backend (este proyecto, Spring Boot): `C:\Users\WALDO\Documents\Proyectos\Plataforma deportiva\backend`
- Frontend admin (Angular): `C:\Users\WALDO\Documents\Proyectos\Plataforma deportiva\frontend\administration-angular`
- Frontend usuario (Flutter): `C:\Users\WALDO\Documents\Proyectos\Plataforma deportiva\frontend\usuario-flutter`

## Stack del proyecto

- **Java 25**, **Spring Boot 4.0.6**, Maven (`mvnw`).
- **Lombok** + **MapStruct** (`componentModel="spring"`, `unmappedTargetPolicy/unmappedSourcePolicy = IGNORE`) para mappers entidad↔DTO.
- **PostgreSQL** + **Flyway** para migraciones (nunca editar una migración ya aplicada; siempre una nueva `V{n}__descripcion.sql`). Ojo: hoy Flyway está desactivado y el esquema lo crea Hibernate al arrancar; lee "Base de datos y migraciones: estado real" antes de crear una migración.
- **Spring Security** con **JWT** (`jjwt`), stateless, `LoginRateLimiterFilter` (bucket4j) sobre login.
- **springdoc-openapi** (Swagger UI en `/swagger-ui.html`).
- Base path global: `server.servlet.context-path=/api`.
- Además: Bean Validation, `spring-boot-starter-mail` (SMTP) y `dotenv-java` (declarada en el `pom.xml` pero sin uso). Versiones que importan: Lombok 1.18.46, MapStruct 1.6.3, jjwt 0.12.6, bucket4j 8.10.1, springdoc 2.8.8; Hibernate 7.2 viene con Spring Boot.

## Arquitectura: Clean Architecture + monolito modular

La estructura verificada contra el código (módulos, capas, seguridad, persistencia y cómo agregar un recurso) está en [ARCHITECTURE.md](ARCHITECTURE.md), en la raíz del backend; léelo antes de crear un módulo.

Documentado además en el propio repo (son archivos de texto sin extensión `.md`, fáciles de pasar por alto; los dos primeros describen módulos que no existen, así que ante una duda prevalece `ARCHITECTURE.md`):
- `src/main/java/com/platform/backend/documentation`
- `src/main/java/com/platform/backend/modules/documentation` ("Informe de Estructura Arquitectónica")
- `src/main/java/com/platform/backend/Principios SOLID`
- `src/main/resources/db/migration/README.md` (flujo de Flyway)

Cada módulo de negocio vive en `com.platform.backend.modules.<modulo>` (plural en minúscula: `teams`, `matches`, `players`, `sportcomplexes`, `users`, `events`, `courts`, `holidays`, `menu`, `permissions`) con exactamente estas 4 capas:

```
modules/<modulo>/
├── domain/
│   ├── entities/          <Noun>Entity.java          — entidad JPA (@Entity), sin dependencias hacia afuera
│   └── irepositories/     I<Noun>Repository.java      — contrato de repositorio (puerto de salida), lenguaje de dominio
├── application/
│   ├── iservices/         I<Noun>Service.java         — contrato de caso de uso (puerto de entrada)
│   ├── services/          <Noun>Service.java          — implementación del caso de uso
│   └── mappers/           <Noun>Mapper.java           — MapStruct, Request/Entity → Entity/Response
├── infrastructure/
│   └── persistence/
│       ├── jpa/           <Noun>JpaRepository.java    — JpaRepository<Entity, UUID> crudo (Spring Data)
│       └── repositories/  <Noun>Repository.java       — adapta JpaRepository a I<Noun>Repository
└── presentation/
    ├── controllers/       <Noun>Controller.java        — REST, retorna ResponseEntity<ApiResponse<T>>
    ├── requests/<Verbo><Noun>Request/<Verbo><Noun>Request.java   — un archivo+carpeta por DTO, ej. CreateTeamRequest
    └── responses/<Noun>Response/<Noun>Response.java              — un archivo+carpeta por DTO
```

Reglas de dependencia: `domain` no depende de nada hacia afuera; `application` depende de `domain`; `infrastructure`/`presentation` dependen de `application`+`domain`. Nunca exponer la entidad JPA directamente en un controller — siempre pasar por Request/Response + Mapper.

### Piezas que viven fuera de las 4 carpetas básicas (ya existen: respétalas, no inventes otras)

- `domain/exception/<Noun>Error.java`: `enum` con los códigos de error que los clientes reciben como motivo (`EventFormatError`, `CategoryError`, `TeamError`, `PasswordChangeError`). El nombre de la constante es el código estable; los frontends lo traducen.
- `domain/enums/<Noun>Status.java`: `enum` de dominio cuando la columna sigue siendo texto libre (solo `teams`: `TeamStatus`, con `isValid(String)` para validar lo que manda el cliente).
- `infrastructure/seed/`: `CommandLineRunner` de datos iniciales (`menu`, `users`); deben ser idempotentes.
- `modules/users/infrastructure/configuration/security/`: `SecurityConfig`, `UserDetailsServiceImpl`, `AuthenticatedUser` y `AuditorAwareImpl` (ver deuda técnica #7).
- Excepción: `ClientType` vive en `presentation/requests/LoginRequest/`, junto al request de login.
- Carpetas vacías (`teams/{application/dtos, domain/policies, infrastructure/configuration, infrastructure/repositories, presentation/middlewares}` y `users/{application/dtos, presentation/middlewares}`): existen solo en disco local porque git no versiona carpetas vacías. No forman parte del patrón; no las repliques en un módulo nuevo.

### Convención de nombres (seguir exacto)

- Interfaz de servicio: `I<Noun>Service` en carpeta `iservices`. Implementación: `<Noun>Service` en `services`.
- Interfaz de repositorio (dominio): `I<Noun>Repository` en carpeta `irepositories`. Adaptador: `<Noun>Repository` en `infrastructure/persistence/repositories`. Repositorio Spring Data crudo: `<Noun>JpaRepository` en `infrastructure/persistence/jpa`.
- Entidad: `<Noun>Entity`, `@Entity @Table(name="snake_case_plural")`, extiende `BaseEntity`/`BaseTranslatableEntity` (de `shared`) para heredar id UUID (`@UuidGenerator`) y auditoría (`createdBy/At`, `updatedBy/At`, soft delete vía `deletedBy/At`).
- Mapper: `<Noun>Mapper`.
- DTOs: `Create<Noun>Request`, `Update<Noun>Request`, `<Noun>Response` — cada uno en su propia carpeta homónima.
- **Singular o plural (es como está en el código, no lo "normalices"):** van en **plural** la entidad, el servicio y su interfaz, el controller y el `JpaRepository` (`TeamsEntity`, `ITeamsService`, `TeamsService`, `TeamsController`, `TeamsJpaRepository`); van en **singular** el repositorio de dominio y su adaptador (`ITeamRepository`, `TeamRepository`), el mapper (`TeamMapper`) y los DTO (`CreateTeamRequest`, `TeamResponse`). Los recursos de unión (`TeamsEvents`, `EventsCategories`, `UsersRoles`, `RolesMenu`) llevan el plural en todas sus piezas.
- Enums de códigos de error: `<Noun>Error`; enums de estado: `<Noun>Status`.

## Módulo `shared` (`com.platform.backend.shared`)

Código transversal sin lógica de negocio de un módulo específico:

- `domain/entities` (`BaseEntity`, `BaseTranslatableEntity`), `domain/interfaces` (`HasUniqueField`) y `domain/utils` (`SoftDeleteHelper`).
- `presentation/responses`: `ApiResponse<T>`, el wrapper de respuesta estándar `{success, status, message, data, timestamp}`.
- Bajo `infraestructure/`: `exceptions` (`GlobalException` `@RestControllerAdvice`, `FieldValidationException`), `security/jwt` (`JwtService`, `JwtAuthenticationFilter`, `JwtAuthenticationEntryPoint`), `security/password` (`PasswordPolicy`, `TemporaryPasswordGenerator`), `ratelimit` (`LoginRateLimiterFilter`), `email` (`EmailService`), `config` (`JacksonConfig`, `JpaAuditConfig`, `SwaggerConfig`) y `audit` (`ApplicationContextProvider`).

**Nota de inconsistencia existente (no "corregirla" sola, es así en el repo real):** dentro de cada módulo la capa se llama `infrastructure` (inglés), pero dentro de `shared` se llama `infraestructure` (español). Al crear algo nuevo en `shared`, respeta el nombre `infraestructure` que ya existe ahí; en un módulo de negocio nuevo, usa `infrastructure`.

## API: contrato de respuesta y errores

- Todo controller retorna `ResponseEntity<ApiResponse<T>>`, nunca la entidad ni un objeto suelto. Usar los factory methods existentes de `ApiResponse` (`ok/created/deleted/error`).
- Validación y errores de negocio se manejan centralizado en `GlobalException` (`@RestControllerAdvice`) — no capturar `Exception` genérico dentro de un controller/service si el `GlobalException` ya lo cubre; en su lugar, lanzar la excepción específica.
- Cómo rechazar una petición por una regla de negocio (patrón de `teams`, `categories` y `event-formats`): un `enum <Noun>Error` en `domain/exception/` y `FieldValidationException(campo, CÓDIGO)` cuando el error es atribuible a un campo (400 con `data: {campo: CÓDIGO}`), o `IllegalStateException(CÓDIGO)` cuando no lo es (400 con el código en `message`, p. ej. `IN_USE`). Nunca uses `BadCredentialsException` ni nada que termine en 401 para un error de formulario: los frontends cierran la sesión ante cualquier 401.
- El frontend Angular espera exactamente esta forma de `ApiResponse` y el campo `admin` (boolean) en la respuesta de login — no cambiar esa forma sin avisar, rompe el contrato con `administration-angular`.

## Auth / rol admin

- El flag `admin` **no** se determina por nombre de rol (los nombres son editables/traducibles). Se determina viendo si el usuario tiene una fila activa en `users_roles` apuntando al UUID fijo configurado en `app.security.admin-role-id`. Seguir ese mismo mecanismo para cualquier chequeo de permisos nuevo, no comparar por string de nombre de rol.

## Migraciones (Flyway)

Seguir el flujo documentado en `src/main/resources/db/migration/README.md`: correr con perfil `generate` para que Hibernate genere `generated-schema.sql`, editarlo a mano (agregar `IF NOT EXISTS`, índices únicos, etc.) y guardarlo como `V{n}__descripcion.sql`, borrando el archivo generado. Nunca editar una migración `V{n}` ya existente/aplicada.

## Testing

- JUnit 5 + Mockito + AssertJ. Tienen tests reales (no boilerplate) `users`, `events`, `teams` (solo `TeamsService`), `matches` (solo `StandingsService`) y la parte de seguridad de `shared` (JWT y generador de contraseñas). Sin tests: `players`, `courts`, `sportcomplexes`, `menu`, el calendario automático, los estados de partido, el rate limiter y `EmailService` (detalle en "Tests (complementa la sección Testing)").
- Si agregas lógica de negocio nueva en `application/services`, sigue el mismo estilo de test que `UsersServiceTest` (mockear repositorio/mapper, `MockitoExtension`) cuando se pida agregar tests.

---

# Contexto de la plataforma

> Sección añadida el 2026-10-12 tras analizar los 3 proyectos y la carpeta `Documentación/` del monorepo. Está verificada contra el **código**, no contra los documentos de diseño. Las partes propias del backend (módulos, endpoints, enums, seeds, tests, estructura de carpetas) se revisaron de nuevo contra el código el 2026-10-17. **Si algo de aquí contradice una sección anterior de este archivo, prevalece esto**: varias secciones previas quedaron desactualizadas (se señalan en "Estado real" de cada proyecto). El bloque "Visión de la plataforma" es idéntico en los 3 CLAUDE.md: si lo cambias, cámbialo en los 3.

## Visión de la plataforma (bloque común a los 3 proyectos)

### Qué es y para quién

- Plataforma **a medida para gestionar campeonatos deportivos** (ligas, torneos): varios campeonatos y eventos a la vez, categorías/divisiones y proyección a nivel nacional. El partido se modela con goles, asistencias, tarjetas y faltas.
- Dos tipos de usuario, una app para cada uno:
  - **Organizador / administrador** → panel web Angular, solo para administradores. Gestiona usuarios, categorías, equipos, jugadores, eventos, canchas, partidos, resultados, reportes y configuración.
  - **Usuario final (jugador, capitán)** → app Flutter. Perfil, equipos, partidos, eventos y ligas, tablas de posiciones, canchas, estadísticas y reportes de su equipo.
- Idioma por defecto: español. Ambos frontends traducen además a `en`, `fr` y `de`.

### Las 3 piezas

| Pieza | Carpeta | Stack | Rol |
|---|---|---|---|
| Backend | `backend` | Java 25, Spring Boot 4, PostgreSQL | API REST única para los dos frontends (`http://localhost:8080/api`, Swagger en `/api/swagger-ui.html`) |
| Admin web | `frontend/administration-angular` | Angular 21 + CoreUI | Panel de organizadores |
| App usuario | `frontend/usuario-flutter` | Flutter | App de jugadores (en la práctica Android, web y Windows) |

Los 3 son repos git independientes. La carpeta `Documentación/` (en la raíz del monorepo, fuera de los repos) guarda la propuesta y el plan de trabajo acordados con el cliente, `documentacion_tecnica.docx` y dumps SQL antiguos.

- **El documento técnico es un borrador (v1.0, abril 2026) y describe una intención, no la realidad**: habla de Java 21 / Spring Boot 3, base `/api/v1`, JWT con refresh token, BLoC/Riverpod en Flutter, roles PLAYER/CAPTAIN/TEAM_ADMIN/SYSTEM_ADMIN con matriz de permisos y módulos de auditoría, rankings, reportes y configuración. Nada de eso existe tal cual en el código. Úsalo para entender qué quiere el negocio, nunca como contrato técnico.
- El plan acordado (24 ago – 20 sep 2026) ordena la entrega: 1) acceso de administradores y usuarios, 2) categorías, equipos y jugadores, 3) eventos y canchas, 4) partidos, resultados, dashboard, reportes y configuración.

### Estado de implementación (oct 2026)

| Dominio | Backend | Admin (Angular) | App usuario (Flutter) |
|---|---|---|---|
| Auth, usuarios y roles | ✅ | ✅ lista, alta con contraseña temporal por email, edición, borrado y asignación de roles; el administrador ve su perfil y cambia su contraseña | Login, registro y completar perfil |
| Menú dinámico por rol | ✅ | ✅ sidebar | ✅ Home (barra inferior y acciones rápidas) |
| Eventos y ligas | ✅ | ✅ CRUD de eventos (lista con búsqueda y filtros, alta, edición y borrado lógico) y, desde cada fila, los equipos inscritos (agregar y quitar) y los partidos (ver "Partidos"); sin calendario automático | ✅ **único módulo maduro** (`leagues`) |
| Equipos, jugadores e invitaciones | ✅ | ✅ CRUD de equipos (lista con búsqueda y filtros, alta, edición, estado y borrado lógico) y jugadores de cada equipo: lista, alta por email (lo agrega o lo traslada al equipo) e invitación de registro por correo si el email no existe; sin edición ni baja de jugadores | ✅ dentro de ligas |
| Partidos, calendario automático y posiciones | ✅ | ✅ parcial, dentro de cada evento: lista, alta, edición (estado y marcador), aplazar, suspender, reprogramar y borrado lógico; **calendario automático** (generar y regenerar, omitiendo los feriados) y aviso de los partidos que caen en un feriado; sin posiciones ni detalle del partido | ✅ dentro de ligas |
| Canchas y complejos | ✅ CRUD | ✅ CRUD de canchas (lista con búsqueda y filtro por complejo, alta, edición y borrado lógico) más el selector del formulario de partidos; los complejos solo se leen y no tienen pantalla | solo lectura (selector al programar) |
| Categorías | ✅ CRUD | ✅ CRUD (lista con búsqueda, alta, edición y borrado lógico; solo si ningún equipo ni evento la usa) | ❌ |
| Descansos y feriados (días sin juego) | ✅ CRUD (`holidays`); cada uno dura uno o varios días y el calendario automático los omite | ✅ CRUD (pantalla «Descansos/Feriados»: lista con búsqueda y filtro, alta, edición y borrado lógico; dura uno o varios días y puede repetirse cada año) | ❌ |
| Rankings, estadísticas, reportes, auditoría, configuración global | ❌ no existen (solo el CRUD crudo de eventos de partido) | ❌ | placeholders "Próximamente" |
| Dashboard | n/a | demo de la plantilla con datos falsos | Home con tarjetas vacías |

### Contrato común backend ↔ frontends (no romper sin avisar)

- Toda respuesta va en `ApiResponse<T> = {success, status, message, data, timestamp}`. Un 400 de validación trae en `data` un mapa `campo → motivo`. Los dos frontends desenvuelven `data` a mano: no hay cliente generado ni librería compartida, así que **un cambio de endpoint o DTO hay que reflejarlo a mano en el servicio de Angular y en el de Flutter**.
- Base `/api` (context-path). Autenticación: `Authorization: Bearer <JWT>`. El JWT dura 24 h y **no hay refresh**. Un token ausente, vencido o inválido en un endpoint protegido responde **401** con `ApiResponse` (antes 403 o un error 500); los dos frontends lo usan para cerrar la sesión (Angular en `authInterceptor`, Flutter en `UnauthorizedInterceptor`).
- **Login**: `POST /users/auth/login` con `email`, `password` y `clientType`. `ADMIN_PANEL` solo admite administradores y `MOBILE_APP` rechaza administradores; si no corresponde, el backend responde 403. Es el único control de "quién entra a qué app".
- **La API no autoriza por rol**: solo exige un JWT válido. Que un endpoint sea "de admin" es hoy una convención de qué app lo llama, no una protección. No asumas que el backend bloquea a un usuario de la app móvil.
- **Admin** = el usuario tiene una fila activa en `users_roles` con el UUID de `app.security.admin-role-id` (backend). Angular repite ese UUID en `environment*.ts` (`adminRoleId`) y **los dos deben coincidir**. Los nombres de rol son editables: nunca comparar por nombre.
- **Menú dinámico**: la navegación de ambos frontends sale de la tabla `menu` del backend. Cada ítem trae `url`, `icon` y `translationKey`. Ambos frontends llaman `GET /menu/for-current-user` (filtrado por los roles del usuario). **Un usuario sin rol, o con un rol sin filas visibles en `roles_menu`, recibe el menú vacío**: hoy el registro no asigna ningún rol, así que a los usuarios de la app hay que darles uno con sus ítems de menú. Para agregar una pantalla: (1) ruta/página en el frontend, (2) ítem en `menu` con esa misma `url`, (3) claves de traducción. Si el frontend no conoce la `url`, el ítem cae a un fallback (Angular redirige a `/dashboard`; Flutter muestra un placeholder).
- **i18n**: claves `modulo.snake_case`, resueltas como `translationKey ?? name`. El `TranslationService` de Angular está calcado de `easy_localization` de Flutter.
- **Invitaciones a equipos**: el backend envía un correo con `<app.frontend.url>/register?inviteToken=<token>`, que abre el registro por invitación de la app Flutter (deep link; en dev `app.frontend.url` apunta a Flutter web en el puerto 5021).

## Contexto de este proyecto: backend (estado real)

### Módulos y modelo de datos

Los 10 módulos tienen las 4 capas con código real (las carpetas vacías de `teams` y `users` solo existen en disco local; ver "Piezas que viven fuera de las 4 carpetas básicas"). Hay 149 endpoints (144 contados el 2026-10-17 sobre las anotaciones de mapping de los controllers, más los 5 de `holidays`) y 23 entidades; **Swagger (`/api/swagger-ui.html`) es la fuente de verdad del listado completo**, no lo repitas aquí.

| Módulo | Para qué | Tablas clave |
|---|---|---|
| `users` | Usuarios (admin y app), auth, roles | `users` (password BCrypt, `username`, `email`), `roles`, `users_roles` |
| `players` | Jugador con un único equipo e invitaciones por email | `players` (propio `email` único y `password_hash`, `id_team`), `team_invitations` |
| `teams` | Equipos e inscripción a eventos | `teams` (`name` único), `teams_events` |
| `events` | Eventos, tipos (liga, torneo…), formatos de juego y categorías | `events`, `events_types`, `events_formats`, `categories`, `events_categories` |
| `matches` | Partidos, calendario automático, posiciones | `matches`, `match_details`, `match_events`, `actions`, `event_schedule_config` |
| `courts`, `sportcomplexes` | Canchas dentro de complejos | `courts` → `sport_complexes` |
| `holidays` | Descansos y feriados (días sin juego, de uno o varios días), que el calendario automático omite | `holidays` |
| `menu` | Menú jerárquico por rol | `menu`, `roles_menu` |
| `permissions` | CRUD de permisos por rol, **sin uso real** (nada los lee) | `permissions` |

- Todas las entidades extienden `BaseEntity` (`BaseTranslatableEntity` también la extiende). Diez extienden `BaseTranslatableEntity` (`actions`, `categories`, `courts`, `events`, `events_formats`, `events_types`, `menu`, `permissions`, `roles`, `sport_complexes`), pero **solo `menu` expone y escribe** las claves de traducción.
- Las relaciones de `matches` (equipos, evento, cancha), `team_invitations.teamId` y `menu.idParentMenu` son **UUID planos sin FK ni validación**: el backend no comprueba que existan. Si lo necesitas, valídalo en el service (como hace `TeamsService` con `teams.categoryId`, que también es un UUID plano).
- Estados y tipos son `String` libres (`"ACTIVE"`, estados de partido e invitación, `status`). Los enums del backend son: `ClientType {ADMIN_PANEL, MOBILE_APP}`, `TeamStatus {ACTIVE, INACTIVE}` (valida el `status` de un equipo, aunque la columna sigue siendo texto) y los códigos de error que reciben los clientes: `EventFormatError {NAME_ALREADY_EXISTS, IN_USE}`, `CategoryError {NAME_ALREADY_EXISTS, IN_USE}`, `TeamError {NAME_ALREADY_EXISTS, CATEGORY_NOT_FOUND, INVALID_STATUS}` y `PasswordChangeError {INCORRECT_CURRENT_PASSWORD, NEW_PASSWORD_SAME_AS_CURRENT}`. Para eventos, los dos frontends usan `status` ∈ `UPCOMING`/`ACTIVE`/`FINISHED`/`CANCELLED` (el backend no lo valida y puede traer otro texto o `null`). El **formato** del evento ya no es texto libre: es una referencia opcional al catálogo `events_formats` (`events.id_event_format`, `idEventFormat` en los requests); la respuesta del evento trae `eventFormat` (objeto) y además `format` (solo el nombre) para la app Flutter, que lo lee como texto.

### API

- Patrón CRUD estándar por recurso: `GET /x`, `GET /x/{id}`, `POST /x` (201), `PUT /x/{id}`, `DELETE /x/{id}` (soft delete) y `DELETE /x/{id}/hard` (físico). Sin `PUT`: `/users-roles`, `/events-categories`, `/teams-events`. `/event-formats` no tiene `/hard`.
- **Públicos** (sin JWT): `POST /users/auth/login`, `POST /users/auth/register`, `POST /players/auth/register` y Swagger. Todo lo demás exige JWT válido, sin más.
- Endpoints propios que usan los frontends hoy:
  - Usuarios: `PUT /users/auth/update-profile`, `PUT /users/auth/change-password` (usuario autenticado cambia su propia contraseña; hoy solo lo usa Angular), `GET /users/admins`, `GET /users/regular`, `POST /users` (crea con contraseña temporal por email), `GET /users-roles/by-user/{userId}`.
  - Menú: `GET /menu` (árbol completo) y `GET /menu/for-current-user` (filtrado por roles del usuario).
  - Eventos: `GET|POST|PUT|DELETE /event-formats` (catálogo de formatos; `GET` lo usa el selector del formulario de eventos), `GET /events/by-event-type/{typeId}` y `.../by-user/{userId}` (filtra por `createdBy`), `GET /event-types/{id}/events`, `GET|POST /events/{id}/schedule-config`, `POST /events/{id}/schedule-config/generate`, `GET /events/{id}/standings`.
  - Partidos: `GET /matches/by-event/{eventId}`, `PATCH /matches/{id}/postpone|suspend|reschedule`, `PATCH /matches/by-event/{eventId}/reschedule-date`.
  - Equipos y jugadores: `GET /teams-events/by-event/{eventId}`, `GET /players/by-team/{teamId}`, `GET /players/search?email=`, `POST /players/invite`, `POST /players/invite-register`, `PATCH /players/{id}/remove-from-team`, `PATCH /players/{id}/status` (solo `ACTIVE`/`INACTIVE`).
- Errores (`GlobalException`): 400 validación (mapa en `data`), 400 `IllegalArgument`/`IllegalState`, 401 credenciales, 403 acceso denegado, 404, 409 genérico ante `DataIntegrityViolation` (se pierde el motivo concreto) y 500 con `ex.getMessage()` para el resto.

### Reglas de negocio no obvias

- **Formatos de evento** (`EventFormatsService`): el nombre es único entre los activos **sin distinguir mayúsculas** y se guarda sin espacios sobrantes; repetirlo es un 400 `data: {name: "NAME_ALREADY_EXISTS"}` (`FieldValidationException`, no el 409 genérico). Borrar un formato que algún evento activo usa es un 400 con `message: "IN_USE"`. Un `PUT` de evento con `idEventFormat: null` deja el formato como está (no se puede quitar uno ya asignado); `POST /events` sin formato es válido (la app Flutter no lo envía).
- **Equipos** (`TeamsService`): igual que los formatos, el nombre es único entre los activos sin distinguir mayúsculas y se guarda sin espacios sobrantes (400 `data: {name: "NAME_ALREADY_EXISTS"}`); un `categoryId` que no existe es un 400 `data: {categoryId: "CATEGORY_NOT_FOUND"}` y un `status` fuera de `TeamStatus` (`ACTIVE`/`INACTIVE`) es un 400 `data: {status: "INVALID_STATUS"}`. `POST /teams` siempre crea el equipo `ACTIVE` (no admite `status`; la app Flutter solo envía `name` y `logoUrl`). Un `logoUrl` en blanco se guarda como `null`: en el `PUT`, `""` quita el logo y `null` lo deja igual (tampoco se puede quitar una categoría ya asignada). Nada lee el `status` de un equipo: uno `INACTIVE` sigue apareciendo en la app y en las inscripciones. Borrar un equipo **no revisa ni toca** sus inscripciones (`teams_events`) ni sus jugadores.
- **Categorías** (`CategoriesService`): mismas reglas de nombre que los formatos (único entre los activos sin distinguir mayúsculas, sin espacios sobrantes, 400 `data: {name: "NAME_ALREADY_EXISTS"}`; antes era el 409 genérico). Tienen `description` opcional (`CreateCategoryRequest`, `UpdateCategoryRequest` y `CategoryResponse`; el update ignora los `null`, así que se vacía con `""`). Borrar una categoría que algún equipo activo (`teams.category_id`, UUID plano) o evento activo (`events_categories`) usa es un 400 con `message: "IN_USE"`. `DELETE /categories/{id}/hard` se conserva (físico, sin esa comprobación: lo frena la FK de `events_categories`, no la de equipos). `CategoriesService` consulta `ITeamRepository`, así que `events` ya depende de `teams` además de `teams` de `events` (sin ciclo de beans).
- **Feriados** (`HolidaysService`, módulo `holidays`): un día, o varios seguidos, en los que no se programan partidos (feriado oficial, vacaciones o cualquier descanso que el administrador decida). Campos: `name` (≤ 100), `date` (primer día), `endDate` (último día, opcional) y `recurring` (se repite cada año desde el mismo mes y día; `HolidaysEntity.appliesOn`/`occurrencesBetween`; un 29 de febrero recurrente cae el 28 en los años sin 29). `endDate` sin enviar al crear es un solo día, y la respuesta lo trae siempre (igual a `date` si dura un día): las filas anteriores a los rangos tienen la columna `holiday_end_date` en `null` y se leen así (`getEndDate()`). Un `endDate` anterior a `date` es un 400 `data: {endDate: "END_DATE_BEFORE_DATE"}` y uno que dura más de 365 días tras el inicio (`MAX_DAYS_AFTER_START`) un 400 `data: {endDate: "RANGE_TOO_LONG"}`. Un descanso recurrente **conserva su duración** en cada año (puede empezar en diciembre y acabar en enero: `occurrencesBetween` mira también el año anterior). Dos feriados no pueden compartir ningún día (`clashesWith` compara los días de ambos, cuatro años por delante si alguno es recurrente por los 29 de febrero): un **recurrente** cubre sus días en todos los años, así que registrar uno de una sola vez en otro año de ese día (o al revés) es un 400 `data: {date: "DATE_ALREADY_EXISTS"}` (`HolidayError`, `FieldValidationException`). El update toma de lo guardado los campos que no se envían para esas comprobaciones y ignora los `null`; si mueve `date` sin enviar `endDate`, el descanso conserva su duración (`keepDurationWhenMoved`), y para volver a un solo día se envía `endDate` igual a `date`. Borrado lógico, sin `/hard`; **borrar o crear un feriado no mueve los partidos ya programados**: el calendario se recalcula al volver a generar. `IHolidaysService.getNonPlayDatesBetween(from, to)` expande los feriados (anuales incluidos) a días concretos y es lo que consume el calendario automático. Sin migración: Hibernate crea la tabla `holidays` (`holiday_date`, `holiday_end_date`, `name`, `recurring`) y añade la columna nueva al reiniciar (Flyway está desactivado en todos los perfiles).
- **Soft delete**: `deletedAt/By` y todas las consultas filtran `...DeletedAtIsNull`. Solo `EventsService.delete`, `EventFormatsService.delete`, `TeamsService.delete` y `CategoriesService.delete` liberan los campos únicos (`SoftDeleteHelper`; el nombre de un equipo o una categoría borrado queda como `<nombre>_DELETED_<id>`, recortado para caber en la columna). En `players` borrar y recrear con el mismo email falla con 409 por el unique de BD, y en `teams` también con los equipos borrados **antes** de este cambio, que conservan su nombre original.
- **Crear usuario** (`UsersManagementService.createUser`, transaccional): valida email y username únicos, genera una contraseña temporal (`app.security.temporary-password-length`, mínimo 8) y la envía por correo; **si el correo falla, hace rollback**. Esa contraseña se cambia con `PUT /users/auth/change-password` (`UsersAuthService.changePassword`): exige la actual y que la nueva (8–72 caracteres, `PasswordPolicy`) sea distinta. Un rechazo sale como **400 con `data: {campo: CÓDIGO}`** (`FieldValidationException`, códigos en `PasswordChangeError`), nunca como 401, porque los frontends cierran sesión ante cualquier 401. No existe marca de "debe cambiarla en el primer login".
- **Login**: email normalizado (trim y minúsculas), `clientType` obligatorio y flag `admin` por `app.security.admin-role-id` (vacío = nadie es admin).
- **Invitaciones**: `EXISTING_USER` (sin token; correo informativo) y `NEW_USER` (token UUID y enlace de registro). Expiran a los `app.invite.expiration-days` (7). Solo se usan los estados `PENDING` y `USED`; la expiración se calcula por fecha.
- **Calendario automático** (`POST /events/{id}/schedule-config/generate`, `EventScheduleConfigService`): round-robin de **una sola vuelta**, exige ≥ 2 equipos inscritos; cada partido va al primer día de `playDays` (en inglés, separados por coma) que no esté bloqueado (ni por `blockedDates` del evento ni por un **feriado** de `holidays`) y en el que ninguno de sus dos equipos juegue ya; slots consecutivos (duración + descanso entre partidos) y, si un día no tiene lugar para otro partido antes de la medianoche, el partido pasa al siguiente día de juego (antes la hora daba la vuelta a las 00:00). Empieza en `max(startDate del evento, hoy)`: nunca programa en el pasado. **Se puede repetir**: reemplaza (soft delete) los partidos pendientes del evento (`MatchStatus.isPending`: `SCHEDULED`/`POSTPONED`/`SUSPENDED`/`RESCHEDULED` **y sin marcador**), conserva los demás (jugados, en juego, cancelados o con marcador) y no vuelve a programar sus cruces (la pareja cuenta sin importar quién era local); esos partidos conservados también ocupan su día. Antes de leer los partidos existentes llama a `IMatchesService.finishEndedMatches(eventId)`, así un partido cuyo tiempo ya terminó cuenta como jugado (se conserva) aunque el job aún no lo haya marcado. Es transaccional: si falla, no se borra nada. Devuelve solo los partidos creados y `warning` (frase en inglés, sin código) si algo queda después de `endDate`. Los errores siguen siendo frases en inglés (`IllegalStateException`): la app Flutter reconoce "no cabe" por el texto (`play days`/`date range`), así que no las cambies por códigos sin avisar. `breakBetweenHalvesMinutes` se guarda pero no se usa.
- **Estados de partido** (String): el enum `MatchStatus` (`matches/domain/enums`) nombra los 8 valores que usan el backend y la app (`SCHEDULED`, `IN_PROGRESS`, `FINISHED`, `COMPLETED`, `CANCELLED`, `POSTPONED`, `SUSPENDED`, `RESCHEDULED`) y define cuáles están "pendientes" (`isPending`); la columna sigue siendo texto libre y `MatchesService` aún usa literales. `postpone` falla en COMPLETED/SUSPENDED, `suspend` en COMPLETED, y `reschedule` solo acepta POSTPONED/SUSPENDED.
- **Fin de un partido y "Finalizado" automático** (`MatchTimingService`, `MatchesService`, `MatchAutoFinishJob`): un partido termina cuando pasan su hora + la **duración + el descanso entre partidos** de la configuración de su evento (`event_schedule_config`), el mismo hueco que reserva el calendario: uno de 60 min con 15 de descanso a las 10:00 termina a las **11:15** y desde ese minuto exacto está terminado. Sin configuración (partidos creados a mano) o sin evento se usan `app.matches.default-duration-minutes` (60) y `app.matches.default-break-minutes` (15). Todo `MatchResponse` trae `endsAt`. (1) **Estado automático**: el job `MatchAutoFinishJob` (`@Scheduled`, cada `app.matches.auto-finish-interval-ms` = 60 000 ms, y una vez al arrancar para ponerse al día) pasa a `FINISHED` los partidos `SCHEDULED`/`RESCHEDULED`/`IN_PROGRESS` ya terminados (`MatchStatus.canFinishAutomatically`); un aplazado, suspendido o cancelado no se toca. `@EnableScheduling` está en `SchedulingConfig`. (2) **El marcador solo se registra cuando termina el partido**: un `PUT /matches/{id}` con `homeScore` o `awayScore` antes de `endsAt` (con la fecha ya actualizada por ese mismo `PUT`) es un 400 `data: {homeScore|awayScore: "RESULT_BEFORE_MATCH_END"}` (`MatchError`, `FieldValidationException`); registrarlo después, sin enviar `status`, deja el partido en `FINISHED` (un `status` enviado explícitamente se respeta, y uno aplazado/suspendido/cancelado conserva el suyo). (3) **Una vez iniciado (pasada su hora) sus datos quedan fijos**: un `PUT /matches/{id}` que cambie `homeTeamId`, `awayTeamId`, `matchDate`, `location`, `courtId` o `eventId` es un 400 `data: {campo: "MATCH_ALREADY_STARTED"}` (`MatchesService.validateDataIsStillEditable`, antes de que el mapper pise lo guardado); un valor enviado **igual** al guardado no cuenta como cambio (la app Flutter manda todos los campos en cada guardado), así que solo `status` y el marcador pueden cambiar. Un partido `POSTPONED` nunca se jugó: sus datos siguen editables hasta que se reprograme (`reschedule` mueve la fecha de uno aplazado o suspendido). **Cambia lo que ve la app Flutter**: su formulario de partido puede guardar un marcador antes del fin o cambiar datos de un partido iniciado y ahora recibe esos 400; además `endsAt` es un campo nuevo que ignora.
- **Reprogramación masiva** (`PATCH /matches/by-event/{eventId}/reschedule-date`, `MatchesService.rescheduleDate`): toma todos los partidos activos del evento en `fromDate`. Con `toDate` los mueve a ese día **conservando la hora** y los `POSTPONED` vuelven a `SCHEDULED`; con `toDate` en `null` los marca `POSTPONED`. A diferencia de `postpone`/`suspend`, **no valida el estado de cada partido** (puede dejar un `COMPLETED` en `POSTPONED`) y responde 404 si ese día no tiene partidos.
- **Posiciones**: solo para eventos cuyo tipo se llama `"liga"` (texto fijo `LEAGUE_EVENT_TYPE_NAME`, sin distinguir mayúsculas). 3/1/0 puntos y cuenta todo partido con ambos marcadores no nulos, sin importar su estado. Orden: puntos, diferencia de goles y goles a favor, los tres descendentes (`STANDINGS_ORDER`).
- **Menú**: `GET /menu/for-current-user` toma los roles activos del usuario, filtra `roles_menu.visible` y arma el árbol con `idParentMenu`; un hijo cuyo padre no es visible sube a la raíz. Solo `visible` se aplica: los flags `can_*` se guardan pero no se usan.
- **Seeds** (sin `@Order`; ambos son `CommandLineRunner` idempotentes): `MenuSeedRunner` corre en **todos** los perfiles (incluido `prd`) y siembra para el rol admin "Dashboard" (`/dashboard`, `cilSpeedometer`, orden 0), "Usuarios" (`/users`), "Eventos" (`/events`), "Formatos de evento" (`/event-formats`), "Equipos" (`/teams`), "Categorías" (`/categories`, `cilTags`), "Canchas" (`/courts`, `cilLocationPin`) y "Feriados" (`/holidays`, `cilFlagAlt`). Cada ítem se busca por su nombre: uno ya existente llamado igual, p. ej. creado a mano para la app Flutter, se reutiliza y entonces también lo ven sus otros roles. `UsersSeedRunner` corre en todos menos `prd` y crea el rol admin (con `JdbcTemplate`, porque el `@UuidGenerator` de Hibernate sobrescribe cualquier id asignado y aquí debe coincidir con `app.security.admin-role-id`) y los usuarios demo `admin@algo.com` y `usuario@algo.com`. **Consecuencia en `prd`:** nadie crea el rol admin y `MenuSeedRunner` sale sin sembrar mientras ese rol no exista; hay que insertarlo a mano con el UUID configurado y reiniciar.

### Seguridad: estado real (no "arreglar" sin que se pida, pero tenerlo presente)

- JWT stateless (jjwt 0.12.6, HMAC). Claims: `id`, `firstName`, `middleName`, `lastName`, `secondLastName`, `admin`, `clientType`; subject = email.
- **No hay autorización por rol**: ni `@PreAuthorize` ni `@EnableMethodSecurity`, y `JwtAuthenticationFilter` asigna siempre `ROLE_USER` (el claim `admin` no se usa para autorizar). Cualquier usuario autenticado, también uno de la app móvil, puede crear o borrar usuarios, asignarse el rol admin con `POST /users-roles` o editar menús y roles.
- `LoginRateLimiterFilter`: 5 intentos/min por IP, fijo en código (bucket4j en memoria; toma la IP de `X-Forwarded-For` o `X-Real-IP`, así que es falsificable). Responde 429 con un JSON propio, no `ApiResponse`.
- CORS abierto (`*` con credenciales) y Swagger público en todos los perfiles.
- Un token vencido, mal formado o con firma inválida no autentica la petición (el filtro lo ignora) y un endpoint protegido responde entonces **401** con `ApiResponse` (`JwtAuthenticationEntryPoint`). Antes respondía 403 sin token y reventaba el filtro con uno vencido. Los endpoints públicos siguen funcionando aunque llegue un token viejo. Cubierto por `JwtSecurityChainTest`.

### Base de datos y migraciones: estado real (corrige la sección "Migraciones")

- **Flyway está desactivado** (`spring.flyway.enabled=false` en dev, local y prd) y `ddl-auto=update`: hoy **el esquema lo crea y altera Hibernate al arrancar**. `db/migration/README.md` dice lo contrario y está desactualizado.
- `V7__create_events_formats_and_link_to_events.sql` (formatos de evento) tampoco se ejecuta solo, pero Hibernate ya crea `events_formats` y `events.id_event_format` al arrancar. Lo que **no** hace Hibernate: pasar los textos que `events.format` ya tenía al catálogo y quitar esa columna. Hay que correr ese script a mano una vez (es idempotente y también sirve si se reactiva Flyway); mientras `events.format` siga ahí, la API lo ignora y los eventos viejos aparecen sin formato.
- Las migraciones V1–V6 no reflejan las entidades (faltan, por ejemplo, `events.url_logo`, `min/max_players_per_team`, `min/max_teams` y `event_schedule_config.blocked_dates`), y V3 es destructiva (`DROP TABLE` y recreación de 18 tablas).
- Consecuencia: una columna nueva en una entidad aparece sola en dev. **Antes de crear una `V{n}`, confirma con el usuario si se va a reactivar Flyway**, porque hoy esa migración no se ejecutaría. Si se pide, sigue el flujo del README (perfil `generate`) y no edites V1–V6. La tabla `holidays` (módulo nuevo) tampoco tiene migración: la crea Hibernate.

### Configuración y cómo correr

- Perfil por defecto `dev`, fijo en `application.properties`. `dev`, `local` y `prd` son idénticos entre sí (PostgreSQL en `localhost:5432`, base `sports_platform`). El archivo `SelectEnv` (`$env:ENV`) **no tiene efecto**; para otro perfil usa `SPRING_PROFILES_ACTIVE` o `-Dspring-boot.run.profiles=...`. `generate` escribe `generated-schema.sql` sin tocar la BD.
- Propiedades propias: `app.json.naming-strategy`, `app.frontend.url` (base del enlace de invitación; hoy `http://localhost:5021`), `app.invite.expiration-days`, `app.security.admin-role-id`, `app.security.temporary-password-length`, `app.matches.default-duration-minutes`, `app.matches.default-break-minutes` y `app.matches.auto-finish-interval-ms`. Además `jwt.*` y `spring.mail.*` (SMTP Gmail, 587, STARTTLS).
- **Los `application*.properties` (main y test) están versionados con secretos reales** (`jwt.secret`, BD, SMTP). No los imprimas ni copies sus valores a docs, commits o respuestas. Lo correcto sería pasarlos a variables de entorno (`dotenv-java` está en el `pom.xml` sin usarse).
- Comandos, desde `backend/`: `./mvnw spring-boot:run`, `./mvnw test`, `./mvnw -Dtest=UsersManagementServiceTest test`, `./mvnw clean package`.

### Tests (complementa la sección Testing)

- Hay 23 clases: `EventFormatsServiceTest` y `EventFormatsControllerTest` (24 tests, todos pasan; verificados con `-Dtest` el 2026-10-12), `CategoriesServiceTest` y `CategoriesControllerTest` (27 tests, todos pasan; mismo método y fecha), `TeamsServiceTest` (19 tests, todos pasan; mismo método y fecha), `EventTypesServiceTest`, `EventTypesControllerTest`, `UsersServiceTest` (en realidad prueba `UsersAuthService`), `UsersManagementServiceTest`, `UsersControllerTest`, `TemporaryPasswordGeneratorTest`, `StandingsServiceTest`, `EventScheduleConfigServiceTest` (25 tests del calendario automático: feriados, días bloqueados, no programar en el pasado, regenerar sin duplicar, medianoche), `MatchStatusTest` (19), `MatchTimingServiceTest` (8; incluye el ejemplo 10:00 + 60 + 15 = 11:15), `MatchesServiceTest` (21: marcador antes/después del fin, Finalizado automático, `endsAt` y datos fijos al iniciar), `MatchAutoFinishJobTest` (2), `HolidaysEntityTest` (25), `HolidaysServiceTest` (25) y `HolidaysControllerTest` (11; todos pasan, verificados el 2026-10-17), `JwtAuthenticationFilterTest`, `JwtSecurityChainTest` (levanta la cadena de seguridad real con un controller de mentira; no requiere `spring-security-test`) y `BackendApplicationTests`. Los tests de controller usan `MockMvc` standalone con `GlobalException`.
- **Sin ningún test**: teams (salvo `TeamsService`), players e invitaciones, matches (la reprogramación masiva, `postpone`/`suspend`/`reschedule` y los controllers; hay de posiciones, calendario automático, fin de partido y `MatchStatus`), courts, sportcomplexes, menu, los seeds, rate limiter y `EmailService`.
- Estado verificado con `./mvnw test` el 2026-10-17, en una compilación limpia: **262 tests, 259 pasan y 3 fallan (los mismos que antes de los feriados)****: `BackendApplicationTests.contextLoads` (no hay bean `JavaMailSender`: los `application*.properties` de test no definen `spring.mail.*`) y 2 de `EventTypesControllerTest` (`create_whenMissingRequiredFields_thenReturn400` y `create_whenNameIsBlank_thenReturn400WithFieldError`), que esperan `$.errors` cuando `GlobalException` devuelve los errores en `$.data`.
- **`contextLoads` usa la BD local real:** los `application*.properties` de test apuntan a la misma BD que dev (`sports_platform` en `localhost:5432`, `ddl-auto=update`) y ese test llega a conectarse (se ve en el log) antes de fallar en `EmailService`. Es el único que abre un pool de conexiones; el resto es Mockito o `MockMvc` standalone.
- Si `./mvnw test` o `spring-boot:run` falla con `ClassFormatError: Invalid method Code length 0` (visto el 2026-10-17: 75 clases de `target/classes` con el constructor generado por Lombok vacío; también falla al cargarlas con `java` suelto), borra `target/` y vuelve a compilar (`./mvnw clean test`): una compilación limpia con Maven de las mismas fuentes sí generó clases válidas. Maven no las recompila porque las ve al día; el origen probable es otra herramienta que escribió en `target/classes` (el IDE), pero no está verificado.

### Deuda técnica conocida (no replicar; corregir solo si se pide)

1. Un usuario con soft delete **puede seguir iniciando sesión** (`UsersJpaRepository.findByEmail` no filtra `deletedAt`), y como `existsByEmail` ignora los borrados se puede crear otro usuario con el mismo email.
2. `users` y `players` son identidades separadas y **no existe login para players**: un jugador registrado por invitación (`POST /players/auth/register`) no puede autenticarse.
3. Las invitaciones `EXISTING_USER` no tienen flujo de aceptar o rechazar. El email del registro no se compara con el de la invitación y no se evitan duplicadas.
4. `breakBetweenHalvesMinutes` se guarda pero se ignora, y los errores del calendario automático son frases en inglés sin código (ver "Calendario automático"). No hay endpoint que marque `COMPLETED` (solo `PUT /matches/{id}` con `status` libre) y `MatchesService.create` no valida que equipos, evento o cancha existan.
5. Los DTO de evento no exponen `urlLogo`, `minTeams` ni `maxTeams` aunque la entidad los tiene, y `UpdateEventRequest` no admite `description` (el panel admin la muestra deshabilitada al editar). Además `EventMapper.updateEntity` usa `NullValuePropertyMappingStrategy.IGNORE`: un `null` en el `PUT` no cambia el campo, así que no se puede vaciar un valor numérico (mín/máx de jugadores por equipo) una vez definido; los textos solo se vacían enviando `""` y el formato (`idEventFormat`) tampoco se puede quitar una vez asignado.
6. `JacksonConfig` define un `ObjectMapper` de Jackson 2 que nadie inyecta, mientras Spring Boot 4 usa Jackson 3: es probable que `app.json.naming-strategy` no tenga efecto (por verificar).
7. `shared` importa `AuthenticatedUser` desde `modules/users`, y la configuración de seguridad vive en `users` en vez de `shared`.
8. Los `String` de estado/tipo y las constantes fijas (rate limit, CORS, 5 años de búsqueda del calendario) contradicen la regla "nada hardcodeado": el código nuevo debe usar `enum` y configuración.
9. Los textos de `documentation` y `modules/documentation` describen módulos que no existen (venue, ranking, report, audit).
10. Sin `@Order` en los seeds: en una BD vacía, si `MenuSeedRunner` corre antes de que exista el rol admin, no otorga los ítems hasta el siguiente arranque (verificado el 2026-10-17 en una BD vacía: el primer arranque no siembra el menú y el segundo sí).
11. Doce entidades implementan `HasUniqueField`, pero solo `EventsService`, `EventFormatsService`, `CategoriesService` y `TeamsService` llaman a `SoftDeleteHelper.prepareForDeletion` al borrar. Los servicios de `actions`, `courts`, `event_types`, `menu`, `players`, `roles`, `sport_complexes` y `users` ponen `deletedAt` a mano (varios importan `SoftDeleteHelper` sin usarlo), así que el valor único de un registro borrado sigue ocupado.
12. La lectura de `app.security.admin-role-id` y la comprobación de admin se repiten en `UsersAuthService`, `UserDetailsServiceImpl`, `UsersManagementService` y los dos seeds (cada uno con su `@Value`): extraerla a un único componente antes de añadir un uso más, como piden las reglas de reutilización.
13. Los `application*.properties` de test son copia de los de `main`: apuntan a la BD de dev y `contextLoads` se conecta a ella (ver "Tests").

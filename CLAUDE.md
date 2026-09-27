# Instrucciones para Claude en este proyecto

## Cómo trabajar (siempre)

- Pensar como un **desarrollador senior** y un **diseñador UX/UI senior** al mismo tiempo (aplicado aquí sobre todo al diseño de la API: respuestas claras, errores útiles, contratos consistentes para quien consume el backend).
- El código debe ser **escalable** y **muy fácil de entender para cualquier persona**, no solo para quien lo escribió.
- Los métodos deben ser **reutilizables**: evitar duplicar lógica entre servicios/mappers, extraer a `shared` cuando aplique a 2+ módulos.
- Nombres muy claros sobre lo que hacen, sin miedo a que sean largos: variables, funciones, clases, métodos, archivos.
  - **Excepción:** el nombre del **archivo** debe ser claro pero **no demasiado largo** — nombres de archivo muy largos rompen la subida de cambios (límite de longitud de ruta en Windows). Si un nombre de archivo se vuelve muy largo, acortarlo sin perder claridad.
- **Siempre respetar el orden, flujo, arquitectura y estructura de carpetas ya implementados en el proyecto.** No introducir un patrón distinto (otra forma de organizar capas, otra convención de nombres, otro estilo de respuesta HTTP) sin que se pida explícitamente. Antes de crear algo nuevo, mirar cómo está hecho en un módulo ya maduro (ej. `modules/teams`) y replicar exactamente ese patrón.

## Proyectos relacionados (monorepo lógico "Plataforma deportiva")

- Backend (este proyecto, Spring Boot): `C:\Users\WALDO\Documents\Proyectos\Plataforma deportiva\backend`
- Frontend admin (Angular): `C:\Users\WALDO\Documents\Proyectos\Plataforma deportiva\frontend\administration-angular`
- Frontend usuario (Flutter): `C:\Users\WALDO\Documents\Proyectos\Plataforma deportiva\frontend\usuario-flutter`

## Stack del proyecto

- **Java 25**, **Spring Boot 4.0.6**, Maven (`mvnw`).
- **Lombok** + **MapStruct** (`componentModel="spring"`, `unmappedTargetPolicy/unmappedSourcePolicy = IGNORE`) para mappers entidad↔DTO.
- **PostgreSQL** + **Flyway** para migraciones (nunca editar una migración ya aplicada; siempre una nueva `V{n}__descripcion.sql`).
- **Spring Security** con **JWT** (`jjwt`), stateless, `LoginRateLimiterFilter` (bucket4j) sobre login.
- **springdoc-openapi** (Swagger UI en `/swagger-ui.html`).
- Base path global: `server.servlet.context-path=/api`.

## Arquitectura: Clean Architecture + monolito modular

Documentado ya en el propio repo (léelo, son archivos de texto sin extensión `.md`, fáciles de pasar por alto):
- `src/main/java/com/platform/backend/documentation`
- `src/main/java/com/platform/backend/modules/documentation` ("Informe de Estructura Arquitectónica")
- `src/main/java/com/platform/backend/Principios SOLID`
- `src/main/resources/db/migration/README.md` (flujo de Flyway)

Cada módulo de negocio vive en `com.platform.backend.modules.<modulo>` (plural en minúscula: `teams`, `matches`, `players`, `sportcomplexes`, `users`, `events`, `courts`, `menu`, `permissions`) con exactamente estas 4 capas:

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

### Convención de nombres (seguir exacto)

- Interfaz de servicio: `I<Noun>Service` en carpeta `iservices`. Implementación: `<Noun>Service` en `services`.
- Interfaz de repositorio (dominio): `I<Noun>Repository` en carpeta `irepositories`. Adaptador: `<Noun>Repository` en `infrastructure/persistence/repositories`. Repositorio Spring Data crudo: `<Noun>JpaRepository` en `infrastructure/persistence/jpa`.
- Entidad: `<Noun>Entity`, `@Entity @Table(name="snake_case_plural")`, extiende `BaseEntity`/`BaseTranslatableEntity` (de `shared`) para heredar id UUID (`@UuidGenerator`) y auditoría (`createdBy/At`, `updatedBy/At`, soft delete vía `deletedBy/At`).
- Mapper: `<Noun>Mapper`.
- DTOs: `Create<Noun>Request`, `Update<Noun>Request`, `<Noun>Response` — cada uno en su propia carpeta homónima.

## Módulo `shared` (`com.platform.backend.shared`)

Código transversal sin lógica de negocio de un módulo específico: `BaseEntity`/`BaseTranslatableEntity`, `ApiResponse<T>` (wrapper de respuesta estándar: `{success, status, message, data, timestamp}`), `GlobalException` (`@RestControllerAdvice`), JWT (`JwtAuthenticationFilter`/`JwtService`), `LoginRateLimiterFilter`, `EmailService`, configs (`JacksonConfig`, `JpaAuditConfig`, `SwaggerConfig`).

**Nota de inconsistencia existente (no "corregirla" sola, es así en el repo real):** dentro de cada módulo la capa se llama `infrastructure` (inglés), pero dentro de `shared` se llama `infraestructure` (español). Al crear algo nuevo en `shared`, respeta el nombre `infraestructure` que ya existe ahí; en un módulo de negocio nuevo, usa `infrastructure`.

## API: contrato de respuesta y errores

- Todo controller retorna `ResponseEntity<ApiResponse<T>>`, nunca la entidad ni un objeto suelto. Usar los factory methods existentes de `ApiResponse` (`ok/created/deleted/error`).
- Validación y errores de negocio se manejan centralizado en `GlobalException` (`@RestControllerAdvice`) — no capturar `Exception` genérico dentro de un controller/service si el `GlobalException` ya lo cubre; en su lugar, lanzar la excepción específica.
- El frontend Angular espera exactamente esta forma de `ApiResponse` y el campo `admin` (boolean) en la respuesta de login — no cambiar esa forma sin avisar, rompe el contrato con `administration-angular`.

## Auth / rol admin

- El flag `admin` **no** se determina por nombre de rol (los nombres son editables/traducibles). Se determina viendo si el usuario tiene una fila activa en `users_roles` apuntando al UUID fijo configurado en `app.security.admin-role-id`. Seguir ese mismo mecanismo para cualquier chequeo de permisos nuevo, no comparar por string de nombre de rol.

## Migraciones (Flyway)

Seguir el flujo documentado en `src/main/resources/db/migration/README.md`: correr con perfil `generate` para que Hibernate genere `generated-schema.sql`, editarlo a mano (agregar `IF NOT EXISTS`, índices únicos, etc.) y guardarlo como `V{n}__descripcion.sql`, borrando el archivo generado. Nunca editar una migración `V{n}` ya existente/aplicada.

## Testing

- JUnit 5 + Mockito + AssertJ. Hoy solo `users` y `events` tienen tests reales (no boilerplate). El resto de módulos (`teams`, `matches`, `players`, `courts`, etc.) no tiene tests todavía.
- Si agregas lógica de negocio nueva en `application/services`, sigue el mismo estilo de test que `UsersServiceTest` (mockear repositorio/mapper, `MockitoExtension`) cuando se pida agregar tests.

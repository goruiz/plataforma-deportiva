# Guía técnica del backend

**Proyecto:** `backend` · **Actualizada:** 23 de octubre de 2026  
**Stack verificado:** Java 25, Spring Boot 4.0.6, Maven, PostgreSQL y Flyway.

Esta guía describe el comportamiento que existe en el código y la configuración del repositorio. No convierte documentos antiguos, borradores de producto ni funciones planeadas en contratos. Para los detalles del diseño acordado, las convenciones internas y el inventario ampliado, consulta [`ARCHITECTURE.md`](ARCHITECTURE.md) y [`CLAUDE.md`](CLAUDE.md); si un documento contradice el código, prevalecen los controladores, servicios, DTO, migraciones y configuración vigentes.

## 1. Qué es y qué no es

Este proyecto es el monolito modular que expone la API REST común de la plataforma deportiva. Lo consumen el panel de administración Angular y la aplicación móvil Flutter; la base de datos de aplicación es PostgreSQL. La aplicación arranca en [`BackendApplication.java`](src/main/java/com/platform/backend/BackendApplication.java) y aplica el prefijo HTTP `/api` mediante `server.servlet.context-path`.

La propuesta técnica antigua y algunos archivos de documentación interna describen módulos como reportes, rankings, auditoría, o permisos detallados que no forman parte del backend implementado. El código actual sí contiene CRUD de permisos, pero el resto de la aplicación no los usa para autorizar operaciones. No deduzcas la existencia de funciones productivas a partir de esos documentos: revisa los controladores y Swagger/OpenAPI.

## 2. Arquitectura y módulos

La aplicación es un monolito modular con una variante de Clean Architecture. Cada módulo de negocio se organiza bajo `src/main/java/com/platform/backend/modules/<modulo>/` en cuatro capas:

| Capa | Responsabilidad | Ejemplos de paquetes |
|---|---|---|
| `domain` | Entidades JPA, contratos de repositorio, enums y errores del dominio. Evita depender de Spring fuera de lo necesario para persistencia actual. | `domain/entities`, `domain/irepositories` |
| `application` | Casos de uso, reglas y mapeadores entre modelos/DTO. | `application/iservices`, `application/services`, `application/mappers` |
| `infrastructure` | Adaptadores de persistencia: Spring Data y sus implementaciones de puertos. | `infrastructure/persistence/jpa`, `infrastructure/persistence/repositories` |
| `presentation` | Controladores REST y modelos de entrada/salida. | `presentation/controllers`, `requests`, `responses` |

`domain` no debería depender de las capas exteriores; `application` usa contratos de `domain`; presentación e infraestructura conectan la aplicación con HTTP y PostgreSQL. Los controladores no deben devolver entidades JPA: utilizan Request/Response y mappers MapStruct. Los servicios suelen delimitar operaciones con `@Transactional`.

El patrón de nombres real incluye algunas formas históricas no uniformes: interfaces de servicio `I<Noun>Service`, implementación `<Noun>Service`, interfaz de dominio `I<Noun>Repository`, adaptador `<Noun>Repository` y repositorio Spring Data `<Noun>JpaRepository`. Conserva la convención del módulo existente en lugar de renombrar todo.

### Dominios implementados

| Módulo | Alcance principal |
|---|---|
| `users` | Cuentas de administradores y usuarios de la app, autenticación, roles y asignaciones. Aloja también configuración de seguridad. |
| `players` | Jugadores, asociación a un único equipo e invitaciones/registro. |
| `teams` | Equipos, categorías asignadas e inscripciones a eventos (`teams_events`). |
| `events` | Eventos, categorías, tipos y formatos de evento con reglas de competición. |
| `matches` | Partidos, marcador y estados, calendario configurable, posiciones y motor de campeonato. |
| `courts`, `sportcomplexes` | Canchas y complejos deportivos, incluidos datos de ubicación del complejo. |
| `holidays` | Fechas de descanso que el generador de calendario debe evitar. |
| `organizations` | Clientes/organizaciones y su estado; el propietario de plataforma puede gestionarlos. |
| `menu` | Árbol de navegación, visibilidad por rol y administración del menú. |
| `permissions` | CRUD de permisos; en el estado actual no hay consumidores que apliquen esos permisos a las llamadas de negocio. |

`shared/` concentra piezas transversales: entidades base/auditoría, paginación, respuestas HTTP, excepciones, seguridad JWT, correo, límite de intentos de login y ámbito multi-organización. La ortografía del paquete común es literalmente `shared/infraestructure` (con una “c” extra); los módulos usan `infrastructure`.

**Acoplamientos y particularidades a tener presentes:** la configuración de seguridad y `AuditorAwareImpl` viven en `modules/users`; `shared` accede a información del usuario autenticado para auditoría y tenancy. El motor puro del campeonato es una adición deliberada dentro de `matches/application/engine`, no otro módulo CRUD.

## 3. Recorrido de una petición

Ejemplo conceptual para crear un recurso: `SecurityFilterChain` procesa CORS, el filtro de límite de login y JWT; el controller valida el DTO con Bean Validation, delega al contrato de servicio y envuelve la respuesta en `ApiResponse`; el servicio aplica reglas y transacción, usa el puerto del repositorio, y el adaptador de infraestructura ejecuta la operación de Spring Data. `GlobalException` normaliza las excepciones que salen de la petición.

Las clases principales para seguir el flujo son:

- Seguridad: `modules/users/infrastructure/configuration/security/SecurityConfig.java`.
- Respuesta: `shared/presentation/responses/ApiResponse.java`.
- Excepciones HTTP: `shared/infraestructure/exceptions/GlobalException.java`.
- Persistencia multi-tenant: `shared/domain/tenancy/` y `shared/infraestructure/tenancy/`.

## 4. Contrato HTTP, validación y errores

### Prefijo y documentación de API

Los paths de los controladores se declaran sin el context-path: por ejemplo, el controller declara `/events`, pero la URL completa es `/api/events`. La interfaz Swagger UI está configurada en `/api/swagger-ui.html`; OpenAPI JSON en `/api/v3/api-docs`. Úsala para descubrir la lista exacta de rutas y esquemas expuestos: el backend contiene muchos recursos y el inventario de este documento es intencionalmente por familias, no una especificación generada.

### Envoltorio de todas las respuestas

El contrato común es `ApiResponse<T>`:

```json
{
  "success": true,
  "status": 200,
  "message": "OK",
  "data": {},
  "timestamp": "..."
}
```

El controller devuelve el objeto bajo `data`; los clientes Angular y Flutter desenvuelven esa propiedad. Los POST de creación suelen devolver HTTP 201; las lecturas/actualizaciones, 200. El factory `deleted()` devuelve 200 y `data: null` (no asumir 204). En las listas paginadas, `data` es un `PageResponse<T>` con `content`, `page`, `size`, `totalElements` y `totalPages`.

Las listas de usuarios (`/users`, `/users/owners`, `/users/admins`, `/users/regular`) se solicitan con `page` desde cero, `size` y `search`. Los límites están en `app.pagination.*`; una petición fuera de rango puede producir 400. No supongas que una lista de otro módulo ya pagina: comprueba su controller y OpenAPI.

### Errores observables

| Causa | HTTP y forma habitual |
|---|---|
| Bean Validation o `FieldValidationException` | 400; `data` contiene `campo: motivo` (los motivos de dominio estables suelen ser códigos en inglés). |
| `IllegalArgumentException` / `IllegalStateException` | 400; texto/código en `message`. El motor de campeonato comunica así las acciones/rechazos conocidos. |
| Credenciales incorrectas | 401; el error de auth se envuelve en `ApiResponse`. Un token ausente/inválido en ruta protegida también termina en 401. |
| Acceso a función de plataforma no permitido | 403. En login, el `message` puede ser un código `LoginError` que distingue por qué se rechazó el tipo de cliente o la organización. |
| Recurso no encontrado | 404. |
| Conflicto de integridad de PostgreSQL | 409 genérico; no siempre expone qué constraint produjo el conflicto. |
| Excepción no mapeada | 500. La implementación actual utiliza `ex.getMessage()`; no conviertas ese texto incidental en contrato ni lo muestres como dato seguro al usuario. |

Para errores nuevos de negocio, prefiere enums/códigos estables y `FieldValidationException` cuando corresponda a campos. Si un frontend traduce el código, cambiar su nombre o trasladarlo entre `data` y `message` es un cambio de contrato.

El JSON de DTO está en camelCase en el contrato actual. Aunque hay propiedades de configuración para la estrategia de nombres y un `JacksonConfig`, no presupongas que un cambio en ellas altera el `ObjectMapper` de MVC: verifica la serialización real y los clientes antes de modificar el casing.

## 5. Autenticación, autorización y límites de tenancy

### Autenticación

Spring Security es stateless: no usa sesión de servidor, desactiva CSRF, valida JWT y BCrypt codifica contraseñas. `LoginRateLimiterFilter` protege el endpoint de login. Los únicos paths permitidos sin token según `SecurityConfig` son:

- `POST /api/users/auth/login`
- `POST /api/users/auth/register`
- `POST /api/players/auth/register`
- Swagger/OpenAPI.

El login incluye `clientType`: `ADMIN_PANEL` para el panel Angular y `MOBILE_APP` para Flutter. La duración configurada del JWT es 24 horas y no existe flujo de refresh token. Un 401 es la señal que ambos frontends usan para descartar la sesión. Login de administrador se decide por el UUID configurado para el rol, no por el nombre visible; el UUID debe coordinarse con la configuración Angular `adminRoleId`.

### Autorización: distinguir rol de token válido

`@EnableMethodSecurity`, `@PreAuthorize` y `AccessRules` restringen operaciones de plataforma (por ejemplo organizaciones, roles, menú y permisos) al propietario; ciertas operaciones de gestión de usuarios requieren administrador. Sin embargo, **la mayoría de los CRUD deportivos solo comprueba que exista un JWT válido**. No extrapoles un sistema general de permisos por rol ni confíes en ocultar botones del cliente como control de acceso.

El rol de administrador de una organización y el rol de propietario de plataforma son independientes. El propietario recibe acceso de plataforma por su rol propio; no necesita el rol Admin. La configuración de ambos IDs y su coherencia con los datos sembrados es esencial.

### Aislamiento por organización

El aislamiento se aplica en el backend mediante filtros de Hibernate configurados en las entidades de tenant y el principal JWT, no mediante filtros de interfaz. El usuario administrador autenticado con `organizationId` queda acotado a esa organización; el propietario de plataforma no queda restringido y ve todas. Recursos relacionados con un evento/equipo/partido heredan el alcance por referencias y filtros propios. Las migraciones V3 y el paquete `shared/domain/tenancy` describen/implementan las columnas y condiciones.

**Límite confirmado del estado actual:** `CurrentTenant.scope()` devuelve ámbito sin restricción cuando el principal no existe, es propietario o no tiene `organizationId`. Esto incluye las cuentas de la app móvil sin organización (y tareas sin principal). La decisión de tenancy/visibilidad de cuentas móviles no está cerrada en el código; no asumas que la app móvil solo ve una organización. Un cambio en esta política afecta tanto a servicios como a asociaciones, búsquedas, jobs y ambos clientes.

Varias referencias se almacenan como UUID simples y no necesariamente como claves foráneas ni se validan al escribir. En particular, los IDs de equipos/eventos/canchas de partidos y el `teamId` de invitaciones requieren atención: valida explícitamente las relaciones en el servicio antes de confiar en ellas. No generalices integridad por el mero hecho de usar UUID.

## 6. Familias de endpoints relevantes

Todos los paths siguientes llevan `/api` delante. La tabla no sustituye Swagger ni cubre cada endpoint:

| Familia | Paths/operaciones representativas | Uso |
|---|---|---|
| Sesión de usuarios | `/users/auth/login`, `/users/auth/register`, `/users/auth/update-profile`, `/users/auth/change-password` | Inicio de sesión, registro y perfil. El cambio de contraseña es autenticado. |
| Usuarios y roles | `/users`, `/users/owners`, `/users/admins`, `/users/regular`, `/users/counts`; `/roles`; `/users-roles/by-user/{userId}` y asignaciones | Gestión y listas paginadas del panel. La asignación de roles es una superficie protegida. |
| Jugadores | `/players/auth/register`, `/players/by-team/{teamId}`, `/players/search?email=...`, `/players/invite-register` | Registro móvil, consulta y alta/invitación desde equipos. |
| Equipos e inscripciones | `/teams`, `/teams-events`, `/teams-events/by-event/{eventId}` | CRUD de equipos y sus inscripciones a eventos. |
| Eventos y catálogo | `/events`, `/event-types`, `/event-formats`, `/categories`, `/events-categories` | Eventos y catálogos. El formato es el que define reglas de campeonato; no es equivalente al tipo usado por algunos flujos móviles. |
| Partidos y calendario | `/matches`, `/matches/by-event/{eventId}`, `/events/{eventId}/schedule-config`, `/events/{eventId}/schedule-config/generate`, `/events/{eventId}/standings` | CRUD/estado de partidos, generación automática, posiciones. Hay acciones PATCH específicas para aplazar, suspender o reprogramar. |
| Campeonato | `/events/{eventId}/championship`, `/groups/draw`, `/next`, `/matches/{matchId}/advancing-team` | Estado de fases, sorteo, avance de fase/ronda y resolución administrativa de empate. Véase la sección siguiente. |
| Instalaciones y días sin juego | `/courts`, `/sport-complexes`, `/holidays` | Canchas, complejos con coordenadas y feriados/descansos. |
| Menú y gestión de plataforma | `/menu`, `/menu/for-current-user`, `/menu/reorder`, `/roles-menu`, `/roles-menu/by-role/{roleId}`, `/organizations`, `/permissions` | Árbol de navegación y administración exclusiva del propietario en las escrituras indicadas por controller. |

Los CRUD no son uniformes: muchos `DELETE /{id}` son lógicos y algunos recursos tienen una ruta `/hard`, mientras que otros (por ejemplo formatos y feriados) no. Lee el controller/service concreto antes de programar borrado, reutilización de nombres o efectos secundarios.

## 7. Motor de campeonato

El motor vive en `modules/matches/application/engine/` y usa estructuras Java puras, sin Spring ni JPA. `ChampionshipService` traduce evento, formato, inscripciones y partidos a esos tipos; el motor calcula el próximo paso y las rondas, y el servicio persiste el resultado. Las principales piezas son `ChampionshipRules`, `ChampionshipProgress`, `ChampionshipPlanner`, `RoundRobinSchedule`, `StandingsCalculator`, `KnockoutBracket`, `GroupDraw` y `TieResolver`.

Las estructuras admitidas por `EventFormatStructure` son liga (`ROUND_ROBIN`), suizo (`SWISS`), eliminación (`KNOCKOUT`), doble eliminación (`DOUBLE_ELIMINATION`), grupos + eliminación, liga + playoffs y suizo + playoffs. El formato aporta reglas de puntos/desempate, ida/vuelta, rondas, grupos, plazas y final según estructura. Las fases se generan en orden; el endpoint `next` no genera indiscriminadamente toda la competición: genera el calendario completo de ciertas primeras fases o la siguiente ronda de eliminatorias/suizo, según el estado.

Flujo típico del panel: configurar calendario (`POST /events/{id}/schedule-config`), consultar estado completo, sortear grupos si aplica y llamar `POST /events/{id}/championship/next` cuando el motor lo permite. Los grupos pueden sortearse automáticamente al generar por primera vez; llamar al sorteo después de crear partidos de grupos se rechaza. Si una eliminatoria queda empatada en el agregado, el administrador selecciona qué equipo avanza en el partido decisivo mediante PATCH. La API devuelve el campeonato completo en las cuatro operaciones de campeonato.

Los resultados de partido están sujetos a su estado y tiempo: registrar un resultado antes del fin del partido se rechaza. El backend es la autoridad aunque el frontend también adelante validaciones. El motor se cubre con pruebas puras y un simulador (`ChampionshipSimulator`) que recorre campeonatos; consulta `src/test/java/com/platform/backend/modules/matches/application/engine/` al modificar las reglas.

## 8. Persistencia y ownership de Flyway

Flyway es el único propietario del esquema y de los datos iniciales. Las migraciones están en `src/main/resources/db/migration/` y actualmente llegan hasta `V9__menu_groups_and_menus_screen.sql`:

- `V1__migracion_inicial.sql`: esquema base y datos iniciales.
- `V2__sport_complex_location_menu.sql`: coordenadas de complejos y entrada del menú correspondiente.
- `V3__organizations.sql`, `V4__owner_and_admin_roles.sql`, `V5__owner_demo_account.sql`: tenancy y roles/cuenta propietaria.
- `V6__role_menus_screen.sql`, `V7__organizations_screen.sql`: entradas de menú para esas pantallas.
- `V8__users_list_indexes.sql`: índices de listas de usuarios.
- `V9__menu_groups_and_menus_screen.sql`: grupos de menú y su administración.

Para cambiar esquema o seeders, agrega una migración `V{n}__descripcion.sql`; no edites una migración ya aplicada y no crees scripts SQL de instalación ni `CommandLineRunner` para datos. Hibernate tiene `ddl-auto=validate`, no crea/actualiza el esquema. Al inicio Flyway aplica migraciones pendientes y después Hibernate verifica el mapeo. El perfil de producción deshabilita la siembra de usuarios demo; un entorno productivo necesita el procedimiento explícito de creación del primer propietario/admin que use el despliegue.

La migración V1 crea un catálogo de formatos predefinidos y entradas iniciales de navegación, pero no repone automáticamente un dato que alguien haya borrado después de que la migración ya se ejecutó. Los formatos de catálogo globales no llevan organización; las entidades propias de un tenant sí. La columna `organization_id` se añade en V3 para los recursos que pertenecen directamente a una organización.

## 9. Configuración, secretos y ejecución

### Configuración que afecta al contrato

Los archivos están en `src/main/resources/application*.properties`; también hay propiedades de prueba en `src/test/resources/`. `application.properties` activa actualmente el perfil `dev`. Entre las propiedades de aplicación que deben mantenerse coherentes están:

- `server.servlet.context-path` (`/api`), conexión PostgreSQL y configuración de Flyway.
- `jwt.secret`, expiración y credenciales SMTP (`spring.mail.*`); son secretos y no deben copiarse a documentación, logs, ejemplos o repositorios. Los archivos de propiedades contienen valores configurados localmente: antes de distribuir o desplegar, gestiona credenciales y claves mediante el mecanismo seguro del entorno y revisa su rotación/exposición.
- `app.frontend.url`, usada en enlaces de invitación; debe apuntar al frontend correcto en cada entorno.
- `app.security.admin-role-id` y `app.security.owner-role-id`, UUIDs de roles que deben corresponder a la base y a los clientes.
- `app.pagination.*`, `app.menu.max-depth`, configuración del partido y `spring.flyway.placeholders.*`.
- `spring.flyway.placeholders.seed_demo_users`: se siembra en configuración normal y está desactivada en `application-prd.properties`; Flyway es de una sola ejecución por versión, así que cambiarlo más tarde no altera datos ya sembrados.

La dependencia `dotenv-java` aparece en `pom.xml`, pero no se encontró uso de Dotenv en `src/main/java`: no des por hecho que un archivo `.env` se carga automáticamente. Spring Boot sí permite sobreescritura de propiedades por su mecanismo normal; configura los secretos fuera del repositorio y comprueba qué profile/valores efectivos usa cada despliegue, sin poner secretos en comandos compartidos.

### Prerrequisitos y comandos (Windows / PowerShell)

Necesitas un JDK compatible con Java 25 y PostgreSQL accesible con los valores configurados. El wrapper Maven del repo evita depender de una instalación global:

```powershell
# Ejecutar pruebas
.\mvnw.cmd test

# Compilar y empaquetar
.\mvnw.cmd clean package

# Arrancar (perfil activo por defecto: dev)
.\mvnw.cmd spring-boot:run

# Cambiar profile al ejecutar, por ejemplo local
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=local"
```

Después de iniciar, verifica `/api/swagger-ui.html`. El backend necesita conectividad de base de datos y migraciones aplicables antes de estar listo. No uses un profile por su nombre sin comprobar qué valores contiene: los archivos actuales pueden apuntar a recursos locales y no equivalen por sí solos a una configuración de nube/producción.

## 10. Pruebas y validación

El proyecto usa `spring-boot-starter-test` y se valida con Maven/JUnit. Hay pruebas de servicios y controladores, filtros JWT, paginación, aislamiento de tenancy, entidades de feriados y, especialmente, del motor de campeonato. En esta revisión el árbol contiene 51 archivos Java de pruebas (incluidos fixtures/simulador); eso no significa que cada endpoint o integración con frontend tenga cobertura.

Ejecuta `.\mvnw.cmd test` en una configuración de base de datos compatible. Para pruebas focalizadas puedes pasar una selección Maven, por ejemplo:

```powershell
.\mvnw.cmd "-Dtest=ChampionshipFlowTest,StandingsCalculatorTest" test
.\mvnw.cmd "-Dtest=JwtSecurityChainTest" test
```

Al cambiar DTO/contrato, valida serialización del controller y casos de error además del servicio. Para migrations, valida actualización de una base existente y una base vacía/limpia conforme al alcance de la migración. Las pruebas de frontend de los repos hermanos no sustituyen pruebas del backend.

## 11. Integración con Angular y Flutter

Los tres repositorios son independientes; no comparten modelos generados ni cliente OpenAPI. Cualquier cambio de ruta, JSON, regla de auth o error debe coordinarse explícitamente.

| Cliente | Integración vigente a revisar |
|---|---|
| Admin Angular (`frontend/administration-angular`) | `environment*.ts` define `apiUrl`; el panel desenvuelve `response.data`, el interceptor añade Bearer solo al API y cierra sesión ante 401. Envía `clientType: 'ADMIN_PANEL'`; `adminRoleId` debe coincidir con `app.security.admin-role-id`. Las rutas que aparecen en el menú dinámico deben coincidir con los ítems sembrados por migración; el texto/código de errores se traduce en los diccionarios propios del panel. |
| Flutter (`frontend/usuario-flutter`) | `lib/core/config/app_config.dart` define URL, contexto `/api` en desarrollo local y `API_BASE_URL` como override; `lib/core/network/dio_client.dart`/`api_endpoints.dart` configuran HTTP; login envía `clientType: 'MOBILE_APP'`. El cliente espera camelCase y usa interceptor para 401. La configuración de staging/producción hoy apunta a paths terminados en `/v1`, mientras este backend publica `/api`: debe existir una traducción explícita en infraestructura o corregirse/coordinarse antes de asumir compatibilidad productiva. |

La inscripción por invitación conecta backend y app: el enlace usa `app.frontend.url` y el token de invitación; verifica deep links y formato del token en Flutter si se cambia.

### Checklist para un cambio contractual

1. **Aclarar efecto y compatibilidad:** ruta, verbo, autorización, tenant, paginación, campos nulos y semántica del borrado; buscar consumidores en los dos frontends antes de tocar el DTO.
2. **Backend:** cambiar Request/Response, mapper, controller, servicio, validaciones y errores en el mismo módulo; agregar/ajustar tests. Mantener `ApiResponse` y códigos estables.
3. **Base de datos:** nueva migración Flyway para columnas, índices, restricciones o seed; no editar versiones aplicadas. Si el recurso es multi-tenant, añade columna/relación tenant, filtro Hibernate y repositorio tenant-aware; cubre también búsquedas por ID y referencias derivadas.
4. **Menú:** si se agrega una pantalla Angular, crear su ruta y entrada de menú mediante migración nueva. Otorgar visibilidad a los roles correctos; las entradas de grupo no reciben asignación propia. Las pantallas owner-only deben restringirse tanto en backend como en el cliente.
5. **Angular:** actualizar el modelo y servicio de la feature, formularios/errores y traducciones; verificar ruta de menú e ID de rol.
6. **Flutter:** revisar `api_endpoints.dart`, datasource/modelos y mapeo de JSON, interceptor/auth, traducciones y navegación que consumen el mismo endpoint.
7. **Verificación integrada:** correr Maven tests/build, probar los códigos HTTP y JSON reales (Swagger/MockMvc o entorno aislado), aplicar la migración en una base representativa y validar el recorrido de ambos clientes; revisar URL base/profile en cada entorno.
8. **Documentación coordinada:** actualizar la guía del proyecto afectado y las guías de los otros repos cuando cambie un contrato compartido. Separar con claridad lo implementado de lo planeado.

## 12. Mapa rápido de archivos

- `pom.xml`: Java, Spring Boot, dependencias y build.
- `src/main/resources/application*.properties`: perfiles y configuración; tratar los valores secretos con cuidado.
- `src/main/resources/db/migration/`: única fuente de cambios de esquema y seed.
- `src/main/java/com/platform/backend/modules/`: lógica y API por dominio.
- `src/main/java/com/platform/backend/shared/`: respuesta, errores, seguridad, tenancy y componentes transversales.
- `src/main/java/com/platform/backend/modules/matches/application/engine/`: algoritmos puros del campeonato.
- `src/test/java/`: pruebas, fixtures y simulación.
- [`ARCHITECTURE.md`](ARCHITECTURE.md): arquitectura y patrones de extensión.
- [`CLAUDE.md`](CLAUDE.md): contexto del producto, reglas y detalles del estado actual; úsalo como referencia suplementaria, confirmando siempre el código.




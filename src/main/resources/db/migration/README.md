# Migraciones de Base de Datos — Flyway

## Configuración actual

| Entorno | `ddl-auto` | Flyway |
|---------|-----------|--------|
| `dev` | `validate` | Activo |
| `prd` | `validate` | Activo |

Hibernate **no modifica** el esquema en ningún entorno. Flyway es el único responsable de crear y modificar tablas.

---

## Generar SQL a partir de las entidades

Cuando agregues nuevas entidades, usa el perfil `generate` para que Hibernate escriba el SQL automáticamente.

### Requisito
La base de datos debe estar corriendo (Hibernate la necesita para determinar el dialecto).

### Comando

```powershell
./mvnw spring-boot:run "-Dspring-boot.run.profiles=generate"
```

La app arranca, escanea todas las entidades JPA, escribe el archivo y termina. El resultado queda en:

```
src/main/resources/db/migration/generated-schema.sql
```

### Ejemplo de output generado

```sql
create table teams (
    id uuid not null,
    name varchar(255),
    created_by uuid,
    created_at timestamp(6) not null,
    updated_by uuid,
    updated_at timestamp(6),
    primary key (id)
);
```

---

## Flujo para crear una nueva migración

```
1. Crear la entidad JPA
        ↓
2. Correr el perfil generate
        ↓
3. Abrir generated-schema.sql
        ↓
4. Extraer solo el CREATE TABLE de la entidad nueva
        ↓
5. Guardar como V{n}__create_{nombre}_table.sql
        ↓
6. Borrar generated-schema.sql
        ↓
7. Arrancar la app normal — Flyway aplica la migración
```

---

## Ajustes manuales al SQL generado

El output de Hibernate requiere dos ajustes antes de usarlo como migration:

### 1. Agregar `IF NOT EXISTS`

Hibernate genera:
```sql
create table teams (...);
```

Cambiarlo a:
```sql
create table if not exists teams (...);
```

Esto hace que el script sea seguro para correr sobre una BD que ya tenga la tabla (útil en local).

### 2. Agregar índices únicos (si aplica)

Hibernate no genera índices únicos a menos que uses `@Column(unique = true)` en la entidad. Si un campo debe ser único (como `email` o `username`), agrégalo manualmente:

```sql
create unique index if not exists idx_teams_name on teams (name);
```

---

## Convención de nombres

```
V{versión}__{descripción}.sql
```

| Archivo | Descripción |
|---------|-------------|
| `V1__create_users_table.sql` | Creación inicial de la tabla `users` |
| `V2__create_teams_table.sql` | Tabla del módulo de equipos |
| `V3__add_status_to_users.sql` | Columna nueva en tabla existente |

- El número de versión debe ser mayor al anterior
- La descripción usa `_` como separador de palabras
- Flyway **nunca** vuelve a ejecutar un script ya aplicado

---

## Cómo funciona `baseline-on-migrate`

Configurado en `application.properties`:

```properties
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=0
```

Esto resuelve el caso donde la BD ya tiene tablas pero no tiene la tabla de historial `flyway_schema_history` (por ejemplo, tablas creadas anteriormente por `ddl-auto=update`). Flyway crea la tabla de historial, establece la línea base en versión `0` y ejecuta todas las migraciones a partir de `V1`.

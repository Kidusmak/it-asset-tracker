# IT Asset & Equipment Tracker API

Spring Boot 3 (Java 21) + PostgreSQL REST API for tracking hardware assets and employee assignments,
secured with Spring Security (HTTP Basic, stateless) and role-based access control.

## Run with Docker

```bash
cp .env.example .env      # optional - compose has working defaults
docker compose up --build
```

API: http://localhost:8080 - PostgreSQL data lives in the `postgres_data` volume.

An initial admin account is created on first start (default `admin` / `Admin@12345`;
override with `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD`).
`POST /api/auth/register` always creates a `ROLE_STAFF` user, so nobody can self-register as admin.

## Run locally (without Docker)

Needs Java 21, Maven and a PostgreSQL database:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/assetdb
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
mvn spring-boot:run
mvn test        # unit tests (no database needed)
```

## Endpoints

| Method | Endpoint | Roles |
|--------|----------|-------|
| POST | `/api/auth/register` | Public |
| GET | `/api/categories` | ADMIN, STAFF |
| POST | `/api/categories` | ADMIN |
| GET | `/api/assets` (`?status=AVAILABLE\|ASSIGNED\|MAINTENANCE`) | ADMIN, STAFF |
| GET | `/api/assets/my-assets` | ADMIN, STAFF |
| POST | `/api/assets` | ADMIN |
| PATCH | `/api/assets/{id}/assign/{userId}` | ADMIN |
| PATCH | `/api/assets/{id}/return` | ADMIN |
| DELETE | `/api/assets/{id}` | ADMIN |

## Try it

```bash
# register a staff user
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"secret123"}'

# admin: create category + asset
curl -u admin:Admin@12345 -X POST localhost:8080/api/categories -H 'Content-Type: application/json' \
  -d '{"name":"Laptop"}'
curl -u admin:Admin@12345 -X POST localhost:8080/api/assets -H 'Content-Type: application/json' \
  -d '{"name":"HP ProBook 450","serialNumber":"SN-0001","categoryId":1}'

# admin: assign asset 1 to user 2 (alice), then list alice's assets
curl -u admin:Admin@12345 -X PATCH localhost:8080/api/assets/1/assign/2
curl -u alice:secret123 localhost:8080/api/assets/my-assets

# return it
curl -u admin:Admin@12345 -X PATCH localhost:8080/api/assets/1/return
```

## Behaviour notes

- **DTOs everywhere** - entities are never returned from controllers.
- **Assignment rule** - only `AVAILABLE` assets can be assigned, otherwise `400`. Returning requires the asset to be `ASSIGNED`.
- **Creating assets** - `status` is optional (default `AVAILABLE`; `MAINTENANCE` allowed; `ASSIGNED` is rejected, use the assign endpoint).
- **Validation** - blank names / missing serial numbers / bad enum values (body or `?status=`) return a structured `400`.
- **Errors** - every error returns `{timestamp, status, error, message, path}` (validation errors also include an `errors` map):
  `400` bad request, `401` not authenticated, `403` wrong role, `404` not found, `409` duplicate username / category / serial number.
- **Passwords** - hashed with `BCryptPasswordEncoder`.
- **Config** - DB credentials come from `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` (no defaults in `application.yml`).
- Schema is created automatically by Hibernate (`ddl-auto=update`, override with `SPRING_JPA_HIBERNATE_DDL_AUTO`).

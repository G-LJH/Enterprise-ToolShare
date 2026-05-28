# Tool Share Backend

Spring Boot backend scaffold for the Tool Share project.

## Run locally

```bash
docker compose up -d postgres

cd backend
mvn spring-boot:run
```

This project does not include a Maven Wrapper script (`mvnw`/`mvnw.cmd`), so a local Maven installation is required.

If your machine does not have PostgreSQL installed, start the database with Docker first. When the backend runs on your host machine, it should connect to `localhost`, not the Docker service hostname:

```bash
set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tool_share
set SPRING_DATASOURCE_USERNAME=postgres
set SPRING_DATASOURCE_PASSWORD=postgres
set SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
```

## Run with Docker

From the project root:

```bash
docker compose up -d --build
```

## Health check

GET http://localhost:8080/api/health

## Database migration

- Flyway is configured under `src/main/resources/db/migration`
- Default runtime datasource uses PostgreSQL: `jdbc:postgresql://localhost:5432/tool_share`
- Default runtime credentials are `postgres` / `postgres`
- In `docker compose`, the backend container connects to PostgreSQL with `jdbc:postgresql://postgres:5432/tool_share`
- Tests use H2 through `src/test/resources/application-test.yml`
- You can override runtime DB settings with `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SPRING_DATASOURCE_DRIVER_CLASS_NAME`

## Database version endpoint

GET http://localhost:8080/api/db/version

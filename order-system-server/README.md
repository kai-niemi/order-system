# Order System Server

The server is a Spring Boot application exposing a hypermedia-driven REST API using HAL and HAL-FORMS.

Start the server from the project root:

```bash
./run-server.sh
```

By default, the server listens on port `8090`.

See [API.md](../API.md) for a walkthrough of the available resources and operations.

## Connecting to a Database

The server uses PostgreSQL through the pgJDBC driver.

See the [project README](../README.md#quick-start) for instructions on creating the database and application role.

### Application Configuration

The baseline configuration is defined in [application.yml](src/main/resources/application.yml).

The datasource settings can be configured as follows:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/orders}
    username: ${DB_USER:orders}
    password: ${DB_PASSWORD:orders}
```

These defaults are intended for local development.

### Environment Variables

You can override the database connection without modifying `application.yml`:

```bash
DB_URL=jdbc:postgresql://localhost:5432/orders \
DB_USER=orders \
DB_PASSWORD=orders \
./run-server.sh
```

### Command-Line Arguments

Spring Boot also supports overriding configuration through command-line arguments:

```bash
./run-server.sh \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/orders \
  --spring.datasource.username=orders \
  --spring.datasource.password=orders
```

## Database Migrations

The database must exist before starting the application.

Database schema changes are managed through Flyway. Migrations run automatically at startup, provided the configured
database role has the necessary permissions.

As this is a demonstration application, Flyway is configured to recreate the schema when migration validation fails due
to changes in existing migrations. **This deletes existing data.**

On startup, the application automatically generates sample customers and products if both tables are empty.

---
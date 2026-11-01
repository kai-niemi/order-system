<!-- TOC -->
* [Order System](#order-system)
  * [Quick Start](#quick-start)
    * [1. Set up PostgreSQL](#1-set-up-postgresql)
    * [2. Build the application](#2-build-the-application)
    * [3. Start the server](#3-start-the-server)
    * [4. Explore the API](#4-explore-the-api)
    * [5. Start the hypermedia client](#5-start-the-hypermedia-client)
  * [Building](#building)
    * [Prerequisites](#prerequisites)
    * [Clone and Build the Project](#clone-and-build-the-project)
  * [Documentation](#documentation)
  * [Terms of Use](#terms-of-use)
<!-- TOC -->

[![Java CI](https://github.com/kai-niemi/order-system/actions/workflows/maven.yml/badge.svg?branch=main)](https://github.com/kai-niemi/order-system/actions/workflows/maven.yml)

# Order System

A sample order management application demonstrating the REST principles, with an emphasis on hypermedia-driven
interactions.

Built with Spring Boot, Spring HATEOAS, HAL, and HAL-FORMS, the application demonstrates resource discovery, navigation,
and state-dependent operations. It includes a hypermedia client that discovers available operations through links and
affordances rather than hardcoded endpoint URLs.

See [API.md](API.md) for a walkthrough covering API discovery, order placement with idempotency keys, order and
line-item navigation, cancellation, and error handling.

## Quick Start

Requires Java 21 or later and a running PostgreSQL instance.

### 1. Set up PostgreSQL

Connect to PostgreSQL as an administrator:

```bash
psql -U postgres
```

Depending on your PostgreSQL installation, particularly on macOS, the administrator role may be your operating system
username rather than `postgres`.

Create a dedicated application role:

```sql
CREATE ROLE orders WITH LOGIN PASSWORD 'orders';
```

Create the database:

```sql
CREATE DATABASE orders OWNER orders;
```

If the database already exists, you can change its ownership instead:

```sql
ALTER DATABASE orders OWNER TO orders;
```

Changing database ownership does not automatically change ownership of existing tables, schemas, or sequences. Existing
installations may require additional permissions.

The application uses the following development defaults:

| Setting  | Value       |
|----------|-------------|
| Host     | `localhost` |
| Port     | `5432`      |
| Database | `orders`    |
| Username | `orders`    |
| Password | `orders`    |

Verify the connection:

```bash
psql -h localhost -U orders -d orders
```

These credentials are intended for local development only.

See [Connecting to a Database](order-system-server/README.md#connecting-to-a-database) to configure a different connection.

### 2. Build the application

```bash
./mvnw clean install
```

### 3. Start the server

```bash
./run-server.sh
```

The server runs on port `8090` by default.

### 4. Explore the API

Retrieve the API entry point:

```bash
curl -i -H 'Accept: application/hal+json' \
  http://localhost:8090/
```

The response contains links to available resources and operations.

For a walkthrough of the API, including HAL-FORMS affordances and idempotent order creation, see [API.md](API.md).

### 5. Start the hypermedia client

With the server running, start the interactive client:

```bash
./run-client.sh
```

The client demonstrates how to navigate the API and discover available operations through hypermedia controls rather
than constructing endpoint URLs.

See the [client documentation](order-system-client/README.md) for available commands.

## Building

### Prerequisites

- macOS, Linux, or Windows with a compatible shell (MinGW)
- [Java 21+ JDK](https://openjdk.org/projects/jdk/21/)
- [Git](https://git-scm.com/downloads)
- PostgreSQL

### Clone and Build the Project

```bash
git clone https://github.com/kai-niemi/order-system.git
cd order-system
chmod +x mvnw
./mvnw clean install
```

The Maven build produces artifacts for the server and client modules.

For server configuration, database connectivity, and other runtime options, see
the [server documentation](order-system-server/README.md).

## Documentation

- [API Documentation](https://order-system.cloudneutral.se/): Link relations, problem types, and workflow guides.
- [REST API Walkthrough](API.md): Resource discovery, hypermedia controls, order placement, idempotency, cancellation, and error handling.
- [Server](order-system-server/README.md): Server configuration and database connectivity.
- [Client](order-system-client/README.md): Interactive hypermedia client and available commands.
 
## Terms of Use

Licensed under the [MIT License](LICENSE.txt).

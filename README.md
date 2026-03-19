# Ares

Ares is a multi-tenant business application platform.

The workspace is organized into:
- `docs` for plans and architecture notes
- `desktop` for the desktop application
- `mobile` for the React Native mobile application
- `server` for the Spring Boot backend

Core architecture direction:
- Spring Boot backend is the main source of business truth
- desktop and mobile are clients of the backend
- users belong to a company tenant
- data must stay isolated between companies

The old `minifakt` project is reference material only.
Implementation in this repository is treated as new source.

## Local PostgreSQL

Local backend development uses PostgreSQL in Docker.

1. Create a local env file:

```bash
cp .env.example .env
```

2. Start PostgreSQL:

```bash
docker compose up -d
```

3. Run the Spring Boot server from `server`.

The backend reads local DB settings from:
- `ARES_DB_HOST`
- `ARES_DB_PORT`
- `ARES_DB_NAME`
- `ARES_DB_USERNAME`
- `ARES_DB_PASSWORD`

To reset local database state completely, stop the containers and remove the Docker volume used by PostgreSQL.

# PLAN_02A_local-postgres.md

## Project
Ares

## Goal
Set up a repeatable local PostgreSQL environment for `server` so backend foundation work can proceed against a stable database using Docker.

---

## Summary

This plan defines the local development database setup for Ares.
The goal is to avoid machine-specific PostgreSQL installation drift and give the backend a predictable local runtime target.

The recommended approach is:

- run PostgreSQL in Docker
- keep database persistence in a named Docker volume
- configure Spring Boot through environment variables and development profiles
- keep secrets and machine-specific values out of committed source files

This plan is intentionally narrow.
It supports `PLAN_02_server-foundation.md` by providing the local database runtime needed for configuration, Flyway, and initial backend startup.

---

## Scope

This plan covers:

- local PostgreSQL through Docker Compose
- local database container naming and defaults
- local environment variable strategy
- Spring Boot local datasource integration
- local startup and reset workflow
- documentation for local DB usage

This plan does not cover:

- containerizing the Spring Boot application
- production database provisioning
- CI database setup
- backup strategy
- monitoring and observability
- cloud deployment

---

## Decision

Use Docker for the local PostgreSQL database.

Rationale:

- reproducible local setup
- no requirement for manual host PostgreSQL installation
- easier reset and rebuild during early schema work
- good fit for Spring Boot + Flyway local development
- aligns well with future team onboarding and CI usage

For now, only the database should be containerized.
The Spring Boot application should continue running directly on the host during development.

---

## Local Database Direction

Recommended local defaults:

- database engine: PostgreSQL
- version: PostgreSQL 16 or 17
- database name: `ares`
- database user: `ares`
- database password: local development value from environment
- host port: `5432` unless already occupied on the machine

Persistence:

- use a named Docker volume for PostgreSQL data

Networking:

- local `server` should connect through standard host/port settings

---

## Implementation

### 1. Docker Compose File

Add a root-level local development compose file.

Recommended filename:

- `docker-compose.yml`

Recommended service direction:

- one `postgres` service
- official PostgreSQL image
- named volume for `/var/lib/postgresql/data`
- environment-driven database name, user, and password
- explicit port mapping

Recommended service naming:

- service name: `postgres`
- container name may be omitted unless there is a strong reason to pin it

Rules:

- keep the compose file simple
- avoid adding extra tools like pgAdmin unless there is an immediate need
- do not add unrelated services in this step

### 2. Environment Variable Strategy

Use environment variables for local database configuration.

Recommended approach:

- commit `.env.example`
- keep real local values in `.env`
- ensure `.env` is ignored by git

Likely variables:

- `ARES_DB_HOST`
- `ARES_DB_PORT`
- `ARES_DB_NAME`
- `ARES_DB_USERNAME`
- `ARES_DB_PASSWORD`

Optional Docker-specific variables may mirror the same values if useful.

Rules:

- avoid hard-coding passwords into compose or Spring config
- keep variable names clear and backend-specific

### 3. Spring Boot Local Integration

Wire the server local profile to the Dockerized PostgreSQL instance.

Recommended direction:

- keep shared settings in `application.yaml`
- put local datasource defaults or placeholders in `application-dev.yaml`
- resolve actual sensitive values from environment variables

Required outcome:

- starting local PostgreSQL and then starting Spring Boot should allow a successful connection
- configuration errors should fail loudly

Rules:

- do not enable Hibernate schema auto-creation as the main schema strategy
- keep Flyway and datasource configuration aligned

### 4. Developer Workflow

Define the minimum local workflow for database usage.

Recommended workflow:

1. start PostgreSQL with Docker Compose
2. start Spring Boot with the development profile
3. let Flyway apply migrations on startup
4. inspect or reset the database only through explicit local commands

Recommended reset behavior:

- a developer may remove the named Docker volume when a full local reset is needed
- resets must be treated as local-development-only actions

### 5. Documentation

Document the local DB setup in a way that supports the next implementation phase.

Documentation should cover:

- how to start the database
- which environment variables are required
- how Spring Boot connects to it
- how to reset local data
- common failure points such as port conflicts

Likely places:

- `README.md`
- optional local notes in `docs` if needed later

---

## Deliverables

- root `docker-compose.yml` exists for local PostgreSQL
- `.env.example` documents required DB variables
- Spring Boot local configuration points to the Dockerized PostgreSQL instance
- local startup workflow is documented
- the repository supports repeatable local DB startup for backend work

---

## Acceptance Criteria

- `docker compose up` starts a local PostgreSQL instance for Ares
- the database persists across container restarts through a named volume
- `server` can connect to the local PostgreSQL instance using environment-based configuration
- local configuration does not require secrets committed to source control
- the setup is simple enough for repeated schema and Flyway work during backend foundation

---

## Risks

- hard-coding local credentials into committed files creates future cleanup problems
- adding too many local services now increases maintenance cost without real value
- using host-installed PostgreSQL instead of Docker can create environment drift
- not documenting reset behavior can slow down migration-heavy development

---

## Next Step After This Plan

After this plan is executed, the server foundation work in `PLAN_02_server-foundation.md` can proceed with:

- datasource configuration
- Flyway migrations
- initial schema foundation
- backend startup verification

---

## Assumptions

- Docker and Docker Compose are available on the development machine
- PostgreSQL remains the chosen local and primary backend database
- the server continues to run directly on the host during development

---

## Notes

- Keep local setup boring and predictable.
- Optimize for repeatable development, not for production parity in every detail.

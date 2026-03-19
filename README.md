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

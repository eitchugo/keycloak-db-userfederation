# keycloak-db-userfederation
A provider to create/read/update users from a database

## Building

```
docker compose build
docker compose run maven
```

## Installing

Copy jars from `maven/dist` to `keycloak/providers`

In development, start keycloak with

```
bin/kc.[sh|bat] start-dev
```

In production, build the provider with

```
bin/kc.[sh|bat] build
```

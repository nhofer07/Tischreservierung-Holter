#!/bin/sh
set -eu

if [ -n "${MONGODB_PASSWORD_FILE:-}" ]; then
    export QUARKUS_MONGODB_CREDENTIALS_PASSWORD="$(cat "$MONGODB_PASSWORD_FILE")"
    test -n "$QUARKUS_MONGODB_CREDENTIALS_PASSWORD"
fi

exec java -jar /app/quarkus-run.jar

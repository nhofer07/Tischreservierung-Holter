#!/bin/bash
set -euo pipefail

if [[ -z "${MONGO_APP_USER:-}" || -z "${MONGO_APP_PASSWORD:-}" ]]; then
  echo "MONGO_APP_USER und MONGO_APP_PASSWORD müssen gesetzt sein." >&2
  exit 1
fi

mongosh --quiet \
  --username "$MONGO_INITDB_ROOT_USERNAME" \
  --password "$MONGO_INITDB_ROOT_PASSWORD" \
  --authenticationDatabase admin \
  tischreservierung \
  --eval "db.createUser({user: '$MONGO_APP_USER', pwd: '$MONGO_APP_PASSWORD', roles: [{role: 'readWrite', db: 'tischreservierung'}]})"

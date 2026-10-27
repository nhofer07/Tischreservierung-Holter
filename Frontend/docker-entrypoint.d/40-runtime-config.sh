#!/bin/sh
set -eu

tenant="${ENTRA_TENANT_ID:-14ddf41a-9afb-4810-bc22-227e34a9a007}"
client="${ENTRA_CLIENT_ID:-8fe2707e-b93a-4657-ad94-d0440a067b8b}"
test_login="${ENABLE_LOCAL_TEST_LOGIN:-false}"

for identifier in "$tenant" "$client"; do
    if ! printf '%s' "$identifier" | grep -Eq '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'; then
        echo 'Entra Tenant-ID und Client-ID müssen gültige UUIDs sein.' >&2
        exit 1
    fi
done

case "$test_login" in
    true|false) ;;
    *) echo 'ENABLE_LOCAL_TEST_LOGIN muss true oder false sein.' >&2; exit 1 ;;
esac

printf 'window.deskvisionConfig = {"entraTenantId":"%s","entraClientId":"%s","enableLocalTestLogin":%s};\n' \
    "$tenant" "$client" "$test_login" > /usr/share/nginx/html/runtime-config.js

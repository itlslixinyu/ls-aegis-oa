#!/usr/bin/env bash
# Fixed R29 MySQL upgrade. No Spring context, dependencies, full master, seed, or baseline sync.
set -euo pipefail
if [[ $# -eq 1 && "$1" == "--help" ]]; then
  printf '%s\n' 'Usage: bash scripts/migrate-r29.sh validate|status|apply --target DB_HOST:DB_PORT/DB_NAME [--acknowledge-write]'
  printf '%s\n' 'Required: MIGRATION_IMAGE (existing local candidate), MIGRATION_NETWORK (existing DB network), MIGRATION_ENV_FILE (dedicated DB role only).'
  printf '%s\n' 'Does not build/deploy/start dependencies or mount production files. See docker/ls-aegis-server/MIGRATION-R29.md.'
  exit 0
fi
if [[ $# -lt 3 ]]; then echo '[ERROR] explicit action and --target required; use --help' >&2; exit 2; fi
if [[ -z "${MIGRATION_IMAGE:-}" || -z "${MIGRATION_NETWORK:-}" || -z "${MIGRATION_ENV_FILE:-}" ]]; then
  echo '[ERROR] dedicated migration image, network and DB env file must be explicit; use --help' >&2; exit 2
fi
if [[ ! -f "$MIGRATION_ENV_FILE" ]]; then echo '[ERROR] migration DB environment file missing' >&2; exit 2; fi
# Do not shell-source/print the env file or put credentials into command arguments.
# A standalone process avoids the production Compose's unrelated JWT/Redis secrets and private-key/data mounts.
docker image inspect "$MIGRATION_IMAGE" >/dev/null
docker network inspect "$MIGRATION_NETWORK" >/dev/null
exec docker run --rm --pull never --network "$MIGRATION_NETWORK" --user 10001:10001 \
  --label itls.migration.release=R29 --env-file "$MIGRATION_ENV_FILE" --entrypoint java "$MIGRATION_IMAGE" \
  -Dloader.main=com.ls.aegis.boot.tools.R29MigrationCli -cp /app/app.jar \
  org.springframework.boot.loader.launch.PropertiesLauncher "$@"

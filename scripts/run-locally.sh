#!/usr/bin/env bash
#
# Start PipelineCRM without Docker: a local PostgreSQL, the packaged jar, and the Vite dev
# server. This is the path that was actually executed while the demo was built, so if the
# compose stack is unavailable to you, this one is known to work.
#
#   scripts/run-locally.sh          start everything
#   scripts/run-locally.sh --stop   stop everything
#
# Requires: Java 21, Maven, Node 22, and a PostgreSQL 16 server binary.
#
# Each service is started with setsid so it leads its own process group, with stdin and
# stdout detached from this script. Without that the children inherit the script's stdout
# and a caller reading it waits for the services rather than for the script.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CLUSTER="${PIPELINECRM_PGDATA:-/var/lib/pipelinecrm-pg}"
PGPORT="${PIPELINECRM_PGPORT:-5433}"
PGBIN="${PIPELINECRM_PGBIN:-/usr/lib/postgresql/16/bin}"
API_PORT="${PIPELINECRM_API_PORT:-8080}"
WEB_PORT="${PIPELINECRM_WEB_PORT:-5173}"
RUNTIME="${TMPDIR:-/tmp}/pipelinecrm"
mkdir -p "$RUNTIME"

# Kill the whole process group: npx and pg_ctl both leave children behind otherwise.
stop_group() {
    local pidfile="$1"
    [[ -f "$pidfile" ]] || return 0
    kill -TERM -- "-$(cat "$pidfile")" 2>/dev/null || true
    rm -f "$pidfile"
}

stop_everything() {
    stop_group "$RUNTIME/api.pid"
    stop_group "$RUNTIME/web.pid"
    su postgres -c "$PGBIN/pg_ctl -D $CLUSTER stop" 2>/dev/null || true
    echo "stopped."
}

# A port already answering means someone else's process is there. Continuing would wait on
# a health check that a stranger satisfies, and report a stack that was never started.
require_free_port() {
    local port="$1" what="$2"
    if curl -s --noproxy '*' -m 2 -o /dev/null "http://127.0.0.1:$port/" \
       || nc -z 127.0.0.1 "$port" 2>/dev/null; then
        echo "port $port is already in use, so $what cannot start there." >&2
        echo "run 'scripts/run-locally.sh --stop', or set PIPELINECRM_$3_PORT." >&2
        exit 1
    fi
}

# Waits for the service to answer, but gives up if it dies or takes implausibly long, so a
# broken start fails loudly instead of hanging forever.
await() {
    local url="$1" pidfile="$2" what="$3" logfile="$4" waited=0
    until curl -s --noproxy '*' -m 2 -f "$url" >/dev/null 2>&1; do
        if ! kill -0 "$(cat "$pidfile")" 2>/dev/null; then
            echo "$what exited before it became healthy. Last lines of $logfile:" >&2
            tail -20 "$logfile" >&2
            exit 1
        fi
        if (( waited >= 120 )); then
            echo "$what did not become healthy within 120s. See $logfile." >&2
            exit 1
        fi
        sleep 2
        waited=$(( waited + 2 ))
    done
}

if [[ "${1:-}" == "--stop" ]]; then
    stop_everything
    exit 0
fi

require_free_port "$API_PORT" "the API" API
require_free_port "$WEB_PORT" "the frontend" WEB

echo "==> PostgreSQL"
if [[ ! -d "$CLUSTER" ]]; then
    mkdir -p "$CLUSTER"
    chown postgres "$CLUSTER"
    su postgres -c "$PGBIN/initdb -D $CLUSTER -U pipelinecrm --auth=trust" >/dev/null
fi
su postgres -c "$PGBIN/pg_ctl -D $CLUSTER -o '-p $PGPORT -k /tmp' -l $RUNTIME/pg.log start" >/dev/null 2>&1 || true
until psql -h 127.0.0.1 -p "$PGPORT" -U pipelinecrm -d postgres -c 'select 1' >/dev/null 2>&1; do sleep 1; done
psql -h 127.0.0.1 -p "$PGPORT" -U pipelinecrm -d postgres \
     -c "CREATE DATABASE pipelinecrm OWNER pipelinecrm" >/dev/null 2>&1 || true
echo "    listening on $PGPORT"

echo "==> Building the backend (skipping the slow gates; run 'mvn verify' for those)"
mvn -B -q -f "$ROOT/backend/pom.xml" package \
    -DskipTests -Dpit.skip=true -Dcrap.skip=true -Dcoverage.skip=true -Dcpd.skip=true

echo "==> API on $API_PORT"
DATABASE_URL="jdbc:postgresql://127.0.0.1:$PGPORT/pipelinecrm" \
DATABASE_USER=pipelinecrm DATABASE_PASSWORD=pipelinecrm \
PIPELINECRM_SECURITY_JWT_SECRET="a-development-secret-that-is-long-enough-to-sign" \
SERVER_PORT="$API_PORT" \
    setsid java -jar "$ROOT/backend/bootstrap/target/pipelinecrm.jar" \
    </dev/null >"$RUNTIME/api.log" 2>&1 &
echo $! > "$RUNTIME/api.pid"
await "http://127.0.0.1:$API_PORT/actuator/health" "$RUNTIME/api.pid" "the API" "$RUNTIME/api.log"
echo "    healthy"

echo "==> Frontend on $WEB_PORT"
(cd "$ROOT/frontend" && npm install --silent --no-audit --no-fund)
cd "$ROOT/frontend"
API_ORIGIN="http://127.0.0.1:$API_PORT" \
    setsid npx vite --host 127.0.0.1 --port "$WEB_PORT" --strictPort \
    </dev/null >"$RUNTIME/web.log" 2>&1 &
echo $! > "$RUNTIME/web.pid"
cd "$ROOT"
await "http://127.0.0.1:$WEB_PORT/" "$RUNTIME/web.pid" "the frontend" "$RUNTIME/web.log"
echo "    healthy"

cat <<INFO

PipelineCRM is running.

    http://localhost:$WEB_PORT

    sam@pipelinecrm.demo   / sam-password    (SALES)
    robin@pipelinecrm.demo / robin-password  (SALES)
    mo@pipelinecrm.demo    / mo-password     (MANAGER)

Logs: $RUNTIME/{pg,api,web}.log        Stop: scripts/run-locally.sh --stop
INFO

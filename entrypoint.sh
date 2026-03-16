#!/usr/bin/env bash
set -euo pipefail

# If DATABASE_URL exists, parse it safely with python3 and export SPRING_* variables
if [ -n "${DATABASE_URL:-}" ]; then
  # Use python to safely parse URL components (handles @ and : in password)
  eval "$(python3 - <<'PY'
import os, sys
from urllib.parse import urlparse, unquote
u = os.environ.get('DATABASE_URL', '')
if not u:
    sys.exit(0)
p = urlparse(u)
username = p.username or ''
password = p.password or ''
host = p.hostname or ''
port = p.port
db = p.path[1:] if p.path.startswith('/') else p.path
if port:
    jdbc = f"jdbc:postgresql://{host}:{port}/{db}?sslmode=require"
else:
    jdbc = f"jdbc:postgresql://{host}/{db}?sslmode=require"
# Print export lines (shell-safe quoted)
print(f"export SPRING_DATASOURCE_URL={repr(jdbc)}")
print(f"export SPRING_DATASOURCE_USERNAME={repr(unquote(username))}")
print(f"export SPRING_DATASOURCE_PASSWORD={repr(unquote(password))}")
PY
)"
fi

# Optional: show a masked confirmation in logs (host/db only, no secrets)
if [ -n "${SPRING_DATASOURCE_URL:-}" ]; then
  # extract host/db for log (do NOT print credentials)
  host_db=$(echo "$SPRING_DATASOURCE_URL" | sed -E 's@jdbc:postgresql://@@; s@\?.*@@')
  echo "Using SPRING_DATASOURCE_URL host/db: ${host_db}"
fi

# Make sure server binds on PORT if set (Spring picks it up from server.port property using ${PORT:8080})
# Exec the app (replace /app/app.jar if jar path differs)
exec java -jar /app/app.jar
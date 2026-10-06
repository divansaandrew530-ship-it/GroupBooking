#!/usr/bin/env bash
set -euo pipefail
# Copy GroupBookingLA UI templates and static files into this Spring Boot tree.
# Usage: ./scripts/copy-ui.sh /path/to/groupbookingla-ui
ui_root="${1:-}"
if [[ -z "$ui_root" || ! -d "$ui_root/src/main/resources" ]]; then
  echo "Usage: $0 /path/to/groupbookingla-ui" >&2
  exit 1
fi
root="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$root/src/main/resources/templates" "$root/src/main/resources/static"
cp -a "$ui_root/src/main/resources/templates/." "$root/src/main/resources/templates/"
cp -a "$ui_root/src/main/resources/static/." "$root/src/main/resources/static/"
echo "Copied UI from $ui_root into $root/src/main/resources"

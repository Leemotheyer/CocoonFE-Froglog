#!/usr/bin/env bash
export PATH="/c/Python312:/c/Program Files/Eclipse Adoptium/jdk-17.0.18.8-hotspot/bin:$PATH"
export FROGLOG_GAME_MENU_PATCH="${FROGLOG_GAME_MENU_PATCH:-1}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
exec ./scripts/build-froglog-apk.sh

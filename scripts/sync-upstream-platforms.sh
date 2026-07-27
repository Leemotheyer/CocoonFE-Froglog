#!/usr/bin/env bash
# Merge platform JSON updates from upstream inssekt/CocoonFE main.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if ! git remote get-url upstream &>/dev/null; then
  git remote add upstream https://github.com/inssekt/CocoonFE.git
fi

git fetch upstream main
git merge upstream/main -m "Merge upstream platforms from inssekt/CocoonFE main"

echo "Merged upstream/main. Resolve conflicts if any, then test platforms/index.json."

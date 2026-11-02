#!/usr/bin/env bash
# One-time setup on a fresh computer: clones the launcher project next to this repo, pins it to the
# version this repo was tested against, copies our files in and prepares the Python tools.
#
#   scripts/setup.sh
#
# Needs git and python3. Building the launcher also needs JDK 17 to 22 and Android SDK platform 34
# (build-tools 34.0.0); see README.md. If you already have a clone, use scripts/overlay.sh instead.
set -eu

UPSTREAM_URL="https://github.com/Gathin23/cleanyourtv.git"
UPSTREAM_COMMIT="3cd274f866b71ec15e4f7d8c796514b0baa8033c"   # tested 2026-11-02

here="$(cd "$(dirname "$0")/.." && pwd)"
clone="$here/cleanyourtv"

if [ ! -d "$clone/.git" ]; then
  git clone "$UPSTREAM_URL" "$clone"
fi
git -C "$clone" fetch --quiet origin
git -C "$clone" checkout --quiet "$UPSTREAM_COMMIT"

"$here/scripts/overlay.sh" "$clone"

if [ ! -x "$clone/tools/venv/bin/python" ]; then
  python3 -m venv "$clone/tools/venv"
  "$clone/tools/venv/bin/pip" install --quiet -r "$clone/tools/requirements.txt"
fi

echo
echo "ready: cd cleanyourtv, then follow README.md step 3 (TV_HOST=<tv ip>)"

#!/usr/bin/env bash
# Copies this repo's device list and theme into a clone of Gathin23/cleanyourtv, where its tools expect them.
#
#   scripts/overlay.sh ~/Documents/scaler/Project/cleanyourtv
#
# After that, from inside the clone:
#   examples/nokia-atv-r2/debloat.txt   the Nokia ATV R2 list (tools/disable.sh, tools/restore.sh)
#   themes/bhamley-glass/               the theme (tools/theme.sh)
set -eu

here="$(cd "$(dirname "$0")/.." && pwd)"
dest="${1:-}"

if [ -z "$dest" ] || [ ! -f "$dest/tools/disable.sh" ] || [ ! -d "$dest/themes" ]; then
  echo "usage: scripts/overlay.sh <path to your cleanyourtv clone>" >&2
  echo "that folder must contain tools/disable.sh and themes/" >&2
  exit 1
fi

mkdir -p "$dest/examples/nokia-atv-r2" "$dest/themes/bhamley-glass"
cp "$here/device/nokia-atv-r2/debloat.txt" "$here/device/nokia-atv-r2/tv.json" "$dest/examples/nokia-atv-r2/"
rm -rf "$dest/themes/bhamley-glass"
cp -r "$here/themes/bhamley-glass" "$dest/themes/bhamley-glass"

echo "copied into $dest:"
echo "  examples/nokia-atv-r2/debloat.txt, tv.json"
echo "  themes/bhamley-glass/ (config.json + 3 wallpapers)"

#!/usr/bin/env bash
# Settings for Glass Home, sent over adb. Run from anywhere; the first argument is the Clean Your TV clone.
#
#   TV_HOST=192.168.1.8 glasshome/set_data.sh <clone> renewals "Netflix=2026-11-12;Prime Video=2026-11-28;Hotstar=2026-12-03"
#       a date repeats every month; add /y for a yearly one: "Prime Video=2026-11-28/y"
#   TV_HOST=... glasshome/set_data.sh <clone> place "Pune, India" 18.5204 73.8567
#   TV_HOST=... glasshome/set_data.sh <clone> reset-taste
set -euo pipefail
CLONE="${1:?usage: set_data.sh <clone> renewals|place|reset-taste ...}"
WHAT="${2:?renewals, place or reset-taste}"
PY="$CLONE/tools/venv/bin/python"
[ -n "${TV_HOST:-}" ] || { echo "set TV_HOST to the TV IP address" >&2; exit 2; }
B="am broadcast -n com.nbhamley.glasshome/.SetData"
case "$WHAT" in
  renewals)   "$PY" -I "$CLONE/tools/tv.py" "$B --es renewals '${3:?the list, e.g. Netflix=2026-11-12;Hotstar=2026-12-03}'" ;;
  place)      "$PY" -I "$CLONE/tools/tv.py" "$B --es place '${3:?name}' --es lat '${4:?latitude}' --es lon '${5:?longitude}'" ;;
  reset-taste) "$PY" -I "$CLONE/tools/tv.py" "$B --ez reset_taste true" ;;
  *) echo "renewals, place or reset-taste" >&2; exit 2 ;;
esac

#!/usr/bin/env bash
# Install Glass Home on the TV next to Clean Your TV. Run from a Clean Your TV clone (it uses its tools/).
#
#   TV_HOST=192.168.1.8 glasshome/install.sh <cleanyourtv clone> <path to app-debug.apk> [--home]
#
# Without --home it only installs, so Clean Your TV stays Home. With --home it also makes Glass Home the Home
# app and sets its clock as the screensaver. Undo: install.sh <clone> --undo
set -euo pipefail
CLONE="${1:?usage: install.sh <cleanyourtv clone> <apk> [--home] | <clone> --undo}"
ARG2="${2:-}"
PY="$CLONE/tools/venv/bin/python"
[ -x "$PY" ] || { echo "no $PY: run the Clean Your TV setup first" >&2; exit 2; }
[ -n "${TV_HOST:-}" ] || { echo "set TV_HOST to the TV IP address" >&2; exit 2; }
tv() { "$PY" -I "$CLONE/tools/tv.py" "$@"; }

if [ "$ARG2" = "--undo" ]; then
  tv 'cmd package set-home-activity com.gathin.tvhome/.MainActivity'
  tv 'settings put secure screensaver_components com.gathin.tvhome/.ClockDream'
  echo "Clean Your TV is Home again. Glass Home is still installed; remove it with: pm uninstall com.nbhamley.glasshome"
  exit 0
fi

APK="$ARG2"
[ -f "$APK" ] || { echo "no APK at '$APK'" >&2; exit 2; }
"$PY" -I "$CLONE/tools/b64push.py" "$APK" /data/local/tmp/glasshome.apk
OUT=$(tv 'pm install -r /data/local/tmp/glasshome.apk; rm -f /data/local/tmp/glasshome.apk')
echo "$OUT"
case "$OUT" in *Success*) ;; *) echo "install failed, nothing else was changed" >&2; exit 1 ;; esac

if [ "${3:-}" = "--home" ]; then
  tv 'cmd package set-home-activity com.nbhamley.glasshome/.HomeActivity'
  tv 'settings put secure screensaver_components com.nbhamley.glasshome/.GlassDream'
  tv 'input keyevent KEYCODE_HOME'
  echo "Glass Home is Home. If the Home key still shows the old screen, restart the TV once."
else
  tv 'monkey -p com.nbhamley.glasshome -c android.intent.category.LAUNCHER 1' >/dev/null 2>&1 || true
  echo "Installed. It opened once on the TV; Clean Your TV is still Home. Add --home to make it Home."
fi

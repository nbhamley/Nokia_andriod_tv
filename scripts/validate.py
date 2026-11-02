#!/usr/bin/env python3
"""Offline checks for this repo: the theme, the wallpapers and the debloat list.

    python3 scripts/validate.py --upstream ~/Documents/scaler/Project/cleanyourtv
    python3 scripts/validate.py --upstream <clone> --packages r2_all.txt     # also compare with the TV's package dump

No TV is needed. What it cannot tell you (smoothness on the real GPU, how the screen looks on the panel, what the
remote's mic sends) is covered by docs/TEST_PLAN.md. Exit status is 1 when any check fails.

--upstream is optional but recommended: the theme keys are checked against the launcher's own documented keys.
The wallpaper checks need numpy and Pillow and are skipped, with a note, when they are missing.
"""
import argparse
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, ".."))
THEME = os.path.join(ROOT, "themes", "bhamley-glass")
LIST = os.path.join(ROOT, "device", "nokia-atv-r2", "debloat.txt")
TVJSON = os.path.join(ROOT, "device", "nokia-atv-r2", "tv.json")

# Packages that must never be in the list: the remote and key middleware, the firmware updater, the
# Assistant and Chromecast the owner kept, and the streaming apps in use.
PROTECTED = [
    "com.apps.virtualkey", "com.apps.smartkey", "com.apps.fivekey", "com.apps.bluetoothremote",
    "com.apps.sys", "com.apps.puresound", "com.apps.function.audiolink", "com.apps.ota", "com.apps.authota",
    "com.google.android.sss.authbridge", "com.google.android.tv.remote.service",
    "com.google.android.katniss", "com.google.android.apps.mediashell",
    "com.netflix.ninja", "com.amazon.amazonvideo.livingroom", "in.startv.hotstar",
    "com.apple.atve.androidtv.appletv", "com.google.android.youtube.tv",
    "com.realtek.keyinterceptserver", "com.realtek.tv.passthrough", "com.android.tv",
]
STOCK_LAUNCHER = "com.google.android.tvlauncher"
RANGES = {  # key: (low, high), from the launcher's themes/README.md
    "columns": (1, 8), "dock_opacity": (30, 100), "wallpaper_dim": (0, 90),
    "wallpaper_rotation_minutes": (0, 1440), "home_idle_clock_minutes": (0, 1440),
}
PKG_RE = re.compile(r"^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$")

results = []


def check(name, ok, detail=""):
    results.append((name, ok, detail))
    print("  [%s] %s%s" % ("PASS" if ok else "FAIL", name, ("  " + detail) if detail else ""))
    return ok


def note(text):
    print("  [note] " + text)


def active_entries(path):
    out = []
    with open(path, encoding="utf-8") as fh:
        for line in fh:
            s = line.split("#", 1)[0].strip()
            if s:
                out.append(s)
    return out


def documented_keys(upstream):
    path = os.path.join(upstream, "themes", "README.md")
    keys = set()
    with open(path, encoding="utf-8") as fh:
        for line in fh:
            m = re.match(r"^\| `([a-z0-9_]+)` \|", line)
            if m:
                keys.add(m.group(1))
    return keys


def lum(rgb):
    """WCAG relative luminance of an sRGB array (..., 3) with values 0..255."""
    import numpy as np
    c = rgb.astype("float64") / 255.0
    lin = np.where(c <= 0.03928, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)
    return 0.2126 * lin[..., 0] + 0.7152 * lin[..., 1] + 0.0722 * lin[..., 2]


def test_theme(upstream):
    print("\nTheme: themes/bhamley-glass/config.json")
    try:
        with open(os.path.join(THEME, "config.json"), encoding="utf-8") as fh:
            cfg = json.load(fh)
    except Exception as exc:  # noqa: BLE001
        check("config.json is valid JSON", False, str(exc))
        return
    check("config.json is valid JSON", True, "%d keys" % len(cfg))

    if upstream:
        known = documented_keys(upstream)
        unknown = sorted(k for k in cfg if k not in known)
        check("every key is documented by the launcher", not unknown,
              ("unknown: " + ", ".join(unknown)) if unknown else "%d of %d documented keys used" % (len(cfg), len(known)))
    else:
        note("--upstream not given: key names were not checked against the launcher")

    bad = [k for k, (lo, hi) in RANGES.items() if k in cfg and not (lo <= cfg[k] <= hi)]
    check("numeric values are inside the documented ranges", not bad, ", ".join(bad))

    apps = cfg.get("dock_apps", [])
    badpkg = [p for p in apps if not PKG_RE.match(p)]
    check("dock_apps are well-formed package names", bool(apps) and not badpkg, "%d apps" % len(apps))

    missing = []
    for group in cfg.get("wallpaper_moods", {}).values():
        for name in group:
            if not os.path.exists(os.path.join(THEME, "wallpapers", name)):
                missing.append(name)
    check("every wallpaper named in wallpaper_moods exists", not missing, ", ".join(missing))
    check("morning, evening and night are all defined",
          set(cfg.get("wallpaper_moods", {})) >= {"morning", "evening", "night"})
    check("motion is on (liquid look) and can be dialled back", cfg.get("focus_overshoot") is True
          and cfg.get("advanced_effects") is True and cfg.get("reduced_motion") is False,
          "fallbacks: focus_overshoot=false, then reduced_motion=true")


def test_wallpapers():
    print("\nWallpapers: themes/bhamley-glass/wallpapers")
    try:
        import numpy as np
        from PIL import Image
    except ImportError:
        note("numpy or Pillow missing: wallpaper checks skipped (pip install numpy pillow)")
        return
    for name in ("morning", "evening", "night"):
        path = os.path.join(THEME, "wallpapers", name + ".jpg")
        if not os.path.exists(path):
            check(name + ": file exists", False)
            continue
        img = Image.open(path)
        check(name + ": 1920x1080 JPEG", img.size == (1920, 1080) and img.format == "JPEG", "%dx%d" % img.size)
        kb = os.path.getsize(path) // 1024
        # the launcher project's theme recipe keeps every picture under 300 KB; the memory cost is the decoded
        # bitmap, about 8 MB either way
        check(name + ": file under 300 KB (theme recipe limit)", kb < 300,
              "%d KB (decodes to %.1f MB with wallpaper_true_color)" % (kb, 1920 * 1080 * 4 / 1048576.0))
        px = np.asarray(img.convert("RGB"))
        h = px.shape[0]
        top = lum(px[: int(h * 0.12)])
        bottom = lum(px[int(h * 0.60):])
        # white text on the brightest 0.5% of each band
        c_top = 1.05 / (np.percentile(top, 99.5) + 0.05)
        c_bot = 1.05 / (np.percentile(bottom, 99.5) + 0.05)
        check(name + ": white text readable in the top band (clock, Wi-Fi, Settings)", c_top >= 4.5, "contrast %.1f:1" % c_top)
        check(name + ": white text readable over the dock and tiles", c_bot >= 7.0, "contrast %.1f:1" % c_bot)
        # banding: after JPEG, long runs of one value in dark areas mean visible steps
        luma = (0.299 * px[..., 0] + 0.587 * px[..., 1] + 0.114 * px[..., 2]).astype("uint8")
        runs = []
        for y in range(int(h * 0.60), h - 4, 20):
            row = luma[y]
            edges = np.flatnonzero(np.diff(row) != 0)
            lengths = np.diff(np.concatenate(([-1], edges, [len(row) - 1])))
            starts = np.concatenate(([0], edges + 1))[: len(lengths)]
            dark = row[starts] < 40
            runs.extend(lengths[dark].tolist())
        p95 = float(np.percentile(runs, 95)) if runs else 0.0
        check(name + ": no banding in the dark gradient", p95 <= 8, "95th percentile run %.0f px" % p95)
        mean = float(luma.mean())
        check(name + ": dark enough overall for a TV in a lit room", 15 <= mean <= 90, "mean luma %.0f/255" % mean)


def test_list(packages):
    print("\nDebloat list: device/nokia-atv-r2/debloat.txt")
    entries = active_entries(LIST)
    check("list parses", bool(entries), "%d active entries" % len(entries))
    dupes = sorted({e for e in entries if entries.count(e) > 1})
    check("no duplicate entries", not dupes, ", ".join(dupes))
    malformed = [e for e in entries if not PKG_RE.match(e)]
    check("every entry is a package name", not malformed, ", ".join(malformed))
    hit = sorted(set(entries) & set(PROTECTED))
    check("none of the %d protected packages are listed" % len(PROTECTED), not hit, ", ".join(hit))
    check("the stock launcher is the last entry (disabled only after the new one is in)",
          entries[-1:] == [STOCK_LAUNCHER], entries[-1] if entries else "")
    with open(LIST, encoding="utf-8") as fh:
        text = fh.read()
    check("header carries the apply and undo commands",
          "tools/disable.sh examples/nokia-atv-r2/debloat.txt" in text
          and "tools/restore.sh examples/nokia-atv-r2/debloat.txt" in text)
    try:
        with open(TVJSON, encoding="utf-8") as fh:
            meta = json.load(fh)
        check("tv.json has tv, os, brand and notes", all(meta.get(k) for k in ("tv", "os", "brand", "notes")))
    except Exception as exc:  # noqa: BLE001
        check("tv.json has tv, os, brand and notes", False, str(exc))

    if packages:
        with open(packages, encoding="utf-8") as fh:
            have = {ln.strip().replace("package:", "") for ln in fh if ln.strip()}
        absent = [e for e in entries if e not in have]
        check("every entry exists on the TV (from %s)" % os.path.basename(packages), not absent,
              ("missing: " + ", ".join(absent)) if absent else "%d of %d packages present" % (len(entries), len(entries)))
        gone = [p for p in PROTECTED if p not in have]
        note("protected packages not on this TV (fine, just not applicable): " + (", ".join(gone) or "none"))
    else:
        note("--packages not given: entries were not compared with the TV's package dump")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--upstream", help="path to a clone of Gathin23/cleanyourtv")
    ap.add_argument("--packages", help="file with `adb shell pm list packages` output")
    args = ap.parse_args()
    upstream = os.path.expanduser(args.upstream) if args.upstream else None
    if upstream and not os.path.isfile(os.path.join(upstream, "themes", "README.md")):
        print("--upstream does not look like a cleanyourtv clone: " + upstream, file=sys.stderr)
        return 2

    test_theme(upstream)
    test_wallpapers()
    test_list(os.path.expanduser(args.packages) if args.packages else None)

    failed = [r for r in results if not r[1]]
    print("\n%d checks, %d passed, %d failed" % (len(results), len(results) - len(failed), len(failed)))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())

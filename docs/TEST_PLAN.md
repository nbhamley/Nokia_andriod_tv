# Test plan

Two kinds of tests: **offline** ones that need no TV (`scripts/validate.py`, plus the launcher project's own test
suite), and **on-TV** ones below. Run the commands from inside your `cleanyourtv` clone with `export TV_HOST=<tv ip>`.

## The seven requests at a glance

| # | Request | Status | How it is tested | Pass when |
|---|---|---|---|---|
| 1 | Fun games on Home (Super Over, Snake, Wordle) | Not built (Phase 3) | Acceptance criteria below, once built | Playable with the D-pad alone, launches in under 2 s |
| 2 | A stunning home screen, detail first | Theme and wallpapers built | `validate.py` (offline), T1 screenshot and T2 smoothness (on TV) | Offline all pass, and the screenshot looks right to you |
| 3 | Personalised rows: thriller, action, animation | Not built (Phase 2) | Acceptance criteria below | Rows reorder after likes and survive a reboot |
| 4 | Liquid-glass look and animations | Config-level only: frosted dock, glass focus, springy highlight. True glass tiles are Phase 2 | T1 and T2 | No dropped-frame regression against the stock theme |
| 5 | Like / dislike swipe deck | Not built (Phase 3) | Acceptance criteria below | 20 swipes persist after a reboot, no network from the launcher |
| 6 | Voice orb ("Jarvis") | Not built (Phase 4); feasibility only | T3 mic probe | We know which key the mic sends and who records |
| 7 | Better video quality | Checks only | T4 | Settings found and set, DRM level known |

## T1. The look (points 2 and 4)

```bash
tools/theme.sh themes/bhamley-glass
tools/venv/bin/python -I tools/tv.py --screenshot home.png
```

Look for: wallpaper matches the time of day (morning until noon, evening until just after sunset, then night); clock,
Wi-Fi and Settings readable at the top; the dock readable over the dark lower part; the focus highlight glowing in each app's
colour. Send me `home.png`; the first screenshot is what Phase 2 is tuned against. Then try
`"cinematic_home": true` in `config.json`, push again and compare.

## T2. Smoothness (points 2 and 4)

The theme turns motion up (`focus_overshoot`, `advanced_effects`) on a 2 GB entry-level board, so measure rather than guess.
`bench.py` presses a fixed key sequence and reads real frame timings; it changes nothing on the TV.

```bash
tools/theme.sh themes/default
tools/venv/bin/python tools/bench.py --keys 36 --series 3 --out-dir results-default
tools/theme.sh themes/bhamley-glass
tools/venv/bin/python tools/bench.py --keys 36 --series 3 --out-dir results-glass
```

Pass: the glass run's jank ratio is no more than about two percentage points above the default run, and the median key
response stays under about 100 ms (my thresholds, the usual "feels instant" line). If it fails, set
`"focus_overshoot": false`, push and measure again; if it still fails, `"reduced_motion": true`.

## T3. What does the remote's mic button send? (point 6)

Everything about the voice orb depends on this, so it is tested before anything is built. Use plain `adb` (not `sudo adb`),
press and **hold** the mic button when told, and paste the output to me.

```bash
# a) which key is it? (Ctrl+C after pressing once)
adb shell getevent -lt
# b) what does Android think it is?
adb logcat -c; adb logcat -v time | grep -iE 'keycode|voice|assist|search|katniss'
# c) who starts recording while the button is held? (run b/c while holding)
adb shell dumpsys audio | grep -iE 'record|capture|source|client' | head -20
```

Good outcomes: a key such as `KEY_VOICECOMMAND`, `KEY_SEARCH` or Android's `KEYCODE_VOICE_ASSIST` / `KEYCODE_SEARCH`, and an
audio recording session opened by `com.google.android.katniss`. That means an app can listen for the key, and the open
question is only whether the remote's audio is also offered to other apps. If the audio never reaches Android at all, the
orb falls back to showing the animation while Assistant does the listening.

## T4. Picture quality (point 7)

```bash
adb shell getprop | grep -iE 'widevine|drm|hdcp|hdr'
adb shell dumpsys display | grep -iE 'hdr|mode'
adb shell pm list features | grep -iE 'hdr|4k|uhd'
```

On the TV, with a Netflix or YouTube title playing, open the player's stats (YouTube: Settings, Stats for nerds; Netflix
shows a playback-info overlay on many TV builds, search the web for your build's key sequence) and note the resolution
and codec actually streamed. Then check, and write down, what is available under Settings, Display and Sound: picture mode,
noise reduction and motion smoothing (off for streaming), and "match content frame rate" if it exists. The panel is
1920x1080 and software cannot add resolution, so the realistic win is the right picture mode, a 5 GHz or wired link and
the DRM level (Widevine L1 allows HD from Netflix and Prime; L3 caps them near SD).

## Acceptance criteria for what is not built yet

- **Game cards (1):** each game is a dock card; opens in under 2 s; every action reachable with the D-pad and OK/Back; steady
  frame rate in `bench.py` terms; Back always returns to Home. Super Over: one-button timing, three outcomes at least
  (miss, single, boundary). Wordle: English first.
- **Personalised rows (3):** after 10 likes in a genre that genre's row moves to the top; order survives a reboot; a Kids
  row never shows titles above the chosen age rating; the launcher itself still declares no INTERNET permission
  (`adb shell dumpsys package com.gathin.tvhome | grep -c INTERNET` prints 0).
- **Taste Deck (5):** left is dislike, right is like, up is "already seen"; 20 swipes survive a reboot; likes are stored on the
  TV only; a title opens in its app, or the app opens if that app has no deep link.

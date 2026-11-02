# Roadmap

Design rule, inherited from the launcher: the launcher itself stays small, with no internet permission and no
background services. Anything that needs the network (movie data, voice) is a **separate module app** that shows up
as a card on Home, as described in the launcher's [`docs/modules.md`](https://github.com/Gathin23/cleanyourtv/blob/main/docs/modules.md).

Household: a 9-year-old boy watches too, so every phase keeps a kid-safe shelf in mind, and nothing about what anyone
watches or likes leaves the TV.

## Phase 1: this repo today

- [x] Nokia ATV R2 debloat list, checked against the TV's own package list
- [x] `bhamley-glass` theme: three time-of-day wallpapers, frosted dock, app-coloured focus glow, springy highlight
- [ ] On-TV check: screenshots of the new Home, `tools/bench.py` for smoothness, decide `cinematic_home` on or off

## Phase 2: launcher fork (look and feel)

Needs a fork of the launcher, since themes only change keys. All of it must run on Android 11 and 2 GB, where
real-time blur is too expensive, so the glass is built from pre-blurred layers.

- [ ] Liquid-glass tiles: translucent body, thin specular rim lit from the upper left, soft caustic on the lower edge
- [ ] Focus physics: critically damped springs with a small overshoot, parallax on the wallpaper layers, tile lift
- [ ] Personalised rows: *Thrillers*, *Action*, *Animation* (the household's picks), plus a *Kids* shelf
- [ ] Per-element polish pass on a real screenshot: spacing, type scale, safe margins, focus ring contrast

## Phase 3: modules

- [ ] **Games** (D-pad only): *Super Over* (one-button cricket timing game), *Snake*, *Wordle* (English first)
- [ ] **Taste Deck**: poster cards, left on the D-pad is dislike, right is like, up for "already seen". Posters and
  metadata from TMDB (free key); likes stored on the TV only and used to order the personalised rows. Streaming apps do
  not share watch history or recommendations, so this is our own taste model. Opening a title uses deep links:
  reliable for Netflix, hit-or-miss for Hotstar and Prime, which may only open the app.

## Phase 4: voice orb ("Jarvis")

- [ ] Prototype first: find out what the remote's mic key sends (voice-assist or search key) and whether a third-party
  app can get the remote's audio. This decides everything else, and Assistant stays the fallback.
- [ ] Animated orb that listens, understands "play <title> on <app>", and shows result cards to pick from
- [ ] Optional: a language model for looser requests ("something scary but not too scary for my son")

## Picture quality: what is realistic

The panel is 1920x1080 and the board is an entry-level Realtek, so software cannot add resolution. What can help:

- Picture mode: Movie or Standard, with noise reduction and motion smoothing off for streaming
- Match content frame rate, if the TV exposes it (Display and Sound, advanced display settings)
- A 5 GHz Wi-Fi link or Ethernet, and the apps' streaming quality set to the highest
- Finding out the DRM level (Widevine L1 or L3), which caps Netflix and Prime resolution. The usual
  `dumpsys media.drm` is not available on this firmware, so try:

```bash
adb shell getprop | grep -iE 'widevine|drm|hdcp|hdr'
adb shell dumpsys display | grep -iE 'hdr|mode'
adb shell pm list features | grep -iE 'hdr|4k|uhd'
```

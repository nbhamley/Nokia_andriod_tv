# Roadmap

Design rule, inherited from the launcher: the launcher itself stays small, with no internet permission and no
background services. Anything that needs the network (movie data for Favorites and Swipe) is a **separate module app**
that talks to the launcher, as described in the launcher's
[`docs/modules.md`](https://github.com/Gathin23/cleanyourtv/blob/main/docs/modules.md).

Household: a 9-year-old boy watches too, so there is a Kids shelf in the plan, and nothing about what anyone watches or
likes leaves the TV.

## Decisions

- **Skipped for now:** the arcade (Super Over, Snake, Wordle) and the voice orb ("Jarvis"), to protect the 2 GB RAM
  budget. The remote's mic button keeps opening Assistant. Both can come back later as separate modules.
- **Home is liquid-glass inspired**, laid out as below. The design is a Design canvas called *Nokia TV Home, liquid glass*
  (Home plus the three screensavers); changes are agreed there before any code is written.
- **Dates and prices of subscriptions are typed in by the owner.** The TV has no way to know them, and it never reads
  bank, SMS or email data.

## Home layout (1920x1080, with a TV-safe margin of 96 px sides and 56 px top)

```
 9:41 PM              |  Favorites                    |  Swipe
 Monday, 2 November   |  4 picks: thriller, action,   |  stacked poster card
 Pune, Sunrise 6:35   |  animation (focus moves here) |  Dislike <-   -> Like
 ---------------------+-------------------------------+-----------------------
 Streaming            | Video | Inputs         | System          (glass panel,
 Netflix Prime Hotstar Apple TV | YouTube | HDMI 1, HDMI 2, AV | Wi-Fi, Settings   apps in sections)
```

1. Date, time and place, top left (the sunrise or sunset time is computed on the TV, no network).
2. **Favorites**, top centre: suggested shows and movies, with the app each one opens in.
3. **Swipe**, top right: a stacked poster card; left on the D-pad is dislike, right is like.
4. Apps, bottom, in labelled sections inside one frosted panel (Streaming, Video, Inputs, System).

## Screensaver

An iOS-style clock that follows the day: very large thin time, the date above it, and under it the place and the next sun
event. A small glass widget lists **upcoming subscription renewals** (name, date, days left). A slim bar at the bottom shows
where the clock sits in the day (night, morning, evening) and the wallpaper changes with it. The clock drifts to a new spot
each minute, so the panel never burns in.

The three moods already exist as wallpapers. To follow the *real* sun the launcher needs coordinates, which are
`latitude` and `longitude` in its `config.xml` (not theme keys): Pune is 18.5204 and 73.8567. Without them the switch happens
at 07:00 and 18:30.

## Phase 1: this repo today

- [x] Nokia ATV R2 debloat list, checked against the TV's own package list
- [x] `bhamley-glass` theme: three time-of-day wallpapers, frosted dock, app-coloured focus glow, springy highlight
- [x] Offline checks (`scripts/validate.py`) and a run of the launcher project's own test suite
- [ ] On-TV check: screenshots of the new Home, `tools/bench.py` for smoothness

## Phase 2: launcher fork (layout and look)

**Started as a separate app, `glasshome/`** (installs next to Clean Your TV, which stays as the fallback), because themes only change keys and the upstream launcher has no such layout. Everything must run on Android 11 and 2 GB, where
real-time blur is too expensive, so the glass is built from pre-blurred wallpapers, translucent layers and a specular rim.

- [ ] Top row: date and time block, Favorites shelf, Swipe card (the launcher has no such layout yet)
- [ ] Apps in sections, inside one glass panel (the launcher has a flat grid, no sections)
- [ ] Screensaver: the layout above, plus `latitude` and `longitude` set to Pune
- [ ] Renewals widget reading a small JSON file on the TV (pushed with `tools/tv.py --push`; an on-screen editor later)
- [ ] Glass tiles and focus physics: specular rim, soft caustic, critically damped springs with a small overshoot

How it is built and tested: the sandbox that writes the code cannot reach the Android SDK, so every Java change is built by
you (or by GitHub Actions on this repo) and tested on the TV; it is delivered in small steps so a compile error is easy to
find.

## Phase 3: modules (need the network, so they are separate apps)

- [ ] **Favorites and Swipe data:** posters and metadata from TMDB (free key). Likes are stored on the TV only and order the
  Favorites row (thriller, action, animation, plus a Kids shelf). Streaming apps do not share watch history or
  recommendations, so this is our own taste model. Opening a title uses deep links: reliable for Netflix, hit-or-miss for
  Hotstar and Prime, which may only open the app.

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

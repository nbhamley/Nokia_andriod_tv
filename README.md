# Nokia ATV R2: a clean, glassy home screen

A calmer, ad-free Home for a **Nokia ATV R2** Android TV, built on top of the open-source
[Clean Your TV](https://github.com/Gathin23/cleanyourtv) launcher (MIT). This repo holds what is specific to this TV:

| Folder | What it is |
|---|---|
| [`device/nokia-atv-r2/`](device/nokia-atv-r2/) | The debloat list and `tv.json` for this model, checked against the TV's own package list |
| [`themes/bhamley-glass/`](themes/bhamley-glass/) | A liquid-glass theme: morning, evening and night wallpapers (drawn in code), frosted dock, glowing focus |
| [`tools/make_wallpapers.py`](tools/make_wallpapers.py) | Regenerates the wallpapers, same seed gives the same pixels |
| [`scripts/`](scripts/) | `setup.sh` (fresh computer) and `overlay.sh` (copy our files into an existing clone) |
| [`docs/ROADMAP.md`](docs/ROADMAP.md) | What comes next: personalised rows, games, like/dislike deck, voice orb, picture quality |

## The TV

| | |
|---|---|
| Model | Nokia ATV R2, manufacturer SCBC, board `rtd285o` (Realtek) |
| Firmware | Android TV 11, `NOKIA/NOKIA/R2:11/RTT7.220308.001/1050` |
| Screen / memory | 1920x1080 at 320 dpi, 2 GB RAM |
| Apps the owner installed | Hotstar, Apple TV (Netflix and Prime Video come preloaded) |

## Setup

You need a computer on the same Wi-Fi as the TV, with `git`, `python3`, JDK 17 to 22 (a full JDK; JDK 23 and newer
stops the Gradle build) and Android SDK platform 34 with build-tools 34.0.0.

On the TV: Settings, Device Preferences, About, press OK on **Build** seven times, then Developer options and turn on
**USB debugging** (and network debugging if you see it). Keep the TV on, and accept "Allow USB debugging?" on first connect.

```bash
git clone git@github.com:nbhamley/Nokia_andriod_tv.git
cd Nokia_andriod_tv
scripts/setup.sh                       # clones the launcher project, pins it, copies our files in
# already have a clone?  scripts/overlay.sh /path/to/cleanyourtv

cd cleanyourtv
export TV_HOST=<tv ip>
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64      # any JDK from 17 to 22

tools/venv/bin/python -I tools/tv.py --probe             # model, Android version, firmware: should say ATV R2

launcher/build-and-install.sh                            # 1. the new Home first
tools/theme.sh themes/bhamley-glass                      # 2. the look
tools/venv/bin/python -I tools/tv.py 'settings put secure screensaver_components com.gathin.tvhome/.ClockDream'
                                                         # 3. the clock screensaver (the list turns off Google's)
tools/disable.sh examples/nokia-atv-r2/debloat.txt       # 4. the debloat, last
```

**Order matters.** The debloat list disables the stock launcher, so the new launcher must be installed first; the
scripts refuse to disable the only launcher, but do not rely on that.

## What is kept, what goes

Kept on purpose: the remote's mic button (Google Assistant), Chromecast built-in, Netflix, Prime Video, Hotstar,
Apple TV, YouTube, the remote and key middleware, HDMI and TV inputs, and firmware updates.

Disabled: the data-consent agent, recommendation rows and S-Channel promos, partner auto-installs, Vewd, E-SHOW and
sticker overlays, TV Booster, the speed overlay, WorldClock, Wi-Fi hotspot, the Play Movies and YouTube Music preloads,
the print service, factory tools and the stock launcher. Live TV is not used on this set, so it is simply left off the
dock; its input services stay enabled because HDMI shares them.

Everything is *disabled*, not uninstalled, and the TV records its state first.

## Undo

```bash
tools/restore.sh examples/nokia-atv-r2/debloat.txt       # re-enables the list and hands Home back to the stock launcher
tools/theme.sh --reset                                   # drops the theme
```

## Tuning the theme

The theme is plain JSON ([`config.json`](themes/bhamley-glass/config.json)); every key is documented in the launcher's
[themes/README.md](https://github.com/Gathin23/cleanyourtv/blob/main/themes/README.md#keys). Two switches worth trying on the TV:

- `"cinematic_home": true` swaps the wallpaper hero for a top shelf with the focused app's artwork.
- If the highlight stutters on this 2 GB set, set `"focus_overshoot": false` and then `"reduced_motion": true`.
  `tools/bench.py` in the launcher project measures key latency and janky frames before and after.

Push again with `tools/theme.sh themes/bhamley-glass` after any change.

## Credits

Launcher, tools and theme engine: [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv) (MIT). The wallpapers
here are generated in code and released under CC0, see [`themes/bhamley-glass/CREDITS.md`](themes/bhamley-glass/CREDITS.md).

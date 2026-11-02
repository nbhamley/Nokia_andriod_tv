# Glass Home

A small, separate Home app that draws the mockup: date, time and place top left, **Favorites** in the top centre,
**Swipe** (like or dislike) top right, and the apps in sections along the bottom, plus a screensaver that follows
sunrise and sunset and lists subscription renewals.

- Its own package, `com.nbhamley.glasshome`: it installs **next to** Clean Your TV, which stays as the fallback.
- Plain Android views, no libraries, **no internet permission**, nothing runs in the background.
- Wallpapers: the three moods from `themes/bhamley-glass`, picked by the time of day and your sunrise and sunset.
- Favorites and Swipe use a starter list in `app/src/main/assets/catalog.json` (titles, which app, genre). It is not
  live: edit it as you like. Likes and dislikes stay on the TV and move genres up or down. A tile opens the streaming
  app (the apps do not offer a way to jump to one title, so it opens on the app's home).
- Inputs (HDMI 1, 2, AV) come from the TV itself. "All apps" lists everything else.

## Build

The sandbox that wrote this code could not compile it, so GitHub builds it: push the repo, open
**Actions, Build Glass Home**, and download the `glasshome-apk` artifact (`app-debug.apk`). A red build names the
file and line; send me that text.

## Install and try (Clean Your TV stays Home)

```bash
export TV_HOST=192.168.1.8
glasshome/install.sh ~/Documents/scaler/Project/cleanyourtv ~/Downloads/app-debug.apk
```

It opens once on the TV. When you like it:

```bash
glasshome/install.sh ~/Documents/scaler/Project/cleanyourtv ~/Downloads/app-debug.apk --home
glasshome/install.sh ~/Documents/scaler/Project/cleanyourtv --undo        # back to Clean Your TV
```

## Your data

```bash
glasshome/set_data.sh <clone> renewals "Netflix=2026-11-12;Prime Video=2026-11-28;Hotstar=2026-12-03"
glasshome/set_data.sh <clone> place "Pune, India" 18.5204 73.8567
glasshome/set_data.sh <clone> reset-taste
```

Only the dates you type in are shown; the TV cannot know them.
